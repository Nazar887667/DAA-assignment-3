#!/usr/bin/env python3
"""Reads results/tables/*.csv and writes the plots into results/plots/."""
import csv, math, os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
T = os.path.join(ROOT, "results", "tables")
P = os.path.join(ROOT, "results", "plots")
os.makedirs(P, exist_ok=True)


def read(name):
    with open(os.path.join(T, name + ".csv")) as f:
        return list(csv.DictReader(f))


mst, topo, scc, dij = read("mst"), read("topo"), read("scc"), read("dijkstra")
dsuvar, chain = read("dsu_variants"), read("dsu_chain")


def series(rows, key_x, cond, key_y="avg_us"):
    pts = sorted((float(r[key_x]), float(r[key_y])) for r in rows if cond(r))
    return [p[0] for p in pts], [p[1] for p in pts]


def algo_series(sname):
    """name -> rows filter, for the five algorithms on a given series"""
    return {
        "Kruskal": (mst, lambda r: r["series"] == sname and r["algorithm"] == "Kruskal"),
        "Prim": (mst, lambda r: r["series"] == sname and r["algorithm"] == "Prim"),
        "Dijkstra": (dij, lambda r: r["series"] == sname and r["graph"] == "undirected connected"),
        "SCC (Tarjan)": (scc, lambda r: r["series"] == sname),
        "Topological sort (DAG)": (topo, lambda r: r["series"] == sname and r["input"] == "DAG"),
    }


STYLE = {"Kruskal": ("tab:blue", "o"), "Prim": ("tab:orange", "s"), "Dijkstra": ("tab:green", "^"),
         "SCC (Tarjan)": ("tab:red", "D"), "Topological sort (DAG)": ("tab:purple", "v")}


def main_plot(sname, xkey, xlabel, title, fname):
    fig, ax = plt.subplots(figsize=(7.5, 5))
    for name, (rows, cond) in algo_series(sname).items():
        x, y = series(rows, xkey, cond)
        c, m = STYLE[name]
        ax.plot(x, y, marker=m, color=c, label=name)
    ax.set_xscale("log"); ax.set_yscale("log")
    ax.set_xlabel(xlabel); ax.set_ylabel("average execution time (µs, 5 runs)")
    ax.set_title(title); ax.grid(True, which="both", alpha=0.3); ax.legend()
    fig.tight_layout(); fig.savefig(os.path.join(P, fname), dpi=150); plt.close(fig)


main_plot("scaleV", "V", "number of vertices V  (E = 5V; V=10 uses E=30)",
          "Execution time vs. number of vertices", "time_vs_vertices.png")
main_plot("scaleE", "E", "number of edges E  (V = 1000 fixed)",
          "Execution time vs. number of edges", "time_vs_edges.png")

fig, axes = plt.subplots(1, 2, figsize=(12, 4.6))
for ax, (sname, xkey, xl) in zip(axes, [("scaleV", "V", "V (E = 5V)"), ("scaleE", "E", "E (V = 1000)")]):
    for algo, (c, m) in (("Kruskal", STYLE["Kruskal"]), ("Prim", STYLE["Prim"])):
        x, y = series(mst, xkey, lambda r: r["series"] == sname and r["algorithm"] == algo)
        ax.plot(x, y, marker=m, color=c, label=algo)
    ax.set_xscale("log"); ax.set_yscale("log"); ax.set_xlabel(xl); ax.set_ylabel("avg time (µs)")
    ax.grid(True, which="both", alpha=0.3); ax.legend()
fig.suptitle("Kruskal vs. Prim: execution time"); fig.tight_layout()
fig.savefig(os.path.join(P, "kruskal_vs_prim_time.png"), dpi=150); plt.close(fig)

fig, ax = plt.subplots(figsize=(7.5, 5))
for algo, key, c, m, lab in (("Kruskal", "comparisons", "tab:blue", "o", "Kruskal: sort comparisons"),
                             ("Prim", "comparisons", "tab:orange", "s", "Prim: heap comparisons"),
                             ("Kruskal", "edge_exams", "tab:cyan", "^", "Kruskal: edges examined"),
                             ("Prim", "edge_exams", "tab:red", "v", "Prim: edge examinations (adjacency scans)")):
    x, y = series(mst, "E", lambda r, a=algo: r["series"] == "scaleE" and r["algorithm"] == a, key)
    ax.plot(x, y, marker=m, color=c, label=lab)
ax.set_xscale("log"); ax.set_yscale("log"); ax.set_xlabel("E (V = 1000)"); ax.set_ylabel("operation count")
ax.set_title("Kruskal vs. Prim: operation counts"); ax.grid(True, which="both", alpha=0.3); ax.legend(fontsize=8)
fig.tight_layout(); fig.savefig(os.path.join(P, "kruskal_vs_prim_ops.png"), dpi=150); plt.close(fig)

fig, axes = plt.subplots(1, 2, figsize=(12, 4.6))
cols = {"naive (no optimisation)": "tab:red", "path compression only": "tab:orange", "union by rank only": "tab:green", "both": "tab:blue"}
for variant, c in cols.items():
    x, y = series(chain, "n", lambda r, v=variant: r["variant"] == v, "dsu_path_steps")
    axes[0].plot(x, y, marker="o", color=c, label=variant)
    x, y = series(chain, "n", lambda r, v=variant: r["variant"] == v, "avg_us")
    axes[1].plot(x, y, marker="o", color=c, label=variant)
