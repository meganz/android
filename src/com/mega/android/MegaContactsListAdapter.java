package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mega.android.utils.Util;
import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;
import com.mega.sdk.NodeList;
import com.mega.sdk.UserList;

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

public class MegaContactsListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
	int positionClicked;
	ArrayList<MegaUser> contacts;
	
	MegaApiAndroid megaApi;
	
	public static ArrayList<String> pendingAvatars = new ArrayList<String>();
	
	private class UserAvatarListenerList implements MegaRequestListenerInterface{

		Context context;
		ViewHolderContactsList holder;
		MegaContactsListAdapter adapter;
		
		public UserAvatarListenerList(Context context, ViewHolderContactsList holder, MegaContactsListAdapter adapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart()");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			log("onRequestFinish()");
			if (e.getErrorCode() == MegaError.API_OK){
				
				pendingAvatars.remove(request.getEmail());
				
				if (holder.contactMail.compareTo(request.getEmail()) == 0){
					File avatar = null;
					if (context.getExternalCacheDir() != null){
						avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
					}
					else{
						avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
					}
					Bitmap bitmap = null;
					if (avatar.exists()){
						if (avatar.length() > 0){
							BitmapFactory.Options bOpts = new BitmapFactory.Options();
							bOpts.inPurgeable = true;
							bOpts.inInputShareable = true;
							bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
							if (bitmap == null) {
								avatar.delete();
							}
							else{
								holder.imageView.setImageBitmap(bitmap);
							}
						}
					}
					adapter.notifyDataSetChanged();
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			log("onRequestTemporaryError");
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public MegaContactsListAdapter(Context _context, ArrayList<MegaUser> _contacts) {
		this.context = _context;
		this.contacts = _contacts;
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	/*private view holder class*/
    private class ViewHolderContactsList {
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
        int currentPosition;
        String contactMail;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	
		final int _position = position;
		
		ViewHolderContactsList holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_contact_list, parent, false);
			holder = new ViewHolderContactsList();
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
			holder = (ViewHolderContactsList) convertView.getTag();
		}

		holder.currentPosition = position;
		
		MegaUser contact = (MegaUser) getItem(position);
		holder.contactMail = contact.getEmail();
		
//		ItemContact rowItem = (ItemContact) getItem(position);
		
		UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);
		holder.textViewContactName.setText(contact.getEmail());
		File avatar = null;
		if (context.getExternalCacheDir() != null){
			avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
		}
		else{
			avatar = new File(context.getCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
		}
		Bitmap bitmap = null;
		if (avatar.exists()){
			if (avatar.length() > 0){
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (bitmap == null) {
					avatar.delete();
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
				}
				else{
					holder.imageView.setImageBitmap(bitmap);
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);	
				}
				else{
					megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);	
				}			
			}
		}	
		else{
			if (!pendingAvatars.contains(contact.getEmail())){
				pendingAvatars.add(contact.getEmail());
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
				}
			}
		}
		
		NodeList sharedNodes = megaApi.getInShares(contact);
		
		String sharedNodesDescription = getDescription(sharedNodes);
		
		holder.textViewContent.setText(sharedNodesDescription);
		
        if (position < 2){
        	holder.statusImageView.setImageResource(R.drawable.contact_green_dot);
        }
        else if (position == 2){
        	holder.statusImageView.setImageResource(R.drawable.contact_yellow_dot);
        }
        else if (position == 3){
        	holder.statusImageView.setImageResource(R.drawable.contact_red_dot);
        }
        else{
        	holder.statusImageView.setImageResource(R.drawable.contact_green_dot);
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
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(this);
		
		return convertView;
	}

	@Override
    public int getCount() {
        return contacts.size();
    }
 
    @Override
    public Object getItem(int position) {
        return contacts.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }
    
	@Override
	public void onClick(View v) {
		ViewHolderContactsList holder = (ViewHolderContactsList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaUser c = (MegaUser) getItem(currentPosition);
		
		switch (v.getId()){
			case R.id.contact_list_option_properties:{
				Intent i = new Intent(context, ContactPropertiesMainActivity.class);
				i.putExtra("name", c.getEmail());
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.contact_list_three_dots:{
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
	
	public void setContacts (ArrayList<MegaUser> contacts){
		this.contacts = contacts;
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public String getDescription(NodeList nodes){
		int numFolders = 0;
		int numFiles = 0;
		
		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			}
			else{
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		
		return info;
	}
	
	private static void log(String log) {
		Util.log("MegaContactsListAdapter", log);
	}
}
