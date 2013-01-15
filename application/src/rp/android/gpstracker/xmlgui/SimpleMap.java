package rp.android.gpstracker.xmlgui;

import java.util.HashMap;
import java.util.Map;

public class SimpleMap
{
    Map<String, String> map = new HashMap<String, String>();
 public  SimpleMap(){
   map.put("w", "works");
   map.put("b", "beneficiary");
 }
 public String getName(String s){
   return  map.get(s);
 }
}
