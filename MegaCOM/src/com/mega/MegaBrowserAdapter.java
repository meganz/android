package com.mega;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
        TextView textView;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewHolder holder = null;
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_file_list, parent, false);
			holder = new ViewHolder();
			holder.imageView = (ImageView) convertView.findViewById(R.id.file_list_thumbnail);
			holder.textView = (TextView) convertView.findViewById(R.id.file_list_text);
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		ItemFileListBrowser rowItem = (ItemFileListBrowser) getItem(position);
		
		holder.textView.setText(rowItem.getName());
		holder.imageView.setImageResource(rowItem.getImageId());
		
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
