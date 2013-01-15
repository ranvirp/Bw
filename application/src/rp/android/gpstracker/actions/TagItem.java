package rp.android.gpstracker.actions;
//import rp.android.bwtracker.R;
import java.io.InputStream;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import rp.android.gpstracker.R;
import rp.android.gpstracker.actions.ControlTracking;
import rp.android.gpstracker.actions.NameTrack;
import rp.android.gpstracker.db.DBAccess;
import rp.android.gpstracker.db.DatabaseHelper;
import rp.android.gpstracker.db.GPStracking.Tracks;
import rp.android.gpstracker.logger.GPSLoggerServiceManager;
import rp.android.gpstracker.util.Constants;
import rp.android.gpstracker.viewer.map.CommonLoggerMap;
import rp.android.gpstracker.xmlgui.RunForm;
import rp.android.gpstracker.xmlgui.SimpleMap;
import rp.android.gpstracker.xmlgui.XmlGuiForm;
import rp.android.gpstracker.xmlgui.XmlGuiFormField;
import rp.android.gpstracker.xmlgui.XmlGuiPickOne;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.AssetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

/**
 * Empty Activity that pops up the edit or new naming
 * 
 * @version $Id$
 * @author ranvir (c) Jul 27, 2010, home
 */
public class TagItem extends Activity
{
   
   String borw="";
  // private GPSLoggerServiceManager mLoggerServiceManager;
   private static final String TAG="BW.BwStart";
   public static final String PREF_NAME = null;
   String mitemType;
   XmlGuiForm theForm = new XmlGuiForm();
   private final View.OnClickListener mBwlistener = new View.OnClickListener()
      {
         @Override
         public void onClick( View v )
         {
            
            int id = v.getId();
            Intent intent = new Intent();
             String tagText=  theForm.fields.elementAt(0).getData().toString();
             String tablename =mitemType+"s";
             DBAccess db=new DBAccess(TagItem.this);
             
            SharedPreferences shared = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = shared.edit();
            
               // finish();
         }
      };
   

   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      String itemType=this.getIntent().getStringExtra("itemType");
      mitemType=itemType;
      if (itemType.equals("photo")) {
      setContentView(R.layout.tagphoto);
      } else if (itemType.equals("video")) {
         setContentView(R.layout.tagvideo);
         } else {
           // setContentView(R.layout.tagTrack);
         }
      if (readForm()) {
         LinearLayout linearLayout = (LinearLayout)findViewById(R.id.layout1);
         View view = (View) new XmlGuiPickOne(this,(theForm.fields.elementAt(0).isRequired() ? "*" : "") + theForm.fields.elementAt(0).getLabel(),theForm.fields.elementAt(0).getOptions());
         linearLayout.addView(view);
         Button bt=new Button(TagItem.this);
         bt.setText("Save");
         bt.setOnClickListener(mBwlistener);
      }
      //get form and add to the view 
           
   }
 private Boolean readForm()
 {
    try {
    AssetManager assetManager = getAssets();
    //InputStream is = url.openConnection().getInputStream();
    SimpleMap sm = new SimpleMap();
    SharedPreferences shared = TagItem.this.getSharedPreferences(BwStart.PREF_NAME, Activity.MODE_PRIVATE);
    String itemType = this.getIntent().getStringExtra("itemType");
    String bwType = shared.getString("bwtype","b");
    InputStream is = assetManager.open(bwType+"_"+itemType+"_tags.xml");
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = factory.newDocumentBuilder();
    Document dom = db.parse(is);
    Element root = dom.getDocumentElement();
    NodeList tags = root.getElementsByTagName("tags");
    if (tags.getLength() < 1) {
       // nothing here??
       //Log.e(tag,"No form, let's bail");
       return false;
    }
    Node tag = tags.item(0);
   
    
    // process form level
    NamedNodeMap map = tag.getAttributes();
  
    // now process the fields
    XmlGuiFormField tempField =  new XmlGuiFormField();
    tempField.setName(map.getNamedItem("category").getNodeValue());
    tempField.setLabel(map.getNamedItem("label").getNodeValue());
    tempField.setType("choice");
    tempField.setRequired(true);
    NodeList indtags = root.getElementsByTagName("tag");
    String options="";
    for (int i=0;i<indtags.getLength();i++) {
       Node fieldNode = indtags.item(i);
       NamedNodeMap attr = fieldNode.getAttributes();
       options =options+attr.getNamedItem("tagtext").getNodeValue();
       if (i!=indtags.getLength()-1) {
            options =options+"|";
       }
    }
    tempField.setOptions(options);
    theForm.getFields().add(tempField);
    
    return true;
 } catch (Exception e) {
    //Log.e(tag,"Error occurred in ProcessForm:" + e.getMessage());
    e.printStackTrace();
    return false;
 }

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
