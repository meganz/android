package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.listeners.GetPeerAttributesListener;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatAttachmentAvatarListener;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MessageVoiceClip;
import mega.privacy.android.app.lollipop.megachat.PendingMessageSingle;
import mega.privacy.android.app.lollipop.megachat.RemovedMessage;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUtilsAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LinksUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaChatMessage.END_CALL_REASON_CANCELLED;
import static nz.mega.sdk.MegaChatMessage.END_CALL_REASON_NO_ANSWER;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener, RotatableAdapter {

    private static int MAX_WIDTH_FILENAME_LAND = 455;
    private static int MAX_WIDTH_FILENAME_PORT = 180;

    //margins of management message and hour
    private final static int MANAGEMENT_MESSAGE_LAND = 28;
    private final static int MANAGEMENT_MESSAGE_PORT = 48;

    //margins of management message and hour in a CALL
    private static int MANAGEMENT_MESSAGE_CALL_LAND = 40;
    private static int MANAGEMENT_MESSAGE_CALL_PORT = 60;

    //paddings of hours (right and left)
    private static int PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND = 9;
    private static int PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT = 16;

    //margins of normal/attachment contacts messages and hour
    private static int CONTACT_MESSAGE_LAND = 28;
    private static int CONTACT_MESSAGE_PORT = 48;

    private final static int MAX_WIDTH_NAME_LAND = 340;
    private final static int MAX_WIDTH_NAME_PORT = 200;

    private final static int MAX_WIDTH_MESSAGE_LAND = 310;
    private final static int MAX_WIDTH_MESSAGE_PORT = 275;

    private final static int TYPE_HEADER = 0;
    private final static int TYPE_ITEM = 1;

    private final static int LAYOUT_WIDTH = 330;

    Context context;
    private int positionClicked;
    private ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
    private ArrayList<RemovedMessage> removedMessages = new ArrayList<>();

    private RecyclerView listFragment;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    boolean multipleSelect;
    HashMap<Long, Integer> messagesSelectedInChat = new HashMap<>();

    private MegaChatLollipopAdapter megaChatAdapter;
    private ArrayList<MessageVoiceClip> messagesPlaying;
    private int placeholderCount = 0;

    private Handler handlerVoiceNotes;
    private Runnable runnableVC;
    ChatController cC;

    private long myUserHandle = -1;

    DisplayMetrics outMetrics;
    DatabaseHandler dbH = null;
    MegaChatRoom chatRoom;
    private HashMap<Long, Long> pendingPreviews = new HashMap<>();

    private class ChatVoiceClipAsyncTask extends AsyncTask<MegaNodeList, Void, Integer> {
        MegaChatLollipopAdapter.ViewHolderMessageChat holder;
        int position;
        long userHandle;
        MegaNodeList nodeList;

        public ChatVoiceClipAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder, int position, long userHandle) {
            this.holder = holder;
            this.userHandle = userHandle;
            this.position = position;
        }

        @Override
        protected void onPreExecute() {
            if (holder == null) {
                holder = (ViewHolderMessageChat) listFragment.findViewHolderForAdapterPosition(position);
            }
            if (holder == null) return;

            logDebug("ChatVoiceClipAsyncTask:onPreExecute");
            if (userHandle == myUserHandle) {
                holder.uploadingOwnProgressbarVoiceclip.setVisibility(View.VISIBLE);
                holder.contentOwnMessageVoiceClipPlay.setVisibility(View.GONE);
                holder.notAvailableOwnVoiceclip.setVisibility(View.GONE);
            } else {
                holder.uploadingContactProgressbarVoiceclip.setVisibility(View.VISIBLE);
                holder.contentContactMessageVoiceClipPlay.setVisibility(View.GONE);
                holder.notAvailableContactVoiceclip.setVisibility(View.GONE);
            }
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(MegaNodeList... params) {
            nodeList = params[0];
            ((ChatActivityLollipop) context).sendToDownload(nodeList);
            return 1;
        }
    }

    private class ChatPreviewAsyncTask extends AsyncTask<MegaNode, Void, Integer> {

        MegaChatLollipopAdapter.ViewHolderMessageChat holder;
        MegaNode node;
        Bitmap preview;
        long msgId;

        public ChatPreviewAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder, long msgId) {
            this.holder = holder;
            this.msgId = msgId;
        }

        @Override
        protected Integer doInBackground(MegaNode... params) {
            logDebug("ChatPreviewAsyncTask-doInBackground");
            node = params[0];
            preview = getPreviewFromFolder(node, context);

            if (preview != null) {
                previewCache.put(node.getHandle(), preview);
                return 0;
            } else {
                if (pendingPreviews.containsKey(node.getHandle())) {
                    logDebug("The preview is already downloaded or added to the list");
                    return 1;
                } else {
                    return 2;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer param) {
            logDebug("ChatPreviewAsyncTask-onPostExecute");
            if (param == 0) {
                logDebug("Preview recovered from folder");
                int position = holder.getCurrentPosition();
                if(position<=messages.size()){
                    AndroidMegaChatMessage message = messages.get(position - 1);
                    if (message.getMessage() != null) {
                        if (message.getMessage().getMegaNodeList() != null) {
                            if (message.getMessage().getMegaNodeList().get(0) != null) {
                                long nodeMessageHandle = message.getMessage().getMegaNodeList().get(0).getHandle();

                                if (nodeMessageHandle == node.getHandle()) {
                                    if (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                                        setOwnPreview(holder, preview, node);
                                        int status = message.getMessage().getStatus();
                                        if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                            setErrorStateOnPreview(holder, preview);
                                        }
                                    } else {
                                        setContactPreview(holder, preview, node);
                                    }

                                } else {
                                    logWarning("The nodeHandles are not equal!");
                                }
                            }
                        }
                    }
                }else{
                    logWarning("Messages removed");
                }
            } else if (param == 2) {
                File previewFile = new File(getPreviewFolder(context), node.getBase64Handle() + ".jpg");
                logDebug("GET PREVIEW OF HANDLE: " + node.getHandle() + " to download here: " + previewFile.getAbsolutePath());
                pendingPreviews.put(node.getHandle(), msgId);
                PreviewDownloadListener listener = new PreviewDownloadListener(context, (ViewHolderMessageChat) holder, megaChatAdapter, node);
                megaApi.getPreview(node, previewFile.getAbsolutePath(), listener);
            }
        }
    }

    private class ChatLocalPreviewAsyncTask extends AsyncTask<MegaNode, Void, Integer> {

        MegaNode node;
        Bitmap preview;
        File cacheDir;
        File destination;
        long msgId;
        MegaChatLollipopAdapter.ViewHolderMessageChat holder;

        public ChatLocalPreviewAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder, long msgId) {
            this.holder = holder;
            this.msgId = msgId;
        }

        @Override
        protected Integer doInBackground(MegaNode... params) {
            logDebug("ChatLocalPreviewAsyncTask-doInBackground");

            node = params[0];

            if (node == null) {
                return 3;
            }
            preview = getPreviewFromFolder(node, context);

            if (preview != null) {
                previewCache.put(node.getHandle(), preview);
                return 0;
            } else {
                destination = buildPreviewFile(context, node.getName());

                if (isFileAvailable(destination)) {
                    if (destination.length() == node.getSize()) {
                        File previewDir = getPreviewFolder(context);
                        File previewFile = new File(previewDir, node.getBase64Handle() + ".jpg");
                        logDebug("Base 64 handle: " + node.getBase64Handle() + ", Handle: " + node.getHandle());
                        boolean previewCreated = MegaUtilsAndroid.createPreview(destination, previewFile);

                        if (previewCreated) {
                            preview = getBitmapForCache(previewFile, context);
                            destination.delete();
                            return 0;
                        } else {
                            return 1;
                        }
                    } else {
                        destination.delete();
                        return 1;
                    }
                }

                if (pendingPreviews.containsKey(node.getHandle())) {
                    logDebug("The image is already downloaded or added to the list");
                    return 1;
                } else {
                    return 2;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer param) {
            logDebug("ChatLocalPreviewAsyncTask-onPostExecute");

            if (param == 0) {
                int position = holder.getCurrentPosition();

                AndroidMegaChatMessage message = messages.get(position - 1);

                long nodeMessageHandle = message.getMessage().getMegaNodeList().get(0).getHandle();

                if (nodeMessageHandle == node.getHandle()) {
                    if (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                        setOwnPreview(holder, preview, node);
                        int status = message.getMessage().getStatus();
                        if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                            setErrorStateOnPreview(holder, preview);
                        }
                    } else {

                    }

                } else {
                    logWarning("The nodeHandles are not equal!");
                }
            } else if (param == 2) {
                logWarning("No preview and no generated correctly");
            }
        }
    }

    private class ChatUploadingPreviewAsyncTask extends AsyncTask<String, Void, Boolean> {
        String filePath;
        MegaChatLollipopAdapter adapter;
        int position;

        public ChatUploadingPreviewAsyncTask(MegaChatLollipopAdapter adapter, int position) {
            this.adapter = adapter;
            this.position = position;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            filePath = params[0];
            File currentFile = new File(filePath);
            if (MimeTypeList.typeForName(filePath).isImage()) {
                logDebug("Is image");

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                //ARGB_8888 would create huge memory pressure to app, since we are creating preview, we don't need to have ARGB_8888 as the standard
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap preview;

                ExifInterface exif;
                int orientation = ExifInterface.ORIENTATION_NORMAL;
                try {
                    exif = new ExifInterface(currentFile.getAbsolutePath());
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {
                    logWarning("EXCEPTION", e);
                }

                // Calculate inSampleSize
                options.inSampleSize = Util.calculateInSampleSize(options, 1000, 1000);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;

                preview = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
                if (preview != null) {
                    preview = rotateBitmap(preview, orientation);

                    long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(filePath));
                    if (preview != null) {
                        //put preview bitmap to memory cache
                        setPreviewCache(fingerprintCache, preview);
                        return true;
                    }
                    return false;
                }
            } else if (MimeTypeList.typeForName(filePath).isPdf()) {
                logDebug("Is pdf");

                FileOutputStream out = null;
                int pageNumber = 0;
                try {

                    PdfiumCore pdfiumCore = new PdfiumCore(context);
                    File previewDir = getPreviewFolder(context);
                    File previewFile = new File(previewDir, currentFile.getName() + ".jpg");

                    PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(currentFile, ParcelFileDescriptor.MODE_READ_ONLY));
                    pdfiumCore.openPage(pdfDocument, pageNumber);
                    int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
                    int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
                    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
                    Bitmap preview = resizeBitmapUpload(bmp, width, height);
                    out = new FileOutputStream(previewFile);
                    boolean result = preview.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                    pdfiumCore.closeDocument(pdfDocument);

                    if (preview != null && result) {
                        logDebug("Compress OK");
                        long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(previewFile.getPath()));
                        //put preview bitmap to memory cache
                        setPreviewCache(fingerprintCache, preview);
                        return true;
                    } else if (!result) {
                        logWarning("Not Compress");
                        return false;
                    }
                } catch (Exception e) {
                    logError("Pdf thumbnail could not be created", e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Exception e) {
                        logWarning("Error closing FileOutputStream", e);
                    }
                }
            } else if (MimeTypeList.typeForName(filePath).isVideo()) {
                logDebug("Is video");
                File previewDir = getPreviewFolder(context);
                File previewFile = new File(previewDir, currentFile.getName() + ".jpg");

                Bitmap bmPreview = createVideoPreview(filePath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                if (bmPreview == null) {
                    logWarning("Create video preview NULL");
//                    bmPreview= ThumbnailUtilsLollipop.loadVideoThumbnail(filePath, context);
                } else {
                    logDebug("Create Video preview worked!");
                }

                if (bmPreview != null) {
                    try {
                        previewFile.createNewFile();
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(previewFile);
//                            Bitmap resizedBitmap = ThumbnailUtilsLollipop.resizeBitmapUpload(bmPreview, bmPreview.getWidth(), bmPreview.getHeight());
                            boolean result = bmPreview.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                            if (result) {
                                logDebug("Compress OK");
                                long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(previewFile.getPath()));
                                //put preview bitmap to memory cache
                                setPreviewCache(fingerprintCache, bmPreview);
                                return true;
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            logError("Error with FileOutputStream", e);
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                logError("Error closing FileOutputStream", e);
                            }
                        }

                    } catch (IOException e1) {
                        logError("Error creating new preview file", e1);
                    }
                } else {
                    logWarning("Create video preview NULL");
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean isContinue) {
            logDebug("ChatUploadingPreviewAsyncTask-onPostExecute");
            if (isContinue) {
                //notify adapter to update view
                adapter.notifyItemChanged(position);
            } else {
                logWarning("The preview is NULL!");
            }
        }
    }

    public MegaChatLollipopAdapter(Context _context, MegaChatRoom chatRoom, ArrayList<AndroidMegaChatMessage> _messages, ArrayList<MessageVoiceClip> _messagesPlaying, ArrayList<RemovedMessage> _removedMessages, RecyclerView _listView) {
        logDebug("New adapter");
        this.context = _context;
        this.messages = _messages;
        this.positionClicked = INVALID_POSITION;
        this.chatRoom = chatRoom;
        this.removedMessages = _removedMessages;
        this.messagesPlaying = _messagesPlaying;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        listFragment = _listView;

        megaChatAdapter = this;

        if (messages != null) {
            logDebug("Number of messages: " + messages.size());
        } else {
            logWarning("Number of messages: NULL");
        }

        myUserHandle = megaChatApi.getMyUserHandle();
        logDebug("MyUserHandle: " + myUserHandle);
    }

    public void updateChatRoom(MegaChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public static class ViewHolderMessageChat extends RecyclerView.ViewHolder{
        public ViewHolderMessageChat(View view) {
            super(view);
        }

        boolean contentVisible;
        int currentPosition;
        long userHandle;
        String fullNameTitle;
        //        boolean nameRequested = false;
        boolean nameRequestedAction = false;

        RelativeLayout itemLayout;

        RelativeLayout previewFramePort;
        RelativeLayout previewFrameLand;

        RelativeLayout dateLayout;
        TextView dateText;
        RelativeLayout ownMessageLayout;
        RelativeLayout titleOwnMessage;
        //        TextView meText;
        TextView timeOwnText;
        RelativeLayout contentOwnMessageLayout;
        private RelativeLayout ownMessageSelectLayout;
        private ImageView ownMessageSelectIcon;
        private EmojiTextView contentOwnMessageText;

        //Own rich links
        RelativeLayout urlOwnMessageLayout;
        RelativeLayout urlOwnMessageTextrl;
        RelativeLayout forwardOwnRichLinks;

        private EmojiTextView urlOwnMessageText;
        LinearLayout urlOwnMessageWarningButtonsLayout;
        Button neverRichLinkButton;
        Button alwaysAllowRichLinkButton;
        Button notNowRichLinkButton;
        RelativeLayout urlOwnMessageTitleLayout;
        private EmojiTextView urlOwnMessageTitle;
        TextView urlOwnMessageDescription;

        LinearLayout urlOwnMessageDisableButtonsLayout;
        Button noDisableButton;
        Button yesDisableButton;

        LinearLayout urlOwnMessageIconAndLinkLayout;
        ImageView urlOwnMessageIcon;
        TextView urlOwnMessageLink;

        RoundedImageView urlOwnMessageImage;

        RelativeLayout urlOwnMessageGroupAvatarLayout;
        RoundedImageView urlOwnMessageGroupAvatar;
        TextView urlOwnMessageGroupAvatarText;

        RelativeLayout urlContactMessageGroupAvatarLayout;
        RoundedImageView urlContactMessageGroupAvatar;
        TextView urlContactMessageGroupAvatarText;

        //Contact's rich links
        RelativeLayout urlContactMessageLayout;
        private EmojiTextView urlContactMessageText;
        RelativeLayout urlContactMessageTitleLayout;
        private EmojiTextView urlContactMessageTitle;
        TextView urlContactMessageDescription;
        RelativeLayout forwardContactRichLinks;

        LinearLayout urlContactMessageIconAndLinkLayout;
        ImageView urlContactMessageIcon;
        TextView urlContactMessageLink;
        RelativeLayout errorUploadingRichLink;
        RoundedImageView urlContactMessageImage;

        RoundedImageView contentOwnMessageThumbLand;
        RelativeLayout gradientOwnMessageThumbLand;
        ImageView videoIconOwnMessageThumbLand;
        TextView videoTimecontentOwnMessageThumbLand;

        RoundedImageView contentOwnMessageThumbPort;
        RelativeLayout gradientOwnMessageThumbPort;
        ImageView videoIconOwnMessageThumbPort;
        TextView videoTimecontentOwnMessageThumbPort;

        RelativeLayout contentOwnMessageFileLayout;
        ImageView contentOwnMessageFileThumb;
        TextView contentOwnMessageFileName;
        TextView contentOwnMessageFileSize;
        RelativeLayout errorUploadingFile;
        RelativeLayout errorUploadingContact;

        RelativeLayout contentOwnMessageVoiceClipLayout;
        ImageView contentOwnMessageVoiceClipPlay;
        SeekBar contentOwnMessageVoiceClipSeekBar;
        TextView contentOwnMessageVoiceClipDuration;
        RelativeLayout errorUploadingVoiceClip;
        long totalDurationOfVoiceClip;
        RelativeLayout uploadingOwnProgressbarVoiceclip;
        ImageView notAvailableOwnVoiceclip;
        RelativeLayout uploadingContactProgressbarVoiceclip;
        ImageView notAvailableContactVoiceclip;

        RelativeLayout contentOwnMessageContactLayout;
        RelativeLayout contentOwnMessageContactLayoutAvatar;
        RoundedImageView contentOwnMessageContactThumb;
        private EmojiTextView contentOwnMessageContactName;
        public EmojiTextView contentOwnMessageContactEmail;
        RelativeLayout forwardOwnContact;

        ImageView iconOwnTypeDocLandPreview;
        ImageView iconOwnTypeDocPortraitPreview;

        RelativeLayout transparentCoatingLandscape;
        RelativeLayout transparentCoatingPortrait;
        RelativeLayout uploadingProgressBarPort;
        RelativeLayout uploadingProgressBarLand;

        RelativeLayout errorUploadingPortrait;
        RelativeLayout errorUploadingLandscape;

        RelativeLayout forwardOwnPortrait;
        RelativeLayout forwardOwnLandscape;
        RelativeLayout forwardOwnFile;

        LinearLayout newMessagesLayout;
        TextView newMessagesText;

        TextView retryAlert;
        ImageView triangleIcon;

        //Location message
        RelativeLayout transparentCoatingLocation;
        RelativeLayout uploadingProgressBarLocation;
        RelativeLayout forwardOwnMessageLocation;
        RelativeLayout mainOwnMessageItemLocation;
        RoundedImageView previewOwnLocation;
        RelativeLayout separatorPreviewOwnLocation;
        RelativeLayout triangleErrorLocation;
        RelativeLayout pinnedOwnLocationLayout;
        TextView pinnedOwnLocationInfoText;
        TextView pinnedLocationTitleText;

        //Contact's message

        RelativeLayout contactMessageLayout;
        RelativeLayout titleContactMessage;

        TextView timeContactText;
        private EmojiTextView nameContactText;

        RoundedImageView contactImageView;
        RelativeLayout contentContactMessageLayout;
        private RelativeLayout contactMessageSelectLayout;
        private ImageView contactMessageSelectIcon;
        private EmojiTextView contentContactMessageText;

        RoundedImageView contentContactMessageThumbLand;
        RelativeLayout gradientContactMessageThumbLand;
        ImageView videoIconContactMessageThumbLand;
        TextView videoTimecontentContactMessageThumbLand;
        RelativeLayout forwardContactPreviewLandscape;

        RoundedImageView contentContactMessageThumbPort;
        RelativeLayout gradientContactMessageThumbPort;
        ImageView videoIconContactMessageThumbPort;
        TextView videoTimecontentContactMessageThumbPort;
        RelativeLayout forwardContactPreviewPortrait;

        RelativeLayout contentContactMessageAttachLayout;

        RelativeLayout contentContactMessageFile;
        RelativeLayout forwardContactFile;
        ImageView contentContactMessageFileThumb;
        TextView contentContactMessageFileName;
        TextView contentContactMessageFileSize;

        RelativeLayout layoutAvatarMessages;

        RelativeLayout contentContactMessageContactLayout;
        RelativeLayout forwardContactContact;
        RelativeLayout contentContactMessageContactLayoutAvatar;
        RoundedImageView contentContactMessageContactThumb;
        private EmojiTextView contentContactMessageContactName;
        public EmojiTextView contentContactMessageContactEmail;
        private EmojiTextView contentContactMessageContactInitialLetter;

        RelativeLayout contentContactMessageVoiceClipLayout;
        ImageView contentContactMessageVoiceClipPlay;
        SeekBar contentContactMessageVoiceClipSeekBar;
        TextView contentContactMessageVoiceClipDuration;

        ImageView iconContactTypeDocLandPreview;
        ImageView iconContactTypeDocPortraitPreview;

        RelativeLayout ownManagementMessageLayout;
        private EmojiTextView ownManagementMessageText;
        ImageView ownManagementMessageIcon;

        private EmojiTextView contactManagementMessageText;
        ImageView contactManagementMessageIcon;
        RelativeLayout contactManagementMessageLayout;

        //Location message
        RelativeLayout forwardContactMessageLocation;
        RelativeLayout mainContactMessageItemLocation;
        RoundedImageView previewContactLocation;
        RelativeLayout separatorPreviewContactLocation;
        TextView pinnedContactLocationTitleText;
        RelativeLayout pinnedContactLocationLayout;
        TextView pinnedContactLocationInfoText;

        public String filePathUploading;

        public long getUserHandle() {
            return userHandle;
        }

        public int getCurrentPosition() {
            return currentPosition;
        }

        public void setMyImageView(Bitmap bitmap) {
            contentOwnMessageContactThumb.setImageBitmap(bitmap);
        }

        public void setContactImageView(Bitmap bitmap) {
            contentContactMessageContactThumb.setImageBitmap(bitmap);
            contentContactMessageContactInitialLetter.setVisibility(View.GONE);
        }
    }

    public static class ViewHolderHeaderChat extends RecyclerView.ViewHolder {
        public ViewHolderHeaderChat(View view) {
            super(view);
        }

        RelativeLayout firstMessage;
        ImageView loadingMessages;
    }

    ViewHolderMessageChat holder;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            logDebug("Create header");
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_item_chat, parent, false);
            ViewHolderHeaderChat holder = new ViewHolderHeaderChat(v);

            holder.firstMessage = v.findViewById(R.id.first_message_chat);
            holder.loadingMessages = v.findViewById(R.id.loading_messages_image);

            return holder;
        } else {
            logDebug("Create item message");
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);
            dbH = DatabaseHandler.getDbHandler(context);

            cC = new ChatController(context);

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_chat, parent, false);
            holder = new ViewHolderMessageChat(v);
            holder.contentVisible = true;
            holder.itemLayout = v.findViewById(R.id.message_chat_item_layout);
            holder.dateLayout = v.findViewById(R.id.message_chat_date_layout);
            //Margins
            RelativeLayout.LayoutParams dateLayoutParams = (RelativeLayout.LayoutParams) holder.dateLayout.getLayoutParams();
            dateLayoutParams.setMargins(0, scaleHeightPx(8, outMetrics), 0, scaleHeightPx(8, outMetrics));
            holder.dateLayout.setLayoutParams(dateLayoutParams);

            holder.dateText = v.findViewById(R.id.message_chat_date_text);

            holder.newMessagesLayout = v.findViewById(R.id.message_chat_new_relative_layout);
            holder.newMessagesText = v.findViewById(R.id.message_chat_new_text);

            if (((ChatActivityLollipop) context).getDeviceDensity() == 1) {

                MANAGEMENT_MESSAGE_CALL_LAND = 45;
                MANAGEMENT_MESSAGE_CALL_PORT = 65;

                CONTACT_MESSAGE_LAND = 31;
                CONTACT_MESSAGE_PORT = 55;

                PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND = 10;
                PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT = 18;
            }

            //Own messages

            holder.ownMessageLayout = v.findViewById(R.id.message_chat_own_message_layout);
            holder.titleOwnMessage = v.findViewById(R.id.title_own_message_layout);
            holder.timeOwnText = v.findViewById(R.id.message_chat_time_text);

            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holder.titleOwnMessage.setPadding(0, 0, scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics), 0);
            } else {
                holder.titleOwnMessage.setPadding(0, 0, scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics), 0);
            }

            holder.previewFramePort = v.findViewById(R.id.preview_frame_portrait);
            holder.previewFrameLand = v.findViewById(R.id.preview_frame_landscape);

            holder.contentOwnMessageLayout = v.findViewById(R.id.content_own_message_layout);
            holder.ownMessageSelectLayout = v.findViewById(R.id.own_message_select_layout);
            holder.ownMessageSelectIcon = v.findViewById(R.id.own_message_select_icon);
            holder.ownMessageSelectLayout.setVisibility(View.GONE);

            holder.contentOwnMessageText = v.findViewById(R.id.content_own_message_text);
            holder.contentOwnMessageText.setNeccessaryShortCode(false);

            //Own rich links message
            holder.urlOwnMessageLayout = v.findViewById(R.id.url_own_message_layout);
            holder.urlOwnMessageLayout.setVisibility(View.GONE);
            holder.urlOwnMessageTextrl = v.findViewById(R.id.url_own_message_text_rl);

            if (((ChatActivityLollipop) context).getDeviceDensity() == 1 && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                holder.urlOwnMessageLayout.getLayoutParams().width = LAYOUT_WIDTH;
            }
            holder.forwardOwnRichLinks = v.findViewById(R.id.forward_own_rich_links);
            holder.forwardOwnRichLinks.setTag(holder);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);

            holder.urlOwnMessageText = v.findViewById(R.id.url_own_message_text);
            holder.urlOwnMessageText.setNeccessaryShortCode(false);
            holder.urlOwnMessageText.setTag(holder);

            holder.urlOwnMessageWarningButtonsLayout = v.findViewById(R.id.url_own_message_buttons_warning_layout);
            holder.neverRichLinkButton = v.findViewById(R.id.url_never_button);
            holder.alwaysAllowRichLinkButton = v.findViewById(R.id.url_always_allow_button);
            holder.notNowRichLinkButton = v.findViewById(R.id.url_not_now_button);

            holder.urlOwnMessageDisableButtonsLayout = v.findViewById(R.id.url_own_message_buttons_disable_layout);
            holder.yesDisableButton = v.findViewById(R.id.url_yes_disable_button);
            holder.noDisableButton = v.findViewById(R.id.url_no_disable_button);
            holder.urlOwnMessageTitleLayout = v.findViewById(R.id.url_own_message_enable_layout_inside);
            holder.urlOwnMessageTitle = v.findViewById(R.id.url_own_message_title);
            holder.urlOwnMessageDescription = v.findViewById(R.id.url_own_message_description);

            holder.urlOwnMessageIconAndLinkLayout = v.findViewById(R.id.url_own_message_icon_link_layout);
            holder.urlOwnMessageIcon = v.findViewById(R.id.url_own_message_icon);
            holder.urlOwnMessageLink = v.findViewById(R.id.url_own_message_link);

            holder.urlOwnMessageImage = v.findViewById(R.id.url_own_message_image);
            int radiusImageRL = scaleWidthPx(10, outMetrics);
            holder.urlOwnMessageImage.setCornerRadius(radiusImageRL);
            holder.urlOwnMessageImage.setBorderWidth(0);
            holder.urlOwnMessageImage.setOval(false);

            //Group avatar of chat links
            holder.urlOwnMessageGroupAvatarLayout = v.findViewById(R.id.url_chat_own_message_image);
            holder.urlOwnMessageGroupAvatar = v.findViewById(R.id.content_url_chat_own_message_contact_thumb);
            holder.urlOwnMessageGroupAvatarText = v.findViewById(R.id.content_url_chat_own_message_contact_initial_letter);

            //Group avatar of chat links
            holder.urlContactMessageGroupAvatarLayout = v.findViewById(R.id.url_chat_contact_message_image);
            holder.urlContactMessageGroupAvatar = v.findViewById(R.id.content_url_chat_contact_message_contact_thumb);
            holder.urlContactMessageGroupAvatarText = v.findViewById(R.id.content_url_chat_contact_message_contact_initial_letter);

            int radius;
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                radius = scaleWidthPx(10, outMetrics);
            } else {
                radius = scaleWidthPx(15, outMetrics);
            }
            int colors[] = {0x70000000, 0x00000000};
            GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, colors);
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadii(new float[]{radius, 0, radius, 0, radius, radius, radius, radius});

            holder.contentOwnMessageThumbLand = v.findViewById(R.id.content_own_message_thumb_landscape);
            holder.contentOwnMessageThumbLand.setCornerRadius(radius);
            holder.contentOwnMessageThumbLand.setBorderWidth(1);
            holder.contentOwnMessageThumbLand.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentOwnMessageThumbLand.setOval(false);

            holder.gradientOwnMessageThumbLand = v.findViewById(R.id.gradient_own_message_thumb_landscape);
            holder.gradientOwnMessageThumbLand.setBackground(shape);

            holder.videoIconOwnMessageThumbLand = v.findViewById(R.id.video_icon_own_message_thumb_landscape);
            holder.videoTimecontentOwnMessageThumbLand = v.findViewById(R.id.video_time_own_message_thumb_landscape);

            holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            holder.contentOwnMessageThumbPort = v.findViewById(R.id.content_own_message_thumb_portrait);
            holder.contentOwnMessageThumbPort.setCornerRadius(radius);
            holder.contentOwnMessageThumbPort.setBorderWidth(1);
            holder.contentOwnMessageThumbPort.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentOwnMessageThumbPort.setOval(false);

            holder.errorUploadingFile = v.findViewById(R.id.error_uploading_file);
            holder.errorUploadingContact = v.findViewById(R.id.error_uploading_contact);
            holder.errorUploadingRichLink = v.findViewById(R.id.error_uploading_rich_link);

            holder.gradientOwnMessageThumbPort = v.findViewById(R.id.gradient_own_message_thumb_portrait);
            holder.gradientOwnMessageThumbPort.setBackground(shape);

            holder.videoIconOwnMessageThumbPort = v.findViewById(R.id.video_icon_own_message_thumb_portrait);
            holder.videoTimecontentOwnMessageThumbPort = v.findViewById(R.id.video_time_own_message_thumb_portrait);

            holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            holder.contentOwnMessageFileLayout = v.findViewById(R.id.content_own_message_file_layout);
            holder.forwardOwnFile = v.findViewById(R.id.forward_own_file);
            holder.forwardOwnFile.setTag(holder);
            holder.forwardOwnFile.setVisibility(View.GONE);

            holder.contentOwnMessageFileThumb = v.findViewById(R.id.content_own_message_file_thumb);
            holder.contentOwnMessageFileName = v.findViewById(R.id.content_own_message_file_name);
            holder.contentOwnMessageFileSize = v.findViewById(R.id.content_own_message_file_size);

            holder.totalDurationOfVoiceClip = 0;

            //my voice clip:
            holder.contentOwnMessageVoiceClipLayout = v.findViewById(R.id.content_own_message_voice_clip_layout);
            holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);
            holder.contentOwnMessageVoiceClipPlay = v.findViewById(R.id.content_own_message_voice_clip_play_pause);
            holder.contentOwnMessageVoiceClipPlay.setTag(holder);
            holder.contentOwnMessageVoiceClipSeekBar = v.findViewById(R.id.content_own_message_voice_clip_seekBar);
            holder.contentOwnMessageVoiceClipSeekBar.setProgress(0);
            holder.contentOwnMessageVoiceClipDuration = v.findViewById(R.id.content_own_message_voice_clip_duration);
            holder.contentOwnMessageVoiceClipDuration.setText(milliSecondsToTimer(0));
            holder.uploadingOwnProgressbarVoiceclip = v.findViewById(R.id.uploading_own_progressbar_voiceclip);
            holder.uploadingOwnProgressbarVoiceclip.setVisibility(View.GONE);

            holder.notAvailableOwnVoiceclip = v.findViewById(R.id.content_own_message_voice_clip_not_available);
            holder.notAvailableOwnVoiceclip.setVisibility(View.GONE);
            holder.notAvailableOwnVoiceclip.setTag(holder);
            holder.notAvailableOwnVoiceclip.setOnClickListener(this);

            holder.errorUploadingVoiceClip = v.findViewById(R.id.error_uploading_voice_clip);

            holder.contentOwnMessageContactLayout = v.findViewById(R.id.content_own_message_contact_layout);
            holder.contentOwnMessageContactLayoutAvatar = v.findViewById(R.id.content_own_message_contact_layout_avatar);
            holder.contentOwnMessageContactThumb = v.findViewById(R.id.content_own_message_contact_thumb);
            holder.contentOwnMessageContactName = v.findViewById(R.id.content_own_message_contact_name);
            holder.contentOwnMessageContactName.setNeccessaryShortCode(false);
            holder.contentOwnMessageContactEmail = v.findViewById(R.id.content_own_message_contact_email);

            holder.forwardOwnContact = v.findViewById(R.id.forward_own_contact);
            holder.forwardOwnContact.setTag(holder);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.iconOwnTypeDocLandPreview = v.findViewById(R.id.own_attachment_type_icon_lands);
            holder.iconOwnTypeDocPortraitPreview = v.findViewById(R.id.own_attachment_type_icon_portrait);

            holder.retryAlert = v.findViewById(R.id.not_sent_own_message_text);
            holder.triangleIcon = v.findViewById(R.id.own_triangle_icon);

            holder.transparentCoatingPortrait = v.findViewById(R.id.transparent_coating_portrait);
            holder.transparentCoatingPortrait.setVisibility(View.GONE);

            holder.transparentCoatingLandscape = v.findViewById(R.id.transparent_coating_landscape);
            holder.transparentCoatingLandscape.setVisibility(View.GONE);

            holder.uploadingProgressBarPort = v.findViewById(R.id.uploadingProgressBarPort);
            holder.uploadingProgressBarPort.setVisibility(View.GONE);
            holder.uploadingProgressBarLand = v.findViewById(R.id.uploadingProgressBarLand);
            holder.uploadingProgressBarLand.setVisibility(View.GONE);

            holder.errorUploadingPortrait = v.findViewById(R.id.error_uploading_portrait);
            holder.errorUploadingPortrait.setVisibility(View.GONE);
            holder.errorUploadingLandscape = v.findViewById(R.id.error_uploading_landscape);
            holder.errorUploadingLandscape.setVisibility(View.GONE);

            holder.forwardOwnPortrait = v.findViewById(R.id.forward_own_preview_portrait);
            holder.forwardOwnPortrait.setTag(holder);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setOnClickListener(this);

            holder.forwardOwnLandscape = v.findViewById(R.id.forward_own_preview_landscape);
            holder.forwardOwnLandscape.setTag(holder);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setOnClickListener(this);

            holder.ownManagementMessageText = v.findViewById(R.id.own_management_message_text);
            holder.ownManagementMessageText.setNeccessaryShortCode(false);
            holder.ownManagementMessageLayout = v.findViewById(R.id.own_management_message_layout);
            holder.ownManagementMessageIcon = v.findViewById(R.id.own_management_message_icon);

            //Contact messages
            holder.contactMessageLayout = v.findViewById(R.id.message_chat_contact_message_layout);
            holder.titleContactMessage = v.findViewById(R.id.title_contact_message_layout);

            holder.contactImageView = v.findViewById(R.id.contact_thumbnail);
            holder.timeContactText = v.findViewById(R.id.contact_message_chat_time_text);
            holder.nameContactText = v.findViewById(R.id.contact_message_chat_name_text);

            holder.contentContactMessageLayout = v.findViewById(R.id.content_contact_message_layout);
            holder.contactMessageSelectLayout = v.findViewById(R.id.contact_message_select_layout);
            holder.contactMessageSelectIcon = v.findViewById(R.id.contact_message_select_icon);
            holder.contactMessageSelectLayout.setVisibility(View.GONE);

            holder.contentContactMessageText = v.findViewById(R.id.content_contact_message_text);
            holder.contentContactMessageText.setNeccessaryShortCode(false);
            holder.contentContactMessageThumbLand = v.findViewById(R.id.content_contact_message_thumb_landscape);

            holder.contentContactMessageThumbLand.setCornerRadius(radius);
            holder.contentContactMessageThumbLand.setBorderWidth(1);
            holder.contentContactMessageThumbLand.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentContactMessageThumbLand.setOval(false);
            holder.forwardContactPreviewLandscape = v.findViewById(R.id.forward_contact_preview_landscape);
            holder.forwardContactPreviewLandscape.setTag(holder);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);

            //Contact rich links message
            holder.urlContactMessageLayout = v.findViewById(R.id.url_contact_message_layout);
            if (((ChatActivityLollipop) context).getDeviceDensity() == 1 && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                holder.urlContactMessageLayout.getLayoutParams().width = LAYOUT_WIDTH;
            }

            holder.forwardContactRichLinks = v.findViewById(R.id.forward_contact_rich_links);
            holder.forwardContactRichLinks.setTag(holder);
            holder.forwardContactRichLinks.setVisibility(View.GONE);

            holder.urlContactMessageText = v.findViewById(R.id.url_contact_message_text);
            holder.urlContactMessageText.setNeccessaryShortCode(false);
            holder.urlContactMessageText.setTag(holder);

            holder.urlContactMessageTitleLayout = v.findViewById(R.id.url_contact_message_enable_layout_inside);
            holder.urlContactMessageTitle = v.findViewById(R.id.url_contact_message_title);
            holder.urlContactMessageDescription = v.findViewById(R.id.url_contact_message_description);

            holder.urlContactMessageIconAndLinkLayout = v.findViewById(R.id.url_contact_message_icon_link_layout);
            holder.urlContactMessageIcon = v.findViewById(R.id.url_contact_message_icon);
            holder.urlContactMessageLink = v.findViewById(R.id.url_contact_message_link);

            holder.urlContactMessageImage = v.findViewById(R.id.url_contact_message_image);
            holder.urlContactMessageImage.setCornerRadius(radiusImageRL);
            holder.urlContactMessageImage.setBorderWidth(0);
            holder.urlContactMessageImage.setOval(false);

            holder.contentContactMessageThumbPort = v.findViewById(R.id.content_contact_message_thumb_portrait);
            holder.contentContactMessageThumbPort.setCornerRadius(radius);
            holder.contentContactMessageThumbPort.setBorderWidth(1);
            holder.contentContactMessageThumbPort.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentContactMessageThumbPort.setOval(false);
            holder.forwardContactPreviewPortrait = v.findViewById(R.id.forward_contact_preview_portrait);
            holder.forwardContactPreviewPortrait.setTag(holder);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.gradientContactMessageThumbLand = v.findViewById(R.id.gradient_contact_message_thumb_landscape);
            holder.gradientContactMessageThumbLand.setBackground(shape);

            holder.videoIconContactMessageThumbLand = v.findViewById(R.id.video_icon_contact_message_thumb_landscape);
            holder.videoTimecontentContactMessageThumbLand = v.findViewById(R.id.video_time_contact_message_thumb_landscape);

            holder.gradientContactMessageThumbLand.setVisibility(View.GONE);

            holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            holder.gradientContactMessageThumbPort = v.findViewById(R.id.gradient_contact_message_thumb_portrait);
            holder.gradientContactMessageThumbPort.setBackground(shape);

            holder.videoIconContactMessageThumbPort = v.findViewById(R.id.video_icon_contact_message_thumb_portrait);
            holder.videoTimecontentContactMessageThumbPort = v.findViewById(R.id.video_time_contact_message_thumb_portrait);

            holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            holder.contentContactMessageAttachLayout = v.findViewById(R.id.content_contact_message_attach_layout);

            holder.contentContactMessageFile = v.findViewById(R.id.content_contact_message_file);
            holder.forwardContactFile = v.findViewById(R.id.forward_contact_file);
            holder.forwardContactFile.setTag(holder);
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.contentContactMessageFileThumb = v.findViewById(R.id.content_contact_message_file_thumb);
            holder.contentContactMessageFileName = v.findViewById(R.id.content_contact_message_file_name);
            holder.contentContactMessageFileSize = v.findViewById(R.id.content_contact_message_file_size);

            holder.layoutAvatarMessages = v.findViewById(R.id.layout_avatar);
            holder.contentContactMessageContactLayout = v.findViewById(R.id.content_contact_message_contact_layout);

            //contact voice clip:
            holder.contentContactMessageVoiceClipLayout = v.findViewById(R.id.content_contact_message_voice_clip_layout);
            RelativeLayout.LayoutParams paramsVoiceClip = (RelativeLayout.LayoutParams) holder.contentContactMessageVoiceClipLayout.getLayoutParams();
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                paramsVoiceClip.leftMargin = scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
            } else {
                paramsVoiceClip.leftMargin = scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
            }
            holder.contentContactMessageVoiceClipLayout.setLayoutParams(paramsVoiceClip);
            holder.contentContactMessageVoiceClipLayout.setVisibility(View.GONE);
            holder.contentContactMessageVoiceClipPlay = v.findViewById(R.id.content_contact_message_voice_clip_play_pause);
            holder.contentContactMessageVoiceClipPlay.setTag(holder);
            holder.contentContactMessageVoiceClipSeekBar = v.findViewById(R.id.content_contact_message_voice_clip_seekBar);
            holder.contentContactMessageVoiceClipSeekBar.setProgress(0);
            holder.contentContactMessageVoiceClipDuration = v.findViewById(R.id.content_contact_message_voice_clip_duration);
            holder.contentContactMessageVoiceClipDuration.setText(milliSecondsToTimer(0));
            holder.uploadingContactProgressbarVoiceclip = v.findViewById(R.id.uploading_contact_progressbar_voiceclip);
            holder.uploadingContactProgressbarVoiceclip.setVisibility(View.GONE);
            holder.notAvailableContactVoiceclip = v.findViewById(R.id.content_contact_message_voice_clip_not_available);
            holder.notAvailableContactVoiceclip.setVisibility(View.GONE);
            holder.notAvailableContactVoiceclip.setTag(holder);
            holder.notAvailableContactVoiceclip.setOnClickListener(this);

            holder.forwardContactContact = v.findViewById(R.id.forward_contact_contact);
            holder.forwardContactContact.setTag(holder);
            holder.forwardContactContact.setVisibility(View.GONE);

            holder.contentContactMessageContactLayoutAvatar = v.findViewById(R.id.content_contact_message_contact_layout_avatar);
            holder.contentContactMessageContactThumb = v.findViewById(R.id.content_contact_message_contact_thumb);
            holder.contentContactMessageContactName = v.findViewById(R.id.content_contact_message_contact_name);
            holder.contentContactMessageContactName.setNeccessaryShortCode(false);
            holder.contentContactMessageContactEmail = v.findViewById(R.id.content_contact_message_contact_email);
            holder.contentContactMessageContactInitialLetter = v.findViewById(R.id.content_contact_message_contact_initial_letter);

            holder.iconContactTypeDocLandPreview = v.findViewById(R.id.contact_attachment_type_icon_lands);
            holder.iconContactTypeDocPortraitPreview = v.findViewById(R.id.contact_attachment_type_icon_portrait);

            holder.contactManagementMessageLayout = v.findViewById(R.id.contact_management_message_layout);
            holder.contactManagementMessageText = v.findViewById(R.id.contact_management_message_text);
            holder.contactManagementMessageText.setNeccessaryShortCode(false);
            holder.contactManagementMessageIcon = v.findViewById(R.id.contact_management_message_icon);

            //Location message
            holder.transparentCoatingLocation = v.findViewById(R.id.transparent_coating_location);
            holder.uploadingProgressBarLocation = v.findViewById(R.id.uploadingProgressBarLocation);
            holder.forwardOwnMessageLocation = v.findViewById(R.id.forward_own_location);
            holder.forwardOwnMessageLocation.setTag(holder);
            holder.forwardOwnMessageLocation.setVisibility(View.GONE);
            holder.mainOwnMessageItemLocation = v.findViewById(R.id.own_main_item_location);
            holder.previewOwnLocation = v.findViewById(R.id.own_rounded_imageview_location);
            holder.previewOwnLocation.setCornerRadius(px2dp(12, outMetrics));
            holder.previewOwnLocation.setBorderWidth(0);
            holder.previewOwnLocation.setOval(false);
            holder.separatorPreviewOwnLocation = v.findViewById(R.id.own_separator_imageview_location);

            if (((ChatActivityLollipop) context).getDeviceDensity() == 1 && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                holder.previewOwnLocation.getLayoutParams().width = LAYOUT_WIDTH;
                holder.separatorPreviewOwnLocation.getLayoutParams().width = LAYOUT_WIDTH;
            }

            holder.triangleErrorLocation = v.findViewById(R.id.error_uploading_location);

            holder.pinnedOwnLocationLayout = v.findViewById(R.id.own_pinned_location_layout);
            holder.pinnedOwnLocationInfoText = v.findViewById(R.id.own_info_pinned_location);
            holder.pinnedLocationTitleText = v.findViewById(R.id.own_title_pinned_location);

            holder.forwardContactMessageLocation = v.findViewById(R.id.forward_contact_location);
            holder.forwardContactMessageLocation.setTag(holder);
            holder.forwardContactMessageLocation.setVisibility(View.GONE);
            holder.mainContactMessageItemLocation = v.findViewById(R.id.contact_main_item_location);
            holder.previewContactLocation = v.findViewById(R.id.contact_rounded_imageview_location);
            holder.previewContactLocation.setCornerRadius(px2dp(12, outMetrics));
            holder.previewContactLocation.setBorderWidth(0);
            holder.previewContactLocation.setOval(false);
            holder.separatorPreviewContactLocation = v.findViewById(R.id.contact_separator_imageview_location);
            if (((ChatActivityLollipop) context).getDeviceDensity() == 1 && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                holder.previewContactLocation.getLayoutParams().width = LAYOUT_WIDTH;
                holder.separatorPreviewContactLocation.getLayoutParams().width = LAYOUT_WIDTH;
            }

            holder.pinnedContactLocationTitleText = v.findViewById(R.id.contact_title_pinned_location);
            holder.pinnedContactLocationLayout = v.findViewById(R.id.contact_pinned_location_layout);
            holder.pinnedContactLocationInfoText = v.findViewById(R.id.contact_info_pinned_location);

            RelativeLayout.LayoutParams paramsLocation = (RelativeLayout.LayoutParams) holder.mainContactMessageItemLocation.getLayoutParams();
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                paramsLocation.leftMargin = scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
            } else {
                paramsLocation.leftMargin = scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
            }
            holder.mainContactMessageItemLocation.setLayoutParams(paramsLocation);

            if (((ChatActivityLollipop) context).getDeviceDensity() == 1) {
                MAX_WIDTH_FILENAME_LAND = 290;
                MAX_WIDTH_FILENAME_PORT = 140;
            }

            RelativeLayout.LayoutParams paramsContactContact = (RelativeLayout.LayoutParams) holder.contentContactMessageContactLayout.getLayoutParams();
            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            RelativeLayout.LayoutParams paramsContactAttach = (RelativeLayout.LayoutParams) holder.contentContactMessageAttachLayout.getLayoutParams();
            RelativeLayout.LayoutParams paramsContactRichLink = (RelativeLayout.LayoutParams) holder.urlContactMessageLayout.getLayoutParams();

            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();

            if (!isScreenInPortrait(context)) {
                paramsContactContact.leftMargin = scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
                paramsContactManagement.rightMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
                paramsContactAttach.leftMargin = scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
                paramsContactRichLink.leftMargin = scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);

                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
                paramsOwnManagement.rightMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
                holder.nameContactText.setMaxWidthEmojis(px2dp(MAX_WIDTH_NAME_LAND, outMetrics));
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics), 0, 0, 0);
                holder.titleOwnMessage.setPadding(0, 0, scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics), 0);
            } else {
                paramsContactContact.leftMargin = scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
                paramsContactManagement.rightMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
                paramsContactAttach.leftMargin = scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
                paramsContactRichLink.leftMargin = scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
                paramsOwnManagement.rightMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);

                holder.nameContactText.setMaxWidthEmojis(px2dp(MAX_WIDTH_NAME_PORT, outMetrics));
                holder.titleContactMessage.setPadding(px2dp(CONTACT_MESSAGE_PORT, outMetrics), 0, 0, 0);
                holder.titleOwnMessage.setPadding(0, 0, scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics), 0);
            }

            holder.contentContactMessageContactLayout.setLayoutParams(paramsContactContact);
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);
            holder.contentContactMessageAttachLayout.setLayoutParams(paramsContactAttach);
            holder.urlContactMessageLayout.setLayoutParams(paramsContactRichLink);
            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);


            v.setTag(holder);

            return holder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderHeaderChat) {
            ViewHolderHeaderChat holderHeaderChat = (ViewHolderHeaderChat) holder;

            boolean isFullHistoryLoaded = megaChatApi.isFullHistoryLoaded(chatRoom.getChatId());
            holderHeaderChat.firstMessage.setVisibility(isFullHistoryLoaded ? View.VISIBLE : View.GONE);
            holderHeaderChat.loadingMessages.setVisibility(isFullHistoryLoaded ? View.GONE : View.VISIBLE);
            holderHeaderChat.loadingMessages.setImageDrawable(ContextCompat.getDrawable(context,
                    isScreenInPortrait(context) ? R.drawable.loading_chat_messages : R.drawable.loading_chat_messages_landscape));
        } else {
            logDebug("ViewHolderMessageChat: " + position);
            AndroidMegaChatMessage androidMessage = messages.get(position - 1);
            if (androidMessage.isUploading()) {
                logDebug("isUploading");
                onBindViewHolderUploading(holder, position);
            } else {
                logDebug("isSent");
                onBindViewHolderMessage(holder, position);
            }
        }
    }

    public void onBindViewHolderUploading(RecyclerView.ViewHolder holder, int position) {
        logDebug("position: " + position);

        ((ViewHolderMessageChat) holder).itemLayout.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ((ViewHolderMessageChat) holder).itemLayout.setLayoutParams(params);
        ((ViewHolderMessageChat) holder).currentPosition = position;

        ((ViewHolderMessageChat) holder).ownMessageSelectLayout.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).forwardOwnRichLinks.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardOwnPortrait.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardOwnLandscape.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardOwnFile.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardOwnContact.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardContactRichLinks.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardContactPreviewPortrait.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardContactPreviewLandscape.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardContactFile.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardContactContact.setVisibility(View.GONE);


        ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingContact.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingFile.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingVoiceClip.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingRichLink.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
        }else{
            ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
        }
        ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

        ((ViewHolderMessageChat) holder).contentOwnMessageText.setVisibility(View.VISIBLE);
        ((ViewHolderMessageChat) holder).iconOwnTypeDocLandPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

        AndroidMegaChatMessage message = messages.get(position - 1);
        ((ViewHolderMessageChat) holder).itemLayout.setTag(holder);
        ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(this);
        ((ViewHolderMessageChat) holder).itemLayout.setOnLongClickListener(this);

        if (message.isUploading()) {
            if (message.getInfoToShow() != -1) {
                setInfoToShow(position, ((ViewHolderMessageChat) holder), true, message.getInfoToShow(),
                        formatDate(context, message.getPendingMessage().getUploadTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message.getPendingMessage().getUploadTimestamp()));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageText.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).previewFrameLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).urlOwnMessageLayout.setVisibility(View.GONE);


            hideLayoutsLocationMessages(position, ((ViewHolderMessageChat) holder));

            String path = message.getPendingMessage().getFilePath();
            File voiceClipDir = getCacheFolder(context, VOICE_CLIP_FOLDER);
            String name = message.getPendingMessage().getName();
            int type = message.getPendingMessage().getType();
            if (path != null) {
                if(isVoiceClip(path) && (type==TYPE_VOICE_CLIP) || path.contains(voiceClipDir.getAbsolutePath())){
                    logDebug("TYPE_VOICE_CLIP - message.getPendingMessage().getState() " + message.getPendingMessage().getState());
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipPlay.setImageResource(R.drawable.ic_play_grey);

                    ((ViewHolderMessageChat) holder).uploadingOwnProgressbarVoiceclip.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipPlay.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).notAvailableOwnVoiceclip.setVisibility(View.GONE);

                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipDuration.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipDuration.setText("--:--");

                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipSeekBar.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipSeekBar.setProgress(0);
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipSeekBar.setEnabled(false);
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);

                    ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                    ((ViewHolderMessageChat) holder).errorUploadingVoiceClip.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

                    if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                        ((ViewHolderMessageChat) holder).errorUploadingVoiceClip.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).notAvailableOwnVoiceclip.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).uploadingOwnProgressbarVoiceclip.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipPlay.setVisibility(View.GONE);
                    }

                }else{
                    ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setVisibility(View.VISIBLE);

                    Bitmap preview = null;
                    ((ViewHolderMessageChat) holder).filePathUploading = path;
                    logDebug("Path of the file: " + path);

                    if (MimeTypeList.typeForName(path).isImage() || MimeTypeList.typeForName(path).isPdf() || MimeTypeList.typeForName(path).isVideo()) {
                        logDebug("isImage, isPdf or isVideo");

                        ((ViewHolderMessageChat) holder).errorUploadingFile.setVisibility(View.GONE);

                        preview = getPreview(path, context);

                        if (preview != null) {
                            setUploadingPreview((ViewHolderMessageChat) holder, preview);
                            logDebug("preview!");
                        } else {
                            logWarning("No preview!");
                            if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                                ((ViewHolderMessageChat) holder).errorUploadingFile.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);
                            }
                            try {
                                new ChatUploadingPreviewAsyncTask(this, position).execute(path);
                            } catch (Exception e) {
                                logWarning("Error creating preview (Too many AsyncTasks)", e);
                            }
                        }
                    } else {

                        if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                            ((ViewHolderMessageChat) holder).errorUploadingFile.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);
                        }
                    }

                    logDebug("Node handle: " + message.getPendingMessage().getNodeHandle());

                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        logDebug("Landscape configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                        ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setMaxWidth((int) width);
                        ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setMaxWidth((int) width);
                    } else {
                        logDebug("Portrait configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                        ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setMaxWidth((int) width);
                        ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setMaxWidth((int) width);
                    }

                    ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setText(name);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(name).getIconResourceId());
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                    logDebug("State of the message: " + message.getPendingMessage().getState());
                    if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                        ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_error);
                    } else {
                        ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_uploading);
                    }
                }

            } else {
                logWarning("Path is null");
            }
        } else {
            logWarning("ERROR: The message is no UPLOADING");
        }
    }

    private void hideLayoutsLocationMessages(int position, ViewHolderMessageChat holder){
        if (isHolderNull(position, holder)) {
            return;
        }

        holder.transparentCoatingLocation.setVisibility(View.GONE);
        holder.uploadingProgressBarLocation.setVisibility(View.GONE);
        holder.forwardOwnMessageLocation.setVisibility(View.GONE);
        holder.mainOwnMessageItemLocation.setVisibility(View.GONE);
        holder.previewOwnLocation.setVisibility(View.GONE);
        holder.triangleErrorLocation.setVisibility(View.GONE);
        holder.pinnedOwnLocationLayout.setVisibility(View.GONE);
        holder.pinnedOwnLocationInfoText.setVisibility(View.GONE);

        holder.forwardContactMessageLocation.setVisibility(View.GONE);
        holder.mainContactMessageItemLocation.setVisibility(View.GONE);
        holder.previewContactLocation.setVisibility(View.GONE);
        holder.pinnedContactLocationLayout.setVisibility(View.GONE);
        holder.pinnedContactLocationInfoText.setVisibility(View.GONE);

    }

    public void onBindViewHolderMessage(RecyclerView.ViewHolder holder, int position) {
        logDebug("Position: " + position);

        ((ViewHolderMessageChat) holder).itemLayout.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams paramsDefault = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ((ViewHolderMessageChat) holder).itemLayout.setLayoutParams(paramsDefault);
        ((ViewHolderMessageChat) holder).currentPosition = position;

        ((ViewHolderMessageChat) holder).ownMessageSelectLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contactMessageSelectLayout.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingContact.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingFile.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingVoiceClip.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingRichLink.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).transparentCoatingLandscape.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).transparentCoatingPortrait.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).uploadingProgressBarPort.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).uploadingProgressBarLand.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).errorUploadingPortrait.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).errorUploadingLandscape.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).iconOwnTypeDocLandPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).iconContactTypeDocLandPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).urlOwnMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardOwnRichLinks.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).urlContactMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).forwardContactRichLinks.setVisibility(View.GONE);

        hideLayoutsLocationMessages(position, ((ViewHolderMessageChat) holder));

        ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contentContactMessageVoiceClipLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);

        AndroidMegaChatMessage androidMessage = messages.get(position - 1);
        MegaChatMessage message = messages.get(position - 1).getMessage();
        ((ViewHolderMessageChat) holder).userHandle = message.getUserHandle();

        int messageType = message.getType();
        logDebug("Message type: " + messageType);

        if(isKnownMessage(messageType)){
            ((ViewHolderMessageChat) holder).itemLayout.setTag(holder);
            ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(this);
            ((ViewHolderMessageChat) holder).itemLayout.setOnLongClickListener(this);
            ((ViewHolderMessageChat) holder).contentContactMessageText.setTag(holder);
            ((ViewHolderMessageChat) holder).contentContactMessageText.setOnClickListener(this);
            ((ViewHolderMessageChat) holder).contentContactMessageText.setOnLongClickListener(this);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setTag(holder);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setOnClickListener(this);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setOnLongClickListener(this);
        }else{
            logWarning("Not known message: disable click - position: " + position);
            ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(null);
            ((ViewHolderMessageChat) holder).itemLayout.setOnLongClickListener(null);
            ((ViewHolderMessageChat) holder).contentContactMessageText.setOnClickListener(null);
            ((ViewHolderMessageChat) holder).contentContactMessageText.setOnLongClickListener(null);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setOnClickListener(null);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setOnLongClickListener(null);
        }

        switch (messageType) {

            case MegaChatMessage.TYPE_ALTER_PARTICIPANTS: {
                logDebug("ALTER PARTICIPANT MESSAGE!!");
                bindAlterParticipantsMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_PRIV_CHANGE: {
                logDebug("PRIVILEGE CHANGE message");
                bindPrivChangeMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CONTAINS_META: {
                logDebug("TYPE_CONTAINS_META");
                bindContainsMetaMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_NORMAL: {
                logDebug("TYPE_NORMAL");
                if (androidMessage.getRichLinkMessage() != null) {
                    bindMegaLinkMessage((ViewHolderMessageChat) holder, androidMessage, position);
                } else {
                    bindNormalMessage((ViewHolderMessageChat) holder, androidMessage, position);
                }
                break;
            }
            case MegaChatMessage.TYPE_NODE_ATTACHMENT: {
                logDebug("TYPE_NODE_ATTACHMENT");
                bindNodeAttachmentMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_VOICE_CLIP: {
                logDebug("TYPE_VOICE_CLIP");
                bindVoiceClipAttachmentMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT: {
                logDebug("TYPE_CONTACT_ATTACHMENT");
                bindContactAttachmentMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CHAT_TITLE: {
                logDebug("TYPE_CHAT_TITLE");
                bindChangeTitleMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_TRUNCATE: {
                logDebug("TYPE_TRUNCATE");
                bindTruncateMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT: {
                logDebug("TYPE_REVOKE_NODE_ATTACHMENT");
                bindRevokeNodeMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CALL_ENDED:
            case MegaChatMessage.TYPE_CALL_STARTED: {
                logDebug("TYPE_CALL_ENDED or TYPE_CALL_STARTED");
                bindCallMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE:
            case MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE:
            case MegaChatMessage.TYPE_SET_PRIVATE_MODE:{
                bindChatLinkMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_INVALID: {
                logWarning("TYPE_INVALID");
                bindNoTypeMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_UNKNOWN: {
                logWarning("TYPE_UNKNOWN");
                hideMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            default: {
                logDebug("DEFAULT MegaChatMessage");
                hideMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
        }

        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(scaleWidthPx(11, outMetrics), scaleHeightPx(-14, outMetrics), scaleWidthPx(62, outMetrics), scaleHeightPx(16, outMetrics));
//        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);


        ChatActivityLollipop activity = (ChatActivityLollipop) context;
        long unreadCount = Math.abs(activity.getGeneralUnreadCount());
        if (unreadCount == 0 || activity.getLastIdMsgSeen() == MEGACHAT_INVALID_HANDLE || activity.getLastIdMsgSeen() != message.getMsgId()) {
            ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.GONE);
            return;
        }

        MegaChatMessage nextMessage = messages.get(position).getMessage();
        int typeMessage = nextMessage.getType();
        int codeMessage = nextMessage.getCode();

        if (typeMessage >= MegaChatMessage.TYPE_LOWEST_MANAGEMENT && typeMessage <= MegaChatMessage.TYPE_SET_PRIVATE_MODE
                && (typeMessage != MegaChatMessage.TYPE_CALL_ENDED || (codeMessage != END_CALL_REASON_CANCELLED && codeMessage != END_CALL_REASON_NO_ANSWER))) {
            ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.GONE);
            ((ChatActivityLollipop) context).setLastIdMsgSeen(nextMessage.getMsgId());
            return;
        }

        logDebug("Last message ID match!");
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ((ViewHolderMessageChat) holder).newMessagesLayout.getLayoutParams();
        long userHandle = (message.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS || message.getType() == MegaChatMessage.TYPE_PRIV_CHANGE) ? message.getHandleOfAction() : message.getUserHandle();

        params.addRule(RelativeLayout.BELOW, userHandle == myUserHandle ? R.id.message_chat_own_message_layout : R.id.message_chat_contact_message_layout);
        ((ViewHolderMessageChat) holder).newMessagesLayout.setLayoutParams(params);

        String numberString;
        long unreadMessages = Math.abs(((ChatActivityLollipop) context).getGeneralUnreadCount());
        if (((ChatActivityLollipop) context).getGeneralUnreadCount() < 0) {
            numberString = "+" + unreadMessages;
        } else {
            numberString = unreadMessages + "";
        }

        String contentUnreadText = context.getResources().getQuantityString(R.plurals.number_unread_messages, (int) unreadMessages, numberString);
        ((ViewHolderMessageChat) holder).newMessagesText.setText(contentUnreadText);
        ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.VISIBLE);
        ((ChatActivityLollipop) context).setNewVisibility(true);
        ((ChatActivityLollipop) context).setPositionNewMessagesLayout(position);
    }

    public boolean isKnownMessage(int messageType){
        switch (messageType) {

            case MegaChatMessage.TYPE_ALTER_PARTICIPANTS:
            case MegaChatMessage.TYPE_PRIV_CHANGE:
            case MegaChatMessage.TYPE_CONTAINS_META:
            case MegaChatMessage.TYPE_NORMAL:
            case MegaChatMessage.TYPE_NODE_ATTACHMENT:
            case MegaChatMessage.TYPE_VOICE_CLIP:
            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
            case MegaChatMessage.TYPE_CHAT_TITLE:
            case MegaChatMessage.TYPE_TRUNCATE:
            case MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT:
            case MegaChatMessage.TYPE_CALL_STARTED:
            case MegaChatMessage.TYPE_CALL_ENDED:{
                return true;
            }
            case MegaChatMessage.TYPE_UNKNOWN:
            case MegaChatMessage.TYPE_INVALID:
            default: {
                return false;
            }
        }
    }

    public void bindCallMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindCallMessage");

        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
        MegaChatMessage message = androidMessage.getMessage();

        if (message.getUserHandle() == myUserHandle) {
            logDebug("MY message!!");
            logDebug("MY message ID!!: " + message.getMsgId());

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics),0,0,0);
            }


            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            String textToShow = "";

            if(message.getType()==MegaChatMessage.TYPE_CALL_STARTED){
                ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_calling));
                textToShow = context.getResources().getString(R.string.call_started_messages);
            }
            else{
                switch(message.getTermCode()){
                    case MegaChatMessage.END_CALL_REASON_ENDED:{

                        ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_calling));


                        int hours = message.getDuration() / 3600;
                        int minutes = (message.getDuration() % 3600) / 60;
                        int seconds = message.getDuration() % 60;

                        textToShow = chatRoom.isGroup() ? context.getString(R.string.group_call_ended_message) :
                                context.getString(R.string.call_ended_message);

                        if(hours != 0){
                            String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
                            textToShow = textToShow + textHours;
                            if((minutes != 0)||(seconds != 0)){
                                textToShow = textToShow+", ";
                            }
                        }

                        if(minutes != 0){
                            String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
                            textToShow = textToShow + textMinutes;
                            if(seconds != 0){
                                textToShow = textToShow+", ";
                            }
                        }

                        if(seconds != 0){
                            String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
                            textToShow = textToShow + textSeconds;
                        }

                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                            textToShow = textToShow.replace("[C]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/C]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                    case MegaChatMessage.END_CALL_REASON_REJECTED:{

                        ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_rejected));

                        textToShow = String.format(context.getString(R.string.call_rejected_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");

                        } catch (Exception e) {
                        }

                        break;
                    }
                    case END_CALL_REASON_NO_ANSWER:{

                        ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_failed));

                        textToShow = String.format(context.getString(R.string.call_not_answered_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                    case MegaChatMessage.END_CALL_REASON_FAILED:{

                        ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_failed));

                        textToShow = String.format(context.getString(R.string.call_failed_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                    case END_CALL_REASON_CANCELLED:{

                        ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_cancelled));

                        textToShow = String.format(context.getString(R.string.call_cancelled_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                }
            }

            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }

            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics);
            }
            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }
            ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);

            String textToShow = "";

            if(message.getType() == MegaChatMessage.TYPE_CALL_STARTED){
                ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_started));
                if (((ChatActivityLollipop) context).isGroup()) {
                    ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.VISIBLE);
                }

                textToShow = context.getResources().getString(R.string.call_started_messages);

            }else{

                switch(message.getTermCode()){
                    case MegaChatMessage.END_CALL_REASON_ENDED:{

                        ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_started));

                        int hours = message.getDuration() / 3600;
                        int minutes = (message.getDuration() % 3600) / 60;
                        int seconds = message.getDuration() % 60;

                        textToShow = chatRoom.isGroup() ? context.getString(R.string.group_call_ended_message) :
                                context.getString(R.string.call_ended_message);

                        if(hours != 0){
                            String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
                            textToShow = textToShow + textHours;
                            if((minutes != 0)||(seconds != 0)){
                                textToShow = textToShow+", ";
                            }
                        }

                        if(minutes != 0){
                            String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
                            textToShow = textToShow + textMinutes;
                            if(seconds != 0){
                                textToShow = textToShow+", ";
                            }
                        }

                        if(seconds != 0){
                            String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
                            textToShow = textToShow + textSeconds;
                        }
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                            textToShow = textToShow.replace("[C]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/C]", "</font>");
                        } catch (Exception e) {
                        }


                        break;
                    }
                    case MegaChatMessage.END_CALL_REASON_REJECTED:{
                        ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_rejected));
                        textToShow = String.format(context.getString(R.string.call_rejected_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                    case END_CALL_REASON_NO_ANSWER:{
                        ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_missed));

                        textToShow = String.format(context.getString(R.string.call_missed_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                    case MegaChatMessage.END_CALL_REASON_FAILED:{

                        ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_failed));

                        textToShow = String.format(context.getString(R.string.call_failed_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                    case END_CALL_REASON_CANCELLED:{

                        ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_missed));

                        textToShow = String.format(context.getString(R.string.call_missed_messages));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#DE000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                        } catch (Exception e) {
                        }

                        break;
                    }
                }
            }

            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }

            ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(result);
        }
    }

    public void bindAlterParticipantsMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindAlterParticipantsMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);

        MegaChatMessage message = androidMessage.getMessage();

        if (message.getHandleOfAction() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            logDebug("Me alter participant");
            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            int privilege = message.getPrivilege();
            logDebug("Privilege of me: " + privilege);
            String textToShow = "";
            String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

            if (privilege != MegaChatRoom.PRIV_RM) {
                logDebug("I was added");

                if(message.getUserHandle() == message.getHandleOfAction()){
                    textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(megaChatApi.getMyFullname()));
                }
                else{
                    textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(megaChatApi.getMyFullname()), toCDATA(fullNameAction));
                }

                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                    textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/C]", "</font>");
                } catch (Exception e) {
                }
            } else {
                logDebug("I was removed or left");
                if (message.getUserHandle() == message.getHandleOfAction()) {
                    logDebug("I left the chat");
                    textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), toCDATA(megaChatApi.getMyFullname()));
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    } catch (Exception e) {
                    }
                } else {
                    textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(megaChatApi.getMyFullname()), toCDATA(fullNameAction));
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                        textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/C]", "</font>");
                    } catch (Exception e) {
                    }
                }
            }

            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }
            ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setVisibility(View.GONE);
            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

        } else {
            logDebug("CONTACT Message type ALTER PARTICIPANTS");
            int privilege = message.getPrivilege();
            logDebug("Privilege of the user: " + privilege);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }
            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);
            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            setContactMessageName(position, holder, message.getHandleOfAction(), false);

            String textToShow = "";
            if (privilege != MegaChatRoom.PRIV_RM) {
                logDebug("Participant was added");
                if (message.getUserHandle() == myUserHandle) {
                    logDebug("By me");

                    if(message.getUserHandle() == message.getHandleOfAction()){
                        textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(holder.fullNameTitle));
                    }
                    else{
                        textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(holder.fullNameTitle), toCDATA(megaChatApi.getMyFullname()));
                    }

                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                        textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/C]", "</font>");
                    } catch (Exception e) {
                    }
                } else {
//                        textToShow = String.format(context.getString(R.string.message_add_participant), message.getHandleOfAction()+"");
                    logDebug("By other");
                    String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

                    if(message.getUserHandle() == message.getHandleOfAction()){
                        textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(holder.fullNameTitle));
                    }
                    else{
                        textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(holder.fullNameTitle), toCDATA(fullNameAction));
                    }

                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                        textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/C]", "</font>");
                    } catch (Exception e) {
                    }

                }
            }//END participant was added
            else {
                logDebug("Participant was removed or left");
                if (message.getUserHandle() == myUserHandle) {
                    textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(holder.fullNameTitle), toCDATA(megaChatApi.getMyFullname()));
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                        textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/C]", "</font>");
                    } catch (Exception e) {
                    }
                } else {

                    if (message.getUserHandle() == message.getHandleOfAction()) {
                        logDebug("The participant left the chat");

                        textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), toCDATA(holder.fullNameTitle));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                        } catch (Exception e) {
                        }

                    } else {
                        logDebug("The participant was removed");
                        String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

                        textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(holder.fullNameTitle), toCDATA(fullNameAction));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                            textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/C]", "</font>");
                        } catch (Exception e) {
                        }
                    }
