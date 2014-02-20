package com.mega.android;

import java.util.ArrayList;
import java.util.Iterator;
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

public class MegaRubbishBinGridAdapter extends BaseAdapter implements OnClickListener{
	
	Context context;
	List<ItemFileBrowser> rowItems;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
	int positionClicked;
	
	public MegaRubbishBinGridAdapter(Context _context, List<ItemFileBrowser> _items) {
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
        ImageButton optionUndo1;
        ImageButton optionDeletePermanently1;
        ImageView arrowSelection2;
        RelativeLayout optionsLayout2;
        ImageButton optionUndo2;
        ImageButton optionDeletePermanently2;
        int currentPosition;
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
				convertView = inflater.inflate(R.layout.item_rubbishbin_grid, parent, false);
				holder = new ViewHolder();
				holder.itemLayout1 = (RelativeLayout) convertView.findViewById(R.id.rubbishbin_grid_item_layout1);
				holder.itemLayout2 = (RelativeLayout) convertView.findViewById(R.id.rubbishbin_grid_item_layout2);
				
				//Set width and height itemLayout1
				RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
				paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
				paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				holder.itemLayout1.setLayoutParams(paramsIL1);
				
				//Set width and height itemLayout2
				RelativeLayout.LayoutParams paramsIL2 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
				paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
				paramsIL2.addRule(RelativeLayout.RIGHT_OF, R.id.rubbishbin_grid_item_layout1);
				paramsIL2.addRule(RelativeLayout.LEFT_OF, R.id.rubbishbin_grid_separator_final);
				paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				TranslateAnimation anim = new TranslateAnimation(Util.px2dp(-5*scaleW, outMetrics), Util.px2dp(-5*scaleW, outMetrics), 0, 0);
		        anim.setDuration(0);
		        
		        holder.itemLayout2.startAnimation(anim);
				holder.itemLayout2.setLayoutParams(paramsIL2);
				
				holder.imageView1 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_thumbnail1);
	            holder.imageView2 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_thumbnail2);
	            
	            
				RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(75*scaleW, outMetrics),Util.px2dp(75*scaleH, outMetrics));
				paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
				holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
				paramsIV1.setMargins(Util.px2dp(41*scaleW, outMetrics), Util.px2dp(41*scaleH, outMetrics), Util.px2dp(41*scaleW, outMetrics), 0);
				holder.imageView1.setLayoutParams(paramsIV1);
				
				RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(75*scaleW, outMetrics),Util.px2dp(75*scaleH, outMetrics));
				paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
				holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
				paramsIV2.setMargins(Util.px2dp(41*scaleW, outMetrics), Util.px2dp(41*scaleH, outMetrics), Util.px2dp(41*scaleW, outMetrics), 0);
				holder.imageView2.setLayoutParams(paramsIV2);

				holder.textViewFileName1 = (TextView) convertView.findViewById(R.id.rubbishbin_grid_filename1);
				holder.textViewFileName2 = (TextView) convertView.findViewById(R.id.rubbishbin_grid_filename2);
				
				holder.textViewFileSize1 = (TextView) convertView.findViewById(R.id.rubbishbin_grid_filesize1);
				holder.textViewFileSize2 = (TextView) convertView.findViewById(R.id.rubbishbin_grid_filesize2);
				
				holder.imageButtonThreeDots1 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_three_dots1);
				holder.imageButtonThreeDots2 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_three_dots2);
				
				holder.optionsLayout1 = (RelativeLayout) convertView.findViewById(R.id.rubbishbin_grid_options1);
				holder.optionUndo1 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_option_undo1);
				holder.optionUndo1.setPadding(Util.px2dp((87*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionDeletePermanently1 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_option_delete_permanently1);
				holder.optionDeletePermanently1.setPadding(Util.px2dp((75*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.arrowSelection1 = (ImageView) convertView.findViewById(R.id.rubbishbin_grid_arrow_selection1);
				holder.arrowSelection1.setVisibility(View.GONE);

				holder.optionsLayout2 = (RelativeLayout) convertView.findViewById(R.id.rubbishbin_grid_options2);
				holder.optionUndo2 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_option_undo2);
				holder.optionUndo2.setPadding(Util.px2dp((87*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.optionDeletePermanently2 = (ImageButton) convertView.findViewById(R.id.rubbishbin_grid_option_delete_permanently2);
				holder.optionDeletePermanently2.setPadding(Util.px2dp((75*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
				holder.arrowSelection2 = (ImageView) convertView.findViewById(R.id.rubbishbin_grid_arrow_selection2);
				holder.arrowSelection2.setVisibility(View.GONE);
				
				holder.currentPosition = position;

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
			
			holder.imageView1.setTag(holder);
			holder.imageView1.setOnClickListener(this);
			
			holder.imageView2.setTag(holder);
			holder.imageView2.setOnClickListener(this);
			
			holder.imageButtonThreeDots1.setTag(holder);
			holder.imageButtonThreeDots1.setOnClickListener(this);
			
			holder.imageButtonThreeDots2.setTag(holder);
			holder.imageButtonThreeDots2.setOnClickListener(this);
			
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
			
			holder.optionUndo1.setTag(holder);
			holder.optionUndo1.setOnClickListener(this);
			
			holder.optionDeletePermanently1.setTag(holder);
			holder.optionDeletePermanently1.setOnClickListener(this);
			
			holder.optionUndo2.setTag(holder);
			holder.optionUndo2.setOnClickListener(this);
			
			holder.optionDeletePermanently2.setTag(holder);
			holder.optionDeletePermanently2.setOnClickListener(this);
		}
		else{
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_rubbishbin_empty_grid, parent, false);
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

	@Override
	public void onClick(View v) {

		ViewHolder holder = (ViewHolder) v.getTag();
		int currentPosition = holder.currentPosition;
		
		switch(v.getId()){
			case R.id.rubbishbin_grid_thumbnail1:{
				Intent i = new Intent(context, FullScreenImageViewer.class);
				i.putExtra("position", currentPosition);
				i.putExtra("names", names);
				i.putExtra("imageIds", imageIds);
				context.startActivity(i);	
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.rubbishbin_grid_thumbnail2:{
				Intent i = new Intent(context, FullScreenImageViewer.class);
				i.putExtra("position", currentPosition+1);
				i.putExtra("names", names);
				i.putExtra("imageIds", imageIds);
				context.startActivity(i);	
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.rubbishbin_grid_three_dots1:{
				if (positionClicked == -1){
					positionClicked = currentPosition;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == currentPosition){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.rubbishbin_grid_three_dots2:{
				if (positionClicked == -1){
					positionClicked = currentPosition + 1;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == (currentPosition + 1)){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.rubbishbin_grid_option_undo1:{
				Toast.makeText(context, "Undo_position"+currentPosition, Toast.LENGTH_SHORT).show();
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.rubbishbin_grid_option_undo2:{
				Toast.makeText(context, "Undo_position"+(currentPosition+1), Toast.LENGTH_SHORT).show();
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.rubbishbin_grid_option_delete_permanently1:{
				Toast.makeText(context, "Delete permanently_position"+currentPosition, Toast.LENGTH_SHORT).show();
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.rubbishbin_grid_option_delete_permanently2:{
				Toast.makeText(context, "Delete permanently_position"+(currentPosition+1), Toast.LENGTH_SHORT).show();
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
		}
	}
}
