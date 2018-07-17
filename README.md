# ArduSim

ArduSim is a novel real-time flight simulator, oriented to the development of communications protocols, and flight coordination protocols among multicopters performing planned missions or forming a swarm. It is able to simulate up to 256 UAVs (aka. drones) simultaneously, if the hardware meets the recommended requirements.

The Communication with multicopters uses the MAVLink protocol, a *de facto* standard for current flight controllers, which makes the deployment of a new protocol on real UAVs a trivial task.

The communication among multicopters emulates an Ad-hoc WiFi network link in the 5 GHz frequency band, where all the data packets are broadcasted. ArduSim is prepared to include new wireless models in future versions.

At this moment, documentation is being generated and very incomplete.

## Table of contents

[ArduSim setup](#markdown-header-ardusim-setup)

[Protocol development](#markdown-header-protocol-development)

[ArduSim usage](#markdown-header-ardusim-usage)

[Deployment on real devices](#markdown-header-deployment-on-real-devices---raspberry-pi-3-b+)

[Copyright information](#markdown-header-copyright-information)

## ArduSim setup

Follow [this link](help/setup.md) for detailed instructions.

## Protocol development

Follow [this link](help/development.md) for detailed instructions.

## ArduSim usage

ArduSim can be executed with the following command line:

    java -jar ArduSim.jar -c <arg> [-r <arg> [-p <arg> -s <arg>]] [-h]

... diferentiate three roles as explained in the previous section





[//]: # (Recomendar ejecutar como root administrador para usar disco ram)
[//]: # (En una terminal de linux o windows)
[//]: # (Al guardar lo del diálogo results, indicar los ficheros que se guardan con su contenido)



## Deployment on real devices - Raspberry Pi 3 b+

You can deploy the implemented protocol on real UAVs just changing the ArduSim execution parameters, as explained before. In order to do this, the real multicopter must meet the following requirements:



As an example, in [this link](help/deployment.md) you can follow detailed instructions to setup de deployment on a muticopter with Pixhawk flight controller, with a Raspberry Pi 3 attached to it.  

## Cite

[//]: # (Explicar cómo citar, indicando el journal cuando se publique)

## Copyright information

ArduSim is published under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). Some third part libraries were used and are under their own license as detailed:

[ArduPilot](https://github.com/ArduPilot/ardupilot) is the single UAV simulator ArduSim is based on, and is built under the [GNU General Public License v3.0](https://github.com/ArduPilot/ardupilot/blob/master/COPYING.txt).

MAVLink libraries are used to communicate with real or virtual flight controller in native language and are built with [MAVLink Java generator and library](https://github.com/ghelle/MAVLinkJava) tool. [MAVLink message definition XML files](https://github.com/mavlink/mavlink/tree/master/message_definitions) are under the MIT-licence.

Apache Commons CLI is used to parse command line arguments and under [license](http://www.apache.org/licenses/LICENSE-2.0) from the Apache Software Foundation (ASF) and under one or more contributor license agreements.

The AtomicDoubleArray implementation comes from Doug Lea and under [CC0 1.0 Universal license](http://creativecommons.org/publicdomain/zero/1.0/), and is based on [Guava](https://github.com/google/guava) from Google, which is under [Apache license](https://github.com/google/guava/blob/master/COPYING).

The VerticalFlowLayout implementation comes from Vassili Dzuba and under [Artistic License](https://opensource.org/licenses/artistic-license-2.0).

[Kryo library](https://github.com/EsotericSoftware/kryo) is used to serialize transferred data and under [BSD 3-clause license](https://github.com/EsotericSoftware/kryo/blob/master/LICENSE.md), and requires three [libraries](https://github.com/EsotericSoftware/kryo/tree/master/lib): [minlog](https://github.com/EsotericSoftware/minlog) and [reflectASM](https://github.com/EsotericSoftware/reflectasm) under the same license, and [objenesis](http://objenesis.org/license.html) under [Apache license](http://www.apache.org/licenses/LICENSE-2.0).

[javatuples](https://www.javatuples.org/) is used to return multiple values on functions and under [Apache license](https://www.javatuples.org/license.html).

[RXTX](http://rxtx.qbang.org/wiki/index.php/Main_Page) is used to communicate with a real flight controller through serial port and under [LGPL v 2.1 license](http://www.gnu.org/licenses/lgpl.txt).

Statistical Machine Intelligence and Learning Engine ([Smile](https://github.com/haifengl/smile)) is used for the protocol Chemotaxis (core, data, interpolation and math modules) and under [Apache license](https://github.com/haifengl/smile/blob/master/LICENSE).

[Sigar](https://github.com/hyperic/sigar) is used to measure CPU ussage under Windows and is under [Apache license](https://github.com/hyperic/sigar/blob/master/LICENSE).

