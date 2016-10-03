/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shishir
 */
import javax.swing.*;
import java.util.*;
import javax.swing.event.*;
import java.net.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.awt.Point;
import java.awt.Desktop;
import java.io.IOException;
public class DownloadManagerUI extends javax.swing.JFrame implements Observer {

    /**
     * Creates new form DownloadManagerUI
     */
    private Download download;
    private Proxy proxy=Proxy.NO_PROXY;
    private ProxyConfigUI proxyConfig=null;
    private DownloadInfoUI dInfo=null;
    private int rowIndex=-1;
    /*variable to define the location of target file*/
    private static String fileLocation=null;
    // variable to define the location of temporary file
    private static String tempLocation=null;
    DefaultTableModel model ;
    // Array list to store the objects of Download class...
    ArrayList<Download> downloadList=new ArrayList<>();
    // fires when the row has been selected...
    private void tableValueChanged(){
        rowIndex=downloadTable.getSelectedRow();
        Download d=downloadList.get(rowIndex);
        updateButtons(d);
    }
    // called a automaticall at the start of the application setting the location of 
    // target file and temporary file
    private static void setDefaultLocation(){
        try{
            if(System.getProperty("os.name").startsWith("Windows")){
                // to the home directory of the user depends upon OS
                fileLocation=System.getProperty("user.home")+"\\Downloads";
                File f=new File(System.getProperty("user.home")+"\\Documents"+"\\Internet Downloader");
                if(!f.exists())
                    f.mkdir();
                tempLocation=f.getAbsolutePath();
            }
            else{
                fileLocation=System.getProperty("user.dir");
                try{
                    File temp=File.createTempFile("temp-file", ".tmp");
                    String abpath=temp.getAbsolutePath();
                    String Location=abpath.substring(0, abpath.lastIndexOf(File.separator));
                    temp=new File(Location+"/Internet Downloader");
                    if(!temp.exists())
                        temp.mkdir();
                    tempLocation=temp.getAbsolutePath();
                }
                catch(Exception e){}
            }
            saveTo.setText(fileLocation);
        }
        catch(ArrayIndexOutOfBoundsException | ArithmeticException e){}
    }
    // method to add downloads
    private void addDownload(){
        if(proxyConfig!=null)
            proxy=proxyConfig.getProxy();
        //System.out.println(proxyConfig.isUsingProxy());
        // URL verification
        URL verifiedUrl = verifyUrl(addUrl.getText());
        if(verifiedUrl!=null){
            download=new Download(verifiedUrl,fileLocation,tempLocation,proxy);
            // adding DownloadManager class obect as an observer to the Download class object...
            download.addObserver((Observer)this);
            downloadList.add(download);    // adding download object to the list ...
            addRow(download);             // adding row to the table ..
            addUrl.setText("");          // clears the addURL field....
            updateButtons(download);
            download.startDownload();
        }
        else{
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    JOptionPane.showMessageDialog(null, "Invalid Download URL", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }
    // method to verify URL
    private URL verifyUrl(String url){
        URL verifiedUrl=null;
        boolean b1=url.startsWith("http://")||url.startsWith("https://");
        boolean b2=false;
        for(int i=0;i<url.length()&&b1;++i){
            if(url.charAt(i)==' '){
                b2=true;
                break;
            }
        }
        if(b1==true&&b2==false){
            try{
                verifiedUrl=new URL(url);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        //System.out.println(verifiedUrl.toString());
        return verifiedUrl;
    }
    // method to add new row
    private void addRow(Download d){
        // setting data structure of the table to the object model....
        model=(DefaultTableModel)downloadTable.getModel();
        model.addRow(new Object[]{d.getFileName(),null,null,0,Download.statuses[d.status],null});
        int index=downloadTable.getRowCount();
        // notify row insertion to all event listeners of the table....
        model.fireTableRowsInserted(index-1, index-1);
    }
    // invokes when pause button fires....
    private void actionPause(){
        if(rowIndex!=-1){
            Download d=downloadList.get(rowIndex);
            d.setPause();
            updateButtons(d);
        }
        else
            JOptionPane.showMessageDialog(this, "Please Select the download to be paused", "Warning", JOptionPane.WARNING_MESSAGE);
    }
    // invlokes when resume button fires ...
    private void actionResume(){
        if(rowIndex!=-1){
            Download d=downloadList.get(rowIndex);
            // setting data structure of the table to the object model....
            model=(DefaultTableModel)downloadTable.getModel();
            d.setResume();
            // notify the table rows updated to all the listeners of table ...
            model.fireTableRowsUpdated(rowIndex, rowIndex);
            updateButtons(d);
        }
        else
            JOptionPane.showMessageDialog(this, "Please Select the download to be resumed", "Warning", JOptionPane.INFORMATION_MESSAGE);
    }
    // invokes when cancel button fires ...
    private void actionCancel(){
        if(rowIndex!=-1){
            Download d=downloadList.get(rowIndex);
            d.setCancel();
            updateButtons(d);
        }
        else
            JOptionPane.showMessageDialog(this, "Please Select the download to be cancel", "Warning", JOptionPane.INFORMATION_MESSAGE);
    }
    // invokes when clear buttons fires...
    private void actionClear(){
        if(rowIndex!=-1){
            Download d=downloadList.remove(rowIndex);
            d.deleteObservers();
            model=(DefaultTableModel)downloadTable.getModel();
            model.removeRow(rowIndex);
            rowIndex=-1;
            //model.fireTableRowsDeleted(rowIndex, rowIndex);
        }
        else
            JOptionPane.showMessageDialog(this, "Please Select the download to be clear", "Warning", JOptionPane.INFORMATION_MESSAGE);
    }
    // called by the Dowload class object to notify the objects....
    @Override
    public void update(Observable o, Object arg) {
        model = (DefaultTableModel)downloadTable.getModel();
        int index=downloadList.indexOf(o);
        Download d=(Download)o;
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                // setting the values at specified column of the table...
                downloadTable.setValueAt(d.getSize(), index, 1);
                downloadTable.setValueAt(d.getTransferRate(), index, 2);
                downloadTable.setValueAt(d.getProgress(), index, 3);
                downloadTable.setValueAt(Download.statuses[d.status], index, 4);
                downloadTable.setValueAt(d.resumeCapability, index, 5);
                model.fireTableRowsUpdated(index, index);
            }
        });
    }
    // called when exit button invoked ...
    public void onExit(){
        boolean flag=false;
        for(int i=0;i<downloadList.size();++i){
            if(downloadList.get(i).status==2){
                flag=true;
                break;
            }
        }
        if(flag){
            JOptionPane.showMessageDialog(this,"sorry you cannot close the window while appending", null, JOptionPane.INFORMATION_MESSAGE);
        }
        else
            System.exit(0);
    }
    // update buttons..
    private void updateButtons(Download d){
        if(rowIndex==-1) return;
        int status=d.status;
        SwingUtilities.invokeLater(() -> {
            switch(status){
                case 0: resumeButton.setEnabled(false);
                pauseButton.setEnabled(true);
                cancelButton.setEnabled(true);
                clearButton.setEnabled(false);
                break;
                case 1: resumeButton.setEnabled(true);
                pauseButton.setEnabled(false);
                cancelButton.setEnabled(true);
                clearButton.setEnabled(true);
                break;
                case 2: resumeButton.setEnabled(false);
                pauseButton.setEnabled(false);
                cancelButton.setEnabled(false);
                clearButton.setEnabled(false);
                break;
                case 3: resumeButton.setEnabled(false);
                pauseButton.setEnabled(false);
                cancelButton.setEnabled(false);
                clearButton.setEnabled(true);
                break;
                case 4: resumeButton.setEnabled(true);
                pauseButton.setEnabled(false);
                cancelButton.setEnabled(false);
                clearButton.setEnabled(true);
                break;
                default:resumeButton.setEnabled(true);
                pauseButton.setEnabled(false);
                cancelButton.setEnabled(false);
                clearButton.setEnabled(true);
            }
        });
    }
    public DownloadManagerUI() {
        initComponents();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popUp = new javax.swing.JPopupMenu();
        open = new javax.swing.JMenuItem();
        openFolder = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        downloadInfo = new javax.swing.JMenuItem();
        downloadsPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        downloadTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        resumeButton = new javax.swing.JButton();
        pauseButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        addUrlPanel = new javax.swing.JPanel();
        addUrl = new javax.swing.JTextField();
        downloadButton = new javax.swing.JButton();
        fileButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        saveTo = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        exit = new javax.swing.JMenuItem();
        settingMenu = new javax.swing.JMenu();
        proxySettings = new javax.swing.JMenuItem();

        open.setText("Open");
        open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openActionPerformed(evt);
            }
        });
        popUp.add(open);

        openFolder.setText("Open Folder");
        openFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFolderActionPerformed(evt);
            }
        });
        popUp.add(openFolder);
        popUp.add(jSeparator1);

        downloadInfo.setText("Download Info");
        downloadInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadInfoActionPerformed(evt);
            }
        });
        popUp.add(downloadInfo);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Internet Downloader");
        setBackground(new java.awt.Color(255, 255, 255));
        setIconImage(new ImageIcon(getClass().getResource("earth-download.png")).getImage());
        setLocationByPlatform(true);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        downloadsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Downloads"));

        downloadTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "File Name", "Size", "Transfer Rate", "Progress", "Status", "Resume Capability"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        downloadTable.getColumnModel().getColumn(3).setCellRenderer(new ProgressRenderer(0,100));
        downloadTable.setToolTipText("download table");
        downloadTable.setComponentPopupMenu(popUp);
        downloadTable.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        downloadTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        downloadTable.setSurrendersFocusOnKeystroke(true);
        downloadTable.getTableHeader().setReorderingAllowed(false);
        downloadTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e){
                if(!e.getValueIsAdjusting())
                tableValueChanged();
            }
        });
        downloadTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                downloadTableMousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(downloadTable);
        if (downloadTable.getColumnModel().getColumnCount() > 0) {
            downloadTable.getColumnModel().getColumn(0).setPreferredWidth(150);
            downloadTable.getColumnModel().getColumn(1).setResizable(false);
            downloadTable.getColumnModel().getColumn(1).setPreferredWidth(10);
            downloadTable.getColumnModel().getColumn(2).setResizable(false);
            downloadTable.getColumnModel().getColumn(2).setPreferredWidth(30);
            downloadTable.getColumnModel().getColumn(3).setResizable(false);
            downloadTable.getColumnModel().getColumn(3).setPreferredWidth(120);
            downloadTable.getColumnModel().getColumn(4).setResizable(false);
            downloadTable.getColumnModel().getColumn(5).setResizable(false);
        }

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Options"));

        resumeButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        resumeButton.setText("Resume");
        resumeButton.setEnabled(false);
        resumeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resumeButtonActionPerformed(evt);
            }
        });

        pauseButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        pauseButton.setText("Pause");
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseButtonActionPerformed(evt);
            }
        });

        cancelButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cancelButton.setText("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        clearButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        clearButton.setText("Clear");
        clearButton.setEnabled(false);
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(resumeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(68, 68, 68)
                .addComponent(pauseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(80, 80, 80)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(resumeButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                    .addComponent(pauseButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(clearButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout downloadsPanelLayout = new javax.swing.GroupLayout(downloadsPanel);
        downloadsPanel.setLayout(downloadsPanelLayout);
        downloadsPanelLayout.setHorizontalGroup(
            downloadsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        downloadsPanelLayout.setVerticalGroup(
            downloadsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(downloadsPanelLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 287, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        addUrlPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Add URL"));

        addUrl.setToolTipText("add URL to begin download");

        downloadButton.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        downloadButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Button-Download-icon.png"))); // NOI18N
        downloadButton.setText("Download");
        downloadButton.setToolTipText("click to download");
        downloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });

        fileButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/folder-my-pictures-icon.png"))); // NOI18N
        fileButton.setToolTipText("select file location");
        fileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Save To:");

        saveTo.setEditable(false);
        saveTo.setBorder(null);

        javax.swing.GroupLayout addUrlPanelLayout = new javax.swing.GroupLayout(addUrlPanel);
        addUrlPanel.setLayout(addUrlPanelLayout);
        addUrlPanelLayout.setHorizontalGroup(
            addUrlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addUrlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(addUrlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(addUrlPanelLayout.createSequentialGroup()
                        .addComponent(addUrl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fileButton, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(10, 10, 10))
                    .addGroup(addUrlPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(saveTo, javax.swing.GroupLayout.PREFERRED_SIZE, 455, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addComponent(downloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        addUrlPanelLayout.setVerticalGroup(
            addUrlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addUrlPanelLayout.createSequentialGroup()
                .addGroup(addUrlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addUrl)
                    .addComponent(fileButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(addUrlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addGap(3, 3, 3))
            .addGroup(addUrlPanelLayout.createSequentialGroup()
                .addComponent(downloadButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Untitled-1.png"))); // NOI18N

        fileMenu.setText("File");

        exit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        exit.setText("Exit");
        exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitActionPerformed(evt);
            }
        });
        fileMenu.add(exit);

        jMenuBar1.add(fileMenu);

        settingMenu.setText("Settings");

        proxySettings.setText("Proxy Settings");
        proxySettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proxySettingsActionPerformed(evt);
            }
        });
        settingMenu.add(proxySettings);

        jMenuBar1.add(settingMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(downloadsPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(addUrlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addUrlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(downloadsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitActionPerformed
        onExit();
    }//GEN-LAST:event_exitActionPerformed

    private void resumeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resumeButtonActionPerformed
        actionResume();
    }//GEN-LAST:event_resumeButtonActionPerformed

    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadButtonActionPerformed
        if(fileLocation==null){
            JOptionPane.showMessageDialog(this, "Please choose the file location\nbefore starting download", "oops", JOptionPane.WARNING_MESSAGE);
        }
        else
            addDownload();
    }//GEN-LAST:event_downloadButtonActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        actionClear();
    }//GEN-LAST:event_clearButtonActionPerformed

    private void pauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseButtonActionPerformed
        actionPause();
    }//GEN-LAST:event_pauseButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        actionCancel();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void fileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileButtonActionPerformed
        JFileChooser chooser=new JFileChooser();
        chooser.setCurrentDirectory(new File(fileLocation));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        SwingUtilities.invokeLater(() -> {
            if(chooser.showDialog(null,"choose file location")==JFileChooser.APPROVE_OPTION){
                File f=chooser.getSelectedFile();
                fileLocation=f.getAbsolutePath();
                saveTo.setText(fileLocation);
            }
        });
        
    }//GEN-LAST:event_fileButtonActionPerformed

    private void downloadTableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_downloadTableMousePressed
        Point p=evt.getPoint();
        int row=downloadTable.rowAtPoint(p);
        downloadTable.setRowSelectionInterval(row, row);
        Download d=downloadList.get(row);
        if(d.status!=3){
            open.setEnabled(false);
            openFolder.setEnabled(false);
        }
    }//GEN-LAST:event_downloadTableMousePressed

    private void openActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openActionPerformed
        int row=downloadTable.getSelectedRow();
        String path=downloadList.get(row).getLocation();
        //System.out.println(path);
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "sorry could not open the file");
        }
    }//GEN-LAST:event_openActionPerformed

    private void openFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFolderActionPerformed
        int row=downloadTable.getSelectedRow();
        String path=downloadList.get(row).getLocation();
        try {
            String dir=path.substring(0, path.lastIndexOf('\\'));
            Desktop.getDesktop().open(new File(dir));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "sorry could not open the file");
        }
    }//GEN-LAST:event_openFolderActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        onExit();
    }//GEN-LAST:event_formWindowClosing

    private void proxySettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proxySettingsActionPerformed
        if(proxyConfig==null){
            proxyConfig = new ProxyConfigUI(this,true);
        }
        proxyConfig.setVisible(true);
    }//GEN-LAST:event_proxySettingsActionPerformed

    private void downloadInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadInfoActionPerformed
        int row=downloadTable.getSelectedRow();
        String path=downloadList.get(row).getLocation();
        try {
            Download d=downloadList.get(row);
            if(dInfo==null)
                dInfo = new DownloadInfoUI(this,true);
            dInfo.setPath(d, path);
            dInfo.setVisible(true);
        } 
        catch (Exception ex) {}
    }//GEN-LAST:event_downloadInfoActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DownloadManagerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DownloadManagerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DownloadManagerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DownloadManagerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DownloadManagerUI().setVisible(true);
                setDefaultLocation();
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField addUrl;
    private javax.swing.JPanel addUrlPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearButton;
    private javax.swing.JButton downloadButton;
    private javax.swing.JMenuItem downloadInfo;
    private javax.swing.JTable downloadTable;
    private javax.swing.JPanel downloadsPanel;
    private javax.swing.JMenuItem exit;
    private javax.swing.JButton fileButton;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JMenuItem open;
    private javax.swing.JMenuItem openFolder;
    private javax.swing.JButton pauseButton;
    private javax.swing.JPopupMenu popUp;
    private javax.swing.JMenuItem proxySettings;
    private javax.swing.JButton resumeButton;
    private static javax.swing.JTextField saveTo;
    private javax.swing.JMenu settingMenu;
    // End of variables declaration//GEN-END:variables

}
