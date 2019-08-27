package mega.privacy.android.app.lollipop.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.InvitationContactInfo;
import mega.privacy.android.app.utils.FileUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_MEGA_CONTACT;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_MEGA_CONTACT_HEADER;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_PHONE_CONTACT;
import static mega.privacy.android.app.lollipop.InvitationContactInfo.TYPE_PHONE_CONTACT_HEADER;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;

public class InvitationContactsAdapter extends RecyclerView.Adapter<InvitationContactsAdapter.ViewHolderPhoneContactsLollipop> implements MegaRequestListenerInterface {

    private final static String IMAGE_EXTENSION = ".jpg";
    private final static int HEADER_HOLDER_ID = -1;
    private Context context;
    private List<InvitationContactInfo> contactData;
    private LayoutInflater inflater;
    private OnItemClickListener callback;
    private MegaApiAndroid megaApi;

    public InvitationContactsAdapter(Context context, List<InvitationContactInfo> phoneContacts, OnItemClickListener callback, MegaApiAndroid megaApi) {
        this.context = context;
        this.contactData = phoneContacts == null ? new ArrayList<InvitationContactInfo>() : phoneContacts;
        this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.callback = callback;
        this.megaApi = megaApi;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (e.getErrorCode() == MegaError.API_OK) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    public class ViewHolderPhoneContactsLollipop extends RecyclerView.ViewHolder implements View.OnClickListener {
        private RelativeLayout contactLayout;
        private TextView contactNameTextView, displayLabelTextView, headerTextView, initialLetter;
        private RoundedImageView imageView;
        private long contactId;
        private String contactName, displayLabel;

        private ViewHolderPhoneContactsLollipop(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            log("CI contact get clicked");
            int position = getAdapterPosition();
            if (callback != null && position >= 0 && position < contactData.size()) {
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
        InvitationContactInfo item = getItem(position);
        if(item != null) {
            return item.getType();
        }
        return -1;
    }

    public InvitationContactInfo getItem(int position) {
        if (position < contactData.size()) {
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
        holder.contactId = HEADER_HOLDER_ID;
        rowView.setTag(holder);
        return holder;
    }

    private ViewHolderPhoneContactsLollipop createContactHolder(ViewGroup parentView) {
        log("create Contact Holder");
        View rowView = inflater.inflate(R.layout.contact_explorer_item, parentView, false);
        ViewHolderPhoneContactsLollipop holder = new ViewHolderPhoneContactsLollipop(rowView);
        holder.contactLayout = rowView.findViewById(R.id.contact_list_item_layout);
        holder.contactNameTextView = rowView.findViewById(R.id.contact_explorer_name);
        holder.displayLabelTextView = rowView.findViewById(R.id.contact_explorer_phone_mail);
        holder.imageView = rowView.findViewById(R.id.contact_explorer_thumbnail);
        holder.initialLetter = rowView.findViewById(R.id.contact_explorer_initial_letter);
        holder.initialLetter.setVisibility(View.GONE);
        rowView.setTag(holder);
        return holder;
    }

    private void bindHeader(ViewHolderPhoneContactsLollipop holder, InvitationContactInfo contact) {
        holder.headerTextView.setText(contact.getName());
        holder.headerTextView.setTextColor(context.getResources().getColor(R.color.black));
        holder.headerTextView.setBackgroundColor(Color.WHITE);
    }

    private void bindContact(ViewHolderPhoneContactsLollipop holder, InvitationContactInfo contact, boolean isMegaContact) {
        holder.displayLabel = contact.getDisplayInfo();
        holder.contactName = contact.getName();
        holder.contactId = contact.getId();
        holder.contactNameTextView.setText(holder.contactName);
        holder.displayLabelTextView.setText(holder.displayLabel);
        holder.contactLayout.setBackgroundColor(Color.WHITE);

        if (contact.isHighlighted()) {
            setItemHighlighted(holder.contactLayout);
        } else {
            Bitmap bitmap;
            if (isMegaContact) {
                bitmap = getMegaUserAvatar(contact);
            } else {
                bitmap = createPhoneContactBitmap(holder.contactId);
            }

            // create default one if unable to get user pre-set avatar
            if (bitmap == null) {
                log("create default avatar as unable to get user pre-set one");
                String initial = contact.getInitial();
                String color = contact.getAvatarColor();
                bitmap = Util.createDefaultAvatar(color, initial);
            }

            contact.setBitmap(bitmap);
            holder.imageView.setImageBitmap(bitmap);
        }
    }

    private Bitmap createPhoneContactBitmap(long id) {
        log("createPhoneContactBitmap");
        InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
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

    private void setItemHighlighted(View view) {
        log("setItemHighlighted");
        view.setBackgroundColor(context.getResources().getColor(R.color.contactSelected));
        ImageView imageView = view.findViewById(R.id.contact_explorer_thumbnail);
        Bitmap image = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_select_avatar);
        if (image != null) {
            imageView.setImageBitmap(image);
        }
    }

    private void setItemNormal(View view, Bitmap bitmap) {
        log("setItemNormal");
        view.setBackgroundColor(context.getResources().getColor(R.color.white));
        if (bitmap != null) {
            ImageView imageView = view.findViewById(R.id.contact_explorer_thumbnail);
            imageView.setImageBitmap(bitmap);
        }
    }

    private Bitmap getMegaUserAvatar(InvitationContactInfo contact) {
        log("getMegaUserAvatar");
        String email = contact.getDisplayInfo();
        File avatar = buildAvatarFile(context, email + IMAGE_EXTENSION);
        String path = avatar.getAbsolutePath();
        if (FileUtils.isFileAvailable(avatar)) {
            log("avatar exists in: " + avatar.getAbsolutePath());
            BitmapFactory.Options bOpts = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
            if (bitmap == null) {
                avatar.delete();
                megaApi.getUserAvatar(email, path, this);
            } else {
                return bitmap;
            }
        } else {
            megaApi.getUserAvatar(email, path, this);
        }
        return null;
    }

    private static void log(String message) {
        Util.log("InvitationContactsAdapter", message);
    }
}
