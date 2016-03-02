import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MultiCli {
    private static final int SUBSCRIBE = 0;
    private static final int CREATE = 1;
    private static final int PUBLISH = 2;
    private static final int PULL = 3;
	
	private static String localHash;
	
	public PSDaemon cliD;
	
	/* Internal functions */
	private void makeDir(File dir, String dirType) {
    	try {
    		dir.mkdir();
    	} catch (SecurityException se) {
    		System.err.println("Client: Lacking filesystem permissions to create " + dirType + ".");
    		System.exit(-1);
    	}
	}
	private void contentsToFile(File g, String contents) {
		try (PrintWriter p = new PrintWriter(g)) {
			p.println(contents);
		} catch (SecurityException se) {
			System.err.println("Client: Lacking filesystem permissions to create Topic dataStore entry.");
			System.exit(-1);
		} catch (FileNotFoundException fe) {
			System.err.println("Client: File missing; could not write the new Topic dataStore entry.");
			System.exit(-1);
		}
	}
	private Pair<String, Pair<Date, Integer>> constructNewPair(Date time, int size, String hash) {
		Pair<Date, Integer> metaData = new Pair<Date, Integer>(time, size);
		Pair<String, Pair<Date, Integer>> newPair = 
				new Pair<String, Pair<Date, Integer>>(hash, metaData);
		return newPair;
	}
	
	/* Constructor - initializes daemon to manage topics and messages
	 * while also listening for broadcast messages from the server.
	 */
	public MultiCli(int portNumber, String hash) {
		localHash = hash;
        
        /* Initialize to listen to broadcast messages in the style of a multiple threaded Server */
        cliD = new PSDaemon(portNumber);
        
		cliD.dataStore =  new File(System.getProperty("user.dir"), "my_store-" + localHash);
		
        if (!cliD.dataStore.exists()) {
        	makeDir(cliD.dataStore, "dataStore");
        } /* The Daemon runs once. */

        cliD.start();
    }

	/* Publish/Subscribe client processing */
	public String requestService(String hostName, int portNumber, String[] data)
			throws ParseException, SecurityException, UnknownHostException, IOException {
		try (	/* Establish connection */
				Socket socket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
		) {
			int requestType = Integer.parseInt(data[0]);
			String hashID = data[2];
			int daemonPortNumber = Integer.parseInt(data[3]);
			String topic = data[4];
			String date = data[5];
			int size = Integer.parseInt(data[6]);
			String contents = data[7];

			/* Request errors */
			if (daemonPortNumber != cliD.portNumber) {
				System.err.println("Client: Error requesting Service, invalid daemon portNumber.");
        		System.exit(-1);
			}
			if (requestType < 0 || requestType > 3) {
				System.err.println("Client: Error requesting Service, invalid requestType value.");
        		System.exit(-1);
			}
			if (requestType > SUBSCRIBE && !hashID.equals(cliD.myServerHashID)) {
        		System.err.println("Client: Error requesting Service, invalid hashID.");
        		System.exit(-1);
			}
        	if (requestType == CREATE && cliD.topicLists.containsKey(topic)) {
            	System.err.println("Client: Error requesting CREATE, Topic already exists.");
            	System.exit(-1);
        	}
        	if (!hashID.equals("\t") && requestType == PUBLISH && !cliD.topicLists.containsKey(topic)) {
            	System.err.println("Client: Error requesting PUBLISH, no such Topic.");
            	System.exit(-1);
        	}
			/* Send */
        	for (int d = 0; d < 8; ++d) {
        		out.println(data[d]);
        	}
        	
        	/* Receive */
        	String input = in.readLine();
        	int response;
        	String returnHashID = in.readLine();
        	
        	/* Response errors and processing */
        	if (input == null) {
        		System.err.println("Client: Server response error, null.");
        		System.exit(-1);
        	}
        	if ( (response = Integer.parseInt(input)) == 0) {
        		/* Should also have the Server send an error number. */
        		System.err.println("Client: Server PS Protocol Error.");
        		System.exit(-1);
        	}
        	if (returnHashID == null) {
        		System.err.println("Client: Server response token error, null.");
        		System.exit(-1);
        	}
        	if (returnHashID.equals("\t")) {
        		System.err.println("Client: Server response error, response token undefined.");
        		System.exit(-1);
        	}
        	if (response != 1 && requestType < PULL || response <  8 && requestType == PULL) {
        		System.err.println("Client: Server response error, invalid count.");
        		System.exit(-1);
        	}
        	/* Sub */
    		if (requestType == SUBSCRIBE) {
    			cliD.myServerHashID = returnHashID;
    			if (topic == "\t")
    				return cliD.myServerHashID;
    			
    		} else if (cliD.myServerHashID.equals("\t")) {
    			cliD.myServerHashID = returnHashID; /* The response token is permanent. */
    		}
    		System.out.printf("Client: received response token %s.\n", cliD.myServerHashID);

    		/* Pub */
        	if (requestType > SUBSCRIBE && !returnHashID.equals(cliD.myServerHashID)) {
            	System.err.println("Client: Server response error, invalid response token.");
            	System.exit(-1);
        	}
        	if (requestType < PULL) {

        		File f =  new File(cliD.dataStore, topic);
        		if (!f.exists()) { /* CREATE */
        			makeDir(f, "Topic");
        		}
        		
        		SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMddHHmmssSSSZ");
        		Date timeStamp = simpleDate.parse(date);
        		
        		if (requestType == PUBLISH) {
        			File g = new File(f, date + "-" + returnHashID);
        			contentsToFile(g, contents);
        		}
        		
        		Pair<String, Pair<Date, Integer>> newPair = constructNewPair(timeStamp, size, returnHashID);
        		ArrayList<Pair<String, Pair<Date, Integer>>> topicList = null;
        		if (!cliD.topicLists.contains(topic))
        			topicList = new ArrayList<Pair<String, Pair<Date, Integer>>>();
        		else
        			topicList = cliD.topicLists.get(topic);
        		
        		topicList.add(0, newPair);
        		cliD.topicLists.put(topic, topicList);
        		return returnHashID;
        	}
        	/* Pull */
        	input = in.readLine();
        	int nr = -1;
        	if (input == null || (nr = Integer.parseInt(input)) <= 0) {
        		System.err.println("Client: Server response error, zero or unspecified number of Topic file updates.");
        		System.exit(-1);
        	}
        	while ((returnHashID = in.readLine()) != null && nr > 0) {
    	    	topic = in.readLine();
        		date = in.readLine();
        		input = in.readLine();
        		if (topic == null || date == null || input == null) {
                	System.err.println("Client: Error requesting PULL, null input.");
                	System.exit(-1);
        		}
        		if (!cliD.topicLists.containsKey(topic)) {
                	System.err.println("Client: Error requesting PULL, update Topic not found.");
                	System.exit(-1);
        		}
        		SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMddHHmmssSSSZ");
        		Date timeStamp = simpleDate.parse(date);
    	    	size = Integer.parseInt(input);
    	    	
        		/* Write message contents to file. */
    	    	contents = "";
    	    	char[] message = new char[size];
            	int r = in.read(message, 0, size);
            	
            	if (r != size) {
                	System.err.println("Client: Error writing file for Topic update.");
                	System.exit(-1);
    	    	} else {
    	    		contents = new String(message);
    	    	}
        		File f = new File(cliD.dataStore, topic);
        		File g = new File(f, date + "-" + returnHashID);
    			contentsToFile(g, contents);
    			
        		/* Update class members. */
        		Pair<String, Pair<Date, Integer>> newPair = constructNewPair(timeStamp, size, returnHashID);	
        		ArrayList<Pair<String, Pair<Date, Integer>>> topicList = cliD.topicLists.get(topic);
        		topicList.add(0, newPair);
        		cliD.topicLists.put(topic, topicList);
        		
        		/* Next message */
        		nr--;
        		returnHashID = in.readLine();
        	}
        	if (nr != 0 || returnHashID != null) {
        		System.err.println("Client: Error updating Topic with files, invalid count or author ID.");
        		System.exit(-1);
        	}

        } catch (UnknownHostException e) {
        	System.err.println("Client: Don't know about host " + hostName);
        	System.exit(1);
        } catch (IOException e) {
        	System.err.println("Client: Couldn't get I/O for the connection to " + hostName);
        	System.exit(1);
        }
		return cliD.myServerHashID;
	}
}
