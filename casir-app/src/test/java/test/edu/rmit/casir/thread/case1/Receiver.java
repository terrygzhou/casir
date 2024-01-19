package test.edu.rmit.casir.thread.case1;

import org.apache.log4j.Logger;

public class Receiver implements Runnable {

	Queue q;
	Logger logger = Logger.getLogger(Receiver.class);
	int index;

	Receiver(Queue q, int i) {
		this.index = i;
		this.q = q;
		new Thread(this, "Receiver" + this.index).start();
	}

	public void run() {
		while (true) {
			logger.info("receiver.Start");
			q.rec_order();
			q.processOrder();
			q.rec_pay();
			logger.info("receiver.END");
			
		}
	}

}
