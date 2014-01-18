package com.mega;

import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MegaBrowserAdapter extends BaseAdapter {
	
	Context context;
	List<ItemFileListBrowser> rowItems;

	public MegaBrowserAdapter(Context _context, List<ItemFileListBrowser> _items) {
		this.context = _context;
		this.rowItems = _items;
	}
	
	/*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        TextView textViewFileName;
        TextView textViewFileSize;
        TextView textViewUpdated;
        ImageButton imageButtonThreeDots;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final int _position = position;
		
		ViewHolder holder = null;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_file_list, parent, false);
			holder = new ViewHolder();
			holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
			holder.textViewFileName = (TextView) convertView.findViewById(R.id.file_list_filename);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_list_filesize);
			holder.textViewUpdated = (TextView) convertView.findViewById(R.id.file_list_updated);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.file_list_three_dots);
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		ItemFileListBrowser rowItem = (ItemFileListBrowser) getItem(position);
		
		holder.textViewFileName.setText(rowItem.getName());
		holder.textViewFileSize.setText("100 KB");
		holder.textViewUpdated.setText("Updated: 1 week ago");
		holder.imageView.setImageResource(rowItem.getImageId());
		
		holder.imageButtonThreeDots.setOnClickListener(
					new OnClickListener() {
						public void onClick(View v) {
							String cadena = "ImageButton " + (_position + 1) + " clicked";
	                        Toast toast = Toast.makeText( context, cadena,Toast.LENGTH_LONG);
	                        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
	                        toast.show();
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
}
