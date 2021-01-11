package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;


public class MegaContactsAttachedLollipopAdapter extends RecyclerView.Adapter<MegaContactsAttachedLollipopAdapter.ViewHolderContacts> implements OnClickListener, View.OnLongClickListener {

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
								if (holder instanceof ViewHolderContactsList){
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

	ViewHolderContactsList holderList = null;

	@Override
	public ViewHolderContacts onCreateViewHolder(ViewGroup parent, int viewType) {
		logDebug("onCreateViewHolder");

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

	    dbH = DatabaseHandler.getDbHandler(context);

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

	@Override
	public void onBindViewHolder(ViewHolderContacts holder, int position) {
		logDebug("onBindViewHolder");

		ViewHolderContactsList holderList = (ViewHolderContactsList) holder;
		onBindViewHolderList(holderList, position);
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
			}
			else{
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

		holder.textViewContent.setText(contact.getMail());

		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);
	}

	public void createDefaultAvatar(ViewHolderContacts holder, MegaContactDB contact){
		int color;
		if (contact.getHandle().equals(megaApi.getMyUserHandle())) {
			color = getColorAvatar(megaApi.getMyUser());
		} else {
			color = getColorAvatar(MegaApiJava.base64ToUserHandle(contact.getHandle()));
		}
		String fullName = contact.getName();
		if (holder instanceof ViewHolderContactsList) {
			Bitmap bitmap = getDefaultAvatar(color, fullName, AVATAR_SIZE, true);
			((ViewHolderContactsList) holder).imageView.setImageBitmap(bitmap);
		}
	}


	@Override
    public int getItemCount() {
        return contacts.size();
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
		if(!isOnline(context)){
			if(context instanceof ManagerActivityLollipop){
				((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
			}
			return;
		}

		ViewHolderContacts holder = (ViewHolderContacts) v.getTag();
		int currentPosition = holder.getAdapterPosition();
		MegaContactDB c = (MegaContactDB) getItem(currentPosition);

		switch (v.getId()){
			case R.id.contact_list_three_dots:
			case R.id.contact_grid_three_dots:{
				logDebug("Click contact three dots!");
				if(!multipleSelect){
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

		holderList = (ViewHolderContactsList) listFragment.findViewHolderForAdapterPosition(position);
		if(holderList!=null){
			setContactStatus(state, holderList.contactStateIcon);
		}
		else{
			logWarning("Holder is NULL");
			notifyItemChanged(position);
		}
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
