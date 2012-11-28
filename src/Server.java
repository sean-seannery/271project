/**
 * 
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.UUID;

/**
 * @author sam
 *
 */
public class Server {
	
	private int port;
	private boolean isGradeServer;
	private boolean isStatServer;
	private int processID;

	private int currentBallotNumber;	
	private Hashtable<Integer,ArrayList<ServerMessage> > messageHash;

	public static final ArrayList<String> StatServers = new ArrayList<String>(Arrays.asList("megatron.cs.ucsb.edu"));
	public static final ArrayList<String> GradeServers = new ArrayList<String>(Arrays.asList("1.2.3.4",
			"1.2.3.4",
			"1.2.3.4"));
	

	public Server() {
		processID = UUID.randomUUID().hashCode();
		currentBallotNumber = 0;
		port = 3000;
		isGradeServer = false;
		isStatServer = false;
		messageHash = new Hashtable<Integer,ArrayList<ServerMessage> >();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Server myServer = new Server();
		myServer.processArgs(args);
		myServer.start();
	}
	
	
	public void start() {
		System.out.println("Starting Server... ServerID=" + getProcessId());
		ServerSocket socket;
		
	    try {
			socket = new ServerSocket(this.port);
		
			System.out.println("Server listening on port " + port + "....");
			
			while (true) {
			
				Socket connected_socket;
							
				connected_socket = socket.accept();
								
				ServerThread t;
				if (this.isGradeServer) {
					t = new GradeServerThread(this, connected_socket);
					t.run();
				} else if (this.isStatServer) {
					t = new StatServerThread(this, connected_socket);
					t.run();
				}
							
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/* processArgs
	 * This function handles the arguments being passed to the program from the user.
	 *  @param args  this is a String array of arguments that get passed to the program.  Should use args from main()
	 */
	private void processArgs(String[] args){
		
		String usage = "\nUsage: \n java server -statserver|-gradeserver [-p portnumber] [-help] \n \n" +
					   "-statserver         If this server will host STATS.txt then you should use this option \n" +
					   "                    to indicate that.  Either -statfile or -gradefile is required \n \n" +
					   "-gradeserver        If this server will host GRADES.txt then you should use this option \n" +
					   "                    to indicate that.  Either -statfile or -gradefile is required \n \n" +
					   "-p, -port           The port number this server listens on.  If this option is not provided \n" +
					   "                    it will default to listening on port 3000. \n \n" +
					   "-help               Prints this usage information. \n \n";
		
		if (args.length == 0) {
			System.out.print("Incorrect number of arguments. Exiting Program. \n " + usage);
			System.exit(1);
		}
		//process each argument depending on what it is
		for (int i = 0; i < args.length; i++) {		
			try{
				if (args[i].equals("-statserver")) {
					this.isStatServer = true;
				} else if (args[i].equals("-gradeserver")) {
					this.isGradeServer = true;
				} else if (args[i].equals("-port") || args[i].equals("-p")) {
					this.port = Integer.parseInt(args[i+1]);
					i++;
				} else if (args[i].equals("-help")) {
					System.out.print(usage);
					System.exit(0);
				} 
			} catch (NumberFormatException e) {
				System.out.print("Port number must be a valid integer (ex. 3000). Exiting Program. \n ");
				System.exit(1);
			} catch (Exception e) {
				System.out.print("Incorrect number of arguments. Exiting Program. \n " + usage);	
				System.exit(1);
			}
			
		}
		//ensure that either -gradeserver or -statserver was specified
		if (!this.isGradeServer && !this.isStatServer) {
			System.out.print("You must specify either the -gradeserver or the -statserver option. Exiting Program. \n " + usage);
			System.exit(1);
		}
		
	}
	
	
	public synchronized void appendFile(String data) {
		String filename = "TEST.txt";
		if (isStatServer){
			filename = "STATS.txt";
		} else if (isGradeServer) {
			filename = "GRADES.txt";
		}
		
		try{
		
			File outfile = new File(filename);
 
    		//if file doesnt exists, then create it
    		if(!outfile.exists()){
    			outfile.createNewFile();
    		}
 
    	    BufferedWriter buffer = new BufferedWriter( new FileWriter(outfile.getName(),true) );
    	    buffer.write(data);
            buffer.close(); 
 
	        System.out.println("Wrote to file");
 
    	}catch(IOException e){
    		System.out.println("Error Writing Local File:");
    		e.printStackTrace();
    	}
	}
	
	
	public Hashtable<Integer,ArrayList<ServerMessage> > getMessageHash() {
		return messageHash;
	}

	public synchronized void setMessageHash(Hashtable<Integer,ArrayList<ServerMessage> > msgh) {
		this.messageHash = msgh;
	}
	
	
	public int getCurrentBallotNumber() {
		return currentBallotNumber;
	}

	public synchronized void setCurrentBallotNumber(int currentBallotNumber) {
		this.currentBallotNumber = currentBallotNumber;		   
	}
	
	public int getProcessId() {
		return processID;
	}
}
