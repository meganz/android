package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.content.Intent;
import android.graphics.Bitmap;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Typeface;
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
import mega.privacy.android.app.activities.ManageChatHistoryActivity;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static nz.mega.sdk.MegaChatApi.*;

public class MegaParticipantsChatLollipopAdapter extends RecyclerView.Adapter<MegaParticipantsChatLollipopAdapter.ViewHolderParticipants> implements OnClickListener {

    private static final int MAX_PARTICIPANTS_CHANGE_TO_PRIVATE = 100;
    private static final int HEADER_POSITION = 0;
    private static final int ADD_PARTICIPANTS_POSITION = 1;
    private static final int COUNT_HEADER_POSITION = 1;
    private static final int COUNT_HEADER_AND_ADD_PARTICIPANTS_POSITIONS = 2;
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

    private ChatController chatC;

    public MegaParticipantsChatLollipopAdapter(GroupChatInfoActivityLollipop groupChatInfoActivity, RecyclerView listView) {
        this.groupChatInfoActivity = groupChatInfoActivity;
        this.listFragment = listView;
        this.chatId = groupChatInfoActivity.getChatHandle();
        this.isPreview = groupChatInfoActivity.getChat().isPreview();

        megaApi = MegaApplication.getInstance().getMegaApi();
        megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        Display display = groupChatInfoActivity.getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        chatC = groupChatInfoActivity.getChatC();
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
        private ImageView verifiedIcon;
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
    }

    public static class ViewHolderParticipantsHeader extends ViewHolderParticipants {
        public ViewHolderParticipantsHeader(View v) {
            super(v);
        }

        private TextView infoNumParticipantsText;
        private ImageView editImageView;
        private LinearLayout notificationsLayout;
        private RelativeLayout notificationsSwitchLayout;
        private SwitchCompat notificationsSwitch;
        private TextView notificationsTitle;
        private TextView notificationsSubTitle;
        private View dividerNotifications;
        private LinearLayout chatLinkLayout;
        private View chatLinkSeparator;
        private LinearLayout privateLayout;
        private TextView privateTitle;
        private TextView privateText;
        private View privateSeparator;
        private RelativeLayout sharedFilesLayout;
        private RelativeLayout manageChatLayout;
        private TextView retentionTimeText;
        private View dividerClearLayout;
        private RelativeLayout leaveChatLayout;
        private View dividerLeaveLayout;
        private RelativeLayout archiveChatLayout;
        private TextView archiveChatTitle;
        private ImageView archiveChatIcon;
        private View archiveChatSeparator;
        private RelativeLayout observersLayout;
        private TextView observersNumberText;
        private RelativeLayout participantsLayout;
        private View observersSeparator;
        private RoundedImageView avatarImageView;
        private EmojiTextView infoTitleChatText;
    }

    @Override
    public ViewHolderParticipants onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;

        switch (viewType) {
            case ITEM_VIEW_TYPE_HEADER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_group_participants, parent, false);
                ViewHolderParticipantsHeader holderHeader = new ViewHolderParticipantsHeader(v);

                holderHeader.avatarImageView = v.findViewById(R.id.chat_group_properties_thumbnail);
                holderHeader.infoTitleChatText = v.findViewById(R.id.chat_group_contact_properties_info_title);
                if (isScreenInPortrait(groupChatInfoActivity)) {
                    holderHeader.infoTitleChatText.setMaxWidthEmojis(dp2px(MAX_WIDTH_CHAT_TITLE_PORT, outMetrics));
                } else {
                    holderHeader.infoTitleChatText.setMaxWidthEmojis(dp2px(MAX_WIDTH_CHAT_TITLE_LAND, outMetrics));
                }

                holderHeader.editImageView = v.findViewById(R.id.chat_group_contact_properties_edit_icon);
                holderHeader.editImageView.setOnClickListener(this);

                //Notifications Layout
                holderHeader.notificationsLayout = v.findViewById(R.id.chat_group_contact_properties_notifications_layout);
                holderHeader.notificationsLayout.setVisibility(View.VISIBLE);
                holderHeader.notificationsTitle = v.findViewById(R.id.chat_group_contact_properties_notifications_title);
                holderHeader.notificationsSubTitle = v.findViewById(R.id.chat_group_contact_properties_notifications_muted_text);
                holderHeader.notificationsSubTitle.setVisibility(View.GONE);
                holderHeader.notificationsSwitchLayout = v.findViewById(R.id.chat_group_contact_properties_layout);
                holderHeader.notificationsSwitchLayout.setOnClickListener(this);
                holderHeader.notificationsSwitch = v.findViewById(R.id.chat_group_contact_properties_switch);
                holderHeader.notificationsSwitch.setClickable(false);
                holderHeader.dividerNotifications = v.findViewById(R.id.divider_notifications_layout);

