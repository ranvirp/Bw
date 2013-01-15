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
package rp.android.gpstracker.util;

import java.io.File;

import rp.android.gpstracker.db.GPStracking;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;


/**
 * Various application wide constants
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class Constants1
{
   public static final String BWDIR = "/BW/";   
   public static String getSdCardDirectory( Context ctx )
   {
      // Read preference and ensure start and end with '/' symbol
      String dir = PreferenceManager.getDefaultSharedPreferences(ctx).getString("SDDIR_DIR1", BWDIR);
      if( !dir.startsWith("/") )
      {
         dir = "/" + dir;
      }
      if( !dir.endsWith("/") )
      {
         dir = dir + "/" ;
      }
      dir = Environment.getExternalStorageDirectory().getAbsolutePath() + dir;
      
      // If neither exists or can be created fall back to default
      File dirHandle = new File(dir);
      if( !dirHandle.exists() && !dirHandle.mkdirs() )
      {
         dir = Environment.getExternalStorageDirectory().getAbsolutePath() + BWDIR;
      }
      return dir;
   }
  
}
