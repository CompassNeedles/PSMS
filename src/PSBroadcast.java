import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;

public class PSBroadcast extends Thread {
	
	private static ArrayList<ArrayList<String>> outProcess;

    /* Constructor: spawn new thread */
    public PSBroadcast(ArrayList<ArrayList<String>> inProcess) {
        super("PSBroadcast");  /* Thread group */
        PSBroadcast.outProcess = inProcess;
    }
    
    public void run() {

    	System.out.println("-------------------------");
    	System.out.println("Server: BROADCASTING (in)");
		for (ArrayList<String> a: outProcess) {
			for (String o: a)
				System.out.println(o);
        	System.out.println("-------");
		}
    	System.out.println("-------------------------");
    	
    	if (outProcess == null)
    		return;

    	for (ArrayList<String> a: outProcess) {
    		String uName = a.get(0); /* hostName */
    		a.remove(0);
    		String topic = a.get(0); /* communicate to host */
    		a.remove(0);
    		int portNumber = Integer.parseInt(a.get(0));
    		a.remove(0);
    		System.out.printf("Server: BROADCASTING portNumber(%d)\n", portNumber);
    		String uID = a.get(0); /* use to verify account */
    		a.remove(0);
    		
    		try (
    				Socket socket = new Socket(uName, portNumber);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
    		) {
    			System.out.printf("BROADCASTING portNumber(%d)\n", portNumber);
    			out.println(4 + a.size()/2*5); /* Predicted: */
    			
    			out.println(uID); /* Validates broadcast topic. Next validates number of messages. */
    			out.println(topic);
    			
    			/* Service each uID: get file from path in a, etc. */
    			PSProtocol ps = new PSProtocol();
    			PSProtocol.streamInBroadcastInput(a);
    			
    			String[] IDTopic = { uID, topic };
    			ArrayList<String> output = ps.processInput(PSProtocol.BROADCASTING, IDTopic);

    			System.out.println("--------------------------");
    			System.out.println("Server: BROADCASTING (out)");
    			for (String o: output) {
    				System.out.println(o);
    				out.println(o);
    			}
    			System.out.println("--------------------------");
    			socket.close();
    			
    		} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
    	}
    }
}
