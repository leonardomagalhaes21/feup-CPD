#include <papi.h>
#include <chrono>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

using namespace std;
using namespace std::chrono;

double computeGFLOPS(int n, double elapsedSec) {
    return (2.0 * n * n * n) / (elapsedSec * 1e9);
}

void runTest(const string &methodName, void (*func)(int, int), int lin, int col, int EventSet, ofstream &csv) {
    long long values[2] = {0, 0};

    int ret = PAPI_start(EventSet);
    if(ret != PAPI_OK) handle_error(ret);

    auto start = high_resolution_clock::now();
    
    func(lin, col);

    auto end = high_resolution_clock::now();

    ret = PAPI_stop(EventSet, values);
    if(ret != PAPI_OK) handle_error(ret);

    duration<double> elapsed = end - start;
    double gflops = computeGFLOPS(lin, elapsed.count());
    csv << methodName << "," << lin << ",NA," << elapsed.count() << "," << gflops << ","
        << values[0] << "," << values[1] << "\n";

    csv.flush();
    ret = PAPI_reset(EventSet);
}

void runTestBlock(const string &methodName, void (*func)(int, int, int), int lin, int col, int blockSize, int EventSet, ofstream &csv) {
    long long values[2] = {0, 0};

    int ret = PAPI_start(EventSet);
    if(ret != PAPI_OK) handle_error(ret);

    auto start = high_resolution_clock::now();

    func(lin, col, blockSize);

    auto end = high_resolution_clock::now();

    ret = PAPI_stop(EventSet, values);
    if(ret != PAPI_OK) handle_error(ret);

    duration<double> elapsed = end - start;
    double gflops = computeGFLOPS(lin, elapsed.count());
    csv << methodName << "," << lin << "," << blockSize << "," 
        << elapsed.count() << "," << gflops << "," << values[0] << "," << values[1] << "\n";

    csv.flush();
    ret = PAPI_reset(EventSet);
}

int main() {
    vector<int> sizes_small = {600, 1000, 1400, 1800, 2200, 2600, 3000};

    vector<int> sizes_large = {4096, 6144, 8192, 10240};
    
    vector<int> block_sizes = {128, 256, 512, 1024};
    

    ofstream csv("data/benchmark_results.csv", ios::out);
    if (!csv.is_open()) {
        cerr << "Error opening data/benchmark_results.csv" << endl;
        return 1;
    }
    csv << "Method,MatrixSize,BlockSize,Time(s),GFLOPS,PAPI_L1_DCM,PAPI_L2_DCM\n";
    
    int EventSet = PAPI_NULL;
    int ret = PAPI_library_init(PAPI_VER_CURRENT);
    if(ret != PAPI_VER_CURRENT) {
        cerr << "PAPI library init error" << endl;
        return 1;
    }
    ret = PAPI_create_eventset(&EventSet);
    if(ret != PAPI_OK) handle_error(ret);

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if(ret != PAPI_OK) handle_error(ret);
    
    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if(ret != PAPI_OK) handle_error(ret);
    
    // Run tests from matrixproduct.cpp and multicore_matrixproduct.cpp for small sizes
    for (int n : sizes_small) {
        runTest("OnMult", OnMult, n, n, EventSet, csv);
        runTest("OnMultLine", OnMultLine, n, n, EventSet, csv);
        runTest("OnMultLineParallelOuterFor", OnMultLineParallelOuterFor, n, n, EventSet, csv);
        runTest("OnMultLineParallelInnerFor", OnMultLineParallelInnerFor, n, n, EventSet, csv);
    }
    
    // For block multiplication, run tests on large sizes for the different block sizes
    for (int n : sizes_large) {
        for (int bs : block_sizes) {
            runTestBlock("OnMultBlock", OnMultBlock, n, n, bs, EventSet, csv);
        }
    }
    
    ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
    if(ret != PAPI_OK) handle_error(ret);

    ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
    if(ret != PAPI_OK) handle_error(ret);

    ret = PAPI_destroy_eventset(&EventSet);
    if(ret != PAPI_OK) handle_error(ret);
    
    csv.close();
    cout << "Benchmark results saved to benchmark_results.csv" << endl;
    return 0;
}