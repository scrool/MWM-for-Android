package org.metawatch.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.metawatch.communityedition.R;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;
import org.metawatch.manager.widgets.WidgetManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WidgetSetup extends Activity {
	
	private class WidgetListAdaptor extends BaseExpandableListAdapter {
		
		private Map<String,WidgetData> widgetMap;
		private List<List<String>> groups;
		private LayoutInflater mInflater;
		
		public void init(Context context) {
			
			mInflater = LayoutInflater.from(context);
			
			widgetMap = WidgetManager.getCachedWidgets(context, null);
					
			ArrayList<String> rows = new ArrayList<String>(Arrays.asList(MetaWatchService.getWidgets(context).split("\\|")));
			groups = new ArrayList<List<String>>();
			
			for(String line : rows) {
				String[] widgets = (line).split(",");
				List<String> list = new ArrayList<String>(Arrays.asList(widgets));
				groups.add(list);
			}
			
		}
		
		public void set(int groupPosition, int childPosition, String value) {
			while (groupPosition >= groups.size()) {
				groups.add(new ArrayList<String>());
			}
			
			while (childPosition >= groups.get(groupPosition).size()) {
				groups.get(groupPosition).add("");
			}
			
			groups.get(groupPosition).set(childPosition, value);
			
			tidy();
		}

		private void tidy() {
			// Tidy up any empty groups
			for(List<String> row : groups) {
				boolean isEmpty = true;
				for(String entry : row) {
					if( !Utils.stringIsEmpty(entry) ) {
						isEmpty = false;
					}
				}
				if( isEmpty ) {
					row.clear();
				}
			}
		}
		
		public String get() {
	        StringBuilder out = new StringBuilder();
	    	for(List<String> row : groups) {
	    		if(out.length()>0)
	    			out.append("|");
	    		
	    		StringBuilder line = new StringBuilder();
	    		for(String id : row) {
	        		if(id!="") {
		        		if(line.length()>0)
		        			line.append(",");
		        		
		        		line.append(id);
	        		}
	    		}
	    		
	    		out.append(line);
	    	}
	    	
	    	return out.toString();
		}
		
		public Object getChild(int groupPosition, int childPosition) {
			if(groupPosition>=groups.size()) {
				return "";
			} else if( childPosition>=groups.get(groupPosition).size()) {
				return "";
			}
			return groups.get(groupPosition).get(childPosition);
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			
			if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
			}
			
			TextView label = (TextView) convertView.findViewById(R.id.text);
			ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
			
			String id = (String) getChild(groupPosition, childPosition);
			String name = id;
			
			Bitmap bmp = null;
			if(Utils.stringIsEmpty(id)) {
				id = "";
				name = "<Add Widget>";
				icon.setVisibility(View.GONE);
			} else {
			
				icon.setVisibility(View.VISIBLE);
	            if(widgetMap.containsKey(id)) {
	            	name = widgetMap.get(id).description;
	            	bmp = widgetMap.get(id).bitmap;        	
	            } else {
	            	bmp = Bitmap.createBitmap(1, 1, Config.ALPHA_8);
	            }
            	
            }
			
			label.setText(name);
			if (bmp!=null) {
				icon.setImageBitmap( Bitmap.createScaledBitmap(bmp, bmp.getWidth()*2, bmp.getHeight()*2, false) );
			}
			
			return convertView;
		}

		public int getChildrenCount(int groupPosition) {
			return groupPosition >= groups.size() ? 1 : groups.get(groupPosition).size()+1;
		}

		public Object getGroup(int groupPosition) {
			return groupPosition >= groups.size() ? null : groups.get(groupPosition);
		}

		public int getGroupCount() {
			return groups.size()+1;
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
		
		private String getGroupName(int group) {
	        StringBuilder nameSb = new StringBuilder();
	        nameSb.append("Row ");
	        nameSb.append(group+1);
	        return nameSb.toString();
		}
		
		private String getGroupLabel(int group) {
			
			int widgetCount = 0;
			if(group<groups.size()) {
				for(String entry : groups.get(group)) {
					if(!Utils.stringIsEmpty(entry))
						widgetCount++;
				}
			}
			
			StringBuilder nameSb = new StringBuilder();
	        if(widgetCount==0) {
	        	nameSb.append("empty");
	        }
	        else if(widgetCount==1) {
	        	nameSb.append("1 widget");
	        }
	        else {
	        	nameSb.append(widgetCount);
	        	nameSb.append(" widgets");
	        }
	        
	        return nameSb.toString();
		}

		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_expandable_list_item_2, null);
			}
			
			TextView line1 = (TextView) convertView.findViewById(android.R.id.text1);
			TextView line2 = (TextView) convertView.findViewById(android.R.id.text2);
			
			line1.setText(getGroupName(groupPosition));
			line2.setText(getGroupLabel(groupPosition));
			
			return convertView;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
	}
	
	private ExpandableListView widgetList;
	private WidgetListAdaptor adapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.widget_setup);  
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        setContentView(R.layout.widget_setup);
        adapter = null;
        onStart();
    }
    
	@Override
	protected void onStart() {
		super.onStart();
		
		if(adapter!=null)
			return;
			
		widgetList = (ExpandableListView) findViewById(R.id.widgetList);		
		widgetList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				Intent i = new Intent(getApplicationContext(), WidgetPicker.class);
				i.putExtra("groupPosition", groupPosition);
				i.putExtra("childPosition", childPosition);
				startActivityForResult(i,  1);
				return false;
			}
		});
				
		adapter = new WidgetListAdaptor();
		adapter.init(this);
		
	    widgetList.setAdapter(adapter);
		
		refreshPreview();
	}
	
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	if (resultCode == Activity.RESULT_OK) {      	  
        	String id = data.getStringExtra("selectedWidget");
        	int groupPosition = data.getIntExtra("groupPosition", -1);
        	int childPosition = data.getIntExtra("childPosition", -1);
        	
        	if(groupPosition>-1 && childPosition>-1) {
        		
        		adapter.set(groupPosition, childPosition, id);
        		
        		adapter.notifyDataSetChanged();
            	storeWidgetLayout();
            	refreshPreview();
            	Idle.updateIdle(this, true);
    	        if(MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
    	        	Idle.sendOledIdle(this);
    	        }
        	
        	}
        }
    }
    
    private void refreshPreview() {
    	if (Preferences.logging) Log.d(MetaWatch.TAG, "WidgetSetup.refreshPreview() start");
    	if (!Idle.isBusy())
    		Idle.updateIdlePages(this, true);
    	
    	LinearLayout ll = (LinearLayout) findViewById(R.id.idlePreviews);
    	
    	ll.removeAllViews();
    	  	
    	int pages = Idle.numPages();
    	for(int i=0; i<pages; ++i) {
    		Bitmap bmp = Idle.createIdle(this, true, i);;

    		if (bmp!=null) {
    			
    			int backCol = Color.LTGRAY;
    			int viewId = (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) 
    					? R.layout.idle_screen_preview_oled
    				    : R.layout.idle_screen_preview;
    			
        		if(Preferences.invertLCD || MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
        			bmp = Utils.invertBitmap(bmp);
        			backCol = 0xff111111;
        		}
        		
        		bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth()*2, bmp.getHeight()*2, false);
        		    			
	    		LayoutInflater factory = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	
	    		final Context context = this;
	    		View v = factory.inflate(viewId, null);
	    		ImageView iv = (ImageView)v.findViewById(R.id.image);
	    		iv.setImageBitmap(bmp);
	    		iv.setClickable(true);
	    		iv.setBackgroundColor(backCol);
	    		iv.setTag(i);
	    		iv.setOnClickListener(new OnClickListener() {
	    		    //@Override
	    		    public void onClick(View v) {
	    		    	Integer page = (Integer)v.getTag();
	    		        Idle.toPage(context, page);
	    		        Idle.updateIdle(v.getContext(), true);
	    		        
	    		        if(MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
	    		        	Idle.sendOledIdle(v.getContext());
	    		        }
	    		    }
	    		});
	    		ll.addView(v);
    		}
    	}
    	if (Preferences.logging) Log.d(MetaWatch.TAG, "WidgetSetup.refreshPreview() end");
    }
    
    private void storeWidgetLayout() {
    	MetaWatchService.saveWidgets(this, adapter.get());
    }
}