//                        textToShow = String.format(context.getString(R.string.message_remove_participant), message.getHandleOfAction()+"");
                }
            } //END participant removed

            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }
            ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(result);

        }
    }

    public void bindPrivChangeMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindPrivChangeMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);

        MegaChatMessage message = androidMessage.getMessage();

        if (message.getHandleOfAction() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            logDebug("A moderator change my privilege");
            int privilege = message.getPrivilege();
            logDebug("Privilege of the user: " + privilege);

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            String privilegeString = "";
            if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
            } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
            } else if (privilege == MegaChatRoom.PRIV_RO) {
                privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
            } else {
                logDebug("Change to other");
                privilegeString = "Unknow";
            }

            String textToShow = "";

            if (message.getUserHandle() == myUserHandle) {
                logDebug("I changed my Own permission");
                textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(megaChatApi.getMyFullname()), toCDATA(privilegeString), toCDATA(megaChatApi.getMyFullname()));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                    textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/C]", "</font>");
                    textToShow = textToShow.replace("[D]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/D]", "</font>");
                    textToShow = textToShow.replace("[E]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/E]", "</font>");
                } catch (Exception e) {
                }
            } else {
                logDebug("I was change by someone");
                String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

                textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(megaChatApi.getMyFullname()), toCDATA(privilegeString), toCDATA(fullNameAction));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                    textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/C]", "</font>");
                    textToShow = textToShow.replace("[D]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/D]", "</font>");
                    textToShow = textToShow.replace("[E]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/E]", "</font>");
                } catch (Exception e) {
                }
            }

            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }
            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setVisibility(View.GONE);
            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);


            logDebug("Visible own management message!");

        } else {
            logDebug("Participant privilege change!");

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }
            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);
            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            setContactMessageName(position, holder, message.getHandleOfAction(), false);

            int privilege = message.getPrivilege();
            String privilegeString = "";
            if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
            } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
            } else if (privilege == MegaChatRoom.PRIV_RO) {
                privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
            } else {
                logDebug("Change to other");
                privilegeString = "Unknow";
            }

            String textToShow = "";
            if (message.getUserHandle() == myUserHandle) {
                logDebug("The privilege was change by me");
                textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(holder.fullNameTitle), toCDATA(privilegeString), toCDATA(megaChatApi.getMyFullname()));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                    textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/C]", "</font>");
                    textToShow = textToShow.replace("[D]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/D]", "</font>");
                    textToShow = textToShow.replace("[E]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/E]", "</font>");
                } catch (Exception e) {
                }

            } else {
                logDebug("By other");
                String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

                textToShow = String.format(context.getString(R.string.message_permissions_changed), toCDATA(holder.fullNameTitle), toCDATA(privilegeString), toCDATA(fullNameAction));
                try {
                    textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                    textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/C]", "</font>");
                    textToShow = textToShow.replace("[D]", "<font color=\'#868686\'>");
                    textToShow = textToShow.replace("[/D]", "</font>");
                    textToShow = textToShow.replace("[E]", "<font color=\'#060000\'>");
                    textToShow = textToShow.replace("[/E]", "</font>");
                } catch (Exception e) {
                }
            }
            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }

            ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(result);
        }
    }

    public void bindContainsMetaMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindContainsMetaMessage()");

        MegaChatMessage message = androidMessage.getMessage();
        MegaChatContainsMeta meta = message.getContainsMeta();
        if (meta == null) {
            bindNoTypeMessage(holder, androidMessage, position);

        } else if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
            String urlString = meta.getRichPreview().getUrl();
            try {
                URL url = new URL(urlString);
                urlString = url.getHost();

            } catch (MalformedURLException e) {
                logError("EXCEPTION", e);
            }

            String title = meta.getRichPreview().getTitle();
            String text = meta.getRichPreview().getText();
            String description = meta.getRichPreview().getDescription();
            String imageFormat = meta.getRichPreview().getImageFormat();
            String image = meta.getRichPreview().getImage();
            String icon = meta.getRichPreview().getIcon();


            Bitmap bitmapImage = getBitmapFromString(image);
            Bitmap bitmapIcon = getBitmapFromString(icon);

            if (message.getUserHandle() == myUserHandle) {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
                holder.titleOwnMessage.setGravity(Gravity.RIGHT);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
                }else{
                    holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
                }

                logDebug("MY message handle!!: " + message.getMsgId());
                if (messages.get(position - 1).getInfoToShow() != -1) {
                    setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                            formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                            formatTime(message));
                }

                //Forwards element (own messages):
                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                    holder.forwardOwnRichLinks.setVisibility(View.GONE);
                } else {
                    holder.forwardOwnRichLinks.setVisibility(View.VISIBLE);
                    holder.forwardOwnRichLinks.setOnClickListener(this);
                }

                int status = message.getStatus();

                if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                    logDebug("Show triangle retry!");
                    ((ViewHolderMessageChat) holder).urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.light_background_text_rich_link));
                    ((ViewHolderMessageChat)holder).errorUploadingRichLink.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).forwardOwnRichLinks.setVisibility(View.GONE);
                }else if((status==MegaChatMessage.STATUS_SENDING)){
                    ((ViewHolderMessageChat) holder).urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.light_background_text_rich_link));
                    ((ViewHolderMessageChat)holder).errorUploadingRichLink.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).forwardOwnRichLinks.setVisibility(View.GONE);
                }else{
                    logDebug("Status: " + message.getStatus());
                    ((ViewHolderMessageChat) holder).urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_background_text_rich_link));
                    ((ViewHolderMessageChat)holder).errorUploadingRichLink.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);

                }

                holder.contactMessageLayout.setVisibility(View.GONE);
                holder.ownMessageLayout.setVisibility(View.VISIBLE);

                holder.ownManagementMessageLayout.setVisibility(View.GONE);
                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);

                holder.contentOwnMessageText.setVisibility(View.GONE);
                holder.urlOwnMessageLayout.setVisibility(View.VISIBLE);

                holder.forwardOwnPortrait.setVisibility(View.GONE);
                holder.forwardOwnLandscape.setVisibility(View.GONE);
                holder.forwardOwnFile.setVisibility(View.GONE);
                holder.forwardOwnContact.setVisibility(View.GONE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);

                holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);

                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);
                holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
                holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

                SimpleSpanBuilder ssb = formatText(context, text);

                checkEmojiSize(text, holder.urlOwnMessageText);
                if(ssb!=null){
                    ((ViewHolderMessageChat)holder).urlOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                }
                else{
                    ((ViewHolderMessageChat)holder).urlOwnMessageText.setText(text);
                }

                holder.urlOwnMessageTitle.setVisibility(View.VISIBLE);
                holder.urlOwnMessageTitle.setText(title);
                holder.urlOwnMessageTitle.setMaxLines(1);
                holder.urlOwnMessageDescription.setText(description);
                holder.urlOwnMessageDescription.setMaxLines(2);
                holder.urlOwnMessageIconAndLinkLayout.setVisibility(View.VISIBLE);
                holder.urlOwnMessageLink.setText(urlString);

                if (bitmapImage != null) {
                    holder.urlOwnMessageImage.setImageBitmap(bitmapImage);
                    holder.urlOwnMessageImage.setVisibility(View.VISIBLE);
                    holder.urlOwnMessageGroupAvatarLayout.setVisibility(View.GONE);
                    holder.urlOwnMessageTitleLayout.setGravity(Gravity.RIGHT);
                } else {
                    holder.urlOwnMessageGroupAvatarLayout.setVisibility(View.GONE);
                    holder.urlOwnMessageImage.setVisibility(View.GONE);
                    holder.urlOwnMessageTitleLayout.setGravity(Gravity.LEFT);
                }

                if (bitmapIcon != null) {
                    holder.urlOwnMessageIcon.setImageBitmap(bitmapIcon);
                    holder.urlOwnMessageIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.urlOwnMessageIcon.setVisibility(View.GONE);
                }

                if (isOnline(context)) {
                    if(isMultipleSelect()){
                        holder.urlOwnMessageText.setLinksClickable(false);
                    }else{
                        holder.urlOwnMessageText.setLinksClickable(true);
                        Linkify.addLinks(holder.urlOwnMessageText, Linkify.WEB_URLS);
                        holder.urlOwnMessageText.setLinkTextColor(ContextCompat.getColor(context, R.color.white));
                    }
                }else{
                    holder.urlOwnMessageText.setLinksClickable(false);
                }

                checkMultiselectionMode(position, holder, true, message.getMsgId());

                if (!multipleSelect) {
                    if (positionClicked != INVALID_POSITION && positionClicked == position) {
                        holder.forwardOwnRichLinks.setEnabled(false);
                    } else {
                        holder.forwardOwnRichLinks.setEnabled(true);
                    }
                }

                interceptLinkClicks(context, holder.urlOwnMessageText);
            } else {
                long userHandle = message.getUserHandle();
                logDebug("Contact message!!: " + userHandle);

                setContactMessageName(position, holder, userHandle, true);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
                }else{
                    holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
                }

                if (messages.get(position - 1).getInfoToShow() != -1) {
                    setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                            formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                            formatTime(message));
                }

                if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                    holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                    setContactAvatar(holder, userHandle, holder.fullNameTitle);

                } else {
                    holder.layoutAvatarMessages.setVisibility(View.GONE);
                }

                holder.ownMessageLayout.setVisibility(View.GONE);
                holder.contactMessageLayout.setVisibility(View.VISIBLE);

                holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
                holder.contactManagementMessageLayout.setVisibility(View.GONE);

                holder.contentContactMessageText.setVisibility(View.GONE);
                holder.urlContactMessageLayout.setVisibility(View.VISIBLE);
                holder.contentContactMessageVoiceClipLayout.setVisibility(View.GONE);

                //Forwards element (contact messages):
                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                    holder.forwardContactRichLinks.setVisibility(View.GONE);
                }
                else {
                    holder.forwardContactRichLinks.setVisibility(View.VISIBLE);
                    holder.forwardContactRichLinks.setOnClickListener(this);
                }

                holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
                holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
                holder.forwardContactFile.setVisibility(View.GONE);
                holder.forwardContactContact.setVisibility(View.GONE);

                holder.contentContactMessageAttachLayout.setVisibility(View.GONE);
                holder.contentContactMessageContactLayout.setVisibility(View.GONE);

                //Rick link
                holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
                holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

                holder.urlContactMessageTitle.setVisibility(View.VISIBLE);
                holder.urlContactMessageTitle.setText(title);
                holder.urlContactMessageDescription.setText(description);
                holder.urlContactMessageIconAndLinkLayout.setVisibility(View.VISIBLE);
                holder.urlContactMessageLink.setText(urlString);

                checkEmojiSize(text, holder.urlContactMessageText);

                //Color always status SENT
                ((ViewHolderMessageChat) holder).urlContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                SimpleSpanBuilder ssb = formatText(context, text);
                if (ssb != null) {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(text);

                    if (bitmapImage != null) {
                        holder.urlContactMessageImage.setImageBitmap(bitmapImage);
                        holder.urlContactMessageImage.setVisibility(View.VISIBLE);
                        holder.urlContactMessageGroupAvatarLayout.setVisibility(View.GONE);
                        holder.urlContactMessageTitleLayout.setGravity(Gravity.RIGHT);
                    } else {
                        holder.urlContactMessageGroupAvatarLayout.setVisibility(View.GONE);
                        holder.urlContactMessageImage.setVisibility(View.GONE);
                        holder.urlContactMessageTitleLayout.setGravity(Gravity.LEFT);
                    }
                }

                if (bitmapIcon != null) {
                    holder.urlContactMessageIcon.setImageBitmap(bitmapIcon);
                    holder.urlContactMessageIcon.setVisibility(View.VISIBLE);
                } else {
                   holder.urlContactMessageIcon.setVisibility(View.GONE);
                }

                if (isOnline(context)) {
                    if(isMultipleSelect()){
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setLinksClickable(false);
                    }else{
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setLinksClickable(true);
                        Linkify.addLinks(((ViewHolderMessageChat) holder).urlContactMessageText, Linkify.WEB_URLS);
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setLinkTextColor(ContextCompat.getColor(context, R.color.accentColor));
                    }
                }else{
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setLinksClickable(false);
                }

                checkMultiselectionMode(position, holder, false, message.getMsgId());

                if (!multipleSelect) {
                    if (positionClicked != INVALID_POSITION && positionClicked == position) {
                        holder.forwardContactRichLinks.setEnabled(false);
                    } else {
                        holder.forwardContactRichLinks.setEnabled(true);
                    }
                }

                interceptLinkClicks(context, holder.urlContactMessageText);
            }
        } else if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID) {
            if (message.getUserHandle() == myUserHandle) {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
                holder.titleOwnMessage.setGravity(Gravity.RIGHT);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
                }else{
                    holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
                }

                if (messages.get(position - 1).getInfoToShow() != -1) {
                    setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                            formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                            formatTime(message));
                }

                holder.ownMessageLayout.setVisibility(View.VISIBLE);
                holder.contactMessageLayout.setVisibility(View.GONE);
                holder.ownManagementMessageLayout.setVisibility(View.GONE);
                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);

                //Forward element (own message)
                holder.forwardOwnRichLinks.setVisibility(View.GONE);
                holder.forwardOwnPortrait.setVisibility(View.GONE);
                holder.forwardOwnLandscape.setVisibility(View.GONE);
                holder.forwardOwnFile.setVisibility(View.GONE);
                holder.forwardOwnContact.setVisibility(View.GONE);

                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                holder.ownManagementMessageLayout.setVisibility(View.GONE);
                holder.contentOwnMessageText.setVisibility(View.VISIBLE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

                int status = message.getStatus();

                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    logDebug("Show triangle retry!");
                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.VISIBLE);
                    holder.retryAlert.setVisibility(View.VISIBLE);

                } else if ((status == MegaChatMessage.STATUS_SENDING)) {
                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);

                } else {
                    logDebug("Status: " + message.getStatus());

                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.contentOwnMessageText.setText(context.getString(R.string.error_meta_message_invalid));

                if (isOnline(context)) {
                    if(isMultipleSelect()){
                        holder.contentOwnMessageText.setLinksClickable(false);
                    }else{
                        holder.contentOwnMessageText.setLinksClickable(true);
                        Linkify.addLinks(holder.contentOwnMessageText, Linkify.WEB_URLS);
                        holder.contentOwnMessageText.setLinkTextColor(ContextCompat.getColor(context, R.color.white));
                    }
                }else {
                    holder.contentOwnMessageText.setLinksClickable(false);
                }


            }else{
                long userHandle = message.getUserHandle();
                logDebug("Contact message!!: " + userHandle);

                setContactMessageName(position, holder, userHandle, true);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
                }else{
                    holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
                }

                //forward element (contact message)
                holder.forwardContactRichLinks.setVisibility(View.GONE);
                holder.forwardContactContact.setVisibility(View.GONE);
                holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
                holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
                holder.forwardContactFile.setVisibility(View.GONE);

                if (messages.get(position - 1).getInfoToShow() != -1) {
                    setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                            formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                            formatTime(message));
                }

                holder.ownMessageLayout.setVisibility(View.GONE);
                holder.contactMessageLayout.setVisibility(View.VISIBLE);

                holder.contactManagementMessageLayout.setVisibility(View.GONE);
                holder.contentContactMessageLayout.setVisibility(View.VISIBLE);


                if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                    ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                    setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle);
                } else {
                    ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
                }

                ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).urlContactMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

                //Color always status SENT
                ((ViewHolderMessageChat) holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                ((ViewHolderMessageChat) holder).contentContactMessageText.setText(context.getString(R.string.error_meta_message_invalid));
            }
        }
        else if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
            bindGeoLocationMessage(holder, androidMessage, position);
        }
        else {
            logWarning("Link to bind as a no type message");
            bindNoTypeMessage(holder, androidMessage, position);
        }
    }

    private Bitmap getBitmapFromString(String imageString){

        if (imageString != null) {
            try {
                byte[] decodedBytes = Base64.decode(imageString, 0);
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            } catch (Exception e) {
                logError("Error getting image", e);
            }
        }
        return null;
    }

    private Bitmap getResizeBitmap (Bitmap originalBitmap) {
        if (originalBitmap == null) return null;

        int widthResizeBitmap = originalBitmap.getWidth();
        int heightResizeBitmap = originalBitmap.getWidth() / 2;
        int topResizeBitmap = heightResizeBitmap / 2;
        int bottomResizeBitmap = topResizeBitmap + heightResizeBitmap;
        Bitmap resizeBitmap = Bitmap.createBitmap(widthResizeBitmap, heightResizeBitmap, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(resizeBitmap);
        Rect desRect = new Rect(0, 0, widthResizeBitmap, heightResizeBitmap);
        Rect srcRect =  new Rect(0, topResizeBitmap, widthResizeBitmap, bottomResizeBitmap);
        canvas.drawBitmap(originalBitmap, srcRect, desRect, null);

        return resizeBitmap;
    }

    public void bindGeoLocationMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindGeoLocationMessage()");
        MegaChatMessage message = androidMessage.getMessage();
        MegaChatContainsMeta meta = message.getContainsMeta();

        String image = meta.getGeolocation().getImage();
        float latitude = meta.getGeolocation().getLatitude();
        float longitude = meta.getGeolocation().getLongitude();

        Bitmap bitmapImage = null;

        if (image != null) {
            byte[] decodedBytes = Base64.decode(image, 0);
            bitmapImage = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            bitmapImage = getResizeBitmap(bitmapImage);
        }

        if (message.getUserHandle() == myUserHandle) {
            holder.layoutAvatarMessages.setVisibility(View.GONE);
            holder.titleOwnMessage.setGravity(Gravity.RIGHT);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            logDebug("MY message handle!!: " + message.getMsgId());
            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).mainOwnMessageItemLocation.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).previewOwnLocation.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).pinnedOwnLocationLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).pinnedOwnLocationInfoText.setVisibility(View.VISIBLE);

            String location =convertToDegrees(latitude, longitude);

            ((ViewHolderMessageChat) holder).pinnedOwnLocationInfoText.setText(location);

            if (bitmapImage != null) {
                holder.previewOwnLocation.setImageBitmap(bitmapImage);
            } else {
                logWarning("Error getting bitmap");
            }

            int status = message.getStatus();

            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                logDebug("Show triangle retry!");
                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).transparentCoatingLocation.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).uploadingProgressBarLocation.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).triangleErrorLocation.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat) holder).pinnedOwnLocationInfoText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                ((ViewHolderMessageChat) holder).pinnedLocationTitleText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));

                holder.forwardOwnMessageLocation.setVisibility(View.GONE);

            }else if((status==MegaChatMessage.STATUS_SENDING)){
                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).transparentCoatingLocation.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).uploadingProgressBarLocation.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).triangleErrorLocation.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).pinnedOwnLocationInfoText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                ((ViewHolderMessageChat) holder).pinnedLocationTitleText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));

                holder.forwardOwnMessageLocation.setVisibility(View.GONE);

            }else{
                logDebug("Status: " + message.getStatus());
                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).transparentCoatingLocation.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).uploadingProgressBarLocation.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).triangleErrorLocation.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).pinnedOwnLocationInfoText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                ((ViewHolderMessageChat) holder).pinnedLocationTitleText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                //Forwards element (own messages):
                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                    holder.forwardOwnMessageLocation.setVisibility(View.GONE);
                }
                else {
                    holder.forwardOwnMessageLocation.setVisibility(View.VISIBLE);
                    holder.forwardOwnMessageLocation.setOnClickListener(this);
                }
            }

            if (message.isEdited()) {
                Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.pinnedLocationTitleText.setText(context.getString(R.string.title_geolocation_message)+" ");
                holder.pinnedLocationTitleText.append(edited);
            }
            else {
                holder.pinnedLocationTitleText.setText(context.getString(R.string.title_geolocation_message));
            }

            holder.contactMessageLayout.setVisibility(View.GONE);
            holder.ownMessageLayout.setVisibility(View.VISIBLE);

            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);

            holder.contentOwnMessageText.setVisibility(View.GONE);
            holder.urlOwnMessageLayout.setVisibility(View.GONE);

            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.previewFrameLand.setVisibility(View.GONE);
            holder.previewFramePort.setVisibility(View.GONE);

            holder.contentOwnMessageFileLayout.setVisibility(View.GONE);

            holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);

            holder.contentOwnMessageContactLayout.setVisibility(View.GONE);
            holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
            holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

            checkMultiselectionMode(position, holder, true, message.getMsgId());

            if (!multipleSelect) {
                if (positionClicked != INVALID_POSITION && positionClicked == position) {
                    holder.forwardOwnMessageLocation.setEnabled(false);
                } else {
                    holder.forwardOwnMessageLocation.setEnabled(true);
                }
            }

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        logDebug("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        logDebug("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        logDebug("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(holder, userHandle, holder.fullNameTitle);
            } else {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);

            holder.mainContactMessageItemLocation.setVisibility(View.VISIBLE);
            holder.previewContactLocation.setVisibility(View.VISIBLE);

            holder.pinnedContactLocationLayout.setVisibility(View.VISIBLE);
            holder.pinnedContactLocationInfoText.setVisibility(View.VISIBLE);

            String location =convertToDegrees(latitude, longitude);

            ((ViewHolderMessageChat) holder).pinnedContactLocationInfoText.setText(location);

            if (message.isEdited()) {
                Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.pinnedContactLocationTitleText.setText(context.getString(R.string.title_geolocation_message)+" ");
                holder.pinnedContactLocationTitleText.append(edited);
            }
            else {
                holder.pinnedContactLocationTitleText.setText(context.getString(R.string.title_geolocation_message));
            }

            if (bitmapImage != null) {
                holder.previewContactLocation.setImageBitmap(bitmapImage);
            }
            else {
                logWarning("Error getting bitmap");
            }

            //Forwards element (own messages):
            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardContactMessageLocation.setVisibility(View.GONE);
            } else {
                holder.forwardContactMessageLocation.setVisibility(View.VISIBLE);
                holder.forwardContactMessageLocation.setOnClickListener(this);
            }

            checkMultiselectionMode(position, holder, false, message.getMsgId());

            if (!multipleSelect) {
                if (positionClicked != INVALID_POSITION && positionClicked == position) {
                    holder.forwardContactMessageLocation.setEnabled(false);
                } else {
                    holder.forwardContactMessageLocation.setEnabled(true);
                }
            }
        }
    }

    public void bindMegaLinkMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindMegaLinkMessage()");

        MegaChatMessage message = androidMessage.getMessage();
        MegaNode node = androidMessage.getRichLinkMessage().getNode();

        if (message.getUserHandle() == myUserHandle) {
            logDebug("MY message handle!!: " + message.getMsgId());

            holder.layoutAvatarMessages.setVisibility(View.GONE);
            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        holder.dateLayout.setVisibility(View.VISIBLE);
                        holder.dateText.setText(formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT));
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        logDebug("CHAT_ADAPTER_SHOW_TIME");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        logDebug("CHAT_ADAPTER_SHOW_NOTHING");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            holder.contactMessageLayout.setVisibility(View.GONE);
            holder.ownMessageLayout.setVisibility(View.VISIBLE);

            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);

            holder.contentOwnMessageText.setVisibility(View.GONE);
            holder.urlOwnMessageLayout.setVisibility(View.VISIBLE);

            //Forward element (own messages):
            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardOwnRichLinks.setVisibility(View.GONE);
            }
            else {
                holder.forwardOwnRichLinks.setVisibility(View.VISIBLE);
                holder.forwardOwnRichLinks.setOnClickListener(this);
            }

            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.urlOwnMessageIconAndLinkLayout.setVisibility(View.GONE);
            holder.previewFrameLand.setVisibility(View.GONE);
            holder.previewFramePort.setVisibility(View.GONE);
            holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
            holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);
            holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

            //MEGA link
            holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
            holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

            String messageContent = "";
            if (message.getContent() != null) {
                messageContent = converterShortCodes(message.getContent());
            }

            if (message.isEdited()) {
                Spannable content = new SpannableString(messageContent);
                int status = message.getStatus();
                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    logDebug("Show triangle retry!");
                    holder.urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.light_background_text_rich_link));
                    holder.errorUploadingRichLink.setVisibility(View.VISIBLE);
                    holder.forwardOwnRichLinks.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.VISIBLE);
                } else if ((status == MegaChatMessage.STATUS_SENDING)) {
                    logDebug("Status not received by server: " + message.getStatus());
                    holder.urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.light_background_text_rich_link));
                    holder.errorUploadingRichLink.setVisibility(View.GONE);
                    holder.forwardOwnRichLinks.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                } else {
                    logDebug("Status: " + message.getStatus());
                    holder.urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_background_text_rich_link));
                    holder.errorUploadingRichLink.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                SimpleSpanBuilder ssb = formatText(context, messageContent);
                if (ssb != null) {
                    ssb.append(" " + context.getString(R.string.edited_message_text), new RelativeSizeSpan(0.70f), new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), new StyleSpan(Typeface.ITALIC));
                    holder.urlOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    holder.urlOwnMessageText.setText(content + " ");

                    Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                    edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    ((ViewHolderMessageChat) holder).urlOwnMessageText.append(edited);
                }

                checkEmojiSize(messageContent, holder.contentOwnMessageText);

            } else {

                SimpleSpanBuilder ssb = formatText(context, messageContent);
                int status = message.getStatus();

                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    logDebug("Show triangle retry!");
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    ((ViewHolderMessageChat) holder).urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.light_background_text_rich_link));
                    ((ViewHolderMessageChat) holder).errorUploadingRichLink.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).forwardOwnRichLinks.setVisibility(View.GONE);
                } else if ((status == MegaChatMessage.STATUS_SENDING)) {
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    ((ViewHolderMessageChat) holder).urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.light_background_text_rich_link));

                    ((ViewHolderMessageChat) holder).errorUploadingRichLink.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).forwardOwnRichLinks.setVisibility(View.GONE);
                } else {
                    logDebug("Status: " + message.getStatus());
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    ((ViewHolderMessageChat) holder).urlOwnMessageTextrl.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_background_text_rich_link));
                    ((ViewHolderMessageChat) holder).errorUploadingRichLink.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);
                }

                checkEmojiSize(messageContent, holder.urlOwnMessageText);

                if (ssb != null) {
                    holder.urlOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    holder.urlOwnMessageText.setText(messageContent);
                }
            }

            holder.urlOwnMessageIconAndLinkLayout.setVisibility(View.VISIBLE);
            holder.urlOwnMessageLink.setText(androidMessage.getRichLinkMessage().getServer());

            holder.urlOwnMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_launcher));
            holder.urlOwnMessageIcon.setVisibility(View.VISIBLE);

            holder.urlOwnMessageText.setOnClickListener(this);
            holder.urlOwnMessageText.setOnLongClickListener(this);

            if (node != null) {
                ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setText(node.getName());

                ((ViewHolderMessageChat) holder).urlOwnMessageImage.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).urlOwnMessageGroupAvatarLayout.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setMaxLines(1);
                ((ViewHolderMessageChat) holder).urlOwnMessageDescription.setMaxLines(2);

                if (node.isFile()) {
                    Bitmap thumb = null;
                    thumb = getThumbnailFromCache(node);
                    if (thumb != null) {
                        previewCache.put(node.getHandle(), thumb);
                        ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageBitmap(thumb);
                    } else {
                        thumb = getThumbnailFromFolder(node, context);
                        if (thumb != null) {
                            previewCache.put(node.getHandle(), thumb);
                            ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageBitmap(thumb);
                        } else {
                            ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        }
                    }
                    ((ViewHolderMessageChat) holder).urlOwnMessageDescription.setText(getSizeString(node.getSize()));
                } else {
                    holder.urlOwnMessageImage.setImageResource(getFolderIcon(node, ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE));
                    ((ViewHolderMessageChat) holder).urlOwnMessageDescription.setText(androidMessage.getRichLinkMessage().getFolderContent());
                }
            } else {
                if(androidMessage.getRichLinkMessage()!=null && androidMessage.getRichLinkMessage().isChat()){
                    long numParticipants = androidMessage.getRichLinkMessage().getNumParticipants();

                    if(numParticipants!=-1){
                        holder.urlOwnMessageText.setOnClickListener(this);
                        holder.urlOwnMessageText.setOnLongClickListener(this);

                        ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setText(androidMessage.getRichLinkMessage().getTitle());
                        ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setMaxLines(1);
                        ((ViewHolderMessageChat) holder).urlOwnMessageDescription.setVisibility(View.VISIBLE);

                        ((ViewHolderMessageChat) holder).urlOwnMessageDescription.setText(context.getString(R.string.number_of_participants, numParticipants));

                        ((ViewHolderMessageChat) holder).urlOwnMessageImage.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).urlOwnMessageGroupAvatarLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).urlOwnMessageGroupAvatar.setImageDrawable(context.getDrawable(R.drawable.calls));
                    }
                    else{
                        if (isOnline(context)) {
                            if(isMultipleSelect()){
                                ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(this);
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setOnClickListener(this);
                            }else{
                                ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(null);
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setOnClickListener(null);
                            }
                        }else{
                            ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(null);
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setOnClickListener(null);
                        }

                        ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setText(context.getString(R.string.invalid_chat_link));
                        ((ViewHolderMessageChat) holder).urlOwnMessageTitle.setMaxLines(2);
                        ((ViewHolderMessageChat) holder).urlOwnMessageDescription.setVisibility(View.INVISIBLE);

                        ((ViewHolderMessageChat) holder).urlOwnMessageImage.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).urlOwnMessageGroupAvatarLayout.setVisibility(View.GONE);
                    }
                }
                else{
                    logWarning("Chat-link error: null");
                }
            }

            checkMultiselectionMode(position, holder, true, message.getMsgId());

            if (!multipleSelect) {
                if (positionClicked != INVALID_POSITION && positionClicked == position) {
                    holder.forwardOwnRichLinks.setEnabled(false);
                } else {
                    holder.forwardOwnRichLinks.setEnabled(true);
                }
            }

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);
            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle);
            } else {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
            }

            String messageContent = "";
            if (message.getContent() != null) {
                messageContent = converterShortCodes(message.getContent());
            }

            if (message.isEdited()) {
                logDebug("Message is edited");
                SimpleSpanBuilder ssb = formatText(context, messageContent);
                if (ssb != null) {
                    ssb.append(" " + context.getString(R.string.edited_message_text), new RelativeSizeSpan(0.70f), new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), new StyleSpan(Typeface.ITALIC));
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    messageContent = messageContent + " ";
                    Spannable content = new SpannableString(messageContent);
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                    edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    CharSequence indexedText = TextUtils.concat(messageContent, edited);
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(indexedText);
                }

            } else {
                //Color always status SENT
                ((ViewHolderMessageChat) holder).urlContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                SimpleSpanBuilder ssb = formatText(context, messageContent);
                if (ssb != null) {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(messageContent);
                }
            }

            checkEmojiSize(messageContent, holder.urlContactMessageText);

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);

            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
            holder.contactManagementMessageLayout.setVisibility(View.GONE);

            holder.contentContactMessageVoiceClipLayout.setVisibility(View.GONE);

            holder.contentContactMessageText.setVisibility(View.GONE);
            holder.urlContactMessageLayout.setVisibility(View.VISIBLE);

            //Forward element (contact messages):
            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardContactRichLinks.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactRichLinks.setVisibility(View.VISIBLE);
                holder.forwardContactRichLinks.setOnClickListener(this);
            }

            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.forwardContactContact.setVisibility(View.GONE);

            holder.contentContactMessageAttachLayout.setVisibility(View.GONE);
            holder.contentContactMessageContactLayout.setVisibility(View.GONE);

            //MEGA link
            holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
            holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

            holder.urlContactMessageIconAndLinkLayout.setVisibility(View.VISIBLE);
            holder.urlContactMessageLink.setText(androidMessage.getRichLinkMessage().getServer());

            holder.urlContactMessageIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_launcher));
            holder.urlContactMessageIcon.setVisibility(View.VISIBLE);

            holder.urlContactMessageText.setOnClickListener(this);
            holder.urlContactMessageText.setOnLongClickListener(this);

            if (node != null) {
                ((ViewHolderMessageChat) holder).urlContactMessageTitle.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).urlContactMessageTitle.setText(node.getName());
                ((ViewHolderMessageChat) holder).urlContactMessageTitle.setMaxLines(1);

                ((ViewHolderMessageChat) holder).urlContactMessageImage.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).urlContactMessageGroupAvatarLayout.setVisibility(View.GONE);

                if (node.isFile()) {
                    Bitmap thumb = null;
                    thumb = getThumbnailFromCache(node);

                    if (thumb != null) {
                        previewCache.put(node.getHandle(), thumb);
                        ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageBitmap(thumb);
                    } else {
                        thumb = getThumbnailFromFolder(node, context);
                        if (thumb != null) {
                            previewCache.put(node.getHandle(), thumb);
                            ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageBitmap(thumb);
                        } else {
                            ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                        }
                    }
                    ((ViewHolderMessageChat) holder).urlContactMessageDescription.setText(getSizeString(node.getSize()));
                } else {
                    if (node.isInShare()) {
                        ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageResource(R.drawable.ic_folder_incoming_list);
                    } else {
                        ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageResource(R.drawable.ic_folder_list);
                    }
                    ((ViewHolderMessageChat) holder).urlContactMessageDescription.setText(androidMessage.getRichLinkMessage().getFolderContent());
                }
            } else {
                if(androidMessage.getRichLinkMessage()!=null && androidMessage.getRichLinkMessage().isChat()){

                    ((ViewHolderMessageChat) holder).urlContactMessageImage.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).urlContactMessageGroupAvatarLayout.setVisibility(View.VISIBLE);

                    long numParticipants = androidMessage.getRichLinkMessage().getNumParticipants();
                    if(numParticipants!=-1){
                        holder.urlContactMessageText.setOnClickListener(this);
                        holder.urlContactMessageText.setOnLongClickListener(this);

                        ((ViewHolderMessageChat) holder).urlContactMessageTitle.setVisibility(View.VISIBLE);

                        ((ViewHolderMessageChat) holder).urlContactMessageTitle.setText(androidMessage.getRichLinkMessage().getTitle());
                        ((ViewHolderMessageChat) holder).urlContactMessageTitle.setMaxLines(1);
                        ((ViewHolderMessageChat) holder).urlContactMessageDescription.setVisibility(View.VISIBLE);

                        ((ViewHolderMessageChat) holder).urlContactMessageDescription.setText(context.getString(R.string.number_of_participants, numParticipants));
                        ((ViewHolderMessageChat) holder).urlContactMessageGroupAvatar.setImageDrawable(context.getDrawable(R.drawable.calls));
//                        createGroupChatAvatar(holder, androidMessage.getRichLinkMessage().getTitle(), false);
                    }
                    else{
                        if (isOnline(context)) {
                            if(isMultipleSelect()){
                                ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(this);
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setOnClickListener(this);
                            }else{
                                ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(null);
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setOnClickListener(null);
                            }
                        }else{
                            ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(null);
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setOnClickListener(null);
                        }

                        ((ViewHolderMessageChat) holder).urlContactMessageTitle.setVisibility(View.VISIBLE);

                        ((ViewHolderMessageChat) holder).urlContactMessageTitle.setText(context.getString(R.string.invalid_chat_link));
                        ((ViewHolderMessageChat) holder).urlContactMessageTitle.setMaxLines(2);
                        ((ViewHolderMessageChat) holder).urlContactMessageDescription.setVisibility(View.INVISIBLE);

                        holder.urlContactMessageGroupAvatarLayout.setVisibility(View.GONE);
                    }
                }
                else{
                    logWarning("Chat-link error: richLinkMessage is NULL");
                }
            }

            checkMultiselectionMode(position, holder, false, message.getMsgId());

            if(!multipleSelect){
                if(positionClicked != INVALID_POSITION && positionClicked == position){
                    holder.forwardContactRichLinks.setEnabled(false);
                }else{
                    holder.forwardContactRichLinks.setEnabled(true);
                }
            }
        }
    }

    public void bindNormalMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            logDebug("MY message handle!!: " + message.getMsgId());
            holder.layoutAvatarMessages.setVisibility(View.GONE);
             holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                 holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.VISIBLE);
            holder.contactMessageLayout.setVisibility(View.GONE);
            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);

            //Forward element (own message)
            holder.forwardOwnRichLinks.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);

            String messageContent = "";

            if (message.getContent() != null) {
                messageContent = converterShortCodes(message.getContent());
            }

            int lastPosition = messages.size();

            if (lastPosition == position) {

                if (MegaChatApi.hasUrl(messageContent)) {

                    if(((ChatActivityLollipop)context).checkMegaLink(message)==-1) {
                        logDebug("Is a link - not from MEGA");

                        if (MegaApplication.isShowRichLinkWarning()) {
                            logWarning("SDK - show link rich warning");
                            if (((ChatActivityLollipop) context).showRichLinkWarning == RICH_WARNING_TRUE) {
                                logWarning("ANDROID - show link rich warning");

                                holder.urlOwnMessageLayout.setVisibility(View.VISIBLE);

                                holder.urlOwnMessageText.setText(messageContent);
                                holder.urlOwnMessageTitle.setVisibility(View.VISIBLE);
                                holder.urlOwnMessageTitle.setText(context.getString(R.string.title_enable_rich_links));
                                holder.urlOwnMessageTitle.setMaxLines(10);

                                holder.urlOwnMessageDescription.setText(context.getString(R.string.text_enable_rich_links));
                                holder.urlOwnMessageDescription.setMaxLines(30);

                                holder.urlOwnMessageImage.setVisibility(View.VISIBLE);
                                holder.urlOwnMessageGroupAvatarLayout.setVisibility(View.GONE);

                                holder.urlOwnMessageImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_rich_link));

                                holder.urlOwnMessageIcon.setVisibility(View.GONE);

                                holder.alwaysAllowRichLinkButton.setOnClickListener(this);
                                holder.alwaysAllowRichLinkButton.setTag(holder);

                                holder.notNowRichLinkButton.setOnClickListener(this);
                                holder.notNowRichLinkButton.setTag(holder);

                                holder.contentOwnMessageText.setVisibility(View.GONE);
                                holder.previewFrameLand.setVisibility(View.GONE);
                                holder.previewFramePort.setVisibility(View.GONE);

                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

                                holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.VISIBLE);
                                ((ChatActivityLollipop) context).hideKeyboard();
                                holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

                                int notNowCounter = MegaApplication.getCounterNotNowRichLinkWarning();

                                if(notNowCounter>=3){
                                    holder.neverRichLinkButton.setVisibility(View.VISIBLE);
                                    holder.neverRichLinkButton.setOnClickListener(this);
                                    holder.neverRichLinkButton.setTag(holder);
                                } else {
                                    holder.neverRichLinkButton.setVisibility(View.GONE);
                                    holder.neverRichLinkButton.setOnClickListener(null);
                                }

                                return;
                            } else if (((ChatActivityLollipop) context).showRichLinkWarning == RICH_WARNING_CONFIRMATION) {
                                logWarning("ANDROID - show link disable rich link confirmation");

                                holder.urlOwnMessageLayout.setVisibility(View.VISIBLE);
                                holder.urlOwnMessageText.setText(messageContent);
                                holder.urlOwnMessageTitle.setVisibility(View.VISIBLE);
                                holder.urlOwnMessageTitle.setText(context.getString(R.string.title_confirmation_disable_rich_links));
                                holder.urlOwnMessageTitle.setMaxLines(10);

                                holder.urlOwnMessageDescription.setText(context.getString(R.string.text_confirmation_disable_rich_links));
                                holder.urlOwnMessageDescription.setMaxLines(30);

                                holder.urlOwnMessageImage.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat) holder).urlOwnMessageGroupAvatarLayout.setVisibility(View.GONE);
                                holder.urlOwnMessageImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_rich_link));

                                holder.urlOwnMessageIcon.setVisibility(View.GONE);

                                holder.noDisableButton.setOnClickListener(this);
                                holder.noDisableButton.setTag(holder);

                                holder.yesDisableButton.setOnClickListener(this);
                                holder.yesDisableButton.setTag(holder);

                                holder.contentOwnMessageText.setVisibility(View.GONE);
                                holder.previewFrameLand.setVisibility(View.GONE);
                                holder.previewFramePort.setVisibility(View.GONE);

                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

                                holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
                                holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    else{
                        logWarning("It a MEGA link: wait for info update");
                    }
                }
            }
            if (message.isEdited()) {
                logDebug("MY Message is edited");

                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                holder.contentOwnMessageText.setVisibility(View.VISIBLE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);
                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

                int status = message.getStatus();

                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    logDebug("Show triangle retry!");
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.VISIBLE);
                    holder.retryAlert.setVisibility(View.VISIBLE);
                }else if((status==MegaChatMessage.STATUS_SENDING)){
                    logDebug("Status not received by server: " + message.getStatus());
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }else{
                    logDebug("Status: "+message.getStatus());
                    isRemovingTextMessage(position, holder, message);
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                SimpleSpanBuilder ssb = formatText(context, messageContent);
                if(ssb!=null){
                    ssb.append(" "+context.getString(R.string.edited_message_text), new RelativeSizeSpan(0.70f), new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), new StyleSpan(Typeface.ITALIC));
                    ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                }
                else{
                    Spannable content = new SpannableString(messageContent);
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(content+" ");

                    Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                    edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    ((ViewHolderMessageChat) holder).contentOwnMessageText.append(edited);
                }

                checkEmojiSize(messageContent, holder.contentOwnMessageText);
                if (isOnline(context)) {
                    if(isMultipleSelect()){
                        holder.contentOwnMessageText.setLinksClickable(false);
                    }else{
                        holder.contentOwnMessageText.setLinksClickable(true);
                        Linkify.addLinks(holder.contentOwnMessageText, Linkify.WEB_URLS);
                        holder.contentOwnMessageText.setLinkTextColor(ContextCompat.getColor(context, R.color.white));
                    }
                }else{
                    holder.contentOwnMessageText.setLinksClickable(false);
                }

                checkMultiselectionMode(position, holder, true, message.getMsgId());

            }else if (message.isDeleted()) {
                logDebug("MY Message is deleted");
                holder.contentOwnMessageLayout.setVisibility(View.GONE);
                holder.ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                holder.ownManagementMessageText.setText(context.getString(R.string.text_deleted_message));

                holder.ownManagementMessageLayout.setVisibility(View.GONE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.contentOwnMessageThumbLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);
                holder.contentOwnMessageThumbPort.setVisibility(View.GONE);

                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);
                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

            } else {
                logDebug("Message not edited not deleted");

                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                holder.ownManagementMessageLayout.setVisibility(View.GONE);
                holder.contentOwnMessageText.setVisibility(View.VISIBLE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);
                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

                SimpleSpanBuilder ssb = formatText(context, messageContent);
                int status = message.getStatus();
                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    logDebug("Show triangle retry!");
                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.VISIBLE);
                    holder.retryAlert.setVisibility(View.VISIBLE);

                } else if ((status == MegaChatMessage.STATUS_SENDING)) {
                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);

                } else {
                    logDebug("Status: " + message.getStatus());
                    isRemovingTextMessage(position, holder, message);
                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                checkEmojiSize(messageContent, holder.contentOwnMessageText);

                if (ssb != null) {
                    holder.contentOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    holder.contentOwnMessageText.setText(messageContent);
                }

                if (isOnline(context)) {
                    if(isMultipleSelect()){
                        holder.contentOwnMessageText.setLinksClickable(false);
                    }else{
                        holder.contentOwnMessageText.setLinksClickable(true);
                        Linkify.addLinks(holder.contentOwnMessageText, Linkify.WEB_URLS);
                        holder.contentOwnMessageText.setLinkTextColor(ContextCompat.getColor(context, R.color.white));
                    }
                }else {
                    holder.contentOwnMessageText.setLinksClickable(false);
                }

                checkMultiselectionMode(position, holder, true, message.getMsgId());
            }

            interceptLinkClicks(context, holder.contentOwnMessageText);
        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            //forward element (contact message)
            holder.forwardContactRichLinks.setVisibility(View.GONE);
            holder.forwardContactContact.setVisibility(View.GONE);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactFile.setVisibility(View.GONE);

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);

            holder.contactManagementMessageLayout.setVisibility(View.GONE);
            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);

            holder.contentContactMessageVoiceClipLayout.setVisibility(View.GONE);

            if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle);
            } else {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
            }

            if (message.isEdited()) {
                logDebug("Message is edited");

                String messageContent = "";
                if (message.getContent() != null) {
                    messageContent = converterShortCodes(message.getContent());
                }
                ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).urlContactMessageLayout.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

                SimpleSpanBuilder ssb = formatText(context, messageContent);
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (ssb != null) {
                        ssb.append(" " + context.getString(R.string.edited_message_text), new RelativeSizeSpan(0.70f), new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), new StyleSpan(Typeface.ITALIC));
                        holder.contentContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                    } else {
                        messageContent = messageContent + " ";
                        Spannable content = new SpannableString(messageContent);
                        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        CharSequence indexedText = TextUtils.concat(messageContent, edited);
                        holder.contentContactMessageText.setText(indexedText);
                    }

                if (isOnline(context)) {
                    if(isMultipleSelect()){
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                    }else {
                        Linkify.addLinks(((ViewHolderMessageChat) holder).contentContactMessageText, Linkify.WEB_URLS);
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(true);
                    }
                } else {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                }

                checkMultiselectionMode(position, holder, false, message.getMsgId());

            }else if (message.isDeleted()) {
                logDebug("Message is deleted");

                ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);


                ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

                if (((ChatActivityLollipop) context).isGroup()) {
                    String textToShow = String.format(context.getString(R.string.text_deleted_message_by), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle));
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#00BFA5\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    } catch (Exception e) {
                    }
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(result);
                } else {
                    ((ViewHolderMessageChat) holder).contactManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                    ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(context.getString(R.string.text_deleted_message));
                }
            } else {

                ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.VISIBLE);

                String messageContent = "";

                ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).urlContactMessageLayout.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);


                if (message.getContent() != null) {
                    messageContent = message.getContent();
                }

                checkEmojiSize(messageContent, holder.contentContactMessageText);

                //Color always status SENT
                ((ViewHolderMessageChat) holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                SimpleSpanBuilder ssb = formatText(context, messageContent);
                if (ssb != null) {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(messageContent);
                }

                if (isOnline(context)) {
                    if(isMultipleSelect()){
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                    }else{
                        Linkify.addLinks(((ViewHolderMessageChat) holder).contentContactMessageText, Linkify.WEB_URLS);
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(true);
                    }
                } else {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                }

                checkMultiselectionMode(position, holder, false, message.getMsgId());
            }

            interceptLinkClicks(context, holder.contentContactMessageText);
        }
    }

    private void checkEmojiSize(String message, EmojiTextView textView) {
        if (EmojiManager.getInstance().isOnlyEmojis(message)) {
            int numEmojis = EmojiManager.getInstance().getNumEmojis(message);
            textView.setLineSpacing(1, 1.2f);
            switch (numEmojis) {
                case 1: {
                    textView.setEmojiSize(px2dp(EMOJI_SIZE_EXTRA_HIGH, outMetrics));
                    break;
                }
                case 2: {
                    textView.setEmojiSize(px2dp(EMOJI_SIZE_HIGH, outMetrics));
                    break;
                }
                case 3: {
                    textView.setEmojiSize(px2dp(EMOJI_SIZE_MEDIUM, outMetrics));
                    break;
                }
                default: {
                    textView.setEmojiSize(px2dp(EMOJI_SIZE, outMetrics));
                    break;
                }
            }
        } else {
            textView.setLineSpacing(1, 1.0f);
            textView.setEmojiSize(px2dp(EMOJI_SIZE, outMetrics));
        }
    }

    public void bindNodeAttachmentMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("position: " + position);

        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            logDebug("MY message!!");
            holder.layoutAvatarMessages.setVisibility(View.GONE);

            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }
            logDebug("MY message handle!!: " + message.getMsgId());
            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.VISIBLE);
            holder.contactMessageLayout.setVisibility(View.GONE);

            String messageContent = "";

            if (message.getContent() != null) {
                messageContent = message.getContent();
            }

            AndroidMegaChatMessage androidMsg = messages.get(position - 1);

            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageText.setVisibility(View.GONE);
            holder.urlOwnMessageLayout.setVisibility(View.GONE);

            //Forward element(own message):
            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardOwnFile.setVisibility(View.GONE);
            }
            else {
                holder.forwardOwnFile.setVisibility(View.VISIBLE);
                holder.forwardOwnFile.setOnClickListener(this);
            }

            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.previewFrameLand.setVisibility(View.GONE);
            holder.previewFramePort.setVisibility(View.GONE);

            holder.contentOwnMessageFileLayout.setVisibility(View.VISIBLE);
            holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);
            holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

            int status = message.getStatus();
            logDebug("Status: " + message.getStatus());
            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {

                holder.contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
            }else if (status == MegaChatMessage.STATUS_SENDING) {

                holder.contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
            }else {
                holder.contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
            }

            holder.contentOwnMessageFileThumb.setVisibility(View.VISIBLE);
            holder.contentOwnMessageFileName.setVisibility(View.VISIBLE);
            holder.contentOwnMessageFileSize.setVisibility(View.VISIBLE);

            checkMultiselectionMode(position, holder, true, message.getMsgId());

            if (!multipleSelect) {
                if (positionClicked != INVALID_POSITION && positionClicked == position) {
                    holder.forwardOwnFile.setEnabled(false);
                    holder.forwardOwnPortrait.setEnabled(false);
                    holder.forwardOwnLandscape.setEnabled(false);
                } else {
                    holder.forwardOwnFile.setEnabled(true);
                    holder.forwardOwnPortrait.setEnabled(true);
                    holder.forwardOwnLandscape.setEnabled(true);
                }
            }

            MegaNodeList nodeList = message.getMegaNodeList();
            if (nodeList != null) {
                if (nodeList.size() == 1) {
                    MegaNode node = nodeList.get(0);

                    logDebug("Node Handle: " + node.getHandle());

                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        logDebug("Landscape configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                        holder.contentOwnMessageFileName.setMaxWidth((int) width);
                        holder.contentOwnMessageFileSize.setMaxWidth((int) width);
                    } else {
                        logDebug("Portrait configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                        holder.contentOwnMessageFileName.setMaxWidth((int) width);
                        holder.contentOwnMessageFileSize.setMaxWidth((int) width);
                    }
                    holder.contentOwnMessageFileName.setText(node.getName());

                    long nodeSize = node.getSize();
                    holder.contentOwnMessageFileSize.setText(getSizeString(nodeSize));
                    holder.contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                    Bitmap preview = null;
                    if (node.hasPreview()) {

                        preview = getPreviewFromCache(node);
                        if (preview != null) {
                            previewCache.put(node.getHandle(), preview);
                            setOwnPreview(holder, preview, node);
                            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                setErrorStateOnPreview(holder, preview);
                            }

                        } else {
                            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                holder.errorUploadingFile.setVisibility(View.VISIBLE);
                                holder.retryAlert.setVisibility(View.VISIBLE);
                                holder.forwardOwnFile.setVisibility(View.GONE);
                                holder.forwardOwnPortrait.setVisibility(View.GONE);
                                holder.forwardOwnLandscape.setVisibility(View.GONE);
                            }
                            try {
                                new MegaChatLollipopAdapter.ChatPreviewAsyncTask(holder, message.getMsgId()).execute(node);
                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                logError("Too many AsyncTasks", ex);
                            }


                        }
                    } else {
                        logWarning("Node has no preview on servers");

                        preview = getPreviewFromCache(node);
                        if (preview != null) {

                            previewCache.put(node.getHandle(), preview);
                            if (preview.getWidth() < preview.getHeight()) {

                                logDebug("Portrait");
                                holder.contentOwnMessageThumbPort.setImageBitmap(preview);
                                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    logDebug("Is pfd preview");
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);
                                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    logDebug("Is video preview");
                                    holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoTimecontentOwnMessageThumbPort.setText(timeVideo(node));
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

                                } else {
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);
                                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                                }

                                holder.previewFramePort.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.previewFrameLand.setVisibility(View.GONE);
                                holder.contentOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.errorUploadingLandscape.setVisibility(View.GONE);
                                holder.transparentCoatingLandscape.setVisibility(View.GONE);

                                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                    holder.errorUploadingPortrait.setVisibility(View.VISIBLE);
                                    holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);
                                    holder.retryAlert.setVisibility(View.VISIBLE);

                                    holder.forwardOwnFile.setVisibility(View.GONE);
                                    holder.forwardOwnPortrait.setVisibility(View.GONE);
                                    holder.forwardOwnLandscape.setVisibility(View.GONE);

                                } else {
                                    holder.errorUploadingPortrait.setVisibility(View.GONE);
                                    holder.transparentCoatingPortrait.setVisibility(View.GONE);
                                    holder.retryAlert.setVisibility(View.GONE);

                                    if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                        holder.forwardOwnPortrait.setVisibility(View.GONE);
                                    } else {
                                        holder.forwardOwnPortrait.setVisibility(View.VISIBLE);
                                        holder.forwardOwnPortrait.setOnClickListener(this);
                                    }
                                    holder.forwardOwnFile.setVisibility(View.GONE);
                                    holder.forwardOwnLandscape.setVisibility(View.GONE);
                                }

                                holder.errorUploadingFile.setVisibility(View.GONE);

                                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            } else {
                                logDebug("Landscape");
                                holder.contentOwnMessageThumbLand.setImageBitmap(preview);
                                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    logDebug("Is pfd preview");
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    logDebug("Is video preview");
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoTimecontentOwnMessageThumbLand.setText(timeVideo(node));
                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);

                                } else {
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                }

                                holder.previewFrameLand.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.previewFramePort.setVisibility(View.GONE);
                                holder.contentOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.errorUploadingPortrait.setVisibility(View.GONE);
                                holder.transparentCoatingPortrait.setVisibility(View.GONE);

                                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                    holder.errorUploadingLandscape.setVisibility(View.VISIBLE);
                                    holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);
                                    holder.retryAlert.setVisibility(View.VISIBLE);

                                    holder.forwardOwnFile.setVisibility(View.GONE);
                                    holder.forwardOwnPortrait.setVisibility(View.GONE);
                                    holder.forwardOwnLandscape.setVisibility(View.GONE);

                                } else {
                                    holder.errorUploadingLandscape.setVisibility(View.GONE);
                                    holder.transparentCoatingLandscape.setVisibility(View.GONE);
                                    holder.retryAlert.setVisibility(View.GONE);

                                    if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                        holder.forwardOwnLandscape.setVisibility(View.GONE);
                                    }
                                    else {
                                        holder.forwardOwnLandscape.setVisibility(View.VISIBLE);
                                        holder.forwardOwnLandscape.setOnClickListener(this);
                                    }

                                    holder.forwardOwnFile.setVisibility(View.GONE);
                                    holder.forwardOwnPortrait.setVisibility(View.GONE);
                                }

                                holder.errorUploadingFile.setVisibility(View.GONE);
                                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                            }
                        }else {
                            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                holder.errorUploadingFile.setVisibility(View.VISIBLE);
                                holder.retryAlert.setVisibility(View.VISIBLE);
                                holder.forwardOwnFile.setVisibility(View.GONE);
                                holder.forwardOwnPortrait.setVisibility(View.GONE);
                                holder.forwardOwnLandscape.setVisibility(View.GONE);
                            }
                            try {
                                new MegaChatLollipopAdapter.ChatLocalPreviewAsyncTask(((ViewHolderMessageChat) holder), message.getMsgId()).execute(node);

                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                logError("Too many AsyncTasks", ex);
                            }
                        }
                    }
                } else {
                    long totalSize = 0;
                    int count = 0;
                    for (int i = 0; i < nodeList.size(); i++) {
                        MegaNode temp = nodeList.get(i);
                        count++;
                        logDebug("Node Handle: " + temp.getHandle());
                        totalSize = totalSize + temp.getSize();
                    }

                    holder.contentOwnMessageFileSize.setText(getSizeString(totalSize));

                    MegaNode node = nodeList.get(0);
                    holder.contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                    if (count == 1) {
                        holder.contentOwnMessageFileName.setText(node.getName());
                    } else {
                        holder.contentOwnMessageFileName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, count, count));
                    }
                }
            }
        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);
            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
            holder.contactManagementMessageLayout.setVisibility(View.GONE);

            holder.contentContactMessageVoiceClipLayout.setVisibility(View.GONE);
            if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(holder, userHandle, ((ViewHolderMessageChat) holder).fullNameTitle);
            } else {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
            }

            holder.contentContactMessageText.setVisibility(View.GONE);
            holder.urlContactMessageLayout.setVisibility(View.GONE);

            holder.contentContactMessageThumbLand.setVisibility(View.GONE);

            holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            holder.contentContactMessageThumbPort.setVisibility(View.GONE);

            holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            holder.contentContactMessageAttachLayout.setVisibility(View.VISIBLE);
            holder.contentContactMessageFile.setVisibility(View.VISIBLE);

            //Forward element (contact messages):
            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardContactFile.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactFile.setVisibility(View.VISIBLE);
                holder.forwardContactFile.setOnClickListener(this);
            }

            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            holder.forwardContactRichLinks.setVisibility(View.GONE);
            holder.forwardContactContact.setVisibility(View.GONE);

            holder.contentContactMessageFileThumb.setVisibility(View.VISIBLE);
            holder.contentContactMessageFileName.setVisibility(View.VISIBLE);
            holder.contentContactMessageFileSize.setVisibility(View.VISIBLE);
            holder.contentContactMessageContactLayout.setVisibility(View.GONE);

            checkMultiselectionMode(position, holder, false, message.getMsgId());

            if (!multipleSelect) {
                if (positionClicked != INVALID_POSITION && positionClicked == position) {
                    holder.forwardContactFile.setEnabled(false);
                    holder.forwardContactPreviewPortrait.setEnabled(false);
                    holder.forwardContactPreviewLandscape.setEnabled(false);
                } else {
                    holder.forwardContactFile.setEnabled(true);
                    holder.forwardContactPreviewPortrait.setEnabled(true);
                    holder.forwardContactPreviewLandscape.setEnabled(true);
                }
            }

            MegaNodeList nodeList = message.getMegaNodeList();
            if (nodeList != null) {
                if (nodeList.size() == 1) {
                    MegaNode node = nodeList.get(0);

                    logDebug("Node Handle: " + node.getHandle());

                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        logDebug("Landscape configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                        holder.contentContactMessageFileName.setMaxWidth((int) width);
                        holder.contentContactMessageFileSize.setMaxWidth((int) width);
                    } else {
                        logDebug("Portrait configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                        holder.contentContactMessageFileName.setMaxWidth((int) width);
                        holder.contentContactMessageFileSize.setMaxWidth((int) width);
                    }
                    holder.contentContactMessageFileName.setText(node.getName());
                    long nodeSize = node.getSize();
                    holder.contentContactMessageFileSize.setText(getSizeString(nodeSize));
                    holder.contentContactMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                    logDebug("Get preview of node");
                    Bitmap preview = null;
                    if (node.hasPreview()) {
                        logDebug("Get preview of node");
                        preview = getPreviewFromCache(node);
                        if (preview != null) {
                            previewCache.put(node.getHandle(), preview);

                            if (preview.getWidth() < preview.getHeight()) {
                                logDebug("Portrait");

                                holder.contentContactMessageThumbPort.setImageBitmap(preview);
                                holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    logDebug("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);
                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    logDebug("Contact message - Is video preview");
                                    holder.gradientContactMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoTimecontentContactMessageThumbPort.setText(timeVideo(node));
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

                                } else {
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);
                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                                }

                                holder.contentContactMessageThumbPort.setVisibility(View.VISIBLE);

                                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                    holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
                                }
                                else {
                                    holder.forwardContactPreviewPortrait.setVisibility(View.VISIBLE);
                                    holder.forwardContactPreviewPortrait.setOnClickListener(this);
                                }

                                holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
                                holder.forwardContactFile.setVisibility(View.GONE);

                                holder.contentContactMessageThumbLand.setVisibility(View.GONE);
                                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
                                holder.contentContactMessageFile.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.getLayoutParams();
                                contactThumbParams.setMargins(0, 0, 0, 0);
                                holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                            } else {
                                logDebug("Landscape");

                                holder.contentContactMessageThumbLand.setImageBitmap(preview);
                                holder.contentContactMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    logDebug("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);
                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    logDebug("Contact message - Is video preview");
                                    holder.gradientContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoTimecontentContactMessageThumbLand.setText(timeVideo(node));
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);
                                } else {
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);
                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
                                }

                                holder.contentContactMessageThumbLand.setVisibility(View.VISIBLE);

                                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                    holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
                                }
                                else {
                                    holder.forwardContactPreviewLandscape.setVisibility(View.VISIBLE);
                                    holder.forwardContactPreviewLandscape.setOnClickListener(this);
                                }

                                holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
                                holder.forwardContactFile.setVisibility(View.GONE);

                                holder.contentContactMessageThumbPort.setVisibility(View.GONE);
                                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                                holder.contentContactMessageFile.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) holder.contentContactMessageThumbLand.getLayoutParams();
                                contactThumbParams.setMargins(0, 0, 0, 0);
                                holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);

                            }

                        } else {
                            try {
                                new MegaChatLollipopAdapter.ChatPreviewAsyncTask(holder, message.getMsgId()).execute(node);

                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                logError("Too many AsyncTasks", ex);
                            }
                        }

                    } else {
                        logWarning("Node has no preview on servers");

                        preview = getPreviewFromCache(node);
                        if (preview != null) {
                            previewCache.put(node.getHandle(), preview);
                            setContactPreview(holder, preview, node);
                        } else {

                            try {
                                new MegaChatLollipopAdapter.ChatLocalPreviewAsyncTask(holder, message.getMsgId()).execute(node);

                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                logError("Too many AsyncTasks", ex);
                            }
                        }
                    }
                } else {
                    long totalSize = 0;
                    int count = 0;
                    for (int i = 0; i < nodeList.size(); i++) {
                        MegaNode temp = nodeList.get(i);
                        count++;
                        totalSize = totalSize + temp.getSize();
                    }
                    ((ViewHolderMessageChat) holder).contentContactMessageFileSize.setText(getSizeString(totalSize));
                    MegaNode node = nodeList.get(0);
                    ((ViewHolderMessageChat) holder).contentContactMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                    if (count == 1) {
                        ((ViewHolderMessageChat) holder).contentContactMessageFileName.setText(node.getName());
                    } else {
                        ((ViewHolderMessageChat) holder).contentContactMessageFileName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, count, count));
                    }
                }
            }
        }
        if (!((ViewHolderMessageChat) holder).contentVisible) {
            if (((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.getVisibility() == View.VISIBLE) {
                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.background_node_attachment));
            }
            if (((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.getVisibility() == View.VISIBLE) {
                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.background_node_attachment));
            }
            if (((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.getVisibility() == View.VISIBLE) {
                ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.background_node_attachment));
            }
            if (((ViewHolderMessageChat) holder).contentContactMessageThumbLand.getVisibility() == View.VISIBLE) {
                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.background_node_attachment));
            }
            if (((ViewHolderMessageChat) holder).contentContactMessageThumbPort.getVisibility() == View.VISIBLE) {
                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.background_node_attachment));
            }
            if (((ViewHolderMessageChat) holder).contentContactMessageFileThumb.getVisibility() == View.VISIBLE) {
                ((ViewHolderMessageChat) holder).contentContactMessageFileThumb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.background_node_attachment));
            }
        }
    }


    public void bindVoiceClipAttachmentMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int positionInAdapter) {
        logDebug("positionInAdapter: " + positionInAdapter);

        MegaChatMessage message = androidMessage.getMessage();
        final long messageUserHandle = message.getUserHandle();
        final long messageId = message.getMsgId();
        long messageHandle = -1;

        holder.totalDurationOfVoiceClip = 0;
        MegaNodeList nodeListOwn = message.getMegaNodeList();
        if(nodeListOwn.size() >= 1 && isVoiceClip(nodeListOwn.get(0).getName())) {
            holder.totalDurationOfVoiceClip = getVoiceClipDuration(nodeListOwn.get(0));
            messageHandle = message.getMegaNodeList().get(0).getHandle();

        }

        if(messagesPlaying == null) messagesPlaying = new ArrayList<>();
        boolean exist = false;
        if(!messagesPlaying.isEmpty()){
            for(MessageVoiceClip m:messagesPlaying){
                if(m.getIdMessage() == messageId){
                    exist = true;
                    break;
                }
            }
        }
        if(!exist){
            MessageVoiceClip messagePlaying = new MessageVoiceClip(messageId, messageUserHandle, messageHandle);
            messagesPlaying.add(messagePlaying);
        }

        MessageVoiceClip currentMessagePlaying = null;
        for(MessageVoiceClip m: messagesPlaying){
            if(m.getIdMessage() == messageId){
                currentMessagePlaying = m;
                break;
            }
        }


        if (message.getUserHandle() == myUserHandle) {
            logDebug("MY message!!");
            holder.layoutAvatarMessages.setVisibility(View.GONE);
            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            logDebug("MY message handle!!: " + message.getMsgId());
            if (messages.get(positionInAdapter - 1).getInfoToShow() != -1) {
                setInfoToShow(positionInAdapter, holder, true, messages.get(positionInAdapter -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.VISIBLE);
            holder.contactMessageLayout.setVisibility(View.GONE);

            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageText.setVisibility(View.GONE);
            holder.urlOwnMessageLayout.setVisibility(View.GONE);

            //Forward element(own message):
            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.previewFrameLand.setVisibility(View.GONE);
            holder.previewFramePort.setVisibility(View.GONE);

            holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
            holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

            //voice clip elements
            holder.contentOwnMessageVoiceClipLayout.setVisibility(View.VISIBLE);
            holder.contentOwnMessageVoiceClipSeekBar.setMax((int) holder.totalDurationOfVoiceClip);

            int status = message.getStatus();
            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                logDebug("myMessage: STATUS_SERVER_REJECTED || STATUS_SENDING_MANUAL");
                holder.notAvailableOwnVoiceclip.setVisibility(View.VISIBLE);
                holder.contentOwnMessageVoiceClipPlay.setVisibility(View.GONE);
                holder.uploadingOwnProgressbarVoiceclip.setVisibility(View.GONE);

                holder.contentOwnMessageVoiceClipLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                holder.errorUploadingVoiceClip.setVisibility(View.VISIBLE);
                holder.retryAlert.setVisibility(View.VISIBLE);

                holder.contentOwnMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                holder.contentOwnMessageVoiceClipSeekBar.setEnabled(false);
                holder.contentOwnMessageVoiceClipSeekBar.setProgress(0);
                holder.contentOwnMessageVoiceClipDuration.setText("-:--");

            }else if (status == MegaChatMessage.STATUS_SENDING) {
                logDebug("myMessage: STATUS_SENDING ");
                holder.uploadingOwnProgressbarVoiceclip.setVisibility(View.VISIBLE);
                holder.notAvailableOwnVoiceclip.setVisibility(View.GONE);
                holder.contentOwnMessageVoiceClipPlay.setVisibility(View.GONE);

                holder.contentOwnMessageVoiceClipLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                holder.errorUploadingVoiceClip.setVisibility(View.GONE);
                holder.retryAlert.setVisibility(View.GONE);

                holder.contentOwnMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                holder.contentOwnMessageVoiceClipSeekBar.setEnabled(false);
                holder.contentOwnMessageVoiceClipSeekBar.setProgress(0);
                holder.contentOwnMessageVoiceClipDuration.setText("-:--");

            }else {
                if((holder.totalDurationOfVoiceClip == 0) || (currentMessagePlaying.getIsAvailable() == ERROR_VOICE_CLIP_TRANSFER)){
                    logWarning("myMessage: SENT -> duraton 0 or available == error");
                    holder.notAvailableOwnVoiceclip.setVisibility(View.VISIBLE);
                    holder.uploadingOwnProgressbarVoiceclip.setVisibility(View.GONE);
                    holder.contentOwnMessageVoiceClipPlay.setVisibility(View.GONE);
                    holder.contentOwnMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                    holder.contentOwnMessageVoiceClipSeekBar.setEnabled(false);
                    holder.contentOwnMessageVoiceClipSeekBar.setProgress(0);
                    holder.contentOwnMessageVoiceClipDuration.setText("--:--");

                }else{
                    logDebug("myMessage: SENT -> available ");
                    boolean isDownloaded = false;
                    File vcFile = buildVoiceClipFile(context, message.getMegaNodeList().get(0).getName());
                    if(isFileAvailable(vcFile) && vcFile.length() == message.getMegaNodeList().get(0).getSize()){
                        isDownloaded = true;
                    }

                    if(!isDownloaded){
                        logDebug("myMessage: is not downloaded ");
                        holder.uploadingOwnProgressbarVoiceclip.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageVoiceClipPlay.setVisibility(View.GONE);
                        holder.notAvailableOwnVoiceclip.setVisibility(View.GONE);
                        holder.contentOwnMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                        holder.contentOwnMessageVoiceClipSeekBar.setEnabled(false);
                        holder.contentOwnMessageVoiceClipSeekBar.setProgress(0);
                        holder.contentOwnMessageVoiceClipDuration.setText("--:--");
                        downloadVoiceClip(holder, positionInAdapter, message.getUserHandle(), message.getMegaNodeList());
                    }else{
                        logDebug("myMessage: id " + message.getMsgId() + "is downloaded");
                        if (isDownloaded && currentMessagePlaying.isPlayingWhenTheScreenRotated()) {
                            currentMessagePlaying.setPlayingWhenTheScreenRotated(false);
                            playVoiceClip(currentMessagePlaying, vcFile.getAbsolutePath());
                        }
                        holder.contentOwnMessageVoiceClipPlay.setVisibility(View.VISIBLE);
                        holder.notAvailableOwnVoiceclip.setVisibility(View.GONE);
                        holder.uploadingOwnProgressbarVoiceclip.setVisibility(View.GONE);
                        if(currentMessagePlaying.getMediaPlayer().isPlaying()){
                            holder.contentOwnMessageVoiceClipPlay.setImageResource(R.drawable.ic_pause_grey);
                        }else{
                            holder.contentOwnMessageVoiceClipPlay.setImageResource(R.drawable.ic_play_grey);
                        }

                        if(currentMessagePlaying.getProgress() == 0){
                            holder.contentOwnMessageVoiceClipDuration.setText(milliSecondsToTimer(holder.totalDurationOfVoiceClip));
                        }else{
                            holder.contentOwnMessageVoiceClipDuration.setText(milliSecondsToTimer(currentMessagePlaying.getProgress()));
                        }

                        holder.contentOwnMessageVoiceClipSeekBar.setProgress(currentMessagePlaying.getProgress());
                        holder.contentOwnMessageVoiceClipSeekBar.setEnabled(true);
                        holder.contentOwnMessageVoiceClipSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                if(fromUser) {
                                    updatingSeekBar(messageId, progress);
                                }
                            }
                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) { }
                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) { }
                        });
                    }
                }

                holder.contentOwnMessageVoiceClipDuration.setVisibility(View.VISIBLE);
                holder.contentOwnMessageVoiceClipSeekBar.setVisibility(View.VISIBLE);

                holder.contentOwnMessageVoiceClipLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                holder.errorUploadingVoiceClip.setVisibility(View.GONE);
                holder.retryAlert.setVisibility(View.GONE);
            }

            checkMultiselectionMode(positionInAdapter, holder, true, message.getMsgId());

            if (multipleSelect) {
                holder.contentOwnMessageVoiceClipPlay.setOnClickListener(null);
                holder.contentOwnMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                holder.contentOwnMessageVoiceClipSeekBar.setEnabled(false);
                if(currentMessagePlaying.getMediaPlayer().isPlaying()){
                    holder.contentOwnMessageVoiceClipPlay.setImageResource(R.drawable.ic_play_grey);
                    currentMessagePlaying.getMediaPlayer().pause();
                    currentMessagePlaying.setProgress(currentMessagePlaying.getMediaPlayer().getCurrentPosition());
                    currentMessagePlaying.setPaused(true);
                    removeCallBacks();
                }
            } else {
                holder.contentOwnMessageVoiceClipPlay.setOnClickListener(this);
            }

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message: " + userHandle);

            setContactMessageName(positionInAdapter, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(positionInAdapter - 1).getInfoToShow() != -1) {
                setInfoToShow(positionInAdapter, holder, false, messages.get(positionInAdapter -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);
            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
            holder.contactManagementMessageLayout.setVisibility(View.GONE);

            if (messages.get(positionInAdapter - 1).isShowAvatar() && !isMultipleSelect()) {
                holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(holder, userHandle, holder.fullNameTitle);
            } else {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
            }

            holder.contentContactMessageText.setVisibility(View.GONE);
            holder.urlContactMessageLayout.setVisibility(View.GONE);

            holder.contentContactMessageThumbLand.setVisibility(View.GONE);

            holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            holder.contentContactMessageThumbPort.setVisibility(View.GONE);

            holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            holder.contentContactMessageAttachLayout.setVisibility(View.GONE);
            holder.contentContactMessageFile.setVisibility(View.GONE);

            //Forward element (contact messages):
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            holder.forwardContactRichLinks.setVisibility(View.GONE);
            holder.forwardContactContact.setVisibility(View.GONE);

            holder.contentContactMessageFileThumb.setVisibility(View.GONE);
            holder.contentContactMessageFileName.setVisibility(View.GONE);
            holder.contentContactMessageFileSize.setVisibility(View.GONE);
            holder.contentContactMessageContactLayout.setVisibility(View.GONE);

            //Voice clip elements:
            holder.contentContactMessageVoiceClipLayout.setVisibility(View.VISIBLE);
            holder.contentContactMessageVoiceClipSeekBar.setMax((int) holder.totalDurationOfVoiceClip);

            if((holder.totalDurationOfVoiceClip == 0) || (currentMessagePlaying.getIsAvailable() == ERROR_VOICE_CLIP_TRANSFER)){
                logWarning("ContMessage:SENT -> duraton 0 or available == error");
                holder.notAvailableContactVoiceclip.setVisibility(View.VISIBLE);
                holder.uploadingContactProgressbarVoiceclip.setVisibility(View.GONE);
                holder.contentContactMessageVoiceClipPlay.setVisibility(View.GONE);
                holder.contentContactMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                holder.contentContactMessageVoiceClipSeekBar.setEnabled(false);
                holder.contentContactMessageVoiceClipSeekBar.setProgress(0);
                holder.contentContactMessageVoiceClipDuration.setText("--:--");

            }else{

                boolean isDownloaded = false;

                File vcFile = buildVoiceClipFile(context, message.getMegaNodeList().get(0).getName());
                if(isFileAvailable(vcFile) && vcFile.length() == message.getMegaNodeList().get(0).getSize()){
                    isDownloaded = true;
                }

                if(!isDownloaded){
                    logDebug("ContMessage -> is not downloaded -> downloadVoiceClip");
                    holder.uploadingContactProgressbarVoiceclip.setVisibility(View.VISIBLE);
                    holder.contentContactMessageVoiceClipPlay.setVisibility(View.GONE);
                    holder.notAvailableContactVoiceclip.setVisibility(View.GONE);
                    holder.contentContactMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                    holder.contentContactMessageVoiceClipSeekBar.setEnabled(false);
                    holder.contentContactMessageVoiceClipSeekBar.setProgress(0);
                    holder.contentContactMessageVoiceClipDuration.setText("--:--");
                    downloadVoiceClip(holder, positionInAdapter, message.getUserHandle(), message.getMegaNodeList());

                }else{
                    logDebug("ContMessage -> is downloaded");
                    if (isDownloaded && currentMessagePlaying.isPlayingWhenTheScreenRotated()) {
                        currentMessagePlaying.setPlayingWhenTheScreenRotated(false);
                        playVoiceClip(currentMessagePlaying, vcFile.getAbsolutePath());
                    }

                    holder.contentContactMessageVoiceClipPlay.setVisibility(View.VISIBLE);
                    holder.notAvailableContactVoiceclip.setVisibility(View.GONE);
                    holder.uploadingContactProgressbarVoiceclip.setVisibility(View.GONE);

                    if(currentMessagePlaying.getMediaPlayer().isPlaying()){
                        holder.contentContactMessageVoiceClipPlay.setImageResource(R.drawable.ic_pause_grey);
                    }else{
                        holder.contentContactMessageVoiceClipPlay.setImageResource(R.drawable.ic_play_grey);
                    }

                    if(currentMessagePlaying.getProgress() == 0){
                        holder.contentContactMessageVoiceClipDuration.setText(milliSecondsToTimer(holder.totalDurationOfVoiceClip));
                    }else{
                        holder.contentContactMessageVoiceClipDuration.setText(milliSecondsToTimer(currentMessagePlaying.getProgress()));
                    }

                    holder.contentContactMessageVoiceClipSeekBar.setProgress(currentMessagePlaying.getProgress());
                    holder.contentContactMessageVoiceClipSeekBar.setEnabled(true);

                    holder.contentContactMessageVoiceClipSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if(fromUser) {
                                updatingSeekBar(messageId, progress);
                            }
                        }
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) { }
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) { }
                    });

                }
            }

            holder.contentContactMessageVoiceClipDuration.setVisibility(View.VISIBLE);
            holder.contentContactMessageVoiceClipSeekBar.setVisibility(View.VISIBLE);

            checkMultiselectionMode(positionInAdapter, holder, false, message.getMsgId());

            if (multipleSelect) {
                holder.contentContactMessageVoiceClipPlay.setOnClickListener(null);
                holder.contentContactMessageVoiceClipSeekBar.setOnSeekBarChangeListener(null);
                holder.contentContactMessageVoiceClipSeekBar.setEnabled(false);

                if(currentMessagePlaying.getMediaPlayer().isPlaying()){
                    holder.contentContactMessageVoiceClipPlay.setImageResource(R.drawable.ic_play_grey);
                    currentMessagePlaying.getMediaPlayer().pause();
                    currentMessagePlaying.setProgress(currentMessagePlaying.getMediaPlayer().getCurrentPosition());
                    currentMessagePlaying.setPaused(true);
                    removeCallBacks();
                }
            } else {
                holder.contentContactMessageVoiceClipPlay.setOnClickListener(this);
            }
        }
    }

    public void bindContactAttachmentMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindContactAttachmentMessage");

        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            holder.layoutAvatarMessages.setVisibility(View.GONE);
            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            logDebug("MY message!!");
            logDebug("MY message handle!!: " + message.getMsgId());

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.VISIBLE);
            holder.contactMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageText.setVisibility(View.GONE);
            holder.previewFrameLand.setVisibility(View.GONE);
            holder.previewFramePort.setVisibility(View.GONE);
            holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
            holder.contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);

            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);

            holder.contentOwnMessageContactLayout.setVisibility(View.VISIBLE);
            holder.contentOwnMessageContactThumb.setVisibility(View.VISIBLE);
            holder.contentOwnMessageContactName.setVisibility(View.VISIBLE);
            holder.contentOwnMessageContactEmail.setVisibility(View.VISIBLE);

            int status = message.getStatus();
            logDebug("Status: " + message.getStatus());
            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                holder.contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                holder.errorUploadingContact.setVisibility(View.VISIBLE);
                holder.retryAlert.setVisibility(View.VISIBLE);
                holder.forwardOwnContact.setVisibility(View.GONE);

            } else if (status == MegaChatMessage.STATUS_SENDING) {
                holder.contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                holder.retryAlert.setVisibility(View.GONE);
                holder.errorUploadingContact.setVisibility(View.GONE);
                holder.forwardOwnContact.setVisibility(View.GONE);

            } else {
                holder.contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                holder.retryAlert.setVisibility(View.GONE);
                holder.errorUploadingContact.setVisibility(View.GONE);

                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                    holder.forwardOwnContact.setVisibility(View.GONE);
                }
                else {
                    holder.forwardOwnContact.setVisibility(View.VISIBLE);
                    holder.forwardOwnContact.setOnClickListener(this);
                }
            }

            if (!isScreenInPortrait(context)) {
                logDebug("Landscape configuration");
                holder.contentOwnMessageContactName.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_LAND, outMetrics));
                holder.contentOwnMessageContactEmail.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_LAND, outMetrics));
            } else {
                logDebug("Portrait configuration");
                holder.contentOwnMessageContactName.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_PORT, outMetrics));
                holder.contentOwnMessageContactEmail.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_PORT, outMetrics));
            }

            long userCount = message.getUsersCount();

            if (userCount == 1) {
                holder.contentOwnMessageContactName.setText(converterShortCodes(getNameContactAttachment(message)));
                holder.contentOwnMessageContactEmail.setText(message.getUserEmail(0));
                setUserAvatar(holder, message);
            } else {
                //Show default avatar with userCount
                StringBuilder name = new StringBuilder("");
                name.append(message.getUserName(0));
                for (int i = 1; i < userCount; i++) {
                    name.append(", " + message.getUserName(i));
                }
                holder.contentOwnMessageContactEmail.setText(name);
                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int) userCount, userCount);
                holder.contentOwnMessageContactName.setText(email);
                Bitmap bitmapDefaultAvatar = getDefaultAvatar(getSpecificAvatarColor(AVATAR_PRIMARY_COLOR), userCount + "", AVATAR_SIZE, true);
                holder.contentOwnMessageContactThumb.setImageBitmap(bitmapDefaultAvatar);
            }

           checkMultiselectionMode(position, holder, true, message.getMsgId());

            if (!multipleSelect) {
                if (positionClicked != INVALID_POSITION && positionClicked == position) {
                    holder.forwardOwnContact.setEnabled(false);
                } else {
                    holder.forwardOwnContact.setEnabled(true);
                }
            }

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }
            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);
            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
            holder.contactManagementMessageLayout.setVisibility(View.GONE);

            holder.contentContactMessageVoiceClipLayout.setVisibility(View.GONE);

            if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(holder, userHandle, holder.fullNameTitle);
            } else {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
            }

            holder.contentContactMessageText.setVisibility(View.GONE);
            holder.contentContactMessageAttachLayout.setVisibility(View.GONE);
            holder.urlContactMessageLayout.setVisibility(View.GONE);
            holder.contentContactMessageContactLayout.setVisibility(View.VISIBLE);

            holder.forwardContactRichLinks.setVisibility(View.GONE);
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);

            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardContactContact.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactContact.setVisibility(View.VISIBLE);
                holder.forwardContactContact.setOnClickListener(this);
            }

            holder.contentContactMessageContactThumb.setVisibility(View.VISIBLE);
            holder.contentContactMessageContactName.setVisibility(View.VISIBLE);
            holder.contentContactMessageContactEmail.setVisibility(View.VISIBLE);

            if (!isScreenInPortrait(context)) {
                holder.contentContactMessageContactName.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_LAND, outMetrics));
                holder.contentContactMessageContactEmail.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_LAND, outMetrics));
            } else {
                holder.contentContactMessageContactName.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_PORT, outMetrics));
                holder.contentContactMessageContactEmail.setMaxWidthEmojis(px2dp(MAX_WIDTH_FILENAME_PORT, outMetrics));
            }

            long userCount = message.getUsersCount();

            if (userCount == 1) {
                holder.contentContactMessageContactName.setText(converterShortCodes(getNameContactAttachment(message)));
                holder.contentContactMessageContactEmail.setText(message.getUserEmail(0));
                setUserAvatar(holder, message);

            } else {
                //Show default avatar with userCount
                StringBuilder name = new StringBuilder("");
                name.append(converterShortCodes(message.getUserName(0)));
                for (int i = 1; i < userCount; i++) {
                    name.append(", " + message.getUserName(i));
                }
                holder.contentContactMessageContactEmail.setText(name);
                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int) userCount, userCount);
                holder.contentContactMessageContactName.setText(email);
                Bitmap bitmap = getDefaultAvatar(getSpecificAvatarColor(AVATAR_PRIMARY_COLOR), userCount + "", AVATAR_SIZE, true);
                holder.contentContactMessageContactThumb.setImageBitmap(bitmap);
            }

            checkMultiselectionMode(position, holder, false, message.getMsgId());

            if (!multipleSelect) {
                if (positionClicked != INVALID_POSITION && positionClicked == position) {
                    holder.forwardContactContact.setEnabled(false);
                } else {
                    holder.forwardContactContact.setEnabled(true);
                }
            }
        }
    }

    /**
     * Method for obtaining the name of an attached contact
     *
     * @param message The message sent or received.
     * @return The name or nick of the contact.
     */
    private String getNameContactAttachment(MegaChatMessage message) {
        String email = message.getUserEmail(0);
        MegaUser megaUser = megaApi.getContact(email);
        String name = getMegaUserNameDB(megaUser);
        if (name == null) {
            name = message.getUserName(0);
            if (isTextEmpty(name)) {
                name = email;
            }
        }
        return name;
    }

    public void bindChangeTitleMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindChangeTitleMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);

        MegaChatMessage message = androidMessage.getMessage();

        if (message.getUserHandle() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            logDebug("MY message!!");
            logDebug("MY message handle!!: " + message.getMsgId());
            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            String messageContent = message.getContent();

            String textToShow = String.format(context.getString(R.string.change_title_messages), toCDATA( megaChatApi.getMyFullname()), toCDATA(messageContent));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
                textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/C]", "</font>");
            } catch (Exception e) {
            }

            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }

            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);

            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);

            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            String messageContent = message.getContent();

            String textToShow = String.format(context.getString(R.string.change_title_messages), toCDATA(holder.fullNameTitle), toCDATA(messageContent));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
                textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/C]", "</font>");
            } catch (Exception e) {
            }

            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }

            ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(result);
        }
    }

    public void bindChatLinkMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindChatLinkMessage");

        MegaChatMessage message = androidMessage.getMessage();
        long userHandle = message.getUserHandle();
        logDebug("Contact message!!: " + userHandle);

        if(userHandle==megaChatApi.getMyUserHandle()){
            ((ViewHolderMessageChat) holder).fullNameTitle = megaChatApi.getMyFullname();
        }
        else{
            setContactMessageName(position, holder, userHandle, false);
        }

        ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
        }else{
            holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
        }

        if (messages.get(position - 1).getInfoToShow() != -1) {
            setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                    formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                    formatTime(message));
        }

        ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

        ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
        ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

        checkMultiselectionMode(position, holder, false, message.getMsgId());

        String textToShow = "";
        int messageType = message.getType();
        if(messageType == MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE){
            textToShow = String.format(context.getString(R.string.message_created_chat_link), toCDATA(holder.fullNameTitle));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            } catch (Exception e) {
                e.printStackTrace();
                logError("Exception formating the string: " + context.getString(R.string.message_created_chat_link), e);
            }
        }
        else if(messageType == MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE){
            textToShow = String.format(context.getString(R.string.message_deleted_chat_link), toCDATA(holder.fullNameTitle));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            } catch (Exception e) {
                e.printStackTrace();
                logError("Exception formating the string: " + context.getString(R.string.message_deleted_chat_link), e);
            }
        }
        else{
            textToShow = String.format(context.getString(R.string.message_set_chat_private), toCDATA(holder.fullNameTitle));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#060000\'><b>");
                textToShow = textToShow.replace("[/B]", "</b></font><br/><br/>");
                textToShow += context.getString(R.string.subtitle_chat_message_enabled_ERK);
            } catch (Exception e) {
                e.printStackTrace();
                logError("Exception formating the string: " + context.getString(R.string.message_set_chat_private), e);
            }
        }

        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(result);
    }

    public void bindTruncateMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindTruncateMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }
            logDebug("MY message!!");
            logDebug("MY message handle!!: " + message.getMsgId());

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));

            String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(megaChatApi.getMyFullname()));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            } catch (Exception e) {
            }
            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

            ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setVisibility(View.GONE);
            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);

            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(holder.fullNameTitle));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            } catch (Exception e) {
            }
            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }

            ((ViewHolderMessageChat) holder).contactManagementMessageText.setText(result);
        }
    }

    public void bindRevokeNodeMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindRevokeNodeMessage()");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
        MegaChatMessage message = androidMessage.getMessage();

        if (message.getUserHandle() == myUserHandle) {
            logDebug("MY message!!");
            logDebug("MY message handle!!: " + message.getMsgId());

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            String messageContent = "";

            if (message.getContent() != null) {
                messageContent = message.getContent();
            }

            AndroidMegaChatMessage androidMsg = messages.get(position - 1);
//            ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentOwnMessageText.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).previewFrameLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactEmail.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
            messageContent = "Attachment revoked";
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setText(messageContent);


        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            if (messages.get(position - 1).isShowAvatar() && !isMultipleSelect()) {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle);
            } else {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
            }

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);

            String messageContent = "";

            ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageVoiceClipLayout.setVisibility(View.GONE);


            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.forwardContactContact.setVisibility(View.GONE);
            holder.forwardContactRichLinks.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageFile.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageFileThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageFileName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageFileSize.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageContactEmail.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
            messageContent = "Attachment revoked";
            ((ViewHolderMessageChat) holder).contentContactMessageText.setText(messageContent);
        }
    }

    public void hideMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("hideMessage");
        ((ViewHolderMessageChat) holder).itemLayout.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);
        params.height = 0;
        ((ViewHolderMessageChat) holder).itemLayout.setLayoutParams(params);
    }

    public void bindNoTypeMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        logDebug("bindNoTypeMessage()");

        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            logDebug("MY message handle!!: " + message.getMsgId());

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, true, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.tour_bar_red));


            if(message.getType()==MegaChatMessage.TYPE_INVALID){
                if(message.getCode()==MegaChatMessage.INVALID_FORMAT){
                    ((ViewHolderMessageChat) holder).contentOwnMessageText.setText(context.getString(R.string.error_message_invalid_format));
                }else if(message.getCode()==MegaChatMessage.INVALID_SIGNATURE){
                    ((ViewHolderMessageChat) holder).contentOwnMessageText.setText(context.getString(R.string.error_message_invalid_signature));
                }else{
                    ((ViewHolderMessageChat) holder).contentOwnMessageText.setText(context.getString(R.string.error_message_unrecognizable));
                }
            }else{
                ((ViewHolderMessageChat) holder).contentOwnMessageText.setText(context.getString(R.string.error_message_unrecognizable));
            }

            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

            ((ViewHolderMessageChat) holder).previewFrameLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageVoiceClipLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

        } else {
            long userHandle = message.getUserHandle();
            logDebug("Contact message!!: " + userHandle);
            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                setInfoToShow(position, holder, false, messages.get(position -1).getInfoToShow(),
                        formatDate(context, message.getTimestamp(), DATE_SHORT_FORMAT),
                        formatTime(message));
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.tour_bar_red));


            if(message.getType()==MegaChatMessage.TYPE_INVALID){
                if(message.getCode()==MegaChatMessage.INVALID_FORMAT){
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(context.getString(R.string.error_message_invalid_format));
                }else if(message.getCode()==MegaChatMessage.INVALID_SIGNATURE){
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(context.getString(R.string.error_message_invalid_signature));
                }else{
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(context.getString(R.string.error_message_unrecognizable));
                }
            }else{
                ((ViewHolderMessageChat) holder).contentContactMessageText.setText(context.getString(R.string.error_message_unrecognizable));
            }

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageVoiceClipLayout.setVisibility(View.GONE);

            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.forwardContactContact.setVisibility(View.GONE);
            holder.forwardContactRichLinks.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageFile.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageFileThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageFileName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageFileSize.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentContactMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentContactMessageContactEmail.setVisibility(View.GONE);
        }
    }

    public void setUserAvatar(ViewHolderMessageChat holder, MegaChatMessage message) {
        logDebug("setUserAvatar");

        String name = message.getUserName(0);
        if (name.trim().isEmpty()) {
            name = message.getUserEmail(0);
        }
        String email = message.getUserEmail(0);
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(message.getUserHandle(0));

        ChatAttachmentAvatarListener listener;
        int color = getColorAvatar(userHandleEncoded);
        Bitmap bitmapDefaultAvatar = getDefaultAvatar(color, name, AVATAR_SIZE, true);
        if (myUserHandle == message.getUserHandle()) {
            holder.contentOwnMessageContactThumb.setImageBitmap(bitmapDefaultAvatar);
            listener = new ChatAttachmentAvatarListener(context, holder, this, true);
        } else {
            holder.contentContactMessageContactThumb.setImageBitmap(bitmapDefaultAvatar);
            listener = new ChatAttachmentAvatarListener(context, holder, this, false);
        }

        File avatar = buildAvatarFile(context, email + ".jpg");
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
                        logWarning("megaApi is Null in Offline mode");
                        return;
                    }

                    megaApi.getUserAvatar(email,buildAvatarFile(context,email + ".jpg").getAbsolutePath(),listener);
                } else {
                    if (myUserHandle == message.getUserHandle()) {
                        holder.contentOwnMessageContactThumb.setImageBitmap(bitmap);
                    } else {
                        holder.contentContactMessageContactInitialLetter.setVisibility(View.GONE);
                        holder.contentContactMessageContactThumb.setImageBitmap(bitmap);
                    }
                }
            } else {
                if (megaApi == null) {
                    logWarning("megaApi is Null in Offline mode");
                    return;
                }

                megaApi.getUserAvatar(email,buildAvatarFile(context,email + ".jpg").getAbsolutePath(),listener);
            }
        } else {
            if (megaApi == null) {
                logWarning("megaApi is Null in Offline mode");
                return;
            }

            megaApi.getUserAvatar(email,buildAvatarFile(context,email + ".jpg").getAbsolutePath(),listener);
        }
    }

    private void setContactAvatar(ViewHolderMessageChat holder, long userHandle, String name) {
        /*Default Avatar*/
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
        holder.contactImageView.setImageBitmap(getDefaultAvatar(getColorAvatar(userHandle), name, AVATAR_SIZE, true));

        /*Avatar*/
        ChatAttachmentAvatarListener listener = new ChatAttachmentAvatarListener(context, holder, this, false);
        File avatar = buildAvatarFile(context, userHandleEncoded + ".jpg");
        Bitmap bitmap;
        if (isFileAvailable(avatar) && avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();

                    if (megaApi == null) {
                        logWarning("megaApi is Null in Offline mode");
                        return;
                    }

                    megaApi.getUserAvatar(userHandleEncoded, buildAvatarFile(context, userHandleEncoded + ".jpg").getAbsolutePath(), listener);
                } else {
                    holder.contactImageView.setImageBitmap(bitmap);
                }
        } else {

            if (megaApi == null) {
                logWarning("megaApi is Null in Offline mode");
                return;
            }

            megaApi.getUserAvatar(userHandleEncoded, buildAvatarFile(context, userHandleEncoded + ".jpg").getAbsolutePath(), listener);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_HEADER;
        return TYPE_ITEM;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        logDebug("setMultipleSelect");
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
            notifyDataSetChanged();
        }
        if (this.multipleSelect) {
            messagesSelectedInChat.clear();
        }
    }

    /**
     * Method for selecting or deselecting a chat message.
     *
     * @param msgId    The messages ID.
     */
    public void toggleSelection(long msgId){
        logDebug("The message selected is "+msgId);
        int position = INVALID_POSITION;
        for (AndroidMegaChatMessage message : messages) {
            if (message != null && message.getMessage() != null && message.getMessage().getMsgId() == msgId) {
                position = messages.indexOf(message);
            }
        }

        if (position == INVALID_POSITION) {
            return;
        }

        if (messagesSelectedInChat.get(msgId) != null) {
            logDebug("Message removed");
            messagesSelectedInChat.remove(msgId);
        } else {
            logDebug("Message selected");
            messagesSelectedInChat.put(msgId, position);
        }

        notifyItemChanged(position + 1);

        if (messagesSelectedInChat.isEmpty()) {
            ((ChatActivityLollipop) context).updateActionModeTitle();
        }
    }

    /**
     * Method for selecting all chat messages.
     */
    public void selectAll() {
        for(AndroidMegaChatMessage message:messages){
            if(!isItemChecked(message.getMessage().getMsgId())){
                toggleSelection(message.getMessage().getMsgId());
            }

        }
    }

    /**
     * Method for deselecting all chat messages.
     */
    public void clearSelections() {
        for(AndroidMegaChatMessage message:messages){
            if(isItemChecked(message.getMessage().getMsgId())){
                toggleSelection(message.getMessage().getMsgId());
            }
        }
    }

    /**
     * Method to know if a message is selected or not.
     *
     * @param msgId The message ID.
     * @return True, if selected. False, if not selected.
     */
    private boolean isItemChecked(long msgId) {
        return messagesSelectedInChat.get(msgId) != null;
    }

    /**
     * Method for obtaining how many messages are selected.
     *
     * @return The number of selected messages.
     */
    public int getSelectedItemCount() {
        return messagesSelectedInChat.size();
    }

    /**
     * Method for obtaining the selected messages.
     *
     * @return The selected messages.
     */
    public ArrayList<Integer> getSelectedItems() {
        if(messagesSelectedInChat == null || messagesSelectedInChat.isEmpty())
            return null;

        ArrayList<Integer> positionsMessagesSelected = new ArrayList<>();
        for (HashMap.Entry<Long, Integer> message: messagesSelectedInChat.entrySet()) {
            positionsMessagesSelected.add(message.getValue());
        }
        return positionsMessagesSelected;
    }

    /*
     * Get list of all selected chats
     */
    public ArrayList<AndroidMegaChatMessage> getSelectedMessages() {
        ArrayList<AndroidMegaChatMessage> returnedMessages = new ArrayList<>();
        if (messagesSelectedInChat == null || messagesSelectedInChat.isEmpty())
            return returnedMessages;

        HashMap<Long, Integer> selectedMessagesSorted = sortByValue(messagesSelectedInChat);

        for (HashMap.Entry<Long, Integer> messageSelected : selectedMessagesSorted.entrySet()) {
            for (AndroidMegaChatMessage message : messages) {
                if (message.getMessage().getMsgId() == messageSelected.getKey()) {
                    returnedMessages.add(message);
                    break;
                }
            }
        }
        return returnedMessages;
    }

    /**
     * Method to sort the selected messages depending on the value.
     *
     * @param listMessages HashMap of current selected messages.
     * @return HashMap of selected messages in order.
     */
    private static HashMap<Long, Integer> sortByValue(HashMap<Long, Integer> listMessages) {
        List<Map.Entry<Long, Integer>> list = new LinkedList<>(listMessages.entrySet());
        Collections.sort(list, (o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));
        HashMap<Long, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }

        return temp;
    }

    /**
     * Method get message in the array of messages.
     *
     * @param position The message position.
     * @return The message.
     */
    public AndroidMegaChatMessage getMessageAtPosition(int position) {
        return messages == null || messages.isEmpty() || messages.get(position) == null ? null : messages.get(position);
    }

    @Override
    public int getFolderCount() {
        return 0;
    }

    @Override
    public int getPlaceholderCount() {
        return placeholderCount;
    }

    @Override
    public int getUnhandledItem() {
        return 0;
    }

    public Object getItem(int position) {
        return messages.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setMessages(ArrayList<AndroidMegaChatMessage> messages) {
        this.messages = messages;
        placeholderCount = 0;
        notifyDataSetChanged();
    }

    public void modifyMessage(ArrayList<AndroidMegaChatMessage> messages, int position) {
        this.messages = messages;
        notifyItemChanged(position);
    }

    public void appendMessage(ArrayList<AndroidMegaChatMessage> messages) {
        this.messages = messages;
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessage(ArrayList<AndroidMegaChatMessage> messages, int position) {
        logDebug("position: " + position);
        this.messages = messages;
        notifyItemInserted(position);
        if (position == messages.size()) {
            logDebug("No need to update more");
        } else {
            int itemCount = messages.size() - position;
            logDebug("Update until end - itemCount: " + itemCount);
            notifyItemRangeChanged(position, itemCount + 1);
        }
    }

    public void removeMessage(int position, ArrayList<AndroidMegaChatMessage> messages) {
        this.messages = messages;

        notifyItemRemoved(position);
        if (position != messages.size()) {
            int itemCount = messages.size() - position;
            notifyItemRangeChanged(position, itemCount);
        }
    }

    /**
     * Get request at specified position in Adapter.
     *
     * @param positionInAdapter The position in adapter.
     * @return The message.
     */
    public AndroidMegaChatMessage getMessageAt(int positionInAdapter) {
        int position = positionInAdapter - 1;
        return messages != null && position >= 0 && position < messages.size() ? messages.get(position) : null;
    }

    public void loadPreviousMessages(ArrayList<AndroidMegaChatMessage> messages, int counter) {
        logDebug("counter: " + counter);
        this.messages = messages;
        notifyItemRangeInserted(0, counter);
    }

    private void setErrorStateOnPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap) {
        logDebug("setErrorStateOnPreview()");
        //Error
        holder.uploadingProgressBarPort.setVisibility(View.GONE);
        holder.uploadingProgressBarLand.setVisibility(View.GONE);

        String name = holder.contentOwnMessageFileName.getText().toString();

        if (bitmap.getWidth() < bitmap.getHeight()) {
            logDebug("Portrait");

            holder.errorUploadingLandscape.setVisibility(View.GONE);
            holder.transparentCoatingLandscape.setVisibility(View.GONE);
            holder.errorUploadingFile.setVisibility(View.GONE);
            holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            if (MimeTypeList.typeForName(name).isVideo()) {
                holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);
            } else {
                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            }

            holder.errorUploadingPortrait.setVisibility(View.VISIBLE);
            holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);
            holder.retryAlert.setVisibility(View.VISIBLE);

            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
        } else {
            logDebug("Landscape");
            holder.errorUploadingPortrait.setVisibility(View.GONE);
            holder.transparentCoatingPortrait.setVisibility(View.GONE);
            holder.errorUploadingFile.setVisibility(View.GONE);

            holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            if (MimeTypeList.typeForName(name).isVideo()) {
                holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);
            } else {
                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            }
            holder.errorUploadingLandscape.setVisibility(View.VISIBLE);
            holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);
            holder.retryAlert.setVisibility(View.VISIBLE);

            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
        }
    }

    private void setOwnPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap, MegaNode node) {
        logDebug("setOwnPreview()");

        if (holder != null) {
            if (bitmap.getWidth() < bitmap.getHeight()) {
                logDebug("Portrait");

                holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                    logDebug("Is pfd preview");
                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                    logDebug("Is video preview");
                    holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                    holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);
                    holder.videoTimecontentOwnMessageThumbPort.setText(timeVideo(node));
                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.VISIBLE);

                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

                } else {
                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);
                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                }

                holder.previewFramePort.setVisibility(View.VISIBLE);
                holder.contentOwnMessageThumbPort.setVisibility(View.VISIBLE);

                holder.forwardOwnFile.setVisibility(View.GONE);
                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                    holder.forwardOwnPortrait.setVisibility(View.GONE);
                }
                else {
                    holder.forwardOwnPortrait.setVisibility(View.VISIBLE);
                }
                holder.forwardOwnLandscape.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.previewFrameLand.setVisibility(View.GONE);
                holder.contentOwnMessageThumbLand.setVisibility(View.GONE);
                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);


            } else {
                logDebug("Landscape");

                holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                    logDebug("Is pfd preview");
                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);

                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                    logDebug("Is video preview");
                    holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                    holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);
                    holder.videoTimecontentOwnMessageThumbLand.setText(timeVideo(node));
                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.VISIBLE);

                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);

                } else {
                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);
                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                }

                holder.previewFrameLand.setVisibility(View.VISIBLE);
                holder.contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);
                holder.contentOwnMessageThumbPort.setVisibility(View.GONE);

                holder.forwardOwnFile.setVisibility(View.GONE);
                holder.forwardOwnPortrait.setVisibility(View.GONE);
                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                    holder.forwardOwnLandscape.setVisibility(View.GONE);
                }
                else {
                    holder.forwardOwnLandscape.setVisibility(View.VISIBLE);
                }

                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
            }
        }
    }

    private void setContactPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap, MegaNode node) {
        logDebug("setContactPreview()");
        if (bitmap.getWidth() < bitmap.getHeight()) {
            logDebug("Portrait");
            holder.contentContactMessageThumbPort.setImageBitmap(bitmap);
            holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                logDebug("Contact message - Is pfd preview");
                holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                logDebug("Contact message - Is video preview");
                holder.gradientContactMessageThumbPort.setVisibility(View.VISIBLE);
                holder.videoIconContactMessageThumbPort.setVisibility(View.VISIBLE);
                holder.videoTimecontentContactMessageThumbPort.setText(timeVideo(node));
                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.VISIBLE);

                holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

            } else {
                holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);
                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
            }

            holder.contentContactMessageThumbPort.setVisibility(View.VISIBLE);

            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactPreviewPortrait.setVisibility(View.VISIBLE);
            }
            if(isMultipleSelect()){
                holder.forwardContactPreviewPortrait.setOnClickListener(null);
            }else{
                holder.forwardContactPreviewPortrait.setOnClickListener(this);
            }
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);

            holder.contentContactMessageThumbLand.setVisibility(View.GONE);
            holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
            holder.contentContactMessageFile.setVisibility(View.GONE);
            holder.contentContactMessageFileThumb.setVisibility(View.GONE);
            holder.contentContactMessageFileName.setVisibility(View.GONE);
            holder.contentContactMessageFileSize.setVisibility(View.GONE);

            RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.getLayoutParams();
            contactThumbParams.setMargins(0, 0, 0, 0);
            holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);

