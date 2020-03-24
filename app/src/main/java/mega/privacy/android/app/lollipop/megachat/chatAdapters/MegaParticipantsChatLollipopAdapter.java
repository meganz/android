package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.listeners.ChatParticipantAvatarListener;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class MegaParticipantsChatLollipopAdapter extends RecyclerView.Adapter<MegaParticipantsChatLollipopAdapter.ViewHolderParticipants> implements OnClickListener {

	public static final int ITEM_VIEW_TYPE_NORMAL = 0;
	public static final int ITEM_VIEW_TYPE_ADD_PARTICIPANT = 1;
	private static final int MAX_WIDTH_PORT = 180;
	private static final int MAX_WIDTH_LAND = 260;

	Context context;
	int positionClicked;
	ArrayList<MegaChatParticipant> participants;
	RecyclerView listFragment;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	boolean multipleSelect;
	DatabaseHandler dbH = null;
	private SparseBooleanArray selectedItems;
	boolean isPreview;

	public MegaParticipantsChatLollipopAdapter(Context context, ArrayList<MegaChatParticipant> participants, RecyclerView listView, boolean isPreview) {
		this.context = context;
		this.participants = participants;
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (megaChatApi == null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		this.listFragment = listView;
		this.isPreview = isPreview;
	}
	
	/*private view holder class*/
    public static class ViewHolderParticipants extends RecyclerView.ViewHolder{
    	public ViewHolderParticipants(View v) {
			super(v);
		}   	
		EmojiTextView textViewContactName;
		RelativeLayout itemLayout;
    }
    
    public static class ViewHolderParticipantsList extends ViewHolderParticipants{
    	public ViewHolderParticipantsList(View v) {
			super(v);
		}
    	RoundedImageView imageView;
		MarqueeTextView textViewContent;
		RelativeLayout threeDotsLayout;
		ImageView imageButtonThreeDots;
		ImageView statusImage;

		ImageView permissionsIcon;
		int currentPosition;
		public String contactMail;
		public String userHandle;
		String fullName="";

		public void setImageView(Bitmap bitmap){
			imageView.setImageBitmap(bitmap);
		}
    }

	public static class ViewHolderAddParticipant extends ViewHolderParticipants {
		public ViewHolderAddParticipant(View v) {
			super(v);
		}
		ImageView imageView;
	}

	ViewHolderParticipantsList holderList = null;
	ViewHolderAddParticipant holderAddParticipant = null;

	@Override
	public ViewHolderParticipants onCreateViewHolder(ViewGroup parent, int viewType) {
		logDebug("onCreateViewHolder");
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    dbH = DatabaseHandler.getDbHandler(context);
	   
		View v = null;
		if(viewType == ITEM_VIEW_TYPE_NORMAL) {
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_chat_list, parent, false);
			holderList = new ViewHolderParticipantsList(v);
			holderList.itemLayout = v.findViewById(R.id.participant_list_item_layout);
			holderList.imageView = v.findViewById(R.id.participant_list_thumbnail);
			holderList.textViewContactName = v.findViewById(R.id.participant_list_name);
			holderList.textViewContent = v.findViewById(R.id.participant_list_content);
			holderList.threeDotsLayout = v.findViewById(R.id.participant_list_three_dots_layout);
			holderList.imageButtonThreeDots = v.findViewById(R.id.participant_list_three_dots);
			holderList.permissionsIcon = v.findViewById(R.id.participant_list_permissions);
			holderList.statusImage = v.findViewById(R.id.group_participants_state_circle);

			if(isScreenInPortrait(context)){
				holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_PORT, outMetrics));
				holderList.textViewContent.setMaxWidth(scaleWidthPx(MAX_WIDTH_PORT, outMetrics));
			}else{
				holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_LAND, outMetrics));
				holderList.textViewContent.setMaxWidth(scaleWidthPx(MAX_WIDTH_LAND, outMetrics));
			}

			holderList.itemLayout.setOnClickListener(this);
			holderList.itemLayout.setTag(holderList);
			v.setTag(holderList);
			return holderList;
		}
		else{
			logDebug("Last element - type add participant");
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_participant_chat_list, parent, false);
			holderAddParticipant = new ViewHolderAddParticipant(v);
			holderAddParticipant.itemLayout = v.findViewById(R.id.add_participant_list_item_layout);

			holderAddParticipant.imageView = v.findViewById(R.id.add_participant_list_icon);
			holderAddParticipant.textViewContactName = v.findViewById(R.id.add_participant_list_text);
			if(isScreenInPortrait(context)){
				holderAddParticipant.textViewContactName.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_PORT, outMetrics));
			}else{
				holderAddParticipant.textViewContactName.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_LAND, outMetrics));
			}
			holderAddParticipant.itemLayout.setOnClickListener(this);
			v.setTag(holderAddParticipant);
			return holderAddParticipant;
		}
	}

	@Override
	public void onBindViewHolder(ViewHolderParticipants holder, int position) {
		final int itemType = getItemViewType(position);
		logDebug("position: " + position + ", itemType: "+itemType);

		if(itemType==ITEM_VIEW_TYPE_NORMAL) {
			ViewHolderParticipantsList holderParticipantsList = (ViewHolderParticipantsList) holder;

			holderParticipantsList.currentPosition = position;
			holderParticipantsList.imageView.setImageBitmap(null);

			MegaChatParticipant participant = getItem(position);
			if (participant == null) return;


            checkParticipant(position, participant);
			holderParticipantsList.contactMail = participant.getEmail();
			String userHandleEncoded = MegaApiAndroid.userHandleToBase64(participant.getHandle());
			holderParticipantsList.userHandle = userHandleEncoded;
			logDebug("participant: " + participant.getEmail() + ", handle: " + participant.getHandle());
			holderParticipantsList.fullName = participant.getFullName();

			int userStatus;

			if(megaChatApi.getMyUserHandle() == participant.getHandle()){
				userStatus = megaChatApi.getOnlineStatus();
			}
			else{
				userStatus = megaChatApi.getUserOnlineStatus(participant.getHandle());
			}

			if(userStatus == MegaChatApi.STATUS_ONLINE){
				logDebug("This user is connected");
				holderParticipantsList.statusImage.setVisibility(View.VISIBLE);
				holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
				holderParticipantsList.textViewContent.setText(context.getString(R.string.online_status));
				holderParticipantsList.textViewContent.setVisibility(View.VISIBLE);
			}
			else if(userStatus == MegaChatApi.STATUS_AWAY){
				logDebug("This user is away");
				holderParticipantsList.statusImage.setVisibility(View.VISIBLE);
				holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
				holderParticipantsList.textViewContent.setText(context.getString(R.string.away_status));
				holderParticipantsList.textViewContent.setVisibility(View.VISIBLE);
			}
			else if(userStatus == MegaChatApi.STATUS_BUSY){
				logDebug("This user is busy");
				holderParticipantsList.statusImage.setVisibility(View.VISIBLE);
				holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
				holderParticipantsList.textViewContent.setText(context.getString(R.string.busy_status));
				holderParticipantsList.textViewContent.setVisibility(View.VISIBLE);
			}
			else if(userStatus == MegaChatApi.STATUS_OFFLINE){
				logDebug("This user is offline");
				holderParticipantsList.statusImage.setVisibility(View.VISIBLE);
				holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
				holderParticipantsList.textViewContent.setText(context.getString(R.string.offline_status));
				holderParticipantsList.textViewContent.setVisibility(View.VISIBLE);
			}
			else if(userStatus == MegaChatApi.STATUS_INVALID){
				logWarning("INVALID status: " + userStatus);
				holderParticipantsList.statusImage.setVisibility(View.GONE);
				holderParticipantsList.textViewContent.setVisibility(View.GONE);
			}
			else{
				logDebug("This user status is: " + userStatus);
				holderParticipantsList.statusImage.setVisibility(View.GONE);
				holderParticipantsList.textViewContent.setVisibility(View.GONE);
			}

			if(userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID){
				if(!participant.getLastGreen().isEmpty()){
					holderParticipantsList.textViewContent.setText(participant.getLastGreen());
					holderParticipantsList.textViewContent.isMarqueeIsNecessary(context);
				}
			}
			if (isMultipleSelect() && isItemChecked(position)) {
				holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
			}
			else {
				holder.itemLayout.setBackgroundColor(Color.WHITE);
			}

			holder.textViewContactName.setText(holderParticipantsList.fullName);
			holderParticipantsList.threeDotsLayout.setOnClickListener(this);

			/*Default Avatar*/
			int color = getColorAvatar(context, megaApi, holderParticipantsList.userHandle);
			String name = holderParticipantsList.fullName;
			holderParticipantsList.imageView.setImageBitmap(getDefaultAvatar(context, color, name, AVATAR_SIZE, true));

			/*Avatar*/
			String myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.getMyUserHandle());
			if((holderParticipantsList.userHandle).equals(myUserHandleEncoded)){
				logDebug("It's me!!!");
				File avatar = buildAvatarFile(context,holderParticipantsList.contactMail + ".jpg");
				Bitmap bitmap = null;
				if(avatar!=null){
					if (avatar.exists()) {
						if (avatar.length() > 0) {
							BitmapFactory.Options bOpts = new BitmapFactory.Options();
							bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
							if (bitmap != null) {
								holderParticipantsList.imageView.setImageBitmap(bitmap);
							}
						}
					}
					else{
						logWarning("My avatar Not exists");
					}
				}
				else{
					logWarning("My avatar NULL");
				}

				holderParticipantsList.imageButtonThreeDots.setColorFilter(null);
				holderParticipantsList.threeDotsLayout.setOnClickListener(this);
			}
			else{
				logDebug("NOOOT me!!!");
				ChatParticipantAvatarListener listener = new ChatParticipantAvatarListener(context, holderParticipantsList, this);
				if (holderParticipantsList.contactMail != null) {
					logDebug("The participant is contact!!");

					MegaUser contact = megaApi.getContact(holderParticipantsList.contactMail);
					if(contact!=null){
						File avatar = buildAvatarFile(context,holderParticipantsList.contactMail + ".jpg");
						Bitmap bitmap = null;
						if (isFileAvailable(avatar)) {
							if (avatar.length() > 0) {
								BitmapFactory.Options bOpts = new BitmapFactory.Options();
								bOpts.inPurgeable = true;
								bOpts.inInputShareable = true;
								bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
								if (bitmap == null) {
									avatar.delete();
                                    megaApi.getUserAvatar(contact,buildAvatarFile(context,contact.getEmail() + ".jpg").getAbsolutePath(),listener);
                                } else {
									holderParticipantsList.imageView.setImageBitmap(bitmap);
								}
							} else {
                                megaApi.getUserAvatar(contact,buildAvatarFile(context,contact.getEmail() + ".jpg").getAbsolutePath(),listener);
							}
						} else {
                            megaApi.getUserAvatar(contact,buildAvatarFile(context,contact.getEmail() + ".jpg").getAbsolutePath(),listener);
						}
						holderParticipantsList.imageButtonThreeDots.setColorFilter(null);
					}
					else{
						logDebug("Participant is NOT contact");
                        megaApi.getUserAvatar(holderParticipantsList.contactMail,buildAvatarFile(context,holderParticipantsList.contactMail + ".jpg").getAbsolutePath(),listener);
					}

				} else {
					logDebug("NOT email- Participant is NOT contact");
					if(position!=0){
						holderParticipantsList.imageButtonThreeDots.setColorFilter(ContextCompat.getColor(context, R.color.chat_sliding_panel_separator));
						holderParticipantsList.threeDotsLayout.setOnClickListener(null);
					}

                    megaApi.getUserAvatar(holderParticipantsList.userHandle,buildAvatarFile(context,holderParticipantsList.userHandle + ".jpg").getAbsolutePath(),listener);
				}
			}

			if(isPreview && megaChatApi.getInitState() == MegaChatApi.INIT_ANONYMOUS){
				holderParticipantsList.imageButtonThreeDots.setColorFilter(ContextCompat.getColor(context, R.color.chat_sliding_panel_separator));
				holderParticipantsList.threeDotsLayout.setOnClickListener(null);
				holderParticipantsList.itemLayout.setOnClickListener(null);
			}

			int permission = participant.getPrivilege();

			if (permission == MegaChatRoom.PRIV_STANDARD) {
				holderParticipantsList.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
			} else if (permission == MegaChatRoom.PRIV_MODERATOR) {
				holderParticipantsList.permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
			} else {
				holderParticipantsList.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
			}

			holderParticipantsList.threeDotsLayout.setTag(holder);
		}
		else{
			logDebug("Bind item add participant");
			((ViewHolderAddParticipant)holder).itemLayout.setOnClickListener(this);
		}
	}

	@Override
    public int getItemCount() {
		logDebug("getItemCount");

		if (isPreview) {
			return participants.size();
		}
		else {
			int size = participants.size();
			int permission = participants.get(size-1).getPrivilege();

			if (permission == MegaChatRoom.PRIV_MODERATOR) {
				logDebug("Moderator type");
				int participantNumber = participants.size()+1;
				logDebug("Return value: " + participantNumber);
				return participantNumber;
			} else {
				return participants.size();
			}
		}
    }

	@Override
	public int getItemViewType(int position) {
		logDebug("position: " + position);

		if (isPreview) {
			return  ITEM_VIEW_TYPE_NORMAL;
		}
		else {
			int size = participants.size();

			int permission = participants.get(size-1).getPrivilege();

			if (permission == MegaChatRoom.PRIV_MODERATOR) {
				logDebug("Moderator type");
				if (position>0) {
					return ITEM_VIEW_TYPE_NORMAL;
				} else {
					logDebug("Type ADD_PARTICIPANT");
					return ITEM_VIEW_TYPE_ADD_PARTICIPANT;
				}
			} else {
				return ITEM_VIEW_TYPE_NORMAL;
			}
		}
	}

	public MegaChatParticipant getItem(int position) {
		logDebug("position: " + position);
		if (position < 0 || position > participants.size()) return null;

		int permission = participants.get(participants.size() - 1).getPrivilege();
		if (permission == MegaChatRoom.PRIV_MODERATOR) {
			return participants.get(position - 1);
		}

		return participants.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		logDebug("position: " + p);
		positionClicked = p;
		notifyDataSetChanged();
	}

	public boolean isMultipleSelect() {
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
		if(this.multipleSelect)
		{
			selectedItems = new SparseBooleanArray();
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
	}
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleSelection(i);
			}
		}
	}

	public void clearSelections() {
		if(selectedItems!=null){
			selectedItems.clear();
		}
		notifyDataSetChanged();
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
	 * Get list of all selected participants
	 */
	public ArrayList<MegaChatParticipant> getParticipant() {
		ArrayList<MegaChatParticipant> participants = new ArrayList<MegaChatParticipant>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaChatParticipant u = getParticipantAt(selectedItems.keyAt(i));
				if (u != null){
					participants.add(u);
				}
			}
		}
		return participants;
	}	
	
	/*
	 * Get participant at specified position
	 */
	public MegaChatParticipant getParticipantAt(int position) {
		try {
			if (participants != null) {
				return participants.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}
    
	@Override
	public void onClick(View v) {

		if(!isOnline(context)){
			if(context instanceof GroupChatInfoActivityLollipop){
				((GroupChatInfoActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
			}
			return;
		}

		switch (v.getId()){
			case R.id.participant_list_three_dots_layout:
			case R.id.participant_list_item_layout:{
				logDebug("contact_list_three_dots");
				ViewHolderParticipantsList holder = (ViewHolderParticipantsList) v.getTag();
				int currentPosition = holder.currentPosition;
				MegaChatParticipant p = getItem(currentPosition);
				if(p==null){
					logWarning("Participant is null");
				}
				else{
					logDebug("Selected: " + p.getFullName());
				}
				((GroupChatInfoActivityLollipop) context).showParticipantsPanel(p);
				break;
			}
			case R.id.add_participant_list_item_layout:{
				logDebug("add_participant_item_layout");
				((GroupChatInfoActivityLollipop) context).chooseAddParticipantDialog();
				break;
			}
		}
	}
	
	public void setParticipants (ArrayList<MegaChatParticipant> participants){
		logDebug("participants: " + participants.size());
		this.participants = participants;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	public void updateParticipant(int position, ArrayList<MegaChatParticipant> participants){
		logDebug("updateParticipant");
		this.participants = participants;
		positionClicked = -1;
		notifyItemChanged(position);
	}

	public void removeParticipant(int position, ArrayList<MegaChatParticipant> participants){
		logDebug("updateParticipant");
		this.participants = participants;
		positionClicked = -1;
		notifyItemRemoved(position);
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

	public void updateContactStatus(int position){
		logDebug("position: " + position);

		if(listFragment.findViewHolderForAdapterPosition(position) instanceof MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList){
			notifyItemChanged(position);
		}
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}


	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}

	private void checkParticipant(int position, MegaChatParticipant participant) {
		if (isTextEmpty(participant.getEmail()) && isTextEmpty(participant.getFullName())) {
			if (context instanceof GroupChatInfoActivityLollipop) {
				((GroupChatInfoActivityLollipop) context).addParticipantRequest(position, participant);
			}
		}
	}

	public void updateParticipant(int position, MegaChatParticipant participant) {
		MegaChatParticipant adapterParticipant = getItem(position);
		if (adapterParticipant != null && participant.getHandle() == adapterParticipant.getHandle()) {
			participants.set(position, participant);
			notifyItemChanged(position);
		}
	}
}
