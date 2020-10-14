package main.api.masterslavepattern.safeTakeOff;

public enum TakeOffAlgorithm {
	
	OPTIMAL(0, "Optimal"),
	SIMPLIFIED(1, "Simplified"),
	RANDOM(2, "Random"),
	HUNGARIAN(3, "Hungarian");

	private final int algorithmID;
	private final String algorithmName;
	
	TakeOffAlgorithm(int id, String name) {
		this.algorithmID = id;
		this.algorithmName = name;
	}
	
	public int getID() {
		return this.algorithmID;
	}
	
	public String getName() {
		return this.algorithmName;
	}
	
	public static TakeOffAlgorithm getAlgorithm(String name) {
		TakeOffAlgorithm[] algorithms = TakeOffAlgorithm.values();
		for (TakeOffAlgorithm algorithm : algorithms) {
			if (algorithm.getName().equals(name)) {
				return algorithm;
			}
		}
		
		return null;
	}
	
	public static String[] getAvailableAlgorithms() {
		TakeOffAlgorithm[] algorithms = TakeOffAlgorithm.values();
		String[] res = new String[algorithms.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = algorithms[i].getName();
		}
		
		return res;
	}
	
}