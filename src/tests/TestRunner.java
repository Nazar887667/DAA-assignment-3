package tests;

import connectivity.SCC;
import connectivity.SCCResult;
import connectivity.TopoResult;
import connectivity.TopologicalSort;
import dsu.DSU;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraResult;

import java.util.*;

public class TestRunner {
    static int passed = 0, failed = 0;
    static String currentGroup = "";

    static void group(String name) { currentGroup = name; System.out.println("\n== " + name); }

    static void check(boolean cond, String msg) {
        if (cond) { passed++; System.out.println("  [PASS] " + msg); }
        else { failed++; System.out.println("  [FAIL] " + msg + "   <-- in group: " + currentGroup); }
    }

    static void quiet(boolean cond, String msg) {
        if (cond) passed++; else { failed++; System.out.println("  [FAIL] " + msg); }
    }

    static Graph und(int n, int[][] es) {
        Graph g = new Graph(n, false);
        for (int[] e : es) g.addEdge(e[0], e[1], e[2]);
        return g;
    }

    static Graph dir(int n, int[][] es) {
        Graph g = new Graph(n, true);
        for (int[] e : es) g.addEdge(e[0], e[1], e.length > 2 ? e[2] : 1);
        return g;
    }

    public static void main(String[] args) {
        testDsu();
        testMstSmall();
        testMstDisconnectedAndSingle();
        testMstMultipleAndDuplicates();
        testMstRandomAgainstBruteForce();
        testMstRandomLarge();
        testTopological();
        testScc();
        testDijkstra();
        testDijkstraRandomAgainstBellmanFord();
        testGenerators();

        System.out.println("\n==========================================");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void testDsu() {
        group("DSU");
        DSU d = new DSU(6);
        check(d.componentCount() == 6, "initially 6 singleton sets");
        check(d.union(0, 1) && d.union(2, 3), "union of different sets returns true");
        check(!d.union(1, 0), "union of already-joined sets returns false");
        check(d.connected(0, 1) && !d.connected(1, 2), "connected() reflects unions");
        d.union(1, 2);
        check(d.connected(0, 3) && d.componentCount() == 3, "transitive merge: {0,1,2,3},{4},{5}");

        Random rnd = new Random(7);
        int n = 200;
        DSU[] variants = {new DSU(n, true, true), new DSU(n, true, false), new DSU(n, false, true), new DSU(n, false, false)};
        boolean agree = true;
        for (int i = 0; i < 2000; i++) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            boolean first = variants[0].union(a, b);
            for (int k = 1; k < 4; k++) agree &= (variants[k].union(a, b) == first);
        }
        for (int i = 0; i < 300; i++) {
            int a = rnd.nextInt(n), b = rnd.nextInt(n);
            boolean first = variants[0].connected(a, b);
            for (int k = 1; k < 4; k++) agree &= (variants[k].connected(a, b) == first);
        }
        check(agree, "all 4 optimisation variants give identical answers on 2000 random unions");

        int m = 5000;
        DSU naive = new DSU(m, false, false), fast = new DSU(m);
        for (int i = 0; i + 1 < m; i++) { naive.union(i + 1, i); fast.union(i + 1, i); }
        naive.resetCounters(); fast.resetCounters();
        for (int i = 0; i < m; i++) { naive.find(i); fast.find(i); }
        check(fast.pathSteps < naive.pathSteps / 100,
                "chain: optimised DSU needs far fewer pointer hops (" + fast.pathSteps + " vs " + naive.pathSteps + ")");
        check(new DSU(1).componentCount() == 1, "single-element DSU");
    }

    static void sameWeight(Graph g, String name, long expected) {
        MSTResult k = new Kruskal().run(g), p = new Prim().run(g);
        check(k.totalWeight == expected, name + ": Kruskal weight = " + expected + " (got " + k.totalWeight + ")");
        check(p.totalWeight == expected, name + ": Prim weight = " + expected + " (got " + p.totalWeight + ")");
    }

