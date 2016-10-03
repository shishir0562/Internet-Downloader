/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author shishir
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class Database{
    private static Connection conn=null;
    private static Statement stmt=null;
    private static ResultSet rs=null;
    String sql=null;
    Database(){}
    public static void openConnection(){
        try{
            Class.forName("org.sqlite.JDBC");
            conn=DriverManager.getConnection("jdbc:sqlite:‪‪DATABASE.db");
            stmt=conn.createStatement();
        }
        catch(ClassNotFoundException | SQLException e){
            e.printStackTrace();
        }
    }
    public void insertRow(int row,String []data){
        String srow=String.valueOf(row);
        sql="INSERT INTO DATABASE (SR_NO,FILE_NAME,SIZE,URL,TEMP_LOCATION,FILE_LOCATION,TOTAL_DOWNLOADED,RESUME_CAPABILITY,FILE_TYPE,STATUS)"
                + "VALUES('"+srow+"','"+data[0]+"','"+data[1]+"','"+data[2]+"','"+data[3]+"','"+data[4]+"','"+data[5]+"','"+data[6]+"','"+data[7]+"','"+data[8]+"');";
        try{
            stmt.executeUpdate(sql);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public void deleteRow(int row){
        String srow=String.valueOf(row);
        sql="DELETE FROM DATABASE WHERE SR_NO="+srow+";";
        try{
            stmt.executeUpdate(sql);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public void deleteTable(){
        sql="DELETE TABLE DATABASE;";
        try{
            stmt.executeUpdate(sql);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public void updateRow(int row,String colName){
        String srow=String.valueOf(row);
        sql="UPDATE DATABASE SET"+colName+"=";
    }
    public String[] getRowdata(int row){
        String []data=null;
        return data;
    }
    public void closeConnection(){
        try{
            stmt.close();
            conn.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String ab[]){
        openConnection();
    }
}
