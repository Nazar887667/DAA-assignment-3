package connectivity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SCCResult {
    public final int[] componentOf;
    public final int count;
    public final List<List<Integer>> components;
    public final Map<String, Long> ops = new LinkedHashMap<>();

    public SCCResult(int[] componentOf, int count, List<List<Integer>> components) {
        this.componentOf = componentOf;
        this.count = count;
        this.components = components;
    }

    public int largestSize() {
        int best = 0;
        for (List<Integer> c : components) best = Math.max(best, c.size());
        return best;
    }
}
