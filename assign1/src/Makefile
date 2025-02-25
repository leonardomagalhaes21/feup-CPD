CC = g++
CFLAGS = -O2

SRC = matrixproduct.cpp
TARGET = matrixproduct
LDFLAGS = -lpapi

SRC_MULTICORE = multicore_matrixproduct.cpp
TARGET_MULTICORE = multicore_matrixproduct
LDFLAGS_MULTICORE = -fopenmp -lpapi

.PHONY: all clean singlecore multicore run_singlecore run_tests run_multicore

all: singlecore multicore

clean:
	rm -f $(TARGET) $(TARGET_MULTICORE)

singlecore: $(SRC)
	$(CC) $(CFLAGS) $< -o $(TARGET) $(LDFLAGS)

run_singlecore: singlecore
	./$(TARGET)

run_tests: singlecore
	./$(TARGET) singlecore$$(date +%s%N).csv

multicore: $(SRC_MULTICORE)
	$(CC) $(CFLAGS) $< -o $(TARGET_MULTICORE) $(LDFLAGS_MULTICORE)

run_multicore: multicore
	./$(TARGET_MULTICORE)
