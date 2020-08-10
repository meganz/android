package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
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

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ShareContactInfo;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListenerShare;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class ShareContactsHeaderAdapter extends RecyclerView.Adapter<ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop> implements View.OnClickListener, SectionTitleProvider {

    DatabaseHandler dbH = null;
    private Context mContext;
    OnItemClickListener mItemClickListener;
    private List<ShareContactInfo> shareContacts;

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    public ShareContactsHeaderAdapter(Context context, ArrayList<ShareContactInfo> shareContacts) {
        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
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

        if (contact != null) {
            if (contact.isHeader()) {
                return HEADER_VIEW_TYPE;
            } else if (contact.isProgress()) {
                return ITEM_PROGRESS;
            }
        }

        return ITEM_VIEW_TYPE;
    }

    public class ViewHolderShareContactsLollipop extends RecyclerView.ViewHolder implements View.OnClickListener{

        RelativeLayout itemProgress;
        RelativeLayout itemHeader;
        TextView textHeader;
        RelativeLayout itemLayout;
        EmojiTextView contactNameTextView;
        TextView emailTextView;
        public String mail;
        public RoundedImageView avatar;
        ImageView verifiedIcon;
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

        holder.itemProgress = rowView.findViewById(R.id.item_progress);

        holder.itemHeader = rowView.findViewById(R.id.header);
        holder.textHeader = rowView.findViewById(R.id.text_header);

        holder.itemLayout = rowView.findViewById(R.id.item_content);
        holder.contactNameTextView = rowView.findViewById(R.id.contact_name);

        if(!isScreenInPortrait(mContext)){
            float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_LAND, mContext.getResources().getDisplayMetrics());
            holder.contactNameTextView.setMaxWidthEmojis((int) width);
        }
        else{
            float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_CONTACT_NAME_PORT, mContext.getResources().getDisplayMetrics());
            holder.contactNameTextView.setMaxWidthEmojis((int) width);
        }

        holder.emailTextView = rowView.findViewById(R.id.contact_mail);
        holder.avatar = rowView.findViewById(R.id.contact_avatar);
        holder.verifiedIcon = rowView.findViewById(R.id.verified_icon);
        holder.contactStateIcon = rowView.findViewById(R.id.contact_state);

        return holder;
    }

    @Override
    public void onBindViewHolder(ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop holder, int position) {

        ShareContactInfo contact = getItem(position);

        holder.currentPosition = position;

        holder.itemProgress.setVisibility(View.GONE);
        holder.verifiedIcon.setVisibility(View.GONE);

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

                MegaUser user = megaApi.getContact(mail);
                holder.verifiedIcon.setVisibility(user != null && megaApi.areCredentialsVerified(user) ? View.VISIBLE : View.GONE);

                if (contact.getMegaContactAdapter().getFullName() != null) {
                    name = contact.getMegaContactAdapter().getFullName();
                }
                else {
                    name = mail;
                }
                holder.contactNameTextView.setText(name);
                holder.emailTextView.setText(mail);

                holder.contactStateIcon.setVisibility(View.VISIBLE);
                setContactStatus(megaChatApi.getUserOnlineStatus(contact.getMegaContactAdapter().getMegaUser().getHandle()), holder.contactStateIcon);
                holder.avatar.setImageBitmap(getAvatarShareContact(mContext, contact));
                UserAvatarListenerShare listener = new UserAvatarListenerShare(mContext, holder);

                if (contact.getMegaContactAdapter().isSelected()) {
                    holder.itemLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.new_multiselect_color));
                    holder.avatar.setImageResource(R.drawable.ic_select_folder);
                } else {
                    holder.itemLayout.setBackgroundColor(Color.WHITE);

                    File avatar = buildAvatarFile(mContext, mail + ".jpg");
                    Bitmap bitmap = null;
                    if (isFileAvailable(avatar)) {
                        if (avatar.length() > 0) {
                            BitmapFactory.Options bOpts = new BitmapFactory.Options();
                            bOpts.inPurgeable = true;
                            bOpts.inInputShareable = true;
                            bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                            if (bitmap == null) {
                                avatar.delete();
                                megaApi.getUserAvatar(contact.getMegaContactAdapter().getMegaUser(), buildAvatarFile(mContext, mail + ".jpg").getAbsolutePath(), listener);
                            } else {
                                holder.avatar.setImageBitmap(bitmap);
                            }
                        } else {
                            megaApi.getUserAvatar(contact.getMegaContactAdapter().getMegaUser(), buildAvatarFile(mContext, mail + ".jpg").getAbsolutePath(), listener);
                        }
                    } else {
                        megaApi.getUserAvatar(contact.getMegaContactAdapter().getMegaUser(), buildAvatarFile(mContext, mail + ".jpg").getAbsolutePath(), listener);
                    }
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
                holder.avatar.setImageBitmap(getAvatarShareContact(mContext, contact));
            }
        } else if (contact.isProgress()) {
            holder.itemLayout.setVisibility(View.GONE);
            holder.itemHeader.setVisibility(View.GONE);
            holder.itemProgress.setVisibility(View.VISIBLE);
        }
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
}
