package bgu.spl.mics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

//====================================================================================================================

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus
 * interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {

	// מיקרו שירותים שרשומים לאירועים
	private final ConcurrentHashMap<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers;

	// מיקרו שירותים שרשומים לברודקאסטים
	private final ConcurrentHashMap<Class<? extends Broadcast>, ArrayList<MicroService>> broadcastSubscribers;

	// מפה של תורי EVENT של כל מיקרו שירות
	private final ConcurrentHashMap<MicroService, BlockingQueue<Message>> PersonalQueues;

	// 'קשר בין האירועים לfuter שלהם'
	private final ConcurrentHashMap<Event<?>, Future<?>> eventFutureMap;

	// Singleton instance אפשר עם פיינל כי יודעים שתמיד יהיה שימור במחלקה לא נורא
	// שיצור בזכרון מראש כי בטוח נשתמש בזה
	private final static MessageBusImpl instance = new MessageBusImpl(); // סטטיק אומר שקיים ברמת המחלקה ולא ברמת המופע

	// ====================================================================================================================

	// Constructor
	private MessageBusImpl() { // פרטי מונע יצירה של מופעים חדשים מחוץ למחלקה כדי להבטיח שיהיה סינגלטון
		eventSubscribers = new ConcurrentHashMap<>();
		broadcastSubscribers = new ConcurrentHashMap<>();
		PersonalQueues = new ConcurrentHashMap<>();
		eventFutureMap = new ConcurrentHashMap<>();
	}

	// ====================================================================================================================

	// Getter for Singleton instance
	public static MessageBusImpl getInstance() {
		return instance;
	}

	// ====================================================================================================================

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		eventSubscribers.putIfAbsent(type, new LinkedList<MicroService>());// לא צריך לסנכרן כי כבר מוגדר כמסונכרן בMAP
		Queue<MicroService> queue = eventSubscribers.get(type);// בגאווה זה מחזיר רפרנס ולא עותק של התור
		synchronized (m) {
			synchronized (queue) {
				queue.add(m);
				// System.out.println(m.getName() + " subscribed to event " + type.getSimpleName());
			}
		}
	}
	// ====================================================================================================================

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		broadcastSubscribers.putIfAbsent(type, new ArrayList<MicroService>());
		List<MicroService> list = broadcastSubscribers.get(type);
		synchronized (m) {
			synchronized (list) {
				list.add(m);
				// System.out.println(m.getName() + " subscribed to broadcast " + type.getSimpleName());
			}
		}
	}
	// ====================================================================================================================

	@Override
	// עושה הערה גם כשבודקים אם זה מאותו INSTANCE
	@SuppressWarnings("unchecked")
	// לוטם אמר שמותר להוסיף אם הבעיה נובעת מהבדל בין טיפוס גנרי לטיפוס לא ידוע
	public <T> void complete(Event<T> e, T result) {
		Future<T> future = (Future<T>) eventFutureMap.get(e);
		if (future != null) {
			synchronized (future) {
				future.resolve(result);
				System.out.println("Event " + e.getClass().getSimpleName() + " completed with result: " + result);
			}
		}
	}
	// ====================================================================================================================

	@Override
	public void sendBroadcast(Broadcast b) {
		List<MicroService> brodList = broadcastSubscribers.get(b.getClass()); // לקחת את התור של כל מי שעשה רישום לסוג
																				// הזה
		if (brodList != null) {
			synchronized (brodList) {
				for (MicroService m : brodList) {
					synchronized (m) {
						try {
							PersonalQueues.get(m).put(b);
							System.out.println("Broadcast " + b.getClass().getSimpleName() + " sent to " + m.getName());
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
				}
			}
		}
	}

	// ====================================================================================================================

	public <T> Future<T> sendEvent(Event<T> e) {
		// לא צריך לסנכרן כי המפה כבר מוגדרת ככה
		Queue<MicroService> queue = eventSubscribers.get(e.getClass()); // מחזיר את התור שמכיל את המיקרו שירותים שמטפלים
																		// בזה
		System.out.println("try lock the queue " + e.getClass().getSimpleName());
																
		synchronized (queue) {
			System.out.println("the queue locked " + e.getClass().getSimpleName());
			if (queue == null || queue.isEmpty()) {
				System.out.println("No subscribers for event " + e.getClass().getSimpleName());
				return null;
			}

			MicroService m = queue.poll(); // שולף את המיקרו שירות הראשון בתור שצריך לטפל בזה
			synchronized (m) {
				BlockingQueue<Message> microQueue = PersonalQueues.get(m); // מוצא את התור האישי של המשימות של המיקרו
																			// שירות הזה

				// נכניס את האירוע לתור האישי של המיקרו שירות
				try { // לא צריך לסנכרן את התור פה כי הוא מוגדר מראש כתור חוסם
					microQueue.put(e);
					System.out.println("Event " + e.getClass().getSimpleName() + " sent to " + m.getName());
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}

			Future<T> future = new Future<>();
			eventFutureMap.put(e, future); // מכניסים את הFUTURE החדש עם מפתח של הEVENT
			queue.add(m); // מחזירים את המיקרו שירות לסוף התור
			return future;
		}
	}
	// ====================================================================================================================

	@Override
	public void register(MicroService m) {
		synchronized (m) {
			PersonalQueues.putIfAbsent(m, new LinkedBlockingQueue<Message>());
			System.out.println("Registered microservice: " + m.getName());
		}
	}
	// ====================================================================================================================

	@Override
	public void unregister(MicroService m) {
		synchronized (m) {
			PersonalQueues.remove(m);// נסיר את התור האישי שלו מהמפה של התורים האישיים

			// נסיר את המיקרו שירות מרישומים לEVENT
			for (Queue<MicroService> queue : eventSubscribers.values()) {
				synchronized (queue) {
					queue.remove(m); // במידה ולא קיים ברשימה/תור לא יעשה כלום
				}
			}

			// נסיר את המיקרו שירות מרישומים לBRODCAST
			for (List<MicroService> list : broadcastSubscribers.values()) {
				synchronized (list) {
					list.remove(m);
				}
			}
			System.out.println("Unregistered microservice: " + m.getName());
		}
	}
	// ====================================================================================================================

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		BlockingQueue<Message> microQueue = PersonalQueues.get(m);
		// System.out.println(m.getName() + " waiting for a message");
		Message msg = microQueue.take();
		System.out.println(m.getName() + " received message: " + msg.getClass().getSimpleName());
		return msg;
	}

	// ====================================================ForTestOnly!================================================================

	public BlockingQueue<Message> getPersonalQueues(MicroService m) {
		return this.PersonalQueues.get(m);
	}

	public ArrayList<MicroService> getBrodSub(Class<? extends Broadcast> type) {
		return broadcastSubscribers.get(type);
	}

	public Queue<MicroService> getEventSub(Class<? extends Event<?>> type) {
		return eventSubscribers.get(type);
	}

	public Future<?> getevEntFutureMap(Event<?> event) {
		return eventFutureMap.get(event);
	}

	public ConcurrentHashMap<Class<? extends Event<?>>, Queue<MicroService>> getEventSubscribers() {
		return eventSubscribers;
	}

}
