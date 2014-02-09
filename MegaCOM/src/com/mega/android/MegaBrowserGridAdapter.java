package com.mega.android;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MegaBrowserGridAdapter extends BaseAdapter {
	
	Context context;
	List<ItemFileBrowser> rowItems;
	int positionClicked;
	
	public MegaBrowserGridAdapter(Context _context, List<ItemFileBrowser> _items) {
		this.context = _context;
		this.rowItems = _items;
		this.positionClicked = -1;
	}
	
	/*private view holder class*/
    private class ViewHolder {
        ImageButton imageView1;
        TextView textViewFileName1;
        RelativeLayout itemLayout1;
        ImageButton imageView2;
        TextView textViewFileName2;
        RelativeLayout itemLayout2;
        TextView textViewFileSize1;
        TextView textViewFileSize2;
        ImageButton imageButtonThreeDots1;
        ImageButton imageButtonThreeDots2;
        ImageView arrowSelection1;
        RelativeLayout optionsLayout1;
        ImageButton optionOpen1;
        ImageButton optionProperties1;
        ImageButton optionDownload1;
        ImageButton optionDelete1;
        ImageView arrowSelection2;
        RelativeLayout optionsLayout2;
        ImageButton optionOpen2;
        ImageButton optionProperties2;
        ImageButton optionDownload2;
        ImageButton optionDelete2;
    }
    
	ViewHolder holder = null;
	int positionG;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final int _position = position;
		positionG = position;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if ((position % 2) == 0){
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_file_grid, parent, false);
				holder = new ViewHolder();
				holder.itemLayout1 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout1);
				holder.itemLayout2 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout2);
				
				//Set width and height itemLayout1
				RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
				paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
				paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				holder.itemLayout1.setLayoutParams(paramsIL1);
				
				//Set width and height itemLayout2
				RelativeLayout.LayoutParams paramsIL2 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
				paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
				paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				paramsIL2.addRule(RelativeLayout.RIGHT_OF, R.id.file_grid_item_layout1);
				TranslateAnimation anim = new TranslateAnimation(Util.px2dp(-5*scaleW, outMetrics), Util.px2dp(-5*scaleW, outMetrics), 0, 0);
		        anim.setDuration(0);
		        
		        holder.itemLayout2.startAnimation(anim);
				holder.itemLayout2.setLayoutParams(paramsIL2);
				
				holder.imageView1 = (ImageButton) convertView.findViewById(R.id.file_grid_thumbnail1);
	            holder.imageView2 = (ImageButton) convertView.findViewById(R.id.file_grid_thumbnail2);
	            holder.imageView1.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent i = new Intent(context, FilePropertiesActivity.class);
						ItemFileBrowser rowItem = (ItemFileBrowser) getItem(_position);
						i.putExtra("imageId", rowItem.getImageId());
						i.putExtra("name", rowItem.getName());
						context.startActivity(i);						
					}
				});
	            
	            holder.imageView2.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent i = new Intent(context, FilePropertiesActivity.class);
						ItemFileBrowser rowItem = (ItemFileBrowser) getItem(_position+1);
						i.putExtra("imageId", rowItem.getImageId());
						i.putExtra("name", rowItem.getName());
						context.startActivity(i);						
					}
				});
	            
				RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
				paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
				holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
				paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
				holder.imageView1.setLayoutParams(paramsIV1);
				
				RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
				paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
				holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
				paramsIV2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
				holder.imageView2.setLayoutParams(paramsIV2);

				holder.textViewFileName1 = (TextView) convertView.findViewById(R.id.file_grid_filename1);
				holder.textViewFileName2 = (TextView) convertView.findViewById(R.id.file_grid_filename2);
				
				holder.textViewFileSize1 = (TextView) convertView.findViewById(R.id.file_grid_filesize1);
				holder.textViewFileSize2 = (TextView) convertView.findViewById(R.id.file_grid_filesize2);
				
				holder.imageButtonThreeDots1 = (ImageButton) convertView.findViewById(R.id.file_list_three_dots1);
				holder.imageButtonThreeDots2 = (ImageButton) convertView.findViewById(R.id.file_list_three_dots2);
				
				holder.optionsLayout1 = (RelativeLayout) convertView.findViewById(R.id.file_grid_options1);
				holder.optionOpen1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_open1);
				holder.optionOpen1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionProperties1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_properties1);
				holder.optionProperties1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionDownload1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_download1);
				holder.optionDownload1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionDelete1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_delete1);
				holder.optionDelete1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
				holder.arrowSelection1 = (ImageView) convertView.findViewById(R.id.file_grid_arrow_selection1);
				holder.arrowSelection1.setVisibility(View.GONE);

				holder.optionsLayout2 = (RelativeLayout) convertView.findViewById(R.id.file_grid_options2);
				holder.optionOpen2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_open2);
				holder.optionOpen2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionProperties2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_properties2);
				holder.optionProperties2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionDownload2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_download2);
				holder.optionDownload2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionDelete2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_delete2);
				holder.optionDelete2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
				holder.arrowSelection2 = (ImageView) convertView.findViewById(R.id.file_grid_arrow_selection2);
				holder.arrowSelection2.setVisibility(View.GONE);

				convertView.setTag(holder);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}
			
			ItemFileBrowser rowItem1 = (ItemFileBrowser) getItem(position);
			holder.imageView1.setImageResource(rowItem1.getImageId());
			holder.textViewFileName1.setText(rowItem1.getName());
			holder.textViewFileSize1.setText("100 KB");
			
			ItemFileBrowser rowItem2;
			if (position < (getCount()-1)){
				rowItem2 = (ItemFileBrowser) getItem(position+1);
				holder.imageView2.setImageResource(rowItem2.getImageId());
				holder.textViewFileName2.setText(rowItem2.getName());	
				holder.itemLayout2.setVisibility(View.VISIBLE);
				holder.textViewFileSize2.setText("100 KB");
			}
			else{
				holder.itemLayout2.setVisibility(View.GONE);
			}
			
			holder.imageButtonThreeDots1.setTag(holder);
			holder.imageButtonThreeDots1.setOnClickListener(
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
			
			holder.imageButtonThreeDots2.setTag(holder);
			holder.imageButtonThreeDots2.setOnClickListener(
						new OnClickListener() {
							public void onClick(View v) {
								if (positionClicked == -1){
									positionClicked = _position+1;
									notifyDataSetChanged();
								}
								else{
									if (positionClicked == (_position+1)){
										positionClicked = -1;
										notifyDataSetChanged();
									}
									else{
										positionClicked = _position+1;
										notifyDataSetChanged();
									}
								}
							}
						});
			
			if (positionClicked != -1){
				if (positionClicked == position){
					holder.arrowSelection1.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout1.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					ListView list = (ListView) parent;
					list.smoothScrollToPosition(_position);
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
				else if (positionClicked == (position+1)){
					holder.arrowSelection2.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout2.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					ListView list = (ListView) parent;
					list.smoothScrollToPosition(_position);
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
				}
				else{
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
					
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
			}
			else{
				holder.arrowSelection1.setVisibility(View.GONE);
				LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
				params1.height = 0;
				
				holder.arrowSelection2.setVisibility(View.GONE);
				LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
				params2.height = 0;
			}
			
			holder.optionProperties1.setTag(holder);
			holder.optionProperties1.setOnClickListener(
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
			
			holder.optionProperties2.setTag(holder);
			holder.optionProperties2.setOnClickListener(
						new OnClickListener() {
							public void onClick(View v) {
								Intent i = new Intent(context, FilePropertiesActivity.class);
								i.putExtra("imageId", rowItems.get(_position+1).getImageId());
								i.putExtra("name", rowItems.get(_position+1).getName());
								context.startActivity(i);							
								positionClicked = -1;
								notifyDataSetChanged();
							}
						});
		}
		else{
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_file_empty_grid, parent, false);
			}
		}

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
