package com.mega.android;

import java.util.List;

import com.mega.android.utils.Util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NavigationDrawerAdapter extends BaseAdapter{

	Context context;
	List<String> items;
	
	int positionClicked;
	
	public class ViewHolderNavigationDrawer{
		LinearLayout layout;
		TextView text;
	}
	
	public NavigationDrawerAdapter(Context context, List<String> items){
		this.context = context;
		this.items = items;
		
		this.positionClicked = 0;
	}
	
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
    public Object getItem(int position) {
        return items.get(position);
    }

	@Override
    public long getItemId(int position) {
        return position;
    }  

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolderNavigationDrawer holder = null;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
			holder = new ViewHolderNavigationDrawer();
			holder.layout = (LinearLayout) convertView.findViewById(R.id.drawer_list_layout);
			holder.text = (TextView) convertView.findViewById(R.id.drawer_list_text);
			
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderNavigationDrawer) convertView.getTag();
		}
		
		holder.text.setText((String)getItem(position));
		
		switch(position){
			case 0:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_cloud,0,0,0);
				break;
			case 1:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_saved,0,0,0);
				break;
			case 2:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_shared,0,0,0);
				break;
			case 3:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_contacts,0,0,0);
				break;
			case 4:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_photosync,0,0,0);
				break;
			case 5:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_transfers,0,0,0);
				break;
			case 6:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_account,0,0,0);
				break;
		}
		
		if (positionClicked == position){
			holder.layout.setBackgroundColor(context.getResources().getColor(R.color.navigation_drawer_background_selected));
		}
		else{
			holder.layout.setBackgroundColor(context.getResources().getColor(R.color.navigation_drawer_background));
		}
		
		return convertView;
	}
	
	public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    	notifyDataSetChanged();
    }
    
    private static void log(String log) {
		Util.log("NavigationDrawerAdapter", log);
	}

}
