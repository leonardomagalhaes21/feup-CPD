#include <omp.h>
#include <iostream>
#include <vector>
#include <chrono>
#include "benchmark_result.h"
#include "papi_utils.h"

using namespace std;

BenchmarkResult OnMultLineParallelOuterFor(int m_ar, int m_br, int EventSet, int numThreads) {
    omp_set_num_threads(numThreads);

    char st[100];
    long long values[2];

    auto pha = vector<double>(m_ar * m_ar, 1.0);
    auto phb = vector<double>(m_ar * m_ar, 1.0);
    auto phc = vector<double>(m_ar * m_ar, 0.0);

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

    int i, k, j;

    int ret = PAPI_start(EventSet);
    if(ret != PAPI_OK) handle_error(ret);
    auto start = chrono::high_resolution_clock::now();

#pragma omp parallel for private(i, k, j)
    for (i = 0; i < m_ar; i++) {
        for (k = 0; k < m_br; k++) {
            for (j = 0; j < m_ar; j++) {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    auto end = chrono::high_resolution_clock::now();

    ret = PAPI_stop(EventSet, values);
    if(ret != PAPI_OK) handle_error(ret);

    printf("L1 DCM: %lld \n", values[0]);
    printf("L2 DCM: %lld \n", values[1]);

    double elapsedTime = chrono::duration<double>(end - start).count();

    sprintf(st, "Time: %3.3f seconds\n", elapsedTime);
    cout << st;

    // display 10 elements of the result matrix to verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++) {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    ret = PAPI_reset(EventSet);
    if (ret != PAPI_OK)
        std::cout << "FAIL reset" << endl;

    BenchmarkResult result;
    result.timeSeconds = elapsedTime;
    result.gflops = computeGFLOPS(m_ar, result.timeSeconds);
    result.papiL1DCM = values[0];
    result.papiL2DCM = values[1];
    return result;
}

BenchmarkResult OnMultLineParallelInnerFor(int m_ar, int m_br, int EventSet, int numThreads) {
    omp_set_num_threads(numThreads);

    char st[100];
    long long values[2];

    auto pha = vector<double>(m_ar * m_ar, 1.0);
    auto phb = vector<double>(m_ar * m_ar, 1.0);
    auto phc = vector<double>(m_ar * m_ar, 0.0);

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

    int i, k, j;

    int ret = PAPI_start(EventSet);
    if(ret != PAPI_OK) handle_error(ret);
    auto start = chrono::high_resolution_clock::now();

#pragma omp parallel private(i, k, j) num_threads(4)
    for (i = 0; i < m_ar; i++) {
        for (k = 0; k < m_br; k++) {
#pragma omp for
            for (j = 0; j < m_ar; j++) {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    auto end = chrono::high_resolution_clock::now();
    
    ret = PAPI_stop(EventSet, values);
    if(ret != PAPI_OK) handle_error(ret);

    printf("L1 DCM: %lld \n", values[0]);
    printf("L2 DCM: %lld \n", values[1]);

    double elapsedTime = chrono::duration<double>(end - start).count();
    
    sprintf(st, "Time: %3.3f seconds\n", elapsedTime);
    cout << st;

    // display 10 elements of the result matrix to verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++) {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    ret = PAPI_reset(EventSet);
    if (ret != PAPI_OK)
        std::cout << "FAIL reset" << endl;

    BenchmarkResult result;
    result.timeSeconds = elapsedTime;
    result.gflops = computeGFLOPS(m_ar, result.timeSeconds);
    result.papiL1DCM = values[0];
    result.papiL2DCM = values[1];
    return result;
}

#ifndef DATA_ANALYSIS_BUILD
int main(int argc, char *argv[]) {
    int lin, col, choice;
    cout << "Dimensions: lins=cols ? ";
    cin >> lin;
    col = lin;

    int EventSet = PAPI_NULL;
    int ret;

    ret = PAPI_library_init(PAPI_VER_CURRENT);
    if (ret != PAPI_VER_CURRENT)
        std::cout << "FAIL" << endl;

    ret = PAPI_create_eventset(&EventSet);
    if (ret != PAPI_OK) cout << "ERROR: create eventset" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK) cout << "ERROR: PAPI_L1_DCM" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCM" << endl;

    cout << "Choose multiplication method:" << endl;
    cout << "1. Line Multiplication Parallel Outer loop" << endl;
    cout << "2. Line Multiplication Parallel Inner loop" << endl;
    cin >> choice;

    switch (choice) {
        case 1:
            OnMultLineParallelOuterFor(lin, col, EventSet, 4);
            break;
        case 2:
            OnMultLineParallelInnerFor(lin, col, EventSet, 4);
            break;
        default:
            cout << "Invalid choice" << endl;
    }

    ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_destroy_eventset(&EventSet);
    if (ret != PAPI_OK)
        std::cout << "FAIL destroy" << endl;

    return 0;
}
#endif
