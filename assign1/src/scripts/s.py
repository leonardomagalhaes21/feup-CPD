import pandas as pd
import matplotlib.pyplot as plt
import os

# ====== Load DataFrames from CSV files ======
csv_go = os.path.join("..", "data", "benchmark_results_go.csv")
csv_line = os.path.join("..", "data", "benchmark_results.csv")

df_go = pd.read_csv(csv_go)
df_line = pd.read_csv(csv_line)

# ====== Create subsets for each method ======
df_onmult      = df_line[df_line["Method"] == "OnMult"].copy()
df_onmultline  = df_line[df_line["Method"] == "OnMultLine"].copy()
df_onmultblock = df_line[df_line["Method"] == "OnMultBlock"].copy()
df_par_outer   = df_line[df_line["Method"] == "OnMultLineParallelOuterFor"].copy()
df_par_inner   = df_line[df_line["Method"] == "OnMultLineParallelInnerFor"].copy()

# Helper: average group aggregating
def group_average(df, col, extra_cols=None):
    cols = extra_cols if extra_cols else []
    return df.groupby("MatrixSize", as_index=False)[col].mean()

# Get data for Go implementations
df_go_onmult = df_go[df_go["Method"] == "OnMult"].copy()
df_go_onmultline = df_go[df_go["Method"] == "OnMultLine"].copy()

# Get data for thread-specific runs
def get_thread_data(df, thread_count):
    return df[df["NumThreads"] == thread_count].copy()

# ----- Define functions for each chart page -----

def chart1(ax):
    """C++ OnMult & OnMultLine (Time vs MatrixSize) vs GO OnMult & OnMultLine (Time vs MatrixSize)"""
    ax.cla()
    # C++ implementations
    ax.plot(df_onmult["MatrixSize"], df_onmult["Time(s)"], marker='o', label="C++ OnMult")
    ax.plot(df_onmultline["MatrixSize"], df_onmultline["Time(s)"], marker='s', label="C++ OnMultLine")
    # GO implementations
    ax.plot(df_go_onmult["MatrixSize"], df_go_onmult["Time(s)"], marker='^', label="GO OnMult")
    ax.plot(df_go_onmultline["MatrixSize"], df_go_onmultline["Time(s)"], marker='x', label="GO OnMultLine")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("C++ vs GO Implementations: Execution Time")
    ax.legend()
    ax.grid(True)

