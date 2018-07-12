# Protocol development



## 1 ArduSim architecture

ArduSim is able to run performing three different roles:

* Simulator. The application runs on a PC and uses a SITL instance for each virtual multicopter that it simulates. A GUI is used to control the experiment.
* UAV agent. The application runs on a Raspberry Pi 3 attached to a multicopter. The experiment is controlled from a PC Companion.
* PC Companion. The application runs on a Laptop and allows to control an experiment followed by any number of multicopters.

The code needed to run the PC Companion is completed and needs no further modification. When the protocol developer follows the included [recomendations](#markdown-header-implementation-recomendations), the same code used for simulation is also valid for a real multicopter, which makes the deployment on real devices somewhat trial.

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
* Copter. Includes methods to gather flight information from the multicopter or perform actions like changing the flight mode, as explained in detail in [7.2 UAV control](#markdown-header-7.2-uav-control)
* GUI.
* Tools.









### protocol packages (...)





## 5 Application workflow

roles and diagram

![workflow](ArduSimworkflow.svg)

## 6 Protocol implementation


explain protocol integration functions

## 7 Implementation details


### 7.1 UAV-to-UAV Communications

functions

### 7.2 UAV control

functions

### 7.3 GUI integration

showing progress and drawing additional resources

### 7.4 Implementation recomendations

how to use the same code for real and virtual UAVs