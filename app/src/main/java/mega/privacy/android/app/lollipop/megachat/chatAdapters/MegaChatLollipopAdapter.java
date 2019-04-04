package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
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
import android.widget.TextView;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatAttachmentAvatarListener;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.PendingMessageSingle;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.RTFFormatter;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUtilsAndroid;

import static mega.privacy.android.app.utils.Util.toCDATA;

//import com.vdurmont.emoji.EmojiManager;
//import com.vdurmont.emoji.EmojiParser;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    public static int MAX_WIDTH_NAME_SENDER_GROUP_THUMB_LAND_PICTURE = 194;
    public static int MAX_WIDTH_NAME_SENDER_GROUP_THUMB_PORTRAIT_PICTURE = 136;

    public static int MAX_WIDTH_FILENAME_LAND = 455;
    public static int MAX_WIDTH_FILENAME_PORT = 180;

    //margins of management message and hour
    public static int MANAGEMENT_MESSAGE_LAND = 28;
    public static int MANAGEMENT_MESSAGE_PORT = 48;

    //margins of management message and hour in a CALL
    public static int MANAGEMENT_MESSAGE_CALL_LAND = 40;
    public static int MANAGEMENT_MESSAGE_CALL_PORT = 60;

    //paddings of hours (right and left)
    public static int PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND = 9;
    public static int PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT = 16;

    //margins of normal/attachment contacts messages and hour
    public static int CONTACT_MESSAGE_LAND = 28;
    public static int CONTACT_MESSAGE_PORT = 48;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    Context context;
    int positionClicked;
    ArrayList<AndroidMegaChatMessage> messages;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    boolean multipleSelect;
    private SparseBooleanArray selectedItems;

    private MegaChatLollipopAdapter megaChatAdapter;

    ChatController cC;

    long myUserHandle = -1;

    DisplayMetrics outMetrics;
    DatabaseHandler dbH = null;
    MegaChatRoom chatRoom;

    private ArrayList<Long> pendingPreviews = new ArrayList<Long>();

    private class ChatPreviewAsyncTask extends AsyncTask<MegaNode, Void, Integer> {

        MegaChatLollipopAdapter.ViewHolderMessageChat holder;
        MegaNode node;
        Bitmap preview;

        public ChatPreviewAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder) {
            this.holder = holder;
        }

        @Override
        protected Integer doInBackground(MegaNode... params) {
            log("ChatPreviewAsyncTask-doInBackground");
            node = params[0];
            preview = PreviewUtils.getPreviewFromFolder(node, context);

            if (preview != null) {
                PreviewUtils.previewCache.put(node.getHandle(), preview);
                return 0;
            } else {
                if (pendingPreviews.contains(node.getHandle())) {
                    log("the preview is already downloaded or added to the list");
                    return 1;
                } else {
                    return 2;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer param) {
            log("ChatPreviewAsyncTask-onPostExecute");

            if (param == 0) {
                log("Preview recovered from folder");
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
                                    log("The nodeHandles are not equal!");
                                }
                            }
                        }
                    }
                }else{
                    log("messages removed");
                }


            } else if (param == 2) {
                File previewFile = new File(PreviewUtils.getPreviewFolder(context), node.getBase64Handle() + ".jpg");
                log("GET PREVIEW OF HANDLE: " + node.getHandle() + " to download here: " + previewFile.getAbsolutePath());
                pendingPreviews.add(node.getHandle());
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
        MegaChatLollipopAdapter.ViewHolderMessageChat holder;

        public ChatLocalPreviewAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder) {
            this.holder = holder;
        }

        @Override
        protected Integer doInBackground(MegaNode... params) {
            log("ChatLocalPreviewAsyncTask-doInBackground");

            node = params[0];

            if (node == null) {
                return 3;
            }
            preview = PreviewUtils.getPreviewFromFolder(node, context);

            if (preview != null) {
                PreviewUtils.previewCache.put(node.getHandle(), preview);
                return 0;
            } else {
                if (context.getExternalCacheDir() != null) {
                    cacheDir = context.getExternalCacheDir();
                } else {
                    cacheDir = context.getCacheDir();
                }
                destination = new File(cacheDir, node.getName());

                if (destination.exists()) {
                    if (destination.length() == node.getSize()) {
                        File previewDir = PreviewUtils.getPreviewFolder(context);
                        File previewFile = new File(previewDir, node.getBase64Handle() + ".jpg");
                        log("BASE64: " + node.getBase64Handle() + "name: " + node.getName());
                        boolean previewCreated = MegaUtilsAndroid.createPreview(destination, previewFile);

                        if (previewCreated) {
                            preview = PreviewUtils.getBitmapForCache(previewFile, context);
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

                if (pendingPreviews.contains(node.getHandle())) {
                    log("the image is already downloaded or added to the list");
                    return 1;
                } else {
                    return 2;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer param) {
            log("ChatLocalPreviewAsyncTask-onPostExecute");

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
                    log("The nodeHandles are not equal!");
                }
            } else if (param == 2) {
                log("No preview and no generated correctly");
            }
        }
    }

    private class ChatUploadingPreviewAsyncTask extends AsyncTask<String, Void, Bitmap> {

        MegaChatLollipopAdapter.ViewHolderMessageChat holder;
        String filePath;

        public ChatUploadingPreviewAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder) {
            this.holder = holder;
        }

        @Override
        protected Bitmap doInBackground(String... params) {

            filePath = params[0];
            File currentFile = new File(filePath);
            if (MimeTypeList.typeForName(filePath).isImage()) {
                log("Is image");

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                Bitmap preview;

                ExifInterface exif;
                int orientation = ExifInterface.ORIENTATION_NORMAL;
                try {
                    exif = new ExifInterface(currentFile.getAbsolutePath());
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {
                }

                // Calculate inSampleSize
                options.inSampleSize = Util.calculateInSampleSize(options, 1000, 1000);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;

                preview = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
                if (preview != null) {
                    preview = Util.rotateBitmap(preview, orientation);

                    long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(filePath));
                    PreviewUtils.setPreviewCache(fingerprintCache, preview);
                    return preview;
                }
            } else if (MimeTypeList.typeForName(filePath).isPdf()) {
                log("Is pdf");

                FileOutputStream out = null;
                int pageNumber = 0;
                try {

                    PdfiumCore pdfiumCore = new PdfiumCore(context);
                    File previewDir = PreviewUtils.getPreviewFolder(context);
                    File previewFile = new File(previewDir, currentFile.getName() + ".jpg");

                    PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(currentFile, ParcelFileDescriptor.MODE_READ_ONLY));
                    pdfiumCore.openPage(pdfDocument, pageNumber);
                    int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
                    int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
                    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
                    Bitmap preview = PreviewUtils.resizeBitmapUpload(bmp, width, height);
                    out = new FileOutputStream(previewFile);
                    boolean result = preview.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                    pdfiumCore.closeDocument(pdfDocument);

                    if (preview != null && result) {
                        log("Compress OK");
                        long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(previewFile.getPath()));
                        PreviewUtils.setPreviewCache(fingerprintCache, preview);
                        return preview;
                    } else if (!result) {
                        log("Not Compress");
                    }
                } catch (Exception e) {
                    log("Pdf thumbnail could not be created");
                } finally {
                    try {
                        if (out != null)
                            out.close();
                    } catch (Exception e) {
                    }
                }
            } else if (MimeTypeList.typeForName(filePath).isVideo()) {
                log("Is video");
                File previewDir = PreviewUtils.getPreviewFolder(context);
                File previewFile = new File(previewDir, currentFile.getName() + ".jpg");

                Bitmap bmPreview = PreviewUtils.createVideoPreview(filePath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                if (bmPreview == null) {
                    log("Create video preview NULL");
//                    bmPreview= ThumbnailUtilsLollipop.loadVideoThumbnail(filePath, context);
                } else {
                    log("Create Video preview worked!");
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
                                log("Compress OK");
                                long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(previewFile.getPath()));
                                PreviewUtils.setPreviewCache(fingerprintCache, bmPreview);
                                return bmPreview;
                            }
                        } catch (Exception e) {
                            log("Error with FileOutputStream: " + e.getMessage());
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                log("Error: " + e.getMessage());
                            }
                        }

                    } catch (IOException e1) {
                        log("Error creating new preview file: " + e1.getMessage());
                    }
                } else {
                    log("Create video preview NULL");
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap preview) {
            log("ChatUploadingPreviewAsyncTask-onPostExecute");
            if (preview != null) {
                if (holder.filePathUploading.equals(filePath)) {
                    setUploadingPreview(holder, preview);
                } else {
                    log("The filePaths are not equal!");
                }
            } else {
                log("The preview is NULL!");
            }
        }
    }

    public MegaChatLollipopAdapter(Context _context, MegaChatRoom chatRoom, ArrayList<AndroidMegaChatMessage> _messages, RecyclerView _listView) {
        log("new adapter");
        this.context = _context;
        this.messages = _messages;
        this.positionClicked = -1;
        this.chatRoom = chatRoom;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        listFragment = _listView;

        megaChatAdapter = this;

        if (messages != null) {
            log("Number of messages: " + messages.size());
        } else {
            log("Number of messages: NULL");
        }

        myUserHandle = megaChatApi.getMyUserHandle();
        log("MegaChatLollipopAdapter: MyUserHandle: " + myUserHandle);
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
        EmojiTextView contentOwnMessageText;

        //Own rich links
        RelativeLayout urlOwnMessageLayout;
        RelativeLayout forwardOwnRichLinks;

        EmojiTextView urlOwnMessageText;
        LinearLayout urlOwnMessageWarningButtonsLayout;
        Button neverRichLinkButton;
        Button alwaysAllowRichLinkButton;
        Button notNowRichLinkButton;
        TextView urlOwnMessageTitle;
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
        EmojiTextView urlContactMessageText;
        TextView urlContactMessageTitle;
        TextView urlContactMessageDescription;
        RelativeLayout forwardContactRichLinks;

        LinearLayout urlContactMessageIconAndLinkLayout;
        ImageView urlContactMessageIcon;
        TextView urlContactMessageLink;

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
        RelativeLayout ownTriangleIconFile;
        RelativeLayout ownTriangleIconContact;

        RelativeLayout contentOwnMessageContactLayout;
        RelativeLayout contentOwnMessageContactLayoutAvatar;
        RoundedImageView contentOwnMessageContactThumb;
        TextView contentOwnMessageContactName;
        public TextView contentOwnMessageContactEmail;
        TextView contentOwnMessageContactInitialLetter;
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

        RelativeLayout contactMessageLayout;
        RelativeLayout titleContactMessage;

        TextView timeContactText;
        TextView nameContactText;

        RoundedImageView contactImageView;
        TextView contactInitialLetter;

        RelativeLayout contentContactMessageLayout;
        EmojiTextView contentContactMessageText;

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
        TextView contentContactMessageContactName;
        public TextView contentContactMessageContactEmail;
        TextView contentContactMessageContactInitialLetter;

        ImageView iconContactTypeDocLandPreview;
        ImageView iconContactTypeDocPortraitPreview;

        RelativeLayout ownManagementMessageLayout;
        EmojiTextView ownManagementMessageText;
        ImageView ownManagementMessageIcon;

        EmojiTextView contactManagementMessageText;
        ImageView contactManagementMessageIcon;
        RelativeLayout contactManagementMessageLayout;

        //Management messages with avatar
        RelativeLayout contactManagementAvatarMainLayout;
        RoundedImageView contactManagementAvatarImage;
        TextView contactManagementAvatarInitialLetter;
        TextView contactManagementAvatarMessageText;
        RelativeLayout contactManagementAvatarSubTextLayout;

        public String filePathUploading;

        public long getUserHandle() {
            return userHandle;
        }

        public int getCurrentPosition() {
            return currentPosition;
        }

        public void setMyImageView(Bitmap bitmap) {
            contentOwnMessageContactThumb.setImageBitmap(bitmap);
            contentOwnMessageContactInitialLetter.setVisibility(View.GONE);
        }

        public void setContactImageView(Bitmap bitmap) {
            contentContactMessageContactThumb.setImageBitmap(bitmap);
            contentContactMessageContactInitialLetter.setVisibility(View.GONE);
        }

        public void setContactAvatar(Bitmap bitmap) {
            contactImageView.setImageBitmap(bitmap);
            contactInitialLetter.setVisibility(View.GONE);
        }
    }

    public static class ViewHolderHeaderChat extends RecyclerView.ViewHolder {
        public ViewHolderHeaderChat(View view) {
            super(view);
        }
    }

    ViewHolderMessageChat holder;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");

        if (viewType == TYPE_HEADER) {
            log("Create header");
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_item_chat, parent, false);
            return new ViewHolderHeaderChat(v);
        } else {
            log("Create item message");
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            outMetrics = new DisplayMetrics();
            display.getMetrics(outMetrics);
            float density = context.getResources().getDisplayMetrics().density;

            dbH = DatabaseHandler.getDbHandler(context);

            cC = new ChatController(context);

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_chat, parent, false);
            holder = new ViewHolderMessageChat(v);
            holder.contentVisible = true;
            holder.itemLayout = (RelativeLayout) v.findViewById(R.id.message_chat_item_layout);
            holder.dateLayout = (RelativeLayout) v.findViewById(R.id.message_chat_date_layout);
            //Margins
            RelativeLayout.LayoutParams dateLayoutParams = (RelativeLayout.LayoutParams) holder.dateLayout.getLayoutParams();
            dateLayoutParams.setMargins(0, Util.scaleHeightPx(8, outMetrics), 0, Util.scaleHeightPx(8, outMetrics));
            holder.dateLayout.setLayoutParams(dateLayoutParams);

            holder.dateText = (TextView) v.findViewById(R.id.message_chat_date_text);

            holder.newMessagesLayout = (LinearLayout) v.findViewById(R.id.message_chat_new_relative_layout);
            holder.newMessagesText = (TextView) v.findViewById(R.id.message_chat_new_text);

            if(((ChatActivityLollipop) context).getDeviceDensity() == 1){

                MANAGEMENT_MESSAGE_CALL_LAND= 45;
                MANAGEMENT_MESSAGE_CALL_PORT= 65;

                CONTACT_MESSAGE_LAND = 31;
                CONTACT_MESSAGE_PORT = 55;

                PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND = 10;
                PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT = 18;
            }

            //Own messages
            holder.ownMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_own_message_layout);
            holder.titleOwnMessage = (RelativeLayout) v.findViewById(R.id.title_own_message_layout);
            holder.timeOwnText = (TextView) v.findViewById(R.id.message_chat_time_text);

            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

