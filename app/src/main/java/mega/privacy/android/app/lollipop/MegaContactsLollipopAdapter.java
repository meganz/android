package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class MegaContactsLollipopAdapter extends RecyclerView.Adapter<MegaContactsLollipopAdapter.ViewHolderContacts> implements OnClickListener, View.OnLongClickListener {
	
	public static final int ITEM_VIEW_TYPE_LIST = 0;
	public static final int ITEM_VIEW_TYPE_GRID = 1;
	public static final int ITEM_VIEW_TYPE_LIST_ADD_CONTACT = 2;
	
	Context context;
	int positionClicked;
	ArrayList<MegaUser> contacts;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	RecyclerView listFragment;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	boolean multipleSelect;
	DatabaseHandler dbH = null;
	private SparseBooleanArray selectedItems;
	ContactsFragmentLollipop fragment;
	int adapterType;
	SparseBooleanArray selectedContacts;
	
	private class UserAvatarListenerList implements MegaRequestListenerInterface{

		Context context;
		ViewHolderContacts holder;
		MegaContactsLollipopAdapter adapter;
		
		public UserAvatarListenerList(Context context, ViewHolderContacts holder, MegaContactsLollipopAdapter adapter) {
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
//								holder.imageView.setImageBitmap(bitmap);
								if (holder instanceof ViewHolderContactsGrid){
									((ViewHolderContactsGrid)holder).imageView.setImageBitmap(bitmap);
								}
								else if (holder instanceof ViewHolderContactsList){
									((ViewHolderContactsList)holder).imageView.setImageBitmap(bitmap);
								}
								holder.contactInitialLetter.setVisibility(View.GONE);
							}
						}
					}					
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

	public MegaContactsLollipopAdapter(Context _context, ContactsFragmentLollipop _fragment, ArrayList<MegaUser> _contacts, ImageView _emptyImageView,TextView _emptyTextView, RecyclerView _listView, int adapterType, SparseBooleanArray selectedContacts) {
		this.context = _context;
		this.contacts = _contacts;
		this.fragment = _fragment;
		this.positionClicked = -1;
		this.adapterType = adapterType;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		emptyImageViewFragment = _emptyImageView;
		emptyTextViewFragment = _emptyTextView;
		listFragment = _listView;
		this.selectedContacts = selectedContacts;
	}
	
	public MegaContactsLollipopAdapter(Context _context, ContactsFragmentLollipop _fragment, ArrayList<MegaUser> _contacts, ImageView _emptyImageView,TextView _emptyTextView, RecyclerView _listView, int adapterType) {
		this.context = _context;
		this.contacts = _contacts;
		this.fragment = _fragment;
		this.positionClicked = -1;
		this.adapterType = adapterType;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if(Util.isChatEnabled()){
			if (megaChatApi == null){
				megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
			}
		}
		
		emptyImageViewFragment = _emptyImageView;
		emptyTextViewFragment = _emptyTextView;
		listFragment = _listView;
		this.selectedContacts = null;
	}
	
	/*private view holder class*/
    public static class ViewHolderContacts extends RecyclerView.ViewHolder{
    	public ViewHolderContacts(View v) {
			super(v);
		}   	
		
    	TextView contactInitialLetter;
//        ImageView imageView;
        TextView textViewContactName;
        TextView textViewContent;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
        String contactMail;
    	String lastNameText="";
    	String firstNameText="";
    }
    
    public class ViewHolderContactsList extends ViewHolderContacts{
    	public ViewHolderContactsList(View v) {
			super(v);
		}
    	RoundedImageView imageView;
		ImageView contactStateIcon;
    }
    
    public class ViewHolderContactsGrid extends ViewHolderContacts{
    	public ViewHolderContactsGrid(View v) {
			super(v);
		}
    	ImageView imageView;
    }
    
	ViewHolderContactsList holderList = null;
	ViewHolderContactsGrid holderGrid = null;

	@Override
	public ViewHolderContacts onCreateViewHolder(ViewGroup parent, int viewType) {
		log("onCreateViewHolder");
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

	    dbH = DatabaseHandler.getDbHandler(context);
	    
	    if (viewType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST){
	   
		    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_list, parent, false);
	
		    holderList = new ViewHolderContactsList(v);
		    holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.contact_list_item_layout);
		    holderList.imageView = (RoundedImageView) v.findViewById(R.id.contact_list_thumbnail);	
		    holderList.contactInitialLetter = (TextView) v.findViewById(R.id.contact_list_initial_letter);
		    holderList.textViewContactName = (TextView) v.findViewById(R.id.contact_list_name);
		    holderList.textViewContent = (TextView) v.findViewById(R.id.contact_list_content);
		    holderList.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.contact_list_three_dots);
			holderList.contactStateIcon = (ImageView) v.findViewById(R.id.contact_list_drawable_state);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				log("onCreate: Landscape configuration");
				holderList.textViewContactName.setMaxWidth(Util.scaleWidthPx(280, outMetrics));
			}
			else{
				holderList.textViewContactName.setMaxWidth(Util.scaleWidthPx(230, outMetrics));
			}

		    holderList.itemLayout.setTag(holderList);
		    holderList.itemLayout.setOnClickListener(this);
			holderList.itemLayout.setOnLongClickListener(this);

			v.setTag(holderList);
	
			return holderList;
	    }
	    else if (viewType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID){
	    	
	    	View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_grid, parent, false);
	    	
	    	holderGrid = new ViewHolderContactsGrid(v);
	    	holderGrid.itemLayout = (RelativeLayout) v.findViewById(R.id.contact_grid_item_layout);
		    holderGrid.imageView = (ImageView) v.findViewById(R.id.contact_grid_thumbnail);	
		    holderGrid.contactInitialLetter = (TextView) v.findViewById(R.id.contact_grid_initial_letter);
		    holderGrid.textViewContactName = (TextView) v.findViewById(R.id.contact_grid_name);
		    holderGrid.textViewContent = (TextView) v.findViewById(R.id.contact_grid_content);
		    holderGrid.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.contact_grid_three_dots);

		    holderGrid.itemLayout.setTag(holderGrid);
		    holderGrid.itemLayout.setOnClickListener(this);
			holderGrid.itemLayout.setOnLongClickListener(this);
		    
		    v.setTag(holderGrid);
		    
	    	return holderGrid;	    	
	    }
		else if (viewType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT){
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_list, parent, false);

			holderList = new ViewHolderContactsList(v);
			holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.contact_list_item_layout);
			holderList.imageView = (RoundedImageView) v.findViewById(R.id.contact_list_thumbnail);
			holderList.contactInitialLetter = (TextView) v.findViewById(R.id.contact_list_initial_letter);
			holderList.textViewContactName = (TextView) v.findViewById(R.id.contact_list_name);
			holderList.textViewContent = (TextView) v.findViewById(R.id.contact_list_content);
			holderList.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.contact_list_three_dots);

			//Right margin
			RelativeLayout.LayoutParams actionButtonParams = (RelativeLayout.LayoutParams)holderList.imageButtonThreeDots.getLayoutParams();
			actionButtonParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0);
			holderList.imageButtonThreeDots.setLayoutParams(actionButtonParams);

			holderList.itemLayout.setTag(holderList);
			holderList.itemLayout.setOnClickListener(this);

			holderList.imageButtonThreeDots.setVisibility(View.GONE);

			v.setTag(holderList);

			return holderList;
		}
	    else{
	    	return null;
	    }
	}
	
	@Override
	public void onBindViewHolder(ViewHolderContacts holder, int position) {
		log("onBindViewHolder");
		
		if (adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			ViewHolderContactsList holderList = (ViewHolderContactsList) holder;
			onBindViewHolderList(holderList, position);
		}
		else if (adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID){
			ViewHolderContactsGrid holderGrid = (ViewHolderContactsGrid) holder;
			onBindViewHolderGrid(holderGrid, position);
		}
		else if (adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT){
			ViewHolderContactsList holderList = (ViewHolderContactsList) holder;
			onBindViewHolderListAddContact(holderList, position);
		}
	}
	
	public void onBindViewHolderGrid (ViewHolderContactsGrid holder, int position){
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		
		MegaUser contact = (MegaUser) getItem(position);
		holder.contactMail = contact.getEmail();
		
		if (!multipleSelect) {

			holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
		} 
		else {

			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid_long_click_lollipop));
			}
			else{
				holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
			}
		}

		MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contact.getHandle()));
		if(contactDB!=null){

			holder.firstNameText = contactDB.getName();
			holder.lastNameText = contactDB.getLastName();

			if(holder.firstNameText==null){
				holder.firstNameText="";
			}
			if(holder.lastNameText==null){
				holder.lastNameText="";
			}

			String fullName;

			if (holder.firstNameText.trim().length() <= 0){
				fullName = holder.lastNameText;
			}
			else{
				fullName = holder.firstNameText + " " + holder.lastNameText;
			}

			if (fullName.trim().length() <= 0){
				log("Put email as fullname");
				String email = contact.getEmail();
				String[] splitEmail = email.split("[@._]");
				fullName = splitEmail[0];
			}

			holder.textViewContactName.setText(fullName);
		}
		else{
			String email = contact.getEmail();
			String[] splitEmail = email.split("[@._]");
			String fullName = splitEmail[0];
			holder.textViewContactName.setText(fullName);
		}

		createDefaultAvatar(holder, contact);

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
				bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, bitmap, 3);
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
		
		String sharedNodesDescription = Util.getSubtitleDescription(sharedNodes);
		
		holder.textViewContent.setText(sharedNodesDescription);
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);	
	}

	public void onBindViewHolderListAddContact(ViewHolderContactsList holder, int position){
		log("onBindViewHolderListAddContact");

		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		holder.imageButtonThreeDots.setVisibility(View.GONE);

		MegaUser contact = (MegaUser) getItem(position);
		holder.contactMail = contact.getEmail();
		log("contact: "+contact.getEmail()+" handle: "+contact.getHandle());

		MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contact.getHandle()));
		if(contactDB!=null){
			holder.firstNameText = contactDB.getName();
			holder.lastNameText = contactDB.getLastName();

			String fullName;

			if (holder.firstNameText.trim().length() <= 0){
				fullName = holder.lastNameText;
			}
			else{
				fullName = holder.firstNameText + " " + holder.lastNameText;
			}

			if (fullName.trim().length() <= 0){
				log("Put email as fullname");
				String email = contact.getEmail();
				String[] splitEmail = email.split("[@._]");
				fullName = splitEmail[0];
			}

			holder.textViewContactName.setText(fullName);
		}
		else{
			String email = contact.getEmail();
			String[] splitEmail = email.split("[@._]");
			String fullName = splitEmail[0];
			holder.textViewContactName.setText(fullName);
		}

		createDefaultAvatar(holder, contact);

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

		if (selectedContacts != null) {
			for (int i = 0; i < selectedContacts.size(); i++) {
				if (selectedContacts.get(position) == true) {
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_file_list_selected_row));
				}
				else{
					holder.itemLayout.setBackgroundColor(Color.WHITE);
				}
			}
		}
		else{
			holder.itemLayout.setBackgroundColor(Color.WHITE);
		}

		holder.textViewContent.setText(holder.contactMail);

