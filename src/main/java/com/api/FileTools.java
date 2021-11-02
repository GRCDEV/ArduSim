package com.api;

import com.setup.Main;
import com.setup.sim.gui.MainWindow;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API to read and write files.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FileTools {
	
	/**
	 * Get the folder where ArduSim is running.
	 * @return The folder where ArduSim is running (.jar folder, or project root if running in Eclipse IDE), or null if any error happens.
	 */
	public File getCurrentFolder() {
    	try {
			return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @return the folder ardusim/
	 */
	public Path getArdusimFolder(){
		File workingDirectory = getCurrentFolder();
		return Paths.get(workingDirectory.getParent());
	}
	/**
	 * @return the folder ardusim/src
	 */
	public Path getSourceFolder(){
		return Paths.get(getArdusimFolder() + File.separator + "src");
	}

	/**
	 * @return the folder ardusim/src/main/resources
	 */
	public Path getResourceFolder(){
		if(!ArduSimTools.isRunningFromJar()) {
			String fs = File.separator;
			return Paths.get(getSourceFolder() + fs + "main" + fs + "resources");
		}else{
			return Paths.get(getCurrentFolder().getAbsolutePath());
		}
	}


	/**
	 * Get a file extension.
	 * @param file The file to be checked.
	 * @return The file extension, or empty string if the file has not extension.
	 */
	public String getFileExtension(File file) {
		String fileName = file.getName();
		if(fileName.lastIndexOf(".") > 0) {
			return fileName.substring(fileName.lastIndexOf(".")+1);
		} else {
			return "";
		}
	}

	/**
	 * Parse an ini file to retrieve parameters for a protocol.
	 * @return Map with parameters and their respective value. Empty map if the file has no parameters or it is invalid.
	 */
	public Map<String, String> parseINIFile(File iniFile) {
		Map<String, String> parameters = new HashMap<>();
		List<String> lines = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(iniFile))) {
			String line;
			while ((line = br.readLine()) != null) {
		        lines.add(line);
		    }
		} catch (IOException e) {
			return parameters;
		}
		// Check file length
		if (lines.size() < 1) {
			return parameters;
		}
		List<String> checkedLines = new ArrayList<>();
		for (String s : lines) {
			String line = s.trim();
			if (line.length() > 0 && !line.startsWith("#") && (line.length() - line.replace("=", "").length() == 1)) {
				checkedLines.add(line);
			}
		}
		if (checkedLines.size() > 0) {
			String key, value;
			String[] pair;
			for (String checkedLine : checkedLines) {
				pair = checkedLine.split("=");
				key = pair[0].trim().toUpperCase();
				value = pair[1].trim();
				parameters.put(key, value);
			}
		}
		return parameters;
	}
	
	/**
	 * Store text in a file.
	 * @param destination File to store the text.
	 * @param text Text to be stored in the file.
	 */
	public void storeFile(File destination, String text) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(destination);
			fw.write(text);
			fw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Load image from file.
	 * @param contentRootPath path to the file starting after /src/, inside the <i>src</i> folder (it must start with the corresponding File.separator).
	 * @return	The BufferedImage loaded from the file, or null if any error happens.
	 */
	public BufferedImage loadImage(String contentRootPath) {
		//TODO check from jar
		URL url = null;
		if (ArduSimTools.isRunningFromJar()) {
			url = MainWindow.class.getResource(contentRootPath);
		} else {
			//url = MainWindow.class.getResource(absolutePath);
			try {
				url = new File(this.getSourceFolder().toString(),contentRootPath).toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		try {
			return ImageIO.read(url);
		} catch (Exception e) {
			return null;
		}
	}
	
}
