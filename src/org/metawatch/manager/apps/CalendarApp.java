package org.metawatch.manager.apps;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.text.TextPaint;

public class CalendarApp extends ApplicationBase {

	public final static String APP_ID = "org.metawatch.manager.apps.CalendarApp";
	
	static AppData appData = new AppData() {{
		id = APP_ID;
		name = "Calendar";
	
		supportsDigital = true;
		supportsAnalog = true;
	}};
	
	@Override
	public AppData getInfo() {
		return appData;
	}

	@Override
	public void activate(Context context, int watchType) {
	}

	@Override
	public void deactivate(Context context, int watchType) {
	}
	
	public int getColumn(int dow)
	{
		switch(dow) {
		case Calendar.MONDAY:
			return 5;
		
		case Calendar.TUESDAY:
			return 18;
			
		case Calendar.WEDNESDAY:
			return 31;
			
		case Calendar.THURSDAY:
			return 44;
			
		case Calendar.FRIDAY:
			return 57;
			
		case Calendar.SATURDAY:
			return 70;
			
		case Calendar.SUNDAY:
			return 83;
		}
		
		return 0;
	}

	@Override
	public Bitmap update(Context context, boolean preview, int watchType) {
		Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);	
		
		canvas.drawBitmap(Utils.getBitmap(context, "calendar_app_month.png"), 0, 0, null);	
		
		TextPaint paintSmall = new TextPaint();
		paintSmall.setColor(Color.WHITE);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintSmall.setTextAlign(Align.LEFT);
		
		TextPaint paintSmallNumerals = new TextPaint();
		paintSmallNumerals.setColor(Color.BLACK);
		paintSmallNumerals.setTextSize(FontCache.instance(context).SmallNumerals.size);
		paintSmallNumerals.setTypeface(FontCache.instance(context).SmallNumerals.face);
		paintSmallNumerals.setTextAlign(Align.LEFT);
		
		Calendar cal = Calendar.getInstance();
		
		SimpleDateFormat df = new SimpleDateFormat("MMMM yyyy");
		canvas.drawText(df.format(cal.getTime()), 4, 8, paintSmall);
		
		final int today = cal.get(Calendar.DAY_OF_MONTH);
		
		int yPos = 18;
		final int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		for(int day=1; day<=days; ++day) {
			cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), day);
					
			final int dow = cal.get(Calendar.DAY_OF_WEEK);
			final int xPos = getColumn(dow);
		
			if (day==today) {
				canvas.drawBitmap(Utils.getBitmap(context, "calendar_app_today.png"), xPos-4, yPos-4, null);
			}
			
			canvas.drawText(""+day, xPos, yPos+5, paintSmallNumerals);
			
			if (dow==Calendar.SUNDAY)
				yPos += 13;
		}
		
		drawDigitalAppSwitchIcon(context, canvas, preview);
		
		return bitmap;
	}

	@Override
	public int buttonPressed(Context context, int id) {
		return BUTTON_NOT_USED;
	}

}
