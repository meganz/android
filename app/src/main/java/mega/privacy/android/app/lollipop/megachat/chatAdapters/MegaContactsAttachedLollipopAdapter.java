package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
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

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;


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
			logDebug("onRequestStart()");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
			logDebug("onRequestFinish()");
			if (e.getErrorCode() == MegaError.API_OK){
				boolean avatarExists = false;

				if (holder.contactMail.compareTo(request.getEmail()) == 0){
					File avatar = buildAvatarFile(context,holder.contactMail + ".jpg");
					Bitmap bitmap = null;
					if (isFileAvailable(avatar)){
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
									bitmap = getRoundedRectBitmap(context, bitmap, 3);
									((ViewHolderContactsGrid)holder).imageView.setImageBitmap(bitmap);
								}
								else if (holder instanceof ViewHolderContactsList){
									((ViewHolderContactsList)holder).imageView.setImageBitmap(bitmap);
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			logWarning("onRequestTemporaryError");
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

		EmojiTextView textViewContactName;
        TextView textViewContent;
		ImageView imageButtonThreeDots;
        RelativeLayout itemLayout;
        String contactMail;
		ImageView verifiedIcon;
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
		logDebug("onCreateViewHolder");

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

	    dbH = DatabaseHandler.getDbHandler(context);

	    if (viewType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_LIST||viewType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_CHAT_LIST){

		    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_list, parent, false);

		    holderList = new ViewHolderContactsList(v);
		    holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.contact_list_item_layout);
		    holderList.imageView = (RoundedImageView) v.findViewById(R.id.contact_list_thumbnail);
		    holderList.verifiedIcon = v.findViewById(R.id.verified_icon);
		    holderList.textViewContactName = v.findViewById(R.id.contact_list_name);
		    holderList.textViewContent = (TextView) v.findViewById(R.id.contact_list_content);
		    holderList.imageButtonThreeDots = (ImageView) v.findViewById(R.id.contact_list_three_dots);
			holderList.contactStateIcon = (ImageView) v.findViewById(R.id.contact_list_drawable_state);

			if(!isScreenInPortrait(context)){
				logDebug("Landscape configuration");
				holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(280, outMetrics));
			}
			else{
				holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(230, outMetrics));
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
			holderGrid.verifiedIcon = v.findViewById(R.id.verified_icon);
		    holderGrid.textViewContactName = v.findViewById(R.id.contact_grid_name);
		    holderGrid.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.contact_grid_three_dots);
			holderGrid.contactStateIcon = (ImageView) v.findViewById(R.id.contact_grid_drawable_state);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				holderGrid.textViewContactName.setMaxWidthEmojis(scaleWidthPx(70, outMetrics));
			}
			else{
				holderGrid.textViewContactName.setMaxWidthEmojis(scaleWidthPx(120, outMetrics));
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
		logDebug("onBindViewHolder");

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

		MegaContactDB contact = (MegaContactDB) getItem(position);

		setContactStatus(getUserStatus(MegaApiJava.base64ToUserHandle(contact.getHandle())), holder.contactStateIcon);

		MegaUser user = megaApi.getContact(contact.getMail());
		holder.verifiedIcon.setVisibility(!isItemChecked(position) && user != null && megaApi.areCredentialsVerified(user) ? View.VISIBLE : View.GONE);

		holder.contactMail = contact.getMail();

		if (!multipleSelect) {

			holder.itemLayout.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.background_item_grid));
		}
		else {

			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.background_item_grid_long_click_lollipop));
			}
			else{
				holder.itemLayout.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.background_item_grid));
			}
		}

		holder.textViewContactName.setText(getContactNameDB(contact));

		createDefaultAvatar(holder, contact);

		UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);

		File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
		Bitmap bitmap = null;
		if (isFileAvailable(avatar)){
			if (avatar.length() > 0){
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				bitmap = getRoundedRectBitmap(context, bitmap, 3);
				if (bitmap == null) {
					avatar.delete();
                    megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
                }
				else{
					holder.imageView.setImageBitmap(bitmap);
				}
			}
			else{
                megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
			}
		}
		else{
            megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
		}

//		ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact.getMegaUser());

//		String sharedNodesDescription = getSubtitleDescription(sharedNodes);

