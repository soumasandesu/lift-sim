package MyApp.misc;

import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// JavaSE6Tutorial/docs/CH15.md
// 第 15 章 執行緒（Thread）
// https://github.com/JustinSDK/JavaSE6Tutorial/blob/master/docs/CH15.md

// ======================================================================
// MBox
@Slf4j
public class MBox {
    private final String id;
    private final ArrayList<Msg> mqueue = new ArrayList<>();
    
    /**
     * Message counter for implementing wait/notify pattern.
     * This design uses a single counter to track both messages and waiting threads:
     * <ul>
     *   <li>Positive values: number of messages available in queue</li>
     *   <li>Zero: queue is empty but no thread is currently waiting</li>
     *   <li>Negative values: number of threads waiting for messages (absolute value)</li>
     * </ul>
     * This elegant design allows efficient synchronization without separate condition variables.
     * When a thread calls receive() and queue is empty, it decrements msgCnt (making it negative)
     * and waits. When send() is called, it increments msgCnt and notifies waiting threads.
     */
    private int msgCnt = 0;

    //------------------------------------------------------------
    // MBox
    public MBox(final String id) {
        this.id = id;
    } // MBox

    //------------------------------------------------------------
    // send
    public final synchronized void send(final Msg msg) {
	msgCnt++;
	mqueue.add(msg);
	log.debug("{}: send \"{}\"", id, msg);
	notify(); // see ln 44
    } // send

    //------------------------------------------------------------
    // receive
    public final synchronized Msg receive() {
	// wait if message queue is empty
	if (--msgCnt < 0) {
	    while (true) {
		try {
		    wait(); // see ln 34
		    break;
		} catch (final InterruptedException e) {
		    log.warn("{}.receive: InterruptedException", id);

		    if (msgCnt >= 0)
			break;		// msg arrived already
		    else
			continue;	// no msg yet, continue waiting
		}
	    }
	}

	final Msg msg = mqueue.remove(0);
	log.debug("{}: receiveing \"{}\"", id, msg);
	return msg;
    } // receive
} // MBox
