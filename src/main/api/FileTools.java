package main.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.Main;

/**
 * API to read and write files.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FileTools {
	
	/**
	 * Get the folder where ArduSim is running.
	 * @return The folder where ArduSim is running (.jar folder, or project root if running in Eclipse IDE).
	 */
	public File getCurrentFolder() {
		Class<Main> c = main.Main.class;
		CodeSource codeSource = c.getProtectionDomain().getCodeSource();
	
		File jarFile = null;
	
		if (codeSource != null && codeSource.getLocation() != null) {
			try {
				jarFile = new File(codeSource.getLocation().toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		else {
			String path = c.getResource(c.getSimpleName() + ".class").getPath();
			String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
			try {
				jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			jarFile = new File(jarFilePath);
		}
		return jarFile.getParentFile();
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
			String line = null;
			while ((line = br.readLine()) != null) {
		        lines.add(line);
		    }
		} catch (IOException e) {
			return parameters;
		}
		// Check file length
		if (lines==null || lines.size()<1) {
			return parameters;
		}
		List<String> checkedLines = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.length() > 0 && !line.startsWith("#") && (line.length() - line.replace("=", "").length() == 1)) {
				checkedLines.add(line);
			}
		}
		if (checkedLines.size() > 0) {
			String key, value;
			String[] pair;
			for (int i = 0; i < checkedLines.size(); i++) {
				pair = checkedLines.get(i).split("=");
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
	
}
