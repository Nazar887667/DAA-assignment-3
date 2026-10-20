package shortestpath;

import graph.BinaryMinHeap;
import graph.Edge;
import graph.Graph;

public class Dijkstra {
    private final boolean validate;

    public Dijkstra() { this(true); }

    public Dijkstra(boolean validate) { this.validate = validate; }

    public DijkstraResult run(Graph g, int source) {
        int n = g.vertexCount();
        if (source < 0 || source >= n) throw new IllegalArgumentException("bad source");
        if (validate && g.hasNegativeWeight())
            throw new IllegalArgumentException("Dijkstra requires non-negative edge weights");

        long[] dist = new long[n];
        int[] parent = new int[n];
        boolean[] done = new boolean[n];
        java.util.Arrays.fill(dist, DijkstraResult.INF);
        java.util.Arrays.fill(parent, -1);
        dist[source] = 0;

        BinaryMinHeap pq = new BinaryMinHeap();
        pq.push(0, source, -1);
        long relaxAttempts = 0, relaxSuccess = 0, stale = 0;

        while (!pq.isEmpty()) {
            pq.pop();
            int u = pq.topA;
            if (done[u]) { stale++; continue; }
            done[u] = true;
            for (Edge e : g.neighbors(u)) {
                relaxAttempts++;
                long nd = dist[u] + e.weight();
                if (nd < dist[e.to()] && !done[e.to()]) {
                    dist[e.to()] = nd;
                    parent[e.to()] = u;
                    relaxSuccess++;
                    pq.push(nd, e.to(), u);
                }
            }
        }
        DijkstraResult r = new DijkstraResult(source, dist, parent);
        r.ops.put("edgeExaminations", relaxAttempts);
        r.ops.put("successfulRelaxations", relaxSuccess);
        r.ops.put("heapPush", pq.pushes);
        r.ops.put("heapPop", pq.pops);
        r.ops.put("heapComparisons", pq.comparisons);
        r.ops.put("staleEntriesSkipped", stale);
        return r;
    }
}
