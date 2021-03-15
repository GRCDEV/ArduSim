# ArduSim setup

The ArduSim simulator includes a few protocol examples. Once the project is cloned locally, and the protocol is developed and tested, you can generate the .jar executable file to perform simulations outside an IDE, or even deploy the protocol on real multicopters.

ArduSim uses [SITL](http://ardupilot.org/dev/docs/sitl-simulator-software-in-the-loop.html) as an internal module, a program oriented to simulate a single drone. It is already included in this repository for Linux computers, but if preferred you can install SITL in order to compile an executable multicopter optimized for your computer. Once finished the compilation (see instructions below), just copy the executable multicopter inside the target folder of the project, or next to the generated java file, and ArduSim will automatically notice the multicopter file.

## Table of contents

[System requirements](#1-system-requirements)

[IDE Integration](#2-ide-integration)

[Running ArduSim](#3-running-ardusim)

[Compilation to jar](#4-compilation-to-jar)

[Compile your own SITL files](#5-compile-your-own-sitl-files)


## 1 System Requirements

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

## 2 IDE integration

While not mandatory an IDE will aid any developer, therefore we give a guide on how to correctly import ArduSim into an IDE. We have chosen for Eclipse and IntelliJ because of their popularity and personal preferences. 
If your favourite IDE is not included, the guides can still help you since importing source from git is similar in the various IDEs. 

### 2.1 Eclipse IDE integration

ArduSim and the included protocols have been developed with Eclipse IDE. This section explains how to prepare the developing environment, once Eclipse and Java JDK are correctly installed.

Open Git perspective: *Window --> Perspective --> Open Perspective --> Other... --> Git*.

Use the button of the picture to start clonning the repository:

![clone button](clonebutton.png)

Now copy the following URI in the configuration window and enter your username and password:

    https://github.com/GRCDEV/ArduSim.git

Then, select the master branch, also "Import all existing Eclipse projects after clone finishes", and leave all the remaining options untouched. Please, take note where the repository is going to be cloned.

Finally, change to the default perspective: *Window --> Perspective -->Open Perspective --> Java*.

We recommend to show packages in hierarchical order in *Package explorer* tab. Open the dropdown menu and select *Package Presentation --> Hierarchical*.

*Git Staging* and *Git Repositories* tabs can also be shown to check periodically if a new version of ArduSim has been released.

Continue with this guide before running Main.java

### 2.2 IntelliJ IDE integration

In IntelliJ go to File --> New --> Project from Version Control.

Now copy the following URI in the configuration window.

    https://jamieWubben@bitbucket.org/frafabco/ardusim.git

If asked for a project configuration open as Maven project. Give IntelliJ some time to import everything. 

Continue with this guide before running Main.java


## 3 Running ArduSim
        
### 3.1 Checking necesarry files

ArduSim uses SITL to simulate multicopters. If everything went right the two files: *arducopter* and *copter.parm* should be include in the project (inside of the folder ArduSim/target). However, if prefered (it might increase performance) the SITL can be installed and new files can be compiled (explained in section 4). If you run ArduSim from an executable *.jar* file, put both files in the same folder. This repository provides binaries for Windows (arducopter.exe) and Ubuntu (arducopter) 64bits, so you only need to compile them (see below) if you plan to use other systems.

A csv file called speed.csv (inside ArduSim/target) should also be present. This csv file contains the target speed for the multicopters (one row per speed m/s). If not presented created it yourself.

### 3.2 Setup for Windows

The next steps must only be followed if you want to run ArduSim on Windows.

If any errors occur, please let us know and we will update this section. Possibly, more updated instructions can be found on the [official web page](http://ardupilot.org/dev/docs/sitl-native-on-windows.html) of ArduCopter, but it suggest installing JSBSim simulator, and to compile the SITL yourself. This is not needed for the correct functioning of ArduSim. For those reasons, we suggest to not use the script mentioned in the web page to install SITL, it is better to do it manually.

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


### 3.3 Starting Ardusim for the first time
* Make sure to have at least Java-11 installed.

* In your IDE right-click over *src/main/java/com/setup/Main.java* class and select Run as Java Application (or similar).

The first time, it will fail to run because you need to add some program arguments. There exist four options: *multicopter*, *pccompanion*, *simulator-gui*, and *simulator-cli*. The first two are used in real experiments and the last two are used for simulations. We advise to use the *simulator-gui* for developping and to use the *simulator-cli* to perform automatic testing. 

* Edit the run configuration in your IDE and change the program arguments to *simulator-gui*

Once you have checked that all the necesarry files (*arducopter*, *copter.parm* and *speed.csv*) are inside the folder *ArduSim/target* and you have changed the program arguments to *simulator-gui* you can start Ardusim.

* Run ArduSim

A configuration Dialog should pop up. For now, leave all the parameters as they are.

* press the OK button.
 
Using the default parameters ArduSim will start a certain protocol (FollowMe in this case). Most of the protocols have their own grafical interface to changes some arguments. For now, leave all the parameters as they are.

* press the OK button.

Most likely ArduSim will give you a pop-up that no bing key was found. This Bing key is used for rendering, and if it is not found open-street maps will be used.

* Press the Ok button

A full-sized screen should appear. In the upper-left corner you can see that ArduSim is setting everything up in the background. It will pause for a bit while it is waiting for a GPS fix. Finnaly, the button *setup* should be enabled. 

* Press the Setup button.

Observe how *UAV 1* is taking off. Once that is done, the *start test* button should be enabled. 

* Press the Start test button

Observe how UAV 0 is taking off, starts moving, and is followed by UAV 1. Wait until both UAVs are landed.

* Press the *exit* button.

You now performed your first simulation with ArduSim, congratulations! We now advise to repeat the last steps changing protocols and/or parameters. If you change the protocol make sure to also change the protocol parameters path.

Several temporary files are generated when using ArduSim for simulations. Please, close ArduSim with the application button and not from the *Console* tab of your IDE to force ArduSim to remove the temporary files before exiting.

Remember that ArduSim is a highly asynchronous application, and running it with an IDE in debugging mode could lead to an unexpected behavior of the application.


## 4 Compilation to jar

If you want to use ArduSim outside of the IDE, you have to compiled a *.jar*. The project has been prepared to be compiled both as Java application, or as Maven project. The straightforward solution is to compile and export a runnable Jar file like any other Java project. You can also compile and deploy ArduSim with Maven, but we do not recommend that approach for two reasons: i) it requires advanced knowlegde, and ii) the contents of the folder target will be cleaned, removing the *arducopter* application instance that should be there for simulations, and even the *ardusim.ini* file provided. In order to avoid further problems, we suggest storing a copy of those files anywhere else (*arducopter* or *arducopter.exe*, depending on the running platform, *copter.parm*, and *ardusim.ini*).

## 5 Compile your own SITL files

ArduSim uses SITL to simulate multicopters. You can if prefered compile your own SITL files *arducopter* and *copter.parm* and possibly gain some performance. This is however, only for advanced users and not necesarry to run ArduSim. Below we give a guide on how to do so. We try to be up-to-date. If you have some problems, you can always go to (http://ardupilot.org/dev/docs/sitl-native-on-windows.html) for the newest information. Finally, ArduSim does not support all the versions of the ArduCopter SITL. At this moment we support up to version 4.0.0.

### 5.1 Windows

1. Download ArduPilot. This is the project which enables the user to compile a multicopter or other kinds of UAVs. In the terminal, input these lines:

        git clone https://github.com/ArduPilot/ardupilot.git
        cd ardupilot
        git checkout tags/Copter-3.5.7
        git submodule update --init --recursive
    
    The first command clones the project in the local folder *ardupilot*. The last uploaded version of ArduPilot is usually unstable, and we highly recommend downgrading to the latest stable version for ArduCopter. In the cloned webpage open de *Branch* drop-down list, select the tab *Tags*, look for the most up-to-date version of *Copter*, and put in the third line the tag found. ArduSim has been tested with ArduCopter version 3.5.7, and we suggest using it, as the copter parameters have been modified since then. The last command downloads modules needed by the target ArduPilot compilation.

2. Make the multicopter. In the same *Cygwin terminal* go to the *ardupilot* folder and type:

        cd ArduCopter
        make sitl -j4
        sim_vehicle.py -w

    Once fully loaded, use "Ctrl+D" to close the running program. The second line builds the multicopter firmware *arducopter.elf*, and the third one uses MAVProxy to finally build the multicopter executable file *arducopter.exe* located in *C:\cygwin\home\user_name\ardupilot\build\sitl\bin*. Copy that file, and also the file *C:\cygwin\home\user_name\ardupilot\Tools\autotest\default_params\copter.parm* to the target folder of the Eclipse project to finish the basic setup process. If you plan to execute ArduSim in a real multicopter, copy these two files and *ardusim.ini* from the root of the Eclipse project beside the *.jar* file.

8. It is suggested (optional) to install [ImDisk Virtual Disk Driver](https://sourceforge.net/projects/imdisk-toolkit/) and run ArduCopter as Administrator in order to use a RAM Drive to store temporary files from the virtual multicopters. This setup speeds up the execution when running ArduSim on a computer with a slow hard drive.


### 5.2 Linux

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

### 5.3 SITL setup in MacOS

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
