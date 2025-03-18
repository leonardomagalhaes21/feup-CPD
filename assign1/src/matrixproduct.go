package main

import (
    "fmt"
    "os"
    "time"
)

func OnMult(m_ar, m_br int) float64 {
    pha := make([]float64, m_ar*m_ar)
    phb := make([]float64, m_ar*m_ar)
    phc := make([]float64, m_ar*m_ar)

    for i := 0; i < m_ar; i++ {
        for j := 0; j < m_ar; j++ {
            pha[i*m_ar+j] = 1.0
        }
    }

    for i := 0; i < m_br; i++ {
        for j := 0; j < m_br; j++ {
            phb[i*m_br+j] = float64(i + 1)
        }
    }

    start := time.Now()

    for i := 0; i < m_ar; i++ {
        for j := 0; j < m_br; j++ {
            temp := 0.0
            for k := 0; k < m_ar; k++ {
                temp += pha[i*m_ar+k] * phb[k*m_br+j]
            }
            phc[i*m_ar+j] = temp
        }
    }

    elapsed := time.Since(start)
    elapsedSeconds := elapsed.Seconds()

    // Optional: Display sample results for verification
    if m_ar <= 10 {
        fmt.Println("Result matrix:")
        for i := 0; i < 1; i++ {
            for j := 0; j < min(10, m_br); j++ {
                fmt.Printf("%v ", phc[j])
            }
        }
        fmt.Println()
    }

    return elapsedSeconds
}

func OnMultLine(m_ar, m_br int) float64 {
    pha := make([]float64, m_ar*m_ar)
    phb := make([]float64, m_ar*m_ar)
    phc := make([]float64, m_ar*m_ar)

    for i := 0; i < m_ar; i++ {
        for j := 0; j < m_ar; j++ {
            pha[i*m_ar+j] = 1.0
        }
    }

    for i := 0; i < m_br; i++ {
        for j := 0; j < m_br; j++ {
            phb[i*m_br+j] = float64(i + 1)
        }
    }

    start := time.Now()

    for i := 0; i < m_ar; i++ {
        for k := 0; k < m_br; k++ {
            for j := 0; j < m_ar; j++ {
                phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_br+j]
            }
        }
    }

    elapsed := time.Since(start)
    elapsedSeconds := elapsed.Seconds()

    // Optional: Display sample results for verification
    if m_ar <= 10 {
        fmt.Println("Result matrix:")
        for i := 0; i < 1; i++ {
            for j := 0; j < min(10, m_br); j++ {
                fmt.Printf("%v ", phc[j])
            }
        }
        fmt.Println()
    }

    return elapsedSeconds
}

func calculateGflops(matrixSize int, timeSeconds float64) float64 {
    // For matrix multiplication: 2*N³ operations (N³ multiplications and N³ additions)
    operations := 2.0 * float64(matrixSize) * float64(matrixSize) * float64(matrixSize)
    return operations / (timeSeconds * 1e9) // Convert to GFLOPS
}

func runBenchmarks() {
    // Create or open CSV file
    file, err := os.Create("data/benchmark_results_go.csv")
    if err != nil {
        fmt.Println("Error creating file:", err)
        return
    }
    defer file.Close()

    // Write CSV header
    file.WriteString("Method,MatrixSize,BlockSize,Time(s),GFLOPS\n")

    // Matrix sizes to benchmark
    matrixSizes := []int{600, 1000, 1400, 1800, 2200, 2600, 3000}

    for _, size := range matrixSizes {
        fmt.Printf("Running benchmarks for size %d...\n", size)

        // Standard multiplication
        fmt.Printf("  OnMult %dx%d...\n", size, size)
        timeStandard := OnMult(size, size)
        gflopsStandard := calculateGflops(size, timeStandard)
        
        // Element-line multiplication
        fmt.Printf("  OnMultLine %dx%d...\n", size, size)
        timeLine := OnMultLine(size, size)
        gflopsLine := calculateGflops(size, timeLine)

        // Write results to CSV
        // Using placeholder values for PAPI metrics (these would need actual hardware counters)
        fmt.Fprintf(file, "OnMult,%d,NA,%f,%f\n", size, timeStandard, gflopsStandard)
        fmt.Fprintf(file, "OnMultLine,%d,NA,%f,%f\n", size, timeLine, gflopsLine)
    }

    fmt.Println("Benchmarks completed. Results saved to matrix_benchmark.csv")
}

func min(a, b int) int {
    if a < b {
        return a
    }
    return b
}

func main() {
    var choice int
    fmt.Println("1. Run single matrix multiplication")
    fmt.Println("2. Run benchmarks and export to CSV")
    fmt.Scan(&choice)

    switch choice {
    case 1:
        var size int
        fmt.Print("Matrix size? ")
        fmt.Scan(&size)

        fmt.Println("Choose multiplication method:")
        fmt.Println("1. Standard Multiplication")
        fmt.Println("2. Element-Line Multiplication")
        fmt.Scan(&choice)

        switch choice {
        case 1:
            elapsed := OnMult(size, size)
            fmt.Printf("Time: %f seconds\n", elapsed)
        case 2:
            elapsed := OnMultLine(size, size)
            fmt.Printf("Time: %f seconds\n", elapsed)
        default:
            fmt.Println("Invalid choice")
        }

    case 2:
        runBenchmarks()

    default:
        fmt.Println("Invalid choice")
    }
}