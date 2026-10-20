package connectivity;

import java.util.LinkedHashMap;
import java.util.Map;

public class TopoResult {
    public final int[] order;
    public final boolean hasCycle;
    public final int processed;
    public final Map<String, Long> ops = new LinkedHashMap<>();

    public TopoResult(int[] order, boolean hasCycle, int processed) {
        this.order = order;
        this.hasCycle = hasCycle;
        this.processed = processed;
    }
}
