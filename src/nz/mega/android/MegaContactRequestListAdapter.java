package nz.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nz.mega.android.utils.Util;
import nz.mega.components.RoundedImageView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.format.DateUtils;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MegaContactRequestListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
	int positionClicked;
	ArrayList<MegaContactRequest> contacts;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ListView listFragment;
	MegaApiAndroid megaApi;
	boolean multipleSelect;
	int type;
	
	private class UserAvatarListenerList implements MegaRequestListenerInterface{

		Context context;
		ViewHolderContactsRequestList holder;
		MegaContactRequestListAdapter adapter;
		
		public UserAvatarListenerList(Context context, ViewHolderContactsRequestList holder, MegaContactRequestListAdapter megaContactRequestListAdapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = megaContactRequestListAdapter;
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("onRequestStart()");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
			log("onRequestFinish()");
			if (e.getErrorCode() == MegaError.API_OK){
				boolean avatarExists = false;
				
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
								avatarExists = true;
								holder.imageView.setImageBitmap(bitmap);
								holder.contactInitialLetter.setVisibility(View.GONE);
							}
						}
					}
					
					if(request.getParamType()==1){
						log("(1)request.getText(): "+request.getText());
						holder.nameText=request.getText();
						holder.name=true;
					}
					else if(request.getParamType()==2){
						log("(2)request.getText(): "+request.getText());
						holder.firstNameText = request.getText();
						holder.firstName = true;
					}
					if(holder.name&&holder.firstName){
						holder.textViewContactName.setText(holder.nameText+" "+holder.firstNameText);
						holder.name= false;
						holder.firstName = false;
					}
					
//					if (!avatarExists){
//						createDefaultAvatar();
//					}
				}
			}
//			else{
//				if (holder.contactMail.compareTo(request.getEmail()) == 0){
//					createDefaultAvatar();
//				}
//			}
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

	public MegaContactRequestListAdapter(Context _context, ArrayList<MegaContactRequest> _contacts, ImageView _emptyImageView,TextView _emptyTextView, ListView _listView, int type) {
		log("new adapter");
		this.context = _context;
		this.contacts = _contacts;
		this.positionClicked = -1;
		this.type = type;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		emptyImageViewFragment = _emptyImageView;
		emptyTextViewFragment = _emptyTextView;
		listFragment = _listView;
		
		if(contacts!=null)
    	{
    		log("Number of requests: "+contacts.size());
    	}
    	else{
    		log("Number of requests: NULL");
    	}
	}
	
	/*private view holder class*/
    private class ViewHolderContactsRequestList {
    	public CheckBox checkbox;
    	RoundedImageView imageView;
    	TextView contactInitialLetter;
//        ImageView imageView;
        TextView textViewContactName;
        TextView textViewContent;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
//        ImageView arrowSelection;
        LinearLayout optionsLayout;
        RelativeLayout optionReinvite;
//        ImageButton optionSend;
        RelativeLayout optionDelete;
        int currentPosition;
        String contactMail;
    	boolean name = false;
    	boolean firstName = false;
    	String nameText;
    	String firstNameText;
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		log("getView");
	
		final int _position = position;
		
		ViewHolderContactsRequestList holder = null;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);    	   
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_contact_request_list, parent, false);
			holder = new ViewHolderContactsRequestList();
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.contact_request_list_checkbox);
			holder.checkbox.setClickable(false);
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.contact_request_list_item_layout);
			holder.imageView = (RoundedImageView) convertView.findViewById(R.id.contact_request_list_thumbnail);	
			holder.contactInitialLetter = (TextView) convertView.findViewById(R.id.contact_request_list_initial_letter);
			holder.textViewContactName = (TextView) convertView.findViewById(R.id.contact_request_list_name);
			holder.textViewContent = (TextView) convertView.findViewById(R.id.contact_request_list_content);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.contact_request_list_three_dots);
			holder.optionsLayout = (LinearLayout) convertView.findViewById(R.id.contact_request_list_options);
			holder.optionReinvite = (RelativeLayout) convertView.findViewById(R.id.contact_list_option_reinvite_layout);
//			holder.optionProperties.setPadding(Util.px2dp((70*scaleW), outMetrics), Util.px2dp((20*scaleH), outMetrics), 0, 0);
			holder.optionDelete = (RelativeLayout) convertView.findViewById(R.id.contact_list_option_delete_layout);
//			holder.optionShare.setPadding(Util.px2dp((70*scaleW), outMetrics), Util.px2dp((20*scaleH), outMetrics), 0, 0);
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderContactsRequestList) convertView.getTag();
		}

		holder.currentPosition = position;
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		
		log("Get the MegaContactRequest");
		MegaContactRequest contact = (MegaContactRequest) getItem(position);
		
		if (!multipleSelect) {
			holder.checkbox.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
		} else {
			holder.checkbox.setVisibility(View.VISIBLE);
//			holder.arrowSelection.setVisibility(View.GONE);
			holder.imageButtonThreeDots.setVisibility(View.GONE);

			SparseBooleanArray checkedItems = listFragment.getCheckedItemPositions();
			if (checkedItems.get(position, false) == true) {
				holder.checkbox.setChecked(true);
			} else {
				holder.checkbox.setChecked(false);
			}
		}
		
		createDefaultAvatar(holder);
		
		if(type==ManagerActivity.OUTGOING_REQUEST_ADAPTER)
		{
			holder.contactMail = contact.getTargetEmail();
			holder.textViewContactName.setText(contact.getTargetEmail());
			log("--------------user target: "+contact.getTargetEmail());
		}
		else{
			//Incoming request
						
			holder.contactMail = contact.getSourceEmail();
			log("--------------user source: "+contact.getSourceEmail());
			
			UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);
			
			holder.textViewContactName.setText(contact.getSourceEmail());
			holder.name=false;
			holder.firstName=false;
			MegaUser user = megaApi.getContact(contact.getSourceEmail());
			if(user!=null)
			{
				megaApi.getUserAttribute(user, 1, listener);
				megaApi.getUserAttribute(user, 2, listener);
			}			
			
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
							megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getSourceEmail() + ".jpg", listener);
						}
						else{
							megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + contact.getSourceEmail() + ".jpg", listener);
						}
					}
					else{
						holder.contactInitialLetter.setVisibility(View.GONE);
						holder.imageView.setImageBitmap(bitmap);
					}
				}
				else{
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getSourceEmail() + ".jpg", listener);	
					}
					else{
						megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + contact.getSourceEmail() + ".jpg", listener);	
					}			
				}
			}	
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getSourceEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + contact.getSourceEmail() + ".jpg", listener);
				}
			}
			
			
		}
		

