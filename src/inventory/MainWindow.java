/*
 * Vicky's inventory and project manager.  (c) 2014
 * 
 * Issues (all with the notes text field):
 * 1.  When in text field, trying to copy the selected text first unselects it.
 * 2.  The text field for notes is too small.
 * 3.  Also, the display stretches in weird ways if it is too long.
 * 4.  Single quotes entered into a text field seem to kill the SQL statement or at least cause it to be rejected.  Can they be escaped?
 * 5.  Seems like the insert key is always on when editing a text field.
 * 6.  Why does the first letter always get highlighted and written over?
 * 
 * TODO:  Decide if you want to store files or just links in the Datasheet field.  
 * If file:  How to open?  What to show in the field in the program?  Button?  (click to add, open, or delete)
 * If link:  Probably can just take link and pass it to system PDF viewer.  How to check for system PDF viewer?  Also, in this case, when user enters a link,
 * want to make sure file exists and if so, copy it over to our own folder.  And if link, what to display to the user in the field?  Would prefer something 
 * meaningful even if the underlying data is weird looking.
 * 
 */

package inventory;
import java.awt.EventQueue;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JScrollPane;

import java.awt.Dimension;

import net.miginfocom.swing.MigLayout;

import javax.swing.ListSelectionModel;
import javax.swing.JLabel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.awt.Font;

import org.jdesktop.swingx.JXTable;

public class MainWindow {
	public static final boolean DEBUG = false; // set to true for debug messages to get printed to the terminal, false for silence
	
	private JFrame frmInventoryManager;
	private JXTable inventoryTable; // use JXTable specifically for built-in search function and column sorter (seriously, it's awesome)
	private DBConnector connection; // DB connection
	private InventoryTableModel tableModel;
	private String tableName;
	private JLabel lblFeedbackLabel; // for giving feedback messages
	private JLabel lblNotes;
	private Timer labelTimer; // clears above label after period of time
	private SetupWindow setupWindow;
	private String user;
	private String URL;
	private String pass;
	private String port;
	private String DBName;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmInventoryManager.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws SQLException 
	 */
	public MainWindow() throws SQLException {
		// temporarily hardcode the defaults
		// TODO: store this stuff in a config file and let user set it
		
		/* YOU MUST FILL THESE VALUES IN OR IT WILL NOT RUN */
		tableName = "fill in"; // table name of the table within your database
		user = "fill in"; // user name
		pass = "fill in"; // password
		URL = "fill in"; // the URL of your database, either internal or external network
		port = "fill in"; // default is 5432
		DBName = "fill in"; // text name of your database
		
		initialize();	
		
		// we start with the default table  
		initializeTableModel();
	}
	
	private void initializeTableModel() throws SQLException {
		tableModel = openDBConnection(URL, user, pass, tableName, port, DBName); // to do:  deal with null return
		
		// set up custom cell editors so that we can monitor data entry and catch mistakes:
		// SQL types: string is 12, int is 4, float is 7
		// add in more if your table has more
		for (int i = 0; i < tableModel.getColumnCount(); i++){
			int type = tableModel.getColumnType(i);
			if (type == 12) { // String
				inventoryTable.getColumnModel().getColumn(i).setCellEditor(new TextEditor());
			} else if (type == 4) {
				inventoryTable.getColumnModel().getColumn(i).setCellEditor(new IntegerEditor());
			} else if (type == 7) {
				inventoryTable.getColumnModel().getColumn(i).setCellEditor(new FloatEditor());
			}
		}
		// hide the record number column:
		inventoryTable.getColumnExt("Recordnum").setVisible(false);
	}

