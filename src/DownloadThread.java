/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shishir
 */
import java.net.*;
import java.io.*;
import java.util.*;
public class DownloadThread extends Observable implements Runnable{

    private static final int MAX_SIZE=2097152;                           //2MB
    private URL url;
    private long min_range;
    private long max_range;
    private boolean suspendFlag;
    private volatile boolean stop;
    public int threadStatus;
    public String path;
    public long bytesRead=0;
    public boolean isAlive;
    private long downloaded=0;
    private Proxy proxy;
    DownloadThread(URL url, long min, long max, long downloaded,String path,Proxy proxy){
        this.url=url;
        this.min_range=min;
        this.max_range=max;
        this.path=path;
        this.downloaded=downloaded;
        this.proxy=proxy;
        suspendFlag=false;
        stop=false;
        isAlive=true;
        threadStatus=0;   //downloading....
        Thread t=new Thread(this);
        t.start();
    }
    void mySuspend(){
        suspendFlag=true;
    }
    void stop(){
        stop=true;
    }
    synchronized void myResume(){
        suspendFlag=false;
        notify(); 
    }
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        HttpURLConnection conn=null;
        InputStream stream=null;
        RandomAccessFile file=null;
        byte buffer[];
        try{
            conn =(HttpURLConnection)url.openConnection(proxy);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            conn.setRequestProperty("Range", "bytes="+min_range+"-"+max_range);
            conn.connect();
            stream=conn.getInputStream();
            file=new RandomAccessFile(path,"rwd");
            file.seek(downloaded);
            buffer=new byte[MAX_SIZE];
            while(!stop){
                int read=stream.read(buffer);
                if(read==-1){
                    threadStatus=1;  //completed.....
                    isAlive=false;
                    //setChanged();
                    //notifyObservers(this);
                    break;
                }
                bytesRead=read;
                setChanged();
                notifyObservers(this);
                file.write(buffer, 0, read);
                Thread.sleep(50);
                synchronized(this){
                    while(suspendFlag){
                        wait();
                    }
                }
            }
        }
        catch(IOException | InterruptedException e){
            threadStatus=2;   //error
            isAlive=false;
            setChanged();
            notifyObservers(this);
            e.printStackTrace();
        }
        finally{
            try{
                buffer=null;
                if(stream!=null)
                    stream.close();
                if(file!=null)
                    file.close();
                System.gc();
                System.runFinalization();
                setChanged();
                notifyObservers(this);
            }
            catch(IOException e){}
        }
    }
    
}
