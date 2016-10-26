package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
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
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.RecentChatsFragmentLollipop;
import mega.privacy.android.app.lollipop.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ChatPreferences;
import mega.privacy.android.app.lollipop.tempMegaChatClasses.Message;
import mega.privacy.android.app.utils.Constants;

import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;


public class MegaListChatLollipopAdapter extends RecyclerView.Adapter<MegaListChatLollipopAdapter.ViewHolderChatList> implements OnClickListener {

	static public int ADAPTER_RECENT_CHATS = 0;
	static public int ADAPTER_ARCHIVED_CHATS = ADAPTER_RECENT_CHATS+1;

	Context context;
	int positionClicked;
	ArrayList<MegaChatRoom> chats;
	RecyclerView listFragment;
	MegaApiAndroid megaApi;
	boolean multipleSelect;
	private SparseBooleanArray selectedItems;
	Object fragment;

	DisplayMetrics outMetrics;

	DatabaseHandler dbH = null;
	ChatPreferences chatPrefs = null;

	int adapterType;

	public MegaListChatLollipopAdapter(Context _context, Object _fragment, ArrayList<MegaChatRoom> _chats, RecyclerView _listView, int type) {
		log("new adapter");
		this.context = _context;
		this.chats = _chats;
		this.positionClicked = -1;
		this.fragment = _fragment;
		this.adapterType = type;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		listFragment = _listView;
		
		if(chats!=null)
    	{
    		log("Number of chats: "+chats.size());
    	}
    	else{
    		log("Number of chats: NULL");
    	}
	}
	
	/*private view holder class*/
    public class ViewHolderChatList extends ViewHolder{
    	public ViewHolderChatList(View arg0) {
			super(arg0);
			// TODO Auto-generated constructor stub
		}
    	RoundedImageView imageView;
    	TextView contactInitialLetter;
//        ImageView imageView;
        TextView textViewContactName;
        TextView textViewContent;
		TextView textViewDate;
        ImageButton imageButtonThreeDots;
        RelativeLayout itemLayout;
		RoundedImageView circlePendingMessages;
		TextView numberPendingMessages;
		RelativeLayout layoutPendingMessages;
        ImageView muteIcon;
		ImageView multiselectIcon;
		ImageView contactStateIcon;
        int currentPosition;
        String contactMail;
		String lastNameText="";
		String firstNameText="";
		String fullName = "";

		public String getContactMail (){
			return contactMail;
		}

		public void setImageView(Bitmap bitmap){
			imageView.setImageBitmap(bitmap);
			contactInitialLetter.setVisibility(View.GONE);
		}

		public void setContactName(String contactName){
			textViewContactName.setText(contactName);
		}

		public void setContactInitialLetter(String initialLetter){
			contactInitialLetter.setText(initialLetter);
		}

		public void setFirstNameText(String firstName){
			firstNameText=firstName;
		}

		public void setLastNameText(String lastName){
			lastNameText = lastName;
		}
    }
    ViewHolderChatList holder;
    
