import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class A1_G3_t2 { //This is the main class of FP Growth algorithm.
    static class Node { //This is the class of node in the FP tree.
        int id = -1; //unique id for the item. -1 indicates the root node.
        int cnt = 0; //This is the count of this item node in the tree branch.
        Node parent = null;//parent node
        List<Node> children = new ArrayList<>();//children nodes

        public Node(int _id, int _cnt, Node par) {
            this.id = _id;
            this.cnt = _cnt;
            this.parent = par;
        }

        public void addcnt(int c){ //use this function when constructing fp tree
            this.cnt += c;
        }
    }

    static class Tree {
        Node root = new Node(-1, 0, null);
        Map<Integer, List<Node>> nodemap = new HashMap<>(); //save all nodes for quick tracking. Access all nodes that have certain ID
        public Tree(List<Integer> flist){ //Ready to construct FP tree using F-list.
            for(var id:flist){
                nodemap.put(id, new ArrayList<>());
            }
        }
    
        public void insert(List<Integer> ps, int base_cnt){ //insert one branch(transaction)
            Node par = root;
            for(Integer p: ps){
                Optional<Node> c = par.children.stream().filter(n -> (n.id == p)).findFirst(); //search if the item to insert is already in the next depth of tree
                if(c.isEmpty()){//if not, we need new branch
                    Node new_node = new Node(p, base_cnt, par);
                    par.children.add(new_node);
                    par = new_node;
                    nodemap.get(p).add(new_node);
                }
                else{//if exists, just add the item count
                    c.get().addcnt(base_cnt);
                    par = c.get();
                }
            }
        }
    
        public Map<List<Integer>, Integer> make_pattern_base(int id){ //This method returns the conditional pattern base of certain item, in this tree class. Map of Conditional pattern base and the support.
            Map<List<Integer>, Integer> pattern_bases = new HashMap<>();
            
            List<Node> target_nodes = nodemap.get(id);//find all id nodes
            for(Node t: target_nodes){
                Node cur = t;
                List<Integer> base = new ArrayList<>();
                while(cur.id != -1){ //climb up the tree
                    base.add(cur.id);
                    cur = cur.parent;
                }
                Collections.reverse(base);
                pattern_bases.put(base, t.cnt);//since we climb up the tree, starting node t has the smallest support in the branch.
            }
            return pattern_bases;
        }
    }
  public static void main(String[] args) {
    //DB making & scanning, for finding frequent 1-itemset
    String csv_file = args[0];//data file name
    float minsup = Float.parseFloat(args[1]);//minimum support

    String line = "";
    String splitter = ",";//since csv file
    //***For ease, we convert item name strings into unique integer ids.***
    Map<String, Integer> grocs = new HashMap<>(); //name -> id mapping.
    List<String> id_to_grocs = new ArrayList<>(); //id -> name mapping
    List<Set<Integer>> dataset = new ArrayList<>(); //full dataset saving ids(will be sorted soon)
    Map<Integer, Integer> L1 = new HashMap<>(); //Frequent 1-itemsets.

    Map<Integer, Integer> C1_cnt = new HashMap<>();

    try (BufferedReader csvReader = new BufferedReader(new FileReader(csv_file))) {//read csv file
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
            temp.sort(null);//sort all transactions
            dataset.add(new LinkedHashSet<>(temp));

        }
    } catch (IOException e) {
        e.printStackTrace();
    }


    for(int i=0;i<grocs.size();i++){
        if(C1_cnt.get(i)*1.0/dataset.size() >= minsup){//filter out the non-frequent items
            L1.put(i, C1_cnt.get(i));
        }
    }

    List<Map.Entry<Integer, Integer>> sorting_L1 = new ArrayList<>(L1.entrySet());
    sorting_L1.sort(Comparator.comparing((Map.Entry<Integer, Integer> e) -> e.getValue()).reversed());

    List<Integer> F_list = sorting_L1.stream().map(Map.Entry::getKey).collect(Collectors.toList());//The F-list, in descending frequency order.

    List<List<Integer>> filtered_dataset = new ArrayList<>();

    //second DB scan, to make filtered, ordered transactions
    for(var trans:dataset){
        List<Integer> filtered_trans = new ArrayList<>();
        for(Integer item:F_list){
            if(trans.contains(item)){
                filtered_trans.add(item);
            }
        }
        filtered_dataset.add(filtered_trans);
    }

    //Now make FP tree
    Tree tree = new Tree(F_list);
    for(var trans:filtered_dataset){
        tree.insert(trans, 1);
    }

    //get all frequent itemsets
    Map<List<Integer>, Integer> answer_map = new HashMap<>();//the final mapping we want. (Itemset, support)
    Collections.reverse(F_list);//We make conditional FP trees from less frequent items

    Runnable get_answer = new Runnable() {//FP-tree mining process. Making runnable to use nested function, because we need many variables from main.
        @Override
        public void run(){
            get_all_freq(new ArrayList<>(), Integer.MAX_VALUE, tree, F_list);
        }
        public void get_all_freq (List<Integer> prefix, int max_cnt, Tree fp_tree, List<Integer> prefix_list){//get all frequent itemsets with current prefix, adding more items from fp_tree. With info of current maximum support of tree, and the reversed F-list.
            int start = 0;
            if(prefix.size() > 0){
                start = prefix_list.indexOf(prefix.get(prefix.size()-1)) + 1;
            }
            for(int i=start; i<prefix_list.size(); i++){//Loop until all prefixes are used.
                int new_prefix_id = prefix_list.get(i);
                Tree next_tree = new Tree(prefix_list); //constructing the new conditional FP-tree.
                int min_check = 0;
                for(var base: fp_tree.make_pattern_base(new_prefix_id).entrySet()){
                    min_check += base.getValue();//add from all conditional FP trees
                    next_tree.insert(base.getKey(), base.getValue());//insert branch with the support
                }
                if(min_check*1.0/dataset.size() >= minsup){
                    List<Integer> freq_set = new ArrayList<>(prefix);
                    freq_set.add(new_prefix_id);
                    answer_map.put(freq_set, min_check); //this itemset is checked to be frequent, so put to the answer set.

                    //go deeper
                    get_all_freq(freq_set, Integer.min(max_cnt, min_check), next_tree, prefix_list);
                }

            }
        }
    };

    get_answer.run();//do FP tree mining

    List<Map.Entry<List<Integer>, Integer>> final_Ls = new ArrayList<>(answer_map.entrySet());
    
    final_Ls.sort(Comparator.comparing(Map.Entry::getValue));//the final answer, sorted asceding by support.

    //Print all frequent itemsets and its support.
    for(var itemset:final_Ls){
        String items = String.join(",", itemset.getKey().stream()
                                                        .map((i) -> id_to_grocs.get(i))
                                                        .collect(Collectors.toList()));
        System.out.println(String.format("%s %f", items, itemset.getValue()*1.0/dataset.size()));
    }
  }

}
