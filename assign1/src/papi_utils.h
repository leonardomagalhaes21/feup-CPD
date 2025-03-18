#ifndef PAPI_UTILS_H
#define PAPI_UTILS_H

#include <papi.h>
#include <cstdlib>

inline void handle_error(int retval) {
    printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
    exit(1);
}

inline double computeGFLOPS(int n, double elapsedSec) {
    return (2.0 * n * n * n) / (elapsedSec * 1e9);
}

#endif // PAPI_UTILS_H