//                                                if(chatRoom.isGroup()){
//                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
//                                                    contactThumbParams.setMargins(0, scaleHeightPx(10, outMetrics) ,0, 0);
//                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
////                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
//                                                }
//                                                else{
//                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
//                                                    contactThumbParams.setMargins(0, 0 ,0, 0);
//                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
////                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
//                                                }

        } else {
            logDebug("Landscape");

            holder.contentContactMessageThumbLand.setImageBitmap(bitmap);
            holder.contentContactMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                logDebug("Contact message - Is pfd preview");
                holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);

                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                logDebug("Contact message - Is video preview");
                holder.gradientContactMessageThumbLand.setVisibility(View.VISIBLE);
                holder.videoIconContactMessageThumbLand.setVisibility(View.VISIBLE);
                holder.videoTimecontentContactMessageThumbLand.setText(timeVideo(node));
                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.VISIBLE);

                holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);

            } else {
                holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);
                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
            }

            holder.contentContactMessageThumbLand.setVisibility(View.VISIBLE);

            if (cC.isInAnonymousMode() || isMultipleSelect()) {
                holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactPreviewLandscape.setVisibility(View.VISIBLE);
            }
            if(isMultipleSelect()){
                holder.forwardContactPreviewLandscape.setOnClickListener(null);
            }else{
                holder.forwardContactPreviewLandscape.setOnClickListener(this);
            }
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactFile.setVisibility(View.GONE);

            holder.contentContactMessageThumbPort.setVisibility(View.GONE);
            holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
            holder.contentContactMessageFile.setVisibility(View.GONE);
            holder.contentContactMessageFileThumb.setVisibility(View.GONE);
            holder.contentContactMessageFileName.setVisibility(View.GONE);
            holder.contentContactMessageFileSize.setVisibility(View.GONE);

            RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.getLayoutParams();
            contactThumbParams.setMargins(0, 0, 0, 0);
            holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);

