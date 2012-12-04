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
	private Server parentServer;
	
	//set by child classes
	protected String fileName;
	protected ArrayList <String> peerServers;
	protected String twoPCCoordinator;
	
	public ServerThread(Server psrv, Socket skt){
		this.socket = skt;
		parentServer = psrv;
		this.peerServers = psrv.getPeerServers();
		this.twoPCCoordinator = psrv.getTwoPCCoordinator();
        try
        {
            // create output first
        	outputStream = new ObjectOutputStream(socket.getOutputStream()); //needs this or it wont work
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
            System.out.println("RECIEVED:" + msg);
            
            switch (msg.getType()) {
            
                case ServerMessage.CLIENT_GET_LEADER:
                	
                	if (parentServer.getPaxosLeaders().size() == 0) {
                		parentServer.setPaxosLeader(true);
	                	for (int i = 0; i < this.peerServers.size(); i++){
		        			//ServerMessage leaderMsg = new ServerMessage(ServerMessage.PAXOS_ADD_LEADER, socket.getLocalAddress().getCanonicalHostName(), socket.getLocalAddress().getCanonicalHostName() );
                            ServerMessage leaderMsg = new ServerMessage(ServerMessage.PAXOS_ADD_LEADER, socket.getInetAddress().getCanonicalHostName() , socket.getInetAddress().getCanonicalHostName() );
			        		sendMessage(this.peerServers.get(i), 3000, leaderMsg);
			        	}
	                	reply(new ServerMessage(ServerMessage.LEADER_RESPONSE, socket.getInetAddress().getCanonicalHostName()  ));

                	} else {
                		
                		reply(new ServerMessage(ServerMessage.LEADER_RESPONSE, parentServer.getPaxosLeaders().get(0) ));

                	}
                		
                	break;
            	    
		        case ServerMessage.CLIENT_READ:
		        	//read the file (set in child class)
		        	ServerMessage readResultsMsg = new ServerMessage(ServerMessage.CLIENT_READ, parentServer.readFile(fileName));
		        	readResultsMsg.setSourceAddress(socket.getInetAddress().getHostName());
		        	sendMessage(socket.getInetAddress().getHostName(), 3003, readResultsMsg);
		        	
		        	break;
		        case ServerMessage.CLIENT_APPEND:
		        	//create a new ballot by incrementing current ballot by 1
		        	if (!parentServer.isPaxosLeader()){
		        		parentServer.setPaxosLeader(true);
		        		
		        		//send
		        	}
		        			        	
		        	parentServer.setCurrentBallotNumber(parentServer.getCurrentBallotNumber()+1);
		        	ServerMessage ballotMsg = new ServerMessage(ServerMessage.PAXOS_PREPARE, msg.getMessage(), socket.getLocalAddress().getHostAddress() );
		        	ballotMsg.setBallotNumber(parentServer.getCurrentBallotNumber());
		        	ballotMsg.setBallotProcID(parentServer.getProcessId());
		        	ballotMsg.setSourceAddress(socket.getInetAddress().getHostName());
	        	
		        	//send to all other stat or grade servers
		        	for (int i = 0; i < this.peerServers.size(); i++){
		        		sendMessage(this.peerServers.get(i), 3000, ballotMsg);
		        	}
		        	break;
		   
		        case ServerMessage.PAXOS_PREPARE:
		 
		        	//contents of the incoming prepare message are ballotnum,processesid.
		        	//if the incoming ballot is newer than my ballot, update my ballot and send an ack, otherwise the incoming
		        	//ballot is old and we can ignore it
		        	// if the ballots are the same, process id will be the tie breaker. 
		        	if (msg.getBallotNumber() > parentServer.getCurrentBallotNumber() || (msg.getBallotNumber() == parentServer.getCurrentBallotNumber() && msg.getBallotProcID() >= parentServer.getProcessId()) ){
		        		parentServer.setCurrentBallotNumber(msg.getBallotNumber());
		        		//send the ack message with the current ballot, the last accepted ballot, the current value.
		        		
		        		ServerMessage ackMessage = new ServerMessage(ServerMessage.PAXOS_ACK, msg.getMessage(), socket.getLocalAddress().getHostName() );
		        		ackMessage.setBallotNumber(parentServer.getCurrentBallotNumber());
		        		ackMessage.setLastAcceptNumber(parentServer.getCurrentAcceptNum());
		        		ackMessage.setLastAcceptVal(parentServer.getAcceptValue());
		        		ackMessage.setSourceAddress(msg.getSourceAddress());
		        		sendMessage(socket.getInetAddress().getHostName(), 3000, ackMessage);
		        	
		        	}
		        	break;
		        	
		        case ServerMessage.PAXOS_ACK:
		        	//contents of the incoming ack message are current ballot, the last accepted ballot, the current value
		        	
		        	Hashtable<Integer,ArrayList<ServerMessage> > hash = parentServer.getMessageHash();
		        	ArrayList<ServerMessage> ballot_msgs = hash.get( msg.getBallotNumber() );
		            //add the incoming message to a collection of responses for this ballot
		        	if (ballot_msgs == null){
		        		ballot_msgs = new ArrayList<ServerMessage>();
		        	}
		            ballot_msgs.add(msg);
		        	hash.put(msg.getBallotNumber(), ballot_msgs);
		        	parentServer.setMessageHash(hash);

		        	//check to see if we have gotten a majority of responses... if not, do nothing
		        	if(ballot_msgs.size() > this.peerServers.size()/2)
		        	{
		        		
        				ServerMessage acceptMsg = new ServerMessage(ServerMessage.PAXOS_ACCEPT, msg.getMessage() ,socket.getLocalAddress().getHostAddress() );
        				acceptMsg.setBallotNumber(parentServer.getCurrentBallotNumber());
        				acceptMsg.setSourceAddress(msg.getSourceAddress());
        				sendMessage(this.twoPCCoordinator, 3000, acceptMsg);
		        	
		        	}
		        	
		        	break;
		        	
		        case ServerMessage.PAXOS_ACCEPT:
		        	
		        	parentServer.setPaxosLeaderResponseCount(parentServer.getPaxosLeaderResponseCount() + 1);
		
		        	
		        	if (parentServer.getPaxosLeaderResponseCount() == parentServer.getPaxosLeaders().size()){
		        		//reset response count for the next query
		        		parentServer.setPaxosLeaderResponseCount(0);
		        		String acceptVal = msg.getMessage(); //for tie breakers
		        		for (int i = 0; i < this.peerServers.size(); i++){
		        			ServerMessage vote2pc = new ServerMessage(ServerMessage.TWOPHASE_VOTE_REQUEST, acceptVal);
		        			vote2pc.setSourceAddress(msg.getSourceAddress());
			        		sendMessage(this.peerServers.get(i), 3000, vote2pc);
		        		}
		        	}
		        	
		        	 break;
		        
		        case ServerMessage.PAXOS_ADD_LEADER:
		        	if (!parentServer.getPaxosLeaders().contains(msg.getMessage())){
		        	    parentServer.addPaxosLeaders(msg.getMessage());
		        	}
		        	break;
		        	
		        case ServerMessage.TWOPHASE_VOTE_REQUEST:
		        	//attempt to write to redo log
		        	try {
		        		parentServer.appendFile("APPEND:"+msg.getMessage(), "REDO.log");		        		
		        	} catch (IOException e){
		        		//reply no
		        		ServerMessage replyNo = new ServerMessage(ServerMessage.TWOPHASE_VOTE_NO, msg.getMessage());
		        		replyNo.setSourceAddress(msg.getSourceAddress());
		        		sendMessage(this.twoPCCoordinator, 3000, replyNo);
		        		break;
		        	}
		        	
		        	//reply yes
		        	ServerMessage replyYes = new ServerMessage(ServerMessage.TWOPHASE_VOTE_YES, msg.getMessage());
		        	replyYes.setSourceAddress(msg.getSourceAddress());
	        		sendMessage(this.twoPCCoordinator, 3000, replyYes);
	        		break;
		        	
		        case ServerMessage.TWOPHASE_VOTE_YES:
		        	parentServer.setPaxosLeaderResponseCount(parentServer.getPaxosLeaderResponseCount() + 1);
		
		        	
		        	if (parentServer.getPaxosLeaderResponseCount() == this.peerServers.size()){
		        		//reset response count for the next query
		        		parentServer.setPaxosLeaderResponseCount(0);
		        		String acceptVal = msg.getMessage(); //for tie breakers
		        		for (int i = 0; i < this.peerServers.size(); i++){
		        			ServerMessage commitMsg = new ServerMessage(ServerMessage.TWOPHASE_COMMIT, acceptVal);
		        			commitMsg.setSourceAddress(msg.getSourceAddress());
			        		sendMessage(this.peerServers.get(i), 3000, commitMsg);
		        		}
		        		//tell the client we are writing
		        		sendMessage(msg.getSourceAddress(), 3003, new ServerMessage(ServerMessage.TWOPHASE_COMMIT, "VALUE COMMITED: " + msg.getMessage() ));
		        	}
		        	break;
		        	
		        	//tally yes vote
		        case ServerMessage.TWOPHASE_VOTE_NO:
		        	//send abort
		        	for (int i = 0; i < this.peerServers.size(); i++){
	        			ServerMessage abortMsg = new ServerMessage(ServerMessage.TWOPHASE_ABORT, msg.getMessage());
	        			abortMsg.setSourceAddress(msg.getSourceAddress());
		        		sendMessage(this.peerServers.get(i), 3000, abortMsg);
	        		}
		        	break;
		        	
		        case ServerMessage.TWOPHASE_ABORT:
		        	//cancel the write changes
		        	parentServer.appendFile("ABORT:"+msg.getMessage(), "REDO.log");
		        	//tell client we aborted the write
		        	sendMessage(msg.getSourceAddress(), 3003,new ServerMessage(ServerMessage.TWOPHASE_ABORT, "ABORTED WRITING: " + msg.getMessage()) );
		        	
		        	break;
		        	
		        case ServerMessage.TWOPHASE_COMMIT:
		        	
		        	parentServer.appendFile(msg.getMessage(), fileName);
		        	//write any changes
		        	break;
	        
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
		
		System.out.println("SENDING " + msg + " to Server:" + host + "...");
		
		 try {
		      InetAddress address = InetAddress.getByName(host);
		      System.out.print("      Connecting to Server:"+host+"...");
		      
		      // open socket, then input and output streams to it
		      Socket socket = new Socket(address,port);
		      ObjectOutputStream to_server = new ObjectOutputStream(socket.getOutputStream());
		      System.out.print("Connected");
		      
		      // send command to server, then read and print lines until
		      // the server closes the connection
		      
		      to_server.writeObject(msg); to_server.flush();
		      System.out.println("....SENT");
		 } catch (IOException e){
			 System.out.println("      ERROR: Server failed sending message:" + e.getMessage());
		 }
	}
	
	private void reply(ServerMessage msg){
		
		System.out.println("REPLYING " + msg + " to Server:" + socket.getInetAddress().getHostAddress() + "...");
		
		 try {
		      outputStream.writeObject(msg); outputStream.flush();
		      System.out.println("....SENT");
		 } catch (IOException e){
			 System.out.println("      ERROR: Server failed sending message:" + e.getMessage());
		 }
	}
	
	
}
