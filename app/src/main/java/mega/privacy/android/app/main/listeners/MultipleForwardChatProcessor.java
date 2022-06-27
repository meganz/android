package mega.privacy.android.app.main.listeners;

import static mega.privacy.android.app.utils.ChatUtil.getMegaChatMessage;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatGiphy;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import timber.log.Timber;


//Listener for  multi forward
public class MultipleForwardChatProcessor implements MegaChatRequestListenerInterface {

    private Context context;

    private long[] chatHandles;
    private long[] idMessages;
    private long idChat;

    private MegaChatApiAndroid megaChatApi;

    private ChatController cC;
    private NodeController nC;

    public MultipleForwardChatProcessor(Context context, long[] chatHandles, long[] idMessages, long idChat) {

        this.context = context;
        this.idMessages = idMessages;
        this.chatHandles = chatHandles;
        this.idChat = idChat;

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        cC = new ChatController(context);
        nC = new NodeController(context);
    }

    int counter = 0;
    int error = 0;
    int errorNotAvailable = 0;
    int totalMessages = 0;

    private void checkTypeVoiceClip(MegaChatMessage msg, int value) {
        MegaNodeList nodeList = msg.getMegaNodeList();
        if (nodeList == null) return;

        if (msg.getUserHandle() == megaChatApi.getMyUserHandle()) {
            for (int j = 0; j < nodeList.size(); j++) {
                MegaNode temp = nodeList.get(j);
                megaChatApi.attachVoiceMessage(chatHandles[value], temp.getHandle(), this);
            }
        } else {
            for (int j = 0; j < nodeList.size(); j++) {
                MegaNode temp = nodeList.get(j);
                MegaNode nodeToAttach = nC.checkIfNodeIsMine(temp);
                if (nodeToAttach != null) {
                    megaChatApi.attachVoiceMessage(chatHandles[value], nodeToAttach.getHandle(), this);
                } else {
                    Timber.w("The node: %d is not mine. Not attached.", temp.getHandle());
                }
            }
        }

    }

    private void checkTypeMeta(MegaChatMessage msg, int value) {

        MegaChatContainsMeta meta = msg.getContainsMeta();
        if (meta == null) {
            Timber.w("Meta is null");
            return;
        }

        switch (meta.getType()) {
            case MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW:
                String text = meta.getRichPreview().getText();

                if (chatHandles[value] == idChat) {
                    ((ChatActivity) context).sendMessage(text);
                } else {
                    megaChatApi.sendMessage(chatHandles[value], text);
                }
                break;

            case MegaChatContainsMeta.CONTAINS_META_GEOLOCATION:
                String image = meta.getGeolocation().getImage();
                float latitude = meta.getGeolocation().getLatitude();
                float longitude = meta.getGeolocation().getLongitude();

                if (chatHandles[value] == idChat) {
                    ((ChatActivity) context).sendLocationMessage(longitude, latitude, image);
                } else {
                    megaChatApi.sendGeolocation(chatHandles[value], longitude, latitude, image);
                }
                break;

            case MegaChatContainsMeta.CONTAINS_META_GIPHY:
                MegaChatGiphy giphy = meta.getGiphy();

                if (chatHandles[value] == idChat) {
                    ((ChatActivity) context).sendGiphyMessageFromMegaChatGiphy(giphy);
                } else {
                    megaChatApi.sendGiphy(chatHandles[value], giphy.getMp4Src(), giphy.getWebpSrc(), giphy.getMp4Size(), giphy.getWebpSize()
                            , giphy.getWidth(), giphy.getHeight(), giphy.getTitle());
                }
                break;
        }

        checkTotalMessages();
    }

