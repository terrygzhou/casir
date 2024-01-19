package test.edu.rmit.casir.thread.case1;

import org.apache.log4j.Logger;

public class Sender implements Runnable {

	Queue q;
	int index;
	Logger logger=Logger.getLogger(Sender.class);

	Sender(Queue q, int i) {
		this.q = q;
		this.index=i;
		new Thread(this, "Sender"+this.index).start();
	}

	public void run() {
		int i = 0;
		while (i<3) {
			logger.info("Sender.Start");
			q.sendOrder();
			q.processOrder();
			q.sendPayment(i++);
			logger.info("Sender.END");
		}
	}
}
