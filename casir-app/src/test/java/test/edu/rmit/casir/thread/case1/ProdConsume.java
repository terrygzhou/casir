package test.edu.rmit.casir.thread.case1;

public class ProdConsume {

	public static void main(String args[]) {
		int i = 0;
		while (i < 1) {
			Queue q = new Queue();
			Receiver r=new Receiver(q, i);
			new Sender(q, i);
			// new Receiver(q,1);
			// new Sender(q,1);
			System.out.println("Press Control-C to stop.");
			i++;
		}
	}

}