//		onBindViewHolderList(holder, position);
	}
	
	public void onBindViewHolderList(ViewHolderContactsList holder, int position){
		log("onBindViewHolderList");
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		
		MegaUser contact = (MegaUser) getItem(position);
		holder.contactMail = contact.getEmail();
		log("contact: "+contact.getEmail()+" handle: "+contact.getHandle());

		if(Util.isChatEnabled()){
			holder.contactStateIcon.setVisibility(View.VISIBLE);
			if (megaChatApi != null){
				int userStatus = megaChatApi.getUserOnlineStatus(contact.getHandle());
				if(userStatus == MegaChatApi.STATUS_ONLINE){
					log("This user is connected");
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_connected));
				}
				else{
					log("This user status is: "+userStatus);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_not_connected));
				}
			}
		}
		else{
			holder.contactStateIcon.setVisibility(View.GONE);
		}

		MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contact.getHandle()));
		if(contactDB!=null){
			holder.firstNameText = contactDB.getName();
			holder.lastNameText = contactDB.getLastName();

			String fullName;

			if (holder.firstNameText.trim().length() <= 0){
				fullName = holder.lastNameText;
			}
			else{
				fullName = holder.firstNameText + " " + holder.lastNameText;
			}

			if (fullName.trim().length() <= 0){
				log("Put email as fullname");
				String email = contact.getEmail();
				String[] splitEmail = email.split("[@._]");
				fullName = splitEmail[0];
			}

			holder.textViewContactName.setText(fullName);
		}
		else{
			String email = contact.getEmail();
			String[] splitEmail = email.split("[@._]");
			String fullName = splitEmail[0];
			holder.textViewContactName.setText(fullName);
		}

		if (!multipleSelect) {
			holder.itemLayout.setBackgroundColor(Color.WHITE);

			createDefaultAvatar(holder, contact);

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
		} else {

			if(this.isItemChecked(position)){
				holder.imageView.setImageResource(R.drawable.ic_multiselect);
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
			}
			else{
				holder.itemLayout.setBackgroundColor(Color.WHITE);

				createDefaultAvatar(holder, contact);

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
			}
		}
		
		ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact);
		
		String sharedNodesDescription = Util.getSubtitleDescription(sharedNodes);
		
		holder.textViewContent.setText(sharedNodesDescription);
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);	
	}
	
	public void createDefaultAvatar(ViewHolderContacts holder, MegaUser contact){
		log("createDefaultAvatar()");
		
		if (holder instanceof ViewHolderContactsList){
		
			Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(defaultAvatar);
			Paint p = new Paint();
			p.setAntiAlias(true);
			String color = megaApi.getUserAvatarColor(contact);
			if(color!=null){
				log("The color to set the avatar is "+color);
				p.setColor(Color.parseColor(color));
			}
			else{
				log("Default color to the avatar");
				p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
			}
			
			int radius; 
	        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
	        	radius = defaultAvatar.getWidth()/2;
	        else
	        	radius = defaultAvatar.getHeight()/2;
	        
			c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
			((ViewHolderContactsList)holder).imageView.setImageBitmap(defaultAvatar);

		}
		else if (holder instanceof ViewHolderContactsGrid){
			Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(defaultAvatar);
			Paint p = new Paint();
			p.setAntiAlias(true);
			String color = megaApi.getUserAvatarColor(contact);
			if(color!=null){
				log("The color to set the avatar is "+color);
				p.setColor(Color.parseColor(color));
			}
			else{
				log("Default color to the avatar");
				p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
			}

			p.setStyle(Paint.Style.FILL);

			Path path = ThumbnailUtilsLollipop.getRoundedRect(0, 0, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT , Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, 10, 10,true, true, false, false);

			c.drawPath(path,p);

			((ViewHolderContactsGrid)holder).imageView.setImageBitmap(defaultAvatar);
		}

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = context.getResources().getDisplayMetrics().density;

		String fullName;

		if(holder.firstNameText!=null){
			if (holder.firstNameText.trim().length() <= 0){
				fullName = holder.lastNameText;
			}
			else{
				if(holder.lastNameText!=null){
					fullName = holder.firstNameText + " " + holder.lastNameText;
				}
				else{
					fullName = holder.firstNameText;
				}
			}
		}
		else{
			if(holder.lastNameText!=null){
				fullName = holder.lastNameText;
			}
			else{
				fullName="";
			}
		}

		if (fullName.trim().length() <= 0){
			log("Put email as fullname");
			String email = contact.getEmail();
			String[] splitEmail = email.split("[@._]");
			fullName = splitEmail[0];
		}

		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);
		boolean setInitialByMail = false;

		if (fullName != null){
			if (fullName.trim().length() > 0){
				String firstLetter = fullName.charAt(0) + "";
				firstLetter = firstLetter.toUpperCase(Locale.getDefault());
				holder.contactInitialLetter.setText(firstLetter);
				holder.contactInitialLetter.setTextColor(Color.WHITE);
				holder.contactInitialLetter.setVisibility(View.VISIBLE);
			}else{
				setInitialByMail=true;
			}
		}
		else{
			setInitialByMail=true;
		}
		if(setInitialByMail){
			if (holder.contactMail != null){
				if (holder.contactMail.length() > 0){
					log("email TEXT: " + holder.contactMail);
					log("email TEXT AT 0: " + holder.contactMail.charAt(0));
					String firstLetter = holder.contactMail.charAt(0) + "";
					firstLetter = firstLetter.toUpperCase(Locale.getDefault());
					holder.contactInitialLetter.setText(firstLetter);
					holder.contactInitialLetter.setTextColor(Color.WHITE);
					holder.contactInitialLetter.setVisibility(View.VISIBLE);
				}
			}
		}
		if (adapterType == ITEM_VIEW_TYPE_LIST){
			holder.contactInitialLetter.setTextSize(24);
		}
		else if (adapterType == ITEM_VIEW_TYPE_GRID){
			holder.contactInitialLetter.setTextSize(64);
		}
		else if (adapterType == ITEM_VIEW_TYPE_LIST_ADD_CONTACT){
			holder.contactInitialLetter.setTextSize(24);
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
    public int getItemCount() {
        return contacts.size();
    }
	
	@Override
	public int getItemViewType(int position) {
		return adapterType;
	}
	
	public Object getItem(int position) {
		log("getItem");
		return contacts.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		log("setPositionClicked: "+p);
		positionClicked = p;
		notifyDataSetChanged();
	}
	
	public void setAdapterType(int adapterType){
		this.adapterType = adapterType;
	}
 
	public boolean isMultipleSelect() {
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
		}
		if(this.multipleSelect)
		{
			selectedItems = new SparseBooleanArray();
		}
	}

	public void toggleAllSelection(int pos) {
		log("toggleSelection: "+pos);
		final int positionToflip = pos;

		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}

		if (adapterType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			log("adapter type is LIST");
			MegaContactsLollipopAdapter.ViewHolderContactsList view = (MegaContactsLollipopAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
			if(view!=null){
				log("Start animation: "+pos);
				Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
				flipAnimation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						if (selectedItems.size() <= 0){
							((ContactsFragmentLollipop) fragment).hideMultipleSelect();
						}
						notifyItemChanged(positionToflip);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}
				});
				view.imageView.startAnimation(flipAnimation);
			}
			else{
				log("NULL view pos: "+positionToflip);
				notifyItemChanged(pos);
			}
		}
		else {
			log("adapter type is GRID");
			if (selectedItems.size() <= 0){
				((ContactsFragmentLollipop) fragment).hideMultipleSelect();
			}
			notifyItemChanged(positionToflip);
		}
	}
	
	public void toggleSelection(int pos) {
		log("toggleSelection");

		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);

		if (adapterType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST) {
			log("adapter type is LIST");
			MegaContactsLollipopAdapter.ViewHolderContactsList view = (MegaContactsLollipopAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
			if(view!=null){
				log("Start animation: "+pos);
				Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
				flipAnimation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						if (selectedItems.size() <= 0){
							((ContactsFragmentLollipop) fragment).hideMultipleSelect();
						}
					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}
				});
				view.imageView.startAnimation(flipAnimation);
			}
		}
		else{
			log("adapter type is GRID");
			if (selectedItems.size() <= 0){
				((ContactsFragmentLollipop) fragment).hideMultipleSelect();
			}
		}
	}
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleSelection(i);
			}
		}
	}

	public void clearSelections() {
		log("clearSelections");
		for (int i= 0; i<this.getItemCount();i++){
			if(isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}
	
	private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

	public int getSelectedItemCount() {
		return selectedItems.size();
	}

	public List<Integer> getSelectedItems() {
		List<Integer> items = new ArrayList<Integer>(selectedItems.size());
		for (int i = 0; i < selectedItems.size(); i++) {
			items.add(selectedItems.keyAt(i));
		}
		return items;
	}	
	
	/*
	 * Get list of all selected contacts
	 */
	public ArrayList<MegaUser> getSelectedUsers() {
		ArrayList<MegaUser> users = new ArrayList<MegaUser>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaUser u = getContactAt(selectedItems.keyAt(i));
				if (u != null){
					users.add(u);
				}
			}
		}
		return users;
	}	
	
	/*
	 * Get contact at specified position
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
	public void onClick(View v) {
		log("onClick _ adapterType: " + adapterType);
		if (!(adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT)){
			ViewHolderContacts holder = (ViewHolderContacts) v.getTag();
			int currentPosition = holder.getAdapterPosition();
			MegaUser c = (MegaUser) getItem(currentPosition);
			
			switch (v.getId()){			
				case R.id.contact_list_three_dots:
				case R.id.contact_grid_three_dots:{
					log("click contact three dots!");
					((ManagerActivityLollipop) context).showContactOptionsPanel(c);
					break;
				}			
				case R.id.contact_list_item_layout:
				case R.id.contact_grid_item_layout:{
					log("contact_item_layout");
					if (fragment != null){
						fragment.itemClick(currentPosition);
					}
					break;
				}
			}
		}
		else {
			if(!isMultipleSelect()){
				ViewHolderContactsList holder = (ViewHolderContactsList) v.getTag();
				int currentPosition = holder.getAdapterPosition();
				MegaUser c = (MegaUser) getItem(currentPosition);

				switch (v.getId()){
					case R.id.contact_list_item_layout: {
						log("contact_list_item_layout");
						((AddContactActivityLollipop) context).itemClick(c.getEmail(), currentPosition);
						break;
					}
				}
			}
		}
	}

	@Override
	public boolean onLongClick(View view) {
		log("OnLongCLick");
		ViewHolderContacts holder = (ViewHolderContacts) view.getTag();
		int currentPosition = holder.getAdapterPosition();

		if (!(adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT)){
			fragment.activateActionMode();
			fragment.itemClick(currentPosition);
		}

		return true;
	}

	public void setSelectedContacts(SparseBooleanArray selectedContacts){
		this.selectedContacts = selectedContacts;
		notifyDataSetChanged();
	}

	public MegaUser getDocumentAt(int position) {
		if(position < contacts.size())
		{
			return contacts.get(position);
		}

		return null;
	}
	
	public void setContacts (ArrayList<MegaUser> contacts){
		log("setContacts");
		this.contacts = contacts;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	private static void log(String log) {
		Util.log("MegaContactsLollipopAdapter", log);
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
