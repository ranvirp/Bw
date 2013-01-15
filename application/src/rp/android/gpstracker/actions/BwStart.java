package rp.android.gpstracker.actions;
//import rp.android.bwtracker.R;
import java.util.Calendar;

import rp.android.gpstracker.R;
import rp.android.gpstracker.actions.ControlTracking;
import rp.android.gpstracker.actions.NameTrack;
import rp.android.gpstracker.db.DatabaseHelper;
import rp.android.gpstracker.db.GPStracking.Tracks;
import rp.android.gpstracker.logger.GPSLoggerServiceManager;
import rp.android.gpstracker.util.Constants;
import rp.android.gpstracker.viewer.map.CommonLoggerMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Empty Activity that pops up the edit or new naming
 * 
 * @version $Id$
 * @author ranvir (c) Jul 27, 2010, home
 */
public class BwStart extends Activity
{
   private static final int DIALOG_LOGCONTROL = 26;
   String borw="";
  // private GPSLoggerServiceManager mLoggerServiceManager;
   private static final String TAG="BW.BwStart";
   public static final String PREF_NAME = null;
   private final View.OnClickListener mBwlistener = new View.OnClickListener()
      {
         @Override
         public void onClick( View v )
         {
            Log.w(TAG,"reached here");
            int id = v.getId();
            Intent intent = new Intent();
            Spinner sp = (Spinner) findViewById(R.id.borw);
            String Text = sp.getSelectedItem().toString();
            String bwName="";
            
           
            if (Text.equals("लाभार्थी"))
            {
               borw="b";
            } 
            if (Text.equals("कार्य"))
            {
               borw="w";
            } 
            
            SharedPreferences shared = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = shared.edit();
            
            switch( id )
            {
               case R.id.bw_newbt:
                  Calendar c = Calendar.getInstance();
                  bwName = String.format( "BW_%tY-%tm-%td_%tH%tM%tS", c, c, c, c, c ,c);
                  bwName=borw+"_"+bwName;
                  editor.putString("currentbwid",bwName);
                  editor.putString("bwtype",borw);
                  editor.commit();
                  break;
               case R.id.bw_editbt:
                  EditText et = (EditText) findViewById(R.id.bw_id);
                  bwName=borw+"_"+et.getText().toString();
                  editor.putString("currentbwid",bwName);
                  editor.putString("bwtype",borw);
                  editor.commit();
                  break;
                default:
                  setResult( RESULT_CANCELED, intent );
                  break;
            }
            //check if that entry exists in the table if not, create an entry otherwise do not do anything
            Log.w(TAG,bwName);
            editor.commit();
            
            Intent intent2= new Intent(getApplicationContext(),BwOptions.class);
            intent2.putExtra("bwname",bwName);
            startActivity(intent2);
           // finish();
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
      
      setContentView(R.layout.neworedit);
      Button b_new= (Button) findViewById( R.id.bw_newbt );
      Button b_edit= (Button) findViewById( R.id.bw_editbt );
      b_new.setOnClickListener(mBwlistener);
      b_edit.setOnClickListener(mBwlistener);
      b_new.setClickable(true);
      b_edit.setClickable(true);
      DatabaseHelper dbh = new DatabaseHelper(BwStart.this);
      SQLiteDatabase sqldb = dbh.getWritableDatabase();
      sqldb.close();
     
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      
     //setContentView(view);


    
   }

   @Override
   protected void onPause()
   {
      super.onPause();
     
   }

  }
