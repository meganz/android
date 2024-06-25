package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.main.InvitationContactInfo.TYPE_PHONE_CONTACT;
import static mega.privacy.android.app.main.InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.main.InvitationContactInfo;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

public class InvitationContactsAdapter extends RecyclerView.Adapter<InvitationContactsAdapter.ViewHolderPhoneContacts> {

    private Context context;
    private List<InvitationContactInfo> contactData;
    private LayoutInflater inflater;
    private OnItemClickListener callback;

    public InvitationContactsAdapter(Context context, List<InvitationContactInfo> phoneContacts, OnItemClickListener callback, MegaApiAndroid megaApi) {
        this.context = context;
        this.contactData = phoneContacts == null ? new ArrayList<>() : phoneContacts;
        this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.callback = callback;
    }

    public class ViewHolderPhoneContacts extends RecyclerView.ViewHolder implements View.OnClickListener {
        private RelativeLayout contactLayout;
        private TextView contactNameTextView, displayLabelTextView, headerTextView;
        private RoundedImageView imageView;
        private String contactName, displayLabel;

        private ViewHolderPhoneContacts(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Timber.d("CI contact get clicked");
            int position = getAdapterPosition();
            if (callback != null && position >= 0 && position < contactData.size()) {
                InvitationContactInfo invitationContactInfo = contactData.get(position);
                if (invitationContactInfo.getType() == TYPE_PHONE_CONTACT) {
                    callback.onItemClick(position);
                }
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    @Override
    public int getItemCount() {
        return contactData.size();
    }

    @Override
    public int getItemViewType(int position) {
        InvitationContactInfo item = getItem(position);
        if (item != null) {
            return item.getType();
        }
        return -1;
    }

    public InvitationContactInfo getItem(int position) {
        if (position >= 0 && position < contactData.size()) {
            return contactData.get(position);
        }
        return null;
    }

    public void setContactData(List<InvitationContactInfo> contactData) {
        this.contactData = contactData;
    }

    public List<InvitationContactInfo> getData() {
        return contactData;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public ViewHolderPhoneContacts onCreateViewHolder(ViewGroup parentView, int viewType) {
        switch (viewType) {
            case TYPE_PHONE_CONTACT_HEADER:
                return createHeaderHolder(parentView);
            case TYPE_PHONE_CONTACT:
            default:
                return createContactHolder(parentView);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolderPhoneContacts holder, int position) {
        InvitationContactInfo contact = getItem(position);
        if (contact == null)
            return;

        switch (contact.getType()) {
            case TYPE_PHONE_CONTACT_HEADER -> bindHeader(holder);
            case TYPE_PHONE_CONTACT -> bindContact(holder, contact);
        }
    }

    private ViewHolderPhoneContacts createHeaderHolder(ViewGroup parentView) {
        Timber.d("create Header Holder");
        View rowView = inflater.inflate(R.layout.contact_list_section_header, parentView, false);
        ViewHolderPhoneContacts holder = new ViewHolderPhoneContacts(rowView);
        holder.headerTextView = rowView.findViewById(R.id.section_header);
        rowView.setTag(holder);
        return holder;
    }

    private ViewHolderPhoneContacts createContactHolder(ViewGroup parentView) {
        Timber.d("create Contact Holder");
        View rowView = inflater.inflate(R.layout.contact_explorer_item, parentView, false);
        ViewHolderPhoneContacts holder = new ViewHolderPhoneContacts(rowView);
        holder.contactLayout = rowView.findViewById(R.id.contact_list_item_layout);
        holder.contactNameTextView = rowView.findViewById(R.id.contact_explorer_name);
        holder.displayLabelTextView = rowView.findViewById(R.id.contact_explorer_phone_mail);
        holder.imageView = rowView.findViewById(R.id.contact_explorer_thumbnail);
        rowView.setTag(holder);
        return holder;
    }

    private void bindHeader(ViewHolderPhoneContacts holder) {
        holder.headerTextView.setText(context.getString(R.string.contacts_phone));
    }

    private void bindContact(ViewHolderPhoneContacts holder, InvitationContactInfo contact) {
        holder.displayLabel = contact.getDisplayInfo();
        holder.contactName = contact.getContactName();
        holder.contactNameTextView.setText(holder.contactName);
        holder.displayLabelTextView.setText(holder.displayLabel);

        if (contact.isHighlighted()) {
            setItemHighlighted(holder.contactLayout);
        } else {
            Bitmap bitmap = null;
            if (contact.getPhotoUri() != null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(),
                            Uri.parse(contact.getPhotoUri())
                    );
                } catch (IOException e) {
                    Timber.e(e, "Failed to convert contact's photo Uri to a Bitmap");
                }
            }

            // create default one if unable to get user pre-set avatar
            if (bitmap == null) {
                Timber.d("create default avatar as unable to get user pre-set one");
                int avatarColor = ContextCompat.getColor(context, R.color.grey_500_grey_400);
                bitmap = getDefaultAvatar(avatarColor, contact.getContactName(), AVATAR_SIZE, true, false);
            }
            holder.imageView.setImageBitmap(bitmap);
        }
    }

    private void setItemHighlighted(View view) {
        Timber.d("setItemHighlighted");
        ImageView imageView = view.findViewById(R.id.contact_explorer_thumbnail);
        imageView.setImageResource(R.drawable.ic_chat_avatar_select);
    }
}
