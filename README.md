# ArduSim

ArduSim is a novel flight simulator on real-time, oriented to the development of communications protocols, and flight coordination protocols among multicopters performing planned missions or forming a swarm. It is able to simulate up to 256 UAVs (aka. drones) simultaneously, if the hardware meets the recommended requirements.

The Communication with multicopters uses the MAVLink protocol, a *de facto* standard for current flight controllers, which makes the deployment of a new protocol on real UAVs a trivial task.

The communication among multicopters emulates an Ad-hoc WiFi network link in the 5 GHz frequency band, where all the data packets are broadcasted.


## Table of contents

[ArduSim setup](#markdown-header-ardusim-setup)

[Protocol development](#markdown-header-protocol-development)

[ArduSim usage](#markdown-header-ardusim-usage)

[Deployment on real devices](#markdown-header-deployment-on-real-devices---raspberry-pi-3-example)

[Copyright information](#markdown-header-copyright-information)

## ArduSim setup

Follow [this link](help/setup.md) for detailed instructions.

## Protocol development

Por explicar










## ArduSim usage

ArduSim can be executed with the following command line:

    java -jar ArduSim.jar [-r <arg>] [-doFake -p <arg> -s <arg>]







Recomendar ejecutar como root administrador para usar disco ram
En una terminal de linux o windows




## Deployment on real devices - Raspberry Pi 3 example

You can deploy the developed protocol on real UAVs just changing the ArduSim execution parameters, as explained before. In order to do this, the real multicopter must meet the following requirements:



As an example, in [this link](help/deployment.md) you can follow detailed instructions to setup de deployment on a muticopter with Pixhawk flight controller, with a Raspberry Pi 3 attached to it.  


## Copyright information

TODAVÍA EN ESTUDIO


