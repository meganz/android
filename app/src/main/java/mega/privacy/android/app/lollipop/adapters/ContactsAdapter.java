package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_MEGA_CONTACT;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_MEGA_CONTACT_HEADER;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_PHONE_CONTACT;
import static mega.privacy.android.app.lollipop.ContactInfo.TYPE_PHONE_CONTACT_HEADER;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolderPhoneContactsLollipop> {

    private Context mContext;
    private List<ContactInfo> contactData;
    private LayoutInflater inflater;
    private OnItemClickListener callback;

    public ContactsAdapter(Context context, ArrayList<ContactInfo> phoneContacts, OnItemClickListener callback) {
        this.mContext = context;
        this.contactData = phoneContacts == null ? new ArrayList<ContactInfo>() : phoneContacts;
        this.inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.callback = callback;
    }

    @Override
    public int getItemCount() {
        return contactData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return contactData.get(position).getType();
    }

    public ContactInfo getItem(int position) {
        if (position < contactData.size()) {
            return contactData.get(position);
        }
        return null;
    }

    public void setContactData(ArrayList<ContactInfo> contactData) {
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
        ContactInfo contact = getItem(position);
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
        View rowView = inflater.inflate(R.layout.contact_list_section_header, parentView, false);
        ViewHolderPhoneContactsLollipop holder = new ViewHolderPhoneContactsLollipop(rowView);
        holder.headerTextView = rowView.findViewById(R.id.section_header);
        holder.contactId = -1;
        return holder;
    }

    private ViewHolderPhoneContactsLollipop createContactHolder(ViewGroup parentView) {
        View rowView = inflater.inflate(R.layout.contact_explorer_item, parentView, false);
        ViewHolderPhoneContactsLollipop holder = new ViewHolderPhoneContactsLollipop(rowView);
        holder.contactLayout = rowView.findViewById(R.id.contact_list_item_layout);
        holder.contactNameTextView = rowView.findViewById(R.id.contact_explorer_name);
        holder.phoneEmailTextView = rowView.findViewById(R.id.contact_explorer_phone_mail);
        holder.imageView = rowView.findViewById(R.id.contact_explorer_thumbnail);
        holder.initialLetter = rowView.findViewById(R.id.contact_explorer_initial_letter);
        return holder;
    }

    private void bindHeader(ViewHolderPhoneContactsLollipop holder, ContactInfo contact) {
        holder.headerTextView.setText(contact.getName());
    }

    private void bindContact(ViewHolderPhoneContactsLollipop holder, ContactInfo contact, boolean isMegaContact) {
        holder.contactMail = contact.getEmail();
        holder.contactName = contact.getName();
        holder.contactId = contact.getId();
        holder.contactNameTextView.setText(contact.getName());
        holder.phoneEmailTextView.setText(contact.getEmail());
        holder.contactLayout.setBackgroundColor(Color.WHITE);

        if (!isMegaContact) {
            boolean processFailed = false;
            try {
                InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(),
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, holder.contactId));

                if (inputStream != null) {
                    Bitmap photo = BitmapFactory.decodeStream(inputStream);
                    holder.imageView.setImageBitmap(photo);
                    inputStream.close();
                    holder.initialLetter.setVisibility(View.GONE);
                } else {
                    processFailed = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                processFailed = true;
            }

            if (processFailed) {
                createDefaultAvatar(holder, isMegaContact);
            }
        } else {
            createDefaultAvatar(holder, isMegaContact);
        }
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
            if (callback != null) {
                int position = getAdapterPosition();
                if (position < contactData.size()) {
                    ContactInfo contactInfo = contactData.get(position);
                    if (contactInfo.getType() == TYPE_MEGA_CONTACT || contactInfo.getType() == TYPE_PHONE_CONTACT) {
                        callback.onItemClick(v, position);
                    }
                }
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void createDefaultAvatar(ViewHolderPhoneContactsLollipop holder, boolean isMegaContact) {
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        if (isMegaContact) {
            p.setColor(ContextCompat.getColor(mContext, R.color.lollipop_primary_color));
        } else {
            p.setColor(ContextCompat.getColor(mContext, R.color.color_default_avatar_phone));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        } else {
            radius = defaultAvatar.getHeight() / 2;
        }

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);
        holder.imageView.setImageBitmap(defaultAvatar);


        Display display = ((Activity) mContext).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = mContext.getResources().getDisplayMetrics().density;

        int avatarTextSize = Util.getAvatarTextSize(density);
        log("DENSITY: " + density + ": " + avatarTextSize);
        String initial = "";
        if (isMegaContact &&
                holder.contactMail != null &&
                holder.contactMail.length() > 0) {
            initial = holder.contactMail.charAt(0) + "";

        } else if (holder.contactName != null &&
                holder.contactName.length() > 0) {
            initial = holder.contactName.charAt(0) + "";
        }

        initial = initial.toUpperCase(Locale.getDefault());
        holder.initialLetter.setVisibility(View.VISIBLE);
        holder.initialLetter.setText(initial);
        holder.initialLetter.setTextSize(24);
        holder.initialLetter.setTextColor(Color.WHITE);
    }

    private static void log(String message) {
        Util.log("ContactsAdapter", message);
    }
}