    public void forward(MegaChatRoom chatRoom) {
        for (int k = 0; k < chatHandles.length; k++) {
            for (int i = 0; i < idMessages.length; i++) {
                Timber.d("Forward: %d, Chat ID: %d", idMessages[i], chatHandles[k]);

                MegaChatMessage messageToForward = getMegaChatMessage(context, megaChatApi, idChat, idMessages[i]);
                if (messageToForward == null) {
                    Timber.w("ERROR: message is null on forwarding");
                    continue;
                }

                int type = messageToForward.getType();
                Timber.d("Type of message to forward: %s", type);

                switch (type) {
                    case MegaChatMessage.TYPE_NORMAL: {
                        String text = messageToForward.getContent();
                        if (chatHandles[k] == idChat) {
                            ((ChatActivity) context).sendMessage(text);
                        } else {
                            megaChatApi.sendMessage(chatHandles[k], text);
                        }
                        checkTotalMessages();
                        break;
                    }
                    case MegaChatMessage.TYPE_CONTACT_ATTACHMENT: {
                        MegaChatMessage contactMessage = megaChatApi.forwardContact(idChat, messageToForward.getMsgId(), chatHandles[k]);
                        if (chatHandles[k] == idChat) {
                            if (contactMessage != null) {
                                AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(contactMessage);
                                ((ChatActivity) context).sendMessageToUI(androidMsgSent);
                            }
                        }
                        checkTotalMessages();
                        break;
                    }
                    case MegaChatMessage.TYPE_NODE_ATTACHMENT: {
                        MegaNodeList nodeList = messageToForward.getMegaNodeList();
                        if (nodeList == null) continue;

                        if (messageToForward.getUserHandle() == megaChatApi.getMyUserHandle()) {
                            for (int j = 0; j < nodeList.size(); j++) {
                                MegaNode temp = nodeList.get(j);
                                megaChatApi.attachNode(chatHandles[k], temp.getHandle(), this);
                            }
                        } else {
                            for (int j = 0; j < nodeList.size(); j++) {
                                MegaNode temp = nodeList.get(j);
                                MegaNode nodeToAttach = nC.checkIfNodeIsMine(temp);
                                if (nodeToAttach != null) {
                                    nodeToAttach = cC.authorizeNodeIfPreview(nodeToAttach, chatRoom);
                                    megaChatApi.attachNode(chatHandles[k], nodeToAttach.getHandle(), this);
                                } else {
                                    Timber.w("The node: %d is not mine. Not attached.", temp.getHandle());
                                }
                            }
                        }
                        break;
                    }
                    case MegaChatMessage.TYPE_VOICE_CLIP: {
                        checkTypeVoiceClip(messageToForward, k);
                        break;
                    }
                    case MegaChatMessage.TYPE_CONTAINS_META: {
                        checkTypeMeta(messageToForward, k);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.d("onRequestFinish: %s", request.getRequestString());

        if (request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getChatHandle() == idChat) {
                    AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(request.getMegaChatMessage());
                    if (androidMsgSent != null && context instanceof ChatActivity) {
                        ((ChatActivity) context).sendMessageToUI(androidMsgSent);
                    }
                }
                checkTotalMessages();
            } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                errorNotAvailable++;
                Timber.d("MultipleForwardChatProcessor: %s %d", context.getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available, errorNotAvailable, errorNotAvailable), e.getErrorCode());
            } else {
                error++;
                Timber.e("Attach node error: %s__%d", e.getErrorString(), e.getErrorCode());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    private void checkTotalMessages() {
        totalMessages++;
        Timber.d("Total messages processed: %s", totalMessages);
        if (totalMessages >= chatHandles.length * idMessages.length) {
            Timber.d("All messages processed");

            int success = totalMessages - error - errorNotAvailable;

            if (context instanceof ChatActivity) {
                if (success > 0) {
                    //A message has been forwarded
                    String text = null;
                    int totalErrors = error + errorNotAvailable;
                    if (totalErrors == 0) {
                        if (chatHandles.length > 1) {
                            text = context.getString(R.string.messages_forwarded_success);
                        }
                    } else if (totalErrors == errorNotAvailable) {
                        text = context.getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available, totalErrors, totalErrors);
                    } else {
                        text = context.getResources().getQuantityString(R.plurals.messages_forwarded_partial_error, totalErrors, totalErrors);
                    }

                    if (chatHandles.length == 1) {
                        ((ChatActivity) context).openChatAfterForward(chatHandles[0], text);
                    } else {
                        ((ChatActivity) context).openChatAfterForward(-1, text);
                    }
                } else {
                    //No messages forwarded
                    int totalErrors = error + errorNotAvailable;
                    if (totalErrors == errorNotAvailable) {
                        ((ChatActivity) context).showSnackbar(SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available, totalErrors, totalErrors), -1);
                    } else {
                        String text = context.getResources().getQuantityString(R.plurals.messages_forwarded_partial_error, totalErrors, totalErrors);
                        ((ChatActivity) context).showSnackbar(SNACKBAR_TYPE, text, -1);
                    }

                    ((ChatActivity) context).removeProgressDialog();
                }
            } else if (context instanceof NodeAttachmentHistoryActivity) {
                if (success > 0) {
                    //A message has been forwarded
                    String text = null;
                    int totalErrors = error + errorNotAvailable;
                    if (totalErrors == 0) {
                        text = context.getResources().getQuantityString(R.plurals.messages_forwarded_success_plural, totalMessages);
                    } else if (totalErrors == errorNotAvailable) {
                        text = context.getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available, totalErrors, totalErrors);
                    } else {
                        text = context.getResources().getQuantityString(R.plurals.messages_forwarded_partial_error, totalErrors, totalErrors);
                    }

                    ((NodeAttachmentHistoryActivity) context).showSnackbar(SNACKBAR_TYPE, text);
                } else {
                    //No messages forwarded
                    int totalErrors = error + errorNotAvailable;
                    if (totalErrors == errorNotAvailable) {
                        ((NodeAttachmentHistoryActivity) context).showSnackbar(SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available, totalErrors, totalErrors));
                    } else {
                        String text = context.getResources().getQuantityString(R.plurals.messages_forwarded_partial_error, totalErrors, totalErrors);
                        ((NodeAttachmentHistoryActivity) context).showSnackbar(SNACKBAR_TYPE, text);
                    }
                }
                ((NodeAttachmentHistoryActivity) context).removeProgressDialog();
            }
        }
    }
}
