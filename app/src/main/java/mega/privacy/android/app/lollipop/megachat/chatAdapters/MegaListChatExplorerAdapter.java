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
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerListItem;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;

public class MegaListChatExplorerAdapter extends RecyclerView.Adapter<MegaListChatExplorerAdapter.ViewHolderChatExplorerList> implements View.OnClickListener, View.OnLongClickListener, SectionTitleProvider {

    DisplayMetrics outMetrics;
    
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    ChatController cC;
    DatabaseHandler dbH = null;

    ViewHolderChatExplorerList holder;

    Context context;
    ArrayList<ChatExplorerListItem> items;
    Object fragment;

    public MegaListChatExplorerAdapter(Context _context, Object _fragment, ArrayList<ChatExplorerListItem> _items) {
        log("new adapter");
        this.context = _context;
        this.items = _items;
        this.fragment = _fragment;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        cC = new ChatController(context);
    }

    public static class ViewHolderChatExplorerList extends RecyclerView.ViewHolder {
        public ViewHolderChatExplorerList(View arg0) {
            super(arg0);
        }

        RelativeLayout itemLayout;
        RoundedImageView avatarImage;
        TextView initialLetter;
        EmojiTextView titleText;
        ImageView stateIcon;
        MarqueeTextView lastSeenStateText;
        EmojiTextView participantsText;
    }

