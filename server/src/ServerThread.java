import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;


public abstract class ServerThread extends Thread{

	private Socket socket;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private int currentBallotNumber;
	private int currentAcceptNum;  //this containts the last accepted ballot number
	private String acceptValue; //this contains the current value known to this server (what was last accepted)
	private Server parentServer;
	
	public ServerThread(Server psrv, Socket skt){
		this.socket = skt;
		this.acceptValue = null;
		currentBallotNumber = 0;
		currentAcceptNum = 0;
		parentServer = psrv;
        try
        {
            // create output first
        	outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream  = new ObjectInputStream(socket.getInputStream());

        }
        catch (IOException e) {
            System.out.println("Error Creating Streams: " + e);
            return;

        }
		
	}
	
	public void run()
	{
		//check for message client read/append
		// if read, return values
		// if append start 2pc protocol then paxos protocol
		
		ServerMessage msg;
		
		try {

            msg = (ServerMessage) inputStream.readObject();
            System.out.println("RECIEVED:" + msg.getMessage());
            
            switch (msg.getType()) {
		        case ServerMessage.CLIENT_READ:
		        	//read the file
		        case ServerMessage.CLIENT_APPEND:
		        	//send prepare ballot
		        	currentBallotNumber++;
		        	ServerMessage ballot = new ServerMessage(ServerMessage.PAXOS_PREPARE, currentBallotNumber + "," + this.getId(), socket.getLocalAddress().getHostAddress() );
		        	
		        	System.out.println("My address:" + socket.getLocalAddress().getHostAddress() );

		        	//send to other stat or grade servers
		        	for (int i = 0; i < Server.StatServers.size(); i++){
		        		sendMessage(Server.StatServers.get(i), 3000, ballot);
		        	}
		        	break;
		   
		        case ServerMessage.PAXOS_PREPARE:
		        	//do something
		        	//contents of the message are ballotnum,processesid.
		        	int proposedBallot = Integer.parseInt(msg.getMessage().split(",")[0]);
		        	
		        	if (proposedBallot >= currentBallotNumber){
		        		this.currentBallotNumber = proposedBallot;
		        		ServerMessage ackMessage = new ServerMessage(ServerMessage.PAXOS_ACK, currentBallotNumber + ","+ currentAcceptNum + "," + this.acceptValue ,socket.getLocalAddress().getHostAddress() );
		        		sendMessage(msg.getSourceAddress(), 3000, ackMessage);
		        		System.out.println(msg.getSourceAddress());
		        	}
		        	break;
		        	
		        case ServerMessage.PAXOS_ACK:
		        	//do something
		        	proposedBallot = Integer.parseInt(msg.getMessage().split(",")[0]);
		        	Hashtable<Integer, ArrayList> hash = parentServer.getMessageHash();
		        	ArrayList temp = hash.get(proposedBallot);
		            temp.add(msg);
		        	hash.put(proposedBallot, temp);
		        	parentServer.setMessageHash(hash);

		        	if(temp.size() > Server.StatServers.size()/2)
		        	{
		        		boolean all_null = true;
		        		int highest_accept_num = -1;
		        		String highest_accept_val = null;
		        		for (int i = 0; i < temp.size(); i++){
		        			ServerMessage temp_msg = (ServerMessage)temp.get(i);
		        	     	int proposedacceptnum = Integer.parseInt(temp_msg.getMessage().split(",")[1]);
		        	     	String proposedVal = temp_msg.getMessage().split(",")[2];
		        			
		        	     	if (proposedacceptnum > highest_accept_num ) {
		        	     		
		        	     		highest_accept_num = proposedacceptnum;
		        	     		highest_accept_val = proposedVal;
		        	     	}
		        	     	
				        	if (!proposedVal.equals(null)){
				        		all_null = false;
				        	}
		        		}
		        		
		        		if (all_null) {
		        			//write line of grades / stats into file
		        			
		        			for (int i = 0; i < Server.StatServers.size(); i++){
		        				ServerMessage acceptMsg = new ServerMessage(ServerMessage.PAXOS_ACCEPT, currentBallotNumber +","+ this.acceptValue ,socket.getLocalAddress().getHostAddress() );
				        		sendMessage(Server.StatServers.get(i), 3000, acceptMsg);
				        	}
				        	
		        			
		        		} else {
		        			for (int i = 0; i < Server.StatServers.size(); i++){
		        				ServerMessage acceptMsg = new ServerMessage(ServerMessage.PAXOS_ACCEPT, currentBallotNumber +","+ highest_accept_val ,socket.getLocalAddress().getHostAddress() );
				        		sendMessage(Server.StatServers.get(i), 3000, acceptMsg);
				        	}
		        		}
		        	}
		        	
		        	break;
		        	
		        case ServerMessage.PAXOS_DECIDE:
		        	//do something
		        case ServerMessage.PAXOS_ACCEPT:
		        	//do something
		         	
		        case ServerMessage.TWOPHASE_VOTE_REQUEST:
		        	//reply yes or no
		        case ServerMessage.TWOPHASE_VOTE_YES:
		        	//tally yes vote
		        case ServerMessage.TWOPHASE_VOTE_NO:
		        	//send abort
		        case ServerMessage.TWOPHASE_ABORT:
		        	//cancel the write changes
		        case ServerMessage.TWOPHASE_COMMIT:
		        	//write any changes
	        
	        }
        }
        catch (IOException e) {
            System.out.println(" Exception reading Streams: " + e);
            System.exit(1);             			
        }		
        catch(ClassNotFoundException ex) {
        	//this shouldnt be a problem, only ServerMessages should be sent.
			System.exit(1);
        }
		
       
		
	}
	
	private void sendMessage(String host, int port, ServerMessage msg){
		
		 try {
		      InetAddress address = InetAddress.getByName(host);
		      System.out.print("Connecting to Server...");
		      
		      // open socket, then input and output streams to it
		      Socket socket = new Socket(address,port);
		      ObjectOutputStream to_server = new ObjectOutputStream(socket.getOutputStream());
		      System.out.println("Connected");
		      
		      // send command to server, then read and print lines until
		      // the server closes the connection
		      System.out.print("Sending Message to Server...");
		      to_server.writeObject(msg); to_server.flush();
		      System.out.println("Sent: " + msg);
		 } catch (IOException e){
			 System.out.println("Server failed sending message:" + e.getMessage());
		 }
	}
	
}
