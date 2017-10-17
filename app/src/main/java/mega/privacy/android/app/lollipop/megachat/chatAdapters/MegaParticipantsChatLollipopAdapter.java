package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.ChatParticipantAvatarListener;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;


public class MegaParticipantsChatLollipopAdapter extends RecyclerView.Adapter<MegaParticipantsChatLollipopAdapter.ViewHolderParticipants> implements OnClickListener {

	public static final int ITEM_VIEW_TYPE_NORMAL = 0;
	public static final int ITEM_VIEW_TYPE_ADD_PARTICIPANT = 1;

	Context context;
	int positionClicked;
	ArrayList<MegaChatParticipant> participants;
	RecyclerView listFragment;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	boolean multipleSelect;
	DatabaseHandler dbH = null;
	private SparseBooleanArray selectedItems;

	public MegaParticipantsChatLollipopAdapter(Context _context, ArrayList<MegaChatParticipant> _participants, RecyclerView _listView) {
		this.context = _context;
		this.participants = _participants;
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (megaChatApi == null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		listFragment = _listView;
	}
	
	/*private view holder class*/
    public static class ViewHolderParticipants extends RecyclerView.ViewHolder{
    	public ViewHolderParticipants(View v) {
			super(v);
		}   	

//        ImageView imageView;
        TextView textViewContactName;
		RelativeLayout itemLayout;
    }
    
    public static class ViewHolderParticipantsList extends ViewHolderParticipants{
    	public ViewHolderParticipantsList(View v) {
			super(v);
		}
    	RoundedImageView imageView;
		TextView contactInitialLetter;
		TextView textViewContent;
		ImageButton imageButtonThreeDots;
		ImageView statusImage;

		ImageView permissionsIcon;
		int currentPosition;
		public String contactMail;
		public String userHandle;
		String fullName="";

		public void setImageView(Bitmap bitmap){
			imageView.setImageBitmap(bitmap);
			contactInitialLetter.setVisibility(View.GONE);
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
		log("onCreateViewHolder");
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
    
	    dbH = DatabaseHandler.getDbHandler(context);
	   
		View v = null;
		if(viewType == ITEM_VIEW_TYPE_NORMAL) {
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_chat_list, parent, false);
//			v = LayoutInflater.from(context).inflate(R.layout.item_participant_chat_list, null);
			holderList = new ViewHolderParticipantsList(v);
			holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.participant_list_item_layout);
			holderList.imageView = (RoundedImageView) v.findViewById(R.id.participant_list_thumbnail);
			holderList.contactInitialLetter = (TextView) v.findViewById(R.id.participant_list_initial_letter);
			holderList.textViewContactName = (TextView) v.findViewById(R.id.participant_list_name);
			holderList.textViewContent = (TextView) v.findViewById(R.id.participant_list_content);
			holderList.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.participant_list_three_dots);
			holderList.permissionsIcon = (ImageView) v.findViewById(R.id.participant_list_permissions);
			holderList.statusImage = (ImageView) v.findViewById(R.id.group_participants_state_circle);

			if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
				log("onCreate: Landscape configuration");
				holderList.textViewContactName.setMaxWidth(Util.scaleWidthPx(270, outMetrics));
				holderList.textViewContent.setMaxWidth(Util.scaleWidthPx(270, outMetrics));
			}
			else{
				holderList.textViewContactName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
				holderList.textViewContent.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
			}

