package connectivity;

import graph.Edge;
import graph.Graph;

public class TopologicalSort {
    public TopoResult run(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("topological order needs a directed graph");
        int n = g.vertexCount();
        int[] indeg = new int[n];
        long examined = 0;
        for (int u = 0; u < n; u++) for (Edge e : g.neighbors(u)) { indeg[e.to()]++; examined++; }

        int[] queue = new int[n];
        int head = 0, tail = 0;
        for (int v = 0; v < n; v++) if (indeg[v] == 0) queue[tail++] = v;

        while (head < tail) {
            int u = queue[head++];
            for (Edge e : g.neighbors(u)) {
                examined++;
                if (--indeg[e.to()] == 0) queue[tail++] = e.to();
            }
        }
        boolean cyclic = tail < n;
        TopoResult r = new TopoResult(cyclic ? null : queue, cyclic, tail);
        r.ops.put("edgeExaminations", examined);
        r.ops.put("queuePushes", (long) tail);
        return r;
    }

    public static boolean isValidOrder(Graph g, int[] order) {
        int n = g.vertexCount();
        if (order == null || order.length != n) return false;
        int[] pos = new int[n];
        java.util.Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) {
            if (order[i] < 0 || order[i] >= n || pos[order[i]] != -1) return false;
            pos[order[i]] = i;
        }
        for (Edge e : g.edges()) if (pos[e.from()] >= pos[e.to()]) return false;
        return true;
    }
}
