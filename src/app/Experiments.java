package app;

import connectivity.SCC;
import connectivity.SCCResult;
import connectivity.TopoResult;
import connectivity.TopologicalSort;
import dsu.DSU;
import graph.Graph;
import graph.GraphGenerator;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Experiments {
    static final long SEED = 42;
    static final int REPS = 5;
    static final int WARMUP = 3;
    static final int WARMUP_MS = 400;
    static final int BATCH_MS = 20;
    static final int GLOBAL_WARMUP_MS = 4000;
    static final int MAX_WEIGHT = 100;
    static volatile long sink;

    static final int[][] SCALE_V = {{10, 30}, {100, 500}, {500, 2500}, {1000, 5000}, {2000, 10000},
            {5000, 25000}, {10000, 50000}, {20000, 100000}};
    static final int E_FIXED_V = 1000;
    static final int[] SCALE_E = {1000, 2000, 5000, 10000, 20000, 50000, 100000};
    static final int[] CHAIN_SIZES = {1000, 2000, 4000, 8000, 16000};

    static double[] time(Runnable r) {
        long start = System.nanoTime();
        int runs = 0;
        while (runs < WARMUP || (System.nanoTime() - start) / 1_000_000 < WARMUP_MS) { r.run(); runs++; }
        long c0 = System.nanoTime();
        for (int i = 0; i < 5; i++) r.run();
        double perRunNs = Math.max(1.0, (System.nanoTime() - c0) / 5.0);
        int k = (int) Math.max(1, Math.min(200_000, BATCH_MS * 1_000_000.0 / perRunNs));
        System.gc();
        double[] t = new double[REPS];
        for (int i = 0; i < REPS; i++) {
            long s = System.nanoTime();
            for (int j = 0; j < k; j++) r.run();
            t[i] = (System.nanoTime() - s) / 1000.0 / k;
        }
        double mean = 0;
        for (double x : t) mean += x;
        mean /= REPS;
        double var = 0;
        for (double x : t) var += (x - mean) * (x - mean);
        return new double[]{mean, Math.sqrt(var / REPS)};
    }

    static void globalWarmup() {
        System.out.println("Global JVM warm-up...");
        int[][] sizes = {{200, 1000}, {2000, 10000}};
        long end = System.nanoTime() + GLOBAL_WARMUP_MS * 1_000_000L;
        long round = 0;
        while (System.nanoTime() < end) {
            for (int[] c : sizes) {
                int V = c[0], E = c[1];
                long seed = GraphGenerator.seedFor(SEED, V, E, 90 + (int) (round % 3));
                Graph und = GraphGenerator.connectedUndirected(V, E, seed, MAX_WEIGHT);
                Graph dg = GraphGenerator.randomDirected(V, E, seed + 1, MAX_WEIGHT);
                Graph dag = GraphGenerator.randomDag(V, E, seed + 2, MAX_WEIGHT);
                for (int i = 0; i < 20; i++) {
                    sink += new Kruskal().run(und).totalWeight + new Kruskal(false, false).run(und).totalWeight
                            + new Kruskal(true, false).run(und).totalWeight + new Kruskal(false, true).run(und).totalWeight;
                    sink += new Prim().run(und).totalWeight;
                    sink += new TopologicalSort().run(dag).processed + new TopologicalSort().run(dg).processed;
                    sink += new SCC().run(dg).count;
                    sink += new Dijkstra().run(und, 0).dist[V - 1] + new Dijkstra().run(dg, 0).reachableCount();
                }
            }
            round++;
        }
    }

    static String f(double x) { return String.format(Locale.ROOT, "%.1f", x); }
    static String ops(Map<String, Long> m, String k) { return m.containsKey(k) ? String.valueOf(m.get(k)) : "-"; }

    static class Table {
        final String name;
        final String[] header;
        final List<String[]> rows = new ArrayList<>();
        Table(String name, String... header) { this.name = name; this.header = header; }
        void add(Object... cells) {
            String[] r = new String[cells.length];
            for (int i = 0; i < r.length; i++) r[i] = String.valueOf(cells[i]);
            rows.add(r);
        }
        void write(Path dir, String mdSeries, int[][] onlyConfigs) throws IOException {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve(name + ".csv")))) {
                w.println(String.join(",", header));
                for (String[] r : rows) w.println(String.join(",", r));
            }
            writeMd(dir.resolve(name + "_required.md"), r -> mdSeries == null || (r[0].equals(mdSeries) && isRequired(r, onlyConfigs)));
            writeMd(dir.resolve(name + "_all.md"), r -> true);
        }
        void writeMd(Path p, java.util.function.Predicate<String[]> keep) throws IOException {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(p))) {
                int skip = (Arrays.asList(header).contains("series")) ? 1 : 0;
                w.println("| " + String.join(" | ", Arrays.copyOfRange(header, skip, header.length)) + " |");
                w.println("|" + "---|".repeat(header.length - skip));
                for (String[] r : rows) if (keep.test(r))
                    w.println("| " + String.join(" | ", Arrays.copyOfRange(r, skip, r.length)) + " |");
            }
        }
        static boolean isRequired(String[] r, int[][] cfgs) {
            if (cfgs == null) return true;
            for (int[] c : cfgs) if (r[1].equals(String.valueOf(c[0])) && r[2].equals(String.valueOf(c[1]))) return true;
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "results/tables");
        Files.createDirectories(dir);

        List<Object[]> configs = new ArrayList<>();      // {series, V, E}
        for (int[] c : SCALE_V) configs.add(new Object[]{"scaleV", c[0], c[1]});
        for (int e : SCALE_E) configs.add(new Object[]{"scaleE", E_FIXED_V, e});
        int[][] required = {{10, 30}, {100, 500}, {1000, 5000}};

        Table mst = new Table("mst", "series", "V", "E", "algorithm", "avg_us", "std_us", "mst_weight", "connected",
                "edge_exams", "comparisons", "dsu_find", "dsu_union", "dsu_path_steps", "heap_push", "heap_pop");
        Table dsuVar = new Table("dsu_variants", "series", "V", "E", "variant", "avg_us", "std_us", "dsu_find", "dsu_union", "dsu_path_steps", "mst_weight");
        Table topo = new Table("topo", "series", "V", "E", "input", "avg_us", "std_us", "has_cycle", "ordered_vertices", "order_valid", "edge_exams");
        Table scc = new Table("scc", "series", "V", "E", "avg_us", "std_us", "components", "largest_scc", "edge_exams");
        Table dij = new Table("dijkstra", "series", "V", "E", "graph", "avg_us", "std_us", "reachable", "edge_exams",
                "successful_relaxations", "heap_push", "heap_pop", "heap_comparisons", "stale_pops");
        Table graphs = new Table("graph_properties", "series", "V", "E", "undirected_connected", "directed_graph_has_cycle",
                "dag_has_cycle", "directed_scc_count", "directed_reachable_from_0");

        globalWarmup();
        System.out.println("Running experiments (seed " + SEED + ", " + REPS + " timed reps after >= " + WARMUP + " warm-up runs / " + WARMUP_MS + " ms, batches of ~" + BATCH_MS + " ms)...");
        for (Object[] cfg : configs) {
            String series = (String) cfg[0];
            int V = (int) cfg[1], E = (int) cfg[2];
            System.out.printf("  [%s] V=%d E=%d%n", series, V, E);
            Graph und = GraphGenerator.connectedUndirected(V, E, GraphGenerator.seedFor(SEED, V, E, 1), MAX_WEIGHT);
            Graph digraph = GraphGenerator.randomDirected(V, E, GraphGenerator.seedFor(SEED, V, E, 2), MAX_WEIGHT);
            Graph dag = GraphGenerator.randomDag(V, E, GraphGenerator.seedFor(SEED, V, E, 3), MAX_WEIGHT);

            MSTResult k = new Kruskal().run(und), p = new Prim().run(und);
            double[] tk = time(() -> sink += new Kruskal().run(und).totalWeight);
            double[] tp = time(() -> sink += new Prim().run(und).totalWeight);
            mst.add(series, V, E, "Kruskal", f(tk[0]), f(tk[1]), k.totalWeight, k.connected, ops(k.ops, "edgeExaminations"),
                    ops(k.ops, "sortComparisons"), ops(k.ops, "dsuFind"), ops(k.ops, "dsuUnion"), ops(k.ops, "dsuPathSteps"), "-", "-");
            mst.add(series, V, E, "Prim", f(tp[0]), f(tp[1]), p.totalWeight, p.connected, ops(p.ops, "edgeExaminations"),
                    ops(p.ops, "heapComparisons"), "-", "-", "-", ops(p.ops, "heapPush"), ops(p.ops, "heapPop"));
            if (k.totalWeight != p.totalWeight) throw new IllegalStateException("MST weight mismatch at V=" + V + " E=" + E);

            boolean[][] flags = {{false, false}, {true, false}, {false, true}, {true, true}};
            String[] names = {"naive (no optimisation)", "path compression only", "union by rank only", "both"};
            for (int i = 0; i < 4; i++) {
                final boolean pc = flags[i][0], rk = flags[i][1];
                MSTResult r = new Kruskal(pc, rk).run(und);
                double[] t = time(() -> sink += new Kruskal(pc, rk).run(und).totalWeight);
                dsuVar.add(series, V, E, names[i], f(t[0]), f(t[1]), ops(r.ops, "dsuFind"), ops(r.ops, "dsuUnion"), ops(r.ops, "dsuPathSteps"), r.totalWeight);
            }

            TopoResult td = new TopologicalSort().run(dag), tc = new TopologicalSort().run(digraph);
            double[] ttd = time(() -> sink += new TopologicalSort().run(dag).processed);
            double[] ttc = time(() -> sink += new TopologicalSort().run(digraph).processed);
            topo.add(series, V, E, "DAG", f(ttd[0]), f(ttd[1]), td.hasCycle, td.processed, TopologicalSort.isValidOrder(dag, td.order), td.ops.get("edgeExaminations"));
            topo.add(series, V, E, "random digraph", f(ttc[0]), f(ttc[1]), tc.hasCycle, tc.processed, "n/a (cycle)", tc.ops.get("edgeExaminations"));

            SCCResult sc = new SCC().run(digraph);
            double[] ts = time(() -> sink += new SCC().run(digraph).count);
            scc.add(series, V, E, f(ts[0]), f(ts[1]), sc.count, sc.largestSize(), sc.ops.get("edgeExaminations"));

            DijkstraResult du = new Dijkstra().run(und, 0), dd = new Dijkstra().run(digraph, 0);
            double[] tdu = time(() -> sink += new Dijkstra().run(und, 0).dist[V - 1]);
            double[] tdd = time(() -> sink += new Dijkstra().run(digraph, 0).reachableCount());
            for (Object[] row : new Object[][]{{"undirected connected", du, tdu}, {"directed random", dd, tdd}}) {
                DijkstraResult dr = (DijkstraResult) row[1];
                double[] tt = (double[]) row[2];
                dij.add(series, V, E, row[0], f(tt[0]), f(tt[1]), dr.reachableCount(), dr.ops.get("edgeExaminations"), dr.ops.get("successfulRelaxations"),
                        dr.ops.get("heapPush"), dr.ops.get("heapPop"), dr.ops.get("heapComparisons"), dr.ops.get("staleEntriesSkipped"));
            }
            graphs.add(series, V, E, k.connected, tc.hasCycle, td.hasCycle, sc.count, dd.reachableCount());
        }

        Table chain = new Table("dsu_chain", "n", "variant", "avg_us", "std_us", "dsu_path_steps");
        System.out.println("  DSU chain stress test");
        boolean[][] flags = {{false, false}, {true, false}, {false, true}, {true, true}};
        String[] names = {"naive (no optimisation)", "path compression only", "union by rank only", "both"};
        for (int n : CHAIN_SIZES) for (int i = 0; i < 4; i++) {
            final boolean pc = flags[i][0], rk = flags[i][1];
            long[] steps = new long[1];
            Runnable job = () -> {
                DSU d = new DSU(n, pc, rk);
                for (int j = 0; j + 1 < n; j++) d.union(j + 1, j);
                long s = 0;
                for (int j = 0; j < n; j++) s += d.find(j);
                steps[0] = d.pathSteps;
                sink += s;
            };
            double[] t = time(job);
            chain.add(n, names[i], f(t[0]), f(t[1]), steps[0]);
        }

        mst.write(dir, "scaleV", required);
        dsuVar.write(dir, "scaleV", required);
        topo.write(dir, "scaleV", required);
        scc.write(dir, "scaleV", required);
        dij.write(dir, "scaleV", required);
        graphs.write(dir, "scaleV", required);
        chain.write(dir, null, null);

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("environment.txt")))) {
            w.println("java.version=" + System.getProperty("java.version"));
            w.println("java.vm.name=" + System.getProperty("java.vm.name"));
            w.println("os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            w.println("cpus=" + Runtime.getRuntime().availableProcessors());
            w.println("max_heap_mb=" + Runtime.getRuntime().maxMemory() / (1024 * 1024));
            w.println("seed=" + SEED + " reps=" + REPS + " min_warmup_runs=" + WARMUP + " min_warmup_ms=" + WARMUP_MS + " batch_ms=" + BATCH_MS + " max_weight=" + MAX_WEIGHT);
        }
        System.out.println("Done. Tables written to " + dir.toAbsolutePath());
    }
}
