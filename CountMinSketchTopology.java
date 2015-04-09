package storm.starter.trident.project.countmin; 

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;
import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import storm.trident.TridentState;
import storm.trident.TridentTopology;
import storm.trident.operation.builtin.FilterNull;
import storm.trident.operation.builtin.MapGet;
import storm.trident.operation.builtin.Sum;
import storm.trident.state.StateFactory;
import storm.trident.testing.MemoryMapState;
import storm.trident.testing.Split;
import storm.trident.testing.FixedBatchSpout;
import backtype.storm.tuple.Values;
import storm.starter.trident.project.filters.Print;
import storm.starter.trident.project.functions.Tweet;
import storm.starter.trident.project.functions.TextBuilder;
import storm.starter.trident.project.functions.ToLowerCase;

import java.util.Arrays;

import storm.trident.operation.builtin.Count;

import storm.starter.trident.project.countmin.state.CountMinSketchStateFactory;
import storm.starter.trident.project.countmin.state.CountMinTopK;
import storm.starter.trident.project.countmin.state.CountMinSketchUpdater;
//import storm.starter.trident.project.functions.Split;
import storm.starter.trident.project.countmin.filters.Bloom;
import storm.starter.trident.project.functions.ParseTweet;
import storm.starter.trident.project.spouts.TwitterSampleSpout;

/**
 *@author: SWAGAT KUMAR DASH (Top-K)
 */

public class CountMinSketchTopology {

	 public static StormTopology buildTopology( String[] args, LocalDRPC drpc ) {

        TridentTopology topology = new TridentTopology();

        int width = 2000;	// number of columns in CountMin
		int depth = 10;		// number of rows in CountMin
		int seed = 10;
		int k = 15;			// number of words in Top-K list

		// Twitter's account credentials passed as args
		String consumerKey = args[0];		
        String consumerSecret = args[1];
        String accessToken = args[2];
        String accessTokenSecret = args[3];

        // Twitter topic of interest
        String[] arguments = args.clone();
        String[] topicWords = Arrays.copyOfRange(arguments, 4, arguments.length);
        
        // Create Twitter's spout
		TwitterSampleSpout spoutTweets = new TwitterSampleSpout(consumerKey, consumerSecret,
									accessToken, accessTokenSecret, topicWords);

		// Build a persistent state of words from the stream
		TridentState countMinDBMS = topology.newStream("tweets", spoutTweets)
            .each(new Fields("tweet"), new ParseTweet(), new Fields("text", "tweetId", "user"))				// Parse the tweets into text, tweetID and user
   			.each(new Fields("text", "tweetId", "user"), new TextBuilder(), new Fields("sentence"))     	// Form the sentence with tweet text
			.each(new Fields("sentence"), new Split(), new Fields("allwords"))								// Split each tweet sentence into words (space-delimited)
			.each(new Fields("allwords"), new ToLowerCase(), new Fields("words"))							// convert each of the word into lowercase
			.each(new Fields("words"), new Bloom())															// Pass each word with BloomFilter
			.partitionPersist( new CountMinSketchStateFactory(depth,width,seed,k), new Fields("words"), new CountMinSketchUpdater())	// CountMinSketchStateFactory creates a count-min data structure for the filtered words
			;																															

		// Query the persistent storage for the Top-K words and display
		topology.newDRPCStream("get_count", drpc)
			.stateQuery(countMinDBMS, new Fields("args"), new CountMinTopK(), new Fields("count"))
			.project(new Fields("args", "count"))
			;

		return topology.build();		// The built topology is returned finally

	}

	public static void main(String[] args) throws Exception {
		Config conf = new Config();
        	conf.setDebug( false );
        	conf.setMaxSpoutPending( 10 );

        	LocalCluster cluster = new LocalCluster();
        	LocalDRPC drpc = new LocalDRPC();													// Creating a local cluster toplogy for DRPC
        	cluster.submitTopology("get_count",conf,buildTopology(args, drpc));					//The topology "get_count" is submitted. The call to establish the topology is also placed here

        	for (int i = 0; i < 6; i++) {

            		System.out.println("DRPC RESULT:"+ drpc.execute("get_count","TopK"));		// "TopK" is passed as a dummy argument to the drpc event
            		Thread.sleep( 10000 );
        	}

		System.out.println("STATUS: OK");
		//cluster.shutdown();
        //drpc.shutdown();
	}
}
