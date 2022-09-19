package mega.privacy.android.app.main.megachat.chatAdapters;

import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.setContactLastGreen;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.main.megachat.ChatExplorerFragment;
import mega.privacy.android.app.main.megachat.ChatExplorerListItem;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;
import timber.log.Timber;

public class MegaListChatExplorerAdapter extends RecyclerView.Adapter<MegaListChatExplorerAdapter.ViewHolderChatExplorerList> implements View.OnClickListener, View.OnLongClickListener, SectionTitleProvider {

    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    ChatController cC;

    ViewHolderChatExplorerList holder;
    RecyclerView listView;

    Context context;
    ArrayList<ChatExplorerListItem> items;
    Object fragment;

    SparseBooleanArray selectedItems;
    public static final int MAX_WIDTH_TITLE_PORT = 240;
    public static final int MAX_WIDTH_TITLE_LAND = 340;

    boolean isSearchEnabled;
    SparseBooleanArray searchSelectedItems;

    public MegaListChatExplorerAdapter(Context _context, Object _fragment, ArrayList<ChatExplorerListItem> _items, RecyclerView _listView) {
        Timber.d("New adapter");
        this.context = _context;
        this.items = _items;
        this.fragment = _fragment;
        this.listView = _listView;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        selectedItems = new SparseBooleanArray();

        cC = new ChatController(context);
    }

    public static class ViewHolderChatExplorerList extends RecyclerView.ViewHolder {
        public ViewHolderChatExplorerList(View arg0) {
            super(arg0);
        }

        RelativeLayout itemLayout;
        RelativeLayout itemContainer;
        RoundedImageView avatarImage;
        EmojiTextView titleText;
        ImageView stateIcon;
        MarqueeTextView lastSeenStateText;
        TextView participantsText;
        RelativeLayout headerLayout;
        TextView headerText;

        String email;

        public String getEmail() {
            return email;
        }

        public void setAvatarImage(Bitmap avatarImage) {
            this.avatarImage.setImageBitmap(avatarImage);
        }
    }

    @Override
    public MegaListChatExplorerAdapter.ViewHolderChatExplorerList onCreateViewHolder(ViewGroup parent, int viewType) {

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_explorer_list, parent, false);
        holder = new ViewHolderChatExplorerList(v);

        holder.headerLayout = v.findViewById(R.id.header_layout);
        holder.headerText = v.findViewById(R.id.label_text);
        holder.itemContainer = v.findViewById(R.id.item_container);
        holder.itemLayout = v.findViewById(R.id.chat_explorer_list_item_layout);
        holder.avatarImage = v.findViewById(R.id.chat_explorer_list_avatar);
        holder.titleText = v.findViewById(R.id.chat_explorer_list_title);
        holder.stateIcon = v.findViewById(R.id.chat_explorer_list_contact_state);
        holder.lastSeenStateText = v.findViewById(R.id.chat_explorer_list_last_seen_state);
        holder.participantsText = v.findViewById(R.id.chat_explorer_list_participants);

        if (isScreenInPortrait(context)) {
            holder.titleText.setMaxWidthEmojis(dp2px(MAX_WIDTH_TITLE_PORT, outMetrics));
        } else {
            holder.titleText.setMaxWidthEmojis(dp2px(MAX_WIDTH_TITLE_LAND, outMetrics));
        }

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(MegaListChatExplorerAdapter.ViewHolderChatExplorerList holder, int position) {

        ChatExplorerListItem item = getItem(position);
        MegaChatListItem chat = item.getChat();

        if (item.isHeader()) {
            holder.itemContainer.setVisibility(View.GONE);
            holder.headerLayout.setVisibility(View.VISIBLE);
            if (item.isRecent()) {
                holder.headerText.setText(R.string.recents_label);
            } else {
                holder.headerText.setText(R.string.chats_label);
            }
            return;
        }

        holder.headerLayout.setVisibility(View.GONE);
        holder.itemContainer.setVisibility(View.VISIBLE);
        holder.titleText.setText(item.getTitle());

        if (chat != null && chat.isGroup()) {

            if ((isItemChecked(position) && !isSearchEnabled()) || (isSearchEnabled() && isSearchItemChecked(position))) {
                holder.avatarImage.setImageResource(R.drawable.ic_chat_avatar_select);
            } else {
                createGroupChatAvatar(holder);
            }
            holder.stateIcon.setVisibility(View.GONE);
            holder.lastSeenStateText.setVisibility(View.GONE);
            holder.participantsText.setVisibility(View.VISIBLE);
            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
            long peerCount = chatRoom.getPeerCount() + 1;
            holder.participantsText.setText(context.getResources().getQuantityString(R.plurals.subtitle_of_group_chat, (int) peerCount, peerCount));
        } else {

            holder.participantsText.setVisibility(View.GONE);
            MegaContactAdapter contact = item.getContact();

            long handle = -1;

            if (chat != null) {
                holder.email = megaChatApi.getContactEmail(chat.getPeerHandle());
                Timber.d("Email: %s", holder.email);
            } else if (contact != null && contact.getMegaUser() != null) {
                holder.email = contact.getMegaUser().getEmail();
            }

            if (contact != null && contact.getMegaUser() != null) {
                handle = contact.getMegaUser().getHandle();
            }

            String userHandleEncoded = MegaApiAndroid.userHandleToBase64(handle);

            if ((isItemChecked(position) && !isSearchEnabled()) || (isSearchEnabled() && isSearchItemChecked(position))) {
                holder.avatarImage.setImageResource(R.drawable.ic_chat_avatar_select);
            } else {
                setUserAvatar(holder, userHandleEncoded);
            }

            int userStatus = megaChatApi.getUserOnlineStatus(handle);
            setContactStatus(userStatus, holder.stateIcon, holder.lastSeenStateText, StatusIconLocation.STANDARD);
            setContactLastGreen(context, userStatus, contact.getLastGreen(), holder.lastSeenStateText);
        }


        holder.itemLayout.setOnClickListener(this);
        holder.itemLayout.setOnLongClickListener(this);
    }

