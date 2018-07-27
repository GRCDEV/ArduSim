# ArduSim usage

We already explained that ArduSim can be used in two was. First, it can be used to develop a new protocol, simulating the UAVs behavior for debugging until the protocol is ready to be deployed. Second, it can be used to deploy the new protocol in real multicopters, and playing two roles, as an *UAV agent* and as a *PC Companion* that helps to control de deployment from a laptop. This section explains in detail how to use ArduSim for simulation and for the deployment in real multicopters.

## Table of contents

[1 ArduSim on simulation](#markdown-header-1-ardusim-on-simulation)

[2 ArduSim on real multicopters](#markdown-header-2-ardusim-on-real-multicopters)

## 1 ArduSim on simulation

A simulation can be performed directly in Eclipse or from a *.jar* executable file. You need to run ArduSim with the parameter *-c false* to avoid it to run as a PC Companion. If you run ArduSim from Eclipse IDE you will need to set the parameter in the "run configuration".

We suggest to run ArduSim as Administrator (Windows, requires Imdisk Virtual Disk Driver) or sudoer (Linux). In this mode, temporary files are stored in a virtual hard drive, which speeds up ArduSim when using a slow HHDD, as it could limit the number of virtual multicopters that can be run simultaneously.

### 1.1 Simulation configuration

ArduSim starts showing the following dialog:

![ArduSim configuration dialog](config.png)

This dialog allows to introduce several simulation parameters:

* Simulation parameters:

    *ArduCopter path*.
    *Speeds file*.
    *Number of UAVs*.

* Performance parameters:

    *Screen refresh rate*.
    *Minimum screen redraw distance*.
    *Enable arducopter logging*.
    *Restrict battery capacity*.
    Measure CPU usage*.
    Rendering quality*.

* General parameters:

    *Enable verbose logging*.
    *

* UAV synchronization protocol:


* UAV to UAV communications parameters:


* UAV Collision detection parameters:


* Wind parameters:


al aceptar...


### 1.2 Protocol configuration


### 1.3 Main window





## 2 ArduSim on real multicopters



ArduSim can be executed with the following command line:

    java -jar ArduSim.jar -c <arg> [-r <arg> [-p <arg> -s <arg>]] [-h]

... diferentiate three roles as explained in the previous section







[//]: # (Al guardar lo del diálogo results, indicar los ficheros que se guardan con su contenido)