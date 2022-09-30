package mega.privacy.android.app.main.megachat.chatAdapters;

import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.CallUtil.isStatusConnected;
import static mega.privacy.android.app.utils.CallUtil.milliSecondsToTimer;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.converterShortCodes;
import static mega.privacy.android.app.utils.ChatUtil.getInvalidMetaMessage;
import static mega.privacy.android.app.utils.ChatUtil.getMaxAllowed;
import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.ChatUtil.getUserStatus;
import static mega.privacy.android.app.utils.ChatUtil.getVoiceClipDuration;
import static mega.privacy.android.app.utils.ChatUtil.isEnableChatNotifications;
import static mega.privacy.android.app.utils.ChatUtil.isVoiceClip;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.ChatUtil.transformSecondsInString;
import static mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT;
import static mega.privacy.android.app.utils.TimeUtils.DATE_SHORT_SHORT_FORMAT;
import static mega.privacy.android.app.utils.TimeUtils.formatDate;
import static mega.privacy.android.app.utils.TimeUtils.formatDateAndTime;
import static mega.privacy.android.app.utils.Util.mutateIconSecondary;
import static mega.privacy.android.app.utils.Util.toCDATA;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_JOINING;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.components.textFormatter.TextFormatterViewCompat;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.adapters.RotatableAdapter;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.main.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.main.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.main.megachat.ChatExplorerActivity;
import mega.privacy.android.app.main.megachat.ChatExplorerFragment;
import mega.privacy.android.app.main.megachat.RecentChatsFragment;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.TextUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatGiphy;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import timber.log.Timber;

public class MegaListChatAdapter extends RecyclerView.Adapter<MegaListChatAdapter.ViewHolderChatList> implements OnClickListener, View.OnLongClickListener, SectionTitleProvider, RotatableAdapter {

    public static final int ITEM_VIEW_TYPE_NORMAL_CHATS = 0;
    public static final int ITEM_VIEW_TYPE_ARCHIVED_CHATS = 1;

    public static final int ADAPTER_RECENT_CHATS = 0;
    public static final int ADAPTER_ARCHIVED_CHATS = 1;
    public static final int MAX_WIDTH_TITLE_PORT = 190;
    public static final int MAX_WIDTH_CONTENT_PORT = 200;
    public static final int MAX_WIDTH_TITLE_LAND = 400;
    public static final int MAX_WIDTH_CONTENT_LAND = 410;
    public static final int LAST_MSG_LOADING = 255;

    Context context;
    int positionClicked;
    ArrayList<MegaChatListItem> chats;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    boolean multipleSelect = false;
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    Object fragment;

    DisplayMetrics outMetrics;
    ChatController cC;

    int adapterType;

    public MegaListChatAdapter(Context _context, Object _fragment,
                               ArrayList<MegaChatListItem> _chats, RecyclerView _listView,
                               int type) {
        Timber.d("New adapter");
        this.context = _context;
        this.chats = _chats;
        this.positionClicked = -1;
        this.fragment = _fragment;
        this.adapterType = type;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        listFragment = _listView;

        cC = new ChatController(context);

        if (context instanceof ChatExplorerActivity || context instanceof FileExplorerActivity) {
            multipleSelect = true;
        }
    }

    /*public view holder class*/
    public static class ViewHolderChatList extends ViewHolder {
        public ViewHolderChatList(View arg0) {
            super(arg0);
        }

        RelativeLayout itemLayout;
    }

    public static class ViewHolderNormalChatList extends ViewHolderChatList {
        public ViewHolderNormalChatList(View arg0) {
            super(arg0);
        }

        RoundedImageView imageView;
        EmojiTextView textViewContactName;
        EmojiTextView textViewContent;
        LinearLayout voiceClipOrLocationLayout;
        TextView voiceClipOrLocationText;
        ImageView voiceClipOrLocationIc;
        TextView textViewDate;

        String textFastScroller = "";
        ImageButton imageButtonThreeDots;
        RelativeLayout circlePendingMessages;

        TextView numberPendingMessages;
        ImageView muteIcon;
        ImageView contactStateIcon;
        ImageView privateChatIcon;
        ImageView callInProgressIcon;
        String contactMail;
        String fullName = "";

        public int currentPosition;
        public long userHandle;
        public boolean nameRequestedAction = false;

        public String getContactMail() {
            return contactMail;
        }

        public void setImageView(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }

    public static class ViewHolderArchivedChatList extends ViewHolderChatList {
        public ViewHolderArchivedChatList(View arg0) {
            super(arg0);
        }

        TextView textViewArchived;
    }

    ViewHolderChatList holder;

