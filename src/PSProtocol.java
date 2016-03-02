import java.text.ParseException;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PSProtocol extends MultiSer {
    private static final int SUBSCRIBING = 0;
    private static final int CREATING = 1;
    private static final int PUBLISHING = 2;
    private static final int REQUESTING = 3;
    public static final int BROADCASTING = 4;
    
    private static ArrayList<String> broadcastData = null;
    /* Source: 
     * http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
     */
    private static String readFile(String path, Charset encoding) throws IOException {
    	byte[] encoded = Files.readAllBytes(Paths.get(path));
    	return new String(encoded, encoding);
    }
    
    public static void streamInBroadcastInput(ArrayList<String> inputData) {
    	broadcastData = inputData;
    }
    
    public ArrayList<String> processInput(int inState, String[] inputData)
    		throws ParseException, SecurityException, IOException {

    	ArrayList<String> out = null;
    	/* inputData: userName, hashID, topic, date, size, contents, portNumber */
    	String userName = null;
    	String userID = null;
    	int portNumber = 0;
    	String topic = null;
    	String date = null;
    	int size = 0;
    	String contents = null;
    	
		System.out.println("---------------");
    	System.out.println("Protocol input:");
    	for (String i: inputData)
    		System.out.println(i);
    	System.out.println("---------------");
    	
    	if (inputData.length >= 7) {
    		userName = inputData[0];
    		userID = inputData[1];
    		portNumber = Integer.parseInt(inputData[2]);
    		topic = inputData[3];
    		date = inputData[4];
    		size = Integer.parseInt(inputData[5]);
    		contents = inputData[6];
    	}
    	
    	ArrayList<String> temp = null;
    	
        if (inState == BROADCASTING) {
        	temp = broadcastData;
        	userID = inputData[0];
        	topic = inputData[1];
        }
        
        else if (inState == REQUESTING)
        	temp = outTopic(userName, portNumber, userID, date, topic);
                
        if (inState == REQUESTING || inState == BROADCASTING) {
        	
            if (temp == null) {
            	System.err.println("Server: Error in output for pull/push of Topic data.");
            	return null;
            }
        	System.out.println("----------------------------------------------");
        	System.out.println("Protocol: BROADCAST/PULL Request input buffer:");
        	for (String t: temp)
        		System.out.println(t);
        	System.out.println("----------------------------------------------");
            
        	/* Expand and "serialize" for client: */
        	out = new ArrayList<String>();
        	
        	/* line 2: client UUID */
        	out.add(userID);
            /* line 3: Number of messages pulled */
        	out.add(Integer.toString(temp.size()/2));
        	
        	int c = 0;
        	while (c < temp.size()) {
        		
        		System.out.printf("Iter(+2): %d\n", c);
        		
        		String messageSize = temp.get(c++);
        		String fullPath = temp.get(c++);
        		
        		String[] name = fullPath.substring(fullPath.lastIndexOf("/") + 1).split("-", 3);
        		
        		System.out.println("---------");
        		System.out.println("Protocol: Deconstructed filename ->");
        		for (String n: name)
        			System.out.println(n);
        		System.out.println("---------");
        		
        		String messageUUID = name[2];
        		String messageDate = name[0] + "-" + name[1];
        		
                /* line 4: UUID of author */
        		out.add(messageUUID);
                /* line 5: Topic of message */
        		out.add(topic);
        		
                /* line 6: Date of message (reverse chronological) */
        		out.add(messageDate);
                /* line 7: Size of message (bytes, including terminating newline character) */
        		out.add(messageSize);
        		
                /* line 8: First line of contents (at least one line of contents) */
        		String fileContents = 
        				readFile(fullPath, Charset.defaultCharset()); /* Written with default */
        		out.add(fileContents);
        	}
        	
        	System.out.println("-------------------------------------------");
        	System.out.println("Protocol: Processed BROADCAST/PULL Request:");
        	for (String o: out)
        		System.out.println(o);
        	System.out.println("-------------------------------------------");
        	return out;
        }
        
        
    	int state = SUBSCRIBING;
        /* Create new user account */
        out = subService(userName, portNumber, date);
        if (!topic.equals("\t"))
        	state = CREATING;
        else if (inState == SUBSCRIBING)
        	return out;

        if (userID.equals("\t"))
        	userID = out.get(0);  /* If null error out of inTopic */

        if (state == CREATING) {
            /* Create new topic */
        	out = inTopic(userName, portNumber, userID, date, topic);
        	if (inState <= CREATING)
        		return out;
        	if (!contents.equals("\t"))
        		state = PUBLISHING;
        }
        if (state == PUBLISHING) {
        	/* Create new message */
        	out = pubService(userName, portNumber, userID, date, topic, size, contents);
        }
        return out; /* Printed line by line by the thread servicing the request. */
        
        /* Client expecting (insert "topic" at line 3 if broadcast): 
         * line 1: Length of response, as the number of lines to expect
         * line 2: Client UUID, either new upon the first request, or old for subsequent requests
         * (lines 1 & 2 mandatory, lines >= 3 ignored unless REQUEST or BROADCAST)
         * line 3: Number of messages pulled
         * line 4: UUID of author
         * line 5: Topic of message
         * line 6: Date of message
         * line 7: Size of message (bytes, including terminating newline character)
         * line 8: Contents of message including line separators
         * (lines >= 3 are for each message)
         */
    }
}
