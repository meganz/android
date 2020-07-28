package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;

public class MegaListChatLollipopAdapter extends RecyclerView.Adapter<MegaListChatLollipopAdapter.ViewHolderChatList> implements OnClickListener, View.OnLongClickListener, SectionTitleProvider, RotatableAdapter {

	public static final int ITEM_VIEW_TYPE_NORMAL_SELECTED = 0;
	public static final int ITEM_VIEW_TYPE_NORMAL_UNSELECTED = 1;
	public static final int ITEM_VIEW_TYPE_ARCHIVED_CHATS = 2;

	public static final int ADAPTER_RECENT_CHATS = 0;
	public static final int ADAPTER_ARCHIVED_CHATS = 1;
	public static final int MAX_WIDTH_TITLE_PORT = 190;
	public static final int MAX_WIDTH_CONTENT_PORT = 200;
	public static final int MAX_WIDTH_TITLE_LAND = 400;
	public static final int MAX_WIDTH_CONTENT_LAND = 410;

	Context context;
	int positionClicked;
	ArrayList<MegaChatListItem> chats;
	RecyclerView listFragment;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	boolean multipleSelect = false;
	private SparseBooleanArray selectedItems;
	Object fragment;

	DisplayMetrics outMetrics;
	ChatController cC;

	DatabaseHandler dbH = null;

	int adapterType;

	public MegaListChatLollipopAdapter(Context _context, Object _fragment, ArrayList<MegaChatListItem> _chats, RecyclerView _listView, int type) {
		logDebug("New adapter");
		this.context = _context;
		this.chats = _chats;
		this.positionClicked = -1;
		this.fragment = _fragment;
		this.adapterType = type;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		if (megaChatApi == null){
			megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
		}

		listFragment = _listView;

		cC = new ChatController(context);
		
		if(context instanceof ChatExplorerActivity || context instanceof FileExplorerActivityLollipop){
			selectedItems = new SparseBooleanArray();
			multipleSelect = true;
		}
	}

	/*public view holder class*/
    public static class ViewHolderChatList extends ViewHolder {
		public ViewHolderChatList(View arg0) {
			super(arg0);
		}

		RelativeLayout itemLayout;
	}

	public static class ViewHolderNormalChatList extends ViewHolderChatList{
		public ViewHolderNormalChatList(View arg0) {
			super(arg0);
		}
		RoundedImageView imageView;
		EmojiTextView textViewContactName;
		EmojiTextView textViewContent;
		LinearLayout voiceClipOrLocationLayout;
		TextView voiceClipOrLocationText;
		ImageView voiceClipOrLocationIc;
		TextView textViewDate;
		ImageView iconMyVideoOn;
		ImageView iconMyAudioOff;

		String textFastScroller = "";
		ImageButton imageButtonThreeDots;
		RelativeLayout circlePendingMessages;

		TextView numberPendingMessages;
		ImageView muteIcon;
		ImageView contactStateIcon;
		ImageView privateChatIcon;
		ImageView callInProgressIcon;
		String contactMail;
		String fullName = "";

		public int currentPosition;
		public long userHandle;
		public boolean nameRequestedAction = false;

		public String getContactMail (){
			return contactMail;
		}

		public void setImageView(Bitmap bitmap){
			imageView.setImageBitmap(bitmap);
		}
	}

	public static class ViewHolderArchivedChatList extends ViewHolderChatList {
		public ViewHolderArchivedChatList(View arg0) {
			super(arg0);
		}

		TextView textViewArchived;
	}

    ViewHolderChatList holder;

	@Override
	public void onBindViewHolder(ViewHolderChatList holder, int position) {
		final int itemType = getItemViewType(position);
		logDebug("position: " + position + ", itemType: " + itemType);

		holder.itemView.setVisibility(View.VISIBLE);

		if(itemType == ITEM_VIEW_TYPE_NORMAL_SELECTED || itemType == ITEM_VIEW_TYPE_NORMAL_UNSELECTED) {
			((ViewHolderNormalChatList)holder).imageView.setImageBitmap(null);
			MegaChatListItem chat = (MegaChatListItem) getItem(position);

			setTitle(position, holder);

			((ViewHolderNormalChatList)holder).userHandle = -1;

			if(!chat.isGroup()){
				logDebug("Chat one to one");
				long contactHandle = chat.getPeerHandle();
				String userHandleEncoded = MegaApiAndroid.userHandleToBase64(contactHandle);

				((ViewHolderNormalChatList)holder).contactMail = megaChatApi.getContactEmail(contactHandle);

				if (itemType == ITEM_VIEW_TYPE_NORMAL_SELECTED) {
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
					((ViewHolderNormalChatList) holder).imageView.setImageResource(R.drawable.ic_select_avatar);
				} else {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					setUserAvatar(holder, userHandleEncoded);
				}

				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
				((ViewHolderNormalChatList)holder).privateChatIcon.setVisibility(View.VISIBLE);
				((ViewHolderNormalChatList)holder).contactStateIcon.setVisibility(View.VISIBLE);

				if (outMetrics == null) {
					Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
					outMetrics = new DisplayMetrics ();
					display.getMetrics(outMetrics);
				}

				((ViewHolderNormalChatList)holder).contactStateIcon.setMaxWidth(scaleWidthPx(6,outMetrics));
				((ViewHolderNormalChatList)holder).contactStateIcon.setMaxHeight(scaleHeightPx(6,outMetrics));

				setStatus(position, holder);
			}
			else{
				logDebug("Group chat");
				((ViewHolderNormalChatList)holder).contactStateIcon.setVisibility(View.GONE);

				if(chat.isPublic()){
					((ViewHolderNormalChatList)holder).privateChatIcon.setVisibility(View.GONE);
				}
				else{
					((ViewHolderNormalChatList)holder).privateChatIcon.setVisibility(View.VISIBLE);
				}

				if (itemType == ITEM_VIEW_TYPE_NORMAL_SELECTED) {
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.new_multiselect_color));
					((ViewHolderNormalChatList) holder).imageView.setImageResource(R.drawable.ic_select_avatar);
				} else {
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					createGroupChatAvatar(holder, getTitleChat(chat));
				}
			}

			setPendingMessages(position, holder);

			setTs(position, holder);

			setLastMessage(position, holder);

			checkMuteIcon(position, ((ViewHolderNormalChatList)holder), chat);

