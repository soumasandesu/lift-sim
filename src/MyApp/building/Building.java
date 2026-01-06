package MyApp.building;

import java.lang.Thread;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import MyApp.elevator.*;
import MyApp.kiosk.*;
import MyApp.misc.*;
import MyApp.panel.AdminPanel;
import MyApp.panel.ControlPanel;
import MyApp.panel.Panel;
import MyApp.timer.Timer;

import static java.util.stream.Collectors.toMap;

/**
 * Simulates all functionality of startElevatorStatusCacheThread centralised controller inside startElevatorStatusCacheThread building. <br/>
 * This may be used as entry point for simulation.
 */
public class Building {
    private static final int putStoppingHopMaxRetries = 100;
    /**
     * This is config file path
     */
    private static final String cfgFName = "etc/MyApp.cfg";
    /**
     * Tolerance for determining if elevator is docked at a floor (in meters).
     * Must match Elevator.DOCKING_TOLERANCE_METERS.
     */
    private static final double DOCKING_TOLERANCE_METERS = 0.05;
    /**
     * Indicates the total meters that an Elevator may move vertically for.
     */
    private final int totalDisplacementMeters;
    /**
     * An atomic reference of startElevatorStatusCacheThread dictionary storing all stoppale hops (floors) and the position, in meters, of displacement where the hop is. <br/>
     * See http://stackoverflow.com/questions/21616234/concurrent-read-only-hashmap
     */
    private final AtomicReference<LinkedHashMap<String, Floor>> arefFloorPositions;
    /**
     * Stores all the statuses of all the Elevators inside this Building, as a cache.
     */
    private final ConcurrentHashMap<Elevator, ElevatorStatus> elevatorsStatuses;
    /**
     * Logging module for verbose, debugging and warning/error messages.
     */
    private final Logger log;
    /**
     * A hash table storing all created thread-object in this respective building, with its identifier as the key.
     */
    private final ConcurrentHashMap<String, AppThread> appThreads;
    /**
     * Storage for all panels instances.
     */
    private final ArrayList<Panel> subWnds = new ArrayList<>();
    /**
     *
     */
    private final ConcurrentHashMap<Floor, Kiosk> kiosks = new ConcurrentHashMap<>();
    // http://cookieandcoketw.blogspot.hk/2013/03/java-hashmap-hashtable.html
    // http://www.infoq.com/cn/articles/ConcurrentHashMap
    //JavaDoc for the kioskHoppingRequests
//    private final ConcurrentHashMap<String, Floor> kioskHoppingRequests;
    /**
     * Accessors for different properties in this building configuration.
     */
    private final Properties cfgProps;
    /**
     * Holds the thread that refreshes the cache of statuses of all elevators.
     */
    private Thread threadBuildingRefreshElevatorStatusCache;

