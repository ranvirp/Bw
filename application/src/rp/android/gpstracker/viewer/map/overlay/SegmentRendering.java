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
package rp.android.gpstracker.viewer.map.overlay;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import rp.android.gpstracker.BuildConfig;
import rp.android.gpstracker.R;
import rp.android.gpstracker.db.GPStracking;
import rp.android.gpstracker.db.GPStracking.Media;
import rp.android.gpstracker.db.GPStracking.Waypoints;
import rp.android.gpstracker.util.UnitsI18n;
import rp.android.gpstracker.viewer.map.LoggerMap;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * Creates an overlay that can draw a single segment of connected waypoints
 * 
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class SegmentRendering
{
   public static final int MIDDLE_SEGMENT = 0;
   public static final int FIRST_SEGMENT = 1;
   public static final int LAST_SEGMENT = 2;
   public static final int DRAW_GREEN = 0;
   public static final int DRAW_RED = 1;
   public static final int DRAW_MEASURED = 2;
   public static final int DRAW_CALCULATED = 3;
   public static final int DRAW_DOTS = 4;
   public static final int DRAW_HEIGHT = 5;
   private static final String TAG = "OGT.SegmentRendering";
   private static final float MINIMUM_PX_DISTANCE = 15;

   private static SparseArray<Bitmap> sBitmapCache = new SparseArray<Bitmap>();;

   private int mTrackColoringMethod = DRAW_CALCULATED;

   private ContentResolver mResolver;
   private LoggerMap mLoggerMap;

   private int mPlacement = SegmentRendering.MIDDLE_SEGMENT;
   private Uri mWaypointsUri;
   private Uri mMediaUri;
   private double mAvgSpeed;
   private double mAvgHeight;
   private GeoPoint mGeoTopLeft;
   private GeoPoint mGeoBottumRight;

   private Vector<DotVO> mDotPath;
   private Vector<DotVO> mDotPathCalculation;
   private Path mCalculatedPath;
   private Point mCalculatedStart;
   private Point mCalculatedStop;
   private Path mPathCalculation;
   private Shader mShader;
   private Vector<MediaVO> mMediaPath;
   private Vector<MediaVO> mMediaPathCalculation;

   private GeoPoint mStartPoint;
   private GeoPoint mEndPoint;
   private Point mPrevDrawnScreenPoint;
   private Point mScreenPointBackup;
   private Point mScreenPoint;
   private Point mMediaScreenPoint;
   private int mStepSize = -1;
   private Location mLocation;
   private Location mPrevLocation;
   private Cursor mWaypointsCursor;
   private Cursor mMediaCursor;
   private Uri mSegmentUri;
   private int mWaypointCount = -1;
   private int mWidth;
   private int mHeight;
   private GeoPoint mPrevGeoPoint;
   private int mCurrentColor;
   private Paint dotpaint;
   private Paint radiusPaint;
   private Paint routePaint;
   private Paint defaultPaint;
   private boolean mRequeryFlag;
   private Handler mHandler;
   private static Bitmap sStartBitmap;
   private static Bitmap sStopBitmap;
   private AsyncOverlay mAsyncOverlay;

   private ContentObserver mTrackSegmentsObserver;

   private final Runnable mMediaCalculator = new Runnable()
   {
      @Override
      public void run()
      {
         SegmentRendering.this.calculateMediaAsync();
      }
   };

   private final Runnable mTrackCalculator = new Runnable()
   {
      @Override
      public void run()
      {
         SegmentRendering.this.calculateTrackAsync();
      }
   };

   /**
    * Constructor: create a new TrackingOverlay.
    * 
    * @param loggermap
    * @param segmentUri
    * @param color
    * @param avgSpeed
    * @param handler
    */
   public SegmentRendering(LoggerMap loggermap, Uri segmentUri, int color, double avgSpeed, double avgHeight, Handler handler)
   {
      super();
      mHandler = handler;
      mLoggerMap = loggermap;
      mTrackColoringMethod = color;
      mAvgSpeed = avgSpeed;
      mAvgHeight = avgHeight;
      mSegmentUri = segmentUri;
      mMediaUri = Uri.withAppendedPath(mSegmentUri, "media");
      mWaypointsUri = Uri.withAppendedPath(mSegmentUri, "waypoints");
      mResolver = mLoggerMap.getActivity().getContentResolver();
      mRequeryFlag = true;
      mCurrentColor = Color.rgb(255, 0, 0);

      dotpaint = new Paint();
      radiusPaint = new Paint();
      radiusPaint.setColor(Color.YELLOW);
      radiusPaint.setAlpha(100);
      routePaint = new Paint();
      routePaint.setStyle(Paint.Style.STROKE);
      routePaint.setStrokeWidth(6);
      routePaint.setAntiAlias(true);
      routePaint.setPathEffect(new CornerPathEffect(10));
      defaultPaint = new Paint();
      mScreenPoint = new Point();
      mMediaScreenPoint = new Point();
      mScreenPointBackup = new Point();
      mPrevDrawnScreenPoint = new Point();

      mDotPath = new Vector<DotVO>();
      mDotPathCalculation = new Vector<DotVO>();
      mCalculatedPath = new Path();
      mPathCalculation = new Path();
      mMediaPath = new Vector<MediaVO>();
      mMediaPathCalculation = new Vector<MediaVO>();

      mTrackSegmentsObserver = new ContentObserver(new Handler())
      {

         @Override
         public void onChange(boolean selfUpdate)
         {
            if (!selfUpdate)
            {
               mRequeryFlag = true;
            }
            else
            {
               Log.w(TAG, "mTrackSegmentsObserver skipping change on " + mSegmentUri);
            }
         }
      };
      openResources();
   }

   public void closeResources()
   {
      mResolver.unregisterContentObserver(mTrackSegmentsObserver);
      mHandler.removeCallbacks(mMediaCalculator);
      mHandler.removeCallbacks(mTrackCalculator);
      mHandler.postAtFrontOfQueue(new Runnable()
      {
         @Override
         public void run()
         {
            if (mWaypointsCursor != null)
            {
               mWaypointsCursor.close();
               mWaypointsCursor = null;
            }
            if (mMediaCursor != null)
            {
               mMediaCursor.close();
               mMediaCursor = null;
            }
         }
      });
      SegmentRendering.sStopBitmap = null;
      SegmentRendering.sStartBitmap = null;
   }

   public void openResources()
   {
      mResolver.registerContentObserver(mWaypointsUri, false, mTrackSegmentsObserver);
   }

   /**
    * Private draw method called by both the draw from Google Overlay and the
    * OSM Overlay
    * 
    * @param canvas
    */
   public void draw(Canvas canvas)
   {
      switch (mTrackColoringMethod)
      {
         case DRAW_HEIGHT:
         case DRAW_CALCULATED:
         case DRAW_MEASURED:
         case DRAW_RED:
         case DRAW_GREEN:
            drawPath(canvas);
            break;
         case DRAW_DOTS:
            drawDots(canvas);
            break;
      }
      drawStartStopCircles(canvas);
      drawMedia(canvas);
      
      mWidth = canvas.getWidth();
      mHeight = canvas.getHeight();
   }

   public void calculateTrack()
   {
      mHandler.removeCallbacks(mTrackCalculator);
      mHandler.post(mTrackCalculator);
   }

   /**
    * Either the Path or the Dots are calculated based on he current track
    * coloring method
    */
   private synchronized void calculateTrackAsync()
   {
      mGeoTopLeft = mLoggerMap.fromPixels(0, 0);
      mGeoBottumRight = mLoggerMap.fromPixels(mWidth, mHeight);

      calculateStepSize();

      mScreenPoint.x = -1;
      mScreenPoint.y = -1;
      this.mPrevDrawnScreenPoint.x = -1;
      this.mPrevDrawnScreenPoint.y = -1;

      switch (mTrackColoringMethod)
      {
         case DRAW_HEIGHT:
         case DRAW_CALCULATED:
         case DRAW_MEASURED:
         case DRAW_RED:
         case DRAW_GREEN:
            calculatePath();
            synchronized (mCalculatedPath) // Switch the fresh path with the old Path object
            {
               Path oldPath = mCalculatedPath;
               mCalculatedPath = mPathCalculation;
               mPathCalculation = oldPath;
            }
            break;
         case DRAW_DOTS:
            calculateDots();
            synchronized (mDotPath) // Switch the fresh path with the old Path object
            {
               Vector<DotVO> oldDotPath = mDotPath;
               mDotPath = mDotPathCalculation;
               mDotPathCalculation = oldDotPath;
            }
            break;
      }
      calculateStartStopCircles();
      mAsyncOverlay.onDateOverlayChanged();
   }

   /**
    * Calculated the new contents of segment in the mDotPathCalculation
    */
   private void calculatePath()
   {
      mDotPathCalculation.clear();
      this.mPathCalculation.rewind();

      this.mShader = null;

      GeoPoint geoPoint;
      this.mPrevLocation = null;

      if (mWaypointsCursor == null)
      {
         mWaypointsCursor = this.mResolver.query(this.mWaypointsUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.SPEED, Waypoints.TIME,
               Waypoints.ACCURACY, Waypoints.ALTITUDE }, null, null, null);
         mRequeryFlag = false;
      }
      if (mRequeryFlag)
      {
         mWaypointsCursor.requery();
         mRequeryFlag = false;
      }
      if (mLoggerMap.hasProjection() && mWaypointsCursor.moveToFirst())
      {
         // Start point of the segments, possible a dot
         this.mStartPoint = extractGeoPoint();
         mPrevGeoPoint = mStartPoint;
         this.mLocation = new Location(this.getClass().getName());
         this.mLocation.setLatitude(mWaypointsCursor.getDouble(0));
         this.mLocation.setLongitude(mWaypointsCursor.getDouble(1));
         this.mLocation.setTime(mWaypointsCursor.getLong(3));

         moveToGeoPoint(this.mStartPoint);

         do
         {
            geoPoint = extractGeoPoint();
            // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
            if (geoPoint.getLatitudeE6() == 0 || geoPoint.getLongitudeE6() == 0)
            {
               continue;
            }

            double speed = -1d;
            switch (mTrackColoringMethod)
            {
               case DRAW_GREEN:
               case DRAW_RED:
                  plainLineToGeoPoint(geoPoint);
                  break;
               case DRAW_MEASURED:
                  speedLineToGeoPoint(geoPoint, mWaypointsCursor.getDouble(2));
                  break;
               case DRAW_CALCULATED:
                  this.mPrevLocation = this.mLocation;
                  this.mLocation = new Location(this.getClass().getName());
                  this.mLocation.setLatitude(mWaypointsCursor.getDouble(0));
                  this.mLocation.setLongitude(mWaypointsCursor.getDouble(1));
                  this.mLocation.setTime(mWaypointsCursor.getLong(3));
                  speed = calculateSpeedBetweenLocations(this.mPrevLocation, this.mLocation);
                  speedLineToGeoPoint(geoPoint, speed);
                  break;
               case DRAW_HEIGHT:
                  heightLineToGeoPoint(geoPoint, mWaypointsCursor.getDouble(5));
                  break;
               default:
                  Log.w(TAG, "Unknown coloring method");
                  break;
            }
         }
         while (moveToNextWayPoint());

         this.mEndPoint = extractGeoPoint(); // End point of the segments, possible a dot

      }
      //      Log.d( TAG, "transformSegmentToPath stop: points "+mCalculatedPoints+" from "+moves+" moves" );
   }

   /**
    * @param canvas
    * @param mapView
    * @param shadow
    * @see SegmentRendering#draw(Canvas, MapView, boolean)
    */
   private void calculateDots()
   {
      mPathCalculation.reset();
      mDotPathCalculation.clear();

      if (mWaypointsCursor == null)
      {
         mWaypointsCursor = this.mResolver.query(this.mWaypointsUri, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE, Waypoints.SPEED, Waypoints.TIME,
               Waypoints.ACCURACY }, null, null, null);
      }
      if (mRequeryFlag)
      {
         mWaypointsCursor.requery();
         mRequeryFlag = false;
      }
      if (mLoggerMap.hasProjection() && mWaypointsCursor.moveToFirst())
      {
         GeoPoint geoPoint;

         mStartPoint = extractGeoPoint();
         mPrevGeoPoint = mStartPoint;

         do
         {
            geoPoint = extractGeoPoint();
            // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
            if (geoPoint.getLatitudeE6() == 0 || geoPoint.getLongitudeE6() == 0)
            {
               continue;
            }
            setScreenPoint(geoPoint);

            float distance = (float) distanceInPoints(this.mPrevDrawnScreenPoint, this.mScreenPoint);
            if (distance > MINIMUM_PX_DISTANCE)
            {
               DotVO dotVO = new DotVO();
               dotVO.x = this.mScreenPoint.x;
               dotVO.y = this.mScreenPoint.y;
               dotVO.speed = mWaypointsCursor.getLong(2);
               dotVO.time = mWaypointsCursor.getLong(3);
               dotVO.radius = mLoggerMap.metersToEquatorPixels(mWaypointsCursor.getFloat(4));
               mDotPathCalculation.add(dotVO);

               this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
               this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
            }
         }
         while (moveToNextWayPoint());

         this.mEndPoint = extractGeoPoint();
         DotVO pointVO = new DotVO();
         pointVO.x = this.mScreenPoint.x;
         pointVO.y = this.mScreenPoint.y;
         pointVO.speed = mWaypointsCursor.getLong(2);
         pointVO.time = mWaypointsCursor.getLong(3);
         pointVO.radius = mLoggerMap.metersToEquatorPixels(mWaypointsCursor.getFloat(4));
         mDotPathCalculation.add(pointVO);
      }
   }

   public void calculateMedia()
   {
      mHandler.removeCallbacks(mMediaCalculator);
      mHandler.post(mMediaCalculator);
   }

   public synchronized void calculateMediaAsync()
   {
      mMediaPathCalculation.clear();
      if (mMediaCursor == null)
      {
         mMediaCursor = this.mResolver.query(this.mMediaUri, new String[] { Media.WAYPOINT, Media.URI }, null, null, null);
      }
      else
      {
         mMediaCursor.requery();
      }
      if (mLoggerMap.hasProjection() && mMediaCursor.moveToFirst())
      {
         GeoPoint lastPoint = null;
         int wiggle = 0;
         do
         {
            MediaVO mediaVO = new MediaVO();
            mediaVO.waypointId = mMediaCursor.getLong(0);
            mediaVO.uri = Uri.parse(mMediaCursor.getString(1));

            Uri mediaWaypoint = ContentUris.withAppendedId(mWaypointsUri, mediaVO.waypointId);
            Cursor waypointCursor = null;
            try
            {
               waypointCursor = this.mResolver.query(mediaWaypoint, new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE }, null, null, null);
               if (waypointCursor != null && waypointCursor.moveToFirst())
               {
                  int microLatitude = (int) (waypointCursor.getDouble(0) * 1E6d);
                  int microLongitude = (int) (waypointCursor.getDouble(1) * 1E6d);
                  mediaVO.geopoint = new GeoPoint(microLatitude, microLongitude);
               }
            }
            finally
            {
               if (waypointCursor != null)
               {
                  waypointCursor.close();
               }
            }
            if (isGeoPointOnScreen(mediaVO.geopoint))
            {
               mLoggerMap.toPixels(mediaVO.geopoint, this.mMediaScreenPoint);
               if (mediaVO.geopoint.equals(lastPoint))
               {
                  wiggle += 4;
               }
               else
               {
                  wiggle = 0;
               }
               mediaVO.bitmapKey = getResourceForMedia(mLoggerMap.getActivity().getResources(), mediaVO.uri);
               mediaVO.w = sBitmapCache.get(mediaVO.bitmapKey).getWidth();
               mediaVO.h = sBitmapCache.get(mediaVO.bitmapKey).getHeight();
               int left = (mediaVO.w * 3) / 7 + wiggle;
               int up = (mediaVO.h * 6) / 7 - wiggle;
               mediaVO.x = mMediaScreenPoint.x - left;
               mediaVO.y = mMediaScreenPoint.y - up;

               lastPoint = mediaVO.geopoint;
            }
            mMediaPathCalculation.add(mediaVO);
         }
         while (mMediaCursor.moveToNext());
      }

      synchronized (mMediaPath) // Switch the fresh path with the old Path object
      {
         Vector<MediaVO> oldmMediaPath = mMediaPath;
         mMediaPath = mMediaPathCalculation;
         mMediaPathCalculation = oldmMediaPath;
      }
      if (mMediaPathCalculation.size() != mMediaPath.size())
      {
         mAsyncOverlay.onDateOverlayChanged();
      }
   }

   private void calculateStartStopCircles()
   {
      if ((this.mPlacement == FIRST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT) && this.mStartPoint != null)
      {
         if (sStartBitmap == null)
         {
            sStartBitmap = BitmapFactory.decodeResource(this.mLoggerMap.getActivity().getResources(), R.drawable.stip);
         }
         if (mCalculatedStart == null)
         {
            mCalculatedStart = new Point();
         }
         mLoggerMap.toPixels(this.mStartPoint, mCalculatedStart);

      }
      if ((this.mPlacement == LAST_SEGMENT || this.mPlacement == FIRST_SEGMENT + LAST_SEGMENT) && this.mEndPoint != null)
      {
         if (sStopBitmap == null)
         {
            sStopBitmap = BitmapFactory.decodeResource(this.mLoggerMap.getActivity().getResources(), R.drawable.stip2);
         }
         if (mCalculatedStop == null)
         {
            mCalculatedStop = new Point();
         }
         mLoggerMap.toPixels(this.mEndPoint, mCalculatedStop);
      }
   }

   /**
    * @param canvas
    * @see SegmentRendering#draw(Canvas, MapView, boolean)
    */
   private void drawPath(Canvas canvas)
   {
      switch (mTrackColoringMethod)
      {
         case DRAW_HEIGHT:
         case DRAW_CALCULATED:
         case DRAW_MEASURED:
            routePaint.setShader(this.mShader);
            break;
         case DRAW_RED:
            routePaint.setShader(null);
            routePaint.setColor(Color.RED);
            break;
         case DRAW_GREEN:
            routePaint.setShader(null);
            routePaint.setColor(Color.GREEN);
            break;
         default:
            routePaint.setShader(null);
            routePaint.setColor(Color.YELLOW);
            break;
      }
      synchronized (mCalculatedPath)
      {
         canvas.drawPath(mCalculatedPath, routePaint);
      }
   }

   private void drawDots(Canvas canvas)
   {
      synchronized (mDotPath)
      {
         if (sStopBitmap == null)
         {
            sStopBitmap = BitmapFactory.decodeResource(this.mLoggerMap.getActivity().getResources(), R.drawable.stip2);
         }
         for (DotVO dotVO : mDotPath)
         {
            canvas.drawBitmap(sStopBitmap, dotVO.x - 8, dotVO.y - 8, dotpaint);
            if (dotVO.radius > 8f)
            {
               canvas.drawCircle(dotVO.x, dotVO.y, dotVO.radius, radiusPaint);
            }
         }
      }
   }

   private void drawMedia(Canvas canvas)
   {
      synchronized (mMediaPath)
      {
         for (MediaVO mediaVO : mMediaPath)
         {
            if (mediaVO.bitmapKey != null)
            {
               Log.d(TAG, "Draw bitmap at (" + mediaVO.x + ", " + mediaVO.y + ") on " + canvas);
               canvas.drawBitmap(sBitmapCache.get(mediaVO.bitmapKey), mediaVO.x, mediaVO.y, defaultPaint);
            }
         }
      }
   }

   private void drawStartStopCircles(Canvas canvas)
   {
      if (mCalculatedStart != null)
      {
         canvas.drawBitmap(sStartBitmap, mCalculatedStart.x - 8, mCalculatedStart.y - 8, defaultPaint);
      }
      if (mCalculatedStop != null)
      {
         canvas.drawBitmap(sStopBitmap, mCalculatedStop.x - 5, mCalculatedStop.y - 5, defaultPaint);
      }
   }

   private Integer getResourceForMedia(Resources resources, Uri uri)
   {
      int drawable = 0;
      if (uri.getScheme().equals("file"))
      {
         if (uri.getLastPathSegment().endsWith("3gp"))
         {
            drawable = R.drawable.media_film;
         }
         else if (uri.getLastPathSegment().endsWith("jpg"))
         {
            drawable = R.drawable.media_camera;
         }
         else if (uri.getLastPathSegment().endsWith("txt"))
         {
            drawable = R.drawable.media_notepad;
         }
      }
      else if (uri.getScheme().equals("content"))
      {
         if (uri.getAuthority().equals(GPStracking.AUTHORITY + ".string"))
         {
            drawable = R.drawable.media_mark;
         }
         else if (uri.getAuthority().equals("media"))
         {
            drawable = R.drawable.media_speech;
         }
      }
      Bitmap bitmap = null;
      int bitmapKey = drawable;
      synchronized (sBitmapCache)
      {
         if (sBitmapCache.get(bitmapKey) == null)
         {
            bitmap = BitmapFactory.decodeResource(resources, drawable);
            sBitmapCache.put(bitmapKey, bitmap);

         }
         bitmap = sBitmapCache.get(bitmapKey);
      }
      return bitmapKey;
   }

   /**
    * Set the mPlace to the specified value.
    * 
    * @see SegmentRendering.FIRST
    * @see SegmentRendering.MIDDLE
    * @see SegmentRendering.LAST
    * @param place The placement of this segment in the line.
    */
   public void addPlacement(int place)
   {
      this.mPlacement += place;
   }

   public boolean isLast()
   {
      return (mPlacement >= LAST_SEGMENT);
   }

   public long getSegmentId()
   {
      return Long.parseLong(mSegmentUri.getLastPathSegment());
   }

   /**
    * Set the beginnging to the next contour of the line to the give GeoPoint
    * 
    * @param geoPoint
    */
   private void moveToGeoPoint(GeoPoint geoPoint)
   {
      setScreenPoint(geoPoint);

      if (this.mPathCalculation != null)
      {
         this.mPathCalculation.moveTo(this.mScreenPoint.x, this.mScreenPoint.y);
         this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
         this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
      }
   }

   /**
    * Line to point without shaders
    * 
    * @param geoPoint
    */
   private void plainLineToGeoPoint(GeoPoint geoPoint)
   {
      shaderLineToGeoPoint(geoPoint, 0, 0);
   }

   /**
    * Line to point with speed
    * 
    * @param geoPoint
    * @param height
    */
   private void heightLineToGeoPoint(GeoPoint geoPoint, double height)
   {
      shaderLineToGeoPoint(geoPoint, height, mAvgHeight);
   }

   /**
    * Line to point with speed
    * 
    * @param geoPoint
    * @param speed
    */
   private void speedLineToGeoPoint(GeoPoint geoPoint, double speed)
   {
      shaderLineToGeoPoint(geoPoint, speed, mAvgSpeed);
   }

   private void shaderLineToGeoPoint(GeoPoint geoPoint, double value, double average)
   {
      setScreenPoint(geoPoint);

      //      Log.d( TAG, "Draw line to " + geoPoint+" with speed "+speed );

      if (value > 0)
      {
         int greenfactor = (int) Math.min((127 * value) / average, 255);
         int redfactor = 255 - greenfactor;
         mCurrentColor = Color.rgb(redfactor, greenfactor, 0);
      }
      else
      {
         int greenfactor = Color.green(mCurrentColor);
         int redfactor = Color.red(mCurrentColor);
         mCurrentColor = Color.argb(128, redfactor, greenfactor, 0);
      }

      float distance = (float) distanceInPoints(this.mPrevDrawnScreenPoint, this.mScreenPoint);
      if (distance > MINIMUM_PX_DISTANCE)
      {
         //         Log.d( TAG, "Circle between " + mPrevDrawnScreenPoint+" and "+mScreenPoint );
         int x_circle = (this.mPrevDrawnScreenPoint.x + this.mScreenPoint.x) / 2;
         int y_circle = (this.mPrevDrawnScreenPoint.y + this.mScreenPoint.y) / 2;
         float radius_factor = 0.4f;
         Shader lastShader = new RadialGradient(x_circle, y_circle, distance, new int[] { mCurrentColor, mCurrentColor, Color.TRANSPARENT }, new float[] { 0,
               radius_factor, 0.6f }, TileMode.CLAMP);
         //            Paint debug = new Paint();
         //            debug.setStyle( Paint.Style.FILL_AND_STROKE );
         //            this.mDebugCanvas.drawCircle(
         //                  x_circle,
         //                  y_circle, 
         //                  distance*radius_factor/2, 
         //                  debug );
         //            this.mDebugCanvas.drawCircle(
         //                  x_circle,
         //                  y_circle, 
         //                  distance*radius_factor, 
         //                  debug );
         //            if( distance > 100 )
         //            {
         //               Log.d( TAG, "Created shader for speed " + speed + " on " + x_circle + "," + y_circle );
         //            }
         if (this.mShader != null)
         {
            this.mShader = new ComposeShader(this.mShader, lastShader, Mode.DST_OVER);
         }
         else
         {
            this.mShader = lastShader;
         }
         this.mPrevDrawnScreenPoint.x = this.mScreenPoint.x;
         this.mPrevDrawnScreenPoint.y = this.mScreenPoint.y;
      }

      this.mPathCalculation.lineTo(this.mScreenPoint.x, this.mScreenPoint.y);
   }

   /**
    * Use to update location/point state when calculating the line
    * 
    * @param geoPoint
    */
   private void setScreenPoint(GeoPoint geoPoint)
   {
      mScreenPointBackup.x = this.mScreenPoint.x;
      mScreenPointBackup.y = this.mScreenPoint.x;

      mLoggerMap.toPixels(geoPoint, this.mScreenPoint);
   }

   /**
    * Move to a next waypoint, for on screen this are the points with mStepSize
    * % position == 0 to avoid jittering in the rendering or the points on the
    * either side of the screen edge.
    * 
    * @return if a next waypoint is pointed to with the mWaypointsCursor
    */
   private boolean moveToNextWayPoint()
   {
      boolean cursorReady = true;
      boolean onscreen = isGeoPointOnScreen(extractGeoPoint());
      if (mWaypointsCursor.isLast()) // End of the line, cant move onward
      {
         cursorReady = false;
      }
      else if (onscreen) // Are on screen
      {
         cursorReady = moveOnScreenWaypoint();
      }
      else
      // Are off screen => accelerate
      {
         int acceleratedStepsize = mStepSize * (mWaypointCount / 1000 + 6);
         cursorReady = moveOffscreenWaypoint(acceleratedStepsize);
      }
      return cursorReady;
   }

   /**
    * Move the cursor to the next waypoint modulo of the step size or less if
    * the screen edge is reached
    * 
    * @param trackCursor
    * @return
    */
   private boolean moveOnScreenWaypoint()
   {
      int nextPosition = mStepSize * (mWaypointsCursor.getPosition() / mStepSize) + mStepSize;
      if (mWaypointsCursor.moveToPosition(nextPosition))
      {
         if (isGeoPointOnScreen(extractGeoPoint())) // Remained on screen
         {
            return true; // Cursor is pointing to somewhere
         }
         else
         {
            mWaypointsCursor.move(-1 * mStepSize); // Step back
            boolean nowOnScreen = true; // onto the screen
            while (nowOnScreen) // while on the screen 
            {
               mWaypointsCursor.moveToNext(); // inch forward to the edge
               nowOnScreen = isGeoPointOnScreen(extractGeoPoint());
            }
            return true; // with a cursor point to somewhere
         }
      }
      else
      {
         return mWaypointsCursor.moveToLast(); // No full step can be taken, move to last
      }
   }

   /**
    * Previous path GeoPoint was off screen and the next one will be to or the
    * first on screen when the path reaches the projection.
    * 
    * @return
    */
   private boolean moveOffscreenWaypoint(int flexStepsize)
   {
      while (mWaypointsCursor.move(flexStepsize))
      {
         if (mWaypointsCursor.isLast())
         {
            return true;
         }
         GeoPoint evalPoint = extractGeoPoint();
         // Do no include log wrong 0.0 lat 0.0 long, skip to next value in while-loop
         if (evalPoint.getLatitudeE6() == 0 || evalPoint.getLongitudeE6() == 0)
         {
            continue;
         }
         //         Log.d( TAG, String.format( "Evaluate point number %d ", mWaypointsCursor.getPosition() ) );
         if (possibleScreenPass(mPrevGeoPoint, evalPoint))
         {
            mPrevGeoPoint = evalPoint;
            if (flexStepsize == 1) // Just stumbled over a border
            {
               return true;
            }
            else
            {
               mWaypointsCursor.move(-1 * flexStepsize); // Take 1 step back
               return moveOffscreenWaypoint(flexStepsize / 2); // Continue at halve accelerated speed
            }
         }
         else
         {
            moveToGeoPoint(evalPoint);
            mPrevGeoPoint = evalPoint;
         }

      }
      return mWaypointsCursor.moveToLast();
   }

   /**
    * If a segment contains more then 500 waypoints and is zoomed out more then
    * twice then some waypoints will not be used to render the line, this
    * speeding things along.
    */
   private void calculateStepSize()
   {
      Cursor waypointsCursor = null;
      if (mRequeryFlag || mStepSize < 1 || mWaypointCount < 0)
      {
         try
         {
            waypointsCursor = this.mResolver.query(this.mWaypointsUri, new String[] { Waypoints._ID }, null, null, null);
            mWaypointCount = waypointsCursor.getCount();
         }
         finally
         {
            if (waypointsCursor != null)
            {
               waypointsCursor.close();
            }
         }
      }
      if (mWaypointCount < 250)
      {
         mStepSize = 1;
      }
      else
      {
         int zoomLevel = mLoggerMap.getZoomLevel();
         int maxZoomLevel = mLoggerMap.getMaxZoomLevel();
         if (zoomLevel >= maxZoomLevel - 2)
         {
            mStepSize = 1;
         }
         else
         {
            mStepSize = maxZoomLevel - zoomLevel;
         }
      }
   }

   /**
    * Is a given GeoPoint in the current projection of the map.
    * 
    * @param eval
    * @return
    */
   protected boolean isGeoPointOnScreen(GeoPoint geopoint)
   {
      boolean onscreen = geopoint != null;
      if (geopoint != null && mGeoTopLeft != null && mGeoBottumRight != null)
      {
         onscreen = onscreen && mGeoTopLeft.getLatitudeE6() > geopoint.getLatitudeE6();
         onscreen = onscreen && mGeoBottumRight.getLatitudeE6() < geopoint.getLatitudeE6();
         if (mGeoTopLeft.getLongitudeE6() < mGeoBottumRight.getLongitudeE6())
         {
            onscreen = onscreen && mGeoTopLeft.getLongitudeE6() < geopoint.getLongitudeE6();
            onscreen = onscreen && mGeoBottumRight.getLongitudeE6() > geopoint.getLongitudeE6();
         }
         else
         {
            onscreen = onscreen && (mGeoTopLeft.getLongitudeE6() < geopoint.getLongitudeE6() || mGeoBottumRight.getLongitudeE6() > geopoint.getLongitudeE6());
         }
      }
      return onscreen;
   }

   /**
    * Is a given coordinates are on the screen
    * 
    * @param eval
    * @return
    */
   protected boolean isOnScreen(int x, int y)
   {
      boolean onscreen = x > 0 && y > 0 && x < mWidth && y < mHeight;
      return onscreen;
   }

   /**
    * Calculates in which segment opposited to the projecting a geo point
    * resides
    * 
    * @param p1
    * @return
    */
   private int toSegment(GeoPoint p1)
   {
      //      Log.d( TAG, String.format( "Comparing %s to points TL %s and BR %s", p1, mTopLeft, mBottumRight )); 
      int nr;
      if (p1.getLongitudeE6() < mGeoTopLeft.getLongitudeE6()) // left
      {
         nr = 1;
      }
      else if (p1.getLongitudeE6() > mGeoBottumRight.getLongitudeE6()) // right
      {
         nr = 3;
      }
      else
      // middle
      {
         nr = 2;
      }

      if (p1.getLatitudeE6() > mGeoTopLeft.getLatitudeE6()) // top
      {
         nr = nr + 0;
      }
      else if (p1.getLatitudeE6() < mGeoBottumRight.getLatitudeE6()) // bottom
      {
         nr = nr + 6;
      }
      else
      // middle
      {
         nr = nr + 3;
      }
      return nr;
   }

   private boolean possibleScreenPass(GeoPoint fromGeo, GeoPoint toGeo)
   {
      boolean safe = true;
      if (fromGeo != null && toGeo != null)
      {
         int from = toSegment(fromGeo);
         int to = toSegment(toGeo);

         switch (from)
         {
            case 1:
               safe = to == 1 || to == 2 || to == 3 || to == 4 || to == 7;
               break;
            case 2:
               safe = to == 1 || to == 2 || to == 3;
               break;
            case 3:
               safe = to == 1 || to == 2 || to == 3 || to == 6 || to == 9;
               break;
            case 4:
               safe = to == 1 || to == 4 || to == 7;
               break;
            case 5:
               safe = false;
               break;
            case 6:
               safe = to == 3 || to == 6 || to == 9;
               break;
            case 7:
               safe = to == 1 || to == 4 || to == 7 || to == 8 || to == 9;
               break;
            case 8:
               safe = to == 7 || to == 8 || to == 9;
               break;
            case 9:
               safe = to == 3 || to == 6 || to == 7 || to == 8 || to == 9;
               break;
            default:
               safe = false;
               break;
         }
         //            Log.d( TAG, String.format( "From %d to %d is safe: %s", from, to, safe ) );
      }
      return !safe;
   }

   public void setTrackColoringMethod(int coloring, double avgspeed, double avgHeight)
   {
      if (mTrackColoringMethod != coloring)
      {
         mTrackColoringMethod = coloring;
         calculateTrack();
      }
      mAvgSpeed = avgspeed;
      mAvgHeight = avgHeight;
   }

   /**
    * For the current waypoint cursor returns the GeoPoint
    * 
    * @return
    */
   private GeoPoint extractGeoPoint()
   {
      int microLatitude = (int) (mWaypointsCursor.getDouble(0) * 1E6d);
      int microLongitude = (int) (mWaypointsCursor.getDouble(1) * 1E6d);
      return new GeoPoint(microLatitude, microLongitude);
   }

   /**
    * @param startLocation
    * @param endLocation
    * @return speed in m/s between 2 locations
    */
   private static double calculateSpeedBetweenLocations(Location startLocation, Location endLocation)
   {
      double speed = -1d;
      if (startLocation != null && endLocation != null)
      {
         float distance = startLocation.distanceTo(endLocation);
         float seconds = (endLocation.getTime() - startLocation.getTime()) / 1000f;
         speed = distance / seconds;
         //         Log.d( TAG, "Found a speed of "+speed+ " over a distance of "+ distance+" in a time of "+seconds);
      }
      if (speed > 0)
      {
         return speed;
      }
      else
      {
         return -1d;
      }
   }

   public static int extendPoint(int x1, int x2)
   {
      int diff = x2 - x1;
      int next = x2 + diff;
      return next;
   }

   private static double distanceInPoints(Point start, Point end)
   {
      int x = Math.abs(end.x - start.x);
      int y = Math.abs(end.y - start.y);
      return (double) Math.sqrt(x * x + y * y);
   }

   private boolean handleMediaTapList(List<Uri> tappedUri)
   {
      if (tappedUri.size() == 1)
      {
         return handleMedia(mLoggerMap.getActivity(), tappedUri.get(0));
      }
      else
      {
         BaseAdapter adapter = new MediaAdapter(mLoggerMap.getActivity(), tappedUri);
         mLoggerMap.showMediaDialog(adapter);
         return true;
      }
   }

   public static boolean handleMedia(Context ctx, Uri mediaUri)
   {
      if (mediaUri.getScheme().equals("file"))
      {
         Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
         if (mediaUri.getLastPathSegment().endsWith("3gp"))
         {
            intent.setDataAndType(mediaUri, "video/3gpp");
            ctx.startActivity(intent);
            return true;
         }
         else if (mediaUri.getLastPathSegment().endsWith("jpg"))
         {
            //<scheme>://<authority><absolute path>
            Uri.Builder builder = new Uri.Builder();
            mediaUri = builder.scheme(mediaUri.getScheme()).authority(mediaUri.getAuthority()).path(mediaUri.getPath()).build();
            intent.setDataAndType(mediaUri, "image/jpeg");
            ctx.startActivity(intent);
            return true;
         }
         else if (mediaUri.getLastPathSegment().endsWith("txt"))
         {
            intent.setDataAndType(mediaUri, "text/plain");
            ctx.startActivity(intent);
            return true;
         }
      }
      else if (mediaUri.getScheme().equals("content"))
      {
         if (mediaUri.getAuthority().equals(GPStracking.AUTHORITY + ".string"))
         {
            String text = mediaUri.getLastPathSegment();
            Toast toast = Toast.makeText(ctx, text, Toast.LENGTH_LONG);
            toast.show();
            return true;
         }
         else if (mediaUri.getAuthority().equals("media"))
         {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, mediaUri));
            return true;
         }
      }
      return false;
   }

   public boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      List<Uri> tappedUri = new Vector<Uri>();

      Point tappedPoint = new Point();
      mLoggerMap.toPixels(tappedGeoPoint, tappedPoint);
      for (MediaVO media : mMediaPath)
      {
         if (media.x < tappedPoint.x && tappedPoint.x < media.x + media.w && media.y < tappedPoint.y && tappedPoint.y < media.y + media.h)
         {
            tappedUri.add(media.uri);
         }
      }
      if (tappedUri.size() > 0)
      {
         return handleMediaTapList(tappedUri);
      }
      else
      {
         if (mTrackColoringMethod == DRAW_DOTS)
         {
            DotVO tapped = null;
            synchronized (mDotPath) // Switch the fresh path with the old Path object
            {
               int w = 25;
               for (DotVO dot : mDotPath)
               {
                  //                  Log.d( TAG, "Compare ("+dot.x+","+dot.y+") with tap ("+tappedPoint.x+","+tappedPoint.y+")" );
                  if (dot.x - w < tappedPoint.x && tappedPoint.x < dot.x + w && dot.y - w < tappedPoint.y && tappedPoint.y < dot.y + w)
                  {
                     if (tapped == null)
                     {
                        tapped = dot;
                     }
                     else
                     {
                        tapped = dot.distanceTo(tappedPoint) < tapped.distanceTo(tappedPoint) ? dot : tapped;
                     }
                  }
               }
            }
            if (tapped != null)
            {
               DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(mLoggerMap.getActivity().getApplicationContext());
               String timetxt = timeFormat.format(new Date(tapped.time));
               UnitsI18n units = new UnitsI18n(mLoggerMap.getActivity(), null);
               double speed = units.conversionFromMetersPerSecond(tapped.speed);
               String speedtxt = String.format("%.1f %s", speed, units.getSpeedUnit());
               String text = mLoggerMap.getActivity().getString(R.string.time_and_speed, timetxt, speedtxt);
               Toast toast = Toast.makeText(mLoggerMap.getActivity(), text, Toast.LENGTH_SHORT);
               toast.show();
            }
         }
         return false;
      }
   }

   private static class MediaVO
   {
      @Override
      public String toString()
      {
         return "MediaVO [bitmapKey=" + bitmapKey + ", uri=" + uri + ", geopoint=" + geopoint + ", x=" + x + ", y=" + y + ", w=" + w + ", h=" + h
               + ", waypointId=" + waypointId + "]";
      }

      public Integer bitmapKey;
      public Uri uri;
      public GeoPoint geopoint;
      public int x;
      public int y;
      public int w;
      public int h;
      public long waypointId;
   }

   private static class DotVO
   {
      public long time;
      public long speed;
      public int x;
      public int y;
      public float radius;

      public int distanceTo(Point tappedPoint)
      {
         return Math.abs(tappedPoint.x - this.x) + Math.abs(tappedPoint.y - this.y);
      }
   }

   private class MediaAdapter extends BaseAdapter
   {
      private Context mContext;
      private List<Uri> mTappedUri;
      private int itemBackground;

      public MediaAdapter(Context ctx, List<Uri> tappedUri)
      {
         mContext = ctx;
         mTappedUri = tappedUri;
         TypedArray a = mContext.obtainStyledAttributes(R.styleable.gallery);
         itemBackground = a.getResourceId(R.styleable.gallery_android_galleryItemBackground, 0);
         a.recycle();

      }

      @Override
      public int getCount()
      {
         return mTappedUri.size();
      }

      @Override
      public Object getItem(int position)
      {
         return mTappedUri.get(position);
      }

      @Override
      public long getItemId(int position)
      {
         return position;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent)
      {
         ImageView imageView = new ImageView(mContext);
         imageView.setImageBitmap(sBitmapCache.get(getResourceForMedia(mLoggerMap.getActivity().getResources(), mTappedUri.get(position))));
         imageView.setScaleType(ImageView.ScaleType.FIT_XY);
         imageView.setBackgroundResource(itemBackground);
         return imageView;

      }
   }

   public void setBitmapHolder(AsyncOverlay bitmapOverlay)
   {
      mAsyncOverlay = bitmapOverlay;
   }
}