axes[0].set_ylabel("parent-pointer hops"); axes[1].set_ylabel("avg time (µs)")
for ax in axes:
    ax.set_xscale("log"); ax.set_yscale("log"); ax.set_xlabel("n (elements in the chain)"); ax.grid(True, which="both", alpha=0.3); ax.legend(fontsize=8)
fig.suptitle("DSU worst-case chain: effect of path compression and union by rank"); fig.tight_layout()
fig.savefig(os.path.join(P, "dsu_chain.png"), dpi=150); plt.close(fig)

fig, axes = plt.subplots(1, 2, figsize=(12, 4.6))
x, y = series(scc, "V", lambda r: r["series"] == "scaleV")
xe = [5 * v if v > 10 else 30 for v in x]
axes[0].plot([a + b for a, b in zip(x, xe)], y, marker="D", color="tab:red", label="SCC, V varies (E=5V)")
x2, y2 = series(scc, "E", lambda r: r["series"] == "scaleE")
axes[0].plot([1000 + e for e in x2], y2, marker="o", color="tab:brown", label="SCC, E varies (V=1000)")
axes[0].set_xscale("log"); axes[0].set_yscale("log"); axes[0].set_xlabel("V + E"); axes[0].set_ylabel("avg time (µs)")
axes[0].set_title("SCC runtime vs. input size (expected O(V+E))"); axes[0].grid(True, which="both", alpha=0.3); axes[0].legend()

def norm(rows, cond, f):
    out = []
    for r in rows:
        if cond(r):
            V, E, t = float(r["V"]), float(r["E"]), float(r["avg_us"])
            out.append((V, 1000.0 * t / f(V, E)))
    return sorted(out)

for name, (rows, cond) in algo_series("scaleV").items():
    if name in ("SCC (Tarjan)", "Topological sort (DAG)"):
        f = lambda V, E: V + E
    else:
        f = lambda V, E: (E * math.log2(max(V, 2)))
    pts = norm(rows, cond, f)
    c, m = STYLE[name]
    axes[1].plot([p[0] for p in pts], [p[1] for p in pts], marker=m, color=c, label=name)
axes[1].set_xscale("log"); axes[1].set_yscale("log"); axes[1].set_xlabel("V (E = 5V)")
axes[1].set_ylabel("ns per unit of predicted work")
axes[1].set_title("Time ÷ predicted complexity\n(V+E for SCC/topo, E·log₂V for MST/Dijkstra)")
axes[1].grid(True, which="both", alpha=0.3); axes[1].legend(fontsize=8)
fig.tight_layout(); fig.savefig(os.path.join(P, "scc_and_normalised.png"), dpi=150); plt.close(fig)

fig, ax = plt.subplots(figsize=(7.5, 5))
for inp, c, m in (("DAG", "tab:purple", "v"), ("random digraph", "tab:gray", "x")):
    x, y = series(topo, "V", lambda r, i=inp: r["series"] == "scaleV" and r["input"] == i)
    ax.plot(x, y, marker=m, color=c, label=inp + (" (order produced)" if inp == "DAG" else " (cycle detected)"))
ax.set_xscale("log"); ax.set_yscale("log"); ax.set_xlabel("V (E = 5V)"); ax.set_ylabel("avg time (µs)")
ax.set_title("Kahn topological sort: DAG vs. cyclic input"); ax.grid(True, which="both", alpha=0.3); ax.legend()
fig.tight_layout(); fig.savefig(os.path.join(P, "topo_dag_vs_cyclic.png"), dpi=150); plt.close(fig)

fig, ax = plt.subplots(figsize=(7.5, 5))
for key, c, m in (("edge_exams", "tab:green", "o"), ("heap_push", "tab:olive", "s"), ("heap_comparisons", "tab:teal" if "tab:teal" in matplotlib.colors.TABLEAU_COLORS else "tab:cyan", "^")):
    x, y = series(dij, "V", lambda r: r["series"] == "scaleV" and r["graph"] == "undirected connected", key)
    ax.plot(x, y, marker=m, color=c, label=key)
ax.set_xscale("log"); ax.set_yscale("log"); ax.set_xlabel("V (E = 5V)"); ax.set_ylabel("operation count")
ax.set_title("Dijkstra operation counts"); ax.grid(True, which="both", alpha=0.3); ax.legend()
fig.tight_layout(); fig.savefig(os.path.join(P, "dijkstra_ops.png"), dpi=150); plt.close(fig)
print("plots written to", P)


def summary(sname, xkey):
    rows = {}
    for name, (data, cond) in algo_series(sname).items():
        for r in data:
            if cond(r):
                rows.setdefault((int(r["V"]), int(r["E"])), {})[name] = float(r["avg_us"])
    names = list(algo_series(sname).keys())
    out = ["| V | E | " + " | ".join(n + " (µs)" for n in names) + " |", "|" + "---|" * (2 + len(names))]
    for (V, E) in sorted(rows, key=lambda k: (k[0], k[1])):
        out.append(f"| {V} | {E} | " + " | ".join(f"{rows[(V, E)][n]:.1f}" for n in names) + " |")
    return "\n".join(out) + "\n"


for sname, xkey in (("scaleV", "V"), ("scaleE", "E")):
    with open(os.path.join(T, f"scaling_summary_{sname}.md"), "w") as f:
        f.write(summary(sname, xkey))
print("scaling summaries written")
