package connectivity;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;

public class SCC {
    public SCCResult run(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("SCC needs a directed graph");
        int n = g.vertexCount();
        int[] index = new int[n], low = new int[n], comp = new int[n], edgePtr = new int[n];
        boolean[] onStack = new boolean[n];
        java.util.Arrays.fill(index, -1);
        java.util.Arrays.fill(comp, -1);
        int[] sccStack = new int[n], callStack = new int[n];
        int sccTop = 0, callTop = 0, counter = 0, compCount = 0;
        long examined = 0;
        List<List<Integer>> components = new ArrayList<>();

        for (int s = 0; s < n; s++) {
            if (index[s] != -1) continue;
            index[s] = low[s] = counter++;
            sccStack[sccTop++] = s; onStack[s] = true;
            callStack[callTop++] = s;
            while (callTop > 0) {
                int v = callStack[callTop - 1];
                List<Edge> out = g.neighbors(v);
                if (edgePtr[v] < out.size()) {
                    int w = out.get(edgePtr[v]++).to();
                    examined++;
                    if (index[w] == -1) {
                        index[w] = low[w] = counter++;
                        sccStack[sccTop++] = w; onStack[w] = true;
                        callStack[callTop++] = w;
                    } else if (onStack[w]) {
                        low[v] = Math.min(low[v], index[w]);
                    }
                } else {
                    if (low[v] == index[v]) {
                        List<Integer> members = new ArrayList<>();
                        int w;
                        do {
                            w = sccStack[--sccTop];
                            onStack[w] = false;
                            comp[w] = compCount;
                            members.add(w);
                        } while (w != v);
                        components.add(members);
                        compCount++;
                    }
                    callTop--;
                    if (callTop > 0) {
                        int parent = callStack[callTop - 1];
                        low[parent] = Math.min(low[parent], low[v]);
                    }
                }
            }
        }
        SCCResult r = new SCCResult(comp, compCount, components);
        r.ops.put("edgeExaminations", examined);
        r.ops.put("verticesVisited", (long) counter);
        return r;
    }
}
