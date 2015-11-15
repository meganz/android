package mega.privacy.android.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
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


public class MegaContactsListAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
	int positionClicked;
	ArrayList<MegaUser> contacts;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ListView listFragment;
	MegaApiAndroid megaApi;
	boolean multipleSelect;

	
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
	
	public MegaContactsListAdapter(Context _context, ArrayList<MegaUser> _contacts, ImageView _emptyImageView,TextView _emptyTextView, ListView _listView) {
		this.context = _context;
		this.contacts = _contacts;
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
		
		emptyImageViewFragment = _emptyImageView;
		emptyTextViewFragment = _emptyTextView;
		listFragment = _listView;
	}
	
	/*private view holder class*/
    private class ViewHolderContactsList {
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
        RelativeLayout optionProperties;
//        ImageButton optionSend;
        RelativeLayout optionShare;
        RelativeLayout optionRemove;
        RelativeLayout optionSendFile;
        int currentPosition;
        String contactMail;
    	boolean name = false;
    	boolean firstName = false;
    	String nameText;
    	String firstNameText;
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
			holder.checkbox = (CheckBox) convertView.findViewById(R.id.contact_list_checkbox);
			holder.checkbox.setClickable(false);
			holder.itemLayout = (RelativeLayout) convertView.findViewById(R.id.contact_list_item_layout);
			holder.imageView = (RoundedImageView) convertView.findViewById(R.id.contact_list_thumbnail);	
			holder.contactInitialLetter = (TextView) convertView.findViewById(R.id.contact_list_initial_letter);
			holder.textViewContactName = (TextView) convertView.findViewById(R.id.contact_list_name);
			holder.textViewContent = (TextView) convertView.findViewById(R.id.contact_list_content);
			holder.imageButtonThreeDots = (ImageButton) convertView.findViewById(R.id.contact_list_three_dots);
			holder.optionsLayout = (LinearLayout) convertView.findViewById(R.id.contact_list_options);
			holder.optionProperties = (RelativeLayout) convertView.findViewById(R.id.contact_list_option_properties_layout);
//			holder.optionProperties.setPadding(Util.px2dp((70*scaleW), outMetrics), Util.px2dp((20*scaleH), outMetrics), 0, 0);
			holder.optionShare = (RelativeLayout) convertView.findViewById(R.id.contact_list_option_share_layout);
//			holder.optionShare.setPadding(Util.px2dp((70*scaleW), outMetrics), Util.px2dp((20*scaleH), outMetrics), 0, 0);
			holder.optionRemove = (RelativeLayout) convertView.findViewById(R.id.contact_list_option_remove_layout);
			holder.optionSendFile = (RelativeLayout) convertView.findViewById(R.id.contact_list_option_send_file_layout);
//			holder.optionRemove.setPadding(Util.px2dp((70*scaleW), outMetrics), Util.px2dp((20*scaleH), outMetrics), 0, 0);
			convertView.setTag(holder);
		}
		else{
			holder = (ViewHolderContactsList) convertView.getTag();
		}

		holder.currentPosition = position;
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		
		MegaUser contact = (MegaUser) getItem(position);
		holder.contactMail = contact.getEmail();
		
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
		
		UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);
	
		holder.textViewContactName.setText(contact.getEmail());
		holder.name=false;
		holder.firstName=false;
		megaApi.getUserAttribute(contact, 1, listener);
		megaApi.getUserAttribute(contact, 2, listener);
		
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
					holder.contactInitialLetter.setVisibility(View.GONE);
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
		
		ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact);
		
		String sharedNodesDescription = getDescription(sharedNodes);
		
		holder.textViewContent.setText(sharedNodesDescription);
		
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
		
		holder.optionProperties.setTag(holder);
		holder.optionProperties.setOnClickListener(this);
		holder.optionShare.setOnClickListener(this);
		holder.optionShare.setTag(holder);
		holder.optionRemove.setOnClickListener(this);
		holder.optionRemove.setTag(holder);
		holder.optionSendFile.setOnClickListener(this);
		holder.optionSendFile.setTag(holder);
		
		return convertView;
	}
	
	public void createDefaultAvatar(ViewHolderContactsList holder){
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
	public MegaUser getContactAt(int position) {
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
		ViewHolderContactsList holder = (ViewHolderContactsList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaUser c = (MegaUser) getItem(currentPosition);
		
		switch (v.getId()){
			case R.id.contact_list_option_properties_layout:{
				log("optionProperties");
				Intent i = new Intent(context, ContactPropertiesMainActivity.class);
//				Intent i = new Intent(context, ContactPropertiesActivityLollipop.class);
				i.putExtra("name", c.getEmail());
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.contact_list_option_share_layout:{
				log("optionShare");
				positionClicked = -1;
				List<MegaUser> user = new ArrayList<MegaUser>();
				user.add(c);
				((ManagerActivity) context).pickFolderToShare(user);
				notifyDataSetChanged();
				break;
			}
			//TODO remove contact
			case R.id.contact_list_option_remove_layout:{
				log("Remove contact");
				positionClicked = -1;
				((ManagerActivity) context).removeContact(c);
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
			case R.id.contact_list_option_send_file_layout:{
				log("optionSendFile");
				positionClicked = -1;
				List<MegaUser> user = new ArrayList<MegaUser>();
				user.add(c);
				((ManagerActivity) context).pickContacToSendFile(user);
				notifyDataSetChanged();
				break;
			}			
		}
	}
	
	public void setContacts (ArrayList<MegaUser> contacts){
		this.contacts = contacts;
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
		Util.log("MegaContactsListAdapter", log);
	}
}
