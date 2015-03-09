/*
 * Provide variables and methods for the change tracker list
 */

package inventory;

public class ChangeTrackerObject {
	private int row;
	private int recordnum;
	private boolean newRecord;
	private boolean deletedRecord;
	
	public ChangeTrackerObject(int row, int recordnum, boolean newRecord, boolean deletedRecord){
		this.row = row;
		this.recordnum = recordnum;
		this.newRecord = newRecord;
		this.deletedRecord = deletedRecord;
	}
	
	public int getRow() {
		return row;
	}
	
	public int getRecordnum() {
		return recordnum;
	}
	
	public boolean equals(int row) {
		if (row == this.getRow()) // really only care about row.  I send SQL statements to update entire record (row) at once
			return true;
		else
			return false;
	}
	
	public boolean isNewRecord() {
		return newRecord;
	}
	
	public boolean isDeletedRecord() {
		return deletedRecord;
	}
}
