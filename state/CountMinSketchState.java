/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package storm.starter.trident.project.countmin.state;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.Random;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import storm.trident.state.State;
import storm.trident.state.StateFactory;
import storm.trident.state.StateType;

//import com.clearspring.analytics.stream.membership.Filter;
//import Filter;

/**
 * Count-Min Sketch datastructure.
 * An Improved Data Stream Summary: The Count-Min Sketch and its Applications
 * http://www.eecs.harvard.edu/~michaelm/CS222/countmin.pdf
 * Modified by Preetham MS. Originally by https://github.com/addthis/stream-lib/
 * @author: Preetham MS (pmahish@ncsu.edu)
 */
public class CountMinSketchState implements State {

    public static final long PRIME_MODULUS = (1L << 31) - 1;

    int depth;
    int width;
    long[][] table;
    long[] hashA;
    long size;
    double eps;
    double confidence;
    
    int k;
    PriorityQueue<String> priorityqueue;
    Comparator<String> comparator = new StringComparator();
       
    CountMinSketchState() {
    }

    public CountMinSketchState(int depth, int width, int seed,int k) {
        this.depth = depth;
        this.width = width;
        this.eps = 2.0 / width;
        this.confidence = 1 - 1 / Math.pow(2, depth);
        initTablesWith(depth, width, seed);
        this.k = k;                                                                 // initialize k (for Top-K)
        priorityqueue = new PriorityQueue<String>(k,comparator);                    // initialize the priorityqueue with k and a comparator
    }

    public CountMinSketchState(double epsOfTotalCount, double confidence, int seed) {
        // 2/w = eps ; w = 2/eps
        // 1/2^depth <= 1-confidence ; depth >= -log2 (1-confidence)
        this.eps = epsOfTotalCount;
        this.confidence = confidence;
        this.width = (int) Math.ceil(2 / epsOfTotalCount);
        this.depth = (int) Math.ceil(-Math.log(1 - confidence) / Math.log(2));
        initTablesWith(depth, width, seed);
    }

    public CountMinSketchState(int depth, int width, int size, long[] hashA, long[][] table) {
        this.depth = depth;
        this.width = width;
        this.eps = 2.0 / width;
        this.confidence = 1 - 1 / Math.pow(2, depth);
        this.hashA = hashA;
        this.table = table;
        this.size = size;
    }

    private void initTablesWith(int depth, int width, int seed) {
        this.table = new long[depth][width];
        this.hashA = new long[depth];
        Random r = new Random(seed);
        // We're using a linear hash functions
        // of the form (a*x+b) mod p.
        // a,b are chosen independently for each hash function.
        // However we can set b = 0 as all it does is shift the results
        // without compromising their uniformity or independence with
        // the other hashes.
        for (int i = 0; i < depth; ++i) {
            hashA[i] = r.nextInt(Integer.MAX_VALUE);
        }
    }

    public double getRelativeError() {
        return eps;
    }

    public double getConfidence() {
        return confidence;
    }

    int hash(long item, int i) {
        long hash = hashA[i] * item;
        // A super fast way of computing x mod 2^p-1
        // See http://www.cs.princeton.edu/courses/archive/fall09/cos521/Handouts/universalclasses.pdf
        // page 149, right after Proposition 7.
        hash += hash >> 32;
        hash &= PRIME_MODULUS;
        // Doing "%" after (int) conversion is ~2x faster than %'ing longs.
        return ((int) hash) % width;
    }

    
    public void add(long item, long count) {
        if (count < 0) {
            // Actually for negative increments we'll need to use the median
            // instead of minimum, and accuracy will suffer somewhat.
            // Probably makes sense to add an "allow negative increments"
            // parameter to constructor.
            throw new IllegalArgumentException("Negative increments not implemented");
        }
        for (int i = 0; i < depth; ++i) {
            table[i][hash(item, i)] += count;
        }
        size += count;
    }

    
    public void add(String item, long count) {
        if (count < 0) {
            // Actually for negative increments we'll need to use the median
            // instead of minimum, and accuracy will suffer somewhat.
            // Probably makes sense to add an "allow negative increments"
            // parameter to constructor.
            throw new IllegalArgumentException("Negative increments not implemented");
        }
        int[] buckets = Filter.getHashBuckets(item, depth, width);
        for (int i = 0; i < depth; ++i) {
            table[i][buckets[i]] += count;
        }
        size += count;
        updatepriorityqueue(item);                                  // with each added word, the priority queue needs to be managed too
    }

    /****************************************************************
    *******************StringComparator******************************
    *****************************************************************
    // Comparator class used to compare counts of words in CounMin //
    // decide the basis of formation for a PriorityQueue           //
    ****************************************************************/
    public class StringComparator implements Comparator<String>
    {
        @Override
        public int compare(String x, String y)
        {
        
            if (estimateCount(x) < estimateCount(y))
            {
                return -1;                                            // return -1 if first string in argument has lesser count
            }
            if (estimateCount(x) > estimateCount(y))
            {
                return 1;                                             // return 1 if second string in argument has lesser count
            }
            return 0;                                                 // return 0 if both strings have the same count
        }
    }