def chart2(ax):
    """OnMult vs OnMultLine L1 Cache Misses"""
    ax.cla()
    # Get average L1 cache misses per matrix size
    df_onmult_l1 = group_average(df_onmult, "PAPI_L1_DCM")
    df_onmultline_l1 = group_average(df_onmultline, "PAPI_L1_DCM")
    
    ax.plot(df_onmult_l1["MatrixSize"], df_onmult_l1["PAPI_L1_DCM"], marker='o', label="OnMult L1 Cache Misses")
    ax.plot(df_onmultline_l1["MatrixSize"], df_onmultline_l1["PAPI_L1_DCM"], marker='s', label="OnMultLine L1 Cache Misses")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L1 Cache Misses")
    ax.set_title("OnMult vs OnMultLine: L1 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart3(ax):
    """OnMult vs OnMultLine L2 Cache Misses"""
    ax.cla()
    # Get average L2 cache misses per matrix size
    df_onmult_l2 = group_average(df_onmult, "PAPI_L2_DCM")
    df_onmultline_l2 = group_average(df_onmultline, "PAPI_L2_DCM")
    
    ax.plot(df_onmult_l2["MatrixSize"], df_onmult_l2["PAPI_L2_DCM"], marker='o', label="OnMult L2 Cache Misses")
    ax.plot(df_onmultline_l2["MatrixSize"], df_onmultline_l2["PAPI_L2_DCM"], marker='s', label="OnMultLine L2 Cache Misses")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L2 Cache Misses")
    ax.set_title("OnMult vs OnMultLine: L2 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart4(ax):
    """Compare Block sizes and Line (Time vs MatrixSize)"""
    ax.cla()
    # Line execution time
    df_line_avg = group_average(df_onmultline, "Time(s)")
    ax.plot(df_line_avg["MatrixSize"], df_line_avg["Time(s)"], marker='o', linestyle='-', label="OnMultLine")
    
    # Block execution times
    for bs in [128, 256, 512, 1024]:
        df_blk = df_onmultblock[df_onmultblock["BlockSize"] == bs]
        df_blk_avg = group_average(df_blk, "Time(s)")
        ax.plot(df_blk_avg["MatrixSize"], df_blk_avg["Time(s)"], marker='s', linestyle='--', label=f"Block Size {bs}")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("OnMultLine vs OnMultBlock: Execution Time")
    ax.legend()
    ax.grid(True)

def chart5(ax):
    """Compare Block sizes and Line (L1 Cache Misses vs MatrixSize)"""
    ax.cla()
    # Line L1 cache misses
    df_line_l1 = group_average(df_onmultline, "PAPI_L1_DCM")
    ax.plot(df_line_l1["MatrixSize"], df_line_l1["PAPI_L1_DCM"], marker='o', linestyle='-', label="OnMultLine")
    
    # Block L1 cache misses
    for bs in [128, 256, 512, 1024]:
        df_blk = df_onmultblock[df_onmultblock["BlockSize"] == bs]
        df_blk_l1 = group_average(df_blk, "PAPI_L1_DCM")
        ax.plot(df_blk_l1["MatrixSize"], df_blk_l1["PAPI_L1_DCM"], marker='s', linestyle='--', label=f"Block Size {bs}")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L1 Cache Misses")
    ax.set_title("OnMultLine vs OnMultBlock: L1 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart6(ax):
    """Compare Block sizes and Line (L2 Cache Misses vs MatrixSize)"""
    ax.cla()
    # Line L2 cache misses
    df_line_l2 = group_average(df_onmultline, "PAPI_L2_DCM")
    ax.plot(df_line_l2["MatrixSize"], df_line_l2["PAPI_L2_DCM"], marker='o', linestyle='-', label="OnMultLine")
    
    # Block L2 cache misses
    for bs in [128, 256, 512, 1024]:
        df_blk = df_onmultblock[df_onmultblock["BlockSize"] == bs]
        df_blk_l2 = group_average(df_blk, "PAPI_L2_DCM")
        ax.plot(df_blk_l2["MatrixSize"], df_blk_l2["PAPI_L2_DCM"], marker='s', linestyle='--', label=f"Block Size {bs}")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("L2 Cache Misses")
    ax.set_title("OnMultLine vs OnMultBlock: L2 Cache Misses")
    ax.legend()
    ax.grid(True)

def chart7(ax):
    """Outer, Inner, Single (Time vs MatrixSize)"""
    ax.cla()
    # Single thread (OnMultLine)
    df_single_time = group_average(df_onmultline, "Time(s)")
    ax.plot(df_single_time["MatrixSize"], df_single_time["Time(s)"], marker='o', label="Single Thread")
    
    # Get average time for outer and inner implementations
    df_outer_time = group_average(df_par_outer, "Time(s)")
    df_inner_time = group_average(df_par_inner, "Time(s)")
    
    ax.plot(df_outer_time["MatrixSize"], df_outer_time["Time(s)"], marker='^', label="Outer Loop Parallel")
    ax.plot(df_inner_time["MatrixSize"], df_inner_time["Time(s)"], marker='s', label="Inner Loop Parallel")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("Single vs Outer vs Inner: Execution Time")
    ax.legend()
    ax.grid(True)

def chart8(ax):
    """Outer, Inner, Single (MFLOPS vs MatrixSize)"""
    ax.cla()
    # Convert GFLOPS to MFLOPS for consistency
    df_single_gflops = group_average(df_onmultline, "GFLOPS")
    df_single_gflops["MFLOPS"] = df_single_gflops["GFLOPS"] * 1000
    
    df_outer_gflops = group_average(df_par_outer, "GFLOPS")
    df_outer_gflops["MFLOPS"] = df_outer_gflops["GFLOPS"] * 1000
    
    df_inner_gflops = group_average(df_par_inner, "GFLOPS")
    df_inner_gflops["MFLOPS"] = df_inner_gflops["GFLOPS"] * 1000
    
    ax.plot(df_single_gflops["MatrixSize"], df_single_gflops["MFLOPS"], marker='o', label="Single Thread")
    ax.plot(df_outer_gflops["MatrixSize"], df_outer_gflops["MFLOPS"], marker='^', label="Outer Loop Parallel")
    ax.plot(df_inner_gflops["MatrixSize"], df_inner_gflops["MFLOPS"], marker='s', label="Inner Loop Parallel")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("MFLOPS")
    ax.set_title("Single vs Outer vs Inner: MFLOPS")
    ax.legend()
    ax.grid(True)

def chart9(ax):
    """Outer, Inner (Speedup vs MatrixSize)"""
    ax.cla()
    # Calculate speedup compared to single thread
    df_single_time = group_average(df_onmultline, "Time(s)")
    df_outer_time = group_average(df_par_outer, "Time(s)")
    df_inner_time = group_average(df_par_inner, "Time(s)")
    
    # Merge datasets to calculate speedup
    merged_outer = pd.merge(df_single_time, df_outer_time, on="MatrixSize", suffixes=('_single', '_outer'))
    merged_outer["Speedup"] = merged_outer["Time(s)_single"] / merged_outer["Time(s)_outer"]
    
    merged_inner = pd.merge(df_single_time, df_inner_time, on="MatrixSize", suffixes=('_single', '_inner'))
    merged_inner["Speedup"] = merged_inner["Time(s)_single"] / merged_inner["Time(s)_inner"]
    
    ax.plot(merged_outer["MatrixSize"], merged_outer["Speedup"], marker='^', label="Outer Loop Speedup")
    ax.plot(merged_inner["MatrixSize"], merged_inner["Speedup"], marker='s', label="Inner Loop Speedup")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Speedup")
    ax.set_title("Outer vs Inner: Speedup")
    ax.legend()
    ax.grid(True)

def chart10(ax):
    """Outer, Inner (Efficiency vs MatrixSize)"""
    ax.cla()
    # Calculate efficiency (Speedup/Number of threads)
    df_single_time = group_average(df_onmultline, "Time(s)")
    
    thread_counts = [2, 4, 8, 12]
    for threads in thread_counts:
        df_outer_thread = df_par_outer[df_par_outer["NumThreads"] == threads]
        df_outer_thread_time = group_average(df_outer_thread, "Time(s)")
        
        merged = pd.merge(df_single_time, df_outer_thread_time, on="MatrixSize", suffixes=('_single', f'_outer_{threads}'))
        merged["Speedup"] = merged["Time(s)_single"] / merged[f"Time(s)_outer_{threads}"]
        merged["Efficiency"] = merged["Speedup"] / threads
        
        ax.plot(merged["MatrixSize"], merged["Efficiency"], marker='^', label=f"Outer Loop ({threads} threads)")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Efficiency (Speedup/Threads)")
    ax.set_title("Outer Loop Parallelization: Efficiency")
    ax.legend()
    ax.grid(True)

def chart11(ax):
    """Single, Outer (4, 8, 10, 12 threads) (Time vs MatrixSize)"""
    ax.cla()
    # Single thread execution time
    df_single_time = group_average(df_onmultline, "Time(s)")
    ax.plot(df_single_time["MatrixSize"], df_single_time["Time(s)"], marker='o', label="Single Thread")
    
    # Thread-specific execution times for outer loop
    thread_counts = [4, 8, 12]  # Note: Using 12 instead of 10 as per available data
    for threads in thread_counts:
        df_outer_thread = df_par_outer[df_par_outer["NumThreads"] == threads]
        df_outer_thread_time = group_average(df_outer_thread, "Time(s)")
        ax.plot(df_outer_thread_time["MatrixSize"], df_outer_thread_time["Time(s)"], 
                marker='^', label=f"Outer Loop ({threads} threads)")
    
    ax.set_xlabel("Matrix Size")
    ax.set_ylabel("Execution Time (s)")
    ax.set_title("Single vs Outer Loop Multi-Thread: Execution Time")
    ax.legend()
    ax.grid(True)

def chart12(ax):
    """Single, Inner (4, 8, 10, 12 threads) (Time vs MatrixSize)"""
    ax.cla()
    # Single thread execution time
    df_single_time = group_average(df_onmultline, "Time(s)")
    ax.plot(df_single_time["MatrixSize"], df_single_time["Time(s)"], marker='o', label="Single Thread")
    
    # Thread-specific execution times for inner loop
    thread_counts = [4, 8, 12]  # Note: Using 12 instead of 10 as per available data
    for threads in thread_counts:
        df_inner_thread = df_par_inner[df_par_inner["NumThreads"] == threads]
        df_inner_thread_time = group_average(df_inner_thread, "Time(s)")
        ax.plot(df_inner_thread_time["MatrixSize"], df_inner_thread_time["Time(s)"], 
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