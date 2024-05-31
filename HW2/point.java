public class Point {
    private String name;
    private double x;
    private double y;
    private int clusterAnswer;
    private int cluster;
    private boolean visited;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getClusterAnswer() {
        return clusterAnswer;
    }

    public void setClusterAnswer(int clusterAnswer) {
        this.clusterAnswer = clusterAnswer;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }

    public boolean isVisited() {
    return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }
}