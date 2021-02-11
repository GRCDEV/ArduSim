package com.api.cpuHelper;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Param;
import com.setup.Text;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 
 * Thread used to measure the CPU usage.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class CPUUsageThread extends Thread {
	
	private static final int FINISHED = 1;
	private static final int CPU_LINE_RECEIVED = 2;
	
	private boolean firstSkipped = false;

	@Override
	public void run() {
		
		if (Param.runningOperatingSystem == Param.OS_WINDOWS) {
			try {
				// 1. Load needed library
				String architecture = System.getProperty("os.arch");
				if (architecture.contains("86")) {
					ArduSimTools.loadDll("sigar-x86-winnt.dll");
				} else if (architecture.contains("64")) {
					ArduSimTools.loadDll("sigar-amd64-winnt.dll");
				} else {
					throw new IOException();
				}
				// 2. Avoid showing error as the library is dynamically loaded and was not found in LD_LIBRARY_PATH
				PrintStream original = System.err;
				PrintStream dummy = new PrintStream(new OutputStream() {
					@Override
					public void write(int b) {
					}
				});
				System.setErr(dummy);
				Sigar sigar = new Sigar();
				System.setErr(original);
				// 3. Get CPU information
				try {
					// Number of available cores
					Param.numCPUs = sigar.getCpuList().length;
					// ArduSim process Id
					int pid = (int)sigar.getPid();
					// Cyclic CPU usage retrieval
					long prevTime = System.currentTimeMillis();
					long delta;
					long interval = 1000 * Param.CPU_CHECK_PERIOD;
					while (Param.simStatus != Param.SimulatorState.TEST_FINISHED) {
						try {
							Param.cpu.add(new CPUData(System.nanoTime(),
									sigar.getCpuPerc().getCombined()*100,
									sigar.getProcCpu(pid).getPercent() * 100,
									Param.simStatus));
						} catch (SigarException ignored) {}
						delta = prevTime + interval - System.currentTimeMillis();
						if (delta > 0) {
							API.getArduSim().sleep(delta);
						}
					}
				} catch (SigarException e) {
					ArduSimTools.logGlobal(Text.CPU_ERROR_2);
				}
			} catch (IOException e2) {
				ArduSimTools.logGlobal(Text.CPU_ERROR_1);
			}
		}
		
		if (Param.runningOperatingSystem == Param.OS_LINUX
				|| Param.runningOperatingSystem == Param.OS_MAC) {
			try {
				// 1. Number of available cores
				String[] comd;
				if (Param.runningOperatingSystem == Param.OS_LINUX) {
					comd = new String[]{"/bin/sh", "-c", "nproc"};
				} else {
					comd = new String[]{"/bin/sh", "-c", "sysctl hw.ncpu"};
				}
				
				
				byte[] bo = new byte[100];
				Process p = Runtime.getRuntime().exec(comd);
				p.getInputStream().read(bo);
				if (Param.runningOperatingSystem == Param.OS_LINUX) {
					Param.numCPUs = Integer.parseInt(new String(bo).trim());
				} else {
					Param.numCPUs = Integer.parseInt(new String(bo).trim().split(" ")[1]);
				}
				p.destroy();
				
				// 2. ArduSim process Id
				try {
					bo = new byte[100];
					comd = new String[]{"/bin/sh", "-c", "echo $PPID"};
					p = Runtime.getRuntime().exec(comd);
					p.getInputStream().read(bo);
					int pid = Integer.parseInt(new String(bo).trim());
					p.destroy();
					
					// 3. Cyclic CPU usage retrieval
					List<String> commandLine = new ArrayList<>();
					commandLine.add("/bin/sh");
					ProcessBuilder pb = new ProcessBuilder(commandLine);
					try {
						p = pb.start();
						BufferedWriter output = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
						BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
						if (Param.runningOperatingSystem == Param.OS_LINUX) {
							output.write("top -b -p " + pid + " -d " + Param.CPU_CHECK_PERIOD + "\n");
						} else {
							output.write("top -pid " + pid + " -s " + Param.CPU_CHECK_PERIOD + " -stats pid,cpu\n");
						}
						output.flush();
						int state = CPUUsageThread.FINISHED;
						
						Double global = null;
						double process;
						String s, globalString;
						try {
							while (Param.simStatus != Param.SimulatorState.TEST_FINISHED) {
								if (input.ready()) {
									s = input.readLine().trim();
									if (state == CPUUsageThread.FINISHED) {
										if (Param.runningOperatingSystem == Param.OS_LINUX && s.startsWith("%Cpu(s)")) {
											if (firstSkipped) {
												try {
													global = 100 - NumberFormat.getInstance(Locale.getDefault()).parse(s.split("\\s+")[7]).doubleValue();
													state = CPUUsageThread.CPU_LINE_RECEIVED;
												} catch (ParseException ignored) {}
											} else {
												firstSkipped = true;
											}
										}
										if (Param.runningOperatingSystem == Param.OS_MAC && s.startsWith("CPU")) {
											if (firstSkipped) {
												globalString = s.split("\\s+")[6];
												globalString = globalString.substring(0, globalString.length() - 1);
												try {
													global =100 - NumberFormat.getInstance(Locale.getDefault()).parse(globalString).doubleValue();
													state = CPUUsageThread.CPU_LINE_RECEIVED;
												} catch (ParseException ignored) {}
											} else {
												firstSkipped = true;
											}
										}
									}
									
									// global != null only if no ParseException error happened
									if (state == CPUUsageThread.CPU_LINE_RECEIVED) {
										if (Param.runningOperatingSystem == Param.OS_LINUX && s.startsWith("" + pid)) {
											try {
												process = NumberFormat.getInstance(Locale.getDefault()).parse(s.split("\\s+")[8]).doubleValue();
												Param.cpu.add(new CPUData(System.nanoTime(), global, process, Param.simStatus));
												global = null;
												state = CPUUsageThread.FINISHED;
											} catch (ParseException ignored) {}
										}
										if (Param.runningOperatingSystem == Param.OS_MAC && s.startsWith("" + pid)) {
											try {
												process = NumberFormat.getInstance(Locale.getDefault()).parse(s.split("\\s+")[1]).doubleValue();
												Param.cpu.add(new CPUData(System.nanoTime(), global, process, Param.simStatus));
												global = null;
												state = CPUUsageThread.FINISHED;
											} catch (ParseException ignored) {}
										}
									}
								} else {
									try {
										Thread.sleep(Param.CPU_CONSOLE_TIMEOUT);
									} catch (InterruptedException ignored) {}
								}
							}
						} catch (IOException ignored) {}
					} catch (IOException e) {
						ArduSimTools.logGlobal(Text.CPU_ERROR_4);
					}
				}catch (IOException e) {
					ArduSimTools.logGlobal(Text.CPU_ERROR_3);
				}
			} catch (IOException e1) {
				ArduSimTools.logGlobal(Text.CPU_ERROR_2);
			}
		}
	}

}
