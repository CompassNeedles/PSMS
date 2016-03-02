import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MultiSer {
	
	private static ConcurrentHashMap<String, Pair<Integer, String>> userBase
		= new ConcurrentHashMap<String, Pair<Integer, String>>();
	private static ConcurrentHashMap<String, ArrayList<Pair<String, Date>>> userTopics
		= new ConcurrentHashMap<String, ArrayList<Pair<String, Date>>>();
	private static ConcurrentHashMap<String, ArrayList<Pair<String, Pair<Date, Integer>>>> topicLists = 
			new ConcurrentHashMap<String, ArrayList<Pair<String, Pair<Date, Integer>>>>();
	
	private final static String myHash = UUID.randomUUID().toString();
	private final static File dataStore =
			new File(System.getProperty("user.dir"), "master_store-" + myHash);

	private ArrayList<String> toArray(String uID) {
		ArrayList<String> ref = new ArrayList<String>();
		ref.add(uID);
		return ref;
	}
	private Date getDateFromString(String date) throws ParseException {
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMddHHmmssSSSZ");
		Date timeStamp = simpleDate.parse(date);
		return timeStamp;
	}
	private String dateToString(Date date) {
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMddHHmmssSSSZ");
		String timeStamp = simpleDate.format(date);
		return timeStamp;
	}
	private Pair<Integer, String> newUser(int portNum, String uName) {
		Pair<Integer, String> basicData = new Pair<Integer, String>(portNum, uName);
		return basicData;
	}
	private Pair<String, Date> newUserTopic(String topic, Date date) {
		Pair<String, Date> metaData = new Pair<String, Date>(topic, date);
		return metaData;
	}
	private Pair<String, Pair<Date, Integer>> constructTopicPair(String hash, Date date, int size) {
		Pair<Date, Integer> metaData = new Pair<Date, Integer>(date, size);
		Pair<String, Pair<Date, Integer>> newPair = 
				new Pair<String, Pair<Date, Integer>>(hash, metaData);
		return newPair;
	}
	private int contentsToFile(File g, String contents) {
		try (PrintWriter out = new PrintWriter(g)) {
			out.print(contents);
		} catch (SecurityException se) {
			System.err.println("Server: Lacking filesystem permissions to create Topic dataStore entry.");
			return 0;
		} catch (FileNotFoundException fe) {
			System.err.println("Server: Could not create Topic dataStore entry.");
			return 0;
		}
		return 1;
	}
	
	public ArrayList<String>
	subService(String userName, int portNumber, String date)
			throws ParseException {
		
		System.out.println("Server: SUBSCRIBE.");
		for (Pair<Integer, String> p: userBase.values()) { /* Prioritize SUBSCRIBE */
			if (p.getFirst() == portNumber && p.getSecond().equals(userName)) {
				/* Using userName as hostName.
				 * Need a unique portNumber-hostName pair. */
				System.out.println("Server: SUBSCRIBE request with matching portNumber and userName.");
				return null;
			}
		}
		String uID = UUID.randomUUID().toString();
		userBase.put(uID, newUser(portNumber, userName));
		
		return toArray(uID);
	}

	public ArrayList<String>
	inTopic(String uName, int portNumber, String uID, String date, String topic)
			throws ParseException, SecurityException {

		System.out.println("Server: CREATE.");

		if (!userBase.containsKey(uID)) /* uID absent */
			return null;
		if (!userBase.get(uID).getSecond().equals(uName)) /* uName mismatch (hostName) */
			return null;
		if (!userBase.get(uID).getFirst().equals(portNumber)) /* wrong broadcast number */
			return null;
		
		/* userBase handled in subService, userTopics here */
		if (userTopics.containsKey(uID)) {
			for (Pair<String, Date> p: userTopics.get(uID)) {
				if (p.getFirst().equals(topic))
					return toArray(uID);
			} /* Implied: topic exists; no need to re-subscribe. Check topicLists elsewhere. */
		}
		
		Date never = new Date(Long.MIN_VALUE); /* The Topic store has never been pushed to user */
		if (userTopics.containsKey(uID)) {
			ArrayList<Pair<String, Date>> tempArray = userTopics.get(uID);
			tempArray.add(0, newUserTopic(topic, never));
			userTopics.put(uID, tempArray);
		} else {
			ArrayList<Pair<String, Date>> tempArray = new ArrayList<Pair<String, Date>>();
			tempArray.add(newUserTopic(topic, never));
			userTopics.put(uID, tempArray);
		}
		
		if (topicLists.containsKey(topic)) /* Continue to PUBLISH (assumed end-state)  */
			return toArray(uID);

		/* Create */
		File t = new File(dataStore, topic);
		if (t.exists()) {
			return toArray(uID);
		} else {
			try {
				t.mkdir();
			} catch (SecurityException se) {
				System.err.println("Server: Lacking filesystem permissions to create Topic dataStore.");
				return null;
			}
		}
		
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMddHHmmssSSSZ");
		Date timeStamp = null;
		try {
			timeStamp = simpleDate.parse(date);
		} catch (ParseException pe) {
			System.err.println("Server: Could not parse Topic submission date.");
			t.delete();
			return null;
		}
		
		/* Update list of topics after write to FS. Update userBase and userTopics before write to FS. 
		 * Here prioritize user data over FS and topicList. In pubService prioritize the latter.
		 */
		ArrayList< Pair<String, Pair<Date, Integer>>> newTopic = new ArrayList< Pair<String, Pair<Date, Integer>>>();
		newTopic.add(constructTopicPair(uID, timeStamp, 0));
		topicLists.put(topic, newTopic);

		System.out.println("Server: return from inTopic.");
		return toArray(uID);
	}

	public ArrayList<String>
	pubService(String uName, int portNumber, String uID, String date, String topic, int size, String contents)
			throws ParseException, SecurityException, FileNotFoundException {
		
		System.out.println("Server: PUBLISH.");

		if (!userBase.containsKey(uID)) /* uID absent */
			return null;
		if (!userBase.get(uID).getSecond().equals(uName)) /* uName mismatch */
			return null;
		if (!userBase.get(uID).getFirst().equals(portNumber)) /* wrong broadcast number */
			return null;
		if (!topicLists.containsKey(topic)) /* topic absent */
			return null;

		if (!userTopics.containsKey(uID)) { /* userName not subscribed */
			return null;
		} else {
			boolean present = false;
			for (Pair<String, Date> q: userTopics.get(uID)) {
				if (q.getFirst().equals(topic)) {
					present = true;
					break;
				}
			}
			if (!present)
				return null; /* Error since always passes through inTopic() first */
		}

		File t = new File(dataStore, topic);
		if (!t.exists()) {
			System.err.println("Server: Error locating Topic in dataStore, cannot save message.");
			return null;
		}

		/* Update/push data */
		Date timeStamp = getDateFromString(date);
		Pair<String, Pair<Date, Integer>> newPair = constructTopicPair(uID, timeStamp, size);
		ArrayList<Pair<String, Pair<Date, Integer>>> topicList = topicLists.get(topic);

		/* Modify topic's message list */
		if (   topicList.get(0).getFirst().equals(uID)
			&& topicList.get(0).getSecond().getSecond() == 0) { /* 0 message size implies create */
			
			topicList.set(0, newPair); /* Replace */
		} else {
			topicList.add(0, newPair); /* Add at the beginning */
		}
		/* Update mapping of topics to messages */
		topicLists.put(topic, topicList);

		/* Now write contents to file in Topic dataStore */
		File g = new File(t, date + "-" + uID);
		if (contentsToFile(g, contents) == 0)
			return null;
		
		/* Update user's topic list */
		int i = 0;
		ArrayList<Pair<String, Date>> tempArray = userTopics.get(uID);
		for ( ; i < tempArray.size(); ++i) {
			if (tempArray.get(i).getFirst().equals(topic)) {/* Always enters block */
				timeStamp = tempArray.get(i).getSecond();   /* Never or post-server-push */
				break;
			}
		}
		tempArray.remove(i);
		tempArray.add(0, newUserTopic(topic, timeStamp));
		userTopics.put(uID, tempArray);

		System.out.println("Server: pubService, Issue Broadcast.");
		issueBroadCast(topic, date);
		
		System.out.println("Server: pubService, Return.");
		return toArray(uID);
	}

	public ArrayList<String>
	outTopic(String uName, int portNumber, String uID, String date, String topic)
			throws ParseException {
		
		System.out.println("Server: PULL/PUSH.");

		if (!userBase.containsKey(uID)) /* uID absent */
			return null;
		if (!userBase.get(uID).getSecond().equals(uName)) /* uName mismatch */
			return null;
		if (!userBase.get(uID).getFirst().equals(portNumber)) /* wrong broadcast number */
			return null;
		if (!topicLists.containsKey(topic)) /* topic absent */
			return null;
		/* Check the file system */
		File t = new File(dataStore, topic);
		if (!t.exists()) {
			System.err.println("Server: Error locating Topic in dataStore, cannot retrieve messages.");
			return null;
		}
		
		/* Compare time stamps */
		Date lastUpdate = null;
		if (!userTopics.containsKey(uID)) /* userName not subscribed */
			return null;
		else {
			for (Pair<String, Date> q: userTopics.get(uID)) {
				if (q.getFirst().equals(topic)) {
					lastUpdate = q.getSecond();
					break;
				}
			}
		}
		if (lastUpdate == null) /* Error: not subscribed */
			return null;
		Date requestDate = getDateFromString(date);
		if (lastUpdate.after(requestDate)) {
			return null; /* Request already fulfilled */
		}

		/* Get messages for this topic and output filenames/sizes with a more recent
		 * timeStamp than the requester's last update.
		 */
		ArrayList<Pair<String, Pair<Date, Integer>>> messages = topicLists.get(topic);
		int c = 0;
		Date currentUpdate = messages.get(c).getSecond().getFirst();
		Date upDate = lastUpdate;
		ArrayList<String> output = null;
		
		while (currentUpdate != null && currentUpdate.after(lastUpdate)) {
			if (output == null) /* initialize array */
				output = new ArrayList<String>();
			
			System.out.println("Entered.");
			output.add(Integer.toString(messages.get(c).getSecond().getSecond())); /* File size */
			
			String muID = messages.get(c).getFirst();
			File entry = new File(t, dateToString(currentUpdate) + "-" + muID);
			output.add(entry.getAbsolutePath()); /* File path */
			
			/* Refresh timeStamp */
			upDate = currentUpdate;
			
			/* Next message */
			if (++c >= messages.size())
				break;
			currentUpdate = messages.get(c).getSecond().getFirst();
		}
		
		/* Update user's topic list */
		int i = 0;
		ArrayList<Pair<String, Date>> tempArray = userTopics.get(uID);
		for ( ; i < tempArray.size(); ++i) {
			if (tempArray.get(i).getFirst().equals(topic)) {
				break;
			}
		}
		tempArray.remove(i);
		tempArray.add(0, newUserTopic(topic, upDate));
		userTopics.put(uID, tempArray);
		
		System.out.println("Server outTopic:");
		for (String o: output) {
			System.out.println(o);
		}
		System.out.println("RETURN");
		return output;
	}
	
	private void issueBroadCast(String topic, String date) throws ParseException {
		
		System.out.println("Server: BROADCAST.");
		ArrayList<ArrayList<String>> out = null;
		
		for (Entry<String, Pair<Integer, String>> entry : userBase.entrySet()) {
		    String uID = entry.getKey();
		    Pair<Integer, String> value = entry.getValue();
		    String uName = value.getSecond();
		    int portNumber = value.getFirst();
		    ArrayList<String> output = outTopic(uName, portNumber, uID, date, topic);
		    if (output != null) {
		    	if (out == null) {
		    		out = new ArrayList<ArrayList<String>>();
		    	}
		    	output.add(0, uID);
		    	output.add(0, Integer.toString(portNumber));
		    	output.add(0, topic);
		    	output.add(0, uName);
		    	out.add(output);
		    }
		}
		
		System.out.println("Server: BROADCAST...");
		for (ArrayList<String> a: out) {
			for (String o: a)
				System.out.println(o);
		}
		System.out.println("....... Spawning");
		new PSBroadcast(out).start();
		System.out.println("Server: Spawned BROADCAST");
	}

	public static void main(String[] args) throws SecurityException, Exception, IOException {

	    if (args.length != 1) {
	        System.err.println("Server Usage: java MultiSer <port number 1>");
	        System.exit(1);
	    }
        
        if (!dataStore.exists()) {
        	try {
        		dataStore.mkdir();
        	} catch (SecurityException se) {
        		System.err.println("Server: Lacking filesystem permissions to create dataStore.");
        		System.exit(-1);
        	} catch (Exception e) {
        		System.err.println("Server: Could not create dataStore.");
        		System.exit(-1);
        	}
        }

        int portNumber = Integer.parseInt(args[0]);
        boolean listening = true;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {

        	System.out.printf("Server: Established connection on port %d.\n", portNumber);
            while (listening) {
	            new PSMultiServe(serverSocket.accept()).start();
	        }

	    } catch (IOException e) {
            System.err.println("Server: Could not listen on port " + portNumber + ".");
            System.exit(-1);
        }
	}
}
