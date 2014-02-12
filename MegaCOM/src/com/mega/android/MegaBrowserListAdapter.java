package com.mega.android;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

public class MegaBrowserListAdapter extends BaseAdapter {
	
	Context context;
	List<ItemFileBrowser> rowItems;
	int positionClicked;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;


	public MegaBrowserListAdapter(Context _context, List<ItemFileBrowser> _items) {
		this.context = _context;
		this.rowItems = _items;
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		
		Iterator<ItemFileBrowser> it = rowItems.iterator();
		while (it.hasNext()){
			ItemFileBrowser item = it.next();
			imageIds.add(item.getImageId());
			names.add(item.getName());
		}
	}
	
	/*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        TextView textViewFileName;
        TextView textViewFileSize;
        TextView textViewUpdated;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        ImageView arrowSelection;
        RelativeLayout optionsLayout;
        ImageButton optionOpen;
        ImageButton optionProperties;
        ImageButton optionDownload;
        ImageButton optionDelete;
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
			convertView = inflater.inflate(R.layout.item_file_list, parent, false);
			holder = new ViewHolder();
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_item_layout);
			holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_list_filename);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_list_filesize);
			holder.textViewUpdated = (TextView) convertView.findViewById(R.id.file_list_updated);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.file_list_three_dots);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_options);
			holder.optionOpen = (ImageButton) convertView.findViewById(R.id.file_list_option_open);
			holder.optionOpen.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionProperties = (ImageButton) convertView.findViewById(R.id.file_list_option_properties);
			holder.optionProperties.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDownload = (ImageButton) convertView.findViewById(R.id.file_list_option_download);
			holder.optionDownload.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDelete = (ImageButton) convertView.findViewById(R.id.file_list_option_delete);
			holder.optionDelete.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.file_list_arrow_selection);
			holder.arrowSelection.setVisibility(View.GONE);
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		ItemFileBrowser rowItem = (ItemFileBrowser) getItem(position);
		
		holder.textViewFileName.setText(rowItem.getName());
		holder.textViewFileSize.setText("100 KB");
		holder.textViewUpdated.setText("Updated: 1 week ago");
		holder.imageView.setImageResource(rowItem.getImageId());
		
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
		
		holder.optionOpen.setTag(holder);
		holder.optionOpen.setOnClickListener(
				new OnClickListener() {
			
					@Override
					public void onClick(View v) {
						Intent i = new Intent(context, FullScreenImageViewer.class);
						i.putExtra("position", _position);
						i.putExtra("names", names);
						i.putExtra("imageIds", imageIds);
						context.startActivity(i);	
						positionClicked = -1;
						notifyDataSetChanged();
					}
				});
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(
					new OnClickListener() {
						public void onClick(View v) {
							Intent i = new Intent(context, FilePropertiesActivity.class);
							i.putExtra("imageId", rowItems.get(_position).getImageId());
							i.putExtra("name", rowItems.get(_position).getName());
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
