package mega.privacy.android.app.main.megachat.chatAdapters;

import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.getUserStatus;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.ContactUtil.getContactNameDB;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.megachat.ContactAttachmentActivity;
import mega.privacy.android.domain.entity.Contact;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;


public class MegaContactsAttachedAdapter extends RecyclerView.Adapter<MegaContactsAttachedAdapter.ViewHolderContacts> implements OnClickListener, View.OnLongClickListener {

    Context context;
    int positionClicked;
    ArrayList<Contact> contacts;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    boolean multipleSelect;
    private SparseBooleanArray selectedItems;
    SparseBooleanArray selectedContacts;

    private class UserAvatarListenerList implements MegaRequestListenerInterface {

        Context context;
        ViewHolderContacts holder;
        MegaContactsAttachedAdapter adapter;

        public UserAvatarListenerList(Context context, ViewHolderContacts holder, MegaContactsAttachedAdapter adapter) {
            this.context = context;
            this.holder = holder;
            this.adapter = adapter;
        }

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {
            Timber.d("onRequestStart()");
        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
            Timber.d("onRequestFinish()");
            if (e.getErrorCode() == MegaError.API_OK) {
                boolean avatarExists = false;

                if (holder.contactMail.compareTo(request.getEmail()) == 0) {
                    File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
                    Bitmap bitmap = null;
                    if (isFileAvailable(avatar)) {
                        if (avatar.length() > 0) {
                            BitmapFactory.Options bOpts = new BitmapFactory.Options();
                            bOpts.inPurgeable = true;
                            bOpts.inInputShareable = true;
                            bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                            if (bitmap == null) {
                                avatar.delete();
                            } else {
                                avatarExists = true;
                                if (holder instanceof ViewHolderContactsList) {
                                    ((ViewHolderContactsList) holder).imageView.setImageBitmap(bitmap);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onRequestTemporaryError(MegaApiJava api,
                                            MegaRequest request, MegaError e) {
            Timber.w("onRequestTemporaryError");
        }

        @Override
        public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

        }

    }

    public MegaContactsAttachedAdapter(Context _context, ArrayList<Contact> _contacts, RecyclerView _listView) {
        this.context = _context;
        this.positionClicked = -1;
        this.contacts = _contacts;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        listFragment = _listView;
    }

    /*private view holder class*/
    public static class ViewHolderContacts extends RecyclerView.ViewHolder {
        public ViewHolderContacts(View v) {
            super(v);
        }

        EmojiTextView textViewContactName;
        TextView textViewContent;
        ImageView imageButtonThreeDots;
        RelativeLayout itemLayout;
        String contactMail;
        ImageView verifiedIcon;
    }

    public class ViewHolderContactsList extends ViewHolderContacts {
        public ViewHolderContactsList(View v) {
            super(v);
        }

        RoundedImageView imageView;
        ImageView contactStateIcon;
    }

    ViewHolderContactsList holderList = null;

    @Override
    public ViewHolderContacts onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_list, parent, false);

        holderList = new ViewHolderContactsList(v);
        holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.contact_list_item_layout);
        holderList.imageView = (RoundedImageView) v.findViewById(R.id.contact_list_thumbnail);
        holderList.verifiedIcon = v.findViewById(R.id.verified_icon);
        holderList.textViewContactName = v.findViewById(R.id.contact_list_name);
        holderList.textViewContent = (TextView) v.findViewById(R.id.contact_list_content);
        holderList.imageButtonThreeDots = (ImageView) v.findViewById(R.id.contact_list_three_dots);
        holderList.contactStateIcon = (ImageView) v.findViewById(R.id.contact_list_drawable_state);

        if (!isScreenInPortrait(context)) {
            Timber.d("Landscape configuration");
            holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(280, outMetrics));
        } else {
            holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(230, outMetrics));
        }

        holderList.itemLayout.setTag(holderList);
        holderList.itemLayout.setOnClickListener(this);
        holderList.itemLayout.setOnLongClickListener(this);

        v.setTag(holderList);

        return holderList;
    }

