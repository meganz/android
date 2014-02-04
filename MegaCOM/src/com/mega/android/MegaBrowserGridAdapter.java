package com.mega.android;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
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
        ImageView imageView1;
        TextView textViewFileName1;
        RelativeLayout itemLayout1;
        ImageView imageView2;
        TextView textViewFileName2;
        RelativeLayout itemLayout2;
        TextView textViewFileSize1;
        TextView textViewFileSize2;
        ImageButton imageButtonThreeDots1;
        ImageButton imageButtonThreeDots2;
//        ImageView arrowSelection;
//        RelativeLayout optionsLayout;
//        ImageButton optionOpen;
//        ImageButton optionProperties;
//        ImageButton optionDownload;
//        ImageButton optionDelete;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final int _position = position;
		
		ViewHolder holder = null;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if ((position % 2) == 0){
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_file_grid, parent, false);
				holder = new ViewHolder();
				holder.itemLayout1 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout1);
				holder.itemLayout2 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout2);
				
				holder.imageView1 = (ImageView) convertView.findViewById(R.id.file_grid_thumbnail1);
	            holder.imageView2 = (ImageView) convertView.findViewById(R.id.file_grid_thumbnail2);
	            
				holder.textViewFileName1 = (TextView) convertView.findViewById(R.id.file_grid_filename1);
				holder.textViewFileName2 = (TextView) convertView.findViewById(R.id.file_grid_filename2);
				
				holder.textViewFileSize1 = (TextView) convertView.findViewById(R.id.file_grid_filesize1);
				holder.textViewFileSize2 = (TextView) convertView.findViewById(R.id.file_grid_filesize2);
				
				holder.imageButtonThreeDots1 = (ImageButton) convertView.findViewById(R.id.file_list_three_dots1);
				holder.imageButtonThreeDots2 = (ImageButton) convertView.findViewById(R.id.file_list_three_dots2);
				
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
			
			
			
			
			
			
			
			
			
			
			
	
	//		holder.imageButtonThreeDots.setTag(holder);
	//		holder.imageButtonThreeDots.setOnClickListener(
	//					new OnClickListener() {
	//						public void onClick(View v) {
	//							if (positionClicked == -1){
	//								positionClicked = _position;
	//								notifyDataSetChanged();
	//							}
	//							else{
	//								if (positionClicked == _position){
	//									positionClicked = -1;
	//									notifyDataSetChanged();
	//								}
	//								else{
	//									positionClicked = _position;
	//									notifyDataSetChanged();
	//								}
	//							}
	//						}
	//					});
	//		
	//		if (positionClicked != -1){
	//			if (positionClicked == position){
	//				holder.arrowSelection.setVisibility(View.VISIBLE);
	//				LayoutParams params = holder.optionsLayout.getLayoutParams();
	//				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
	//				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
	//				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_grey);
	//				ListView list = (ListView) parent;
	//				list.smoothScrollToPosition(_position);
	//			}
	//			else{
	//				holder.arrowSelection.setVisibility(View.GONE);
	//				LayoutParams params = holder.optionsLayout.getLayoutParams();
	//				params.height = 0;
	//				holder.itemLayout.setBackgroundColor(Color.WHITE);
	//				holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
	//			}
	//		}
	//		else{
	//			holder.arrowSelection.setVisibility(View.GONE);
	//			LayoutParams params = holder.optionsLayout.getLayoutParams();
	//			params.height = 0;
	//			holder.itemLayout.setBackgroundColor(Color.WHITE);
	//			holder.imageButtonThreeDots.setImageResource(R.drawable.three_dots_background_white);
	//		}
	//		
	//		holder.optionProperties.setTag(holder);
	//		holder.optionProperties.setOnClickListener(
	//					new OnClickListener() {
	//						public void onClick(View v) {
	//							Intent i = new Intent(context, FilePropertiesActivity.class);
	//							i.putExtra("imageId", rowItems.get(_position).getImageId());
	//							i.putExtra("name", rowItems.get(_position).getName());
	//							context.startActivity(i);							
	//							positionClicked = -1;
	//							notifyDataSetChanged();
	//						}
	//					});
	//		
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
