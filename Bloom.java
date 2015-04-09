package storm.starter.trident.project.countmin.filters;

import storm.trident.operation.BaseFilter;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;
import storm.starter.trident.project.countmin.filters.BloomFilter;
import java.io.*;
/**
* Print Filter for printing Trident tuples;
* useful for testing and debugging.
*
* @author SWAGAT
*/
public class Bloom extends BaseFilter {

	int noItems = 14395;							// no.of words from the "stop-words" list 
	int bitsPerItem = 8; 							// no.of bits per entry in the BloomFilter
	int noHashes = 5;								// no.of Hash functions noHashes = floor(0.693*bitsPerItem) = 5
	BloomFilter bloomfilter;
	String DATA_PATH;

public Bloom()
{
	DATA_PATH = "data/stop-words.txt";		//The input data stream for stop-words

    bloomfilter = new BloomFilter(noItems, bitsPerItem, noHashes); // initialize the BloomFilter with parameters

	BufferedReader br = null;
 
	String s;
	try{

 		br = new BufferedReader(new FileReader(DATA_PATH));			// read the file using buffer
 	
		while ((s = br.readLine()) != null) {
			bloomfilter.add(s);										// add each word to BloomFilter
		}
	}
	catch(Exception e){
 		System.out.println("File not Found");
 	}
}
	@Override
public boolean isKeep(TridentTuple tuple) {

	return !(bloomfilter.contains(tuple.getString(0)));					// Every incoming word from the stream is matched with the bloomfilter 
																		// list of words. If present, then return false (to drop)

}
}