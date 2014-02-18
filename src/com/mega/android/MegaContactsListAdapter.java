package com.mega.android;

import java.util.List;

import com.mega.components.RoundedImageView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.opengl.Visibility;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MegaContactsListAdapter extends BaseAdapter {
	
	Context context;
	List<ItemContact> rowItems;
	int positionClicked;
	
	public MegaContactsListAdapter(Context _context, List<ItemContact> _items) {
		this.context = _context;
		this.rowItems = _items;
		this.positionClicked = -1;
	}
	
	/*private view holder class*/
    private class ViewHolder {
    	RoundedImageView imageView;
//        ImageView imageView;
        TextView textViewContactName;
        TextView textViewContent;
        ImageView statusImageView;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        ImageView arrowSelection;
        RelativeLayout optionsLayout;
        ImageButton optionProperties;
        ImageButton optionSend;
        ImageButton optionRemove;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final int _position = position;
		
		ViewHolder holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_contact_list, parent, false);
			holder = new ViewHolder();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.contact_list_item_layout);
			holder.imageView = (RoundedImageView) convertView.findViewById(R.id.contact_list_thumbnail);	        
			holder.textViewContactName = (TextView) convertView.findViewById(R.id.contact_list_name);
			holder.statusImageView = (ImageView) convertView.findViewById(R.id.contact_list_status_dot);
			holder.textViewContent = (TextView) convertView.findViewById(R.id.contact_list_content);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.contact_list_three_dots);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.contact_list_options);
			holder.optionProperties = (ImageButton) convertView.findViewById(R.id.contact_list_option_properties);
			holder.optionProperties.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionSend = (ImageButton) convertView.findViewById(R.id.contact_list_option_send);
			holder.optionSend.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionRemove = (ImageButton) convertView.findViewById(R.id.contact_list_option_remove);
			holder.optionRemove.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((50*scaleW), outMetrics), 0);
			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.contact_list_arrow_selection);
			holder.arrowSelection.setVisibility(View.GONE);
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		ItemContact rowItem = (ItemContact) getItem(position);
		
		holder.textViewContactName.setText(rowItem.getName());
		holder.textViewContent.setText("5 Folders, 10 files");
		holder.imageView.setImageResource(rowItem.getImageId());
        
        if (position < 2){
        	holder.statusImageView.setImageResource(R.drawable.contact_green_dot);
        }
        else if (position == 2){
        	holder.statusImageView.setImageResource(R.drawable.contact_yellow_dot);
        }
        else if (position == 3){
        	holder.statusImageView.setImageResource(R.drawable.contact_red_dot);
        }
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(
					new OnClickListener() {
						public void onClick(View v) {
							if (positionClicked == -1){
								positionClicked = _position;
								notifyDataSetChanged();
							}
							else{
								if (positionClicked == _position){
									positionClicked = -1;
									notifyDataSetChanged();
								}
								else{
									positionClicked = _position;
									notifyDataSetChanged();
								}
							}
						}
					});
		
		if (positionClicked != -1){
			if (positionClicked == position){
				holder.arrowSelection.setVisibility(View.VISIBLE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_grey);
				ListView list = (ListView) parent;
				list.smoothScrollToPosition(_position);
			}
			else{
				holder.arrowSelection.setVisibility(View.GONE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
			}
		}
		else{
			holder.arrowSelection.setVisibility(View.GONE);
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
		}
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(
					new OnClickListener() {
						public void onClick(View v) {
							Intent i = new Intent(context, ContactPropertiesActivity.class);
							i.putExtra("imageId", rowItems.get(_position).getImageId());
							i.putExtra("name", rowItems.get(_position).getName());
							i.putExtra("position", _position);
							context.startActivity(i);							
							positionClicked = -1;
							notifyDataSetChanged();
						}
					});
		
		return convertView;
	}

	@Override
    public int getCount() {
        return rowItems.size();
    }
 
    @Override
    public Object getItem(int position) {
        return rowItems.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return rowItems.indexOf(getItem(position));
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }
}
