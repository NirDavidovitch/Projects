/********************************
*	Developer:	Nir Davidovitch	*
*	Reviewer:	Daniel			*
*	Date:		24.07.2023		*
********************************/

package gateway_server.thread_pool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool implements Executor {
	/*	public for test reasons	*/
	public final ConcurrentLinkedQueue<Thread> threadPool;
	private final WaitablePQueue<Task<?>> waitablePQueue = new WaitablePQueue<>();
	private final AtomicBoolean isShutDown = new AtomicBoolean(false);
	private final Semaphore semPause = new Semaphore(0);
	private boolean isPaused = false;
	private int numOfThreads = 0;

	public ThreadPool(int numOfThreads) {
		this.numOfThreads = numOfThreads;
		threadPool = new ConcurrentLinkedQueue<>();

		for (int i = 0; i < numOfThreads; ++i) {
			Thread thread = new TaskManager();
			threadPool.add(thread);
			thread.start();
		}
	}

	private class Task<T> implements Runnable, Comparable<Task<T>> {
		private Callable<T> callable = null;
		private final Lock doneLock = new ReentrantLock();
		private final Condition doneCond = doneLock.newCondition();
		private final ThreadPoolFuture myFuture = new ThreadPoolFuture();
		private int priority = 0;
		private Thread taskThread = null;
		private Exception myException = null;
		private boolean isDone = false;
		private boolean isCancelled = false;

		private Task(Callable<T> callable, int priority) {
			this.callable = callable;
			this.priority = priority;
		}

		@Override
		public int compareTo(Task o) {
			return o.priority - this.priority;
		}

		@Override
		public void run() {
			taskThread = Thread.currentThread();
			try {
				myFuture.returnValue = callable.call();
			} catch (Exception e) {
				this.myException = e;
			}

			doneLock.lock();
			this.isDone = true;
			doneCond.signal();
			doneLock.unlock();
		}
		private class ThreadPoolFuture implements Future<T> {
			T returnValue = null;

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				if (waitablePQueue.remove(Task.this)) {
					isCancelled = true;
				} else {
					doneLock.lock();
					if (mayInterruptIfRunning && !isDone() && !isCancelled() && (taskThread != null)) {
						isCancelled = true;
						taskThread.interrupt();
					}
					doneLock.unlock();
				}

				return this.isCancelled();
			}

			@Override
			public boolean isCancelled() {
				return Task.this.isCancelled;
			}

			@Override
			public boolean isDone() {
				return Task.this.isDone;
			}

			@Override
			public T get() throws InterruptedException, ExecutionException {
				if (this.isCancelled()) {
					throw new CancellationException();
				}

				doneLock.lock();
				while (!isDone) {
					doneCond.await();
				}
				doneLock.unlock();

				if (null != myException) {
					throw new ExecutionException(myException);
				}

				return myFuture.returnValue;
			}
			@Override
			public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				if (this.isCancelled()) {
					throw new CancellationException();
				}

				timeout = unit.toNanos(timeout);

				doneLock.lock();
				while (!isDone && timeout > 0) {
					timeout = doneCond.awaitNanos(timeout);
				}
				doneLock.unlock();

				if (timeout <= 0) {
					throw new TimeoutException("time out to wait in get task");
				}

				if (null != myException) {
					throw new ExecutionException(myException);
				}

				return myFuture.returnValue;
			}

		}
	}
	private class PauseTask implements Runnable {
		@Override
		public void run() {
			try {
				semPause.acquire();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	private class TaskManager extends Thread {
		boolean isEnabled = true;

		@Override
		public void run() {
			while (isEnabled) {
				Thread.interrupted();

				try {
					waitablePQueue.dequeue().run();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			if (!isShutDown.get()) {
				threadPool.remove(Thread.currentThread());
			}
		}
	}

	@Override
	public void execute(Runnable command) {
		submit(command, Priority.DEFAULT);
	}
	public Future<Void> submit(Runnable runnable, Priority priority) {
		return submit(runnable, priority, null);
	}
	public <T> Future<T> submit(Runnable runnable, Priority priority, T value) {
		Callable<T> callable = Executors.callable(runnable, value);

		return submit(callable, priority);
	}
	public <T> Future<T> submit(Callable<T> callable) {
		return submit(callable, Priority.DEFAULT);
	}
	public <T> Future<T> submit(Callable<T> callable, Priority priority) {
		if (isShutDown.get()) {
			throw new RejectedExecutionException("Task submitted after Shut Down");
		}

		if (null == callable) {
			throw new NullPointerException("sent null as a Callable/Runnable");
		}

		Task<T> newTask = new Task<>(callable, priority.getPriority());

		waitablePQueue.enqueue(newTask);

		return newTask.myFuture;
	}
	public void setNumOfThreads(int newNumOfThreads) {
		if (isPaused) {
			throw new IllegalStateException();
		}

		int res = newNumOfThreads - this.numOfThreads;

		if (res > 0) {
			addThreads(res);
		} else {
			res = -res;
			removeThreads(res, getHighestPriority());
		}

		this.numOfThreads = newNumOfThreads;
	}
	public void pause() {
		if (isPaused) {
			throw new IllegalStateException();
		}

		for (Thread ignore : threadPool) {
			developerSubmit(new PauseTask(), getHighestPriority());
		}
		isPaused = true;
	}
	public void resume() {
		if (!isPaused) {
			throw new IllegalStateException();
		}

		this.semPause.release(this.numOfThreads);
		isPaused = false;
	}
	public void shutDown() {
		if (isPaused) {
			throw new IllegalStateException();
		}

		this.isShutDown.set(true);
		removeThreads(this.numOfThreads, getLowestPriority());
	}
	public void awaitTermination() throws InterruptedException {
		if (!this.isShutDown.get()) {
			throw new IllegalStateException();
		}

		for (Thread t : threadPool) {
			t.join();
		}
	}

	/*	Private Methods	*/
	private void addThreads(int numOfThreads) {
		for (int i = 0; i < numOfThreads; ++i) {
			Thread thread = new Thread(new TaskManager());
			threadPool.add(thread);
			thread.start();
		}
	}
	private void removeThreads(int numOfThreads, int priority) {
		for (int i = 0; i < numOfThreads; ++i) {
			developerSubmit(() -> ((TaskManager) Thread.currentThread()).isEnabled = false, priority);
		}
	}
	private void developerSubmit(Runnable runnable, int priority) {
		Callable<Void> callable = Executors.callable(runnable, null);
		Task<Void> newTask = new Task<>(callable, priority);

		waitablePQueue.enqueue(newTask);
	}
	private int getHighestPriority() {
		return Priority.HIGH.getPriority() + 1;
	}
	private int getLowestPriority() {
		return Priority.LOW.getPriority() - 1;
	}
}