	public void closeSetupWindow() {
		setupWindow.setVisible(false);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmInventoryManager = new JFrame();
		frmInventoryManager.setTitle("Inventory Manager");
		frmInventoryManager.setBounds(100, 100, 1450, 530);
		frmInventoryManager.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					saveTable();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}	
			}
		});
		
		JButton btnCloseButton = new JButton("Close");
		btnCloseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// save first then clean up and exit
				// TODO:  Should give the option to save or ignore changes rather than just blindly saving
				try {
					saveTable();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
				labelTimer.cancel();
				setupWindow.kill();
				try {
					connection.kill();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				frmInventoryManager.dispose();
				if (DEBUG) System.out.println("Successfully finished.");
			}
		});
		frmInventoryManager.getContentPane().setLayout(new MigLayout("", "[141px][][][][][][5px][307px]", "[427px][23px]"));
		
		JScrollPane scrollPaneForInventoryTable = new JScrollPane();
		scrollPaneForInventoryTable.setPreferredSize(new Dimension(4000, 400));
		
		// label for notes:
		lblNotes = new JLabel("Notes: ");
		lblNotes.setFont(new Font("Tahoma", Font.PLAIN, 11));
		frmInventoryManager.getContentPane().add(lblNotes, "cell 1 2 4 2");
		
		inventoryTable = new JXTable();
		inventoryTable.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				setLabel("Table has been modified.");
				
			}
			public void keyReleased(KeyEvent e) {
				updateNotesLabel();
			}
		});
		

		inventoryTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				updateNotesLabel();
			}
		});
		
		inventoryTable.setSurrendersFocusOnKeystroke(true);
		inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		inventoryTable.setFillsViewportHeight(true);
		inventoryTable.setColumnSelectionAllowed(true);
		inventoryTable.setToolTipText("Click to edit a record.  Right click and choose delete to delete a record.  Scroll down and click + to add new.");
		inventoryTable.setCellSelectionEnabled(true);
		scrollPaneForInventoryTable.setViewportView(inventoryTable);
		frmInventoryManager.getContentPane().add(scrollPaneForInventoryTable, "cell 0 0 8 1,growx,aligny top");
		
		JButton btnOpenProjectManager = new JButton("Open Project Manager");
		btnOpenProjectManager.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (DEBUG) System.out.println("This doesn't do anything yet.");
			}
		});
		
		// TODO: populate the list with things from the table, maybe?
		String list[] = {"Add Resistor Ohms", "Add Resistor kOhms", "Add Capacitor uF", "Add Capacitor F", "Add Diode", "Add LED", "Add Blank"};
		JComboBox comboBoxAddRecord = new JComboBox(list);
		comboBoxAddRecord.setToolTipText("Add new default record type or add blank record.");
		frmInventoryManager.getContentPane().add(comboBoxAddRecord, "cell 1 1, growx");
		comboBoxAddRecord.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String selected = (String) comboBoxAddRecord.getSelectedItem();
				if (DEBUG) System.out.println(selected);
				
				switch (selected) {
					case "Add Resistor Ohms": {
						if (DEBUG) System.out.println("In Add Resistor Ohms case"); 
						tableModel.addRow("Resistor", "Drawer", -1, "Ohms", "Through-hole");
						if (DEBUG) System.out.println(inventoryTable.getRowCount());
						break;
					}
					case "Add Resistor kOhms": {
						if (DEBUG) System.out.println("In Add Resistor kOhms case"); 
						tableModel.addRow("Resistor", "Drawer", -1, "kOhms", "Through-hole");
						break;
					}
					case "Add Capacitor uF": {
						if (DEBUG) System.out.println("In Add Capacitor uF case");
						tableModel.addRow("Capacitor", "Drawer", -1, "uF", "Through-hole");
						break;
					}
					case "Add Capacitor F": {
						if (DEBUG) System.out.println("In Add Capacitor F case");
						tableModel.addRow("Capacitor", "Drawer", -1, "F", "Through-hole");
						break;
					}
					case "Add Diode": {
						if (DEBUG) System.out.println("In Add Diode case");
						tableModel.addRow("Diode", "Drawer", -1, "", "Through-hole");
						break;
					}
					case "Add LED": {
						if (DEBUG) System.out.println("In Add LED case");
						tableModel.addRow("LED", "Drawer", -1, "", "Through-hole");
						break;
					}
					case "Add Blank": {
						if (DEBUG) System.out.println("In Add Blank case");
						tableModel.addRow("", "", -1, "", "");
						break;
					}
				}
				// select the first cell in the newly added row:
				inventoryTable.changeSelection(inventoryTable.getRowCount()-1, 0, false, false);
				inventoryTable.editCellAt(inventoryTable.getRowCount()-1, 0);
				inventoryTable.requestFocusInWindow();
			}
		});
		
		JButton btnDelete = new JButton("Delete");
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(tableModel.deleteRow(inventoryTable.getSelectedRow())) {
					setLabel("Record deleted.");
				} else {
					setLabel("Nothing to delete!");
				}
			}
		});
		frmInventoryManager.getContentPane().add(btnDelete, "cell 2 1");
		frmInventoryManager.getContentPane().add(btnSave, "cell 3 1");
		
		lblFeedbackLabel = new JLabel("");
		lblFeedbackLabel.setFont(new Font("Tahoma", Font.ITALIC, 11));
		lblFeedbackLabel.setForeground(Color.GRAY);
		frmInventoryManager.getContentPane().add(lblFeedbackLabel, "cell 5 1");
		frmInventoryManager.getContentPane().add(btnOpenProjectManager, "flowx,cell 7 1,alignx right,aligny center");
		
		JButton btnSetup = new JButton("Setup");
		btnSetup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// open setup window
				setupWindow.setVisible(true);
			}
		});
		frmInventoryManager.getContentPane().add(btnSetup, "cell 7 1");
		frmInventoryManager.getContentPane().add(btnCloseButton, "cell 7 1,alignx right,aligny center");
		
		// timer
		labelTimer = new Timer();
		
		// create the setup window last so it understands there's a connection
		setupWindow = new SetupWindow(this);
		setupWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setupWindow.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				setupWindow.setPrevValues();
				if (DEBUG) System.out.println("Component is now visible");
			}
		});
	}
	
	// display a message for 5 seconds
	// this behaves strangely if you keep pressing buttons quickly
	// TODO: find out why and fix it
	public void setLabel(String message) {
		lblFeedbackLabel.setText(message);
		TimerTask task = new TimerTask() {
			  @Override
			  public void run() {
			    lblFeedbackLabel.setText("");
			  }};
		labelTimer.schedule(task, 5000);
	}

	public InventoryTableModel openDBConnection(String URL, String user, String pass, String table, String port, String db) throws SQLException {
		if (this.hasConnection()) {
			connection.kill();
		}
		// update the strings
		this.URL = URL;
		this.user = user;
		this.pass = pass;
		this.tableName = table;
		this.port = port;
		this.DBName = db;
		
		// open the database
		connection = new DBConnector(this);
		
		// get a view with which to populate the table
		if (connection.getConnection() != null) {
			String selectStatement = "SELECT * FROM " + tableName + " ORDER BY partnum";
			ResultSet rs = connection.runSQL(selectStatement, false);
			
			// get primary key info:
			selectStatement = "SELECT * FROM information_schema.constraint_column_usage WHERE table_schema = 'public' and table_name = '" + tableName + "';";
			ResultSet primaryKeys = connection.runSQL(selectStatement, false);
			
			InventoryTableModel inventoryData = new InventoryTableModel(rs, primaryKeys);  
		    inventoryTable.setModel(inventoryData);
		    
		    return inventoryData;
		} else {
			return null;
		}
	}
	
	private void updateNotesLabel() {
		int row = inventoryTable.getSelectedRow();
		int col = tableModel.getNotesLoc();
		String string = (String) inventoryTable.getValueAt(row, col);
		if (string != null) {
			lblNotes.setText("Notes: " + string);
		} else {
			lblNotes.setText("Notes:");
		}
	}
	
	private void saveTable() throws SQLException {
		if (tableModel.getChangeTrackerListSize() == 0) {
			setLabel("Nothing to save!");
		} else {
			connection.save(tableModel);
			// clear the changeTrackerList!
			tableModel.clearChangeTrackerList();
			
			// let user know something happened
			setLabel("Database updated.");
			}
		}

	public void setTableModel(InventoryTableModel tableModel) {
		this.tableModel = tableModel;
	}
	
	// basic getters and setters
	////////////////////////////
	public String getTableName() {
		return tableName;
	}
	
	public String getURL() {
		return URL;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getPass() {
		return pass;
	}
	
	public String getPort() {
		return port;
	}
	
	public String getDBName() {
		return DBName;
	}
	
	public boolean hasConnection() {
		if (connection == null) { // this will happen in the constructor because the initial connection is in the process of being established
			return false;
		}
		if (connection.getConnection() == null) {
			return false;
		} else {
			return true;
		}
	}
}
