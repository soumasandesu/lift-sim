package MyApp.timer;

import MyApp.misc.*;
import MyApp.building.Building;

import java.lang.Thread;
import java.util.ArrayList;
import java.util.Random;

//======================================================================
// Timer
public class Timer extends AppThread {
    private final int ticks;
    private static MBox timerMBox = null;
    private final Ticker ticker;
    private final ArrayList<ActiveTimer> timerList;

    //------------------------------------------------------------
    // Timer
    public Timer(final String id, final Building appkickstarter) {
        super(id, appkickstarter);
	this.ticker = new Ticker(getMBox());
	timerMBox = getMBox();
	this.timerList = new ArrayList<>();
	this.ticks = Integer.parseInt(building.getProperty("TimerTicks"));
    } // Timer


    //------------------------------------------------------------
    // run
    public void run() {
	log.info("Timer starting...");
	new Thread(ticker).start();

	while (true) {
	    final Msg msg = mbox.receive();

	    if (msg.getSender().equals("Ticker")) {
		chkTimeout();
	    } else {
		if (msg.getDetails().startsWith("set timer, ")) {
		    set(msg);
		} else if (msg.getDetails().startsWith("cancel timer, ")) {
		    cancel(msg);
		} else {
		    final String eMsg = "Invalid command for Timer: "+msg;
		    throw new RuntimeException(eMsg);
		}
	    }
	}
    } // run


    //------------------------------------------------------------
    // chkTimeout
    private void chkTimeout() {
	final long currentTime = System.currentTimeMillis();
	final ArrayList<ActiveTimer> timeoutTimers = new ArrayList<>();
	//log.info("Timer chk...");

	for (final ActiveTimer timer : timerList) {
	    if (timer.timeout(currentTime)) {
		timeoutTimers.add(timer);
	    }
	}

	for (final ActiveTimer timer : timeoutTimers) {
	    final int timerID = timer.getTimerID();
	    final String caller = timer.getCaller();
	    final MBox mbox = building.getThread(caller).getMBox();
	    mbox.send(new Msg("Timer", 999, "["+timerID+"]: Time's up!"));
	    timerList.remove(timer);
	}
    } // chkTimeout


    //------------------------------------------------------------
    // ticker
    private class Ticker implements Runnable {
	private final MBox timerMBox;

	//----------------------------------------
	// ticker
	public Ticker(final MBox timerMBox) {
	    this.timerMBox = timerMBox;
	} // Ticker


	//----------------------------------------
	// run
	public void run() {
	    while (true) {
		try {
		    Thread.sleep(ticks);
		} catch (final InterruptedException e) {
		    Thread.currentThread().interrupt();
		    break;
		}
		mbox.send(new Msg("Ticker", 0, "tick"));
	    }
	} // run
    } // ticker


    //------------------------------------------------------------
    // ActiveTimer
    private static class ActiveTimer {
	private final int  timerID;
	private final long wakeupTime;
	private final String caller;

	//----------------------------------------
	// ActiveTimer
	public ActiveTimer(final int timerID, final long wakeupTime, final String caller) {
	    this.timerID = timerID;
	    this.wakeupTime = wakeupTime;
	    this.caller = caller;
	} // ActiveTimer

	//----------------------------------------
	// getters
	public int    getTimerID() { return this.timerID; }
	public String getCaller()  { return this.caller; }

	//----------------------------------------
	// timeout
	public boolean timeout(final long currentTime) {
	    return currentTime > wakeupTime;
	} // timeout
    } // ActiveTimer


    //------------------------------------------------------------
    // setTimer
    public static int setTimer(final String id, final long sleepTime) {
	final int timerID = new Random().nextInt(9000) + 1000;
	timerMBox.send(new Msg(id, 0, "set timer, "+sleepTime+", "+timerID));
	return timerID;
    } // setTimer


    //------------------------------------------------------------
    // set
    private void set(final Msg msg) {
	final String details = msg.getDetails().substring(11);

	// get timerID
	final String timerIDStr = details.substring(details.indexOf(", ")+2);
	final int timerID = Integer.parseInt(timerIDStr);

	// get wakeup time
	final String sleepTimeStr = details.substring(0, details.indexOf(", "));
	final long sleepTime = Long.parseLong(sleepTimeStr);
	final long wakeupTime = System.currentTimeMillis() + sleepTime;

	// get caller
	final String caller = msg.getSender();

	// add this new timer to timer list
	timerList.add(new ActiveTimer(timerID, wakeupTime, caller));
	//log.info(id+": "+caller+" setting timer: "+
		//"["+sleepTime+"], ["+timerID+"]");
    } // set


    //------------------------------------------------------------
    // cancelTimer
    public static void cancelTimer(final String id, final int timerID) {
	timerMBox.send(new Msg(id, 1, "cancel timer, "+timerID));
    } // cancelTimer


    //------------------------------------------------------------
    // cancel
    private void cancel(final Msg msg) {
	// get timerID
	final String details = msg.getDetails();
	final String timerIDStr = details.substring(details.indexOf(", ")+2);
	final int timerID = Integer.parseInt(timerIDStr);

	// get caller
	final String caller = msg.getSender();

	ActiveTimer cancelTimer = null;

	for (final ActiveTimer timer : timerList) {
	    if (timer.getTimerID() == timerID) {
		if (timer.getCaller().equals(caller)) {
		    cancelTimer = timer;
		    break;
		}
	    }
	}

	if (cancelTimer != null) {
	    timerList.remove(cancelTimer);
	    log.info(id+": "+caller+" cancelling timer: "+"["+timerID+"]");
	} else {
	    log.info(id+": "+caller+" cancelling timer: "+"["+timerID+"]"+
		    " TIMER NOT FOUND!!");
	}

    } // cancel
} // Timer