    /****************************************************************
    *******************updatepriorityqueue***************************
    *****************************************************************
    // Takes care of incoming words and updates the PriorityQueue  //
    // to store only Top-K words                                   //
    ****************************************************************/
    public void updatepriorityqueue(String s)
    {
        int size;
        long count_new, count_head;
        String head, removedhead;

        size = priorityqueue.size();
        if(size<k)                                                   // If queue size is less than k, add the new word to queue
        {
            priorityqueue.remove(s);                                 // remove the word from queue, if it exists, to avoid duplicates
            priorityqueue.offer(s);                                  // add the word to the queue 
        }
        else                                                         // else compare the incoming word with the top-K words in queue
        {
            count_new=estimateCount(s);                              // fetch the count of incoming word
            head=priorityqueue.peek();                               // fetch the head of queue i.e. least count word in queue
            count_head=estimateCount(head);                          // fetch the count of head word
            if(count_new > count_head)                               // compare the counts
            {
                if(priorityqueue.contains(s) && head!=s)             // check if the queue already contains the incoming word 
                    priorityqueue.remove(s);                         // and remove that if it's anything but the head of queue
                removedhead=priorityqueue.poll();                    // remove the head from queue
                priorityqueue.offer(s);                              // add the new word to queue
            }
        }
    }

    /****************************************************************
    *******************printprioq************************************
    *****************************************************************
    // Displays the Top-K words from the PriorityQueue with counts //
    ****************************************************************/
    public String printprioq()
    {
        String result = "";

        Object[] arr = priorityqueue.toArray();
        for ( int i = 0; i<arr.length; i++ ){  
          result+=arr[i].toString() + "(" + String.valueOf(estimateCount(arr[i].toString())) + ") ";
        }
        return result;
    }

    
    public long size() {
        return size;
    }

    /**
     * The estimate is correct within 'epsilon' * (total item count),
     * with probability 'confidence'.
     */
    
    public long estimateCount(long item) {
        long res = Long.MAX_VALUE;
        for (int i = 0; i < depth; ++i) {
            res = Math.min(res, table[i][hash(item, i)]);
        }
        return res;
    }

    
    public long estimateCount(String item) {
        long res = Long.MAX_VALUE;
        int[] buckets = Filter.getHashBuckets(item, depth, width);
        for (int i = 0; i < depth; ++i) {
            res = Math.min(res, table[i][buckets[i]]);
        }
        return res;
    }

    /**
     * Merges count min sketches to produce a count min sketch for their combined streams
     *
     * @param estimators
     * @return merged estimator or null if no estimators were provided
     * @throws CMSMergeException if estimators are not mergeable (same depth, width and seed)
     */
    public static CountMinSketchState merge(CountMinSketchState... estimators) throws CMSMergeException {
        CountMinSketchState merged = null;
        if (estimators != null && estimators.length > 0) {
            int depth = estimators[0].depth;
            int width = estimators[0].width;
            long[] hashA = Arrays.copyOf(estimators[0].hashA, estimators[0].hashA.length);

            long[][] table = new long[depth][width];
            int size = 0;

            for (CountMinSketchState estimator : estimators) {
                if (estimator.depth != depth) {
                    throw new CMSMergeException("Cannot merge estimators of different depth");
                }
                if (estimator.width != width) {
                    throw new CMSMergeException("Cannot merge estimators of different width");
                }
                if (!Arrays.equals(estimator.hashA, hashA)) {
                    throw new CMSMergeException("Cannot merge estimators of different seed");
                }

                for (int i = 0; i < table.length; i++) {
                    for (int j = 0; j < table[i].length; j++) {
                        table[i][j] += estimator.table[i][j];
                    }
                }
                size += estimator.size;
            }

            merged = new CountMinSketchState(depth, width, size, hashA, table);
        }

        return merged;
    }

    public static byte[] serialize(CountMinSketchState sketch) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(bos);
        try {
            s.writeLong(sketch.size);
            s.writeInt(sketch.depth);
            s.writeInt(sketch.width);
            for (int i = 0; i < sketch.depth; ++i) {
                s.writeLong(sketch.hashA[i]);
                for (int j = 0; j < sketch.width; ++j) {
                    s.writeLong(sketch.table[i][j]);
                }
            }
            return bos.toByteArray();
        } catch (IOException e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public static CountMinSketchState deserialize(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        DataInputStream s = new DataInputStream(bis);
        try {
            CountMinSketchState sketch = new CountMinSketchState();
            sketch.size = s.readLong();
            sketch.depth = s.readInt();
            sketch.width = s.readInt();
            sketch.eps = 2.0 / sketch.width;
            sketch.confidence = 1 - 1 / Math.pow(2, sketch.depth);
            sketch.hashA = new long[sketch.depth];
            sketch.table = new long[sketch.depth][sketch.width];
            for (int i = 0; i < sketch.depth; ++i) {
                sketch.hashA[i] = s.readLong();
                for (int j = 0; j < sketch.width; ++j) {
                    sketch.table[i][j] = s.readLong();
                }
            }
            return sketch;
        } catch (IOException e) {
            // Shouldn't happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beginCommit(Long txid) {
        return;
    }

    @Override
    public void commit(Long txid) {
        return;
    }

    @SuppressWarnings("serial")
    protected static class CMSMergeException extends RuntimeException {
   // protected static class CMSMergeException extends FrequencyMergeException {

        public CMSMergeException(String message) {
            super(message);
        }
    }
}
