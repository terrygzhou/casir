package test.edu.rmit.casir.thread.case2;

public class Areceiver implements Runnable {

	private Channel port;
	
	public Areceiver(Channel c) {
		this.port=c;
		new Thread(this, "Areceiver").start();

	}

	public void run() {
		while (true) {
			port.response();
		}
	}

}
