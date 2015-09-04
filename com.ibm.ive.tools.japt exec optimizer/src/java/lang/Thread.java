package java.lang;

public class Thread implements Runnable {
	private Runnable runnable;
	
	public Thread() {
		this(null, null, (String) null);
	}
	
	public Thread(Runnable runnable) {
		this(null, runnable, (String) null);
	}
	
	public Thread(Runnable runnable, String threadName) {
		this(null, runnable, threadName);
	}
	
	public Thread(String threadName) {
		this(null, null, threadName);
	}
	
	public Thread(ThreadGroup group, Runnable runnable) {
		this(group, runnable, (String) null);
	}
	
	public Thread(ThreadGroup group, Runnable runnable, String threadName, long stack) {
		this(group, runnable, threadName);
	}
	
	public Thread(ThreadGroup group, Runnable runnable, String threadName) {
		this.runnable = runnable;
	}
	
	public Thread(ThreadGroup group, String threadName) {
		this(group, null, threadName);
	}
	
	public void run() {
		if (runnable != null) {
			runnable.run();
		}
	}
	
	public static void sleep(long millis) throws InterruptedException {};
	
	public synchronized void start() {}
	
	public static interface UncaughtExceptionHandler {
		public void uncaughtException(Thread thread, Throwable throwable) ;
	}
	
	public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {}
	
	public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler) {}
}