//        holder.meText = (TextView) v.findViewById(R.id.message_chat_me_text);

            holder.previewFramePort = (RelativeLayout) v.findViewById(R.id.preview_frame_portrait);
            holder.previewFrameLand = (RelativeLayout) v.findViewById(R.id.preview_frame_landscape);

            holder.contentOwnMessageLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_layout);
            holder.contentOwnMessageText = (EmojiTextView) v.findViewById(R.id.content_own_message_text);

            //Own rich links message
            holder.urlOwnMessageLayout = (RelativeLayout) v.findViewById(R.id.url_own_message_layout);
            holder.urlOwnMessageLayout.setVisibility(View.GONE);

            if(((ChatActivityLollipop) context).getDeviceDensity() == 1){
                ViewGroup.LayoutParams params=holder.urlOwnMessageLayout.getLayoutParams();
                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    params.width=450;
                }else{
                    params.width=330;
                }
                holder.urlOwnMessageLayout.setLayoutParams(params);
            }
            holder.forwardOwnRichLinks = (RelativeLayout) v.findViewById(R.id.forward_own_rich_links);
            holder.forwardOwnRichLinks.setTag(holder);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);

            holder.urlOwnMessageText = (EmojiTextView) v.findViewById(R.id.url_own_message_text);
            holder.urlOwnMessageText.setTag(holder);

            holder.urlOwnMessageWarningButtonsLayout = (LinearLayout) v.findViewById(R.id.url_own_message_buttons_warning_layout);
            holder.neverRichLinkButton = (Button) v.findViewById(R.id.url_never_button);
            holder.alwaysAllowRichLinkButton = (Button) v.findViewById(R.id.url_always_allow_button);
            holder.notNowRichLinkButton = (Button) v.findViewById(R.id.url_not_now_button);

            holder.urlOwnMessageDisableButtonsLayout = (LinearLayout) v.findViewById(R.id.url_own_message_buttons_disable_layout);
            holder.yesDisableButton = (Button) v.findViewById(R.id.url_yes_disable_button);
            holder.noDisableButton = (Button) v.findViewById(R.id.url_no_disable_button);

            holder.urlOwnMessageTitle = (TextView) v.findViewById(R.id.url_own_message_title);
            holder.urlOwnMessageDescription = (TextView) v.findViewById(R.id.url_own_message_description);

            holder.urlOwnMessageIconAndLinkLayout = (LinearLayout) v.findViewById(R.id.url_own_message_icon_link_layout);
            holder.urlOwnMessageIcon = (ImageView) v.findViewById(R.id.url_own_message_icon);
            holder.urlOwnMessageLink = (TextView) v.findViewById(R.id.url_own_message_link);

            holder.urlOwnMessageImage = (RoundedImageView) v.findViewById(R.id.url_own_message_image);
            int radiusImageRL = Util.scaleWidthPx(10, outMetrics);
            holder.urlOwnMessageImage.setCornerRadius(radiusImageRL);
            holder.urlOwnMessageImage.setBorderWidth(0);
            holder.urlOwnMessageImage.setOval(false);

            //Group avatar of chat links
            holder.urlOwnMessageGroupAvatarLayout = (RelativeLayout) v.findViewById(R.id.url_chat_own_message_image);
            holder.urlOwnMessageGroupAvatar = (RoundedImageView) v.findViewById(R.id.content_url_chat_own_message_contact_thumb);
            holder.urlOwnMessageGroupAvatarText = (TextView) v.findViewById(R.id.content_url_chat_own_message_contact_initial_letter);

            //Group avatar of chat links
            holder.urlContactMessageGroupAvatarLayout = (RelativeLayout) v.findViewById(R.id.url_chat_contact_message_image);
            holder.urlContactMessageGroupAvatar = (RoundedImageView) v.findViewById(R.id.content_url_chat_contact_message_contact_thumb);
            holder.urlContactMessageGroupAvatarText = (TextView) v.findViewById(R.id.content_url_chat_contact_message_contact_initial_letter);

            int radius;
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                radius = Util.scaleWidthPx(10, outMetrics);
            }else{
                radius = Util.scaleWidthPx(15, outMetrics);
            }
            int colors[] = { 0x70000000, 0x00000000};
            GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, colors);;
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadii(new float[]{radius, 0,radius,0,radius,radius,radius,radius});

            holder.contentOwnMessageThumbLand = (RoundedImageView) v.findViewById(R.id.content_own_message_thumb_landscape);
            holder.contentOwnMessageThumbLand.setCornerRadius(radius);
            holder.contentOwnMessageThumbLand.setBorderWidth(1);
            holder.contentOwnMessageThumbLand.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentOwnMessageThumbLand.setOval(false);

            holder.gradientOwnMessageThumbLand = (RelativeLayout) v.findViewById(R.id.gradient_own_message_thumb_landscape);
            holder.gradientOwnMessageThumbLand.setBackground(shape);

            holder.videoIconOwnMessageThumbLand = (ImageView) v.findViewById(R.id.video_icon_own_message_thumb_landscape);
            holder.videoTimecontentOwnMessageThumbLand = (TextView) v.findViewById(R.id.video_time_own_message_thumb_landscape);

            holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            holder.contentOwnMessageThumbPort = (RoundedImageView) v.findViewById(R.id.content_own_message_thumb_portrait);
            holder.contentOwnMessageThumbPort.setCornerRadius(radius);
            holder.contentOwnMessageThumbPort.setBorderWidth(1);
            holder.contentOwnMessageThumbPort.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentOwnMessageThumbPort.setOval(false);

            holder.ownTriangleIconFile = (RelativeLayout) v.findViewById(R.id.own_triangle_icon_file);
            holder.ownTriangleIconContact = (RelativeLayout) v.findViewById(R.id.own_triangle_icon_contact);


            holder.gradientOwnMessageThumbPort = (RelativeLayout) v.findViewById(R.id.gradient_own_message_thumb_portrait);
            holder.gradientOwnMessageThumbPort.setBackground(shape);

            holder.videoIconOwnMessageThumbPort = (ImageView) v.findViewById(R.id.video_icon_own_message_thumb_portrait);
            holder.videoTimecontentOwnMessageThumbPort = (TextView) v.findViewById(R.id.video_time_own_message_thumb_portrait);

            holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            holder.contentOwnMessageFileLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_file_layout);
            holder.forwardOwnFile = (RelativeLayout) v.findViewById(R.id.forward_own_file);
            holder.forwardOwnFile.setTag(holder);
            holder.forwardOwnFile.setVisibility(View.GONE);

            holder.contentOwnMessageFileThumb = (ImageView) v.findViewById(R.id.content_own_message_file_thumb);
            holder.contentOwnMessageFileName = (TextView) v.findViewById(R.id.content_own_message_file_name);
            holder.contentOwnMessageFileSize = (TextView) v.findViewById(R.id.content_own_message_file_size);

            holder.contentOwnMessageContactLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_contact_layout);
            holder.contentOwnMessageContactLayoutAvatar = (RelativeLayout) v.findViewById(R.id.content_own_message_contact_layout_avatar);
            holder.contentOwnMessageContactThumb = (RoundedImageView) v.findViewById(R.id.content_own_message_contact_thumb);
            holder.contentOwnMessageContactName = (TextView) v.findViewById(R.id.content_own_message_contact_name);
            holder.contentOwnMessageContactEmail = (TextView) v.findViewById(R.id.content_own_message_contact_email);

            holder.contentOwnMessageContactInitialLetter = (TextView) v.findViewById(R.id.content_own_message_contact_initial_letter);
            holder.forwardOwnContact = (RelativeLayout) v.findViewById(R.id.forward_own_contact);
            holder.forwardOwnContact.setTag(holder);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.iconOwnTypeDocLandPreview = (ImageView) v.findViewById(R.id.own_attachment_type_icon_lands);
            holder.iconOwnTypeDocPortraitPreview = (ImageView) v.findViewById(R.id.own_attachment_type_icon_portrait);

            holder.retryAlert = (TextView) v.findViewById(R.id.not_sent_own_message_text);
            holder.triangleIcon = (ImageView) v.findViewById(R.id.own_triangle_icon);

            holder.transparentCoatingPortrait = (RelativeLayout) v.findViewById(R.id.transparent_coating_portrait);
            holder.transparentCoatingPortrait.setVisibility(View.GONE);

            holder.transparentCoatingLandscape = (RelativeLayout) v.findViewById(R.id.transparent_coating_landscape);
            holder.transparentCoatingLandscape.setVisibility(View.GONE);

            holder.uploadingProgressBarPort = (RelativeLayout) v.findViewById(R.id.uploadingProgressBarPort);
            holder.uploadingProgressBarPort.setVisibility(View.GONE);
            holder.uploadingProgressBarLand = (RelativeLayout) v.findViewById(R.id.uploadingProgressBarLand);
            holder.uploadingProgressBarLand.setVisibility(View.GONE);

            holder.errorUploadingPortrait = (RelativeLayout) v.findViewById(R.id.error_uploading_portrait);
            holder.errorUploadingPortrait.setVisibility(View.GONE);
            holder.errorUploadingLandscape = (RelativeLayout) v.findViewById(R.id.error_uploading_landscape);
            holder.errorUploadingLandscape.setVisibility(View.GONE);

            holder.forwardOwnPortrait = (RelativeLayout) v.findViewById(R.id.forward_own_preview_portrait);
            holder.forwardOwnPortrait.setTag(holder);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setOnClickListener(this);

            holder.forwardOwnLandscape = (RelativeLayout) v.findViewById(R.id.forward_own_preview_landscape);
            holder.forwardOwnLandscape.setTag(holder);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setOnClickListener(this);

            holder.ownManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.own_management_message_layout);
            holder.ownManagementMessageText = (EmojiTextView) v.findViewById(R.id.own_management_message_text);

            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
                holder.ownManagementMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
            }else{
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
                holder.ownManagementMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
            }
            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);

            holder.ownManagementMessageIcon = (ImageView) v.findViewById(R.id.own_management_message_icon);
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holder.contentOwnMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
                holder.ownManagementMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
            } else {
                holder.contentOwnMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
                holder.ownManagementMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
            }

            //Contact messages
            holder.contactMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_contact_message_layout);
            holder.titleContactMessage = (RelativeLayout) v.findViewById(R.id.title_contact_message_layout);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            holder.contactImageView = (RoundedImageView) v.findViewById(R.id.contact_thumbnail);
            holder.contactInitialLetter = (TextView) v.findViewById(R.id.contact_initial_letter);

            holder.timeContactText = (TextView) v.findViewById(R.id.contact_message_chat_time_text);
            holder.nameContactText = (TextView) v.findViewById(R.id.contact_message_chat_name_text);

            holder.contentContactMessageLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_layout);
            holder.contentContactMessageText = (EmojiTextView) v.findViewById(R.id.content_contact_message_text);

            RelativeLayout.LayoutParams paramsContactMessage = (RelativeLayout.LayoutParams) holder.contentContactMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactMessage.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactMessage.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
            }
            holder.contentContactMessageText.setLayoutParams(paramsContactMessage);

            holder.contentContactMessageThumbLand = (RoundedImageView) v.findViewById(R.id.content_contact_message_thumb_landscape);
            holder.contentContactMessageThumbLand.setCornerRadius(radius);
            holder.contentContactMessageThumbLand.setBorderWidth(1);
            holder.contentContactMessageThumbLand.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentContactMessageThumbLand.setOval(false);
            holder.forwardContactPreviewLandscape = (RelativeLayout) v.findViewById(R.id.forward_contact_preview_landscape);
            holder.forwardContactPreviewLandscape.setTag(holder);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);

            //Contact rich links message
            holder.urlContactMessageLayout = (RelativeLayout) v.findViewById(R.id.url_contact_message_layout);
            if(((ChatActivityLollipop) context).getDeviceDensity() == 1){
                ViewGroup.LayoutParams params=holder.urlContactMessageLayout.getLayoutParams();
                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    params.width=450;
                }else{
                    params.width=330;
                }
                holder.urlContactMessageLayout.setLayoutParams(params);
            }

            RelativeLayout.LayoutParams paramsContactRichLink = (RelativeLayout.LayoutParams) holder.urlContactMessageLayout.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactRichLink.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactRichLink.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
            }
            holder.urlContactMessageLayout.setLayoutParams(paramsContactRichLink);

            holder.forwardContactRichLinks = (RelativeLayout) v.findViewById(R.id.forward_contact_rich_links);
            holder.forwardContactRichLinks.setTag(holder);
            holder.forwardContactRichLinks.setVisibility(View.GONE);

            holder.urlContactMessageText = (EmojiTextView) v.findViewById(R.id.url_contact_message_text);
            holder.urlContactMessageText.setTag(holder);

            holder.urlContactMessageTitle = (TextView) v.findViewById(R.id.url_contact_message_title);
            holder.urlContactMessageDescription = (TextView) v.findViewById(R.id.url_contact_message_description);

            holder.urlContactMessageIconAndLinkLayout = (LinearLayout) v.findViewById(R.id.url_contact_message_icon_link_layout);
            holder.urlContactMessageIcon = (ImageView) v.findViewById(R.id.url_contact_message_icon);
            holder.urlContactMessageLink = (TextView) v.findViewById(R.id.url_contact_message_link);

            holder.urlContactMessageImage = (RoundedImageView) v.findViewById(R.id.url_contact_message_image);
            holder.urlContactMessageImage.setCornerRadius(radiusImageRL);
            holder.urlContactMessageImage.setBorderWidth(0);
            holder.urlContactMessageImage.setOval(false);

            holder.contentContactMessageThumbPort = (RoundedImageView) v.findViewById(R.id.content_contact_message_thumb_portrait);
            holder.contentContactMessageThumbPort.setCornerRadius(radius);
            holder.contentContactMessageThumbPort.setBorderWidth(1);
            holder.contentContactMessageThumbPort.setBorderColor(ContextCompat.getColor(context, R.color.mail_my_account));
            holder.contentContactMessageThumbPort.setOval(false);
            holder.forwardContactPreviewPortrait = (RelativeLayout) v.findViewById(R.id.forward_contact_preview_portrait);
            holder.forwardContactPreviewPortrait.setTag(holder);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.gradientContactMessageThumbLand = (RelativeLayout) v.findViewById(R.id.gradient_contact_message_thumb_landscape);
            holder.gradientContactMessageThumbLand.setBackground(shape);

            holder.videoIconContactMessageThumbLand = (ImageView) v.findViewById(R.id.video_icon_contact_message_thumb_landscape);
            holder.videoTimecontentContactMessageThumbLand = (TextView) v.findViewById(R.id.video_time_contact_message_thumb_landscape);

            holder.gradientContactMessageThumbLand.setVisibility(View.GONE);

            holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            holder.gradientContactMessageThumbPort = (RelativeLayout) v.findViewById(R.id.gradient_contact_message_thumb_portrait);
            holder.gradientContactMessageThumbPort.setBackground(shape);

            holder.videoIconContactMessageThumbPort = (ImageView) v.findViewById(R.id.video_icon_contact_message_thumb_portrait);
            holder.videoTimecontentContactMessageThumbPort = (TextView) v.findViewById(R.id.video_time_contact_message_thumb_portrait);

            holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            holder.contentContactMessageAttachLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_attach_layout);
            RelativeLayout.LayoutParams paramsContactAttach = (RelativeLayout.LayoutParams) holder.contentContactMessageAttachLayout.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactAttach.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactAttach.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
            }
            holder.contentContactMessageAttachLayout.setLayoutParams(paramsContactAttach);

            holder.contentContactMessageFile = (RelativeLayout) v.findViewById(R.id.content_contact_message_file);
            holder.forwardContactFile = (RelativeLayout) v.findViewById(R.id.forward_contact_file);
            holder.forwardContactFile.setTag(holder);
            holder.forwardContactFile.setVisibility(View.GONE);
            holder.contentContactMessageFileThumb = (ImageView) v.findViewById(R.id.content_contact_message_file_thumb);
            holder.contentContactMessageFileName = (TextView) v.findViewById(R.id.content_contact_message_file_name);
            holder.contentContactMessageFileSize = (TextView) v.findViewById(R.id.content_contact_message_file_size);

            holder.layoutAvatarMessages = (RelativeLayout) v.findViewById(R.id.layout_avatar);
            holder.contentContactMessageContactLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout);
            RelativeLayout.LayoutParams paramsContactContact = (RelativeLayout.LayoutParams) holder.contentContactMessageContactLayout.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactContact.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactContact.leftMargin = Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics);
            }
            holder.contentContactMessageContactLayout.setLayoutParams(paramsContactContact);

            holder.forwardContactContact = (RelativeLayout) v.findViewById(R.id.forward_contact_contact);
            holder.forwardContactContact.setTag(holder);
            holder.forwardContactContact.setVisibility(View.GONE);

