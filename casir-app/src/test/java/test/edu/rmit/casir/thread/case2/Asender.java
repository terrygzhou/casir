package test.edu.rmit.casir.thread.case2;

public class Asender implements Runnable {
	private Channel port;

	public Asender(Channel c) {
		this.port = c;
		new Thread(this, "Asender").start();

	}

	public void run() {
		int ei = 0;
		while (true) {
			port.request(new Integer(ei) + "");
			ei = (ei + 1) % 10;
		}
	}
}
