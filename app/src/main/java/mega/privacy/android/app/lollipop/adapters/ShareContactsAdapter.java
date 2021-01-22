package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ShareContactInfo;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;


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

        EmojiTextView textViewName;
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

        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidthEmojis(dp2px(MAX_WIDTH_ADD_CONTACTS, outMetrics));
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

        holder.avatar.setImageBitmap(getAvatarShareContact(context, contact));
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
}
