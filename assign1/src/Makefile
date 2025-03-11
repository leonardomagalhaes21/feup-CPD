CC = g++
CFLAGS = -O2

SRC = matrixproduct.cpp
TARGET = matrixproduct
LDFLAGS = -lpapi

SRC_MULTICORE = multicore_matrixproduct.cpp
TARGET_MULTICORE = multicore_matrixproduct
LDFLAGS_MULTICORE = -fopenmp -lpapi

DATA_SRC = data_analysis.cpp matrixproduct.cpp multicore_matrixproduct.cpp
DATA_TARGET = data_analysis
DATA_LDFLAGS = -fopenmp -lpapi

GO_SRC = matrixproduct.go

.PHONY: all clean singlecore multicore run_singlecore run_tests run_multicore

all: singlecore multicore

clean:
	rm -f $(TARGET) $(TARGET_MULTICORE) $(DATA_TARGET)

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

data_analysis: $(DATA_SRC)
	$(CC) $(CFLAGS) -DDATA_ANALYSIS_BUILD $^ -o $(DATA_TARGET) $(DATA_LDFLAGS)

run_data_analysis: data_analysis
	./$(DATA_TARGET)

run_go:
	go run $(GO_SRC)
