package graph;

public record Edge(int from, int to, int weight) {
    @Override
    public String toString() {
        return from + "-" + to + "(" + weight + ")";
    }
}
