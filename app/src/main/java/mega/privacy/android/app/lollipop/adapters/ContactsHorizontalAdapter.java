package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ContactsHorizontalAdapter extends RecyclerView.Adapter<ContactsHorizontalAdapter.ContactViewHolder> implements View.OnClickListener {

    private Activity context;

    private RecentChatsFragmentLollipop recentChatsFragment;

    private List<MegaContactGetter.MegaContact> contacts;

    private MegaApiAndroid megaApi;

    private AlertDialog sendInvitationDialog;

    public ContactsHorizontalAdapter(Activity context, RecentChatsFragmentLollipop recentChatsFragment, List<MegaContactGetter.MegaContact> data) {
        this.context = context;
        this.contacts = data;
        if (megaApi == null) {
            megaApi = ((MegaApplication) context.getApplication()).getMegaApi();
        }
        this.recentChatsFragment = recentChatsFragment;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_avatar, parent, false);

        ContactViewHolder holder = new ContactViewHolder(v);
        holder.clickableArea = v.findViewById(R.id.item_layout_add);
        holder.itemLayout = v.findViewById(R.id.chip_layout);
        holder.inviteMore = v.findViewById(R.id.invite_more);
        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidth(px2dp(60, outMetrics));
        holder.avatar = v.findViewById(R.id.add_rounded_avatar);
        holder.addIcon = v.findViewById(R.id.add_icon_chip);
        holder.clickableArea.setOnClickListener(this);
        holder.clickableArea.setTag(holder);
        v.setTag(holder);
        return holder;
    }

    @Override
    public void onClick(View v) {
        ContactViewHolder holder = (ContactViewHolder) v.getTag();
        onContactClicked(holder);
    }

    public void onContactClicked(ContactViewHolder holder) {
        final int position = holder.getAdapterPosition();
        final MegaContactGetter.MegaContact contact = contacts.get(position);
        // If click on 'Invite more'.
        if (isInviteMore(contact)) {
            context.startActivityForResult(new Intent(context, InviteContactActivity.class), REQUEST_INVITE_CONTACT_FROM_DEVICE);
        } else {
            showInvitationDialog(holder, contact);
        }
    }

    public void showInvitationDialog(ContactViewHolder holder, MegaContactGetter.MegaContact contact) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    contacts.remove(contact);
                    recentChatsFragment.onContactsCountChange(contacts);
                    notifyDataSetChanged();

                    String email = holder.contactMail;
                    logDebug("Sent invite to: " + email);
                    // for UI smoothness, ignore the callback
                    megaApi.inviteContact(email, null, MegaContactRequest.INVITE_ACTION_ADD);
                    showSnackbar(context, context.getString(R.string.context_contact_request_sent, email));
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dismissDialog();
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.AppCompatAlertDialogStyle);
        String message = String.format(context.getString(R.string.title_confirm_send_invitation),contact.getLocalName());
        builder.setMessage(message);
        String invite = context.getResources().getString(R.string.contact_invite).toUpperCase();
        builder.setPositiveButton(invite, dialogClickListener);
        String cancel = context.getResources().getString(R.string.general_cancel).toUpperCase();
        builder.setNegativeButton(cancel, dialogClickListener);
        sendInvitationDialog = builder.create();
        sendInvitationDialog.show();
    }

    public void dismissDialog() {
        if(sendInvitationDialog != null) {
            sendInvitationDialog.dismiss();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactViewHolder holder, int position) {
        final MegaContactGetter.MegaContact megaContact = getItem(position);
        // Bind 'Invite more'.
        if (isInviteMore(megaContact)) {
            holder.avatar.setImageDrawable(context.getDrawable(R.drawable.invite_more));
            holder.inviteMore.setVisibility(View.VISIBLE);
            holder.textViewName.setVisibility(View.GONE);
            holder.addIcon.setVisibility(View.GONE);
        } else {
            String email = megaContact.getEmail();
            String localName = megaContact.getLocalName();
            holder.contactMail = email;
            holder.inviteMore.setVisibility(View.GONE);
            holder.textViewName.setVisibility(View.VISIBLE);
            holder.textViewName.setText(localName);

            setImageAvatar(megaContact.getHandle(), email, localName, holder.avatar);
            Bitmap bitmap = getUserAvatar(MegaApiAndroid.userHandleToBase64(megaContact.getHandle()), email);
            if (bitmap == null) {
                UserAvatarListener listener = new UserAvatarListener(context, holder);
                megaApi.getUserAvatar(email, buildAvatarFile(context, email + ".jpg").getAbsolutePath(), listener);
            }
        }
    }

    /**
     * To see if the contact element is the 'Invite more'.
     *
     * @param contact Contact to check.
     * @return true, the contact is 'Invite more', otherwise false.
     */
    private boolean isInviteMore(MegaContactGetter.MegaContact contact) {
        return contact.getEmail() == null
                && contact.getId() == null
                && contact.getLocalName() == null
                && contact.getNormalizedPhoneNumber() == null;
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public MegaContactGetter.MegaContact getItem(int position) {
        logDebug("getItem");
        return contacts.get(position);
    }

    public static class ContactViewHolder extends MegaContactsLollipopAdapter.ViewHolderContacts {

        TextView textViewName, inviteMore;

        ImageView addIcon;

        public RoundedImageView avatar;

        RelativeLayout itemLayout, clickableArea;

        ContactViewHolder(View itemView) {
            super(itemView);
        }
    }
}
