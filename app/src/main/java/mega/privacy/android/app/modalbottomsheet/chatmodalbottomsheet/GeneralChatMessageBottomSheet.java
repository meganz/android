package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.openWith;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.showCannotOpenFileDialog;
import static mega.privacy.android.app.utils.ChatUtil.isGeolocation;
import static mega.privacy.android.app.utils.ChatUtil.shareMsgFromChat;
import static mega.privacy.android.app.utils.ChatUtil.shouldReactionBeClicked;
import static mega.privacy.android.app.utils.Constants.CANNOT_OPEN_FILE_SHOWN;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.HANDLE;
import static mega.privacy.android.app.utils.Constants.IMPORT_ONLY_OPTION;
import static mega.privacy.android.app.utils.Constants.INVALID_ID;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static mega.privacy.android.app.utils.Constants.POSITION_SELECTED_MESSAGE;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.OfflineUtils.availableOffline;
import static mega.privacy.android.app.utils.OfflineUtils.removeOffline;
import static mega.privacy.android.app.utils.Util.isOnline;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.ChatReactionsView;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import mega.privacy.android.app.usecase.GetNodeUseCase;
import mega.privacy.android.app.utils.AlertDialogUtil;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.permission.PermissionUtils;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@AndroidEntryPoint
public class GeneralChatMessageBottomSheet extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node = null;
    private MegaNodeList nodeList = null;
    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private int positionMessage;
    private long handle = INVALID_HANDLE;
    private ChatController chatC;
    private MegaChatRoom chatRoom;
    private LinearLayout reactionsLayout;
    private ChatReactionsView reactionsFragment;
    private RelativeLayout optionOpenWith;
    private View reactionSeparator;
    private RelativeLayout optionForward;
    private RelativeLayout optionEdit;
    private RelativeLayout optionCopy;
    private RelativeLayout optionShare;
    private RelativeLayout optionSelect;
    private RelativeLayout optionViewContacts;
    private RelativeLayout optionInfoContacts;
    private RelativeLayout optionStartConversation;
    private RelativeLayout optionInviteContact;
    private RelativeLayout optionImport;
    private RelativeLayout optionDownload;
    private SwitchMaterial offlineSwitch;
    private RelativeLayout optionDelete;
    private LinearLayout forwardSeparator;
    private LinearLayout editSeparator;
    private LinearLayout copySeparator;
    private LinearLayout shareSeparator;
    private LinearLayout selectSeparator;
    private LinearLayout infoSeparator;
    private LinearLayout inviteSeparator;
    private LinearLayout infoFileSeparator;
    private LinearLayout optionSaveOffline;
    private LinearLayout deleteSeparator;

    private AlertDialog cannotOpenFileDialog;

    @Inject
    GetNodeUseCase getNodeUseCase;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_general_chat_messages, null);
        reactionsLayout = contentView.findViewById(R.id.reactions_layout);
        reactionsFragment = contentView.findViewById(R.id.fragment_container_reactions);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        if (savedInstanceState != null) {
            Timber.d("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_ID);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_ID);
            positionMessage = savedInstanceState.getInt(POSITION_SELECTED_MESSAGE, INVALID_POSITION);
            handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
        } else {
            chatId = ((ChatActivity) requireActivity()).idChat;
            messageId = ((ChatActivity) requireActivity()).selectedMessageId;
            positionMessage = ((ChatActivity) requireActivity()).selectedPosition;
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        chatRoom = megaChatApi.getChatRoom(chatId);
        chatC = new ChatController(requireActivity());
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        reactionSeparator = contentView.findViewById(R.id.separator);
        optionOpenWith = contentView.findViewById(R.id.open_with_layout);
        forwardSeparator = contentView.findViewById(R.id.forward_separator);
        optionForward = contentView.findViewById(R.id.forward_layout);
        editSeparator = contentView.findViewById(R.id.edit_separator);
        optionEdit = contentView.findViewById(R.id.edit_layout);
        copySeparator = contentView.findViewById(R.id.copy_separator);
        optionCopy = contentView.findViewById(R.id.copy_layout);
        shareSeparator = contentView.findViewById(R.id.share_separator);
        optionShare = contentView.findViewById(R.id.share_layout);
        selectSeparator = contentView.findViewById(R.id.select_separator);
        optionSelect = contentView.findViewById(R.id.select_layout);
        infoSeparator = contentView.findViewById(R.id.info_separator);
        optionViewContacts = contentView.findViewById(R.id.option_view_layout);
        optionInfoContacts = contentView.findViewById(R.id.option_info_layout);
        inviteSeparator = contentView.findViewById(R.id.invite_separator);
        optionStartConversation = contentView.findViewById(R.id.option_start_conversation_layout);
        optionInviteContact = contentView.findViewById(R.id.option_invite_layout);
        infoFileSeparator = contentView.findViewById(R.id.info_file_separator);
        optionImport = contentView.findViewById(R.id.option_import_layout);
        optionDownload = contentView.findViewById(R.id.option_download_layout);
        optionSaveOffline = contentView.findViewById(R.id.option_save_offline_layout);
        offlineSwitch = contentView.findViewById(R.id.file_properties_switch);
        deleteSeparator = contentView.findViewById(R.id.delete_separator);
        optionDelete = contentView.findViewById(R.id.delete_layout);
        TextView textDelete = contentView.findViewById(R.id.delete_text);

        optionOpenWith.setOnClickListener(this);
        optionForward.setOnClickListener(this);
        optionEdit.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionShare.setOnClickListener(this);
        optionSelect.setOnClickListener(this);
        optionViewContacts.setOnClickListener(this);
        optionInfoContacts.setOnClickListener(this);
        optionStartConversation.setOnClickListener(this);
        optionInviteContact.setOnClickListener(this);
        optionImport.setOnClickListener(this);
        optionDownload.setOnClickListener(this);
        optionSaveOffline.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        if (message == null || message.getMessage() == null || chatRoom == null || message.isUploading()) {
            Timber.w("Message is null");
            closeDialog();
            return;
        }

        boolean isRemovedOrPendingMsg = ((ChatActivity) requireActivity()).hasMessagesRemovedOrPending(message.getMessage());
        if (isRemovedOrPendingMsg) {
            Timber.w("Message is removed or pending");
            closeDialog();
            return;
        }

        hideAllOptions();

        checkReactionsFragment();

        MegaChatMessage megaChatMessage = message.getMessage();
        int typeMessage = megaChatMessage.getType();

        optionSelect.setVisibility(View.VISIBLE);

        if (typeMessage == MegaChatMessage.TYPE_NORMAL || isGeolocation(megaChatMessage) ||
                (typeMessage == MegaChatMessage.TYPE_CONTAINS_META &&
                        megaChatMessage.getContainsMeta() != null &&
                        (megaChatMessage.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW))) {
            optionCopy.setVisibility(View.VISIBLE);
        } else {
            optionCopy.setVisibility(View.GONE);
        }

        if ((chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO) && !chatRoom.isPreview()) {
            optionForward.setVisibility(View.GONE);
            optionEdit.setVisibility(View.GONE);
            optionDelete.setVisibility(View.GONE);
            optionShare.setVisibility(View.GONE);

        } else {
            optionShare.setVisibility(typeMessage != MegaChatMessage.TYPE_NODE_ATTACHMENT
                    || !isOnline(requireContext()) || chatC.isInAnonymousMode()
                    ? View.GONE : View.VISIBLE);

            optionForward.setVisibility((!isOnline(requireContext()) || chatC.isInAnonymousMode()) ? View.GONE : View.VISIBLE);

            if (megaChatMessage.getUserHandle() != megaChatApi.getMyUserHandle() ||
                    !megaChatMessage.isEditable() ||
                    typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                optionEdit.setVisibility(View.GONE);
            } else {
                optionEdit.setVisibility(typeMessage == MegaChatMessage.TYPE_NORMAL ||
                        typeMessage == MegaChatMessage.TYPE_CONTAINS_META ? View.VISIBLE : View.GONE);
            }

            if (megaChatMessage.getUserHandle() != megaChatApi.getMyUserHandle() || !megaChatMessage.isDeletable()) {
                optionDelete.setVisibility(View.GONE);
            } else {
                if (megaChatMessage.getType() == MegaChatMessage.TYPE_NORMAL ||
                        (megaChatMessage.getType() == MegaChatMessage.TYPE_CONTAINS_META &&
                                megaChatMessage.getContainsMeta() != null &&
                                megaChatMessage.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION)) {
                    textDelete.setText(getString(R.string.delete_button));
                } else {
                    textDelete.setText(getString(R.string.context_remove));
                }
                optionDelete.setVisibility(View.VISIBLE);
            }
        }

        optionOpenWith.setVisibility(typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT ? View.VISIBLE : View.GONE);
        optionDownload.setVisibility(typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT ? View.VISIBLE : View.GONE);
        optionImport.setVisibility(typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT && !chatC.isInAnonymousMode() ? View.VISIBLE : View.GONE);

        if (typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
            long userCount = megaChatMessage.getUsersCount();
            long userHandle = megaChatMessage.getUserHandle(0);
            String userEmail = megaChatMessage.getUserEmail(0);

            optionInfoContacts.setVisibility((userCount == 1 &&
                    userHandle != megaChatApi.getMyUserHandle() &&
                    megaApi.getContact(userEmail) != null &&
                    megaApi.getContact(userEmail).getVisibility() == MegaUser.VISIBILITY_VISIBLE) ? View.VISIBLE : View.GONE);

            optionViewContacts.setVisibility(userCount > 1 ? View.VISIBLE : View.GONE);

            if (userCount == 1) {
                optionInviteContact.setVisibility(userHandle != megaChatApi.getMyUserHandle() &&
                        (megaApi.getContact(userEmail) == null ||
                                megaApi.getContact(userEmail).getVisibility() != MegaUser.VISIBILITY_VISIBLE) ? View.VISIBLE : View.GONE);

                optionStartConversation.setVisibility(userHandle != megaChatApi.getMyUserHandle() &&
                        megaApi.getContact(userEmail) != null &&
                        megaApi.getContact(userEmail).getVisibility() == MegaUser.VISIBILITY_VISIBLE &&
                        (chatRoom.isGroup() || userHandle != chatRoom.getPeerHandle(0)) ? View.VISIBLE : View.GONE);
            } else {
                optionStartConversation.setVisibility(View.VISIBLE);
                optionInviteContact.setVisibility(View.GONE);

                for (int i = 0; i < userCount; i++) {
                    String email = megaChatMessage.getUserEmail(i);
                    MegaUser contact = megaApi.getContact(email);
                    if (contact == null || contact.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
                        optionStartConversation.setVisibility(View.GONE);
                        break;
                    }
                }
            }
        }

        if (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
            getNode(megaChatMessage);
        } else {
            checkSeparatorsVisibility();
        }
        offlineSwitch.setOnCheckedChangeListener((v, isChecked) -> onClick(v));

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(CANNOT_OPEN_FILE_SHOWN, false)) {
            contentView.post(() -> cannotOpenFileDialog =
                    showCannotOpenFileDialog(this, requireActivity(),
                            node, ((ChatActivity) requireActivity())::saveNodeByTap));
        }
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        AlertDialogUtil.dismissAlertDialogIfExists(cannotOpenFileDialog);
        super.onDestroyView();
    }

    private void checkReactionsFragment() {
        boolean shouldReactionOptionBeVisible = shouldReactionBeClicked(chatRoom) && !message.isUploading();

        if (shouldReactionOptionBeVisible) {
            reactionsFragment.init(requireActivity(), this, chatId, messageId, positionMessage);
            reactionsLayout.setVisibility(View.VISIBLE);
        } else {
            reactionsLayout.setVisibility(View.GONE);
        }
        reactionSeparator.setVisibility(shouldReactionOptionBeVisible ? View.VISIBLE : View.GONE);
    }

    private void hideAllOptions() {
        optionSaveOffline.setVisibility(View.GONE);
        optionOpenWith.setVisibility(View.GONE);
        optionForward.setVisibility(View.GONE);
        optionEdit.setVisibility(View.GONE);
        optionCopy.setVisibility(View.GONE);
        optionShare.setVisibility(View.GONE);
        optionSelect.setVisibility(View.GONE);
        optionViewContacts.setVisibility(View.GONE);
        optionInfoContacts.setVisibility(View.GONE);
        optionStartConversation.setVisibility(View.GONE);
        optionInviteContact.setVisibility(View.GONE);
        optionImport.setVisibility(View.GONE);
        optionDownload.setVisibility(View.GONE);
        optionDelete.setVisibility(View.GONE);
        checkSeparatorsVisibility();
    }

    @Override
    public void onClick(View view) {
        if (message == null) {
            Timber.w("The message is NULL");
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        switch (view.getId()) {
            case R.id.open_with_layout:
                if (node == null) {
                    Timber.w("The selected node is NULL");
                    return;
                }
                cannotOpenFileDialog = openWith(this, requireActivity(), node, ((ChatActivity) requireActivity())::saveNodeByTap);
                return;

            case R.id.forward_layout:
                ((ChatActivity) requireActivity()).forwardMessages(messagesSelected);
                break;

            case R.id.edit_layout:
                ((ChatActivity) requireActivity()).editMessage(messagesSelected);
                break;

            case R.id.copy_layout:
                MegaChatMessage msg = message.getMessage();
                String text = isGeolocation(msg) ? msg.getContainsMeta().getTextMessage() :
                        ((ChatActivity) requireActivity()).copyMessage(message);
                ((ChatActivity) requireActivity()).copyToClipboard(text);
                break;

            case R.id.share_layout:
                if (node == null) {
                    Timber.w("The selected node is NULL");
                    return;
                }

                shareMsgFromChat(requireActivity(), message, chatId);
                break;

            case R.id.select_layout:
                ((ChatActivity) requireActivity()).activateActionModeWithItem(positionMessage);
                break;

            case R.id.option_view_layout:
                Timber.d("View option");
                ContactUtil.openContactAttachmentActivity(requireActivity(), chatId, messageId);
                break;

            case R.id.option_info_layout:
                if (!isOnline(requireContext())) {
                    ((ChatActivity) requireActivity()).showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                boolean isChatRoomOpen = chatRoom != null && !chatRoom.isGroup() &&
                        message.getMessage().getUserHandle(0) == chatRoom.getPeerHandle(0);
                ContactUtil.openContactInfoActivity(requireActivity(), message.getMessage().getUserEmail(0), isChatRoomOpen);
                break;

            case R.id.option_invite_layout:
                if (!isOnline(requireContext())) {
                    ((ChatActivity) requireActivity()).showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                ContactController cC = new ContactController(requireActivity());
                ArrayList<String> contactEmails;
                long usersCount = message.getMessage().getUsersCount();

                if (usersCount == 1) {
                    cC.inviteContact(message.getMessage().getUserEmail(0));
                } else {
                    Timber.d("Num users to invite: %s", usersCount);
                    contactEmails = new ArrayList<>();

                    for (int j = 0; j < usersCount; j++) {
                        String userMail = message.getMessage().getUserEmail(j);
                        contactEmails.add(userMail);
                    }
                    cC.inviteMultipleContacts(contactEmails);
                }
                break;

            case R.id.option_start_conversation_layout:
                long numUsers = message.getMessage().getUsersCount();

                if (numUsers == 1) {
                    ((ChatActivity) requireActivity()).startConversation(message.getMessage().getUserHandle(0));
                } else {
                    Timber.d("Num users to invite: %s", numUsers);
                    ArrayList<Long> contactHandles = new ArrayList<>();

                    for (int j = 0; j < numUsers; j++) {
                        long userHandle = message.getMessage().getUserHandle(j);
                        contactHandles.add(userHandle);
                    }
                    ((ChatActivity) requireActivity()).startGroupConversation(contactHandles);
                }
                break;

            case R.id.option_download_layout:
                if (node == null) {
                    Timber.w("The selected node is NULL");
                    return;
                }

                MegaNodeList nodeList = message.getMessage().getMegaNodeList();
                if (nodeList != null && nodeList.size() > 0) {
                    ((ChatActivity) requireActivity()).downloadNodeList(nodeList);
                }
                break;

            case R.id.option_import_layout:
                if (node == null) {
                    Timber.w("The selected node is NULL");
                    return;
                }

                chatC.importNode(messageId, chatId, IMPORT_ONLY_OPTION);
                break;

            case R.id.file_properties_switch:
            case R.id.option_save_offline_layout:
                if (message == null || node == null) {
                    Timber.w("Message or node is NULL");
                    return;
                }

                if (availableOffline(requireContext(), node)) {
                    MegaOffline mOffDelete = dbH.findByHandle(node.getHandle());
                    removeOffline(mOffDelete, dbH, requireContext());
                    Util.showSnackbar(
                            getActivity(), getResources().getString(R.string.file_removed_offline));
                } else {
                    PermissionUtils.checkNotificationsPermission(requireActivity());
                    ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
                    messages.add(message);
                    chatC.saveForOfflineWithAndroidMessages(messages,
                            megaChatApi.getChatRoom(chatId), (ChatActivity) requireActivity());
                }
                break;

            case R.id.delete_layout:
                ((ChatActivity) requireActivity()).showConfirmationDeleteMessages(messagesSelected, chatRoom);
                break;
        }
        closeDialog();
    }

    /**
     * Method to get the node related to the message
     */
    private void getNode(MegaChatMessage megaChatMessage) {
        if (megaChatMessage.getUserHandle() == megaChatApi.getMyUserHandle()) {
            getNodeUseCase.get(message.getMessage().getMegaNodeList().get(0).getHandle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result, throwable) -> {
                        if (throwable == null) {
                            node = result;
                        }

                        if (node == null) {
                            optionOpenWith.setVisibility(View.GONE);
                            optionForward.setVisibility(View.GONE);
                            optionEdit.setVisibility(View.GONE);
                            optionCopy.setVisibility(View.GONE);
                            optionShare.setVisibility(View.GONE);
                            optionViewContacts.setVisibility(View.GONE);
                            optionInfoContacts.setVisibility(View.GONE);
                            optionStartConversation.setVisibility(View.GONE);
                            optionInviteContact.setVisibility(View.GONE);
                            optionImport.setVisibility(View.GONE);
                            optionDownload.setVisibility(View.GONE);
                            optionSaveOffline.setVisibility(View.GONE);
                            offlineSwitch.setVisibility(View.GONE);
                        } else if (!chatC.isInAnonymousMode()) {
                            offlineSwitch.setChecked(availableOffline(requireContext(), node));
                            optionSaveOffline.setVisibility(View.VISIBLE);
                        }

                        checkSeparatorsVisibility();
                    });
        } else {
            nodeList = megaChatMessage.getMegaNodeList();
            if (nodeList == null || nodeList.size() == 0) {
                Timber.w("NodeList is NULL or empty");
                return;
            }

            node = handle == INVALID_HANDLE ? nodeList.get(0) : getNodeByHandle(handle);
            if (node == null) {
                Timber.w("Node is NULL");
                return;
            }
        }
    }

    /**
     * Get MegaNode
     *
     * @param handle The handle of the node
     * @return The MegaNode
     */
    public MegaNode getNodeByHandle(long handle) {
        for (int i = 0; i < nodeList.size(); i++) {
            MegaNode node = nodeList.get(i);
            if (node.getHandle() == handle) {
                return node;
            }
        }

        return null;
    }

    /**
     * Method for checking the visibility of option separators
     */
    private void checkSeparatorsVisibility() {
        forwardSeparator.setVisibility(optionOpenWith.getVisibility() == View.VISIBLE &&
                optionForward.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        editSeparator.setVisibility(optionForward.getVisibility() == View.VISIBLE &&
                optionEdit.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        copySeparator.setVisibility((optionEdit.getVisibility() == View.VISIBLE ||
                optionForward.getVisibility() == View.VISIBLE) &&
                optionCopy.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        shareSeparator.setVisibility(optionForward.getVisibility() == View.VISIBLE &&
                optionShare.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        selectSeparator.setVisibility((optionSelect.getVisibility() == View.VISIBLE &&
                (optionForward.getVisibility() == View.VISIBLE ||
                        optionCopy.getVisibility() == View.VISIBLE)) ? View.VISIBLE : View.GONE);

        infoSeparator.setVisibility((optionViewContacts.getVisibility() == View.VISIBLE ||
                optionInfoContacts.getVisibility() == View.VISIBLE) &&
                optionSelect.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        inviteSeparator.setVisibility((optionStartConversation.getVisibility() == View.VISIBLE ||
                optionInviteContact.getVisibility() == View.VISIBLE) &&
                (optionViewContacts.getVisibility() == View.VISIBLE ||
                        optionInfoContacts.getVisibility() == View.VISIBLE ||
                        selectSeparator.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);

        infoFileSeparator.setVisibility((optionImport.getVisibility() == View.VISIBLE ||
                optionDownload.getVisibility() == View.VISIBLE ||
                optionSaveOffline.getVisibility() == View.VISIBLE) &&
                optionSelect.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        deleteSeparator.setVisibility(optionDelete.getVisibility());
    }

    public void closeDialog() {
        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putLong(POSITION_SELECTED_MESSAGE, positionMessage);
        outState.putLong(HANDLE, handle);
        outState.putBoolean(CANNOT_OPEN_FILE_SHOWN, AlertDialogUtil.isAlertDialogShown(cannotOpenFileDialog));
    }
}
