//import CountMinSketchState;
package storm.starter.trident.project.countmin.state;

import storm.trident.state.BaseQueryFunction;
import storm.trident.tuple.TridentTuple;
import storm.trident.operation.TridentCollector;
import java.util.List;
import java.util.ArrayList;
import backtype.storm.tuple.Values;
import java.util.*;
import storm.starter.trident.project.countmin.state.CountMinSketchState;

/**
 *@author: SWAGAT
 */

public class CountMinTopK extends BaseQueryFunction<CountMinSketchState, String> {
    public List<String> batchRetrieve(CountMinSketchState state, List<TridentTuple> inputs) {

    List<String> priolist = new ArrayList();

    PriorityQueue<String> prioq; 
    String s;
    s = state.printprioq();
    priolist.add(s);
     
    return priolist;    // return the list of top-k words and their counts in concatenated format
}
    public void execute(TridentTuple tuple, String count, TridentCollector collector) {
        collector.emit(new Values(count));
    }    
}
