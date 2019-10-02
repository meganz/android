package mega.privacy.android.app.lollipop.adapters;

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
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ShareContactInfo;

import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ShareContactsAdapter extends RecyclerView.Adapter<ShareContactsAdapter.ViewHolderChips> implements View.OnClickListener{

    private int positionClicked;
    ArrayList<ShareContactInfo> contacts;
    private MegaApiAndroid megaApi;
    private Context context;

    public ShareContactsAdapter (Context _context, ArrayList<ShareContactInfo> _contacts){
        this.contacts = _contacts;
        this.context = _context;
        this.positionClicked = -1;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }


    public static class ViewHolderChips extends RecyclerView.ViewHolder{
        public ViewHolderChips(View itemView) {
            super(itemView);
        }

        TextView textViewName;
        ImageView deleteIcon;
        RoundedImageView avatar;
        RelativeLayout itemLayout;

    }

    ViewHolderChips holder = null;

    @Override
    public ShareContactsAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_layout_chip);
        holder.itemLayout.setOnClickListener(this);

        holder.textViewName = (TextView) v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidth(px2dp(60, outMetrics));

        holder.avatar = (RoundedImageView) v.findViewById(R.id.rounded_avatar);

        holder.deleteIcon = (ImageView) v.findViewById(R.id.delete_icon_chip);

        holder.itemLayout.setTag(holder);

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ShareContactsAdapter.ViewHolderChips holder, int position) {
        logDebug("Position: " + position);

        ShareContactInfo contact = (ShareContactInfo) getItem(position);
        String[] s;
        if (contact.isPhoneContact()){
            if (contact.getPhoneContactInfo().getName() != null){
                s = contact.getPhoneContactInfo().getName().split(" ");
                if (s != null && s.length > 0){
                    holder.textViewName.setText(s[0]);
                }
                else {
                    holder.textViewName.setText(contact.getPhoneContactInfo().getName());
                }
            }
            else {
                s = contact.getPhoneContactInfo().getEmail().split("[@._]");
                if (s != null && s.length > 0){
                    holder.textViewName.setText(s[0]);
                }
                else {
                    holder.textViewName.setText(contact.getPhoneContactInfo().getEmail());
                }
            }
        }
        else if (contact.isMegaContact()){
            if (contact.getMegaContactAdapter().getFullName() != null) {
                if (contact.getMegaContactAdapter().getMegaUser() == null && contact.getMegaContactAdapter().getMegaContactDB() == null) {
                    s = contact.getMegaContactAdapter().getFullName().split("[@._]");
                    if (s != null && s.length > 0) {
                        holder.textViewName.setText(s[0]);
                    }
                    else {
                        holder.textViewName.setText(contact.getMegaContactAdapter().getFullName());
                    }
                }
                else {
                    s = contact.getMegaContactAdapter().getFullName().split(" ");
                    if (s != null && s.length > 0) {
                        holder.textViewName.setText(s[0]);
                    }
                    else {
                        holder.textViewName.setText(contact.getMegaContactAdapter().getFullName());
                    }
                }
            }
        }
        else {
            s = contact.getMail().split("[@._]");
            if (s != null && s.length > 0) {
                holder.textViewName.setText(s[0]);
            }
            else {
                holder.textViewName.setText(contact.getMail());
            }
        }

        holder.avatar.setImageBitmap(setUserAvatar(contact));
    }

    @Override
    public int getItemCount() {
        return  contacts.size();
    }

    @Override
    public void onClick(View view) {
        logDebug("onClick");

        ShareContactsAdapter.ViewHolderChips holder = (ShareContactsAdapter.ViewHolderChips) view.getTag();
        if(holder!=null){
            int currentPosition = holder.getLayoutPosition();
            logDebug("Current position: " + currentPosition);

            if(currentPosition<0){
                logError("Current position error - not valid value");
                return;
            }
            switch (view.getId()) {
                case R.id.item_layout_chip: {
                    ((AddContactActivityLollipop) context).deleteContact(currentPosition);
                    break;
                }
            }
        }
        else{
            logError("Error. Holder is Null");
        }
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPositionClicked() {
        return positionClicked;
    }

    public void setPositionClicked(int p) {
        logDebug("Position: " + p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setContacts (ArrayList<ShareContactInfo> contacts){
        logDebug("setContacts");
        this.contacts = contacts;

        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        logDebug("Position: " + position);
        return contacts.get(position);
    }

    public Bitmap setUserAvatar(ShareContactInfo contact){
        logDebug("setUserAvatar");

        File avatar = null;
        String mail = null;

        mail = ((AddContactActivityLollipop) context).getShareContactMail(contact);

        if (!contact.isPhoneContact() && !contact.isMegaContact()) {
            return createDefaultAvatar(mail, contact);
        }

        avatar = buildAvatarFile(context,mail + ".jpg");
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
                    return getCircleBitmap(bitmap);
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
        if (contact.isMegaContact() && contact.getMegaContactAdapter().getMegaUser() != null) {
            color = megaApi.getUserAvatarColor(contact.getMegaContactAdapter().getMegaUser());
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
        String firstLetter = fullName.charAt(0) + "";

        logDebug("Draw letter: " + firstLetter);
        Rect bounds = new Rect();

        paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
        int xPos = (c.getWidth()/2);
        int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
        c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);

        return defaultAvatar;
    }
}