    @Override
    public MegaListChatExplorerAdapter.ViewHolderChatExplorerList onCreateViewHolder(ViewGroup parent, int viewType) {

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        dbH = DatabaseHandler.getDbHandler(context);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_explorer_list, parent, false);
        holder = new ViewHolderChatExplorerList(v);

        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.chat_explorer_list_item_layout);
        holder.avatarImage = (RoundedImageView) v.findViewById(R.id.chat_explorer_list_avatar);
        holder.initialLetter = (TextView) v.findViewById(R.id.chat_explorer_list_initial_letter);
        holder.titleText = (EmojiTextView) v.findViewById(R.id.chat_explorer_list_title);
        holder.stateIcon = (ImageView) v.findViewById(R.id.chat_explorer_list_contact_state);
        holder.lastSeenStateText = (MarqueeTextView) v.findViewById(R.id.chat_explorer_list_last_seen_state);
        holder.participantsText = (EmojiTextView) v.findViewById(R.id.chat_explorer_list_participants);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            holder.titleText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
            holder.participantsText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
        }
        else{
            holder.titleText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
            holder.participantsText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
        }

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(MegaListChatExplorerAdapter.ViewHolderChatExplorerList holder, int position) {

        ChatExplorerListItem item = getItem(position);

        holder.titleText.setText(item.getName());

        if (item.getChat() != null && item.getChat().isGroup()) {
            if (item.getName().length() > 0){
                String chatTitle = item.getName().trim();
                String firstLetter = chatTitle.charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                holder.initialLetter.setText(firstLetter);
            }
            createGroupChatAvatar(holder);
            holder.stateIcon.setVisibility(View.GONE);
            holder.lastSeenStateText.setVisibility(View.GONE);
            MegaChatRoom chatRoom = megaChatApi.getChatRoom(item.getChat().getChatId());
            long peerCount = chatRoom.getPeerCount();
            holder.participantsText.setText(context.getResources().getQuantityString(R.plurals.subtitle_of_group_chat, (int) peerCount, peerCount));
        }
        else {            
            long handle = -1;
            MegaContactAdapter contact = null;
            String email = null;
            if (item.getContact() != null) {
                contact = item.getContact();
                if (contact.getMegaUser() != null) {
                    handle = contact.getMegaUser().getHandle();
                    email = contact.getMegaUser().getEmail();
                }
            }
            String userHandleEncoded = MegaApiAndroid.userHandleToBase64(handle);
            setUserAvatar(holder, userHandleEncoded, email);
            
            if(Util.isChatEnabled()){
                if (megaChatApi != null){
                    int userStatus = megaChatApi.getUserOnlineStatus(handle);
                    if(userStatus == MegaChatApi.STATUS_ONLINE){
                        log("This user is connected");
                        holder.stateIcon.setVisibility(View.VISIBLE);
                        holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
                        holder.lastSeenStateText.setText(context.getString(R.string.online_status));
                        holder.lastSeenStateText.setVisibility(View.VISIBLE);
                    }
                    else if(userStatus == MegaChatApi.STATUS_AWAY){
                        log("This user is away");
                        holder.stateIcon.setVisibility(View.VISIBLE);
                        holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
                        holder.lastSeenStateText.setText(context.getString(R.string.away_status));
                        holder.lastSeenStateText.setVisibility(View.VISIBLE);
                    }
                    else if(userStatus == MegaChatApi.STATUS_BUSY){
                        log("This user is busy");
                        holder.stateIcon.setVisibility(View.VISIBLE);
                        holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
                        holder.lastSeenStateText.setText(context.getString(R.string.busy_status));
                        holder.lastSeenStateText.setVisibility(View.VISIBLE);
                    }
                    else if(userStatus == MegaChatApi.STATUS_OFFLINE){
                        log("This user is offline");
                        holder.stateIcon.setVisibility(View.VISIBLE);
                        holder.stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
                        holder.lastSeenStateText.setText(context.getString(R.string.offline_status));
                        holder.lastSeenStateText.setVisibility(View.VISIBLE);
                    }
                    else if(userStatus == MegaChatApi.STATUS_INVALID){
                        log("INVALID status: "+userStatus);
                        holder.stateIcon.setVisibility(View.GONE);
                        holder.lastSeenStateText.setVisibility(View.GONE);
                    }
                    else{
                        log("This user status is: "+userStatus);
                        holder.stateIcon.setVisibility(View.GONE);
                        holder.lastSeenStateText.setVisibility(View.GONE);
                    }

                    if(userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID){
                        if(!contact.getLastGreen().isEmpty()){
                            holder.lastSeenStateText.setText(contact.getLastGreen());
                            holder.lastSeenStateText.isMarqueeIsNecessary(context);
                        }
                    }
                }
            }
            else{
                holder.stateIcon.setVisibility(View.GONE);
            }

            holder.participantsText.setVisibility(View.GONE);

        }
    }

    public void createGroupChatAvatar(ViewHolderChatExplorerList holder){
        log("createGroupChatAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(ContextCompat.getColor(context,R.color.divider_upgrade_account));

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        holder.avatarImage.setImageBitmap(defaultAvatar);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        String firstLetter = holder.initialLetter.getText().toString();

        if(firstLetter.trim().isEmpty()){
            holder.initialLetter.setVisibility(View.INVISIBLE);
        }
        else{
            log("Group chat initial letter is: "+firstLetter);
            if(firstLetter.equals("(")){
                holder.initialLetter.setVisibility(View.INVISIBLE);
            }
            else{
                holder.initialLetter.setText(firstLetter);
                holder.initialLetter.setTextColor(Color.WHITE);
                holder.initialLetter.setVisibility(View.VISIBLE);
                holder.initialLetter.setTextSize(24);
            }
        }
    }
    
    public void setUserAvatar(ViewHolderChatExplorerList holder, String userHandle, String email){
		log("setUserAvatar ");
		createDefaultAvatar(holder, userHandle, email);

		ChatUserAvatarListener listener = new ChatUserAvatarListener(context, holder, this);
		File avatar = null;

		if(email == null){
			if (context.getExternalCacheDir() != null) {
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(), userHandle + ".jpg");
			}else {
				avatar = new File(context.getCacheDir().getAbsolutePath(), userHandle + ".jpg");
			}
		}else{
			if (context.getExternalCacheDir() != null){
				avatar = new File(context.getExternalCacheDir().getAbsolutePath(), email + ".jpg");
			}else{
				avatar = new File(context.getCacheDir().getAbsolutePath(), email + ".jpg");
			}
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

					if(megaApi==null){
						log("setUserAvatar: megaApi is Null in Offline mode");
						return;
					}

					if (context.getExternalCacheDir() != null){
						megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
					}
					else{
						megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
					}
				}else{
					holder.initialLetter.setVisibility(View.GONE);
					holder.avatarImage.setImageBitmap(bitmap);
				}
			}else{

				if(megaApi==null){
					log("setUserAvatar: megaApi is Null in Offline mode");
					return;
				}

				if (context.getExternalCacheDir() != null){
					megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
				}else{
					megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
				}
			}
		}else{

			if(megaApi==null){
				log("setUserAvatar: megaApi is Null in Offline mode");
				return;
			}

			if (context.getExternalCacheDir() != null){
				megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
			}
			else{
				megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
			}
		}
	}

    public void createDefaultAvatar(ViewHolderChatExplorerList holder, String userHandle, String email){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        String color = megaApi.getUserAvatarColor(userHandle);
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
        holder.avatarImage.setImageBitmap(defaultAvatar);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        boolean setInitialByMail = false;

        String fullName = holder.titleText.getText().toString();

        if (fullName != null){
            if (fullName.trim().length() > 0){
                String firstLetter = fullName.charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                holder.initialLetter.setText(firstLetter);
                holder.initialLetter.setTextColor(Color.WHITE);
                holder.initialLetter.setVisibility(View.VISIBLE);
            }else{
                setInitialByMail=true;
            }
        }
        else{
            setInitialByMail=true;
        }
        if(setInitialByMail){
            if (email != null){
                if (email.length() > 0){
                    log("email TEXT: " + email);
                    log("email TEXT AT 0: " + email.charAt(0));
                    String firstLetter = email.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    holder.initialLetter.setText(firstLetter);
                    holder.initialLetter.setTextColor(Color.WHITE);
                    holder.initialLetter.setVisibility(View.VISIBLE);
                }
            }
        }
        holder.initialLetter.setTextSize(24);
    }

    public ChatExplorerListItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        if (items != null) {
            return items.size();
        }
        return 0;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public String getSectionTitle(int position) {
        if (items != null) {
            if (position >= 0 && position < items.size()) {
                String name = items.get(position).getName();
                if (name != null && !name.isEmpty()) {
                    return ""+name.charAt(0);
                }
            }
        }
        return "";
    }

    public void setItems (ArrayList<ChatExplorerListItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    private static void log(String log) {
        Util.log("MegaListChatExplorerAdapter", log);
    }
}
