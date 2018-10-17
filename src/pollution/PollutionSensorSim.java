package pollution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import api.Copter;
import api.Tools;
import api.pojo.UTMCoordinates;
import smile.interpolation.KrigingInterpolation;
import smile.interpolation.variogram.GaussianVariogram;

public class PollutionSensorSim implements PollutionSensor {
	KrigingInterpolation krigData;
	double point[][];
	double data[];
	double error[];
	double dataXSize;
	double dataYSize;
	
	void readData(String file) throws Exception {
		String line;
		String tokens[];
		int size;
		dataXSize = 0;
		dataYSize = 0;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file))));
		try {
			line = reader.readLine();
			while(line.charAt(0) == '%') line = reader.readLine();
			tokens = line.split(" ");
			if (tokens.length != 3) throw new Exception("Format error. File should be in Matrix Market Coordinate Format.");
			
			size = Integer.parseInt(tokens[2]);
			point = new double[size][2];
			data = new double[size];
			error = new double[size];
			for(int i = 0; i < size; i++) {
				line = reader.readLine();
				while(line.charAt(0) == '%') line = reader.readLine();
				tokens = line.split(" ");
				if (tokens.length != 3) throw new Exception("Format error. File should be in Matrix Market Coordinate Format.");
				point[i][0] = Double.parseDouble(tokens[0]);
				point[i][1] = Double.parseDouble(tokens[1]);
				data[i] = Double.parseDouble(tokens[2]);
				error[i] = data[i]*0.01;
				dataXSize = Math.max(dataXSize, point[i][0]);
				dataYSize = Math.max(dataYSize, point[i][1]);
				
			}
		} finally {
			reader.close();
		}
		
	}

	public PollutionSensorSim() throws Exception {
		readData(PollutionParam.pollutionDataFile);
		
		// DEBUG Print data
//		for (int i = 0; i < point.length; i++) {
//			SimTools.println("[" + point[i][0] + ", " + point[i][1] + "] = " + data[i]);
//		}
		// Avoid showing error as the library is dynamically loaded and was not found in LD_LIBRARY_PATH
		PrintStream original = System.err;
		PrintStream dummy = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
			}
		});
		System.setErr(dummy);
		krigData = new KrigingInterpolation(point, data, new GaussianVariogram(981.8083, 658.7948, 500.0), error);
		System.setErr(original);
		
		//SimTools.println("Krige(30, 50) : " + krigData.interpolate(30.0, 50.0));
	}

	@Override
	public double read() {
		UTMCoordinates location = Copter.getUTMLocation(0);
		UTMCoordinates startLocation = Tools.geoToUTM(PollutionParam.startLocation.latitude, PollutionParam.startLocation.longitude);
		double pointX = (location.x - startLocation.x) / PollutionParam.length * dataYSize;
		double pointY = (location.y - startLocation.y) / PollutionParam.length * dataYSize;
		return krigData.interpolate(pointX, pointY);
	}

}
