package thaw.core;

import java.util.Collection;
import java.util.Vector;

public class ThawThreadManager {
	private ThreadGroup threadGroup = new ThreadGroup("Thaw");
	private final Collection<ThawThread> threads = new Vector<ThawThread>();
	private boolean allowFullStop = false;   /* TODO: What is this for? */

	public void add(ThawThread thread) {
		synchronized (threads) {
			threads.add(thread);
		}
	}

	public void remove(ThawThread thread) {
		synchronized (threads) {
			threads.remove(thread);
		}
	}

	public int count() {
		synchronized (threads) {
			return threads.size();
		}
	}

	public void setAllowFullStop(boolean a) {
		synchronized(threads) {
			allowFullStop = a;
			if (allowFullStop) {
				if (threads.size() == 0) {
					Logger.notice(null, "All Thaw threads are stopped.");
				}
			}
		}
	}


	public void listThreads() {
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

	public void stopAll() {
		synchronized(threads) {
			for (ThawRunnable th : threads) {
				if(th != null){
					th.stop();
				}
			}
		}
	}

}
