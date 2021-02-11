# ArduSim setup

This Eclipse Oxygen project includes the ArduSim simulator implementation, and a few protocol examples. Once the project is cloned locally, and the protocol is developed and tested, you can generate the .jar executable file to perform simulations outside Eclipse, or even deploy the protocol on real multicopters.

ArduSim uses [SITL](http://ardupilot.org/dev/docs/sitl-simulator-software-in-the-loop.html) as an internal module, a program oriented to simulate a single drone. It is already included in this repository for Linux computers, but it is recommeded to install SITL in order to compile an executable multicopter optimized for your computer. Once finished the compilation (see instructions below), just copy the executable multicopter inside the target folder of the project, or next to the generated java file, and ArduSim will automatically notice the multicopter file.

## Table of contents

[1 System requirements](#markdown-header-1-system-requirements)

[2 IDE integration](#markdown-header-2-ide-integration)

[3 SITL setup in Windows](#markdown-header-3-sitl-setup-in-windows)

[4 SITL setup in Linux](#markdown-header-4-sitl-setup-in-linux)

[5 SITL setup in MacOS](#markdown-header-5-sitl-setup-in-macos)

## 1 System requirements

###### Minimal:

* Intel core i5 (four cores version)

* RAM 6 GB

* Microsoft Windows 7, Linux or MacOS

* Java SE Development Kit (JDK) 11 32 bits (version 9 would be enough but it is now deprecated)

* Cygwin and ImDisk Virtual Disk Driver (Windows only)

###### Recommended:

* Intel core i7 (four cores with Hyper-Threading)

* RAM 16 GB

* Linux (i.e. Ubuntu 16.04 or Ubuntu 18.04)

* Java SE Development Kit (JDK) 11 64 bits (version 9 would be enough but it is now deprecated)

* Cygwin and ImDisk Virtual Disk Driver (Windows only)

It is highly recommended to run ArduSim on Linux, as performance is significantly reduced on Windows, and may be on MacOS.

## 2.1 IDE integration

While not mandatory a IDE will aid any developer, therefore we give a guide on how to correctly import ArduSim into an IDE. We have chosen for Eclipse and IntelliJ because of their popularity and personal preferences. 
If your favorite IDE is not included, the guides can still help you since importing source from git is similar in the various IDEs. 

### 2.1.1 Eclipse IDE integration

ArduSim and the included main.java.com.api.protocols have been developed with Eclipse IDE. This section explains how to prepare the developing environment, once Eclipse and Java JDK are correctly installed.

Open Git perspective: *Window --> Perspective --> Open Perspective --> Other... --> Git*.

Use the button of the picture to start clonning the repository:

![clone button](clonebutton.png)

Now copy the following URI in the configuration window and enter your user name and password:

    https://jamieWubben@bitbucket.org/frafabco/ardusim.git

Then, select the master branch, also "Import all existing Eclipse projects after clone finishes", and leave all the remaining options untouched. Please, take note where the repository is going to be cloned.

Finally, change to the default perspective: *Window --> Perspective -->Open Perspective --> Java*.

We recommend to show packages in hierarchical order in *Package explorer* tab. Open the dropdown menu and select *Package Presentation --> Hierarchical*.

*Git Staging* and *Git Repositories* tabs can also be shown to check periodically if a new version of ArduSim has been released.

### 2.1.2 IntelliJ IDE integration

In IntelliJ go to File --> New --> Project from Version Control.

Now copy the following URI in the configuration window.

    https://jamieWubben@bitbucket.org/frafabco/ardusim.git

IntelliJ will find two project configurations (Eclipse project and Maven project), select Maven project. Give IntelliJ some time to import everything continue with this guide before running Main.java


## 2.2 Copy necessary files

ArduSim uses SITL to simulate multicopters. Follow the steps in the next sections in order to get two files: *arducopter* and *copter.parm*. These files must be put in a place easy to find in order to execute simulations when running ArduSim. If you put them in the target folder (ardusim/target*), ArduSim will automatically find them when running from the IDE, avoiding to manually select the *arducopter* file each time it is launched. On the other hand, if you run ArduSim from an executable *.jar* file, put both files in the same folder. This repository provides binaries for Windows (arducopter.exe) and Ubuntu (arducopter) 64bits, so you only need to compile them (see below) if you plan to use other systems.

Several temporary files are generated when using ArduSim for simulations. Please, close ArduSim with the application button and not from the *Console* tab of Eclipse to force ArduSim to remove the temporary files before exiting.

Remember that ArduSim is a highly asynchronous application, and running in Eclipse in debugging mode could lead to an unexpected behavior of the application.

Finally, In Eclipse IDE right-click over *src/main/Main.java* class on the *Package Explorer* and select *Run as...*, and *1 Java Application*. In IntelliJ right-click over *src/main/Main.java* class on the *Project Files* and select *Run Main.main()*

The first time, it will fail to run because you need to add the following argument to the command line to be able to run a simulation, but not a PC Companion or a real multicopter, roles that are explained in other sections. To this aim, edit the run configuration automatically created.

    simulator

You can create a csv file with target speed for the multicopters (one row per speed m/s), and a main.java.com.protocols.mission file in *Google Earth* and test the simulator with the protocol *Mission* or *MBCAP* to be sure that the setup is correct.

A protocol can be tested directly in the IDE or from a executable *.jar* file. In the second case, with the proper configuration (see next sections), we suggest to execute the *.jar* file as Administrator/root. This way, temporary files will be stored in a RAM drive, which will increase ArduSim scalability, on the number of virtual multicopters, when using a slow hard drive.

### 2.2 Compilation

The project has been prepared to be compiled both as Java application, or as Maven project. The straightforward solution is to compile and export a runnable Jar file like any other Java project. You can also compile and deploy ArduSim with Maven, but we do not recommend that approach for two reasons: i) it requires advanced knowlegde, and ii) the contents of the folder target will be cleaned, removing the *arducopter* application instance that should be there for simulations, and even the *ardusim.ini* file provided. In order to avoid further problems, we suggest to store a copy of those files anywhere else (*arducopter* or *arducopter.exe*, depending on the running platform, *copter.parm*, and *ardusim.ini*).

## 3 SITL setup in Windows

The next steps must be followed in order to compile a multicopter. Alternatively you can follow the instructions included in the [official web page](http://ardupilot.org/dev/docs/sitl-native-on-windows.html), possibly more updated, but it suggest to install JSBSim simulator that is not needed for the correct functioning of ArduSim. This is the reason why we suggest to not use the script mentioned in the web page to install SITL, it is better to do it manually.

1. Install [Java JDK 11 or 13](https://www.oracle.com/java/technologies/javase-downloads.html) if not present.

2. MAVProxy. [Download](http://firmware.ardupilot.org/Tools/MAVProxy/MAVProxySetup-latest.exe), install it with the default configuration options, and don't start it. This application is required for arducopter compilation and will no longer be used.

3. Cygwin. It is a Linux type command line emulator that enables us to compile and run the virtual multicopter.

    1. Download and run Cygwin [64-bit](https://cygwin.com/setup-x86_64.exe) or [32 bit](https://cygwin.com/setup-x86.exe) installer and accept all the prompts (including default file locations) until you reach the Select Packages dialog. There, with a left click select the packages listed below (search for each one using the text in the "Name" field of the table):

        Name | Category/Name/Description
        --- | ---
        autoconf | Devel - autoconf: Wrapper scripts for autoconf commands
        automake | Devel - automake: Wrapper for multiple versions of Automake
        ccache | Devel - ccache: A C compiler cache for improving recompilation
        g++ | Devel - gcc-g++: GNU Compiler Collection (C++)
        git | Devel - git: Distributed version control system
        libtool | Devel - libtool: Generic library support script
        make | Devel - make: The GNU version of the ‘make’ utility
        gawk | Interpreters - gawk: GNU awk, a pattern scanning and processing language
        libexpat | Libs - libexpat-devel: Expat XML parser library (development files)
        libxml2-devel | Libs - libxml2-devel: Gnome XML library (development)
        libxslt-devel | Libs - libxslt-devel: GNOME XSLT library (development)
        python2-devel | Python - python2-devel: Python2 language interpreter
        procps | System - procps-ng: System and process monitoring utilities

    2. When all the packages are selected, click through the rest of the prompts and accept all other default options (including the additional dependencies). From now, we will use *Cygwin* to refer indistinctly to the 32 (*Cygwin*) and 64 bits (*Cygwin64*) installed program and folders.
    3. Open a *Cygwin terminal* from the desktop to initialize user files and close it.


4. Set up folders/paths in Cygwin. This procedure makes it easy to execute simulated vehicles under SITL (sim_vehicle.py will be found from anywhere), but it is not strictly needed to just compile a multicopter, if the next steps are followed. Edit the **.bashrc** file located on the user folder *C:\cygwin\home\user_name\.bashrc*, to add the following line. Preferably use the vi editor integrated with Cygwin, as the file is directly located on the folder where the *Cygwin terminal* opens. Otherwise, use any Windows text editor, but then you have to remove later all the carriage returns (*\r*) with *"sed -i 's/\r//g' .bashrc"* in a *Cygwin terminal*.

        export PATH=$PATH:$HOME/ardupilot/Tools/autotest

5. Install required Python packages.

    Open a *Cygwin terminal* from the desktop and install the following packages:

        python -m ensurepip --user
        python -m pip install --user future
        python -m pip install --user lxml
        python -m pip install --user uavcan

6. Download ArduPilot. This is the project which enables the user to compile a multicopter or other kinds of UAVs. In the terminal, input this lines:

        git clone https://github.com/ArduPilot/ardupilot.git
        cd ardupilot
        git checkout tags/Copter-3.5.7
        git submodule update --init --recursive
    
    The first command clones the project in the local folder *ardupilot*. The last uploaded version of ArduPilot is usually unstable, and we highly recommend to downgrade to the latest stable version for ArduCopter. In the cloned webpage open de *Branch* drop-down list, select the tab *Tags*, look for the most up-to-date version of *Copter*, and put in the third line the tag found. ArduSim has been tested with ArduCopter version 3.5.7, and we suggest to use it, as the copter parameters have been modified since then. The last command downloads modules needed by the target ArduPilot compilation.

7. Make the multicopter. In the same *Cygwin terminal* and already within the *ardupilot* folder type:

        cd ArduCopter
        make sitl -j4
        sim_vehicle.py -w

    Once fully loaded, use "Ctrl+D" to close the running program. The second line builds the multicopter firmware *arducopter.elf*, and the third one uses MAVProxy to finally build the multicopter executable file *arducopter.exe* located in *C:\cygwin\home\user_name\ardupilot\build\sitl\bin*. Copy that file, and also the file *C:\cygwin\home\user_name\ardupilot\Tools\autotest\default_params\copter.parm* to the target folder of the Eclipse project to finish the basic setup process. If you plan to execute ArduSim in a real multicopter, copy these two files and *ardusim.ini* from the root of the Eclipse project beside the *.jar* file.

8. It is suggested (optional) to install [ImDisk Virtual Disk Driver](https://sourceforge.net/projects/imdisk-toolkit/) and run ArduCopter as Administrator in order to use a RAM Drive to store temporary files from the virtual multicopters. This setup speeds up the execution when running ArduSim on a computer with a slow hard drive.


## 4 SITL setup in Linux

The next steps must be followed in order to compile a multicopter. Alternatively, you can follow the instructions included in the [official web page](http://ardupilot.org/dev/docs/setting-up-sitl-on-linux.html), possibly more updated. All steps are done in the same terminal. The process is explained for Debian based sistems (Ubuntu, Mint...). For RPM based systems (CentOS, Fedora...) use *yum* installer and install manually the dependencies included in the script mentioned later on.
The following steps are tested on Ubuntu 16.04 or Ubuntu 18.04. There are some (yet unsolved) bugs for Ubuntu 20.04, while we are working on sloving them we suggest to just use the binary files given in the repository (ArduCopter 3.5.7) and skip the following steps.  

1. Install Java JDK 11 or higher if not present. Both Oracle and openJDK can be used, Oracle is prefered because it can improve performance. However, Oracle has made installation a bit cumbersome (creating an account, etc.). Therefore, we provide the openJDK installation. 

        sudo apt install openjdk-14-jre-headless
        sudo apt install openjdk-14-jdk-headless
        java -version

2. Install git if not present:

        sudo apt-get install git
        sudo apt-get install gitk git-gui

3. Download ArduPilot. This is the project which enables the user to compile a multicopter or other kinds of UAVs. In a terminal, go to your home folder (*/home/user_name*) and input this lines:

        git clone https://github.com/ArduPilot/ardupilot.git
        cd ardupilot
        git checkout tags/Copter-3.5.7 (where 3.5.7 represents any ArduCopter version to be used)
        git submodule update --init --recursive

4. Be sure that you have Python 2.x installed:

        python --version (to check if it is already installed)
        sudo apt-get install python-minimal python-pip (to install)

5. Use the following command to install the required packages and reload the path with the *dot* command:

    5.1 For ArduCopter version 3.5.7 install the following packages:

        sudo apt-get remove modemmanager
        sudo apt-get install python-matplotlib python-serial python-wxgtk3.0 python-wxtools python-lxml
        sudo apt-get install python-scipy python-opencv ccache gawk python-pexpect
        sudo pip install future pymavlink MAVProxy

    5.2 For ArduCopter versions 3.6.x you have to run a script, but it points to an URL that must be updated to work. Edit the file *Tools/scripts/install-prereqs-ubuntu.sh* and modify the following line:
    
        ARM_TARBALL_URL="http://firmware.ardupilot.org/Tools/PX4-tools/$ARM_TARBALL" (current line)
        ARM_TARBALL_URL="http://firmware.ardupilot.org/Tools/PX4-tools/archived/$ARM_TARBALL" (new line)

    If you have Google Earth installed, you must run the following command or the script will fail:

        wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo apt-key add -

    Finally, from the same folder run the script and update the profile:

        Tools/scripts/install-prereqs-ubuntu.sh -y
        . ~/.profile

    5.3 For ArduCopter versions 4.x.x the script points to the right location, so the first problem is solved, but if you have Google Earth installed you already have a problem to solve:

        wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo apt-key add -

    Finally, from the same folder run the script and update the profile:

        Tools/environment_install/install-prereqs-ubuntu.sh -y
        . ~/.profile

6. Make the multicopter:

        cd ArduCopter
        ../Tools/autotest/sim_vehicle.py -w

    The command will start the compilation process, and when it finishes it should show a message like "IMU1 is using GPS", which means that the virtual copter is running. Then, use "Ctrl+C" to close the running program. The multicopter runnable file *arducopter* is located in *ardupilot/build/sitl/bin*. Copy that file, and also the file *ardupilot/Tools/autotest/default_params/copter.parm* to the target folder of the Eclipse project to finish the basic setup process. If you plan to execute ArduSim in a real multicopter, copy these two files and *ardusim.ini* from the root of the Eclipse project beside the *.jar* file.

## 5 SITL setup in MacOS

The next steps must be followed in order to compile a multicopter in MacOS. Provided instructions use [Macports](https://www.macports.org/) or [Homebrew](https://brew.sh/) software repository systems to install dependencies. You have to chose which to use and install the corresponding package manager.

1. Setup the developer environment

    1. Execute Git to install Xcode:

            git

    2. Accept xcode license:

            sudo xcodebuild -license

2. Execute Java to install it:

        java

3. Install a package repository system (Macports or Homebrew):

    * Macports:

        1. Install Macports: Download [installer](https://www.macports.org/install.php) and follow the included instructions.
        2. Update port definitions & update macports if necessary:

                sudo port -v selfupdate

    * Homebrew:

            /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

4. Download ArduPilot. This is the project which enables the user to compile a multicopter or other kinds of UAVs.

        git clone git://github.com/ArduPilot/ardupilot.git
        cd ardupilot
        git checkout tags/Copter-3.5.7
        git submodule update --init --recursive

5. Install gcc 7 to compile the multicopter:

    * Macports:

            sudo port install gcc7
            sudo port select gcc mp-gcc7

    * Homebrew:

            brew install gcc

6. Install required dependencies:

    * Macports:

            sudo port install python27 py-pip
            sudo port select python python27
            sudo port select pip pip27

    * Homebrew:

            brew install python

7. Install python dependencies:

        pip install --user pyserial

8. Install mavproxy and mavlink:

        pip install --user mavproxy pymavlink
        echo "export PATH=$PATH:$HOME/Library/Python/2.7/bin" >> ~/.bashrc
        . ~/.bashrc

9. Make the multicopter:

        export CC=gcc; export CXX=g++;./waf configure --board sitl
        ./waf --targets bin/arducopter --jobs 1

    Copy the multicopter executable file *arducopter* located in *ardupilot/build/sitl/bin*, and also the file *ardupilot/Tools/autotest/default_params/copter.parm* to the target folder of the Eclipse project to finish the basic setup process. If you plan to execute ArduSim in a real multicopter, copy these two files and *ardusim.ini* from the root of the Eclipse project beside the *.jar* file.