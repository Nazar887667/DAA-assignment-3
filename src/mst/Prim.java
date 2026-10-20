package mst;

import graph.BinaryMinHeap;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;

public class Prim {
    public MSTResult run(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST needs an undirected graph");
        int n = g.vertexCount();
        boolean[] visited = new boolean[n];
        BinaryMinHeap pq = new BinaryMinHeap();
        List<Edge> tree = new ArrayList<>();
        long total = 0, examined = 0, stale = 0;
        int components = 0;

        for (int s = 0; s < n; s++) {
            if (visited[s]) continue;
            components++;
            visited[s] = true;
            for (Edge e : g.neighbors(s)) { examined++; pq.push(e.weight(), e.to(), s); }
            while (!pq.isEmpty()) {
                pq.pop();
                int v = pq.topA, parent = pq.topB;
                if (visited[v]) { stale++; continue; }
                visited[v] = true;
                int w = (int) pq.topKey;
                tree.add(new Edge(parent, v, w));
                total += w;
                for (Edge e : g.neighbors(v)) {
                    examined++;
                    if (!visited[e.to()]) pq.push(e.weight(), e.to(), v);
                }
            }
        }
        MSTResult r = new MSTResult("Prim", tree, total, components, components <= 1);
        r.ops.put("edgeExaminations", examined);
        r.ops.put("heapPush", pq.pushes);
        r.ops.put("heapPop", pq.pops);
        r.ops.put("heapComparisons", pq.comparisons);
        r.ops.put("staleEntriesSkipped", stale);
        return r;
    }
}
