package bgu.spl.mics;

import java.util.concurrent.TimeUnit;

/**
 * A Future object represents a promised result - an object that will
 * eventually be resolved to hold a result of some operation. The class allows
 * Retrieving the result once it is available.
 * 
 * Only private methods may be added to this class.
 * No public constructor is allowed except for the empty constructor.
 */
public class Future<T> {
	private T result; 
	private boolean isResolved; 
	private final Object futureLock; //צריך מנעול כדי ששני ת'רדים לא ינסו לגשת לשיטות שונות בו"ז

	/**
	 * This should be the the only public constructor in this class.
	 */
	public Future() {
		this.result = null;
		this.isResolved = false;
		this.futureLock = new Object(); //כרגע זה אכן אוביקט מופשט אבל ככהנ גאווה תדע להפוך את זה למנעול ברגע שנבצע SYNCHRONIZED
	}
	
	/**
     * retrieves the result the Future object holds if it has been resolved.
     * This is a blocking method! It waits for the computation in case it has
     * not been completed.
     * <p>
     * @return return the result of type T if it is available, if not wait until it is available.
     * 	       
     */
	public T get() {
	synchronized (futureLock) {
		while (!isResolved) {
				try {
					futureLock.wait();
					}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	return result;
	}
	
	/**
     * Resolves the result of this Future object.
     */
	public void resolve (T result) {
		synchronized (futureLock) {
			//אפשר להוסיף בדיקה אם מישהו כבר עדכן אותו אבל אנחנו כרגע חושבות שלא צריך
			this.isResolved=true;
			this.result=result;
			futureLock.notifyAll();
		}	}
	
	/**
     * @return true if this object has been resolved, false otherwise
     */
	public boolean isDone() {
		return isResolved;
	}
	
	/**
     * retrieves the result the Future object holds if it has been resolved,
     * This method is non-blocking, it has a limited amount of time determined
     * by {@code timeout}
     * <p>
     * @param timout 	the maximal amount of time units to wait for the result.
     * @param unit		the {@link TimeUnit} time units to wait.
     * @return return the result of type T if it is available, if not, 
     * 	       wait for {@code timeout} TimeUnits {@code unit}. If time has
     *         elapsed, return null.
     */
	public T get(long timeout, TimeUnit unit) {
		synchronized(futureLock){
			if(isResolved){
				return result;
			}
			try {
				long timeLeft = unit.toMillis(timeout);
				if (timeLeft > 0) {
					futureLock.wait(timeLeft); //לפי צאט משחרר את המנעול תוך כדי המתנה
					//בחרנו במודע לא להגן מפני התעוררות מוקדמת
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return result;
		}
	}
}
