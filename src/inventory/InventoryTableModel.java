/* partly based on online examples from Oracle, partly my own work (Vicky) 
 * https://docs.oracle.com/javase/tutorial/uiswing/components/table.html#data
 * 
 * Purpose:  Provides variables and methods necessary for the table.
 */

package inventory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class InventoryTableModel extends AbstractTableModel {
	private ResultSetMetaData metaData; // probably redundant so remove later (I pull all needed data in constructor, so why am I keeping this?)
	private int numCols, numRows;
	private String[][] columnNamesAndTypes; // col0 = display name, col1 = original database name, col2 = data type  (database name will never have caps, spaces, etc.)
	private int lastRecordNum; // stores highest record number, for ease when adding new records
	private int notesLoc;
	private Object[][] data;
	private List<ChangeTrackerObject> changeTrackerList;// Purpose: whenever a change is made to a record, store the row and col info.
														// Really only care about row, though, so I may drop the col later.
														// When saving, iterate through this list and only save records that appear in it.
														// This is my attempt at reducing computation when updating the table.
														// No big deal for small tables, but saving whole table for something really big would suck,
														// so that's why I'm doing this.
	private int primaryKeyLocation; // program is designed for only one primary key
	
	public InventoryTableModel(ResultSet rs, ResultSet primaryKeys) throws SQLException {
		changeTrackerList = new ArrayList<ChangeTrackerObject>();
		metaData = rs.getMetaData();
		numCols = metaData.getColumnCount();
		
		// get primary key name
		// it's stored in the first record
		primaryKeys.next(); // advance to first record
		String primaryKeyName = (String) primaryKeys.getObject(4); // format comes directly from postgres
			
		// add numRows:
		numRows = 0;
		while (rs.next()) {
		    numRows++;
		}
		rs.beforeFirst(); // move cursor back to beginning
		    
		// now that we know the overall size of the data, we can do stuff
		columnNamesAndTypes = new String[numCols][3];
		data = new Object[numRows][numCols];
		    
		// add column names:
		for (int i = 1; i <= numCols; i++) { // it's frustrating, but there is a discrepancy in numbering between postgresql and java.  Postgresql likes to start at 1 and this carries over into some of the stuff you pull from it
			String temp = metaData.getColumnName(i);
			// check for primary key
			if (temp.equals(primaryKeyName)) {
				// we just want the column number
				primaryKeyLocation = i-1;
			}
			// check for notes location
			if (temp.equals("notes")) {
				// we just want the column number but if the primary key was found first, then we need to subtract 2 instead of 1 (because primary key will be
				// hidden in table view)
				if (i > primaryKeyLocation+1) {
					notesLoc = i-2;
				} else {
					notesLoc = i-1;
				}
			}
			columnNamesAndTypes[i-1][1] = temp; // original name
			temp = Character.toUpperCase(temp.charAt(0)) + temp.substring(1);
		   	columnNamesAndTypes[i-1][0] = temp; // I like to convert first letter to capitals
		   	columnNamesAndTypes[i-1][2] = Integer.toString(metaData.getColumnType(i)); // data type (12 = what normal people call a string, 4 = int, 7 = float)
		}
			
		// populate the data array:
		lastRecordNum = 0; // let's also find the highest recordnum at the same time
		rs.next(); // result sets start before the first record, so we have to advance it right away
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				data[i][j] = rs.getObject(j+1); // this keeps object type intact
				if (j == numCols - 1) {// if true, we are the last col
					if (lastRecordNum < (int) data[i][primaryKeyLocation]) {
						lastRecordNum = (int) data[i][primaryKeyLocation];
					}
				}
			}
			rs.next();
		}
		rs.beforeFirst(); // reset it
			
			
			// here's how you add a table changed listener, if you ever find you need it (which you probably won't unless you have multiple users):
		/*	addTableModelListener(new TableModelListener() {
			    public void tableChanged(TableModelEvent e) {
			    	int row = e.getFirstRow();
			        int column = e.getColumn();
			        InventoryTableModel model = (InventoryTableModel)e.getSource();
			        Object data = model.getValueAt(row, column);
			          
			        System.out.println("tableChanged says:  Row " + row + ", colum " + column + ", value " + data);
			        }
			    });*/
			
			
		}

	public int getNotesLoc() {
		return notesLoc;
	}

	public int getColumnCount() {
        return columnNamesAndTypes.length;
    }

    public int getRowCount() {
        return data.length;
    }

    public String getColumnName(int col) {
        return columnNamesAndTypes[col][0];
    }
    
    public int getColumnType(int col) {
    	return Integer.parseInt(columnNamesAndTypes[col][2]);
    }
    
    public String getOriginalColumnName(int col) {
    	return columnNamesAndTypes[col][1];
    }

    public Object getValueAt(int row, int col) {
    	return data[row][col]; // TODO: there's a strange bug that occasionally comes up here with out of bounds error.  Try to reproduce it.  Bug may have happened due to another now fixed bug, though.
    }

    public boolean isCellEditable(int row, int col) {
    	if (col == primaryKeyLocation) return false; // this is recordnum, which is the uneditable primary key
    	else return true; // everything else is editable
    }

    public void setValueAt(Object value, int row, int col) {
    	if (MainWindow.DEBUG) System.out.println("setValueAt says: " + value);
    	if (MainWindow.DEBUG) System.out.println("old data: " + data[row][col]);
    	
    	// let's try to implement some sanity checking here
    	// we really just need to check type and to make sure numbers are >= 0
    	
        data[row][col] = value;
        
        updateChangeTracker(row, (int) data[row][primaryKeyLocation]); 
        fireTableCellUpdated(row, col);

    }
    
    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  
     */
    public Class getColumnClass(int row, int col) {
        return getValueAt(row, col).getClass();
    }
    
    /*
     * the following method updates the change tracker list
     */
    public void updateChangeTracker(int row, int recordnum) {
    	// row is the row in the array
    	// recordnum is the record number (or row) in the database
    	
    	// see if it exists already
    	if (changeTrackerList.size() > 0) {
    		// iterate through the list and do a search
    		boolean found = false;
    		for (int i = 0; i <= changeTrackerList.size()-1; i++) {
    			if (changeTrackerList.get(i).equals(row)) {
    				found = true;
    				i = changeTrackerList.size();  // break
    			}
    		}

    		if (found == false) {
   				changeTrackerList.add(new ChangeTrackerObject(row, recordnum, false, false));
   			}
    	}
    	else changeTrackerList.add(new ChangeTrackerObject(row, recordnum, false, false));
    	if (MainWindow.DEBUG) System.out.println("changeTrackerList size is " + changeTrackerList.size());
    }
    
    public int getChangeTrackerListSize() {
    	return changeTrackerList.size();
    }
    
    public List<ChangeTrackerObject> getChangeTrackerList() {
    	return changeTrackerList;
    }
    
    public void clearChangeTrackerList() {
    	changeTrackerList.clear();
    }

    // this adds a row and fills in data if nonempty strings are passed in.
    // THIS IS FORMATTED TO FIT MY INVENTORY TABLE SPECIFICALLY
    // if using this with another program, either change it or only ever add blank records (because blank records are all you're gonna get)
    // TODO: make this a bit more generic.  I don't actually like it this way but I'm doing it to get it working.  Make it use column names instead.  That is
    // as generic as I can make it.
	public boolean addRow(String descrip, String loc, float val, String unit, String mount) {
		Object[][] tempData = new Object[numRows][numCols];
		System.arraycopy(data, 0, tempData, 0, data.length);
		data = new Object[numRows+1][numCols];
		System.arraycopy(tempData, 0, data, 0, tempData.length);
		
		changeTrackerList.add(new ChangeTrackerObject(numRows, lastRecordNum+1, true, false));
		
		makeNonEmpty(numRows);

		lastRecordNum++;
		data[numRows][primaryKeyLocation] = lastRecordNum; 
		
		// fill in given values
		// TODO: use column names instead
		if (!descrip.equals("")) data[numRows][1] = descrip;
		if (!loc.equals("")) data[numRows][3] = loc;
		if (val >= 0) data[numRows][4] = val;
		if (!unit.equals("")) data[numRows][5] = unit;
		if (!mount.equals("")) data[numRows][6] = mount;

		numRows++;
		
		fireTableDataChanged();
		return true;
	}

	public boolean deleteRow(int rowNum) {
		// opposite of addRow
		if (rowNum == -1) return false; // exit immediately if we are not passed a valid row number
		// get record number first
		Object[][] tempData = new Object[numRows][numCols];
		System.arraycopy(data, 0, tempData, 0, data.length);
		data = new Object[numRows-1][numCols];
		// now it's a bit harder because we have to eliminate the specified row so we can't just blindly copy
		// also, it's possible we're deleting the last record, which means we have to update lastRecordNum
		int k = 0;
		for (int i = 0; i < numRows; i++) {
			if (i != rowNum) {
				for (int j = 0; j < numCols; j++) {
					data[k][j] = tempData[i][j];
				}
				k++;
			}
		}
		
		changeTrackerList.add(new ChangeTrackerObject(rowNum, (int) tempData[rowNum][primaryKeyLocation], false, true));
		
		if (rowNum == numRows) {
			// we need to update lastRecordNum
			lastRecordNum = 0;
			for (int i = 0; i < numRows; i++) {
				if (lastRecordNum < (int) data[i][primaryKeyLocation]) {
					lastRecordNum = (int) data[i][primaryKeyLocation];
				}
			}
		}
		
		numRows--;
		fireTableDataChanged();
		return true;
	}
	
	// makeNonEmpty simply ensures the given row has no empty fields
	// program will crash with empty fields.  Beware of this if you ever edit the table outside this program.
	private void makeNonEmpty(int row) {
		// go through and make sure all numeric fields are nonzero and all string fields are nonempty
		for (int i = 0; i <= columnNamesAndTypes.length-1; i++) {
			if (Integer.parseInt(columnNamesAndTypes[i][2]) == 12) {
				// string
				data[row][i] = "";
			} else if (Integer.parseInt(columnNamesAndTypes[i][2]) == 4) {
				// int
				data[row][i] = 0;
			} else if (Integer.parseInt(columnNamesAndTypes[i][2]) == 7) {
				// float
				data[row][i] = 0.0;
			}
		}
	}

	public boolean findRecord(int row) {
		for (int i = 0; i < numRows; i++) {
			if (row == (int) data[i][primaryKeyLocation]) {
				return true;
			}
		}
		return false;
	}
	
	public int getPrimaryKey(){
		return primaryKeyLocation;
	}
}	


