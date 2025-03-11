package main

import (
    "fmt"
    "time"
)

func OnMult(m_ar, m_br int) {
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
    fmt.Printf("Time: %s\n", elapsed)

    // display 10 elements of the result matrix to verify correctness
    fmt.Println("Result matrix:")
    for i := 0; i < 1; i++ {
        for j := 0; j < min(10, m_br); j++ {
            fmt.Printf("%v ", phc[j])
        }
    }
    fmt.Println()
}

func OnMultElementLine(m_ar, m_br int) {
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
    fmt.Printf("Time: %s\n", elapsed)

    // display 10 elements of the result matrix to verify correctness
    fmt.Println("Result matrix:")
    for i := 0; i < 1; i++ {
        for j := 0; j < min(10, m_br); j++ {
            fmt.Printf("%v ", phc[j])
        }
    }
    fmt.Println()
}

func min(a, b int) int {
    if a < b {
        return a
    }
    return b
}

func main() {
    var lin, col, choice int
    fmt.Print("Dimensions: lins=cols ? ")
    fmt.Scan(&lin)
    col = lin

    fmt.Println("Choose multiplication method:")
    fmt.Println("1. Standard Multiplication")
    fmt.Println("2. Element-Line Multiplication")
    fmt.Scan(&choice)

    switch choice {
    case 1:
        OnMult(lin, col)
    case 2:
        OnMultElementLine(lin, col)
    default:
        fmt.Println("Invalid choice")
    }
}