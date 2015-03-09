/* this editor will just implement the cell editors so they highlight the text */

package inventory;

import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.JTextField;

import java.awt.Component;
 
public class TextEditor extends DefaultCellEditor {
    JFormattedTextField ftf;
     
    public TextEditor() {
    	super(new JFormattedTextField());
    }
 
    //Override to invoke setValue on the formatted text field.
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected,
            int row, int column) {
        JFormattedTextField ftf =
            (JFormattedTextField)super.getTableCellEditorComponent(
                table, value, isSelected, row, column);
        ftf.setValue(value);
        
        // select the whole text field if you start typing in it freshly:
        JTextField field = (JTextField) editorComponent;
        if (isSelected) {
            field.selectAll();
        }
        return ftf;
    }

 
}