    public void createGroupChatAvatar(ViewHolderChatExplorerList holder) {
        Timber.d("createGroupChatAvatar()");
        String title = holder.titleText.getText().toString();
        holder.avatarImage.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR), title, AVATAR_SIZE, true));
    }

    public void setUserAvatar(ViewHolderChatExplorerList holder, String userHandle) {
        /*Default Avatar*/
        String fullName = holder.titleText.getText().toString();
        if ((fullName == null || fullName.trim().length() <= 0) && holder.email != null && holder.email.length() > 0) {
            fullName = holder.email;
        }
        holder.avatarImage.setImageBitmap(getDefaultAvatar(getColorAvatar(userHandle), fullName, AVATAR_SIZE, true));

        /*Avatar*/
        ChatUserAvatarListener listener = new ChatUserAvatarListener(context, holder);
        File avatar = (holder.email == null) ?
                buildAvatarFile(context, userHandle + ".jpg") :
                buildAvatarFile(context, holder.email + ".jpg");

        Bitmap bitmap = null;
        if (isFileAvailable(avatar)) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();

                    if (megaApi == null) {
                        Timber.w("megaApi is Null in Offline mode");
                        return;
                    }

                    megaApi.getUserAvatar(holder.email, buildAvatarFile(context, holder.email + ".jpg").getAbsolutePath(), listener);
                } else {
                    holder.avatarImage.setImageBitmap(bitmap);
                }
            } else {

                if (megaApi == null) {
                    Timber.w("megaApi is Null in Offline mode");
                    return;
                }

                megaApi.getUserAvatar(holder.email, buildAvatarFile(context, holder.email + ".jpg").getAbsolutePath(), listener);
            }
        } else {

            if (megaApi == null) {
                Timber.w("megaApi is Null in Offline mode");
                return;
            }

            megaApi.getUserAvatar(holder.email, buildAvatarFile(context, holder.email + ".jpg").getAbsolutePath(), listener);
        }
    }

    public ChatExplorerListItem getItem(int position) {
        return items.get(position);
    }

    public ArrayList<ChatExplorerListItem> getItems() {
        return items;
    }

    @Override
    public int getItemCount() {
        if (items != null) {
            return items.size();
        }
        return 0;
    }

    public int getPosition(ChatExplorerListItem item) {
        Timber.d("getPosition");
        return items.indexOf(item);
    }

    @Override
    public void onClick(View v) {
        setClick(v);
    }

    @Override
    public boolean onLongClick(View v) {
        setClick(v);

        return true;
    }

    void setClick(View v) {
        ViewHolderChatExplorerList holder = (ViewHolderChatExplorerList) v.getTag();

        if (v.getId() == R.id.chat_explorer_list_item_layout) {
            ((ChatExplorerFragment) fragment).itemClick(holder.getAdapterPosition());
        }
    }

    @Override
    public String getSectionTitle(int position) {
        if (items != null) {
            if (position >= 0 && position < items.size()) {
                String name = items.get(position).getTitle();
                if (name != null && !name.isEmpty()) {
                    return "" + name.charAt(0);
                }
            }
        }
        return "";
    }

    public void setItems(ArrayList<ChatExplorerListItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void updateItemContactStatus(int position) {
        Timber.d("position: %s", position);

        notifyItemChanged(position);
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    private boolean isSearchItemChecked(int position) {
        return searchSelectedItems != null && searchSelectedItems.get(position);
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

        MegaListChatExplorerAdapter.ViewHolderChatExplorerList view = (MegaListChatExplorerAdapter.ViewHolderChatExplorerList) listView.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
//                    Hide multipleselect
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.avatarImage.startAnimation(flipAnimation);
        } else {
//            Hide multipleselect
        }
    }

    public void setSearchEnabled(boolean searchEnabled) {
        this.isSearchEnabled = searchEnabled;
    }

    public boolean isSearchEnabled() {
        return isSearchEnabled;
    }

    public void setSearchSelectedItems(SparseBooleanArray searchSelectedItems) {
        this.searchSelectedItems = searchSelectedItems;
    }

    /**
     * Clears all the selected items.
     */
    public void clearSelections() {
        if (selectedItems != null) {
            selectedItems.clear();
        }
    }
}
