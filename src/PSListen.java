import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.*;

public class PSListen extends Thread {
    private Socket socket = null;
    private String myID = null;
    private String dataStorePath = null;

    public boolean terminated = false;
    public String updateTopic = null;
    public ArrayList<Pair<String, Pair<Date, Integer>>> toDaemonTemp = null;

    private String readInput(BufferedReader inputStream, int size) {
    	System.out.println("Listener: Broadcast input buffered read, trying.");
    	try {
    		if (size > 0) {
    			char[] cbuf = new char[size];
    			int r = inputStream.read(cbuf, 0, size);
    			System.out.printf("Listener: Read %d vs. %d.\n", r, size);
    			if (r != size) {
    				System.err.println("Listener: Could not read number of Server Broadcast input message bytes.");
    				return null;
    			}
    			char flush = (char) inputStream.read();
    			if (flush != '\n') {
    				System.err.println("Listener: Error flushing Broadcast input Read buffer.");
    				return null;
    			}
    			return new String(cbuf);
    		}
    		return inputStream.readLine();
    		
    	} catch (IOException e) {
    		System.out.println("Listener: Broadcast input buffered reader Error.");
    		e.printStackTrace();
    		return null;
    	}
    }
    private void addPair(String hash, String date, String size) throws ParseException {
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMddHHmmssSSSZ");
		Date timeStamp = simpleDate.parse(date);
		
		if (toDaemonTemp == null)
			toDaemonTemp = new ArrayList<Pair<String, Pair<Date, Integer>>>();
		
    	Pair<Date, Integer> metaData = new Pair<Date, Integer>(timeStamp, Integer.parseInt(size));
    	Pair<String, Pair<Date, Integer>> newPair = new Pair<String, Pair<Date, Integer>>(hash, metaData);
    	toDaemonTemp.add(newPair);
    }
    private void printError() {
    	System.err.println("Listener: Error reading data during Server broadcast connection.");
    	System.exit(-1);
    }
    
    /* Constructor: spawn new thread */
    public PSListen(Socket socket, String hashID, String path) {
        super("PSListen"); /* Thread group */
        this.socket = socket;
        myID = hashID;
        System.out.println("Listener: Daemon Listener");
        System.out.println(myID);
        dataStorePath = path;
    }

    public void run() { /* Per Topic */

        try (
            PrintWriter out =
            	new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in =
            	new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
        	
            System.out.printf("Listener: started, %s.\n", myID);        	
        	/* Expecting: total number of lines, uID, topic
        	 * and then the typical "request" output:
        	 * number of messages, and five lines per message. */
        	String input, uID;
        	int totalInLines = 0, totalMessages = 0;
        	if ((input = in.readLine()) == null ||
        			(totalInLines = Integer.parseInt(input)) == 0) {
        		System.err.println(totalInLines);
        		System.err.println("Listener: Server Broadcast Error, zero input or number of lines.");
        		printError();
        	}
        	System.out.printf("Listener: totalLines, %d.\n", totalInLines);
        
        	if ((uID = in.readLine()) == null || !uID.equals(myID)) {
        		System.err.printf("Listener: my ID, %s  - input ID %s.\n", uID, myID);
        		System.err.println("Listener: Server Broadcast Error, invalid Topic validation ID token.");
        		printError();
        	}
        	System.out.printf("Listener: my ID, %s  - input ID %s.\n", uID, myID);
        	
        	if ((updateTopic = in.readLine()) == null) {
        		System.err.println(updateTopic);
        		System.err.println("Listener: Server Broadcast Error, input Topic null.");
        		printError();
        	}
        	System.out.println(updateTopic);
        	
        	if ((uID = in.readLine()) == null || !uID.equals(myID)) {
        		System.err.printf("Listener: my ID, %s  - input ID %s.\n", uID, myID);
        		System.out.println("Listener: Server Broadcast Error, invalid Response validation ID token.");
        		printError();
        	}
        	System.out.printf("Listener: input ID %s.\n", uID);
        	
        	if ((input = in.readLine()) == null ||
        			(totalMessages = Integer.parseInt(input)) == 0 ||
        			(totalMessages != (totalInLines-4)/5)) { /* Catches inconsistency between Protocol processing and Server Broadcast count */
        		
        		System.err.println(totalMessages);
        		System.err.println("Listener: Server Broadcast Error, inconsistent input counts.");
        		printError();
        	}
        	System.out.printf("Listener: totalMessages, %d.\n", totalMessages);

        	int countLines = 4;
        	String[] messageData = new String[5];
        	/* author, topic, date, size, contents */
        	int size = 0;
        	while ((input = readInput(in, size)) != null) {
        		int index = (countLines-4)%5;
        		
        		System.out.printf("Listener: looping, %d %s.\n", index, input);
        		messageData[index] = input;
        		
        		if (index == 3)
        			size = Integer.parseInt(input);
        		else
        			size = 0;
        		
        		countLines++;
        		if (index < 4)
        			continue;
        		
        		new PSWrite(dataStorePath, updateTopic, messageData);
        		System.out.println("Listener: Adding to temporary buffer.");
        		addPair(messageData[0], messageData[2], messageData[3]);
        	}
        	
        	if (countLines != totalInLines) {
        		System.err.println(countLines);
        		System.err.println(totalInLines);
        		System.err.println("Listener: Error processing number of Server Broadcast input Topic messages.");
        		printError();
        	}

        	terminated = true;
        	System.out.println("Listener: terminated, closing socket.");
        	socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
			e.printStackTrace();
		}
    }
}
