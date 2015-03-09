/*
 * Little window that opens up and allows user to enter URL, table name, and so on.
 * 
 * TODO: save this to a config file and load on start
 * 
 * TODO:  BUG!!  After switching to inventory2, the view doesn't get populated the same way.  Adding records doesn't immediately update, either.
 * Also, when I switch tables, I need to redo the primary key search and recordnum tracker because they aren't getting reset (or at least, recordnum isn't).
 * Right now, when I add a new record, it picks up the record number from the old table.  And finally, when this happens, I sometimes get non-unique key errors.
 * Also, it doesn't appear to be deleting (although it saves).
 * 
 */

package inventory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.GridBagLayout;

import javax.swing.JLabel;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JTextField;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;


public class SetupWindow extends JDialog {

	private JPanel contentPanel = new JPanel();
	private JTextField textFieldURL;
	private JTextField textFieldTable;
	private JTextField textFieldPort;
	private String lastURL, lastTable, lastPort, lastUser, lastPass, lastDBName;
	private JTextField textFieldUser;
	private JTextField textFieldPass;
	private JLabel lblFeedbackLabel; // for giving feedback messages
	private Timer labelTimer; // clears above label after period of time
	private MainWindow mainWindow; // stores reference to caller.  Need to change this to proper callback function code
	private JTextField textFieldDBName;
	private JLabel lblPass;
	private JLabel lblPort;
	private JLabel lblTable;
	private JLabel lblUser;
	private JLabel lblURL;
	private JLabel lblDBName;
	private JButton btnTestConnection;
	private JLabel lblFeedback;
	/**
	 * Create the dialog.
	 * @param mainWindow 
	 */
	public SetupWindow(MainWindow mainWindow) {
		// timer
		labelTimer = new Timer();
		this.mainWindow = mainWindow;
		initWindow();
		textFieldPort.setText(mainWindow.getPort());
		textFieldDBName.setText(mainWindow.getDBName());
		textFieldTable.setText(mainWindow.getTableName());
		textFieldUser.setText(mainWindow.getUser());
		textFieldPass.setText(mainWindow.getPass());
		textFieldURL.setText(mainWindow.getURL());
	}

	// sets the previous values to something new
	public void setPrevValues() { 
		lastURL = textFieldURL.getText();
		lastPort = textFieldPort.getText();
		lastTable = textFieldTable.getText();
		lastUser = textFieldUser.getText();
		lastPass = textFieldPass.getText();
		lastDBName = textFieldDBName.getText();
	}
	
	public void restorePrevValues() {
		textFieldURL.setText(lastURL);
		textFieldPort.setText(lastPort);
		textFieldTable.setText(lastTable);
		textFieldUser.setText(lastUser);
		textFieldPass.setText(lastPass);
		textFieldDBName.setText(lastDBName);
	}
	
	public void setLabel(String message) {
		lblFeedbackLabel.setText(message);
		
		TimerTask task = new TimerTask() {
			  @Override
			  public void run() {
			    lblFeedbackLabel.setText("");
			  }};
		labelTimer.schedule(task, 5000);
	}
	
	public void kill(){
		labelTimer.cancel();
		dispose();
	}

