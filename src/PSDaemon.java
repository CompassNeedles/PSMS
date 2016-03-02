import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class PSDaemon extends Thread {
	
    public int portNumber;
    public String myServerHashID = "\t";
    public File dataStore;
    public ConcurrentHashMap<String, ArrayList<Pair<String, Pair<Date, Integer>>>> topicLists;

    /* Constructor: spawn new thread */
    public PSDaemon(int portNumber) {
        super("PSDaemon"); /* Thread group */
        this.portNumber = portNumber;
		topicLists = new ConcurrentHashMap<String, ArrayList<Pair<String, Pair<Date, Integer>>>>();
    }
    
    public void run() {
    	
    	boolean listening = true;
    	System.out.printf("Daemon: Started %s\n", myServerHashID);

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) { 
            while (listening) {
            	
            	/* data stream */
            	System.out.printf("Daemon: Listening %s\n", myServerHashID);

	            PSListen getData = new PSListen(serverSocket.accept(), myServerHashID, dataStore.getAbsolutePath());
	            System.out.printf("Daemon: Accepted Socket request %s\n", myServerHashID);
	            
	            getData.start();
	            /* while the connection is on the data transfer should take place quickly enough (in PSListen)
	             * while writing of files is done in yet another detached thread (PSWrite) */
	            TimeUnit.SECONDS.sleep(1);
	            System.out.printf("Daemon: Waits- %b\n", getData.terminated);
	            while (!getData.terminated) {
	            	TimeUnit.SECONDS.sleep(1);
	            	System.out.printf("Daemon: Waits- %b\n", getData.terminated);
	            }

	            System.out.println("Daemon: Writes,");
	            topicLists.put(getData.updateTopic, getData.toDaemonTemp);
	            
	            System.out.println("Daemon: Out.");
            }
	    } catch (IOException e) {
            System.err.println("Daemon: Error listening on port number " + portNumber + ".");
            System.exit(-1);
        } catch (InterruptedException e) {
            System.err.println("Daemon: Interrupted while waiting on Listener.");
			e.printStackTrace();
		}
    }
}
