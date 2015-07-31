package nz.mega.android.lollipop;

import java.util.List;

import nz.mega.android.R;
import nz.mega.android.utils.Util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;


public class NavigationDrawerLollipopAdapter extends BaseAdapter{

	Context context;
	List<String> items;
	
	int positionClicked;
	
	public class ViewHolderNavigationDrawer{
		LinearLayout layout;
		TextView text;
	}
	
	public NavigationDrawerLollipopAdapter(Context context, List<String> items){
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
		holder.layout.setBackgroundColor(context.getResources().getColor(R.color.white));
		
		switch(position){
			case 0:				
				if (positionClicked == position){
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.cloud_drive_red,0,0,0);	
				}
				else{
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.cloud_drive_grey,0,0,0);	
				}
				break;			
			case 1:
				if (positionClicked == position){
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.saved_for_offline_red,0,0,0);	
				}
				else{
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.saved_for_offline_grey,0,0,0);	
				}
				break;
			case 2:
				if (positionClicked == position){
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.camera_uploads_red,0,0,0);	
				}
				else{
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.camera_uploads_grey,0,0,0);	
				}
				break;
			case 3:
				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_inbox,0,0,0);
				break;
			case 4:
				if (positionClicked == position){
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.shared_items_red,0,0,0);	
				}
				else{
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.shared_items_grey,0,0,0);	
				}
				break;
			case 5:
				if (positionClicked == position){
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.contacts_red,0,0,0);	
				}
				else{
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.contacts_grey,0,0,0);	
				}
				break;
			case 6:
				if (positionClicked == position){
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.settings_red,0,0,0);	
				}
				else{
					holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.settings_grey,0,0,0);	
				}
//			case 7:
//				holder.text.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_my_account,0,0,0);
				break;
		}
		
		if (positionClicked == position){
			holder.text.setTextColor(context.getResources().getColor(R.color.pressed_mega));
		}
//		else{
//			holder.layout.setBackgroundColor(context.getResources().getColor(R.color.navigation_drawer_background));
//		}
		
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