	private void initWindow() {
		// the ugly stuff below is thanks to WindowBuilder
		setBounds(100, 100, 395, 292);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{1.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		{
			lblURL = new JLabel("Location:");
			GridBagConstraints gbc_lblURL = new GridBagConstraints();
			gbc_lblURL.anchor = GridBagConstraints.WEST;
			gbc_lblURL.insets = new Insets(0, 0, 5, 5);
			gbc_lblURL.gridx = 0;
			gbc_lblURL.gridy = 0;
			contentPanel.add(lblURL, gbc_lblURL);
		}
		{
			lblPort = new JLabel("Port:");
			GridBagConstraints gbc_lblPort = new GridBagConstraints();
			gbc_lblPort.anchor = GridBagConstraints.WEST;
			gbc_lblPort.insets = new Insets(0, 0, 5, 5);
			gbc_lblPort.gridx = 1;
			gbc_lblPort.gridy = 0;
			contentPanel.add(lblPort, gbc_lblPort);
		}
		{
			textFieldURL = new JTextField();
			lblURL.setLabelFor(textFieldURL);
			GridBagConstraints gbc_textFieldURL = new GridBagConstraints();
			gbc_textFieldURL.ipadx = 50;
			gbc_textFieldURL.anchor = GridBagConstraints.WEST;
			gbc_textFieldURL.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldURL.gridx = 0;
			gbc_textFieldURL.gridy = 1;
			contentPanel.add(textFieldURL, gbc_textFieldURL);
			textFieldURL.setColumns(10);
		}
		lblPort.setLabelFor(textFieldPort);
		{
			textFieldPort = new JTextField();
			GridBagConstraints gbc_textFieldPort = new GridBagConstraints();
			gbc_textFieldPort.ipadx = 50;
			gbc_textFieldPort.anchor = GridBagConstraints.WEST;
			gbc_textFieldPort.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldPort.gridx = 1;
			gbc_textFieldPort.gridy = 1;
			contentPanel.add(textFieldPort, gbc_textFieldPort);
			textFieldPort.setColumns(10);
		}
		{
			lblDBName = new JLabel("Database:");
			GridBagConstraints gbc_lblDBName = new GridBagConstraints();
			gbc_lblDBName.anchor = GridBagConstraints.WEST;
			gbc_lblDBName.insets = new Insets(0, 0, 5, 5);
			gbc_lblDBName.gridx = 0;
			gbc_lblDBName.gridy = 2;
			contentPanel.add(lblDBName, gbc_lblDBName);
		}
		{
			lblTable = new JLabel("Table:");
			GridBagConstraints gbc_lblTable = new GridBagConstraints();
			gbc_lblTable.anchor = GridBagConstraints.WEST;
			gbc_lblTable.insets = new Insets(0, 0, 5, 5);
			gbc_lblTable.gridx = 1;
			gbc_lblTable.gridy = 2;
			contentPanel.add(lblTable, gbc_lblTable);
		}
		{
			textFieldDBName = new JTextField();
			lblDBName.setLabelFor(textFieldDBName);
			GridBagConstraints gbc_textFieldDBName = new GridBagConstraints();
			gbc_textFieldDBName.ipadx = 50;
			gbc_textFieldDBName.anchor = GridBagConstraints.WEST;
			gbc_textFieldDBName.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldDBName.gridx = 0;
			gbc_textFieldDBName.gridy = 3;
			contentPanel.add(textFieldDBName, gbc_textFieldDBName);
			textFieldDBName.setColumns(10);
		}
		lblTable.setLabelFor(textFieldTable);
		{
			textFieldTable = new JTextField();
			GridBagConstraints gbc_textFieldTable = new GridBagConstraints();
			gbc_textFieldTable.anchor = GridBagConstraints.WEST;
			gbc_textFieldTable.ipadx = 50;
			gbc_textFieldTable.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldTable.gridx = 1;
			gbc_textFieldTable.gridy = 3;
			contentPanel.add(textFieldTable, gbc_textFieldTable);
			textFieldTable.setColumns(10);
		}
		{
			lblUser = new JLabel("User Name:");
			GridBagConstraints gbc_lblUser = new GridBagConstraints();
			gbc_lblUser.anchor = GridBagConstraints.WEST;
			gbc_lblUser.insets = new Insets(0, 0, 5, 5);
			gbc_lblUser.gridx = 0;
			gbc_lblUser.gridy = 4;
			contentPanel.add(lblUser, gbc_lblUser);
		}
		{
			lblPass = new JLabel("Database Password:");
			GridBagConstraints gbc_lblPass = new GridBagConstraints();
			gbc_lblPass.anchor = GridBagConstraints.WEST;
			gbc_lblPass.insets = new Insets(0, 0, 5, 5);
			gbc_lblPass.gridx = 1;
			gbc_lblPass.gridy = 4;
			contentPanel.add(lblPass, gbc_lblPass);
		}
		{
			textFieldUser = new JTextField();
			lblUser.setLabelFor(textFieldUser);
			GridBagConstraints gbc_textFieldUser = new GridBagConstraints();
			gbc_textFieldUser.ipadx = 50;
			gbc_textFieldUser.anchor = GridBagConstraints.WEST;
			gbc_textFieldUser.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldUser.gridx = 0;
			gbc_textFieldUser.gridy = 5;
			contentPanel.add(textFieldUser, gbc_textFieldUser);
			textFieldUser.setColumns(10);
		}
		lblPass.setLabelFor(textFieldPass);
		{
			textFieldPass = new JTextField();
			GridBagConstraints gbc_textFieldPass = new GridBagConstraints();
			gbc_textFieldPass.insets = new Insets(0, 0, 5, 5);
			gbc_textFieldPass.anchor = GridBagConstraints.WEST;
			gbc_textFieldPass.ipadx = 50;
			gbc_textFieldPass.gridx = 1;
			gbc_textFieldPass.gridy = 5;
			contentPanel.add(textFieldPass, gbc_textFieldPass);
			textFieldPass.setColumns(10);
		}

		{
			btnTestConnection = new JButton("Test Connection");
			btnTestConnection.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// open the connection and print a status message
					if (MainWindow.DEBUG) System.out.println("This doesn't do anything yet.");
				}
			});
			GridBagConstraints gbc_btnTestConnection = new GridBagConstraints();
			gbc_btnTestConnection.insets = new Insets(0, 0, 5, 5);
			gbc_btnTestConnection.gridx = 1;
			gbc_btnTestConnection.gridy = 6;
			contentPanel.add(btnTestConnection, gbc_btnTestConnection);
		}
		{
			lblFeedback = new JLabel("FeedBack");
			lblFeedback.setForeground(Color.GRAY);
			lblFeedback.setBackground(Color.WHITE);
			GridBagConstraints gbc_lblFeedback = new GridBagConstraints();
			gbc_lblFeedback.insets = new Insets(0, 0, 5, 5);
			gbc_lblFeedback.gridx = 1;
			gbc_lblFeedback.gridy = 7;
			contentPanel.add(lblFeedback, gbc_lblFeedback);
		}
		
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// if changes were made, open the connection (error out if necessary)
						// then immediately close this window
						// TODO: deal with failure of opening the database (print a status message and stop user from being able to close window unless they cancel)
						// TODO: also need to check if the view is updating 
						// TODO: track if changes were made so we don't unnecessarily open something
						
						if (!lastURL.equals(textFieldURL.getText()) || 
								!lastUser.equals(textFieldUser.getText()) || 
								!lastPass.equals(textFieldPass.getText()) || 
								!lastTable.equals(textFieldTable.getText()) || 
								!lastPort.equals(textFieldPort.getText()) ||
								mainWindow.hasConnection() == false) {
							try {
								mainWindow.openDBConnection(textFieldURL.getText(), textFieldUser.getText(), textFieldPass.getText(), textFieldTable.getText(), textFieldPort.getText(), textFieldDBName.getText());
								if (MainWindow.DEBUG) System.out.println("We've opened a new connection.");
							} catch (SQLException e1) {
								e1.printStackTrace();
							}
								
							if (mainWindow.hasConnection() == false) {
								restorePrevValues();
								try {
									mainWindow.openDBConnection(lastURL, lastUser, lastPass, lastTable, lastPort, lastDBName);
								} catch (SQLException e2) {
									e2.printStackTrace();
								}
							} // to do:  deal with null return
						}
						else {
							if (MainWindow.DEBUG) System.out.println("Didn't change anything.");
						}
						mainWindow.closeSetupWindow();
						}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);

				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// discard changes and close the window
						mainWindow.closeSetupWindow(); // hide it first so user doesn't see weird stuff
						// restore previous values
						restorePrevValues();
						
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
}

