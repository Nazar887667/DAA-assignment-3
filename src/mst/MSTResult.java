package mst;

import graph.Edge;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MSTResult {
    public final String algorithm;
    public final List<Edge> edges;
    public final long totalWeight;
    public final int components;
    public final boolean connected;
    public final Map<String, Long> ops = new LinkedHashMap<>();

    public MSTResult(String algorithm, List<Edge> edges, long totalWeight, int components, boolean connected) {
        this.algorithm = algorithm;
        this.edges = edges;
        this.totalWeight = totalWeight;
        this.components = components;
        this.connected = connected;
    }
}
