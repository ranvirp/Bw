package rp.android.gpstracker.db;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log; 	 

public class DBAccess {
    public class Row extends Object {
    	public long rowId;
    	public String remoteId;
    	public Hashtable<String, Integer> spinners = new Hashtable<String, Integer>();
        public String gpslat;
        public String gpslon;
        public String gpsalt;
        public String gpsacc;
        public String photoid;
        public String ecdate;
        public String stored;
        public Hashtable<String, String> datastrings = new Hashtable<String, String>();
        public Hashtable<String, Boolean> checkboxes = new Hashtable<String, Boolean>();
        public Hashtable<String, Integer> rgroups = new Hashtable<String, Integer>();
        public boolean remote = false;
    }

    private static final String TAG = "Bw_Table";
    private static final String DATABASE_NAME = "bw_info";
    private static final int DATABASE_VERSION = 3;
    private final Context mCtx;
    private SQLiteDatabase db;
    
    /**
     * This class helps open and create the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
        	super(context, DATABASE_NAME, null, DATABASE_VERSION);            
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	
        	db.execSQL("create table bws (id text primary key, active text,downloaded varchar(1));");
        	db.execSQL("create table photos (id int autoincrement not null primary key,bwid text,gpslat text,gpslon text,uri text);");
        	db.execSQL("create table videos (id int autoincrement not null primary key,bwid text,gpslat text,gpslon text,gpsalt text,gpsacc text,uri text);");
        	db.execSQL("create table tracks (id int autoincrement not null primary key,bwid text,uri text);");
        	db.execSQL("create table tags (id int autoincrement not null primary key,objname varchar(20),objid int,tagtext text);");
         
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	
        	
        	db.execSQL("DROP TABLE IF EXISTS photos");
        	db.execSQL("DROP TABLE IF EXISTS videos");
        	db.execSQL("DROP TABLE IF EXISTS tracks");
        	db.execSQL("DROP TABLE IF EXISTS bws");
         db.execSQL("DROP TABLE IF EXISTS tags");
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
           // db.execSQL("DROP TABLE IF EXISTS data");
            onCreate(db);
        }
    }
    private class Tag
    {
       public String id;
       public String objname;
       public int objid;
       public String tagtext;
       
    };
    private class Bw 
    {
       public String id;
       public String downloaded;
       
    };
    private class Photo 
    {
       public int id;
       public String bwid;
       public String gpslat;
       public String gpslon;
       public String gpsalt;
       public String gpsacc;
       public String uri;
    };
    private class Video 
    {
       public int id;
       public String bwid;
       public String gpslat;
       public String gpslon;
       public String gpsalt;
       public String gpsacc;
       public String uri;
    };
    private class Track 
    {
       public int id;
       public String bwid;
       
       public String uri;
    };
    private DatabaseHelper mOpenHelper;
   
    public DBAccess (Context ctx) {
        this.mCtx = ctx;
    }	
    
    public DBAccess open() throws SQLException {
    	mOpenHelper = new DatabaseHelper(mCtx);
    	db = mOpenHelper.getWritableDatabase();
    	return this;
    }

    public void close() {
    	mOpenHelper.close();
    }
    public void addPhoto(String bwid,String uri,String gpslat,String gpslong)
    {
       
    }
    
    

    public void dropTable(String table){
    	db.execSQL("drop table if exists "+table);
    }
   
   
    public void createRow(String table,Hashtable<String, String> values) {
    	
    	ContentValues initialValues = new ContentValues();
    	
    	for(String key : values.keySet()){
    		initialValues.put(key, values.get(key));
    	}
        
        //initialValues.put("stored", "N");
    
        db.replace(table, null, initialValues); // replace
       
    }


    public void deleteRow(String table,String rowId) {
    	
 
        db.delete(table, "id=" + rowId, null);
    }
    
    

       
public List<Photo> fetchAllPhotos(String bwid) {
    	
    
    	
    	Cursor c = db.rawQuery("select * from "+"photos"+" where bwid = "+bwid, null);
    	
        ArrayList<Photo> ret = new ArrayList<Photo>();
     
        try {
            int numRows = c.getCount();
            c.moveToFirst();
            for (int i = 0; i < numRows; ++i) {
            	//Log.i("DBACCESS: ", "Fetch all rows loop "+i);
            	Photo photo = getPhoto(c);
               
                ret.add(photo); 
                c.moveToNext();
            }
        } catch (SQLException e) {
            Log.e("booga", e.toString());
        }
        c.close();
        return ret;
    }
        
    private Photo getPhoto(Cursor c){
    	//Cursor c = thisc;
		Photo photo = new Photo();

        photo.id = c.getInt(0);
        photo.bwid = c.getString(1);
        photo.gpslat = c.getString(2);
        photo.gpslon = c.getString(3);
        photo.gpsalt = c.getString(4);
        photo.gpsacc = c.getString(5);
        photo.uri = c.getString(6);
           return photo;
	}
    public List<Video> fetchAllVideos(String bwid) {
       
       
       
       Cursor c = db.rawQuery("select * from "+"videos"+" where bwid = "+bwid, null);
       
         ArrayList<Video> ret = new ArrayList<Video>();
      
         try {
             int numRows = c.getCount();
             c.moveToFirst();
             for (int i = 0; i < numRows; ++i) {
                //Log.i("DBACCESS: ", "Fetch all rows loop "+i);
                Video video = getVideo(c);
                
                 ret.add(video); 
                 c.moveToNext();
             }
         } catch (SQLException e) {
             Log.e("booga", e.toString());
         }
         c.close();
         return ret;
     }
         
     private Video getVideo(Cursor c){
       //Cursor c = thisc;
       Video video = new Video();

         video.id = c.getInt(0);
         video.bwid = c.getString(1);
         video.gpslat = c.getString(2);
         video.gpslon = c.getString(3);
         video.gpsalt = c.getString(4);
         video.gpsacc = c.getString(5);
         video.uri = c.getString(6);
            return video;
    }
     public List<Track> fetchAllTracks(String bwid) {
        
        
        
        Cursor c = db.rawQuery("select * from "+"tracks"+" where bwid = "+bwid, null);
        
          ArrayList<Track> ret = new ArrayList<Track>();
       
          try {
              int numRows = c.getCount();
              c.moveToFirst();
              for (int i = 0; i < numRows; ++i) {
                 //Log.i("DBACCESS: ", "Fetch all rows loop "+i);
                 Track track = getTrack(c);
                 
                  ret.add(track); 
                  c.moveToNext();
              }
          } catch (SQLException e) {
              Log.e("booga", e.toString());
          }
          c.close();
          return ret;
      }
          
      private Track getTrack(Cursor c){
       
        Track track = new Track();

          track.id = c.getInt(0);
          track.bwid = c.getString(1);
          track.uri = c.getString(2);
             return track;
     }

    
    
    public void updateBw(String bwid,String downloaded){
    	
    	ContentValues args = new ContentValues();
    	args.put("downloaded", downloaded);
       
        db.update("bws", args, "bwid=" + bwid, null);
    }
    public void addTags(String objname,int objid,String tagtext){
       
       ContentValues args = new ContentValues();
       args.put("objname", objname);
       args.put("objid", objid);
       args.put("tagtext", tagtext);
        
         db.replace("tags", null,args);
     }

   
	
	
	
}
