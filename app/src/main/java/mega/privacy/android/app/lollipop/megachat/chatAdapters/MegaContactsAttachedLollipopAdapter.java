package mega.privacy.android.app.lollipop.megachat.chatAdapters;

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
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
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


public class MegaContactsAttachedLollipopAdapter extends RecyclerView.Adapter<MegaContactsAttachedLollipopAdapter.ViewHolderContacts> implements OnClickListener, View.OnLongClickListener {

	public static final int ITEM_VIEW_TYPE_LIST = 0;
	public static final int ITEM_VIEW_TYPE_GRID = 1;

	public static final int ITEM_VIEW_TYPE_CHAT_LIST = 2;

	Context context;
	int positionClicked;
	ArrayList<MegaContactDB> contacts;
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
		MegaContactsAttachedLollipopAdapter adapter;

		public UserAvatarListenerList(Context context, ViewHolderContacts holder, MegaContactsAttachedLollipopAdapter adapter) {
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
									bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, bitmap, 3);
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

	public MegaContactsAttachedLollipopAdapter(Context _context, ArrayList<MegaContactDB> _contacts, RecyclerView _listView) {
		this.context = _context;
		this.positionClicked = -1;
		this.adapterType = MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_CHAT_LIST;
		this.contacts = _contacts;

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		listFragment = _listView;
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
		ImageView contactStateIcon;
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

	    if (viewType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_LIST||viewType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_CHAT_LIST){

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
	    else if (viewType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_GRID){

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
	    else{
	    	return null;
	    }
	}

	@Override
	public void onBindViewHolder(ViewHolderContacts holder, int position) {
		log("onBindViewHolder");

		if (adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_LIST||adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_CHAT_LIST){
			ViewHolderContactsList holderList = (ViewHolderContactsList) holder;
			onBindViewHolderList(holderList, position);
		}
		else if (adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_GRID){
			ViewHolderContactsGrid holderGrid = (ViewHolderContactsGrid) holder;
			onBindViewHolderGrid(holderGrid, position);
		}
	}

	public void onBindViewHolderGrid (ViewHolderContactsGrid holder, int position){
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");

		MegaContactDB contact = (MegaContactDB) getItem(position);
		holder.contactMail = contact.getMail();

		MegaUser user = megaApi.getContact(contact.getMail());
		if(user!=null){
			holder.contactStateIcon.setVisibility(View.VISIBLE);
			if (megaChatApi != null){
				int userStatus = megaChatApi.getUserOnlineStatus(megaApi.base64ToUserHandle(contact.getHandle()));
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
			log("Offline non contact: ");
			holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
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

		holder.textViewContactName.setText(contact.getName());

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
						megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
					}
				}
				else{
					holder.contactInitialLetter.setVisibility(View.GONE);
					holder.imageView.setImageBitmap(bitmap);
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
				}
			}
		}
		else{
			if (context.getExternalCacheDir() != null){
				megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
			}
			else{
				megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
			}
		}

//		ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact.getMegaUser());

//		String sharedNodesDescription = Util.getSubtitleDescription(sharedNodes);

//		holder.textViewContent.setText(sharedNodesDescription);

		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
	}

