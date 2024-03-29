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

import java.util.Calendar;

import rp.android.gpstracker.R;
import rp.android.gpstracker.db.GPStracking.Tracks;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Empty Activity that pops up the dialog to name the track
 *
 * @version $Id$
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class NameTrack extends Activity
{
   private static final int DIALOG_TRACKNAME = 23;

   protected static final String TAG = "OGT.NameTrack";

   private EditText mTrackNameView;
   private boolean paused;
   Uri mTrackUri;

   private final DialogInterface.OnClickListener mTrackNameDialogListener = new DialogInterface.OnClickListener()
   {
      @Override
      public void onClick( DialogInterface dialog, int which )
      {
         String trackName = null;
         switch( which )
         {
            case DialogInterface.BUTTON_POSITIVE:
               trackName = mTrackNameView.getText().toString();        
               ContentValues values = new ContentValues();
               values.put( Tracks.NAME, trackName );
               getContentResolver().update( mTrackUri, values, null, null );
               clearNotification();
               break;
            case DialogInterface.BUTTON_NEUTRAL:
               startDelayNotification();
               break;
            case DialogInterface.BUTTON_NEGATIVE:
               clearNotification();
               break;
            default:
               Log.e( TAG, "Unknown option ending dialog:"+which );
               break;
         }
         finish();
      }


   };
   
   
   private void clearNotification()
   {

      NotificationManager noticationManager = (NotificationManager) this.getSystemService( Context.NOTIFICATION_SERVICE );;
      noticationManager.cancel( R.layout.namedialog );
   }
   
   private void startDelayNotification()
   {
      int resId = R.string.dialog_routename_title;
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString( resId );
      long when = System.currentTimeMillis();
      
      Notification nameNotification = new Notification( icon, tickerText, when );
      nameNotification.flags |= Notification.FLAG_AUTO_CANCEL;
      
      CharSequence contentTitle = getResources().getString( R.string.app_name );
      CharSequence contentText = getResources().getString( resId );
      
      Intent notificationIntent = new Intent( this, NameTrack.class );
      notificationIntent.setData( mTrackUri );
      
      PendingIntent contentIntent = PendingIntent.getActivity( this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK );
      nameNotification.setLatestEventInfo( this, contentTitle, contentText, contentIntent );
      
      NotificationManager noticationManager = (NotificationManager) this.getSystemService( Context.NOTIFICATION_SERVICE );
      noticationManager.notify( R.layout.namedialog, nameNotification );
   }
   
   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      this.setVisible( false );
      paused = false;
      mTrackUri = this.getIntent().getData();
   }
   
   @Override
   protected void onPause()
   {
      super.onPause();
      paused = true;
   }
   
   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      if(  mTrackUri != null )
      {
         showDialog( DIALOG_TRACKNAME );
      }
      else
      {
         Log.e(TAG, "Naming track without a track URI supplied." );
         finish();
      }
   }
   
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKNAME:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.namedialog, null );
            mTrackNameView = (EditText) view.findViewById( R.id.nameField );
            builder
               .setTitle( R.string.dialog_routename_title )
               .setMessage( R.string.dialog_routename_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_okay, mTrackNameDialogListener )
               .setNeutralButton( R.string.btn_skip, mTrackNameDialogListener )
               .setNegativeButton( R.string.btn_cancel, mTrackNameDialogListener )
               .setView( view );
            dialog = builder.create();
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
   
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      switch (id)
      {
         case DIALOG_TRACKNAME:
            String trackName;
            Calendar c = Calendar.getInstance();
            trackName = String.format( getString( R.string.dialog_routename_default ), c, c, c, c, c );
            mTrackNameView.setText( trackName );
            mTrackNameView.setSelection( 0, trackName.length() );
            break;
         default:
            super.onPrepareDialog( id, dialog );
            break;
      }
   }   
}
   