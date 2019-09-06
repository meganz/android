package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
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
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ShareContactInfo;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListenerShare;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;

/**
 * Created by mega on 4/07/18.
 */

public class ShareContactsHeaderAdapter extends RecyclerView.Adapter<ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop> implements View.OnClickListener, SectionTitleProvider {

    public static final int ITEM_VIEW_TYPE_NODE= 0;
    public static final int ITEM_VIEW_TYPE_HEADER = 1;

    DatabaseHandler dbH = null;
    public static int MAX_WIDTH_CONTACT_NAME_LAND=450;
    public static int MAX_WIDTH_CONTACT_NAME_PORT=200;
    private Context mContext;
    OnItemClickListener mItemClickListener;
    private List<ShareContactInfo> shareContacts;




    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    public ShareContactsHeaderAdapter(Context context, ArrayList<ShareContactInfo> shareContacts) {
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        if(Util.isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
            }
        }
        mContext = context;
        this.shareContacts = shareContacts;
    }

    public void setContacts(List<ShareContactInfo> shareContacts){
        this.shareContacts = shareContacts;
        notifyDataSetChanged();

    }

    public ShareContactInfo getItem(int position)
    {
        if(position < shareContacts.size() && position >= 0){
            return shareContacts.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public String getSectionTitle(int position) {
        ShareContactInfo contact = shareContacts.get(position);

        if (contact.isMegaContact() && !contact.isHeader()){
            return contact.getMegaContactAdapter().getFullName().substring(0, 1).toUpperCase();
        }
        else if (!contact.isHeader()) {
            return contact.getPhoneContactInfo().getName().substring(0,1).toUpperCase();
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        ShareContactInfo contact = getItem(position);
        if (contact.isHeader()){
            return ITEM_VIEW_TYPE_HEADER;
        }
        else{
            return ITEM_VIEW_TYPE_NODE;
        }
    }

    public class ViewHolderShareContactsLollipop extends RecyclerView.ViewHolder implements View.OnClickListener{

        RelativeLayout itemHeader;
        TextView textHeader;
        RelativeLayout itemLayout;
        EmojiTextView contactNameTextView;
        TextView emailTextView;
        public String mail;
        public RoundedImageView avatar;
        ImageView contactStateIcon;
        int currentPosition;

        public ViewHolderShareContactsLollipop(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(mItemClickListener != null){
                mItemClickListener.onItemClick(v, getPosition());
            }
        }
    }

    @Override
    public ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);


        View rowView = inflater.inflate(R.layout.item_contact_share, parent, false);
        ViewHolderShareContactsLollipop holder = new ViewHolderShareContactsLollipop(rowView);

        holder.itemHeader = (RelativeLayout) rowView.findViewById(R.id.header);
        holder.textHeader = (TextView) rowView.findViewById(R.id.text_header);

        holder.itemLayout = (RelativeLayout) rowView.findViewById(R.id.item_content);
        holder.contactNameTextView = rowView.findViewById(R.id.contact_name);

        if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_LAND, mContext.getResources().getDisplayMetrics());
            holder.contactNameTextView.setMaxWidth((int) width);
        }
        else{
            float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_PORT, mContext.getResources().getDisplayMetrics());
            holder.contactNameTextView.setMaxWidth((int) width);
        }
        holder.contactNameTextView.setEmojiSize(Util.px2dp(Constants.EMOJI_SIZE, outMetrics));

        holder.emailTextView = (TextView) rowView.findViewById(R.id.contact_mail);
        holder.avatar = (RoundedImageView) rowView.findViewById(R.id.contact_avatar);
        holder.contactStateIcon = (ImageView) rowView.findViewById(R.id.contact_state);

        return holder;
    }

    @Override
    public void onBindViewHolder(ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop holder, int position) {

        ShareContactInfo contact = getItem(position);

        holder.currentPosition = position;

        if (contact.isMegaContact()){
            if (contact.isHeader()){
                holder.itemLayout.setVisibility(View.GONE);
                holder.itemHeader.setVisibility(View.VISIBLE);
                holder.textHeader.setText(mContext.getString(R.string.section_contacts));
            }
            else {
                holder.itemLayout.setVisibility(View.VISIBLE);
                holder.itemHeader.setVisibility(View.GONE);
                holder.contactStateIcon.setVisibility(View.VISIBLE);

                String name;
                String mail = ((AddContactActivityLollipop) mContext).getShareContactMail(contact);
                holder.mail = mail;
                if (contact.getMegaContactAdapter().getFullName() != null) {
                    name = contact.getMegaContactAdapter().getFullName();
                }
                else {
                    name = mail;
                }
                holder.contactNameTextView.setText(name);
                holder.emailTextView.setText(mail);

                if(Util.isChatEnabled()){
                    holder.contactStateIcon.setVisibility(View.VISIBLE);
                    if (megaChatApi != null){
                        int userStatus = megaChatApi.getUserOnlineStatus(contact.getMegaContactAdapter().getMegaUser().getHandle());
                        if(userStatus == MegaChatApi.STATUS_ONLINE){
                            log("This user is connected");
                            holder.contactStateIcon.setVisibility(View.VISIBLE);
                            holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.circle_status_contact_online));
                        }
                        else if(userStatus == MegaChatApi.STATUS_AWAY){
                            log("This user is away");
                            holder.contactStateIcon.setVisibility(View.VISIBLE);
                            holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.circle_status_contact_away));
                        }
                        else if(userStatus == MegaChatApi.STATUS_BUSY){
                            log("This user is busy");
                            holder.contactStateIcon.setVisibility(View.VISIBLE);
                            holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.circle_status_contact_busy));
                        }
                        else if(userStatus == MegaChatApi.STATUS_OFFLINE){
                            log("This user is offline");
                            holder.contactStateIcon.setVisibility(View.VISIBLE);
                            holder.contactStateIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.circle_status_contact_offline));
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
                    holder.contactStateIcon.setVisibility(View.GONE);
                }

                holder.avatar.setImageBitmap(setUserAvatar(contact));

                UserAvatarListenerShare listener = new UserAvatarListenerShare(mContext, holder);

                File avatar = buildAvatarFile(mContext,mail + ".jpg");
                Bitmap bitmap = null;
                if (isFileAvailable(avatar)){
                    if (avatar.length() > 0){
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                            megaApi.getUserAvatar(contact.getMegaContactAdapter().getMegaUser(),buildAvatarFile(mContext,mail + ".jpg").getAbsolutePath(),listener);
                        }
                        else{
                            holder.avatar.setImageBitmap(bitmap);
                        }
                    }
                    else{
                        megaApi.getUserAvatar(contact.getMegaContactAdapter().getMegaUser(),buildAvatarFile(mContext,mail + ".jpg").getAbsolutePath(),listener);
                    }
                }
                else{
                    megaApi.getUserAvatar(contact.getMegaContactAdapter().getMegaUser(),buildAvatarFile(mContext,mail + ".jpg").getAbsolutePath(),listener);
                }
            }
        }
        else if (contact.isPhoneContact()){
            if (contact.isHeader()){
                holder.itemLayout.setVisibility(View.GONE);
                holder.itemHeader.setVisibility(View.VISIBLE);
                holder.textHeader.setText(mContext.getString(R.string.contacts_phone));
            }
            else {
                holder.itemLayout.setVisibility(View.VISIBLE);
                holder.itemHeader.setVisibility(View.GONE);
                holder.contactStateIcon.setVisibility(View.GONE);

                holder.contactNameTextView.setText(contact.getPhoneContactInfo().getName());
                holder.emailTextView.setText(contact.getPhoneContactInfo().getEmail());

                holder.avatar.setImageBitmap(createDefaultAvatar(holder.emailTextView.getText().toString(), contact));
            }
        }
    }

    public Bitmap setUserAvatar(ShareContactInfo contact){
        log("setUserAvatar");

        String mail = null;

        mail = ((AddContactActivityLollipop) mContext).getShareContactMail(contact);

        if (!contact.isPhoneContact() && !contact.isMegaContact()) {
            return createDefaultAvatar(mail, contact);
        }

        File avatar = buildAvatarFile(mContext,mail + ".jpg");
        Bitmap bitmap = null;
        if (isFileAvailable(avatar)){
            if (avatar.length() > 0){
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    return createDefaultAvatar(mail, contact);
                }
                else{
                    return Util.getCircleBitmap(bitmap);
                }
            }
            else{
                return createDefaultAvatar(mail, contact);
            }
        }
        else{
            return createDefaultAvatar(mail, contact);
        }
    }

    public Bitmap createDefaultAvatar(String mail, ShareContactInfo contact){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
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
        if (contact.isMegaContact() && contact.getMegaContactAdapter().getMegaUser() != null) {
            color = megaApi.getUserAvatarColor(contact.getMegaContactAdapter().getMegaUser());
        }
        if(color!=null){
            log("The color to set the avatar is "+color);
            paintCircle.setColor(Color.parseColor(color));
            paintCircle.setAntiAlias(true);
        }
        else{
            log("Default color to the avatar");
            if (contact.isPhoneContact()){
                paintCircle.setColor(ContextCompat.getColor(mContext, R.color.color_default_avatar_phone));
            }
            else {
                paintCircle.setColor(ContextCompat.getColor(mContext, R.color.lollipop_primary_color));
            }
            paintCircle.setAntiAlias(true);
        }


        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius,paintCircle);

        String fullName = null;
        if(contact.isPhoneContact()){
            fullName = contact.getPhoneContactInfo().getName();
            if (fullName == null) {
                fullName = mail;
            }
        }
        else if (contact.isMegaContact()) {
            fullName = contact.getMegaContactAdapter().getFullName();
            if (fullName == null) {
                fullName = mail;
            }
        }
        else{
            //No name, ask for it and later refresh!!
            fullName = mail;
        }
        String firstLetter = ChatUtil.getFirstLetter(fullName);
        if(firstLetter == null || firstLetter.trim().isEmpty() || firstLetter.equals("(")){
            firstLetter = " ";
        }

        log("Draw letter: "+firstLetter);
        Rect bounds = new Rect();

        paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
        int xPos = (c.getWidth()/2);
        int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
        c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);

        return defaultAvatar;
    }

    @Override
    public int getItemCount() {
        if (shareContacts == null) {
            return 0;
        }
        return shareContacts.size();
    }

    @Override
    public void onClick(View v) {

    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener){
        this.mItemClickListener = mItemClickListener;
    }

    private static void log(String log) {
        Util.log("ShareContactsHeaderAdapter", log);
    }
}
