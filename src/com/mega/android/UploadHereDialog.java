package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import com.mega.android.utils.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/*
 * Dialog for Upload here action
 */
public class UploadHereDialog extends DialogFragment implements OnItemClickListener {

	// Upload here actions
	private static ArrayList<ListItem> items;
	static {
		items = new ArrayList<UploadHereDialog.ListItem>();
		items.add(new ListItem(R.string.upload_to_image, MimeType.typeForName(
				"image.png").getIconResourceId(), "image/*"));
		items.add(new ListItem(R.string.upload_to_audio, MimeType.typeForName(
				"song.mp3").getIconResourceId(), "audio/*"));
		items.add(new ListItem(R.string.upload_to_video, MimeType.typeForName(
				"film.mkv").getIconResourceId(), "video/*"));
		items.add(new ListItem(R.string.upload_to_filesystem,
				R.drawable.mime_folder, null));
	}
	
	ListView listView;	
	ExportAdapter adapter;
	
	private static class ListItem {
		public int imageResId;
		public int titleResId;
		public String type;

		public ListItem(int titleResId, int imageResId, String type) {
			this.titleResId = titleResId;
			this.imageResId = imageResId;
			this.type = type;
		}
	}

	public UploadHereDialog() {
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.upload_dialog, container, false);
		
		listView = (ListView) view.findViewById(R.id.upload_dialog_list_view);
		listView.setOnItemClickListener(this);
		
		adapter = new ExportAdapter(getActivity(), R.layout.file_list_item_file, items);
		listView.setAdapter(adapter);
		
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		TextView titleView = (TextView) view.findViewById(R.id.dialog_title);
		titleView.setText(R.string.upload_select_file_type);
		
		return view;		
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		ListItem item = items.get(position);
		Intent intent = new Intent();
		
		// Pick from File System
		if (item.type == null) {
			intent.setAction(FileStorageActivity.Mode.PICK_FILE.getAction());
			intent.setClass(getActivity(), FileStorageActivity.class);
			getActivity().startActivityForResult(intent, ManagerActivity.REQUEST_CODE_GET_LOCAL);
		}
		else{
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.setType(item.type);
			getActivity().startActivityForResult(Intent.createChooser(intent, null), ManagerActivity.REQUEST_CODE_GET);
		}
		dismiss();
	}
	
	/*
	 * Adapter for pick action list
	 */
	private static class ExportAdapter extends ArrayAdapter<ListItem> {
		
		Context mContext;
		
		public ExportAdapter(Context context, int textViewResourceId, List<ListItem> objects) {
			super(context, textViewResourceId, objects);
			mContext = context;
		}
		
		@Override
		public View getView(int position, View v, ViewGroup parent) {
			LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
			View view = inflater.inflate(R.layout.upload_item, parent, false);
			ListItem item = items.get(position);
			ImageView image = (ImageView) view.findViewById(R.id.file_image);
			TextView text = (TextView) view.findViewById(R.id.file_text);
			image.setImageResource(item.imageResId);
			text.setText(mContext.getString(item.titleResId));
			return view;
		}
		
	}

	private void log(String message) {
		Util.log("UploadHereDialog", message);
	}
}
