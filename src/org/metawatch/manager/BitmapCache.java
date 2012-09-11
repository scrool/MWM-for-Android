package org.metawatch.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapCache {
	
	public static class ThemeData {
		public ThemeData( String name ) {
			themeName = name;
		}
		
		public String themeName = "";
		public long timeStamp = 0;

		protected HashMap<String, Object> data = new HashMap<String,Object>();
		
		public Object get(String key) {
			return data.containsKey(key) ? data.get(key) : null;
		}
		
		public Bitmap getBitmap(String key) {
			Object obj = data.get(key);
			return obj instanceof Bitmap ? (Bitmap) obj : null;
		}
		
		public Properties getProperties(String key) {
			Object obj = data.get(key);
			return obj instanceof Properties ? (Properties) obj : null;
		}
		
		public void readTheme(File themeFile) {
			
			HashMap<String, Object> newCache = new HashMap<String,Object>();
			
			FileInputStream fis = null;
			ZipInputStream zis = null;
			
			timeStamp = themeFile.lastModified();
			
			try {
			
			    fis = new FileInputStream(themeFile);
				zis = new ZipInputStream(fis);
						
				ZipEntry ze = zis.getNextEntry();
	            while (ze != null) 
	            { 
	                String entryName = ze.getName();
	                
	            	final int size = (int) ze.getSize();
	            	
	            	// Need to copy into a buffer rather than decoding directly from zis
	            	// as BitmapFactory seems unable to read a .bmp file from a
	            	// ZipInputStream :-\
	            	byte[] buffer = new byte[size];
	            	int offset = 0;
	            	int read=0;
	            	do {
	            		read = zis.read(buffer, offset, size-offset);
	            		offset += read;
	            	} while(read>0);
	            	
	            	if( entryName.toLowerCase().endsWith(".bmp") || entryName.toLowerCase().endsWith(".png")) {	            	
		            	Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, size);
		            	if (bitmap !=null) {
		            		//if (Preferences.logging) Log.d(MetaWatch.TAG, "Loaded "+ze.getName());
		            		newCache.put(entryName, bitmap);
		            	} else {
		            		if (Preferences.logging) Log.d(MetaWatch.TAG, "Failed to load "+ze.getName());
		            	}           	
	            	}
	            	else if( entryName.toLowerCase().endsWith(".xml")) {
	            		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
	            		Properties properties = new Properties();
	            		properties.loadFromXML(byteArrayInputStream);
	            		
	            		newCache.put(entryName, properties);
	            	}

	                zis.closeEntry();
	                ze = zis.getNextEntry();

	            }
				
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			finally {
				try {
					if (zis!=null)
						zis.close();
					if (fis!=null)
						fis.close();
				} catch (IOException e) {
				}
			}
			
			data = newCache;
		   
		}

		public Bitmap getBanner() {
			return getBitmap("theme_banner.png");
		}
		
		public Properties getThemeProperties() {
			return getProperties("theme.xml");		
		}
	
	}
	
	private static class DefaultTheme extends ThemeData {
		public DefaultTheme(Context context) {
			super("");
			this.context = context;
		}
		
		Context context;
		
		@Override
		public void readTheme(File themeFile) {		
		}
		
		@Override
		public Bitmap getBitmap(String key) {
			Bitmap bitmap = super.getBitmap(key);
			if (bitmap!=null) return bitmap;
			
			bitmap = loadBitmapFromAssets(context, key);
			
			if (bitmap!=null) {
				data.put(key, bitmap);
				return bitmap;
			}
			return null;
		}
		
		@Override
		public Properties getProperties(String key) {
			Properties properties = super.getProperties(key);
			if (properties!=null) return properties;
			
			properties = loadPropertiesFromAssets(context, key);
			
			if (properties!=null) {
				data.put(key, properties);
				return properties;
			}
			return null;
		}
		
		private Bitmap loadBitmapFromAssets(Context context, String path) {
			
			try {
				InputStream inputStream = context.getAssets().open(path);
		        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
		        inputStream.close();
		        return bitmap;
			} catch (IOException e) {
				return null;
			}
		}
		
		private Properties loadPropertiesFromAssets(Context context, String path) {
			
			try {
				InputStream inputStream = context.getAssets().open(path);
		        Properties properties = new Properties();
	    		properties.loadFromXML(inputStream);       
		        inputStream.close();
		        return properties;
			} catch (IOException e) {
				return null;
			}
		}
	}
	
	private static DefaultTheme internalTheme = null;
	private static ThemeData currentTheme = null;
	
	private static File getThemeFile(Context context, String themeName) {
		return new File(Utils.getExternalFilesDir(context, "Themes"), themeName+".zip");
	}
	
	public static synchronized Bitmap getBitmap(Context context, String path) {
		
		updateCache(context);
		
		Bitmap bitmap = currentTheme.getBitmap(path);
		if (bitmap!=null) return bitmap;
		
		bitmap = internalTheme.getBitmap(path);
		if (bitmap!=null) return bitmap;
	
		return null;
		
	}

	
	public static synchronized Properties getProperties(Context context, String path) {
		
		updateCache(context);

		Properties properties = currentTheme.getProperties(path);
		if (properties!=null) return properties;
		
		properties = internalTheme.getProperties(path);
		if (properties!=null) return properties;
		
		return new Properties();
	}
	
	public static Bitmap getDefaultThemeBanner(Context context) {
		return internalTheme.getBanner();
	}
	
	private static void updateCache(Context context) {
		
		if (internalTheme==null) {
			internalTheme = new DefaultTheme(context);
		}
		
		File themeFile = getThemeFile(context, Preferences.themeName);
		
		if (currentTheme == null || Preferences.themeName != currentTheme.themeName || (themeFile.lastModified() != currentTheme.timeStamp)) {
			currentTheme = loadTheme(context, Preferences.themeName, themeFile);
		}
	}
	
	public static ThemeData getInternalTheme(Context context) {
		updateCache(context);
		return internalTheme;
	}
	
	public static ThemeData loadTheme(Context context, String themeName) {
		File themeFile = getThemeFile(context, themeName);
		
		return loadTheme(context, themeName, themeFile);
	}
	
	private static ThemeData loadTheme(Context context, String themeName, File themeFile) {
		
		ThemeData theme = new ThemeData(themeName);
		
		if (themeFile.exists()) {
			theme.readTheme(themeFile);
		}
		
		return theme;
	}
	
	private static String getThemeName(String uri) {
		String filename = uri.substring( uri.lastIndexOf('/')+1, uri.length());
		return filename.substring(0, filename.lastIndexOf('.'));
	}
	
	static ProgressDialog dialog = null;
	public static void downloadAndInstallTheme(final Activity callingActivity, final String uri) {
		
		Thread thread = new Thread("ThemeInstaller") {

			
			@Override
			public void run() {
				
				OutputStream fOut = null;
				try {
		
					final Context context = callingActivity;
					
				
					callingActivity.runOnUiThread(new Runnable(){
						
						public void run() {
							dialog = ProgressDialog.show(context, "", 
									"Downloading Theme", true);
							dialog.show();
						}
					} );
					
			    	
				    // Create a URL for the desired page
				    URL url = new URL(uri);
				    
				    String themeName = getThemeName(uri);
				    File themeFile = getThemeFile(context, themeName);
				    			
				    URLConnection uc = url.openConnection();
				    int contentLength = uc.getContentLength();
				    
				    InputStream raw = uc.getInputStream();
				    InputStream in = new BufferedInputStream(raw);
				    byte[] data = new byte[contentLength];
				    int bytesRead=0;
				    int offset=0;
				    
				    while (offset<contentLength) {
				    	bytesRead = in.read(data, offset, data.length-offset);
				    	if (bytesRead==-1)
				    		break;
				    	
				    	offset += bytesRead;
				    }
				    
				    in.close();
				    
				    if (offset!=contentLength)
				    	throw new IOException();
				    
				    fOut = new BufferedOutputStream(new FileOutputStream(themeFile));
				    fOut.write(data);
				    fOut.flush();
		            fOut.close();
		            fOut = null;
		            
		        	Preferences.themeName = themeName;   	
		        	MetaWatchService.saveTheme(context, Preferences.themeName);
		            
		        	Idle.updateIdle(context, true);
		        	
		        	callingActivity.finish();
		        	
		            return;
				} catch (MalformedURLException e) {
				} catch (IOException e) {
				}
				finally {
					if (fOut!=null)
						try {
							fOut.close();
						} catch (IOException e) {
						}
					
					if (dialog!=null)
						dialog.dismiss();
					dialog = null;
				}
			}
		};
		
		thread.start();
	}
	
	
	
}
