//  usage:
//    javac Client.java
//    java Client <host> <APPEND (or READ)>

import java.io.*;
import java.net.*;
import java.util.Scanner;



public class Client {
	
	private static String command;
	private static int whichStatServer = 0;
	private static int whichGradeServer = 0;
	
  public static void main(String[] args) {

      
      processArgs(args);
      
      
      String grades_host = Server.GRADE_SERVERS.get(whichGradeServer);
      String stats_host = Server.STAT_SERVERS.get(whichStatServer);
      int port = 3000;
      
      
      ServerMessage grade_msg = new ServerMessage();
      ServerMessage stat_msg = new ServerMessage();
      ServerMessage prepare = new ServerMessage();
      prepare.setType(ServerMessage.CLIENT_GET_LEADER);
      grade_msg.setSourceAddress(Server.getIP());
      stat_msg.setSourceAddress(Server.getIP());
      
      if(command.equals("APPEND")){
    	  grade_msg.setType(ServerMessage.CLIENT_APPEND);
    	  stat_msg.setType(ServerMessage.CLIENT_APPEND);
      } else if (command.equals("READ")){
    	  grade_msg.setType(ServerMessage.CLIENT_READ);
    	  stat_msg.setType(ServerMessage.CLIENT_READ);
      }
       
      if (!(command.equals("APPEND") || command.equals("READ"))) {
        System.out.println("command should be 'APPEND' or 'READ'");
        System.exit(1);
      }
      int[] grades = new int[10];
      double[] stats = new double[3];
      
      if(command.equals("APPEND")) {
        System.out.println("Reading in numbers");
        String gradeString = num(grades);
        String statsString = calc(grades, stats);
        grade_msg.setMessage(gradeString);
        stat_msg.setMessage(statsString);
      }
      
      
      
      
    //initiate contact with a grade server and get back the grade paxos leader
      ServerMessage line = sendMessage(grades_host,port,prepare, true);
      if(line != null) {
          grades_host = line.getMessage();
          System.out.println("GRADES PAXOS LEADER IS: " + grades_host);     
        }
      
      //initiate contact with the stat server and get back the stat paxos leader
      line = sendMessage(stats_host,port,prepare, true);
      if(line != null) {
          stats_host = line.getMessage();
          System.out.println("STATS PAXOS LEADER IS: " + stats_host);     
        }
      
      if (command.equals("APPEND")) {
      //send the grades and stats to their appropriate paxos leaders
      sendMessage(grades_host,port,grade_msg, false);     
      sendMessage(stats_host,port,stat_msg, false);
      
      //listen on port 3003 for results of read or append.
      try {
          ServerSocket socket = new ServerSocket(3003);
          
          int i = 0;
          //only expecting 2 responses
          while (i < 2) {
              
              
              ClientThread t = new ClientThread(socket.accept());
              t.run();
              i++;
                            
          }
      }
      catch (Exception e) {    // report any exceptions
        System.err.println(e);
      }
      
      }
      else if (command.equals("READ")){
    	//initiate contact with a grade server and get back the grade paxos leader
          ServerMessage readresults = sendMessage(grades_host,port,grade_msg, true);
          if(readresults != null) {
              System.out.println("READING:" + readresults.getMessage());     
            }
          readresults = sendMessage(stats_host,port,stat_msg, true);
          if(readresults != null) {
        	  System.out.println("READING:" + readresults.getMessage());  
            }
      }
    

  }
  
  
  public static ServerMessage sendMessage(String host_addr, int port, ServerMessage myMsg, boolean expectReply) {
	  try {
	      InetAddress address = InetAddress.getByName(host_addr);
	      System.out.print("Connecting to " + host_addr + "...");
	      
	      // open socket, then input and output streams to it

	      Socket socket = new Socket(address, port);
	      ObjectInputStream from_server = new ObjectInputStream(socket.getInputStream());
	      ObjectOutputStream to_server = new ObjectOutputStream(socket.getOutputStream());
	      System.out.println("Connected");
	      
	      // send command to server, then read and print lines until
	      // the server closes the connection
	      System.out.print("SENDING " + myMsg + " to Server:" + host_addr + "...");
	      to_server.writeObject(myMsg); to_server.flush();
	      System.out.println("SENT");
	      
	      ServerMessage retVal = null;
	      if (expectReply){
	    	  retVal = (ServerMessage) from_server.readObject();
	      }
	      from_server.close();
	      to_server.close();
	      socket.close();
	      return retVal;
	     
	      
	    }
	    catch (Exception e) {    // report any exceptions
	      System.err.println(e);
	      return null;
	    }
	  
  }
  
  
  
  public static String num(int g[]) {
	String retVal = "";
    Scanner input = new Scanner( System.in ); 
    for(int i = 0; i < 10; i++) {
        g[i] = input.nextInt();
        retVal += Integer.toString(g[i]) + " ";
    }
    
    return retVal;
  }
  
  public static String calc(int g[], double s[]) {
    int min = g[0];
    int max = g[0];
    double sum = g[0]; 
    for(int i = 1; i < 10; i++) {
        if(g[i] < min)
            min = g[i];
        if(g[i] > max)
            max = g[i];
        sum += g[i];
    }
    s[0] = min;
    s[1] = max;
    s[2] = sum/10;
    return new String ("" + min + " " + max + " " + s[2]);
  }
  
  /* processArgs
	 * This function handles the arguments being passed to the program from the user.
	 *  @param args  this is a String array of arguments that get passed to the program.  Should use args from main()
	 */
	private static void processArgs(String[] args){
		
		
		String usage = "\nUsage: \n java Client APPEND|READ [-statserver|gradeserver servernumber] [-help] \n \n" +
					   "APPEND              Will prompt to read in values to save on the server then wait for feedback \n \n" +
					   "READ                Will return the values of GRADES and STATS from the servers \n \n" +
					   "-statserver,-s num  Specifies the server num correllating to the server list of stat servers \n" +
					   "-gradeserver,-g num  Specifies the server num correllating to the server list of grade servers \n" +
					   "                    it will default to listening on port 3000. \n \n" +
					   "-help               Prints this usage information. \n \n";
		
		if (args.length == 0) {
			System.out.print("Incorrect number of arguments. Exiting Program. \n " + usage);
			System.exit(1);
		}
		
		command = args[0];

		//process each argument depending on what it is
		for (int i = 0; i < args.length; i++) {		
			try{
				 if (args[i].equals("-statserver") || args[i].equals("-s")) {
					whichStatServer = Integer.parseInt(args[i+1]);
					i++;
				} else if (args[i].equals("-gradeserver") || args[i].equals("-g")) {
					whichGradeServer = Integer.parseInt(args[i+1]);
					i++;
				}
				else if (args[i].equals("-help")) {
					System.out.print(usage);
					System.exit(0);
				} 
			} catch (NumberFormatException e) {
				System.out.print("Grade/Stat server must be a  number Correllating to the server list. Exiting Program. \n ");
				System.exit(1);
			} catch (Exception e) {
				System.out.print("Incorrect number of arguments. Exiting Program. \n " + usage);	
				System.exit(1);
			}
			
		}
		
		
	}
}

