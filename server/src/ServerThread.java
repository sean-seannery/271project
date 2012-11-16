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
	
}
