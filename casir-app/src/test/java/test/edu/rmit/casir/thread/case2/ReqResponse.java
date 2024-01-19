package test.edu.rmit.casir.thread.case2;

public class ReqResponse {

	public static void main(String args[]) {
		Channel c = new Channel();
		new Asender(c);
		new Areceiver(c);
	}

}
