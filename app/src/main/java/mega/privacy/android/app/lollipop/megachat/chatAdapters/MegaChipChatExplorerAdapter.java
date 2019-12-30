package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerListItem;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class MegaChipChatExplorerAdapter extends RecyclerView.Adapter<MegaChipChatExplorerAdapter.ViewHolderChips> implements View.OnClickListener{

    ArrayList<ChatExplorerListItem> items;
    private MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    private Context context;
    private Object fragment;

    public MegaChipChatExplorerAdapter (Context _context, Object _fragment, ArrayList<ChatExplorerListItem> _items){
        this.items = _items;
        this.context = _context;
        this.fragment = _fragment;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }
    }


    public static class ViewHolderChips extends RecyclerView.ViewHolder{
        public ViewHolderChips(View itemView) {
            super(itemView);
        }

        EmojiTextView textViewName;
        ImageView deleteIcon;
        RoundedImageView avatar;
        RelativeLayout itemLayout;

        String email;

        public String getEmail() {
            return email;
        }

        public void setAvatar(Bitmap avatar) {
            this.avatar.setImageBitmap(avatar);
        }

    }

    ViewHolderChips holder = null;

    @Override
    public MegaChipChatExplorerAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = v.findViewById(R.id.item_layout_chip);
        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setEmojiSize(Util.px2dp(Constants.EMOJI_SIZE_EXTRA_SMALL, outMetrics));
        holder.textViewName.setMaxWidth(Util.px2dp(60, outMetrics));
        holder.avatar = v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = v.findViewById(R.id.delete_icon_chip);
        holder.deleteIcon.setOnClickListener(this);

        holder.deleteIcon.setTag(holder);

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(MegaChipChatExplorerAdapter.ViewHolderChips holder, int position) {
        logDebug("onBindViewHolderList");

        ChatExplorerListItem item = getItem(position);
        if (item.getChat() != null && item.getChat().isGroup()) {
            holder.textViewName.setText(item.getTitle());
        }
        else {
            String name;
            String [] s;
            if (item.getContact() != null) {
                s = item.getContact().getFullName().split(" ");
                if (s != null && s.length > 0) {
                    name = s[0];
                }
                else {
                    s = item.getTitle().split(" ");
                    if (s!= null && s.length > 0) {
                        name = s[0];
                    }
                    else {
                        name = item.getTitle();
                    }
                }
            }
            else {
                s = item.getTitle().split(" ");
                if (s!= null && s.length > 0) {
                    name = s[0];
                }
                else {
                    name = item.getTitle();
                }
            }
            holder.textViewName.setText(name);
        }
        setUserAvatar(holder, item);
    }

    @Override
    public int getItemCount() {
        if (items == null) return 0;

        return  items.size();
    }

    @Override
    public void onClick(View view) {
        logDebug("onClick");

        MegaChipChatExplorerAdapter.ViewHolderChips holder = (MegaChipChatExplorerAdapter.ViewHolderChips) view.getTag();
        if(holder!=null){
            int currentPosition = holder.getLayoutPosition();
            logDebug("Current position: " + currentPosition);

            if(currentPosition<0){
                logWarning("Current position error - not valid value");
                return;
            }
            switch (view.getId()) {
                case R.id.delete_icon_chip: {
                    ((ChatExplorerFragment) fragment).deleteItemPosition(currentPosition);
                    break;
                }
            }
        }
        else{
            logWarning("Error. Holder is Null");
        }
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setItems (ArrayList<ChatExplorerListItem> items){
        logDebug("setContacts");
        this.items = items;
        notifyDataSetChanged();
    }

    public ChatExplorerListItem getItem(int position) {
        logDebug("position: " + position);
        return items.get(position);
    }

    public ArrayList<ChatExplorerListItem> getItems () {
        return items;
    }

    public void setUserAvatar(ViewHolderChips holder, ChatExplorerListItem item){
        logDebug("setUserAvatar");

        if (item.getChat() != null && item.getChat().isGroup()) {
            createGroupChatAvatar(holder, item);
        }
        else {
            createDefaultAvatar(holder, item);

            ChatUserAvatarListener listener = new ChatUserAvatarListener(context, holder);
            File avatar = null;

            long handle = -1;
            if (item.getChat() != null) {
                holder.email = megaChatApi.getContactEmail(item.getChat().getPeerHandle());
            }
            else if (item.getContact() != null && item.getContact().getMegaUser() != null) {
                holder.email = item.getContact().getMegaUser().getEmail();
            }

            if (item.getContact() != null && item.getContact().getMegaUser() != null) {
                handle = item.getContact().getMegaUser().getHandle();
            }
            String userHandle = MegaApiAndroid.userHandleToBase64(handle);

            if(holder.email == null){
                avatar = buildAvatarFile(context,userHandle + ".jpg");
            }else{
                avatar = buildAvatarFile(context,holder.email + ".jpg");
            }

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

                        megaApi.getUserAvatar(holder.email, buildAvatarFile(context, holder.email + ".jpg").getAbsolutePath(), listener);
                    }else{
                        holder.avatar.setImageBitmap(getCircleBitmap(bitmap));
                    }
                }else{

                    if(megaApi==null){
                        logWarning("megaApi is Null in Offline mode");
                        return;
                    }

                    megaApi.getUserAvatar(holder.email, buildAvatarFile(context, holder.email + ".jpg").getAbsolutePath(), listener);
                }
            }else{

                if(megaApi==null){
                    logWarning("megaApi is Null in Offline mode");
                    return;
                }

                megaApi.getUserAvatar(holder.email, buildAvatarFile(context, holder.email + ".jpg").getAbsolutePath(), listener);
            }
        }
    }

    void createGroupChatAvatar(ViewHolderChips holder, ChatExplorerListItem item){
        logDebug("createGroupChatAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint paintText = new Paint();
        Paint paintCircle = new Paint();
        paintCircle.setColor(ContextCompat.getColor(context,R.color.divider_upgrade_account));
        paintCircle.setAntiAlias(true);

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(150);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        Typeface face = Typeface.SANS_SERIF;
        paintText.setTypeface(face);
        paintText.setAntiAlias(true);
        paintText.setSubpixelText(true);
        paintText.setStyle(Paint.Style.FILL);

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius,paintCircle);

        String firstLetter = ChatUtil.getFirstLetter(item.getTitle());

        logDebug("Draw letter: " + firstLetter);
        Rect bounds = new Rect();

        paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
        int xPos = (c.getWidth()/2);
        int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
        c.drawText(firstLetter, xPos, yPos, paintText);

        holder.avatar.setImageBitmap(defaultAvatar);;
    }

    public void createDefaultAvatar(ViewHolderChips holder, ChatExplorerListItem item){
        logDebug("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT,DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint paintText = new Paint();
        Paint paintCircle = new Paint();

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(150);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        Typeface face = Typeface.SANS_SERIF;
        paintText.setTypeface(face);
        paintText.setAntiAlias(true);
        paintText.setSubpixelText(true);
        paintText.setStyle(Paint.Style.FILL);

        String color = null;
        if (item.getContact() != null && item.getContact().getMegaUser() != null) {
            color = megaApi.getUserAvatarColor(item.getContact().getMegaUser());
        }
        if(color!=null){
            logDebug("The color to set the avatar is " + color);
            paintCircle.setColor(Color.parseColor(color));
        }
        else{
            logDebug("Default color to the avatar");
            paintCircle.setColor(ContextCompat.getColor(context, R.color.color_default_avatar_phone));
        }
        paintCircle.setAntiAlias(true);

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius,paintCircle);
        String firstLetter = ChatUtil.getFirstLetter(item.getTitle());
        if(firstLetter == null || firstLetter.trim().isEmpty() || firstLetter.equals("(")){
            firstLetter = " ";
        }
        Rect bounds = new Rect();

        paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
        int xPos = (c.getWidth()/2);
        int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
        c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);

        holder.avatar.setImageBitmap(defaultAvatar);
    }
}
