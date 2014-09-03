package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/*
 * Adapter for FilestorageActivity list
 */
public class ContactsExplorerAdapter extends BaseAdapter implements OnClickListener {
	
	public static ArrayList<String> pendingAvatars = new ArrayList<String>();

	private class UserAvatarListenerExplorer implements MegaRequestListenerInterface{

		Context context;
		ViewHolderContactsExplorer holder;
		ContactsExplorerAdapter adapter;
		
		public UserAvatarListenerExplorer(Context context, ViewHolderContactsExplorer holder, ContactsExplorerAdapter adapter) {
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
	
	// Listener for item check
	public interface OnItemCheckClickListener {
		public void onItemCheckClick(int position);
	}
		
	private Context mContext;
	MegaApiAndroid megaApi;
	
	private List<MegaUser> currentContacts;
	private OnItemCheckClickListener checkClickListener;
	
	public ContactsExplorerAdapter(Context context, ArrayList<MegaUser> contacts) {
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		setContext(context);
		setContacts(contacts);
	}
	
	public void setContext(Context context) {
		mContext = context;
	}
	
	public void setOnItemCheckClickListener(OnItemCheckClickListener listener) {
		this.checkClickListener = listener;
	}
	
	// Set new contacts
	public void setContacts(List<MegaUser> newContacts){
		currentContacts = newContacts;
		notifyDataSetChanged();
	}
	
	public MegaUser getDocumentAt(int position) {
		return currentContacts.get(position);
	}
	
	@Override
	public int getCount() {
		if (currentContacts == null) {
			return 0;
		}
		int size = currentContacts.size();
		return size == 0 ? 1 : size;
	}

	@Override
    public Object getItem(int position) {
        return currentContacts.get(position);
    }

	@Override
    public long getItemId(int position) {
        return position;
    } 
	
	public class ViewHolderContactsExplorer{
		TextView textView;
		RoundedImageView imageView;
		String contactMail;
		int currentPosition;
	}

	@Override
	public View getView(final int position, View rowView, ViewGroup parentView) {
//		boolean isCheckable = mode == Mode.PICK_FILE;
		boolean isCheckable = false;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (position == 0 && currentContacts.size() == 0) {
			TextView textView = (TextView) inflater.inflate(R.layout.file_list_empty, parentView, false);
			int resId = R.string.manager_folder_is_empty;
			textView.setText(mContext.getString(resId));
			return textView;
		}
		MegaUser contact = (MegaUser) getItem(position);
		
		int layoutId;

//		if (isCheckable){
//			layoutId = R.layout.contacts_explorer_item_checkable;
//		}
//		else{
			layoutId = R.layout.contact_explorer_item;
//		}
		
		ViewHolderContactsExplorer holder = new ViewHolderContactsExplorer();
		rowView = inflater.inflate(layoutId, parentView, false);
		holder.textView = (TextView) rowView.findViewById(R.id.contact_explorer_name);
		holder.imageView = (RoundedImageView) rowView.findViewById(R.id.contact_explorer_thumbnail);
		holder.currentPosition = position;
		holder.contactMail = contact.getEmail();
		
//		if (isCheckable) {
//			View checkArea = rowView.findViewById(R.id.checkbox);
//			checkArea.setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					checkClickListener.onItemCheckClick(position);
//				}
//			});
//		}
		
		UserAvatarListenerExplorer listener = new UserAvatarListenerExplorer(mContext, holder, this);
		holder.textView.setText(contact.getEmail());
		File avatar = null;
		if (mContext.getExternalCacheDir() != null){
			avatar = new File(mContext.getExternalCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
		}
		else{
			avatar = new File(mContext.getCacheDir().getAbsolutePath(), holder.contactMail + ".jpg");
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
					if (mContext.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact, mContext.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact, mContext.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
					}
				}
				else{
					holder.imageView.setImageBitmap(bitmap);
				}
			}
			else{
				if (mContext.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact, mContext.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);	
				}
				else{
					megaApi.getUserAvatar(contact, mContext.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);	
				}			
			}
		}	
		else{
			if (!pendingAvatars.contains(contact.getEmail())){
				pendingAvatars.add(contact.getEmail());
				
				if (mContext.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact, mContext.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact, mContext.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
				}
			}
		}
		
		return rowView;
	}
	
	@Override
	public void onClick(View v) {
		log("click!");
	}	

	private static void log(String message) {
		Util.log("ContactsExplorerAdapter", message);
	}
}
