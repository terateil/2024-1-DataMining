import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

public class DBSCAN {
    private List<Point> points;
    private int numClusters;
    private List<Point> noisePoints;
    private List<List<Point>> clusters;
    private double eps;
    private int minPts;
    private double initialEps;
    private int initialMinPts;

    public DBSCAN(String csvFile, double eps, int minPts) {
        // Read CSV file and initialize data point
        points = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                Point point = new Point();
                point.setName(values[0]);
                point.setX(Double.parseDouble(values[1]));
                point.setY(Double.parseDouble(values[2]));
                point.setClusterAnswer((int)Double.parseDouble(values[3]));
                point.setVisited(false);
                point.setCluster(-1);
                points.add(point);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + csvFile);
            return;
        }

        // perform DBSCAN 
        if (eps > 0 && minPts > 0) {
            performDBSCAN(eps, minPts);
        } else if (eps > 0) {
            minPts = findOptimalMinPts(eps);
            performDBSCAN(eps, minPts);
        } else if (minPts > 0) {
            eps = findOptimalEps(minPts);
            performDBSCAN(eps, minPts);
        } else {
            System.out.println("Invalid input parameters.");
            return;
        }
    }

    public void run() {
        printClusterResults();
    }

    // calculate euclidian distance
    private double calculateDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    private void performDBSCAN(double eps, int minPts) {
        numClusters = 0;
        clusters = new ArrayList<>();
        noisePoints = new ArrayList<>();

        // initialize point
        for (Point point : points) {
            point.setVisited(false);
            point.setCluster(-1);
        }

        for (Point point : points) {
            if (!point.isVisited()) {
                point.setVisited(true);
                List<Point> cluster;
                if (!isCorePoint(point, eps, minPts)) {
                    noisePoints.add(point);
                } else {
                    numClusters++;
                    cluster = expandCluster(point, numClusters, eps, minPts);
                    clusters.add(cluster);
                }
            }
        }

        this.eps = eps;
        this.minPts = minPts;
    }

    private List<Point> findNeighbors(Point point, double eps) {
        List<Point> neighbors = new ArrayList<>();
        for (Point p : points) {
            if (calculateDistance(point, p) <= eps && !point.getName().equals(p.getName())) {
                neighbors.add(p);
            }
        }
        return neighbors;
    }

    private boolean isCorePoint(Point point, double eps, int minPts) {
        List<Point> neighbors = findNeighbors(point, eps);
        int numOfNeighbors = neighbors.size();
        for (Point neighbor : neighbors) {
            if (neighbor.isVisited()) {
                numOfNeighbors--;
            }
        }
        return numOfNeighbors >= minPts;
    }

    private List<Point> expandCluster(Point point, int clusterId, double eps, int minPts) {
        point.setCluster(clusterId);
        List<Point> cluster = new ArrayList<>();
        cluster.add(point);

        List<Point> neighbors = findNeighbors(point, eps);
        for (Point neighbor : neighbors) {
            // if neighbor is already visited, it means that it is already in cluster or noise
            if (!neighbor.isVisited()) {
                neighbor.setVisited(true);

                // if neighbor of core point is also core point, 
                // execuse expandCluster() for that neighbor and will get same clusterId
                if (isCorePoint(neighbor, eps, minPts)) {
                    List<Point> expandedCluster = expandCluster(neighbor, clusterId, eps, minPts);
                    for (Point expandedPoint : expandedCluster) {
                        expandedPoint.setCluster(clusterId);
                        cluster.add(expandedPoint);
                    }
                } else {
                    // if neighbor is not core point, add into cluster
                    neighbor.setCluster(clusterId);
                    cluster.add(neighbor);
                }
            }
        }
        return cluster;
    }

    // print the clustered result
    private void printClusterResults() {
        System.out.println("Number of clusters : " + numClusters);
        System.out.println("Number of noise : " + noisePoints.size());

        for (int i = 0; i < numClusters; i++) {
            System.out.print("Cluster #" + (i + 1) + " => ");
            // sort the name of points
            Collections.sort(clusters.get(i), (p1, p2) -> {
                String num1 = p1.getName().substring(1);
                String num2 = p2.getName().substring(1);
                if (num1.length() == num2.length()) {
                    return Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
                }
                else {
                    return Integer.compare(num1.length(), num2.length());
                }
            });
            for (Point point : clusters.get(i)) {
                System.out.print(point.getName() + " ");
            }
            System.out.println();
        }
    }

    private double findOptimalEps(int minPts) {
        // calculate the distance between each point to minpts-th close point
        List<Double> minptsDistances = new ArrayList<>();
        for (Point point : points) {
            List<Double> pointDistances = new ArrayList<>();
            for (Point otherPoint : points) {
                if (!point.getName().equals(otherPoint.getName())) {
                    double dist = calculateDistance(point, otherPoint);
                    pointDistances.add(dist);
                }
            }
            Collections.sort(pointDistances);
            if (pointDistances.size() >= minPts) {
                minptsDistances.add(pointDistances.get(minPts - 1));
            }
        }

        // sort the distance values
        Collections.sort(minptsDistances);

        // apply elbow method to find optimal eps
        double x1 = 0, y1 = minptsDistances.get(0);
        double x2 = minptsDistances.size() - 1, y2 = minptsDistances.get(minptsDistances.size() - 1);

        // apply kneedle argorithm to find elbow point
        double maxDist = 0;
        int maxDistX = 0;
        for (int i = 0; i < minptsDistances.size(); i++) {
            double x = i, y = minptsDistances.get(i);
            double dist = Math.abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1)
                    / Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            if (dist > maxDist) {
                maxDist = dist;
                maxDistX = i;
            }
        }

        double maxSilhouette = 0, maxDunnIndex = 0;
        int maxSilhouetteX = 0, maxDunnIndexX = 0;
        boolean useSilhouette = true; // if true, apply silhouette
        boolean useDunnIndex = false; // if true, apply dunn index

        // pick 10 points that is up down for 2% 4% 6% 8% 10% of dataset size (based on elbow point)
        for (int j = (int) (maxDistX - minptsDistances.size() * 5 * 0.02);
                j < maxDistX + minptsDistances.size() * 5 * 0.02;
                j += minptsDistances.size() * 0.02) {
            // if out of range, then continue
            if (j < 0 || j >= minptsDistances.size()) {
                continue;
            }

            // apply silhouette to find optimal eps
            if (useSilhouette) {
                performDBSCAN(minptsDistances.get(j), minPts);
                double sil = calculateSilhouette();
                if (sil > maxSilhouette && sil != 1) {
                    maxSilhouette = sil;
                    maxSilhouetteX = j;
                }
            }

            // apply dunn index to find optimal eps
            if (useDunnIndex) {
                double dunnIndex = calculateDunnIndex();
                if (dunnIndex > maxDunnIndex) {
                    maxDunnIndex = dunnIndex;
                    maxDunnIndexX = j;
                }
            }
        }

        System.out.println("Estimated eps : " + minptsDistances.get(maxSilhouetteX));

        if (useSilhouette) {
            return minptsDistances.get(maxSilhouetteX);
        } else if (useDunnIndex) {
            return minptsDistances.get(maxDunnIndexX);
        } else {
            return minptsDistances.get(maxDistX);
        }
    }

    private int findOptimalMinPts(double eps) {
        double maxSilhouette = 0, maxDunnIndex = 0;
        int maxSilhouetteMinpts = 0, maxDunnIndexMinpts = 0;
        boolean useSilhouette = true; // if true, apply silhouette
        boolean useDunnIndex = false; // if true, apply dunn index
        if (useSilhouette) {
            for (int i = 3; i < 11; i++) {
                performDBSCAN(eps, i);
                double sil = calculateSilhouette();
                if (sil > maxSilhouette && sil != 1) {
                    maxSilhouette = sil;
                    maxSilhouetteMinpts = i;
                }
            }
            System.out.println("Estimated Minpts : " + maxSilhouetteMinpts);
            return maxSilhouetteMinpts;
        }
        if (useDunnIndex) {
            for (int i = 3; i < 11; i++) {
                performDBSCAN(eps, i);
                double dunnIndex = calculateDunnIndex();
                if (dunnIndex > maxDunnIndex) {
                    maxDunnIndex = dunnIndex;
                    maxDunnIndexMinpts = i;
                }
            }
            System.out.println("Estimated Minpts : " + maxDunnIndexMinpts);
            return maxDunnIndexMinpts;
        }
        return 4;
    }

    private double calculateSilhouette() {
        double silhouetteCoef = 0.0;
        int numPoints = points.size();

        // ignore noize points
        for (Point point : points) {
            if (point.getCluster() == -1) {
                continue;
            }

            double a = 0.0; // mean of distance between other points in same cluster
            int numSameCluster = 0;

            for (Point otherPoint : points) {
                if (otherPoint.getCluster() == point.getCluster()
                        && !point.getName().equals(otherPoint.getName())) {
                    a += calculateDistance(point, otherPoint);
                    numSameCluster++;
                }
            }

            if (numSameCluster > 0) {
                a /= numSameCluster;
            }

            double b = Double.POSITIVE_INFINITY; // min mean distance between other clusters

            for (int i = 0; i < numClusters; i++) {
                if (i == point.getCluster()) {
                    continue;
                }

                double avgDist = 0.0;
                int numOtherCluster = 0;

                for (Point otherPoint : points) {
                    if (otherPoint.getCluster() == i) {
                        avgDist += calculateDistance(point, otherPoint);
                        numOtherCluster++;
                    }
                }

                if (numOtherCluster > 0) {
                    avgDist /= numOtherCluster;
                    b = Math.min(b, avgDist);
                }
            }

            double silhouette = (b - a) / Math.max(a, b);
            silhouetteCoef += silhouette;
        }

        silhouetteCoef /= (numPoints - noisePoints.size());
        return silhouetteCoef;
    }

    private double calculateDunnIndex() {
        double minInterClusterDist = Double.POSITIVE_INFINITY;
        double maxIntraClusterDist = Double.NEGATIVE_INFINITY;

        // calculate min distance between other clusters
        for (int i = 0; i < numClusters; i++) {
            for (int j = i + 1; j < numClusters; j++) {
                double minDist = Double.POSITIVE_INFINITY;
                for (Point point1 : clusters.get(i)) {
                    for (Point point2 : clusters.get(j)) {
                        double dist = calculateDistance(point1, point2);
                        minDist = Math.min(minDist, dist);
                    }
                }
                minInterClusterDist = Math.min(minInterClusterDist, minDist);
            }
        }

        // calculate max distance in same cluster
        for (List<Point> cluster : clusters) {
            double maxDist = Double.NEGATIVE_INFINITY;
            for (Point point1 : cluster) {
                for (Point point2 : cluster) {
                    if (point1 != point2) {
                        double dist = calculateDistance(point1, point2);
                        maxDist = Math.max(maxDist, dist);
                    }
                }
            }
            maxIntraClusterDist = Math.max(maxIntraClusterDist, maxDist);
        }

        // calculate Dunn Index
        double dunnIndex = minInterClusterDist / maxIntraClusterDist;
        return dunnIndex;
    }
}