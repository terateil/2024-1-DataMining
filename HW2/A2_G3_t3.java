import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class A2_G3_t3 {


    public static class Cluster {
        List<double[]> points;
        double[] centroid;
        List<Integer> ids;
        int size;

        public Cluster(){
            points = new ArrayList<>();
            ids = new ArrayList<>();
        }


        public Cluster(double[] point, int id) {
            points = new ArrayList<>();
            points.add(point);
            centroid = point.clone();
            ids = new ArrayList<>();
            ids.add(id);
            size = 1;
        }

        public void merge(Cluster other) {
            points.addAll(other.points);
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] = (centroid[i] * size + other.centroid[i] * other.size) / (size + other.size);
            }
            size += other.size;
            ids.addAll(other.ids);
        }


        public static List<Cluster> deepCopyClusters(List<Cluster> original) {
            List<Cluster> copy = new ArrayList<>();

            for (Cluster cluster : original) {
                Cluster clusterCopy = new Cluster();
                clusterCopy.ids.addAll(cluster.ids);
                for (double[] point : cluster.points) {
                    clusterCopy.points.add(point.clone());
                }
                clusterCopy.centroid = cluster.centroid.clone();
                clusterCopy.size = cluster.size;
                copy.add(clusterCopy);
            }

            return copy;
        }
    }

    public static List<Cluster> centroidBasedHierarchicalCluster(List<double[]> data, List<Integer> ids) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            clusters.add(new Cluster(data.get(i), ids.get(i)));
        }

        int min_k = 2;
        int max_k = 40;

        double max_score = -1;
        List<Cluster> best_clusters = new ArrayList<>();

        // Perform centroid-based hierarchical clustering
        while (clusters.size() >= min_k) {
            
            //find optimal k based on silhouette score
            if(clusters.size() <= max_k){
                double k_score = calculateSilhouetteScore(clusters, data);
                if(k_score > max_score){
                    best_clusters = Cluster.deepCopyClusters(clusters);
                    max_score = k_score;
                }

            }

            double minDistance = Double.MAX_VALUE;
            int mergeIndex1 = -1;
            int mergeIndex2 = -1;

            // Find the pair of clusters with the closest centroids
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double distance = euclideanDistance(clusters.get(i).centroid, clusters.get(j).centroid);
                    if (distance < minDistance) {
                        minDistance = distance;
                        mergeIndex1 = i;
                        mergeIndex2 = j;
                    }
                }
            }

            // Merge the closest pair of clusters
            clusters.get(mergeIndex1).merge(clusters.get(mergeIndex2));
            clusters.remove(mergeIndex2);


        }

        return best_clusters;
    }

    public static double calculateSilhouetteScore(List<Cluster> clusters, List<double[]> data) {
        double totalSilhouetteScore = 0.0;
        int numPoints = 0;

        for (Cluster cluster : clusters) {
            for (double[] point : cluster.points) {
                double a = calculateAverageDistance(point, cluster.points, 0);
                double b = Double.MAX_VALUE;

                for (Cluster otherCluster : clusters) {
                    if (otherCluster != cluster) {
                        double distance = calculateAverageDistance(point, otherCluster.points, 1);
                        if (distance < b) {
                            b = distance;
                        }
                    }
                }

                double silhouetteScore = (b - a) / Math.max(a, b);
                if(cluster.points.size()==1){
                    silhouetteScore = 0;
                }
                totalSilhouetteScore += silhouetteScore;
                numPoints++;
            }
        }

        return totalSilhouetteScore / numPoints;
    }

    private static double calculateAverageDistance(double[] point, List<double[]> points, int inclusion) {
        double totalDistance = 0.0;
        for (double[] otherPoint : points) {
            totalDistance += (euclideanDistance(point, otherPoint) / (points.size() - inclusion));
        }
        return totalDistance;
    }
    

    public static double euclideanDistance(double[] point1, double[] point2) {
        double sum = 0.0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.pow(point1[i] - point2[i], 2);
        }
        return Math.sqrt(sum);
    }


    public static void main(String[] args) {

    List<Integer> ids = new ArrayList<>();
    List<double[]> data = new ArrayList<>();

    String csvFile = args[0];
    String[] parts = csvFile.split("/");
    String csvName = parts[parts.length-1].substring(0, parts[parts.length-1].length()-4);//without .csv
    String line;
    String csvSeparator = ",";

    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
        while ((line = br.readLine()) != null) {
            String[] values = line.split(csvSeparator);
            List<Double> coords = new ArrayList<>();
            for(int i=0;i<values.length-2;i++){
                coords.add(Double.valueOf(values[i+1]));
            }
            data.add(coords.stream().mapToDouble(Double::doubleValue).toArray());
            ids.add(Integer.valueOf(values[0].substring(1)));
        }
    } catch (IOException e) {
        e.printStackTrace();
    }

    // long startTime = System.currentTimeMillis();


    // Perform hierarchical clustering
    List<Cluster> clusters = centroidBasedHierarchicalCluster(data, ids);

    // End measuring time
    // long endTime = System.currentTimeMillis();

    // long runtime = endTime - startTime;
    // System.out.println("Runtime: " + runtime + " milliseconds");

    // Perform hierarchical clustering


    
    for(int i=0;i<clusters.size();i++){
        System.out.print(String.format("Cluster #%d =>",i+1));
        for(int p=0;p<clusters.get(i).ids.size();p++){
            System.out.print(String.format(" p%d ", clusters.get(i).ids.get(p)));
        }
        System.out.println("");
    }
    // Confirm the write operation
}
}
