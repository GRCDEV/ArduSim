# ArduSim setup

This Eclipse Oxygen (4.7.1a) project includes the ArduSim simulator implementation. Once the project is cloned locally, please generate the .jar executable file.

ArduSim uses [SITL](http://ardupilot.org/dev/docs/sitl-simulator-software-in-the-loop.html) as an internal module, a program oriented to simulate a single drone, so it is also needed to install SITL, and to compile an executable multicopter from it. Once finished the compilation (see instructions below) just copy the executable multicopter next to the java file and the simulator will automatically notice the multicopter file.

## Table of contents

[System requirements](#markdown-header-system-requirements)

[SITL setup in Windows](#markdown-header-sitl-setup-in-windows)

[SITL setup in Linux)](#markdown-header-sitl-setup-in-linux-(tested-in-Ubuntu))

## System requirements

###### Minimal:

* Intel core i5 (four cores version)

* RAM 6 GB

* Microsoft Windows 7

* Java SE Runtime Environment 7 32 bits

###### Recommended:

* Intel core i7 (four cores with Hyper-Threading)

* RAM 16 GB

* Linux (i.e. Ubuntu 16.04)

* Java SE Runtime Environment 8 64 bits (or latest version)

* ImDisk Virtual Disk Driver (under Windows)

## SITL setup in Windows

The next steps must be followed in order to compile a multicopter. Alternatively you can follow the instructions included in the [official web](http://ardupilot.org/dev/docs/sitl-native-on-windows.html), possibly more updated, but it is not necessary to install JSBSim simulator, as suggested there, to the correct functioning of ArduSim. This is the reason why we suggest to not use the script mentioned in the web page to install SITL, it is better to do it manually.

1. Install [Java](https://www.java.com/es/download/) if not present.

2. MAVProxy. [Download](http://firmware.ardupilot.org/Tools/MAVProxy/MAVProxySetup-latest.exe) and install with default configuration options. This application is required for arducopter compilation and will no longer be used.

3. Cygwin. Linux type command line emulator that enables us to compile the multicopter.

	1. Download and run Cygwin [64-bit](https://cygwin.com/setup-x86_64.exe) or [32 bit](https://cygwin.com/setup-x86.exe) installer. Accept all the prompts (including default file locations) until you reach the Select Packages dialog. There, with a left clic select the packages listed below (search for each one using the text in the "Name" field):

		Name | Category/Name/Description
		--- | ---
		autoconf | Devel &#124; autoconf: Wrapper scripts for autoconf commands
		automake | Devel &#124; automake: Wrapper for multiple versions of Automake
		ccache | Devel &#124; ccache: A C compiler cache for improving recompilation
		g++ | Devel &#124; gcc-g++ GNU Compiler Collection (C++)
		git | Devel &#124; git: Distributed version control system
		libtool | Devel &#124; libtool: Generic library support script
		make | Devel &#124; make: The GNU version of the ‘make’ utility
		gawk | Interpreters &#124; gawk: GNU awk, a pattern scanning and processing language
		libexpat | Libs &#124; libexpat-devel: Expat XML parser library (development files)
		libxml2-devel | Libs &#124; libxml2-devel: Gnome XML library (development)
		libxslt-devel | Libs &#124; libxslt-devel: GNOME XSLT library (development)
		python2-devel | Python &#124; python2-devel: Python2 language interpreter
		procps | System &#124; procps-ng: System and process monitoring utilities
   
	2. When all the packages are selected, click through the rest of the prompts and accept all other default options (including the additional dependencies) until the end of the setup. From now we will use *Cygwin* to refer indistinctly to the 32 (*Cygwin*) and 64 bits (*Cygwin64*) installed program and folders.
   
4. Set up folders/paths in Cygwin. This procedure makes it easy to execute simulated vehicles under SITL (sim_vehicle.py will be found from anywhere), but it is not strictly needed to just compile a multicopter, if the next steps are followed.

	1. Open a *Cygwin terminal* from the desktop to initialize user files.
	2. Edit the **.bashrc** file located on the user folder *C:\cygwin\home\user_name\.bashrc*, to add the following line. Preferably use the vi editor integrated with Cygwin, so the file is directly located on the folder where the *Cygwin terminal* opens. Otherwise, use any Windows text editor, but then you have to remove later carriage returns (*\r*) with *"sed -i 's/\r//g' .bashrc"* in a *Cygwin terminal*.

		~~~~
		export PATH=$PATH:$HOME/ardupilot/Tools/autotest
		~~~~

5. Install required Python packages

	Open a *Cygwin terminal* from the desktop and install the following packages:

	~~~~
	python -m ensurepip --user
	python -m pip install --user future
	python -m pip install --user lxml
	python -m pip install --user uavcan`
	~~~~

6. Download ArduPilot. This is the project which enables the user to compile a multicopter or other kinds of UAVs. In the terminal input this lines:

	~~~~
	git clone git://github.com/ArduPilot/ardupilot.git
	cd ardupilot
	git submodule update --init --recursive
	~~~~

7. Make the multicopter. In the same *Cygwin terminal* and already within the *ardupilot* folder type:

	~~~~
	cd ArduCopter
	make sitl -j4
	sim_vehicle.py -w
	~~~~

	Once fully loaded, use "Ctrl+C" to close the running program.

	The second line builds the multicopter firmware *arducopter.elf*, and the third one uses MAVProxy to finally build the multicopter executable file *arducopter.exe* located in *C:\cygwin\home\user_name\ardupilot\build\sitl\bin*. Copy that file next to the ArduSim .jar file, and also the file *C:\cygwin\home\user_name\ardupilot\Tools\autotest\default_params\copter.param* to finish the basic setup process.

8. It is suggested (optional) to install [ImDisk Virtual Disk Driver](https://sourceforge.net/projects/imdisk-toolkit/) and run ArduCopter as Administrator in order to use a RAM Drive to store temporary files from the virtual multicopters, a setup that speeds up the execution when running ArduSim on a computer with a slow hard drive.


## SITL setup in Linux (tested in Ubuntu)

The next steps must be followed in order to compile a multicopter. Alternatively, you can follow the instructions included in the [official web](http://ardupilot.org/dev/docs/setting-up-sitl-on-linux.html), possibly more updated. All steps are done in the same terminal.

1. Install Java if not present.

	On debian based systems (Ubuntu, Mint, ...) run:
	~~~~
	sudo add-apt-repository ppa:webupd8team/java
	sudo apt-get update
	sudo apt-get install oracle-java8-installer
	~~~~
    
	On RPM based systems (CentOS, Fedora, ...) you must go to the [Oracle official download page](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html), accept the license agreement, and download the appropriate file. The, execute the following command, changing the version number accordingly to the download one:
	~~~~
	sudo yum localinstall jre-8u162-linux-x64.rpm
	~~~~

2. Install git if not present.

	On debian based systems (Ubuntu, Mint, ...) run:
	~~~~
	sudo apt-get install git-core
	~~~~

	On RPM based systems (CentOS, Fedora, ...) run:
	~~~~
	sudo yum install git
	~~~~

3. Download ArduPilot. This is the project which enables the user to compile a multicopter or other kinds of UAVs. In a terminal go to your home folder (*/home/user_name*) input this lines:

	~~~~
	git clone git://github.com/ArduPilot/ardupilot.git
	cd ardupilot
	git submodule update --init --recursive
	~~~~

4. Install required packages.

	On debian based systems (Ubuntu, Mint, ...) run:
	~~~~
	sudo apt-get install python-matplotlib python-serial python-wxgtk3.0 python-wxtools python-lxml
	sudo apt-get install python-scipy python-opencv ccache gawk git python-pip python-pexpect
	sudo pip install future pymavlink MAVProxy
	~~~~
    
	On RPM based systems (CentOS, Fedora, ...) run:
	~~~~
	sudo yum install opencv-python wxPython python-pip pyserial scipy python-lxml python-matplotlib python-pexpect python-matplotlib-wx
	~~~~

5. Set up path. This procedure makes it easy to execute simulated vehicles under SITL (sim_vehicle.py will be found from anywhere), but it is not strictly needed to just compile a multicopter, if the next steps are followed.

	Edit the **.bashrc** file located on the user home folder */home/user_name/.bashrc*, to add the following lines:

	~~~~
	export PATH=$PATH:$HOME/ardupilot/Tools/autotest
	export PATH=/usr/lib/ccache:$PATH
	~~~~
    
	Next, reload the PATH using the *dot* command:

	~~~~
	. ~/.bashrc
	~~~~

6. Make the multicopter.

	~~~~
	cd $HOME/ardupilot/ArduCopter
	sim_vehicle.py -w
	~~~~

	Once fully loaded, use "Ctrl+C" to close the running program.

	The second line uses MAVProxy to finally build the multicopter executable file *arducopter* located in *$HOME/ardupilot/build/sitl/bin*. Copy that file next to the ArduSim .jar file, and also the file *$HOME/ardupilot/Tools/autotest/default_params/copter.param* to finish the setup process.