//            holder.contentContactMessageContactLayout.setVisibility(View.GONE);
            holder.contentContactMessageContactLayoutAvatar = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout_avatar);
            holder.contentContactMessageContactThumb = (RoundedImageView) v.findViewById(R.id.content_contact_message_contact_thumb);
            holder.contentContactMessageContactName = (TextView) v.findViewById(R.id.content_contact_message_contact_name);
            holder.contentContactMessageContactEmail = (TextView) v.findViewById(R.id.content_contact_message_contact_email);
            holder.contentContactMessageContactInitialLetter = (TextView) v.findViewById(R.id.content_contact_message_contact_initial_letter);

            holder.iconContactTypeDocLandPreview = (ImageView) v.findViewById(R.id.contact_attachment_type_icon_lands);
            holder.iconContactTypeDocPortraitPreview = (ImageView) v.findViewById(R.id.contact_attachment_type_icon_portrait);

            holder.contactManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.contact_management_message_layout);

            holder.contactManagementMessageText = (EmojiTextView) v.findViewById(R.id.contact_management_message_text);
            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
                holder.contactManagementMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));

            }else{
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
                holder.contactManagementMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));

            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);
            holder.contactManagementMessageIcon = (ImageView) v.findViewById(R.id.contact_management_message_icon);

            if(((ChatActivityLollipop) context).getDeviceDensity() == 1){
                MAX_WIDTH_FILENAME_LAND = 290;
                MAX_WIDTH_FILENAME_PORT = 140;
            }

            holder.contactManagementAvatarMainLayout = (RelativeLayout) v.findViewById(R.id.contact_management_message_layout_with_avatar);
            holder.contactManagementAvatarImage = (RoundedImageView) v.findViewById(R.id.management_contact_thumbnail);
            holder.contactManagementAvatarInitialLetter = (TextView) v.findViewById(R.id.management_contact_initial_letter);
            holder.contactManagementAvatarMessageText = (TextView) v.findViewById(R.id.contact_management_message_text_with_avatar);
            holder.contactManagementAvatarSubTextLayout = (RelativeLayout) v.findViewById(R.id.contact_subtext_layout_with_avatar);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holder.contentContactMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));

                holder.contactManagementMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
            } else {
                holder.contentContactMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));

                holder.contactManagementMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
            }

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

                holder.nameContactText.setMaxWidth(Util.scaleWidthPx(280, outMetrics));
            } else {

                holder.nameContactText.setMaxWidth(Util.scaleWidthPx(160, outMetrics));
            }

            v.setTag(holder);

            return holder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderHeaderChat) {
            log("onBindViewHolder ViewHolderHeaderChat: " + position);
        } else {
            log("onBindViewHolder ViewHolderMessageChat: " + position);
            AndroidMegaChatMessage androidMessage = messages.get(position - 1);

            if (androidMessage.isUploading()) {
                onBindViewHolderUploading(holder, position);
            } else {
                onBindViewHolderMessage(holder, position);
            }
        }
    }

    public void onBindViewHolderUploading(RecyclerView.ViewHolder holder, int position) {
        log("onBindViewHolderUploading(): position" + position);

        ((ViewHolderMessageChat) holder).itemLayout.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ((ViewHolderMessageChat) holder).itemLayout.setLayoutParams(params);

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

        ((ViewHolderMessageChat) holder).currentPosition = position;

        ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).ownTriangleIconContact.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
        }else{
            ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
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
                switch (message.getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getPendingMessage().getUploadTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message.getPendingMessage().getUploadTimestamp()));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message.getPendingMessage().getUploadTimestamp()));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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

            ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

            String path = message.getPendingMessage().getFilePath();
            String name = message.getPendingMessage().getName();
            if (path != null) {

                Bitmap preview = null;
                ((ViewHolderMessageChat) holder).filePathUploading = path;
                log("Path of the file: " + path);
                long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(path));

                if (MimeTypeList.typeForName(path).isImage() || MimeTypeList.typeForName(path).isPdf() || MimeTypeList.typeForName(path).isVideo()) {

                    ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.GONE);

                    preview = PreviewUtils.getPreviewFromCache(fingerprintCache);
                    if (preview != null) {
                        setUploadingPreview((ViewHolderMessageChat) holder, preview);
                        log("preview!");

                    } else {
                        log("No preview!");
                        if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                            ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);
                        }
                        try {
                            new ChatUploadingPreviewAsyncTask(((ViewHolderMessageChat) holder)).execute(path);

                        } catch (Exception e) {
                            //Too many AsyncTasks
                        }
                    }
                } else {

                    if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                        ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);
                    }
                }
                log("Node Name: " + name);

                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    log("Landscape configuration");
                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setMaxWidth((int) width);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setMaxWidth((int) width);
                } else {
                    log("Portrait configuration");
                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setMaxWidth((int) width);
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setMaxWidth((int) width);
                }

                ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setText(name);
                ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(name).getIconResourceId());
                ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                log("State of the message: " + message.getPendingMessage().getState());
                if (message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_UPLOADING || message.getPendingMessage().getState() == PendingMessageSingle.STATE_ERROR_ATTACHING) {
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_error);
                } else {
                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_uploading);
                }

            } else {
                log("Path is null");
            }
        } else {
            log("ERROR: The message is no UPLOADING");
        }
    }

    public void onBindViewHolderMessage(RecyclerView.ViewHolder holder, int position) {
        log("onBindViewHolderMessage(): " + position);

        ((ViewHolderMessageChat) holder).itemLayout.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams paramsDefault = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ((ViewHolderMessageChat) holder).itemLayout.setLayoutParams(paramsDefault);
        ((ViewHolderMessageChat) holder).currentPosition = position;
        ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).ownTriangleIconContact.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.GONE);
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

        ((ViewHolderMessageChat) holder).contactManagementAvatarMainLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contactManagementAvatarSubTextLayout.setVisibility(View.GONE);

        AndroidMegaChatMessage androidMessage = messages.get(position - 1);
        MegaChatMessage message = messages.get(position - 1).getMessage();
        ((ViewHolderMessageChat) holder).userHandle = message.getUserHandle();

        int messageType = message.getType();
        log("Message type: " + messageType);

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

        }
        else{
            log("Not known message: disable click - position: "+position);
            ((ViewHolderMessageChat) holder).itemLayout.setOnClickListener(null);
            ((ViewHolderMessageChat) holder).itemLayout.setOnLongClickListener(null);

            ((ViewHolderMessageChat) holder).contentContactMessageText.setOnClickListener(null);
            ((ViewHolderMessageChat) holder).contentContactMessageText.setOnLongClickListener(null);

            ((ViewHolderMessageChat) holder).contentOwnMessageText.setOnClickListener(null);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setOnLongClickListener(null);
        }

        switch (messageType) {

            case MegaChatMessage.TYPE_ALTER_PARTICIPANTS: {
                log("ALTER PARTICIPANT MESSAGE!!");
                bindAlterParticipantsMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_PRIV_CHANGE: {
                log("PRIVILEGE CHANGE message");
                bindPrivChangeMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CONTAINS_META: {
                log("MegaChatMessage.TYPE_CONTAINS_META");
                bindContainsMetaMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_NORMAL: {
                log("MegaChatMessage.TYPE_NORMAL");
                if (androidMessage.getRichLinkMessage() != null) {
                    bindMegaLinkMessage((ViewHolderMessageChat) holder, androidMessage, position);
                } else {
                    bindNormalMessage((ViewHolderMessageChat) holder, androidMessage, position);
                }
                break;
            }
            case MegaChatMessage.TYPE_NODE_ATTACHMENT: {
                log("MegaChatMessage.TYPE_NODE_ATTACHMENT");
                bindNodeAttachmentMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT: {
                log("MegaChatMessage.TYPE_CONTACT_ATTACHMENT");
                bindContactAttachmentMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CHAT_TITLE: {
                log("MegaChatMessage.TYPE_CHAT_TITLE");
                bindChangeTitleMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_TRUNCATE: {
                log("MegaChatMessage.TYPE_TRUNCATE");
                bindTruncateMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT: {
                log("MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT");
                bindRevokeNodeMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_CALL_ENDED:
            case MegaChatMessage.TYPE_CALL_STARTED: {
                log("MegaChatMessage.TYPE_CALL_ENDED or TYPE_CALL_STARTED");
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
                log("MegaChatMessage.TYPE_INVALID");
                bindNoTypeMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            case MegaChatMessage.TYPE_UNKNOWN: {
                log("MegaChatMessage.TYPE_UNKNOWN");
                hideMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
            default: {
                log("DEFAULT MegaChatMessage");
                hideMessage((ViewHolderMessageChat) holder, androidMessage, position);
                break;
            }
        }

        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(-14, outMetrics), Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
//        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);

        if (((ChatActivityLollipop) context).lastIdMsgSeen != -1) {

            if (((ChatActivityLollipop) context).lastIdMsgSeen == message.getMsgId()) {

                log("onBindViewHolder:Last message id match!");

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ((ViewHolderMessageChat) holder).newMessagesLayout.getLayoutParams();

                if ((message.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) || (message.getType() == MegaChatMessage.TYPE_PRIV_CHANGE)) {
                    if (message.getHandleOfAction() == myUserHandle) {
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_own_message_layout);
                        ((ViewHolderMessageChat) holder).newMessagesLayout.setLayoutParams(params);
                    } else {
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_contact_message_layout);
                        ((ViewHolderMessageChat) holder).newMessagesLayout.setLayoutParams(params);
                    }

                } else {
                    if (message.getUserHandle() == megaChatApi.getMyUserHandle()) {
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_own_message_layout);
                        ((ViewHolderMessageChat) holder).newMessagesLayout.setLayoutParams(params);
                    } else {
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_contact_message_layout);
                        ((ViewHolderMessageChat) holder).newMessagesLayout.setLayoutParams(params);
                    }
                }

                String numberString;
                long unreadMessages = Math.abs(((ChatActivityLollipop) context).generalUnreadCount);
                if (((ChatActivityLollipop) context).generalUnreadCount < 0) {
                    numberString = "+" + unreadMessages;
                } else {
                    numberString = unreadMessages + "";
                }

                String contentUnreadText = context.getResources().getQuantityString(R.plurals.number_unread_messages, (int) unreadMessages, numberString);
                ((ViewHolderMessageChat) holder).newMessagesText.setText(contentUnreadText);

                ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.VISIBLE);
//                ((ChatActivityLollipop)context).showJumpMessage();
                ((ChatActivityLollipop) context).setNewVisibility(true);

                log("Set positionNewMessagesLayout: "+position);
                ((ChatActivityLollipop) context).positionNewMessagesLayout = position;
            } else {
                ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.GONE);
            }
        } else {
            ((ViewHolderMessageChat) holder).newMessagesLayout.setVisibility(View.GONE);
        }
    }

    public boolean isKnownMessage(int messageType){
        switch (messageType) {

            case MegaChatMessage.TYPE_ALTER_PARTICIPANTS:
            case MegaChatMessage.TYPE_PRIV_CHANGE:
            case MegaChatMessage.TYPE_CONTAINS_META:
            case MegaChatMessage.TYPE_NORMAL:
            case MegaChatMessage.TYPE_NODE_ATTACHMENT:
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
        log("bindCallMessage");

        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
        MegaChatMessage message = androidMessage.getMessage();

        if (message.getUserHandle() == myUserHandle) {
            log("MY message!!");
            log("MY message handle!!: " + message.getMsgId());

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics),0,0,0);
            }


            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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

                        textToShow = context.getString(R.string.call_ended_message);

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
                    case MegaChatMessage.END_CALL_REASON_NO_ANSWER:{

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
                    case MegaChatMessage.END_CALL_REASON_CANCELLED:{

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
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics);
            }
            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }
        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);
            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
                }
            ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_CALL_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);
//            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }

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

                        textToShow = context.getString(R.string.call_ended_message);

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
                    case MegaChatMessage.END_CALL_REASON_NO_ANSWER:{
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
                    case MegaChatMessage.END_CALL_REASON_CANCELLED:{

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
        log("bindAlterParticipantsMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);

        MegaChatMessage message = androidMessage.getMessage();

        if (message.getHandleOfAction() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            log("me alter participant");
            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            int privilege = message.getPrivilege();
            log("Privilege of me: " + privilege);
            String textToShow = "";
            String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

            if (privilege != MegaChatRoom.PRIV_RM) {
                log("I was added");

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
                log("I was removed or left");
                if (message.getUserHandle() == message.getHandleOfAction()) {
                    log("I left the chat");
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
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }
        } else {
            log("CONTACT Message type ALTER PARTICIPANTS");
            int privilege = message.getPrivilege();
            log("Privilege of the user: " + privilege);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);

                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }
            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);
            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }

            setContactMessageName(position, holder, message.getHandleOfAction(), false);

            String textToShow = "";
            if (privilege != MegaChatRoom.PRIV_RM) {
                log("Participant was added");
                if (message.getUserHandle() == myUserHandle) {
                    log("By me");

                    if(message.getUserHandle() == message.getHandleOfAction()){
                        textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(holder.fullNameTitle));
                    }
                    else{
                        textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle), toCDATA(megaChatApi.getMyFullname()));
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
                    log("By other");
                    String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

                    if(message.getUserHandle() == message.getHandleOfAction()){
                        textToShow = String.format(context.getString(R.string.message_joined_public_chat_autoinvitation), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle));
                    }
                    else{
                        textToShow = String.format(context.getString(R.string.message_add_participant), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle), toCDATA(fullNameAction));
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
                log("Participant was removed or left");
                if (message.getUserHandle() == myUserHandle) {
                    textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle), toCDATA(megaChatApi.getMyFullname()));
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
                        log("The participant left the chat");

                        textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle));
                        try {
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                        } catch (Exception e) {
                        }

                    } else {
                        log("The participant was removed");
                        String fullNameAction = getContactMessageName(position, holder, message.getUserHandle());

                        textToShow = String.format(context.getString(R.string.message_remove_participant), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle), toCDATA(fullNameAction));
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
        log("bindPrivChangeMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);


        MegaChatMessage message = androidMessage.getMessage();

        if (message.getHandleOfAction() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            log("a moderator change my privilege");
            int privilege = message.getPrivilege();
            log("Privilege of the user: " + privilege);

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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
                log("Change to other");
                privilegeString = "Unknow";
            }

            String textToShow = "";

            if (message.getUserHandle() == myUserHandle) {
                log("I changed my Own permission");
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
                log("I was change by someone");
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
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);


            log("Visible own management message!");

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                } else {
                    log("NOT selected");
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }
        } else {
            log("Participant privilege change!");

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }
            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);
            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                } else {
                    log("NOT selected");
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }

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
                log("Change to other");
                privilegeString = "Unknow";
            }

            String textToShow = "";
            if (message.getUserHandle() == myUserHandle) {
                log("The privilege was change by me");
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
                log("By other");
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
        log("bindContainsMetaMessage()");

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
                log("EXCEPTION: "+e.getMessage());
            }

            String title = meta.getRichPreview().getTitle();
            String text = meta.getRichPreview().getText();
            String description = meta.getRichPreview().getDescription();
            String imageFormat = meta.getRichPreview().getImageFormat();
            String image = meta.getRichPreview().getImage();
            String icon = meta.getRichPreview().getIcon();

            Bitmap bitmapImage = null;
            Bitmap bitmapIcon = null;

            if (image != null) {
                byte[] decodedBytes = Base64.decode(image, 0);
                bitmapImage = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            }

            if (icon != null) {
                byte[] decodedBytes2 = Base64.decode(icon, 0);
                bitmapIcon = BitmapFactory.decodeByteArray(decodedBytes2, 0, decodedBytes2.length);
            }

            if (message.getUserHandle() == myUserHandle) {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
                holder.titleOwnMessage.setGravity(Gravity.RIGHT);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
                }else{
                    holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
                }

                log("MY message handle!!: " + message.getMsgId());
                if (messages.get(position - 1).getInfoToShow() != -1) {
                    switch (messages.get(position - 1).getInfoToShow()) {
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                int status = message.getStatus();

                if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                    log("Show triangle retry!");
                    ((ViewHolderMessageChat)holder).urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);

                }else if((status==MegaChatMessage.STATUS_SENDING)){
                    ((ViewHolderMessageChat)holder).urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);

                }else{
                    log("Status: "+message.getStatus());
                    ((ViewHolderMessageChat)holder).urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                    ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);
                }

                holder.contactMessageLayout.setVisibility(View.GONE);
                holder.ownMessageLayout.setVisibility(View.VISIBLE);

                holder.ownManagementMessageLayout.setVisibility(View.GONE);
                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);

                holder.contentOwnMessageText.setVisibility(View.GONE);
                holder.urlOwnMessageLayout.setVisibility(View.VISIBLE);

                //Forwards element (own messages):
                if (cC.isInAnonymousMode()) {
                    holder.forwardOwnRichLinks.setVisibility(View.GONE);
                }
                else {
                    holder.forwardOwnRichLinks.setVisibility(View.VISIBLE);
                }
                if(isMultipleSelect()){
                    holder.forwardOwnRichLinks.setOnClickListener(null);
                }else{
                    holder.forwardOwnRichLinks.setOnClickListener(this);
                }
                holder.forwardOwnPortrait.setVisibility(View.GONE);
                holder.forwardOwnLandscape.setVisibility(View.GONE);
                holder.forwardOwnFile.setVisibility(View.GONE);
                holder.forwardOwnContact.setVisibility(View.GONE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);
                holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
                holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

                SimpleSpanBuilder ssb = null;

                try{

                    RTFFormatter formatter = new RTFFormatter(text, context);
                    ssb = formatter.setRTFFormat();
                }
                catch (Exception e){
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

                if(EmojiManager.getInstance().isOnlyEmojis(text)){
                    int numEmojis = EmojiManager.getInstance().getNumEmojis(text);
                    log("Only emojis ");
                    switch (numEmojis) {
                        case 1: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                            }
                            break;
                        }
                        case 2: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                            }
                            break;
                        }
                        case 3: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                            }
                            break;
                        }
                        default: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }
                            break;
                        }
                    }
                }else{
                    log("Not only emojis");
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.0f);
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                    }else{
                        ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                    }
                }

