package mst;

import dsu.DSU;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Kruskal {
    private final boolean pathCompression;
    private final boolean unionByRank;

    public Kruskal() { this(true, true); }

    public Kruskal(boolean pathCompression, boolean unionByRank) {
        this.pathCompression = pathCompression;
        this.unionByRank = unionByRank;
    }

    public MSTResult run(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST needs an undirected graph");
        int n = g.vertexCount();
        Edge[] sorted = g.edges().toArray(new Edge[0]);
        final long[] cmp = {0};
        Arrays.sort(sorted, (x, y) -> { cmp[0]++; return Integer.compare(x.weight(), y.weight()); });

        DSU dsu = new DSU(n, pathCompression, unionByRank);
        List<Edge> tree = new ArrayList<>();
        long total = 0;
        long examined = 0;
        for (Edge e : sorted) {
            if (n > 0 && tree.size() == n - 1) break;
            examined++;
            if (dsu.union(e.from(), e.to())) { tree.add(e); total += e.weight(); }
        }
        int components = dsu.componentCount();
        MSTResult r = new MSTResult("Kruskal", tree, total, components, components <= 1);
        r.ops.put("edgeExaminations", examined);
        r.ops.put("sortComparisons", cmp[0]);
        r.ops.put("dsuFind", dsu.findCalls);
        r.ops.put("dsuUnion", dsu.unionCalls);
        r.ops.put("dsuPathSteps", dsu.pathSteps);
        return r;
    }
}
