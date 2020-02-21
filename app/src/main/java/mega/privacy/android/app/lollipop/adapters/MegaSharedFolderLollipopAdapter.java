package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class MegaSharedFolderLollipopAdapter extends RecyclerView.Adapter<MegaSharedFolderLollipopAdapter.ViewHolderShareList> implements OnClickListener, View.OnLongClickListener {
	
	Context context;
	int positionClicked;
	ArrayList<MegaShare> shareList;
	MegaNode node;
//	RecyclerView listViewActivity;
	
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	DatabaseHandler dbH = null;
	
//	boolean removeShare = false;
	boolean multipleSelect = false;
	
	OnItemClickListener mItemClickListener;
	RecyclerView listFragment;
	
	AlertDialog permissionsDialog;
	SparseBooleanArray selectedItems;
	
	final MegaSharedFolderLollipopAdapter megaSharedFolderAdapter;
	
	ProgressDialog statusDialog;
	
	public static ArrayList<String> pendingAvatars = new ArrayList<String>();

	@Override
	public boolean onLongClick(View v) {

        ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
        int currentPosition = holder.currentPosition;

        if (context instanceof FileContactListActivityLollipop) {
            ((FileContactListActivityLollipop) context).activateActionMode();
            ((FileContactListActivityLollipop) context).itemClick(currentPosition);
        }

		return true;
	}

	private class UserAvatarListenerList implements MegaRequestListenerInterface{

		Context context;
		ViewHolderShareList holder;
		MegaSharedFolderLollipopAdapter adapter;
		
		public UserAvatarListenerList(Context context, ViewHolderShareList holder, MegaSharedFolderLollipopAdapter adapter) {
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}
		
		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			logDebug("onRequestStart() avatar");
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			logDebug("onRequestFinish() avatar");
			if (e.getErrorCode() == MegaError.API_OK){
				
				if(request.getEmail()!=null)
				{
					pendingAvatars.remove(request.getEmail());
					
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
									holder.imageView.setImageBitmap(bitmap);
								}
							}
						}
					}
				}
			}
			else{
				logError("E: " + e.getErrorCode() + "_" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			logWarning("onRequestTemporaryError");
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

		}
	}
	
	public MegaSharedFolderLollipopAdapter(Context _context, MegaNode node, ArrayList<MegaShare> _shareList, RecyclerView _lv) {
		this.context = _context;
		this.node = node;
		this.shareList = _shareList;
		this.positionClicked = -1;
		this.megaSharedFolderAdapter = this;
		this.listFragment = _lv;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (megaChatApi == null) {
			megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
		}
	}
	
	public void setContext(Context context){
		this.context = context;
	}
	
	public void setNode(MegaNode node){
		this.node = node;
	}
	
	/*private view holder class*/
    class ViewHolderShareList extends RecyclerView.ViewHolder implements View.OnClickListener{
    	RoundedImageView imageView;
		EmojiTextView textViewContactName;
        TextView textViewPermissions;
        RelativeLayout threeDotsLayout;
        RelativeLayout itemLayout;
        int currentPosition;
        String contactMail;
    	boolean name = false;
    	boolean firstName = false;
    	ImageView stateIcon;
    	
    	public ViewHolderShareList(View itemView) {
			super(itemView);
            itemView.setOnClickListener(this);
		}
    	
    	@Override
		public void onClick(View v) {
			if(mItemClickListener != null){
				mItemClickListener.onItemClick(v, getPosition());
			}			
		}
    }
    
    public interface OnItemClickListener {
		   public void onItemClick(View view , int position);
	}
	
	public void SetOnItemClickListener(final OnItemClickListener mItemClickListener){
		this.mItemClickListener = mItemClickListener;
	}


	@Override
	public ViewHolderShareList onCreateViewHolder(ViewGroup parent, int viewType) {
		
		listFragment = (RecyclerView) parent;
		
		dbH = DatabaseHandler.getDbHandler(context);
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shared_folder, parent, false);
		ViewHolderShareList holder = new ViewHolderShareList(v);
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.shared_folder_item_layout);
		holder.itemLayout.setOnClickListener(this);
		holder.itemLayout.setOnLongClickListener(this);
		holder.imageView = (RoundedImageView) v.findViewById(R.id.shared_folder_contact_thumbnail);

		holder.textViewContactName = v.findViewById(R.id.shared_folder_contact_name);
		holder.textViewContactName.setTypeEllipsize(TextUtils.TruncateAt.MIDDLE);
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			holder.textViewContactName.setMaxWidthEmojis(scaleWidthPx(280,outMetrics));
		} else {
			holder.textViewContactName.setMaxWidthEmojis(scaleWidthPx(225,outMetrics));
		}

		holder.textViewPermissions = (TextView) v.findViewById(R.id.shared_folder_contact_permissions);
		holder.threeDotsLayout = (RelativeLayout) v.findViewById(R.id.shared_folder_three_dots_layout);

		holder.stateIcon = (ImageView) v.findViewById(R.id.shared_folder_state_icon);

		v.setTag(holder); 
		
		return holder;
	}
	

	@Override
	public void onBindViewHolder(ViewHolderShareList holder, int position) {
		logDebug("Position: " + position);

		holder.currentPosition = position;
		
		//Check if the share
		MegaShare share = (MegaShare) getItem(position);

		if (share.getUser() != null){
			holder.contactMail = share.getUser();
			MegaUser contact = megaApi.getContact(holder.contactMail);

			if(contact!=null){
				MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contact.getHandle()));
				if(contactDB!=null){
					if(!contactDB.getName().equals("")){
						holder.textViewContactName.setText(contactDB.getName()+" "+contactDB.getLastName());
					}
					else{
						holder.textViewContactName.setText(holder.contactMail);
					}
				}
				else{
					logWarning("The contactDB is null: ");
					holder.textViewContactName.setText(holder.contactMail);
				}

				holder.stateIcon.setVisibility(View.VISIBLE);
				if (megaChatApi != null) {
					int userStatus = megaChatApi.getUserOnlineStatus(contact.getHandle());
					if (userStatus == MegaChatApi.STATUS_ONLINE) {
						logDebug("This user is connected");
						holder.stateIcon.setVisibility(View.VISIBLE);
						holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online_grid));
					} else if (userStatus == MegaChatApi.STATUS_AWAY) {
						logDebug("This user is away");
						holder.stateIcon.setVisibility(View.VISIBLE);
						holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away_grid));
					} else if (userStatus == MegaChatApi.STATUS_BUSY) {
						logDebug("This user is busy");
						holder.stateIcon.setVisibility(View.VISIBLE);
						holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy_grid));
					} else if (userStatus == MegaChatApi.STATUS_OFFLINE) {
						logDebug("This user is offline");
						holder.stateIcon.setVisibility(View.VISIBLE);
						holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline_grid));
					} else if (userStatus == MegaChatApi.STATUS_INVALID) {
						logWarning("INVALID status: " + userStatus);
						holder.stateIcon.setVisibility(View.GONE);
					} else {
						logDebug("This user status is: " + userStatus);
						holder.stateIcon.setVisibility(View.GONE);
					}
				}
			}
			else{
				holder.textViewContactName.setText(holder.contactMail);
			}

			if(multipleSelect && this.isItemChecked(position)){
				holder.imageView.setImageResource(R.drawable.ic_select_avatar);
				holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

			}else{

				holder.itemLayout.setBackgroundColor(Color.WHITE);
				/*Default Avatar*/
				int color = getColorAvatar(context, megaApi, contact);
				String name = " ";
				if(holder.textViewContactName!=null){
					name = holder.textViewContactName.getText().toString();
				}else if(holder.contactMail != null && holder.contactMail.length() > 0){
					name = holder.contactMail;
				}
				holder.imageView.setImageBitmap(getDefaultAvatar(context, color, name, AVATAR_SIZE, true));

				/*Avatar*/
				if(contact!=null){
					UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);
					holder.name=false;
					holder.firstName=false;
					megaApi.getUserAttribute(contact, 1, listener);
					megaApi.getUserAttribute(contact, 2, listener);

					File avatar = buildAvatarFile(context,holder.contactMail + ".jpg");
					Bitmap bitmap;
					if (isFileAvailable(avatar) && avatar.length() > 0){
						BitmapFactory.Options bOpts = new BitmapFactory.Options();
						bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
						if (bitmap == null) {
							avatar.delete();
							megaApi.getUserAvatar(contact,buildAvatarFile(context,holder.contactMail + ".jpg").getAbsolutePath(),listener);
						}
						else{
							holder.imageView.setImageBitmap(bitmap);
						}
					}
					else{
						megaApi.getUserAvatar(contact,buildAvatarFile(context,holder.contactMail + ".jpg").getAbsolutePath(),listener);
					}
				}
			}

			int accessLevel = share.getAccess();
			switch(accessLevel){
				case MegaShare.ACCESS_OWNER:
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

			if(share.isPending()){
				String pending = " "+context.getString(R.string.pending_outshare_indicator);
				holder.textViewPermissions.append(pending);
			}
		}

        holder.threeDotsLayout.setTag(holder);
		holder.threeDotsLayout.setOnClickListener(this);
	}

	@Override
    public int getItemCount() {
        return shareList.size();
    }
 
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
		logDebug("onClick");
		ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
		int currentPosition = holder.currentPosition;
		final MegaShare s = (MegaShare) getItem(currentPosition);
				
		switch (v.getId()){			
			case R.id.shared_folder_three_dots_layout:{
				if(multipleSelect){
					((FileContactListActivityLollipop) context).itemClick(currentPosition);
				}
				else{
					((FileContactListActivityLollipop) context).showOptionsPanel(s);
				}

				break;
			}			
			case R.id.shared_folder_item_layout:{
				((FileContactListActivityLollipop) context).itemClick(currentPosition);
				break;
			}
		}
	}
	
	public void setShareList (ArrayList<MegaShare> shareList){
		logDebug("setShareList");
		this.shareList = shareList;
		positionClicked = -1;
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
	
	public void toggleSelection(int pos) {
		if (selectedItems.get(pos, false)) {
			logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
		}
		else {
			logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);

		MegaSharedFolderLollipopAdapter.ViewHolderShareList view = (MegaSharedFolderLollipopAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
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
						((FileContactListActivityLollipop) context).hideMultipleSelect();
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			view.imageView.startAnimation(flipAnimation);
		}
	}

	public void toggleAllSelection(int pos) {
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
		MegaSharedFolderLollipopAdapter.ViewHolderShareList view = (MegaSharedFolderLollipopAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
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
						((FileContactListActivityLollipop) context).hideMultipleSelect();
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

	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleAllSelection(i);
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

	public MegaShare getContactAt(int position) {
		try {
			if(shareList != null){
				return shareList.get(position);
			}
		} catch (IndexOutOfBoundsException e) {}
		return null;
	}

	private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }
	
	/*
	 * Get list of all selected contacts
	 */
//	public List<MegaUser> getSelectedUsers() {
//		ArrayList<MegaUser> users = new ArrayList<MegaUser>();
//		
//		for (int i = 0; i < selectedItems.size(); i++) {
//			if (selectedItems.valueAt(i) == true) {
//				MegaUser u = getContactAt(selectedItems.keyAt(i));
//				if (u != null){
//					users.add(u);
//				}
//			}
//		}
//		return users;
//	}
	
	/*
	 * Get list of all selected shares
	 */
	public ArrayList<MegaShare> getSelectedShares() {
		ArrayList<MegaShare> shares = new ArrayList<MegaShare>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaShare s = getContactAt(selectedItems.keyAt(i));
				if (s != null){
					shares.add(s);
				}
			}
		}
		return shares;
	}
	
	public void setNodes(ArrayList <MegaShare> _shareList){
		this.shareList = _shareList;
		notifyDataSetChanged();
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}

	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
