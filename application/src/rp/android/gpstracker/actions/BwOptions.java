/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package rp.android.gpstracker.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import rp.android.gpstracker.R;
import rp.android.gpstracker.actions.ShareTrack.ShareProgressListener;
import rp.android.gpstracker.actions.tasks.KmzCreator;
import rp.android.gpstracker.actions.tasks.UploadZip;
import rp.android.gpstracker.db.DatabaseHelper;
import rp.android.gpstracker.logger.GPSLoggerServiceManager;
import rp.android.gpstracker.util.Constants;
import rp.android.gpstracker.util.Constants1;
import rp.android.gpstracker.viewer.map.CommonLoggerMap;
import rp.android.gpstracker.xmlgui.RunForm;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Empty Activity that pops up the dialog to add a note to the most current
 * point in the logger service
 * 
 * @version $Id$
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class BwOptions extends Activity
{
   private static final int DIALOG_INSERTNOTE = 27;

   private static final String TAG = "OGT.BwOptions";

   private static final int MENU_PICTURE = 9;
   private static final int MENU_VOICE = 11;
   private static final int MENU_VIDEO = 12;
   private static final int MENU_TRACKS = 13;
   private static final int DIALOG_TEXT = 32;
   private static final int DIALOG_NAME = 33;

   private GPSLoggerServiceManager mLoggerServiceManager;
   private DatabaseHelper mHelper=new DatabaseHelper(BwOptions.this);

   /**
    * Action to take when the LoggerService is bound
    */
   private Runnable mServiceBindAction;

   private boolean paused;
   private Button name;
   private Button text;
   private Button voice;
   private Button picture;
   private Button video;
   private EditText mNoteNameView;
   private EditText mNoteTextView;
   
   private final View.OnClickListener mBwlistener = new View.OnClickListener()
   {
      @Override
      public void onClick( View v )
      {
         int id = v.getId();
         Intent intent = new Intent();
         switch( id )
         {
            case R.id.bw_addpicture:
               addPicture();
               break;
            case R.id.bw_addvideo:
               addVideo();
               break;
            case R.id.bw_addtracks:
               
               addTracks();
               break;
            case R.id.bw_adddata:
               
               addData();
               break;
 case R.id.bw_save:
    SharedPreferences shared = getSharedPreferences(BwStart.PREF_NAME, MODE_PRIVATE);

    String bwid=shared.getString("currentbwid","-1");
               new UploadZip(BwOptions.this,bwid,"http://www.rbsserver.com/bw/upload");
               break;
             default:
               setResult( RESULT_CANCELED, intent );
               break;
         }
        // finish();
         // finish();
         
      }
   };
   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
    //  this.setVisible(false);
      paused = false;
      setContentView(R.layout.bw_options);
      LinearLayout lv = (LinearLayout) findViewById(R.id.bw_name);
      TextView tv =new TextView(this);
      tv.setText(getIntent().getStringExtra("bwname"));
      lv.addView(tv);
      Button b_picture= (Button) findViewById( R.id.bw_addpicture);
      Button b_video= (Button) findViewById( R.id.bw_addvideo );
      Button b_tracks= (Button) findViewById( R.id.bw_addtracks );
      Button b_save= (Button) findViewById( R.id.bw_save );
      Button b_data = (Button) findViewById(R.id.bw_adddata);
      b_picture.setOnClickListener(mBwlistener);
      b_video.setOnClickListener(mBwlistener);
      b_tracks.setOnClickListener(mBwlistener);
      b_data.setOnClickListener(mBwlistener);
      b_save.setOnClickListener(mBwlistener);
     
     // mLoggerServiceManager = new GPSLoggerServiceManager(this);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      
     //mLoggerServiceManager.startup(this, mServiceBindAction);
   }

   @Override
   protected void onPause()
   {
      super.onPause();
     // mLoggerServiceManager.shutdown(this);
      paused = true;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int,
    * android.content.Intent)
    */
   @Override
   protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);
                 if (resultCode != RESULT_CANCELED)
            {
               File file;
               Uri uri;
               File newFile;
               String newName;
               Uri fileUri;
               android.net.Uri.Builder builder;
               boolean isLocal = false;
               SharedPreferences shared = getSharedPreferences(BwStart.PREF_NAME, MODE_PRIVATE);

               String bwid=shared.getString("currentbwid","-1");
               Log.d(TAG,"reached here after saving picture");
               switch (requestCode)
               {
                  case MENU_PICTURE:
                     file = new File(Constants.getSdCardTmpFile(BwOptions.this));
                     file.getParentFile().mkdirs();
                     
                     Calendar c = Calendar.getInstance();
                     newName = String.format("Picture_%tY-%tm-%td_%tH%tM%tS.jpg", c, c, c, c, c, c);
                     String dir = Constants1.getSdCardDirectory(BwOptions.this);
                     String fname=dir+bwid+"/images/"+ newName;
                       fname=    fname.replace(" ", "\\ ");
                     Log.d(TAG,dir);
                     Log.d(TAG,bwid);
                     if (dir!="-1"){
                        if (bwid!="-1"){
                     newFile = new File(fname );
                     File parent = newFile.getParentFile();
                     if(!parent.exists() && !parent.mkdirs()){
                        throw new IllegalStateException("Couldn't create dir: " + parent);
                    }
                     isLocal = file.renameTo(newFile); //
                     if (!isLocal)
                     {
                        Log.w(TAG, "Failed rename will try copy image: " + file.getAbsolutePath());
                        isLocal = copyFile(file, newFile);
                     }
                     if (isLocal)
                     {
                        System.gc();
                        Options opts = new Options();
                        opts.inJustDecodeBounds = true; 
                        Bitmap bm = BitmapFactory.decodeFile(newFile.getAbsolutePath(), opts);
                        String height, width;
                        if (bm != null)
                        {
                           height = Integer.toString(bm.getHeight());
                           width = Integer.toString(bm.getWidth());
                        }
                        else
                        {
                           height = Integer.toString(opts.outHeight);
                           width = Integer.toString(opts.outWidth);
                        }
                        bm = null;
                        builder = new Uri.Builder();
                        fileUri = builder.scheme("file").appendEncodedPath("/").appendEncodedPath(newFile.getAbsolutePath())
                              .appendQueryParameter("width", width).appendQueryParameter("height", height).build();
                      //  BwOptions.this.mLoggerServiceManager.storeMediaUri(fileUri);
                        //add to the database the mediauri //that is all
                     //   dbHelper.insertPhoto(bwid,fileUri.toString(),newName);

                     }
                     else
                     {
                        Log.e(TAG, "Failed either rename or copy image: " + file.getAbsolutePath());
                     }}}
                     else {
                        //external storage not ready;
                     }
                     break;
                  case MENU_VIDEO:
                     Uri contentUri = intent.getData();
                     String[] proj = { MediaStore.Images.Media.DATA };
                     Cursor cursor = managedQuery(contentUri, proj, null, null, null);
                     int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                     cursor.moveToFirst();
                     String tmppath = cursor.getString(column_index);

                     file =new File(tmppath);
                    // file = new File(Constants.getSdCardTmpFile(BwOptions.this));
                     //file.getParentFile().mkdirs();
                     c = Calendar.getInstance();
                     newName = String.format("Video_%tY%tm%td_%tH%tM%tS.3gp", c, c, c, c, c, c);
                     String dir1 = Constants1.getSdCardDirectory(BwOptions.this);
                     String fname1=dir1+bwid+"/videos/"+ newName;
                       fname1=    fname1.replace(" ", "\\ ");
                    
                     newFile = new File(fname1);
                     newFile.getParentFile().mkdirs();
                     isLocal = file.renameTo(newFile);
                     if (!isLocal)
                     {
                        Log.w(TAG, "Failed rename will try copy video: " + file.getAbsolutePath());
                        isLocal = copyFile(file, newFile);
                     }
                     if (isLocal)
                     {
                        builder = new Uri.Builder();
                        fileUri = builder.scheme("file").appendPath(newFile.getAbsolutePath()).build();
                       // BwOptions.this.mLoggerServiceManager.storeMediaUri(fileUri);                        
                     }
                     else
                     {
                        Log.e(TAG, "Failed either rename or copy video: " + file.getAbsolutePath());
                     }
                     break;
                  case MENU_TRACKS:
                     Uri trackuri;
                     c = Calendar.getInstance();
                     trackuri=intent.getData();
                     newName = String.format("Track_%tY-%tm-%td_%tH%tM%tS.jpg", c, c, c, c, c, c);
                     
                     String dir2 = Constants1.getSdCardDirectory(BwOptions.this);
                     String fname2=dir2+bwid+"/tracks/"+ newName;
                       fname2=    fname2.replace(" ", "\\ ");
                       newFile = new File(fname2);
                       newFile.getParentFile().mkdirs();
                       ShareTrack st = new ShareTrack();
                       new KmzCreator(this, trackuri, fname2, st.new ShareProgressListener(fname2)).execute();
                       
                     String x="";
                     break;
                  case MENU_VOICE:
                     uri = Uri.parse(intent.getDataString());
                     //BwOptions.this.mLoggerServiceManager.storeMediaUri(uri);
                     break;
                  default:
                     Log.e(TAG, "Returned form unknow activity: " + requestCode);
                     break;
               }
            }
            else
            {
               Log.w(TAG, "Received unexpected resultcode " + resultCode);
            }
            setResult(resultCode, new Intent());
            //finish();
         
      
   }

   /***
    * 
    */
   private void addTracks()
   {
      Intent intent2= new Intent(getApplicationContext(),CommonLoggerMap.class);
      startActivityForResult(intent2,MENU_TRACKS);
     
   }
  
   /***
    * Collecting additional data
    */
   private void addPicture()
   {
      Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
      File file = new File(Constants.getSdCardTmpFile(this));
           Log.d( TAG, "Picture requested at: " + file );
      i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
      i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1);
      startActivityForResult(i, MENU_PICTURE);
   }

   /***
    * Collecting additional data
    */
   private void addVideo()
   {
      Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
      File file = new File(Constants.getSdCardTmpFile(this));
     // i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
      i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1);

      try
      {
         startActivityForResult(i, MENU_VIDEO);
      }
      catch (ActivityNotFoundException e)
      {
         Log.e(TAG, "Unable to start Activity to record video", e);
      }
   }
 private void addData(){
    Intent in = new Intent(getApplicationContext(),RunForm.class);
    try {
    startActivity(in);
    } catch (ActivityNotFoundException e){
      Toast toast= Toast.makeText(BwOptions.this,"activity not found",3);
       toast.show();
    }
 }
   private void addVoice()
   {
      Intent intent = new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);
      try
      {
         startActivityForResult(intent, MENU_VOICE);
      }
      catch (ActivityNotFoundException e)
      {
         Log.e(TAG, "Unable to start Activity to record audio", e);
      }
   }

   private static boolean copyFile(File fileIn, File fileOut)
   {
      boolean succes = false;
      FileInputStream in = null;
      FileOutputStream out = null;
      try
      {
         in = new FileInputStream(fileIn);
         out = new FileOutputStream(fileOut);
         byte[] buf = new byte[8192];
         int i = 0;
         while ((i = in.read(buf)) != -1)
         {
            out.write(buf, 0, i);
         }
         succes = true;
      }
      catch (IOException e)
      {
         Log.e(TAG, "File copy failed", e);
      }
      finally
      {
         if (in != null)
         {
            try
            {
               in.close();
            }
            catch (IOException e)
            {
               Log.w(TAG, "File close after copy failed", e);
            }
         }
         if (in != null)
         {
            try
            {
               out.close();
            }
            catch (IOException e)
            {
               Log.w(TAG, "File close after copy failed", e);
            }
         }
      }
      return succes;
   }
}
