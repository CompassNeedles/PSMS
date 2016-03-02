import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class PSWrite extends Thread {

    public PSWrite(String dataStorePath, String updateTopic, String[] mData) throws FileNotFoundException {
    	if (!mData[1].equals(updateTopic))
    		System.err.println("Writer: Server Broadcast Connection Topic Update Error.");
    	if (mData[4].length() != Integer.parseInt(mData[3]))
    		System.err.printf("Writer: Server Broadcast Message Size Read Error, %d vs. %d.\n",
    				mData[4].length(), Integer.parseInt(mData[3]));
    	
    	System.out.println("WRITE");
    	File f = new File(dataStorePath);
    	File g = new File(f, updateTopic);
    	File h = new File(g, mData[2] + "-" + mData[0]);
    	System.out.println("Writer: Files created:");
    	System.out.println(h.getAbsolutePath());
    	PrintWriter p = new PrintWriter(h);
    	System.out.println("Writer: Files open");
    	p.println(mData[4]);
    	System.out.println("Writer: Files written");
    	p.close();
    	System.out.println("WRITE-OUT");
    }
	
}
