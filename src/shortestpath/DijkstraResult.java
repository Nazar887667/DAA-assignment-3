package shortestpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DijkstraResult {
    public static final long INF = Long.MAX_VALUE;
    public final int source;
    public final long[] dist;
    public final int[] parent;
    public final Map<String, Long> ops = new LinkedHashMap<>();

    public DijkstraResult(int source, long[] dist, int[] parent) {
        this.source = source;
        this.dist = dist;
        this.parent = parent;
    }

    public boolean isReachable(int v) { return dist[v] != INF; }

    public int reachableCount() {
        int c = 0;
        for (long d : dist) if (d != INF) c++;
        return c;
    }

    public List<Integer> pathTo(int target) {
        List<Integer> path = new ArrayList<>();
        if (!isReachable(target)) return path;
        for (int v = target; v != -1; v = parent[v]) path.add(v);
        Collections.reverse(path);
        return path;
    }
}
