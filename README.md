# Assignment 3 — Smart City Infrastructure and Route Planning

**Author:** Baktybay Nazar (group SE-2530) · **GitHub:** Nazar887667
**Language:** Java 21 (no external libraries) · **Plots:** Python 3 + matplotlib (only for drawing the figures)

A Java application that models a city as a weighted graph and solves three planning problems: the cheapest road network that connects every district (**MST**), connectivity analysis of one-way streets and service dependencies (**topological order, SCC**), and shortest routes from a chosen district (**Dijkstra**). The README is the full report: design, complexity analysis, experiments, results, tests and conclusions.

---

## Table of contents
1. [Overview](#1-overview)
2. [Implementation](#2-implementation)
3. [Complexity analysis](#3-complexity-analysis)
4. [Experimental setup](#4-experimental-setup)
5. [Results](#5-results)
6. [Comparison and discussion](#6-comparison-and-discussion)
7. [Testing](#7-testing)
8. [Conclusion](#8-conclusion)
9. [How to build, test and reproduce](#9-how-to-build-test-and-reproduce)

---

## 1. Overview

**City model.** Vertices are districts/locations; edges are roads or infrastructure links; weights are construction costs (MST task) or travel distances (shortest-path task).

| Task | Graph type | Algorithms |
|---|---|---|
| 1. Infrastructure planning | undirected, weighted | Kruskal + DSU, Prim + binary heap |
| 2. Connectivity | directed (one-way roads, service dependencies) | Kahn topological sort, Tarjan SCC |
| 3. Route planning | directed or undirected, non-negative weights | Dijkstra + binary heap |

**Graph representation.** One class, `graph.Graph`, stores an **adjacency list** (`List<Edge>[]`) plus a flat edge list. Vertices are `0..V-1`; `Edge` is an immutable record `(from, to, weight)`. For undirected graphs each logical edge appears in both adjacency lists but only once in the edge list (Kruskal sorts that list; Prim/Dijkstra walk the adjacency lists). Space is `O(V + E)`. An adjacency list is the right choice because all test graphs are sparse (`E ≈ 3V–5V`), and every algorithm here scans a vertex's neighbours rather than querying "is there an edge u-v?".

**Repository layout**

```
assignment-3/
├── src/
│   ├── graph/          Graph, Edge, BinaryMinHeap, GraphGenerator (seeded)
│   ├── dsu/            DSU (path compression + union by rank, each switchable)
│   ├── mst/            Kruskal, Prim, MSTResult
│   ├── connectivity/   TopologicalSort (Kahn), SCC (Tarjan), result classes
│   ├── shortestpath/   Dijkstra, DijkstraResult
│   ├── app/            Demo (smart-city example), Experiments (benchmark driver)
│   └── tests/          TestRunner (dependency-free test suite, 291 checks)
├── results/
│   ├── tables/         CSV + Markdown tables written by Experiments
│   ├── plots/          PNG figures written by scripts/plot.py
│   └── test_output.txt full test log
├── scripts/            build.sh, run_tests.sh, run_experiments.sh, plot.py
└── README.md
```

A runnable example of all three tasks on an 8-district city is in `app.Demo` (`java -cp out app.Demo`).

---

## 2. Implementation

### Main design decisions

* **Instrumented algorithms.** Every algorithm returns a result object with an `ops` map of operation counters (edge examinations, sort/heap comparisons, DSU `find`/`union` calls, parent-pointer hops, heap pushes/pops, …). The counters are exact and deterministic, so they can be compared with the theory independent of timing noise.
* **DSU (`dsu.DSU`).** Array-based forest with an **iterative** `find` (no recursion, so long chains cannot overflow the stack). *Path compression* re-attaches every node on the search path directly to the root (two-pass). *Union by rank* hangs the shallower tree under the deeper one. Both can be switched off independently, which is what the ablation experiment in §5.2 uses. `union` returns `false` when both elements are already in the same set — Kruskal uses this return value as its cycle test.
* **Kruskal.** Copies and sorts the edge list by weight (comparator counts comparisons), then scans it with the DSU. It **stops as soon as `V-1` edges are chosen**, so heavy edges at the end of the list are never examined. On a disconnected graph it naturally returns a minimum spanning *forest*; `connected` is derived from the DSU's component count.
* **Prim.** Lazy-deletion variant over my own array-based `BinaryMinHeap` (so heap comparisons can be counted): scanning a newly visited vertex pushes every edge to an unvisited neighbour; popped entries whose endpoint is already in the tree are skipped (*stale* entries). It restarts from every unvisited vertex, so it also returns a spanning forest on disconnected graphs and reports the number of components. This makes "Kruskal weight == Prim weight" testable on disconnected graphs too.
* **Topological order — Kahn's algorithm.** Computes in-degrees, repeatedly removes vertices of in-degree 0. If fewer than `V` vertices can be removed, a cycle exists: the result has `hasCycle = true`, `order = null`, and reports how many vertices could be ordered. `isValidOrder(g, order)` independently verifies that an order is a permutation in which every edge goes forward — used by the tests and by the experiment tables.
* **SCC — Tarjan's algorithm, iterative.** One DFS pass with an explicit call stack and an SCC stack. Iterative because a recursive version overflows the Java stack on deep graphs (the test suite runs a 200,000-vertex path). Every vertex receives exactly one component id.
* **Dijkstra.** Binary heap with lazy deletion (duplicate entries allowed, stale pops skipped). Stores distances (`long`, `INF = Long.MAX_VALUE` marks unreachable vertices) and a parent array for path reconstruction (`pathTo(v)`). The constructor-level guard **rejects negative weights** with an `IllegalArgumentException`; an unguarded mode exists only to demonstrate the failure (§3.7 and tests).
* **Reproducible graph generation (`graph.GraphGenerator`).** Fixed-seed `java.util.Random`; simple graphs (no self-loops, no parallel edges); integer weights uniform in `[1, 100]` so duplicate weights occur naturally. Details in §4.
* **Dependency-free tests.** `tests.TestRunner` needs only the JDK, so `./scripts/run_tests.sh` works anywhere. It contains brute-force oracles (details in §7).

---

## 3. Complexity analysis

**Assumptions.** `V` vertices, `E` edges. Graph stored as adjacency lists. Priority queue is a **binary heap** (`push`/`pop` cost `O(log n)`). Sorting is comparison-based `O(E log E)`. For a connected graph `E ≥ V−1` and `E ≤ V²`, so `log E = Θ(log V)`. `α` is the inverse Ackermann function (`α(n) ≤ 4` for any n that fits in the universe).

### 3.1 Disjoint Set Union

| Variant | `find` / `union` (amortised) | Space |
|---|---|---|
| naive linking, no compression | `O(n)` worst case (chain) | `O(n)` |
| union by rank only | `O(log n)` (tree height ≤ log₂ n) | `O(n)` |
| path compression only | `O(log n)` amortised | `O(n)` |
| **both (implemented default)** | **`O(α(n))` amortised** | `O(n)` |

*Why the optimisations help.* Without them a sequence of unions can build a path of length `n`, and each `find` walks the whole path. **Union by rank** guarantees that a tree of height `h` has at least `2^h` nodes, so height never exceeds `log₂ n`. **Path compression** makes every later `find` on the same nodes almost free, because after one traversal all visited nodes point straight at the root. Together they give the near-constant `α(n)` bound (Tarjan 1975). `V`/`E` effect: Kruskal performs `2E` finds and at most `V−1` successful unions, so the DSU contributes `O(E·α(V))` — essentially linear and negligible next to the sort. Measured effect: §5.2.

### 3.2 Kruskal

Sorting: `O(E log E) = O(E log V)`. DSU phase: `O(E·α(V))`. **Total `O(E log V)`**, dominated by the sort. Space `O(V + E)` (sorted edge copy, DSU arrays, tree). Performance depends mostly on `E`; `V` only matters through `log V` and the DSU size. The early exit after `V−1` tree edges means the DSU phase often touches far fewer than `E` edges on dense graphs (§5.3), but the *sort still processes all `E`*.

### 3.3 Prim (binary heap, lazy deletion)

Each undirected edge is pushed at most once (when its first-visited endpoint is scanned) so the heap holds at most `E` entries; there are `≤ E` pushes and `≤ E` pops, each `O(log E)`; adjacency scanning costs `O(V + E)`. **Total `O(E log V)`**, space `O(V + E)`. With an indexed heap and decrease-key it is `O(E log V)` with a smaller heap (`≤ V`); with a Fibonacci heap `O(E + V log V)`; with a plain array and no heap `O(V²)`, which wins on very dense graphs. Performance depends on `E` (scans + pushes) and on `V` through `log`.

### 3.4 Topological ordering (Kahn)

Computing in-degrees scans every edge once (`O(V + E)`); each vertex enters the queue once and each edge is relaxed once. **Time `O(V + E)`, extra space `O(V)`.** Cycle detection is free: if the queue empties before `V` vertices were removed, a cycle exists. On cyclic input the algorithm can stop earlier than on a DAG (the queue empties), but the initial in-degree pass over all edges is always paid.

### 3.5 SCC (Tarjan)

One DFS visits each vertex once and examines each edge once: **time `O(V + E)`, space `O(V)`** for `index`, `low`, the two stacks and component ids (plus `O(V)` for the output). Runtime grows linearly with `V + E` in theory; the practical behaviour (§6.4) shows additional memory effects.

### 3.6 Dijkstra (binary heap, lazy deletion)

At most `E` successful relaxations ⇒ at most `E + 1` heap pushes, each `O(log E)`; each vertex is finalised once; each edge is examined once. **Total `O((V + E) log V)`**, which is `O(E log V)` for connected graphs; space `O(V + E)`. Variants: Fibonacci heap `O(E + V log V)`; array scan `O(V²)`. In practice the number of successful relaxations is much smaller than `E` (§6.5), so the `E log V` bound is pessimistic: the `E` term mostly comes from scanning adjacency lists.

### 3.7 Why Dijkstra fails with negative edge weights

Dijkstra *finalises* a vertex the moment it is removed from the heap, assuming no later path can be cheaper. This is valid only if extending a path never makes it shorter, i.e. all weights are `≥ 0`. A negative edge breaks the invariant.

Counter-example (also an automated test): edges `s→A (2)`, `s→B (5)`, `B→A (−4)`. Dijkstra pops `A` first (tentative distance 2) and finalises it. Then it pops `B` (5) and sees `B→A` would give `5 − 4 = 1 < 2`, but `A` is already finalised, so the improvement is ignored. Reported `dist(A) = 2`, true shortest `dist(A) = 1` (path `s→B→A`). With a negative **cycle** shortest paths are not even defined. The implementation therefore rejects graphs with negative weights; for such graphs use Bellman–Ford (`O(VE)`) or Johnson's algorithm.

### 3.8 Summary

| Algorithm | Time | Extra space | Depends on |
|---|---|---|---|
| DSU find/union (both optimisations) | `O(α(n))` amortised | `O(n)` | n only |
| Kruskal | `O(E log V)` | `O(V + E)` | E (sort) |
| Prim (binary heap) | `O(E log V)` | `O(V + E)` | E, log V |
| Topological sort (Kahn) | `O(V + E)` | `O(V)` | V + E |
| SCC (Tarjan) | `O(V + E)` | `O(V)` | V + E |
| Dijkstra (binary heap) | `O((V + E) log V)` | `O(V + E)` | E, log V |

---

## 4. Experimental setup

**Graph sizes (required).**

| Graph | V | E |
|---|---|---|
| Small | 10 | 30 |
| Medium | 100 | 500 |
| Large | 1,000 | 5,000 |

In addition, to obtain enough points for meaningful curves and plots, I ran two scaling series that contain the three required sizes:
* **Series V** (V varies, `E = 5V`): `V ∈ {10*, 100, 500, 1000, 2000, 5000, 10000, 20000}` (*`V=10` uses `E=30` as required).
* **Series E** (V fixed at 1000, E varies): `E ∈ {1000, 2000, 5000, 10000, 20000, 50000, 100000}`.

**Graph generation (all reproducible).** `java.util.Random` with seed `42·1,000,003 + 10,007·V + 31·E + kind` (`kind` = 1 undirected, 2 directed, 3 DAG), so the same `(V, E)` always yields the same graph. Weights are integers uniform in `[1, 100]`. All graphs are simple (no self-loops, no parallel edges).
* **Undirected graphs for MST and Dijkstra:** a random spanning tree is generated first (each vertex is attached to a random earlier vertex of a random permutation), which **guarantees connectivity**; then uniformly random extra edges are added until exactly `E` edges exist. The experiment output records that every generated graph is connected (`graph_properties` table).
* **Directed graphs for SCC, Dijkstra (directed) and topological sort on cyclic input:** `E` distinct ordered pairs chosen uniformly at random. These graphs **contain cycles**: the `graph_properties` table confirms it (Kahn's algorithm reports a cycle for every size). The expected strongly-connected structure is documented in the results (one giant SCC plus a few small ones for `E ≥ 2V`).
* **DAGs for topological sort:** a random vertex permutation is fixed and only edges from an earlier to a later vertex are created, so **no cycle can exist** (the experiment re-verifies the produced order with `isValidOrder`).

**Measurement procedure (`app.Experiments`).**
1. Graphs are generated once, outside the timed regions.
2. A global JVM warm-up (4 s, all algorithms) runs first so JIT compilation is in a steady state.
3. For every algorithm and graph: warm-up runs (≥ 3 runs and ≥ 400 ms), one `System.gc()`, then **5 repetitions**. Each repetition measures with `System.nanoTime()` a *batch* of `k` back-to-back executions (`k` chosen so a batch takes ≈ 20 ms) and divides by `k`; this removes timer granularity for algorithms that run in a few microseconds. The tables report the **mean of the 5 repetitions** and their standard deviation, in microseconds per execution.
4. **Operation counts** are exact and deterministic; they are taken from a single instrumented run.
5. The program aborts if Kruskal and Prim ever disagree on the MST weight.

**Environment of the reported run** (`results/tables/environment.txt`): OpenJDK 21.0.12, Linux amd64, **1 CPU core**, 966 MB max heap. *Absolute times depend on the machine and on a shared single core (JIT compiler threads compete with the benchmark thread); trends and operation counts are what to compare. Re-running `scripts/run_experiments.sh` regenerates every table and plot on your own machine.*

---

## 5. Results

All times are average **microseconds per execution** over 5 repetitions (± = standard deviation in the `std_us` column). Full CSVs are in `results/tables/`.

### 5.0 Generated graphs

| V | E | undirected_connected | directed_graph_has_cycle | dag_has_cycle | directed_scc_count | directed_reachable_from_0 |
|---|---|---|---|---|---|---|
| 10 | 30 | true | true | false | 1 | 10 |
| 100 | 500 | true | true | false | 3 | 98 |
| 1000 | 5000 | true | true | false | 23 | 995 |

`directed_scc_count` / `directed_reachable_from_0` describe the random directed graph of the same size. Every generated undirected graph is connected, every random directed graph is cyclic, every DAG is acyclic.

### 5.1 Task 1 — Kruskal vs. Prim (required sizes)

| V | E | algorithm | avg_us | std_us | mst_weight | connected | edge_exams | comparisons | dsu_find | dsu_union | dsu_path_steps | heap_push | heap_pop |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 10 | 30 | Kruskal | 9.5 | 2.0 | 151 | true | 14 | 112 | 28 | 14 | 17 | - | - |
| 10 | 30 | Prim | 3.8 | 0.5 | 151 | true | 60 | 195 | - | - | - | 30 | 30 |
| 100 | 500 | Kruskal | 33.7 | 3.4 | 1294 | true | 237 | 3826 | 474 | 237 | 417 | - | - |
| 100 | 500 | Prim | 51.7 | 6.0 | 1294 | true | 1000 | 7715 | - | - | - | 500 | 500 |
| 1000 | 5000 | Kruskal | 929.8 | 18.8 | 12715 | true | 2604 | 50390 | 5208 | 2604 | 4668 | - | - |
| 1000 | 5000 | Prim | 1164.9 | 110.7 | 12715 | true | 10000 | 110696 | - | - | - | 5000 | 5000 |

* `edge_exams`: Kruskal = edges taken from the sorted list before stopping; Prim = adjacency entries scanned (always `2E`).
* `comparisons`: Kruskal = comparator calls while sorting; Prim = comparisons inside heap push/pop.

Both algorithms return the **same MST weight** in every configuration (the program enforces it).

### 5.2 DSU — effect of path compression and union by rank

Kruskal's algorithm run with four DSU variants (same graphs; `dsu_path_steps` = number of parent-pointer hops over the whole run):

| V | E | variant | avg_us | std_us | dsu_find | dsu_union | dsu_path_steps | mst_weight |
|---|---|---|---|---|---|---|---|---|
| 10 | 30 | naive (no optimisation) | 3.5 | 2.8 | 28 | 14 | 26 | 151 |
| 10 | 30 | path compression only | 2.0 | 0.8 | 28 | 14 | 21 | 151 |
| 10 | 30 | union by rank only | 0.9 | 0.1 | 28 | 14 | 19 | 151 |
| 10 | 30 | both | 0.8 | 0.0 | 28 | 14 | 17 | 151 |
| 100 | 500 | naive (no optimisation) | 45.1 | 10.6 | 474 | 237 | 3555 | 1294 |
| 100 | 500 | path compression only | 28.5 | 2.3 | 474 | 237 | 664 | 1294 |
| 100 | 500 | union by rank only | 27.4 | 1.1 | 474 | 237 | 611 | 1294 |
| 100 | 500 | both | 26.9 | 1.1 | 474 | 237 | 417 | 1294 |
| 1000 | 5000 | naive (no optimisation) | 1832.6 | 346.5 | 5208 | 2604 | 401772 | 12715 |
| 1000 | 5000 | path compression only | 1143.9 | 53.6 | 5208 | 2604 | 10329 | 12715 |
| 1000 | 5000 | union by rank only | 749.1 | 17.0 | 5208 | 2604 | 7258 | 12715 |
| 1000 | 5000 | both | 996.7 | 17.2 | 5208 | 2604 | 4668 | 12715 |

**Adversarial chain test** — `n` unions `union(i+1, i)` (builds a path under naive linking) followed by one `find` for every element:

| n | variant | avg_us | std_us | dsu_path_steps |
|---|---|---|---|---|
| 1000 | naive (no optimisation) | 1047.0 | 43.9 | 499500 |
| 1000 | path compression only | 15.4 | 2.0 | 1997 |
| 1000 | union by rank only | 9.5 | 0.2 | 1996 |
| 1000 | both | 9.6 | 0.3 | 1996 |
| 2000 | naive (no optimisation) | 3246.0 | 111.2 | 1999000 |
| 2000 | path compression only | 22.7 | 0.4 | 3997 |
| 2000 | union by rank only | 21.1 | 2.5 | 3996 |
| 2000 | both | 21.1 | 3.0 | 3996 |
| 4000 | naive (no optimisation) | 12539.9 | 123.2 | 7998000 |
| 4000 | path compression only | 46.9 | 3.1 | 7997 |
| 4000 | union by rank only | 42.4 | 5.4 | 7996 |
| 4000 | both | 41.0 | 2.7 | 7996 |
| 8000 | naive (no optimisation) | 52326.7 | 1939.7 | 31996000 |
| 8000 | path compression only | 90.7 | 1.5 | 15997 |
| 8000 | union by rank only | 82.3 | 6.8 | 15996 |
| 8000 | both | 81.8 | 3.0 | 15996 |
| 16000 | naive (no optimisation) | 197608.0 | 1325.4 | 127992000 |
| 16000 | path compression only | 189.2 | 3.2 | 31997 |
| 16000 | union by rank only | 151.9 | 2.4 | 31996 |
| 16000 | both | 281.5 | 3.9 | 31996 |

![DSU chain](../../../IdeaProjects/DAA-assignment-3/results/plots/dsu_chain.png)

### 5.3 Task 1 — scaling of Kruskal and Prim

Average time (µs), series V (`E = 5V`):

| V | E | Kruskal (µs) | Prim (µs) | Dijkstra (µs) | SCC (Tarjan) (µs) | Topological sort (DAG) (µs) |
|---|---|---|---|---|---|---|
| 10 | 30 | 9.5 | 3.8 | 0.9 | 0.6 | 0.3 |
| 100 | 500 | 33.7 | 51.7 | 33.0 | 11.9 | 2.5 |
| 500 | 2500 | 445.2 | 512.9 | 185.2 | 45.5 | 17.2 |
| 1000 | 5000 | 929.8 | 1164.9 | 433.9 | 109.3 | 45.8 |
| 2000 | 10000 | 1595.2 | 2875.5 | 1135.8 | 299.5 | 122.9 |
| 5000 | 25000 | 4459.0 | 9180.7 | 3109.7 | 1004.8 | 420.2 |
| 10000 | 50000 | 9298.7 | 18812.8 | 7193.9 | 2326.2 | 1125.8 |
| 20000 | 100000 | 20855.6 | 42382.5 | 34859.2 | 12513.4 | 2280.5 |

Average time (µs), series E (`V = 1000`):

| V | E | Kruskal (µs) | Prim (µs) | Dijkstra (µs) | SCC (Tarjan) (µs) | Topological sort (DAG) (µs) |
|---|---|---|---|---|---|---|
| 1000 | 1000 | 310.3 | 124.4 | 117.6 | 60.0 | 13.9 |
| 1000 | 2000 | 261.3 | 416.7 | 242.2 | 66.7 | 23.1 |
| 1000 | 5000 | 700.6 | 1331.1 | 424.5 | 100.9 | 54.5 |
| 1000 | 10000 | 1687.0 | 2848.5 | 642.3 | 174.3 | 71.4 |
| 1000 | 20000 | 3712.0 | 5226.9 | 1437.6 | 436.2 | 127.3 |
| 1000 | 50000 | 7874.4 | 13401.0 | 2265.9 | 1055.2 | 387.9 |
| 1000 | 100000 | 15833.7 | 31045.5 | 4946.5 | 1861.6 | 979.6 |

![Kruskal vs Prim time](../../../IdeaProjects/DAA-assignment-3/results/plots/kruskal_vs_prim_time.png)
![Kruskal vs Prim operations](../../../IdeaProjects/DAA-assignment-3/results/plots/kruskal_vs_prim_ops.png)

### 5.4 Task 2 — topological ordering (DAG vs. graph with cycles)

| V | E | input | avg_us | std_us | has_cycle | ordered_vertices | order_valid | edge_exams |
|---|---|---|---|---|---|---|---|---|
| 10 | 30 | DAG | 0.3 | 0.0 | false | 10 | true | 60 |
| 10 | 30 | random digraph | 0.3 | 0.4 | true | 0 | n/a (cycle) | 30 |
| 100 | 500 | DAG | 2.5 | 0.2 | false | 100 | true | 1000 |
| 100 | 500 | random digraph | 1.3 | 0.1 | true | 2 | n/a (cycle) | 507 |
| 1000 | 5000 | DAG | 45.8 | 2.9 | false | 1000 | true | 10000 |
| 1000 | 5000 | random digraph | 25.5 | 1.9 | true | 5 | n/a (cycle) | 5027 |

`ordered_vertices` = how many vertices Kahn's algorithm could place before the queue ran empty. For DAGs it equals `V` and the order is verified valid; for cyclic input the cycle is detected.

![Topo DAG vs cyclic](../../../IdeaProjects/DAA-assignment-3/results/plots/topo_dag_vs_cyclic.png)

### 5.5 Task 2 — strongly connected components

| V | E | avg_us | std_us | components | largest_scc | edge_exams |
|---|---|---|---|---|---|---|
| 10 | 30 | 0.6 | 0.0 | 1 | 10 | 30 |
| 100 | 500 | 11.9 | 0.9 | 3 | 98 | 500 |
| 1000 | 5000 | 109.3 | 7.9 | 23 | 978 | 5000 |

Extended runs (all sizes of both series):

| V | E | avg_us | std_us | components | largest_scc | edge_exams |
|---|---|---|---|---|---|---|
| 10 | 30 | 0.6 | 0.0 | 1 | 10 | 30 |
| 100 | 500 | 11.9 | 0.9 | 3 | 98 | 500 |
| 500 | 2500 | 45.5 | 1.2 | 8 | 493 | 2500 |
| 1000 | 5000 | 109.3 | 7.9 | 23 | 978 | 5000 |
| 2000 | 10000 | 299.5 | 16.8 | 26 | 1975 | 10000 |
| 5000 | 25000 | 1004.8 | 129.4 | 68 | 4933 | 25000 |
| 10000 | 50000 | 2326.2 | 26.9 | 136 | 9865 | 50000 |
| 20000 | 100000 | 12513.4 | 2696.7 | 255 | 19746 | 100000 |
| 1000 | 1000 | 60.0 | 7.0 | 977 | 22 | 1000 |
| 1000 | 2000 | 66.7 | 2.4 | 339 | 662 | 2000 |
| 1000 | 5000 | 100.9 | 1.7 | 23 | 978 | 5000 |
| 1000 | 10000 | 174.3 | 29.7 | 1 | 1000 | 10000 |
| 1000 | 20000 | 436.2 | 92.3 | 1 | 1000 | 20000 |
| 1000 | 50000 | 1055.2 | 240.7 | 1 | 1000 | 50000 |
| 1000 | 100000 | 1861.6 | 289.1 | 1 | 1000 | 100000 |

### 5.6 Task 3 — Dijkstra

| V | E | graph | avg_us | std_us | reachable | edge_exams | successful_relaxations | heap_push | heap_pop | heap_comparisons | stale_pops |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 10 | 30 | undirected connected | 0.9 | 0.0 | 10 | 60 | 17 | 18 | 18 | 84 | 8 |
| 10 | 30 | directed random | 1.2 | 0.1 | 10 | 30 | 14 | 15 | 15 | 53 | 5 |
| 100 | 500 | undirected connected | 33.0 | 5.0 | 100 | 1000 | 192 | 193 | 193 | 2234 | 93 |
| 100 | 500 | directed random | 13.8 | 0.1 | 98 | 493 | 131 | 132 | 132 | 1275 | 34 |
| 1000 | 5000 | undirected connected | 433.9 | 11.7 | 1000 | 10000 | 1931 | 1932 | 1932 | 35464 | 932 |
| 1000 | 5000 | directed random | 372.3 | 87.5 | 995 | 4973 | 1401 | 1402 | 1402 | 23176 | 407 |

`directed random` has unreachable vertices (e.g. 98 of 100, 995 of 1000 reachable from vertex 0), which Dijkstra reports as unreachable (`INF`, no path).

![Dijkstra ops](../../../IdeaProjects/DAA-assignment-3/results/plots/dijkstra_ops.png)

### 5.7 Required plots

**Execution time vs. number of vertices** (log–log, `E = 5V`):

![time vs vertices](../../../IdeaProjects/DAA-assignment-3/results/plots/time_vs_vertices.png)

**Execution time vs. number of edges** (log–log, `V = 1000`):

![time vs edges](../../../IdeaProjects/DAA-assignment-3/results/plots/time_vs_edges.png)

**SCC scaling and measured time ÷ predicted complexity:**

![SCC scaling](../../../IdeaProjects/DAA-assignment-3/results/plots/scc_and_normalised.png)

---

## 6. Comparison and discussion

### 6.1 Kruskal vs. Prim

* **Same answer, different cost.** MST weights are identical in all 15 benchmark configurations. (Trees can differ when weights tie, the weight cannot.)
* **Theory: both `O(E log V)`.** The right-hand plot of §5.7 divides measured time by `E·log₂V`; Kruskal stays at roughly 10–20 ns per unit and Prim at roughly 23–30 ns per unit (for `V ≥ 500`), i.e. both follow the predicted growth and differ by a constant factor.
* **Operation counts explain the constant.** On the `V=1000`, `E=100,000` graph Kruskal sorts with about 1.06 M comparisons and then examines only **3,607 edges** (3.6 %) before the tree is complete. Prim does exactly `2E = 200,000` adjacency examinations, `E = 100,000` heap pushes and pops, and about 3.04 M heap comparisons — roughly 3× more comparisons — because its lazy heap fills with entries that are discarded later (`E − V + 1` stale pops). Measured time: 15.8 ms (Kruskal) vs 31.0 ms (Prim). At `V=20,000`, `E=100,000`: 20.9 ms vs 42.4 ms.
* **Sparse and tiny graphs favour Prim.** At `V = E = 1000` (almost a tree) Prim took 124 µs vs 310 µs for Kruskal, and at `V=10` Prim is also faster; with few edges Kruskal's sort overhead and comparator calls cost more than a short heap run. At the required Medium and Large sizes Kruskal was faster in every measurement (34 vs 52 µs, and 930 vs 1165 µs / 701 vs 1331 µs at Large), though the Large gap is within the noise discussed below.
* **Noise level.** The configuration `V=1000, E=5000` appears in both series and was measured twice: Kruskal 930 vs 701 µs, Prim 1165 vs 1331 µs. Differences below roughly 30 % between two algorithms at the same size are therefore not significant; the ≈2× gaps at large `E` are.
* **Design trade-off.** The lazy-deletion heap is simple but wasteful on dense graphs; an indexed heap with decrease-key (heap size ≤ V) or an `O(V²)` array version would cut Prim's cost there. Kruskal needs all edges up front and a sort, but benefits from the early exit.

### 6.2 DSU optimisations

* On the Large graph (`V=1000`, `E=5000`) the number of parent-pointer hops falls from **401,772** (no optimisation) to 10,329 (compression only), 7,258 (rank only) and **4,668** (both) — an 86× reduction; with both optimisations a `find` needs on average 0.9 hops.
* In the adversarial chain test the gap is dramatic and matches the theory: for `n=16,000` the naive DSU performs **127,992,000** hops (`≈ n²/2`) and takes 207 ms, whereas the other three variants need ≈ `2n` = 32,000 hops and 0.16–0.20 ms — about **1000× faster**. Hops of the naive variant grow 4× when `n` doubles (quadratic); the optimised variants double (linear for `n` operations, i.e. `O(1)` per operation).
* Inside Kruskal the DSU is a small share of the total time (the sort dominates), so end-to-end times of the four variants are close and noisy (e.g. 750–1830 µs at `V=1000`): the hop counts, not the wall-clock difference, are the reliable evidence on random inputs. Both optimisations matter most on adversarial input or when the union-find is the bottleneck.
* Which one helps more? Either alone already removes the quadratic behaviour (≈ `2n` hops in the chain test); together they achieve the lowest hop count on random graphs (4,668 vs 7,258 and 10,329).

### 6.3 Topological ordering: DAGs and cyclic graphs

* For all 15 random DAGs the order is valid (`order_valid = true`); for all 15 random digraphs the cycle is detected.
* **Why the cyclic graphs are cheaper.** In a random digraph with `E = 5V` almost no vertex has in-degree 0, so Kahn's queue empties after only 0–5 vertices (e.g. 5 of 1000) and the algorithm stops, whereas on the DAG it processes all vertices and all `E` edges: 25.5 µs vs 45.8 µs at `V=1000`, `E=5000`. The in-degree pass over all `E` edges is paid in both cases (edge examinations ≈ `E` for cyclic input, `2E` for DAGs). Near the sparse end (`V=E=1000`) 928 of 1000 vertices are ordered before the cycle blocks the rest, and the two run times are equal (≈ 14 µs).
* Scaling matches `O(V + E)` in operation counts exactly (`edge_exams = 2E` for every DAG); see §6.4 for the memory effect on time.

### 6.4 SCC — how runtime changes as the graph grows

* **Operation count is exactly linear.** Tarjan examines each edge exactly once: `edge_exams = E` in every row of the SCC table, and every vertex is visited once.
* **Time grows a bit faster than linearly.** From `V+E = 40` to `120,000` (3,000× larger) the time grows from 0.6 µs to 12.5 ms. Per unit of `V+E` the cost rises from about 15 ns (`V≤1000`) to 35 ns (`V=5000`) and about 100 ns at `V=20,000` (right plot of §5.7). A separate check with different random graphs (not part of the tables) gave 35–38 ns at `V=5000` and 135–200 ns at `V=40,000`, so the trend is reproducible. Since the work counted by the algorithm is exactly linear, the extra cost comes from the machine rather than from the algorithm. My **hypothesis** is memory locality — adjacency lists hold references to separately allocated `Edge` objects, and edges of a random graph point to random vertices, so large graphs cause cache misses — plus noise from a single shared CPU; I did not profile to confirm this.
* **Effect of `E` at fixed `V=1000`:** 60 µs at `E=1000` to 1.86 ms at `E=100,000`. At low density the fixed per-vertex work dominates (100× more edges costs only ≈ 30× more time); between `E=50,000` and `100,000` time grows 1.76×, close to linear in `E`, as `O(V+E)` predicts.
* **Structure of the SCCs** (random digraphs, `V=1000`): `E=1000` → 977 components (largest 22 vertices); `E=2000` → 339 (largest 662); `E=5000` → 23 (largest 978); `E ≥ 10,000` → a single SCC. A giant strongly connected component appears once the average out-degree exceeds 1 and absorbs almost everything at higher density.

### 6.5 Dijkstra

* **Operation counts.** On the undirected connected graphs, edge examinations are exactly `2E`, heap pushes = successful relaxations + 1 (1,932 = 1,931 + 1 at `V=1000, E=5000`), pops equal pushes, and about 48 % of pops are stale at that size.
* **The `E log V` bound is pessimistic.** With `V=1000` fixed and `E` rising 100× (1,000 → 100,000), successful relaxations (and heap pushes) rise only 4× (1,000 → 4,152) and heap comparisons about 7× (12,190 → 88,335), because most edges fail the relaxation test. Run time grows 42× (117 µs → 4.95 ms), driven mostly by scanning `2E` adjacency entries. Heap work grows roughly as `V·log(E/V)`, not `E`.
* **Across sizes (series V):** from 0.9 µs (`V=10`) to 34.9 ms (`V=20,000`); normalised by `E·log₂V` the cost is flat (≈ 9–11 ns) up to `V=10,000`, then jumps to ≈ 25 ns at `V=20,000` — consistent with the memory-locality effect described in §6.4 and with the noise level of this machine.
* **Directed random graphs** are cheaper (each edge is stored once, so `E` examinations instead of `2E`) and leave a few vertices unreachable; these are reported as `INF`/no path and do not affect correctness.

### 6.6 Theory vs. experiment — overall

| Algorithm | Predicted | Observed |
|---|---|---|
| Kruskal | `O(E log V)` | normalised time roughly constant (10–20 ns per `E·log₂V`); early exit examines between 3.6 % (`V=1000, E=100,000`) and ≈ 100 % (`V=1000, E=1000`) of the edges, depending on density |
| Prim | `O(E log V)` | normalised time roughly constant (23–30 ns for `V ≥ 500`); exactly `E` pushes/pops, `2E` scans; ≈ 3× Kruskal's comparisons on dense graphs |
| Kahn | `O(V + E)` | operation count exactly linear; time per unit rises moderately with size |
| Tarjan SCC | `O(V + E)` | operation count exactly linear (`E` examinations); time per unit rises ≈ 7× over 3 orders of magnitude (hypothesised cache effect) |
| Dijkstra | `O(E log V)` | scans `2E`, but heap work grows far slower than `E` |
| DSU | `O(α(n))` | ≈ 0.9 hops/find on random graphs; ≈ 2 hops per element on the adversarial chain, versus `n/2` hops/find for the naive version |

Limitations: one CPU core, JIT and GC noise (std-dev columns show it; differences under ≈ 30 % should not be interpreted), a single seed per configuration, and a single JVM run. The operation counts are exact and are the more robust basis for comparing algorithms.

---

## 7. Testing

Run with `./scripts/run_tests.sh`; **291 checks, 0 failures** (log in `results/test_output.txt`). The suite uses small hand-checked graphs *and* random graphs cross-checked against independent brute-force oracles.

| Required test category | What is tested |
|---|---|
| **Small graphs** | 4- and 5-vertex textbook MSTs (weights 6 and 16); triangle with one heavy edge; single edge; textbook Dijkstra graph; textbook DAG and SCC graph |
| **Disconnected graphs** | Graph with 3 components incl. an isolated vertex: both MST algorithms report `connected = false`, 3 components, forest weight 8; graph with no edges |
| **Graphs containing cycles** | Triangle MST drops the heavy edge; Kahn detects a 3-cycle with tail, a self-loop, and a cycle injected into 300 random DAGs (all detected); random digraphs of benchmark size are cyclic |
| **Multiple valid MSTs** | Equal-weight 4-cycle (4 MSTs), K4 with all weights equal (16 MSTs), mostly-equal weights: Kruskal and Prim give identical weight, both return `V−1` edges |
| **Unreachable vertices** | Dijkstra reports `INF` and empty path for unreachable vertices, `reachableCount` correct; one-way road is not traversable backwards |
| **Duplicate weights** | Random graphs with weights in `{1..k}`, k ≤ 6, compared with brute force; parallel edges (cheapest chosen); ties in Dijkstra |
| **Single vertex** | MST (empty tree, weight 0, connected), topological sort (`[0]`), SCC (1 component), Dijkstra (distance 0), DSU |

**Correctness properties verified**

* *Kruskal == Prim:* 300 random tiny graphs compared to a **brute force over all spanning-edge subsets**, 200 further random graphs, and the benchmark sizes (including very sparse and very dense).
* *Topological order valid for every DAG:* 300 random DAGs checked with `isValidOrder`, plus the 1000/5000 benchmark DAG.
* *Cyclic graphs identified:* every injected cycle is detected; the order is `null` and `processed < V`.
* *Every vertex is in exactly one SCC:* checked for every test graph; Tarjan compared against a **reachability (transitive closure) oracle** on 300 random digraphs; a 200,000-vertex path confirms there is no stack overflow.
* *Dijkstra:* compared with **Bellman–Ford** on 400 random directed/undirected graphs; reconstructed paths start at the source, end at the target and their edge weights sum to the reported distance.
* *Negative weights:* rejected with an exception; the unguarded variant demonstrably returns a wrong distance on the counter-example of §3.7 (test asserts true `1` vs reported `2`).
* *DSU:* all four optimisation variants agree on 2,000 random unions; the optimised DSU needs > 100× fewer hops on a long chain.
* *Generators:* same seed → identical graph; different seed → different graph; no self-loops/parallel edges; impossible parameters rejected.

---

## 8. Conclusion

* **Minimum spanning tree.** Kruskal and Prim always return the same weight. Kruskal (sort + DSU) was ≈ 2× faster than the heap-based Prim on large and dense inputs here because it sorts once and stops after `V−1` edges, while the lazy heap in Prim handles every edge and discards most entries. Prim was faster on very sparse graphs and tiny inputs. Use Kruskal when edges are available as a list and the graph is dense or large; use Prim when the graph is already an adjacency structure and sparse (or in an indexed-heap / `O(V²)` form on dense graphs).
* **DSU.** Path compression and union by rank each remove the quadratic worst case; together they give `≈ α(n)` behaviour (0.9 hops/find on the benchmark graphs, ≈ 1000× less work than naive linking on an adversarial chain). Implement both — they cost a few lines.
* **Connectivity.** Kahn's algorithm gives a topological order and detects cycles in `O(V+E)`; use it for dependency/scheduling problems (e.g. starting infrastructure services in order), and treat "no order" as a cycle alarm. Tarjan's SCC runs in one `O(V+E)` pass and identifies mutually reachable zones of one-way street networks; I implemented it iteratively to be safe on deep graphs. Empirically both scale linearly in operations and slightly worse in time on large random graphs, probably because of memory locality.
* **Shortest paths.** Dijkstra with a binary heap is the right tool for non-negative distances; its heap work grows much more slowly than `E` in practice. It must not be used with negative weights (shown by theory and by a failing counter-example); use Bellman–Ford/Johnson there.
* **Practical lesson.** Asymptotic classes predicted the growth correctly, but constants (sorting vs. heap, edge scanning, memory locality) decided which algorithm was faster, and operation counters were a more reliable measure than wall-clock time on a noisy single-core machine.

---

## 9. How to build, test and reproduce

Requirements: JDK 17+ (developed with 21), Python 3 with matplotlib (only for plots).

```bash
./scripts/build.sh            # javac -> out/
./scripts/run_tests.sh        # builds, runs 291 checks, exit code 1 on failure
java -cp out app.Demo         # smart-city example for all three tasks
./scripts/run_experiments.sh  # builds, runs all experiments (~2-3 min), rewrites results/tables and results/plots
```

Everything is deterministic except wall-clock timings: graphs, operation counts and MST weights are identical on every run (fixed seeds); times vary with the machine.
