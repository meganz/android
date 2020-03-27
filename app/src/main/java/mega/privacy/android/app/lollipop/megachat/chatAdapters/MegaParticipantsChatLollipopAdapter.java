package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.content.Intent;
import android.graphics.Bitmap;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.utils.AvatarUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static nz.mega.sdk.MegaChatApi.*;

public class MegaParticipantsChatLollipopAdapter extends RecyclerView.Adapter<MegaParticipantsChatLollipopAdapter.ViewHolderParticipants> implements OnClickListener {

    private final static int MAX_WIDTH_CHAT_TITLE_PORT = 200;
    private final static int MAX_WIDTH_CHAT_TITLE_LAND = 300;
    private static final int ITEM_VIEW_TYPE_NORMAL = 0;
    private static final int ITEM_VIEW_TYPE_ADD_PARTICIPANT = 1;
    public static final int ITEM_VIEW_TYPE_HEADER = 2;
    private static final int MAX_WIDTH_PORT = 180;
    private static final int MAX_WIDTH_LAND = 260;

    private GroupChatInfoActivityLollipop groupChatInfoActivity;
    private DisplayMetrics outMetrics;
    private ArrayList<MegaChatParticipant> participants;
    private RecyclerView listFragment;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    private long chatId;
    private boolean isPreview;

    public MegaParticipantsChatLollipopAdapter(GroupChatInfoActivityLollipop groupChatInfoActivity, ArrayList<MegaChatParticipant> participants, RecyclerView listView) {
        this.groupChatInfoActivity = groupChatInfoActivity;
        this.participants = participants;
        this.listFragment = listView;
        this.chatId = groupChatInfoActivity.getChatHandle();
        this.isPreview = groupChatInfoActivity.getChat().isPreview();

        megaApi = MegaApplication.getInstance().getMegaApi();
        megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        Display display = groupChatInfoActivity.getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
    }

    /*private view holder class*/
    public static class ViewHolderParticipants extends RecyclerView.ViewHolder {
        public ViewHolderParticipants(View v) {
            super(v);
        }

        RelativeLayout itemLayout;
    }

    public static class ViewHolderParticipantsList extends ViewHolderParticipants {
        public ViewHolderParticipantsList(View v) {
            super(v);
        }

        private RoundedImageView imageView;
        private MarqueeTextView textViewContent;
        private EmojiTextView textViewContactName;
        private RelativeLayout threeDotsLayout;
        private ImageView imageButtonThreeDots;
        private ImageView statusImage;

        private ImageView permissionsIcon;
        private int currentPosition;
        private String contactMail;
        private String userHandle;
        private String fullName = "";

        public void setImageView(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }

        public String getContactMail() {
            return contactMail;
        }

        public String getUserHandle() {
            return userHandle;
        }
    }

    public static class ViewHolderAddParticipant extends ViewHolderParticipants {
        public ViewHolderAddParticipant(View v) {
            super(v);
        }

        private ImageView imageView;
    }

    public static class ViewHolderParticipantsHeader extends ViewHolderParticipants {
        public ViewHolderParticipantsHeader(View v) {
            super(v);
        }

        private LinearLayout infoLayout;
        private RelativeLayout avatarLayout;
        private TextView infoNumParticipantsText;
        private RelativeLayout infoTextContainerLayout;
        private ImageView editImageView;
        private LinearLayout notificationsLayout;
        private SwitchCompat notificationsSwitch;
        private TextView notificationsTitle;
        private View dividerNotifications;
        private LinearLayout chatLinkLayout;
        private TextView chatLinkTitleText;
        private View chatLinkSeparator;
        private LinearLayout privateLayout;
        private View privateSeparator;
        private RelativeLayout sharedFilesLayout;
        private View dividerSharedFilesLayout;
        private RelativeLayout clearChatLayout;
        private View dividerClearLayout;
        private RelativeLayout leaveChatLayout;
        private View dividerLeaveLayout;
        private RelativeLayout archiveChatLayout;
        private TextView archiveChatTitle;
        private ImageView archiveChatIcon;
        private View archiveChatSeparator;
        private RelativeLayout observersLayout;
        private TextView observersNumberText;
        private View observersSeparator;
        private TextView participantsTitle;
        private RoundedImageView avatarImageView;
        private EmojiTextView infoTitleChatText;
    }

