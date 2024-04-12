import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

class A1_G3_t1 {
  public static void main(String[] args) {
    //make dataset, L1
    String csv_file = args[0];//csv file path
    float minsup = Float.parseFloat(args[1]);//minimum support

    String line = "";
    String splitter = ",";
    //***For ease, we convert item name strings into unique integer ids.***
    Map<String, Integer> grocs = new HashMap<>(); //name -> id mapping.
    List<String> id_to_grocs = new ArrayList<>(); //id -> name mapping
    List<Set<Integer>> dataset = new ArrayList<>(); //full dataset saving ids(will be sorted)
    Map<LinkedHashSet<Integer>, Integer> L1 = new HashMap<>();//full dataset saving ids(will be sorted soon)

    Map<Integer, Integer> C1_cnt = new HashMap<>();//C1 set.

    try (BufferedReader csvReader = new BufferedReader(new FileReader(csv_file))) {
        while((line = csvReader.readLine()) != null) {
            String[] trans_line = line.split(splitter);
            ArrayList<Integer> temp = new ArrayList<>();
            
            for(String item:trans_line) {
                if(!grocs.containsKey(item)){//if new item
                    grocs.put(item, grocs.size());//add new string <-> id mapping
                    id_to_grocs.add(item);
                    C1_cnt.put(grocs.get(item), 0);//add first count
                }
                
                int id = grocs.get(item);
                temp.add(id);
                C1_cnt.put(id, C1_cnt.get(id) + 1);//update the counts
                
            }
            temp.sort(null);
            dataset.add(new LinkedHashSet<>(temp));

        }
    } catch (IOException e) {
        e.printStackTrace();
    }

    for(int i=0;i<grocs.size();i++){
        
        if(C1_cnt.get(i)*1.0/dataset.size() >= minsup){//filter out the non-frequent items
            L1.put(new LinkedHashSet<>(Set.of(i)), C1_cnt.get(i));
        }
    }

    List<Map<LinkedHashSet<Integer>, Integer>> Cs = new ArrayList<>(); //C1, C2, ...
    List<Map<LinkedHashSet<Integer>, Integer>> Ls = new ArrayList<>(); //L1, L2, ...
    Cs.add(null);//placeholder for C0
    Cs.add(null);//placeholder for C1
    Ls.add(null);//placeholder for L0
    Ls.add(L1);//algorithm starts from L1


    for(int k=2; !Ls.get(Ls.size()-1).isEmpty(); k++)//Make C2, L2, C3, L3, ...
    {
        List<LinkedHashSet<Integer>> L_info = List.copyOf(Ls.get(k-1).keySet());
        Cs.add(apriori_gen(L_info, k));//add all candidates, C_k list made.
        for(Map.Entry<LinkedHashSet<Integer>, Integer> itemset: Cs.get(Cs.size()-1).entrySet()) {//for all candidates in C
            for(Set<Integer> trans: dataset) {//for all transactions
                if(trans.containsAll(itemset.getKey())){//find and add count for candidates in the transactions
                    Cs.get(Cs.size()-1).put(itemset.getKey(), itemset.getValue() + 1);//set the itemset support info
                }
            }
        }

        Map<LinkedHashSet<Integer>, Integer> L = new HashMap<>();
        for(Map.Entry<LinkedHashSet<Integer>, Integer> itemset: Cs.get(Cs.size()-1).entrySet()) {
            if(itemset.getValue()*1.0/dataset.size() >= minsup){//filter out all non-frequent itemsets
                L.put(itemset.getKey(), itemset.getValue());//L_k list made.
            }
        }
        Ls.add(L);

    }

    //Combine all L_k to sort and print
    List<Map.Entry<LinkedHashSet<Integer>, Integer>> final_Ls = new ArrayList<>();
    Ls.remove(0);//remove L0 placeholder
    for(var itemset:Ls){
        final_Ls.addAll(itemset.entrySet());
    }
    final_Ls.sort(Comparator.comparing(Map.Entry::getValue));//sort ascending order of frequency.

    //Pring all frequent itemsets and its support.
    for(var itemset:final_Ls){
        String items = String.join(",", itemset.getKey().stream()
                                                        .map((i) -> id_to_grocs.get(i))
                                                        .collect(Collectors.toList()));
        System.out.println(String.format("%s %f", items, itemset.getValue()*1.0/dataset.size()));
        
    }
  }



  public static Map<LinkedHashSet<Integer>, Integer> apriori_gen(List<LinkedHashSet<Integer>> L, int k){//the apriori-gen function. Uses L_{k-1}, k.
    Map<LinkedHashSet<Integer>, Integer> C = new HashMap<>(); //will fill and return this
    for(var a:L){
        for(var b:L){
            boolean c = true;
            Iterator<Integer> a_items = a.iterator();
            Iterator<Integer> b_items = b.iterator();
            // System.out.println(b.size());
            // System.out.println(k);
            for(int i=0;i<k-2;i++){//check if the first k-2 items are same in two {k-1}-itemsets.
                if(a_items.next()!=b_items.next()){
                    c = false;
                    break;
                }
            }
            if(!c){
                continue;//if not, search for another pairs
            }
            int b_last = b_items.next();
            if(a_items.next()>=b_last){ //should be a_last < b_last.
                continue;
            }
            //Now all conditions are matched.
            //self-join
            LinkedHashSet<Integer> new_itemset = new LinkedHashSet<>(a);
            new_itemset.add(b_last);//just adding b_last to a is enough.

            //now for new itemset, check if all {k-1}-subsets of it exists in L_{k-1}.
            for(var rm:new_itemset){
                LinkedHashSet<Integer> sub_itemset = new LinkedHashSet<>(new_itemset);
                sub_itemset.remove(rm);//test all subsets by removing one item
                boolean contains = false;
                for(var cand:L){
                    if(cand.containsAll(sub_itemset)){
                        contains = true;
                        break;
                    }
                }
                if(contains){//this subset is in L_{k-1}. pass
                    C.put(new_itemset, 0);
                }

            }
            
        }
    }


    return C;//return C_k for making L_k.
  }
}
