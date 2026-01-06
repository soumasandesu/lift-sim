package MyApp.misc;

import MyApp.building.Building;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;


//======================================================================
// AppThread
@Slf4j
public abstract class AppThread extends Thread {
	/**
	 * Represents that the identifier for such object in the building elevator and kiosk system.
	 */
    protected final String id;
    /**
     * Reference to the parent building for such object holds in.
     */
    protected final Building building;
    protected final MBox mbox;
    protected final HashMap<Integer, String> queue;

    //------------------------------------------------------------
    // AppThread
    public AppThread(final String id, final Building building) {
        super(id);
		this.id = id;
		this.building = building;
		this.mbox = new MBox(id);
		building.putThread(this);
		this.queue = new HashMap<>();
    } // AppThread


    //------------------------------------------------------------
    // getters
    public MBox getMBox() { return mbox; }
    /**
     * To retrieve the identifier of such object in this respective building.
     */
    public String getID() { return id; }
    public HashMap<Integer, String> getQueue() {return queue;}
    
    public void setQueue() {

    };
}
