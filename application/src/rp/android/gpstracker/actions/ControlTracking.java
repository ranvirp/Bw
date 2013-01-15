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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import rp.android.gpstracker.R;
import rp.android.gpstracker.actions.ShareTrack.ShareProgressListener;
import rp.android.gpstracker.actions.tasks.KmzCreator;
import rp.android.gpstracker.db.GPStracking.Tracks;
import rp.android.gpstracker.logger.GPSLoggerService;
import rp.android.gpstracker.logger.GPSLoggerServiceManager;
import rp.android.gpstracker.util.Constants;
import rp.android.gpstracker.util.Constants1;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Empty Activity that pops up the dialog to name the track
 * 
 * @version $Id$
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class ControlTracking extends Activity
{
   private static final int DIALOG_LOGCONTROL = 26;
   private static final int TRACK_STOP=21;//updated by ranvir
   private static final String TAG = "OGT.ControlTracking";

   private GPSLoggerServiceManager mLoggerServiceManager;
   private Button start;
   private Button pause;
   private Button resume;
   private Button stop;
   private boolean paused;

   private final View.OnClickListener mLoggingControlListener = new View.OnClickListener()
      {
         @Override
         public void onClick( View v )
         {
            int id = v.getId();
            Intent intent = new Intent();
            switch( id )
            {
               case R.id.logcontrol_start:
                  long loggerTrackId = mLoggerServiceManager.startGPSLogging( null );
                  
                  // Start a naming of the track
                  Intent namingIntent = new Intent( ControlTracking.this, NameTrack.class );
                  namingIntent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, loggerTrackId ) );
                  startActivity( namingIntent );
                  
                  // Create data for the caller that a new track has been started
                  ComponentName caller = ControlTracking.this.getCallingActivity();
                  if( caller != null )
                  {
                     intent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, loggerTrackId ) );
                     setResult( RESULT_OK, intent );
                  }                  
                  break;
               case R.id.logcontrol_pause:
                  mLoggerServiceManager.pauseGPSLogging();
                  setResult( RESULT_OK, intent );
                  break;
               case R.id.logcontrol_resume:
                  mLoggerServiceManager.resumeGPSLogging();
                  setResult( RESULT_OK, intent );
                  break;
               case R.id.logcontrol_stop:
                  mLoggerServiceManager.stopGPSLogging();
                  SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ControlTracking.this);
                  long mTrackId=preferences.getLong("SERVICESTATE_TRACKID",-1);
                  Uri trackuri = Uri.withAppendedPath(Tracks.CONTENT_URI, String.valueOf(mTrackId));
                  
                  SharedPreferences shared = ControlTracking.this.getSharedPreferences(BwStart.PREF_NAME, Activity.MODE_PRIVATE);

                  String bwid=shared.getString("currentbwid","-1");
                  Calendar c = Calendar.getInstance();
                  String newName = String.format("Track_%tY-%tm-%td_%tH%tM%tS.kmz", c, c, c, c, c, c);
                  
                  String dir2 = Constants1.getSdCardDirectory(ControlTracking.this);
                  String fname2=dir2+bwid+"/tracks/"+ newName;
                    fname2=    fname2.replace(" ", "\\ ");
                    File newFile = new File(fname2);
                    newFile.getParentFile().mkdirs();
                    ShareTrack st = new ShareTrack();
                   // new KmzCreator(ControlTracking.this, trackuri, fname2, st.new ShareProgressListener(fname2)).execute();
                    KmzCreator kz = new KmzCreator(ControlTracking.this, trackuri, newName, null);
                   
                  //  kz.setExportDirectoryPath(dir2);
                    try
                  {
                       
                     Uri uri=kz.execute().get();
                     File f= new File(new URI(uri.toString()));
                     Boolean bl=f.renameTo(newFile);
                     if (!bl){
                      bl=  InsertNote.copyFile(f, newFile);
                     }
                     String s="";
                  }
                  catch (InterruptedException e)
                  {
                     e.printStackTrace();
                  }
                  catch (ExecutionException e)
                  {
                     e.printStackTrace();
                  }
                  catch (URISyntaxException e)
                  {
                     e.printStackTrace();
                  }

                  setResult( TRACK_STOP, intent );
                
                  break;
               default:
                  setResult( RESULT_CANCELED, intent );
                  break;
            }
            finish();
         }
      };
   private OnClickListener mDialogClickListener = new OnClickListener()
      {
         @Override
         public void onClick( DialogInterface dialog, int which )
         {
            setResult( RESULT_CANCELED,  new Intent() );
            finish();
         }
      };

   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      
      this.setVisible( false );
      paused = false;
      mLoggerServiceManager = new GPSLoggerServiceManager( this );
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      mLoggerServiceManager.startup( this, new Runnable()
         {
            @Override
            public void run()
            {
               showDialog( DIALOG_LOGCONTROL );
            }
         } );
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      mLoggerServiceManager.shutdown( this );
      paused = true;
   }

   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch( id )
      {
         case DIALOG_LOGCONTROL:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.logcontrol, null );
            builder.setTitle( R.string.dialog_tracking_title ).
            setIcon( android.R.drawable.ic_dialog_alert ).
            setNegativeButton( R.string.btn_cancel, mDialogClickListener ).
            setView( view );
            dialog = builder.create();
            start = (Button) view.findViewById( R.id.logcontrol_start );
            pause = (Button) view.findViewById( R.id.logcontrol_pause );
            resume = (Button) view.findViewById( R.id.logcontrol_resume );
            stop = (Button) view.findViewById( R.id.logcontrol_stop );
            start.setOnClickListener( mLoggingControlListener );
            pause.setOnClickListener( mLoggingControlListener );
            resume.setOnClickListener( mLoggingControlListener );
            stop.setOnClickListener( mLoggingControlListener );
            dialog.setOnDismissListener( new OnDismissListener()
               {
                  @Override
                  public void onDismiss( DialogInterface dialog )
                  {
                     if( !paused )
                     {
                        finish();
                     }
                  }
               });
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      switch( id )
      {
         case DIALOG_LOGCONTROL:
            updateDialogState( mLoggerServiceManager.getLoggingState() );
            break;
         default:
            break;
      }
      super.onPrepareDialog( id, dialog );
   }
   
    

   private void updateDialogState( int state )
   {
      switch( state )
      {
         case Constants.STOPPED:
            start.setEnabled( true );
            pause.setEnabled( false );
            resume.setEnabled( false );
            stop.setEnabled( false );
            break;
         case Constants.LOGGING:
            start.setEnabled( false );
            pause.setEnabled( true );
            resume.setEnabled( false );
            stop.setEnabled( true );
            break;
         case Constants.PAUSED:
            start.setEnabled( false );
            pause.setEnabled( false );
            resume.setEnabled( true );
            stop.setEnabled( true );
            break;
         default:
            Log.w( TAG, String.format( "State %d of logging, enabling and hope for the best....", state ) );
            start.setEnabled( false );
            pause.setEnabled( false );
            resume.setEnabled( false );
            stop.setEnabled( false );
            break;
      }
   }
}
