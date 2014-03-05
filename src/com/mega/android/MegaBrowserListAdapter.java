package com.mega.android;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;

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

public class MegaBrowserListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
	MegaApiAndroid megaApi;

	List<ItemFileBrowser> rowItems;
	int positionClicked;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
//	ArrayList<MegaNode> nodes;
		
	public MegaBrowserListAdapter(Context _context, List<ItemFileBrowser> _items) {
		this.context = _context;
		this.rowItems = _items;
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
//		this.nodes = new ArrayList<MegaNode>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		Iterator<ItemFileBrowser> it = rowItems.iterator();
		while (it.hasNext()){
			//Esto lo tengo que quitar cuando haga el visor
			ItemFileBrowser item = it.next();
			log("handle: " + item.getNodeHandle());
			MegaNode n = megaApi.getNodeByHandle(item.getNodeHandle());
//			nodes.add(n);
			
			imageIds.add(R.drawable.sal01);
			names.add("NombrePrueba");
			//HASTA AQUI
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
        int currentPosition;
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
			holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
			holder.textViewFileSize = (TextView) convertView.findViewById(R.id.file_list_filesize);
			holder.textViewUpdated = (TextView) convertView.findViewById(R.id.file_list_updated);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.file_list_three_dots);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.file_list_options);
			holder.optionOpen = (ImageButton) convertView.findViewById(R.id.file_list_option_open);
			holder.optionOpen.setPadding(Util.px2dp((15*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
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
		
		holder.currentPosition = position;
		
		ItemFileBrowser rowItem = (ItemFileBrowser) getItem(position);
		
		MegaNode node = megaApi.getNodeByHandle(rowItem.getNodeHandle());
		
		holder.textViewFileName.setText(node.getName());
		
		if (node.isFolder()){
			holder.textViewFileSize.setText("");
			holder.imageView.setImageResource(R.drawable.mime_folder);
		}
		else{
			long nodeSize = node.getSize();
			holder.textViewFileSize.setText(Util.getSizeString(nodeSize));
			
			holder.imageView.setImageResource(MimeType.typeForName(node.getName()).getIconResourceId());
		}
		
		long nodeDate = node.getCreationTime();
		if (nodeDate != 0){
			try{ 
				holder.textViewUpdated.setText(Util.getDateString(nodeDate));
			}
			catch(Exception ex) {
				holder.textViewUpdated.setText(""); 
			}
		}
		else{
			holder.textViewUpdated.setText("");
		}
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
		
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
		holder.optionOpen.setOnClickListener(this);
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(this);
		
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

		switch (v.getId()){
			case R.id.file_list_option_open:{
				Intent i = new Intent(context, FullScreenImageViewer.class);
				i.putExtra("position", currentPosition);
				i.putExtra("names", names);
				i.putExtra("imageIds", imageIds);
				context.startActivity(i);	
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.file_list_option_properties:{
				Intent i = new Intent(context, FilePropertiesActivity.class);
				MegaNode n = megaApi.getNodeByHandle(rowItems.get(currentPosition).getNodeHandle());
				
				if (n.isFolder()){
					i.putExtra("imageId", R.drawable.mime_folder);
				}
				else{
					i.putExtra("imageId", MimeType.typeForName(n.getName()).getIconResourceId());	
				}				
				i.putExtra("name", n.getName());
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.file_list_three_dots:{
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
		}		
	}
	
	private static void log(String log) {
		Util.log("MegaBrowserListAdapter", log);
	}
}