//                                                if(chatRoom.isGroup()){
//                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
//                                                    contactThumbParams.setMargins(0, scaleHeightPx(10, outMetrics),0, 0);
//                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
////                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
//                                                }
//                                                else{
//                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
//                                                    contactThumbParams.setMargins(0, 0 ,0, 0);
//                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
////                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
//                                                }
        }
    }

    private void setPreview(long handle, MegaChatLollipopAdapter.ViewHolderMessageChat holder, MegaNode node, long msgId) {
        logDebug("handle: " + handle);

        if (holder != null) {
            AndroidMegaChatMessage megaMessage = getMessageAt(holder.getAdapterPosition());

            if(megaMessage == null || megaMessage.getMessage() == null || megaMessage.getMessage().getMsgId() != msgId)
                return;

            File previewDir = getPreviewFolder(context);
            String base64 = MegaApiJava.handleToBase64(handle);
            File preview = new File(previewDir, base64 + ".jpg");
            if (preview.exists()) {

                if (preview.length() > 0) {
                    Bitmap bitmap = getBitmapForCache(preview, context);

                    if (bitmap != null) {

                        previewCache.put(handle, bitmap);

                        if (holder.userHandle == megaChatApi.getMyUserHandle()) {

                            String name = holder.contentOwnMessageFileName.getText().toString();

                            if (bitmap.getWidth() < bitmap.getHeight()) {
                                logDebug("Portrait");

                                holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    logDebug("Is pfd preview");
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    logDebug("Is video preview");

                                    holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);

                                    if (node != null) {
                                        holder.videoTimecontentOwnMessageThumbPort.setText(timeVideo(node));
                                        holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                    } else {
                                        holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                                    }

                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

                                } else {
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);
                                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                                }

                                holder.previewFramePort.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageThumbPort.setVisibility(View.VISIBLE);

                                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                    holder.forwardOwnPortrait.setVisibility(View.GONE);
                                }
                                else {
                                    holder.forwardOwnPortrait.setVisibility(View.VISIBLE);
                                }
                                if(isMultipleSelect()){
                                    holder.forwardOwnPortrait.setOnClickListener(null);
                                }else{
                                    holder.forwardOwnPortrait.setOnClickListener(this);
                                }
                                holder.forwardOwnLandscape.setVisibility(View.GONE);
                                holder.forwardOwnFile.setVisibility(View.GONE);

                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.previewFrameLand.setVisibility(View.GONE);
                                holder.contentOwnMessageThumbLand.setVisibility(View.GONE);

                                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                            } else {
                                logDebug("Landscape");
                                holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    logDebug("Is pfd preview");
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);

                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    logDebug("Is video preview");
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);

                                    if (node != null) {
                                        holder.videoTimecontentOwnMessageThumbLand.setText(timeVideo(node));
                                        holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                    } else {
                                        holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                    }

                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);

                                } else {
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                }

                                holder.previewFrameLand.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.previewFramePort.setVisibility(View.GONE);
                                holder.contentOwnMessageThumbPort.setVisibility(View.GONE);

                                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                    holder.forwardOwnLandscape.setVisibility(View.GONE);
                                }
                                else {
                                    holder.forwardOwnLandscape.setVisibility(View.VISIBLE);
                                }
                                if(isMultipleSelect()){
                                    holder.forwardOwnLandscape.setOnClickListener(null);
                                }else{
                                    holder.forwardOwnLandscape.setOnClickListener(this);
                                }
                                holder.forwardOwnPortrait.setVisibility(View.GONE);
                                holder.forwardOwnFile.setVisibility(View.GONE);

                                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                            }
                        } else {
                            logDebug("Update my contacts preview");
                            String name = holder.contentContactMessageFileName.getText().toString();

                            if (bitmap.getWidth() < bitmap.getHeight()) {

                                logDebug("Portrait");
                                holder.contentContactMessageThumbPort.setImageBitmap(bitmap);
                                holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    logDebug("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    logDebug("Contact message - Is video preview");

                                    holder.gradientContactMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.VISIBLE);

                                    if (node != null) {
                                        holder.videoTimecontentContactMessageThumbPort.setText(timeVideo(node));
                                        holder.videoTimecontentContactMessageThumbPort.setVisibility(View.VISIBLE);
                                    } else {
                                        holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                                    }

                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

                                } else {
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);
                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                                }

                                holder.contentContactMessageThumbPort.setVisibility(View.VISIBLE);
                                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                    holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
                                }
                                else {
                                    holder.forwardContactPreviewPortrait.setVisibility(View.VISIBLE);
                                }
                                if(isMultipleSelect()){
                                    holder.forwardContactPreviewPortrait.setOnClickListener(null);
                                }else{
                                    holder.forwardContactPreviewPortrait.setOnClickListener(this);
                                }
                                holder.contentContactMessageThumbLand.setVisibility(View.GONE);
                                holder.forwardContactPreviewLandscape.setVisibility(View.GONE);

                                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

