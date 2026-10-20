package dsu;

public class DSU {
    private final int[] parent;
    private final int[] rank;
    private int components;
    private final boolean pathCompression;
    private final boolean unionByRank;

    public long findCalls = 0;
    public long unionCalls = 0;
    public long successfulUnions = 0;
    public long pathSteps = 0;

    public DSU(int n) { this(n, true, true); }

    public DSU(int n, boolean pathCompression, boolean unionByRank) {
        this.parent = new int[n];
        this.rank = new int[n];
        this.components = n;
        this.pathCompression = pathCompression;
        this.unionByRank = unionByRank;
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    public int find(int x) {
        findCalls++;
        int root = x;
        while (parent[root] != root) { root = parent[root]; pathSteps++; }
        if (pathCompression) {
            while (parent[x] != root) { int next = parent[x]; parent[x] = root; x = next; }
        }
        return root;
    }

    public boolean union(int a, int b) {
        unionCalls++;
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (unionByRank) {
            if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            if (rank[ra] == rank[rb]) rank[ra]++;
        } else {
            parent[rb] = ra;
        }
        components--;
        successfulUnions++;
        return true;
    }

    public boolean connected(int a, int b) { return find(a) == find(b); }
    public int componentCount() { return components; }
    public int size() { return parent.length; }

    public void resetCounters() { findCalls = unionCalls = successfulUnions = pathSteps = 0; }
}
