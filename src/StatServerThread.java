import java.net.Socket;


public class StatServerThread extends ServerThread {

	public StatServerThread(Server psrv, Socket skt) {
		super(psrv, skt);
		this.fileName = "STATS.txt";
	}

}