    /**
     * Initialisation of the Building simulation element. <br/>
     * It will also instantiate all lifts, kiosks, control panels and other related stuffs.
     *
     * @throws InvalidPropertiesFormatException When the <code>*.cfg</code> file is missing one of following of properties:
     *                                          <ul>
     *                                          <li><code>DisplacementMeters</code></li>
     *                                          <li><code>FloorNames</code></li>
     *                                          <li><code>FloorPositions</code></li>
     *                                          </ul>
     *                                          or
     *                                          <ul>
     *                                          <li>Amount of <code>floorNames</code> is not the same as that of <code>floorPositions</code>.</li>
     *                                          </ul>
     */
    public Building() throws InvalidPropertiesFormatException {
        // read system config from property file
        final Properties props = new Properties();
        try (final FileInputStream in = new FileInputStream(cfgFName)) {
            props.load(in);
        } catch (final FileNotFoundException e) {
            System.out.println("Failed to open config file (" + cfgFName + ").");
            System.exit(-1);
        } catch (final IOException e) {
            System.out.println("Error reading config file (" + cfgFName + ").");
            System.exit(-1);
        }
        this.cfgProps = props;

        // values for final properties
        if (cfgProps.containsKey("DisplacementMeters"))
            this.totalDisplacementMeters = Integer.parseInt(cfgProps.getProperty("DisplacementMeters"));
        else
            throw new InvalidPropertiesFormatException("missing DisplacementMeters");

        {
            final String[] floorNames;
            if (cfgProps.containsKey("FloorNames"))
                floorNames = cfgProps.getProperty("FloorNames").split("\\|");
            else
                throw new InvalidPropertiesFormatException("missing FloorNames");

            final double[] floorPositions;
            if (cfgProps.containsKey("FloorPositions")) {
                final Stream<String> s = Arrays.stream(cfgProps.getProperty("FloorPositions").split("\\|"));
                floorPositions = s.mapToDouble(Double::parseDouble).toArray();
            } else
                throw new InvalidPropertiesFormatException("missing FloorPositions");

            // for key-value pair, asserting array length size is same is required
            if (floorNames.length != floorPositions.length)
                throw new InvalidPropertiesFormatException("floorNames.length != floorPositions.length");

            // A dictionary storing all stoppale hops (floors) and the position, in meters, of displacement where the hop is.
            final ConcurrentHashMap<String, Floor> floorPositions1 = new ConcurrentHashMap<>();
            Floor lowerFloor = null;
            for (int i = 0; i < floorNames.length; i++) {
                final Floor floor = new Floor(floorNames[i], floorPositions[i]);
                floor.setLowerFloor(lowerFloor);
                floorPositions1.put(floorNames[i], floor);
                lowerFloor = floor;
            }

            final LinkedHashMap<String, Floor> floorPositions2 = floorPositions1.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            this.arefFloorPositions = new AtomicReference<>(floorPositions2);
        }

        // get and configure logger
        final ConsoleHandler conHd = new ConsoleHandler();
        conHd.setFormatter(new LogFormatter());
        final Logger logger = Logger.getLogger(Building.class.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(conHd);
        logger.setLevel(Level.INFO);
        this.log = logger;
        this.appThreads = new ConcurrentHashMap<>();

//        kioskHoppingRequests = new ConcurrentHashMap<>();
        // elevatorsStatuses already initialized as final field
    }

    /**
     * Java.exe entry point for loading up the Building simulation element.
     */
    public static void main(final String[] args) {
        final Building building;
        try {
            building = new Building();
        } catch (final Exception e) {
            System.out.println("Cannot instantiate Building object:");
            e.printStackTrace();
            return;
        }
        
        final Panel window = new AdminPanel(building.getFloorNames());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("caught an application exit signal.");
            building.appThreads.values().forEach(AppThread::interrupt);
            building.subWnds.forEach(Panel::dismissInfo);
            window.dismissInfo();
        }));

        window.showInfo();
        building.startApp();
    }

    /**
     * Start running up the world of the simulation.
     */
    @SuppressWarnings("WeakerAccess")
    public void startApp() {
        // This is for elevator use implement by steven and kers
        final Timer timer = new Timer("timer", this);

        // Create Kiosks k0 = floor 1 kiosk, k1 = floor 2 kiosk ......
        final int kc = Integer.parseInt(this.getProperty("Kiosks"));
        final ArrayList<Floor> floors = new ArrayList<>(getFloorPositions().values());
        for (int i = 0; i < kc; i++) {
            final Floor floor = floors.get(i);
            final Kiosk kiosk = new Kiosk("k" + i, this, floor);
            kiosk.start();
            kiosks.put(floor, kiosk);
            this.appThreads.put(kiosk.getID(), kiosk);
        }

        // Create elevator e0 = elevator 1, e1 = elevator 2 ......
        final int e = Integer.parseInt(this.getProperty("Elevators"));
        getLogger().info(String.format("Elevators = %d", e));
        for (int i = 0; i < e; i++) {
            final Elevator elevator = new Elevator("e" + i, this);
            elevator.start();
            this.appThreads.put(elevator.getID(), elevator);
        }

        startElevatorStatusCacheThread();

        // This is for elevator use implement by steven and kers
        timer.start();
        this.appThreads.put(timer.getID(), timer);

        // Wait all the thread object created. Then open control panel GUI
        final ControlPanel controlPanel = new ControlPanel(this);
        this.subWnds.add(controlPanel);
        controlPanel.showInfo();

        // show kiosk panel for testing
        final KioskPanel kioskPanel = new KioskPanel(this);
        this.subWnds.add(kioskPanel);
        kioskPanel.showInfo();

        getLogger().info(
                String.format("Threads (%d): %s",
                        appThreads.size(),
                        String.join(", ", appThreads.values().stream().map(AppThread::getID).sorted().collect(Collectors.toList())
                        )
                )
        );
    }

    /**
     * Ensures that the elevator status cache thread is running. Create new thread if not exist or not alive.
     */
    private void startElevatorStatusCacheThread() {
        if (this.threadBuildingRefreshElevatorStatusCache != null && this.threadBuildingRefreshElevatorStatusCache.isAlive())
            return;

        this.threadBuildingRefreshElevatorStatusCache = new Thread(() -> {
            while (true) {
                final Collection<Elevator> elevators = this.getThreads(Elevator.class);
                elevators.forEach(e -> this.elevatorsStatuses.put(e, e.getStatus()));
                try {
                    Thread.sleep(200);
                } catch (final InterruptedException e) {
                    System.out.println("BuildingRefreshElevatorStatusCache interrupted");
                    break;
                }
            }
        }, "threadBuildingRefreshElevatorStatusCache");
        this.threadBuildingRefreshElevatorStatusCache.start();
    }

    /**
     * Retrieves the Logger module for recording.
     *
     * @return The Logger moudle.
     */
    public Logger getLogger() {
        return log;
    }

    /**
     * Kiosk and elevator are appThread object. When they create, they will add into this method.<br/>
     * This method is for <code>Building:getThread(String id)</code>
     */
    public void putThread(AppThread appThread) {
        appThreads.put(appThread.getID(), appThread);
    }

    /**
     * Getting the specify thread. Also get the thread's attribute. <br/>
     * E.g.:
     * <code>Eleavtor ((Elevator)(this.getThread("e" + 1))).getStatus()</code>
     * -> get the e1(Elevator 2) status
     *
     * @param id The element identifier for the simulation object.
     */
    public AppThread getThread(String id) {
        return appThreads.get(id);
    }

    /**
     * Get threads from the threads storage that matches the type extending.
     *
     * @param type The class object of the type that extends the AppThread.
     * @param <T>  The type that extends the AppThread.
     * @return A list of the threads that matches the type extending.
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public <T extends AppThread> Collection<T> getThreads(Class<T> type) {
        return appThreads.values().stream().filter((t) -> t.getClass() == type).map(t -> (T) t).collect(Collectors.toList());
    }

    /**
     * Get a kiosk that is from that floor, by a floor object.
     *
     * @param floor The <code>Floor</code> to get a kiosk.
     * @return The <code>Kiosk</code> object, or <code>null</code> if not found.
     */
    public Kiosk getKioskByFloor(Floor floor) {
        return this.kiosks.get(floor);
    }

    /**
     * Get all kiosks that is instantiated automatically by this {@code Building} instance.
     * @return A {@code Collection} of {@code Kiosk}s that belongs to this {@code Building}.
     */
    public Collection<Kiosk> getKiosks() {
        return this.kiosks.values();
    }

    /**
     * Get config file key value pair
     *
     * @param property Key of the configuration property.
     * @return The value of the specified configuration property.
     */
    public String getProperty(String property) {
        return cfgProps.getProperty(property);
    }

    /**
     * Get all elevators that is instantiated automatically by this {@code Building} instance.
     * @return A {@code Collection} of {@code Elevator}s that belongs to this {@code Building}.
     */
    public Collection<Elevator> getElevators() {
        return this.getThreads(Elevator.class);
    }

    // TODO: remove if wasting space

    /**
     * Get all statuses of different elevators accordingly.
     *
     * @return A {@code Collection} of {@code ElevatorStatus}es.
     */
    public Collection<ElevatorStatus> getElevatorStatus() {
        return this.elevatorsStatuses.values();
    }

    /**
     * Get the total displacement that an elevator may travel for vertically within the building.
     *
     * @return The displacement, in meters.
     */
    public final int getTotalDisplacementMeters() {
        return totalDisplacementMeters;
    }

    /**
     * Get an dictionary for all floors, with their names and the displacement that matches the vertical position of the floor.
     *
     * @return A <code>Map</code> object that contains startElevatorStatusCacheThread list of: <br/>
     * <code>String</code> of the floor names and <br/>
     * <code>Floor</code> of the real floor object, which contains displacement that matches the vertical position of the floor in meters.
     */
    public final Map<String, Floor> getFloorPositions() {
        return arefFloorPositions.get();
    }

    /**
     * Get all the floor names as a String array.
     *
     * @return A string array of all floor names.
     */
    public final String[] getFloorNames() {
        Collection<Floor> floors = getFloorPositions().values();
        return floors.stream().sorted().map(Floor::getName).toArray(String[]::new);
    }

    /**
     * Get floor object by the floor name.
     *
     * @param floorName The alias of the floor that is human-readable.
     * @return The floor object that is inside the building, containing the information and specifications about the floor.
     */
    public final Floor getFloorPosition(String floorName) {
        return arefFloorPositions.get().get(floorName);
    }

    /**
     * Provides a method for Kiosk to put a new hop request for an elevator to stop at.
     *
     * @param kiosk     The source Kiosk that puts the request into this Building.
     * @param destFloor The destination floor that, after passenger boarding from the source floor, which floor to let passenger alight.
     * @return The elevator that is assigned for passenger to board, or <code>null</code> if retried <code>putStoppingHopMaxRetries</code> times.
     * @throws IndexOutOfBoundsException Throws when floor name, which is value of <code>destFloor</code>, does not exist in <code>floorPositions</code>.
     */
    public synchronized Elevator putNewHopRequest(final Kiosk kiosk, final String destFloor) throws IndexOutOfBoundsException {
        final Floor src = kiosk.getFloor();
        final Floor dest = getFloorPositions().get(destFloor);

        if (dest == null)
            throw new IndexOutOfBoundsException("destFloor key not exist in floorPositions"); // TODO: throw or null;

        if (src.equals(dest))
            return null; // won't assign any but shit you donk

        final boolean isGoingUp = dest.getYPosition() - src.getYPosition() > 0;

        // TODO: sort by: queueCount, direction, distance to src, speed (~=braking dist)
        // TODO: calculate which lift to catch the request
        final ArrayList<ElevatorStatus> ess = new ArrayList<>(elevatorsStatuses.values());
        ess.sort(new ElevatorStatusDistanceToFloorComparator(isGoingUp, src));

        int tries = 0;
        for (int i = 0; i < ess.size() && tries < putStoppingHopMaxRetries; i = ++tries % ess.size()) {
            final ElevatorStatus es = ess.get(i);

            // push back to the lift to update its next destination.
            if (es.getElevator().putNewDestination(src)) {
                // return an Elevator that such src:dest pair assigned to
                return es.getElevator();
            }
        }

        // return null if retried many times but failed at all
        return null;
    }

    /**
     * Get all docked {@code Elevator}s from specified {@code Floor}.
     * @param floor The specified {@code Floor} to find.
     * @return A {@code Collection} of {@code Elevator}s that is docked on that {@code Floor}.
     */
    public Collection<Elevator> getDockedElevatorsFromFloor(final Floor floor) {
        final ArrayList<Elevator> elevators = new ArrayList<>();

        for (final Elevator e : getElevators()) {
            final double elevYPos = e.getStatus().getYPosition();
            final double floorYPos = floor.getYPosition();

            if (elevYPos < floorYPos - DOCKING_TOLERANCE_METERS || elevYPos > floorYPos + DOCKING_TOLERANCE_METERS)
                continue;
            elevators.add(e);
        }

        return elevators;
    }
}
