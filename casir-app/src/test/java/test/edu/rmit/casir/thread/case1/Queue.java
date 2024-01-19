package test.edu.rmit.casir.thread.case1;

import org.apache.log4j.Logger;

public class Queue {

	int n;
	boolean rec_pay_monitor = false;
	boolean snd_payment_monitor=true;
	boolean order_monitor = true;
	Logger logger = Logger.getLogger(Queue.class);
	
	
	synchronized void sendOrder() {
		while (!order_monitor)
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		logger.info("! order");
		order_monitor = false;
		notify();
	}
	
	synchronized int rec_order() {
		while (!order_monitor)
			try {
				wait();
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		logger.info("?msg" + n);
		order_monitor = false;
		notify();
		return n;
	}
	
	synchronized int rec_pay() {
		while (!rec_pay_monitor)
			try {
				wait();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		logger.info("? payment" + n);
		rec_pay_monitor = false;
		notify();
		return n;
	}


	

	synchronized void processOrder() {
		while(order_monitor)
			try {
				wait();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		logger.info("process order");
		order_monitor=true;
		notify();
	}
	

	synchronized void sendPayment(int n) {
		while (!snd_payment_monitor) {
			try {
				wait();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.n = n;
		snd_payment_monitor = true;
		logger.info("send payment" + n);
		notify();
	}
	
	
}
