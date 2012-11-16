//  usage:
//    javac Client.java
//    java Client <host> <APPEND (or READ)>

import java.io.*;
import java.net.*;
import java.util.Scanner;

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
      int[] grades = new int[10];
      double[] stats = new double[3];
      
      if(command.equals("APPEND")) {
        System.out.println("Reading in numbers");
        num(grades);
        calc(grades, stats);
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
  public static void num(int g[]) {
    Scanner input = new Scanner( System.in ); 
    for(int i = 0; i < 10; i++) {
        g[i] = input.nextInt();
    }
  }
  public static void calc(int g[], double s[]) {
    int min = g[0];
    int max = g[0];
    double sum = g[0];
    Scanner input = new Scanner( System.in ); 
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
  }
}