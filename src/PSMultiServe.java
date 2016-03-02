import java.net.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.io.*;

public class PSMultiServe extends Thread {
    private Socket socket = null;
    
    private void multiSerExitError(int c) {
    	if (c == -2)
    		System.out.println("Server: Error reading input number of message bytes.");
    	else if (c == -1)
    		System.out.println("Server: Invalid request.");
    	else if (c == 0)
        	System.out.println("Server: Null input error on request.");
    	else if (c < 7)
    		System.out.println("Server: Missing input data.");
    	System.exit(-1);
    }

    /* Constructor: spawn new thread */
    public PSMultiServe(Socket socket) {
        super("PSMultiServe");  /* Thread group */
        this.socket = socket;
    }
    
    public void run() {

        try (
            PrintWriter out =
            	new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in =
            	new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
        	String inputLine = in.readLine();
        	int state = -1;
        	int c = -1;
        	if (inputLine == null || (state = Integer.parseInt(inputLine)) < 0 || state > 3) {
        		multiSerExitError(c);
        	}
        	
        	ArrayList<String> output;
        	String[] inputData = new String[7];
        	/* inputData: userName, hashID, portNumber, topic, date, numLines, contents */
        	for (c = 0; c < 6; c++) {
        		inputLine = in.readLine();
        		if (inputLine == null)
        			multiSerExitError(c);
        		inputData[c] = inputLine;
        	}
        	int r = Integer.parseInt(inputData[(c = 5)]);
        	char[] message = new char[r];
        	c = in.read(message, 0, r);
        	if (r != c)
        		multiSerExitError(-2);
        	inputData[6] = new String(message);
        	
        	PSProtocol psp = new PSProtocol();
            output = psp.processInput(state, inputData);
            
            /* Send response to Client */
            if (output == null)
            	out.println("0");
            else {
            	out.println(Integer.toString(output.size()));
            	for (int o = 0; o < output.size(); ++o)
            		out.println(output.get(o));
            }
            socket.close();

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