//                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);
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

                } else {
                    holder.urlOwnMessageImage.setVisibility(View.GONE);
                }

                if (bitmapIcon != null) {
                    holder.urlOwnMessageIcon.setImageBitmap(bitmapIcon);
                    holder.urlOwnMessageIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.urlOwnMessageIcon.setVisibility(View.GONE);
                }

                if (Util.isOnline(context)) {
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

                if (!multipleSelect) {
                    if (positionClicked != -1) {
                        if (positionClicked == position) {
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                            holder.forwardOwnRichLinks.setEnabled(false);
                        } else {
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            holder.forwardOwnRichLinks.setEnabled(true);
                        }
                    } else {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardOwnRichLinks.setEnabled(true);
                    }
                } else {
                    if (this.isItemChecked(position)) {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        holder.forwardOwnRichLinks.setEnabled(false);
                    } else {
                        log("NOT selected");
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardOwnRichLinks.setEnabled(true);
                    }
                }
            } else {
                long userHandle = message.getUserHandle();
                log("Contact message!!: " + userHandle);
                setContactMessageName(position, holder, userHandle, true);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
                }else{
                    holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
                }

                if (messages.get(position - 1).getInfoToShow() != -1) {
                    switch (messages.get(position - 1).getInfoToShow()) {
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                            log("CHAT_ADAPTER_SHOW_ALL");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                            ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                            log("CHAT_ADAPTER_SHOW_TIME--");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                            ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                if (messages.get(position - 1).isShowAvatar()) {
                    holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                    setContactAvatar(holder, userHandle, holder.fullNameTitle, false);

                } else {
                    holder.layoutAvatarMessages.setVisibility(View.GONE);
                }

                holder.ownMessageLayout.setVisibility(View.GONE);
                holder.contactMessageLayout.setVisibility(View.VISIBLE);

                holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
                holder.contactManagementMessageLayout.setVisibility(View.GONE);

                holder.contentContactMessageText.setVisibility(View.GONE);
                holder.urlContactMessageLayout.setVisibility(View.VISIBLE);

                //Forwards element (contact messages):
                if (cC.isInAnonymousMode()) {
                    holder.forwardContactRichLinks.setVisibility(View.GONE);
                }
                else {
                    holder.forwardContactRichLinks.setVisibility(View.VISIBLE);
                }
                if(isMultipleSelect()){
                    holder.forwardContactRichLinks.setOnClickListener(null);
                }else{
                    holder.forwardContactRichLinks.setOnClickListener(this);
                }
                holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
                holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
                holder.forwardContactFile.setVisibility(View.GONE);
                holder.forwardContactContact.setVisibility(View.GONE);

                holder.contentContactMessageAttachLayout.setVisibility(View.GONE);
                holder.contentContactMessageContactLayout.setVisibility(View.GONE);


//                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
//                ((ViewHolderMessageChat)holder).urlOwnMessageLayout.setVisibility(View.GONE);
//                ((ViewHolderMessageChat)holder).urlOwnMessageIconAndLinkLayout.setVisibility(View.GONE);

//                ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
//                ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
//                ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

//                ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
//
//                ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
//
//                ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);
//                ((ViewHolderMessageChat) holder).contentContactMessageFile.setVisibility(View.GONE);

                //Rick link
                holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
                holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

                holder.urlContactMessageTitle.setVisibility(View.VISIBLE);
                holder.urlContactMessageTitle.setText(title);
                holder.urlContactMessageDescription.setText(description);
                holder.urlContactMessageIconAndLinkLayout.setVisibility(View.VISIBLE);
                holder.urlContactMessageLink.setText(urlString);


                if(EmojiManager.getInstance().isOnlyEmojis(text)){
                    int numEmojis = EmojiManager.getInstance().getNumEmojis(text);
                    log("Only emojis ");
                    switch (numEmojis) {
                        case 1: {
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                            }
                            break;
                        }
                        case 2: {
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                            }
                            break;
                        }
                        case 3: {
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                            }
                            break;
                        }
                        default: {
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }
                            break;
                        }
                    }
                }else{
                    log("Not only emojis");
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.0f);
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                    }else{
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                    }
                }

                //Color always status SENT
                ((ViewHolderMessageChat) holder).urlContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                SimpleSpanBuilder ssb = null;

                try {
                    RTFFormatter formatter = new RTFFormatter(text, context);
                    ssb = formatter.setRTFFormat();
                } catch (Exception e) {
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

                if (ssb != null) {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(text);
                }

                if(bitmapImage!=null){
                    ((ViewHolderMessageChat)holder).urlContactMessageImage.setImageBitmap(bitmapImage);
                    ((ViewHolderMessageChat)holder).urlContactMessageImage.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).urlContactMessageGroupAvatarLayout.setVisibility(View.GONE);
                }
                else{
                    ((ViewHolderMessageChat)holder).urlContactMessageImage.setVisibility(View.GONE);
                }

                if (bitmapIcon != null) {
                    ((ViewHolderMessageChat) holder).urlContactMessageIcon.setImageBitmap(bitmapIcon);
                    ((ViewHolderMessageChat) holder).urlContactMessageIcon.setVisibility(View.VISIBLE);
                } else {
                    ((ViewHolderMessageChat) holder).urlContactMessageIcon.setVisibility(View.GONE);
                }

                if (Util.isOnline(context)) {
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

                if (!multipleSelect) {
                    if (positionClicked != -1) {
                        if (positionClicked == position) {
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            holder.forwardContactRichLinks.setEnabled(false);
                            listFragment.smoothScrollToPosition(positionClicked);
                        } else {
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            holder.forwardContactRichLinks.setEnabled(true);

                        }
                    } else {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardContactRichLinks.setEnabled(true);

                    }
                } else {
                    log("Multiselect ON");
                    if (this.isItemChecked(position)) {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        holder.forwardContactRichLinks.setEnabled(false);

                    } else {
                        log("NOT selected");
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardContactRichLinks.setEnabled(true);

                    }
                }
            }
        } else if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID) {
            if (message.getUserHandle() == myUserHandle) {
                holder.layoutAvatarMessages.setVisibility(View.GONE);
                holder.titleOwnMessage.setGravity(Gravity.RIGHT);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
                }else{
                    holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
                }

                if (messages.get(position - 1).getInfoToShow() != -1) {
                    switch (messages.get(position - 1).getInfoToShow()) {
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                            holder.dateLayout.setVisibility(View.VISIBLE);
                            holder.dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                            log("CHAT_ADAPTER_SHOW_TIME");
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
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
                    log("Show triangle retry!");
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
                    log("Status: " + message.getStatus());

                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.contentOwnMessageText.setText(context.getString(R.string.error_meta_message_invalid));

                if (Util.isOnline(context)) {
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
                log("Contact message!!: " + userHandle);

                setContactMessageName(position, holder, userHandle, true);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
                }else{
                    holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
                }

                //forward element (contact message)
                holder.forwardContactRichLinks.setVisibility(View.GONE);
                holder.forwardContactContact.setVisibility(View.GONE);
                holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
                holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
                holder.forwardContactFile.setVisibility(View.GONE);

                if (messages.get(position - 1).getInfoToShow() != -1) {
                    switch (messages.get(position - 1).getInfoToShow()) {
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                            log("CHAT_ADAPTER_SHOW_ALL");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                            ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                            log("CHAT_ADAPTER_SHOW_TIME--");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                            ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                holder.ownMessageLayout.setVisibility(View.GONE);
                holder.contactMessageLayout.setVisibility(View.VISIBLE);

                holder.contactManagementMessageLayout.setVisibility(View.GONE);
                holder.contentContactMessageLayout.setVisibility(View.VISIBLE);


                if (messages.get(position - 1).isShowAvatar()) {
                    ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                    setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle, false);
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

                //COlor always status SENT
                ((ViewHolderMessageChat) holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                ((ViewHolderMessageChat) holder).contentContactMessageText.setText(context.getString(R.string.error_meta_message_invalid));
            }
        }
        else {
            log("Link to bind as a no type message");
            bindNoTypeMessage(holder, androidMessage, position);
        }
    }


    public void bindMegaLinkMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        log("bindMegaLinkMessage()");

        MegaChatMessage message = androidMessage.getMessage();
        MegaNode node = androidMessage.getRichLinkMessage().getNode();

        if (message.getUserHandle() == myUserHandle) {
            log("MY message handle!!: " + message.getMsgId());

            holder.layoutAvatarMessages.setVisibility(View.GONE);
            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        holder.dateLayout.setVisibility(View.VISIBLE);
                        holder.dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
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
            if (cC.isInAnonymousMode()) {
                holder.forwardOwnRichLinks.setVisibility(View.GONE);
            }
            else {
                holder.forwardOwnRichLinks.setVisibility(View.VISIBLE);
            }
            if(isMultipleSelect()){
                holder.forwardOwnRichLinks.setOnClickListener(null);
            }else{
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
            holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

            //MEGA link
            holder.urlOwnMessageWarningButtonsLayout.setVisibility(View.GONE);
            holder.urlOwnMessageDisableButtonsLayout.setVisibility(View.GONE);

            if (message.isEdited()) {
                Spannable content = new SpannableString(message.getContent());
                int status = message.getStatus();
                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    log("Show triangle retry!");
                    holder.urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.VISIBLE);
                    holder.retryAlert.setVisibility(View.VISIBLE);

                } else if ((status == MegaChatMessage.STATUS_SENDING)) {
                    log("Status not received by server: " + message.getStatus());
                    holder.urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);

                } else {
                    log("Status: " + message.getStatus());
                    holder.urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                SimpleSpanBuilder ssb = null;

                try {
                    RTFFormatter formatter = new RTFFormatter(message.getContent(), context);
                    ssb = formatter.setRTFFormat();
                } catch (Exception e) {
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

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

                if(EmojiManager.getInstance().isOnlyEmojis(message.getContent())){
                    int numEmojis = EmojiManager.getInstance().getNumEmojis(message.getContent());
                    log("Only emojis ");
                    switch (numEmojis) {
                        case 1: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                            }
                            break;
                        }
                        case 2: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                            }
                            break;
                        }
                        case 3: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                            }
                            break;
                        }
                        default: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }
                            break;
                        }
                    }
                }else{
                    log("Not only emojis");
                    ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.0f);
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                    }else{
                        ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                    }
                }

            } else {

                SimpleSpanBuilder ssb = null;

                try {
                    if (message.getContent() != null) {
                        RTFFormatter formatter = new RTFFormatter(message.getContent(), context);
                        ssb = formatter.setRTFFormat();
                    }
                } catch (Exception e) {
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

                int status = message.getStatus();

                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    log("Show triangle retry!");
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                    ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);

                } else if ((status == MegaChatMessage.STATUS_SENDING)) {
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                    ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

                } else {
                    log("Status: " + message.getStatus());
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));

                    ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);
                }

                if(EmojiManager.getInstance().isOnlyEmojis(message.getContent())){
                    int numEmojis = EmojiManager.getInstance().getNumEmojis(message.getContent());
                    log("Only emojis ");
                    switch (numEmojis) {
                        case 1: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                            }
                            break;
                        }
                        case 2: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                            }
                            break;
                        }
                        case 3: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                            }
                            break;
                        }
                        default: {
                            ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }
                            break;
                        }
                    }
                }else{
                    log("Not only emojis");
                    ((ViewHolderMessageChat) holder).urlOwnMessageText.setLineSpacing(1, 1.0f);
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                    }else{
                        ((ViewHolderMessageChat) holder).urlOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                    }
                }

//                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);
                if (ssb != null) {
                    holder.urlOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    holder.urlOwnMessageText.setText(message.getContent());
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
                    thumb = ThumbnailUtils.getThumbnailFromCache(node);
                    if (thumb != null) {
                        PreviewUtils.previewCache.put(node.getHandle(), thumb);
                        ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageBitmap(thumb);
                    } else {
                        thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                        if (thumb != null) {
                            PreviewUtils.previewCache.put(node.getHandle(), thumb);
                            ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageBitmap(thumb);
                        } else {
                            ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        }
                    }
                    ((ViewHolderMessageChat) holder).urlOwnMessageDescription.setText(Formatter.formatFileSize(context, node.getSize()));
                } else {
                    if (node.isInShare()) {
                        ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageResource(R.drawable.ic_folder_incoming_list);
                    } else {
                        ((ViewHolderMessageChat) holder).urlOwnMessageImage.setImageResource(R.drawable.ic_folder_list);
                    }
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
//                        createGroupChatAvatar(holder, androidMessage.getRichLinkMessage().getTitle(), true);
                    }
                    else{
                        if (Util.isOnline(context)) {
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
                    log("Chat-link error: null");
                }
            }

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        holder.forwardOwnRichLinks.setEnabled(false);
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardOwnRichLinks.setEnabled(true);

                    }
                } else {
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardOwnRichLinks.setEnabled(true);

                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    holder.forwardOwnRichLinks.setEnabled(false);

                } else {
                    log("NOT selected");
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardOwnRichLinks.setEnabled(true);

                }
            }
        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);
            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            if (messages.get(position - 1).isShowAvatar()) {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle, false);
            } else {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
            }

            String messageContent = "";
            if (message.isEdited()) {
                log("Message is edited");

                if (message.getContent() != null) {
                    messageContent = message.getContent();
                }

                SimpleSpanBuilder ssb = null;
                try {
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                        RTFFormatter formatter = new RTFFormatter(messageContent, context);
                        ssb = formatter.setRTFFormat();
                    }

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
                } catch (Exception e) {
                    ssb = null;
                    log("FORMATTER EXCEPTION!!!");
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

                if (message.getContent() != null) {
                    messageContent = message.getContent();
                }

                //Color always status SENT
                ((ViewHolderMessageChat) holder).urlContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                SimpleSpanBuilder ssb = null;

                try {
                    RTFFormatter formatter = new RTFFormatter(messageContent, context);
                    ssb = formatter.setRTFFormat();
                } catch (Exception e) {
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

                if (ssb != null) {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setText(messageContent);
                }
            }

            if(EmojiManager.getInstance().isOnlyEmojis(messageContent)){
                int numEmojis = EmojiManager.getInstance().getNumEmojis(messageContent);
                log("Only emojis ");
                switch (numEmojis) {
                    case 1: {
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                        }else{
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                        }
                        break;
                    }
                    case 2: {
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                        }else{
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                        }
                        break;
                    }
                    case 3: {
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                        }else{
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                        }
                        break;
                    }
                    default: {
                        ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.2f);
                        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                        }else{
                            ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                        }
                        break;
                    }
                }
            }else{
                log("Not only emojis");
                ((ViewHolderMessageChat) holder).urlContactMessageText.setLineSpacing(1, 1.0f);
                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                }else{
                    ((ViewHolderMessageChat) holder).urlContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                }
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);

            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
            holder.contactManagementMessageLayout.setVisibility(View.GONE);

            holder.contentContactMessageText.setVisibility(View.GONE);
            holder.urlContactMessageLayout.setVisibility(View.VISIBLE);

            //Forward element (contact messages):
            if (cC.isInAnonymousMode()) {
                holder.forwardContactRichLinks.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactRichLinks.setVisibility(View.VISIBLE);
            }
            if(isMultipleSelect()){
                holder.forwardContactRichLinks.setOnClickListener(null);
            }else{
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
                    thumb = ThumbnailUtils.getThumbnailFromCache(node);

                    if (thumb != null) {
                        PreviewUtils.previewCache.put(node.getHandle(), thumb);
                        ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageBitmap(thumb);
                    } else {
                        thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                        if (thumb != null) {
                            PreviewUtils.previewCache.put(node.getHandle(), thumb);
                            ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageBitmap(thumb);
                        } else {
                            ((ViewHolderMessageChat) holder).urlContactMessageImage.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                        }
                    }
                    ((ViewHolderMessageChat) holder).urlContactMessageDescription.setText(Formatter.formatFileSize(context, node.getSize()));
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
                        if (Util.isOnline(context)) {
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
                    log("Chat-link error: richLinkMessage is NULL");
                }
            }

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        holder.forwardContactRichLinks.setEnabled(false);

                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardContactRichLinks.setEnabled(true);

                    }
                } else {
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardContactRichLinks.setEnabled(true);

                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    holder.forwardContactRichLinks.setEnabled(false);

                } else {
                    log("NOT selected");
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardContactRichLinks.setEnabled(true);

                }
            }
        }

    }


    public void bindNormalMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        log("bindNormalMessage()  position: "+position);

        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            log("MY message handle!!: " + message.getMsgId());
            holder.layoutAvatarMessages.setVisibility(View.GONE);
             holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                 holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        holder.dateLayout.setVisibility(View.VISIBLE);
                        holder.dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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
                messageContent = message.getContent();
            }

            int lastPosition = messages.size();

            if (lastPosition == position) {

                if (MegaChatApi.hasUrl(messageContent)) {

                    if(((ChatActivityLollipop)context).checkMegaLink(message)==-1) {
                        log("Is a link - not from MEGA");

                        if (MegaApplication.isShowRichLinkWarning()) {
                            log("SDK - show link rich warning");
                            if (((ChatActivityLollipop) context).showRichLinkWarning == Constants.RICH_WARNING_TRUE) {
                                log("ANDROID - show link rich warning");

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
                            } else if (((ChatActivityLollipop) context).showRichLinkWarning == Constants.RICH_WARNING_CONFIRMATION) {
                                log("ANDROID - show link disable rich link confirmation");

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
                        log("It a MEGA link: wait for info update");
                    }
                }
            }

            if (message.isEdited()) {
                log("MY Message is edited");

                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                holder.contentOwnMessageText.setVisibility(View.VISIBLE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

                int status = message.getStatus();
                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    log("Show triangle retry!");
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.VISIBLE);
                    holder.retryAlert.setVisibility(View.VISIBLE);
                }else if((status==MegaChatMessage.STATUS_SENDING)){
                    log("Status not received by server: "+message.getStatus());
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }else{
                    log("Status: "+message.getStatus());
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                SimpleSpanBuilder ssb = null;

                try {
                    RTFFormatter formatter = new RTFFormatter(messageContent, context);
                    ssb = formatter.setRTFFormat();
                } catch (Exception e) {
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

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

                if(EmojiManager.getInstance().isOnlyEmojis(messageContent)){
                    int numEmojis = EmojiManager.getInstance().getNumEmojis(messageContent);
                    log("Only emojis ");
                    switch (numEmojis) {
                        case 1: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                            }
                            break;
                        }
                        case 2: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                            }
                            break;
                        }
                        case 3: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                            }
                            break;
                        }
                        default: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }
                            break;
                        }
                    }
                }else{
                    log("Not only emojis");
                    ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.0f);
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                    }else{
                        ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                    }
                }

