/**
 * 
 */


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author sam
 *
 */
public class Server {
	
	private int port;
	private boolean isGradeServer;
	private boolean isStatServer;
	
	private boolean isPaxosLeader;
	private int currentBallotNumber;
	private int processId;
	
	
	public static final ArrayList<String> StatServers = new ArrayList<String>(Arrays.asList("megatron.cs.ucsb.edu"));
	public static final ArrayList<String> GradeServers = new ArrayList<String>(Arrays.asList("1.2.3.4",
			"1.2.3.4",
			"1.2.3.4"));
	
	Queue<ServerMessage> messageQueue;

	public Server() {
		currentBallotNumber = 0;
		messageQueue = new LinkedList<ServerMessage>();
		port = 3000;
		isGradeServer = false;
		isStatServer = false;
		isPaxosLeader = false;
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Server myServer = new Server();
		myServer.processArgs(args);
		myServer.start();
	}
	
	
	public void start() {
		System.out.println("Starting Server...");
		ServerSocket socket;
		
	    try {
			socket = new ServerSocket(this.port);
		
			System.out.println("Server listening on port " + port + "....");
			
			while (true) {
			
				Socket connected_socket;
				
			
				connected_socket = socket.accept();
			
					
				ServerThread t;
				if (this.isGradeServer) {
					t = new GradeServerThread(connected_socket);
					t.run();
				} else if (this.isStatServer) {
					t = new StatServerThread(connected_socket);
					t.run();
				}
			
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	
}