			if(context instanceof ChatExplorerActivity || context instanceof FileExplorerActivityLollipop){

				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setVisibility(View.GONE);
				if(chat.getOwnPrivilege()==MegaChatRoom.PRIV_RM||chat.getOwnPrivilege()==MegaChatRoom.PRIV_RO){
					((ViewHolderNormalChatList)holder).imageView.setAlpha(.4f);

					holder.itemLayout.setOnClickListener(null);
					holder.itemLayout.setOnLongClickListener(null);

					((ViewHolderNormalChatList)holder).circlePendingMessages.setAlpha(.4f);

					((ViewHolderNormalChatList)holder).textViewContent.setTextColor(context.getResources().getColor(R.color.text_secondary));
					((ViewHolderNormalChatList)holder).textViewDate.setTextColor(context.getResources().getColor(R.color.text_secondary));
					((ViewHolderNormalChatList)holder).textViewContactName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
				}
				else{
					((ViewHolderNormalChatList)holder).imageView.setAlpha(1.0f);

					((ViewHolderNormalChatList)holder).imageButtonThreeDots.setTag(holder);

					holder.itemLayout.setOnClickListener(this);
					holder.itemLayout.setOnLongClickListener(null);

					((ViewHolderNormalChatList)holder).circlePendingMessages.setAlpha(1.0f);

					((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
					((ViewHolderNormalChatList)holder).textViewDate.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
					((ViewHolderNormalChatList)holder).textViewContactName.setTextColor(ContextCompat.getColor(context, R.color.file_list_first_row));
				}
			}
			else{
				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);

				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setTag(holder);

				holder.itemLayout.setOnClickListener(this);
				holder.itemLayout.setOnLongClickListener(this);
			}
			if (chat.isCallInProgress() && megaChatApi != null && megaChatApi.getNumCalls() != 0) {
				MegaChatCall call = megaChatApi.getChatCall(chat.getChatId());
				if (call != null) {
					if(call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT || call.getStatus() == MegaChatCall.CALL_STATUS_DESTROYED){
						((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout.setVisibility(View.GONE);
						((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
						((ViewHolderNormalChatList) holder).textViewContent.setVisibility(View.VISIBLE);
						((ViewHolderNormalChatList) holder).iconMyAudioOff.setVisibility(View.GONE);
						((ViewHolderNormalChatList) holder).iconMyVideoOn.setVisibility(View.GONE);
						if(chat.isGroup()){
							((ViewHolderNormalChatList) holder).callInProgressIcon.setVisibility(View.VISIBLE);
							((ViewHolderNormalChatList) holder).textViewContent.setText(context.getString(R.string.ongoing_call_messages));
						}else{
							((ViewHolderNormalChatList)holder).callInProgressIcon.setVisibility(View.GONE);
							((ViewHolderNormalChatList)holder).textViewContent.setText(context.getString(R.string.title_notification_call_in_progress));
						}
					}else if(call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS || call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT ){
						((ViewHolderNormalChatList)holder).callInProgressIcon.setVisibility(View.GONE);
						((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout.setVisibility(View.GONE);
						((ViewHolderNormalChatList)holder).textViewContent.setText(context.getString(R.string.call_started_messages));
						((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
						((ViewHolderNormalChatList)holder).textViewContent.setVisibility(View.VISIBLE);
						if(call.hasLocalAudio()){
							((ViewHolderNormalChatList)holder).iconMyAudioOff.setVisibility(View.GONE);
						}else{
							((ViewHolderNormalChatList)holder).iconMyAudioOff.setVisibility(View.VISIBLE);
						}
						if(call.hasLocalVideo()){
							((ViewHolderNormalChatList)holder).iconMyVideoOn.setVisibility(View.VISIBLE);
						}else{
							((ViewHolderNormalChatList)holder).iconMyVideoOn.setVisibility(View.GONE);
						}
					}
					return;
				}
			}

			((ViewHolderNormalChatList)holder).callInProgressIcon.setVisibility(View.GONE);
			((ViewHolderNormalChatList)holder).iconMyAudioOff.setVisibility(View.GONE);
			((ViewHolderNormalChatList)holder).iconMyVideoOn.setVisibility(View.GONE);
		}
		else if(itemType == ITEM_VIEW_TYPE_ARCHIVED_CHATS) {
			if (context instanceof ManagerActivityLollipop && ((ManagerActivityLollipop) context).isSearchOpen()) {
				holder.itemView.setVisibility(View.GONE);
				return;
			}

			((ViewHolderArchivedChatList)holder).textViewArchived.setOnClickListener(this);
			((ViewHolderArchivedChatList)holder).textViewArchived.setTag(holder);

			holder.itemLayout.setOnClickListener(null);
			holder.itemLayout.setOnLongClickListener(null);

			ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();
			if(archivedChats!=null){
				((ViewHolderArchivedChatList)holder).textViewArchived.setText(context.getString(R.string.archived_chats_show_option, archivedChats.size()));
			}
			else{
				((ViewHolderArchivedChatList)holder).textViewArchived.setText(context.getString(R.string.archived_chats_title_section));
			}
		}
	}

	/**
	 * Method to get the holder.
	 *
	 * @param position Position in the adapter.
	 * @return The ViewHolderNormalChatList in this position.
	 */
	private ViewHolderNormalChatList getHolder(int position) {
		return (ViewHolderNormalChatList) listFragment.findViewHolderForAdapterPosition(position);
	}

	private void checkMuteIcon(int position, ViewHolderNormalChatList holder, final MegaChatListItem chat) {
		if (holder == null) {
			holder = getHolder(position);
		}
		if (holder == null)
			return;

		if (!(context instanceof ManagerActivityLollipop)) {
			holder.muteIcon.setVisibility(View.GONE);
			return;
		}
		holder.muteIcon.setVisibility(isEnableChatNotifications(chat.getChatId()) ? View.GONE : View.VISIBLE);
	}

	/**
	 * Method for updating the UI when the Dnd changes.
	 *
	 * @param position The position in adapter.
	 */
	public void updateMuteIcon(int position) {
		MegaChatListItem chat = getChatAt(position);
		if (chat == null)
			return;

		ViewHolderNormalChatList holder = getHolder(position);
		if (holder != null) {
			checkMuteIcon(position, holder, chat);
		} else {
			notifyItemChanged(position);
		}
	}

	public void setUserAvatar(ViewHolderChatList holder, String userHandle){

		/*Default Avatar*/
		String name = null;
		if (((ViewHolderNormalChatList)holder).fullName != null && ((ViewHolderNormalChatList)holder).fullName.trim().length() > 0){
			name = ((ViewHolderNormalChatList)holder).fullName;
		}else if(((ViewHolderNormalChatList)holder).contactMail != null && ((ViewHolderNormalChatList)holder).contactMail.length() > 0){
			name = ((ViewHolderNormalChatList)holder).contactMail;
		}
		((ViewHolderNormalChatList)holder).imageView.setImageBitmap(getDefaultAvatar(getColorAvatar(userHandle), name, AVATAR_SIZE, true));

		/*Avatar*/
		ChatUserAvatarListener listener = new ChatUserAvatarListener(context, holder);
		File avatar = ((ViewHolderNormalChatList)holder).contactMail == null ?
                buildAvatarFile(context,userHandle + ".jpg") :
                buildAvatarFile(context,((ViewHolderNormalChatList)holder).contactMail + ".jpg");

		Bitmap bitmap = null;
		if (isFileAvailable(avatar)){
			if (avatar.length() > 0){
				BitmapFactory.Options bOpts = new BitmapFactory.Options();
				bOpts.inPurgeable = true;
				bOpts.inInputShareable = true;
				bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
				if (bitmap == null) {
					avatar.delete();

					if(megaApi==null){
						logWarning("megaApi is Null in Offline mode");
						return;
					}

                    megaApi.getUserAvatar(((ViewHolderNormalChatList)holder).contactMail,buildAvatarFile(context,((ViewHolderNormalChatList)holder).contactMail + ".jpg").getAbsolutePath(),listener);
                }else{
					((ViewHolderNormalChatList)holder).imageView.setImageBitmap(bitmap);
				}
			}else{

				if(megaApi==null){
					logWarning("megaApi is Null in Offline mode");
					return;
				}
                megaApi.getUserAvatar(((ViewHolderNormalChatList)holder).contactMail,buildAvatarFile(context,((ViewHolderNormalChatList)holder).contactMail + ".jpg").getAbsolutePath(),listener);
			}
		}else{

			if(megaApi==null){
				logWarning("megaApi is Null in Offline mode");
				return;
			}
            megaApi.getUserAvatar(((ViewHolderNormalChatList)holder).contactMail,buildAvatarFile(context,((ViewHolderNormalChatList)holder).contactMail + ".jpg").getAbsolutePath(),listener);
		}
	}

	public String formatStringDuration(int duration) {

		if (duration > 0) {
			int hours = duration / 3600;
			int minutes = (duration % 3600) / 60;
			int seconds = duration % 60;

			String timeString;
			if (hours > 0) {
				timeString = " %d " + context.getResources().getString(R.string.initial_hour) + " %d " + context.getResources().getString(R.string.initial_minute);
				timeString = String.format(timeString, hours, minutes);
			} else if(minutes>0){
				timeString = " %d " + context.getResources().getString(R.string.initial_minute) + " %02d " + context.getResources().getString(R.string.initial_second);
				timeString = String.format(timeString, minutes, seconds);
			}
			else{
				timeString = " %02d " + context.getResources().getString(R.string.initial_second);
				timeString = String.format(timeString, seconds);
			}
			return timeString;
		}
		return "0";
	}

	@Override
	public ViewHolderChatList onCreateViewHolder(ViewGroup parent, int viewType) {
		logDebug("onCreateViewHolder");

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);

		dbH = DatabaseHandler.getDbHandler(context);
		View v = null;

		if(viewType == ITEM_VIEW_TYPE_NORMAL_SELECTED || viewType == ITEM_VIEW_TYPE_NORMAL_UNSELECTED) {
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat_list, parent, false);
			holder = new ViewHolderNormalChatList(v);
			holder.itemLayout = v.findViewById(R.id.recent_chat_list_item_layout);
			((ViewHolderNormalChatList) holder).muteIcon = v.findViewById(R.id.recent_chat_list_mute_icon);

			((ViewHolderNormalChatList) holder).imageView = v.findViewById(R.id.recent_chat_list_thumbnail);
			((ViewHolderNormalChatList) holder).textViewContactName = v.findViewById(R.id.recent_chat_list_name);
			((ViewHolderNormalChatList) holder).textViewContent = v.findViewById(R.id.recent_chat_list_content);

			if(isScreenInPortrait(context)){
				((ViewHolderNormalChatList) holder).textViewContactName.setMaxWidthEmojis(px2dp(MAX_WIDTH_TITLE_PORT, outMetrics));
				((ViewHolderNormalChatList) holder).textViewContent.setMaxWidthEmojis(px2dp(MAX_WIDTH_CONTENT_PORT, outMetrics));
			}else{
				((ViewHolderNormalChatList) holder).textViewContactName.setMaxWidthEmojis(px2dp(MAX_WIDTH_TITLE_LAND, outMetrics));
				((ViewHolderNormalChatList) holder).textViewContent.setMaxWidthEmojis(px2dp(MAX_WIDTH_CONTENT_LAND, outMetrics));
			}
			((ViewHolderNormalChatList) holder).textViewContent.setNeccessaryShortCode(false);

			((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout = v.findViewById(R.id.last_message_voice_clip_or_location);
			((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout.setVisibility(View.GONE);
			((ViewHolderNormalChatList) holder).voiceClipOrLocationText = v.findViewById(R.id.last_message_voice_clip_or_location_text);
			((ViewHolderNormalChatList) holder).voiceClipOrLocationIc = v.findViewById(R.id.last_message_voice_clip_or_location_ic);

			((ViewHolderNormalChatList)holder).textViewDate = v.findViewById(R.id.recent_chat_list_date);
			((ViewHolderNormalChatList)holder).iconMyAudioOff = v.findViewById(R.id.recent_chat_list_micro_off);

			((ViewHolderNormalChatList)holder).iconMyAudioOff.setVisibility(View.GONE);

			((ViewHolderNormalChatList)holder).iconMyVideoOn = v.findViewById(R.id.recent_chat_list_video_on);
			((ViewHolderNormalChatList)holder).iconMyVideoOn.setVisibility(View.GONE);


			((ViewHolderNormalChatList)holder).imageButtonThreeDots = v.findViewById(R.id.recent_chat_list_three_dots);

			if((context instanceof ManagerActivityLollipop) || (context instanceof ArchivedChatsActivity)){
				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setOnClickListener(this);
			}else{
				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setVisibility(View.GONE);
				((ViewHolderNormalChatList)holder).imageButtonThreeDots.setOnClickListener(null);
			}

			((ViewHolderNormalChatList)holder).circlePendingMessages = (RelativeLayout) v.findViewById(R.id.recent_chat_list_unread_circle);
			((ViewHolderNormalChatList)holder).numberPendingMessages = (TextView) v.findViewById(R.id.recent_chat_list_unread_number);

			((ViewHolderNormalChatList)holder).contactStateIcon = (ImageView) v.findViewById(R.id.recent_chat_list_contact_state);
			((ViewHolderNormalChatList)holder).privateChatIcon = (ImageView) v.findViewById(R.id.recent_chat_list_private_icon);

			((ViewHolderNormalChatList)holder).callInProgressIcon = (ImageView) v.findViewById(R.id.recent_chat_list_call_in_progress);
			((ViewHolderNormalChatList)holder).callInProgressIcon.setVisibility(View.GONE);

		}else if(viewType == ITEM_VIEW_TYPE_ARCHIVED_CHATS){
			v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archived_chat_option_list, parent, false);
			holder = new ViewHolderArchivedChatList(v);
			holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_archived_chat_option_list_layout);

			((ViewHolderArchivedChatList)holder).textViewArchived = (TextView) v.findViewById(R.id.archived_chat_option_text);
		}

		v.setTag(holder);

		return holder;
	}

	public void setUnreadCount(int unreadMessages, ViewHolderChatList holder){
		logDebug("unreadMessages: " + unreadMessages);

		Bitmap image=null;
		String numberString = "";

		int heightPendingMessageIcon = (int) context.getResources().getDimension(R.dimen.width_image_pending_message_one_digit);

		if(unreadMessages<0){
			unreadMessages = Math.abs(unreadMessages);
			logDebug("Unread number: " + unreadMessages);
			numberString = "+"+unreadMessages;
		}
		else{
			numberString = unreadMessages+"";
		}

//		numberString="20";
		int size = numberString.length();

		((ViewHolderNormalChatList)holder).circlePendingMessages.setVisibility(View.VISIBLE);
		switch(size){
			case 0:{
				logWarning("0 digits - error!");
				((ViewHolderNormalChatList)holder).circlePendingMessages.setVisibility(View.GONE);
				break;
			}
			case 1:{
				((ViewHolderNormalChatList)holder).circlePendingMessages.setBackground(context.getResources().getDrawable(R.drawable.ic_unread_1));
				break;
			}
			case 2:{
				((ViewHolderNormalChatList)holder).circlePendingMessages.setBackground(context.getResources().getDrawable(R.drawable.ic_unread_2));
				break;
			}
			case 3:{
				((ViewHolderNormalChatList)holder).circlePendingMessages.setBackground(context.getResources().getDrawable(R.drawable.ic_unread_3));
				break;
			}
			default:{
				((ViewHolderNormalChatList)holder).circlePendingMessages.setBackground(context.getResources().getDrawable(R.drawable.ic_unread_4));
				break;
			}
		}
		((ViewHolderNormalChatList)holder).numberPendingMessages.setText(numberString);
	}

	public void createGroupChatAvatar(ViewHolderChatList holder, String chatTitle){
		((ViewHolderNormalChatList)holder).imageView.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR), chatTitle, AVATAR_SIZE, true));
	}

	@Override
	public int getItemCount() {

		if(context instanceof ManagerActivityLollipop){
			ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();

			if(archivedChats!=null && archivedChats.size()>0){
				return chats.size()+1;
			}
			else{
				return chats.size();
			}
		}
		else{
			return chats.size();
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (position >= chats.size()) {
			return ITEM_VIEW_TYPE_ARCHIVED_CHATS;
		} else if (multipleSelect && isItemChecked(position)) {
			return ITEM_VIEW_TYPE_NORMAL_SELECTED;
		} else {
			return ITEM_VIEW_TYPE_NORMAL_UNSELECTED;
		}
	}

	public boolean isMultipleSelect() {
		logDebug("isMultipleSelect");
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		logDebug("setMultipleSelect");
		if (!this.multipleSelect && multipleSelect) {
			selectedItems = new SparseBooleanArray();
		}
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
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

		if (!(listFragment.findViewHolderForLayoutPosition(pos) instanceof ViewHolderNormalChatList)) {
			return;
		}

		ViewHolderNormalChatList view = (ViewHolderNormalChatList) listFragment.findViewHolderForLayoutPosition(pos);
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
						if(context instanceof ManagerActivityLollipop || context instanceof ArchivedChatsActivity){
							((RecentChatsFragmentLollipop) fragment).hideMultipleSelect();
						}
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

		if (!(listFragment.findViewHolderForLayoutPosition(pos) instanceof ViewHolderNormalChatList)) {
			return;
		}

		ViewHolderNormalChatList view = (ViewHolderNormalChatList) listFragment.findViewHolderForLayoutPosition(pos);
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
						if(context instanceof ManagerActivityLollipop || context instanceof ArchivedChatsActivity){
							((RecentChatsFragmentLollipop) fragment).hideMultipleSelect();
						}
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			view.imageView.startAnimation(flipAnimation);
		}
		else {
			if (selectedItems.size() <= 0){
				if(context instanceof ManagerActivityLollipop || context instanceof ArchivedChatsActivity){
					((RecentChatsFragmentLollipop) fragment).hideMultipleSelect();
				}
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
		if (selectedItems != null) {
			logDebug("get SelectedItems");
			List<Integer> items = new ArrayList<Integer>(selectedItems.size());
			for (int i = 0; i < selectedItems.size(); i++) {
				items.add(selectedItems.keyAt(i));
			}
			return items;
		} else {
			return null;
		}
	}

	@Override
	public int getFolderCount() {
		return 0;
	}

	@Override
	public int getPlaceholderCount() {
		return 0;
	}

	@Override
	public int getUnhandledItem() {
		return -1;
	}

	/*
	 * Get request at specified position
	 */
	public MegaChatListItem getChatAt(int position) {
		try {
			if (chats != null) {
				return chats.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}

	/*
	 * Get list of all selected chats
	 */
	public ArrayList<MegaChatListItem> getSelectedChats() {
		ArrayList<MegaChatListItem> chats = new ArrayList<MegaChatListItem>();

		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaChatListItem r = getChatAt(selectedItems.keyAt(i));
				if (r != null){
					chats.add(r);
				}
			}
		}
		return chats;
	}

    public Object getItem(int position) {
        return chats.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPositionClicked (){
    	return positionClicked;
    }

    public void setPositionClicked(int p){
		logDebug("position: " + p);
    	positionClicked = p;
		notifyDataSetChanged();
    }

	@Override
	public void onClick(View v) {
		ViewHolderChatList holder = (ViewHolderChatList) v.getTag();

		switch (v.getId()){
			case R.id.recent_chat_list_three_dots:{
				int currentPosition = holder.getAdapterPosition();
				logDebug("Current position: " + currentPosition);
				MegaChatListItem c = (MegaChatListItem) getItem(currentPosition);
				if(context instanceof ManagerActivityLollipop) {

					if (multipleSelect) {
						((RecentChatsFragmentLollipop) fragment).itemClick(currentPosition);
					} else {
						((ManagerActivityLollipop) context).showChatPanel(c);
					}
				}
				else if(context instanceof ArchivedChatsActivity) {
					if (multipleSelect) {
						((RecentChatsFragmentLollipop) fragment).itemClick(currentPosition);
					} else {
						((ArchivedChatsActivity) context).showChatPanel(c);
					}
				}

				break;
			}
			case R.id.recent_chat_list_item_layout:{
				logDebug("Click layout!");
				int currentPosition = holder.getAdapterPosition();
				logDebug("Current position: " + currentPosition);
				MegaChatListItem c = (MegaChatListItem) getItem(currentPosition);

				if(context instanceof ManagerActivityLollipop){
					((RecentChatsFragmentLollipop) fragment).itemClick(currentPosition);
				}
				else if(context instanceof ChatExplorerActivity || context instanceof FileExplorerActivityLollipop){
					((ChatExplorerFragment) fragment).itemClick(currentPosition);
				}
				else if(context instanceof ArchivedChatsActivity){
					((RecentChatsFragmentLollipop) fragment).itemClick(currentPosition);
				}

				break;
			}
			case R.id.archived_chat_option_text:{
				logDebug("Show archived chats");

				Intent archivedChatsIntent = new Intent(context, ArchivedChatsActivity.class);
				context.startActivity(archivedChatsIntent);
				break;
			}
		}
	}

	@Override
	public boolean onLongClick(View view) {
		logDebug("OnLongCLick");
		ViewHolderChatList holder = (ViewHolderChatList) view.getTag();
		int currentPosition = holder.getAdapterPosition();

		if(context instanceof ManagerActivityLollipop || context instanceof ArchivedChatsActivity) {
			((RecentChatsFragmentLollipop) fragment).activateActionMode();
			((RecentChatsFragmentLollipop) fragment).itemClick(currentPosition);
		}

		return true;
	}


	public void updateNonContactName(int pos, long userHandle){
		logDebug("updateNonContactName: " + pos + "_" + userHandle);
		ViewHolderNormalChatList view = (ViewHolderNormalChatList) listFragment.findViewHolderForLayoutPosition(pos);

		if(view!=null){
			if(view.userHandle == userHandle){
				notifyItemChanged(pos);
			}
		}
	}

	public void setStatus(int position, ViewHolderChatList holder){
		logDebug("position: "+position);

		if(holder!=null){
			MegaChatListItem chat = chats.get(position);
			setContactStatus(megaChatApi.getUserOnlineStatus(chat.getPeerHandle()), ((ViewHolderNormalChatList)holder).contactStateIcon);
		}
		else{
			logWarning("Holder is NULL: " + position);
			notifyItemChanged(position);
		}
	}


	public void updateContactStatus(int position, long userHandle, int state) {
		holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
		if (holder != null) {
			setContactStatus(megaChatApi.getUserOnlineStatus(userHandle), ((ViewHolderNormalChatList) holder).contactStateIcon);
		} else {
			logWarning("Holder is NULL");
			notifyItemChanged(position);
		}
	}

	public void setTitle(int position, ViewHolderChatList holder) {
		logDebug("position: " + position);
		if (holder == null) {
			holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
		}

		if(holder!=null){

			MegaChatListItem chat = chats.get(position);
			String title = getTitleChat(chat);

			if(title!=null){
				logDebug("ChatRoom id: "+chat.getChatId());
				logDebug("chat timestamp: "+chat.getLastTimestamp());
				String date = formatDateAndTime(context,chat.getLastTimestamp(), DATE_LONG_FORMAT);
				logDebug("date timestamp: "+date);
				int maxAllowed = getMaxAllowed(title);
				((ViewHolderNormalChatList)holder).textViewContactName.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxAllowed)});
				title = converterShortCodes(title);
				((ViewHolderNormalChatList)holder).textViewContactName.setText(title);

				if(!chat.isGroup()){
					((ViewHolderNormalChatList)holder).fullName = title;
				}
				else{
					createGroupChatAvatar(holder,title);
				}
			}
		}
		else{
			logWarning("Holder is NULL: " + position);
			notifyItemChanged(position);
		}
	}

	public void setTs(int position, ViewHolderChatList holder) {
		logDebug("position: " + position);

		if (holder == null) {
			holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
		}

		if(holder!=null){
			MegaChatListItem chat = chats.get(position);

			int messageType = chat.getLastMessageType();

			if(messageType==MegaChatMessage.TYPE_INVALID) {
				((ViewHolderNormalChatList)holder).textViewDate.setVisibility(View.GONE);
			}
			else{
				logDebug("ChatRoom ID: " + chat.getChatId());
				logDebug("Chat timestamp: " + chat.getLastTimestamp());
				String date = formatDateAndTime(context,chat.getLastTimestamp(), DATE_LONG_FORMAT);
				String dateFS = formatDate(context,chat.getLastTimestamp(), DATE_SHORT_SHORT_FORMAT);
				logDebug("Date timestamp: " + date);
				((ViewHolderNormalChatList)holder).textViewDate.setText(date);
				((ViewHolderNormalChatList)holder).textFastScroller = dateFS;
				((ViewHolderNormalChatList)holder).textViewDate.setVisibility(View.VISIBLE);
			}
		}
		else{
			logWarning("Holder is NULL: " + position);
			notifyItemChanged(position);
		}
	}

	public void setPendingMessages(int position, ViewHolderChatList holder){
		logDebug("position: " + position);
		if(holder == null){
			holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
		}

		if(holder!=null){
			MegaChatListItem chat = chats.get(position);
			int unreadMessages = chat.getUnreadCount();
			logDebug("Unread messages: " + unreadMessages);
			if(chat.getUnreadCount()!=0){
				setUnreadCount(unreadMessages, holder);
			}
			else{
				((ViewHolderNormalChatList)holder).circlePendingMessages.setVisibility(View.GONE);
			}
		}
		else{
			logWarning("Holder is NULL: " + position);
			notifyItemChanged(position);
		}
	}

	private String getMessageSenderName(MegaChatRoom chatRoom, long chatId, long handle) {
		String fullNameAction = getNicknameContact(handle);

		if (isTextEmpty(fullNameAction) && chatRoom != null) {
			fullNameAction = chatRoom.getPeerFullnameByHandle(handle);
		}

		if (isTextEmpty(fullNameAction)) {
			fullNameAction = cC.getFullName(handle, chatId);
		}

		return fullNameAction;
	}

	public void setLastMessage(int position, ViewHolderChatList holder){
		logDebug("position: " + position);
		if(holder == null){
			holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
		}

		if(holder!=null){
			MegaChatListItem chat = chats.get(position);
			int messageType = chat.getLastMessageType();
			MegaChatMessage lastMessage = megaChatApi.getMessage(chat.getChatId(), chat.getLastMessageId());
			logDebug("MessageType: " + messageType);
			String lastMessageString = converterShortCodes(chat.getLastMessage());

            ((ViewHolderNormalChatList)holder).voiceClipOrLocationLayout.setVisibility(View.GONE);

			if(messageType==MegaChatMessage.TYPE_INVALID){
				logDebug("Message Type -> INVALID");
				((ViewHolderNormalChatList)holder).textViewContent.setText(context.getString(R.string.no_conversation_history));
				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
				((ViewHolderNormalChatList)holder).textViewDate.setVisibility(View.GONE);
			}
			else if(messageType==255){
				logDebug("Message Type -> LOADING");
				((ViewHolderNormalChatList)holder).textViewContent.setText(context.getString(R.string.general_loading));
				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
				((ViewHolderNormalChatList)holder).textViewDate.setVisibility(View.GONE);
			}
			else if(messageType==MegaChatMessage.TYPE_ALTER_PARTICIPANTS){
				logDebug("Message Type -> TYPE_ALTER_PARTICIPANTS");
				int privilege = chat.getLastMessagePriv();
				logDebug("Privilege: " + privilege);
				String textToShow = "";

				if(chat.getLastMessageHandle()==megaChatApi.getMyUserHandle()){
					logDebug("I have changed the permissions");

					MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
					String fullNameAction = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageSender());
					if (isTextEmpty(fullNameAction)) {
						if(!((ViewHolderNormalChatList)holder).nameRequestedAction){
							logDebug("Call for nonContactHandle: "+ chat.getLastMessageSender());
							fullNameAction = context.getString(R.string.unknown_name_label);
							((ViewHolderNormalChatList)holder).nameRequestedAction=true;
							((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageSender();

							ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());

							megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
							megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
							megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
						}
						else{
							logDebug("Name already asked and no name received: handle " + chat.getLastMessageSender());
						}

					}

					if(privilege!=MegaChatRoom.PRIV_RM){
						logDebug("I was added");
						String myFullName = megaChatApi.getMyFullname();
						if(myFullName==null){
							myFullName = "";
						}
						if(myFullName.trim().length()<=0){
							myFullName = megaChatApi.getMyEmail();
						}

						if(chat.getLastMessageSender() == chat.getLastMessageHandle()){
							textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(myFullName));
						}
						else{
							textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(myFullName), toCDATA(fullNameAction));
						}

						try{
							textToShow = textToShow.replace("[A]", "");
							textToShow = textToShow.replace("[/A]", "");
							textToShow = textToShow.replace("[B]", "");
							textToShow = textToShow.replace("[/B]", "");
							textToShow = textToShow.replace("[C]", "");
							textToShow = textToShow.replace("[/C]", "");
						}
						catch (Exception e){}
					}
					else{
						logDebug("I was removed or left");
						if(chat.getLastMessageSender()==chat.getLastMessageHandle()){
							logDebug("I left the chat");
							String myFullName = megaChatApi.getMyFullname();
							if(myFullName==null){
								myFullName = "";
							}
							if(myFullName.trim().length()<=0){
								myFullName = megaChatApi.getMyEmail();
							}
							textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), toCDATA(myFullName));
							try{
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
								textToShow = textToShow.replace("[B]", "");
								textToShow = textToShow.replace("[/B]", "");
							}
							catch (Exception e){}
						}
						else{
							String myFullName = megaChatApi.getMyFullname();
							if(myFullName==null){
								myFullName = "";
							}
							if(myFullName.trim().length()<=0){
								myFullName = megaChatApi.getMyEmail();
							}
							textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(myFullName), toCDATA(fullNameAction));
							try{
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
								textToShow = textToShow.replace("[B]", "");
								textToShow = textToShow.replace("[/B]", "");
								textToShow = textToShow.replace("[C]", "");
								textToShow = textToShow.replace("[/C]", "");
							}
							catch (Exception e){}
						}
					}

					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}

					((ViewHolderNormalChatList)holder).textViewContent.setText(result);
				}
				else{

					MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
					String fullNameTitle = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageHandle());

					if(fullNameTitle.trim().length()<=0){
						if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
							logDebug("Call for nonContactHandle: " + chat.getLastMessageHandle());
							fullNameTitle = context.getString(R.string.unknown_name_label);
							((ViewHolderNormalChatList)holder).nameRequestedAction=true;
							((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageHandle();

							ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageHandle(), chat.isPreview());

							megaChatApi.getUserFirstname(chat.getLastMessageHandle(), chatRoom.getAuthorizationToken(), listener);
							megaChatApi.getUserLastname(chat.getLastMessageHandle(), chatRoom.getAuthorizationToken(), listener);
							megaChatApi.getUserEmail(chat.getLastMessageHandle(), listener);
						}
						else{
							logDebug("Name already asked and no name received: handle " + chat.getLastMessageSender());
						}
					}

					if(privilege!=MegaChatRoom.PRIV_RM){
						logDebug("Participant was added");
						if(chat.getLastMessageSender()==megaChatApi.getMyUserHandle()){
							logDebug("By me");
							String myFullName = megaChatApi.getMyFullname();
							if(myFullName==null){
								myFullName = "";
							}
							if(myFullName.trim().length()<=0){
								myFullName = megaChatApi.getMyEmail();
							}

							if(chat.getLastMessageSender() == chat.getLastMessageHandle()){
								textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(fullNameTitle));
							}
							else{
								textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(fullNameTitle), toCDATA(myFullName));
							}
							try{
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
								textToShow = textToShow.replace("[B]", "");
								textToShow = textToShow.replace("[/B]", "");
								textToShow = textToShow.replace("[C]", "");
								textToShow = textToShow.replace("[/C]", "");
							}
							catch (Exception e){}
						}
						else{
							logDebug("By other");
							String fullNameAction = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageSender());
							if(fullNameAction.trim().length()<=0){

								if(fullNameAction.isEmpty()){
									if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
										logDebug("Call for nonContactHandle: " + chat.getLastMessageSender());
										fullNameAction = context.getString(R.string.unknown_name_label);
										((ViewHolderNormalChatList)holder).nameRequestedAction=true;
										((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageSender();

										ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());
										megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
									}
									else{
										logDebug("Name already asked and no name received: handle " + chat.getLastMessageSender());
									}
								}
							}

