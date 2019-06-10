package mega.privacy.android.app.lollipop.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.InvitationContactInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_MEGA_CONTACT;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_MEGA_CONTACT_HEADER;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_PHONE_CONTACT;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolderPhoneContactsLollipop> {

    private Context mContext;
    private List<InvitationContactInfo> contactData;
    private LayoutInflater inflater;
    private OnItemClickListener callback;

    public ContactsAdapter(Context context, ArrayList<InvitationContactInfo> phoneContacts, OnItemClickListener callback) {
        this.mContext = context;
        this.contactData = phoneContacts == null ? new ArrayList<InvitationContactInfo>() : phoneContacts;
        this.inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.callback = callback;
    }

    class ViewHolderPhoneContactsLollipop extends RecyclerView.ViewHolder implements View.OnClickListener {
        private RelativeLayout contactLayout;
        private TextView contactNameTextView;
        private TextView phoneEmailTextView;
        private TextView headerTextView;
        private RoundedImageView imageView;
        private TextView initialLetter;
        private long contactId;
        private String contactName;
        private String contactMail;

        public ViewHolderPhoneContactsLollipop(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            log("CI contact get clicked");
            int position = getAdapterPosition();
            if (callback != null && position < contactData.size()) {
                InvitationContactInfo invitationContactInfo = contactData.get(position);
                if (invitationContactInfo.getType() == TYPE_MEGA_CONTACT || invitationContactInfo.getType() == TYPE_PHONE_CONTACT) {
                    boolean isSelected = !invitationContactInfo.isHighlighted();
                    if (isSelected) {
                        invitationContactInfo.setHighlighted(true);
                        setItemHighlighted(v);
                    } else {
                        invitationContactInfo.setHighlighted(false);
                        setItemNormal(v, invitationContactInfo.getBitmap());
                    }

                    callback.onItemClick(v, position);
                }
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    @Override
    public int getItemCount() {
        return contactData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return contactData.get(position).getType();
    }

    public InvitationContactInfo getItem(int position) {
        if (position < contactData.size()) {
            return contactData.get(position);
        }
        return null;
    }

    public void setContactData(ArrayList<InvitationContactInfo> contactData) {
        this.contactData = contactData;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public ViewHolderPhoneContactsLollipop onCreateViewHolder(ViewGroup parentView, int viewType) {
        switch (viewType) {
            case TYPE_MEGA_CONTACT_HEADER:
            case TYPE_PHONE_CONTACT_HEADER:
                return createHeaderHolder(parentView);
            case TYPE_MEGA_CONTACT:
            case TYPE_PHONE_CONTACT:
            default:
                return createContactHolder(parentView);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolderPhoneContactsLollipop holder, int position) {
        InvitationContactInfo contact = getItem(position);
        int type = contact.getType();
        switch (type) {
            case TYPE_MEGA_CONTACT_HEADER:
            case TYPE_PHONE_CONTACT_HEADER:
                bindHeader(holder, contact);
                break;
            case TYPE_PHONE_CONTACT:
                bindContact(holder, contact, false);
                break;
            case TYPE_MEGA_CONTACT:
                bindContact(holder, contact, true);
                break;
        }
    }

    private ViewHolderPhoneContactsLollipop createHeaderHolder(ViewGroup parentView) {
        log("create Header Holder");
        View rowView = inflater.inflate(R.layout.contact_list_section_header, parentView, false);
        ViewHolderPhoneContactsLollipop holder = new ViewHolderPhoneContactsLollipop(rowView);
        holder.headerTextView = rowView.findViewById(R.id.section_header);
        holder.contactId = -1;
        return holder;
    }

    private ViewHolderPhoneContactsLollipop createContactHolder(ViewGroup parentView) {
        log("create Contact Holder");
        View rowView = inflater.inflate(R.layout.contact_explorer_item, parentView, false);
        ViewHolderPhoneContactsLollipop holder = new ViewHolderPhoneContactsLollipop(rowView);
        holder.contactLayout = rowView.findViewById(R.id.contact_list_item_layout);
        holder.contactNameTextView = rowView.findViewById(R.id.contact_explorer_name);
        holder.phoneEmailTextView = rowView.findViewById(R.id.contact_explorer_phone_mail);
        holder.imageView = rowView.findViewById(R.id.contact_explorer_thumbnail);
        holder.initialLetter = rowView.findViewById(R.id.contact_explorer_initial_letter);
        return holder;
    }

    private void bindHeader(ViewHolderPhoneContactsLollipop holder, InvitationContactInfo contact) {
        holder.headerTextView.setText(contact.getName());
        holder.headerTextView.setTextColor(mContext.getResources().getColor(R.color.black));
        holder.headerTextView.setBackgroundColor(Color.WHITE);
    }

    private void bindContact(ViewHolderPhoneContactsLollipop holder, InvitationContactInfo contact, boolean isMegaContact) {
        holder.contactMail = contact.getEmail();
        holder.contactName = contact.getName();
        holder.contactId = contact.getId();
        holder.contactNameTextView.setText(contact.getName());
        holder.phoneEmailTextView.setText(contact.getEmail());
        holder.contactLayout.setBackgroundColor(Color.WHITE);

        if (contact.isHighlighted()) {
            log("contact is selected");
            setItemHighlighted(holder.contactLayout);
        } else {
            log("contact is not selected");
            Bitmap bitmap = null;
            if (isMegaContact) {
                //todo get Mega contact bitmap
            } else {
                bitmap = createPhoneContactBitmap(holder.contactId);
            }

            // create default one if unable to get user pre-set avatar
            if (bitmap == null) {
                log("create default avatar as unable to get user pre-set one");
                bitmap = createDefaultAvatar();
                String initial = "";
                if (isMegaContact &&
                        holder.contactMail != null &&
                        holder.contactMail.length() > 0) {
                    initial = holder.contactMail.charAt(0) + "";

                } else if (holder.contactName != null &&
                        holder.contactName.length() > 0) {
                    initial = holder.contactName.charAt(0) + "";
                }

                final int INITIAL_TEXT_SIZE = 24;
                initial = initial.toUpperCase(Locale.getDefault());
                holder.initialLetter.setVisibility(View.VISIBLE);
                holder.initialLetter.setText(initial);
                holder.initialLetter.setTextSize(INITIAL_TEXT_SIZE);
                holder.initialLetter.setTextColor(Color.WHITE);
            } else {
                log("hide initial as got user pre-set avatar");
                holder.initialLetter.setVisibility(View.GONE);
            }

            contact.setBitmap(bitmap);
            holder.imageView.setImageBitmap(bitmap);
        }
    }

    private Bitmap createPhoneContactBitmap(long id) {
        log("createPhoneContactBitmap");
        InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(),
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id));
        Bitmap photo = null;
        try {
            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return photo;
    }

    private Bitmap createDefaultAvatar() {
        log("createDefaultAvatar()");
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(ContextCompat.getColor(mContext, R.color.color_default_avatar_phone));

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        } else {
            radius = defaultAvatar.getHeight() / 2;
        }

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);
        return defaultAvatar;
    }

    private void setItemHighlighted(View view) {
        log("setItemHighlighted");
        view.setBackgroundColor(mContext.getResources().getColor(R.color.contactSelected));
        ImageView imageView = view.findViewById(R.id.contact_explorer_thumbnail);
        Bitmap image = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_select_folder);
        if (image != null) {
            imageView.setImageBitmap(image);
        }

        TextView initialLetter = view.findViewById(R.id.contact_explorer_initial_letter);
        initialLetter.setVisibility(View.GONE);
    }

    private void setItemNormal(View view, Bitmap bitmap) {
        log("setItemNormal");
        view.setBackgroundColor(mContext.getResources().getColor(R.color.white));
        if (bitmap != null) {
            ImageView imageView = view.findViewById(R.id.contact_explorer_thumbnail);
            imageView.setImageBitmap(bitmap);
        }

        TextView initialLetter = view.findViewById(R.id.contact_explorer_initial_letter);
        initialLetter.setVisibility(View.VISIBLE);
    }

    private static void log(String message) {
        Util.log("ContactsAdapter", message);
    }
}
