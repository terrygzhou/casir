package test.edu.rmit.casir.thread.case2;

import org.apache.log4j.Logger;

public class Channel {
	Logger logger = Logger.getLogger(test.edu.rmit.casir.thread.case2.Channel.class);
	boolean valueSet = false;

	public synchronized void request(String msg) {
		while (!valueSet)
			try {
				wait();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		logger.info("send message: " + msg);
		valueSet = true;
		notify();
		// return;
	}

	public synchronized void response() {
		while (valueSet)
			try {
				wait();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		logger.info("receive message");
		valueSet = false;
		notify();
		// return;
	}
}
