package app;

import connectivity.SCC;
import connectivity.SCCResult;
import connectivity.TopoResult;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.DijkstraResult;

import java.util.List;

public class Demo {
    static final String[] D = {"CityCentre", "Airport", "Harbor", "University", "TechPark", "OldTown", "Stadium", "Hospital"};

    public static void main(String[] args) {
        Graph roads = new Graph(8, false);
        int[][] r = {{0, 1, 12}, {0, 3, 4}, {0, 5, 3}, {0, 7, 6}, {1, 4, 9}, {1, 6, 14}, {2, 5, 8}, {2, 7, 11},
                {3, 4, 5}, {3, 5, 7}, {4, 6, 10}, {5, 7, 2}, {6, 7, 13}, {2, 3, 15}};
        for (int[] e : r) roads.addEdge(e[0], e[1], e[2]);

        System.out.println("=== Task 1: Minimum spanning tree (road construction costs) ===");
        for (MSTResult res : new MSTResult[]{new Kruskal().run(roads), new Prim().run(roads)}) {
            System.out.println(res.algorithm + ": total cost = " + res.totalWeight + ", connected = " + res.connected);
            for (Edge e : res.edges) System.out.println("   " + D[e.from()] + " - " + D[e.to()] + " (" + e.weight() + ")");
            System.out.println("   operations: " + res.ops);
        }

        Graph oneWay = new Graph(8, true);
        int[][] ow = {{0, 3}, {3, 4}, {4, 0}, {0, 5}, {5, 7}, {7, 5}, {1, 0}, {1, 4}, {6, 1}, {2, 7}, {6, 2}};
        for (int[] e : ow) oneWay.addEdge(e[0], e[1], 1);
        SCCResult scc = new SCC().run(oneWay);
        System.out.println("\n=== Task 2: Strongly connected components (one-way streets) ===");
        System.out.println(scc.count + " components:");
        for (List<Integer> comp : scc.components) {
            StringBuilder sb = new StringBuilder("   { ");
            for (int v : comp) sb.append(D[v]).append(' ');
            System.out.println(sb.append('}'));
        }

        Graph deps = new Graph(6, true);
        String[] svc = {"Power", "Water", "Network", "TrafficLights", "Sensors", "Dashboard"};
        int[][] dp = {{0, 2}, {0, 1}, {2, 4}, {2, 3}, {4, 5}, {3, 5}, {1, 4}};
        for (int[] e : dp) deps.addEdge(e[0], e[1], 1);
        TopoResult t = new TopologicalSort().run(deps);
        System.out.print("\nService start-up order: ");
        for (int v : t.order) System.out.print(svc[v] + " ");
        deps.addEdge(5, 0, 1);
        TopoResult tc = new TopologicalSort().run(deps);
        System.out.println("\nAfter adding Dashboard -> Power: cycle detected = " + tc.hasCycle + " (only " + tc.processed + " of 6 services orderable)");

        Graph km = new Graph(8, false);
        int[][] k = {{0, 1, 18}, {0, 3, 4}, {0, 5, 3}, {0, 7, 6}, {1, 4, 20}, {2, 5, 8}, {2, 7, 11}, {3, 4, 5}, {4, 6, 10}, {5, 7, 2}};
        for (int[] e : k) km.addEdge(e[0], e[1], e[2]);
        Graph withIsland = new Graph(9, false);
        for (Edge e : km.edges()) withIsland.addEdge(e.from(), e.to(), e.weight());
        DijkstraResult d = new Dijkstra().run(withIsland, 0);
        System.out.println("\n=== Task 3: Dijkstra from CityCentre (km) ===");
        for (int v = 0; v < 9; v++) {
            String name = v < 8 ? D[v] : "IsolatedIsland";
            if (!d.isReachable(v)) { System.out.println("   " + name + ": unreachable"); continue; }
            StringBuilder p = new StringBuilder();
            for (int x : d.pathTo(v)) p.append(p.length() == 0 ? "" : " -> ").append(D[x]);
            System.out.println("   " + name + ": " + d.dist[v] + " km via " + p);
        }
    }
}