    @Override
    public ViewHolderParticipants onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");

        View v;
        switch (viewType) {
            case ITEM_VIEW_TYPE_HEADER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_group_participants, parent, false);
                ViewHolderParticipantsHeader holderHeader = new ViewHolderParticipantsHeader(v);

                holderHeader.avatarImageView = v.findViewById(R.id.chat_group_properties_thumbnail);
                holderHeader.infoLayout = v.findViewById(R.id.chat_group_contact_properties_info_layout);
                holderHeader.avatarLayout = v.findViewById(R.id.chat_group_properties_avatar_layout);
                holderHeader.infoTextContainerLayout = v.findViewById(R.id.chat_group_contact_properties_info_text_container);
                holderHeader.infoTitleChatText = v.findViewById(R.id.chat_group_contact_properties_info_title);
                if (isScreenInPortrait(groupChatInfoActivity)) {
                    holderHeader.infoTitleChatText.setMaxWidthEmojis(px2dp(MAX_WIDTH_CHAT_TITLE_PORT, outMetrics));
                } else {
                    holderHeader.infoTitleChatText.setMaxWidthEmojis(px2dp(MAX_WIDTH_CHAT_TITLE_LAND, outMetrics));
                }

                holderHeader.editImageView = v.findViewById(R.id.chat_group_contact_properties_edit_icon);
                holderHeader.editImageView.setOnClickListener(this);

                //Notifications Layout
                holderHeader.notificationsLayout = v.findViewById(R.id.chat_group_contact_properties_notifications_layout);

                holderHeader.notificationsTitle = v.findViewById(R.id.chat_group_contact_properties_notifications_title);
                holderHeader.notificationsSwitch = v.findViewById(R.id.chat_group_contact_properties_switch);
                holderHeader.notificationsSwitch.setOnClickListener(this);
                holderHeader.dividerNotifications = v.findViewById(R.id.divider_notifications_layout);

                holderHeader.infoNumParticipantsText = v.findViewById(R.id.chat_group_contact_properties_info_participants);

                //Chat links
                holderHeader.chatLinkLayout = v.findViewById(R.id.chat_group_contact_properties_chat_link_layout);
                holderHeader.chatLinkTitleText = v.findViewById(R.id.chat_group_contact_properties_chat_link);
                holderHeader.chatLinkSeparator = v.findViewById(R.id.divider_chat_link_layout);

                //Private chat
                holderHeader.privateLayout = v.findViewById(R.id.chat_group_contact_properties_private_layout);
                holderHeader.privateSeparator = v.findViewById(R.id.divider_private_layout);

                //Chat Shared Files Layout
                holderHeader.sharedFilesLayout = v.findViewById(R.id.chat_group_contact_properties_chat_files_shared_layout);
                holderHeader.sharedFilesLayout.setOnClickListener(this);
                holderHeader.dividerSharedFilesLayout = v.findViewById(R.id.divider_chat_files_shared_layout);

                //Clear chat Layout
                holderHeader.clearChatLayout = v.findViewById(R.id.chat_group_contact_properties_clear_layout);
                holderHeader.clearChatLayout.setOnClickListener(this);
                holderHeader.dividerClearLayout = v.findViewById(R.id.divider_clear_layout);

                //Archive chat Layout
                holderHeader.archiveChatLayout = v.findViewById(R.id.chat_group_contact_properties_archive_layout);
                holderHeader.archiveChatLayout.setOnClickListener(this);

                holderHeader.archiveChatSeparator = v.findViewById(R.id.divider_archive_layout);

                holderHeader.archiveChatTitle = v.findViewById(R.id.chat_group_contact_properties_archive);
                holderHeader.archiveChatIcon = v.findViewById(R.id.chat_group_contact_properties_archive_icon);