			holderList.itemLayout.setTag(holderList);
			v.setTag(holderList);
			return holderList;
		}
		else{
			log("Last element - type add participant");
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_participant_chat_list, parent, false);
//			v = LayoutInflater.from(context).inflate(R.layout.item_add_participant_chat_list, null);
			holderAddParticipant = new ViewHolderAddParticipant(v);
			holderAddParticipant.itemLayout = (RelativeLayout) v.findViewById(R.id.add_participant_list_item_layout);
			//Right margin
			RecyclerView.LayoutParams itemLayoutParams = (RecyclerView.LayoutParams) holderList.itemLayout.getLayoutParams();
			itemLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(8, outMetrics));
			holderList.itemLayout.setLayoutParams(itemLayoutParams);

			holderAddParticipant.imageView = (ImageView) v.findViewById(R.id.add_participant_list_icon);
			holderAddParticipant.textViewContactName = (TextView) v.findViewById(R.id.add_participant_list_text);
			holderAddParticipant.itemLayout.setOnClickListener(this);
			v.setTag(holderAddParticipant);
			return holderAddParticipant;
		}
	}

	@Override
	public void onBindViewHolder(ViewHolderParticipants holder, int position) {
		log("onBindViewHolder: "+position);

		final int itemType = getItemViewType(position);
		log("itemType: "+itemType);

		if(itemType==ITEM_VIEW_TYPE_NORMAL) {
			((ViewHolderParticipantsList)holder).currentPosition = position;
			((ViewHolderParticipantsList)holder).imageView.setImageBitmap(null);
			((ViewHolderParticipantsList)holder).contactInitialLetter.setText("");

			MegaChatParticipant participant = (MegaChatParticipant) getItem(position);
			((ViewHolderParticipantsList)holder).contactMail = participant.getEmail();
			String userHandleEncoded = MegaApiAndroid.userHandleToBase64(participant.getHandle());
			((ViewHolderParticipantsList)holder).userHandle = userHandleEncoded;
			log("participant: " + participant.getEmail() + " handle: " + participant.getHandle());
			((ViewHolderParticipantsList)holder).fullName = participant.getFullName();

			int userStatus = participant.getStatus();
			if(userStatus == MegaChatApi.STATUS_ONLINE){
				log("This user is connected");
				((ViewHolderParticipantsList) holder).statusImage.setVisibility(View.VISIBLE);
				((ViewHolderParticipantsList) holder).statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
			}
			else if(userStatus == MegaChatApi.STATUS_AWAY){
				log("This user is away");
				((ViewHolderParticipantsList) holder).statusImage.setVisibility(View.VISIBLE);
				((ViewHolderParticipantsList) holder).statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
			}
			else if(userStatus == MegaChatApi.STATUS_BUSY){
				log("This user is busy");
				((ViewHolderParticipantsList) holder).statusImage.setVisibility(View.VISIBLE);
				((ViewHolderParticipantsList) holder).statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
			}
			else if(userStatus == MegaChatApi.STATUS_OFFLINE){
				log("This user is offline");
				((ViewHolderParticipantsList) holder).statusImage.setVisibility(View.VISIBLE);
				((ViewHolderParticipantsList) holder).statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
			}
			else if(userStatus == MegaChatApi.STATUS_INVALID){
				log("INVALID status: "+userStatus);
				((ViewHolderParticipantsList) holder).statusImage.setVisibility(View.GONE);
			}
			else{
				log("This user status is: "+userStatus);
				((ViewHolderParticipantsList) holder).statusImage.setVisibility(View.GONE);
			}

			if (!multipleSelect) {
				holder.itemLayout.setBackgroundColor(Color.WHITE);
			} else {

				if (this.isItemChecked(position)) {
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_file_list_selected_row));
				} else {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
				}
			}

			holder.textViewContactName.setText(((ViewHolderParticipantsList)holder).fullName);
			((ViewHolderParticipantsList)holder).imageButtonThreeDots.setOnClickListener(this);

			createDefaultAvatar(((ViewHolderParticipantsList)holder));

			String myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.getMyUserHandle());
			if((((ViewHolderParticipantsList)holder).userHandle).equals(myUserHandleEncoded)){
				log("It's me!!!");
				File avatar = null;
				if (context.getExternalCacheDir() != null) {
					avatar = new File(context.getExternalCacheDir().getAbsolutePath(), ((ViewHolderParticipantsList)holder).contactMail + ".jpg");
				} else {
					avatar = new File(context.getCacheDir().getAbsolutePath(), ((ViewHolderParticipantsList)holder).contactMail + ".jpg");
				}
				Bitmap bitmap = null;
				if(avatar!=null){
					if (avatar.exists()) {
						if (avatar.length() > 0) {
							BitmapFactory.Options bOpts = new BitmapFactory.Options();
							bOpts.inPurgeable = true;
							bOpts.inInputShareable = true;
							bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
							if (bitmap != null) {
								((ViewHolderParticipantsList)holder).contactInitialLetter.setVisibility(View.GONE);
								((ViewHolderParticipantsList)holder).imageView.setImageBitmap(bitmap);
							}
						}
					}
					else{
						log("My avatar Not exists");
					}
				}
				else{
					log("My avatar NULL");
				}

				((ViewHolderParticipantsList) holder).imageButtonThreeDots.setColorFilter(null);
				((ViewHolderParticipantsList)holder).imageButtonThreeDots.setOnClickListener(this);
			}
			else{
				log("NOOOT me!!!");
				ChatParticipantAvatarListener listener = new ChatParticipantAvatarListener(context, ((ViewHolderParticipantsList)holder), this);
				if (((ViewHolderParticipantsList)holder).contactMail != null) {
					log("The participant is contact!!");

					MegaUser contact = megaApi.getContact(((ViewHolderParticipantsList)holder).contactMail);
					if(contact!=null){
						File avatar = null;
						if (context.getExternalCacheDir() != null) {
							avatar = new File(context.getExternalCacheDir().getAbsolutePath(), ((ViewHolderParticipantsList)holder).contactMail + ".jpg");
						} else {
							avatar = new File(context.getCacheDir().getAbsolutePath(), ((ViewHolderParticipantsList)holder).contactMail + ".jpg");
						}
						Bitmap bitmap = null;
						if (avatar.exists()) {
							if (avatar.length() > 0) {
								BitmapFactory.Options bOpts = new BitmapFactory.Options();
								bOpts.inPurgeable = true;
								bOpts.inInputShareable = true;
								bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
								if (bitmap == null) {
									avatar.delete();
									if (context.getExternalCacheDir() != null) {
										megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
									} else {
										megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
									}
								} else {
									((ViewHolderParticipantsList)holder).contactInitialLetter.setVisibility(View.GONE);
									((ViewHolderParticipantsList)holder).imageView.setImageBitmap(bitmap);
								}
							} else {
								if (context.getExternalCacheDir() != null) {
									megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
								} else {
									megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
								}
							}
						} else {
							if (context.getExternalCacheDir() != null) {
								megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
							} else {
								megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
							}
						}
						((ViewHolderParticipantsList) holder).imageButtonThreeDots.setColorFilter(null);
					}
					else{
						log("Participant is NOT contact");
						if (context.getExternalCacheDir() != null) {
							megaApi.getUserAvatar(((ViewHolderParticipantsList)holder).contactMail, context.getExternalCacheDir().getAbsolutePath() + "/" + ((ViewHolderParticipantsList)holder).contactMail + ".jpg", listener);
						} else {
							megaApi.getUserAvatar(((ViewHolderParticipantsList)holder).contactMail, context.getCacheDir().getAbsolutePath() + "/" + ((ViewHolderParticipantsList)holder).contactMail + ".jpg", listener);
						}
					}

				} else {
					log("NOT email- Participant is NOT contact");
					if(position!=0){
						((ViewHolderParticipantsList) holder).imageButtonThreeDots.setColorFilter(ContextCompat.getColor(context, R.color.chat_sliding_panel_separator));
						((ViewHolderParticipantsList)holder).imageButtonThreeDots.setOnClickListener(null);
					}

					if (context.getExternalCacheDir() != null) {
						megaApi.getUserAvatar(((ViewHolderParticipantsList)holder).userHandle, context.getExternalCacheDir().getAbsolutePath() + "/" + ((ViewHolderParticipantsList)holder).userHandle + ".jpg", listener);
					} else {
						megaApi.getUserAvatar(((ViewHolderParticipantsList)holder).userHandle, context.getCacheDir().getAbsolutePath() + "/" + ((ViewHolderParticipantsList)holder).userHandle + ".jpg", listener);
					}
				}
			}

			((ViewHolderParticipantsList)holder).textViewContent.setText(((ViewHolderParticipantsList)holder).contactMail);

			int permission = participant.getPrivilege();

			if (permission == MegaChatRoom.PRIV_STANDARD) {
				((ViewHolderParticipantsList)holder).permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
			} else if (permission == MegaChatRoom.PRIV_MODERATOR) {
				((ViewHolderParticipantsList)holder).permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
			} else {
				((ViewHolderParticipantsList)holder).permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
			}

			((ViewHolderParticipantsList)holder).imageButtonThreeDots.setTag(holder);
		}
		else{
			log("Bind item add participant");
			((ViewHolderAddParticipant)holder).itemLayout.setOnClickListener(this);
		}
	}
	
	public void createDefaultAvatar(ViewHolderParticipantsList holder){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		String color = megaApi.getUserAvatarColor(holder.userHandle);
		if(color!=null){
			log("The color to set the avatar is "+color);
			p.setColor(Color.parseColor(color));
		}
		else{
			log("Default color to the avatar");
			p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
		}

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		((ViewHolderParticipantsList)holder).imageView.setImageBitmap(defaultAvatar);

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = context.getResources().getDisplayMetrics().density;

//		String fullName;
		if(holder.fullName!=null&&(!(holder.fullName.trim().isEmpty()))){
			String firstLetter = holder.fullName.charAt(0) + "";
			firstLetter = firstLetter.toUpperCase(Locale.getDefault());
			holder.contactInitialLetter.setText(firstLetter);
			holder.contactInitialLetter.setTextColor(Color.WHITE);
			holder.contactInitialLetter.setVisibility(View.VISIBLE);

			holder.contactInitialLetter.setTextSize(24);
		}
		else{
			holder.contactInitialLetter.setVisibility(View.GONE);
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
		log("getItemCount");
		int permission = participants.get(0).getPrivilege();

		if (permission == MegaChatRoom.PRIV_MODERATOR) {
			log("getItemCount: moderator type");
			int participantNumber = participants.size()+1;
			log("return value: "+participantNumber);
			return participantNumber;
		} else {
			return participants.size();
		}
    }

	@Override
	public int getItemViewType(int position) {
		log("getItemViewType: position"+position);
		int permission = participants.get(0).getPrivilege();

		if (permission == MegaChatRoom.PRIV_MODERATOR) {
			log("getItemViewType: moderator type");
			if (position<participants.size()) {
				return ITEM_VIEW_TYPE_NORMAL;
			} else {
				log("Type ADD_PARTICIPANT");
				return ITEM_VIEW_TYPE_ADD_PARTICIPANT;
			}
		} else {
			return ITEM_VIEW_TYPE_NORMAL;
		}
	}

	public Object getItem(int position) {
		log("getItem: "+position);
		log("participants size: "+participants.size());
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
		log("setPositionClicked: "+p);
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

		if(!Util.isOnline(context)){
			if(context instanceof GroupChatInfoActivityLollipop){
				((GroupChatInfoActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
			}
			return;
		}

		switch (v.getId()){
			case R.id.participant_list_three_dots:{
				log("contact_list_three_dots");
				ViewHolderParticipantsList holder = (ViewHolderParticipantsList) v.getTag();
				int currentPosition = holder.currentPosition;
				MegaChatParticipant p = (MegaChatParticipant) getItem(currentPosition);
				if(p==null){
					log("Participant is null");
				}
				else{
					log("Selected: "+p.getFullName());
				}
				((GroupChatInfoActivityLollipop) context).showParticipantsPanel(p);
				break;
			}
			case R.id.add_participant_list_item_layout:{
				log("add_participant_item_layout");
				((GroupChatInfoActivityLollipop) context).chooseAddParticipantDialog();
				break;
			}
		}
	}
	
	public void setParticipants (ArrayList<MegaChatParticipant> participants){
		log("setParticipants: "+participants.size());
		this.participants = participants;
		positionClicked = -1;
		notifyDataSetChanged();
	}

	public void updateParticipant(int position, ArrayList<MegaChatParticipant> participants){
		log("updateParticipant");
		this.participants = participants;
		positionClicked = -1;
		notifyItemChanged(position);
	}

	public void removeParticipant(int position, ArrayList<MegaChatParticipant> participants){
		log("updateParticipant");
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

	public void updateContactStatus(int position, long userHandle, int state){
		log("updateContactStatus: "+position);

		holderList = (MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList) listFragment.findViewHolderForAdapterPosition(position);
		if(holderList!=null){

			if(state == MegaChatApi.STATUS_ONLINE){
				log("This user is connected");
				holderList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
			}
			else if(state == MegaChatApi.STATUS_AWAY){
				log("This user is away");
				holderList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
			}
			else if(state == MegaChatApi.STATUS_BUSY){
				log("This user is busy");
				holderList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
			}
			else{
				log("This user status is: "+state);
				holderList.statusImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
			}
		}
		else{
			log("Holder is NULL");
			notifyItemChanged(position);
		}

	}
	
	private static void log(String log) {
		Util.log("MegaParticipantsChatLollipopAdapter", log);
	}

	public RecyclerView getListFragment() {
		return listFragment;
	}


	public void setListFragment(RecyclerView listFragment) {
		this.listFragment = listFragment;
	}
}
