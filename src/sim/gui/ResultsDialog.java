package sim.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import api.GUIHelper;
import main.Text;
import main.Tools;

/** This class generates a dialog when the experiment finalizes, with the experiment configuration and the results. */

public class ResultsDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();

	@SuppressWarnings("unused")
	private ResultsDialog() {
		this("", null, false);
	}

	public ResultsDialog(String s, Frame frame, boolean isModal) {
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
				saveButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = new JFileChooser();
						chooser.setCurrentDirectory(GUIHelper.getCurrentFolder());
						chooser.setDialogTitle(Text.RESULTS_DIALOG_TITLE);
						chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
						FileNameExtensionFilter filter1 = new FileNameExtensionFilter(Text.RESULTS_DIALOG_SELECTION, Text.FILE_EXTENSION_TXT);
						chooser.addChoosableFileFilter(filter1);
						int retrival = chooser.showSaveDialog(null);
						if (retrival == JFileChooser.APPROVE_OPTION) {
							Tools.storeResults(s, chooser.getSelectedFile());
							dispose();
						}
					}
				});
				buttonPane.add(saveButton);
				getRootPane().setDefaultButton(saveButton);
			}
			{
				JButton closeButton = new JButton(Text.RESULTS_IGNORE_DATA);
				closeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				buttonPane.add(closeButton);
			}
		}
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setTitle(Text.RESULTS_TITLE);
		this.pack();
		this.setSize(this.getWidth() + ((Integer)UIManager.get(Text.SCROLLBAR_WIDTH)).intValue(), 300);
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setVisible(true);
	}

}
