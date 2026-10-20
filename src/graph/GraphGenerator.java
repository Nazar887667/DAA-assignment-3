package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class GraphGenerator {
    private GraphGenerator() {}

    public static long seedFor(long base, int n, int m, int kind) {
        return base * 1_000_003L + n * 10_007L + m * 31L + kind;
    }

    public static Graph connectedUndirected(int n, int m, long seed, int maxWeight) {
        long maxEdges = (long) n * (n - 1) / 2;
        if (m < n - 1 || m > maxEdges) throw new IllegalArgumentException("need n-1 <= m <= n(n-1)/2");
        Random rnd = new Random(seed);
        Graph g = new Graph(n, false);
        Set<Long> used = new HashSet<>();
        int[] perm = shuffled(n, rnd);
        for (int i = 1; i < n; i++) {
            int u = perm[i], v = perm[rnd.nextInt(i)];
            used.add(key(u, v, n, false));
            g.addEdge(u, v, 1 + rnd.nextInt(maxWeight));
        }
        addRandomEdges(g, used, m, rnd, maxWeight, false);
        return g;
    }

    public static Graph randomDirected(int n, int m, long seed, int maxWeight) {
        long maxEdges = (long) n * (n - 1);
        if (m > maxEdges) throw new IllegalArgumentException("too many edges");
        Random rnd = new Random(seed);
        Graph g = new Graph(n, true);
        addRandomEdges(g, new HashSet<>(), m, rnd, maxWeight, true);
        return g;
    }

    public static Graph randomDag(int n, int m, long seed, int maxWeight) {
        long maxEdges = (long) n * (n - 1) / 2;
        if (m > maxEdges) throw new IllegalArgumentException("too many edges for a DAG");
        Random rnd = new Random(seed);
        int[] perm = shuffled(n, rnd);
        Graph g = new Graph(n, true);
        Set<Long> used = new HashSet<>();
        while (g.edgeCount() < m) {
            int i = rnd.nextInt(n), j = rnd.nextInt(n);
            if (i == j) continue;
            if (i > j) { int t = i; i = j; j = t; }
            int u = perm[i], v = perm[j];
            if (used.add(key(u, v, n, true))) g.addEdge(u, v, 1 + rnd.nextInt(maxWeight));
        }
        return g;
    }

    private static void addRandomEdges(Graph g, Set<Long> used, int m, Random rnd, int maxW, boolean directed) {
        int n = g.vertexCount();
        while (g.edgeCount() < m) {
            int u = rnd.nextInt(n), v = rnd.nextInt(n);
            if (u == v) continue;
            if (used.add(key(u, v, n, directed))) g.addEdge(u, v, 1 + rnd.nextInt(maxW));
        }
    }

    private static long key(int u, int v, int n, boolean directed) {
        if (!directed && u > v) { int t = u; u = v; v = t; }
        return (long) u * n + v;
    }

    private static int[] shuffled(int n, Random rnd) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        return p;
    }
}