//		holder.name=false;
//		holder.firstName=false;
//		megaApi.getUserAttribute(contact, 1, listener);
//		megaApi.getUserAttribute(contact, 2, listener);
		
		int status = contact.getStatus();
		switch(status)
		{
			case MegaContactRequest.STATUS_ACCEPTED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (ACCEPTED)");
				break;
			}
			case MegaContactRequest.STATUS_DELETED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (DELETED)");
				break;
			}
			case MegaContactRequest.STATUS_DENIED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (DENIED)");
				break;
			}
			case MegaContactRequest.STATUS_IGNORED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (IGNORED)");
				break;
			}
			case MegaContactRequest.STATUS_REMINDED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (REMINDED)");
				break;
			}
			case MegaContactRequest.STATUS_UNRESOLVED:
			{
				holder.textViewContent.setText(""+DateUtils.getRelativeTimeSpanString(contact.getCreationTime() * 1000)+" (PENDING)");
				break;
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
//				list.smoothScrollToPosition(_position);
			}
			else{
//				holder.arrowSelection.setVisibility(View.GONE);
				LayoutParams params = holder.optionsLayout.getLayoutParams();
				params.height = 0;
				holder.itemLayout.setBackgroundColor(Color.WHITE);
				holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
			}
		}
		else{
//			holder.arrowSelection.setVisibility(View.GONE);
			LayoutParams params = holder.optionsLayout.getLayoutParams();
			params.height = 0;
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.imageButtonThreeDots.setImageResource(R.drawable.action_selector_ic);
		}
		
		holder.optionReinvite.setTag(holder);
		holder.optionReinvite.setOnClickListener(this);
		holder.optionDelete.setOnClickListener(this);
		holder.optionDelete.setTag(holder);
		
		return convertView;
	}
	
	public void createDefaultAvatar(ViewHolderContactsRequestList holder){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(ManagerActivity.DEFAULT_AVATAR_WIDTH_HEIGHT,ManagerActivity.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(context.getResources().getColor(R.color.color_default_avatar_mega));
		
		int radius; 
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
        	radius = defaultAvatar.getWidth()/2;
        else
        	radius = defaultAvatar.getHeight()/2;
        
		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		holder.imageView.setImageBitmap(defaultAvatar);
		
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = context.getResources().getDisplayMetrics().density;
	    
	    int avatarTextSize = getAvatarTextSize(density);
	    log("DENSITY: " + density + ":::: " + avatarTextSize);
	    if (holder.contactMail != null){
		    if (holder.contactMail.length() > 0){
		    	String firstLetter = holder.contactMail.charAt(0) + "";
		    	firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		    	holder.contactInitialLetter.setVisibility(View.VISIBLE);
		    	holder.contactInitialLetter.setText(firstLetter);
		    	holder.contactInitialLetter.setTextSize(32);
		    	holder.contactInitialLetter.setTextColor(Color.WHITE);
		    }
	    }
	}
		
	private int getAvatarTextSize (float density){
		float textSize = 0.0f;
		
		if (density > 3.0){
			textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
		}
		else if (density > 2.0){
			textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
		}
		else if (density > 1.5){
			textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
		}
		else if (density > 1.0){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
		}
		else if (density > 0.75){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
		}
		else{
			textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f); 
		}
		
		return (int)textSize;
	}

	@Override
    public int getCount() {
        return contacts.size();
    }
 
	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
	}
	
	/*
	 * Get document at specified position
	 */
	public MegaContactRequest getContactAt(int position) {
		try {
			if (contacts != null) {
				return contacts.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
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
		ViewHolderContactsRequestList holder = (ViewHolderContactsRequestList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaContactRequest c = (MegaContactRequest) getItem(currentPosition);
		
		switch (v.getId()){
			case R.id.contact_list_option_reinvite_layout:{
				log("optionReinvite");
				((ManagerActivity) context).reinviteContact(c);				
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.contact_list_option_delete_layout:{
				log("Remove Invitation");
				((ManagerActivity) context).removeInvitationContact(c);
				notifyDataSetChanged();	
				break;
			}			
			case R.id.contact_request_list_three_dots:{
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
	
	public void setContacts (ArrayList<MegaContactRequest> contacts){
		log("SETCONTACTS!!!!");
		this.contacts = contacts;
		if(contacts!=null)
		{
			log("num requests: "+contacts.size());
		}
		positionClicked = -1;
		notifyDataSetChanged();
	}
	
	public String getDescription(ArrayList<MegaNode> nodes){
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
		Util.log("MegaContactRequestListAdapter", log);
	}
}
