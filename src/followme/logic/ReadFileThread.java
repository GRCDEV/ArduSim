package followme.logic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class ReadFileThread extends Thread{

	public static final String SEPARATOR = ",";
	private RecursoCompartido recurso;
	public static String pathFile = "files/2018-2-15-14-30-47logs_msg.txt";
	
	public ReadFileThread() {
		
	}
	
	public ReadFileThread(String f) {
		this.pathFile = f;
	}
	
	public void run() {

		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(pathFile));
			String line = br.readLine();
			String[] fields = null;
			long time = 0;

			while (null != line) {
				fields = line.split(SEPARATOR);
				Nodo n = null;
				int tipo = Integer.parseInt(fields[0]);
				long tiempo = Long.parseLong(fields[1]);
				
				if (tipo == 0) {
					double east = Double.parseDouble(fields[2]);
					double north = Double.parseDouble(fields[3]);
					double z = Double.parseDouble(fields[4]);
					double zRel = Double.parseDouble(fields[5]);
					double speed = Double.parseDouble(fields[6]);
					double heading = Double.parseDouble(fields[7]);
					n = new Nodo(tipo, tiempo, east, north, z, zRel, speed, heading);
				}
				else if (tipo == 1) {
					int ch1 = Integer.parseInt(fields[2]);
					int ch2 = Integer.parseInt(fields[3]);
					int ch3 = Integer.parseInt(fields[4]);
					int ch4 = Integer.parseInt(fields[5]);
					n = new Nodo(tipo, tiempo, ch1, ch2, ch3, ch4);
				}

			
				recurso.put(n);
				long t = Long.parseLong(fields[1]);
				long diferencia = t - time;
				time = t;


				line = br.readLine();
			}
		} catch (Exception e) {
			// ...
		} finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
}
