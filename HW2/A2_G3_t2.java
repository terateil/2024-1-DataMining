public class A2_G3_t2 {
    public static void main(String[] args) {
        // check the number of parameters
        if (args.length < 1 || args.length > 3) {
            System.out.println("Usage: java Main <csv_file> [<eps> <min_pts>]");
            return;
        }

        String csvFile = args[0];
        double eps = -1;
        int minPts = -1;

        if (args.length >= 2) {
            // if given parameter is int
            if (Double.parseDouble(args[1]) - (int)Double.parseDouble(args[1]) == 0) {
                minPts = Integer.parseInt(args[1]);
                if (args.length == 3) {
                    eps = Double.parseDouble(args[2]);
                }
            }
            // if given parameter is double
            else {
                eps = Double.parseDouble(args[1]);
                if (args.length == 3) {
                    minPts = Integer.parseInt(args[2]);
                }
            }
        }

        // perform DBSCAN and print
        DBSCAN dbscan = new DBSCAN(csvFile, eps, minPts);
        dbscan.run();
    }
}