    static void testMstSmall() {
        group("MST: small graphs");
        Graph g = und(4, new int[][]{{0, 1, 1}, {1, 2, 2}, {2, 3, 3}, {0, 3, 4}, {0, 2, 5}});
        sameWeight(g, "4-vertex graph", 6);
        MSTResult k = new Kruskal().run(g);
        check(k.edges.size() == 3 && k.connected && k.components == 1, "Kruskal: 3 edges, connected");

        Graph h = und(5, new int[][]{{0, 1, 2}, {0, 3, 6}, {1, 2, 3}, {1, 3, 8}, {1, 4, 5}, {2, 4, 7}, {3, 4, 9}});
        sameWeight(h, "5-vertex graph (textbook example)", 16);

        Graph c = und(3, new int[][]{{0, 1, 1}, {1, 2, 1}, {0, 2, 10}});
        sameWeight(c, "triangle with one heavy edge", 2);

        sameWeight(und(2, new int[][]{{0, 1, 7}}), "single edge", 7);
    }

    static void testMstDisconnectedAndSingle() {
        group("MST: disconnected graphs and single vertex");
        Graph g = und(6, new int[][]{{0, 1, 4}, {1, 2, 1}, {0, 2, 2}, {3, 4, 5}});
        MSTResult k = new Kruskal().run(g), p = new Prim().run(g);
        check(!k.connected && !p.connected, "both detect the graph is NOT connected");
        check(k.components == 3 && p.components == 3, "3 components: {0,1,2}, {3,4}, {5}");
        check(k.totalWeight == 8 && p.totalWeight == 8, "minimum spanning forest weight = 3 + 5 = 8");
        check(k.edges.size() == 3 && p.edges.size() == 3, "forest has V - components = 3 edges");

        Graph none = new Graph(4, false);
        check(!new Kruskal().run(none).connected && new Kruskal().run(none).totalWeight == 0, "no edges: not connected, weight 0");

        Graph one = new Graph(1, false);
        MSTResult k1 = new Kruskal().run(one), p1 = new Prim().run(one);
        check(k1.connected && p1.connected, "single vertex is connected");
        check(k1.totalWeight == 0 && p1.totalWeight == 0 && k1.edges.isEmpty() && p1.edges.isEmpty(), "single vertex: empty MST, weight 0");
    }

    static void testMstMultipleAndDuplicates() {
        group("MST: multiple valid MSTs and duplicate weights");
        Graph sq = und(4, new int[][]{{0, 1, 5}, {1, 2, 5}, {2, 3, 5}, {3, 0, 5}});
        sameWeight(sq, "equal-weight square (4 valid MSTs)", 15);
        Graph k4 = new Graph(4, false);
        for (int i = 0; i < 4; i++) for (int j = i + 1; j < 4; j++) k4.addEdge(i, j, 3);
        sameWeight(k4, "K4 all weights equal (16 valid MSTs)", 9);
        Graph mix = und(5, new int[][]{{0, 1, 2}, {1, 2, 2}, {2, 3, 2}, {3, 4, 2}, {0, 4, 2}, {0, 2, 3}, {1, 3, 3}});
        sameWeight(mix, "mostly duplicate weights", 8);
        MSTResult a = new Kruskal().run(sq), b = new Prim().run(sq);
        check(a.edges.size() == 3 && b.edges.size() == 3, "both return a tree with V-1 edges even if the trees differ");
        Graph par = und(2, new int[][]{{0, 1, 9}, {0, 1, 4}});
        sameWeight(par, "parallel edges: cheapest one chosen", 4);
    }