                holderHeader.infoNumParticipantsText = v.findViewById(R.id.chat_group_contact_properties_info_participants);

                //Chat links
                holderHeader.chatLinkLayout = v.findViewById(R.id.chat_group_contact_properties_chat_link_layout);
                holderHeader.chatLinkSeparator = v.findViewById(R.id.divider_chat_link_layout);

                //Private chat
                holderHeader.privateLayout = v.findViewById(R.id.chat_group_contact_properties_private_layout);
                holderHeader.privateTitle = v.findViewById(R.id.chat_group_contact_properties_private);
                holderHeader.privateText = v.findViewById(R.id.chat_group_contact_properties_private_text);
                holderHeader.privateSeparator = v.findViewById(R.id.divider_private_layout);

                //Chat Shared Files Layout
                holderHeader.sharedFilesLayout = v.findViewById(R.id.chat_group_contact_properties_chat_files_shared_layout);
                holderHeader.sharedFilesLayout.setOnClickListener(this);

                //Clear chat Layout
                holderHeader.manageChatLayout =  v.findViewById(R.id.manage_chat_history_group_info_layout);
                holderHeader.manageChatLayout.setOnClickListener(this);
                holderHeader.retentionTimeText =  v.findViewById(R.id.manage_chat_history_group_info_subtitle);
                holderHeader.retentionTimeText.setVisibility(View.GONE);
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
                holderHeader.participantsLayout =  v.findViewById(R.id.chat_group_contact_properties_participants_title);
                holderHeader.observersSeparator = v.findViewById(R.id.divider_observers_layout);

                v.setTag(holderHeader);
                return holderHeader;

