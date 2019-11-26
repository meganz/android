package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
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
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.RecentChatsFragmentLollipop;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaContactRequest;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;

public class ContactsHorizontalAdapter extends RecyclerView.Adapter<ContactsHorizontalAdapter.ContactViewHolder> implements View.OnClickListener {

    private Activity context;

    private RecentChatsFragmentLollipop recentChatsFragment;

    private List<MegaContactGetter.MegaContact> contacts;

    private MegaApiAndroid megaApi;

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
        holder.textViewInitialLetter = v.findViewById(R.id.contact_list_initial_letter);
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
        showConfirmSentDialog(holder);
    }

    private void showConfirmSentDialog(final ContactViewHolder holder) {
        final int position = holder.getAdapterPosition();
        final MegaContactGetter.MegaContact contact = contacts.get(position);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        contacts.remove(position);
                        recentChatsFragment.onContactsCountChange(contacts);
                        notifyDataSetChanged();

                        String email = holder.contactMail;
                        logDebug("Sent invite to: " + email);
                        // for UI smoothness, ignore the callback
                        megaApi.inviteContact(email, null, MegaContactRequest.INVITE_ACTION_ADD);
                        showSnackBar(context, SNACKBAR_TYPE, context.getString(R.string.context_contact_request_sent, email), -1);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String message = String.format(context.getString(R.string.title_confirm_send_invitation),contact.getLocalName());
        builder.setMessage(message);
        String invite = context.getResources().getString(R.string.contact_invite).toUpperCase();
        builder.setPositiveButton(invite, dialogClickListener);
        String cancel = context.getResources().getString(R.string.general_cancel).toUpperCase();
        builder.setNegativeButton(cancel, dialogClickListener);
        builder.show();
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactViewHolder holder, int position) {
        final MegaContactGetter.MegaContact megaContact = getItem(position);
        String email = megaContact.getEmail();
        holder.contactMail = email;
        holder.textViewName.setText(megaContact.getLocalName());
        UserAvatarListener listener = new UserAvatarListener(context, holder);
        setDefaultAvatar(megaContact, holder);
        File avatar = buildAvatarFile(context, email + ".jpg");
        Bitmap bitmap;
        if (isFileAvailable(avatar) && avatar.length() > 0) {
            BitmapFactory.Options bOpts = new BitmapFactory.Options();
            bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
            if (bitmap == null) {
                avatar.delete();
                megaApi.getUserAvatar(email, avatar.getAbsolutePath(), listener);
            } else {
                holder.textViewInitialLetter.setVisibility(View.INVISIBLE);
                holder.avatar.setImageBitmap(bitmap);
            }
        } else {
            megaApi.getUserAvatar(email, avatar.getAbsolutePath(), listener);
        }
    }

    public void setDefaultAvatar(MegaContactGetter.MegaContact contact, ContactViewHolder holder) {
        int color = colorAvatar(context, megaApi, contact.getId());
        Bitmap background = getDefaultAvatar(context, color, contact.getLocalName(), AVATAR_SIZE, true);
        holder.avatar.setImageBitmap(background);
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

        public TextView textViewName,textViewInitialLetter;

        ImageView addIcon;

        public RoundedImageView avatar;

        RelativeLayout itemLayout, clickableArea;

        ContactViewHolder(View itemView) {
            super(itemView);
        }
    }
}
