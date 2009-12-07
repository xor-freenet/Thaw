package thaw.core;

import java.util.Vector;
import java.util.Iterator;


public class ThawThread extends Thread {
	private static ThreadGroup threadGroup = new ThreadGroup("Thaw");
	private final static Vector<ThawThread> threads = new Vector<ThawThread>();
	private static boolean allowFullStop = false;

	private Object parent;
	private ThawRunnable target;
	private String name;


	public ThawThread(ThawRunnable target, String name) {
		this(target, name, null);
	}

	public ThawThread(ThawRunnable target, String name, Object parent) {
		super(threadGroup, name);

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

		if (threads.size() == 0) {
			Logger.notice(this, "All Thaw threads are stopped");
		}
	}

	public ThawRunnable getTarget() {
		return target;
	}

	public Object getParent() {
		return parent;
	}


	public static void setAllowFullStop(boolean a) {
		allowFullStop = a;

		synchronized(threads) {
			if (allowFullStop) {
				if (threads.size() == 0) {
					Logger.notice(null, "All Thaw threads are stopped.");
				}
			}
		}
	}


	public static void listThreads() {
		synchronized(threads) {
			Logger.info(null,
				    Integer.toString(threadGroup.activeCount())+" threads "+
				    "("+Integer.toString(threads.size())+" known)");

			for (ThawThread th : threads){
				if (th != null) {
					if (th.getParent() != null) {
						Logger.info(null,
								"'"+th.getName()+"' "+
								"(parent: '"+th.getParent().getClass().getName()+"')");
					} else {
						Logger.info(null,
								"'"+th.getName()+"' "+
								"(parent: unknown)");
					}
				}
			}
		}
	}

	public static void stopAll() {
		synchronized(threads) {
			for (ThawThread th : threads) {
				if( (th != null) && (th.getTarget() != null) ){
					th.getTarget().stop();
				}
			}
		}
	}
}