	public void onBindViewHolderList(ViewHolderContactsList holder, int position){
		log("onBindViewHolderList");
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");

		MegaContactDB contact = (MegaContactDB) getItem(position);
		holder.contactMail = contact.getMail();
		log("contact: "+contact.getMail()+" handle: "+contact.getHandle());

		if(Util.isChatEnabled()){
			holder.contactStateIcon.setVisibility(View.VISIBLE);
			if (megaChatApi != null){
				int userStatus = megaChatApi.getUserOnlineStatus(megaApi.base64ToUserHandle(contact.getHandle()));
				if(userStatus == MegaChatApi.STATUS_ONLINE){
					log("This user is connected");
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
				}
				else if(userStatus == MegaChatApi.STATUS_AWAY){
					log("This user is away");
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
				}
				else if(userStatus == MegaChatApi.STATUS_BUSY){
					log("This user is busy");
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
				}
				else{
					log("This user status is: "+userStatus);
					holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
				}
			}
		}
		else{
			holder.contactStateIcon.setVisibility(View.GONE);
		}

		holder.textViewContactName.setText(contact.getName());

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
							megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
						}
						else{
							megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
						}
					}
					else{
						holder.contactInitialLetter.setVisibility(View.GONE);
						holder.imageView.setImageBitmap(bitmap);
					}
				}
				else{
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
					}
				}
			}
			else{
				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
				}
				else{
					megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
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
								megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
							}
							else{
								megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
							}
						}
						else{
							holder.contactInitialLetter.setVisibility(View.GONE);
							holder.imageView.setImageBitmap(bitmap);
						}
					}
					else{
						if (context.getExternalCacheDir() != null){
							megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
						}
						else{
							megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
						}
					}
				}
				else{
					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(contact.getMail(), context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(contact.getMail(), context.getCacheDir().getAbsolutePath() + "/" + contact.getMail() + ".jpg", listener);
					}
				}
			}
		}

		if (adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_CHAT_LIST){

			holder.textViewContent.setText(contact.getMail());
		}

		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
	}

	public void createDefaultAvatar(ViewHolderContacts holder, MegaContactDB contact){
		log("createDefaultAvatar()");

		if (holder instanceof ViewHolderContactsList){

			Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(defaultAvatar);
			Paint p = new Paint();
			p.setAntiAlias(true);
			String color = megaApi.getUserAvatarColor(contact.getHandle());
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
			String color = megaApi.getUserAvatarColor(contact.getHandle());
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

		String fullName = contact.getName();

		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);

		String firstLetter = fullName.charAt(0) + "";
		firstLetter = firstLetter.toUpperCase(Locale.getDefault());
		holder.contactInitialLetter.setText(firstLetter);
		holder.contactInitialLetter.setTextColor(Color.WHITE);
		holder.contactInitialLetter.setVisibility(View.VISIBLE);

		if (adapterType == ITEM_VIEW_TYPE_LIST||adapterType == ITEM_VIEW_TYPE_CHAT_LIST){
			holder.contactInitialLetter.setTextSize(24);
		}
		else if (adapterType == ITEM_VIEW_TYPE_GRID){
			holder.contactInitialLetter.setTextSize(64);
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
			MegaContactsAttachedLollipopAdapter.ViewHolderContactsList view = (MegaContactsAttachedLollipopAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
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
			MegaContactsAttachedLollipopAdapter.ViewHolderContactsList view = (MegaContactsAttachedLollipopAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
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
	public ArrayList<MegaContactDB> getSelectedUsers() {
		ArrayList<MegaContactDB> users = new ArrayList<MegaContactDB>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaContactDB u = getContactAt(selectedItems.keyAt(i));
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
	public MegaContactDB getContactAt(int position) {
		MegaContactDB megaContactDB = null;
		try {
			if (contacts != null) {
				megaContactDB = contacts.get(position);
				return megaContactDB;
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}
    
	@Override
	public void onClick(View v) {
		log("onClick _ adapterType: " + adapterType);

		if(!Util.isOnline(context)){
			if(context instanceof ManagerActivityLollipop){
				((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
			}
			return;
		}

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

		if(adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_CHAT_LIST){
			ViewHolderContacts holder = (ViewHolderContacts) v.getTag();
			int currentPosition = holder.getAdapterPosition();
			MegaContactDB c = (MegaContactDB) getItem(currentPosition);

			switch (v.getId()){
				case R.id.contact_list_three_dots:
				case R.id.contact_grid_three_dots:{
					log("click contact three dots!");
					if(multipleSelect){
//						if (fragment != null){
//							fragment.itemClick(currentPosition);
//						}
					}
					else{
						if((c.getMail().equals(megaChatApi.getMyEmail()))){
							((ContactAttachmentActivityLollipop) context).showSnackbar(context.getString(R.string.contact_is_me));
						}
						else{
							((ContactAttachmentActivityLollipop) context).showOptionsPanel(c.getMail());
						}
					}
					break;
				}
				case R.id.contact_list_item_layout:
				case R.id.contact_grid_item_layout:{
					log("contact_item_layout");
					((ContactAttachmentActivityLollipop) context).itemClick(currentPosition);
					break;
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

		return true;
	}

	public void setSelectedContacts(SparseBooleanArray selectedContacts){
		this.selectedContacts = selectedContacts;
		notifyDataSetChanged();
	}

	public MegaContactDB getDocumentAt(int position) {
		MegaContactDB megaContactAdapter = null;
		if(position < contacts.size())
		{
			megaContactAdapter = contacts.get(position);
			return megaContactAdapter;
		}

		return null;
	}
	
	public void setContacts (ArrayList<MegaContactDB> contacts){
		log("setContacts");
		this.contacts = contacts;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	public void updateContactStatus(int position, long userHandle, int state){
		log("updateContactStatus: "+position);

		if (adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			holderList = (ViewHolderContactsList) listFragment.findViewHolderForAdapterPosition(position);
			if(holderList!=null){

				if(state == MegaChatApi.STATUS_ONLINE){
					log("This user is connected");
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
				}
				else if(state == MegaChatApi.STATUS_AWAY){
					log("This user is away");
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
				}
				else if(state == MegaChatApi.STATUS_BUSY){
					log("This user is busy");
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
				}
				else{
					log("This user status is: "+state);
					holderList.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
				}

			}
			else{
				log("Holder is NULL");
				notifyItemChanged(position);
			}
		}
//		else if (adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID){
//			holderGrid = (ViewHolderContactsGrid) listFragment.findViewHolderForAdapterPosition(position);
//		}
	}

	private static void log(String log) {
		Util.log("MegaContactsAttachedLollipopAdapter", log);
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
