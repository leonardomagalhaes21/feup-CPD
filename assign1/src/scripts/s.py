import pandas as pd
import matplotlib.pyplot as plt
import os
import numpy as np

# ====== Load DataFrames from CSV files ======
go_benchmark_files = [
    os.path.join("..", "data", "benchmark_results_go1.csv"),
    os.path.join("..", "data", "benchmark_results_go2.csv"),
    os.path.join("..", "data", "benchmark_results_go3.csv"),
    os.path.join("..", "data", "benchmark_results_go4.csv")
]

df_go_all = pd.DataFrame()

# Read and concatenate all Go benchmark files
for file in go_benchmark_files:
    try:
        df = pd.read_csv(file)
        df_go_all = pd.concat([df_go_all, df], ignore_index=True)
    except FileNotFoundError:
        print(f"Warning: Could not find file {file}")
        continue

# Handle 'NA' values in Go data
df_go_all = df_go_all.replace('NA', np.nan)

# Calculate the mean for each combination of Method and MatrixSize for Go data
df_go = df_go_all.groupby(['Method', 'MatrixSize'], as_index=False).mean()

# Load C++ benchmark result files and concatenate them
benchmark_files = [
    os.path.join("..", "data", "benchmark_results1.csv"),
    os.path.join("..", "data", "benchmark_results2.csv"),
    os.path.join("..", "data", "benchmark_results3.csv"),
    os.path.join("..", "data", "benchmark_results4.csv")
]

df_all = pd.DataFrame()

# Read and concatenate all C++ benchmark files
for file in benchmark_files:
    try:
        df = pd.read_csv(file)
        df_all = pd.concat([df_all, df], ignore_index=True)
    except FileNotFoundError:
        print(f"Warning: Could not find file {file}")
        continue

# Handle 'NA' values correctly in C++ data
df_all = df_all.replace('NA', np.nan)

# Calculate the mean for each combination of Method, MatrixSize for C++ data
df_line = df_all.groupby(['Method', 'MatrixSize'], as_index=False).mean()

# ====== Create subsets for each method ======
df_onmult = df_line[df_line["Method"] == "OnMult"].copy()
df_onmultline = df_line[df_line["Method"] == "OnMultLine"].copy()
df_onmultblock = df_line[df_line["Method"] == "OnMultBlock"].copy()
df_par_outer = df_line[df_line["Method"] == "OnMultLineParallelOuterFor"].copy()
df_par_inner = df_line[df_line["Method"] == "OnMultLineParallelInnerFor"].copy()

# Get data for Go implementations
df_go_onmult = df_go[df_go["Method"] == "OnMult"].copy()
df_go_onmultline = df_go[df_go["Method"] == "OnMultLine"].copy()

# ----- Define functions for each chart page -----

def chart1(ax):
    """C++ OnMult & OnMultLine (Time vs MatrixSize) vs GO OnMult & OnMultLine (Time vs MatrixSize)"""
    ax.cla()
    # Filter for matrix sizes up to 3000
    df_small_onmult = df_onmult[df_onmult["MatrixSize"] <= 3000]
    df_small_onmultline = df_onmultline[df_onmultline["MatrixSize"] <= 3000]
    
    # Filter GO data 
    df_small_go_onmult = df_go_onmult[df_go_onmult["MatrixSize"] <= 3000]
    df_small_go_onmultline = df_go_onmultline[df_go_onmultline["MatrixSize"] <= 3000]
    
    # C++ implementations
    ax.plot(df_small_onmult["MatrixSize"], df_small_onmult["Time(s)"], marker='o', label="C++ OnMult")
    ax.plot(df_small_onmultline["MatrixSize"], df_small_onmultline["Time(s)"], marker='s', label="C++ OnMultLine")
    
    # GO implementations
    ax.plot(df_small_go_onmult["MatrixSize"], df_small_go_onmult["Time(s)"], marker='^', label="GO OnMult")
    ax.plot(df_small_go_onmultline["MatrixSize"], df_small_go_onmultline["Time(s)"], marker='x', label="GO OnMultLine")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("C++ vs GO Implementations: Execution Time")
    ax.legend()
    ax.grid(True)

