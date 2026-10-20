package graph;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private final int n;
    private final boolean directed;
    private final List<Edge>[] adj;
    private final List<Edge> edges = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        this.n = n;
        this.directed = directed;
        this.adj = new List[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
    }

    public void addEdge(int u, int v, int w) {
        checkVertex(u);
        checkVertex(v);
        Edge e = new Edge(u, v, w);
        edges.add(e);
        adj[u].add(e);
        if (!directed) adj[v].add(new Edge(v, u, w));
    }

    public void addEdge(int u, int v) {
        addEdge(u, v, 1);
    }

    private void checkVertex(int v) {
        if (v < 0 || v >= n) throw new IllegalArgumentException("vertex out of range: " + v);
    }

    public int vertexCount() { return n; }
    public int edgeCount() { return edges.size(); }
    public boolean isDirected() { return directed; }
    public List<Edge> neighbors(int u) { return adj[u]; }
    public List<Edge> edges() { return edges; }

    public boolean hasNegativeWeight() {
        for (Edge e : edges) if (e.weight() < 0) return true;
        return false;
    }
}
