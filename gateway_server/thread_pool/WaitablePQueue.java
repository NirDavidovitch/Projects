package gateway_server.thread_pool;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeoutException;

public class WaitablePQueue<E> {
	private PriorityQueue<E> pq = null;
	private final Object monitorObj = new Object();

	public WaitablePQueue(Comparator<E> comp) {
		pq = new PriorityQueue<>(comp);
	}

	public WaitablePQueue() {
		pq = new PriorityQueue<>();
	}

	public boolean enqueue(E e) {
		//change to exception
		boolean toReturn = false;

		synchronized (monitorObj) {
			toReturn = pq.add(e);
			monitorObj.notify();
		}

		return toReturn;
	}

	public E dequeue() throws InterruptedException {
		E toReturn = null;

		synchronized (monitorObj) {
			while (pq.isEmpty()) {
				monitorObj.wait();
			}
			toReturn = pq.poll();
		}

		return toReturn;
	}

	public E dequeue(long timeout) throws InterruptedException, TimeoutException {
		E toReturn = null;
		LocalDateTime timeAfterTimeOut = null;

		synchronized (monitorObj) {
			timeAfterTimeOut = LocalDateTime.now().plusSeconds(timeout);
			while (pq.isEmpty() && LocalDateTime.now().isBefore(timeAfterTimeOut)) {
				monitorObj.wait(1000 * timeout);
			}

			if (pq.isEmpty()) {
				throw new TimeoutException("time ran out!");
			}
			toReturn = pq.poll();
		}

		return toReturn;
	}


	public boolean remove(Object obj) {
		synchronized (monitorObj) {
			return pq.remove(obj);
		}
	}

	public boolean isEmpty() {
		return pq.isEmpty();
	}
}
