package followme.logic;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JComboBox;
import javax.swing.border.TitledBorder;

import api.Tools;
import followme.pojo.Nodo;
import followme.pojo.RecursoCompartido;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.DefaultComboBoxModel;

public class FollowMeDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final JPanel contentPanel = new JPanel();

	/**
	 * Launch the application.
	 */
	String[] mFollow = new String[] {"Lineal", "Matriz",
            "Circle"};
	private JTextField TxtRadioDistance;
	
	private JComboBox mSelectComboBox = new JComboBox(mFollow);
	private JTextField TxtUpdateTime;
	private JTextField TxtOtherOption;
	public int mTypeFollowMe=1;
	
	private JLabel LblOptionSelect;
	private JTextField txtDataFilePath;
	
	/*
	public static void main(String[] args) {
		try {
			FollowMeDialog dialog = new FollowMeDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * Create the dialog.
	 */
	public FollowMeDialog() {
		setTitle(FollowMeText.PROTOCOL_TEXT + " Configurations");
		
		
		setBounds(100, 100, 450, 352);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);;
		
	
		
		JPanel Cpanel1 = new JPanel();
		Cpanel1.setBorder(new TitledBorder(null, "Swarm configuration", TitledBorder.LEFT, TitledBorder.TOP, null, new Color(51, 51, 51)));
		Cpanel1.setBounds(12, 12, 426, 80);
		contentPanel.add(Cpanel1);
		Cpanel1.setLayout(null);
		
		JLabel lblOptions = new JLabel("Select options");
		lblOptions.setBounds(12, 22, 108, 15);
		Cpanel1.add(lblOptions);
		mSelectComboBox.setBounds(166, 17, 248, 24);
		mSelectComboBox.setModel(new DefaultComboBoxModel(new String[] {"Lineal", "Matrix", "Circle"}));
		
		
		mSelectComboBox.setSelectedIndex(1);
		Cpanel1.add(mSelectComboBox);
		
		LblOptionSelect = new JLabel("Iter-UAV distance (m): ");
		LblOptionSelect.setBounds(12, 49, 359, 15);
		Cpanel1.add(LblOptionSelect);
		
		TxtRadioDistance = new JTextField("75", 12);
		TxtRadioDistance.setHorizontalAlignment(SwingConstants.RIGHT);
		TxtRadioDistance.setBounds(282, 53, 132, 19);
		Cpanel1.add(TxtRadioDistance);
		
		JPanel Cpanel2 = new JPanel();
		Cpanel2.setLayout(null);
		Cpanel2.setBorder(new TitledBorder(null, "Other configurations", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		Cpanel2.setBounds(12, 170, 426, 80);
		contentPanel.add(Cpanel2);
		
		JLabel Lbl = new JLabel("Network refresh period (ms)");
		Lbl.setVerticalAlignment(SwingConstants.TOP);
		Lbl.setBounds(12, 24, 246, 15);
		Cpanel2.add(Lbl);
		
		TxtUpdateTime = new JTextField("1000", 10);
		TxtUpdateTime.setBounds(331, 22, 83, 19);
		Cpanel2.add(TxtUpdateTime);
		
		TxtOtherOption = new JTextField("10", 2);
		TxtOtherOption.setBounds(331, 49, 83, 19);
		TxtOtherOption.setVisible(false); 
		
		
		Cpanel2.add(TxtOtherOption);
		
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		JButton BtnDataSource = new JButton("...");
		
		JLabel lblOtherOption = new JLabel("Other option");
		lblOtherOption.setVerticalAlignment(SwingConstants.TOP);
		lblOtherOption.setBounds(12, 51, 246, 15);
		lblOtherOption.setVisible(false);
		Cpanel2.add(lblOtherOption);
		
		JPanel CpSource = new JPanel();
		CpSource.setLayout(null);
		CpSource.setBorder(new TitledBorder(null, "Path definition file", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		CpSource.setBounds(12, 94, 426, 64);
		contentPanel.add(CpSource);
		
		txtDataFilePath = new JTextField();
		txtDataFilePath.setEnabled(false);
		txtDataFilePath.setEditable(false);
		txtDataFilePath.setBounds(100, 28, 314, 19);
		CpSource.add(txtDataFilePath);
		
		
		BtnDataSource.setHorizontalAlignment(SwingConstants.RIGHT);
		BtnDataSource.setBounds(12, 25, 49, 25);
		CpSource.add(BtnDataSource);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			
			okButton.setActionCommand("OK");
			buttonPane.add(okButton);
			getRootPane().setDefaultButton(okButton);
			
			cancelButton.setActionCommand("Cancel");
			buttonPane.add(cancelButton);
			
		}
		
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
			
		});
		
		cancelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				CancelAction();
			}
			
		});
		
		BtnDataSource.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				WayPointsSource();
			}
			
		});
	
		mSelectComboBox.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		    	JComboBox cb = (JComboBox)e.getSource();
		    	int selectedIndex = cb.getSelectedIndex();
		    	
		    	switch (selectedIndex) {
				case 0:
						System.out.println("Lineal");
						LblOptionSelect.setText("Get into distance (m): ");
						mTypeFollowMe = 0;
					break;
				case 1:
						System.out.println("Matriz");
						LblOptionSelect.setText("Get into distance (m): ");
						
						
						mTypeFollowMe = 1;	
					break;
				case 2:
						System.out.println("Circle");
						LblOptionSelect.setText("Get into radio (m): ");
				
						mTypeFollowMe = 2;
					break;
					
				
				}
		    }
		});
		
	}
	
	
	private void okAction() {
		
		try {
			
			//LanderParam.distMax = Integer.parseInt(txtDistMax.getText());
			//LanderParam.altitude = Double.parseDouble(altitudeField.getText());
			
			FollowMeParam.FormacionUsada = mTypeFollowMe;
			
			FollowMeParam.DistanceLinearOffset = Double.parseDouble(TxtRadioDistance.getText());
			
			FollowMeParam.DistanceRadio = Double.parseDouble(TxtRadioDistance.getText());
			
			//Integer.parseInt(TxtRadioDistance.getText());
			
			FollowMeParam.FollowMeBeaconingPeriod = Integer.parseInt(TxtUpdateTime.getText()); 
			
			//LanderParam.isSimulation = isSimulationCheckBox.isSelected();
			
			//Tools.setProtocolConfigured();
			
			Tools.setProtocolConfigured();
			
			//GUI.log("Coordenada final :"+ LanderParam.LocationEnd.toString());
			
			//GUI.log("Distance radio final :"+ FollowMeParam.DistanceRadio);
			
			
			dispose();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Numeric format.", "Format error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void CancelAction() {
		try {
						
			
			dispose();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Numeric Format.", "Format error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void WayPointsSource() {
	
	String SEPARATOR = ",";
	RecursoCompartido recurso = new RecursoCompartido();

	BufferedReader br = null;
	FileReader fr = null;

	System.out.println("Valor del tipo de dato: " + mTypeFollowMe );
	
	try {

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

		int result = fileChooser.showOpenDialog(new JDialog());

		File f = null;

		if (result == JFileChooser.APPROVE_OPTION) {
			// user selects a file
			f = fileChooser.getSelectedFile();		
			txtDataFilePath.setText(fileChooser.getSelectedFile().getPath());

		}
		// System.out.println(f.getPath());
		fr = new FileReader(f);

		br = new BufferedReader(fr);
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
			} else if (tipo == 1) {
				int ch1 = Integer.parseInt(fields[2]);
				int ch2 = Integer.parseInt(fields[3]);
				int ch3 = Integer.parseInt(fields[4]);
				int ch4 = Integer.parseInt(fields[5]);
				n = new Nodo(tipo, tiempo, ch1, ch2, ch3, ch4);
			}

			recurso.put(n);
			long t = Long.parseLong(fields[1]);
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

	FollowMeParam.recurso.set(headingInterpolar(recurso));
}

private static RecursoCompartido headingInterpolar(RecursoCompartido recurso) {
	RecursoCompartido rc = null;
	rc = recurso;
	rc.bubbleSort();

	ArrayList<Nodo> listaAux = new ArrayList<Nodo>();
	while (!rc.vacio) {
		Nodo aux = rc.pop();
		listaAux.add(aux);
	}
	// Copiar Heading de los primeros type 1 desde el primer type 0
	double headingInicial = 0;

	int i;
	for (i = 0; i < listaAux.size(); i++) {
		if (listaAux.get(i).type == 0) {
			headingInicial = listaAux.get(i).heading;
			break;
		}
	}
	for (int j = 0; j < i; j++) {
		listaAux.get(j).heading = headingInicial;
	}

	// Calcular heading para todos los type 1 que se encuentrean entre 2 type 0

	int x = 0, y = 0;
	while (listaAux.get(x).type != 0 && x < listaAux.size())
		x++;
	y = x + 1;
	while (y < listaAux.size()) {
		double headingX = 0.0, headingY = 0.0;
		long timeX = 0, timeY = 0;

		if (listaAux.get(x).type == 0) {
			timeX = listaAux.get(x).time;
			headingX = listaAux.get(x).heading;
		}

		while (y < listaAux.size() && listaAux.get(y).type != 0)
			y++;
		if (y >= listaAux.size())
			break;
		if (listaAux.get(y).type == 0) {
			timeY = listaAux.get(y).time;
			headingY = listaAux.get(y).heading;
		}

		double headingDif = headingY - headingX;
		long timeDif = timeY - timeX;

		while (x < y) {
			x++;
			listaAux.get(x).heading = headingX + ((listaAux.get(x).time - timeX) * (headingDif) / (timeDif));
		}
		x = y;
		y = x + 1;
	}
	for (Nodo nodo : listaAux) {
		rc.put(nodo);
	}

	return rc;
}
}
