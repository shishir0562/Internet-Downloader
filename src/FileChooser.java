/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shishir
 */
import java.io.File;
import javax.swing.JFileChooser;
public class FileChooser implements Runnable{

    private JFileChooser chooser;
    private Object o;
    String fileLocation=null;
    FileChooser(JFileChooser chooser, Object o, String location){
        this.chooser=chooser;
        this.o=o;
        fileLocation=location;
        Thread t=new Thread(this);
        t.start();
    }
    @Override
    public void run() {
        try{
            chooser.setCurrentDirectory(new File(fileLocation));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if(chooser.showDialog(null, "choose file location")==JFileChooser.APPROVE_OPTION){
                File f=chooser.getSelectedFile();
                fileLocation=f.getAbsolutePath();
            }
        }
        catch(Exception e){}
        finally{
            o=(Object)fileLocation;
        }
    }
    
}
