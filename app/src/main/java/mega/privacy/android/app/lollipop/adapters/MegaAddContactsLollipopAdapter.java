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
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.CacheFolderManager.isFileAvailable;

/**
 * Created by mega on 28/11/17.
 */

public class MegaAddContactsLollipopAdapter extends RecyclerView.Adapter<MegaAddContactsLollipopAdapter.ViewHolderChips> implements View.OnClickListener{

    private int positionClicked;
    ArrayList<MegaContactAdapter> contacts;
    private MegaApiAndroid megaApi;
    private Context context;

    public MegaAddContactsLollipopAdapter (Context _context, ArrayList <MegaContactAdapter> _contacts){
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
    public MegaAddContactsLollipopAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_layout_chip);
        holder.itemLayout.setOnClickListener(this);

        holder.textViewName = (TextView) v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidth(Util.px2dp(60, outMetrics));

        holder.avatar = (RoundedImageView) v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = (ImageView) v.findViewById(R.id.delete_icon_chip);

        holder.itemLayout.setTag(holder);

        v.setTag(holder);


        return holder;
    }

    @Override
    public void onBindViewHolder(MegaAddContactsLollipopAdapter.ViewHolderChips holder, int position) {
        log("onBindViewHolderList");

        MegaContactAdapter contact = (MegaContactAdapter) getItem(position);

        String[] s;

        if (contact.getFullName() != null) {
            if (contact.getMegaUser() == null && contact.getMegaContactDB() == null) {
                s = contact.getFullName().split("[@._]");
                if (s != null && s.length > 0) {
                    holder.textViewName.setText(s[0]);
                }
                else {
                    holder.textViewName.setText(contact.getFullName());
                }
            }
            else {
                s = contact.getFullName().split(" ");
                if (s != null && s.length > 0) {
                    holder.textViewName.setText(s[0]);
                }
                else {
                    holder.textViewName.setText(contact.getFullName());
                }
            }
        }

        holder.avatar.setImageBitmap(setUserAvatar(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public void onClick(View view) {
        log("onClick");

        ViewHolderChips holder = (ViewHolderChips) view.getTag();
        if(holder!=null){
            int currentPosition = holder.getLayoutPosition();
            log("onClick -> Current position: "+currentPosition);

            if(currentPosition<0){
                log("Current position error - not valid value");
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
            log("Error. Holder is Null");
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
        log("setPositionClicked: "+p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setContacts (ArrayList<MegaContactAdapter> contacts){
        log("setContacts");
        this.contacts = contacts;

        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        log("getItem");
        return contacts.get(position);
    }

    public Bitmap setUserAvatar(MegaContactAdapter contact){
        log("setUserAvatar");

        File avatar = null;
        String mail;

        if (contact.getMegaUser() != null && contact.getMegaUser().getEmail() != null) {
            mail = contact.getMegaUser().getEmail();
        }
        else if (contact.getMegaContactDB() != null && contact.getMegaContactDB().getMail() != null) {
            mail = contact.getMegaContactDB().getMail();
        }
        else {
            mail = contact.getFullName();
        }

        if (contact.getMegaUser() == null && contact.getMegaContactDB() == null) {
            return createDefaultAvatar(contact.getFullName(), contact);
        }

        avatar = buildAvatarFile(context, mail + ".jpg");
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

    public Bitmap createDefaultAvatar(String mail, MegaContactAdapter contact){
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
        if (contact.getMegaUser() != null) {
            color = megaApi.getUserAvatarColor(contact.getMegaUser());
        }
        if(color!=null){
            log("The color to set the avatar is "+color);
            paintCircle.setColor(Color.parseColor(color));
            paintCircle.setAntiAlias(true);
        }
        else{
            log("Default color to the avatar");
            paintCircle.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
            paintCircle.setAntiAlias(true);
        }


        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius,paintCircle);

        String fullName = null;
        if(contact.getFullName()!=null){
            fullName = contact.getFullName();
        }
        else{
            //No name, ask for it and later refresh!!
            fullName = mail;
        }
        String firstLetter = fullName.charAt(0) + "";

        log("Draw letter: "+firstLetter);
        Rect bounds = new Rect();

        paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
        int xPos = (c.getWidth()/2);
        int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
        c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);

        return defaultAvatar;
    }

    private static void log(String log) {
        Util.log("MegaAddContactsLollipopAdapter", log);
    }
}