                //Leave chat Layout
                holderHeader.leaveChatLayout = v.findViewById(R.id.chat_group_contact_properties_leave_layout);
                holderHeader.leaveChatLayout.setOnClickListener(this);
                holderHeader.dividerLeaveLayout = v.findViewById(R.id.divider_leave_layout);

                //Observers layout
                holderHeader.observersLayout = v.findViewById(R.id.chat_group_observers_layout);
                holderHeader.observersNumberText = v.findViewById(R.id.chat_group_observers_number_text);
                holderHeader.observersSeparator = v.findViewById(R.id.divider_observers_layout);

                holderHeader.participantsTitle = v.findViewById(R.id.chat_group_contact_properties_title_text);

                return holderHeader;

            case ITEM_VIEW_TYPE_NORMAL:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_chat_list, parent, false);
                ViewHolderParticipantsList holderList = new ViewHolderParticipantsList(v);

                holderList.itemLayout = v.findViewById(R.id.participant_list_item_layout);
                holderList.imageView = v.findViewById(R.id.participant_list_thumbnail);
                holderList.textViewContactName = v.findViewById(R.id.participant_list_name);
                holderList.textViewContent = v.findViewById(R.id.participant_list_content);
                holderList.threeDotsLayout = v.findViewById(R.id.participant_list_three_dots_layout);
                holderList.imageButtonThreeDots = v.findViewById(R.id.participant_list_three_dots);
                holderList.permissionsIcon = v.findViewById(R.id.participant_list_permissions);
                holderList.statusImage = v.findViewById(R.id.group_participants_state_circle);

                if (isScreenInPortrait(groupChatInfoActivity)) {
                    holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_PORT, outMetrics));
                    holderList.textViewContent.setMaxWidth(scaleWidthPx(MAX_WIDTH_PORT, outMetrics));
                } else {
                    holderList.textViewContactName.setMaxWidthEmojis(scaleWidthPx(MAX_WIDTH_LAND, outMetrics));
                    holderList.textViewContent.setMaxWidth(scaleWidthPx(MAX_WIDTH_LAND, outMetrics));
                }

                holderList.itemLayout.setOnClickListener(this);
                holderList.itemLayout.setTag(holderList);

                v.setTag(holderList);
                return holderList;

            case ITEM_VIEW_TYPE_ADD_PARTICIPANT:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_participant_chat_list, parent, false);
                ViewHolderAddParticipant holderAddParticipant = new ViewHolderAddParticipant(v);

                holderAddParticipant.itemLayout = v.findViewById(R.id.add_participant_list_item_layout);
                holderAddParticipant.itemLayout.setOnClickListener(this);
                holderAddParticipant.imageView = v.findViewById(R.id.add_participant_list_icon);

                v.setTag(holderAddParticipant);
                return holderAddParticipant;

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolderParticipants holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_HEADER:
                ViewHolderParticipantsHeader holderHeader = (ViewHolderParticipantsHeader) holder;

                int color = ContextCompat.getColor(groupChatInfoActivity, R.color.divider_upgrade_account);
                String title = getChat().getTitle();
                holderHeader.avatarImageView.setImageBitmap(getDefaultAvatar(groupChatInfoActivity, color, title, AVATAR_SIZE, true));

                holderHeader.infoTitleChatText.setText(getChat().getTitle());

                if (getChat().isArchived()) {
                    holderHeader.archiveChatTitle.setText(groupChatInfoActivity.getString(R.string.general_unarchive));
                    holderHeader.archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.ic_b_unarchive));
                } else {
                    holderHeader.archiveChatTitle.setText(groupChatInfoActivity.getString(R.string.general_archive));
                    holderHeader.archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.ic_b_archive));
                }

                long participantsCount = getChat().getPeerCount();

                if (isPreview) {
                    holderHeader.notificationsLayout.setVisibility(View.GONE);
                    holderHeader.dividerNotifications.setVisibility(View.GONE);
                    holderHeader.chatLinkLayout.setVisibility(View.GONE);
                    holderHeader.chatLinkSeparator.setVisibility(View.GONE);
                    holderHeader.privateLayout.setVisibility(View.GONE);
                    holderHeader.privateSeparator.setVisibility(View.GONE);
                    holderHeader.clearChatLayout.setVisibility(View.GONE);
                    holderHeader.dividerClearLayout.setVisibility(View.GONE);
                    holderHeader.archiveChatLayout.setVisibility(View.GONE);
                    holderHeader.archiveChatSeparator.setVisibility(View.GONE);
                    holderHeader.leaveChatLayout.setVisibility(View.GONE);
                    holderHeader.dividerLeaveLayout.setVisibility(View.GONE);
                    holderHeader.editImageView.setVisibility(View.GONE);
                } else {
                    participantsCount++;

                    if (getChat().getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR) {
                        holderHeader.editImageView.setVisibility(View.VISIBLE);
                        holderHeader.dividerClearLayout.setVisibility(View.VISIBLE);
                        holderHeader.clearChatLayout.setVisibility(View.VISIBLE);
                        holderHeader.dividerLeaveLayout.setVisibility(View.VISIBLE);

                        if (getChat().isPublic()) {
                            holderHeader.privateLayout.setVisibility(View.VISIBLE);
                            holderHeader.privateLayout.setOnClickListener(this);
                            holderHeader.privateSeparator.setVisibility(View.VISIBLE);
                        } else {
                            logDebug("Private getChat()");
                            holderHeader.privateLayout.setVisibility(View.GONE);
                            holderHeader.privateSeparator.setVisibility(View.GONE);
                        }
                    } else {
                        holderHeader.editImageView.setVisibility(View.GONE);
                        holderHeader.dividerClearLayout.setVisibility(View.GONE);
                        holderHeader.clearChatLayout.setVisibility(View.GONE);
                        holderHeader.privateLayout.setVisibility(View.GONE);
                        holderHeader.privateSeparator.setVisibility(View.GONE);

                        if (getChat().getOwnPrivilege() < MegaChatRoom.PRIV_RO) {
                            holderHeader.leaveChatLayout.setVisibility(View.GONE);
                            holderHeader.dividerLeaveLayout.setVisibility(View.GONE);
                        }
                    }

                    if (getChat().isPublic() && getChat().getOwnPrivilege() >= MegaChatRoom.PRIV_RO) {
                        holderHeader.chatLinkLayout.setVisibility(View.VISIBLE);
                        holderHeader.chatLinkLayout.setOnClickListener(this);
                        holderHeader.chatLinkSeparator.setVisibility(View.VISIBLE);
                    } else {
                        holderHeader.chatLinkLayout.setVisibility(View.GONE);
                        holderHeader.chatLinkSeparator.setVisibility(View.GONE);
                        groupChatInfoActivity.setChatLink(null);
                    }

                    if (getChat().isArchived()) {
                        holderHeader.archiveChatTitle.setText(groupChatInfoActivity.getString(R.string.general_unarchive));
                        holderHeader.archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.ic_b_unarchive));
                    } else {
                        holderHeader.archiveChatTitle.setText(groupChatInfoActivity.getString(R.string.general_archive));
                        holderHeader.archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.ic_b_archive));
                    }

                    holderHeader.notificationsSwitch.setChecked(!groupChatInfoActivity.isChatMuted());
                }

                holderHeader.infoNumParticipantsText.setText(groupChatInfoActivity.getString(R.string.number_of_participants, participantsCount));

                if (getChat().getNumPreviewers() < 1) {
                    holderHeader.observersSeparator.setVisibility(View.GONE);
                    holderHeader.observersLayout.setVisibility(View.GONE);
                } else {
                    holderHeader.observersSeparator.setVisibility(View.VISIBLE);
                    holderHeader.observersLayout.setVisibility(View.VISIBLE);
                    holderHeader.observersNumberText.setText(getChat().getNumPreviewers() + "");
                }
                break;

            case ITEM_VIEW_TYPE_NORMAL:
                ViewHolderParticipantsList holderParticipantsList = (ViewHolderParticipantsList) holder;
                MegaChatParticipant participant = getParticipant(position);
                if (participant == null) return;

                holderParticipantsList.currentPosition = position;
                holderParticipantsList.imageView.setImageBitmap(null);

                checkParticipant(position, participant);
                holderParticipantsList.contactMail = participant.getEmail();
                holderParticipantsList.userHandle = MegaApiAndroid.userHandleToBase64(participant.getHandle());
                holderParticipantsList.fullName = participant.getFullName();

                int userStatus;

                if (megaChatApi.getMyUserHandle() == participant.getHandle()) {
                    userStatus = megaChatApi.getOnlineStatus();
                } else {
                    userStatus = megaChatApi.getUserOnlineStatus(participant.getHandle());
                }

                holderParticipantsList.statusImage.setVisibility(View.VISIBLE);
                holderParticipantsList.textViewContent.setVisibility(View.VISIBLE);

                switch (userStatus) {
                    case STATUS_ONLINE:
                        holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.circle_status_contact_online));
                        holderParticipantsList.textViewContent.setText(groupChatInfoActivity.getString(R.string.online_status));
                        break;

                    case STATUS_AWAY:
                        holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.circle_status_contact_away));
                        holderParticipantsList.textViewContent.setText(groupChatInfoActivity.getString(R.string.away_status));
                        break;

                    case STATUS_BUSY:
                        holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.circle_status_contact_busy));
                        holderParticipantsList.textViewContent.setText(groupChatInfoActivity.getString(R.string.busy_status));
                        break;

                    case STATUS_OFFLINE:
                        holderParticipantsList.statusImage.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.circle_status_contact_offline));
                        holderParticipantsList.textViewContent.setText(groupChatInfoActivity.getString(R.string.offline_status));
                        break;

                    case STATUS_INVALID:
                    default:
                        holderParticipantsList.statusImage.setVisibility(View.GONE);
                        holderParticipantsList.textViewContent.setVisibility(View.GONE);
                }

                if (userStatus != STATUS_ONLINE && userStatus != STATUS_BUSY && userStatus != STATUS_INVALID && !participant.getLastGreen().isEmpty()) {
                    holderParticipantsList.textViewContent.setText(participant.getLastGreen());
                    holderParticipantsList.textViewContent.isMarqueeIsNecessary(groupChatInfoActivity);
                }

                holderParticipantsList.textViewContactName.setText(holderParticipantsList.fullName);
                holderParticipantsList.threeDotsLayout.setOnClickListener(this);
                holderParticipantsList.imageButtonThreeDots.setColorFilter(null);

                /*Default Avatar*/
                int avatarColor = getColorAvatar(groupChatInfoActivity, megaApi, holderParticipantsList.userHandle);
                holderParticipantsList.imageView.setImageBitmap(getDefaultAvatar(groupChatInfoActivity, avatarColor, holderParticipantsList.fullName, AVATAR_SIZE, true));

                /*Avatar*/
                String myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.getMyUserHandle());
                if ((holderParticipantsList.userHandle).equals(myUserHandleEncoded)) {
                    Bitmap bitmap = getAvatarBitmap(holderParticipantsList.contactMail);
                    if (bitmap != null) {
                        holderParticipantsList.setImageView(bitmap);
                    }
                } else {
                    String nameFileHandle = holderParticipantsList.userHandle;
                    String nameFileEmail = holderParticipantsList.contactMail;
                    MegaUser contact = null;
                    Bitmap bitmap;
                    
                    if (holderParticipantsList.contactMail == null) {
                        holderParticipantsList.imageButtonThreeDots.setColorFilter(ContextCompat.getColor(groupChatInfoActivity, R.color.chat_sliding_panel_separator));
                        holderParticipantsList.threeDotsLayout.setOnClickListener(null);
                        bitmap = getAvatarBitmap(nameFileHandle);
                    } else {
                        contact = megaApi.getContact(holderParticipantsList.contactMail);
                        bitmap = getUserAvatar(nameFileHandle, nameFileEmail);
                    }
                    
                    if (bitmap != null) {
                        holderParticipantsList.setImageView(bitmap);
                    } else {
                        GetAttrUserListener listener = new GetAttrUserListener(groupChatInfoActivity, holderParticipantsList, this);
                        
                        if (contact != null) {
                            megaApi.getUserAvatar(contact, buildAvatarFile(groupChatInfoActivity, nameFileEmail + JPG_EXTENSION).getAbsolutePath(), listener);
                        } else if (participant.getEmail() != null) {
                            megaApi.getUserAvatar(participant.getEmail(), buildAvatarFile(groupChatInfoActivity, nameFileEmail + JPG_EXTENSION).getAbsolutePath(), listener);
                        } else {
                            megaApi.getUserAvatar(holderParticipantsList.userHandle, buildAvatarFile(groupChatInfoActivity, nameFileHandle + JPG_EXTENSION).getAbsolutePath(), listener);
                        }
                    }
                }

                if (isPreview && megaChatApi.getInitState() == INIT_ANONYMOUS) {
                    holderParticipantsList.imageButtonThreeDots.setColorFilter(ContextCompat.getColor(groupChatInfoActivity, R.color.chat_sliding_panel_separator));
                    holderParticipantsList.threeDotsLayout.setOnClickListener(null);
                    holderParticipantsList.itemLayout.setOnClickListener(null);
                }

                int permission = participant.getPrivilege();

                if (permission == MegaChatRoom.PRIV_STANDARD) {
                    holderParticipantsList.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
                } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
                    holderParticipantsList.permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
                } else {
                    holderParticipantsList.permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
                }

                holderParticipantsList.threeDotsLayout.setTag(holder);
                break;

            case ITEM_VIEW_TYPE_ADD_PARTICIPANT:
                ((ViewHolderAddParticipant) holder).itemLayout.setOnClickListener(this);
                break;
        }
    }

    /**
     * Checks if the user is previewing the chat and if is moderator.
     * If the user is moderator, they appears in the last position of the participants list.
     *
     * @return True if the current chat is not a preview and if the user is moderator, false otherwise.
     */
    private boolean isNotPreviewAndLastParticipantModerator() {
        return !isPreview && participants.get(participants.size() - 1).getPrivilege() == MegaChatRoom.PRIV_MODERATOR;
    }

    /**
     * Gets the number of items in the adapter.
     * It depends on the user:
     *
     * If they is not previewing the chat and they are a moderator there are two more views in the adapter (Header + Add participant views),
     *      so the count in the adapter is the same as in the array plus 2.
     *
     * Otherwise, there is only one more view in the adapter (Header),
     *      so the count in the adapter is the same as in the array plus 1.
     *
     * @return Number of items in the adapter.
     */
    @Override
    public int getItemCount() {
        if (isNotPreviewAndLastParticipantModerator()) {
            return participants.size() + 2;
        } else {
            return participants.size() + 1;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_VIEW_TYPE_HEADER;
        } else if (position == 1 && isNotPreviewAndLastParticipantModerator()) {
            return ITEM_VIEW_TYPE_ADD_PARTICIPANT;
        } else {
            return ITEM_VIEW_TYPE_NORMAL;
        }
    }

    /**
     * Gets the position of the participant in the array.
     * It depends on the user:
     *
     * If they is not previewing the chat and they are a moderator there are two more views in the adapter (Header + Add participant views),
     *      so the position in the array is the same as in the adapter minus 2.
     *
     * Otherwise, there is only one more view in the adapter (Header),
     *      so the position in the array is the same as in the adapter minus 1.
     *
     * @param adapterPosition   the position of the participant in the adapter.
     * @return The position of the participant in the array.
     */
    public int getParticipantPositionInArray(int adapterPosition) {
        if (isNotPreviewAndLastParticipantModerator()) {
            return adapterPosition - 2;
        } else {
            return adapterPosition - 1;
        }
    }

    private MegaChatParticipant getParticipant(int position) {
        int positionInArray = getParticipantPositionInArray(position);

        if (positionInArray < 0 || positionInArray > participants.size()) {
            return null;
        }

        return participants.get(positionInArray);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onClick(View v) {

        if (!isOnline(groupChatInfoActivity)) {
            groupChatInfoActivity.showSnackbar(groupChatInfoActivity.getString(R.string.error_server_connection_problem));
            return;
        }

        switch (v.getId()) {
            case R.id.participant_list_three_dots_layout:
            case R.id.participant_list_item_layout:
                ViewHolderParticipantsList holder = (ViewHolderParticipantsList) v.getTag();
                int currentPosition = holder.currentPosition;
                MegaChatParticipant p = getParticipant(currentPosition);
                groupChatInfoActivity.showParticipantsPanel(p);
                break;

            case R.id.add_participant_list_item_layout:
                groupChatInfoActivity.chooseAddParticipantDialog();
                break;

            case R.id.chat_group_contact_properties_edit_icon:
                groupChatInfoActivity.showRenameGroupDialog(false);
                break;

            case R.id.chat_group_contact_properties_leave_layout:
                groupChatInfoActivity.showConfirmationLeaveChat();
                break;

            case R.id.chat_group_contact_properties_clear_layout:
                groupChatInfoActivity.showConfirmationClearChat();
                break;

            case R.id.chat_group_contact_properties_archive_layout:
                new ChatController(groupChatInfoActivity).archiveChat(groupChatInfoActivity.getChat());
                break;

            case R.id.chat_group_contact_properties_switch: {
                groupChatInfoActivity.setChatMuted();

                ChatController chatC = new ChatController(groupChatInfoActivity);
                if (groupChatInfoActivity.isChatMuted()) {
                    chatC.muteChat(chatId);
                } else {
                    chatC.unmuteChat(chatId);
                }
                break;

            }
            case R.id.chat_group_contact_properties_chat_link_layout: {
                megaChatApi.queryChatLink(chatId, groupChatInfoActivity);
                break;
            }
            case R.id.chat_group_contact_properties_private_layout: {
                groupChatInfoActivity.showConfirmationPrivateChatDialog();
                break;
            }
            case R.id.chat_group_contact_properties_chat_files_shared_layout: {
                Intent nodeHistoryIntent = new Intent(groupChatInfoActivity, NodeAttachmentHistoryActivity.class);
                nodeHistoryIntent.putExtra("chatId", chatId);
                groupChatInfoActivity.startActivity(nodeHistoryIntent);
                break;
            }
        }
    }

    public void setParticipants(ArrayList<MegaChatParticipant> participants) {
        this.participants = participants;
        notifyDataSetChanged();
    }

    public void updateParticipant(int position, ArrayList<MegaChatParticipant> participants) {
        this.participants = participants;
        notifyItemChanged(position);
    }

    public void removeParticipant(int position, ArrayList<MegaChatParticipant> participants) {
        this.participants = participants;
        notifyItemRemoved(position);
    }

    public String getDescription(ArrayList<MegaNode> nodes) {
        int numFolders = 0;
        int numFiles = 0;

        for (int i = 0; i < nodes.size(); i++) {
            MegaNode c = nodes.get(i);
            if (c.isFolder()) {
                numFolders++;
            } else {
                numFiles++;
            }
        }

        String info = "";
        if (numFolders > 0) {
            info = numFolders + " " + groupChatInfoActivity.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
            if (numFiles > 0) {
                info = info + ", " + numFiles + " " + groupChatInfoActivity.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
            }
        } else {
            if (numFiles == 0) {
                info = numFiles + " " + groupChatInfoActivity.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
            } else {
                info = numFiles + " " + groupChatInfoActivity.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
            }
        }

        return info;
    }

    public void updateContactStatus(int position) {
        logDebug("position: " + position);

        if (listFragment.findViewHolderForAdapterPosition(position) instanceof MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList) {
            notifyItemChanged(position);
        }
    }

    /**
     * Checks if the participant has attributes.
     * If not, it stores the participant to ask for their when necessary.
     *
     * @param position      the position of the participant in the adapter.
     * @param participant   the participant to check.
     */
    private void checkParticipant(int position, MegaChatParticipant participant) {
        if (!groupChatInfoActivity.hasParticipantAttributes(participant)) {
            groupChatInfoActivity.addParticipantRequest(position, participant);
        }
    }

    private MegaChatRoom getChat() {
        return groupChatInfoActivity.getChat();
    }
}