    static long bruteForceMst(Graph g) {
        int n = g.vertexCount(), m = g.edgeCount();
        List<Edge> es = g.edges();
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << m); mask++) {
            if (Integer.bitCount(mask) != n - 1) continue;
            DSU d = new DSU(n);
            long w = 0;
            for (int i = 0; i < m; i++) if ((mask >> i & 1) == 1) { d.union(es.get(i).from(), es.get(i).to()); w += es.get(i).weight(); }
            if (d.componentCount() == 1) best = Math.min(best, w);
        }
        return best;
    }

    static void testMstRandomAgainstBruteForce() {
        group("MST: random tiny graphs vs. brute force (all spanning trees enumerated)");
        int ok = 0, total = 0;
        for (int seed = 0; seed < 300; seed++) {
            Random r = new Random(seed);
            int n = 2 + r.nextInt(5);
            int maxE = Math.min(n * (n - 1) / 2, 12);
            int m = (n - 1) + r.nextInt(maxE - (n - 1) + 1);
            Graph g = GraphGenerator.connectedUndirected(n, m, seed, 1 + r.nextInt(6));
            long expected = bruteForceMst(g);
            long k = new Kruskal().run(g).totalWeight, p = new Prim().run(g).totalWeight;
            total++;
            if (k == expected && p == expected) ok++;
            else System.out.println("  mismatch seed=" + seed + " brute=" + expected + " kruskal=" + k + " prim=" + p);
        }
        check(ok == total, "Kruskal == Prim == brute force on " + total + " random graphs (" + ok + " matched)");
    }

    static void testMstRandomLarge() {
        group("MST: Kruskal vs Prim on random connected graphs (incl. the benchmark sizes)");
        int[][] cfg = {{10, 30}, {100, 500}, {1000, 5000}, {500, 499}, {300, 20000}};
        for (int[] c : cfg) {
            Graph g = GraphGenerator.connectedUndirected(c[0], c[1], 99, 100);
            MSTResult k = new Kruskal().run(g), p = new Prim().run(g);
            check(k.connected && p.connected, "V=" + c[0] + " E=" + c[1] + ": generated graph is connected");
            check(k.totalWeight == p.totalWeight, "V=" + c[0] + " E=" + c[1] + ": equal MST weight (" + k.totalWeight + ")");
            check(k.edges.size() == c[0] - 1 && p.edges.size() == c[0] - 1, "V=" + c[0] + ": both trees have V-1 edges");
        }
        for (int seed = 0; seed < 200; seed++) {
            Random r = new Random(seed);
            int n = 5 + r.nextInt(60);
            int m = n - 1 + r.nextInt(3 * n);
            m = Math.min(m, n * (n - 1) / 2);
            Graph g = GraphGenerator.connectedUndirected(n, m, seed, 10);
            quiet(new Kruskal().run(g).totalWeight == new Prim().run(g).totalWeight, "random equality seed=" + seed);
        }
        check(true, "200 further random graphs: Kruskal weight == Prim weight (failures would be printed above)");
        Graph g = GraphGenerator.connectedUndirected(200, 1000, 5, 50);
        long w = new Kruskal().run(g).totalWeight;
        check(new Kruskal(false, false).run(g).totalWeight == w && new Kruskal(true, false).run(g).totalWeight == w
                && new Kruskal(false, true).run(g).totalWeight == w, "Kruskal weight independent of DSU optimisations");
    }

    static void testTopological() {
        group("Topological ordering");
        TopologicalSort ts = new TopologicalSort();
        Graph dag = dir(6, new int[][]{{5, 2}, {5, 0}, {4, 0}, {4, 1}, {2, 3}, {3, 1}});
        TopoResult r = ts.run(dag);
        check(!r.hasCycle && TopologicalSort.isValidOrder(dag, r.order), "textbook DAG: valid order " + Arrays.toString(r.order));

        Graph cyc = dir(4, new int[][]{{0, 1}, {1, 2}, {2, 0}, {2, 3}});
        TopoResult rc = ts.run(cyc);
        check(rc.hasCycle && rc.order == null, "3-cycle plus tail: cycle detected, no order returned");
        check(rc.processed < 4, "only " + rc.processed + " of 4 vertices could be ordered");

        Graph selfLoop = dir(2, new int[][]{{0, 0}, {0, 1}});
        check(ts.run(selfLoop).hasCycle, "self-loop counts as a cycle");

        Graph one = new Graph(1, true);
        TopoResult r1 = ts.run(one);
        check(!r1.hasCycle && r1.order.length == 1 && r1.order[0] == 0, "single vertex: order [0]");

        Graph empty = new Graph(5, true);
        check(!ts.run(empty).hasCycle && TopologicalSort.isValidOrder(empty, ts.run(empty).order), "no edges: any order is valid");

        int validDag = 0, detected = 0, trials = 300;
        for (int seed = 0; seed < trials; seed++) {
            Random rnd = new Random(seed);
            int n = 2 + rnd.nextInt(80);
            int m = rnd.nextInt(Math.min(n * (n - 1) / 2, 4 * n) + 1);
            Graph d = GraphGenerator.randomDag(n, m, seed, 10);
            TopoResult tr = ts.run(d);
            if (!tr.hasCycle && TopologicalSort.isValidOrder(d, tr.order)) validDag++;
            if (d.edgeCount() > 0) {
                Edge e = d.edges().get(0);
                d.addEdge(e.to(), e.from(), 1);
                if (ts.run(d).hasCycle) detected++; else System.out.println("  undetected cycle, seed=" + seed);
            } else detected++;
        }
        check(validDag == trials, "valid order for all " + trials + " random DAGs (" + validDag + ")");
        check(detected == trials, "cycle detected in all " + trials + " DAGs with an injected back edge (" + detected + ")");

        Graph big = GraphGenerator.randomDag(1000, 5000, 42, 100);
        check(TopologicalSort.isValidOrder(big, ts.run(big).order), "benchmark-size DAG (1000/5000) ordered correctly");
        Graph bigCyc = GraphGenerator.randomDirected(1000, 5000, 42, 100);
        check(ts.run(bigCyc).hasCycle, "benchmark-size random digraph (1000/5000) contains a cycle");
    }

    static boolean[][] closure(Graph g) {
        int n = g.vertexCount();
        boolean[][] reach = new boolean[n][n];
        for (int i = 0; i < n; i++) reach[i][i] = true;
        for (Edge e : g.edges()) reach[e.from()][e.to()] = true;
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) if (reach[i][k]) for (int j = 0; j < n; j++) if (reach[k][j]) reach[i][j] = true;
        return reach;
    }

    static boolean everyVertexInExactlyOneScc(SCCResult r, int n) {
        int[] seen = new int[n];
        int total = 0;
        for (List<Integer> c : r.components) for (int v : c) { seen[v]++; total++; }
        for (int v = 0; v < n; v++) if (seen[v] != 1) return false;
        return total == n;
    }

    static void testScc() {
        group("Strongly connected components");
        SCC scc = new SCC();
        Graph g = dir(8, new int[][]{{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 5}, {5, 3}, {6, 5}, {6, 7}, {7, 6}});
        SCCResult r = scc.run(g);
        check(r.count == 3, "textbook graph has exactly 3 SCCs");
        check(r.componentOf[0] == r.componentOf[1] && r.componentOf[1] == r.componentOf[2], "{0,1,2} together");
        check(r.componentOf[3] == r.componentOf[4] && r.componentOf[4] == r.componentOf[5], "{3,4,5} together");
        check(r.componentOf[6] == r.componentOf[7] && r.componentOf[0] != r.componentOf[3] && r.componentOf[3] != r.componentOf[6], "{6,7} separate from others");
        check(everyVertexInExactlyOneScc(r, 8), "every vertex belongs to exactly one SCC");

        Graph dag = GraphGenerator.randomDag(50, 120, 3, 5);
        check(scc.run(dag).count == 50, "DAG: every vertex is its own SCC");
        Graph ring = new Graph(10, true);
        for (int i = 0; i < 10; i++) ring.addEdge(i, (i + 1) % 10, 1);
        check(scc.run(ring).count == 1, "directed ring: one SCC");
        check(scc.run(new Graph(1, true)).count == 1, "single vertex: one SCC");
        check(scc.run(new Graph(5, true)).count == 5, "no edges: V singleton SCCs");

        Graph path = new Graph(200000, true);
        for (int i = 0; i + 1 < 200000; i++) path.addEdge(i, i + 1, 1);
        check(scc.run(path).count == 200000, "200,000-vertex path handled without stack overflow");

        int ok = 0, trials = 300;
        for (int seed = 0; seed < trials; seed++) {
            Random rnd = new Random(seed);
            int n = 1 + rnd.nextInt(25);
            int m = rnd.nextInt(Math.min(n * (n - 1), 2 * n) + 1);
            Graph rg = GraphGenerator.randomDirected(n, m, seed, 5);
            SCCResult sr = scc.run(rg);
            boolean[][] reach = closure(rg);
            boolean good = everyVertexInExactlyOneScc(sr, n);
            for (int i = 0; i < n && good; i++) for (int j = 0; j < n; j++) {
                boolean same = reach[i][j] && reach[j][i];
                if (same != (sr.componentOf[i] == sr.componentOf[j])) { good = false; break; }
            }
            if (good) ok++; else System.out.println("  SCC mismatch seed=" + seed);
        }
        check(ok == trials, "Tarjan == reachability oracle on " + trials + " random digraphs (" + ok + " matched)");
    }

    static long[] bellmanFord(Graph g, int s) {
        int n = g.vertexCount();
        long[] d = new long[n];
        Arrays.fill(d, DijkstraResult.INF);
        d[s] = 0;
        for (int i = 0; i < n; i++) {
            boolean changed = false;
            for (Edge e : g.edges()) {
                if (d[e.from()] != DijkstraResult.INF && d[e.from()] + e.weight() < d[e.to()]) { d[e.to()] = d[e.from()] + e.weight(); changed = true; }
                if (!g.isDirected() && d[e.to()] != DijkstraResult.INF && d[e.to()] + e.weight() < d[e.from()]) { d[e.from()] = d[e.to()] + e.weight(); changed = true; }
            }
            if (!changed) break;
        }
        return d;
    }

    static void testDijkstra() {
        group("Dijkstra");
        Graph g = dir(6, new int[][]{{0, 1, 7}, {0, 2, 9}, {0, 5, 14}, {1, 2, 10}, {1, 3, 15}, {2, 3, 11}, {2, 5, 2}, {3, 4, 6}, {5, 4, 9}});
        DijkstraResult r = new Dijkstra().run(g, 0);
        check(Arrays.equals(r.dist, new long[]{0, 7, 9, 20, 20, 11}), "textbook graph distances " + Arrays.toString(r.dist));
        check(r.pathTo(4).equals(List.of(0, 2, 5, 4)), "path to 4 is 0-2-5-4");
        check(r.pathTo(0).equals(List.of(0)), "path to source is [source]");

        Graph u = dir(5, new int[][]{{0, 1, 3}, {1, 2, 4}, {3, 4, 1}});
        DijkstraResult ru = new Dijkstra().run(u, 0);
        check(ru.dist[3] == DijkstraResult.INF && ru.dist[4] == DijkstraResult.INF, "vertices 3,4 unreachable -> INF");
        check(ru.pathTo(3).isEmpty() && !ru.isReachable(4) && ru.reachableCount() == 3, "no path for unreachable vertices, 3 reachable");

        Graph oneWay = dir(2, new int[][]{{0, 1, 5}});
        check(new Dijkstra().run(oneWay, 1).dist[0] == DijkstraResult.INF, "one-way road: 0 not reachable from 1");

        check(new Dijkstra().run(new Graph(1, true), 0).dist[0] == 0, "single vertex: distance 0");
        Graph zero = dir(3, new int[][]{{0, 1, 0}, {1, 2, 0}});
        check(Arrays.equals(new Dijkstra().run(zero, 0).dist, new long[]{0, 0, 0}), "zero-weight edges allowed");
        Graph ties = dir(4, new int[][]{{0, 1, 1}, {0, 2, 1}, {1, 3, 1}, {2, 3, 1}});
        check(new Dijkstra().run(ties, 0).dist[3] == 2, "two equal-length shortest paths: distance 2");
        Graph undirected = und(3, new int[][]{{0, 1, 2}, {1, 2, 2}, {0, 2, 5}});
        check(new Dijkstra().run(undirected, 2).dist[0] == 4, "undirected graph: 2->1->0 = 4");

        Graph neg = dir(3, new int[][]{{0, 1, 2}, {0, 2, 5}, {2, 1, -4}});
        boolean threw = false;
        try { new Dijkstra().run(neg, 0); } catch (IllegalArgumentException ex) { threw = true; }
        check(threw, "negative edge weight is rejected with IllegalArgumentException");
        long[] truth = bellmanFord(neg, 0);
        long[] wrong = new Dijkstra(false).run(neg, 0).dist;
        check(truth[1] == 1 && wrong[1] == 2, "counter-example: true dist(1) = 1 (via 0->2->1) but unguarded Dijkstra says " + wrong[1]);
    }

    static void testDijkstraRandomAgainstBellmanFord() {
        group("Dijkstra vs. Bellman-Ford oracle on random graphs");
        int ok = 0, trials = 400;
        for (int seed = 0; seed < trials; seed++) {
            Random rnd = new Random(seed);
            int n = 1 + rnd.nextInt(40);
            boolean directed = rnd.nextBoolean();
            Graph g;
            if (directed) g = GraphGenerator.randomDirected(n, rnd.nextInt(Math.min(n * (n - 1), 4 * n) + 1), seed, 20);
            else {
                int maxE = n * (n - 1) / 2;
                int m = Math.min(maxE, n - 1 + rnd.nextInt(2 * n));
                g = n == 1 ? new Graph(1, false) : GraphGenerator.connectedUndirected(n, Math.max(m, n - 1), seed, 20);
            }
            int src = rnd.nextInt(n);
            long[] d = new Dijkstra().run(g, src).dist;
            if (Arrays.equals(d, bellmanFord(g, src))) ok++; else System.out.println("  mismatch seed=" + seed);
        }
        check(ok == trials, "Dijkstra == Bellman-Ford on " + trials + " random graphs (" + ok + " matched)");

        Graph g = GraphGenerator.randomDirected(300, 1500, 8, 50);
        DijkstraResult r = new Dijkstra().run(g, 0);
        boolean consistent = true;
        for (int v = 0; v < 300; v++) {
            if (!r.isReachable(v)) continue;
            List<Integer> p = r.pathTo(v);
            long sum = 0;
            for (int i = 0; i + 1 < p.size(); i++) {
                long best = Long.MAX_VALUE;
                for (Edge e : g.neighbors(p.get(i))) if (e.to() == p.get(i + 1)) best = Math.min(best, e.weight());
                sum += best;
            }
            consistent &= (sum == r.dist[v]) && p.get(0) == 0 && p.get(p.size() - 1) == v;
        }
        check(consistent, "reconstructed paths start at the source, end at the target and sum to the reported distance");
    }

    static void testGenerators() {
        group("Graph generators (reproducibility)");
        Graph a = GraphGenerator.connectedUndirected(100, 500, 123, 100), b = GraphGenerator.connectedUndirected(100, 500, 123, 100);
        check(a.edges().equals(b.edges()), "same seed -> identical graph");
        Graph c = GraphGenerator.connectedUndirected(100, 500, 124, 100);
        check(!a.edges().equals(c.edges()), "different seed -> different graph");
        check(a.edgeCount() == 500, "requested edge count produced exactly");
        Set<String> seen = new HashSet<>();
        boolean simple = true;
        for (Edge e : a.edges()) { simple &= e.from() != e.to(); simple &= seen.add(Math.min(e.from(), e.to()) + ":" + Math.max(e.from(), e.to())); }
        check(simple, "no self-loops or parallel edges");
        boolean threw = false;
        try { GraphGenerator.connectedUndirected(10, 5, 1, 10); } catch (IllegalArgumentException ex) { threw = true; }
        check(threw, "too few edges for a connected graph is rejected");
    }
}