//		holder.textViewContent.setText(sharedNodesDescription);

		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
	}

	public void onBindViewHolderList(ViewHolderContactsList holder, int position){
		holder.imageView.setImageBitmap(null);

		MegaContactDB contact = (MegaContactDB) getItem(position);

		MegaUser user = megaApi.getContact(contact.getMail());
		boolean isNotChecked = !multipleSelect || !isItemChecked(position);
		holder.verifiedIcon.setVisibility(isNotChecked && user != null && megaApi.areCredentialsVerified(user) ? View.VISIBLE : View.GONE);

		holder.contactMail = contact.getMail();

		holder.contactStateIcon.setVisibility(View.VISIBLE);

		setContactStatus(getUserStatus(MegaApiJava.base64ToUserHandle(contact.getHandle())), holder.contactStateIcon);
		holder.textViewContactName.setText(getContactNameDB(contact));

		if (!multipleSelect) {
			holder.itemLayout.setBackgroundColor(Color.WHITE);

			createDefaultAvatar(holder, contact);

			UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);

			File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
			Bitmap bitmap = null;
			if (isFileAvailable(avatar)){
				if (avatar.length() > 0){
					BitmapFactory.Options bOpts = new BitmapFactory.Options();
					bOpts.inPurgeable = true;
					bOpts.inInputShareable = true;
					bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
					if (bitmap == null) {
						avatar.delete();
                        megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
                    }
					else{
						holder.imageView.setImageBitmap(bitmap);
					}
				}
				else{
                    megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
				}
			}
			else{
                megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
			}
		} else {

			if(this.isItemChecked(position)){
				holder.imageView.setImageResource(R.drawable.ic_select_avatar);
				holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
			}
			else{
				holder.itemLayout.setBackgroundColor(Color.WHITE);

				createDefaultAvatar(holder, contact);

				UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);

				File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
				Bitmap bitmap = null;
				if (isFileAvailable(avatar)){
					if (avatar.length() > 0){
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bOpts.inPurgeable = true;
						bOpts.inInputShareable = true;
						bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (bitmap == null) {
							avatar.delete();
                            megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
                        }
						else{
							holder.imageView.setImageBitmap(bitmap);
						}
					}
					else{
                        megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
					}
				}
				else{
                    megaApi.getUserAvatar(contact.getMail(),buildAvatarFile(context,contact.getMail() + ".jpg").getAbsolutePath(),listener);
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
		int color = getColorAvatar(Long.parseLong(contact.getHandle()));
		String fullName = getContactNameDB(contact);

		if (holder instanceof ViewHolderContactsList){
			Bitmap bitmap = getDefaultAvatar(color, fullName, AVATAR_SIZE, true);
			((ViewHolderContactsList)holder).imageView.setImageBitmap(bitmap);
		}
		else if (holder instanceof ViewHolderContactsGrid){
			Bitmap bitmap = getDefaultAvatar(color, fullName, AVATAR_SIZE_GRID, false);
			((ViewHolderContactsGrid)holder).imageView.setImageBitmap(bitmap);
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
		logDebug("position: " + position);
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
		logDebug("position: "+p);
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
		logDebug("position: " + pos);
		final int positionToflip = pos;

		if (selectedItems.get(pos, false)) {
			logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
		}
		else {
			logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
		}

		if (adapterType == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST){
			logDebug("Adapter type is LIST");
			MegaContactsAttachedLollipopAdapter.ViewHolderContactsList view = (MegaContactsAttachedLollipopAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
			if(view!=null){
				logDebug("Start animation: " + pos);
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
				logWarning("NULL view pos: " + positionToflip);
				notifyItemChanged(pos);
			}
		}
		else {
			logDebug("Adapter type is GRID");
			if (selectedItems.size() <= 0){
				((ContactsFragmentLollipop) fragment).hideMultipleSelect();
			}
			notifyItemChanged(positionToflip);
		}
	}

	public void toggleSelection(int pos) {
		logDebug("position: " + pos);

		if (selectedItems.get(pos, false)) {
			logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
		}
		else {
			logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);

		if (adapterType == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
			logDebug("Adapter type is LIST");
			MegaContactsAttachedLollipopAdapter.ViewHolderContactsList view = (MegaContactsAttachedLollipopAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
			if(view!=null){
				logDebug("Start animation: " + pos);
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
			logDebug("Adapter type is GRID");
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
		logDebug("clearSelections");
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
		logDebug("adapterType: " + adapterType);

		if(!isOnline(context)){
			if(context instanceof ManagerActivityLollipop){
				((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
			}
			return;
		}

		if(adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_CHAT_LIST){
			ViewHolderContacts holder = (ViewHolderContacts) v.getTag();
			int currentPosition = holder.getAdapterPosition();
			MegaContactDB c = (MegaContactDB) getItem(currentPosition);

			switch (v.getId()){
				case R.id.contact_list_three_dots:
				case R.id.contact_grid_three_dots:{
					logDebug("Click contact three dots!");
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
					logDebug("contact_item_layout");
					((ContactAttachmentActivityLollipop) context).itemClick(currentPosition);
					break;
				}
			}
		}
	}

	@Override
	public boolean onLongClick(View view) {
		logDebug("OnLongCLick");

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
		logDebug("setContacts");
		this.contacts = contacts;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	public void updateContactStatus(int position, long userHandle, int state){
		logDebug("position: " + position);

		if (adapterType == MegaContactsAttachedLollipopAdapter.ITEM_VIEW_TYPE_LIST){
			holderList = (ViewHolderContactsList) listFragment.findViewHolderForAdapterPosition(position);
			if(holderList!=null){
				setContactStatus(state, holderList.contactStateIcon);
			}
			else{
				logWarning("Holder is NULL");
				notifyItemChanged(position);
			}
		}
//		else if (adapterType == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_GRID){
//			holderGrid = (ViewHolderContactsGrid) listFragment.findViewHolderForAdapterPosition(position);
//		}
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}

	public void updateContact(MegaContactDB contactDB, int position) {
		if (position >= 0 && position < getItemCount()) {
			contacts.set(position, contactDB);
			notifyItemChanged(position);
		}
	}
}
