#ifndef BENCHMARK_RESULT_H
#define BENCHMARK_RESULT_H

struct BenchmarkResult {
    double timeSeconds;
    double gflops;
    long long papiL1DCM;
    long long papiL2DCM;
};

#endif // BENCHMARK_RESULT_H