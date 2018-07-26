# Deployment on real devices - Raspberry Pi 3 B+

The first deployment of a protocol requieres to configure hardware and software of one or more real multicopters. On later deployments, it will be enough to copy some files to the Raspberry Pi as detailed in [this link](setup.md#markdown-header-23-copy-necessary-files).

You are supposed to previously own a Pixhawk controlled multicopter, and a Raspberry Pi 3 B+ with Raspbian OS attached to it.

## Table of contents

[Hardware setup](#markdown-header-1-hardware-setup)

[Software setup](#markdown-header-2-software-setup)

## 1 Hardware setup

### 1.1 Raspberry - Pixhawk serial link

ArduSim communicates with the flight controller through serial port, so we need to stablish a connection between them.

A Pixhawk controller has two telemetry ports, one tipically used by one telemetry wireless transmitter and another available. On the other hand, a Raspberry Pi 3 has a 40 pins GPIO where we can connect the telemetry port as a serial 3.3V link, with a cable similar to the one shown on the next image (it needs modifications), following the instructions in the following [link](http://ardupilot.org/dev/docs/raspberry-pi-via-mavlink.html).

![cable](DF13cable.jpg)

### 1.2 Wireless ad-hoc network

Regarding the communication among multicopters, we need to create an Ad-hoc network among them.

On a previous work ([On the impact of inter-UAV communications interference in the 2.4 GHz band](http://ieeexplore.ieee.org/document/7986413/)), we found that most of the remote controls available in the market jam the 2.4 GHz frequency band, so it is convenient to use an external WiFi adapter to use the 5 GHz band on Raspberry Pi 1, 2, and 3. We used an Alfa AWUS051NH dual band adapter on our setup. Rasbian already has this adapter driver, so we didn't need to install it.

## 2 Software setup

### 2.1 Raspberry - Pixhawk serial link

We have to follow two steps to successfully configure the Raspberry Pi 3. First, we have to enable the serial port link, and then we have to install the library that ArduSim uses to connect to the flight controller through the serial port.

#### Enabling serial port

Raspbian, the Raspberry Pi operating system, may be using the serial port by default for the standard output, so it would send a lot of useless data to the flight controller. To avoid this, we have to keep the serial port enabled while disabling the output. Open the GUI tool in "Preferences", and enable "Serial Port" and disable "Serial Console" in the "Interfaces" tab. Alternatively, you can use the console utility with the following commands. Then go to *"Interfacing Options" - "Serial"* and enable it, but then you must check the file */boot/cmdline.txt* after reboot and remove the text *"console=serial0,115200"* if found.

    sudo apt-get update
    sudo raspi-config

Finally we have to enable the **ttyAMA0** serial port, which is disabled by default on the Raspberry Pi model 3 (not in the previous versions) to be able to use the bluetooth output through the GPIO connector, so we need to swap serial and bluetooth ports. Edit the file */boot/config.txt/* and add this two lines (the first one could already be there):

    enable_uart=1
    dtoverlay=pi3-miniuart-bt

Alternatively, you can completely disable bluetooth with this overlay:

    dtoverlay=pi3-disable-bt

Next, restart the device and check that the *ttyAMA0* port is available again with the next command (a line must show: serial0 -> ttyAMA0):

    ls -l /dev

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
        export LD_LIBRARY_PATH=/home/pi/libs

    As in step 3, check the installed Java path.

6. Create two symbolic links to the binary library in the folders */home/pi/libs* and */usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/arm*:

        sudo ln -s /usr/lib/jni/librxtxSerial-2.2pre1.so /home/pi/libs/librxtxSerial.so
        sudo ln -s /usr/lib/jni/librxtxSerial-2.2pre1.so /usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/arm/librxtxSerial.so

    Please, check if the paths match with your installed library and Java version.

Finally, restart the Raspberry pi 3 for the changes to take effect.


### 2.2 Wireless ad-hoc network

1. Check the regulatory region used for the WiFi adapter and allowed frequencies:

        iw reg get (shows current limitations and the country set)
        iwlist wlan0 channel (shows allowed frequencies)

    One or both commands will show the limitations on different frequency ranges. You are not allowed to do an Ad-hoc network on a specific frequency if a text like *"no-IBSS"* appears.
    
    If your current region is applied and it is forbidden to stablish an Ad-hoc network in the 5 GHz frequency band you should use the 2.4 GHz band. Otherwise,if your region is not applied, you must change it with the *Raspberry Pi Configuration*  tool, on the tab *Localisation*, option *set wifi region*, and then restart the device.

2. Network configuration. Raspbian Jessie and Stretch have changed the way a network is configured. The file */etc/network/interfaces* must remain untouched. Please, generate the file */etc/network/interfaces.d/wlan0* with the following content:

        auto wlan0
        iface wlan0 inet static
        address 192.168.1.2
        netmask 255.255.255.0
        wireless-channel 36
        wireless-essid NETWORK_NAME
        wireless-mode ad-hoc

    Now edit the file */etc/dhcpcd.conf* and add the following command at the end of the file:
    
        denyinterfaces wlan0

    Finally restart the Raspberry Pi. This way we leave the loopback interface untouched, and ethernet connection under DHCP control. We use a static network address named *NETWORK_NAME*. You also have to change the network address for each multicopter used in the group/swarm. We have found that the network manager makes a mess and thinks that the regulatory domain (WiFi country) is unset when using Raspbian in desktop mode. Don't care about it, as you can check, the Ad-hoc network is up and functioning once you restart the device (network manager becomes useless).

    May be you are using other wireless adapters. If this is the case, we found that Raspbian changes randomly the wireless adapter identifier when using more than one at the same time. This issue could avoid ArduSim from working adequately sometimes. To solve it, you have to fix the adapters identifier editing the file */lib/udev/rules.d/75-persistent-net-generator.rules* and replace the corresponding line with:

        KERNEL!="eth*[0-9]|ath*|wlan*[0-9]|msh*|ra*|sta*|ctc*|lcs*|hsi*", \

    Then, unplug the external adapter and restart the device, and when it has fully booted plug in the  external adapter. /etc/udev/rules.d/70-persistent-net.rules should be created with definitions for persistent rules for wlan0 and wlan1. Now check that the configuration already set in this chapter is applied to the correct wlanX adapter.

### 2.3 ArduSim autostart

You can start ArduSim with a remote SSH connection from a computer once the multicopter and the Raspberry Pi 3 are turned on, but it is more practical to start ArduSim automatically on the Raspberry startup. To do so, we wrote a simple service (*start.service*) with the following content:

    [Unit]
    Description=ArduSim
    After=network-online.target
    Wants=network-online.target
    
    [Service]
    Type=oneshot
    RemainAfterExit=true
    ExecStart=/sbin/ifconfig wlan0
    ExecStart=/sbin/iwconfig wlan0
    ExecStart=/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/bin/java -jar /home/pi/Desktop/ArduSim.jar -c false -r true -p "PROTOCOL" -s 2.5
    WorkingDirectory=/home/pi/Desktop/
    StandardOutput=syslog
    StandardError=syslog
    SyslogIdentifier=ardusim
    Restart=no
    User=pi
    
    [Install]
    WantedBy=multi-user.target

This service allows us to execute the application and, at the same time, shows and stores the standard output in a file. It waits the network to be configured and runs ArduSim with the protocol *"PROTOCOL"*, and with a maximum speed of 2.5 m/s for the multicopter. The first two *ExecStart* commands are optional and could be used for debugging purposes, as it shows the configuration of the WiFi adapter and let's you check if the ad-hoc network is correctly configured at system startup.

To store the output of ArduSim to a file, we also need to specify th target file to the system log service. Create the file */etc/rsyslog.d/ardusim.conf* with the following content:

    if $programname == 'ardusim' then /home/pi/Desktop/ArduSim.log
    if $programname == 'ardusim' then ~

Each time the service writes something to the stdout or sterr, it will be redirected to the specified file. To enable modifications restart the logging service:

    sudo systemctl restart rsyslog

Next, copy the service file and test it to be sure that it is working:

    sudo chmod 644 start.service
    sudo cp start.service /etc/systemd/system/start.service
    sudo systemctl daemon-reload
    sudo systemctl start start.service

Check the content of the file */home/pi/Desktop/Ardusim.log* to be sure that the service is working fine. If the service fails or behaves unexpectedly, stop the service an repeat the previous commands, but the first, until the service works fine. Then, use the following command to enable the service on startup:

    sudo systemctl enable start.service

Finally, restart the device and check the log file to be sure that ArduSim has started with the system. Don't forget to store a mission file with ArduSim if the protocol under test requires it.