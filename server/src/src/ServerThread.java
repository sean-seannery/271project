import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public abstract class ServerThread extends Thread{

	Socket socket;
	ObjectInputStream inputStream;
	ObjectOutputStream outputStream;
	int id;
	
	
	public ServerThread(Socket skt){
		this.socket = skt;
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
		        	//initiate 2pc protocol
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
		        case ServerMessage.PAXOS_PREPARE:
		        	//do something
		        case ServerMessage.PAXOS_ACK:
		        	//do something
		        case ServerMessage.PAXOS_DECIDE:
		        	//do something
		        case ServerMessage.PAXOS_ACCEPT:
		        	//do something
	        
	        
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
	
}
