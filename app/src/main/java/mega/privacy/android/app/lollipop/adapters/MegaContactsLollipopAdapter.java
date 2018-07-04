package mega.privacy.android.app.lollipop.adapters;

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
import android.util.TypedValue;
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
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;


public class MegaContactsLollipopAdapter extends RecyclerView.Adapter<MegaContactsLollipopAdapter.ViewHolderContacts> implements OnClickListener, View.OnLongClickListener {
	
	public static final int ITEM_VIEW_TYPE_LIST = 0;
	public static final int ITEM_VIEW_TYPE_GRID = 1;
	public static final int ITEM_VIEW_TYPE_LIST_ADD_CONTACT = 2;
	public static int MAX_WIDTH_CONTACT_NAME_LAND=450;
	public static int MAX_WIDTH_CONTACT_NAME_PORT=200;

	private Context context;
	private int positionClicked;
	private ArrayList<MegaContactAdapter> contacts;
	private RecyclerView listFragment;
	private MegaApiAndroid megaApi;
	private MegaChatApiAndroid megaChatApi;
	private boolean multipleSelect;
	private DatabaseHandler dbH = null;
	private SparseBooleanArray selectedItems;
	private ContactsFragmentLollipop fragment;
	private int adapterType;
	private SparseBooleanArray selectedContacts;
	

	public MegaContactsLollipopAdapter(Context _context, ContactsFragmentLollipop _fragment, ArrayList<MegaContactAdapter> _contacts, RecyclerView _listView, int adapterType) {
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
		
		listFragment = _listView;
	}
	
	/*private view holder class*/
    public static class ViewHolderContacts extends RecyclerView.ViewHolder{
    	public ViewHolderContacts(View v) {
			super(v);
		}

		public TextView contactInitialLetter;
//        ImageView imageView;
        TextView textViewContactName;
        TextView textViewContent;
        RelativeLayout itemLayout;
        public String contactMail;
    }
    
    public class ViewHolderContactsList extends ViewHolderContacts{
    	public ViewHolderContactsList(View v) {
			super(v);
		}
    	public RoundedImageView imageView;
		ImageView contactStateIcon;
		RelativeLayout threeDotsLayout;
    }
    
