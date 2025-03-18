#include <stdio.h>
#include <iostream>
#include <chrono>
#include "benchmark_result.h"
#include "papi_utils.h"

using namespace std;

BenchmarkResult OnMult(int m_ar, int m_br, int EventSet)
{
    long long values[2];

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = (double)1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    int ret = PAPI_start(EventSet);
    if (ret != PAPI_OK)
        handle_error(ret);
    auto start = chrono::high_resolution_clock::now();

    for (i = 0; i < m_ar; i++)
    {
        for (j = 0; j < m_br; j++)
        {
            temp = 0;
            for (k = 0; k < m_ar; k++)
            {
                temp += pha[i * m_ar + k] * phb[k * m_br + j];
            }
            phc[i * m_ar + j] = temp;
        }
    }

    auto end = chrono::high_resolution_clock::now();
    ret = PAPI_stop(EventSet, values);
    if (ret != PAPI_OK)
        handle_error(ret);

    printf("L1 DCM: %lld \n", values[0]);
    printf("L2 DCM: %lld \n", values[1]);

    double elapsedTime = chrono::duration<double>(end - start).count();

    sprintf(st, "Time: %3.3f seconds\n", elapsedTime);
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
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

// add code here for line x line matriz multiplication
BenchmarkResult OnMultLine(int m_ar, int m_br, int EventSet)
{
    long long values[2];

    char st[100];
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = (double)1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    int ret = PAPI_start(EventSet);
    if (ret != PAPI_OK)
        handle_error(ret);
    auto start = chrono::high_resolution_clock::now();

    for (i = 0; i < m_ar; i++)
    {
        for (k = 0; k < m_br; k++)
        {
            for (j = 0; j < m_ar; j++)
            {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    auto end = chrono::high_resolution_clock::now();

    ret = PAPI_stop(EventSet, values);
    if (ret != PAPI_OK)
        handle_error(ret);

    printf("L1 DCM: %lld \n", values[0]);
    printf("L2 DCM: %lld \n", values[1]);

    double elapsedTime = chrono::duration<double>(end - start).count();

    sprintf(st, "Time: %3.3f seconds\n", elapsedTime);
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
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

// add code here for block x block matriz multiplication
BenchmarkResult OnMultBlock(int m_ar, int m_br, int bkSize, int EventSet)
{
    long long values[2];

    char st[100];
    int i, j, k, x, y;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = i + 1;

    int ret = PAPI_start(EventSet);
    if (ret != PAPI_OK)
        handle_error(ret);
    auto start = chrono::high_resolution_clock::now();

    for (x = 0; x < m_ar; x += bkSize)
    {
        for (y = 0; y < m_ar; y += bkSize)
        {
            for (i = 0; i < m_ar; i++)
            {
                for (k = y; k < min(y + bkSize, m_ar); k++)
                {
                    for (j = x; j < min(x + bkSize, m_ar); j++)
                    {
                        phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                    }
                }
            }
        }
    }

    auto end = chrono::high_resolution_clock::now();
    ret = PAPI_stop(EventSet, values);
    if (ret != PAPI_OK)
        handle_error(ret);

    printf("L1 DCM: %lld \n", values[0]);
    printf("L2 DCM: %lld \n", values[1]);

    double elapsedTime = chrono::duration<double>(end - start).count();

    sprintf(st, "Time: %3.3f seconds\n", elapsedTime);
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
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

void init_papi()
{
    int retval = PAPI_library_init(PAPI_VER_CURRENT);
    if (retval != PAPI_VER_CURRENT && retval < 0)
    {
        printf("PAPI library version mismatch!\n");
        exit(1);
    }
    if (retval < 0)
        handle_error(retval);

    std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
              << " MINOR: " << PAPI_VERSION_MINOR(retval)
              << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

#ifndef DATA_ANALYSIS_BUILD
int main(int argc, char *argv[])
{
    char c;
    int lin, col, blockSize;
    int op;

    int EventSet = PAPI_NULL;
    int ret;

    ret = PAPI_library_init(PAPI_VER_CURRENT);
    if (ret != PAPI_VER_CURRENT)
        std::cout << "FAIL" << endl;

    ret = PAPI_create_eventset(&EventSet);
    if (ret != PAPI_OK)
        cout << "ERROR: create eventset" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L1_DCM" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L2_DCM" << endl;

    op = 1;
    do
    {
        cout << endl
             << "1. Multiplication" << endl;
        cout << "2. Line Multiplication" << endl;
        cout << "3. Block Multiplication" << endl;
        cout << "Selection?: ";
        cin >> op;
        if (op == 0)
            break;
        printf("Dimensions: lins=cols ? ");
        cin >> lin;
        col = lin;

        switch (op)
        {
        case 1:
            OnMult(lin, col, EventSet);
            break;
        case 2:
            OnMultLine(lin, col, EventSet);
            break;
        case 3:
            cout << "Block Size? ";
            cin >> blockSize;
            OnMultBlock(lin, col, blockSize, EventSet);
            break;
        }

    } while (op != 0);

    ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_destroy_eventset(&EventSet);
    if (ret != PAPI_OK)
        std::cout << "FAIL destroy" << endl;
}
#endif