            case ITEM_VIEW_TYPE_NORMAL:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_chat_list, parent, false);
                ViewHolderParticipantsList holderList = new ViewHolderParticipantsList(v);

                holderList.itemLayout = v.findViewById(R.id.participant_list_item_layout);
                holderList.imageView = v.findViewById(R.id.participant_list_thumbnail);
                holderList.verifiedIcon = v.findViewById(R.id.verified_icon);
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

                holderList.itemLayout.setTag(holderList);

                v.setTag(holderList);
                return holderList;

            case ITEM_VIEW_TYPE_ADD_PARTICIPANT:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_participant_chat_list, parent, false);
                ViewHolderAddParticipant holderAddParticipant = new ViewHolderAddParticipant(v);

                holderAddParticipant.itemLayout = v.findViewById(R.id.add_participant_list_item_layout);
                holderAddParticipant.itemLayout.setOnClickListener(this);

                v.setTag(holderAddParticipant);
                return holderAddParticipant;

            default:
                return null;
        }
    }

    /**
     * Method to know if a chat is inactive and no participants should be shown.
     *
     * @return True, in case the participant section has to be hidden. False, in the opposite case.
     */
    private boolean isNecessaryToHideParticipants() {
        return getChat().getPeerCount() == 0 && !getChat().isActive();
    }

    @Override
    public void onBindViewHolder(ViewHolderParticipants holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_HEADER:
                ViewHolderParticipantsHeader holderHeader = (ViewHolderParticipantsHeader) holder;

                String title = getChat().getTitle();
                holderHeader.avatarImageView.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR), title, AVATAR_SIZE, true));

                holderHeader.infoTitleChatText.setText(getChat().getTitle());

                if (getChat().isArchived()) {
                    holderHeader.archiveChatTitle.setText(groupChatInfoActivity.getString(R.string.general_unarchive));
                    holderHeader.archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.ic_b_unarchive));
                } else {
                    holderHeader.archiveChatTitle.setText(groupChatInfoActivity.getString(R.string.general_archive));
                    holderHeader.archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(groupChatInfoActivity, R.drawable.ic_b_archive));
                }

                long participantsCount = getChat().getPeerCount();
                holderHeader.participantsLayout.setVisibility(isNecessaryToHideParticipants() ? View.GONE : View.VISIBLE);

                if (isPreview) {
                    holderHeader.notificationsLayout.setVisibility(View.GONE);
                    holderHeader.dividerNotifications.setVisibility(View.GONE);
                    holderHeader.chatLinkLayout.setVisibility(View.GONE);
                    holderHeader.chatLinkSeparator.setVisibility(View.GONE);
                    holderHeader.privateLayout.setVisibility(View.GONE);
                    holderHeader.privateSeparator.setVisibility(View.GONE);
                    holderHeader.manageChatLayout.setVisibility(View.GONE);
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
                        holderHeader.manageChatLayout.setVisibility(View.VISIBLE);
                        holderHeader.dividerLeaveLayout.setVisibility(View.VISIBLE);
                        holderHeader.privateLayout.setVisibility(View.VISIBLE);
                        holderHeader.privateSeparator.setVisibility(View.VISIBLE);

                        if (!getChat().isPublic()) {
                            holderHeader.privateTitle.setText(R.string.private_chat);
                            holderHeader.privateTitle.setAllCaps(false);
                            holderHeader.privateTitle.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                            holderHeader.privateTitle.setTextColor(ContextCompat.getColor(groupChatInfoActivity, R.color.primary_text));
                            holderHeader.privateText.setText(R.string.make_chat_private_option_text);
                            holderHeader.privateLayout.setOnClickListener(null);
                        } else {
                            holderHeader.privateTitle.setText(R.string.make_chat_private_option);
                            holderHeader.privateTitle.setAllCaps(true);
                            holderHeader.privateTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

                            if (participantsCount <= MAX_PARTICIPANTS_CHANGE_TO_PRIVATE) {
                                holderHeader.privateTitle.setTextColor(ContextCompat.getColor(groupChatInfoActivity, R.color.accentColor));
                                holderHeader.privateText.setText(R.string.make_chat_private_option_text);
                                holderHeader.privateLayout.setOnClickListener(this);
                            } else {
                                holderHeader.privateTitle.setTextColor(ContextCompat.getColor(groupChatInfoActivity, R.color.emoji_icons));
                                holderHeader.privateText.setText(R.string.make_chat_private_not_available_text);
                                holderHeader.privateLayout.setOnClickListener(null);
                            }
                        }

                        updateRetentionTimeLayout(holderHeader.retentionTimeText, getUpdatedRetentionTimeFromAChat(getChat().getChatId()));
                    } else {
                        holderHeader.editImageView.setVisibility(View.GONE);
                        holderHeader.dividerClearLayout.setVisibility(View.GONE);
                        holderHeader.manageChatLayout.setVisibility(View.GONE);
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

                    if (getChat().isActive()) {
                        holderHeader.notificationsLayout.setVisibility(View.VISIBLE);
                        holderHeader.dividerNotifications.setVisibility(View.VISIBLE);
                    } else {
                        holderHeader.notificationsLayout.setVisibility(View.GONE);
                        holderHeader.dividerNotifications.setVisibility(View.GONE);
                    }

                    checkSpecificChatNotifications(chatId, holderHeader.notificationsSwitch, holderHeader.notificationsSubTitle);
                }

                holderHeader.infoNumParticipantsText.setText(isNecessaryToHideParticipants()
                        ? groupChatInfoActivity.getString(R.string.inactive_chat)
                        : groupChatInfoActivity.getResources().getQuantityString(R.plurals.subtitle_of_group_chat,
                        (int) participantsCount, (int) participantsCount));

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
                holderParticipantsList.itemLayout.setVisibility(isNecessaryToHideParticipants() ? View.GONE : View.VISIBLE);

                MegaChatParticipant participant = getParticipant(position);
                if (participant == null) return;

                long handle = participant.getHandle();
                holderParticipantsList.userHandle = MegaApiAndroid.userHandleToBase64(handle);
                holderParticipantsList.currentPosition = position;
                holderParticipantsList.imageView.setImageBitmap(null);

                Bitmap avatarBitmap = checkParticipant(holderParticipantsList, position, participant);
                if (avatarBitmap != null) {
                    holderParticipantsList.setImageView(avatarBitmap);
                } else {
                    /*Default Avatar*/
                    int avatarColor = getColorAvatar(holderParticipantsList.userHandle);
                    holderParticipantsList.imageView.setImageBitmap(getDefaultAvatar(avatarColor, holderParticipantsList.fullName, AVATAR_SIZE, true));
                }

                MegaUser contact = participant.isEmpty() ? null : megaApi.getContact(participant.getEmail());
                holderParticipantsList.verifiedIcon.setVisibility(contact != null && megaApi.areCredentialsVerified(contact) ? View.VISIBLE : View.GONE);


                int userStatus = handle == megaChatApi.getMyUserHandle() ? megaChatApi.getOnlineStatus() : getUserStatus(handle);
                setContactStatus(userStatus, ((ViewHolderParticipantsList) holder).statusImage, ((ViewHolderParticipantsList) holder).textViewContent);
                setContactLastGreen(groupChatInfoActivity, userStatus, participant.getLastGreen(), ((ViewHolderParticipantsList) holder).textViewContent);

                holderParticipantsList.textViewContactName.setText(holderParticipantsList.fullName);
                holderParticipantsList.threeDotsLayout.setOnClickListener(this);
                holderParticipantsList.itemLayout.setOnClickListener(this);
                holderParticipantsList.imageButtonThreeDots.setColorFilter(null);

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
        if (participants.isEmpty()) {
            return false;
        }

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
            return participants.size() + COUNT_HEADER_AND_ADD_PARTICIPANTS_POSITIONS;
        } else {
            return participants.size() + COUNT_HEADER_POSITION;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == HEADER_POSITION) {
            return ITEM_VIEW_TYPE_HEADER;
        } else if (isNotPreviewAndLastParticipantModerator() && position == ADD_PARTICIPANTS_POSITION) {
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
            return adapterPosition - COUNT_HEADER_AND_ADD_PARTICIPANTS_POSITIONS;
        } else {
            return adapterPosition - COUNT_HEADER_POSITION;
        }
    }

    /**
     * Gets the position of the participant in the adapter.
     * It depends on the user:
     *
     * If they is not previewing the chat and they are a moderator there are two more views in the adapter (Header + Add participant views),
     *      so the position in the adapter is the same as in the array plus 2.
     *
     * Otherwise, there is only one more view in the adapter (Header),
     *      so the position in the adapter is the same as in the array plus 1.
     *
     * @param arrayPosition   the position of the participant in the array.
     * @return The position of the participant in the adapter.
     */
    private int getParticipantPositionInAdapter(int arrayPosition) {
        if (isNotPreviewAndLastParticipantModerator()) {
            return arrayPosition + COUNT_HEADER_AND_ADD_PARTICIPANTS_POSITIONS;
        } else {
            return arrayPosition + COUNT_HEADER_POSITION;
        }
    }

    private MegaChatParticipant getParticipant(int position) {
        int positionInArray = getParticipantPositionInArray(position);

        if (positionInArray < 0 || positionInArray >= participants.size()) {
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

            case R.id.manage_chat_history_group_info_layout:
                Intent intentManageChat = new Intent(groupChatInfoActivity, ManageChatHistoryActivity.class);
                intentManageChat.putExtra(CHAT_ID, chatId);
                intentManageChat.putExtra(IS_FROM_CONTACTS, false);
                groupChatInfoActivity.startActivity(intentManageChat);
                break;

            case R.id.chat_group_contact_properties_archive_layout:
                new ChatController(groupChatInfoActivity).archiveChat(groupChatInfoActivity.getChat());
                break;

            case R.id.chat_group_contact_properties_layout:
                ViewHolderParticipantsHeader holderHeader = (ViewHolderParticipantsHeader) listFragment.findViewHolderForAdapterPosition(0);
                if (holderHeader != null) {
                    if (holderHeader.notificationsSwitch.isChecked()) {
                        createMuteNotificationsAlertDialogOfAChat(groupChatInfoActivity, chatId);
                    } else {
                        MegaApplication.getPushNotificationSettingManagement().controlMuteNotificationsOfAChat(groupChatInfoActivity, NOTIFICATIONS_ENABLED, chatId);
                    }
                }
                break;

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

    public void checkNotifications(long chatId) {
        ViewHolderParticipantsHeader holderHeader = (ViewHolderParticipantsHeader) listFragment.findViewHolderForAdapterPosition(0);
        if (holderHeader != null) {
            checkSpecificChatNotifications(chatId, holderHeader.notificationsSwitch, holderHeader.notificationsSubTitle);
        }
    }

    /**
     * Sets the participants in the adapter and notifies it.
     *
     * @param participants  participants' list to set
     */
    public void setParticipants(ArrayList<MegaChatParticipant> participants) {
        this.participants = participants;
        notifyDataSetChanged();
    }

    /**
     * Updates a participant in the adapter.
     *
     * @param positionInArray      position in
     * @param participants  list of MegaChatParticipant
     */
    public void updateParticipant(int positionInArray, ArrayList<MegaChatParticipant> participants) {
        this.participants = participants;
        notifyItemChanged(getParticipantPositionInAdapter(positionInArray));
    }

    /**
     * Updates a participant due to a change in their status.
     *
     * @param positionInArray  participant's position in the array without taking into account the header and add participants items.
     */
    public void updateContactStatus(int positionInArray) {
        int positionInAdapter = getParticipantPositionInAdapter(positionInArray);
        if (listFragment.findViewHolderForAdapterPosition(positionInAdapter) instanceof MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList) {
            notifyItemChanged(positionInAdapter);
        }
    }

    /**
     * Method for updating the text indicating the retention time.
     *
     * @param seconds The retention time.
     */
    public void updateRetentionTimeUI(long seconds) {
        ViewHolderParticipantsHeader holderHeader = (ViewHolderParticipantsHeader) listFragment.findViewHolderForAdapterPosition(0);
        if (holderHeader != null) {
            updateRetentionTimeLayout(holderHeader.retentionTimeText, seconds);
        }
    }

    /**
     * Checks if the participant has attributes.
     * If not, it stores the participant to ask for their when necessary.
     *
     * @param position      the position of the participant in the adapter.
     * @param participant   the participant to check.
     * @return The participant's avatar if they have, null otherwise.
     */
    private Bitmap checkParticipant(ViewHolderParticipantsList holderParticipantsList, int position, MegaChatParticipant participant) {
        boolean needUpdate = false;

        if (participant.isEmpty()) {
            long handle = participant.getHandle();

            String fullName = chatC.getParticipantFullName(handle);
            if (!isTextEmpty(fullName)) {
                participant.setFullName(fullName);
            }

            String email = chatC.getParticipantEmail(handle);
            if (!isTextEmpty(email)) {
                participant.setEmail(email);
            }

            if (groupChatInfoActivity.hasParticipantAttributes(participant)) {
                participant.setEmpty(false);
                needUpdate = true;
            }
        }

        holderParticipantsList.fullName = participant.getFullName();
        holderParticipantsList.contactMail = participant.getEmail();

        Bitmap avatarBitmap = null;

        /*Avatar*/
        String myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaChatApi.getMyUserHandle());
        if ((holderParticipantsList.userHandle).equals(myUserHandleEncoded)) {
            avatarBitmap = getUserAvatar(myUserHandleEncoded, megaApi.getMyEmail());
        } else {
            String nameFileHandle = holderParticipantsList.userHandle;
            String nameFileEmail = holderParticipantsList.contactMail;

            if (isTextEmpty(nameFileEmail)) {
                holderParticipantsList.imageButtonThreeDots.setColorFilter(ContextCompat.getColor(groupChatInfoActivity, R.color.chat_sliding_panel_separator));
                holderParticipantsList.threeDotsLayout.setOnClickListener(null);
                holderParticipantsList.itemLayout.setOnClickListener(null);
                avatarBitmap = getAvatarBitmap(nameFileHandle);
            } else {
                avatarBitmap = getUserAvatar(nameFileHandle, nameFileEmail);
            }
        }

        boolean hasAvatar = participant.hasAvatar();
        participant.setHasAvatar(avatarBitmap != null);

        if (!needUpdate && hasAvatar != participant.hasAvatar()) {
            needUpdate = true;
        }

        boolean avatar = participant.hasAvatar();

        if (needUpdate) {
            int arrayPosition = getParticipantPositionInArray(position);
            groupChatInfoActivity.updateParticipant(arrayPosition, participant);
            participants.set(arrayPosition, participant);
        }

        if (!groupChatInfoActivity.hasParticipantAttributes(participant) || avatarBitmap == null) {
            groupChatInfoActivity.addParticipantRequest(position, participant);
        }

        return avatarBitmap;
    }

    private MegaChatRoom getChat() {
        return groupChatInfoActivity.getChat();
    }
}
