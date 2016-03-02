import java.util.Date;
import java.util.UUID;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class MultiCliTester {
	private static final int SUBSCRIBE = 0;
	private static final int CREATE = 1;
	private static final int PUBLISH = 2;
	private static final int REQUEST = 3;
	
	private static final int base = 8090;
	
	public static void main(String[] args) throws UnknownHostException {
		int serverNumber = 4;
		int daemonNumber = 00;
		String ID1 = UUID.randomUUID().toString();
		String ID2 = UUID.randomUUID().toString();
		String ID3 = UUID.randomUUID().toString();

		MultiCli client1 = new MultiCli(base + daemonNumber + 1, ID1);
		MultiCli client2 = new MultiCli(base + daemonNumber + 2, ID2);
		MultiCli client3 = new MultiCli(base + daemonNumber + 3, ID3);
		
		String thisSessionIP = "127.0.0.1";
		
		String[] data = new String[8];

		String requestType = Integer.toString(PUBLISH);
		String userName = thisSessionIP;
		String hashID = "\t";
		String portNumD = Integer.toString(base + daemonNumber + 1);
		String topic = "first messages";
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMddHHmmssSSSZ");
		String date = simpleDate.format(new Date());
		String contents = "Hello World!" + Character.toString('\n') + Character.toString('\n') + Character.toString('\n') 
				+ "Hello World!" + Character.toString('\n') + Character.toString('\n');
		String bytes = Integer.toString(contents.length());
		
		data[0] = requestType;
		data[1] = userName;
		data[2] = hashID;
		data[3] = portNumD;
		data[4] = topic;
		data[5] = date;
		data[6] = bytes;
		data[7] = contents;
		
		System.out.println("MultiCliTester, TestSuite:");
		System.out.println("MultiCliTester: 8091 PUB T1 (BC: 8091[1]).");
		System.out.println("MultiCliTester: 8092 SUB T1.");
		System.out.println("MultiCliTester: 8092 PUB T1 (BC: 8091[1], 8092[2]).");
		System.out.println("MultiCliTester: 8092 SUB T1.");
		System.out.println("MultiCliTester: 8093 SUB T2.");
		System.out.println("MultiCliTester: 8093 SUB T1.");
		System.out.println("MultiCliTester: 8093 PUL T1.");
		System.out.println("MultiCliTester: ExpectedMsgCounts: 8091: 2, 8092: 2, 8093: 2.");
		
		try {
			System.out.printf("Client: requesting PUB lo %d %s\n", base + serverNumber, data[3]);
			hashID = client1.requestService(thisSessionIP, base + serverNumber, data);
			System.out.println("MultiCliTester: returned.");
			System.out.println(hashID);
			
			data[0] = Integer.toString(SUBSCRIBE);
			data[2] = "\t";
			data[3] = Integer.toString(base + daemonNumber + 2);
			data[5] = simpleDate.format(new Date());
			
			System.out.printf("Client: requesting SUB lo %d %s\n", base + serverNumber, data[3]);
			hashID = client2.requestService(thisSessionIP, base + serverNumber, data);
			System.out.println("MultiCliTester: returned.");
			System.out.println(hashID);
			
			data[0] = Integer.toString(PUBLISH);
			data[2] = hashID;
			data[5] = simpleDate.format(new Date());
			
			System.out.printf("Client: requesting PUB lo %d %s\n", base + serverNumber, data[3]);
			hashID = client2.requestService(thisSessionIP, base + serverNumber, data);
			System.out.println("MultiCliTester: returned.");
			System.out.println(hashID);
			
			data[0] = Integer.toString(CREATE);
			data[2] = "\t";
			data[3] = Integer.toString(base + daemonNumber + 3);
			data[4] = "second messages";
			data[5] = simpleDate.format(new Date());
			
			System.out.printf("Client: requesting SUB new Topic lo %d %s\n", base + serverNumber, data[3]);
			hashID = client3.requestService(thisSessionIP, base + serverNumber, data);
			System.out.println("MultiCliTester: returned.");
			System.out.println(hashID);
			
			data[0] = Integer.toString(SUBSCRIBE);
			data[2] = hashID;
			data[4] = "first messages";
			data[5] = simpleDate.format(new Date());
			
			System.out.printf("Client: requesting SUB Topic lo %d %s\n", base + serverNumber, data[3]);
			hashID = client3.requestService(thisSessionIP, base + serverNumber, data);
			System.out.println("MultiCliTester: returned.");
			System.out.println(hashID);
			
			data[0] = Integer.toString(REQUEST);
			data[5] = simpleDate.format(new Date());
			
			System.out.printf("Client: requesting PULL lo %d %s\n", base + serverNumber, data[3]);
			hashID = client3.requestService(thisSessionIP, base + serverNumber, data);
			System.out.println("MultiCliTester: returned.");
			System.out.println(hashID);
			
		} catch (SecurityException | ParseException | IOException e) {
			System.err.println("Client: Error sending request.");
			e.printStackTrace();
		}
	}
}
