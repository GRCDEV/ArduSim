package lander.logic;

import api.pojo.GeoCoordinates;

public class LanderSensorRecognition implements LanderSensorInterface {

	private static api.pojo.GeoCoordinates LocationRecognition;
	private static double mLatitude;
	private static double mLongitude;
	
		
	void readData(String file) {
		file ="";
		
		String Latitude = "39.476444";
		String Longitude= "-0.333413";
		
		LocationRecognition = new GeoCoordinates(Double.parseDouble(Latitude),Double.parseDouble(Longitude));
		mLatitude= Double.parseDouble(Latitude);
		mLongitude= Double.parseDouble(Longitude);
		
				
	}

	public LanderSensorRecognition() throws Exception {
		//readData(PollutionParam.pollutionDataFile);
		
		readData("test");
	}

	@Override
	public GeoCoordinates readGeoordinates() {
		// TODO Auto-generated method stub
		return LocationRecognition;
	}

	@Override
	public double Latitude() {
		// TODO Auto-generated method stub
		return mLatitude;
	}

	@Override
	public double Longitude() {
		// TODO Auto-generated method stub
		return mLongitude;
	}
}
