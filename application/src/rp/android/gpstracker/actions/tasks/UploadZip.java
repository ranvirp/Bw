package rp.android.gpstracker.actions.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.HttpVersion;
import org.apache.ogt.http.client.ClientProtocolException;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.FileBody;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.params.CoreProtocolPNames;

import rp.android.gpstracker.actions.utils.ZipUtility;
import rp.android.gpstracker.util.Constants1;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

public class UploadZip extends AsyncTask<Void, Integer, String>
{
   String bwId;
   Context ctx;
   String url="";
   String uploadUrl;
   public UploadZip(Context ctx,String bwid,String uploadUrl)
   {
      bwId=bwid;
      this.ctx = ctx;
      this.uploadUrl=uploadUrl;
   }
   protected String doInBackground(Void... params)
   {
    //  android.os.Debug.waitForDebugger();//for debugging
     File f= createZipFile(bwId);

      HttpResponse res = uploadZipFile(uploadUrl,f);
      return "1";
   }
   protected void onPostExecute(Uri uri)
   {
      
   }
   HttpResponse uploadZipFile(String uploadUrl,File zipFile){
      try
      {
         URL url = new URL(uploadUrl);
         
      }
      catch (MalformedURLException e1)
      {
         e1.printStackTrace();
      }
      FileBody bin = new FileBody(zipFile);
      MultipartEntity entity = new MultipartEntity();
      try
      {
         entity.addPart("message", new StringBody(bwId));
      }
      catch (UnsupportedEncodingException e)
      {
         e.printStackTrace();
      }
      entity.addPart("file", bin);
      HttpClient httpclient = new DefaultHttpClient();
      httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
      HttpPost httppost = new HttpPost(uploadUrl);
      httppost.setEntity(entity);
      try
      {
         HttpResponse response = httpclient.execute(httppost);
         return response;
      }
      catch (ClientProtocolException e)
      {
         e.printStackTrace();
         return null;
      }
      catch (IOException e)
      {
         e.printStackTrace();
         return null;
      }
      
   }
 File createZipFile(String bwid)
 {
    String zipFilePath;
   
       zipFilePath = Constants1.getSdCardDirectory(ctx) + "zips"+"/"+bwid+".zip";
       File f = new File(zipFilePath);
       f.getParentFile().mkdirs();
       String exportableDir=Constants1.getSdCardDirectory(ctx)+bwid;
   
        try
      {
         ZipUtility.zipDirectory(new File(exportableDir),f);
      }
      catch (IOException e)
      {
         Toast.makeText(ctx,"could not zip",Toast.LENGTH_LONG).show();
         e.printStackTrace();
      }
   // deleteRecursive(new File(mExportDirectoryPath));

    return f;

     }
 public static boolean deleteRecursive(File file)
 {
    if (file.isDirectory())
    {
       String[] children = file.list();
       for (int i = 0; i < children.length; i++)
       {
          boolean success = deleteRecursive(new File(file, children[i]));
          if (!success)
          {
             return false;
          }
       }
    }
    return file.delete();
 }
}