							if(chat.getLastMessageSender() == chat.getLastMessageHandle()){
								textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(fullNameTitle));
							}
							else{
								textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(fullNameTitle), toCDATA(fullNameAction));
							}

							try{
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
								textToShow = textToShow.replace("[B]", "");
								textToShow = textToShow.replace("[/B]", "");
								textToShow = textToShow.replace("[C]", "");
								textToShow = textToShow.replace("[/C]", "");
							}
							catch (Exception e){}

						}
					}//END participant was added
					else{
						logDebug("Participant was removed or left");
						if(chat.getLastMessageSender()==megaChatApi.getMyUserHandle()){
							String myFullName = megaChatApi.getMyFullname();
							if(myFullName==null){
								myFullName = "";
							}
							if(myFullName.trim().length()<=0){
								myFullName = megaChatApi.getMyEmail();
							}
							textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(fullNameTitle), toCDATA(myFullName));
							try{
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
								textToShow = textToShow.replace("[B]", "");
								textToShow = textToShow.replace("[/B]", "");
								textToShow = textToShow.replace("[C]", "");
								textToShow = textToShow.replace("[/C]", "");
							}
							catch (Exception e){}
						}
						else{

							if(chat.getLastMessageSender()==chat.getLastMessageHandle()){
								logDebug("The participant left the chat");

								textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), toCDATA(fullNameTitle));
								try{
									textToShow = textToShow.replace("[A]", "");
									textToShow = textToShow.replace("[/A]", "");
									textToShow = textToShow.replace("[B]", "");
									textToShow = textToShow.replace("[/B]", "");
								}
								catch (Exception e){}

							}
							else{
								logDebug("The participant was removed");
								String fullNameAction = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageSender());
								if(fullNameAction.trim().length()<=0){
									if(fullNameAction.isEmpty()){
										if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
											logDebug("Call for nonContactHandle: " + chat.getLastMessageSender());
											fullNameAction = context.getString(R.string.unknown_name_label);
											((ViewHolderNormalChatList)holder).nameRequestedAction=true;
											((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageSender();

											ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());
											megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
											megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
											megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
										}
										else{
											logDebug("Name already asked and no name received: handle" + chat.getLastMessageSender());
										}
									}
								}

								textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(fullNameTitle), toCDATA(fullNameAction));
								try{
									textToShow = textToShow.replace("[A]", "");
									textToShow = textToShow.replace("[/A]", "");
									textToShow = textToShow.replace("[B]", "");
									textToShow = textToShow.replace("[/B]", "");
									textToShow = textToShow.replace("[C]", "");
									textToShow = textToShow.replace("[/C]", "");
								}
								catch (Exception e){}
							}
