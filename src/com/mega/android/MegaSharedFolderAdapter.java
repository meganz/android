package com.mega.android;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.android.utils.Util;
import com.mega.components.RoundedImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaGlobalListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaShare;
import com.mega.sdk.MegaUser;

public class MegaSharedFolderAdapter extends BaseAdapter implements OnClickListener, MegaRequestListenerInterface {
	
	Context context;
	int positionClicked;
	ArrayList<MegaShare> shareList;
	MegaNode node;
	ListView listViewActivity;
	
	MegaApiAndroid megaApi;
	
	boolean removeShare = false;
	boolean multipleSelect = false;
	
	AlertDialog permissionsDialog;
	
	final MegaSharedFolderAdapter megaSharedFolderAdapter;
	
	ProgressDialog statusDialog;
	
	public static ArrayList<String> pendingAvatars = new ArrayList<String>();
	
	private class UserAvatarListenerList implements MegaRequestListenerInterface{

		Context context;
		ViewHolderShareList holder;
		MegaSharedFolderAdapter adapter;
		
		public UserAvatarListenerList(Context context, ViewHolderShareList holder, MegaSharedFolderAdapter adapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart() avatar");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			log("onRequestFinish() avatar");
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
				}
			}
			else{
				log("E: " + e.getErrorCode() + "_" + e.getErrorString());	
				//TODO Si no tiene avatar, holder.imageView.setImageBitmap(IMAGEN_POR_DEFECTO);
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
	
	public MegaSharedFolderAdapter(Context _context, MegaNode node, ArrayList<MegaShare> _shareList, ListView _lv) {
		this.context = _context;
		this.node = node;
		this.shareList = _shareList;
		this.positionClicked = -1;
		this.megaSharedFolderAdapter = this;
		this.listViewActivity = _lv;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	public void setContext(Context context){
		this.context = context;
	}
	
	public void setNode(MegaNode node){
		this.node = node;
	}
	
	public void setListViewActivity(ListView lv){
		this.listViewActivity = lv;
	}
		
	/*private view holder class*/
    private class ViewHolderShareList {
    	CheckBox checkbox;
    	RoundedImageView imageView;
//        ImageView imageView;
        TextView textViewContactName; 
        TextView textViewPermissions;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        RelativeLayout optionsLayout;
        ImageButton optionPermissions;
        ImageButton optionRemoveShare;
        int currentPosition;
        String contactMail;
    } 

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		log("getView");
		
		//log("Position: " + position + "_TOTAL: " + getCount());
	
		listViewActivity = (ListView) parent;
		final int _position = position;
		
		ViewHolderShareList holder = new ViewHolderShareList();
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_shared_folder, parent, false);
			holder = new ViewHolderShareList();
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.shared_folder_contact_checkbox);
			holder.checkbox.setClickable(false);
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.shared_folder_item_layout);
			holder.imageView = (RoundedImageView) convertView.findViewById(R.id.shared_folder_contact_thumbnail);
			((RelativeLayout.LayoutParams) holder.imageView.getLayoutParams()).setMargins(Util.px2dp((15*scaleW), outMetrics), Util.px2dp((5*scaleH), outMetrics), Util.px2dp((15*scaleW), outMetrics), 0);
			holder.imageView.getLayoutParams().width = Util.px2dp((40*scaleW), outMetrics);
			holder.imageView.getLayoutParams().height = Util.px2dp((40*scaleH), outMetrics);
			holder.textViewContactName = (TextView) convertView.findViewById(R.id.shared_folder_contact_name);
			holder.textViewContactName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			holder.textViewContactName.setSingleLine();
			holder.textViewPermissions = (TextView) convertView.findViewById(R.id.shared_folder_contact_permissions);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.shared_folder_contact_three_dots);
			holder.optionsLayout = (RelativeLayout) convertView.findViewById(R.id.shared_folder_contact_options);
			holder.optionPermissions = (ImageButton) convertView.findViewById(R.id.shared_folder_permissions_option);
			holder.optionPermissions.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionRemoveShare = (ImageButton) convertView.findViewById(R.id.shared_folder_remove_share_option);
			holder.optionRemoveShare.setPadding(Util.px2dp((50*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//			holder.arrowSelection = (ImageView) convertView.findViewById(R.id.shared_folder_contact_arrow_selection);
//			holder.arrowSelection.setVisibility(View.GONE);
			convertView.setTag(holder); 
		}
		else{
			holder = (ViewHolderShareList) convertView.getTag();
		}
		
		if (!multipleSelect){
			holder.checkbox.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
		}
		else{
			holder.checkbox.setVisibility(View.VISIBLE);
//			holder.arrowSelection.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.GONE);
			
			SparseBooleanArray checkedItems = listViewActivity.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true){
				holder.checkbox.setChecked(true);
			}
			else{
				holder.checkbox.setChecked(false);
			}
		}

		holder.currentPosition = position;
		
		//Check if the share
		MegaShare share = (MegaShare) getItem(position);
		if (share.getUser() == null){
			holder.contactMail = context.getString(R.string.file_properties_shared_folder_public_link);
			holder.textViewContactName.setText(holder.contactMail);
		}
		else{
			holder.contactMail = share.getUser();
			MegaUser contact = megaApi.getContact(holder.contactMail);
			holder.textViewContactName.setText(holder.contactMail);
			
			int accessLevel = share.getAccess();
			switch(accessLevel){
				case MegaShare.ACCESS_FULL:{
					holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_full_access));
					break;
				}
				case MegaShare.ACCESS_READ:{
					holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_read_only));
					break;
				}
				case MegaShare.ACCESS_READWRITE:{
					holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_read_write));
					break;	
				}
			}
			
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
		}
		
        holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
		
		if (positionClicked != -1){
			if (positionClicked == position){
//				holder.arrowSelection.setVisibility(View.VISIBLE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
				ListView list = (ListView) parent;
				list.smoothScrollToPosition(_position);
				
				holder.optionPermissions.getLayoutParams().width = Util.px2dp((120*scaleW), outMetrics);
				((TableRow.LayoutParams) holder.optionPermissions.getLayoutParams()).setMargins(Util.px2dp((29*scaleW), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
				holder.optionRemoveShare.getLayoutParams().width = Util.px2dp((140*scaleW), outMetrics);
				((TableRow.LayoutParams) holder.optionRemoveShare.getLayoutParams()).setMargins(Util.px2dp((17*scaleW), outMetrics), Util.px2dp((2*scaleH), outMetrics), 0, 0);
			}
			else{
//				holder.arrowSelection.setVisibility(View.GONE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_properties_available_layout));
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
		}
		else{
//			holder.arrowSelection.setVisibility(View.GONE);
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_properties_available_layout));
			holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
		}
		
		if (share.getUser() != null){
			holder.optionPermissions.setVisibility(View.VISIBLE);
			holder.optionPermissions.setTag(holder);
			holder.optionPermissions.setOnClickListener(this);
		}
		else{
			holder.optionPermissions.setVisibility(View.GONE);			
		}
		
		holder.optionRemoveShare.setTag(holder);
		holder.optionRemoveShare.setOnClickListener(this);
		
		return convertView;
	}

	@Override
    public int getCount() {
        return shareList.size();
    }
 
    @Override
    public Object getItem(int position) {
        return shareList.get(position);
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
		log("onClick");
		ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
		int currentPosition = holder.currentPosition;
		final MegaShare s = (MegaShare) getItem(currentPosition);
		MegaUser c = null;
		if (s.getUser() != null){
			c = megaApi.getContact(s.getUser());
		}
				
		switch (v.getId()){
			case R.id.shared_folder_permissions_option:{
				log("En el adapter - change");
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
				dialogBuilder.setTitle(context.getString(R.string.file_properties_shared_folder_permissions));
				final CharSequence[] items = {context.getString(R.string.file_properties_shared_folder_read_only), context.getString(R.string.file_properties_shared_folder_read_write), context.getString(R.string.file_properties_shared_folder_full_access)};
				dialogBuilder.setSingleChoiceItems(items, s.getAccess(), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						removeShare = false;
						ProgressDialog temp = null;
						try{
							temp = new ProgressDialog(context);
							temp.setMessage(((Activity)context).getString(R.string.context_sharing_folder));
							temp.show();
						}
						catch(Exception e){
							return;
						}
						statusDialog = temp;
						switch(item) {
	                        case 0:{
	                        	MegaUser u = megaApi.getContact(s.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_READ, megaSharedFolderAdapter);
	                        	break;
	                        }
	                        case 1:{
	                        	MegaUser u = megaApi.getContact(s.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_READWRITE, megaSharedFolderAdapter);
                                break;
	                        }
	                        case 2:{
	                        	MegaUser u = megaApi.getContact(s.getUser());
	                        	megaApi.share(node, u, MegaShare.ACCESS_FULL, megaSharedFolderAdapter);
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
				
				positionClicked = -1;
				((FileContactListActivity)context).refreshView();
				break;
			}
			case R.id.shared_folder_remove_share_option:{
				log("En el adapter - remove");
				ProgressDialog temp = null;
				try{
					temp = new ProgressDialog(context);
					temp.setMessage(((Activity)context).getString(R.string.context_sharing_folder)); 
					temp.show();
				}
				catch(Exception e){
					return;
				}
				statusDialog = temp;
				if (c != null){
					removeShare = true;
					MegaUser u = megaApi.getContact(s.getUser());
					megaApi.share(node, u, MegaShare.ACCESS_UNKNOWN, this);
				}
				else{
					megaApi.disableExport(node, this);
				}
				
				positionClicked = -1;
//				((FileContactListActivity)context).refreshView();
				break;
			}
			case R.id.shared_folder_contact_three_dots:{
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
	
	public void setShareList (ArrayList<MegaShare> shareList){
		log("setShareList");
		this.shareList = shareList;
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	private static void log(String log) {
		Util.log("MegaSharedFolderAdapter", log);
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
		} 
		catch (Exception ex) {}
		
		if (request.getType() == MegaRequest.TYPE_EXPORT){
			if (e.getErrorCode() == MegaError.API_OK){
				Toast.makeText(context, "The node is now private", Toast.LENGTH_LONG).show();
			}
			else{
				Util.showErrorAlertDialog(e, (Activity)context);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_SHARE){
			if (removeShare){
				if (e.getErrorCode() == MegaError.API_OK){
					ArrayList<MegaShare> sl = megaApi.getOutShares(node);
					Toast.makeText(context, "Share correctly removed: " +sl.size(), Toast.LENGTH_LONG).show();
					for(int i=0;i<sl.size();i++){
						MegaShare sh = sl.get(i);
						if (sh.getAccess() == MegaShare.ACCESS_UNKNOWN){
							sl.remove(i);
						}
					}
					Toast.makeText(context, "Share correctly after: " +sl.size(), Toast.LENGTH_LONG).show();
					setShareList(sl);
				}
				else{
					Util.showErrorAlertDialog(e, (Activity)context);
				}
				removeShare = false;
			}
			else{
				permissionsDialog.dismiss();
				if (e.getErrorCode() == MegaError.API_OK){
					Toast.makeText(context, "The folder has been shared correctly", Toast.LENGTH_LONG).show();
					ArrayList<MegaShare> sl = megaApi.getOutShares(node);
					setShareList(sl);
				}
				else{
					Util.showErrorAlertDialog(e, (Activity)context);
				}
			}
		}
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		if(this.multipleSelect != multipleSelect){
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public MegaShare getContactAt(int position) {
		try {
			if(shareList != null){
				return shareList.get(position);
			}
		} catch (IndexOutOfBoundsException e) {}
		return null;
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
