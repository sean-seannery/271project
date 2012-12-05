import java.io.ObjectInputStream;
import java.net.Socket;

//creates a new thread to listen for responses.  This needs to be a thread in case a message returns concurrently with another.
public class ClientThread extends Thread {
	  Socket mySocket;
	  public ClientThread(Socket skt){	  mySocket = skt;	  }
	  
	  public void run() {
		  try {
			  ObjectInputStream from_server = new ObjectInputStream(mySocket.getInputStream());
	          ServerMessage line = (ServerMessage) from_server.readObject();
	          if(line != null) {
	              System.out.println("\n \n Message: " + line.getMessage() + " Type: " + line.getTypeName() + " from Server" + mySocket.getInetAddress().getHostAddress());     
	          }
	          mySocket.close();     
		  } catch (Exception e) {			  e.printStackTrace();		  }
	  }
}