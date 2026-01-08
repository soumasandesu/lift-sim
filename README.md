# Lift Simulation System

Elevator simulation system built with Java 25 LTS.

## Features

- Multi-elevator simulation with physics-based movement
- Kiosk-based floor request system
- Real-time elevator status monitoring
- RFID-based destination selection
- Admin panel for system management

## Requirements

- Java 25 LTS
- Maven 3.6+

## Building

```bash 
mvn clean compile
```

## Running

```bash
mvn exec:java -Dexec.mainClass="MyApp.building.Building"
```

Or directly:

```bash
java -cp target/classes MyApp.building.Building
```

## Configuration

Edit `etc/MyApp.cfg` to configure:
- Number of elevators
- Number of kiosks
- Floor names and positions
- Elevator physics parameters

## Project Structure

```
src/
├── MyApp/
│   ├── building/     # Building and floor management
│   ├── elevator/      # Elevator simulation logic
│   ├── kiosk/         # Kiosk and request handling
│   ├── misc/          # Utilities (MBox, Msg, RFID, etc.)
│   ├── panel/         # GUI panels
│   └── timer/         # Timer system
etc/
├── MyApp.cfg          # Configuration file
└── RFID_DB            # RFID database
```

## Technologies

- Java 25 LTS
- Lombok (for reducing boilerplate code)
- Maven (build tool)
- Swing (GUI)