def chart2(ax):
    """OnMult vs OnMultLine L1 Cache Misses"""
    ax.cla()
    # Filter for matrix sizes up to 3000
    df_small_onmult = df_onmult[df_onmult["MatrixSize"] <= 3000]
    df_small_onmultline = df_onmultline[df_onmultline["MatrixSize"] <= 3000]
    
    # Plot directly without grouping, since we already grouped by Method and MatrixSize
    if len(df_small_onmult) > 0:
        ax.plot(df_small_onmult["MatrixSize"], df_small_onmult["PAPI_L1_DCM"], marker='o', label="OnMult L1 Cache Misses")
    
    if len(df_small_onmultline) > 0:
        ax.plot(df_small_onmultline["MatrixSize"], df_small_onmultline["PAPI_L1_DCM"], marker='s', label="OnMultLine L1 Cache Misses")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L1 Cache Misses")
    ax.set_title("OnMult vs OnMultLine: L1 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart3(ax):
    """OnMult vs OnMultLine L2 Cache Misses"""
    ax.cla()
    # Filter for matrix sizes up to 3000
    df_small_onmult = df_onmult[df_onmult["MatrixSize"] <= 3000]
    df_small_onmultline = df_onmultline[df_onmultline["MatrixSize"] <= 3000]
    
    if len(df_small_onmult) > 0:
        ax.plot(df_small_onmult["MatrixSize"], df_small_onmult["PAPI_L2_DCM"], marker='o', label="OnMult L2 Cache Misses")
    
    if len(df_small_onmultline) > 0:
        ax.plot(df_small_onmultline["MatrixSize"], df_small_onmultline["PAPI_L2_DCM"], marker='s', label="OnMultLine L2 Cache Misses")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L2 Cache Misses")
    ax.set_title("OnMult vs OnMultLine: L2 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart4(ax):
    """Compare Block sizes and Line (Time vs MatrixSize)"""
    ax.cla()
    large_sizes = [4096, 6144, 8192, 10240]
    df_large_line = df_onmultline[df_onmultline["MatrixSize"].isin(large_sizes)]
    
    # Line execution time
    if len(df_large_line) > 0:
        ax.plot(df_large_line["MatrixSize"], df_large_line["Time(s)"], marker='o', linestyle='-', label="OnMultLine")
    
    # Create a new DataFrame for displaying block results by matrix size
    df_block_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            block_data = df[df["Method"] == "OnMultBlock"]
            df_block_raw = pd.concat([df_block_raw, block_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    for bs in [128, 256, 512, 1024]:
        df_bs = df_block_raw[df_block_raw["BlockSize"] == bs]
        if len(df_bs) > 0:
            # Group by MatrixSize to get average time for each block size
            df_bs_avg = df_bs.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
            ax.plot(df_bs_avg["MatrixSize"], df_bs_avg["Time(s)"], 
                    marker='s', linestyle='--', label=f"Block Size {bs}")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("OnMultLine vs OnMultBlock: Execution Time")
    ax.legend()
    ax.grid(True)

def chart5(ax):
    """Compare Block sizes and Line (L1 Cache Misses vs MatrixSize)"""
    ax.cla()
    large_sizes = [4096, 6144, 8192, 10240]
    df_large_line = df_onmultline[df_onmultline["MatrixSize"].isin(large_sizes)]
    
    # Line L1 cache misses
    if len(df_large_line) > 0:
        ax.plot(df_large_line["MatrixSize"], df_large_line["PAPI_L1_DCM"], 
                marker='o', linestyle='-', label="OnMultLine")
    
    # Create a new DataFrame for displaying block results by matrix size
    df_block_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            block_data = df[df["Method"] == "OnMultBlock"]
            df_block_raw = pd.concat([df_block_raw, block_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    for bs in [128, 256, 512, 1024]:
        df_bs = df_block_raw[df_block_raw["BlockSize"] == bs]
        if len(df_bs) > 0:
            # Group by MatrixSize to get average L1 cache misses for each block size
            df_bs_avg = df_bs.groupby("MatrixSize", as_index=False)["PAPI_L1_DCM"].mean()
            ax.plot(df_bs_avg["MatrixSize"], df_bs_avg["PAPI_L1_DCM"], 
                    marker='s', linestyle='--', label=f"Block Size {bs}")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L1 Cache Misses")
    ax.set_title("OnMultLine vs OnMultBlock: L1 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart6(ax):
    """Compare Block sizes and Line (L2 Cache Misses vs MatrixSize)"""
    ax.cla()
    large_sizes = [4096, 6144, 8192, 10240]
    df_large_line = df_onmultline[df_onmultline["MatrixSize"].isin(large_sizes)]
    
    # Line L2 cache misses
    if len(df_large_line) > 0:
        ax.plot(df_large_line["MatrixSize"], df_large_line["PAPI_L2_DCM"], 
                marker='o', linestyle='-', label="OnMultLine")
    
    # Create a new DataFrame for displaying block results by matrix size
    df_block_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            block_data = df[df["Method"] == "OnMultBlock"]
            df_block_raw = pd.concat([df_block_raw, block_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    for bs in [128, 256, 512, 1024]:
        df_bs = df_block_raw[df_block_raw["BlockSize"] == bs]
        if len(df_bs) > 0:
            # Group by MatrixSize to get average L2 cache misses for each block size
            df_bs_avg = df_bs.groupby("MatrixSize", as_index=False)["PAPI_L2_DCM"].mean()
            ax.plot(df_bs_avg["MatrixSize"], df_bs_avg["PAPI_L2_DCM"], 
                    marker='s', linestyle='--', label=f"Block Size {bs}")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L2 Cache Misses")
    ax.set_title("OnMultLine vs OnMultBlock: L2 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart7(ax):
    """Outer, Inner, Single (Time vs MatrixSize)"""
    ax.cla()
    small_sizes = [600, 1000, 1400, 1800, 2200, 2600, 3000]
    df_small_line = df_onmultline[df_onmultline["MatrixSize"].isin(small_sizes)]
    
    # Single thread (OnMultLine)
    if len(df_small_line) > 0:
        ax.plot(df_small_line["MatrixSize"], df_small_line["Time(s)"], marker='o', label="Single Thread")
    
    # Get data for parallel implementations
    df_par_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            par_data = df[(df["Method"] == "OnMultLineParallelOuterFor") | 
                          (df["Method"] == "OnMultLineParallelInnerFor")]
            df_par_raw = pd.concat([df_par_raw, par_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    # Group by Method and MatrixSize to get average time
    df_outer_raw = df_par_raw[df_par_raw["Method"] == "OnMultLineParallelOuterFor"]
    df_inner_raw = df_par_raw[df_par_raw["Method"] == "OnMultLineParallelInnerFor"]
    
    if len(df_outer_raw) > 0:
        df_outer_avg = df_outer_raw.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
        ax.plot(df_outer_avg["MatrixSize"], df_outer_avg["Time(s)"], 
                marker='^', label="Outer Loop Parallel")
    
    if len(df_inner_raw) > 0:
        df_inner_avg = df_inner_raw.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
        ax.plot(df_inner_avg["MatrixSize"], df_inner_avg["Time(s)"], 
                marker='s', label="Inner Loop Parallel")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("Single vs Outer vs Inner: Execution Time")
    ax.legend()
    ax.grid(True)

def chart8(ax):
    """Outer, Inner, Single (MFLOPS vs MatrixSize)"""
    ax.cla()
    small_sizes = [600, 1000, 1400, 1800, 2200, 2600, 3000]
    df_small_line = df_onmultline[df_onmultline["MatrixSize"].isin(small_sizes)].copy()
    
    # Convert GFLOPS to MFLOPS for single thread
    if len(df_small_line) > 0:
        df_small_line.loc[:, "MFLOPS"] = df_small_line["GFLOPS"] * 1000
        ax.plot(df_small_line["MatrixSize"], df_small_line["MFLOPS"], 
                marker='o', label="Single Thread")
    
    # Get data for parallel implementations
    df_par_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            par_data = df[(df["Method"] == "OnMultLineParallelOuterFor") | 
                          (df["Method"] == "OnMultLineParallelInnerFor")]
            df_par_raw = pd.concat([df_par_raw, par_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    # Group by Method and MatrixSize to get average GFLOPS
    df_outer_raw = df_par_raw[df_par_raw["Method"] == "OnMultLineParallelOuterFor"]
    df_inner_raw = df_par_raw[df_par_raw["Method"] == "OnMultLineParallelInnerFor"]
    
    if len(df_outer_raw) > 0:
        df_outer_avg = df_outer_raw.groupby("MatrixSize", as_index=False)["GFLOPS"].mean()
        df_outer_avg.loc[:, "MFLOPS"] = df_outer_avg["GFLOPS"] * 1000
        ax.plot(df_outer_avg["MatrixSize"], df_outer_avg["MFLOPS"], 
                marker='^', label="Outer Loop Parallel")
    
    if len(df_inner_raw) > 0:
        df_inner_avg = df_inner_raw.groupby("MatrixSize", as_index=False)["GFLOPS"].mean()
        df_inner_avg.loc[:, "MFLOPS"] = df_inner_avg["GFLOPS"] * 1000
        ax.plot(df_inner_avg["MatrixSize"], df_inner_avg["MFLOPS"], 
                marker='s', label="Inner Loop Parallel")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("MFLOPS")
    ax.set_title("Single vs Outer vs Inner: MFLOPS")
    ax.legend()
    ax.grid(True)

def chart9(ax):
    """Outer, Inner Speedup by Thread Count vs MatrixSize"""
    ax.cla()
    small_sizes = [600, 1000, 1400, 1800, 2200, 2600, 3000]
    df_small_line = df_onmultline[df_onmultline["MatrixSize"].isin(small_sizes)]
    
    # Get data for parallel implementations with thread information preserved
    df_par_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            par_data = df[(df["Method"] == "OnMultLineParallelOuterFor") | 
                          (df["Method"] == "OnMultLineParallelInnerFor")]
            df_par_raw = pd.concat([df_par_raw, par_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    thread_counts = [2, 4, 8]
    
    # Process outer loop parallel implementation
    for threads in thread_counts:
        # Filter by thread count for outer loop
        df_outer_thread = df_par_raw[(df_par_raw["Method"] == "OnMultLineParallelOuterFor") & 
                                    (df_par_raw["NumThreads"] == threads)]
        
        if len(df_outer_thread) > 0 and len(df_small_line) > 0:
            # Group by MatrixSize to get average time for this thread count
            df_outer_thread_avg = df_outer_thread.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
            
            # Merge with single thread data to calculate speedup
            merged_outer = pd.merge(df_small_line[["MatrixSize", "Time(s)"]], 
                                df_outer_thread_avg, on="MatrixSize", suffixes=('_single', '_outer'))
            merged_outer["Speedup"] = merged_outer["Time(s)_single"] / merged_outer["Time(s)_outer"]
            
            # Plot speedup for this thread count
            ax.plot(merged_outer["MatrixSize"], merged_outer["Speedup"], 
                    marker='^', linestyle='-', label=f"Outer Loop ({threads} threads)")
    
    # Process inner loop parallel implementation
    for threads in thread_counts:
        # Filter by thread count for inner loop
        df_inner_thread = df_par_raw[(df_par_raw["Method"] == "OnMultLineParallelInnerFor") & 
                                    (df_par_raw["NumThreads"] == threads)]
        
        if len(df_inner_thread) > 0 and len(df_small_line) > 0:
            # Group by MatrixSize to get average time for this thread count
            df_inner_thread_avg = df_inner_thread.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
            
            # Merge with single thread data to calculate speedup
            merged_inner = pd.merge(df_small_line[["MatrixSize", "Time(s)"]], 
                                df_inner_thread_avg, on="MatrixSize", suffixes=('_single', '_inner'))
            merged_inner["Speedup"] = merged_inner["Time(s)_single"] / merged_inner["Time(s)_inner"]
            
            # Plot speedup for this thread count
            ax.plot(merged_inner["MatrixSize"], merged_inner["Speedup"], 
                    marker='s', linestyle='--', label=f"Inner Loop ({threads} threads)")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Speedup")
    ax.set_title("Speedup by Thread Count")
    ax.legend()
    ax.grid(True)

def chart10(ax):
    """Outer (Efficiency vs MatrixSize) for different thread counts"""
    ax.cla()
    small_sizes = [600, 1000, 1400, 1800, 2200, 2600, 3000]
    df_small_line = df_onmultline[df_onmultline["MatrixSize"].isin(small_sizes)]
    
    # Get data for parallel implementations
    df_par_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            par_data = df[df["Method"] == "OnMultLineParallelOuterFor"]
            df_par_raw = pd.concat([df_par_raw, par_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    # Calculate efficiency for each thread count
    thread_counts = [2, 4, 8]
    for threads in thread_counts:
        df_thread = df_par_raw[df_par_raw["NumThreads"] == threads]
        
        if len(df_thread) > 0 and len(df_small_line) > 0:
            # Group by MatrixSize to get average time
            df_thread_avg = df_thread.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
            
            # Merge with single thread data to calculate speedup
            merged = pd.merge(df_small_line[["MatrixSize", "Time(s)"]], 
                          df_thread_avg, on="MatrixSize", suffixes=('_single', f'_outer_{threads}'))
            merged["Speedup"] = merged["Time(s)_single"] / merged[f"Time(s)_outer_{threads}"]
            merged["Efficiency"] = merged["Speedup"] / threads
            
            ax.plot(merged["MatrixSize"], merged["Efficiency"], 
                    marker='^', label=f"Outer Loop ({threads} threads)")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Efficiency (Speedup/Threads)")
    ax.set_title("Outer Loop Parallelization: Efficiency")
    ax.legend()
    ax.grid(True)

def chart11(ax):
    """Single vs Outer (different thread counts) (Time vs MatrixSize)"""
    ax.cla()
    small_sizes = [600, 1000, 1400, 1800, 2200, 2600, 3000]
    df_small_line = df_onmultline[df_onmultline["MatrixSize"].isin(small_sizes)]
    
    # Single thread execution time
    if len(df_small_line) > 0:
        ax.plot(df_small_line["MatrixSize"], df_small_line["Time(s)"], 
                marker='o', label="Single Thread")
    
    # Get data for parallel implementations
    df_par_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            par_data = df[df["Method"] == "OnMultLineParallelOuterFor"]
            df_par_raw = pd.concat([df_par_raw, par_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    thread_counts = [2, 4, 8]
    for threads in thread_counts:
        df_thread = df_par_raw[df_par_raw["NumThreads"] == threads]
        
        if len(df_thread) > 0:
            # Group by MatrixSize to get average time
            df_thread_avg = df_thread.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
            
            ax.plot(df_thread_avg["MatrixSize"], df_thread_avg["Time(s)"], 
                    marker='^', label=f"Outer Loop ({threads} threads)")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("Single vs Outer Loop Multi-Thread: Execution Time")
    ax.legend()
    ax.grid(True)

def chart12(ax):
    """Single vs Inner (different thread counts) (Time vs MatrixSize)"""
    ax.cla()
    small_sizes = [600, 1000, 1400, 1800, 2200, 2600, 3000]
    df_small_line = df_onmultline[df_onmultline["MatrixSize"].isin(small_sizes)]
    
    # Single thread execution time
    if len(df_small_line) > 0:
        ax.plot(df_small_line["MatrixSize"], df_small_line["Time(s)"], 
                marker='o', label="Single Thread")
    
    # Get data for parallel implementations
    df_par_raw = pd.DataFrame()
    for file in benchmark_files:
        try:
            df = pd.read_csv(file)
            par_data = df[df["Method"] == "OnMultLineParallelInnerFor"]
            df_par_raw = pd.concat([df_par_raw, par_data], ignore_index=True)
        except FileNotFoundError:
            continue
    
    thread_counts = [2, 4, 8]
    for threads in thread_counts:
        df_thread = df_par_raw[df_par_raw["NumThreads"] == threads]
        
        if len(df_thread) > 0:
            # Group by MatrixSize to get average time
            df_thread_avg = df_thread.groupby("MatrixSize", as_index=False)["Time(s)"].mean()
            
            ax.plot(df_thread_avg["MatrixSize"], df_thread_avg["Time(s)"], 
                    marker='s', label=f"Inner Loop ({threads} threads)")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("Single vs Inner Loop Multi-Thread: Execution Time")
    ax.legend()
    ax.grid(True)

# Create a list of page functions.
pages = [chart1, chart2, chart3, chart4, chart5, chart6,
         chart7, chart8, chart9, chart10, chart11, chart12]
page_index = 0

# Create the main figure and register key events.
fig, ax = plt.subplots(figsize=(10, 6))

def update_chart():
    fig.suptitle(f"Page {page_index+1} of {len(pages)}")
    pages[page_index](ax)
    fig.canvas.draw_idle()

def on_key(event):
    global page_index
    if event.key == "right":
        page_index = (page_index + 1) % len(pages)
        update_chart()
    elif event.key == "left":
        page_index = (page_index - 1) % len(pages)
        update_chart()

fig.canvas.mpl_connect("key_press_event", on_key)
update_chart()
plt.show()