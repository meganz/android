package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApi;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaUser;
import com.mega.sdk.ShareList;
import com.mega.sdk.UserList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Dialog for Upload here action
 */
public class ShareFolderContactsDialog extends DialogFragment implements OnItemClickListener, MegaRequestListenerInterface {

	ListView listView;	
	ContactAdapter adapter;
	public static MegaApiAndroid megaApi = null;
	Context context;
	
	UserList contacts;
	ArrayList<MegaUser> visibleContacts = new ArrayList<MegaUser>();
	
	MegaNode node = null;

	AlertDialog permissionsDialog;
	
	final ShareFolderContactsDialog shareFolderContactsDialog = this;
	
	ProgressDialog statusDialog;

	public ShareFolderContactsDialog() {
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		this.contacts = megaApi.getContacts();
		visibleContacts.clear();
		for (int i=0;i<contacts.size();i++){
			log("contact: " + contacts.get(i).getEmail() + "_" + contacts.get(i).getVisibility());
			if ((contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) || (megaApi.getInShares(contacts.get(i)).size() != 0)){
				visibleContacts.add(contacts.get(i));
			}
		}
		
		View view = inflater.inflate(R.layout.share_folder_contacts_dialog, container, false);
		
		listView = (ListView) view.findViewById(R.id.contact_dialog_list_view);
		listView.setOnItemClickListener(this);
		
		adapter = new ContactAdapter(getActivity(), R.layout.file_list_item_file, visibleContacts);
		listView.setAdapter(adapter);
		
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		TextView titleView = (TextView) view.findViewById(R.id.dialog_title);
		titleView.setText(R.string.file_properties_shared_folder_select_contact);
		
		return view;		
	}
	
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		this.setShowsDialog(false);
		final MegaUser contact = visibleContacts.get(position);
		
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
		dialogBuilder.setTitle(context.getString(R.string.file_properties_shared_folder_permissions));
		final CharSequence[] items = {context.getString(R.string.file_properties_shared_folder_read_only), context.getString(R.string.file_properties_shared_folder_read_write), context.getString(R.string.file_properties_shared_folder_full_access)};
		dialogBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				ProgressDialog temp = null;
				try{
					temp = new ProgressDialog(context);
					temp.setMessage(getString(R.string.context_sharing_folder));
					temp.show();
				}
				catch(Exception e){
					return;
				}
				statusDialog = temp;
				permissionsDialog.dismiss();
				
				switch(item) {
                    case 0:{
                    	megaApi.share(node, contact.getEmail(), MegaShare.ACCESS_READ, shareFolderContactsDialog);
                    	break;
                    }
                    case 1:{
                    	megaApi.share(node, contact.getEmail(), MegaShare.ACCESS_READWRITE, shareFolderContactsDialog);
                        break;
                    }
                    case 2:{
                    	megaApi.share(node, contact.getEmail(), MegaShare.ACCESS_FULL, shareFolderContactsDialog);
                        break;
                    }
                }
			}
		});
		permissionsDialog = dialogBuilder.create();
		permissionsDialog.show();
		Resources resources = permissionsDialog.getContext().getResources();
		int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");
		TextView alertTitle = (TextView) permissionsDialog.getWindow().getDecorView().findViewById(alertTitleId);
        alertTitle.setTextColor(resources.getColor(R.color.mega));
		int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
		View titleDivider = permissionsDialog.getWindow().getDecorView().findViewById(titleDividerId);
		titleDivider.setBackgroundColor(resources.getColor(R.color.mega));
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }
	
	/*
	 * Adapter for pick action list
	 */
	private static class ContactAdapter extends ArrayAdapter<MegaUser> {
		
		/*private view holder class*/
	    private class ViewHolderContactAdapter {
	    	RoundedImageView imageView;
	        TextView textViewContactName; 
	        int currentPosition;
	        String contactMail;
	    } 
		
		private class UserAvatarListenerList implements MegaRequestListenerInterface{

			Context context;
			ViewHolderContactAdapter holder;
			ContactAdapter adapter;
			
			public UserAvatarListenerList(Context context, ViewHolderContactAdapter holder, ContactAdapter adapter) {
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
		
		ArrayList<MegaUser> visibleContacts;
		Context context;
		
		public ContactAdapter(Context context, int textViewResourceId, ArrayList<MegaUser> objects) {
			super(context, textViewResourceId, objects);
			this.visibleContacts = objects;
			this.context = context;
		}
		
		@Override
		public View getView(int position, View v, ViewGroup parent) {
			ViewHolderContactAdapter holder = new ViewHolderContactAdapter();
			
			LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
			View view = inflater.inflate(R.layout.share_folder_contacts_item, parent, false);
			MegaUser contact = visibleContacts.get(position);
			holder.imageView = (RoundedImageView) view.findViewById(R.id.shared_folder_contact_list_thumbnail);
			holder.textViewContactName = (TextView) view.findViewById(R.id.shared_folder_contact_list_name);
			holder.textViewContactName.setText(contact.getEmail());
			holder.contactMail = contact.getEmail();
			
			UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);
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
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
				}
			}
		
			return view;
		}
		
	}
	
	public MegaNode getNode(){
		return node;
	}
	
	public void setNode(MegaNode node){
		this.node = node;
	}

	private static void log(String message) {
		Util.log("ShareFolderContactsDialog", message);
	}
	
	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		
		try { 
			statusDialog.dismiss();
			dismiss();
		} 
		catch (Exception ex) {}
		
		if (request.getType() == MegaRequest.TYPE_SHARE){
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(context, "The folder has been shared correctly", Toast.LENGTH_LONG).show();
				ShareList sl = megaApi.getOutShares(node);
			}
			else{
				Util.showErrorAlertDialog(e, (Activity)context);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
}
