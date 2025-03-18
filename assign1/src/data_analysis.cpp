#include <fstream>
#include <iostream>
#include <string>
#include <vector>
#include "benchmark_result.h"
#include "papi_utils.h"

using namespace std;

extern BenchmarkResult OnMult(int m_ar, int m_br, int EventSet);
extern BenchmarkResult OnMultLine(int m_ar, int m_br, int EventSet);
extern BenchmarkResult OnMultBlock(int m_ar, int m_br, int bkSize, int EventSet);
extern BenchmarkResult OnMultLineParallelOuterFor(int m_ar, int m_br, int EventSet, int numThreads);
extern BenchmarkResult OnMultLineParallelInnerFor(int m_ar, int m_br, int EventSet, int numThreads);

void runTest(const string &methodName, BenchmarkResult (*func)(int, int, int), int lin, int col, int EventSet, ofstream &csv)
{

    BenchmarkResult result = func(lin, col, EventSet);

    csv << methodName << "," << lin << ",NA," << result.timeSeconds << "," << result.gflops << ","
        << result.papiL1DCM << "," << result.papiL2DCM << ",NA" << "\n";

    csv.flush();
}

void runTestBlock(const string &methodName, BenchmarkResult (*func)(int, int, int, int), int lin, int col, int blockSize, int EventSet, ofstream &csv)
{

    BenchmarkResult result = func(lin, col, EventSet, blockSize);

    csv << methodName << "," << lin << "," << blockSize << "," << result.timeSeconds << "," << result.gflops << ","
        << result.papiL1DCM << "," << result.papiL2DCM << ",NA" << "\n";

    csv.flush();
}

void runTestMulticore(const string &methodName,
                      BenchmarkResult (*func)(int, int, int, int),
                      int lin, int col, int EventSet, int numThreads, ofstream &csv)
{
    BenchmarkResult result = func(lin, col, EventSet, numThreads);
    csv << methodName << "," << lin << ",NA," << result.timeSeconds << ","
        << result.gflops << "," << result.papiL1DCM << "," << result.papiL2DCM
        << "," << numThreads << "\n";
    csv.flush();
}

int main()
{
    vector<int> sizes_small = {600, 1000, 1400, 1800, 2200, 2600, 3000};

    vector<int> sizes_large = {4096, 6144, 8192, 10240};

    vector<int> block_sizes = {128, 256, 512, 1024};

    vector<int> thread_counts = {2, 4, 8, 12};

    ofstream csv("data/benchmark_results.csv", ios::out);
    if (!csv.is_open())
    {
        cerr << "Error opening data/benchmark_results.csv" << endl;
        return 1;
    }
    csv << "Method,MatrixSize,BlockSize,Time(s),GFLOPS,PAPI_L1_DCM,PAPI_L2_DCM,NumThreads\n";

    int EventSet = PAPI_NULL;
    int ret = PAPI_library_init(PAPI_VER_CURRENT);
    if (ret != PAPI_VER_CURRENT)
    {
        cerr << "PAPI library init error" << endl;
        return 1;
    }
    ret = PAPI_create_eventset(&EventSet);
    if (ret != PAPI_OK)
        handle_error(ret);

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        handle_error(ret);

    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        handle_error(ret);

    // Run tests from matrixproduct.cpp and multicore_matrixproduct.cpp for small sizes
    for (int n : sizes_small)
    {
        runTest("OnMult", OnMult, n, n, EventSet, csv);
        runTest("OnMultLine", OnMultLine, n, n, EventSet, csv);
        for (int t : thread_counts)
        {
            runTestMulticore("OnMultLineParallelOuterFor", OnMultLineParallelOuterFor, n, n, EventSet, t, csv);
            runTestMulticore("OnMultLineParallelInnerFor", OnMultLineParallelInnerFor, n, n, EventSet, t, csv);
        }
    }

    // For block multiplication, run tests on large sizes for the different block sizes
    for (int n : sizes_large)
    {
        for (int bs : block_sizes)
        {
            runTestBlock("OnMultBlock", OnMultBlock, n, n, bs, EventSet, csv);
        }
    }

    ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        handle_error(ret);

    ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        handle_error(ret);

    ret = PAPI_destroy_eventset(&EventSet);
    if (ret != PAPI_OK)
        handle_error(ret);

    csv.close();
    cout << "Benchmark results saved to benchmark_results.csv" << endl;
    return 0;
}