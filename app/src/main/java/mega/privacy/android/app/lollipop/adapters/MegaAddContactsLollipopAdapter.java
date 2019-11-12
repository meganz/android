package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_ADD_CONTACTS;
import static mega.privacy.android.app.utils.FileUtils.isFileAvailable;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.Util.colorAvatar;
import static mega.privacy.android.app.utils.Util.getCircleBitmap;
import static mega.privacy.android.app.utils.Util.getDefaultAvatar;
import static mega.privacy.android.app.utils.Util.px2dp;


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

        EmojiTextView textViewName;
        ImageView deleteIcon;
        RoundedImageView avatar;
        RelativeLayout itemLayout;

    }

    ViewHolderChips holder = null;

    @Override
    public MegaAddContactsLollipopAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = v.findViewById(R.id.item_layout_chip);
        holder.itemLayout.setOnClickListener(this);

        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidthEmojis(px2dp(MAX_WIDTH_ADD_CONTACTS, outMetrics));
        holder.avatar = v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = v.findViewById(R.id.delete_icon_chip);
        holder.itemLayout.setTag(holder);
        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(MegaAddContactsLollipopAdapter.ViewHolderChips holder, int position) {
        logDebug("onBindViewHolderList");

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
        setUserAvatar(contact);

    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public void onClick(View view) {
        logDebug("onClick");

        ViewHolderChips holder = (ViewHolderChips) view.getTag();
        if(holder!=null){
            int currentPosition = holder.getLayoutPosition();
            logDebug("Current position: "+currentPosition);

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

    public void setContacts (ArrayList<MegaContactAdapter> contacts){
        logDebug("setContacts");
        this.contacts = contacts;

        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        logDebug("Position: " + position);
        return contacts.get(position);
    }

    public void setUserAvatar(MegaContactAdapter contact){
        logDebug("setUserAvatar");

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

        /*Default avatar*/
        holder.avatar.setImageBitmap(getDefaultAvatar(colorAvatar(context, megaApi, contact.getMegaUser(), false), mail, AVATAR_SIZE));

        /*Avatar*/
        avatar = buildAvatarFile(context, mail + ".jpg");
        Bitmap bitmap;
        if (isFileAvailable(avatar) && avatar.length() > 0){
            BitmapFactory.Options bOpts = new BitmapFactory.Options();
            bOpts.inPurgeable = true;
            bOpts.inInputShareable = true;
            bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
            if (bitmap != null) {
                holder.avatar.setImageBitmap(getCircleBitmap(bitmap));
            }
        }
    }
}
