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

import java.util.List;

import rp.android.gpstracker.R;
import rp.android.gpstracker.breadcrumbs.BreadcrumbsService;
import rp.android.gpstracker.breadcrumbs.BreadcrumbsService.LocalBinder;
import rp.android.gpstracker.breadcrumbs.BreadcrumbsTracks;
import rp.android.gpstracker.db.GPStracking.MetaData;
import rp.android.gpstracker.util.Constants;
import rp.android.gpstracker.util.Pair;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * Empty Activity that pops up the dialog to describe the track
 * 
 * @version $Id: NameTrack.java 888 2011-03-14 19:44:44Z rcgroot@gmail.com $
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class DescribeTrack extends Activity
{
   private static final int DIALOG_TRACKDESCRIPTION = 42;

   protected static final String TAG = "OGT.DescribeTrack";

   private static final String ACTIVITY_ID = "ACTIVITY_ID";

   private static final String BUNDLE_ID = "BUNDLE_ID";

   private Spinner mActivitySpinner;
   private Spinner mBundleSpinner;
   private EditText mDescriptionText;
   private CheckBox mPublicCheck;
   private Button mOkayButton;
   private boolean paused;
   private Uri mTrackUri;
   private ProgressBar mProgressSpinner;

   private AlertDialog mDialog;
   private BreadcrumbsService mService;
   boolean mBound = false;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      this.setVisible(false);
      paused = false;

      mTrackUri = this.getIntent().getData();
      Intent service = new Intent(this, BreadcrumbsService.class);
      startService(service);
   }

   @Override
   protected void onStart()
   {
      super.onStart();
      Intent intent = new Intent(this, BreadcrumbsService.class);
      bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      if (mTrackUri != null)
      {
         showDialog(DIALOG_TRACKDESCRIPTION);
      }
      else
      {
         Log.e(TAG, "Describing track without a track URI supplied.");
         finish();
      }
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      paused = true;
   }

   @Override
   protected void onStop()
   {
      if (mBound)
      {
         unbindService(mConnection);
         mBound = false;
         mService = null;
      }
      super.onStop();
   }

   @Override
   protected void onDestroy()
   {
      if (isFinishing())
      {
         Intent service = new Intent(this, BreadcrumbsService.class);
         stopService(service);
      }
      super.onDestroy();
   }

   @Override
   protected Dialog onCreateDialog(int id)
   {
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.describedialog, null);
            mActivitySpinner = (Spinner) view.findViewById(R.id.activity);
            mBundleSpinner = (Spinner) view.findViewById(R.id.bundle);
            mDescriptionText = (EditText) view.findViewById(R.id.description);
            mPublicCheck = (CheckBox) view.findViewById(R.id.public_checkbox);
            mProgressSpinner = (ProgressBar) view.findViewById(R.id.progressSpinner);
            builder.setTitle(R.string.dialog_description_title).setMessage(R.string.dialog_description_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_okay, mTrackDescriptionDialogListener).setNegativeButton(R.string.btn_cancel, mTrackDescriptionDialogListener).setView(view);
            mDialog = builder.create();
            setUiEnabled();

            mDialog.setOnDismissListener(new OnDismissListener()
               {
                  @Override
                  public void onDismiss(DialogInterface dialog)
                  {
                     if (!paused)
                     {
                        finish();
                     }
                  }
               });
            return mDialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            setUiEnabled();
            connectBreadcrumbs();
            break;
         default:
            super.onPrepareDialog(id, dialog);
            break;
      }
   }

   private void connectBreadcrumbs()
   {
      if (mService != null && !mService.isAuthorized())
      {
         mService.collectBreadcrumbsOauthToken();
      }
   }

   private void saveBreadcrumbsPreference(int activityPosition, int bundlePosition)
   {
      Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
      editor.putInt(ACTIVITY_ID, activityPosition);
      editor.putInt(BUNDLE_ID, bundlePosition);
      editor.commit();
   }

   private void loadBreadcrumbsPreference()
   {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

      int activityPos = prefs.getInt(ACTIVITY_ID, 0);
      activityPos = activityPos < mActivitySpinner.getCount() ? activityPos : 0;
      mActivitySpinner.setSelection(activityPos);

      int bundlePos = prefs.getInt(BUNDLE_ID, 0);
      bundlePos = bundlePos < mBundleSpinner.getCount() ? bundlePos : 0;
      mBundleSpinner.setSelection(bundlePos);
   }

   private ContentValues buildContentValues(String key, String value)
   {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MetaData.KEY, key);
      contentValues.put(MetaData.VALUE, value);
      return contentValues;
   }

   private void setUiEnabled()
   {
      boolean enabled = mService != null && mService.isAuthorized();
      if (mProgressSpinner != null)
      {
         if (enabled)
         {
            mProgressSpinner.setVisibility(View.GONE);
         }
         else
         {
            mProgressSpinner.setVisibility(View.VISIBLE);
         }
      }

      if (mDialog != null)
      {
         mOkayButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
      }
      for (View view : new View[] { mActivitySpinner, mBundleSpinner, mDescriptionText, mPublicCheck, mOkayButton })
      {
         if (view != null)
         {
            view.setEnabled(enabled);
         }
      }
      if (enabled)
      {
         mActivitySpinner.setAdapter(getActivityAdapter());
         mBundleSpinner.setAdapter(getBundleAdapter());
         loadBreadcrumbsPreference();
      }
   }

   public SpinnerAdapter getActivityAdapter()
   {
      List<Pair<Integer, Integer>> activities = mService.getActivityList();
      ArrayAdapter<Pair<Integer, Integer>> adapter = new ArrayAdapter<Pair<Integer, Integer>>(this, android.R.layout.simple_spinner_item, activities);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   public SpinnerAdapter getBundleAdapter()
   {
      List<Pair<Integer, Integer>> bundles = mService.getBundleList();
      ArrayAdapter<Pair<Integer, Integer>> adapter = new ArrayAdapter<Pair<Integer, Integer>>(this, android.R.layout.simple_spinner_item, bundles);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      return adapter;
   }

   private ServiceConnection mConnection = new ServiceConnection()
      {
         @Override
         public void onServiceConnected(ComponentName className, IBinder service)
         {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            setUiEnabled();
         }

         @Override
         public void onServiceDisconnected(ComponentName arg0)
         {
            mService = null;
            mBound = false;
         }
      };

   private final DialogInterface.OnClickListener mTrackDescriptionDialogListener = new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(DialogInterface dialog, int which)
         {
            switch (which)
            {
               case DialogInterface.BUTTON_POSITIVE:
                  Uri metadataUri = Uri.withAppendedPath(mTrackUri, "metadata");
                  Integer activityId = ((Pair<Integer, Integer>)mActivitySpinner.getSelectedItem()).second;
                  Integer bundleId = ((Pair<Integer, Integer>)mBundleSpinner.getSelectedItem()).second;
                  saveBreadcrumbsPreference(mActivitySpinner.getSelectedItemPosition(), mBundleSpinner.getSelectedItemPosition());
                  String description = mDescriptionText.getText().toString();
                  String isPublic = Boolean.toString(mPublicCheck.isChecked());
                  ContentValues[] metaValues = { buildContentValues(BreadcrumbsTracks.ACTIVITY_ID, activityId.toString()), buildContentValues(BreadcrumbsTracks.BUNDLE_ID, bundleId.toString()),
                        buildContentValues(BreadcrumbsTracks.DESCRIPTION, description), buildContentValues(BreadcrumbsTracks.ISPUBLIC, isPublic), };
                  getContentResolver().bulkInsert(metadataUri, metaValues);
                  Intent data = new Intent();
                  data.setData(mTrackUri);
                  if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.NAME))
                  {
                     data.putExtra(Constants.NAME, getIntent().getExtras().getString(Constants.NAME));
                  }
                  setResult(RESULT_OK, data);
                  break;
               case DialogInterface.BUTTON_NEGATIVE:
                  break;
               default:
                  Log.e(TAG, "Unknown option ending dialog:" + which);
                  break;
            }
            finish();
         }
      };
}
