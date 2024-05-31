import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.lang.Math;

class Point {
    int id;
    List<Float> coord;
    int cluster = -1;
    public Point(int _id, List<Float> _coord){
        this.id = _id;
        this.coord = _coord;
    }
    public Point copy(){
        return new Point(-1, this.coord);
    }
    public float square_dist(Point a){
        float sd = 0;
        for(int i=0;i<coord.size();i++){
            sd += (this.coord.get(i)-a.coord.get(i))*(this.coord.get(i)-a.coord.get(i));
        }
        return sd;
    }
    public float dist(Point a){
        return (float)Math.sqrt(square_dist(a));
    }
    public void move(List<Float> _coord){
        this.coord = _coord;
    }
}


class A2_G3_t1 {
  static final int MAX_K = 40;
  static final int MIN_K = 2;
  static final int MAX_IT = 100;
  static int METHOD = 1;//0 = elbow, 1 = shiluette
  public static void main(String[] args) {
    String csvFile = args[0];
    String line;
    String csvSeparator = ",";

    List<Point> points = new ArrayList<>();
    Random random = new Random();
    float[] inertia = new float[MAX_K+1];
    float[] silh = new float[MAX_K+1];

    List<Point>[][] clusters = new List[MAX_K+1][MAX_K+1];
    for(int i=0;i<MAX_K+1;i++){
        for(int j=0;j<MAX_K+1;j++){
            clusters[i][j] = new ArrayList<>();
        }
    }

    int opt_k = MIN_K;

    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
        while ((line = br.readLine()) != null) {
            String[] values = line.split(csvSeparator);
            List<Float> coords = new ArrayList<>();
            for(int i=0;i<values.length-2;i++){
                coords.add(Float.valueOf(values[i+1]));
            }
            points.add(new Point(Integer.valueOf(values[0].substring(1)), coords));
        }
    } catch (IOException e) {
        e.printStackTrace();
    }

    if(args.length == 2){
        opt_k = Integer.valueOf(args[1]);
    }


    int min_k = MIN_K;
    int max_k = MAX_K;

    if(args.length == 2){
        min_k = opt_k;
        max_k = opt_k;
    }
    
    for(int k=min_k; k<=max_k; k++){
        //Step 1. Set k centroids

        List<Point> centroids = new ArrayList<>();
        //Initial centroid, pick from all points randomly
        centroids.add(points.get(random.nextInt(points.size())).copy());
        for(int c=1; c<k; c++){
            List<Float> cumulative_prob_dist = new ArrayList<>();
            float sum_md = 0;
            //make prob dists based on min squared distance from centroids
            for(int i=0; i<points.size(); i++){
                Point p = points.get(i);
                float square_md = centroids.stream().map(cent -> cent.square_dist(p)).min(Float::compareTo).orElseThrow();
                sum_md += square_md;
                cumulative_prob_dist.add(sum_md);
            }
            final float sum_md_cp = sum_md;
            cumulative_prob_dist.replaceAll(f -> f/sum_md_cp);//normalize to ensure the sum is 1
            float rand_float = random.nextFloat(); //random number 0~1
            for(int i=0; i<cumulative_prob_dist.size(); i++){
                if(rand_float <= cumulative_prob_dist.get(i)){
                    centroids.add(points.get(i).copy()); //This point is new centroid.
                    break;
                }
            }
        }
        
        //Step 2. Do k-means
        for(int s=0; s<MAX_IT; s++){
            for(int j=0;j<MAX_K+1;j++){
                clusters[k][j] = new ArrayList<>();
            }
            for(int i=0;i<points.size();i++){ //Assign clusters based on current centroids
                Point p = points.get(i);
                int cluster_id = IntStream.range(0, k).boxed().collect(Collectors.minBy((a, b) -> Float.compare(centroids.get(a).square_dist(p), centroids.get(b).square_dist(p)))).orElseThrow();
                clusters[k][cluster_id].add(p);
                p.cluster = cluster_id;
            }
            for(int i=0;i<k;i++){ //update centroids based on updated clusters right before
                int dim = clusters[k][i].get(0).coord.size();
                List<Float> new_coord = new ArrayList<>();
                for(int j=0;j<dim;j++){
                    final int j_cp = j;
                    new_coord.add((float)clusters[k][i].stream().map(p -> p.coord.get(j_cp)).mapToDouble(Float::doubleValue).average().orElseThrow());
                }
                centroids.get(i).move(new_coord);
            }
        }

        //Step 3. Calculate the values for methods to determine k
        if(METHOD == 0){
            for(int i=0;i<k;i++){
                Point cent = centroids.get(i);
                inertia[k] += (float)clusters[k][i].stream().map(p -> p.square_dist(cent)).mapToDouble(Float::doubleValue).sum();
            }
        }
        else if(METHOD == 1){
            float sum_silh = 0;

            for(int i = 0; i<points.size(); i++){
                Point p = points.get(i);
                float[] sum_dist = new float[k];
                int[] num_dist = new int[k];
                for(int j=0;j<points.size();j++){
                    Point pj = points.get(j);
                    if(i==j){
                        continue;
                    }
                    num_dist[pj.cluster] += 1;
                    sum_dist[pj.cluster] = ((sum_dist[pj.cluster] / num_dist[pj.cluster]) * (num_dist[pj.cluster] - 1)) + (p.dist(pj)/num_dist[pj.cluster]); //update as mean value
                }
                float a = 0;
                float b = Float.MAX_VALUE;
                for(int j=0;j<k;j++){
                    if(j==p.cluster){
                        a = sum_dist[j];
                    }
                    else{
                        if(sum_dist[j]<b){
                            b = sum_dist[j];
                        }
                    }
                }
                sum_silh += ((b - a) / Math.max(a, b));
            }

            sum_silh /= points.size();
            silh[k] = sum_silh;
        }
        
        

    }
    //Step 4. method to find optimal k
    if(args.length == 1){
        if(METHOD == 0){
            float a = inertia[MAX_K] - inertia[MIN_K];
            float b = -(MAX_K - MIN_K);
            float c = MAX_K*inertia[MIN_K] - MIN_K*inertia[MAX_K];

            float max_dist = 0;

            for(int k = MIN_K; k <= MAX_K; k++){
                float dist = Math.abs(a*k + b*inertia[k] + c)/(float)Math.sqrt(a*a + b*b);
                if(dist>max_dist){
                    max_dist = dist;
                    opt_k = k;
                }
            }

        }
       
        else if(METHOD == 1){
            float max_silh = silh[MIN_K];
            for(int k = MIN_K; k <= MAX_K; k++){
                if(silh[k]>max_silh){
                    max_silh = silh[k];
                    opt_k = k;
                }
            }
        }

        System.out.println(String.format("estimated k: %d", opt_k));
        
    }

    //Step 5. Print results
    
    for(int i=0;i<opt_k;i++){
        System.out.print(String.format("Cluster #%d =>",i+1));
        for(int p=0;p<clusters[opt_k][i].size();p++){
            System.out.print(String.format(" p%d", clusters[opt_k][i].get(p).id));
        }
        System.out.println("");

    }

    

  }
}