    @Override
    public void onBindViewHolder(ViewHolderContacts holder, int position) {
        Timber.d("onBindViewHolder");

        ViewHolderContactsList holderList = (ViewHolderContactsList) holder;
        onBindViewHolderList(holderList, position);
    }

    public void onBindViewHolderList(ViewHolderContactsList holder, int position) {
        holder.imageView.setImageBitmap(null);

        Contact contact = (Contact) getItem(position);

        MegaUser user = megaApi.getContact(contact.getEmail());
        boolean isNotChecked = !multipleSelect || !isItemChecked(position);
        holder.verifiedIcon.setVisibility(isNotChecked && user != null && megaApi.areCredentialsVerified(user) ? View.VISIBLE : View.GONE);

        holder.contactMail = contact.getEmail();

        holder.contactStateIcon.setVisibility(View.VISIBLE);

        setContactStatus(getUserStatus(contact.getUserId()), holder.contactStateIcon, StatusIconLocation.STANDARD);
        holder.textViewContactName.setText(getContactNameDB(contact));

        if (!multipleSelect) {
            createDefaultAvatar(holder, contact);

            UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);

            File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
            Bitmap bitmap = null;
            if (isFileAvailable(avatar)) {
                if (avatar.length() > 0) {
                    BitmapFactory.Options bOpts = new BitmapFactory.Options();
                    bOpts.inPurgeable = true;
                    bOpts.inInputShareable = true;
                    bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                    if (bitmap == null) {
                        avatar.delete();
                        megaApi.getUserAvatar(contact.getEmail(), buildAvatarFile(context, contact.getEmail() + ".jpg").getAbsolutePath(), listener);
                    } else {
                        holder.imageView.setImageBitmap(bitmap);
                    }
                } else {
                    megaApi.getUserAvatar(contact.getEmail(), buildAvatarFile(context, contact.getEmail() + ".jpg").getAbsolutePath(), listener);
                }
            } else {
                megaApi.getUserAvatar(contact.getEmail(), buildAvatarFile(context, contact.getEmail() + ".jpg").getAbsolutePath(), listener);
            }
        } else {

            if (this.isItemChecked(position)) {
                holder.imageView.setImageResource(R.drawable.ic_chat_avatar_select);
            } else {
                createDefaultAvatar(holder, contact);

                UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);

                File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
                Bitmap bitmap = null;
                if (isFileAvailable(avatar)) {
                    if (avatar.length() > 0) {
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                            megaApi.getUserAvatar(contact.getEmail(), buildAvatarFile(context, contact.getEmail() + ".jpg").getAbsolutePath(), listener);
                        } else {
                            holder.imageView.setImageBitmap(bitmap);
                        }
                    } else {
                        megaApi.getUserAvatar(contact.getEmail(), buildAvatarFile(context, contact.getEmail() + ".jpg").getAbsolutePath(), listener);
                    }
                } else {
                    megaApi.getUserAvatar(contact.getEmail(), buildAvatarFile(context, contact.getEmail() + ".jpg").getAbsolutePath(), listener);
                }
            }
        }

        holder.textViewContent.setText(contact.getEmail());

        holder.imageButtonThreeDots.setTag(holder);
        holder.imageButtonThreeDots.setOnClickListener(this);
    }

    public void createDefaultAvatar(ViewHolderContacts holder, Contact contact) {
        int color;
        MegaUser user = megaApi.getMyUser();
        if (user != null && contact.getUserId() == user.getHandle()) {
            color = getColorAvatar(user);
        } else {
            color = getColorAvatar(contact.getUserId());
        }
        String fullName = contact.getFirstName();
        if (holder instanceof ViewHolderContactsList) {
            Bitmap bitmap = getDefaultAvatar(color, fullName, AVATAR_SIZE, true);
            ((ViewHolderContactsList) holder).imageView.setImageBitmap(bitmap);
        }
    }


    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public Object getItem(int position) {
        Timber.d("position: %s", position);
        return contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPositionClicked() {
        return positionClicked;
    }

    public void setPositionClicked(int p) {
        Timber.d("position: %s", p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }

    public void toggleAllSelection(int pos) {
        Timber.d("position: %s", pos);
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }

        Timber.d("Adapter type is LIST");
        MegaContactsAttachedAdapter.ViewHolderContactsList view = (MegaContactsAttachedAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    notifyItemChanged(positionToflip);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        } else {
            Timber.w("NULL view pos: %s", positionToflip);
            notifyItemChanged(pos);
        }
    }

    public void toggleSelection(int pos) {
        Timber.d("position: %s", pos);

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);

        Timber.d("Adapter type is LIST");
        MegaContactsAttachedAdapter.ViewHolderContactsList view = (MegaContactsAttachedAdapter.ViewHolderContactsList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        }
    }

    public void selectAll() {
        for (int i = 0; i < this.getItemCount(); i++) {
            if (!isItemChecked(i)) {
                toggleSelection(i);
            }
        }
    }

    public void clearSelections() {
        Timber.d("clearSelections");
        for (int i = 0; i < this.getItemCount(); i++) {
            if (isItemChecked(i)) {
                toggleAllSelection(i);
            }
        }
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    /*
     * Get list of all selected contacts
     */
    public ArrayList<Contact> getSelectedUsers() {
        ArrayList<Contact> users = new ArrayList<Contact>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {
                Contact u = getContactAt(selectedItems.keyAt(i));
                if (u != null) {
                    users.add(u);
                }
            }
        }
        return users;
    }

    /*
     * Get contact at specified position
     */
    public Contact getContactAt(int position) {
        Contact contact = null;
        try {
            if (contacts != null) {
                contact = contacts.get(position);
                return contact;
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        if (!isOnline(context)) {
            if (context instanceof ManagerActivity) {
                ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            }
            return;
        }

        ViewHolderContacts holder = (ViewHolderContacts) v.getTag();
        int currentPosition = holder.getAdapterPosition();
        Contact c = (Contact) getItem(currentPosition);

        switch (v.getId()) {
            case R.id.contact_list_three_dots:
            case R.id.contact_grid_three_dots: {
                Timber.d("Click contact three dots!");
                if (!multipleSelect) {
                    if ((c.getEmail().equals(megaChatApi.getMyEmail()))) {
                        ((ContactAttachmentActivity) context).showSnackbar(context.getString(R.string.contact_is_me));
                    } else {
                        ((ContactAttachmentActivity) context).showOptionsPanel(c.getEmail());
                    }
                }
                break;
            }
            case R.id.contact_list_item_layout:
            case R.id.contact_grid_item_layout: {
                Timber.d("contact_item_layout");
                ((ContactAttachmentActivity) context).itemClick(currentPosition);
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        Timber.d("OnLongCLick");

        ViewHolderContacts holder = (ViewHolderContacts) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        return true;
    }

    public void setSelectedContacts(SparseBooleanArray selectedContacts) {
        this.selectedContacts = selectedContacts;
        notifyDataSetChanged();
    }

    public Contact getDocumentAt(int position) {
        Contact megaContactAdapter = null;
        if (position < contacts.size()) {
            megaContactAdapter = contacts.get(position);
            return megaContactAdapter;
        }

        return null;
    }

    public void setContacts(ArrayList<Contact> contacts) {
        Timber.d("setContacts");
        this.contacts = contacts;
        positionClicked = -1;
        notifyDataSetChanged();
    }

    public RecyclerView getListFragment() {
        return listFragment;
    }

    public void setListFragment(RecyclerView listFragment) {
        this.listFragment = listFragment;
    }

    public void updateContact(Contact contactDB, int position) {
        if (position >= 0 && position < getItemCount()) {
            contacts.set(position, contactDB);
            notifyItemChanged(position);
        }
    }
}
