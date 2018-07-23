# Protocol development



## 1 ArduSim architecture

ArduSim is able to run performing three different roles:

* Simulator. The application runs on a PC and uses a SITL instance for each virtual multicopter that it simulates. A GUI is used to control the experiment.
* UAV agent. The application runs on a Raspberry Pi 3 attached to a multicopter. The experiment is controlled from a PC Companion.
* PC Companion. The application runs on a Laptop and allows to control an experiment followed by any number of multicopters.

The code needed to run the PC Companion is completed and needs no further modification. When the protocol developer follows the included [recomendations](#markdown-header-85-implementation-recomendations), the same code used for simulation is also valid for a real multicopter, which makes the deployment on real devices somewhat trial. In order to make it possible, the multicopters are assigned a unique identifier (ID) based on their MAC address, or a number starting in zero if ArduSim behaves as a simulator.

### 2 Simulator

To simulate a great number of UAVs simultaneously, we have used the SITL application as a basic development module. SITL contains control code resembling a real UAV, simulating its physical and flying properties with great accuracy. A SITL instance is executed for each virtual UAV, and it runs together with its physical engine on a single process, as seen in the following image:
![architectureVirtual](architectureVirtual.png)
The proposed simulation platform relies on a multiagent simulation architecture that implements a high-level control logic above SITL itself.

ArduSim includes the simulation of packet broadcasting between UAVs (*Simulated broadcast*), and the detection of possible collisions (*UAV Collision detector*).

Each virtual multicopter is composed of an agent in charge of controlling the UAV behaviour, and the different threads required for the protocol being tested. The communication between UAVs requires a minimum of two threads, one for sending data packets (*Listener*), and another one for their reception (*Talker*). It is highly recommended that the thread *Listener* keeps always listening for new data packets to avoid CPU overhead when virtual buffers are full. In that situation many calculations are performed to process all stored packets. Furthermore, up-to-date packets are discarded, and older packets are used, as in a real link (in a real WiFi adapter new packets are discarded if the buffer is full and the application receives that old packets when it starts to receive).

An ArduSim agent includes a SITL instance, and a thread (Controller) in charge of sending commands to the multicopter, and of receiving the information that it generates.

The protocol under development can run more threads to control the behavior of the multicopter, like *Protocol logic*, but it is highly recommended to control the multicopter from the thread *Listener* if the multicopter behavior depends on the information received from other multicopters, to avoid any lag between the moment it receives information and the moment when the control action is applied.

### 3 UAV agent

The ArduSim simulator has been designed to facilitate the deployment of the implemented protocols in real UAVs.

When running ArduSim in Raspberry Pi 3, all the simulation-dependent software elements are disabled merely by changing an execution parameter, which makes the deployment of a newly developed protocol somewhat trivial.

The requirements to deploy on a real device are shown in the [Deployment on real devices - Raspberry Pi 3](help/deployment.md) section.

The following image shows the architecture of the application when running on a real device.
![architectureReal](architectureReal.png)

## 4 Packages structure

The Eclipse project in organized in packages. We suggest to enable the hierarchical presentation of packages in Eclipse to easyly understand the project structure, which includes the following packages:

* **main**. This package includes the main method that runs the application (*Main.java*) as well as general parameters (*Param.java*) and text shown in GUI or messages (*Text.java*).
* **sim**. This package includes the parameters, logic and GUI related to simulation but to to the execution on a real multicopter.
* **pccompanion**. This package includes the parameters and GUI needed by the PC Companion, as well as the threads needed to communicate with the real multicopters.
* **uavController**. This package includes the parameters related to the real or virtual multicopter, the thread needed to control it, and the threads needed to communicate with the PC Companion.
* **api**. This is the most important package and includes the following elements:
    * pojo. Collection of objects already used in ArduSim and that could be useful for any protocol; FlightMode of the multicopter, coordinates in UTM, geographic or screen frame...
    * WaypointReachedListener interface. Any protocol implementing this class can perform actions when the multicopter reaches a waypoint of the current mission (example available in MBCAP protocol, class BeaconingThread).
    * Copter. Includes methods to gather flight information from the multicopter or perform actions, like changing the flight mode, as explained in detail in "[8.2 UAV control](#markdown-header-82-uav-control)" section.
    * GUI. Includes methods to update the GUI and/or console during the protocol execution, as explained in detail in "[8.3 GUI integration](#markdown-header-83-gui-integration)" section.
    * Tools. Includes methods to coordinate the execution of the protocol with the simulator, transform coordinates between different frames, and load missions among other utilities, as explained in detail in "[8.4 Available utilities](#markdown-header-84-available-utilities)" section.
    * files. This package includes resources used when ArduSim is used as a Simulator.
* **... (protocols packages)**

Each new protocol must be selfcontained in an independent package. This way, the protocol code will be fully independent from ArduSim code, making it easyly understandable for other programmers.

Several protocols have been already included in ArduSim:

* **None**. Completed. Mission based. This protocol simply makes the multicopters to follow a planned mission.
* **MBCAP**. Completed. Mission based. It avoids collisions among multicopters that are following a planned mission, as explained in the paper "MBCAP: Mission Based Collision Avoidance Protocol for UAVs (doi: 10.1109/AINA.2018.00090)".
* **Swarm protocol**. Almost completed. Swarm based. It makes a swarm to follow a mission stored in one of them while keeping the formation. Furthermore, the takeoff of all the multicopters is safe until forming up in flight.
* **Chemotaxis**. Almost completed. Single multicopter. It enables a multicopter to dynamically move around an area looking for a hotspot using a sensor (e.g. heat on wildfires, pollution peaks,...).
* **Follow Me**. In progress. Swarm based. A swarm follows a multicopter that is manually controlled by a pilot.
* **Fishing**. In progress. Single multicopter. A multicopter follows a fishing boat drawing circles over it.

Please, feel free to reuse code from this protocols when developing a new one. "None" is the better starting point developing a protocol where all the multicopters must follow a planned mission, while "Swarm protocol" is more adecuate when developing a protocol for a swarm.

## 5 Application workflow

As explained in section "[1 ArduSim architecture](#markdown-header-1-Ardusim-architecture)", ArduSim can be run performing three different roles. The next diagram shows the timeline of ArduSim execution since it is started until if finishes:

![workflow](ArduSimworkflow.svg)

Rectangular boxes represent the functions included in section "[7 Protocol implementation](#markdown-header-7-protocol-implementation)" that must be implemented by the developer, while comments are automatic processes performed by ArduSim.

ArduSim starts loading the implemented protocols and parsing the command line to know which role will it run among other parameters.

### 5.1 PC Companion

First of all, communications are set online. Then, when the user presses the "Setup" button the command is issued and the real multicopters execute the "*setupActionPerformed()*" method. Once the setup process is completed the user can start the experiment with the corresponding button and the multicopters execute the method "*startExperimentActionPerformed()*". Immediately, if the developer needs it, a dialog can be opened with the function "*openPCCompanionDialog()*" where the user could analize the data packets sent among the multicopters.

At any moment, the user can start actions to take control over the multicopters if the protocol behavior is unexpected, like land or return to the launch location.

### 5.2 UAV agent

ArduSim assigns an unique ID to the multicopter and then loads the planned mission from a file "*loadMission()*", if the protocol needs it. Then, the method "*initializeDataStructures()*" is launched, where the developer can initialize the variables needed by the protocol taking into account the number of multicopters being run in the same ArduSim instance (one in this case, but many for simulations).

The path followed by the multicopter that is logged for further analysis is started and the application waits until the multicopter is located by the GPS system. Some information is retrieved from the UAV and the planned mission is sent to it if needed by the protocol. Next, the function "*sendInitialConfiguration()*" can be used to retrieve more information or send commands to the multicopter needed before starting the threads of the protocol with the function "*startThreads()*". We recommend to retrieve UAV configuration in this step. Please remember that the threads are started here, but they execution probably must wait until the user presses the "Setup" and/or "Start" buttons, when the functions "*setupActionPerformed()*" and "*startExperimentActionPerformed()*" are run.

The experiment is finished when the multicopter lands and engines stop. Some protocols will land the UAV automatically, but others could finish while the UAV is flying. In the second case, the function "*forceExperimentEnd()*" must be implemented to detect the end of the experiment and land the multicopter.

Once the experiment is finished, the methods "*getExperimentResults()*" and "*getExperimentConfiguration()*" allow the developer to generate Strings with general information and configuration of the protocol that will be included in the default log files. Additionally, the method "*logData()*" can be used to store files with more information.

### 5.3 Simulator

The most important difference between "UAV agent" and "Simulator" roles implementation is the number of multicopters that run in the same machine, one in the former and many in the later. It is highly suggested to store data in array variables with the length of the number of UAVs that run in the machine, so in the real UAV the array length will be 1 and the code will be valid for both roles. Good implementation examples can be found in "MBCAP" and "Swarm protocol" protocols. A function is provided to know which ID (multicopter) corresponds to a specific position in the array, as detailed in section "[8.4 Available utilities](#markdown-header-84-available-utilities)". This function will provide the ID of the multicopter when running on a real UAV, as the array has a length of 1. On the other hand, no function is provided to get a UAV location in the array given the ID, as it will always be 0 in the real UAV and in simulation the ID is equivalent to the position in the array.

When ArduSim is run, it also checks if the computer meets some requirements and opens a general configuration dialog. There, the user can set many simulation parameters, as well as the maximum flight speed for each virtual multicopter. Then, when the configuration is set, the function "*openConfigurationDialog()*" may launch a dialog to introduce values for parameters of the protocol under development. Next, planned missions are loaded for the multicopters specified by the protocol.

In the next step the main window of the simulator is opened an IDs are assigned to the virtual UAVs. The function "*initializeDataStructures()*" allows the programmer to initialize the variables needed by the protocol. As stated before, it is highly recommended to use arrays with the length of the number of UAVs that are being run on the same machine at the same time. This way the code will be valid in simulation an running in a real multicopter.

In simulation, another difference compared to a "UAV agent" is the presence of a GUI. By default, the UAVs, their planned mission, if any, and the path they are following is being draw automatically. Nevertheless, the developer may want to draw additional elements, like other objects to define a safety fence around the UAVs or to show information related to the protocol. In that case, the function "*loadResources()*" allow the programmer to dynamically load images to be drawn. The function "*drawResources()*" can be used to draw this elements each time the screen is updated. The methods "*rescaleDataStructures()* and "*rescaleShownResources()*" allow to change the way the elements are drawn each time the drawing scale changes. Additionally, the method "*setInitialState()* can be implemented to show a text for each UAV with its initial state in the protocol that is shown when the "Progress dialog" is opened.

The function "*setStartingLocation()*" provides the starting location of the multicopters and then the simulation can start. Once the current location is acquired, the virtual UAV-to-UAV communication link is stablished and the collision detection is enabled. The functions "*sendInitialConfiguration()*", "*startThreads()*", "*setupActionPerformed()*", "*startExperimentActionPerformed()*", "*forceExperimentEnd()*", "*getExperimentResults()*", "*getExperimentConfiguration()*", and "*logData()*" have to be implemented the same way already explained for the "UAV agent" role.

There are another two differences compared to a "UAV agent". First, a set of pictures are downloaded from Google Static Maps to integrate the background image, which is geopositioned on the theoretical location of the virtual UAVs. Second, storing the experiment results is optional.

## 6 Development environment

DOWNLOAD OF PROJECT IN ECLIPSE AND INTEGRATION WITH arducopter...



## 7 Protocol implementation

To start a new protocol, the developer creates a new package (*ProtocolName*) to contain the corresponding Java classes. Then, the first step consists in create a new class that "extends" "*api.ProtocolHelper.java*". This class forces the developer to implement the functions already mentioned, to integrate the protocol in ArduSim. An extended explanation of the functions follows:

* *void* **setProtocol()**. Assign a String name to the protocol to enable the implementation, using the variable *this.protocol*.
* *void* **openConfigurationDialog()**. It opens a dialog implemented by the protocol developer and that allows the user to input parameters related to the protocol. Always remember to issue the command *api.Tools.setProtocolConfigured(true);* to force the simlator to open the main window.
* *void* **openPCCompanionDialog(** *JFrame* **)**. Optional. This method enables the developer to implement a dialog to analyze the behavior of the protocol on the PC Companion, when the protocol is deployed in real multicopters. If using this method, an additional thread must be implemented to update the information shown in the dialog, based on the data packets that are being broadcasted from the real UAVs. The thread must be started once the dialog is completely built.
* *boolean* **loadMission()**. On a real multicopter it must return true if and only if a planned mission must be followed by the UAV. The mission file must be stored beside ArduSim jar file.
* *void* **initializeDataStructures()**. The protocol being developed will need several variables shared among threads that should be declared following the package structure shown below. This method allows to initialize the variables once the number of UAVs running in the same machine is known (more than one if a simulation is performed).
* *void* **loadResources()**. Optional. Used to load from file elements that the protocol will draw in screen.
* *Pair<GeoCoordinates, Double>[]* **setStartingLocation()**. Used to set the location where the multicopters appear in simulation, including latitude, longitude and heading. By default, they appear at 0 absolute altitude.
* *boolean* **sendInitialConfiguration(** *int* **)**. Reads from the multicopter any additional configuration values needed by the protocol and sends to it any command needed to configure it.
* *void* **startThreads()**. This method is used to start the threads used by the protocol. Then,ArduSim waits the user to press the setup button. In general, the threads must use methods included in *api.Tools* to wait until the setup or start button is pressed before performing any action.
* *void* **setupActionPerformed()**. This method must wait until any action required for the setup step is finished. It is more addecuate to simply wait until the protocol threads finish the action using a shared concurrent variable than implementing here the actions, as the may require the use of more than one thread.
* *void* **startExperimentActionPeformed()**. It is useful to perform the takeoff or start a mission from the ground. In the protocol needs to perform additional actions over the UAV, it is recommended to do it in the protocol threads to allow this thread to check periodically if the experiment finishes. In the first case, that threads should wait until a shared variable is changed when the takeoff process finishes, while in the second case the control over the multicopter is released immediately after the mission start, so the protocol must have its own logic to decide when to take control over the multicopter.
* *void* **forceExperimentEnd()**. Optional. An experiment is considered to be finished when all UAVs land and stop engines. Once the previous method finishes, this one is issued periodically and allows to land the UAVs to finish the experiment if a condition is met, for example, when the UAV is following a planned mission and is close enough to the last waypoint (*api.Copter.landIfMissionEnded(int)*). The protocol may issue other actions to land the UAV from other threads. In that case, this method could be left unimplemented.
* *String* **getExperimentResults()**. Optional. Allows to add data related to the protocol to the information shown in the results dialog.
* *String* **getExperimentConfiguration()**.Optional. The developer has the option to show the value of the parameters used in the protocol in the results dialog so they could be stored to be able to reproduce the same experiment again.
* *void* **logData(** String, String **)**. Optional. Stores information gathered by the protocol during the experiment.
* *String* **setInitialState()**. Optional. ArduSim can show a String representing the state of the protocol for each UAV in the Progress Dialog. This method sets the initial value or initial state of the protocol when the multicopter is ready to fly.
* *void* **rescaleDataStructures()**. Optional. Used when additional elements are drawn in screen by the protocol. It rescales data structures used for drawing when the drawing scale changes.
* *void* **rescaleShownResources()**.Optional. Used when additional elements are drawn in screen by the protocol. It rescales the resources (images) used for drawing when the drawing scale changes.
* *void* **drawResources(** Graphics2D, BoardPanel **)**. Optional. Periodically draws the resources used in the protocol in the Graphics2D element of the specified BoardPanel.

The recommended package structure for the protocol follows:

* **gui**. This package should contain graphical elements, such as the dialog used to input values for the protocol parameters or the dialog used in the PC Companion when deploying the protocol in real multicopters. The later is optional.
* **logic**. It should contain classes related to the protocolo logic, for example:
    * *ProtocolNameHelper.java*. The protocol implementation already detailed.
    * *ProtocolNameParam.java*. Declaration of variables needed by the protocol. Please, see other protocol as example.
    * *ProtocolNameText.java*. Texts used in GUI or messages for the protocol.
    * *ProtocolNamePCCompanionThread.java*. Thread that can be implemented to update data in the dialog used by the PC Companion, if implemented.
    * *ProtocolNameOtherThreads.java*. Threads needed by the protocol. If more than one multicopter is needed and they have to communicate among them, then at least one thread to talk and another to listen to other UAVs must be implemented. Remember that the listener thread must listen always even when no data packets are expected to avoid the buffer to fill with undesired packets.
* **pojo**. Should contain classes to define objects useful for the protocol and used in the previous packages.

## 8 Implementation details

This sections includes several details the way ArduSim implements relevant elements needed by the developer, and some implementation recommendations to make the same code work in virtual and real multicopters, which would make the code more clear and easy to re-use. 

### 8.1 UAV-to-UAV Communications

Real multicopters use WiFi to broadcast UDP messages among them. On the other hand, broadcast is simulated among virtual UAVs when ArduSim is run as a simulator. In order to make the same code valid for both roles, an abstraction layer has been implemented over communications. Two functions have been implemented that help the developer to ignore communication details:

* *void* **api.Copter.sendBroadcastMessage(** *int, byte[]* **)**. A multicopter sends a broadcast message to other UAVs encoded in a byte array. Please, remember that broadcast messages are also received by the sender, and they must be explicitly ignored in the sender.
* *byte[]* **api.Copter.receiveMessage(** *int* **)**. A multicopter receives the next message sent from another UAV. The method blocks until a message is received, as in a real socket. Please, make the listener thread to listen continuously for new packets to avoid the buffer to fill. You would loose data updates, keeping in buffer old messages. Moreover, when ArduSim is run as a simulator, performance issues may happen for a few seconds when you start to read messages while the buffer is flushed and many UAVs are simulated at the same time.

### 8.2 UAV control

The Java Class *api.Copter.java* includes several functions to send commands to the multicopter and to retrieve information already gathered from it. Most commands return a boolean meaning whether the command was successfully completed or not, which allows the developer to treat errors at a higher level. An integer value represents the position of the multicopter the command is applied to in the array of UAVs running in the same machine (one in a real UAV, and many in simulation).

Simple command functions:

* *boolean* **setParameter(** *int, ControllerParam, double* **)**. The developer can modify one of the parameters of the flight controller as included in *uavController.UAVParam.ControllerParam* enumerator. The most appropriate place would be the function *sendInitialConfiguration(int)* of the protocol implementation, before starting the protocol threads (see section "[5 Application workflow](#markdown-header-5-application-workflow)").
* *Double* **getParameter(** *int, ControllerParam* **)**. Parameter values can be retrieved from the flight controller at any time, but again the most appropriate place is the method *sendInitialConfiguration(int)*.
* *boolean* **setFlightMode(** *int, FlightMode* **)**. It changes the flight mode as defined in *api.pojo.FlightMode*.
* *boolean* **armEngines(** *int* **)**. It arms the engines so the flight could be started. The multicopter must be on the ground and in an armable flight mode. On a real UAV, the hardware switch for safety arm must be pressed, if available.
* *boolean* **guidedTakeOff(** *int, double* **)**. It takes off to the target relative altitude. The multicopter must be in GUIDED flight mode and armed.
* *boolean* **setHalfThrottle(** *int* **)**. To start a mission, the throttle value must be moved from the minimum value (default value) to a higher value, once the engines are armed and the flight mode is set to AUTO. This method rises the throttle value overriding the corresponding channel of the remote control. Moreover, it set yaw, pitch and roll to their trim (middle) value to keep the UAV static in the horizontal plane. Channel override is enabled by default and cannot be enabled again once used the function *returnRCControl(int)*.
* *void* **channelsOverride(** *int, int, int, int, int* **)**. It allows to simulate the joysticks of the remote control, providing values for yaw, pitch, roll and throttle. Commands must be issued at least once a second in a loop or the control could be returned to the real remote. Channel override is enabled by default and cannot be enabled again once used the function *returnRCControl(int)*.
* *boolean* **returnRCControl(** *int* **)**. It allows to release the control of the flight to the remote control, canceling the channels overriding. It is used by the PC Companion and may be used by any protocol, but be aware, it can only be used once and a pilot must be ready and with the remote control turned on or the multicopter would crash!
* *boolean* **setSpeed(** *int, double* **)**. It modifies the planned flight speed, it is, the maximum flight speed for the multicopter. In a mission, it is the constant speed it will follow through a straight line, and in GUIDED flight mode it is the maximum speed adopted by the flight controller while executing commands.
* *boolean* **moveUAVNonBlocking(** *int, GeoCoordinates, float* **)**. It send a command to go to a specific location in GUIDED flight mode.
* *boolean* **moveUAV(** *int, GeoCoordinates, float, double, double* **)**. It performs the same way as the previous method but additionaly it blocks until the multicopter approaches enough to the destination.
* *boolean* **clearMission(** *int* **)**. It removes the mission stored in the flight controller, if any.
* *boolean* **sendMission(** *int, List<Waypoint>* **)**. It sends a new mission to the flight controller. We recommend to remove the current mission before sending a new one.
* *boolean* **retrieveMission(** *int* **)**. Recovers the mission stored in the flight controller and makes it available with the function *api.Tools.getUAVMission(int)*. It also updates the simplified mission shown on screen, also available with the function *api.Tools.getUAVMissionSimplified(int)*.
* *boolean* **setCurrentWaypoint(** *int, int* **)**. It changes the current waypoint in the mission the UAV has to follow.

Complex command functions. These functions have been built combining functions from the previous list to perform more complex tasks:

* *boolean* **startMissionFromGround(** *int** **)**. It takes off and starts the planned mission stored in the flight controller. The multicopter must be on the ground and in an armable flight mode. On a real UAV, the hardware switch for safety arm must be pressed, if available.
* *boolean* **startMissionsFromGround()**. It takes off all the UAVs at the same time and starts the planned missions stored in the flight controllers. The multicopters must be on the ground and in an armable flight mode. On real UAVs, the hardware switch for safety arm must be pressed, if available.
* *boolean* **takeOffNonBlocking(** *int, double* **)**. It performs all the needed actions to take off without waiting to reach the target altitude. The multicopter must be on the ground and not armed.
* *boolean* **takeOff(** *int, double* **)**. It also performs the take off but waits until the multicopter reaches the target altitude. The multicopter must be on the ground and not armed.
* *boolean* **takeOffAllUAVsNonBlocking(** *double[]* **)**. It takes off all the UAVs at the same time as in function *takeOffNonBlocking(int, double)*.
* *boolean* **takeOffAllUAVs(** *double[]* **)**. It takes off all the UAVs at the same time as in function *takeOff(int, double)*.
* *boolean* **stopUAV(** *int* **)**. It sharply stops the multicopter in flight while performing a planned mission. The mission can be resumed later changing to AUTO flight mode.
* *void* **landIfMissionEnded(** *int, double* **)**. This method lands the multicopter if it is close enough to the last waypoint, and can be launched periodically. Useful for UAVs that follow a planned mission.
* *boolean* **landUAV(** *int* **)**. This method is used to land a UAV.
* *boolean* **landAllUAVs()**. In this case, all the UAVs receive the land command.
* *boolean* **cleanAndSendMissionToUAV(** *int, List<Waypoint>* **)**. It deletes the current mission of the UAV, sends a new one, and retrieves it from the flight controller to show it on the GUI, using three simple command functions.

Follows a list of information retrieval functions that don't need to communicate with the flight controller. The data could be slightly outdated, as this information is retrieved periodically.

* *FlightMode* **getFlightMode(** *int* **)**. This method provides the current flight mode of the multicopter.
* *boolean* **isFlying(** *int* **)**. It reports whether the multicopter is flying or not (on the ground and engines off).
* *Quintet<Long, Point2D.Double, Double, Double, Double>* **getData(** *int* **)**. This method gives the most up-to-date data received from the flight controller, including coordinates, speed, acceleration and the moment they were received from the flight controller.
* *Point2D.Double* **getUTMLocation(** *int* **)**. It provides only the current UTM coordinates.
* *GeoCoordinates* **getGeoLocation(** *int* **)**. In this case, it provides the current geographic coordinates (latitude and longitude).
* *Point3D[]* **getLastKnownLocations(** *int* **)**. This function gives the last known locations of the UAV in ascending order.
* *double* **getZRelative(** *int* **)**. It provides the current relative altitude over the home location.
* *double* **getZ(** *int* **)**. It provides the current absolute altitude over the sea level.
* *double* **getSpeed(** *int* **)**. This method gives the current flight speed.
* *Triplet<Double, Double, Double>* **getSpeeds(** *int* **)**. In this case, the current flight speed for the three cartesian axes is provided.
* *double* **getPlannedSpeed(** *int* **)**. This method provides the maximum flying speed used by the flight controller. In a mission, it is the constant speed it will follow through a straight line, and in GUIDED flight mode it is the maximum speed adopted by the flight controller while executing commands.
* *double* **getHeading(** *int* **)**. This method gives the current yaw or heading of the multicopter.
* *void* **setWaypointReachedListener(** *WaypointReachedListener* **)**. Any Java Class can implement *WaypointReachedListener* as *mbcap.logic.BeaconingThread* does. Then, using this method that Class would be able to apply some logic each time the flight controller detects that a waypoint has been reached. Useful for UAVs that follow a planned mission.
* *int* **getCurrentWaypoint(** *int* **)**. It provides the identifier of the current waypoint of the mission. Useful for UAVs that follow a planned mission.
* *boolean* **isLastWaypointReached(** *int* **)**. It asserts if the last waypoint of the mission has been reached. Useful for UAVs that follow a planned mission.
* *String* **getUAVPrefix(** *int* **)**. This function builds a String with convenience text that should be prepended to any message that a multicopter could publish with the function *api.GUI.log(String)*.

Experimental functions not directly included in *api.Copter.java* Class:

* *void* **getController(** *int* **).msgTarget(** *Double, Double, Double, Double, Double, Double* **)**. This function allows to move a UAV in GUIDED flight mode towards a set of coordinates, or at a certain speed. Speed based motion has not been tested already and this function is experimental.
* *void* **msgYaw(** *float* **)**. This function allows to modify the yaw or heading of the multicopter. This function has not been tested already and is experimental.

### 8.3 GUI integration

A few functions have been implemented to update the data already shown in the GUI, and to allow the developer to introduce new elements in the drawing panel.

The next list of functions allow the developer to update the GUI, and even close ArduSim when an unexpected behavior is detected, showing a message before closing the application.

* *void* **log(** *String* **)**. This method shows a message in console. Furthermore, is ArduSim runs as a simulator, the same message is shown in the log in the upper left corner of the main window.
* *boolean* **isVerboseLoggingEnabled()**. It returns true if console and GUI logging is performed in verbose mode. This helper can be useful to show some messages of the protocol only in that mode.
* *void* **updateProtocolState(** *int, String* **)**. The progress dialog shows general information for each running virtual UAV. This function is used to show there the current estate of the protocol to compare the behavior when different UAVs are in a different state.
* *void* **updateGlobalInformation(** *String* **)**. On the upper right corner of the main window, below the interaction buttons, there is a String where you can show any information with this function.
* *void* **warn(** *String, String* **)**. On a real UAV it writes a mesage to console, while in simulation it opens a dialog to warn the user.
* *void* **exit(** *String* **)**. The behavior is the same as the previous method, but additionally it closes ArduSim with a error code. If ArduSim runs as a simulator, before exiting all SITL instances are closed and temporary files are removed.

The following functions are useful to draw new elements in the main panel using the methods *loadResources())*,  *drawResources(Graphics2D, BoardPanel)*, *rescaleDataStructures()*, and *rescaleShownResources()* in the protocol implementation, as explained in section "[7 Protocol implementation](#markdown-header-7-protocol-implementation)".

* *Point2D.Double* **locatePoint(** *double, double* **)**. It provides the screen coordinates of a point given its UTM coordinates.
* *Color* **getUAVColor(** *int* **)**. It provides the Color assigned to a UAV to be used to draw linear elements on the screen.

The last function may be used in the PC Companion dialog, if implemented, to get a list of UAVs detected. It is useful to build the GUI before launching a thread to update it depending on the present UAVs.

* *StatusPacket[]* **getDetectedUAVs()**. Returns an array with the number of detected UAVs as size, of an object with the ID of the multicopters. A usage example can be found in the protocol MBCAP.

### 8.4 Available utilities




functions in Tools.java

### 8.5 Implementation recomendations

how to use the same code for real and virtual UAVs