    public class ViewHolderContactsGrid extends ViewHolderContacts{
    	public ViewHolderContactsGrid(View v) {
			super(v);
		}
		public ImageView imageView;
		ImageView contactStateIcon;
		ImageButton imageButtonThreeDots;
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
		    holderList.threeDotsLayout = (RelativeLayout) v.findViewById(R.id.contact_list_three_dots_layout);
			holderList.contactStateIcon = (ImageView) v.findViewById(R.id.contact_list_drawable_state);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				log("onCreate: Landscape configuration");
				//holderList.textViewContactName.setMaxWidth(Util.scaleWidthPx(290, outMetrics));
				float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_LAND, context.getResources().getDisplayMetrics());
				holderList.textViewContactName.setMaxWidth((int) width);
			}
			else{
				//holderList.textViewContactName.setMaxWidth(Util.scaleWidthPx(240, outMetrics));
				float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_PORT, context.getResources().getDisplayMetrics());
				holderList.textViewContactName.setMaxWidth((int) width);
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
			holderGrid.contactStateIcon = (ImageView) v.findViewById(R.id.contact_grid_drawable_state);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				holderGrid.textViewContactName.setMaxWidth(Util.scaleWidthPx(70, outMetrics));
			}
			else{
				holderGrid.textViewContactName.setMaxWidth(Util.scaleWidthPx(120, outMetrics));
			}

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

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_LAND, context.getResources().getDisplayMetrics());
				holderList.textViewContactName.setMaxWidth((int) width);
			}
			else{
				float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_PORT, context.getResources().getDisplayMetrics());
				holderList.textViewContactName.setMaxWidth((int) width);
			}

			holderList.textViewContent = (TextView) v.findViewById(R.id.contact_list_content);
			holderList.threeDotsLayout = (RelativeLayout) v.findViewById(R.id.contact_list_three_dots_layout);

			//Right margin
			RelativeLayout.LayoutParams actionButtonParams = (RelativeLayout.LayoutParams)holderList.threeDotsLayout.getLayoutParams();
			actionButtonParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0);
			holderList.threeDotsLayout.setLayoutParams(actionButtonParams);

			holderList.itemLayout.setTag(holderList);
			holderList.itemLayout.setOnClickListener(this);

			holderList.threeDotsLayout.setVisibility(View.GONE);

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
		
		MegaContactAdapter contact = (MegaContactAdapter) getItem(position);
		holder.contactMail = contact.getMegaUser().getEmail();

		if(Util.isChatEnabled()){
			holder.contactStateIcon.setVisibility(View.VISIBLE);
			if (megaChatApi != null){
				int userStatus = megaChatApi.getUserOnlineStatus(contact.getMegaUser().getHandle());
				if(userStatus == MegaChatApi.STATUS_ONLINE){
					log("This user is connected");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online_grid));
				}
				else if(userStatus == MegaChatApi.STATUS_AWAY){
					log("This user is away");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away_grid));
				}
				else if(userStatus == MegaChatApi.STATUS_BUSY){
					log("This user is busy");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy_grid));
				}
				else if(userStatus == MegaChatApi.STATUS_OFFLINE){
					log("This user is offline");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline_grid));
				}
				else if(userStatus == MegaChatApi.STATUS_INVALID){
					log("INVALID status: "+userStatus);
					holder.contactStateIcon.setVisibility(View.GONE);
				}
				else{
					log("This user status is: "+userStatus);
					holder.contactStateIcon.setVisibility(View.GONE);
				}
			}
		}
		else{
			holder.contactStateIcon.setVisibility(View.GONE);
		}
		
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

		holder.textViewContactName.setText(contact.getFullName());

		createDefaultAvatar(holder, contact);

		UserAvatarListener listener = new UserAvatarListener(context, holder);

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
						megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
				}
				else{
					log("Do not ask for user avatar - its in cache: "+avatar.getAbsolutePath());
					holder.contactInitialLetter.setVisibility(View.GONE);
					holder.imageView.setImageBitmap(bitmap);
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
				}			
			}
		}	
		else{
			if (context.getExternalCacheDir() != null){
				megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
			}
			else{
				megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
			}
		}
		
		ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact.getMegaUser());
		
		String sharedNodesDescription = Util.getSubtitleDescription(sharedNodes);
		
		holder.textViewContent.setText(sharedNodesDescription);
		
		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);	
	}

	public void onBindViewHolderListAddContact(ViewHolderContactsList holder, int position){
		log("onBindViewHolderListAddContact");

		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		holder.threeDotsLayout.setVisibility(View.GONE);

		MegaContactAdapter contact = (MegaContactAdapter) getItem(position);
		holder.contactMail = contact.getMegaUser().getEmail();
		log("contact: "+contact.getMegaUser().getEmail()+" handle: "+contact.getMegaUser().getHandle());

		holder.textViewContactName.setText(contact.getFullName());

		createDefaultAvatar(holder, contact);

		UserAvatarListener listener = new UserAvatarListener(context, holder);

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
						megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
				}
				else{
					holder.contactInitialLetter.setVisibility(View.GONE);
					holder.imageView.setImageBitmap(bitmap);
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
				}
			}
		}
		else{
			if (context.getExternalCacheDir() != null){
				megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
			}
			else{
				megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
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
		
		MegaContactAdapter contact = (MegaContactAdapter) getItem(position);
		holder.contactMail = contact.getMegaUser().getEmail();
		log("contact: "+contact.getMegaUser().getEmail()+" handle: "+contact.getMegaUser().getHandle());

		if(Util.isChatEnabled()){
			holder.contactStateIcon.setVisibility(View.VISIBLE);
			if (megaChatApi != null){
				int userStatus = megaChatApi.getUserOnlineStatus(contact.getMegaUser().getHandle());
				if(userStatus == MegaChatApi.STATUS_ONLINE){
					log("This user is connected");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
				}
				else if(userStatus == MegaChatApi.STATUS_AWAY){
					log("This user is away");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
				}
				else if(userStatus == MegaChatApi.STATUS_BUSY){
					log("This user is busy");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
				}
				else if(userStatus == MegaChatApi.STATUS_OFFLINE){
					log("This user is offline");
					holder.contactStateIcon.setVisibility(View.VISIBLE);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
				}
				else if(userStatus == MegaChatApi.STATUS_INVALID){
					log("INVALID status: "+userStatus);
					holder.contactStateIcon.setVisibility(View.GONE);
				}
				else{
					log("This user status is: "+userStatus);
					holder.contactStateIcon.setVisibility(View.GONE);
				}
			}
		}
		else{
			holder.contactStateIcon.setVisibility(View.GONE);
		}

		holder.textViewContactName.setText(contact.getFullName());

		if (!multipleSelect) {
			holder.itemLayout.setBackgroundColor(Color.WHITE);

			createDefaultAvatar(holder, contact);

			UserAvatarListener listener = new UserAvatarListener(context, holder);

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
							megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
						}
						else{
							megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
						}
					}
					else{
						log("Do not ask for user avatar - its in cache: "+avatar.getAbsolutePath());
						holder.contactInitialLetter.setVisibility(View.GONE);
						holder.imageView.setImageBitmap(bitmap);
					}
				}
				else{
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
				}
			}
		} else {

			if(this.isItemChecked(position)){
				holder.imageView.setImageResource(R.drawable.ic_select_avatar);
				holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
			}
			else{
				holder.itemLayout.setBackgroundColor(Color.WHITE);

				createDefaultAvatar(holder, contact);

				UserAvatarListener listener = new UserAvatarListener(context, holder);

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
								megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
							}
							else{
								megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
							}
						}
						else{
							holder.contactInitialLetter.setVisibility(View.GONE);
							holder.imageView.setImageBitmap(bitmap);
						}
					}
					else{
						if (context.getExternalCacheDir() != null){
							megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
						}
						else{
							megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
						}
					}
				}
				else{
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact.getMegaUser(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact.getMegaUser(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMegaUser().getEmail() + ".jpg", listener);
					}
				}
			}
		}

		ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact.getMegaUser());

		String sharedNodesDescription = Util.getSubtitleDescription(sharedNodes);

		holder.textViewContent.setText(sharedNodesDescription);
		
		holder.threeDotsLayout.setTag(holder);
		holder.threeDotsLayout.setOnClickListener(this);
	}
	
	public void createDefaultAvatar(ViewHolderContacts holder, MegaContactAdapter contact){
		log("createDefaultAvatar()");
		
		if (holder instanceof ViewHolderContactsList){
		
			Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(defaultAvatar);
			Paint p = new Paint();
			p.setAntiAlias(true);
			String color = megaApi.getUserAvatarColor(contact.getMegaUser());
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
			String color = megaApi.getUserAvatarColor(contact.getMegaUser());
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

		String fullName = contact.getFullName();

		int avatarTextSize = Util.getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		String firstLetter = fullName.charAt(0) + "";
		firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		holder.contactInitialLetter.setText(firstLetter);
		holder.contactInitialLetter.setTextColor(Color.WHITE);
		holder.contactInitialLetter.setVisibility(View.VISIBLE);

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

//	public void toggleAllSelection(int pos) {
//		log("toggleAllSelection: "+pos);
//		final int positionToflip = pos;
//
//		if (selectedItems.get(pos, false)) {
//			log("delete pos: "+pos);
//			selectedItems.delete(pos);
//		}
//		else {
//			log("PUT pos: "+pos);
//			selectedItems.put(pos, true);
//		}
//		notifyItemChanged(positionToflip);
//
//		if (adapterType == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
//			log("adapter type is LIST");
//			MegaContactsLollipopAdapter.ViewHolderContactsList view = (MegaContactsLollipopAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
//			if(view!=null){
//				log("Start animation: "+pos);
////				Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
//
////				view.imageView.startAnimation(flipAnimation);
//				ObjectAnimator imageViewObjectAnimator = ObjectAnimator.ofFloat(view.imageView ,
//						"rotationY", 360f, 0f);
//				imageViewObjectAnimator.setDuration(500);
//
//				imageViewObjectAnimator.addListener(new Animator.AnimatorListener() {
//					@Override
//					public void onAnimationStart(Animator animation) {
//						log("START animation--");
//					}
//
//					@Override
//					public void onAnimationEnd(Animator animation) {
//						if (selectedItems.size() <= 0){
//							log("Force hideMultiselect");
//							((ContactsFragmentLollipop) fragment).hideMultipleSelect();
//						}
//						log("END animation--");
//
//					}
//
//					@Override
//					public void onAnimationCancel(Animator animation) {
//
//					}
//
//					@Override
//					public void onAnimationRepeat(Animator animation) {
//
//					}
//				});
//
//				imageViewObjectAnimator.start();
//			}
//			else{
//				log("NULL view pos: "+positionToflip);
//                log("multiselect: "+isMultipleSelect());
//				notifyItemChanged(pos);
//			}
//		}
//		else {
//			log("adapter type is GRID");
//			if (selectedItems.size() <= 0){
//				((ContactsFragmentLollipop) fragment).hideMultipleSelect();
//			}
//			notifyItemChanged(positionToflip);
//		}
//	}
	
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
				toggleSelection(i);
			}
		}
	}

	public void clearSelectionsNoAnimations() {
		log("clearSelections");
		for (int i= 0; i<this.getItemCount();i++){
			if(isItemChecked(i)){
				selectedItems.delete(i);
				notifyItemChanged(i);
			}
		}
	}

	private boolean isItemChecked(int position) {
		if(selectedItems!=null){
			return selectedItems.get(position);
		}
		return false;
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
		MegaContactAdapter megaContactAdapter = null;
		try {
			if (contacts != null) {
				megaContactAdapter = contacts.get(position);
				return megaContactAdapter.getMegaUser();
			}
		} catch (IndexOutOfBoundsException e) {
			log("EXCEPTION: "+e.getMessage());
		}
		return null;
	}
    
	@Override
	public void onClick(View v) {
		log("onClick _ adapterType: " + adapterType);

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if (!(adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT)){
			ViewHolderContacts holder = (ViewHolderContacts) v.getTag();
			int currentPosition = holder.getAdapterPosition();
			try {
				MegaContactAdapter c = (MegaContactAdapter) getItem(currentPosition);

				switch (v.getId()){
					case R.id.contact_list_three_dots_layout:
					case R.id.contact_grid_three_dots:{
						log("click contact three dots!");
						if(multipleSelect){
							if (fragment != null){
								fragment.itemClick(currentPosition);
							}
						}
						else{
							((ManagerActivityLollipop) context).showContactOptionsPanel(c);
						}

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
			} catch (IndexOutOfBoundsException e) {
				log("EXCEPTION: "+e.getMessage());
			}
		}
		else {
			if(!isMultipleSelect()){
				ViewHolderContactsList holder = (ViewHolderContactsList) v.getTag();
				int currentPosition = holder.getAdapterPosition();
				try {
					MegaContactAdapter c = (MegaContactAdapter) getItem(currentPosition);
					switch (v.getId()){
						case R.id.contact_list_item_layout: {
							log("contact_list_item_layout");
							((AddContactActivityLollipop) context).itemClick(c.getMegaUser().getEmail(), currentPosition);
							break;
						}
					}
				} catch (IndexOutOfBoundsException e) {
					log("EXCEPTION: "+e.getMessage());
				}
			}
		}
	}

	@Override
	public boolean onLongClick(View view) {
		log("OnLongCLick");
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		ViewHolderContacts holder = (ViewHolderContacts) view.getTag();
		int currentPosition = holder.getAdapterPosition();

		fragment.activateActionMode();
		fragment.itemClick(currentPosition);

		return true;
	}

	public void setSelectedContacts(SparseBooleanArray selectedContacts){
		this.selectedContacts = selectedContacts;
		notifyDataSetChanged();
	}

	public MegaUser getDocumentAt(int position) {
		MegaContactAdapter megaContactAdapter = null;
		if(position < contacts.size())
		{
			megaContactAdapter = contacts.get(position);
			return megaContactAdapter.getMegaUser();
		}

		return null;
	}
	
	public void setContacts (ArrayList<MegaContactAdapter> contacts){
		log("setContacts");
		this.contacts = contacts;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	public void updateContactStatus(int position, long userHandle, int state){
		log("updateContactStatus: "+position);

		if (adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			holderList = (ViewHolderContactsList) listFragment.findViewHolderForAdapterPosition(position);
			if(holderList!=null){

				if(state == MegaChatApi.STATUS_ONLINE){
					log("This user is connected");
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
					holderList.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else if(state == MegaChatApi.STATUS_AWAY){
					log("This user is away");
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
					holderList.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else if(state == MegaChatApi.STATUS_BUSY){
					log("This user is busy");
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
					holderList.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else if(state == MegaChatApi.STATUS_OFFLINE){
					log("This user is offline");
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
					holderList.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else{
					log("This user status is: "+state);
					holderList.contactStateIcon.setVisibility(View.GONE);
				}
			}
			else{
				log("Holder is NULL");
				notifyItemChanged(position);
			}
		}else if (adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID){
			holderGrid = (ViewHolderContactsGrid) listFragment.findViewHolderForAdapterPosition(position);

			if(holderGrid!=null){

				if(state == MegaChatApi.STATUS_ONLINE){
					log("This user is connected");
					holderGrid.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online_grid));
					holderGrid.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else if(state == MegaChatApi.STATUS_AWAY){
					log("This user is away");
					holderGrid.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away_grid));
					holderGrid.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else if(state == MegaChatApi.STATUS_BUSY){
					log("This user is busy");
					holderGrid.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy_grid));
					holderGrid.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else if(state == MegaChatApi.STATUS_OFFLINE){
					log("This user is offline");
					holderGrid.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
					holderGrid.contactStateIcon.setVisibility(View.VISIBLE);
				}
				else{
					log("This user status is: "+state);
					holderGrid.contactStateIcon.setVisibility(View.GONE);
				}
			}
			else{
				log("Holder is NULL");
				notifyItemChanged(position);
			}
		}
	}

	/*public void startOneToOneChat(MegaUser user){
		log("startOneToOneChat");
		MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
		MegaChatPeerList peers = MegaChatPeerList.createInstance();
		if(chat==null){
			log("No chat, create it!");
			peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
			megaChatApi.createChat(false, peers, this);
		}
		else{
			log("There is already a chat, open it!");
			Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
			intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
			intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
			this.startActivity(intentOpenChat);
		}
	}*/

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
