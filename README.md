# Top-K-twitter-words
A Top-K list of frequent words from twitter feed through a Bloomfilter of stop-words

***********Software that needs to be installed (if any) with download and installation instructions**********************

The evaluator should have a working Apache Storm installation as per the project requirements. 

***********Environment variable settings************************************

The environment variable settings necessary to navigate to the "home/apache-storm-0.9.3/examples/storm-starter" directory where the submitted tutorial folder could be unzipped and read from, are as below:

export PATH="$HOME/apache-storm-0.9.3/bin:$PATH"
export STORM_HOME="$HOME/apache-storm-0.9.3"
export TRIDENT_STARTER=\
"$STORM_HOME/examples/storm-starter"

The environment variable settings to access the twitter stream also need to be mentioned as below:
TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET, TWITTER_ACCESS_TOKEN and TWITTER_ACCESS_SECRET with their respective values for the user. These would be used as twitter spouts in both Parts B and C.

Save these variables in the $HOME/.bashrc file.

***********Instructions on how to run the program***************************

1. Extract the assignment from the submitted zip file.
2. Copy the extracted folder 'project' into the "$HOME/apache-storm-0.9.3/examples/storm-starter/src/jvm/storm/starter/trident/" directory.
3. Copy the list of stop-words i.e. the text file "stop-words.txt", into the directory "home/apache-storm-0.9.3/examples/storm-starter/data". Or the evaluator may use their own list of stop-words.
Please not: The variable "noItems" on line 17 in Bloom.java is initialised to the current number of words from the stop-words list provided alongwith the project zip file. In case the evaluator decides to use their own stop-words list, initialise this variable accordingly. Changes are limited to this file only.
4. cd into the directory '$TRIDENT_STARTER' and enter the command 'mvn package' to build the package.
5. The directory includes files for both Part-B and Part-C of P2.  

Submit the 'CountMinSketchTopology.java' to Storm with the command 'storm jar target/storm-starter-0.9.3-jar-with-dependencies.jar storm.starter.trident.project.countmin.CountMinSketchTopology $TWITTER_CONSUMER_KEY $TWITTER_CONSUMER_SECRET $TWITTER_ACCESS_TOKEN $TWITTER_ACCESS_SECRET' (To save the results into a text file, append '> output.txt &' to the command).

***********Instructions on how to interpret the results**********************

1. The output text is usually large and needs to be interpreted carefully for results. For convenience, use the command: cat output.txt | grep "DRPC RESULT" to read the output of interest from the file.
2. Each extracted line (as above) would contain a list of words (k=15) and their counts in parantheses. This list is the top-k words according to their counts in the CountMin. 
3. Since all the words in stop-words list are in lowercase, the stream of words from tweets are converted to lowercase first before being filtered by the BloomFilter to give meaningful results of the counts. Hence, all the words in the output list are also in lowercase.
4. The stream of results is run over a loop for 6 iterations with a time interval of 10 seconds between each of them to allow the priority queue to be filled with new words (if any) with larger counts. 
5. At the end of all iterations, "STATUS: OK" is printed and the program could be stopped by command Ctrl+C.

***********Sample input and output files (if applicable)**********************

Part-C
DRPC RESULT:[["TopK","http:\/\/t.co\/98xr2tna3y(2) love(11) #eclipse(10) :(19) yg(12) &(13) #kca(11) eclipse(19) -(50) follow(12) #eclipse2015(18) اللهم(16) الله(21) rt(371) ...(13) "]]
DRPC RESULT:[["TopK","...(17) #kca(18) \u2026(17) follow(20) &(20) اللهم(21) love(17) :(23) -(69) eclipse(30) الله(25) #eclipse2015(26) ＠lov(32) rt(606) yg(28) "]]

***********References***********

1. BloomFilter.java (http://www.javamex.com/tutorials/collections/bloom_filter_java.shtml)
2. Stop-words (version stop-words-collection-2014-02-24.zip) (https://code.google.com/p/stop-words/)
3. Reading file in Java (http://www.mkyong.com/java/how-to-read-file-from-java-bufferedreader-example/)
4. PriorityQueue comparator (http://stackoverflow.com/questions/683041/java-how-do-i-use-a-priorityqueue)