//                                holder.contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                                holder.contentContactMessageFile.setVisibility(View.GONE);
                                holder.forwardContactFile.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) holder.contentContactMessageThumbPort.getLayoutParams();
                                contactThumbParams.setMargins(0, 0, 0, 0);
                                holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);

//                                if(chatRoom.isGroup()){
//                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbPort.getLayoutParams();
//                                    contactThumbParams.setMargins(0, scaleHeightPx(10, outMetrics) ,0, 0);
//                                    holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
////                                    holder.contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
//                                }
//                                else{
//                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbPort.getLayoutParams();
//                                    contactThumbParams.setMargins(0, 0 ,0, 0);
//                                    holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
////                                    holder.contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
//                                }

                            } else {

                                logDebug("Landscape");
                                holder.contentContactMessageThumbLand.setImageBitmap(bitmap);
                                holder.contentContactMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    logDebug("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);

                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    logDebug("Contact message - Is video preview");
                                    holder.gradientContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.VISIBLE);

                                    if (node != null) {
                                        holder.videoTimecontentContactMessageThumbLand.setText(timeVideo(node));
                                        holder.videoTimecontentContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    } else {
                                        logWarning("Landscape: Node is NULL");
                                        holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
                                    }

                                    holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);

                                } else {
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);
                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
                                }

                                holder.contentContactMessageThumbLand.setVisibility(View.VISIBLE);

                                if (cC.isInAnonymousMode() || isMultipleSelect()) {
                                    holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
                                }
                                else {
                                    holder.forwardContactPreviewLandscape.setVisibility(View.VISIBLE);
                                }
                                if(isMultipleSelect()){
                                    holder.forwardContactPreviewLandscape.setOnClickListener(null);
                                }else{
                                    holder.forwardContactPreviewLandscape.setOnClickListener(this);
                                }