    @Override
    public void onBindViewHolder(ViewHolderChatList holder, int position) {
        final int itemType = getItemViewType(position);
        Timber.d("position: %d, itemType: %d", position, itemType);

        if (itemType == ITEM_VIEW_TYPE_NORMAL_CHATS) {
            MegaChatListItem chat = (MegaChatListItem) getItem(position);

            setTitle(position, holder);

            ((ViewHolderNormalChatList) holder).userHandle = -1;

            if (!chat.isGroup()) {
                Timber.d("Chat one to one");
                long contactHandle = chat.getPeerHandle();
                String userHandleEncoded = MegaApiAndroid.userHandleToBase64(contactHandle);

                ((ViewHolderNormalChatList) holder).contactMail = megaChatApi.getContactEmail(contactHandle);

                if (isItemChecked(position)) {
                    ((ViewHolderNormalChatList) holder).imageView.setImageResource(R.drawable.ic_chat_avatar_select);
                } else {
                    holder.itemLayout.setBackground(null);
                    setUserAvatar(holder, userHandleEncoded);
                }

                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
                ((ViewHolderNormalChatList) holder).privateChatIcon.setVisibility(View.VISIBLE);
                ((ViewHolderNormalChatList) holder).contactStateIcon.setVisibility(View.VISIBLE);

                if (outMetrics == null) {
                    Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
                    outMetrics = new DisplayMetrics();
                    display.getMetrics(outMetrics);
                }

                setStatus(position, holder);
            } else {
                Timber.d("Group chat");
                ((ViewHolderNormalChatList) holder).contactStateIcon.setVisibility(View.GONE);

                if (chat.isPublic()) {
                    ((ViewHolderNormalChatList) holder).privateChatIcon.setVisibility(View.GONE);
                } else {
                    ((ViewHolderNormalChatList) holder).privateChatIcon.setVisibility(View.VISIBLE);
                }

                if (isItemChecked(position)) {
                    ((ViewHolderNormalChatList) holder).imageView.setImageResource(R.drawable.ic_chat_avatar_select);
                } else {
                    holder.itemLayout.setBackground(null);
                    createGroupChatAvatar(holder, getTitleChat(chat));
                }
            }

            setPendingMessages(position, holder);

            setTs(position, holder);

            setLastMessage(position, holder);

            checkMuteIcon(position, ((ViewHolderNormalChatList) holder), chat);

            if (context instanceof ChatExplorerActivity || context instanceof FileExplorerActivity) {

                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setVisibility(View.GONE);
                if (chat.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chat.getOwnPrivilege() == MegaChatRoom.PRIV_RO) {
                    ((ViewHolderNormalChatList) holder).imageView.setAlpha(.4f);

                    holder.itemLayout.setOnClickListener(null);
                    holder.itemLayout.setOnLongClickListener(null);

                    ((ViewHolderNormalChatList) holder).circlePendingMessages.setAlpha(.4f);

                    int textColor = ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary);
                    ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(textColor);
                    ((ViewHolderNormalChatList) holder).textViewDate.setTextColor(textColor);
                    ((ViewHolderNormalChatList) holder).textViewContactName.setTextColor(textColor);
                } else {
                    ((ViewHolderNormalChatList) holder).imageView.setAlpha(1.0f);

                    ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setTag(holder);

                    holder.itemLayout.setOnClickListener(this);
                    holder.itemLayout.setOnLongClickListener(null);

                    ((ViewHolderNormalChatList) holder).circlePendingMessages.setAlpha(1.0f);

                    ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                    ((ViewHolderNormalChatList) holder).textViewDate.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                    ((ViewHolderNormalChatList) holder).textViewContactName.setTextColor(ContextCompat.getColor(context, R.color.black));
                }
            } else {
                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setVisibility(View.VISIBLE);

                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setTag(holder);

                holder.itemLayout.setOnClickListener(this);
                holder.itemLayout.setOnLongClickListener(this);
            }
        } else if (itemType == ITEM_VIEW_TYPE_ARCHIVED_CHATS) {
            if (context instanceof ManagerActivity && ((ManagerActivity) context).isSearchOpen()) {
                holder.itemView.setVisibility(View.GONE);
                return;
            }

            holder.itemView.setVisibility(View.VISIBLE);
            ((ViewHolderArchivedChatList) holder).textViewArchived.setOnClickListener(this);
            ((ViewHolderArchivedChatList) holder).textViewArchived.setTag(holder);

            holder.itemLayout.setOnClickListener(null);
            holder.itemLayout.setOnLongClickListener(null);

            ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();
            if (archivedChats != null) {
                ((ViewHolderArchivedChatList) holder).textViewArchived.setText(context.getString(R.string.archived_chats_show_option, archivedChats.size()));
            } else {
                ((ViewHolderArchivedChatList) holder).textViewArchived.setText(context.getString(R.string.archived_chats_title_section));
            }
        }
    }

    /**
     * Method to get the holder.
     *
     * @param position Position in the adapter.
     * @return The ViewHolderNormalChatList in this position.
     */
    private ViewHolderNormalChatList getHolder(int position) {
        return (ViewHolderNormalChatList) listFragment.findViewHolderForAdapterPosition(position);
    }

    private void checkMuteIcon(int position, ViewHolderNormalChatList holder, final MegaChatListItem chat) {
        if (holder == null) {
            holder = getHolder(position);
        }

        if (holder == null)
            return;

        if (!(context instanceof ManagerActivity)) {
            holder.muteIcon.setVisibility(View.GONE);
            return;
        }

        holder.muteIcon.setVisibility(isEnableChatNotifications(chat.getChatId()) ? View.GONE : View.VISIBLE);
    }

    /**
     * Method for updating the UI when the Dnd changes.
     *
     * @param position The position in adapter.
     */
    public void updateMuteIcon(int position) {
        MegaChatListItem chat = getChatAt(position);
        if (chat == null)
            return;

        ViewHolderNormalChatList holder = getHolder(position);
        if (holder != null) {
            checkMuteIcon(position, holder, chat);
        } else {
            notifyItemChanged(position);
        }
    }

