#include <omp.h>

#include <cstdlib>
#include <iomanip>
#include <iostream>
#include <vector>

using namespace std;

#define SYSTEMTIME clock_t

void OnMultLineParallelOuterFor(int m_ar, int m_br) {
    char st[100];

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

    double start = omp_get_wtime();

#pragma omp parallel for private(i, k, j)
    for (i = 0; i < m_ar; i++) {
        for (k = 0; k < m_br; k++) {
            for (j = 0; j < m_ar; j++) {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    double end = omp_get_wtime();
    sprintf(st, "Time: %3.3f seconds\n", end - start);
    cout << st;

    // display 10 elements of the result matrix to verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++) {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;
}

void OnMultLineParallelInnerFor(int m_ar, int m_br) {
    char st[100];

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

    double start = omp_get_wtime();

#pragma omp parallel private(i, k, j) num_threads(x)
    for (i = 0; i < m_ar; i++) {
        for (k = 0; k < m_br; k++) {
#pragma omp for
            for (j = 0; j < m_ar; j++) {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    double end = omp_get_wtime();
    sprintf(st, "Time: %3.3f seconds\n", end - start);
    cout << st;

    // display 10 elements of the result matrix to verify correctness
    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++) {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;
}

#ifndef DATA_ANALYSIS_BUILD
int main(int argc, char *argv[]) {
    int lin, col, choice;
    cout << "Dimensions: lins=cols ? ";
    cin >> lin;
    col = lin;

    cout << "Choose multiplication method:" << endl;
    cout << "1. Line Multiplication Parallel Outer loop" << endl;
    cout << "2. Line Multiplication Parallel Inner loop" << endl;
    cin >> choice;

    switch (choice) {
        case 1:
            OnMultLineParallelOuterFor(lin, col);
            break;
        case 2:
            OnMultLineParallelInnerFor(lin, col);
            break;
        default:
            cout << "Invalid choice" << endl;
    }

    return 0;
}
#endif
