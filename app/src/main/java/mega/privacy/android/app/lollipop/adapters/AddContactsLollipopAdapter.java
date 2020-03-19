package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
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
import mega.privacy.android.app.lollipop.PhoneContactInfo;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;


public class AddContactsLollipopAdapter extends RecyclerView.Adapter<AddContactsLollipopAdapter.ViewHolderChips> implements View.OnClickListener{

    private int positionClicked;
    ArrayList<PhoneContactInfo> contacts;
    private MegaApiAndroid megaApi;
    private Context context;

    public AddContactsLollipopAdapter (Context _context, ArrayList<PhoneContactInfo> _contacts){
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
    public AddContactsLollipopAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_layout_chip);
        holder.itemLayout.setOnClickListener(this);

        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidthEmojis(px2dp(MAX_WIDTH_ADD_CONTACTS, outMetrics));
        holder.textViewName.setTypeEllipsize(TextUtils.TruncateAt.MIDDLE);
        holder.avatar = (RoundedImageView) v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = (ImageView) v.findViewById(R.id.delete_icon_chip);

        holder.itemLayout.setTag(holder);

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(AddContactsLollipopAdapter.ViewHolderChips holder, int position) {
        logDebug("onBindViewHolderList");

        PhoneContactInfo contact = (PhoneContactInfo) getItem(position);
        String[] s;
        if (contact.getName() != null){
            s = contact.getName().split(" ");
            if (s != null && s.length > 0){
                holder.textViewName.setText(s[0]);
            }
            else {
                holder.textViewName.setText(contact.getName());
            }
        }
        else {
            s = contact.getEmail().split("[@._]");
            if (s != null && s.length > 0){
                holder.textViewName.setText(s[0]);
            }
            else {
                holder.textViewName.setText(contact.getEmail());
            }
        }
        int color = ContextCompat.getColor(context, R.color.color_default_avatar_phone);
        holder.avatar.setImageBitmap(getDefaultAvatar(context, color, holder.textViewName.getText().toString(), AVATAR_SIZE, true));
    }

    @Override
    public int getItemCount() {
        return  contacts.size();
    }

    @Override
    public void onClick(View view) {
        logDebug("onClick");

        AddContactsLollipopAdapter.ViewHolderChips holder = (AddContactsLollipopAdapter.ViewHolderChips) view.getTag();
        if(holder!=null){
            int currentPosition = holder.getLayoutPosition();
            logDebug("onClick -> Current position: " + currentPosition);

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
        logDebug("Position clicked: " + p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setContacts (ArrayList<PhoneContactInfo> contacts){
        logDebug("setContacts");
        this.contacts = contacts;

        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        logDebug("Position: " + position);
        return contacts.get(position);
    }
}
