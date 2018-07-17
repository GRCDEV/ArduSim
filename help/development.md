# Protocol development



## 1 ArduSim architecture

ArduSim is able to run performing three different roles:

* Simulator. The application runs on a PC and uses a SITL instance for each virtual multicopter that it simulates. A GUI is used to control the experiment.
* UAV agent. The application runs on a Raspberry Pi 3 attached to a multicopter. The experiment is controlled from a PC Companion.
* PC Companion. The application runs on a Laptop and allows to control an experiment followed by any number of multicopters.

The code needed to run the PC Companion is completed and needs no further modification. When the protocol developer follows the included [recomendations](#markdown-header-implementation-recomendations), the same code used for simulation is also valid for a real multicopter, which makes the deployment on real devices somewhat trial. In order to make it possible, the multicopters are assigned a unique identifier (ID) based on their MAC address, or a number starting in zero if ArduSim behaves as a simulator.

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

* main
* sim
* pccompaniom
* uavController
* api
* files
* ...

The developer only has to care about the package *api*, but a simple explanation for each package follows:

### main

This package includes the main method that runs the application (*Main.java*) as well as general parameters (*Param.java*) and text shown in GUI or messages (*Text.java*).

### sim

This package includes the parameters, logic and GUI related to simulation but to to the execution on a real multicopter.

### pccompanion

This package includes the parameters and GUI needed by the PC Companion, as well as the threads needed to communicate with the real multicopters.

### uavController

This package includes the parameters related to the real or virtual multicopter, the thread needed to control it, and the threads needed to communicate with the PC Companion.

### files

This package includes resources used when ArduSim is used as a Simulator.

### api

This is the most important package and includes the following elements:

* pojo. Collection of objects already used in ArduSim and that could be useful for any protocol; FlightMode of the multicopter, coordinates in UTM, geographic or screen frame...
* WaypointReachedListener interface. Any protocol implementing this class can perform actions when the multicopter reaches a waypoint of the current mission (example available in MBCAP protocol, class BeaconingThread).
* Copter. Includes methods to gather flight information from the multicopter or perform actions, like changing the flight mode, as explained in detail in "[7.2 UAV control](#markdown-header-72-uav-control)" section.
* GUI. Includes methods to update the GUI and/or console during the protocol execution, as explained in detail in "[7.3 GUI integration](#markdown-header-73-gui-integration)" section.
* Tools. Includes methods to coordinate the execution of the protocol with the simulator, transform coordinates between different frames, and load missions among other utilities, as explained in detail in "[7.4 Available utilities](#markdown-header-74-available-utilities)" section.

### protocol packages (...)

Each new protocol must be selfcontained in an independent package. This way, the protocol code will be fully independent from ArduSim code, making it easyly understandable for other programmers.

Several protocols have been already included in ArduSim:

* None. Completed. Mission based. This protocol simply makes the multicopters to follow a planned mission.
* MBCAP. Completed. Mission based. It avoids collisions among multicopters that are following a planned mission, as explained in the paper "MBCAP: Mission Based Collision Avoidance Protocol for UAVs (doi: 10.1109/AINA.2018.00090)".
* Swarm protocol. Almost completed. Swarm based. It makes a swarm to follow a mission stored in one of them while keeping the formation. Furthermore, the takeoff of all the multicopters is safe until forming up in flight.
* Chemotaxis. Almost completed. Single multicopter. It enables a multicopter to dynamically move around an area looking for a hotspot using a sensor (e.g. heat on wildfires, pollution peaks,...).
* Follow Me. In progress. Swarm based. A swarm follows a multicopter that is manually controlled by a pilot.
* Fishing. In progress. Single multicopter. A multicopter follows a fishing boat drawing circles over it.

Please, feel free to reuse code from this protocols when developing a new one. "None" is the better starting point developing a protocol where all the multicopters must follow a planned mission, while "Swarm protocol" is more adecuate when developing a protocol for a swarm.

## 5 Application workflow

As explained in section "[1 ArduSim architecture](#markdown-header-1-Ardusim-architecture)", ArduSim can be run performing three different roles. The next diagram shows the timeline of ArduSim execution since it is started until if finishes:

![workflow](ArduSimworkflow.svg)

Rectangular boxes represent the functions included in section "[6 Protocol implementation](#markdown-header-6-protocol-implementation)" that must be implemented by the developer, while comments are automatic processes performed by ArduSim.

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

The most important difference between "UAV agent" and "Simulator" roles implementation is the number of multicopters that run in the same machine, one in the former and many in the later. It is highly suggested to store data in array variables with the length of the number of UAVs that run in the machine, so in the real UAV the array length will be 1 and the code will be valid for both roles. Good implementation examples can be found in "MBCAP" and "Swarm protocol" protocols. A function is provided to know which ID (multicopter) corresponds to a specific position in the array, as detailed in section "[7.4 Available utilities](#markdown-header-74-available-utilities)". This function will provide the ID of the multicopter when running on a real UAV, as the array has a length of 1. On the other hand, no function is provided to get a UAV location in the array given the ID, as it will always be 0 in the real UAV and in simulation the ID is equivalent to the position in the array.

When ArduSim is run, it also checks if the computer meets some requirements and opens a general configuration dialog. There, the user can set many simulation parameters, as well as the maximum flight speed for each virtual multicopter. Then, when the configuration is set, the function "*openConfigurationDialog()*" may launch a dialog to introduce values for parameters of the protocol under development. Next, planned missions are loaded for the multicopters specified by the protocol.

In the next step the main window of the simulator is opened an IDs are assigned to the virtual UAVs. The function "*initializeDataStructures()*" allows the programmer to initialize the variables needed by the protocol. As stated before, it is highly recommended to use arrays with the length of the number of UAVs that are being run on the same machine at the same time. This way the code will be valid in simulation an running in a real multicopter.

In simulation, another difference compared to a "UAV agent" is the presence of a GUI. By default, the UAVs, their planned mission, if any, and the path they are following is being draw automatically. Nevertheless, the developer may want to draw additional elements, like other objects to define a safety fence around the UAVs or to show information related to the protocol. In that case, the function "*loadResources()*" allow the programmer to dynamically load images to be drawn. The function "*drawResources()*" can be used to draw this elements each time the screen is updated. The methods "*rescaleDataStructures()* and "*rescaleShownResources()*" allow to change the way the elements are drawn each time the drawing scale changes. Additionally, the method "*setInitialState()* can be implemented to show a text for each UAV with its initial state in the protocol that is shown when the "Progress dialog" is opened.

The function "*setStartingLocation()*" provides the starting location of the multicopters and then the simulation can start. Once the current location is acquired, the virtual UAV-to-UAV communication link is stablished and the collision detection is enabled. The functions "*sendInitialConfiguration()*", "*startThreads()*", "*setupActionPerformed()*", "*startExperimentActionPerformed()*", "*forceExperimentEnd()*", "*getExperimentResults()*", "*getExperimentConfiguration()*", and "*logData()*" have to be implemented the same way already explained for the "UAV agent" role.

There are another two differences compared to a "UAV agent". First, a set of pictures are downloaded from Google Static Maps to integrate the background image, which is geopositioned on the theoretical location of the virtual UAVs. Second, storing the experiment results is optional.

## 6 Protocol implementation

To start a new protocol, the developer creates a new package to contain the corresponding Java classes. Then, the first step consists in create a new class that "extends" "*src.api.ProtocolHelper.java*". This class forces the developer to implement the functions already mentioned, to integrate the protocol in ArduSim. An extended explanation of the functions follows:

* **setProtocol()**.


protocol available when extends Helper and setProtocol

explain protocol integration functions

## 7 Implementation details


### 7.1 UAV-to-UAV Communications

functions

### 7.2 UAV control

functions (including beta in controller class)

### 7.3 GUI integration

showing progress and drawing additional resources

### 7.4 Available utilities

functions in Tools.java

### 7.4 Implementation recomendations

how to use the same code for real and virtual UAVs
package structure