//                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);

                if (Util.isOnline(context)) {
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

                if (!multipleSelect) {
                    if (positionClicked != -1) {
                        if (positionClicked == position) {
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        } else {
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
                    if (this.isItemChecked(position)) {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    } else {
                        log("NOT selected");
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

            }else if (message.isDeleted()) {
                log("Message is deleted");
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

                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

            } else {
                log("Message not edited not deleted");

                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                holder.ownManagementMessageLayout.setVisibility(View.GONE);
                holder.contentOwnMessageText.setVisibility(View.VISIBLE);

                holder.previewFrameLand.setVisibility(View.GONE);
                holder.previewFramePort.setVisibility(View.GONE);

                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

                SimpleSpanBuilder ssb = null;

                try {
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                        RTFFormatter formatter = new RTFFormatter(messageContent, context);
                        ssb = formatter.setRTFFormat();
                    }
                } catch (Exception e) {
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

                int status = message.getStatus();

                if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                    log("Show triangle retry!");
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
                    log("Status: " + message.getStatus());

                    holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    holder.contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                    holder.triangleIcon.setVisibility(View.GONE);
                    holder.retryAlert.setVisibility(View.GONE);
                }

                if(EmojiManager.getInstance().isOnlyEmojis(messageContent)){
                    int numEmojis = EmojiManager.getInstance().getNumEmojis(messageContent);
                    log("Only emojis ");
                    switch (numEmojis) {
                        case 1: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                            }
                            break;
                        }
                        case 2: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                            }
                            break;
                        }
                        case 3: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                            }
                            break;
                        }
                        default: {
                            ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }
                            break;
                        }
                    }
                }else{
                    log("Not only emojis");
                    ((ViewHolderMessageChat) holder).contentOwnMessageText.setLineSpacing(1, 1.0f);
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                    }else{
                        ((ViewHolderMessageChat) holder).contentOwnMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                    }
                }

                if (ssb != null) {
                    holder.contentOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    holder.contentOwnMessageText.setText(messageContent);
                }

                if (Util.isOnline(context)) {
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

                if (!multipleSelect) {
                    if (positionClicked != -1) {
                        if (positionClicked == position) {
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        } else {
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
                    if (this.isItemChecked(position)) {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    } else {
                        log("NOT selected");
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }
            }
        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            //forward element (contact message)
            holder.forwardContactRichLinks.setVisibility(View.GONE);
            holder.forwardContactContact.setVisibility(View.GONE);
            holder.forwardContactPreviewLandscape.setVisibility(View.GONE);
            holder.forwardContactPreviewPortrait.setVisibility(View.GONE);
            holder.forwardContactFile.setVisibility(View.GONE);

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);

            holder.contactManagementMessageLayout.setVisibility(View.GONE);
            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);


            if (messages.get(position - 1).isShowAvatar()) {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle, false);
            } else {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
            }

            if (message.isEdited()) {
                log("Message is edited");

                String messageContent = "";
                if (message.getContent() != null) {
                    messageContent = message.getContent();
                }
                ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat) holder).urlContactMessageLayout.setVisibility(View.GONE);

                ((ViewHolderMessageChat) holder).contentContactMessageAttachLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

                SimpleSpanBuilder ssb = null;
                try {
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                        RTFFormatter formatter = new RTFFormatter(messageContent, context);
                        ssb = formatter.setRTFFormat();
                    }

                    if (ssb != null) {
                        ssb.append(" " + context.getString(R.string.edited_message_text), new RelativeSizeSpan(0.70f), new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), new StyleSpan(Typeface.ITALIC));
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                    } else {
                        messageContent = messageContent + " ";
                        Spannable content = new SpannableString(messageContent);
                        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        CharSequence indexedText = TextUtils.concat(messageContent, edited);
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setText(indexedText);
                    }
                } catch (Exception e) {
                    ssb = null;
                    log("FORMATTER EXCEPTION!!!");
                    messageContent = messageContent + " ";
                    Spannable content = new SpannableString(messageContent);
                    content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                    edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    CharSequence indexedText = TextUtils.concat(messageContent, edited);
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(indexedText);
                }

                if (Util.isOnline(context)) {
                    if(isMultipleSelect()){
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                    }else {
                        Linkify.addLinks(((ViewHolderMessageChat) holder).contentContactMessageText, Linkify.WEB_URLS);
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(true);
                    }
                } else {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                }

                if (!multipleSelect) {
                    if (positionClicked != -1) {
                        if (positionClicked == position) {
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        } else {
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");

                    if (this.isItemChecked(position)) {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                    } else {
                        log("NOT selected");
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

            }else if (message.isDeleted()) {
                log("Message is deleted");

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

                if(EmojiManager.getInstance().isOnlyEmojis(messageContent)){
                    int numEmojis = EmojiManager.getInstance().getNumEmojis(messageContent);
                    log("Only emojis ");
                    switch (numEmojis) {
                        case 1: {
                            ((ViewHolderMessageChat) holder).contentContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(35, outMetrics));
                            }
                            break;
                        }
                        case 2: {
                            ((ViewHolderMessageChat) holder).contentContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(17, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(30, outMetrics));
                            }
                            break;
                        }
                        case 3: {
                            ((ViewHolderMessageChat) holder).contentContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(15, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(25, outMetrics));
                            }
                            break;
                        }
                        default: {
                            ((ViewHolderMessageChat) holder).contentContactMessageText.setLineSpacing(1, 1.2f);
                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(11, outMetrics));
                            }else{
                                ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                            }
                            break;
                        }
                    }
                }else{
                    log("Not only emojis");
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setLineSpacing(1, 1.0f);
                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(10, outMetrics));
                    }else{
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setEmojiSize(Util.scaleWidthPx(20, outMetrics));
                    }
                }

                //Color always status SENT
                ((ViewHolderMessageChat) holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                SimpleSpanBuilder ssb = null;

                try {
                    RTFFormatter formatter = new RTFFormatter(messageContent, context);
                    ssb = formatter.setRTFFormat();
                } catch (Exception e) {
                    log("FORMATTER EXCEPTION!!!");
                    ssb = null;
                }

                if (ssb != null) {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                } else {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setText(messageContent);
                }

                if (Util.isOnline(context)) {
                    if(isMultipleSelect()){
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                    }else{
                        Linkify.addLinks(((ViewHolderMessageChat) holder).contentContactMessageText, Linkify.WEB_URLS);
                        ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(true);
                    }
                } else {
                    ((ViewHolderMessageChat) holder).contentContactMessageText.setLinksClickable(false);
                }

                if (!multipleSelect) {
                    if (positionClicked != -1) {
                        if (positionClicked == position) {
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        } else {
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");

                    if (this.isItemChecked(position)) {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                    } else {
                        log("NOT selected");
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }
            }
        }
    }

    public void bindNodeAttachmentMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        log("bindNodeAttachmentMessage()--> position: "+position);

        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            log("MY message!!");
            holder.layoutAvatarMessages.setVisibility(View.GONE);

            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }
            log("MY message handle!!: " + message.getMsgId());
            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        holder.dateLayout.setVisibility(View.VISIBLE);
                        holder.dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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
            if (cC.isInAnonymousMode()) {
                holder.forwardOwnFile.setVisibility(View.GONE);
            }
            else {
                holder.forwardOwnFile.setVisibility(View.VISIBLE);
            }
            if(isMultipleSelect()){
                holder.forwardOwnFile.setOnClickListener(null);
            }else{
                holder.forwardOwnFile.setOnClickListener(this);
            }
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);
            holder.forwardOwnContact.setVisibility(View.GONE);

            holder.previewFrameLand.setVisibility(View.GONE);
            holder.previewFramePort.setVisibility(View.GONE);

            holder.contentOwnMessageFileLayout.setVisibility(View.VISIBLE);
            holder.contentOwnMessageContactLayout.setVisibility(View.GONE);

            int status = message.getStatus();
            log("Status: " + message.getStatus());
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

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        holder.forwardOwnFile.setEnabled(false);
                        holder.forwardOwnPortrait.setEnabled(false);
                        holder.forwardOwnLandscape.setEnabled(false);
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardOwnFile.setEnabled(true);
                        holder.forwardOwnPortrait.setEnabled(true);
                        holder.forwardOwnLandscape.setEnabled(true);
                    }
                } else {
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardOwnFile.setEnabled(true);
                    holder.forwardOwnPortrait.setEnabled(true);
                    holder.forwardOwnLandscape.setEnabled(true);

                }
            } else {
                log("Multiselect ON");

                if (this.isItemChecked(position)) {
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    holder.forwardOwnFile.setEnabled(false);
                    holder.forwardOwnPortrait.setEnabled(false);
                    holder.forwardOwnLandscape.setEnabled(false);
                } else {
                    log("NOT selected");
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardOwnFile.setEnabled(true);
                    holder.forwardOwnPortrait.setEnabled(true);
                    holder.forwardOwnLandscape.setEnabled(true);
                }
            }

            MegaNodeList nodeList = message.getMegaNodeList();
            if (nodeList != null) {

                if (nodeList.size() == 1) {
                    MegaNode node = nodeList.get(0);
                    log("Node Name: " + node.getName());

                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        log("Landscape configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                        holder.contentOwnMessageFileName.setMaxWidth((int) width);
                        holder.contentOwnMessageFileSize.setMaxWidth((int) width);
                    } else {
                        log("Portrait configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                        holder.contentOwnMessageFileName.setMaxWidth((int) width);
                        holder.contentOwnMessageFileSize.setMaxWidth((int) width);
                    }
                    holder.contentOwnMessageFileName.setText(node.getName());

                    long nodeSize = node.getSize();
                    holder.contentOwnMessageFileSize.setText(Util.getSizeString(nodeSize));
                    holder.contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                    Bitmap preview = null;
                    if (node.hasPreview()) {

                        preview = PreviewUtils.getPreviewFromCache(node);
                        if (preview != null) {
                            PreviewUtils.previewCache.put(node.getHandle(), preview);
                            setOwnPreview(holder, preview, node);
                            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                setErrorStateOnPreview(holder, preview);
                            }

                        } else {
                            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                holder.ownTriangleIconFile.setVisibility(View.VISIBLE);
                                holder.retryAlert.setVisibility(View.VISIBLE);
                                holder.forwardOwnFile.setVisibility(View.GONE);
                                holder.forwardOwnPortrait.setVisibility(View.GONE);
                                holder.forwardOwnLandscape.setVisibility(View.GONE);
                            }
                            try {
                                new MegaChatLollipopAdapter.ChatPreviewAsyncTask(holder).execute(node);
                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                log("Too many AsyncTasks");
                            }


                        }
                    } else {
                        log("Node has no preview on servers");

                        preview = PreviewUtils.getPreviewFromCache(node);
                        if (preview != null) {

                            PreviewUtils.previewCache.put(node.getHandle(), preview);
                            if (preview.getWidth() < preview.getHeight()) {

                                log("Portrait");
                                holder.contentOwnMessageThumbPort.setImageBitmap(preview);
                                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    log("Is pfd preview");
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);
                                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    log("Is video preview");
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

                                    if (cC.isInAnonymousMode()) {
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
                                    holder.forwardOwnFile.setVisibility(View.GONE);
                                    holder.forwardOwnLandscape.setVisibility(View.GONE);
                                }

                                holder.ownTriangleIconFile.setVisibility(View.GONE);

                                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            } else {
                                log("Landscape");
                                holder.contentOwnMessageThumbLand.setImageBitmap(preview);
                                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    log("Is pfd preview");
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    log("Is video preview");
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

                                    if (cC.isInAnonymousMode()) {
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

                                    holder.forwardOwnFile.setVisibility(View.GONE);
                                    holder.forwardOwnPortrait.setVisibility(View.GONE);
                                }

                                holder.ownTriangleIconFile.setVisibility(View.GONE);
                                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                            }
                        }else {
                            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                holder.ownTriangleIconFile.setVisibility(View.VISIBLE);
                                holder.retryAlert.setVisibility(View.VISIBLE);
                                holder.forwardOwnFile.setVisibility(View.GONE);
                                holder.forwardOwnPortrait.setVisibility(View.GONE);
                                holder.forwardOwnLandscape.setVisibility(View.GONE);
                            }
                            try {
                                new MegaChatLollipopAdapter.ChatLocalPreviewAsyncTask(((ViewHolderMessageChat) holder)).execute(node);

                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                log("Too many AsyncTasks");
                            }
                        }
                    }
                } else {
                    long totalSize = 0;
                    int count = 0;
                    for (int i = 0; i < nodeList.size(); i++) {
                        MegaNode temp = nodeList.get(i);
                        if (!(megaChatApi.isRevoked(chatRoom.getChatId(), temp.getHandle()))) {
                            count++;
                            log("Node Name: " + temp.getName());
                            totalSize = totalSize + temp.getSize();
                        }
                    }

                    holder.contentOwnMessageFileSize.setText(Util.getSizeString(totalSize));


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
            log("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        holder.dateLayout.setVisibility(View.VISIBLE);
                        holder.dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        holder.titleContactMessage.setVisibility(View.VISIBLE);
                        holder.timeContactText.setText(TimeUtils.formatTime(message));
                        holder.timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleContactMessage.setVisibility(View.VISIBLE);
                        holder.timeContactText.setText(TimeUtils.formatTime(message));
                        holder.timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.timeContactText.setVisibility(View.GONE);
                        holder.titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);
            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
            holder.contactManagementMessageLayout.setVisibility(View.GONE);

            if (messages.get(position - 1).isShowAvatar()) {
                holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(holder, userHandle, ((ViewHolderMessageChat) holder).fullNameTitle, false);
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
            if (cC.isInAnonymousMode()) {
                holder.forwardContactFile.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactFile.setVisibility(View.VISIBLE);
            }
            if(isMultipleSelect()){
                holder.forwardContactFile.setOnClickListener(null);
            }else{
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

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                        holder.forwardContactFile.setEnabled(false);
                        holder.forwardContactPreviewPortrait.setEnabled(false);
                        holder.forwardContactPreviewLandscape.setEnabled(false);

                    } else {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardContactFile.setEnabled(true);
                        holder.forwardContactPreviewPortrait.setEnabled(true);
                        holder.forwardContactPreviewLandscape.setEnabled(true);
                    }
                } else {
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardContactFile.setEnabled(true);
                    holder.forwardContactPreviewPortrait.setEnabled(true);
                    holder.forwardContactPreviewLandscape.setEnabled(true);

                }
            } else {
                log("Multiselect ON");

                if (this.isItemChecked(position)) {
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    holder.forwardContactFile.setEnabled(false);
                    holder.forwardContactPreviewPortrait.setEnabled(false);
                    holder.forwardContactPreviewLandscape.setEnabled(false);

                } else {
                    log("NOT selected");
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardContactFile.setEnabled(true);
                    holder.forwardContactPreviewPortrait.setEnabled(true);
                    holder.forwardContactPreviewLandscape.setEnabled(true);
                }
            }

            MegaNodeList nodeList = message.getMegaNodeList();
            if (nodeList != null) {
                if (nodeList.size() == 1) {
                    MegaNode node = nodeList.get(0);
                    log("Node Name: " + node.getName());

                    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        log("Landscape configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                        holder.contentContactMessageFileName.setMaxWidth((int) width);
                        holder.contentContactMessageFileSize.setMaxWidth((int) width);
                    } else {
                        log("Portrait configuration");
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                        holder.contentContactMessageFileName.setMaxWidth((int) width);
                        holder.contentContactMessageFileSize.setMaxWidth((int) width);
                    }
                    holder.contentContactMessageFileName.setText(node.getName());
                    long nodeSize = node.getSize();
                    holder.contentContactMessageFileSize.setText(Util.getSizeString(nodeSize));
                    holder.contentContactMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                    log("Get preview of node");
                    Bitmap preview = null;
                    if (node.hasPreview()) {
                        log("Get preview of node");
                        preview = PreviewUtils.getPreviewFromCache(node);
                        if (preview != null) {
                            PreviewUtils.previewCache.put(node.getHandle(), preview);

                            if (preview.getWidth() < preview.getHeight()) {
                                log("Portrait");

                                holder.contentContactMessageThumbPort.setImageBitmap(preview);
                                holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    log("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);
                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    log("(1)Contact message - Is video preview");
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

                                if (cC.isInAnonymousMode()) {
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

//                                            if(chatRoom.isGroup()){
//                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
//                                                contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
//                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
////                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
//                                            }
//                                            else{
//                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
//                                                contactThumbParams.setMargins(0, 0 ,0, 0);
//                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
////                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
//                                            }
                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.getLayoutParams();
                                contactThumbParams.setMargins(0, 0, 0, 0);
                                holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                            } else {
                                log("Landscape");

                                holder.contentContactMessageThumbLand.setImageBitmap(preview);
                                holder.contentContactMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                                    log("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);
                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                                    log("(2)Contact message - Is video preview");
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

                                if (cC.isInAnonymousMode()) {
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

                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams) holder.contentContactMessageThumbLand.getLayoutParams();
                                contactThumbParams.setMargins(0, 0, 0, 0);
                                holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);

//                                            if(chatRoom.isGroup()){
//                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
//                                                contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
//                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
////                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
//                                            }
//                                            else{
//                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
//                                                contactThumbParams.setMargins(0, 0 ,0, 0);
//                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
////                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
//                                            }
                            }

                        } else {
                            try {
                                new MegaChatLollipopAdapter.ChatPreviewAsyncTask(((ViewHolderMessageChat) holder)).execute(node);

                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                log("Too many AsyncTasks");
                            }
                        }

                    } else {
                        log("Node has no preview on servers");

                        preview = PreviewUtils.getPreviewFromCache(node);
                        if (preview != null) {
                            PreviewUtils.previewCache.put(node.getHandle(), preview);
                            setContactPreview(holder, preview, node);
                        } else {

                            try {
                                new MegaChatLollipopAdapter.ChatLocalPreviewAsyncTask(((ViewHolderMessageChat) holder)).execute(node);

                            } catch (Exception ex) {
                                //Too many AsyncTasks
                                log("Too many AsyncTasks");
                            }
                        }
                    }
                } else {
                    long totalSize = 0;
                    int count = 0;
                    for (int i = 0; i < nodeList.size(); i++) {
                        MegaNode temp = nodeList.get(i);
                        if (!(megaChatApi.isRevoked(chatRoom.getChatId(), temp.getHandle()))) {
                            count++;
                            log("Node Name: " + temp.getName());
                            totalSize = totalSize + temp.getSize();
                        }
                    }
                    ((ViewHolderMessageChat) holder).contentContactMessageFileSize.setText(Util.getSizeString(totalSize));
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

    public void bindContactAttachmentMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        log("bindContactAttachmentMessage");

        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            holder.layoutAvatarMessages.setVisibility(View.GONE);
            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            log("MY message!!");
            log("MY message handle!!: " + message.getMsgId());

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        holder.dateLayout.setVisibility(View.VISIBLE);
                        holder.dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.VISIBLE);
                        holder.timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            holder.ownMessageLayout.setVisibility(View.VISIBLE);
            holder.contactMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.contentOwnMessageText.setVisibility(View.GONE);
            holder.previewFrameLand.setVisibility(View.GONE);
            holder.previewFramePort.setVisibility(View.GONE);
            holder.contentOwnMessageFileLayout.setVisibility(View.GONE);

            holder.forwardOwnFile.setVisibility(View.GONE);
            holder.forwardOwnPortrait.setVisibility(View.GONE);
            holder.forwardOwnLandscape.setVisibility(View.GONE);
            holder.forwardOwnRichLinks.setVisibility(View.GONE);

            holder.contentOwnMessageContactLayout.setVisibility(View.VISIBLE);
            holder.contentOwnMessageContactThumb.setVisibility(View.VISIBLE);
            holder.contentOwnMessageContactName.setVisibility(View.VISIBLE);
            holder.contentOwnMessageContactEmail.setVisibility(View.VISIBLE);

            int status = message.getStatus();
            log("Status: " + message.getStatus());
            if ((status == MegaChatMessage.STATUS_SERVER_REJECTED) || (status == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                holder.contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                holder.ownTriangleIconContact.setVisibility(View.VISIBLE);
                holder.retryAlert.setVisibility(View.VISIBLE);
                holder.forwardOwnContact.setVisibility(View.GONE);

            } else if (status == MegaChatMessage.STATUS_SENDING) {
                holder.contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                holder.retryAlert.setVisibility(View.GONE);
                holder.ownTriangleIconContact.setVisibility(View.GONE);
                holder.forwardOwnContact.setVisibility(View.GONE);

            } else {
                holder.contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                holder.retryAlert.setVisibility(View.GONE);
                holder.ownTriangleIconContact.setVisibility(View.GONE);

                if (cC.isInAnonymousMode()) {
                    holder.forwardOwnContact.setVisibility(View.GONE);
                }
                else {
                    holder.forwardOwnContact.setVisibility(View.VISIBLE);
                }
                if(isMultipleSelect()){
                    holder.forwardOwnContact.setOnClickListener(null);
                }else{
                    holder.forwardOwnContact.setOnClickListener(this);
                }
            }

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                log("Landscape configuration");
                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                holder.contentOwnMessageContactName.setMaxWidth((int) width);
                holder.contentOwnMessageContactEmail.setMaxWidth((int) width);
            } else {
                log("Portrait configuration");
                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                holder.contentOwnMessageContactName.setMaxWidth((int) width);
                holder.contentOwnMessageContactEmail.setMaxWidth((int) width);
            }

            long userCount = message.getUsersCount();

            if (userCount == 1) {
                String name = "";
                name = message.getUserName(0);
                if (name.trim().isEmpty()) {
                    name = message.getUserEmail(0);
                }
                String email = message.getUserEmail(0);
                holder.contentOwnMessageContactName.setText(name);
                holder.contentOwnMessageContactEmail.setText(email);
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
                createDefaultAvatar(holder, null, email, true, userCount);
            }

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                        holder.forwardOwnContact.setEnabled(false);
                    } else {
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardOwnContact.setEnabled(true);
                    }
                } else {
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardOwnContact.setEnabled(true);
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    holder.forwardOwnContact.setEnabled(false);
                } else {
                    log("NOT selected");
                    holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardOwnContact.setEnabled(true);
                }
            }
        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }
            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        holder.dateLayout.setVisibility(View.VISIBLE);
                        holder.dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        holder.titleContactMessage.setVisibility(View.VISIBLE);
                        holder.timeContactText.setText(TimeUtils.formatTime(message));
                        holder.timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.titleContactMessage.setVisibility(View.VISIBLE);
                        holder.timeContactText.setText(TimeUtils.formatTime(message));
                        holder.timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        holder.dateLayout.setVisibility(View.GONE);
                        holder.timeContactText.setVisibility(View.GONE);
                        holder.titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            holder.ownMessageLayout.setVisibility(View.GONE);
            holder.contactMessageLayout.setVisibility(View.VISIBLE);
            holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
            holder.contactManagementMessageLayout.setVisibility(View.GONE);

            if (messages.get(position - 1).isShowAvatar()) {
                holder.layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(holder, userHandle, holder.fullNameTitle, false);
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

            if (cC.isInAnonymousMode()) {
                holder.forwardContactContact.setVisibility(View.GONE);
            }
            else {
                holder.forwardContactContact.setVisibility(View.VISIBLE);
            }
            if(isMultipleSelect()){
                holder.forwardContactContact.setOnClickListener(null);
            }else{
                holder.forwardContactContact.setOnClickListener(this);
            }
            holder.contentContactMessageContactThumb.setVisibility(View.VISIBLE);
            holder.contentContactMessageContactName.setVisibility(View.VISIBLE);
            holder.contentContactMessageContactEmail.setVisibility(View.VISIBLE);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                log("Landscape configuration");
                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                holder.contentContactMessageContactName.setMaxWidth((int) width);
                holder.contentContactMessageContactEmail.setMaxWidth((int) width);
            } else {
                log("Portrait configuration");
                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                holder.contentContactMessageContactName.setMaxWidth((int) width);
                holder.contentContactMessageContactEmail.setMaxWidth((int) width);
            }

            long userCount = message.getUsersCount();

            if (userCount == 1) {
                String name = "";
                name = message.getUserName(0);
                if (name.trim().isEmpty()) {
                    name = message.getUserEmail(0);
                }
                String email = message.getUserEmail(0);
                log("Contact Name: " + name);
                holder.contentContactMessageContactName.setText(name);
                holder.contentContactMessageContactEmail.setText(email);
                setUserAvatar(holder, message);

            } else {
                //Show default avatar with userCount
                StringBuilder name = new StringBuilder("");
                name.append(message.getUserName(0));
                for (int i = 1; i < userCount; i++) {
                    name.append(", " + message.getUserName(i));
                }
                log("Names of attached contacts: " + name);
                holder.contentContactMessageContactName.setText(name);
                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int) userCount, userCount);
                holder.contentContactMessageContactEmail.setText(email);
                createDefaultAvatar(holder, null, email, false, userCount);
            }

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                        holder.forwardContactContact.setEnabled(false);

                    } else {
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        holder.forwardContactContact.setEnabled(true);
                    }
                }else {
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardContactContact.setEnabled(true);

                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    holder.forwardContactContact.setEnabled(false);

                } else {
                    log("NOT selected");
                    holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    holder.forwardContactContact.setEnabled(true);

                }
            }
        }
    }

    public void bindChangeTitleMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        log("bindChangeTitleMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);

        MegaChatMessage message = androidMessage.getMessage();

        if (message.getUserHandle() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            log("MY message!!");
            log("MY message handle!!: " + message.getMsgId());
            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            String messageContent = message.getContent();

            String textToShow = String.format(context.getString(R.string.change_title_messages),toCDATA( megaChatApi.getMyFullname()), toCDATA(messageContent));
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
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);

            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }
        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);
            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);

            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }

            String messageContent = message.getContent();

            String textToShow = String.format(context.getString(R.string.change_title_messages), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle), toCDATA(messageContent));
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
        log("bindChatLinkMessage");

        MegaChatMessage message = androidMessage.getMessage();
        long userHandle = message.getUserHandle();
        log("Contact message!!: " + userHandle);

        if(userHandle==megaChatApi.getMyUserHandle()){
            ((ViewHolderMessageChat) holder).fullNameTitle = megaChatApi.getMyFullname();
        }
        else{
            setContactMessageName(position, holder, userHandle, false);
        }

        ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
        }else{
            holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
        }

        if (messages.get(position - 1).getInfoToShow() != -1) {
            switch (messages.get(position - 1).getInfoToShow()) {
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                    log("CHAT_ADAPTER_SHOW_ALL");
                    ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context, message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                    ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                    ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                    break;
                }
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                    log("CHAT_ADAPTER_SHOW_TIME--");
                    ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                    ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                    break;
                }
                case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                    log("CHAT_ADAPTER_SHOW_NOTHING");
                    ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                    break;
                }
            }
        }

        ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

        ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

        if (!multipleSelect) {
            if (positionClicked != -1) {
                if (positionClicked == position) {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    listFragment.smoothScrollToPosition(positionClicked);
                } else {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            }
        } else {
            log("Multiselect ON");
            if (this.isItemChecked(position)) {
                holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
            } else {
                log("NOT selected");
                holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            }
        }

        ((ViewHolderMessageChat) holder).contactManagementAvatarMainLayout.setVisibility(View.VISIBLE);

        setContactAvatar(((ViewHolderMessageChat) holder), userHandle, toCDATA(((ViewHolderMessageChat) holder).fullNameTitle), true);

        String textToShow = "";
        int messageType = message.getType();
        if(messageType == MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE){
            textToShow = String.format(context.getString(R.string.message_created_chat_link), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            } catch (Exception e) {
            }
            ((ViewHolderMessageChat) holder).contactManagementAvatarSubTextLayout.setVisibility(View.GONE);
        }
        else if(messageType == MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE){
            textToShow = String.format(context.getString(R.string.message_deleted_chat_link), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            } catch (Exception e) {
            }
            ((ViewHolderMessageChat) holder).contactManagementAvatarSubTextLayout.setVisibility(View.GONE);
        }
        else{
            textToShow = String.format(context.getString(R.string.message_set_chat_private), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle));
            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<b>");
                textToShow = textToShow.replace("[/B]", "</b>");
            } catch (Exception e) {
            }
            ((ViewHolderMessageChat) holder).contactManagementAvatarSubTextLayout.setVisibility(View.VISIBLE);
        }

        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }

        ((ViewHolderMessageChat) holder).contactManagementAvatarMessageText.setText(result);
    }

    public void bindTruncateMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        log("bindTruncateMessage");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }
            log("MY message!!");
            log("MY message handle!!: " + message.getMsgId());

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
            String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(megaChatApi.getMyFullname()));
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
            ((ViewHolderMessageChat) holder).ownManagementMessageText.setText(result);

            ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).ownManagementMessageIcon.setVisibility(View.GONE);
            RelativeLayout.LayoutParams paramsOwnManagement = (RelativeLayout.LayoutParams) holder.ownManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsOwnManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.ownManagementMessageText.setLayoutParams(paramsOwnManagement);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }
        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);

            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageIcon.setVisibility(View.GONE);

            RelativeLayout.LayoutParams paramsContactManagement = (RelativeLayout.LayoutParams) holder.contactManagementMessageText.getLayoutParams();
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics);
            }else{
                paramsContactManagement.leftMargin = Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics);
            }
            holder.contactManagementMessageText.setLayoutParams(paramsContactManagement);

            ((ViewHolderMessageChat) holder).nameContactText.setVisibility(View.GONE);

            if (!multipleSelect) {
                if (positionClicked != -1) {
                    if (positionClicked == position) {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                        listFragment.smoothScrollToPosition(positionClicked);
                    } else {
                        holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            } else {
                log("Multiselect ON");
                if (this.isItemChecked(position)) {
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                } else {
                    log("NOT selected");
                    holder.contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }

            String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(((ViewHolderMessageChat) holder).fullNameTitle));
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
        log("bindRevokeNodeMessage()");
        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
        MegaChatMessage message = androidMessage.getMessage();

        if (message.getUserHandle() == myUserHandle) {
            log("MY message!!");
            log("MY message handle!!: " + message.getMsgId());

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.LEFT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND, outMetrics),0,0,0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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

            ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactEmail.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
            messageContent = "Attachment revoked";
            ((ViewHolderMessageChat) holder).contentOwnMessageText.setText(messageContent);


        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);
            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(MANAGEMENT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            ((ViewHolderMessageChat) holder).ownMessageLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contactMessageLayout.setVisibility(View.VISIBLE);

            if (messages.get(position - 1).isShowAvatar()) {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                setContactAvatar(((ViewHolderMessageChat) holder), userHandle, ((ViewHolderMessageChat) holder).fullNameTitle, false);
            } else {
                ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.GONE);
            }

            ((ViewHolderMessageChat) holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contactManagementMessageLayout.setVisibility(View.GONE);

            String messageContent = "";

            ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);

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
        log("hideMessage");
        ((ViewHolderMessageChat) holder).itemLayout.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);
        params.height = 0;
        ((ViewHolderMessageChat) holder).itemLayout.setLayoutParams(params);
    }

    public void bindNoTypeMessage(ViewHolderMessageChat holder, AndroidMegaChatMessage androidMessage, int position) {
        log("bindNoTypeMessage()");

        ((ViewHolderMessageChat) holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
        MegaChatMessage message = androidMessage.getMessage();
        if (message.getUserHandle() == myUserHandle) {
            log("MY message handle!!: " + message.getMsgId());

            ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                ((ViewHolderMessageChat) holder).titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeOwnText.setText(TimeUtils.formatTime(message));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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

            ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

        } else {
            long userHandle = message.getUserHandle();
            log("Contact message!!: " + userHandle);
            setContactMessageName(position, holder, userHandle, true);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_LAND,outMetrics),0,0,0);
            }else{
                holder.titleContactMessage.setPadding(Util.scaleWidthPx(CONTACT_MESSAGE_PORT, outMetrics),0,0,0);
            }

            if (messages.get(position - 1).getInfoToShow() != -1) {
                switch (messages.get(position - 1).getInfoToShow()) {
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL: {
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).dateText.setText(TimeUtils.formatDate(context,message.getTimestamp(), TimeUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME: {
                        log("CHAT_ADAPTER_SHOW_TIME--");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).timeContactText.setText(TimeUtils.formatTime(message));
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING: {
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat) holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).timeContactText.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleContactMessage.setVisibility(View.GONE);
                        break;
                    }
                }
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
        log("setUserAvatar");

        String name = "";
        name = message.getUserName(0);
        if (name.trim().isEmpty()) {
            name = message.getUserEmail(0);
        }
        String email = message.getUserEmail(0);
        log("Contact Name: " + name);
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(message.getUserHandle(0));

        ChatAttachmentAvatarListener listener;
        if (myUserHandle == message.getUserHandle()) {
            createDefaultAvatar(holder, userHandleEncoded, name, true,0);
            listener = new ChatAttachmentAvatarListener(context, holder, this, true);
        } else {
            createDefaultAvatar(holder, userHandleEncoded, name, false, 0);
            listener = new ChatAttachmentAvatarListener(context, holder, this, false);
        }

        File avatar = null;
        if (context.getExternalCacheDir() != null) {
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), email + ".jpg");
        } else {
            avatar = new File(context.getCacheDir().getAbsolutePath(), email + ".jpg");
        }
        Bitmap bitmap = null;
        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();

                    if (megaApi == null) {
                        log("setUserAvatar: megaApi is Null in Offline mode");
                        return;
                    }

                    if (context.getExternalCacheDir() != null) {
                        megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                    } else {
                        megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                    }
                } else {
                    if (myUserHandle == message.getUserHandle()) {
                        holder.contentOwnMessageContactInitialLetter.setVisibility(View.GONE);
                        holder.contentOwnMessageContactThumb.setImageBitmap(bitmap);
                    } else {
                        holder.contentContactMessageContactInitialLetter.setVisibility(View.GONE);
                        holder.contentContactMessageContactThumb.setImageBitmap(bitmap);
                    }
                }
            } else {
                if (megaApi == null) {
                    log("setUserAvatar: megaApi is Null in Offline mode");
                    return;
                }

                if (context.getExternalCacheDir() != null) {
                    megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                } else {
                    megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                }
            }
        } else {
            if (megaApi == null) {
                log("setUserAvatar: megaApi is Null in Offline mode");
                return;
            }

            if (context.getExternalCacheDir() != null) {
                megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
            } else {
                megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
            }
        }
    }

    public void setContactAvatar(ViewHolderMessageChat holder, long userHandle, String name, boolean isManagement) {
        log("setContactAvatar");

        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
        createDefaultAvatarContact(holder, userHandleEncoded, name, isManagement);
        ChatAttachmentAvatarListener listener = new ChatAttachmentAvatarListener(context, holder, this, false);

        File avatar = null;
        if (context.getExternalCacheDir() != null) {
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), userHandleEncoded + ".jpg");
        } else {
            avatar = new File(context.getCacheDir().getAbsolutePath(), userHandleEncoded + ".jpg");
        }

        Bitmap bitmap = null;
        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();

                    if (megaApi == null) {
                        log("setUserAvatar: megaApi is Null in Offline mode");
                        return;
                    }

                    if (context.getExternalCacheDir() != null) {
                        megaApi.getUserAvatar(userHandleEncoded, context.getExternalCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                    } else {
                        megaApi.getUserAvatar(userHandleEncoded, context.getCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                    }
                } else {
                    if(isManagement){
                        holder.contactManagementAvatarInitialLetter.setVisibility(View.GONE);
                        holder.contactManagementAvatarImage.setImageBitmap(bitmap);
                    }
                    else{
                        holder.contactInitialLetter.setVisibility(View.GONE);
                        holder.contactImageView.setImageBitmap(bitmap);
                    }
                }
            } else {
                if (megaApi == null) {
                    log("setUserAvatar: megaApi is Null in Offline mode");
                    return;
                }

                if (context.getExternalCacheDir() != null) {
                    megaApi.getUserAvatar(userHandleEncoded, context.getExternalCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                } else {
                    megaApi.getUserAvatar(userHandleEncoded, context.getCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                }
            }
        } else {

            if (megaApi == null) {
                log("setUserAvatar: megaApi is Null in Offline mode");
                return;
            }

            if (context.getExternalCacheDir() != null) {
                megaApi.getUserAvatar(userHandleEncoded, context.getExternalCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
            } else {
                megaApi.getUserAvatar(userHandleEncoded, context.getCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
            }
        }
    }

    public void createDefaultAvatarContact(ViewHolderMessageChat holder, String userHandle, String name, boolean isManagement) {
        log("createDefaultAvatarContact");
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        String color = megaApi.getUserAvatarColor(userHandle);
        if (userHandle != null) {
            if (color != null) {
                log("The color to set the avatar is " + color);
                p.setColor(Color.parseColor(color));
            } else {
                log("Default color to the avatar");
                p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
            }
        } else {
            p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth() / 2;
        else
            radius = defaultAvatar.getHeight() / 2;

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if(isManagement){
            holder.contactManagementAvatarImage.setImageBitmap(defaultAvatar);

            if (name != null) {
                if (name.trim().length() > 0) {
                    String firstLetter = name.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    holder.contactManagementAvatarInitialLetter.setText(firstLetter);
                    holder.contactManagementAvatarInitialLetter.setTextColor(Color.WHITE);
                    holder.contactManagementAvatarInitialLetter.setVisibility(View.VISIBLE);
                }
            }
        }
        else{
            holder.contactImageView.setImageBitmap(defaultAvatar);

            if (name != null) {
                if (name.trim().length() > 0) {
                    String firstLetter = name.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    holder.contactInitialLetter.setText(firstLetter);
                    holder.contactInitialLetter.setTextColor(Color.WHITE);
                    holder.contactInitialLetter.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void createGroupChatAvatar(ViewHolderMessageChat holder, String title, boolean isMyMsg){
        log("createGroupChatAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(ContextCompat.getColor(context,R.color.divider_upgrade_account));

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);

        if(isMyMsg){
            holder.urlOwnMessageGroupAvatar.setImageBitmap(defaultAvatar);
        }
        else{
            holder.urlContactMessageGroupAvatar.setImageBitmap(defaultAvatar);
        }

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float density  = context.getResources().getDisplayMetrics().density;

        if (title.length() > 0){
            String chatTitle = title.trim();
            String firstLetter = chatTitle.charAt(0) + "";
            firstLetter = firstLetter.toUpperCase(Locale.getDefault());
            if(firstLetter.trim().isEmpty()){
                if(isMyMsg){
                    holder.urlOwnMessageGroupAvatarText.setVisibility(View.INVISIBLE);
                }
                else{
                    holder.urlContactMessageGroupAvatarText.setVisibility(View.INVISIBLE);
                }
            }
            else{
                log("Group chat initial letter is: "+firstLetter);
                if(firstLetter.equals("(")){
                    if(isMyMsg){
                        holder.urlOwnMessageGroupAvatarText.setVisibility(View.INVISIBLE);
                    }
                    else{
                        holder.urlContactMessageGroupAvatarText.setVisibility(View.INVISIBLE);
                    }
                }
                else{
                    if(isMyMsg){
                        holder.urlOwnMessageGroupAvatarText.setText(firstLetter);
                        holder.urlOwnMessageGroupAvatarText.setTextColor(Color.WHITE);
                        holder.urlOwnMessageGroupAvatarText.setVisibility(View.VISIBLE);
                        holder.urlOwnMessageGroupAvatarText.setTextSize(32);
                    }
                    else{
                        holder.urlContactMessageGroupAvatarText.setText(firstLetter);
                        holder.urlContactMessageGroupAvatarText.setTextColor(Color.WHITE);
                        holder.urlContactMessageGroupAvatarText.setVisibility(View.VISIBLE);
                        holder.urlContactMessageGroupAvatarText.setTextSize(32);
                    }
                }
            }
        }
    }

    public void createDefaultAvatar(ViewHolderMessageChat holder, String userHandle, String name, boolean isMyMsg, long userCount) {
        log(" createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        String color = megaApi.getUserAvatarColor(userHandle);
        if (userHandle != null) {
            if (color != null) {
                log("The color to set the avatar is " + color);
                p.setColor(Color.parseColor(color));
            } else {
                log("Default color to the avatar");
                p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
            }
        } else {
            p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
        }


        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth() / 2;
        else
            radius = defaultAvatar.getHeight() / 2;

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (isMyMsg) {
            holder.contentOwnMessageContactThumb.setImageBitmap(defaultAvatar);
            if(userHandle!=null){
                if (name != null) {
                    if (name.trim().length() > 0) {
                        String firstLetter = name.charAt(0) + "";
                        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                        holder.contentOwnMessageContactInitialLetter.setText(firstLetter);
                        holder.contentOwnMessageContactInitialLetter.setTextColor(Color.WHITE);
                        holder.contentOwnMessageContactInitialLetter.setVisibility(View.VISIBLE);
                    }
                    holder.contentOwnMessageContactInitialLetter.setTextSize(24);

                }
            }else{
                if (name != null) {
                    String firstLetter = userCount + "";

//                    holder.contentOwnMessageContactInitialLetter.setText(name);
                    holder.contentOwnMessageContactInitialLetter.setText(firstLetter);
                    holder.contentOwnMessageContactInitialLetter.setTextColor(Color.WHITE);
                    holder.contentOwnMessageContactInitialLetter.setVisibility(View.VISIBLE);
                    holder.contentOwnMessageContactInitialLetter.setTextSize(24);

                }
            }

        } else {
            holder.contentContactMessageContactThumb.setImageBitmap(defaultAvatar);

            if(userHandle!=null){
                if (name != null) {
                    if (name.trim().length() > 0) {
                        String firstLetter = name.charAt(0) + "";
                        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                        holder.contentContactMessageContactInitialLetter.setText(firstLetter);
                        holder.contentContactMessageContactInitialLetter.setTextColor(Color.WHITE);
                        holder.contentContactMessageContactInitialLetter.setVisibility(View.VISIBLE);
                    }
                    holder.contentContactMessageContactInitialLetter.setTextSize(24);

                }
            }else{
                if (name != null) {
//                    holder.contentContactMessageContactInitialLetter.setText(name);
                    String firstLetter = userCount + "";
                    holder.contentContactMessageContactInitialLetter.setText(firstLetter);
                    holder.contentContactMessageContactInitialLetter.setTextColor(Color.WHITE);
                    holder.contentContactMessageContactInitialLetter.setVisibility(View.VISIBLE);
                    holder.contentContactMessageContactInitialLetter.setTextSize(24);

                }
            }

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
        log("isMultipleSelect");
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        log("setMultipleSelect");
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
            notifyDataSetChanged();
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }

    public void toggleSelection(int pos) {
        log("toggleSelection");

        if (selectedItems.get(pos, false)) {
            log("delete pos: " + pos);
            selectedItems.delete(pos);
        } else {
            log("PUT pos: " + pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
        if (selectedItems.size() <= 0){
                    ((ChatActivityLollipop) context).hideMultipleSelect();

        }
//        ((ChatActivityLollipop) context).hideMultipleSelect();

        }





    public void selectAll() {
        for (int i = 0; i < this.getItemCount(); i++) {
            if (!isItemChecked(i)) {
                toggleSelection(i);
            }
        }
    }

    public void updateSelectionOnScroll() {
        log("updateSelectionOnScroll");

        List<Integer> selected = getSelectedItems();
        selectedItems.clear();

        for (int i = 0; i < selected.size(); i++) {
            int pos = selected.get(i);
            selectedItems.put(pos + 1, true);
            notifyItemChanged(pos);
            notifyItemChanged(pos + 1);
        }
    }

    public void clearSelections() {
        log("clearSelection");
        for (int i= 0; i<this.getItemCount();i++){
            if(isItemChecked(i)){
                toggleSelection(i);
            }
        }
//        if (selectedItems != null) {
//            selectedItems.clear();
//        }
//        notifyDataSetChanged();
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
     * Get request at specified position
     */
    public AndroidMegaChatMessage getMessageAt(int position) {
        try {
            if (messages != null) {
                return messages.get(position - 1);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    /*
     * Get list of all selected chats
     */
    public ArrayList<AndroidMegaChatMessage> getSelectedMessages() {
        ArrayList<AndroidMegaChatMessage> messages = new ArrayList<AndroidMegaChatMessage>();

        if(selectedItems!=null){
            for (int i = 0; i < selectedItems.size(); i++) {
                if (selectedItems.valueAt(i) == true) {

                    AndroidMegaChatMessage m = getMessageAt(selectedItems.keyAt(i));
                    if (m != null) {
                        messages.add(m);
                    }
                }
            }
        }

        return messages;
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
        log("addMessage: " + position);
        this.messages = messages;
        notifyItemInserted(position);
        if (position == messages.size()) {
            log("No need to update more");
        } else {
            log("Update until end");
            int itemCount = messages.size() - position;
            log("itemCount: " + itemCount);
            notifyItemRangeChanged(position, itemCount + 1);
        }
    }

    public void removeMessage(int position, ArrayList<AndroidMegaChatMessage> messages) {
        log("removeMessage: size: " + messages.size());
        this.messages = messages;
        notifyItemRemoved(position);

        if (position == messages.size()) {
            log("No need to update more");
        } else {
            log("Update until end");
            int itemCount = messages.size() - position;
            log("itemCount: " + itemCount);
            notifyItemRangeChanged(position, itemCount);
        }
    }

    public void loadPreviousMessage(ArrayList<AndroidMegaChatMessage> messages) {
        this.messages = messages;
        notifyItemInserted(0);
    }

    public void loadPreviousMessages(ArrayList<AndroidMegaChatMessage> messages, int counter) {
        log("loadPreviousMessages: " + counter);
        this.messages = messages;
        notifyItemRangeInserted(0, counter);
    }

    private void setErrorStateOnPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap) {
        log("setErrorStateOnPreview()");
        //Error
        holder.uploadingProgressBarPort.setVisibility(View.GONE);
        holder.uploadingProgressBarLand.setVisibility(View.GONE);

        String name = holder.contentOwnMessageFileName.getText().toString();

        if (bitmap.getWidth() < bitmap.getHeight()) {
            log("Portrait");

            holder.errorUploadingLandscape.setVisibility(View.GONE);
            holder.transparentCoatingLandscape.setVisibility(View.GONE);
            holder.ownTriangleIconFile.setVisibility(View.GONE);
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
            log("Landscape");
            holder.errorUploadingPortrait.setVisibility(View.GONE);
            holder.transparentCoatingPortrait.setVisibility(View.GONE);
            holder.ownTriangleIconFile.setVisibility(View.GONE);

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
        log("setOwnPreview()");

        if (holder != null) {
            if (bitmap.getWidth() < bitmap.getHeight()) {
                log("Portrait");

                holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                    log("Is pfd preview");
                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                    log("Is video preview");
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
                if (cC.isInAnonymousMode()) {
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
                log("Landscape");

                holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                    log("Is pfd preview");
                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);

                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                    log("Is video preview");
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
                if (cC.isInAnonymousMode()) {
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
        log("setContactPreview()");
        if (bitmap.getWidth() < bitmap.getHeight()) {
            log("Portrait");
            holder.contentContactMessageThumbPort.setImageBitmap(bitmap);
            holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                log("Contact message - Is pfd preview");
                holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                log("(3)Contact message - Is video preview");
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

            if (cC.isInAnonymousMode()) {
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
//                                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
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
            log("Landscape");

            holder.contentContactMessageThumbLand.setImageBitmap(bitmap);
            holder.contentContactMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (MimeTypeList.typeForName(node.getName()).isPdf()) {
                log("Contact message - Is pfd preview");
                holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);

                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            } else if (MimeTypeList.typeForName(node.getName()).isVideo()) {
                log("(4)Contact message - Is video preview");
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

            if (cC.isInAnonymousMode()) {
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
//                                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics),0, 0);
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

    private void setPreview(long handle, MegaChatLollipopAdapter.ViewHolderMessageChat holder, MegaNode node) {
        log("setPreview() handle: " + handle);

        if (holder != null) {
            File previewDir = PreviewUtils.getPreviewFolder(context);
            String base64 = MegaApiJava.handleToBase64(handle);
            File preview = new File(previewDir, base64 + ".jpg");
            if (preview.exists()) {

                if (preview.length() > 0) {
                    Bitmap bitmap = PreviewUtils.getBitmapForCache(preview, context);

                    if (bitmap != null) {

                        PreviewUtils.previewCache.put(handle, bitmap);

                        if (holder.userHandle == megaChatApi.getMyUserHandle()) {

                            String name = holder.contentOwnMessageFileName.getText().toString();

                            log("Update my preview: " + name);
                            if (bitmap.getWidth() < bitmap.getHeight()) {
                                log("Portrait");

                                holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    log("Is pfd preview");
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    log("Is video preview");

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

                                if (cC.isInAnonymousMode()) {
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
                                log("Landscape");
                                holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    log("Is pfd preview");
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);

                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    log("Is video preview");
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

                                if (cC.isInAnonymousMode()) {
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
                            log("Update my contacts preview");
                            String name = holder.contentContactMessageFileName.getText().toString();

                            if (bitmap.getWidth() < bitmap.getHeight()) {

                                log("Portrait");
                                holder.contentContactMessageThumbPort.setImageBitmap(bitmap);
                                holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    log("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    log("(5)Contact message - Is video preview");

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
                                if (cC.isInAnonymousMode()) {
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
//                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
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

                                log("Landscape");
                                holder.contentContactMessageThumbLand.setImageBitmap(bitmap);
                                holder.contentContactMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(name).isPdf()) {
                                    log("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);

                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                                } else if (MimeTypeList.typeForName(name).isVideo()) {
                                    log("(6)Contact message - Is video preview");
                                    holder.gradientContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.VISIBLE);

                                    if (node != null) {
                                        holder.videoTimecontentContactMessageThumbLand.setText(timeVideo(node));
                                        holder.videoTimecontentContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    } else {
                                        log("setPreview:Landscape:Node is NULL");
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

                                if (cC.isInAnonymousMode()) {
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
//                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics),0, 0);
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
                log("Preview not exists");
            }
        }
    }

    private void setUploadingPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap) {
        log("setUploadingPreview() " + holder.filePathUploading);

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
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_LAND, outMetrics),0);
            }else{
                holder.titleOwnMessage.setPadding(0,0,Util.scaleWidthPx(PADDING_RIGHT_HOUR_OF_OWN_MESSAGE_PORT, outMetrics),0);
            }

            if (bitmap != null) {
                log("Bitmap not null - Update uploading my preview");

                int currentPosition = holder.getLayoutPosition();
                log("currentPosition holder: " + currentPosition);

                if (currentPosition == -1) {
                    log("The position cannot be recovered - had changed");
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        AndroidMegaChatMessage message = messages.get(i);
                        if (message.isUploading()) {
                            String path = message.getPendingMessage().getFilePath();
                            if (path.equals(holder.filePathUploading)) {
                                currentPosition = i + 1;
                                log("Found current position: " + currentPosition);
                                break;
                            }
                        }
                    }
                }
                log("Messages size: " + messages.size());
                if (currentPosition > messages.size()) {
                    log("Position not valid");
                    return;
                }

                AndroidMegaChatMessage message = messages.get(currentPosition - 1);
                if (message.getPendingMessage() != null) {
                    log("State of the message: " + message.getPendingMessage().getState());
                    log("Attachment: " + message.getPendingMessage().getFilePath());

                    if (bitmap.getWidth() < bitmap.getHeight()) {
                        log("Portrait show preview");
                        holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                        holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);
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
                        log("Landscape show preview");
                        holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                        holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);
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
                        log("Message is on ERROR state");
                        holder.urlOwnMessageLayout.setVisibility(View.GONE);
                        String name = holder.contentOwnMessageFileName.getText().toString();

                        //Error
                        holder.uploadingProgressBarPort.setVisibility(View.GONE);
                        holder.uploadingProgressBarLand.setVisibility(View.GONE);
                        holder.ownTriangleIconFile.setVisibility(View.GONE);

                        holder.retryAlert.setVisibility(View.VISIBLE);

                        if (bitmap.getWidth() < bitmap.getHeight()) {
                            log("Portrait");
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
                            log("Landscape");
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
                        log("Message is in progress state");
                        holder.urlOwnMessageLayout.setVisibility(View.GONE);

                        //In progress
                        holder.retryAlert.setVisibility(View.GONE);
                        holder.ownTriangleIconFile.setVisibility(View.GONE);
                        holder.errorUploadingPortrait.setVisibility(View.GONE);
                        holder.errorUploadingLandscape.setVisibility(View.GONE);

                        if (bitmap.getWidth() < bitmap.getHeight()) {
                            log("Portrait");
                            holder.transparentCoatingLandscape.setVisibility(View.GONE);
                            holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarPort.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarLand.setVisibility(View.GONE);
                        } else {
                            log("Landscape");
                            holder.transparentCoatingPortrait.setVisibility(View.GONE);
                            holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarLand.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarPort.setVisibility(View.GONE);
                        }
                    }
                } else {

                    log("The pending message is NULL-- cannot set preview");
                }
            } else {
                log("Bitmap is NULL");
            }
        } else {
            log("Holder is NULL");
        }
    }

    private static void log(String log) {
        Util.log("MegaChatLollipopAdapter", log);
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

            log("onRequestFinish: " + request.getType() + "__" + request.getRequestString());
            log("onRequestFinish: Node: " + request.getNodeHandle() + "_" + request.getName());

            if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE) {
                if (e.getErrorCode() == MegaError.API_OK) {

                    long handle = request.getNodeHandle();
                    pendingPreviews.remove(handle);

                    setPreview(handle, holder, node);
                } else {
                    log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
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
        log("timeVideo");
        if(n!=null){
            int duration = n.getDuration();
            String timeString = "";
            log("duration: "+n.getDuration());
            if (duration > 0) {
                int hours = duration / 3600;
                int minutes = (duration % 3600) / 60;
                int seconds = duration % 60;

                if (hours > 0) {
                    timeString = String.format("%d:%d:%02d", hours, minutes, seconds);
                } else {
                    timeString = String.format("%d:%02d", minutes, seconds);
                }

                log("The duration is: " + hours + " " + minutes + " " + seconds);
            }
            return timeString;
        }
        return "";
    }

    public void checkItem (View v, ViewHolderMessageChat holder, int[] screenPosition, int[] dimens) {

        ImageView imageView = null;

        int position = holder.getCurrentPosition();
        if(position<=messages.size()){
            AndroidMegaChatMessage message = messages.get(position - 1);
            if (message.getMessage() != null) {
                if (message.getMessage().getMegaNodeList() != null) {
                    if (message.getMessage().getMegaNodeList().get(0) != null) {
                        if (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                            if (holder.contentOwnMessageThumbPort.getVisibility() == View.VISIBLE) {
                                log("checkItem: contentOwnMessageThumbPort");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_own_message_thumb_portrait);
                            }
                            else if (holder.contentOwnMessageThumbLand.getVisibility() == View.VISIBLE) {
                                log("checkItem: contentOwnMessageThumbLand");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_own_message_thumb_landscape);
                            }
                            else if (holder.contentOwnMessageFileThumb.getVisibility() == View.VISIBLE) {
                                log("checkItem: contentOwnMessageFileThumb");
                                imageView = (ImageView) v.findViewById(R.id.content_own_message_file_thumb);
                            }
                        }
                        else {
                            if (holder.contentContactMessageThumbPort.getVisibility() == View.VISIBLE) {
                                log("checkItem: contentContactMessageThumbPort");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_contact_message_thumb_portrait);
                            }
                            else if (holder.contentContactMessageThumbLand.getVisibility() == View.VISIBLE) {
                                log("checkItem: contentContactMessageThumbLand");
                                imageView = (RoundedImageView) v.findViewById(R.id.content_contact_message_thumb_landscape);
                            }
                            else if (holder.contentContactMessageFileThumb.getVisibility() == View.VISIBLE) {
                                log("checkItem: contentContactMessageFileThumb");
                                imageView = (ImageView) v.findViewById(R.id.content_contact_message_file_thumb);
                            }
                        }
                    }
                }
            }
        }else{
            log("messages removed");
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
        log("setNodeAttachmentVisibility: "+position);
        if (holder != null) {
            holder.contentVisible = visible;
            notifyItemChanged(position);
        }
    }

    @Override
    public void onClick(View v) {
        log("onClick()");

        ViewHolderMessageChat holder = (ViewHolderMessageChat) v.getTag();
        int currentPosition = holder.getAdapterPosition();
        log("onClick -> Current position: "+currentPosition);

        if(currentPosition<0){
            log("Current position error - not valid value");
            return;
        }

        switch (v.getId()) {
            case R.id.forward_own_rich_links:
            case R.id.forward_own_contact:
            case R.id.forward_own_file:
            case R.id.forward_own_preview_portrait:
            case R.id.forward_own_preview_landscape:
            case R.id.forward_contact_rich_links:
            case R.id.forward_contact_contact:
            case R.id.forward_contact_file:
            case R.id.forward_contact_preview_portrait:
            case R.id.forward_contact_preview_landscape:{
                ArrayList<AndroidMegaChatMessage> messageArray = new ArrayList<>();
                messageArray.add(messages.get(currentPosition - 1));
                ((ChatActivityLollipop) context).forwardMessages(messageArray);
                break;
            }
            case R.id.content_own_message_text:
            case R.id.content_contact_message_text:
            case R.id.url_own_message_text:
            case R.id.url_contact_message_text:
            case R.id.message_chat_item_layout:{
                int[] screenPosition = new int[2];
                int [] dimens = new int[4];
                checkItem(v, holder, screenPosition, dimens);
//                ((ChatActivityLollipop) context).itemClick(currentPosition, dimens, v);
                ((ChatActivityLollipop) context).itemClick(currentPosition, dimens);
                break;
            }
            case R.id.url_always_allow_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = Constants.RICH_WARNING_FALSE;
                megaApi.enableRichPreviews(true);
                this.notifyItemChanged(currentPosition);
                break;
            }
            case R.id.url_no_disable_button:
            case R.id.url_not_now_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = Constants.RICH_WARNING_FALSE;
                int counter = MegaApplication.getCounterNotNowRichLinkWarning();
                if (counter < 1) {
                    counter = 1;
                } else {
                    counter++;
                }
                megaApi.setRichLinkWarningCounterValue(counter);
                this.notifyItemChanged(currentPosition);
                break;
            }
            case R.id.url_never_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = Constants.RICH_WARNING_CONFIRMATION;
                this.notifyItemChanged(currentPosition);
                break;
            }
            case R.id.url_yes_disable_button: {
                ((ChatActivityLollipop) context).showRichLinkWarning = Constants.RICH_WARNING_FALSE;
                megaApi.enableRichPreviews(false);
                this.notifyItemChanged(currentPosition);
                break;
            }

        }

//        this.notifyItemChanged(currentPosition);
    }

    @Override
    public boolean onLongClick(View view) {
        log("OnLongCLick");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }
        ViewHolderMessageChat holder = (ViewHolderMessageChat) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        if (!isMultipleSelect()){
            if(currentPosition<1){
                log("Position not valid: "+currentPosition);
            }else{
                log("Position valid: "+currentPosition);

                if(!messages.get(currentPosition-1).isUploading()){
                    if(MegaApplication.isShowInfoChatMessages()){
                        ((ChatActivityLollipop) context).showMessageInfo(currentPosition);
                    }else{
                        AndroidMegaChatMessage messageR = messages.get(currentPosition-1);
                        if(messageR.getMessage().getType() == MegaChatMessage.TYPE_CONTAINS_META){
                            log("TYPE_CONTAINS_META ");

                            MegaChatContainsMeta meta = messageR.getMessage().getContainsMeta();
                            if(meta==null){
                            }else if(meta!=null && meta.getType()==MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW){
//                                setMultipleSelect(true);
                                ((ChatActivityLollipop) context).activateActionMode();
                                ((ChatActivityLollipop) context).itemClick(currentPosition, null);

//
//                                if(currentPosition<1){
//                                    log("Position not valid");
//                                }else{
//
//                                }
                            }else{
                            }
                        }else{
                            log("OTHER TYPE ");

 //                           setMultipleSelect(true);
                            ((ChatActivityLollipop) context).activateActionMode();
                            ((ChatActivityLollipop) context).itemClick(currentPosition, null);

//
//                            if(currentPosition<1){
//                                log("Position not valid");
//                            }else{
//                                ((ChatActivityLollipop) context).itemClick(currentPosition);
//                            }
                        }
                    }
                }else{
                    log("message uploading ");

                }
            }
        }
        return true;
    }

    void setContactMessageName(int pos, ViewHolderMessageChat holder, long handle, boolean visibility) {
        if (isHolderNull(pos, holder)) {
            return;
        }

        if (!visibility) {
            holder.fullNameTitle = getContactMessageName(pos, holder, handle);
            return;
        }

        if (chatRoom.isGroup()) {
            holder.fullNameTitle = getContactMessageName(pos, holder, handle);
            holder.nameContactText.setVisibility(View.VISIBLE);
            holder.nameContactText.setText(holder.fullNameTitle);
        }
        else {
            holder.fullNameTitle = chatRoom.getTitle();
            holder.nameContactText.setVisibility(View.GONE);
        }
    }

    String getContactMessageName(int pos, ViewHolderMessageChat holder, long handle) {
        if (isHolderNull(pos, holder)) {
            return null;
        }

        String name = cC.getFullName(handle, chatRoom);

        if (name == null) {
            name = "";
        }

        if (name.trim().length() <= 0) {
            log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
            name = "Unknown name";
            if (!holder.nameRequestedAction) {
                log("3-Call for nonContactName: " + handle);
                holder.nameRequestedAction = true;
                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, handle, chatRoom.isPreview(), pos);
                megaChatApi.getUserFirstname(handle, chatRoom.getAuthorizationToken(), listener);
                megaChatApi.getUserLastname(handle, chatRoom.getAuthorizationToken(), listener);
                megaChatApi.getUserEmail(handle, listener);
            }
            else {
                log("4-Name already asked and no name received: " + handle);
            }
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
 }
