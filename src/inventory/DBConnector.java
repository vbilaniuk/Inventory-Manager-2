/*
 * This establishes a connection to a postgresql database via jdbc driver.
 * Allows communication with it via sql statements.
 * Contains the basic code for saving/updating the table.
 * 
 * TODO: build in checks to make sure we get successful queries/updates.  I'm a little worried about firing off large numbers of changes in rapid succession.
 * 
 */

package inventory;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBConnector{
	private Connection connection;
	private String db;
	private String table;
	private String user;
	private String URL;
	private String pass;
	private String port;
	private MainWindow callingWindow; // need to change this
	private ResultSet rs;
	
	public DBConnector(MainWindow callingWindow){
		this.callingWindow = callingWindow;
		this.URL = callingWindow.getURL();
		this.user = callingWindow.getUser();
		this.pass = callingWindow.getPass();
		this.db = callingWindow.getDBName();
		this.table = callingWindow.getTableName();
		this.port = callingWindow.getPort();
		connection = Connect();
	}
	
	// this version will always connect with the settings stored in this object
	public Connection Connect() {
		return Connect(this.URL, this.user, this.pass, this.db, this.port);
	}
	
	// this version requires parameters 
	public Connection Connect(String URL, String user, String pass, String table, String port) {
		try {
			 
			Class.forName("org.postgresql.Driver"); // see http://jdbc.postgresql.org/documentation/93/load.html

		} catch (ClassNotFoundException error) {

			if (MainWindow.DEBUG) System.out.println("JDBC Driver Load Error");
			//error.printStackTrace();
			return null;

		}
		 
		try {
			Properties props = new Properties();
			String url = "jdbc:postgresql://" + URL + ":" + port + "/" + db; 
			props.setProperty("user", user);
			props.setProperty("password", pass);
			connection = DriverManager.getConnection(url, props);
			return connection;
		} catch (SQLException e) {
 
			if (MainWindow.DEBUG) System.out.println("Connection Failed! Check output console");
			//e.printStackTrace();
			return null;
		}
	}
	
	// basic getters/setters:
	/////////////////////////
	public Connection getConnection() {
		return connection;
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
	
	public String getDB() {
		return db;
	}
	
	public String getPort() {
		return port;
	}
	///////////////////////////

	// save the table
	public void save(InventoryTableModel tableModel) throws SQLException {
		ResultSet rs = null;
		int size = 0;
		callingWindow.setLabel("Saving...");
		String sql = "";
		
		// go through the changeTrackerList and fire off SQL update statements for each item in the list
		for (int i = 0; i <= tableModel.getChangeTrackerListSize()-1; i++) {
			int rowToChange = tableModel.getChangeTrackerList().get(i).getRow();

			if (tableModel.getChangeTrackerList().get(i).isDeletedRecord()) {
				if (MainWindow.DEBUG) System.out.println("Deleting record number " + tableModel.getChangeTrackerList().get(i).getRecordnum());
				// first check to see if that sucker exists in the database!
				sql = "SELECT * FROM " + table + " WHERE recordnum = " + tableModel.getChangeTrackerList().get(i).getRecordnum() + ";";
				rs = runSQL(sql, false); // fire SQL statement off
				
				// find size of result set
				while (rs.next()) size++;
				if (size > 0) {
					sql = "DELETE FROM " + table + " WHERE recordnum = " + tableModel.getChangeTrackerList().get(i).getRecordnum() + ";";
				} else {
					callingWindow.setLabel("Record could not be deleted.");
					sql = "";
				}
			} else if (tableModel.getChangeTrackerList().get(i).isNewRecord()) {
				if (MainWindow.DEBUG) System.out.println("Saving change number " + rowToChange);
				
				// find record first in table to see if it still exists (we may have deleted it before saving)
				if(tableModel.findRecord(tableModel.getChangeTrackerList().get(i).getRecordnum())) {
					// set up SQL statement for new record
					sql = "INSERT INTO " + table + " (";
					
					for (int j = 0; j < tableModel.getColumnCount(); j++) {
						if (j >= tableModel.getColumnCount()-1) {
							sql = sql + tableModel.getOriginalColumnName(j) + ") VALUES (";
						} else {
							sql = sql + tableModel.getOriginalColumnName(j) + ", ";
						}
					}
					
					for (int j = 0; j < tableModel.getColumnCount(); j++) {
						if (tableModel.getColumnType(j) == 12) {
							// string
							sql = sql + "'";
							String temp = (String) tableModel.getValueAt(rowToChange, j);
							if (j >= tableModel.getColumnCount()-1) { // if it's at the last column you want saved, you have to treat it differently
								if (temp == null) temp = "";
								sql = sql + temp + "' ";
							} else {
								if (temp == null) temp = "";
								sql = sql + temp + "', ";
							}
						}
						
						if (tableModel.getColumnType(j) == 4 || tableModel.getColumnType(j) == 7) {
							// int or float
							// TO TEST:  What happens if an int or float field is empty?  If record is created in this program, it won't be empty, but it could
							// come empty from the database.
							// partial answer: This program dies if you have an empty int or float field.  You can't even get to saving.  It won't even initialize.
							// work around for now is to make sure any int/float fields you add outside this program are set to 0
							sql = sql + " ";
							if (j >= tableModel.getColumnCount()-1) {
								sql = sql + tableModel.getValueAt(rowToChange, j);
							} else {
								sql = sql + tableModel.getValueAt(rowToChange, j) + ", ";
							}
						}
					}
					sql = sql + ");";
				} else {
					callingWindow.setLabel("Couldn't insert record.");
					sql = "";
				}
			} else {
				// set up SQL statement to update existing record
				// first make sure record exists
				sql = "SELECT * FROM " + table + " WHERE recordnum = " + tableModel.getChangeTrackerList().get(i).getRecordnum() + ";";
				rs = runSQL(sql, false); // fire SQL statement off
				
				// find size of result set
				while (rs.next()) size++;
				if (size > 0) {
					sql = "UPDATE " + table + " SET ";
					for (int j = 0; j < tableModel.getColumnCount(); j++) {
						if (tableModel.getColumnType(j) == 12) {
							// string
							sql = sql + tableModel.getOriginalColumnName(j) + " = '";
							String temp = (String) tableModel.getValueAt(rowToChange, j);
							if (j >= tableModel.getColumnCount()-1) { // if it's at the last column you want saved, you have to treat it differently
								if (temp == null) temp = "";
								sql = sql + temp + "' ";
							} else {
								if (temp == null) temp = "";
								sql = sql + temp + "', ";
							}
						}
						
						if (tableModel.getColumnType(j) == 4 || tableModel.getColumnType(j) == 7) {
							// int or float
							// TO TEST:  What happens if an int or float field is empty?  If record is created in this program, it won't be empty, but it could
							// come empty from the database.
							// partial answer: This program dies if you have an empty int or float field.  You can't even get to saving.  It won't even initialize.
							// work around for now is to make sure any int/float fields you add outside this program are set to 0
							sql = sql + tableModel.getOriginalColumnName(j) + " = ";
							if (j >= tableModel.getColumnCount()-1) {
								sql = sql + tableModel.getValueAt(rowToChange, j) + " ";
							} else {
								sql = sql + tableModel.getValueAt(rowToChange, j) + ", ";
							}
						}
					}
				} else {
					callingWindow.setLabel("Could not update record.");
				}
				if (!sql.isEmpty()) {
					sql = sql + "WHERE recordnum = " + tableModel.getValueAt(rowToChange,  tableModel.getPrimaryKey()) + ";";
				}
			}
			if (MainWindow.DEBUG) System.out.println(sql);
			if  (!sql.isEmpty()) {
				runSQL(sql, true);// fire SQL statement off
			}
		}
	}

	// generalized SQL handler
	// returns a result set if you performed a select statement
	// returns null if successfully updated (result set is useless in an update and should be ignored)
	public ResultSet runSQL(String selectStatement, boolean updating) throws SQLException {
		if (!updating) {
			Statement st = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE); //scrollable, don't show changes from others, updatable
			rs = st.executeQuery(selectStatement);    
			return rs;
		} else {
			// we are performing an update and must use a prepared statement instead
			PreparedStatement pst = connection.prepareStatement(selectStatement);
			pst.executeUpdate();
			return null;
		}
}
	
	public void kill() throws SQLException {
		rs.close();
		connection.close();
	}
}