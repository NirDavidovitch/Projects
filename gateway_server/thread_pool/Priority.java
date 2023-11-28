package gateway_server.thread_pool;

@SuppressWarnings("UnusedAssignment")
public enum Priority {
	LOW(1),
	DEFAULT(5),
	HIGH(10);

	int priorityNumber = 0;
	Priority(int priority) {
		this.priorityNumber = priority;
	}

	public int getPriority() {
		return this.priorityNumber;
	}
}