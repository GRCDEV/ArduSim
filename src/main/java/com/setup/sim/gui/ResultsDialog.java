package com.setup.sim.gui;

import com.api.API;
import com.api.ArduSimTools;
import com.setup.Text;
import com.setup.sim.logic.SimTools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/** This class generates a dialog when the experiment finalizes, with the experiment configuration and the results.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class ResultsDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();

	@SuppressWarnings("unused")
	private ResultsDialog() {
		this("", null, false);
	}

	public ResultsDialog(final String s, final Frame frame, boolean isModal) {
		super(frame, isModal);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JTextArea textArea = new JTextArea();
			textArea.setOpaque(false);
			textArea.setEditable(false);
			textArea.setText(s);
			JScrollPane pane = new JScrollPane(textArea);
			contentPanel.add(pane);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton saveButton = new JButton(Text.RESULTS_SAVE_DATA);
				saveButton.addActionListener(e -> {
					JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(API.getFileTools().getCurrentFolder());
					chooser.setDialogTitle(Text.RESULTS_DIALOG_TITLE);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					FileNameExtensionFilter filter1 = new FileNameExtensionFilter(Text.RESULTS_DIALOG_SELECTION, Text.FILE_EXTENSION_TXT);
					chooser.addChoosableFileFilter(filter1);
					chooser.setAcceptAllFileFilterUsed(false);
					int retrieval = chooser.showSaveDialog(null);
					if (retrieval == JFileChooser.APPROVE_OPTION) {
						final File file = chooser.getSelectedFile();
						if (file.exists()) {
							Object[] options = {Text.YES_OPTION, Text.NO_OPTION};
							int result = JOptionPane.showOptionDialog(frame,
									Text.STORE_QUESTION,
									Text.STORE_WARNING,
									JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE,
									null,
									options,
									options[1]);
							if (result == JOptionPane.YES_OPTION) {
								(new Thread(() -> ArduSimTools.storeResults(s, file))).start();
								dispose();
							}
						} else {
							(new Thread(() -> ArduSimTools.storeResults(s, file))).start();
							dispose();
						}
					}
				});
				buttonPane.add(saveButton);
				getRootPane().setDefaultButton(saveButton);
			}
			{
				JButton closeButton = new JButton(Text.RESULTS_IGNORE_DATA);
				closeButton.addActionListener(e -> dispose());
				buttonPane.add(closeButton);
			}
		}
		
		SimTools.addEscListener(this, false);
		
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setTitle(Text.RESULTS_TITLE);
		this.pack();
		this.setSize(this.getWidth() + (Integer) UIManager.get(Text.SCROLLBAR_WIDTH), 300);
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setVisible(true);
	}

}
