/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shishir
 */
import java.awt.Component;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.*;
public class ProgressRenderer extends JProgressBar implements TableCellRenderer {

    ProgressRenderer(int min, int max){
        super(min,max);
        setStringPainted(true);
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof Integer){
            setValue((Integer)value);
        }
        return(this);
    }
    
}