//                                holder.contentContactMessageThumbLandFramework.setVisibility(View.VISIBLE);

                                holder.contentContactMessageThumbPort.setVisibility(View.GONE);
                                holder.forwardContactPreviewPortrait.setVisibility(View.GONE);

                                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                                holder.contentContactMessageThumbPortFramework.setVisibility(View.GONE);
                                holder.contentContactMessageFile.setVisibility(View.GONE);
                                holder.forwardContactFile.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) holder.contentContactMessageThumbLand.getLayoutParams();
                                contactThumbParams.setMargins(0, 0, 0, 0);
                                holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);

//                                if(chatRoom.isGroup()){
//                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbLand.getLayoutParams();
//                                    contactThumbParams.setMargins(0, scaleHeightPx(10, outMetrics),0, 0);
//                                    holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
////                                    holder.contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
//                                }
//                                else{
//                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbLand.getLayoutParams();
//                                    contactThumbParams.setMargins(0, 0 ,0, 0);
//                                    holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
////                                    holder.contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
//
//                                }

                            }
                        }
                    }
                }
            } else {
                logWarning("Preview not exists");
            }
        }
    }

    private void setUploadingPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap) {
        logDebug("holder.filePathUploading: " + holder.filePathUploading);

        if (holder != null) {

            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
            holder.urlOwnMessageLayout.setVisibility(View.GONE);

            holder.forwardOwnRichLinks.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.ownManagementMessageLayout.setVisibility(View.GONE);

            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (bitmap != null) {
                logDebug("Bitmap not null - Update uploading my preview");

                int currentPosition = holder.getLayoutPosition();
                logDebug("currentPosition holder: " + currentPosition);

                if (currentPosition == -1) {
                    logWarning("The position cannot be recovered - had changed");
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        AndroidMegaChatMessage message = messages.get(i);
                        if (message.isUploading()) {
                            String path = message.getPendingMessage().getFilePath();
                            if (path.equals(holder.filePathUploading)) {
                                currentPosition = i + 1;
                                logDebug("Found current position: " + currentPosition);
                                break;
                            }
                        }
                    }
                }
                logDebug("Messages size: " + messages.size());
                if (currentPosition > messages.size()) {
                    logWarning("Position not valid");
                    return;
                }

                AndroidMegaChatMessage message = messages.get(currentPosition - 1);
                if (message.getPendingMessage() != null) {
                    logDebug("State of the message: " + message.getPendingMessage().getState());
                    logDebug("Attachment: " + message.getPendingMessage().getFilePath());

                    if (bitmap.getWidth() < bitmap.getHeight()) {
                        logDebug("Portrait show preview");
                        holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                        holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        holder.previewFramePort.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageThumbPort.setVisibility(View.VISIBLE);

                        holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                        holder.previewFrameLand.setVisibility(View.GONE);
                        holder.contentOwnMessageThumbLand.setVisibility(View.GONE);

                        holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                        holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                        holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                        holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                        holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                        holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                    } else {
                        logDebug("Landscape show preview");
                        holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                        holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        holder.previewFrameLand.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageThumbLand.setVisibility(View.VISIBLE);

                        holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                        holder.previewFramePort.setVisibility(View.GONE);
                        holder.contentOwnMessageThumbPort.setVisibility(View.GONE);
                        holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                        holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                        holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                        holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                        holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                        holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                    }

                    if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                        logWarning("Message is on ERROR state");
                        holder.urlOwnMessageLayout.setVisibility(View.GONE);
                        String name = holder.contentOwnMessageFileName.getText().toString();

                        //Error
                        holder.uploadingProgressBarPort.setVisibility(View.GONE);
                        holder.uploadingProgressBarLand.setVisibility(View.GONE);
                        holder.errorUploadingFile.setVisibility(View.GONE);

                        holder.retryAlert.setVisibility(View.VISIBLE);

                        if (bitmap.getWidth() < bitmap.getHeight()) {
                            logDebug("Portrait");
                            holder.errorUploadingLandscape.setVisibility(View.GONE);
                            holder.transparentCoatingLandscape.setVisibility(View.GONE);
                            holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                            if (MimeTypeList.typeForName(name).isVideo()) {
                                holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);

                            } else {
                                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                            }

                            holder.errorUploadingPortrait.setVisibility(View.VISIBLE);
                            holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);

                        } else {
                            logDebug("Landscape");
                            holder.transparentCoatingPortrait.setVisibility(View.GONE);
                            holder.errorUploadingPortrait.setVisibility(View.GONE);
                            holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            if (MimeTypeList.typeForName(name).isVideo()) {
                                holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);

                            } else {
                                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                            }

                            holder.errorUploadingLandscape.setVisibility(View.VISIBLE);
                            holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);
                        }
                    } else {
                        logDebug("Message is in progress state");
                        holder.urlOwnMessageLayout.setVisibility(View.GONE);

                        //In progress
                        holder.retryAlert.setVisibility(View.GONE);
                        holder.errorUploadingFile.setVisibility(View.GONE);
                        holder.errorUploadingPortrait.setVisibility(View.GONE);
                        holder.errorUploadingLandscape.setVisibility(View.GONE);

                        if (bitmap.getWidth() < bitmap.getHeight()) {
                            logDebug("Portrait");
                            holder.transparentCoatingLandscape.setVisibility(View.GONE);
                            holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarPort.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarLand.setVisibility(View.GONE);
                        } else {
                            logDebug("Landscape");
                            holder.transparentCoatingPortrait.setVisibility(View.GONE);
                            holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarLand.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarPort.setVisibility(View.GONE);
                        }
                    }
                } else {

                    logWarning("The pending message is NULL-- cannot set preview");
                }
            } else {
                logWarning("Bitmap is NULL");
            }
        } else {
            logWarning("Holder is NULL");
        }
    }

    private class PreviewDownloadListener implements MegaRequestListenerInterface {
        Context context;
        MegaChatLollipopAdapter.ViewHolderMessageChat holder;
        MegaChatLollipopAdapter adapter;
        MegaNode node;

        PreviewDownloadListener(Context context, MegaChatLollipopAdapter.ViewHolderMessageChat holder, MegaChatLollipopAdapter adapter, MegaNode node) {
            this.context = context;
            this.holder = holder;
            this.adapter = adapter;
            this.node = node;
        }

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {

        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

            logDebug("onRequestFinish: " + request.getType() + "__" + request.getRequestString());
            logDebug("onRequestFinish: Node: " + request.getNodeHandle());

            if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE) {
                if (e.getErrorCode() == MegaError.API_OK) {

                    long handle = request.getNodeHandle();
                    long msgId = INVALID_HANDLE;
                    if(pendingPreviews.containsKey(handle)){
                        msgId = pendingPreviews.get(handle);
                        pendingPreviews.remove(handle);
                    }
                    setPreview(handle, holder, node, msgId);

                } else {
                    logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }

        @Override
        public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

        }

        @Override
        public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

        }
    }

    public String timeVideo(MegaNode n) {
        logDebug("timeVideo");
        return getVideoDuration(n.getDuration());
    }

    private void checkItem (View v, ViewHolderMessageChat holder, int[] screenPosition, int[] dimens) {

        ImageView imageView = null;

        int position = holder.getCurrentPosition();
        if(position<=messages.size()){
            AndroidMegaChatMessage message = messages.get(position - 1);
            if (message.getMessage() != null) {
                if (message.getMessage().getMegaNodeList() != null) {
                    if (message.getMessage().getMegaNodeList().get(0) != null) {
                        if (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                            if (holder.contentOwnMessageThumbPort.getVisibility() == View.VISIBLE) {
                                logDebug("contentOwnMessageThumbPort");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_own_message_thumb_portrait);
                            }
                            else if (holder.contentOwnMessageThumbLand.getVisibility() == View.VISIBLE) {
                                logDebug("contentOwnMessageThumbLand");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_own_message_thumb_landscape);
                            }
                            else if (holder.contentOwnMessageFileThumb.getVisibility() == View.VISIBLE) {
                                logDebug("contentOwnMessageFileThumb");
                                imageView = (ImageView) v.findViewById(R.id.content_own_message_file_thumb);
                            }
                        }
                        else {
                            if (holder.contentContactMessageThumbPort.getVisibility() == View.VISIBLE) {
                                logDebug("contentContactMessageThumbPort");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_contact_message_thumb_portrait);
                            }
                            else if (holder.contentContactMessageThumbLand.getVisibility() == View.VISIBLE) {
                                logDebug("contentContactMessageThumbLand");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_contact_message_thumb_landscape);
                            }
                            else if (holder.contentContactMessageFileThumb.getVisibility() == View.VISIBLE) {
                                logDebug("contentContactMessageFileThumb");
                                imageView = (ImageView) v.findViewById(R.id.content_contact_message_file_thumb);
                            }
                        }
                    }
                }
            }
        }else{
            logWarning("Messages removed");
        }

        ((ChatActivityLollipop) context).holder_imageDrag = holder;
        ((ChatActivityLollipop) context).position_imageDrag = position;
