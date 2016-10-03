/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shishir
 */
import java.awt.HeadlessException;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
public class Download extends Observable implements Observer{
    
    public static final String statuses[]={"downloading...","paused","appending...","completed","cancelled","error"};
    private static final int DOWNLOADING = 0;
    private static final int PAUSE = 1;
    private static final int APPENDING = 2;
    private static final int COMPLETE = 3;
    private static final int CANCEL = 4;
    private static final int ERROR = 5;
    private static final int kb=1024;
    private static final int mb=kb*1024;
    private static final long gb=mb*1024;
    private URL url;
    private long bytesRead=0;
    private long totalDownloaded=0;
    private long contentLength;
    private boolean redirect=false;
    private String fileLocation;
    private String tempLocation;
    private ArrayList<DownloadThread> threadArray=null;
    private DownloadThread t;
    private long bytesReceived[];
    private int countCompleted=0;
    private int threadCount;
    private long start_time=0;
    private long current_time=0;
    private long size=0;
    private String contentType=null;
    private Proxy proxy;
    public String resumeCapability=null;
    public int status;
    Download(URL url, String fileLocation,String tempLocation,Proxy proxy){
        this.contentLength = 0;
        this.fileLocation=fileLocation;
        this.url=url;
        this.tempLocation=tempLocation;
        this.proxy=proxy;
        status = DOWNLOADING;
        mkTempDir();
    }
    public void startDownload(){
        download();
    }
    private void mkTempDir(){
        File f=new File(tempLocation+"\\"+getFile(url));
        if(!f.exists())
            f.mkdir();
        else{
            
        }
        tempLocation=f.getAbsolutePath();
    }
    public String getUrl(){
        return url.toString();
    }
    public int getStatus(){
        return status;
    }
    public String getSize(){
        if(size>=gb)
            return String.format("%.2f", (double)size/gb)+" GB";
        if(size>=mb)
            return String.format("%.2f", (double)size/mb)+" MB";
        else
            return String.format("%.2f", (double)size/kb)+" KB";
    }
    public int getProgress(){
        Double d=new Double(0);
        if(size>0){
            d=(double)(totalDownloaded*100)/size;
        }
        return (int)Math.ceil(d);
    }
    public String getTransferRate(){
        if(status==COMPLETE||status==APPENDING) return "0 KB/sec";
        current_time=System.currentTimeMillis();
        double tr=(double)(bytesRead*1000)/(current_time-start_time);
        if(tr<=mb)
            return String.format("%.3f", tr/kb)+" KB/sec";
        else
            return String.format("%.3f", tr/mb)+" MB/sec";
    }
    public String getLocation(){
        return fileLocation;
    }
    public String getFileExtention(){
        return (contentType!=null)?contentType:null;
    }
    public String getResumeCapability(){
        return resumeCapability;
    }
    public void setPause(){
        status=PAUSE;
        bytesRead=0;
        try{
            for(int i=0;i<threadArray.size();++i){
                if(threadArray.get(i).isAlive)
                    threadArray.get(i).mySuspend();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        setChanged();
        notifyObservers(this);
    }
    public void setCancel(){
        //downloaded=0;
        bytesRead=0;
        if(status!=ERROR){
            try{
                for(int i=0;i<threadArray.size();++i){
                    if(threadArray.get(i).isAlive){
                        threadArray.get(i).stop();
                        threadArray.get(i).deleteObservers();
                    }
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        status=CANCEL;
        setChanged();
        notifyObservers(this);
    }
    public void setResume(){
        bytesRead=0;
        if(status==PAUSE){
            try{
                status=DOWNLOADING;
                setChanged();
                notifyObservers(this);
                start_time=System.currentTimeMillis();
                for(int i=0;i<threadArray.size();++i){
                        if(threadArray.get(i).isAlive)
                            threadArray.get(i).myResume();
                    }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        if(status==CANCEL||status==ERROR){
            download();
        }
    }
    public String getFileName(){
        return getFile(url);
    }
    private void setLocation(String type){           //may create problems.....
        String path=getFile(url);
        fileLocation=fileLocation+"\\"+path;
        int index=path.lastIndexOf('.');
        if(index==-1)
            fileLocation=fileLocation+"."+type;
    }
    private String getFile(URL url){
        String filename=null;
        try{
            filename = new URL(URLDecoder.decode(url.toString(), "UTF-8")).getPath();
            filename=filename.substring(filename.lastIndexOf('/')+1);
        }
        catch(Exception e){}
        return filename;
    }
    private void download(){
        HttpURLConnection connection=null;
        try{
            connection = (HttpURLConnection)url.openConnection(proxy);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("HEAD");
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            int rc=connection.getResponseCode();
            if(rc==HttpURLConnection.HTTP_MOVED_TEMP||rc==HttpURLConnection.HTTP_MOVED_PERM||rc==HttpURLConnection.HTTP_SEE_OTHER){
                redirect=true;
                String newUrl=connection.getHeaderField("Location");
                url=new URL(newUrl);
                connection=(HttpURLConnection)url.openConnection(proxy);
                connection.setRequestMethod("HEAD");
                connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            }
            size=connection.getContentLengthLong();
            System.out.println(connection.getHeaderField("Content-Disposition"));
            if(contentType==null){
                contentType=connection.getContentType();
                System.out.println(contentType);
                contentType=contentType.substring(contentType.lastIndexOf('/')+1);
                setLocation(contentType);
            }
            System.out.println(size);
            if((rc/100==2||redirect)&&size!=-1){
                //size=connection.getContentLengthLong();
                threadCount=1;
                if(connection.getHeaderField("Accept-Ranges")!=null){
                    resumeCapability="YES";
                    if(size<=10*mb)
                        threadCount=1;
                    if(size<=20*mb)
                        threadCount=2;
                    else
                    if(size<=100*mb)
                        threadCount=4;
                    else
                    if(size<=48*gb)
                        threadCount=8;
                    else
                    if(size<=96*gb)
                        threadCount=16;
                    else
                        threadCount=32;
                    long bytes=size/threadCount;
                    long start=0,end=bytes+size%threadCount;
                    if(status!=CANCEL||status!=ERROR||threadArray==null){
                        threadArray=new ArrayList<>(threadCount);
                        bytesReceived=new long[threadCount];
                        for(int i=0;i<threadCount;++i){
                            bytesReceived[i]=start;
                            start=end+1;
                            end+=bytes;
                        }
                    }
                    start=0;
                    end=bytes+size%threadCount;
                    status=DOWNLOADING;
                    setChanged();
                    notifyObservers(this);
                    start_time=System.currentTimeMillis();
                    for(int i=0;i<threadCount;++i){
                        threadArray.add(i, new DownloadThread(url,bytesReceived[i],end,bytesReceived[i]-start,tempLocation+"\\"+"temp"+i+".FILE",proxy));
                        threadArray.get(i).addObserver(this);
                        start=end+1;
                        end+=bytes;
                    }    
                }
                else{
                    resumeCapability="NO";
                    status=DOWNLOADING;
                    setChanged();
                    notifyObservers(this);
                    threadCount=1;
                    bytesReceived= new long[threadCount];
                    bytesReceived[0]=0;
                    threadArray=new ArrayList<>(threadCount);
                    start_time=System.currentTimeMillis();
                    threadArray.add(0,new DownloadThread(url,0,size,0,tempLocation+"\\"+"temp0"+".FILE",proxy));
                    threadArray.get(0).addObserver(this);
                }
            }
            else{
                status=ERROR;
                setChanged();
                notifyObservers(this);
                if(size==-1)
                    JOptionPane.showMessageDialog(null, "Error", "Error", JOptionPane.ERROR_MESSAGE);
                else
                    JOptionPane.showMessageDialog(null, "Error "+rc, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch(SocketTimeoutException e){
            status=ERROR;
            setChanged();
            notifyObservers(this);
            JOptionPane.showMessageDialog(null, "Connection Timeout", "Error", JOptionPane.ERROR_MESSAGE);
        }
        catch(IOException | HeadlessException e){
            status=ERROR;
            setChanged();
            notifyObservers(this);
            JOptionPane.showMessageDialog(null, "Error in Connection", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void append(){
        status=APPENDING;
        setChanged();
        notifyObservers();
        RandomAccessFile file=null;
        FileInputStream fin=null;
        String path;
        FileChannel fochan=null,fichan=null;
        try{
            file=new RandomAccessFile(getLocation(),"rwd");
            fochan=file.getChannel();
            MappedByteBuffer buff;
            for(int i=0;i<threadArray.size();++i){
                path=threadArray.get(i).path;
                fin=new FileInputStream(path);
                fichan=fin.getChannel();
                buff=fichan.map(FileChannel.MapMode.READ_ONLY, 0, fichan.size());
                fochan.write(buff);
                try{
                    buff.clear();
                    buff=null;
                    fin.close();
                    fichan.close();
                    System.runFinalization();
                }
                catch(Exception e){}
            }
            status=COMPLETE;
            setChanged();
            notifyObservers(this);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                if(file!=null)
                    file.close();
                if(fin!=null)
                    fin.close();
                if(fochan!=null)
                    fochan.close();
                if(fichan!=null)
                    fichan.close();
                System.gc();
                System.runFinalization();
                for(int i=0;i<threadArray.size();++i){
                    System.runFinalization();
                    path=threadArray.get(i).path;
                    new File(path).delete();
                }
                
            }
            catch(Exception e){}
        }
    }
    @Override
    public void update(Observable o, Object arg) {
        t=(DownloadThread)o;
        switch(t.threadStatus){
            case 0:
                bytesReceived[threadArray.indexOf(o)]+=t.bytesRead;
                totalDownloaded+=t.bytesRead;
                bytesRead=totalDownloaded;
                setChanged();
                notifyObservers(this);
                break;
            case 1:
                countCompleted++;
                if(countCompleted==threadCount)
                    append();
                break;
            case 2:
                status=ERROR;
                setChanged();
                notifyObservers(this);
        }
    }
}
