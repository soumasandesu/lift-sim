package MyApp.misc;

import java.util.logging.Logger;
import java.util.ArrayList;
import lombok.AllArgsConstructor;

// JavaSE6Tutorial/docs/CH15.md
// 第 15 章 執行緒（Thread）
// https://github.com/JustinSDK/JavaSE6Tutorial/blob/master/docs/CH15.md

// ======================================================================
// MBox
@AllArgsConstructor
public class MBox {
    private final String id;
    private final Logger log;
    private final ArrayList<Msg> mqueue = new ArrayList<>();
    private int msgCnt = 0;

    //------------------------------------------------------------
    // send
    public final synchronized void send(final Msg msg) {
	msgCnt++;
	mqueue.add(msg);
	log.fine(id + ": send \"" + msg + "\"");
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
		    log.warning(id + ".receive: InterruptedException");

		    if (msgCnt >= 0)
			break;		// msg arrived already
		    else
			continue;	// no msg yet, continue waiting
		}
	    }
	}

	final Msg msg = mqueue.remove(0);
	log.fine(id + ": receiveing \"" + msg + "\"");
	return msg;
    } // receive
} // MBox
