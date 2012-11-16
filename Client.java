//  usage:
//    javac Client.java
//    java Client <host> <APPEND (or READ)>

import java.io.*;
import java.net.*;

public class Client {
  public static void main(String[] args) {
      if (args.length != 2) {
        System.out.println("need 2 arguments");
        System.exit(1);
      }
      String host = args[0];
      int port = 9999;
      String command = args[1];
      
      if (!(command.equals("APPEND") || command.equals("READ"))) {
        System.out.println("command should be 'APPEND' or 'READ'");
        System.exit(1);
      }
    try {
      InetAddress address = InetAddress.getByName(host);
     
      // open socket, then input and output streams to it
      Socket socket = new Socket(address,port);
      BufferedReader from_server = 
        new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter to_server = new PrintWriter(socket.getOutputStream());

      // send command to server, then read and print lines until
      // the server closes the connection
      to_server.println(command); to_server.flush();
      String line;
      while ((line = from_server.readLine()) != null) {
        System.out.println(line);
      }
    }
    catch (Exception e) {    // report any exceptions
      System.err.println(e);
    }
  }
}