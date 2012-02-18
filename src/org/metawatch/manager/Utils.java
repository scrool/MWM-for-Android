                                                                     
                                                                     
                                                                     
                                             
 /*****************************************************************************
  *  Copyright (c) 2011 Meta Watch Ltd.                                       *
  *  www.MetaWatch.org                                                        *
  *                                                                           *
  =============================================================================
  *                                                                           *
  *  Licensed under the Apache License, Version 2.0 (the "License");          *
  *  you may not use this file except in compliance with the License.         *
  *  You may obtain a copy of the License at                                  *
  *                                                                           *
  *    http://www.apache.org/licenses/LICENSE-2.0                             *
  *                                                                           *
  *  Unless required by applicable law or agreed to in writing, software      *
  *  distributed under the License is distributed on an "AS IS" BASIS,        *
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
  *  See the License for the specific language governing permissions and      *
  *  limitations under the License.                                           *
  *                                                                           *
  *****************************************************************************/

 /*****************************************************************************
  * Utils.java                                                                *
  * Utils                                                                     *
  * Different utils                                                           *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.util.Log;

public class Utils {

	static public String Meeting_Title = "There is no meeting Dave";
	static public String Meeting_Location = "Nobody likes you Dave";
	
	
	public static String getContactNameFromNumber(Context context, String number) {
		
		try {
			if (number.equals(""))
				return "Private number";
	
			String[] projection = new String[] { PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER };
			
			Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
			Cursor c = context.getContentResolver().query(contactUri, projection, null, null, null);
			
			if (c==null)
				return number;
			
			if (c.moveToFirst()) {
				String name = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
				c.close();
				
				if (name.length() > 0)
					return name;
				else
					return number;
			}
			
			c.close();
			return number;
		}
		catch(java.lang.IllegalStateException e) {
			return number;
		}
	}
	
	public static Bitmap getContactPhotoFromNumber(Context context, String number) {

		try {
			if (number.equals(""))
				return null;
	
			String[] projection = new String[] {PhoneLookup.PHOTO_ID, PhoneLookup.NUMBER};
			
			Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
			Cursor c = context.getContentResolver().query(contactUri, projection, null, null, null);
			
			if (c==null)
				return null;
			
			if (c.moveToFirst()) {
				int photoID = c.getInt(c.getColumnIndex(PhoneLookup.PHOTO_ID));
				c.close();

				Uri photoUri = ContactsContract.Data.CONTENT_URI;
				c = context.getContentResolver().query(photoUri, new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO, ContactsContract.Data.PHOTO_ID}, Data.PHOTO_ID + " = " + photoID, null, null);
				
				if (c.moveToFirst() == true) {
		            try {
		            	ByteArrayInputStream rawPhotoStream = new ByteArrayInputStream(c.getBlob(c.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO)));
		            	Bitmap contactPhoto = BitmapFactory.decodeStream(rawPhotoStream);
		            	c.close();
		            	return contactPhoto;
		            }
		            catch (NullPointerException ex) {
		            	c.close();
		            	return null;
		            }
		        }
			}
			
			c.close();
			return null;
		}
		catch(java.lang.IllegalStateException e) {
			return null;
		}

	}
	
	public static int getUnreadSmsCount(Context context) {

		int count = 0;
		try {
			Cursor cursor = context.getContentResolver().query(
					Uri.withAppendedPath(Uri.parse("content://sms"), "inbox"), 
					new String[] { "_id" }, 
					"read=0", 
					null, 
					null
				);
			
			if (cursor != null) {
				try {
					count = cursor.getCount();
				} finally {
					cursor.close();
				}
			}
		}
		catch (java.lang.IllegalStateException e) {
			Log.d(MetaWatch.TAG, "Failed to query SMS content provider");
		}
		return count;
	}
	
	public static int getMissedCallsCount(Context context) {
		int missed = 0;
		try {
			Cursor cursor = context.getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, null, null, null, null);
			cursor.moveToFirst();

			while (true) {
				if (cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)) == 3)
					missed += cursor.getInt(cursor.getColumnIndex(CallLog.Calls.NEW));

				if (cursor.isLast())
					break;

				cursor.moveToNext();
			}
			cursor.close();

		} catch (Exception x) {
		}
		return missed;
	}
	
	public static String readCalendar(Context context, int Return) {
		long now = new Date().getTime();
		final long CurrentTime = System.currentTimeMillis();

		try {

			String titletemp="";
			String locationtemp="";
			String MeetingTime;
			long currentremaintime;
			long begintemp=0;
			long elapsedtimetemp=0;

			currentremaintime=0;
			//location="nowhere";

			ContentResolver cr = context.getContentResolver();
			Cursor cursor = cr.query(Uri.parse("content://com.android.calendar/calendars"), new String[]{ "_id","name"}, null, null, null);
			cursor.moveToFirst();
			String[] CalNames = new String[cursor.getCount()];
			int[] CalIds = new int[cursor.getCount()];
			for (int i = 0; i < CalNames.length; i++) {
				CalIds[i] = cursor.getInt(0);
				CalNames[i] = cursor.getString(1);
				cursor.moveToNext();
			}
			cursor.close();
			Uri.Builder builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();

			ContentUris.appendId(builder, now );
			ContentUris.appendId(builder, now + DateUtils.DAY_IN_MILLIS);	        
			Cursor eventCursor = cr.query(builder.build(),
					new String[] { "event_id", "begin", "end", "allDay"}, null, null, "startDay ASC, startMinute ASC");
			// For a full list of available columns see http://tinyurl.com/yfbg76w
			MeetingTime="None";
			while (eventCursor.moveToNext()) {
				if ((eventCursor.getLong(1) > (CurrentTime+(1000*60*1.2))) &&(eventCursor.getString(3).equals("0"))){
					String uid2 = eventCursor.getString(0);	
					Uri CALENDAR_URI = Uri.parse("content://com.android.calendar/events/" + uid2);
					Log.d(MetaWatch.TAG,"CalendarService.GetData(): Calendar URI: "+ CALENDAR_URI);
					Cursor c = cr.query(CALENDAR_URI,new String[] { "title", "eventLocation", "description",}, null, null, null); 
					Log.d(MetaWatch.TAG,"CalendarService.GetData(): Calendar cursor: "+ c.getCount());
					if (c.moveToFirst())
					{	
						//Log.d(Constants.LOG_TAG,"CalendarService.GetData(): Calendar title: "+ c.getString(c.getColumnIndex("title")));
						//Log.d(Constants.LOG_TAG,"CalendarService.GetData(): Calendar location: "+ c.getString(c.getColumnIndex("eventLocation")));
						titletemp = c.getString(c.getColumnIndex("title"));
						locationtemp = c.getString(c.getColumnIndex("eventLocation"));    
					}

					c.close();
					begintemp = eventCursor.getLong(1);	
					MeetingTime = new SimpleDateFormat("HH:mm").format(begintemp);


					elapsedtimetemp = (begintemp-CurrentTime);

					Log.d(MetaWatch.TAG,"CalendarService.GetData(): Next Meeting time : "+ MeetingTime);
					Log.d(MetaWatch.TAG,"CalendarService.GetData(): Next Meeting Title : " + titletemp);
					Log.d(MetaWatch.TAG,"CalendarService.GetData(): Next Meeting Location : " + locationtemp);


					if (currentremaintime != 0) {
						if (currentremaintime > elapsedtimetemp){
							currentremaintime = elapsedtimetemp;
							Meeting_Title = titletemp;
							Meeting_Location = locationtemp;
						}
					}
					else
					{
						currentremaintime = elapsedtimetemp;
						Meeting_Title = titletemp;
						Meeting_Location = locationtemp;

					}

					break;
				}
			}   
			eventCursor.close();


			/*** Schedule ending notification ***/
			if (!MeetingTime.equals("None")){

				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MILLISECOND, (int)currentremaintime);
				Intent intent = new Intent("org.metawatch.manager.UPDATE_CALENDAR");

				intent.putExtra("Calendar", titletemp);
				// In reality, you would want to have a static variable for the request code instead of 192837
				PendingIntent sender = PendingIntent.getBroadcast(context, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				// Get the AlarmManager service
				AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
				Log.d(MetaWatch.TAG,"CalendarService: Next Meeting alarm time : "+ cal);
			}



			if (Return==1){
				return String.valueOf(currentremaintime);
			}
			else{
				return MeetingTime;
			}
		}
		catch(Exception x) {
			Log.d(MetaWatch.TAG, "Utils.readCalendar(): caught exception: " + x.toString());
			return "None";
		}

	}


	public static int getUnreadGmailCount(Context context, String account, String label) {
		Log.d(MetaWatch.TAG, "Utils.getUnreadGmailCount(): account='"+account+"' label='"+label+"'");
		try {
			int nameColumn = 0;

			Cursor c = context.getContentResolver().query(Uri.parse("content://gmail-ls/labels/" + account), null, null, null, null);
			c.moveToFirst();

			for (int i = 0; i < c.getColumnCount(); i++)
				if (c.getColumnName(i).equals("canonicalName")) {
					nameColumn = i;
					break;
				}

			while (true) {
				if (c.getString(nameColumn).equals(label))
					for (int i = 0; i < c.getColumnCount(); i++) {
						if (c.getColumnName(i).equals("numUnreadConversations")) {
							int count = Integer.parseInt(c.getString(i));
							Log.d(MetaWatch.TAG,
									"Utils.getUnreadGmailCount(): found count, returning " + count);
							return count;
						}
					}

				c.moveToNext();

				if (c.isLast()) {
					break;
				}
			}
		} catch (Exception x) {
			Log.d(MetaWatch.TAG, "Utils.getUnreadGmailCount(): caught exception: " + x.toString());
		}

		Log.d(MetaWatch.TAG, "Utils.getUnreadGmailCount(): couldn't find count, returning 0.");
		return 0;
	}
	
	public static String getGoogleAccountName(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccounts();
		int count = accounts.length;
		Account account = null;

		for (int i = 0; i < count; i++) {
			account = accounts[i];
			if (account.type.equals("com.google")) {
				return account.name;
			}
		}
		return "";
	}
	
	static final Uri k9AccountsUri = Uri.parse("content://com.fsck.k9.messageprovider/accounts/");
	static final String k9UnreadUri = "content://com.fsck.k9.messageprovider/account_unread/";
	
	private static int k9UnreadCount = 0;	
	private static long k9LastRefresh = 0;
	
	public static int getUnreadK9Count(Context context) {
		long time = System.currentTimeMillis();
		if(time - k9LastRefresh > 1*60*1000)
			refreshUnreadK9Count(context);
		
		return k9UnreadCount;
	}
	
	private static int getUnreadK9Count(Context context, int accountNumber) {
		try {
			Cursor cur = context.getContentResolver().query(Uri.parse(k9UnreadUri+"/"+accountNumber+"/"), null, null, null, null);
		    if (cur!=null) {
		    	Log.d(MetaWatch.TAG, "k9: "+cur.getCount()+ " unread rows returned");
		    			    	
		    	if (cur.getCount()>0) {
			    	cur.moveToFirst();
			    	int unread = 0;
			    	int nameIndex = cur.getColumnIndex("accountName");
			    	int unreadIndex = cur.getColumnIndex("unread");
			    	do {
			    		String acct = cur.getString(nameIndex);
			    		int unreadForAcct = cur.getInt(unreadIndex);
			    		Log.d(MetaWatch.TAG, "k9: "+acct+" - "+unreadForAcct+" unread");
			    		unread += unreadForAcct;
			    	} while (cur.moveToNext());
				    cur.close();
				    return unread;
		    	}
		    }
		    else {
		    	Log.d(MetaWatch.TAG, "Failed to query k9 unread contentprovider.");
		    }
		}
		catch (IllegalStateException e) {
			Log.d(MetaWatch.TAG, "k-9 unread uri unknown.");
		}
		return 0;
	}
	
	public static void refreshUnreadK9Count(Context context) {		
		int accounts = getK9AccountCount(context);
		if (accounts>0) {
			int count = 0;
			for (int acct=0; acct<accounts; ++acct) {
				count += getUnreadK9Count(context, acct);
			}
			k9UnreadCount = count;
			k9LastRefresh = System.currentTimeMillis();
		}	
	}
	
	public static int getK9AccountCount(Context context) {
		try {
			Cursor cur = context.getContentResolver().query(k9AccountsUri, null, null, null, null);
		    if (cur!=null) {
		    	Log.d(MetaWatch.TAG, "k9: "+cur.getCount()+ " account rows returned");

		    	int count = cur.getCount();
		    	
		    	cur.close();
		    	
		    	return count;
		    }
		    else {
		    	Log.d(MetaWatch.TAG, "Failed to query k9 unread contentprovider.");
		    }
		}
		catch (IllegalStateException e) {
			Log.d(MetaWatch.TAG, "k-9 accounts uri unknown.");
		}
		catch (java.lang.SecurityException e) {
			Log.d(MetaWatch.TAG, "Permissions failure accessing k-9 databases");
		}
		return 0;

	}
	
	public static Bitmap loadBitmapFromAssets(Context context, String path) {
		try {
			InputStream inputStream = context.getAssets().open(path);
	        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
	        inputStream.close();
	        //Log.d(MetaWatch.TAG, "ok");
	        return bitmap;
		} catch (IOException e) {
			//Log.d(MetaWatch.TAG, e.toString());
			return null;
		}
	}
	/*
	public static Bitmap loadBitmapFromPath(Context context, String path) {
			return BitmapFactory.decodeFile(path);
	}
	*/
	
	public static Bitmap ditherTo1bit(Bitmap input, boolean inverted) {
	
		Bitmap output = input.copy(Config.RGB_565, true);
		
		double[][] pixels = new double[input.getWidth()][input.getHeight()];
		
		final int w=input.getWidth();
		final int h=input.getHeight();
		
		for(int y=0; y<h; ++y) {
		   for(int x=0; x<w; ++x) {
			   int col = input.getPixel(x, y);
			   
			   double R = ((col >> 16) & 0xff)/256.0; 
		       double G = ((col >> 8) & 0xff)/256.0;
		       double B = (col & 0xff)/256.0;
		        
		       pixels[x][y] = ( (0.3*R) + (0.59*G) + (0.11*B) );		        
		   }
		}
		
		for(int y=0; y<h; ++y) {
			   for(int x=0; x<w; ++x) {
				   double oldpixel = pixels[x][y];
				   double newpixel = oldpixel<0.5 ? 0 : 1;
				   	   
				   pixels[x][y] = newpixel;
				   double quant_error = oldpixel - newpixel;
				   if(x<w-1)
					   pixels[x+1][y] += 7.0/16.0 * quant_error;
				   if(x>0 && y<h-1)
					   pixels[x-1][y+1] += 3.0/16.0 * quant_error;
				   if(y<h-1)
					   pixels[x][y+1] += 5.0/16.0 * quant_error;
				   if(x<w-1 && y<h-1)
					   pixels[x+1][y+1] += 1.0/16.0 * quant_error;
			
				   int col = 0;
				   if (inverted)
					   col = newpixel > 0.5 ? 0xff000000 : 0xffffffff;   
				   else
					   col = newpixel > 0.5 ? 0xffffffff : 0xff000000;
				   output.setPixel(x, y, col);
			   
			   }
		}

		return output;
	}
	
	public static Bitmap resize(Bitmap bm, int newHeight, int newWidth) {

		int width = bm.getWidth();
		int height = bm.getHeight();

		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
		return resizedBitmap;
	}
	
	public static void dumpBitmapToSdCard(Bitmap bitmap, String filename) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(filename);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public static String getVersion(Context context) {		
		try {
			PackageManager packageManager = context.getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionName;
		} catch (NameNotFoundException e) {
		}
		return "unknown";
	}
	
	public static boolean isGmailAccessSupported(Context context) {
		
		
		try {
			PackageManager packageManager = context.getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo("com.google.android.gm", 0);
			// check for Gmail version earlier than v2.3.5 (169)
			if (packageInfo.versionCode < 169)
					return true;			
			
		} catch (NameNotFoundException e) {
		}
		
		
		return false;
	}
	
	public static String ReadInputStream(InputStream in) throws IOException {
		StringBuffer stream = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			stream.append(new String(b, 0, n));
		}
		return stream.toString();
	}
	
	public static void drawWrappedText(String text, Canvas canvas, int x, int y, int width, TextPaint paint, android.text.Layout.Alignment align) {
		canvas.save();
		StaticLayout layout = new StaticLayout(text, paint, width, align, 1.0f, 0, false);
		canvas.translate(x, y); //position the text
		layout.draw(canvas);
		canvas.restore();	
	}
	
	public static void drawOutlinedText(String text, Canvas canvas, int x, int y, TextPaint col, TextPaint outline) {
		canvas.drawText(text, x+1, y, outline);
		canvas.drawText(text, x-1, y, outline);
		canvas.drawText(text, x, y+1, outline);
		canvas.drawText(text, x, y-1, outline);
	
		canvas.drawText(text, x, y, col);
	}
	
	public static void drawWrappedOutlinedText(String text, Canvas canvas, int x, int y, int width, TextPaint col, TextPaint outline, android.text.Layout.Alignment align) {
		drawWrappedText(text, canvas, x-1, y, width, outline, align);
		drawWrappedText(text, canvas, x+1, y, width, outline, align);
		drawWrappedText(text, canvas, x, y-1, width, outline, align);
		drawWrappedText(text, canvas, x, y+1, width, outline, align);
		
		drawWrappedText(text, canvas, x, y, width, col, align);
	}
	
	public static Bitmap DrawIconCountWidget(Context context, int width, int height, Bitmap icon, int count, TextPaint textPaint) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		
		canvas.drawBitmap(icon, 0, 3, null);
		canvas.drawText(Integer.toString(count), 12, 29, textPaint);
		
		return bitmap;
	}
	public static Bitmap DrawIconStringWidget(Context context, int width, int height, Bitmap icon, String count, TextPaint textPaint) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		
		canvas.drawBitmap(icon, 0, 3, null);
		canvas.drawText(count, 12, 29, textPaint);
		
		return bitmap;
	}

}
