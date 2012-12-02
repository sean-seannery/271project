/**
 * 
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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


	//these variables need to be synchronized
	private int currentBallotNumber;	
	private String acceptValue;
	private Hashtable<Integer,ArrayList<ServerMessage> > messageHash;
	private int currentAcceptNum;  //this containts the last accepted ballot number
	 //this contains the current value known to this server (what was last accepted)
	private boolean isPaxosLeader;
	private ArrayList<String> paxosLeaders;
	private int paxosLeaderResponseCount;
	private ServerSocket socket;

	public static final ArrayList<String> StatServers = new ArrayList<String>(Arrays.asList("megatron.cs.ucsb.edu", "beavis.cs.ucsb.edu"));
	public static final ArrayList<String> GradeServers = new ArrayList<String>(Arrays.asList("1.2.3.4",
			"1.2.3.4",
			"1.2.3.4"));
	public static final String stat2PCLeader = "beavis.cs.ucsb.edu";
	public static final String grade2PCLeader = "1.2.3.4";
	

	public Server() {
		paxosLeaderResponseCount = 0;
		paxosLeaders = new ArrayList<String>();
		currentAcceptNum = 0;
		acceptValue = null;
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
	
	
	/**
	 *  Start the server
	 */
	public void start() {
		System.out.println("Starting Server... ServerID=" + getProcessId());
		
		
	    try {
			this.socket = new ServerSocket(this.port);
			
			System.out.println("Server listening on " + socket.getInetAddress().getHostAddress() + ":" + port );
			System.out.println("=============================================");
					
			while (!Thread.interrupted()) {
			
				Socket connected_socket = socket.accept();
				
				ServerThread t;
				if (this.isGradeServer) {
					t = new GradeServerThread(this, connected_socket);
					t.run();
				} else if (this.isStatServer) {
					t = new StatServerThread(this, connected_socket);
					t.run();
				}
							
			}
			
			stop();

		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void stop(){
		System.out.println("=============================================");
		System.out.println(" \n \n Shutting Down Server....");
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
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
	
	
	/**
	 * @param data string that you want to append to a file
	 * @param filename file you want to write to.
	 * @throws IOException
	 */
	public synchronized void appendFile(String data, String filename) throws IOException{
			
		try{
		
			File outfile = new File(filename);
 
    		//if file doesnt exists, then create it
    		if(!outfile.exists()){
    			outfile.createNewFile();
    		}
 
    	    BufferedWriter buffer = new BufferedWriter( new FileWriter(outfile.getName(),true) );
    	    buffer.write(data + "\n");
            buffer.close(); 
 
	        System.out.println("WROTE TO FILE " + filename + ": " + data);
 
    	}catch(IOException e){
    		System.out.println("Error Writing Local File:");
    		e.printStackTrace();
    		throw new IOException(e);
    	}
	}
	
	/**
	 * @param filename 
	 * @return contents of the file name specified
	 */
	public String readFile( String filename) {
		String contents = "";
		try{
			File outfile = new File(filename);
    	    BufferedReader buffer = new BufferedReader( new FileReader(outfile) );    	    
    	
    	    String line = buffer.readLine();
    	    while (line != null ) {
    	    	contents += line;
    	    	line = buffer.readLine();	    	
    	    }
    	    
            buffer.close(); 
 
    	}catch(IOException e){
    		System.out.println("Error Writing Local File:");
    		e.printStackTrace();
    	}
		 return contents;
	}
	
	/*********************** GETTERS AND SETTERS **************************/
	
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

	public synchronized String getAcceptValue() {
		return acceptValue;
	}

	public synchronized void setAcceptValue(String acceptValue) {
		this.acceptValue = acceptValue;
	}

	public synchronized int getCurrentAcceptNum() {
		return currentAcceptNum;
	}

	public synchronized void setCurrentAcceptNum(int currentAcceptNum) {
		this.currentAcceptNum = currentAcceptNum;
	}

	public synchronized boolean isPaxosLeader() {
		return isPaxosLeader;
	}

	public synchronized void setPaxosLeader(boolean isPaxosLeader) {
		this.isPaxosLeader = isPaxosLeader;
	}

	public synchronized int getPaxosLeaderResponseCount() {
		return paxosLeaderResponseCount;
	}

	public synchronized void setPaxosLeaderResponseCount(int paxosLeaderCount) {
		this.paxosLeaderResponseCount = paxosLeaderCount;
	}

	public synchronized ArrayList<String> getPaxosLeaders() {
		return paxosLeaders;
	}

	public boolean isGradeServer() {
		return isGradeServer;
	}

	public boolean isStatServer() {
		return isStatServer;
	}

	public synchronized void addPaxosLeaders(String newleader) {
		this.paxosLeaders.add(newleader);
	}
	
}