	@Override
	public void onBindViewHolder(ViewHolderChatList holder, int position) {

		holder.currentPosition = position;
		holder.imageView.setImageBitmap(null);
		holder.contactInitialLetter.setText("");
		
		log("Get the ChatRoom: "+position);
		MegaChatRoom chat = (MegaChatRoom) getItem(position);
		log("ChatRoom handle: "+chat.getChatId());

		long numberContacts = chat.getPeerCount();
		if(!chat.isGroup()){
			log("Chat one to one");
			long contactHandle = chat.getPeerHandle(0);
			String userHandleEncoded = MegaApiAndroid.userHandleToBase64(contactHandle);
			MegaUser user = megaApi.getContact(userHandleEncoded);
			if(user!=null){
				log("El email del user es: "+user.getEmail());
			}
			else{
				log("El user es NULL");
			}
			holder.contactMail = user.getEmail();
			MegaContact contactDB = dbH.findContactByHandle(String.valueOf(contactHandle));
//				MegaContact contactDB = dbH.findContactByHandle("6135453135");
			if(contactDB!=null){
				holder.firstNameText = contactDB.getName();
				holder.lastNameText = contactDB.getLastName();

				if (holder.firstNameText.trim().length() <= 0){
					holder.fullName = holder.lastNameText;
				}
				else{
					holder.fullName = holder.firstNameText + " " + holder.lastNameText;
				}

				if (holder.fullName.trim().length() <= 0){
					log("Put email as fullname");
					String email = holder.contactMail;
					String[] splitEmail = email.split("[@._]");
					holder.fullName = splitEmail[0];
				}

//				holder.textViewContactName.setText(holder.fullName);
			}
			else{
				String email = holder.contactMail;
				String[] splitEmail = email.split("[@._]");
				holder.fullName = splitEmail[0];
//				holder.textViewContactName.setText(holder.fullName);
			}

			log("ChatRoom title: "+chat.getTitle());
			holder.textViewContactName.setText(chat.getTitle());

			if (!multipleSelect) {
				//Multiselect OFF
				holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
				if (positionClicked != -1){
					if (positionClicked == position){
						holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
						listFragment.smoothScrollToPosition(positionClicked);
					}
					else{
						holder.itemLayout.setBackgroundColor(Color.WHITE);
					}
				}
				else{
					holder.itemLayout.setBackgroundColor(Color.WHITE);
				}
				holder.multiselectIcon.setVisibility(View.GONE);
				setUserAvatar(holder, user);
			} else {
				log("Multiselect ON");

				if(this.isItemChecked(position)){
//					holder.imageButtonThreeDots.setVisibility(View.GONE);
					holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
					holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
					createMultiselectTick(holder);
				}
				else{
					log("NOT selected");
					holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
					holder.itemLayout.setBackgroundColor(Color.WHITE);
					holder.multiselectIcon.setVisibility(View.GONE);
					setUserAvatar(holder, user);
				}
			}
			holder.contactStateIcon.setVisibility(View.VISIBLE);

			holder.contactStateIcon.setMaxWidth(Util.scaleWidthPx(6,outMetrics));
			holder.contactStateIcon.setMaxHeight(Util.scaleHeightPx(6,outMetrics));

			RelativeLayout.LayoutParams stateIconParams = (RelativeLayout.LayoutParams)holder.contactStateIcon.getLayoutParams();
			stateIconParams.setMargins(Util.scaleWidthPx(6, outMetrics), Util.scaleHeightPx(4, outMetrics), 0, 0);
			holder.contactStateIcon.setLayoutParams(stateIconParams);

			int state = chat.getOnlineStatus();
			if(state == MegaChatApi.STATUS_ONLINE){
				log("This user is connected: "+chat.getTitle());
				holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_connected));
			}
			else{
				log("This user status is: "+state+  " " + chat.getTitle());
				holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_not_connected));
			}
		}
		else{
			log("Group chat");
			holder.contactStateIcon.setVisibility(View.GONE);
			log("ChatRoom title: "+chat.getTitle());
			holder.textViewContactName.setText(chat.getTitle());
		}

		int unreadMessages = chat.getUnreadCount();
		log("Unread messages: "+unreadMessages);
		if(chat.getUnreadCount()!=0){
			setPendingMessages(unreadMessages, holder);
		}
		else{
			holder.layoutPendingMessages.setVisibility(View.GONE);
		}

		log("The last message is text!");
		log("The last message is mine");
		Spannable me = new SpannableString("Me: ");
		me.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.file_list_first_row)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		holder.textViewContent.setText(me);
		Spannable myMessage = new SpannableString("Contenido del mensaje");
		myMessage.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		holder.textViewContent.append(myMessage);

		holder.textViewDate.setText(TimeChatUtils.formatDate(1472123750L, TimeChatUtils.DATE_LONG_FORMAT));

		/*

		if(messages!=null){
			Message lastMessage = messages.get(messages.size()-1);

			if(lastMessage.getType()==Message.TEXT){
				log("The last message is text!");

				//Set margin contentTextView - more margin bottom duration
				RelativeLayout.LayoutParams contentTextViewParams = (RelativeLayout.LayoutParams)holder.textViewContent.getLayoutParams();
				contentTextViewParams.setMargins(Util.scaleWidthPx(13, outMetrics), 0, Util.scaleWidthPx(65, outMetrics), 2);
				holder.textViewContent.setLayoutParams(contentTextViewParams);

				String myMail = ((ManagerActivityLollipop) context).getMyAccountInfo().getMyUser().getEmail();
				if(lastMessage.getUser().getMail().equals(myMail)){
					log("The last message is mine");
					Spannable me = new SpannableString("Me: ");
					me.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.file_list_first_row)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					holder.textViewContent.setText(me);
					Spannable myMessage = new SpannableString(lastMessage.getMessage());
					myMessage.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.file_list_second_row)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					holder.textViewContent.append(myMessage);
				}
				else{
					log("The last message NOT mine");
					if(lastMessage.isRead()){
						log("Message READ");
						holder.textViewContent.setTextColor(context.getResources().getColor(R.color.file_list_second_row));
					}
					else{
						log("Message NOt read");
						holder.textViewContent.setTextColor(context.getResources().getColor(R.color.accentColor));
					}
					holder.textViewContent.setText(lastMessage.getMessage());
				}
			}
			else if(lastMessage.getType()==Message.VIDEO){

				//The last message is a call
				log("The last message is a call!");

				//Set margin contentTextView - more margin bottom duration
				RelativeLayout.LayoutParams contentTextViewParams = (RelativeLayout.LayoutParams)holder.textViewContent.getLayoutParams();
				contentTextViewParams.setMargins(Util.scaleWidthPx(13, outMetrics), 0, Util.scaleWidthPx(65, outMetrics), Util.scaleHeightPx(2, outMetrics));
				holder.textViewContent.setLayoutParams(contentTextViewParams);

//				String videoCallString = context.getResources().getString(R.string.videocall_item);
				Spannable videoCall = new SpannableString("Video call");
				videoCall.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context,R.color.file_list_first_row)), 0, videoCall.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				holder.textViewContent.setText(videoCall);
				int duration = (int) lastMessage.getDuration();
				String s = formatStringDuration(duration);
				Spannable durationString = new SpannableString(s);
				durationString.setSpan(new RelativeSizeSpan(0.85f), 0, durationString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				durationString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context,R.color.file_list_second_row)), 0, durationString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				holder.textViewContent.append(durationString);
			}


			if(chat.isMute()){
				holder.muteIcon.setVisibility(View.VISIBLE);
				RelativeLayout.LayoutParams muteIconParams = (RelativeLayout.LayoutParams)holder.muteIcon.getLayoutParams();
				muteIconParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(14, outMetrics), 0, 0);
				holder.muteIcon.setLayoutParams(muteIconParams);

				//Set margin
				RelativeLayout.LayoutParams nameTextViewParams = (RelativeLayout.LayoutParams)holder.textViewContactName.getLayoutParams();
				nameTextViewParams.setMargins(0, Util.scaleHeightPx(12, outMetrics), 0, 0);
				holder.textViewContactName.setLayoutParams(nameTextViewParams);
			}
			else{
				holder.muteIcon.setVisibility(View.GONE);
				//Set margin
				RelativeLayout.LayoutParams nameTextViewParams = (RelativeLayout.LayoutParams)holder.textViewContactName.getLayoutParams();
				nameTextViewParams.setMargins(Util.scaleWidthPx(13, outMetrics), Util.scaleHeightPx(12, outMetrics), 0, 0);
				holder.textViewContactName.setLayoutParams(nameTextViewParams);
			}

			holder.textViewDate.setText(TimeChatUtils.formatDate(lastMessage, TimeChatUtils.DATE_LONG_FORMAT));
		}*/

		chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chat.getChatId()));
		if(chatPrefs!=null) {
			log("Chat prefs exists!!!");
			boolean notificationsEnabled = true;
			if (chatPrefs.getNotificationsEnabled() != null) {
				notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
			}

			if (!notificationsEnabled) {
				log("Chat is MUTE");
				holder.muteIcon.setVisibility(View.VISIBLE);
				RelativeLayout.LayoutParams muteIconParams = (RelativeLayout.LayoutParams)holder.muteIcon.getLayoutParams();
				muteIconParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(14, outMetrics), 0, 0);
				holder.muteIcon.setLayoutParams(muteIconParams);
			}
			else{
				log("Chat with notifications enabled!!");
				holder.muteIcon.setVisibility(View.GONE);
			}
		}
		else{
			log("Chat prefs is NULL");
			holder.muteIcon.setVisibility(View.GONE);
		}

		holder.imageButtonThreeDots.setTag(holder);
		holder.imageButtonThreeDots.setOnClickListener(this);		
	}

	public void setUserAvatar(ViewHolderChatList holder, MegaUser contact){
		log("setUserAvatar");

		createDefaultAvatar(holder);

		ChatUserAvatarListener listener = new ChatUserAvatarListener(context, holder, this);

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

			log("The duration is: " + hours + " " + minutes + " " + seconds);

			return timeString;
		}
		return "0";
	}

	@Override
	public ViewHolderChatList onCreateViewHolder(ViewGroup parent, int viewType) {
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);

		dbH = DatabaseHandler.getDbHandler(context);

		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat_list, parent, false);
		holder = new ViewHolderChatList(v);
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.recent_chat_list_item_layout);
		holder.muteIcon = (ImageView) v.findViewById(R.id.recent_chat_list_call_icon);
		holder.multiselectIcon = (ImageView) v.findViewById(R.id.recent_chat_list_multiselect_icon);
		holder.imageView = (RoundedImageView) v.findViewById(R.id.recent_chat_list_thumbnail);
		holder.contactInitialLetter = (TextView) v.findViewById(R.id.recent_chat_list_initial_letter);
		holder.textViewContactName = (TextView) v.findViewById(R.id.recent_chat_list_name);
		//Set margin
		RelativeLayout.LayoutParams nameTextViewParams = (RelativeLayout.LayoutParams)holder.textViewContactName.getLayoutParams();
		nameTextViewParams.setMargins(Util.scaleWidthPx(13, outMetrics), Util.scaleHeightPx(12, outMetrics), 0, 0);
		holder.textViewContactName.setLayoutParams(nameTextViewParams);

		holder.textViewContent = (TextView) v.findViewById(R.id.recent_chat_list_content);
		holder.textViewDate = (TextView) v.findViewById(R.id.recent_chat_list_date);
		holder.imageButtonThreeDots = (ImageButton) v.findViewById(R.id.recent_chat_list_three_dots);
		//Right margin
		RelativeLayout.LayoutParams actionButtonParams = (RelativeLayout.LayoutParams)holder.imageButtonThreeDots.getLayoutParams();
		actionButtonParams.setMargins(0, 0, Util.scaleWidthPx(8, outMetrics), 0);
		holder.imageButtonThreeDots.setLayoutParams(actionButtonParams);

		holder.layoutPendingMessages = (RelativeLayout) v.findViewById(R.id.recent_chat_list_unread_layout);
		holder.circlePendingMessages = (RoundedImageView) v.findViewById(R.id.recent_chat_list_unread_circle);
		holder.numberPendingMessages = (TextView) v.findViewById(R.id.recent_chat_list_unread_number);

		holder.contactStateIcon = (ImageView) v.findViewById(R.id.recent_chat_list_contact_state);

		holder.itemLayout.setOnClickListener(this);

		v.setTag(holder);

		return holder;
	}

	public void setPendingMessages(int unreadMessages, ViewHolderChatList holder){
		log("setPendingMessages: "+unreadMessages);

		Bitmap circle = Bitmap.createBitmap(150,150, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(circle);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(context.getResources().getColor(R.color.accentColor));

		int radius;
		if (circle.getWidth() < circle.getHeight())
			radius = circle.getWidth()/2;
		else
			radius = circle.getHeight()/2;

		c.drawCircle(circle.getWidth()/2, circle.getHeight()/2, radius, p);
		holder.circlePendingMessages.setImageBitmap(circle);

		holder.layoutPendingMessages.setVisibility(View.VISIBLE);
		holder.numberPendingMessages.setText(unreadMessages+"");
	}

	public void createMultiselectTick (ViewHolderChatList holder){
		log("createMultiselectTick");

		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(context.getResources().getColor(R.color.grey_info_menu));

		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		holder.imageView.setImageBitmap(defaultAvatar);

		holder.contactInitialLetter.setVisibility(View.GONE);
		holder.multiselectIcon.setVisibility(View.VISIBLE);
	}
	
	public void createDefaultAvatar(ViewHolderChatList holder){
		log("createDefaultAvatar()");
		
		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint p = new Paint();
		p.setAntiAlias(true);

		MegaUser contact = megaApi.getContact(holder.contactMail);
		if(contact!=null){
			String color = megaApi.getUserAvatarColor(contact);
			if(color!=null){
				log("The color to set the avatar is "+color);
				p.setColor(Color.parseColor(color));
			}
			else{
				log("Default color to the avatar");
				p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
			}
		}
		else{
			log("Contact is NULL");
			p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
		}

		int radius; 
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
        	radius = defaultAvatar.getWidth()/2;
        else
        	radius = defaultAvatar.getHeight()/2;
        
		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
		holder.imageView.setImageBitmap(defaultAvatar);
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = context.getResources().getDisplayMetrics().density;

		int avatarTextSize = getAvatarTextSize(density);
		log("DENSITY: " + density + ":::: " + avatarTextSize);
		boolean setInitialByMail = false;

		if (holder.fullName != null){
			if (holder.fullName.trim().length() > 0){
				String firstLetter = holder.fullName.charAt(0) + "";
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
		holder.contactInitialLetter.setTextSize(24);
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
        return chats.size();
    }
 
	public boolean isMultipleSelect() {
		log("isMultipleSelect");
		return multipleSelect;
	}
	
	public void setMultipleSelect(boolean multipleSelect) {
		log("setMultipleSelect");
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
		ViewHolderChatList view = (ViewHolderChatList) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			log("Start animation: "+view.contactMail);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			view.imageView.startAnimation(flipAnimation);
		}

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
//		notifyDataSetChanged();
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
	 * Get request at specified position
	 */
	public MegaChatRoom getChatAt(int position) {
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
	public List<MegaChatRoom> getSelectedChats() {
		ArrayList<MegaChatRoom> chats = new ArrayList<MegaChatRoom>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaChatRoom r = getChatAt(selectedItems.keyAt(i));
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
		log("setPositionClicked: "+p);
    	positionClicked = p;
		notifyDataSetChanged();
    }
    
	@Override
	public void onClick(View v) {
		ViewHolderChatList holder = (ViewHolderChatList) v.getTag();
		int currentPosition = holder.currentPosition;
		MegaChatRoom c = (MegaChatRoom) getItem(currentPosition);

		switch (v.getId()){	
			case R.id.recent_chat_list_three_dots:{
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
				log("click three dots!");
				((ManagerActivityLollipop) context).showChatPanel(c);
				break;
			}
			case R.id.recent_chat_list_item_layout:{
				log("click layout!");
//				if(multipleSelect){
//					toggleSelection(holder);
//				}

				((RecentChatsFragmentLollipop) fragment).itemClick(currentPosition);

				break;
			}
		}
	}
	
	public void setChats (ArrayList<MegaChatRoom> chats){
		log("SETCONTACTS!!!!");
		this.chats = chats;
		if(chats!=null)
		{
			log("num requests: "+chats.size());
		}
		positionClicked = -1;
//		listFragment.invalidate();
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
		Util.log("MegaListChatLollipopAdapter", log);
	}
}