    public void setUserAvatar(ViewHolderChatList holder, String userHandle) {

        /*Default Avatar*/
        String name = null;
        if (((ViewHolderNormalChatList) holder).fullName != null && ((ViewHolderNormalChatList) holder).fullName.trim().length() > 0) {
            name = ((ViewHolderNormalChatList) holder).fullName;
        } else if (((ViewHolderNormalChatList) holder).contactMail != null && ((ViewHolderNormalChatList) holder).contactMail.length() > 0) {
            name = ((ViewHolderNormalChatList) holder).contactMail;
        }
        ((ViewHolderNormalChatList) holder).imageView.setImageBitmap(getDefaultAvatar(getColorAvatar(userHandle), name, AVATAR_SIZE, true));

        /*Avatar*/
        ChatUserAvatarListener listener = new ChatUserAvatarListener(context, holder);
        File avatar = ((ViewHolderNormalChatList) holder).contactMail == null ?
                buildAvatarFile(context, userHandle + ".jpg") :
                buildAvatarFile(context, ((ViewHolderNormalChatList) holder).contactMail + ".jpg");

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

                    megaApi.getUserAvatar(((ViewHolderNormalChatList) holder).contactMail, buildAvatarFile(context, ((ViewHolderNormalChatList) holder).contactMail + ".jpg").getAbsolutePath(), listener);
                } else {
                    ((ViewHolderNormalChatList) holder).imageView.setImageBitmap(bitmap);
                }
            } else {

                if (megaApi == null) {
                    Timber.w("megaApi is Null in Offline mode");
                    return;
                }
                megaApi.getUserAvatar(((ViewHolderNormalChatList) holder).contactMail, buildAvatarFile(context, ((ViewHolderNormalChatList) holder).contactMail + ".jpg").getAbsolutePath(), listener);
            }
        } else {

            if (megaApi == null) {
                Timber.w("megaApi is Null in Offline mode");
                return;
            }
            megaApi.getUserAvatar(((ViewHolderNormalChatList) holder).contactMail, buildAvatarFile(context, ((ViewHolderNormalChatList) holder).contactMail + ".jpg").getAbsolutePath(), listener);
        }
    }

    public String formatStringDuration(int duration) {

        if (duration > 0) {
            int hours = duration /
                    3600;
            int minutes = (duration % 3600) / 60;
            int seconds = duration % 60;

            String timeString;
            if (hours > 0) {
                timeString = " %d " + context.getResources().getString(R.string.initial_hour) + " %d " + context.getResources().getString(R.string.initial_minute);
                timeString = String.format(timeString, hours, minutes);
            } else if (minutes > 0) {
                timeString = " %d " + context.getResources().getString(R.string.initial_minute) + " %02d " + context.getResources().getString(R.string.initial_second);
                timeString = String.format(timeString, minutes, seconds);
            } else {
                timeString = " %02d " + context.getResources().getString(R.string.initial_second);
                timeString = String.format(timeString, seconds);
            }
            return timeString;
        }
        return "0";
    }

    @Override
    public ViewHolderChatList onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = null;

        if (viewType == ITEM_VIEW_TYPE_NORMAL_CHATS) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_chat_list, parent, false);
            holder = new ViewHolderNormalChatList(v);
            holder.itemLayout = v.findViewById(R.id.recent_chat_list_item_layout);
            ((ViewHolderNormalChatList) holder).muteIcon = v.findViewById(R.id.recent_chat_list_mute_icon);

            ((ViewHolderNormalChatList) holder).imageView = v.findViewById(R.id.recent_chat_list_thumbnail);
            ((ViewHolderNormalChatList) holder).textViewContactName = v.findViewById(R.id.recent_chat_list_name);
            ((ViewHolderNormalChatList) holder).textViewContent = v.findViewById(R.id.recent_chat_list_content);
            TextFormatterViewCompat.applyFormatting(((ViewHolderNormalChatList) holder).textViewContent);

            ((ViewHolderNormalChatList) holder).textViewContent.setNeccessaryShortCode(false);

            ((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout = v.findViewById(R.id.last_message_voice_clip_or_location);
            ((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout.setVisibility(View.GONE);
            ((ViewHolderNormalChatList) holder).voiceClipOrLocationText = v.findViewById(R.id.last_message_voice_clip_or_location_text);
            ((ViewHolderNormalChatList) holder).voiceClipOrLocationIc = v.findViewById(R.id.last_message_voice_clip_or_location_ic);

            ((ViewHolderNormalChatList) holder).textViewDate = v.findViewById(R.id.recent_chat_list_date);

            ((ViewHolderNormalChatList) holder).imageButtonThreeDots = v.findViewById(R.id.recent_chat_list_three_dots);

            if ((context instanceof ManagerActivity) || (context instanceof ArchivedChatsActivity)) {
                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setOnClickListener(this);
            } else {
                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setVisibility(View.GONE);
                ((ViewHolderNormalChatList) holder).imageButtonThreeDots.setOnClickListener(null);
            }

            ((ViewHolderNormalChatList) holder).circlePendingMessages = (RelativeLayout) v.findViewById(R.id.recent_chat_list_unread_circle);
            ((ViewHolderNormalChatList) holder).numberPendingMessages = (TextView) v.findViewById(R.id.recent_chat_list_unread_number);

            ((ViewHolderNormalChatList) holder).contactStateIcon = (ImageView) v.findViewById(R.id.recent_chat_list_contact_state);
            ((ViewHolderNormalChatList) holder).privateChatIcon = (ImageView) v.findViewById(R.id.recent_chat_list_private_icon);

            ((ViewHolderNormalChatList) holder).callInProgressIcon = (ImageView) v.findViewById(R.id.recent_chat_list_call_in_progress);
            ((ViewHolderNormalChatList) holder).callInProgressIcon.setVisibility(View.GONE);

        } else if (viewType == ITEM_VIEW_TYPE_ARCHIVED_CHATS) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archived_chat_option_list, parent, false);
            holder = new ViewHolderArchivedChatList(v);
            holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_archived_chat_option_list_layout);

            ((ViewHolderArchivedChatList) holder).textViewArchived = (TextView) v.findViewById(R.id.archived_chat_option_text);
        }

        v.setTag(holder);

        return holder;
    }

    public void setUnreadCount(int unreadMessages, ViewHolderChatList holder) {
        Timber.d("unreadMessages: %s", unreadMessages);

        Bitmap image = null;
        String numberString = "";

        int heightPendingMessageIcon = (int) context.getResources().getDimension(R.dimen.width_image_pending_message_one_digit);

        if (unreadMessages < 0) {
            unreadMessages = Math.abs(unreadMessages);
            Timber.d("Unread number: %s", unreadMessages);
            numberString = "+" + unreadMessages;
        } else {
            numberString = unreadMessages + "";
        }

        int size = numberString.length();

        ((ViewHolderNormalChatList) holder).circlePendingMessages.setVisibility(View.VISIBLE);
        switch (size) {
            case 0: {
                Timber.w("0 digits - error!");
                ((ViewHolderNormalChatList) holder).circlePendingMessages.setVisibility(View.GONE);
                break;
            }
            case 1: {
                ((ViewHolderNormalChatList) holder).circlePendingMessages.setBackgroundResource(R.drawable.bg_unread_1);
                break;
            }
            case 2: {
                ((ViewHolderNormalChatList) holder).circlePendingMessages.setBackgroundResource(R.drawable.bg_unread_2);
                break;
            }
            case 3: {
                ((ViewHolderNormalChatList) holder).circlePendingMessages.setBackgroundResource(R.drawable.bg_unread_3);
                break;
            }
            default: {
                ((ViewHolderNormalChatList) holder).circlePendingMessages.setBackgroundResource(R.drawable.bg_unread_4);
                break;
            }
        }
        ((ViewHolderNormalChatList) holder).numberPendingMessages.setText(numberString);
    }

    public void createGroupChatAvatar(ViewHolderChatList holder, String chatTitle) {
        ((ViewHolderNormalChatList) holder).imageView.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR), chatTitle, AVATAR_SIZE, true));
    }

    @Override
    public int getItemCount() {

        if (context instanceof ManagerActivity) {
            ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();

            if (archivedChats != null && archivedChats.size() > 0) {
                return chats.size() + 1;
            } else {
                return chats.size();
            }
        } else {
            return chats.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= chats.size()) {
            return ITEM_VIEW_TYPE_ARCHIVED_CHATS;
        } else {
            return ITEM_VIEW_TYPE_NORMAL_CHATS;
        }
    }

    public boolean isMultipleSelect() {
        Timber.d("isMultipleSelect");
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        Timber.d("setMultipleSelect");
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
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

        if (!(listFragment.findViewHolderForLayoutPosition(pos) instanceof ViewHolderNormalChatList)) {
            return;
        }

        ViewHolderNormalChatList view = (ViewHolderNormalChatList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    hideMultipleSelect();
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

        final boolean delete;
        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
            delete = true;
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
            delete = false;
        }

        if (!(listFragment.findViewHolderForLayoutPosition(pos) instanceof ViewHolderNormalChatList)) {
            return;
        }

        ViewHolderNormalChatList view = (ViewHolderNormalChatList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (!delete) {
                        notifyItemChanged(pos);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    hideMultipleSelect();
                    if (delete) {
                        notifyItemChanged(pos);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            view.imageView.startAnimation(flipAnimation);
        } else {
            hideMultipleSelect();
            notifyItemChanged(pos);
        }
    }

    private void hideMultipleSelect() {
        if (selectedItems.size() <= 0
                && (context instanceof ManagerActivity || context instanceof ArchivedChatsActivity)) {
            ((RecentChatsFragment) fragment).hideMultipleSelect();
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
        if (selectedItems != null) {
            Timber.d("get SelectedItems");
            List<Integer> items = new ArrayList<Integer>(selectedItems.size());
            for (int i = 0; i < selectedItems.size(); i++) {
                items.add(selectedItems.keyAt(i));
            }
            return items;
        } else {
            return null;
        }
    }

    @Override
    public int getFolderCount() {
        return 0;
    }

    @Override
    public int getPlaceholderCount() {
        return 0;
    }

    @Override
    public int getUnhandledItem() {
        return -1;
    }

    /*
     * Get request at specified position
     */
    public MegaChatListItem getChatAt(int position) {
        try {
            if (chats != null) {
                return chats.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    /*
     * Get list of all selected chats
     */
    public ArrayList<MegaChatListItem> getSelectedChats() {
        ArrayList<MegaChatListItem> chats = new ArrayList<MegaChatListItem>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {
                MegaChatListItem r = getChatAt(selectedItems.keyAt(i));
                if (r != null) {
                    chats.add(r);
                }
            }
        }
        return chats;
    }

    public Object getItem(int position) {
        return chats.get(position);
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

    @Override
    public void onClick(View v) {
        ViewHolderChatList holder = (ViewHolderChatList) v.getTag();

        switch (v.getId()) {
            case R.id.recent_chat_list_three_dots: {
                int currentPosition = holder.getAdapterPosition();
                Timber.d("Current position: %s", currentPosition);
                MegaChatListItem c = (MegaChatListItem) getItem(currentPosition);
                if (context instanceof ManagerActivity) {

                    if (multipleSelect) {
                        ((RecentChatsFragment) fragment).itemClick(currentPosition);
                    } else {
                        ((ManagerActivity) context).showChatPanel(c);
                    }
                } else if (context instanceof ArchivedChatsActivity) {
                    if (multipleSelect) {
                        ((RecentChatsFragment) fragment).itemClick(currentPosition);
                    } else {
                        ((ArchivedChatsActivity) context).showChatPanel(c);
                    }
                }

                break;
            }
            case R.id.recent_chat_list_item_layout: {
                Timber.d("Click layout!");
                int currentPosition = holder.getAdapterPosition();
                Timber.d("Current position: %s", currentPosition);
                MegaChatListItem c = (MegaChatListItem) getItem(currentPosition);

                if (context instanceof ManagerActivity) {
                    ((RecentChatsFragment) fragment).itemClick(currentPosition);
                } else if (context instanceof ChatExplorerActivity || context instanceof FileExplorerActivity) {
                    ((ChatExplorerFragment) fragment).itemClick(currentPosition);
                } else if (context instanceof ArchivedChatsActivity) {
                    ((RecentChatsFragment) fragment).itemClick(currentPosition);
                }

                break;
            }
            case R.id.archived_chat_option_text: {
                Timber.d("Show archived chats");

                Intent archivedChatsIntent = new Intent(context, ArchivedChatsActivity.class);
                context.startActivity(archivedChatsIntent);
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        Timber.d("OnLongCLick");
        ViewHolderChatList holder = (ViewHolderChatList) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        if (context instanceof ManagerActivity || context instanceof ArchivedChatsActivity) {
            ((RecentChatsFragment) fragment).activateActionMode();
            ((RecentChatsFragment) fragment).itemClick(currentPosition);
        }

        return true;
    }


    public void updateNonContactName(int pos, long userHandle) {
        Timber.d("updateNonContactName: %d_%d", pos, userHandle);
        ViewHolderNormalChatList view = (ViewHolderNormalChatList) listFragment.findViewHolderForLayoutPosition(pos);

        if (view != null) {
            if (view.userHandle == userHandle) {
                notifyItemChanged(pos);
            }
        }
    }

    public void setStatus(int position, ViewHolderChatList holder) {
        Timber.d("position: %s", position);

        if (holder != null) {
            MegaChatListItem chat = chats.get(position);
            setContactStatus(getUserStatus(chat.getPeerHandle()), ((ViewHolderNormalChatList) holder).contactStateIcon, StatusIconLocation.STANDARD);
        } else {
            Timber.w("Holder is NULL: %s", position);
            notifyItemChanged(position);
        }
    }


    public void updateContactStatus(int position, long userHandle, int state) {
        holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            setContactStatus(megaChatApi.getUserOnlineStatus(userHandle), ((ViewHolderNormalChatList) holder).contactStateIcon, StatusIconLocation.STANDARD);
        } else {
            Timber.w("Holder is NULL");
            notifyItemChanged(position);
        }
    }

    public void setTitle(int position, ViewHolderChatList holder) {
        Timber.d("position: %s", position);
        if (holder == null) {
            holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
        }

        if (holder != null) {

            MegaChatListItem chat = chats.get(position);
            String title = getTitleChat(chat);

            if (title != null) {
                Timber.d("ChatRoom id: %s", chat.getChatId());
                Timber.d("chat timestamp: %s", chat.getLastTimestamp());
                String date = formatDateAndTime(context, chat.getLastTimestamp(), DATE_LONG_FORMAT);
                Timber.d("date timestamp: %s", date);
                int maxAllowed = getMaxAllowed(title);
                ((ViewHolderNormalChatList) holder).textViewContactName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxAllowed)});
                title = converterShortCodes(title);
                ((ViewHolderNormalChatList) holder).textViewContactName.setText(title);

                if (!chat.isGroup()) {
                    ((ViewHolderNormalChatList) holder).fullName = title;
                } else {
                    createGroupChatAvatar(holder, title);
                }
            }
        } else {
            Timber.w("Holder is NULL: %s", position);
            notifyItemChanged(position);
        }
    }

    public void setTs(int position, ViewHolderChatList holder) {
        Timber.d("position: %s", position);

        if (holder == null) {
            holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
        }

        if (holder != null) {
            MegaChatListItem chat = chats.get(position);

            int messageType = chat.getLastMessageType();

            if (messageType == MegaChatMessage.TYPE_INVALID) {
                ((ViewHolderNormalChatList) holder).textViewDate.setVisibility(View.GONE);
            } else {
                Timber.d("ChatRoom ID: %s", chat.getChatId());
                Timber.d("Chat timestamp: %s", chat.getLastTimestamp());
                String date = formatDateAndTime(context, chat.getLastTimestamp(), DATE_LONG_FORMAT);
                String dateFS = formatDate(chat.getLastTimestamp(), DATE_SHORT_SHORT_FORMAT);
                Timber.d("Date timestamp: %s", date);
                ((ViewHolderNormalChatList) holder).textViewDate.setText(date);
                ((ViewHolderNormalChatList) holder).textFastScroller = dateFS;
                ((ViewHolderNormalChatList) holder).textViewDate.setVisibility(View.VISIBLE);
            }
        } else {
            Timber.w("Holder is NULL: %s", position);
            notifyItemChanged(position);
        }
    }

    public void setPendingMessages(int position, ViewHolderChatList holder) {
        Timber.d("position: %s", position);
        if (holder == null) {
            holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
        }

        if (holder != null) {
            MegaChatListItem chat = chats.get(position);
            int unreadMessages = chat.getUnreadCount();
            Timber.d("Unread messages: %s", unreadMessages);
            if (chat.getUnreadCount() != 0) {
                setUnreadCount(unreadMessages, holder);
            } else {
                ((ViewHolderNormalChatList) holder).circlePendingMessages.setVisibility(View.GONE);
            }
        } else {
            Timber.w("Holder is NULL: %s", position);
            notifyItemChanged(position);
        }
    }

    /**
     * Method for displaying the appropriate string when there is a call in a chat
     *
     * @param holder The ViewHolderChatList.
     * @param chat   The MegaChatListItem
     * @return True, if there is a call in that chat. False, if not
     */
    private boolean updateLastCallMessage(ViewHolderChatList holder, MegaChatListItem chat) {
        if (holder == null || megaChatApi.getNumCalls() == 0)
            return false;

        MegaChatCall call = megaChatApi.getChatCall(chat.getChatId());
        if (call == null || !isStatusConnected(context, chat.getChatId()))
            return false;

        switch (call.getStatus()) {
            case CALL_STATUS_TERMINATING_USER_PARTICIPATION:
            case CALL_STATUS_USER_NO_PRESENT:
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                if (call.isRinging()) {
                    ((ViewHolderNormalChatList) holder).textViewContent.setText(context.getString(R.string.notification_subtitle_incoming));
                } else {
                    ((ViewHolderNormalChatList) holder).callInProgressIcon.setVisibility(View.VISIBLE);
                    ((ViewHolderNormalChatList) holder).textViewContent.setText(context.getString(R.string.ongoing_call_messages));
                }
                return true;

            case CALL_STATUS_JOINING:
            case CALL_STATUS_IN_PROGRESS:
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                ((ViewHolderNormalChatList) holder).textViewContent.setText(context.getString(MegaApplication.getChatManagement().isRequestSent(call.getCallId()) ?
                        R.string.outgoing_call_starting :
                        R.string.call_started_messages));
                return true;
        }

        return false;
    }

    public void setLastMessage(int position, ViewHolderChatList holder) {
        Timber.d("position: %s", position);
        if (holder == null) {
            holder = (ViewHolderChatList) listFragment.findViewHolderForAdapterPosition(position);
        }

        if (holder != null) {
            ((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout.setVisibility(View.GONE);
            ((ViewHolderNormalChatList) holder).callInProgressIcon.setVisibility(View.GONE);

            MegaChatListItem chat = chats.get(position);
            if (updateLastCallMessage(holder, chat)) {
                Timber.d("Exist a call in position %s", position);
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, R.attr.colorSecondary));
                ((ViewHolderNormalChatList) holder).textViewContent.setVisibility(View.VISIBLE);
                return;
            }

            int messageType = chat.getLastMessageType();
            MegaChatMessage lastMessage = megaChatApi.getMessage(chat.getChatId(), chat.getLastMessageId());
            Timber.d("MessageType: %s", messageType);
            String lastMessageString = converterShortCodes(chat.getLastMessage());

            if (messageType == MegaChatMessage.TYPE_INVALID) {
                Timber.d("Message Type -> INVALID");
                ((ViewHolderNormalChatList) holder).textViewContent.setText(context.getString(R.string.no_conversation_history));
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                ((ViewHolderNormalChatList) holder).textViewDate.setVisibility(View.GONE);
                return;
            }

            if (messageType == LAST_MSG_LOADING) {
                Timber.d("Message Type -> LOADING");
                ((ViewHolderNormalChatList) holder).textViewContent.setText(context.getString(R.string.general_loading));
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                ((ViewHolderNormalChatList) holder).textViewDate.setVisibility(View.GONE);
                return;
            }

            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
            if (chatRoom == null) {
                Timber.e("The chat room is null");
                return;
            }

            if (messageType == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) {
                Timber.d("Message Type -> TYPE_ALTER_PARTICIPANTS");

                String textToShow = cC.createManagementString(lastMessage, chatRoom);
                ((ViewHolderNormalChatList) holder).textViewContent.setText(textToShow);
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_PRIV_CHANGE) {
                String textToShow = cC.createManagementString(lastMessage, chatRoom);
                ((ViewHolderNormalChatList) holder).textViewContent.setText(textToShow);
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_TRUNCATE) {
                Timber.d("Message type TRUNCATE");

                String textToShow = cC.createManagementString(lastMessage, chatRoom);
                ((ViewHolderNormalChatList) holder).textViewContent.setText(textToShow);
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_SET_RETENTION_TIME) {
                String text;
                String fullName;
                String timeFormatted = transformSecondsInString(chatRoom.getRetentionTime());

                if (chat.getLastMessageSender() == megaChatApi.getMyUserHandle()) {
                    String myFullName = megaChatApi.getMyFullname();

                    if (isTextEmpty(myFullName)) {
                        myFullName = megaChatApi.getMyEmail();
                    }

                    fullName = toCDATA(myFullName);
                } else {
                    String fullNameAction = cC.getParticipantFullName(chat.getLastMessageSender());
                    if (isTextEmpty(fullNameAction) && !((ViewHolderNormalChatList) holder).nameRequestedAction) {
                        fullNameAction = context.getString(R.string.unknown_name_label);
                        ((ViewHolderNormalChatList) holder).nameRequestedAction = true;
                        ((ViewHolderNormalChatList) holder).userHandle = chat.getLastMessageSender();
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());
                        megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
                        megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
                        megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
                    }

                    fullName = toCDATA(fullNameAction);
                }

                if (isTextEmpty(timeFormatted)) {
                    text = String.format(context.getString(R.string.retention_history_disabled), toCDATA(fullName));
                } else {
                    text = String.format(context.getString(R.string.retention_history_changed_by), toCDATA(fullName), timeFormatted);
                }
                text = TextUtil.removeFormatPlaceholder(text);

                Spanned result = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY);
                ((ViewHolderNormalChatList) holder).textViewContent.setText(result);
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ContextCompat.getColor(context, R.color.grey_600_white_087));
            } else if (messageType == MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE) {
                Timber.d("Message type TYPE_PUBLIC_HANDLE_CREATE");
                String fullNameAction = getFullNameAction(chat);

                String textToShow = String.format(context.getString(R.string.message_created_chat_link), toCDATA(fullNameAction));

                try {
                    textToShow = textToShow.replace("[A]", "");
                    textToShow = textToShow.replace("[/A]", "");
                    textToShow = textToShow.replace("[B]", "");
                    textToShow = textToShow.replace("[/B]", "");
                } catch (Exception e) {
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderNormalChatList) holder).textViewContent.setText(result);

                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE) {
                Timber.d("Message type TYPE_PUBLIC_HANDLE_DELETE");
                String fullNameAction = getFullNameAction(chat);

                String textToShow = String.format(context.getString(R.string.message_deleted_chat_link), toCDATA(fullNameAction));

                try {
                    textToShow = textToShow.replace("[A]", "");
                    textToShow = textToShow.replace("[/A]", "");
                    textToShow = textToShow.replace("[B]", "");
                    textToShow = textToShow.replace("[/B]", "");
                } catch (Exception e) {
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderNormalChatList) holder).textViewContent.setText(result);

                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_SET_PRIVATE_MODE) {
                Timber.d("Message type TYPE_SET_PRIVATE_MODE");

                String fullNameAction = getFullNameAction(chat);

                String textToShow = String.format(context.getString(R.string.message_set_chat_private), toCDATA(fullNameAction));

                try {
                    textToShow = textToShow.replace("[A]", "");
                    textToShow = textToShow.replace("[/A]", "");
                    textToShow = textToShow.replace("[B]", "");
                    textToShow = textToShow.replace("[/B]", "");
                } catch (Exception e) {
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderNormalChatList) holder).textViewContent.setText(result);

                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_CHAT_TITLE) {
                Timber.d("Message type TYPE_CHAT_TITLE");

                String textToShow = cC.createManagementString(lastMessage, chatRoom);
                ((ViewHolderNormalChatList) holder).textViewContent.setText(textToShow);
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_CALL_STARTED) {
                Timber.d("Message type TYPE_CALL_STARTED");
                updateLastCallMessage(holder, chat);
            } else if (messageType == MegaChatMessage.TYPE_CALL_ENDED) {
                Timber.d("Message type TYPE_CALL_ENDED");

                String textToShow = cC.createManagementString(lastMessage, chatRoom);
                ((ViewHolderNormalChatList) holder).textViewContent.setText(textToShow);
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else if (messageType == MegaChatMessage.TYPE_CONTAINS_META) {
                Timber.d("Message type TYPE_CONTAINS_META");

                long messageId = chat.getLastMessageId();
                MegaChatMessage message = megaChatApi.getMessage(chat.getChatId(), messageId);
                if (message == null) return;

                MegaChatContainsMeta meta = message.getContainsMeta();
                if (meta == null) {
                    Timber.w("MegaChatContainsMeta is null.");
                    return;
                }

                long lastMsgSender = chat.getLastMessageSender();

                if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
                    Timber.d("Message type TYPE_CONTAINS_META:CONTAINS_META_GEOLOCATION");
                    ((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderNormalChatList) holder).voiceClipOrLocationText.setText(R.string.title_geolocation_message);
                    ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                    ((ViewHolderNormalChatList) holder).textViewContent.setText("");
                    if (lastMsgSender == megaChatApi.getMyUserHandle()) {

                        Timber.d("The last message is mine: %s", lastMsgSender);
                        ((ViewHolderNormalChatList) holder).textViewContent.setText(context.getString(R.string.word_me) + " ");
                    } else {
                        Timber.d("The last message NOT mine%s", lastMsgSender);

                        if (chat.isGroup()) {
                            ((ViewHolderNormalChatList) holder).currentPosition = position;
                            ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

                            String fullNameAction = converterShortCodes(cC.getParticipantFullName(lastMsgSender));

                            if (isTextEmpty(fullNameAction)) {
                                if (!(((ViewHolderNormalChatList) holder).nameRequestedAction)) {
                                    Timber.d("Call for nonContactName: %s", lastMsgSender);
                                    fullNameAction = context.getString(R.string.unknown_name_label);
                                    ((ViewHolderNormalChatList) holder).nameRequestedAction = true;
                                    ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;
                                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chatRoom.isPreview());
                                    megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                    megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                    megaChatApi.getUserEmail(lastMsgSender, listener);
                                } else {
                                    Timber.w("Name already asked and no name received: %s", lastMsgSender);
                                }
                            }

                            ((ViewHolderNormalChatList) holder).textViewContent.setText(fullNameAction + ": ");
                        }
                    }

                    setVoiceClipOrLocationLayout(((ViewHolderNormalChatList) holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList) holder).voiceClipOrLocationText, ((ViewHolderNormalChatList) holder).textViewContent, R.drawable.ic_location_small, chat.getUnreadCount() == 0);
                } else if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
                    Timber.d("Rich link message");
                    if (lastMessageString == null) {
                        Timber.w("Message Type-> %d last content is NULL", messageType);
                        lastMessageString = context.getString(R.string.error_message_unrecognizable);
                    } else {
                        Timber.d("Message Type-> %d last content: %slength: %d", messageType, lastMessageString, lastMessageString.length());
                    }

                    if (lastMsgSender == megaChatApi.getMyUserHandle()) {

                        Timber.d("The last message is mine: %s", lastMsgSender);
                        Spannable me = new SpannableString(context.getString(R.string.word_me) + " ");
                        me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        if (lastMessageString != null) {
                            Spannable myMessage = new SpannableString(lastMessageString);
                            myMessage.setSpan(new ForegroundColorSpan(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            CharSequence indexedText = TextUtils.concat(me, myMessage);
                            ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                            ((ViewHolderNormalChatList) holder).textViewContent.setText(indexedText);
                        }
                    } else {
                        Timber.d("The last message NOT mine: %s", lastMsgSender);

                        if (chat.isGroup()) {
                            ((ViewHolderNormalChatList) holder).currentPosition = position;
                            ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

                            String fullNameAction = converterShortCodes(cC.getParticipantFullName(lastMsgSender));

                            if (isTextEmpty(fullNameAction)) {
                                if (!(((ViewHolderNormalChatList) holder).nameRequestedAction)) {
                                    Timber.d("Call for nonContactName: %s", lastMsgSender);
                                    fullNameAction = context.getString(R.string.unknown_name_label);
                                    ((ViewHolderNormalChatList) holder).nameRequestedAction = true;
                                    ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;
                                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chatRoom.isPreview());
                                    megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                    megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                    megaChatApi.getUserEmail(lastMsgSender, listener);
                                } else {
                                    Timber.w("Name already asked and no name received: %s", lastMsgSender);
                                }
                            }

                            Spannable name = new SpannableString(fullNameAction + ": ");
                            name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            Spannable myMessage = new SpannableString(lastMessageString);
                            CharSequence indexedText = TextUtils.concat(name, myMessage);
                            ((ViewHolderNormalChatList) holder).textViewContent.setText(indexedText);
                        } else {
                            ((ViewHolderNormalChatList) holder).textViewContent.setText(lastMessageString);
                        }
                    }
                } else if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY) {
                    MegaChatGiphy giphy = meta.getGiphy();
                    String giphyTitle = null;

                    if (giphy != null) {
                        giphyTitle = giphy.getTitle();
                    }

                    if (isTextEmpty(giphyTitle)) {
                        giphyTitle = lastMessageString;

                        if (isTextEmpty(giphyTitle)) {
                            giphyTitle = context.getString(R.string.error_message_unrecognizable);
                        }
                    }

                    int contentColor = chat.getUnreadCount() == 0 ? R.color.grey_054_white_054 : R.color.teal_300_teal_200;
                    CharSequence giphyTextContent = null;

                    if (lastMsgSender == megaChatApi.getMyUserHandle()) {
                        Spannable me = new SpannableString(context.getString(R.string.word_me) + " ");
                        me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        Spannable myMessage = new SpannableString(giphyTitle);
                        myMessage.setSpan(new ForegroundColorSpan(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        giphyTextContent = TextUtils.concat(me, myMessage);
                        contentColor = R.color.grey_054_white_054;
                    } else if (chat.isGroup()) {
                        ((ViewHolderNormalChatList) holder).currentPosition = position;
                        ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

                        String fullNameAction = converterShortCodes(cC.getParticipantFullName(lastMsgSender));

                        if (isTextEmpty(fullNameAction) && !(((ViewHolderNormalChatList) holder).nameRequestedAction) && chatRoom != null) {
                            fullNameAction = context.getString(R.string.unknown_name_label);
                            ((ViewHolderNormalChatList) holder).nameRequestedAction = true;
                            ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chatRoom.isPreview());
                            megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                            megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                            megaChatApi.getUserEmail(lastMsgSender, listener);
                        }

                        Spannable name = new SpannableString(fullNameAction + ": ");
                        name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        Spannable myMessage = new SpannableString(giphyTitle);
                        myMessage.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, contentColor)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        giphyTextContent = TextUtils.concat(name, myMessage);
                    }

                    ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ContextCompat.getColor(context, contentColor));
                    ((ViewHolderNormalChatList) holder).textViewContent.setText(giphyTextContent != null ? giphyTextContent : giphyTitle);

                } else if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID) {
                    Timber.w("Invalid meta message");

                    String invalidMetaMessage = getInvalidMetaMessage(message);

                    if (lastMsgSender == megaChatApi.getMyUserHandle()) {

                        Timber.d("The last message is mine: %s", lastMsgSender);
                        Spannable me = new SpannableString(context.getString(R.string.word_me) + " ");
                        me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        Spannable myMessage = new SpannableString(invalidMetaMessage);
                        myMessage.setSpan(new ForegroundColorSpan(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        CharSequence indexedText = TextUtils.concat(me, myMessage);
                        ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                        ((ViewHolderNormalChatList) holder).textViewContent.setText(indexedText);
                    } else {
                        Timber.d("The last message NOT mine: %s", lastMsgSender);

                        if (chat.isGroup()) {
                            ((ViewHolderNormalChatList) holder).currentPosition = position;
                            ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

                            String fullNameAction = converterShortCodes(cC.getParticipantFullName(lastMsgSender));

                            if (isTextEmpty(fullNameAction)) {
                                if (!(((ViewHolderNormalChatList) holder).nameRequestedAction)) {
                                    Timber.d("Call for nonContactName: %s", lastMsgSender);
                                    fullNameAction = context.getString(R.string.unknown_name_label);
                                    ((ViewHolderNormalChatList) holder).nameRequestedAction = true;
                                    ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;
                                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chatRoom.isPreview());
                                    megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                    megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                    megaChatApi.getUserEmail(lastMsgSender, listener);
                                } else {
                                    Timber.w("Name already asked and no name received: %s", lastMsgSender);
                                }
                            }

                            Spannable name = new SpannableString(fullNameAction + ": ");
                            name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            Spannable myMessage = new SpannableString(invalidMetaMessage);
                            CharSequence indexedText = TextUtils.concat(name, myMessage);
                            ((ViewHolderNormalChatList) holder).textViewContent.setText(indexedText);
                        } else {
                            ((ViewHolderNormalChatList) holder).textViewContent.setText(invalidMetaMessage);
                        }
                    }
                }
            } else if (messageType == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                long contactsCount = lastMessage.getUsersCount();
                String contactAttachmentMessage = converterShortCodes(context.getString(R.string.contacts_sent, String.valueOf(contactsCount)));
                Spannable myMessage = new SpannableString(contactAttachmentMessage);

                if (chat.getLastMessageSender() == megaChatApi.getMyUserHandle()) {
                    Spannable me = new SpannableString(context.getString(R.string.word_me) + " ");
                    ((ViewHolderNormalChatList) holder).textViewContent.setText(TextUtils.concat(me, myMessage));
                } else {
                    Spannable sender = new SpannableString(megaChatApi.getUserFullnameFromCache(chat.getLastMessageSender()) + ": ");
                    ((ViewHolderNormalChatList) holder).textViewContent.setText(TextUtils.concat(sender, myMessage));
                }
            } else {
                //OTHER TYPE OF MESSAGE
                if (lastMessageString == null) {
                    Timber.w("Message Type-> %d last content is NULL ", messageType);
                    lastMessageString = context.getString(R.string.error_message_unrecognizable);
                } else if (messageType == MegaChatMessage.TYPE_VOICE_CLIP) {
                    lastMessageString = "";
                    ((ViewHolderNormalChatList) holder).voiceClipOrLocationLayout.setVisibility(View.VISIBLE);
                    long idLastMessage = chat.getLastMessageId();
                    long idChat = chat.getChatId();
                    MegaChatMessage m = megaChatApi.getMessage(idChat, idLastMessage);
                    if (m == null || m.getMegaNodeList() == null || m.getMegaNodeList().size() < 1 || !isVoiceClip(m.getMegaNodeList().get(0).getName())) {
                        ((ViewHolderNormalChatList) holder).voiceClipOrLocationText.setText("--:--");
                    } else {
                        long duration = getVoiceClipDuration(m.getMegaNodeList().get(0));
                        ((ViewHolderNormalChatList) holder).voiceClipOrLocationText.setText(milliSecondsToTimer(duration));
                    }

                }

                long lastMsgSender = chat.getLastMessageSender();


                if (lastMsgSender == megaChatApi.getMyUserHandle()) {

                    Timber.d("The last message is mine: %s", lastMsgSender);
                    Spannable me = new SpannableString(context.getString(R.string.word_me) + " ");
                    me.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, me.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    if (lastMessageString != null) {
                        Spannable myMessage = new SpannableString(lastMessageString);
                        myMessage.setSpan(new ForegroundColorSpan(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary)), 0, myMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        CharSequence indexedText = TextUtils.concat(me, myMessage);
                        ((ViewHolderNormalChatList) holder).textViewContent.setText(indexedText);

                        ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
                    }
                } else {
                    Timber.d("The last message NOT mine: %s", lastMsgSender);

                    if (chat.isGroup()) {
                        ((ViewHolderNormalChatList) holder).currentPosition = position;
                        ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

                        String fullNameAction = converterShortCodes(cC.getParticipantFullName(lastMsgSender));

                        if (isTextEmpty(fullNameAction)) {
                            if (!(((ViewHolderNormalChatList) holder).nameRequestedAction)) {
                                Timber.d("Call for nonContactHandle: %s", lastMsgSender);
                                fullNameAction = context.getString(R.string.unknown_name_label);
                                ((ViewHolderNormalChatList) holder).nameRequestedAction = true;
                                ((ViewHolderNormalChatList) holder).userHandle = lastMsgSender;

                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, lastMsgSender, chat.isPreview());

                                megaChatApi.getUserFirstname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                megaChatApi.getUserLastname(lastMsgSender, chatRoom.getAuthorizationToken(), listener);
                                megaChatApi.getUserEmail(lastMsgSender, listener);
                            } else {
                                Timber.w("Name already asked and no name received: handle %s", lastMsgSender);
                            }
                        }

                        Spannable name = new SpannableString(fullNameAction + ": ");
                        name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        Spannable contactMessage = new SpannableString(lastMessageString);
                        CharSequence indexedText = TextUtils.concat(name, contactMessage);
                        ((ViewHolderNormalChatList) holder).textViewContent.setText(indexedText);
                    } else {
                        ((ViewHolderNormalChatList) holder).textViewContent.setText(lastMessageString);
                    }
                }

                setVoiceClipOrLocationLayout(((ViewHolderNormalChatList) holder).voiceClipOrLocationIc, ((ViewHolderNormalChatList) holder).voiceClipOrLocationText, ((ViewHolderNormalChatList) holder).textViewContent, R.drawable.ic_mic_on_small, chat.getUnreadCount() == 0);
            }

            if (chat.getUnreadCount() == 0) {
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
            } else {
                ((ViewHolderNormalChatList) holder).textViewContent.setTextColor(ColorUtils.getThemeColor(context, R.attr.colorSecondary));
            }
        } else {
            Timber.w("Holder is NULL: %s", position);
            notifyItemChanged(position);
        }
    }

    private void setVoiceClipOrLocationLayout(ImageView image, TextView text, TextView senderText, int resource, boolean isRead) {
        if (isRead) {
            image.setImageDrawable(mutateIconSecondary(context, resource, R.color.grey_300_grey_600));
            text.setTextColor(ContextCompat.getColor(context, R.color.grey_300_grey_600));
            senderText.setTextColor(ColorUtils.getThemeColor(context, android.R.attr.textColorSecondary));
        } else {
            image.setImageDrawable(mutateIconSecondary(context, resource, R.color.teal_300_teal_200));
            text.setTextColor(ContextCompat.getColor(context, R.color.teal_300_teal_200));
            senderText.setTextColor(ColorUtils.getThemeColor(context, R.attr.colorSecondary));
        }
    }

    public String getFullNameAction(MegaChatListItem chat) {
        String fullNameAction = "";
        if (chat.getLastMessageSender() == megaChatApi.getMyUserHandle()) {
            fullNameAction = megaChatApi.getMyFullname();
            if (fullNameAction == null) {
                fullNameAction = "";
            }
            if (fullNameAction.trim().length() <= 0) {
                fullNameAction = megaChatApi.getMyEmail();
            }
        } else {
            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chat.getChatId());
            fullNameAction = cC.getParticipantFullName(chat.getLastMessageSender());
            if (isTextEmpty(fullNameAction)) {
                if (!(((ViewHolderNormalChatList) holder).nameRequestedAction)) {
                    Timber.d("Call for nonContactHandle: %s", chat.getLastMessageSender());
                    fullNameAction = context.getString(R.string.unknown_name_label);
                    ((ViewHolderNormalChatList) holder).nameRequestedAction = true;
                    ((ViewHolderNormalChatList) holder).userHandle = chat.getLastMessageSender();
                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, chat.getLastMessageSender(), chat.isPreview());
                    megaChatApi.getUserFirstname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
                    megaChatApi.getUserLastname(chat.getLastMessageSender(), chatRoom.getAuthorizationToken(), listener);
                    megaChatApi.getUserEmail(chat.getLastMessageSender(), listener);
                } else {
                    Timber.w("Name already asked and no name received: %s", chat.getLastMessageSender());
                }
            }
        }
        return fullNameAction;
    }

    public void setChats(ArrayList<MegaChatListItem> updatedChats) {
        Timber.d("Number of updated chats: %s", updatedChats.size());
        this.chats = updatedChats;

        positionClicked = -1;

        if (listFragment != null) {
            listFragment.invalidate();
        }

        notifyDataSetChanged();
    }

    public void updateMultiselectionPosition(int oldPosition) {
        Timber.d("oldPosition: %s", oldPosition);

        List<Integer> selected = getSelectedItems();
        boolean movedSelected = false;

        if (isItemChecked(oldPosition)) {
            movedSelected = true;
        }

        selectedItems.clear();

        if (movedSelected) {
            selectedItems.put(0, true);
        }

        for (int i = 0; i < selected.size(); i++) {
            int pos = selected.get(i);
            if (pos != oldPosition) {
                if (pos < oldPosition) {
                    selectedItems.put(pos + 1, true);
                } else {
                    selectedItems.put(pos, true);
                }
            }
        }
    }

    @Override
    public String getSectionTitle(int position) {
        if (holder instanceof ViewHolderNormalChatList) {
            if (((ViewHolderNormalChatList) holder).textFastScroller.isEmpty()) {
                return null;
            } else {
                return ((ViewHolderNormalChatList) holder).textFastScroller;
            }
        } else {
            return null;
        }
    }

    public void modifyChat(ArrayList<MegaChatListItem> chats, int position) {
        this.chats = chats;
        notifyItemChanged(position);
    }

    public void removeChat(ArrayList<MegaChatListItem> chats, int position) {
        this.chats = chats;
        notifyItemRemoved(position);
    }
}