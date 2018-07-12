# Deployment on real devices - Raspberry Pi 3

The first deployment of a protocol requieres to configure hardware and software. On later deployments it will be enough to copy the .jar executable file.

You are supposed to previously have a Pixhawk controlled multicopter and a Raspberry Pi 3 attached to it.

## Table of contents

[Hardware setup](#markdown-header-hardware-setup)

[Software setup](#markdown-header-software-setup)

## Hardware setup

### Raspberry - Pixhawk serial link

ArduSim communicates with the flight controller through serial port, so we need to stablish a connection between them.

A Pixhawk controller has two telemetry ports, one tipically used by one telemetry wireless transmitter and another available. On the other hand, a Raspberry Pi 3 has a 40 pins GPIO where we can connect the telemetry port as a serial 3.3V link, with a cable similar to the one shown on the next image (it needs modifications), following the instructions in the following [link](http://ardupilot.org/dev/docs/raspberry-pi-via-mavlink.html).

![cable](DF13cable.jpg)


### Wireless ad-hoc network

Regarding the communication among multicopters, we need to create an Ad-hoc network among them.

On a previous work ([On the impact of inter-UAV communications interference in the 2.4 GHz band](http://ieeexplore.ieee.org/document/7986413/)), we found that most of the remote controls available in the market jam the 2.4 GHz frequency band, so it is convenient to use an external WiFi adapter to use the 5 GHz band on Raspberry Pi 1, 2, and 3. We used an Alfa AWUS051NH dual band adapter on our setup. Rasbian already has this adapter driver, so we didn't need to install it.


## Software setup

### Raspberry - Pixhawk serial link

We have to follow two steps to successfully configure the Raspberry Pi 3. First, we have to enable the serial port link, and then we have to install the library that ArduSim uses to connect to the flight controller through the serial port.

#### Enabling serial port

The **ttyAMA0** serial port is disabled by default on the Raspberry Pi model 3 (not in the previous versions) to enable the bluetooth output through the GPIO connector, so we need to disable bluetooth to have that port available again. Edit the file */boot/config.txt/* and add this two lines (the first one could already be there):

    set enable_uart=1
    dtoverlay=pi3-miniuart-bt

Next, restart the device and check that the *ttyAMA0* port is available again with the next command:

    ls -l /dev

Raspbian, the Raspberry Pi operating system, may be using the serial port by default for the standard output, so it would send a lot of useless data to the flight controller. To avoid this, we have to run the Raspbian configuration utility with the next command, and disable console output to the serial port, but keeping the serial hardware enabled. Go to: *"Interfacing Options" - "Serial"*.

    sudo apt-get update
    sudo raspi-config

After rebooting and just to be sure, it is good idea check if the file */boot/cmdline.txt* contains *"console=serial0,115200"* for output, deleting this piece of text if it exists.

#### Enabling serial communication for ArduSim

Ardusim uses [RXTX library](http://rxtx.qbang.org/wiki/index.php/Main_Page) from Trent Jarvi et al. to communicate the Raspberry Pi with the flight controller, so you have to install it via Internet.

1. Create folders */home/pi/libs* and */home/pi/javalibs* to store the binary and Java components, respectively.

2. Download the file *rxtx-2.2pre2-bins.zip* from [this link](http://rxtx.qbang.org/wiki/index.php/Download), and copy the file *RXTXcomm.jar* that it contains to the folder */home/pi/javalibs*.

3. Edit the file */etc/environment* and add these two lines:

        JAVA_HOME="/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt"
        CLASSPATH="/home/pi/javalibs/RXTXcomm.jar"

    Please, check if the Java home path matches with your installed Java version (Java 8 is already included with Raspbian).

4. Install the binary library version 2.2pre1. When you run ArduSim you will notice a warning message in the console advising of a version mismatch between the binary and the Java libraries versions. Don't take care, it is normal and causes no problems.

        sudo apt-get install librxtx-java

5. Edit the file */home/pi/.bashrc* adding the following lines at the end:

        export JAVA_HOME="/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt"
        export PATH=$PATH:$JAVA_HOME/bin
        export CLASSPATH=/home/pi/javalibs/RXTXcomm.jar
        export LD_LIBRARY_PATH=/home/pi/lib

    As in step 3, check the installed Java path.

6. Create two symbolic links to the binary library in the folders */home/pi/libs* and */usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/arm*:

        sudo ln -s /usr/lib/jni/librxtxSerial-2.2pre1.so /home/pi/libs/librxtxSerial.so
        sudo ln -s /usr/lib/jni/librxtxSerial-2.2pre1.so /usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/arm/librxtxSerial.so

    Please, check if the paths match with your installed library and Java version.

Finally, restart the Raspberry pi 3 for the changes to take effect.


### Wireless ad-hoc network

1. Change the regulatory region to yours with the *Raspberry Pi Configuration*  tool, on the tab *Localisation*, option *set wifi region*, and then restart the device. You must inquire if your country allows to stablish an Ad-hoc network in the 5 GHz band with these commands:

        iw reg get (shows current limitations)
        iwlist wlan1 channel (shows allowed frequencies)

    One or both commands will show your limitations on different frequency ranges. You are not allowed to do an Ad-hoc network on a specific frequency if a text like *"no-IBSS"* appears. In that case, you should try on the 2.4 GHz band.

2. Network configuration. Our setup is done in the 5.18 GHz frequency (channel 36), and the wireless adapter identifier was wlan1, so edit wlan1 configuration in the file */etc/network/interfaces* and leave others adapters untoched:

        auto wlan1
        iface wlan1 inet static
        address 192.168.1.2
        netmask 255.255.255.0
        wireless-mode ad-hoc
        wireless-channel 36
        wireless-essid NETWORK_NAME

    We use a static network address named *NETWORK_NAME*. You have to change the network address for each multicopter used in the group/swarm.

    We found that Raspbian changes randomly the wireless adapter identifier when using more than one at the same time. As the Raspberry Pi 3 already has an integrated 2.4 GHz adapter and we use an external 5 GHz adapter, this issue avoids ArduSim from working adequately sometimes. To solve this issue, you have to fix the adapters identifier editing the file */lib/udev/rules.d/75-persistent-net-generator.rules* and replace the corresponding line with:

        KERNEL!="eth*[0-9]|ath*|wlan*[0-9]|msh*|ra*|sta*|ctc*|lcs*|hsi*", \

Then, unplug the external adapter and restart the device, turn it on, and when it has fully booted plug in the adapter. /etc/udev/rules.d/70-persistent-net.rules should be created with definitions for persistent rules for wlan0 and wlan1. Now check that the configuration already set in this chapter is applied to the correct wlanX adapter.


### ArduSim autostart

You can start ArduSim with a remote SSH connection from a computer once the multicopter and the Raspberry Pi 3 are turned on, but it is more practical to start ArduSim automatically on the Raspberry startup. To do so, we wrote a simple script (*start.sh*)with the following content:

    #!/bin/bash
    java -jar /home/pi/Desktop/ArduSim.jar 2>&1 | tee -a /home/pi/Desktop/log.txt

These command allows us to execute the application and, at the same time, shows and stores the standard output in a file.

We found an additional problem with the wireless ad-hoc network. It seems that the wireless regulatory region is set after the adapter uses this configuration. It changes successfully to ad-hoc mode, but it can't change to 5.18 GHz frequency at the same time. To solve this issue, we included another line just before the execution of the application, in the same script (*start.sh*):

    echo sudoerpassword | sudo -S iwconfig wlan1 freq 5.18G

The script will run each time the Raspberry Pi 3 start, just adding the following line to the end of the file */home/pi/.config/lxsession/LXDE-pi/autostart*:

    @/usr/bin/lxterminal -e /home/pi/Desktop/inicio.sh

