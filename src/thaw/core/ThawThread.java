package thaw.core;

public class ThawThread implements ThawRunnable {
	private final static ThawThreadManager threads = new ThawThreadManager();

	private Object parent;
	private ThawRunnable target;
	private String name;


	public ThawThread(ThawRunnable target, String name) {
		this(target, name, null);
	}

	public ThawThread(ThawRunnable target, String name, Object parent) {
		this.target = target;
		this.name = name;
		this.parent = parent;
	}

	public void run() {
		Logger.info(this, "Starting thread '"+name+"' ...");

		synchronized(threads) {
			threads.add(this);
		}

		target.run();

		synchronized(threads) {
			threads.remove(this);
		}

		Logger.info(this, "Thread '"+name+"' finished");

		if (threads.count() == 0) {
			Logger.notice(this, "All Thaw threads are stopped");
		}
	}

	public ThawRunnable getTarget() {
		return target;
	}

	public Object getParent() {
		return parent;
	}

	public Object getName() {
		return parent;
	}

	public void stop() {

	}

	public static ThawThreadManager getThawThreadManager() {
		return threads;
	}
}