//        ((ChatActivityLollipop) context).imageDrag = imageView;
        if (imageView != null) {
            imageView.getLocationOnScreen(screenPosition);
            dimens[0] = screenPosition[0] + (imageView.getWidth() / 2);
            dimens[1] = screenPosition[1] + (imageView.getHeight() / 2);
            dimens[2] = imageView.getWidth();
            dimens[3] = imageView.getHeight();
        }
    }

    public void setNodeAttachmentVisibility (boolean visible, ViewHolderMessageChat holder, int position){
        logDebug("position: " + position);
        if (holder != null) {
            holder.contentVisible = visible;
            notifyItemChanged(position);
        }
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick()");

        ViewHolderMessageChat holder = (ViewHolderMessageChat) v.getTag();
        int currentPositionInAdapter = holder.getAdapterPosition();
        if(currentPositionInAdapter<0){
            logWarning("Current position error - not valid value");
            return;
        }

        switch (v.getId()) {
            case R.id.content_own_message_voice_clip_play_pause:
            case R.id.content_contact_message_voice_clip_play_pause:
                if(!(((ChatActivityLollipop)context).isRecordingNow())){
                    playOrPauseVoiceClip(currentPositionInAdapter, holder);
                }
                break;

            case R.id.content_own_message_voice_clip_not_available:
            case R.id.content_contact_message_voice_clip_not_available:{
                ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_message_voice_clip), -1);
                break;
            }

            case R.id.forward_own_rich_links:
            case R.id.forward_own_contact:
            case R.id.forward_own_file:
            case R.id.forward_own_preview_portrait:
            case R.id.forward_own_preview_landscape:
            case R.id.forward_contact_rich_links:
            case R.id.forward_contact_contact:
            case R.id.forward_contact_file:
            case R.id.forward_contact_preview_portrait:
            case R.id.forward_contact_preview_landscape:
            case R.id.forward_own_location:
            case R.id.forward_contact_location:{
                ArrayList<AndroidMegaChatMessage> messageArray = new ArrayList<>();
                int currentPositionInMessages = currentPositionInAdapter -1;
                messageArray.add(messages.get(currentPositionInMessages));
                ((ChatActivityLollipop) context).forwardMessages(messageArray);
                break;
            }
            case R.id.content_own_message_text:
            case R.id.content_contact_message_text:
            case R.id.url_own_message_text:
            case R.id.url_contact_message_text:
                if (isIsClickAlreadyIntercepted()) {
                    resetIsClickAlreadyIntercepted();
                    break;
                }

            case R.id.message_chat_item_layout:{
                int[] screenPosition = new int[2];
                int [] dimens = new int[4];
                checkItem(v, holder, screenPosition, dimens);
                ((ChatActivityLollipop) context).itemClick(currentPositionInAdapter, dimens);
                break;
            }
            case R.id.url_always_allow_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = RICH_WARNING_FALSE;
                megaApi.enableRichPreviews(true);
                this.notifyItemChanged(currentPositionInAdapter);
                break;
            }
            case R.id.url_no_disable_button:
            case R.id.url_not_now_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = RICH_WARNING_FALSE;
                int counter = MegaApplication.getCounterNotNowRichLinkWarning();
                if (counter < 1) {
                    counter = 1;
                } else {
                    counter++;
                }
                megaApi.setRichLinkWarningCounterValue(counter);
                this.notifyItemChanged(currentPositionInAdapter);
                break;
            }
            case R.id.url_never_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = RICH_WARNING_CONFIRMATION;
                this.notifyItemChanged(currentPositionInAdapter);
                break;
            }
            case R.id.url_yes_disable_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = RICH_WARNING_FALSE;
                megaApi.enableRichPreviews(false);
                this.notifyItemChanged(currentPositionInAdapter);
                break;
            }

        }
    }


    /*
     * Set appropriate values ​​when a playback starts
     */
    private void prepareMediaPlayer(final long msgId) {
        if(handlerVoiceNotes == null){
            handlerVoiceNotes = new Handler();
        }
        if(messagesPlaying==null || messagesPlaying.isEmpty()) return;

        for (MessageVoiceClip m : messagesPlaying) {
            if ((m.getIdMessage() == msgId)&&(m.getMediaPlayer().isPlaying())) {
                logDebug("prepareMediaPlayer");

                runnableVC = new Runnable() {
                    @Override
                    public void run() {
                        statePlaying(msgId);
                        handlerVoiceNotes.postDelayed(this, 50);
                    }
                };
                statePlaying(msgId);
                handlerVoiceNotes.postDelayed(runnableVC, 50);
            }
        }
    }

    /*
     * Update the view while a voice clip is playing
     */
    private void statePlaying(long msgId){
        if(messages==null || messages.isEmpty() || messagesPlaying==null || messagesPlaying.isEmpty()) return;

        for(MessageVoiceClip m: messagesPlaying){
            if((m.getIdMessage() == msgId)&&(m.getMediaPlayer().isPlaying())){
                m.setProgress(m.getMediaPlayer().getCurrentPosition());
                for(int i=0; i<messages.size(); i++){
                    if(messages.get(i).getMessage().getMsgId() == m.getIdMessage()){
                        int positionInAdapter = i+1;
                        ViewHolderMessageChat holder = (ViewHolderMessageChat) listFragment.findViewHolderForAdapterPosition(positionInAdapter);
                        if(holder!=null){
                            if(m.getUserHandle() == megaChatApi.getMyUserHandle()){
                                holder.contentOwnMessageVoiceClipPlay.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageVoiceClipPlay.setImageResource(R.drawable.ic_pause_grey);
                                holder.contentOwnMessageVoiceClipSeekBar.setProgress(m.getProgress());
                                holder.contentOwnMessageVoiceClipDuration.setText(milliSecondsToTimer(m.getProgress()));
                            }else{
                                holder.contentContactMessageVoiceClipPlay.setVisibility(View.VISIBLE);
                                holder.contentContactMessageVoiceClipPlay.setImageResource(R.drawable.ic_pause_grey);
                                holder.contentContactMessageVoiceClipSeekBar.setProgress(m.getProgress());
                                holder.contentContactMessageVoiceClipDuration.setText(milliSecondsToTimer(m.getProgress()));
                            }
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    /*
     * Download a voice note using an asynctask
     */
    private void downloadVoiceClip(final ViewHolderMessageChat holder,int position, long userHandle, final MegaNodeList nodeList){
        logDebug("downloadVoiceClip() ");
        try {
            new MegaChatLollipopAdapter.ChatVoiceClipAsyncTask(holder, position, userHandle).execute(nodeList);
        } catch (Exception ex) {
            logError("Too many AsyncTasks", ex);
        }
    }

    /*
     * When the download of a voice clip ends, update the view with the result
     */
    public void finishedVoiceClipDownload(long nodeHandle, int resultTransfer){
        if(messages==null || messages.isEmpty() || messagesPlaying==null || messagesPlaying.isEmpty()) return;
        logDebug("nodeHandle = "+nodeHandle+", the result of transfer is "+resultTransfer);

        for(MessageVoiceClip messagevc : messagesPlaying){
            if(messagevc.getMessageHandle() == nodeHandle){
                messagevc.setIsAvailable(resultTransfer);
                for(int posArray=0; posArray<messages.size();posArray++){
                    if(messages.get(posArray).getMessage().getMsgId() == messagevc.getIdMessage()){
                        int positionInAdapter = posArray+1;
                        ViewHolderMessageChat holder = (ViewHolderMessageChat) listFragment.findViewHolderForAdapterPosition(positionInAdapter);
                        if(holder!=null) notifyItemChanged(positionInAdapter);
                        break;
                    }
                }
            }
        }
    }

    /*
      * Updating the seekBar element and the mediaplayer progress
     */
    private void updatingSeekBar(long messageId, int progress){
        if(messages==null || messages.isEmpty() || messagesPlaying==null || messagesPlaying.isEmpty()) return;

        for(MessageVoiceClip m: messagesPlaying){
            if(m.getIdMessage() == messageId) {
                logDebug("Update mediaplayer");
                m.setProgress(progress);
                m.getMediaPlayer().seekTo(m.getProgress());
                for(int i=0; i<messages.size(); i++){
                    if(messages.get(i).getMessage().getMsgId() == m.getIdMessage()){
                        int positionInAdapter = i+1;
                        ViewHolderMessageChat holder = (ViewHolderMessageChat) listFragment.findViewHolderForAdapterPosition(positionInAdapter);
                        if(holder!=null){
                            logDebug("Update holder views");
                            if(m.getUserHandle() == megaChatApi.getMyUserHandle()){
                                holder.contentOwnMessageVoiceClipDuration.setText(milliSecondsToTimer(m.getProgress()));
                            }else{
                                holder.contentContactMessageVoiceClipDuration.setText(milliSecondsToTimer(m.getProgress()));
                            }
                        }
                        break;
                    }
                }
                break;
            }
        }
    }


    /*
     * Play or pause a voice clip
     */
    private void playOrPauseVoiceClip(int positionInAdapter, ViewHolderMessageChat holder){
        AndroidMegaChatMessage currentMessage = getMessageAtPosition(positionInAdapter-1);
        if (currentMessage == null || currentMessage.getMessage() == null ||
                currentMessage.getMessage().getType() != MegaChatMessage.TYPE_VOICE_CLIP ||
                messagesPlaying == null || messagesPlaying.isEmpty())
            return;

        for(MessageVoiceClip m: messagesPlaying){
            if(m.getIdMessage() == currentMessage.getMessage().getMsgId()){
                if(m.getMediaPlayer().isPlaying()){
                    logDebug("isPlaying: PLAY -> PAUSE");
                    pauseVoiceClip(m,positionInAdapter);
                    return;
                }
                if(m.isPaused()){
                    logDebug("notPlaying: PAUSE -> PLAY");
                    playVoiceClip(m, null);
                    return;
                }

                logDebug("notPlaying: find voice clip");
                MegaNodeList nodeList = currentMessage.getMessage().getMegaNodeList();
                if(nodeList.size()<1 || !isVoiceClip(nodeList.get(0).getName())) break;

                File vcFile = buildVoiceClipFile(context, nodeList.get(0).getName());
                if(!isFileAvailable(vcFile) || vcFile.length() != nodeList.get(0).getSize()) downloadVoiceClip(holder, positionInAdapter, currentMessage.getMessage().getUserHandle(), nodeList);

                playVoiceClip(m, vcFile.getAbsolutePath());
                break;
            }
        }
    }


    /*
     * Pause the voice clip
     */
    private void pauseVoiceClip(MessageVoiceClip m, int positionInAdapter){
        m.getMediaPlayer().pause();
        m.setProgress(m.getMediaPlayer().getCurrentPosition());
        m.setPaused(true);
        removeCallBacks();
        notifyItemChanged(positionInAdapter);
    }

    /*
     * Play the voice clip
     */
    private void playVoiceClip(MessageVoiceClip m, String voiceClipPath){

        stopAllReproductionsInProgress();
        final long mId = m.getIdMessage();
        ((ChatActivityLollipop) context).startProximitySensor();

        if(voiceClipPath == null){
            m.getMediaPlayer().seekTo(m.getProgress());
            m.getMediaPlayer().start();
            m.setPaused(false);
            prepareMediaPlayer(mId);
        }else{
            try {
                m.getMediaPlayer().reset();
                m.getMediaPlayer().setDataSource(voiceClipPath);
                m.getMediaPlayer().setLooping(false);
                m.getMediaPlayer().prepare();
                m.setPaused(false);
                m.setProgress(m.getMediaPlayer().getCurrentPosition());

            } catch (IOException e) {
                e.printStackTrace();
            }
            m.getMediaPlayer().seekTo(m.getProgress());
        }

        m.getMediaPlayer().setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                logDebug("mediaPlayerVoiceNotes:onError");
                ((ChatActivityLollipop) context).stopProximitySensor();
                mediaPlayer.reset();
                return true;
            }
        });
        m.getMediaPlayer().setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                logDebug("mediaPlayerVoiceNotes:onPrepared");
                mediaPlayer.start();
                prepareMediaPlayer(mId);

            }
        });

        m.getMediaPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                logDebug("mediaPlayerVoiceNotes:setOnCompletionListener");
                removeCallBacks();
                completionMediaPlayer(mId);
            }
        });
    }

    /*
    * Stop the playing when this message is removed*/
    public void stopPlaying(long msgId){
        ((ChatActivityLollipop) context).stopProximitySensor();

        if(messagesPlaying==null || messagesPlaying.isEmpty()) return;

        for(MessageVoiceClip m: messagesPlaying) {
            if(m.getIdMessage() == msgId){
                if(m.getMediaPlayer().isPlaying()){
                    m.getMediaPlayer().stop();
                }
                m.getMediaPlayer().release();
                m.setMediaPlayer(null);
                messagesPlaying.remove(m);
                break;
            }
        }
    }

    public void pausePlaybackInProgress(){
        if (messagesPlaying == null || messagesPlaying.isEmpty()) return;
        for (MessageVoiceClip m : messagesPlaying) {
            if (m.getMediaPlayer().isPlaying()) {
                int positionInAdapter = getAdapterItemPosition(m.getIdMessage());
                if (positionInAdapter != -1) pauseVoiceClip(m, positionInAdapter);
                break;
            }
        }
    }

    /*
     * Restore values when a playback ends
     */
    private void completionMediaPlayer(long msgId){
        if(messages==null || messages.isEmpty() || messagesPlaying==null || messagesPlaying.isEmpty()) return;

        for(MessageVoiceClip m: messagesPlaying) {
            if(m.getIdMessage() == msgId){
                logDebug("completionMediaPlayer ");
                m.setProgress(0);
                m.setPaused(false);
                if(m.getMediaPlayer().isPlaying()){
                    m.getMediaPlayer().stop();
                }
                m.getMediaPlayer().seekTo(m.getProgress());
                for(int i=0; i<messages.size(); i++){
                    if(messages.get(i).getMessage().getMsgId() == msgId){
                        int positionInAdapter = i+1;
                        notifyItemChanged(positionInAdapter);
                        break;
                    }
                }
                break;
            }
        }
    }

    /* Get the voice clip that it is playing*/

    public MessageVoiceClip getVoiceClipPlaying(){
        if(messagesPlaying==null || messagesPlaying.isEmpty()) return null;
        for(MessageVoiceClip m:messagesPlaying){
            if(m.getMediaPlayer().isPlaying()){
                return m;
            }
        }
        return null;
    }

    /*
     * Stop the voice notes that are playing and update the necessary views
     */
    public void stopAllReproductionsInProgress(){

        if (messagesPlaying == null || messagesPlaying.isEmpty()) return;
        removeCallBacks();

        for(MessageVoiceClip m:messagesPlaying){
            if(m.getMediaPlayer().isPlaying()){
                logDebug("PLAY -> STOP");
                completionMediaPlayer(m.getIdMessage());
            }
        }
    }

    public void destroyVoiceElemnts(){
        logDebug("destroyVoiceElemnts()");
        removeCallBacks();
        if(messagesPlaying==null || messagesPlaying.isEmpty()) return;
        for(MessageVoiceClip m:messagesPlaying){
            m.getMediaPlayer().release();
            m.setMediaPlayer(null);
        }
        messagesPlaying.clear();
    }

    @Override
    public boolean onLongClick(View view) {
        logDebug("OnLongCLick");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }
        ViewHolderMessageChat holder = (ViewHolderMessageChat) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        if (isMultipleSelect() || currentPosition < 1 || messages.get(currentPosition-1).isUploading())
            return true;

        if(MegaApplication.isShowInfoChatMessages()){
            ((ChatActivityLollipop) context).showMessageInfo(currentPosition);
        }
        else{
            ((ChatActivityLollipop) context).itemLongClick(currentPosition);
        }

        return true;
    }

    private void removeCallBacks(){
        logDebug("removeCallBacks()");

        if(handlerVoiceNotes==null) return;

        if (runnableVC != null) {
            handlerVoiceNotes.removeCallbacks(runnableVC);
        }
        ((ChatActivityLollipop) context).stopProximitySensor();

        runnableVC = null;
        handlerVoiceNotes.removeCallbacksAndMessages(null);
        handlerVoiceNotes = null;
    }

    private void setContactMessageName(int pos, ViewHolderMessageChat holder, long handle, boolean visibility) {
        if (isHolderNull(pos, holder)) {
            return;
        }
        holder.fullNameTitle = getContactMessageName(pos, holder, handle);
        if (!visibility) return;

        if (chatRoom.isGroup()) {
            holder.nameContactText.setVisibility(View.VISIBLE);
            holder.nameContactText.setText(holder.fullNameTitle);
        }
        else {
            holder.nameContactText.setVisibility(View.GONE);
        }
    }

    private void isRemovingTextMessage(int pos, ViewHolderMessageChat holder, MegaChatMessage message) {
        if (isHolderNull(pos, holder)) {
            return;
        }
        boolean isRemoved = false;
        if (removedMessages != null && removedMessages.size() > 0) {
            for (RemovedMessage removeMsg : removedMessages) {
                if (removeMsg.getMsgId() == message.getMsgId() && removeMsg.getMsgTempId() == message.getTempId()) {
                    isRemoved = true;
                    break;
                }
            }
        }

        if (isRemoved) {
            holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
        } else {
            holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
        }
    }

    private String getContactMessageName(int pos, ViewHolderMessageChat holder, long handle) {
        if (isHolderNull(pos, holder)) {
            return null;
        }

        String name = cC.getParticipantFullName(handle);
        if (!isTextEmpty(name)) {
            return name;
        }

        logWarning("NOT found in DB");
        name = context.getString(R.string.unknown_name_label);
        if (!holder.nameRequestedAction) {
            logDebug("Call for nonContactName: " + handle);
            holder.nameRequestedAction = true;
            int privilege = chatRoom.getPeerPrivilegeByHandle(handle);
            if (privilege == MegaChatRoom.PRIV_UNKNOWN || privilege == MegaChatRoom.PRIV_RM) {
                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, handle, chatRoom.isPreview(), pos);
                megaChatApi.getUserFirstname(handle, chatRoom.getAuthorizationToken(), listener);
                megaChatApi.getUserLastname(handle, chatRoom.getAuthorizationToken(), listener);
                megaChatApi.getUserEmail(handle, listener);
            } else {
                MegaHandleList handleList = MegaHandleList.createInstance();
                handleList.addMegaHandle(handle);
                megaChatApi.loadUserAttributes(chatRoom.getChatId(), handleList,
                        new GetPeerAttributesListener(context, holder, this));
            }
        } else {
            logWarning("Name already asked and no name received: " + handle);
        }
        return name;
    }

    boolean isHolderNull(int pos, ViewHolderMessageChat holder) {
        if (holder ==  null) {
            holder = (ViewHolderMessageChat) listFragment.findViewHolderForAdapterPosition(pos);
            if (holder == null) {
                notifyItemChanged(pos);
                return true;
            }
        }

        return false;
    }

    public ViewHolderMessageChat queryIfHolderNull(int pos) {
        ViewHolderMessageChat holder = (ViewHolderMessageChat) listFragment.findViewHolderForAdapterPosition(pos);
        if (holder == null) {
            return null;
        }

        return holder;
    }

    private void setInfoToShow (int position, final ViewHolderMessageChat holder, boolean ownMessage, int infotToShow, String dateText, String timeText) {

        if (isHolderNull(position, holder)) {
            return;
        }

        if (ownMessage) {
            switch (infotToShow) {
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                    logDebug("CHAT_ADAPTER_SHOW_ALL");
                    holder.dateLayout.setVisibility(View.VISIBLE);
                    holder.dateText.setText(dateText);
                    holder.titleOwnMessage.setVisibility(View.VISIBLE);
                    holder.timeOwnText.setText(timeText);
                    break;
                }
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                    logDebug("CHAT_ADAPTER_SHOW_TIME");
                    holder.dateLayout.setVisibility(View.GONE);
                    holder.titleOwnMessage.setVisibility(View.VISIBLE);
                    holder.timeOwnText.setText(timeText);
                    break;
                }
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                    logDebug("CHAT_ADAPTER_SHOW_NOTHING");
                    holder.dateLayout.setVisibility(View.GONE);
                    holder.titleOwnMessage.setVisibility(View.GONE);
                    break;
                }
            }
        } else {
            switch (infotToShow) {
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                    logDebug("CHAT_ADAPTER_SHOW_ALL");
                    holder.dateLayout.setVisibility(View.VISIBLE);
                    holder.dateText.setText(dateText);
                    holder.titleContactMessage.setVisibility(View.VISIBLE);
                    holder.timeContactText.setText(timeText);
                    holder.timeContactText.setVisibility(View.VISIBLE);
                    break;
                }
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                    logDebug("CHAT_ADAPTER_SHOW_TIME");
                    holder.dateLayout.setVisibility(View.GONE);
                    holder.titleContactMessage.setVisibility(View.VISIBLE);
                    holder.timeContactText.setText(timeText);
                    holder.timeContactText.setVisibility(View.VISIBLE);
                    break;
                }
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                    logDebug("CHAT_ADAPTER_SHOW_NOTHING");
                    holder.dateLayout.setVisibility(View.GONE);
                    holder.timeContactText.setVisibility(View.GONE);
                    holder.titleContactMessage.setVisibility(View.GONE);
                    break;
                }
            }
        }
    }

    private void checkMultiselectionMode(int positionInAdapter, final ViewHolderMessageChat holder, boolean ownMessage, long messageId) {
        if (isHolderNull(positionInAdapter, holder))
            return;

        if (multipleSelect) {
            if (ownMessage) {
                holder.ownMessageSelectLayout.setVisibility(View.VISIBLE);
                if (this.isItemChecked(messageId)) {
                    holder.ownMessageSelectIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.media_select_ic));
                } else {
                    holder.ownMessageSelectIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_unselect_chatroom));
                }
            } else {
                holder.contactMessageSelectLayout.setVisibility(View.VISIBLE);
                if (this.isItemChecked(messageId)) {
                    holder.contactMessageSelectIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.media_select_ic));
                } else {
                    holder.contactMessageSelectIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_unselect_chatroom));
                }
            }

        } else if (positionClicked != INVALID_POSITION && positionClicked == positionInAdapter) {
            listFragment.smoothScrollToPosition(positionClicked);
        }
    }

    private int getAdapterItemPosition(long id) {
        for (int position = 0; position < messages.size(); position++) {
            if (messages.get(position).getMessage().getMsgId() == id) {
                return position + 1;
            }
        }
        return -1;
    }

    public MegaChatRoom getChatRoom() {
        return chatRoom;
    }
}