//                        textToShow = String.format(context.getString(R.string.message_remove_participant), message.getHandleOfAction()+"");
						}
					} //END participant removed

					Spanned result = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
						result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
					} else {
						result = Html.fromHtml(textToShow);
					}

					((ViewHolderNormalChatList)holder).textViewContent.setText(result);
				}
				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
			}
			else if(messageType==MegaChatMessage.TYPE_PRIV_CHANGE){
				logDebug("PRIVILEGE CHANGE message");

				int privilege = chat.getLastMessagePriv();
				logDebug("Privilege of the user: " + privilege);

				String privilegeString = "";
				if(privilege==MegaChatRoom.PRIV_MODERATOR){
					privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
				}
				else if(privilege==MegaChatRoom.PRIV_STANDARD){
					privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
				}
				else if(privilege==MegaChatRoom.PRIV_RO){
					privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
				}
				else {
					logDebug("Change to other");
					privilegeString = "Unknow";
				}

				String textToShow = "";

				if(chat.getLastMessageHandle()==megaChatApi.getMyUserHandle()){
					logDebug("A moderator change my privilege");

					if(chat.getLastMessageSender()==megaChatApi.getMyUserHandle()){
						logDebug("I changed my Own permission");
						String myFullName = megaChatApi.getMyFullname();
						if(myFullName==null){
							myFullName = "";
						}
						if(myFullName.trim().length()<=0){
							myFullName = megaChatApi.getMyEmail();
						}
						textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(myFullName), toCDATA(privilegeString), toCDATA(myFullName));
						try{
							textToShow = textToShow.replace("[A]", "");
							textToShow = textToShow.replace("[/A]", "");
							textToShow = textToShow.replace("[B]", "");
							textToShow = textToShow.replace("[/B]", "");
							textToShow = textToShow.replace("[C]", "");
							textToShow = textToShow.replace("[/C]", "");
							textToShow = textToShow.replace("[D]", "");
							textToShow = textToShow.replace("[/D]", "");
							textToShow = textToShow.replace("[E]", "");
							textToShow = textToShow.replace("[/E]", "");
						}
						catch (Exception e){}
					}
					else{
						logDebug("I was change by someone");
						MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
						String fullNameAction = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageSender());

						if(fullNameAction.trim().length()<=0){
							if(fullNameAction.isEmpty()){
								if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
									logDebug("Call for nonContactHandle: " + chat.getLastMessageSender());
									fullNameAction = context.getString(R.string.unknown_name_label);
									((ViewHolderNormalChatList)holder).nameRequestedAction=true;
									((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageSender();

									ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());
									megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
									megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
									megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
								}
								else{
									logDebug("Name already asked and no name received: handle" + chat.getLastMessageSender());
								}
							}
						}
						String myFullName = megaChatApi.getMyFullname();
						if(myFullName==null){
							myFullName = "";
						}
						if(myFullName.trim().length()<=0){
							myFullName = megaChatApi.getMyEmail();
						}

						textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(myFullName), toCDATA(privilegeString), toCDATA(fullNameAction));
						try{
							textToShow = textToShow.replace("[A]", "");
							textToShow = textToShow.replace("[/A]", "");
							textToShow = textToShow.replace("[B]", "");
							textToShow = textToShow.replace("[/B]", "");
							textToShow = textToShow.replace("[C]", "");
							textToShow = textToShow.replace("[/C]", "");
							textToShow = textToShow.replace("[D]", "");
							textToShow = textToShow.replace("[/D]", "");
							textToShow = textToShow.replace("[E]", "");
							textToShow = textToShow.replace("[/E]", "");
						}
						catch (Exception e){}
					}
				}
				else{
					logDebug("Participant privilege change!");

					MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
					String fullNameTitle = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageHandle());

					if(fullNameTitle.trim().length()<=0){
						if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
							logDebug("Call for nonContactHandle: " + chat.getLastMessageHandle());
							fullNameTitle = context.getString(R.string.unknown_name_label);
							((ViewHolderNormalChatList)holder).nameRequestedAction=true;
							((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageHandle();

							ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageHandle(), chat.isPreview());

							megaChatApi.getUserFirstname(chat.getLastMessageHandle(), chatRoom.getAuthorizationToken(), listener);
							megaChatApi.getUserLastname(chat.getLastMessageHandle(), chatRoom.getAuthorizationToken(), listener);
							megaChatApi.getUserEmail(chat.getLastMessageHandle(), listener);
						}
						else{
							logDebug("Name already asked and no name received: handle " + chat.getLastMessageHandle());
						}
					}

					if(chat.getLastMessageSender()==megaChatApi.getMyUserHandle()){
						logDebug("The privilege was change by me");
						String myFullName = megaChatApi.getMyFullname();
						if(myFullName==null){
							myFullName = "";
						}
						if(myFullName.trim().length()<=0){
							myFullName = megaChatApi.getMyEmail();
						}
						textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(fullNameTitle), toCDATA(privilegeString), toCDATA(myFullName));
						try{
							textToShow = textToShow.replace("[A]", "");
							textToShow = textToShow.replace("[/A]", "");
							textToShow = textToShow.replace("[B]", "");
							textToShow = textToShow.replace("[/B]", "");
							textToShow = textToShow.replace("[C]", "");
							textToShow = textToShow.replace("[/C]", "");
							textToShow = textToShow.replace("[D]", "");
							textToShow = textToShow.replace("[/D]", "");
							textToShow = textToShow.replace("[E]", "");
							textToShow = textToShow.replace("[/E]", "");
						}
						catch (Exception e){}

					}
					else{
						logDebug("By other");
						String fullNameAction = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageSender());
						if(fullNameAction.trim().length()<=0){
							if(fullNameAction.isEmpty()){
								if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
									logDebug("Call for nonContactHandle: " + chat.getLastMessageSender());
									fullNameAction = context.getString(R.string.unknown_name_label);
									((ViewHolderNormalChatList)holder).nameRequestedAction=true;
									((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageSender();

									ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());

									megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
									megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
									megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
								}
								else{
									logDebug("Name already asked and no name received: handle " + chat.getLastMessageSender());
								}
							}
						}

						textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(fullNameTitle), toCDATA(privilegeString), toCDATA(fullNameAction));
						try{
							textToShow = textToShow.replace("[A]", "");
							textToShow = textToShow.replace("[/A]", "");
							textToShow = textToShow.replace("[B]", "");
							textToShow = textToShow.replace("[/B]", "");
							textToShow = textToShow.replace("[C]", "");
							textToShow = textToShow.replace("[/C]", "");
							textToShow = textToShow.replace("[D]", "");
							textToShow = textToShow.replace("[/D]", "");
							textToShow = textToShow.replace("[E]", "");
							textToShow = textToShow.replace("[/E]", "");
						}
						catch (Exception e){}
					}
				}

				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);

				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
			}
			else if(messageType==MegaChatMessage.TYPE_TRUNCATE){
				logDebug("Message type TRUNCATE");

				String textToShow = null;
				if(chat.getLastMessageSender() == megaChatApi.getMyUserHandle()){
					String myFullName = megaChatApi.getMyFullname();
					if(myFullName==null){
						myFullName = "";
					}
					if(myFullName.trim().length()<=0){
						myFullName = megaChatApi.getMyEmail();
					}
					textToShow = String.format(context.getString(R.string.history_cleared_by),toCDATA(myFullName));
				}
				else{
					MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
					String fullNameAction = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageSender());
					if(fullNameAction.trim().length()<=0){
						if(fullNameAction.isEmpty()){
							if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
								logDebug("Call for nonContactHandle: " + chat.getLastMessageSender());
								fullNameAction = context.getString(R.string.unknown_name_label);
								((ViewHolderNormalChatList)holder).nameRequestedAction=true;
								((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageSender();

								ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());

								megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
								megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
								megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
							}
							else{
								logDebug("Name already asked and no name received: handle " + chat.getLastMessageSender());
							}
						}
					}

					textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(fullNameAction));
				}

				try{
					textToShow = textToShow.replace("[A]", "");
					textToShow = textToShow.replace("[/A]", "");
					textToShow = textToShow.replace("[B]", "");
					textToShow = textToShow.replace("[/B]", "");
				}
				catch (Exception e){}

				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);

				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
			}
			else if(messageType==MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE) {
				logDebug("Message type TYPE_PUBLIC_HANDLE_CREATE");
				String fullNameAction = getFullNameAction(chat);

				String textToShow = String.format(context.getString(R.string.message_created_chat_link), toCDATA(fullNameAction));

				try{
					textToShow = textToShow.replace("[A]", "");
					textToShow = textToShow.replace("[/A]", "");
					textToShow = textToShow.replace("[B]", "");
					textToShow = textToShow.replace("[/B]", "");
				}
				catch (Exception e){}

				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);

				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
			}
			else if(messageType==MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE) {
				logDebug("Message type TYPE_PUBLIC_HANDLE_DELETE");
				String fullNameAction = getFullNameAction(chat);

				String textToShow = String.format(context.getString(R.string.message_deleted_chat_link), toCDATA(fullNameAction));

				try{
					textToShow = textToShow.replace("[A]", "");
					textToShow = textToShow.replace("[/A]", "");
					textToShow = textToShow.replace("[B]", "");
					textToShow = textToShow.replace("[/B]", "");
				}
				catch (Exception e){}

				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);

				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
			}
			else if(messageType==MegaChatMessage.TYPE_SET_PRIVATE_MODE) {
				logDebug("Message type TYPE_SET_PRIVATE_MODE");

				String fullNameAction = getFullNameAction(chat);

				String textToShow = String.format(context.getString(R.string.message_set_chat_private), toCDATA(fullNameAction));

				try{
					textToShow = textToShow.replace("[A]", "");
					textToShow = textToShow.replace("[/A]", "");
					textToShow = textToShow.replace("[B]", "");
					textToShow = textToShow.replace("[/B]", "");
				}
				catch (Exception e){}

				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
				}
				else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);

				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
			}
			else if(messageType==MegaChatMessage.TYPE_CHAT_TITLE) {
				logDebug("Message type TYPE_CHAT_TITLE");

				String messageContent = chat.getLastMessage();
				String fullNameAction = getFullNameAction(chat);

				String textToShow = String.format(context.getString(R.string.change_title_messages), toCDATA(fullNameAction), converterShortCodes(messageContent));

				try {
					textToShow = textToShow.replace("[A]", "");
					textToShow = textToShow.replace("[/A]", "");
					textToShow = textToShow.replace("[B]", "");
					textToShow = textToShow.replace("[/B]", "");
					textToShow = textToShow.replace("[C]", "");
					textToShow = textToShow.replace("[/C]", "");
				} catch (Exception e) {
				}

				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);

				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));

			}else if(messageType==MegaChatMessage.TYPE_CALL_STARTED){
				logDebug("Message type TYPE_CALL_STARTED");

				String textToShow = context.getResources().getString(R.string.call_started_messages);
				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);
				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));

			}else if(messageType==MegaChatMessage.TYPE_CALL_ENDED){
				logDebug("Message type TYPE_CALL_ENDED");

				String messageContent = chat.getLastMessage();

				char separator = 0x01;
				String separatorString = separator + "";

				String [] sp = messageContent.split(separatorString);

				String textToShow = "";

				if(sp.length>=2){

					String durationString = sp[0];
					String termCodeString = sp[1];

					int duration = Integer.parseInt(durationString);
					int termCode = Integer.parseInt(termCodeString);

					switch(termCode){
						case MegaChatMessage.END_CALL_REASON_ENDED:{

							if(chat.isGroup()){
								textToShow = context.getString(R.string.group_call_ended_no_duration_message);
							}else {
								int hours = duration / 3600;
								int minutes = (duration % 3600) / 60;
								int seconds = duration % 60;
								textToShow = context.getString(R.string.call_ended_message);
								if (hours != 0) {
									String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
									textToShow = textToShow + textHours;
									if ((minutes != 0) || (seconds != 0)) {
										textToShow = textToShow + ", ";
									}
								}
								if (minutes != 0) {
									String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
									textToShow = textToShow + textMinutes;
									if (seconds != 0) {
										textToShow = textToShow + ", ";
									}
								}
								if (seconds != 0) {
									String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
									textToShow = textToShow + textSeconds;
								}
							}

							try{
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
								textToShow = textToShow.replace("[B]", "");
								textToShow = textToShow.replace("[/B]", "");
								textToShow = textToShow.replace("[C]", "");
								textToShow = textToShow.replace("[/C]", "");
							}catch (Exception e){
							}

							break;
						}
						case MegaChatMessage.END_CALL_REASON_REJECTED:{

							textToShow = String.format(context.getString(R.string.call_rejected_messages));
							try {
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
							} catch (Exception e) {
							}

							break;
						}
						case MegaChatMessage.END_CALL_REASON_NO_ANSWER:{

							long lastMsgSender = chat.getLastMessageSender();
							if(lastMsgSender==megaChatApi.getMyUserHandle()){
								textToShow = String.format(context.getString(R.string.call_not_answered_messages));
							}
							else{
								textToShow = String.format(context.getString(R.string.call_missed_messages));
							}

							try {
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
							} catch (Exception e) {
							}

							break;
						}
						case MegaChatMessage.END_CALL_REASON_FAILED:{

							textToShow = String.format(context.getString(R.string.call_failed_messages));
							try {
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
							} catch (Exception e) {
							}

							break;
						}
						case MegaChatMessage.END_CALL_REASON_CANCELLED:{

							long lastMsgSender = chat.getLastMessageSender();
							if(lastMsgSender==megaChatApi.getMyUserHandle()){
								textToShow = String.format(context.getString(R.string.call_cancelled_messages));
							}
							else{
								textToShow = String.format(context.getString(R.string.call_missed_messages));
							}

							try {
								textToShow = textToShow.replace("[A]", "");
								textToShow = textToShow.replace("[/A]", "");
							} catch (Exception e) {
							}

							break;
						}
					}
				}

				Spanned result = null;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
					result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
				} else {
					result = Html.fromHtml(textToShow);
				}

				((ViewHolderNormalChatList)holder).textViewContent.setText(result);

				((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
			}
			else if(messageType==MegaChatMessage.TYPE_CONTAINS_META){
				logDebug("Message type TYPE_CONTAINS_META");

				long messageId = chat.getLastMessageId();
				MegaChatMessage message = megaChatApi.getMessage(chat.getChatId(), messageId);
				if(message==null) return;

				MegaChatContainsMeta meta = message.getContainsMeta();
				if(meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
					logDebug("Message type TYPE_CONTAINS_META:CONTAINS_META_GEOLOCATION");
					long lastMsgSender = chat.getLastMessageSender();
					((ViewHolderNormalChatList)holder).voiceClipOrLocationLayout.setVisibility(View.VISIBLE);
					((ViewHolderNormalChatList)holder).voiceClipOrLocationText.setText(R.string.title_geolocation_message);
					((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_first_row));
					((ViewHolderNormalChatList)holder).textViewContent.setText("");
					if(lastMsgSender==megaChatApi.getMyUserHandle()){

						logDebug("The last message is mine: " + lastMsgSender);
						((ViewHolderNormalChatList)holder).textViewContent.setText(context.getString(R.string.word_me)+" ");
						setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_location_small, true);
					}
					else{
						logDebug("The last message NOT mine" + lastMsgSender);

						if(chat.isGroup()){
							MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());

							((ViewHolderNormalChatList)holder).currentPosition = position;
							((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;

							String fullNameAction = "";
							if(chatRoom!=null){
								fullNameAction = converterShortCodes(getMessageSenderName(chatRoom, chat.getChatId(), lastMsgSender));
								if(fullNameAction==null){
									fullNameAction = "";
								}

								if(fullNameAction.trim().length()<=0){
									fullNameAction = cC.getFirstName(lastMsgSender, chatRoom);
								}
							}
							else{
								logWarning("ERROR: the chatroom is NULL: " + chat.getChatId());
							}

							if(fullNameAction.trim().length()<=0){

//					megaChatApi.getUserFirstname();
								if(fullNameAction.isEmpty()){
									if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
										logDebug("Call for nonContactName: " + lastMsgSender);
										fullNameAction = context.getString(R.string.unknown_name_label);
										((ViewHolderNormalChatList)holder).nameRequestedAction=true;
										((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;
										ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chatRoom.isPreview());
										megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserEmail(lastMsgSender, listener);
									}
									else{
										logWarning("Name already asked and no name received: " + lastMsgSender);
									}
								}
							}

							((ViewHolderNormalChatList)holder).textViewContent.setText(fullNameAction+": ");
							if(chat.getUnreadCount()==0){
								logDebug("Message READ");
								setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_location_small, true);
							}
							else{
								logDebug("Message NOt read");
								setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_location_small, false);
							}
						}
						else{
							if(chat.getUnreadCount()==0){
								logDebug("Message READ");
								setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_location_small, true);
							}
							else{
								logDebug("Message NOt read");
								setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_location_small, false);
							}
						}
					}
				}
				else if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
					logDebug("Rich link message");
					if(lastMessageString==null){
						logWarning("Message Type-> " + messageType + " last content is NULL");
						lastMessageString = context.getString(R.string.error_message_unrecognizable);
					}
					else{
						logDebug("Message Type-> " + messageType + " last content: " + lastMessageString + "length: " + lastMessageString.length());
					}

					long lastMsgSender = chat.getLastMessageSender();
					if(lastMsgSender==megaChatApi.getMyUserHandle()){

						logDebug("The last message is mine: " + lastMsgSender);
						Spannable me = new SpannableString(context.getString(R.string.word_me)+" ");
						me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_first_row)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

						if(lastMessageString!=null) {
							Spannable myMessage = new SpannableString(lastMessageString);
							myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							CharSequence indexedText = TextUtils.concat(me, myMessage);
							((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
							((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);
						}
					}
					else{
						logDebug("The last message NOT mine: " + lastMsgSender);

						if(chat.isGroup()){
							MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());

							((ViewHolderNormalChatList)holder).currentPosition = position;
							((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;

							String fullNameAction = "";
							if(chatRoom!=null){
								fullNameAction = converterShortCodes(getMessageSenderName(chatRoom, chat.getChatId(), lastMsgSender));
								if(fullNameAction==null){
									fullNameAction = "";
								}

								if(fullNameAction.trim().length()<=0){
									fullNameAction = cC.getFirstName(lastMsgSender, chatRoom);
								}
							}
							else{
								logWarning("ERROR: the chatroom is NULL: " + chat.getChatId());
							}

							if(fullNameAction.trim().length()<=0){

//					megaChatApi.getUserFirstname();
								if(fullNameAction.isEmpty()){
									if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
										logDebug("Call for nonContactName: " + lastMsgSender);
										fullNameAction = context.getString(R.string.unknown_name_label);
										((ViewHolderNormalChatList)holder).nameRequestedAction=true;
										((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;
										ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chatRoom.isPreview());
										megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserEmail(lastMsgSender, listener);
									}
									else{
										logWarning("Name already asked and no name received: " + lastMsgSender);
									}
								}
							}

							Spannable name = new SpannableString(fullNameAction+": ");
							name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

							if(chat.getUnreadCount()==0){
								logDebug("Message READ");
								Spannable myMessage = new SpannableString(lastMessageString);
								myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								CharSequence indexedText = TextUtils.concat(name, myMessage);
								((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);
							}
							else{
								logDebug("Message NOt read");
								Spannable myMessage = new SpannableString(lastMessageString);
								myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								CharSequence indexedText = TextUtils.concat(name, myMessage);
								((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);
							}
						}
						else{
							if(chat.getUnreadCount()==0){
								logDebug("Message READ");
								((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
							}
							else{
								logDebug("Message NOT read");
								((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
							}

							((ViewHolderNormalChatList)holder).textViewContent.setText(lastMessageString);
						}
					}
				}
				else if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID) {
					logWarning("Invalid meta message");
					long lastMsgSender = chat.getLastMessageSender();
					if(lastMsgSender==megaChatApi.getMyUserHandle()){

						logDebug("The last message is mine: " + lastMsgSender);
						Spannable me = new SpannableString(context.getString(R.string.word_me)+" ");
						me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_first_row)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

						Spannable myMessage = new SpannableString(context.getString(R.string.error_meta_message_invalid));
						myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						CharSequence indexedText = TextUtils.concat(me, myMessage);
						((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
						((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);
					}
					else{
						logDebug("The last message NOT mine: " + lastMsgSender);

						if(chat.isGroup()){
							MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());

							((ViewHolderNormalChatList)holder).currentPosition = position;
							((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;

							String fullNameAction = "";
							if(chatRoom!=null){
								fullNameAction = converterShortCodes(getMessageSenderName(chatRoom, chat.getChatId(), lastMsgSender));
								if(fullNameAction==null){
									fullNameAction = "";
								}

								if(fullNameAction.trim().length()<=0){
									fullNameAction = cC.getFirstName(lastMsgSender, chatRoom);
								}
							}
							else{
								logWarning("ERROR: the chatroom is NULL: " + chat.getChatId());
							}

							if(fullNameAction.trim().length()<=0){

//					megaChatApi.getUserFirstname();
								if(fullNameAction.isEmpty()){
									if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
										logDebug("Call for nonContactName: " + lastMsgSender);
										fullNameAction = context.getString(R.string.unknown_name_label);
										((ViewHolderNormalChatList)holder).nameRequestedAction=true;
										((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;
										ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chatRoom.isPreview());
										megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
										megaChatApi.getUserEmail(lastMsgSender, listener);
									}
									else{
										logWarning("Name already asked and no name received: " + lastMsgSender);
									}
								}
							}

							Spannable name = new SpannableString(fullNameAction+": ");
							name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

							if(chat.getUnreadCount()==0){
								logDebug("Message READ");
								Spannable myMessage = new SpannableString(context.getString(R.string.error_meta_message_invalid));
								myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								CharSequence indexedText = TextUtils.concat(name, myMessage);
								((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);
							}
							else{
								logDebug("Message NOT read");
								Spannable myMessage = new SpannableString(context.getString(R.string.error_meta_message_invalid));
								myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								CharSequence indexedText = TextUtils.concat(name, myMessage);
								((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);
							}
						}
						else{
							if(chat.getUnreadCount()==0){
								logDebug("Message READ");
								((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
							}
							else{
								logDebug("Message NOT read");
								((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
							}

							((ViewHolderNormalChatList)holder).textViewContent.setText(context.getString(R.string.error_meta_message_invalid));
						}
					}
				}
			} else if (messageType == MegaChatMessage.TYPE_CONTACT_ATTACHMENT && lastMessage != null && lastMessage.getUsersCount() > 1) {
				long contactsCount = lastMessage.getUsersCount();
				String contactAttachmentMessage = converterShortCodes(context.getString(R.string.contacts_sent, String.valueOf(contactsCount)));
				Spannable myMessage = new SpannableString(contactAttachmentMessage);

				if (chat.getLastMessageSender() == megaChatApi.getMyUserHandle()) {
					Spannable me = new SpannableString(context.getString(R.string.word_me) + " ");
					me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_first_row)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					CharSequence indexedText = TextUtils.concat(me, myMessage);
					((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
					((ViewHolderNormalChatList) holder).textViewContent.setText(indexedText);
				} else if (chat.isGroup()) {
					MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
					long lastMsgSender = chat.getLastMessageSender();

					((ViewHolderNormalChatList) holder).currentPosition = position;
					((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

					String fullNameAction = "";
					if (chatRoom != null) {
						fullNameAction = converterShortCodes(getMessageSenderName(chatRoom, chat.getChatId(), lastMsgSender));
						if (fullNameAction == null) {
							fullNameAction = "";
						}

						if (fullNameAction.trim().length() <= 0) {
							fullNameAction = cC.getFirstName(lastMsgSender, chatRoom);
						}
					}

					if (fullNameAction.trim().length() <= 0 && fullNameAction.isEmpty() && !(((ViewHolderNormalChatList) holder).nameRequestedAction)) {
						fullNameAction = context.getString(R.string.unknown_name_label);
						((ViewHolderNormalChatList) holder).nameRequestedAction = true;
						((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

						ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chat.isPreview());

						megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
						megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
						megaChatApi.getUserEmail(lastMsgSender, listener);
					}

					Spannable name = new SpannableString(fullNameAction + ": ");
					name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					if (chat.getUnreadCount() == 0) {
						myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					} else {
						myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}

					((ViewHolderNormalChatList) holder).textViewContent.setText(TextUtils.concat(name, myMessage));
				} else {
					if (chat.getUnreadCount() == 0) {
						((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
					} else {
						((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
					}

					((ViewHolderNormalChatList) holder).textViewContent.setText(contactAttachmentMessage);
				}
			}
			else{
				//OTHER TYPE OF MESSAGE
				if(lastMessageString==null){
					logWarning("Message Type-> " + messageType + " last content is NULL ");
					lastMessageString = context.getString(R.string.error_message_unrecognizable);
				}
				else if(messageType==MegaChatMessage.TYPE_VOICE_CLIP){
					lastMessageString = "";
					((ViewHolderNormalChatList)holder).voiceClipOrLocationLayout.setVisibility(View.VISIBLE);
					long idLastMessage = chat.getLastMessageId();
					long idChat = chat.getChatId();
					MegaChatMessage m = megaChatApi.getMessage(idChat, idLastMessage);
					if (m == null || m.getMegaNodeList() == null || m.getMegaNodeList().size() < 1 || !isVoiceClip(m.getMegaNodeList().get(0).getName())) {
						((ViewHolderNormalChatList) holder).voiceClipOrLocationText.setText("--:--");
					} else {
						long duration = getVoiceClipDuration(m.getMegaNodeList().get(0));
						((ViewHolderNormalChatList) holder).voiceClipOrLocationText.setText(milliSecondsToTimer(duration));
					}

				}

				long lastMsgSender = chat.getLastMessageSender();

				if(lastMsgSender==megaChatApi.getMyUserHandle()){

					logDebug("The last message is mine: " + lastMsgSender);
					Spannable me = new SpannableString(context.getString(R.string.word_me)+" ");
					me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_first_row)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					if(lastMessageString!=null) {
						SimpleSpanBuilder ssb = formatText(context, lastMessageString);
						Spannable myMessage;
						if(ssb == null){
							myMessage = new SpannableString(lastMessageString);
						}else{
							myMessage = ssb.build();
						}
						myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						CharSequence indexedText = TextUtils.concat(me, myMessage);
						((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);

						((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
						setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_mic_on_small, true);
					}
				}
				else{
					logDebug("The last message NOT mine: " + lastMsgSender);

					if(chat.isGroup()){
						MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());

						((ViewHolderNormalChatList)holder).currentPosition = position;
						((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;

						String fullNameAction = "";
						if(chatRoom!=null){
							fullNameAction = converterShortCodes(getMessageSenderName(chatRoom, chat.getChatId(), lastMsgSender));
							if(fullNameAction==null){
								fullNameAction = "";
							}

							if(fullNameAction.trim().length()<=0){
								fullNameAction = cC.getFirstName(lastMsgSender, chatRoom);
							}
						}
						else{
							logWarning("ERROR: the chatroom is NULL: " + chat.getChatId());
						}

						if(fullNameAction.trim().length()<=0){

//					megaChatApi.getUserFirstname();
							if(fullNameAction.isEmpty()){
								if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
									logDebug("Call for nonContactHandle: " + lastMsgSender);
									fullNameAction = context.getString(R.string.unknown_name_label);
									((ViewHolderNormalChatList)holder).nameRequestedAction=true;
									((ViewHolderNormalChatList)holder).userHandle = lastMsgSender;

									ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chat.isPreview());

									megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
									megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
									megaChatApi.getUserEmail(lastMsgSender, listener);
								}
								else{
									logWarning("Name already asked and no name received: handle " + lastMsgSender);
								}
							}
						}

						Spannable name = new SpannableString(fullNameAction + ": ");
						name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						SimpleSpanBuilder ssb = formatText(context, lastMessageString);
						Spannable contactMessage;
						if (ssb == null) {
							contactMessage = new SpannableString(lastMessageString);
						} else {
							contactMessage = ssb.build();
						}
						boolean isRead;
						if(chat.getUnreadCount()==0){
							logDebug("Message READ");
							contactMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.file_list_second_row)), 0, contactMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
							isRead = true;
						}
						else{
							logDebug("Message NOT read");
							contactMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, contactMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
							isRead = false;
						}
						CharSequence indexedText = TextUtils.concat(name, contactMessage);
						((ViewHolderNormalChatList)holder).textViewContent.setText(indexedText);
						setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_mic_on_small, isRead);

					}
					else{
						if(chat.getUnreadCount()==0){
							logDebug("Message READ");
							((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
							setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_mic_on_small, true);
						}
						else{
							logDebug("Message NOT read");
							((ViewHolderNormalChatList)holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
							setVoiceClipOrLocationLayout(((ViewHolderNormalChatList)holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList)holder).voiceClipOrLocationText, R.drawable.ic_mic_on_small, false);
						}

						SimpleSpanBuilder ssb = formatText(context, lastMessageString);
						if (ssb == null) {
							((ViewHolderNormalChatList) holder).textViewContent.setText(lastMessageString);
						} else {
							((ViewHolderNormalChatList) holder).textViewContent.setText(ssb.build(), TextView.BufferType.SPANNABLE);
						}

					}
				}
			}
		}
		else{
			logWarning("Holder is NULL: " + position);
			notifyItemChanged(position);
		}
	}

	private void setVoiceClipOrLocationLayout(ImageView image, TextView text, int resource, boolean isRead) {
		if (isRead) {
			image.setImageDrawable(mutateIconSecondary(context, resource, R.color.ic_mic_read_message));
			text.setTextColor(ContextCompat.getColor(context, R.color.ic_mic_read_message));
		}
		else {
			image.setImageDrawable(mutateIconSecondary(context, resource, R.color.ic_mic_unread_message));
			text.setTextColor(ContextCompat.getColor(context, R.color.ic_mic_unread_message));
		}
	}

	public String getFullNameAction(MegaChatListItem chat){
		String fullNameAction = "";
		if(chat.getLastMessageSender() == megaChatApi.getMyUserHandle()){
			fullNameAction = megaChatApi.getMyFullname();
			if(fullNameAction==null){
				fullNameAction = "";
			}
			if(fullNameAction.trim().length()<=0){
				fullNameAction = megaChatApi.getMyEmail();
			}
		}
		else{
			MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
			fullNameAction = getMessageSenderName(chatRoom, chat.getChatId(), chat.getLastMessageSender());
			if(fullNameAction.trim().length()<=0){
				if(fullNameAction.isEmpty()){
					if(!(((ViewHolderNormalChatList)holder).nameRequestedAction)){
						logDebug("Call for nonContactHandle: " + chat.getLastMessageSender());
						fullNameAction = context.getString(R.string.unknown_name_label);
						((ViewHolderNormalChatList)holder).nameRequestedAction=true;
						((ViewHolderNormalChatList)holder).userHandle = chat.getLastMessageSender();
						ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());
						megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
						megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
						megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
					}
					else{
						logWarning("Name already asked and no name received: " + chat.getLastMessageSender());
					}
				}
			}
		}
		return fullNameAction;
	}

	public void setChats (ArrayList<MegaChatListItem> updatedChats){
		logDebug("Number of updated chats: "+ updatedChats.size());
		this.chats = updatedChats;

		positionClicked = -1;

		if(listFragment!=null){
            listFragment.invalidate();
        }

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

	public void updateMultiselectionPosition(int oldPosition){
		logDebug("oldPosition: " + oldPosition);

		List<Integer> selected = getSelectedItems();
		boolean movedSelected = false;

		if(isItemChecked(oldPosition)){
			movedSelected=true;
		}

		selectedItems.clear();

		if(movedSelected){
			selectedItems.put(0, true);
		}

		for(int i=0;i<selected.size();i++){
			int pos = selected.get(i);
			if(pos!=oldPosition){
				if(pos<oldPosition){
					selectedItems.put(pos+1, true);
				}
				else{
					selectedItems.put(pos, true);
				}
			}

//			notifyItemChanged(pos);
//			notifyItemChanged(pos+1);
		}
	}

	@Override
	public String getSectionTitle(int position) {
		if(holder instanceof ViewHolderNormalChatList){
			if(((ViewHolderNormalChatList)holder).textFastScroller.isEmpty()){
				return null;
			}else{
				return ((ViewHolderNormalChatList)holder).textFastScroller;
			}
		}
		else{
			return null;
		}
	}

	public void modifyChat(ArrayList<MegaChatListItem> chats, int position){
		this.chats = chats;
		notifyItemChanged(position);
	}

	public void removeChat(ArrayList<MegaChatListItem> chats, int position){
		this.chats = chats;
		notifyItemRemoved(position);
	}
}