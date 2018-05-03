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
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import mega.privacy.android.app.components.WrapEmojiconTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatAttachmentAvatarListener;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.PendingMessage;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.RTFFormatter;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUtilsAndroid;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    public static int MAX_WIDTH_NAME_SENDER_GROUP_THUMB_LAND_PICTURE=194;
    public static int MAX_WIDTH_NAME_SENDER_GROUP_THUMB_PORTRAIT_PICTURE=136;

    public static int MAX_WIDTH_FILENAME_LAND=475;
    public static int MAX_WIDTH_FILENAME_PORT=200;

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

    private class ChatPreviewAsyncTask extends AsyncTask<MegaNode, Void, Integer>{

        MegaChatLollipopAdapter.ViewHolderMessageChat holder;
        MegaNode node;
        Bitmap preview;

        public ChatPreviewAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder) {
            this.holder = holder;
        }

        @Override
        protected Integer doInBackground(MegaNode... params){
            node = params[0];
            preview = PreviewUtils.getPreviewFromFolder(node, context);

            if (preview != null){
                PreviewUtils.previewCache.put(node.getHandle(), preview);
                return 0;
            }
            else{
                if (pendingPreviews.contains(node.getHandle())){
                    log("the preview is already downloaded or added to the list");
                    return 1;
                }
                else{
                    return 2;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer param){
            if (param == 0){
                log("Preview recovered from folder");
                int position = holder.getCurrentPosition();

                AndroidMegaChatMessage message = messages.get(position-1);

                if(message.getMessage()!=null){
                    if(message.getMessage().getMegaNodeList()!=null){
                        if(message.getMessage().getMegaNodeList().get(0)!=null){
                            long nodeMessageHandle = message.getMessage().getMegaNodeList().get(0).getHandle();

                            if (nodeMessageHandle == node.getHandle()){
                                if(message.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()){
                                    setOwnPreview(holder, preview, node);
                                    int status = message.getMessage().getStatus();
                                    if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                        setErrorStateOnPreview(holder, preview);
                                    }
                                }
                                else{
                                    setContactPreview(holder, preview, node);
                                }

                            }
                            else{
                                log("The nodeHandles are not equal!");
                            }
                        }
                    }
                }
            }
            else if(param == 2){
                File previewFile = new File(PreviewUtils.getPreviewFolder(context), node.getBase64Handle()+".jpg");
                log("GET PREVIEW OF HANDLE: " + node.getHandle()+" to download here: " + previewFile.getAbsolutePath());
                pendingPreviews.add(node.getHandle());
                PreviewDownloadListener listener = new PreviewDownloadListener(context, (ViewHolderMessageChat) holder, megaChatAdapter);
                megaApi.getPreview(node, previewFile.getAbsolutePath(), listener);
            }
        }
    }

    private class ChatLocalPreviewAsyncTask extends AsyncTask<MegaNode, Void, Integer>{

        MegaNode node;
        Bitmap preview;
        File cacheDir;
        File destination;
        MegaChatLollipopAdapter.ViewHolderMessageChat holder;

        public ChatLocalPreviewAsyncTask(MegaChatLollipopAdapter.ViewHolderMessageChat holder) {
            this.holder = holder;
        }

        @Override
        protected Integer doInBackground(MegaNode... params){
            node = params[0];

            if (node == null){
                return 3;
            }
            preview = PreviewUtils.getPreviewFromFolder(node, context);

            if (preview != null){
                PreviewUtils.previewCache.put(node.getHandle(), preview);
                return 0;
            }
            else{
                if (context.getExternalCacheDir() != null){
                    cacheDir = context.getExternalCacheDir();
                }
                else{
                    cacheDir = context.getCacheDir();
                }
                destination = new File(cacheDir, node.getName());

                if (destination.exists()){
                    if (destination.length() == node.getSize()){
                        File previewDir = PreviewUtils.getPreviewFolder(context);
                        File previewFile = new File(previewDir, node.getBase64Handle()+".jpg");
                        log("BASE64: " + node.getBase64Handle() + "name: " + node.getName());
                        boolean previewCreated = MegaUtilsAndroid.createPreview(destination, previewFile);

                        if (previewCreated){
                            preview = PreviewUtils.getBitmapForCache(previewFile, context);
                            destination.delete();
                            return 0;
                        }
                        else{
                            return 1;
                        }
                    }
                    else{
                        destination.delete();
                        return 1;
                    }
                }

                if (pendingPreviews.contains(node.getHandle())){
                    log("the image is already downloaded or added to the list");
                    return 1;
                }
                else{
                    return 2;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer param){
            if (param == 0){
                int position = holder.getCurrentPosition();

                AndroidMegaChatMessage message = messages.get(position-1);

                long nodeMessageHandle = message.getMessage().getMegaNodeList().get(0).getHandle();

                if (nodeMessageHandle == node.getHandle()){
                    if(message.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()){
                        setOwnPreview(holder, preview, node);
                        int status = message.getMessage().getStatus();
                        if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)) {
                            setErrorStateOnPreview(holder, preview);
                        }
                    }
                    else{

                    }

                }
                else{
                    log("The nodeHandles are not equal!");
                }
            }
            else if(param == 2){
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
            log("doInBackground ChatUploadingPreviewAsyncTask");
            filePath = params[0];
            File currentFile = new File(filePath);
            if (MimeTypeList.typeForName(filePath).isImage()){
                log("Is image");

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                Bitmap preview;

                ExifInterface exif;
                int orientation = ExifInterface.ORIENTATION_NORMAL;
                try {
                    exif = new ExifInterface(currentFile.getAbsolutePath());
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {}

                // Calculate inSampleSize
                options.inSampleSize = Util.calculateInSampleSize(options, 1000, 1000);

                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;

                preview = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);
                if (preview != null){
                    preview = Util.rotateBitmap(preview, orientation);

                    long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(filePath));
                    PreviewUtils.setPreviewCache(fingerprintCache, preview);
                    return preview;
                }
            }
            else if (MimeTypeList.typeForName(filePath).isPdf()){
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

                    if (preview != null && result){
                        log("Compress OK");
                        long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(previewFile.getPath()));
                        PreviewUtils.setPreviewCache(fingerprintCache, preview);
                        return preview;
                    }
                    else if (!result) {
                        log("Not Compress");
                    }
                } catch(Exception e) {
                    log("Pdf thumbnail could not be created");
                } finally {
                    try {
                        if (out != null)
                            out.close();
                    } catch (Exception e) {
                    }
                }
            }
            else if (MimeTypeList.typeForName(filePath).isVideo()){
                log("Is video");
                File previewDir = PreviewUtils.getPreviewFolder(context);
                File previewFile = new File(previewDir, currentFile.getName() + ".jpg");

                Bitmap bmPreview = PreviewUtils.createVideoPreview(filePath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                if(bmPreview==null){
                    log("Create video preview NULL");
//                    bmPreview= ThumbnailUtilsLollipop.loadVideoThumbnail(filePath, context);
                }
                else{
                    log("Create Video preview worked!");
                }

                if(bmPreview!=null){
					try {
                        previewFile.createNewFile();
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(previewFile);
//                            Bitmap resizedBitmap = ThumbnailUtilsLollipop.resizeBitmapUpload(bmPreview, bmPreview.getWidth(), bmPreview.getHeight());
                            boolean result = bmPreview.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                            if (result){
                                log("Compress OK");
                                long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(previewFile.getPath()));
                                PreviewUtils.setPreviewCache(fingerprintCache, bmPreview);
                                return bmPreview;
                            }
                        } catch (Exception e) {
                            log("Error with FileOutputStream: "+e.getMessage());
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                log("Error: "+e.getMessage());
                            }
                        }

                    } catch (IOException e1) {
                        log("Error creating new preview file: "+e1.getMessage());
                    }
                }
                else{
                    log("Create video preview NULL");
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap preview){
            log("onPostExecute ChatUploadingPreviewAsyncTask");
            if (preview != null){
                if (holder.filePathUploading.equals(filePath)){
                    setUploadingPreview(holder, preview);
                }
                else{
                    log("The filePaths are not equal!");
                }
            }
            else{
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

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        listFragment = _listView;

        megaChatAdapter = this;

        if(messages!=null)
        {
            log("Number of messages: "+messages.size());
        }
        else{
            log("Number of messages: NULL");
        }

        myUserHandle = megaChatApi.getMyUserHandle();
        log("MegaChatLollipopAdapter: MyUserHandle: "+myUserHandle);
    }

    public static class ViewHolderMessageChat extends RecyclerView.ViewHolder {
        public ViewHolderMessageChat(View view) {
            super(view);
        }

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
        WrapEmojiconTextView contentOwnMessageText;

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

        ImageView iconOwnTypeDocLandPreview;
        ImageView iconOwnTypeDocPortraitPreview;

        RelativeLayout transparentCoatingLandscape;
        RelativeLayout transparentCoatingPortrait;
        RelativeLayout uploadingProgressBarPort;
        RelativeLayout uploadingProgressBarLand;

        RelativeLayout errorUploadingPortrait;
        RelativeLayout errorUploadingLandscape;

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
        WrapEmojiconTextView contentContactMessageText;

        RoundedImageView contentContactMessageThumbLand;
        RelativeLayout gradientContactMessageThumbLand;
        ImageView videoIconContactMessageThumbLand;
        TextView videoTimecontentContactMessageThumbLand;

        RoundedImageView contentContactMessageThumbPort;
        RelativeLayout gradientContactMessageThumbPort;
        ImageView videoIconContactMessageThumbPort;
        TextView videoTimecontentContactMessageThumbPort;

        RelativeLayout contentContactMessageFileLayout;
        ImageView contentContactMessageFileThumb;
        TextView contentContactMessageFileName;
        TextView contentContactMessageFileSize;

        RelativeLayout layoutAvatarMessages;

        RelativeLayout contentContactMessageContactLayout;
        RelativeLayout contentContactMessageContactLayoutAvatar;
        RoundedImageView contentContactMessageContactThumb;
        TextView contentContactMessageContactName;
        public TextView contentContactMessageContactEmail;
        TextView contentContactMessageContactInitialLetter;

        ImageView iconContactTypeDocLandPreview;
        ImageView iconContactTypeDocPortraitPreview;

        RelativeLayout ownManagementMessageLayout;
        TextView ownManagementMessageText;

        TextView contactManagementMessageText;
        RelativeLayout contactManagementMessageLayout;

        public String filePathUploading;

        public long getUserHandle (){
            return userHandle;
        }

        public int getCurrentPosition (){
            return currentPosition;
        }

        public void setMyImageView(Bitmap bitmap){
            contentOwnMessageContactThumb.setImageBitmap(bitmap);
            contentOwnMessageContactInitialLetter.setVisibility(View.GONE);
        }
        public void setContactImageView(Bitmap bitmap){
            contentContactMessageContactThumb.setImageBitmap(bitmap);
            contentContactMessageContactInitialLetter.setVisibility(View.GONE);
        }
        public void setContactAvatar(Bitmap bitmap){
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

        if(viewType == TYPE_HEADER)
        {
            log("Create header");
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_item_chat, parent, false);
            return  new ViewHolderHeaderChat(v);
        }
        else
        {
            log("Create item message");
            Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
            outMetrics = new DisplayMetrics ();
            display.getMetrics(outMetrics);
            float density  = context.getResources().getDisplayMetrics().density;

            dbH = DatabaseHandler.getDbHandler(context);

            cC = new ChatController(context);

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_chat, parent, false);
            holder = new ViewHolderMessageChat(v);
            holder.itemLayout = (RelativeLayout) v.findViewById(R.id.message_chat_item_layout);
            holder.dateLayout = (RelativeLayout) v.findViewById(R.id.message_chat_date_layout);
            //Margins
            RelativeLayout.LayoutParams dateLayoutParams = (RelativeLayout.LayoutParams)holder.dateLayout.getLayoutParams();
            dateLayoutParams.setMargins(0, Util.scaleHeightPx(8, outMetrics), 0, Util.scaleHeightPx(8, outMetrics));
            holder.dateLayout.setLayoutParams(dateLayoutParams);

            holder.dateText = (TextView) v.findViewById(R.id.message_chat_date_text);

            holder.newMessagesLayout = (LinearLayout) v.findViewById(R.id.message_chat_new_relative_layout);
            holder.newMessagesText = (TextView) v.findViewById(R.id.message_chat_new_text);

            //Own messages
            holder.ownMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_own_message_layout);
            holder.titleOwnMessage = (RelativeLayout) v.findViewById(R.id.title_own_message_layout);
            holder.timeOwnText = (TextView) v.findViewById(R.id.message_chat_time_text);

//        holder.meText = (TextView) v.findViewById(R.id.message_chat_me_text);

            holder.previewFramePort = (RelativeLayout) v.findViewById(R.id.preview_frame_portrait);
            holder.previewFrameLand = (RelativeLayout) v.findViewById(R.id.preview_frame_landscape);

            holder.contentOwnMessageLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_layout);
            holder.contentOwnMessageText = (WrapEmojiconTextView) v.findViewById(R.id.content_own_message_text);

            holder.contentOwnMessageThumbLand = (RoundedImageView)  v.findViewById(R.id.content_own_message_thumb_landscape);
            int radius = Util.scaleWidthPx(15, outMetrics);
            holder.contentOwnMessageThumbLand.setCornerRadius(radius);
            holder.contentOwnMessageThumbLand.setBorderWidth(0);
            holder.contentOwnMessageThumbLand.setOval(false);

            holder.gradientOwnMessageThumbLand = (RelativeLayout)  v.findViewById(R.id.gradient_own_message_thumb_landscape);
            holder.videoIconOwnMessageThumbLand = (ImageView)  v.findViewById(R.id.video_icon_own_message_thumb_landscape);
            holder.videoTimecontentOwnMessageThumbLand = (TextView)  v.findViewById(R.id.video_time_own_message_thumb_landscape);

            holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            holder.contentOwnMessageThumbPort = (RoundedImageView)  v.findViewById(R.id.content_own_message_thumb_portrait);
            holder.contentOwnMessageThumbPort.setCornerRadius(radius);
            holder.contentOwnMessageThumbPort.setBorderWidth(0);
            holder.contentOwnMessageThumbPort.setOval(false);

            holder.ownTriangleIconFile = (RelativeLayout) v.findViewById(R.id.own_triangle_icon_file);
            holder.ownTriangleIconContact = (RelativeLayout) v.findViewById(R.id.own_triangle_icon_contact);

            holder.gradientOwnMessageThumbPort = (RelativeLayout)  v.findViewById(R.id.gradient_own_message_thumb_portrait);
            holder.videoIconOwnMessageThumbPort = (ImageView)  v.findViewById(R.id.video_icon_own_message_thumb_portrait);
            holder.videoTimecontentOwnMessageThumbPort = (TextView)  v.findViewById(R.id.video_time_own_message_thumb_portrait);

            holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            holder.contentOwnMessageFileLayout = (RelativeLayout)  v.findViewById(R.id.content_own_message_file_layout);

            holder.contentOwnMessageFileLayout = (RelativeLayout)  v.findViewById(R.id.content_own_message_file_layout);
            holder.contentOwnMessageFileThumb = (ImageView)  v.findViewById(R.id.content_own_message_file_thumb);
            holder.contentOwnMessageFileName = (TextView)  v.findViewById(R.id.content_own_message_file_name);
            holder.contentOwnMessageFileSize = (TextView)  v.findViewById(R.id.content_own_message_file_size);

            holder.contentOwnMessageContactLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_contact_layout);
            holder.contentOwnMessageContactLayoutAvatar = (RelativeLayout) v.findViewById(R.id.content_own_message_contact_layout_avatar);
            holder.contentOwnMessageContactThumb = (RoundedImageView) v.findViewById(R.id.content_own_message_contact_thumb);
            holder.contentOwnMessageContactName = (TextView)  v.findViewById(R.id.content_own_message_contact_name);
            holder.contentOwnMessageContactEmail = (TextView)  v.findViewById(R.id.content_own_message_contact_email);
            holder.contentOwnMessageContactInitialLetter = (TextView)  v.findViewById(R.id.content_own_message_contact_initial_letter);

            holder.iconOwnTypeDocLandPreview = (ImageView)  v.findViewById(R.id.own_attachment_type_icon_lands);
            holder.iconOwnTypeDocPortraitPreview = (ImageView)  v.findViewById(R.id.own_attachment_type_icon_portrait);

            holder.retryAlert = (TextView) v.findViewById(R.id.not_sent_own_message_text);
            holder.triangleIcon = (ImageView)  v.findViewById(R.id.own_triangle_icon);

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

            holder.ownManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.own_management_message_layout);
            //Margins
            RelativeLayout.LayoutParams ownManagementParams = (RelativeLayout.LayoutParams)holder.ownManagementMessageLayout.getLayoutParams();
            ownManagementParams.addRule(RelativeLayout.ALIGN_LEFT);
            ownManagementParams.setMargins(0, 0, 0, Util.scaleHeightPx(4, outMetrics));
            holder.ownManagementMessageLayout.setLayoutParams(ownManagementParams);

            holder.ownManagementMessageText = (TextView) v.findViewById(R.id.own_management_message_text);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holder.contentOwnMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
                holder.ownManagementMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
            }else{
                holder.contentOwnMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
                holder.ownManagementMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
            }

            //Contact messages////////////////////////////////////////
            holder.contactMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_contact_message_layout);
            holder.titleContactMessage = (RelativeLayout) v.findViewById(R.id.title_contact_message_layout);

            holder.contactImageView = (RoundedImageView) v.findViewById(R.id.contact_thumbnail);
            holder.contactInitialLetter = (TextView) v.findViewById(R.id.contact_initial_letter);

            holder.timeContactText = (TextView) v.findViewById(R.id.contact_message_chat_time_text);
            holder.nameContactText = (TextView) v.findViewById(R.id.contact_message_chat_name_text);

            holder.contentContactMessageLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_layout);
            holder.contentContactMessageText = (WrapEmojiconTextView) v.findViewById(R.id.content_contact_message_text);

            holder.contentContactMessageThumbLand = (RoundedImageView)  v.findViewById(R.id.content_contact_message_thumb_landscape);
            holder.contentContactMessageThumbLand.setCornerRadius(radius);
            holder.contentContactMessageThumbLand.setBorderWidth(0);
            holder.contentContactMessageThumbLand.setOval(false);

            holder.contentContactMessageThumbPort = (RoundedImageView)  v.findViewById(R.id.content_contact_message_thumb_portrait);
            holder.contentContactMessageThumbPort.setCornerRadius(radius);
            holder.contentContactMessageThumbPort.setBorderWidth(0);
            holder.contentContactMessageThumbPort.setOval(false);

            holder.gradientContactMessageThumbLand = (RelativeLayout) v.findViewById(R.id.gradient_contact_message_thumb_landscape);
            holder.videoIconContactMessageThumbLand = (ImageView) v.findViewById(R.id.video_icon_contact_message_thumb_landscape);
            holder.videoTimecontentContactMessageThumbLand = (TextView) v.findViewById(R.id.video_time_contact_message_thumb_landscape);

            holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            holder.gradientContactMessageThumbPort = (RelativeLayout) v.findViewById(R.id.gradient_contact_message_thumb_portrait);
            holder.videoIconContactMessageThumbPort = (ImageView) v.findViewById(R.id.video_icon_contact_message_thumb_portrait);
            holder.videoTimecontentContactMessageThumbPort = (TextView) v.findViewById(R.id.video_time_contact_message_thumb_portrait);

            holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            holder.contentContactMessageFileLayout = (RelativeLayout)  v.findViewById(R.id.content_contact_message_file_layout);
            holder.contentContactMessageFileThumb = (ImageView)  v.findViewById(R.id.content_contact_message_file_thumb);
            holder.contentContactMessageFileName = (TextView)  v.findViewById(R.id.content_contact_message_file_name);
            holder.contentContactMessageFileSize = (TextView)  v.findViewById(R.id.content_contact_message_file_size);

            holder.layoutAvatarMessages = (RelativeLayout)v.findViewById(R.id.layout_avatar);
            holder.contentContactMessageContactLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout);
            holder.contentContactMessageContactLayoutAvatar = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout_avatar);
            holder.contentContactMessageContactThumb = (RoundedImageView) v.findViewById(R.id.content_contact_message_contact_thumb);
            holder.contentContactMessageContactName = (TextView)  v.findViewById(R.id.content_contact_message_contact_name);
            holder.contentContactMessageContactEmail = (TextView)  v.findViewById(R.id.content_contact_message_contact_email);
            holder.contentContactMessageContactInitialLetter = (TextView)  v.findViewById(R.id.content_contact_message_contact_initial_letter);

            holder.iconContactTypeDocLandPreview = (ImageView)  v.findViewById(R.id.contact_attachment_type_icon_lands);
            holder.iconContactTypeDocPortraitPreview = (ImageView)  v.findViewById(R.id.contact_attachment_type_icon_portrait);

            holder.contactManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.contact_management_message_layout);
            //Margins
            RelativeLayout.LayoutParams contactManagementParams = (RelativeLayout.LayoutParams)holder.contactManagementMessageLayout.getLayoutParams();
            contactManagementParams.addRule(RelativeLayout.ALIGN_LEFT);
            holder.contactManagementMessageLayout.setLayoutParams(contactManagementParams);

            holder.contactManagementMessageText = (TextView) v.findViewById(R.id.contact_management_message_text);

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holder.contentContactMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
                holder.contentContactMessageFileName.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
                holder.contentContactMessageFileSize.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
                holder.contactManagementMessageText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
            }else{
                holder.contentContactMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
                holder.contentContactMessageFileName.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
                holder.contentContactMessageFileSize.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
                holder.contactManagementMessageText.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
            }

            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

                holder.nameContactText.setMaxWidth(Util.scaleWidthPx(310, outMetrics));
            }else{

                holder.nameContactText.setMaxWidth(Util.scaleWidthPx(160, outMetrics));
            }

            v.setTag(holder);

            return holder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ViewHolderHeaderChat){
            log("onBindViewHolder ViewHolderHeaderChat: " + position);
        }
        else{
            log("onBindViewHolder ViewHolderMessageChat: " + position);
            AndroidMegaChatMessage androidMessage = messages.get(position-1);

            if(androidMessage.isUploading()){
                onBindViewHolderUploading(holder, position);
            }
            else{
                onBindViewHolderMessage(holder, position);
            }
        }
    }

    public void onBindViewHolderUploading(RecyclerView.ViewHolder holder, int position) {
        log("onBindViewHolderUploading: " + position);

        ((ViewHolderMessageChat) holder).currentPosition = position;

        ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).ownTriangleIconContact.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).ownManagementMessageLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
        ((ViewHolderMessageChat) holder).timeOwnText.setGravity(Gravity.RIGHT);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)  ((ViewHolderMessageChat) holder).timeOwnText.getLayoutParams();
        params.rightMargin = Util.scaleWidthPx(16, outMetrics);
        params.leftMargin = Util.scaleWidthPx(0, outMetrics);
        ((ViewHolderMessageChat) holder).timeOwnText.setLayoutParams(params);

        ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.VISIBLE);
        ((ViewHolderMessageChat)holder).iconOwnTypeDocLandPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

        AndroidMegaChatMessage message = messages.get(position-1);

        if(message.isUploading()){
                if(message.getInfoToShow()!=-1){
                switch (message.getInfoToShow()){

                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL:{
                        log("CHAT_ADAPTER_SHOW_ALL");
                        ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getPendingMessage().getUploadTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                        ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message.getPendingMessage().getUploadTimestamp()));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME:{
                        log("CHAT_ADAPTER_SHOW_TIME");
                        ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message.getPendingMessage().getUploadTimestamp()));
                        break;
                    }
                    case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING:{
                        log("CHAT_ADAPTER_SHOW_NOTHING");
                        ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.GONE);
                        break;
                    }
                }
            }

            ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.GONE);

            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
            ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.VISIBLE);
            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.VISIBLE);

            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

            String path = message.getPendingMessage().getFilePath();
            String name = message.getPendingMessage().getName();
            if(path != null){

                Bitmap preview = null;
                ((ViewHolderMessageChat)holder).filePathUploading = path;
                log("Path of the file: "+path);
                long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(path));

                if (MimeTypeList.typeForName(path).isImage() || MimeTypeList.typeForName(path).isPdf() || MimeTypeList.typeForName(path).isVideo()){

                    ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.GONE);

                    preview = PreviewUtils.getPreviewFromCache(fingerprintCache);
                    if (preview != null){
                        setUploadingPreview((ViewHolderMessageChat)holder, preview);
                        log("preview!");

                    }else{
                        log("No preview!");
                        if(message.getPendingMessage().getState()== PendingMessage.STATE_ERROR){
                            ((ViewHolderMessageChat)holder).ownTriangleIconFile.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                        }
                        try{
                            new ChatUploadingPreviewAsyncTask(((ViewHolderMessageChat)holder)).execute(path);

                        }
                        catch(Exception e){
                            //Too many AsyncTasks
                        }
                    }
                }else{

                    if(message.getPendingMessage().getState()== PendingMessage.STATE_ERROR){
                        ((ViewHolderMessageChat)holder).ownTriangleIconFile.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                    }

                }

                log("Node Name: " + name);

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    log("Landscape configuration");
                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setMaxWidth((int) width);
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setMaxWidth((int) width);
                }
                else{
                    log("Portrait configuration");
                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setMaxWidth((int) width);
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setMaxWidth((int) width);
                }

                ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setText(name);
                ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(name).getIconResourceId());
                ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                log("State of the message: "+message.getPendingMessage().getState());
                if(message.getPendingMessage().getState()== PendingMessage.STATE_ERROR){
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_error);
                }else{
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_uploading);
                }

            }
            else{
                log("Path is null");
            }
        }
        else{
            log("ERROR: The message is no UPLOADING");
        }
    }

    public void onBindViewHolderMessage(RecyclerView.ViewHolder holder, int position) {
        log("onBindViewHolderMessage: " + position);

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

        ((ViewHolderMessageChat)holder).iconOwnTypeDocLandPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).iconContactTypeDocLandPreview.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

        MegaChatMessage message = messages.get(position-1).getMessage();
        ((ViewHolderMessageChat)holder).userHandle = message.getUserHandle();

        log("Message type: "+message.getType());

        if(message.getType()==MegaChatMessage.TYPE_ALTER_PARTICIPANTS){
            log("ALTER PARTICIPANT MESSAGE!!");
            ((ViewHolderMessageChat)holder).layoutAvatarMessages.setVisibility(View.INVISIBLE);

            if(message.getHandleOfAction()==myUserHandle){
                log("me alter participant");

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING:{
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.GONE);

                int privilege = message.getPrivilege();
                log("Privilege of me: "+privilege);
                String textToShow = "";
                String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                if(fullNameAction==null){
                    fullNameAction = "";
                }

                if(fullNameAction.trim().length()<=0){

                    log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    fullNameAction = "Unknown name";
                    if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                        log("3-Call for nonContactName: "+ message.getUserHandle());
                        ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                        megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                        megaChatApi.getUserLastname(message.getUserHandle(), listener);
                        megaChatApi.getUserEmail(message.getUserHandle(), listener);
                    }
                    else{
                        log("4-Name already asked and no name received: "+ message.getUserHandle());
                    }
                }

                if(privilege!=MegaChatRoom.PRIV_RM){
                    log("I was added");
                    textToShow = String.format(context.getString(R.string.message_add_participant), megaChatApi.getMyFullname(), fullNameAction);
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                        textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/C]", "</font>");
                    }
                    catch (Exception e){}
                }
                else{
                    log("I was removed or left");
                    if(message.getUserHandle()==message.getHandleOfAction()){
                        log("I left the chat");
                        textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), megaChatApi.getMyFullname());
                        try{
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                        }
                        catch (Exception e){}
                    }
                    else{
                        textToShow = String.format(context.getString(R.string.message_remove_participant), megaChatApi.getMyFullname(), fullNameAction);
                        try{
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                            textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/C]", "</font>");
                        }
                        catch (Exception e){}
                    }
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(result);

                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);

                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).titleOwnMessage.setGravity(Gravity.LEFT);
                ((ViewHolderMessageChat)holder).timeOwnText.setGravity(Gravity.LEFT);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)   ((ViewHolderMessageChat)holder).timeOwnText.getLayoutParams();
                params.rightMargin = Util.scaleWidthPx(0, outMetrics);
                params.leftMargin = Util.scaleWidthPx(48, outMetrics);
                ((ViewHolderMessageChat)holder).timeOwnText.setLayoutParams(params);

                if (!multipleSelect) {
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                    if(this.isItemChecked(position)){
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                    }
                    else{
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }
            }
            else{
                log("CONTACT Message type ALTER PARTICIPANTS");

                int privilege = message.getPrivilege();
                log("Privilege of the user: "+privilege);

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.VISIBLE);

                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING:{
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).nameContactText.setVisibility(View.GONE);

                if (!multipleSelect) {

                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");

                    if (this.isItemChecked(position)) {
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                    } else {
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

                ((ViewHolderMessageChat)holder).fullNameTitle = cC.getFullName(message.getHandleOfAction(), chatRoom);


                if(((ViewHolderMessageChat)holder).fullNameTitle==null){
                    ((ViewHolderMessageChat)holder).fullNameTitle = "";
                }

                if(((ViewHolderMessageChat)holder).fullNameTitle.trim().length()<=0){

                    log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    ((ViewHolderMessageChat)holder).fullNameTitle = "Unknown name";
                    if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                        log("3-Call for nonContactName: "+ message.getUserHandle());

                        ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getHandleOfAction());

                        megaChatApi.getUserFirstname(message.getHandleOfAction(), listener);
                        megaChatApi.getUserLastname(message.getHandleOfAction(), listener);
                        megaChatApi.getUserEmail(message.getHandleOfAction(), listener);
                    }
                    else{
                        log("4-Name already asked and no name received: "+ message.getUserHandle());
                    }
                }

                String textToShow = "";
                if(privilege!=MegaChatRoom.PRIV_RM){
                    log("Participant was added");
                    if(message.getUserHandle()==myUserHandle){
                        log("By me");
                        textToShow = String.format(context.getString(R.string.message_add_participant), ((ViewHolderMessageChat)holder).fullNameTitle, megaChatApi.getMyFullname());
                        try{
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                            textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/C]", "</font>");
                        }
                        catch (Exception e){}
                    }
                    else{
//                        textToShow = String.format(context.getString(R.string.message_add_participant), message.getHandleOfAction()+"");
                        log("By other");
                        String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                        if(fullNameAction==null){
                            fullNameAction = "";
                        }

                        if(fullNameAction.trim().length()<=0){

                            log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                            fullNameAction = "Unknown name";
                            if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                                log("3-Call for nonContactName: "+ message.getUserHandle());
                                ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                                megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                                megaChatApi.getUserLastname(message.getUserHandle(), listener);
                                megaChatApi.getUserEmail(message.getUserHandle(), listener);
                            }
                            else{
                                log("4-Name already asked and no name received: "+ message.getUserHandle());
                            }
                        }

                        textToShow = String.format(context.getString(R.string.message_add_participant), ((ViewHolderMessageChat)holder).fullNameTitle, fullNameAction);
                        try{
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                            textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/C]", "</font>");
                        }
                        catch (Exception e){}

                    }
                }//END participant was added
                else{
                    log("Participant was removed or left");
                    if(message.getUserHandle()==myUserHandle){
                        textToShow = String.format(context.getString(R.string.message_remove_participant), ((ViewHolderMessageChat)holder).fullNameTitle, megaChatApi.getMyFullname());
                        try{
                            textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                            textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                            textToShow = textToShow.replace("[/C]", "</font>");
                        }
                        catch (Exception e){}
                    }
                    else{

                        if(message.getUserHandle()==message.getHandleOfAction()){
                            log("The participant left the chat");

                            textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), ((ViewHolderMessageChat)holder).fullNameTitle);
                            try{
                                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                                textToShow = textToShow.replace("[/A]", "</font>");
                                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                                textToShow = textToShow.replace("[/B]", "</font>");
                            }
                            catch (Exception e){}

                        }
                        else{
                            log("The participant was removed");
                            String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                            if(fullNameAction==null){
                                fullNameAction = "";
                            }

                            if(fullNameAction.trim().length()<=0){

                                log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                                fullNameAction = "Unknown name";
                                if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                                    log("3-Call for nonContactName: "+ message.getUserHandle());
                                    ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                                    megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                                    megaChatApi.getUserLastname(message.getUserHandle(), listener);
                                    megaChatApi.getUserEmail(message.getUserHandle(), listener);
                                }
                                else{
                                    log("4-Name already asked and no name received: "+ message.getUserHandle());
                                }
                            }

                            textToShow = String.format(context.getString(R.string.message_remove_participant), ((ViewHolderMessageChat)holder).fullNameTitle, fullNameAction);
                            try{
                                textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                                textToShow = textToShow.replace("[/A]", "</font>");
                                textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                                textToShow = textToShow.replace("[/B]", "</font>");
                                textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                                textToShow = textToShow.replace("[/C]", "</font>");
                            }
                            catch (Exception e){}
                        }
//                        textToShow = String.format(context.getString(R.string.message_remove_participant), message.getHandleOfAction()+"");
                    }
                } //END participant removed

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);

            } //END CONTACT MANAGEMENT MESSAGE
        }
        else if(message.getType()==MegaChatMessage.TYPE_PRIV_CHANGE){
            ((ViewHolderMessageChat)holder).layoutAvatarMessages.setVisibility(View.INVISIBLE);

            log("PRIVILEGE CHANGE message");
            if(message.getHandleOfAction()==myUserHandle){
                log("a moderator change my privilege");

                int privilege = message.getPrivilege();
                log("Privilege of the user: "+privilege);

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING:{
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.GONE);

                String privilegeString = "";
                if(privilege==MegaChatRoom.PRIV_MODERATOR){
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_STANDARD){
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_RO){
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                }
                else {
                    log("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";

                if(message.getUserHandle()==myUserHandle){
                    log("I changed my Own permission");
                    textToShow = String.format(context.getString(R.string.message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, megaChatApi.getMyFullname());
                    try{
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
                    }
                    catch (Exception e){}
                }
                else{
                    log("I was change by someone");
                    String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                    if(fullNameAction==null){
                        fullNameAction = "";
                    }

                    if(fullNameAction.trim().length()<=0){

                        log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                        fullNameAction = "Unknown name";
                        if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                            log("3-Call for nonContactName: "+ message.getUserHandle());
                            ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                            megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                            megaChatApi.getUserLastname(message.getUserHandle(), listener);
                            megaChatApi.getUserEmail(message.getUserHandle(), listener);
                        }
                        else{
                            log("4-Name already asked and no name received: "+ message.getUserHandle());
                        }
                    }
                    textToShow = String.format(context.getString(R.string.message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, fullNameAction);
                    try{
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
                    }
                    catch (Exception e){}
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(result);

                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).titleOwnMessage.setGravity(Gravity.LEFT);
                ((ViewHolderMessageChat)holder).timeOwnText.setGravity(Gravity.LEFT);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)   ((ViewHolderMessageChat)holder).timeOwnText.getLayoutParams();
                params.rightMargin = Util.scaleWidthPx(0, outMetrics);
                params.leftMargin = Util.scaleWidthPx(48, outMetrics);
                ((ViewHolderMessageChat)holder).timeOwnText.setLayoutParams(params);


                log("Visible own management message!");

                if (!multipleSelect) {
                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");

                    if (this.isItemChecked(position)) {
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                    } else {
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }
            }
            else{
                log("Participant privilege change!");

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING:{
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).nameContactText.setVisibility(View.GONE);

                if (!multipleSelect) {

                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");

                    if (this.isItemChecked(position)) {
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                    } else {
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

                ((ViewHolderMessageChat)holder).fullNameTitle = cC.getFullName(message.getHandleOfAction(), chatRoom);


                if(((ViewHolderMessageChat)holder).fullNameTitle==null){
                    ((ViewHolderMessageChat)holder).fullNameTitle = "";
                }

                if(((ViewHolderMessageChat)holder).fullNameTitle.trim().length()<=0){

                    log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    ((ViewHolderMessageChat)holder).fullNameTitle = "Unknown name";
                    if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                        log("3-Call for nonContactName: "+ message.getUserHandle());

                        ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getHandleOfAction());

                        megaChatApi.getUserFirstname(message.getHandleOfAction(), listener);
                        megaChatApi.getUserLastname(message.getHandleOfAction(), listener);
                        megaChatApi.getUserEmail(message.getHandleOfAction(), listener);
                    }
                    else{
                        log("4-Name already asked and no name received: "+ message.getUserHandle());
                    }
                }

                int privilege = message.getPrivilege();
                String privilegeString = "";
                if(privilege==MegaChatRoom.PRIV_MODERATOR){
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_STANDARD){
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_RO){
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                }
                else {
                    log("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";
                if(message.getUserHandle()==myUserHandle){
                    log("The privilege was change by me");
                    textToShow = String.format(context.getString(R.string.message_permissions_changed), ((ViewHolderMessageChat)holder).fullNameTitle, privilegeString, megaChatApi.getMyFullname());
                    try{
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
                    }
                    catch (Exception e){}

                }
                else{
                    log("By other");
                    String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                    if(fullNameAction==null){
                        fullNameAction = "";
                    }

                    if(fullNameAction.trim().length()<=0){

                        log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                        fullNameAction = "Unknown name";
                        if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                            log("3-Call for nonContactName: "+ message.getUserHandle());
                            ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                            megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                            megaChatApi.getUserLastname(message.getUserHandle(), listener);
                            megaChatApi.getUserEmail(message.getUserHandle(), listener);
                        }
                        else{
                            log("4-Name already asked and no name received: "+ message.getUserHandle());
                        }
                    }

                    textToShow = String.format(context.getString(R.string.message_permissions_changed), ((ViewHolderMessageChat)holder).fullNameTitle, privilegeString, fullNameAction);
                    try{
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
                    }
                    catch (Exception e){}
                }
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);
            }
        }
        else{
            //OTHER TYPE OF MESSAGES
            ((ViewHolderMessageChat)holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
            if(message.getUserHandle()==myUserHandle) {
                log("MY message!!");
                log("MY message handle!!: "+message.getMsgId());

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING:{
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.GONE);

                if(message.getType()==MegaChatMessage.TYPE_NORMAL||message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                    log("Message type: "+message.getMsgId());

                    String messageContent = "";

                    if(message.getContent()!=null){
                        messageContent = message.getContent();
                    }

                    if(message.isEdited()){

                        log("MY Message is edited");

                        ((ViewHolderMessageChat) holder).contentOwnMessageText.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);

                        Spannable content = new SpannableString(messageContent);
                        int status = message.getStatus();
                        if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                            log("Show triangle retry!");
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                            ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);

                        }
                        else if((status==MegaChatMessage.STATUS_SENDING)){
                            log("Status not received by server: "+message.getStatus());
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                            ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);

                        }
                        else{
                            log("Status: "+message.getStatus());
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.white)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));

                            ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);

                        }

                        SimpleSpanBuilder ssb = null;

                        try{
                            RTFFormatter formatter = new RTFFormatter(messageContent, context);
                            ssb = formatter.setRTFFormat();
                        }
                        catch (Exception e){
                            log("FORMATTER EXCEPTION!!!");
                            ssb = null;
                        }

                        if(ssb!=null){

                            ssb.append(" "+context.getString(R.string.edited_message_text), new RelativeSizeSpan(0.70f), new ForegroundColorSpan(context.getResources().getColor(R.color.white)), new StyleSpan(Typeface.ITALIC));
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(content+" ");

                            Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                            edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            edited.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.white)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.append(edited);
                        }

                        ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);

                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
                        ((ViewHolderMessageChat) holder).timeOwnText.setGravity(Gravity.RIGHT);
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)  ((ViewHolderMessageChat) holder).timeOwnText.getLayoutParams();
                        params.rightMargin = Util.scaleWidthPx(16, outMetrics);
                        params.leftMargin = Util.scaleWidthPx(0, outMetrics);
                        ((ViewHolderMessageChat) holder).timeOwnText.setLayoutParams(params);

                        if (!multipleSelect) {

                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
                            if(this.isItemChecked(position)){
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                    else if(message.isDeleted()){
                        log("Message is deleted");

                        ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                        ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(context.getString(R.string.text_deleted_message));

                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).titleOwnMessage.setGravity(Gravity.LEFT);
                        ((ViewHolderMessageChat)holder).timeOwnText.setGravity(Gravity.LEFT);
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)   ((ViewHolderMessageChat)holder).timeOwnText.getLayoutParams();
                        params.rightMargin = Util.scaleWidthPx(0, outMetrics);
                        params.leftMargin = Util.scaleWidthPx(48, outMetrics);
                        ((ViewHolderMessageChat)holder).timeOwnText.setLayoutParams(params);

                        ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);

                        if (!multipleSelect) {
                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
                            if (this.isItemChecked(position)) {
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                            } else {
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                    else{

                        if(message.getType()==MegaChatMessage.TYPE_NORMAL){
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

                            SimpleSpanBuilder ssb = null;

                            try{
                                if(message.getContent()!=null){
                                    messageContent = message.getContent();
                                    RTFFormatter formatter = new RTFFormatter(messageContent, context);
                                    ssb = formatter.setRTFFormat();
                                }
                            }
                            catch (Exception e){
                                log("FORMATTER EXCEPTION!!!");
                                ssb = null;
                            }

                            int status = message.getStatus();

                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                                log("Show triangle retry!");
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                                ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);

                            }
                            else if((status==MegaChatMessage.STATUS_SENDING)){
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));

                                ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);

                            }
                            else{
                                log("Status: "+message.getStatus());
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));

                                ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);

                            }

                            if(EmojiManager.isOnlyEmojis(messageContent)){
                                log("IS ONLY emoji!!!");
                                List<String> emojis = EmojiParser.extractEmojis(messageContent);
                                int size = emojis.size();
                                log("size of emojis: "+size);
                                switch (size){
                                    case 1:{
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setEmojiconSizeSp(35);
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                    case 2:{
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setEmojiconSizeSp(30);
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                    case 3:{
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setEmojiconSizeSp(25);
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                    default:{
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setEmojiconSizeSp(20);
                                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                }
                            }
                            else{
                                log("IS NOT only emoji");
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setLineSpacing(1,1.0f);
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setEmojiconSizeSp(20);
                            }


//                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);
                            if(ssb!=null){
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);
                            }

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setLinksClickable(true);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setLinkTextColor(ContextCompat.getColor(context, R.color.white));
                            Linkify.addLinks(((ViewHolderMessageChat)holder).contentOwnMessageText, Linkify.WEB_URLS);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.VISIBLE);

                            int status = message.getStatus();
                            log("Status: "+message.getStatus());
                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){

                                ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                            }else if(status==MegaChatMessage.STATUS_SENDING){

                                ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                            }else{

                                ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                            }

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

                            MegaNodeList nodeList = message.getMegaNodeList();
                            if(nodeList != null){

                                if(nodeList.size()==1) {
                                    MegaNode node = nodeList.get(0);
                                    log("Node Name: " + node.getName());

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

                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setText(node.getName());

                                    long nodeSize = node.getSize();
                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setText(Util.getSizeString(nodeSize));


                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                                    Bitmap preview = null;
                                    if (node.hasPreview()){

                                        preview = PreviewUtils.getPreviewFromCache(node);
                                        if (preview != null) {
                                            PreviewUtils.previewCache.put(node.getHandle(), preview);
                                            setOwnPreview((ViewHolderMessageChat) holder, preview, node);
                                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                                setErrorStateOnPreview((ViewHolderMessageChat) holder, preview);
                                            }

                                        } else {
                                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                                ((ViewHolderMessageChat)holder).ownTriangleIconFile.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                                            }
                                            try{
                                                new MegaChatLollipopAdapter.ChatPreviewAsyncTask(((ViewHolderMessageChat)holder)).execute(node);

                                            }
                                            catch(Exception ex){
                                                //Too many AsyncTasks
                                                log("Too many AsyncTasks");
                                            }
                                        }
                                    }
                                    else{

                                        log("Node has no preview on servers");

                                        preview = PreviewUtils.getPreviewFromCache(node);
                                        if (preview != null){
                                            PreviewUtils.previewCache.put(node.getHandle(), preview);
                                            if (preview.getWidth() < preview.getHeight()) {
                                                log("Portrait");

                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setImageBitmap(preview);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                                if (MimeTypeList.typeForName(node.getName()).isPdf()){
                                                    log("Is pfd preview");
                                                    ((ViewHolderMessageChat) holder).iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                                                }else if (MimeTypeList.typeForName(node.getName()).isVideo()){
                                                    log("Is video preview");

                                                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setText(timeVideo(node));
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

                                                }else{
                                                    ((ViewHolderMessageChat) holder).iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                                                }

                                                ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.VISIBLE);


                                                ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat)holder).errorUploadingLandscape.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).transparentCoatingLandscape.setVisibility(View.GONE);

                                                if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                                    ((ViewHolderMessageChat)holder).errorUploadingPortrait.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat)holder).transparentCoatingPortrait.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                                                }
                                                else{
                                                    ((ViewHolderMessageChat)holder).errorUploadingPortrait.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat)holder).transparentCoatingPortrait.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);
                                                }

                                                ((ViewHolderMessageChat)holder).ownTriangleIconFile.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                                            } else {
                                                log("Landscape");

                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setImageBitmap(preview);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                                if (MimeTypeList.typeForName(node.getName()).isPdf()){
                                                    log("Is pfd preview");
                                                    ((ViewHolderMessageChat) holder).iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                                                }else if (MimeTypeList.typeForName(node.getName()).isVideo()){
                                                    log("Is video preview");
                                                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setText(timeVideo(node));
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).iconOwnTypeDocLandPreview.setVisibility(View.GONE);

                                                }else{
                                                    ((ViewHolderMessageChat) holder).iconOwnTypeDocLandPreview.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                                }

                                                ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat) holder).errorUploadingPortrait.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).transparentCoatingPortrait.setVisibility(View.GONE);

                                                if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                                    ((ViewHolderMessageChat) holder).errorUploadingLandscape.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).transparentCoatingLandscape.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.VISIBLE);
                                                }
                                                else{
                                                    ((ViewHolderMessageChat) holder).errorUploadingLandscape.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).transparentCoatingLandscape.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);
                                                }

                                                ((ViewHolderMessageChat) holder).ownTriangleIconFile.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                                            }
                                        }
                                        else{
                                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                                ((ViewHolderMessageChat)holder).ownTriangleIconFile.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                                            }
                                            try{
                                                new MegaChatLollipopAdapter.ChatLocalPreviewAsyncTask(((ViewHolderMessageChat)holder)).execute(node);

                                            }
                                            catch(Exception ex){
                                                //Too many AsyncTasks
                                                log("Too many AsyncTasks");
                                            }
                                        }
                                    }
                                }
                                else{
                                    long totalSize = 0;
                                    int count = 0;
                                    for(int i=0; i<nodeList.size(); i++){
                                        MegaNode temp = nodeList.get(i);
                                        if(!(megaChatApi.isRevoked(chatRoom.getChatId(), temp.getHandle()))){
                                            count++;
                                            log("Node Name: "+temp.getName());
                                            totalSize = totalSize + temp.getSize();
                                        }
                                    }

                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setText(Util.getSizeString(totalSize));


                                    MegaNode node = nodeList.get(0);
                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                                    if(count==1){
                                        ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setText(node.getName());
                                    }
                                    else{
                                        ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, count, count));
                                    }
                                }
                            }
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT){
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.white));
                            messageContent = "Attachment revoked";
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);

                        }
                        else if(message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.VISIBLE);

                            int status = message.getStatus();
                            log("Status: "+message.getStatus());
                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                                ((ViewHolderMessageChat)holder).ownTriangleIconContact.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);


                            }else if(status==MegaChatMessage.STATUS_SENDING){
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.light_rounded_chat_own_message));
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).ownTriangleIconContact.setVisibility(View.GONE);
                            }else{
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.dark_rounded_chat_own_message));
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).ownTriangleIconContact.setVisibility(View.GONE);
                            }

                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                log("Landscape configuration");
                                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setMaxWidth((int) width);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setMaxWidth((int) width);
                            }
                            else{
                                log("Portrait configuration");
                                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setMaxWidth((int) width);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setMaxWidth((int) width);
                            }

                            long userCount  = message.getUsersCount();

                            if(userCount==1){
                                String name = "";
                                name = message.getUserName(0);
                                if(name.trim().isEmpty()){
                                    name = message.getUserEmail(0);
                                }
                                String email = message.getUserEmail(0);
                                log("Contact Name: "+name);

                                ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setText(name);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setText(email);

                                setUserAvatar(((ViewHolderMessageChat)holder), message);
                            }
                            else{
                                //Show default avatar with userCount
                                StringBuilder name = new StringBuilder("");
                                name.append(message.getUserName(0));
                                for(int i=1; i<userCount;i++){
                                    name.append(", "+message.getUserName(i));
                                }
                                log("Names of attached contacts: "+name);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setText(name);

                                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int)userCount, userCount);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setText(email);

                                createDefaultAvatar(((ViewHolderMessageChat)holder), null, email, true);
                            }
                        }

                        ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);

                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
                        ((ViewHolderMessageChat) holder).timeOwnText.setGravity(Gravity.RIGHT);
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)  ((ViewHolderMessageChat) holder).timeOwnText.getLayoutParams();
                        params.rightMargin = Util.scaleWidthPx(16, outMetrics);
                        params.leftMargin = Util.scaleWidthPx(0, outMetrics);
                        ((ViewHolderMessageChat) holder).timeOwnText.setLayoutParams(params);

                        if (!multipleSelect) {

                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");

                            if(this.isItemChecked(position)){
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_TRUNCATE){
                    log("Message type TRUNCATE");

                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                    String textToShow = String.format(context.getString(R.string.history_cleared_by), megaChatApi.getMyFullname());
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(result);

                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).titleOwnMessage.setGravity(Gravity.LEFT);
                    ((ViewHolderMessageChat)holder).timeOwnText.setGravity(Gravity.LEFT);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)   ((ViewHolderMessageChat)holder).timeOwnText.getLayoutParams();
                    params.rightMargin = Util.scaleWidthPx(0, outMetrics);
                    params.leftMargin = Util.scaleWidthPx(48, outMetrics);
                    ((ViewHolderMessageChat)holder).timeOwnText.setLayoutParams(params);

                    if (!multipleSelect) {

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");

                        if (this.isItemChecked(position)) {
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){

                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.change_title_messages), megaChatApi.getMyFullname(), messageContent);
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                        textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/C]", "</font>");
                    }
                    catch (Exception e){}

                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }

                    ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(result);

                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).titleOwnMessage.setGravity(Gravity.LEFT);
                    ((ViewHolderMessageChat)holder).timeOwnText.setGravity(Gravity.LEFT);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)   ((ViewHolderMessageChat)holder).timeOwnText.getLayoutParams();
                    params.rightMargin = Util.scaleWidthPx(0, outMetrics);
                    params.leftMargin = Util.scaleWidthPx(48, outMetrics);
                    ((ViewHolderMessageChat)holder).timeOwnText.setLayoutParams(params);

                    if (!multipleSelect) {

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");

                        if (this.isItemChecked(position)) {
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                }
                else{
                    log("Type message ERROR: "+message.getType());
                    ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.tour_bar_red));
                    ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(context.getString(R.string.error_message_unrecognizable));

                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);

                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).titleOwnMessage.setGravity(Gravity.RIGHT);
                    ((ViewHolderMessageChat) holder).timeOwnText.setGravity(Gravity.RIGHT);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)  ((ViewHolderMessageChat) holder).timeOwnText.getLayoutParams();
                    params.rightMargin = Util.scaleWidthPx(16, outMetrics);
                    params.leftMargin = Util.scaleWidthPx(0, outMetrics);
                    ((ViewHolderMessageChat) holder).timeOwnText.setLayoutParams(params);

                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

                    ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbPort.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                    ((ViewHolderMessageChat) holder).gradientOwnMessageThumbLand.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

//                log("Content: "+message.getContent());
                }
//                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
            }else{
                long userHandle = message.getUserHandle();
                log("Contact message!!: "+userHandle);

                if(((ChatActivityLollipop) context).isGroup()){

                    ((ViewHolderMessageChat)holder).fullNameTitle = cC.getFullName(userHandle, chatRoom);

                    if(((ViewHolderMessageChat)holder).fullNameTitle==null){
                        ((ViewHolderMessageChat)holder).fullNameTitle = "";
                    }

                    if(((ViewHolderMessageChat)holder).fullNameTitle.trim().length()<=0){

                        log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                        ((ViewHolderMessageChat)holder).fullNameTitle = "Unknown name";
                        if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                            log("3-Call for nonContactName: "+ message.getUserHandle());
                            ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, userHandle);
                            megaChatApi.getUserFirstname(userHandle, listener);
                            megaChatApi.getUserLastname(userHandle, listener);
                            megaChatApi.getUserEmail(userHandle, listener);
                        }
                        else{
                            log("4-Name already asked and no name received: "+ message.getUserHandle());
                        }
                    }

                    ((ViewHolderMessageChat)holder).nameContactText.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).nameContactText.setText(((ViewHolderMessageChat)holder).fullNameTitle);
                }
                else{

                    ((ViewHolderMessageChat)holder).fullNameTitle = chatRoom.getTitle();
                    ((ViewHolderMessageChat)holder).nameContactText.setVisibility(View.GONE);
                }

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL:{
                            log("CHAT_ADAPTER_SHOW_ALL");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME--");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING:{
                            log("CHAT_ADAPTER_SHOW_NOTHING");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.VISIBLE);

                if(message.getType()==MegaChatMessage.TYPE_NORMAL||message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                    if (messages.get(position-1).isShowAvatar()){
                        ((ViewHolderMessageChat)holder).layoutAvatarMessages.setVisibility(View.VISIBLE);
                        setContactAvatar(((ViewHolderMessageChat)holder), userHandle,((ViewHolderMessageChat)holder).fullNameTitle);
                    }
                    else{
                        ((ViewHolderMessageChat)holder).layoutAvatarMessages.setVisibility(View.INVISIBLE);
                    }

                    if(message.isEdited()){
                        log("Message is edited");
                        String messageContent = "";
                        if(message.getContent()!=null){
                            messageContent = message.getContent();
                        }

                        ((ViewHolderMessageChat) holder).contentContactMessageText.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat) holder).contentContactMessageFileLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

//                        ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

                        SimpleSpanBuilder ssb = null;
                        try{
                            if(message.getContent()!=null){
                                messageContent = message.getContent();
                                RTFFormatter formatter = new RTFFormatter(messageContent, context);
                                ssb = formatter.setRTFFormat();
                            }

                            if(ssb!=null){
                                ssb.append(" "+context.getString(R.string.edited_message_text), new RelativeSizeSpan(0.70f), new ForegroundColorSpan(context.getResources().getColor(R.color.name_my_account)), new StyleSpan(Typeface.ITALIC));
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                            }
                            else{
                                messageContent = messageContent+" ";
                                Spannable content = new SpannableString(messageContent);
                                content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                                edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                CharSequence indexedText = TextUtils.concat(messageContent, edited);
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setText(indexedText);
                            }
                        }
                        catch (Exception e){
                            ssb = null;
                            log("FORMATTER EXCEPTION!!!");
                            messageContent = messageContent+" ";
                            Spannable content = new SpannableString(messageContent);
                            content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                            edited.setSpan(new RelativeSizeSpan(0.70f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                            CharSequence indexedText = TextUtils.concat(messageContent, edited);
                            ((ViewHolderMessageChat)holder).contentContactMessageText.setText(indexedText);
                        }

                        ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.GONE);

                        if (!multipleSelect) {
                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
                            if(this.isItemChecked(position)){
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                    else if(message.isDeleted()){
                        log("Message is deleted");
                        ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).nameContactText.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

//                        ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                       // ((ViewHolderMessageChat) holder).previewFrameContactPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                        ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                        ((ViewHolderMessageChat) holder).contentContactMessageFileLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

                        if (!multipleSelect) {

                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");

                            if (this.isItemChecked(position)) {
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                            } else {
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }

                        if(((ChatActivityLollipop) context).isGroup()){
                            String textToShow = String.format(context.getString(R.string.text_deleted_message_by), ((ViewHolderMessageChat)holder).fullNameTitle);
                            try{
                                textToShow = textToShow.replace("[A]", "<font color=\'#00BFA5\'>");
                                textToShow = textToShow.replace("[/A]", "</font>");
                                textToShow = textToShow.replace("[B]", "<font color=\'#060000\'>");
                                textToShow = textToShow.replace("[/B]", "</font>");
                            }
                            catch (Exception e){}
                            Spanned result = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                            } else {
                                result = Html.fromHtml(textToShow);
                            }
                            ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(context.getString(R.string.text_deleted_message));
                        }
                    }
                    else{

                        ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.GONE);

                        String messageContent = "";
                        if(message.getType()==MegaChatMessage.TYPE_NORMAL){
                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

//                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);

                            if(message.getContent()!=null){
                                messageContent = message.getContent();
                            }

                            if(EmojiManager.isOnlyEmojis(messageContent)){
                                log("IS ONLY emoji!!!");
                                List<String> emojis = EmojiParser.extractEmojis(messageContent);
                                int size = emojis.size();
                                log("size of emojis: "+size);
                                switch (size){
                                    case 1:{
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setEmojiconSizeSp(35);
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                    case 2:{
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setEmojiconSizeSp(30);
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                    case 3:{
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setEmojiconSizeSp(25);
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                    default:{
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setEmojiconSizeSp(20);
                                        ((ViewHolderMessageChat)holder).contentContactMessageText.setLineSpacing(1,1.2f);
                                        break;
                                    }
                                }

                            }
                            else{
                                log("IS NOT only emoji!!!");
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setLineSpacing(1,1.0f);
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setEmojiconSizeSp(20);
                            }

                            //Color always status SENT
                            ((ViewHolderMessageChat)holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                            SimpleSpanBuilder ssb = null;

                            try{
                                RTFFormatter formatter = new RTFFormatter(messageContent, context);
                                ssb = formatter.setRTFFormat();
                            }
                            catch (Exception e){
                                log("FORMATTER EXCEPTION!!!");
                                ssb = null;
                            }

                            if(ssb!=null){
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setText(ssb.build(), TextView.BufferType.SPANNABLE);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setText(messageContent);
                            }

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setLinksClickable(true);
                            Linkify.addLinks(((ViewHolderMessageChat)holder).contentContactMessageText, Linkify.WEB_URLS);

                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

//                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.VISIBLE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.VISIBLE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);

                            MegaNodeList nodeList = message.getMegaNodeList();
                            if(nodeList != null){
                                if(nodeList.size()==1){
                                    MegaNode node = nodeList.get(0);
                                    log("Node Name: "+node.getName());

                                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                        log("Landscape configuration");
                                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                                        ((ViewHolderMessageChat)holder).contentContactMessageFileName.setMaxWidth((int) width);
                                        ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setMaxWidth((int) width);
                                    }
                                    else{
                                        log("Portrait configuration");
                                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                                        ((ViewHolderMessageChat)holder).contentContactMessageFileName.setMaxWidth((int) width);
                                        ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setMaxWidth((int) width);
                                    }

                                    ((ViewHolderMessageChat)holder).contentContactMessageFileName.setText(node.getName());
                                    long nodeSize = node.getSize();
                                    ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setText(Util.getSizeString(nodeSize));

                                    ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                                    log("Get preview of node");

                                    Bitmap preview = null;
                                    if (node.hasPreview()) {
                                        log("Get preview of node");
                                        preview = PreviewUtils.getPreviewFromCache(node);
                                        if (preview != null){
                                            PreviewUtils.previewCache.put(node.getHandle(), preview);

                                            if (preview.getWidth() < preview.getHeight()) {
                                                log("Portrait");

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setImageBitmap(preview);
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                                if (MimeTypeList.typeForName(node.getName()).isPdf()){
                                                    log("Contact message - Is pfd preview");
                                                    ((ViewHolderMessageChat) holder).iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                                                }else if(MimeTypeList.typeForName(node.getName()).isVideo()){
                                                    log("Contact message - Is video preview");

                                                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setText(timeVideo(node));
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

                                                }else{
                                                    ((ViewHolderMessageChat) holder).iconContactTypeDocPortraitPreview.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                                                }

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.VISIBLE);
//                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.VISIBLE);

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

//                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

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
                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
                                                contactThumbParams.setMargins(0, 0 ,0, 0);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);

                                            }
                                            else {
                                                log("Landscape");

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setImageBitmap(preview);
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                                if (MimeTypeList.typeForName(node.getName()).isPdf()){
                                                    log("Contact message - Is pfd preview");
                                                    ((ViewHolderMessageChat) holder).iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                                                }else if(MimeTypeList.typeForName(node.getName()).isVideo()){
                                                    log("Contact message - Is video preview");
                                                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setText(timeVideo(node));
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.VISIBLE);

                                                    ((ViewHolderMessageChat) holder).iconContactTypeDocLandPreview.setVisibility(View.GONE);

                                                }else{
                                                    ((ViewHolderMessageChat) holder).iconContactTypeDocLandPreview.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
                                                }

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.VISIBLE);
//                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.VISIBLE);

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                                                ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
                                                contactThumbParams.setMargins(0, 0 ,0, 0);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);

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

                                        }
                                        else{
                                            try{
                                                new MegaChatLollipopAdapter.ChatPreviewAsyncTask(((ViewHolderMessageChat)holder)).execute(node);

                                            }
                                            catch(Exception ex){
                                                //Too many AsyncTasks
                                                log("Too many AsyncTasks");
                                            }
                                        }

                                    }
                                    else {
                                        log("Node has no preview on servers");

                                        preview = PreviewUtils.getPreviewFromCache(node);
                                        if (preview != null){
                                            PreviewUtils.previewCache.put(node.getHandle(), preview);

                                            setContactPreview(((ViewHolderMessageChat) holder), preview, node);

                                        } else {

                                            try{
                                                new MegaChatLollipopAdapter.ChatLocalPreviewAsyncTask(((ViewHolderMessageChat)holder)).execute(node);

                                            }
                                            catch(Exception ex){
                                                //Too many AsyncTasks
                                                log("Too many AsyncTasks");
                                            }
                                        }
                                    }
                                }
                                else{
                                    long totalSize = 0;
                                    int count = 0;
                                    for(int i=0; i<nodeList.size(); i++){
                                        MegaNode temp = nodeList.get(i);
                                        if(!(megaChatApi.isRevoked(chatRoom.getChatId(), temp.getHandle()))){
                                            count++;
                                            log("Node Name: "+temp.getName());
                                            totalSize = totalSize + temp.getSize();
                                        }
                                    }
                                    ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setText(Util.getSizeString(totalSize));
                                    MegaNode node = nodeList.get(0);
                                    ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                                    if(count==1){
                                        ((ViewHolderMessageChat)holder).contentContactMessageFileName.setText(node.getName());
                                    }
                                    else{
                                        ((ViewHolderMessageChat)holder).contentContactMessageFileName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, count, count));
                                    }
                                }
                            }
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
//                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);


                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            messageContent = "Attachment revoked";
                            ((ViewHolderMessageChat)holder).contentContactMessageText.setText(messageContent);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
//                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.VISIBLE);

                            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                log("Landscape configuration");
                                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_LAND, context.getResources().getDisplayMetrics());
                                ((ViewHolderMessageChat)holder).contentContactMessageContactName.setMaxWidth((int) width);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setMaxWidth((int) width);
                            }
                            else{
                                log("Portrait configuration");
                                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_FILENAME_PORT, context.getResources().getDisplayMetrics());
                                ((ViewHolderMessageChat)holder).contentContactMessageContactName.setMaxWidth((int) width);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setMaxWidth((int) width);
                            }

                            long userCount  = message.getUsersCount();

                            if(userCount==1){
                                String name = "";
                                name = message.getUserName(0);
                                if(name.trim().isEmpty()){
                                    name = message.getUserEmail(0);
                                }
                                String email = message.getUserEmail(0);
                                log("Contact Name: "+name);

                                ((ViewHolderMessageChat)holder).contentContactMessageContactName.setText(name);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setText(email);

                                setUserAvatar(((ViewHolderMessageChat)holder), message);
                            }
                            else{
                                //Show default avatar with userCount
                                StringBuilder name = new StringBuilder("");
                                name.append(message.getUserName(0));
                                for(int i=1; i<userCount;i++){
                                    name.append(", "+message.getUserName(i));
                                }
                                log("Names of attached contacts: "+name);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactName.setText(name);

                                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int)userCount, userCount);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setText(email);

                                createDefaultAvatar(((ViewHolderMessageChat)holder), null, email, false);
                            }
                        }

                        if (!multipleSelect) {

                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
                            if(this.isItemChecked(position)){
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_TRUNCATE){
                    log("Message type TRUNCATE");
                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).nameContactText.setVisibility(View.GONE);

                    if (!multipleSelect) {

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");

                        if (this.isItemChecked(position)) {
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                    String textToShow = String.format(context.getString(R.string.history_cleared_by), ((ViewHolderMessageChat)holder).fullNameTitle);
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }

                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);

                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
                    log("Message type CHANGE TITLE");

                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).nameContactText.setVisibility(View.GONE);

                    if (!multipleSelect) {

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");

                        if (this.isItemChecked(position)) {
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.change_title_messages), ((ViewHolderMessageChat)holder).fullNameTitle, messageContent);
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#868686\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                        textToShow = textToShow.replace("[C]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/C]", "</font>");
                    }
                    catch (Exception e){}

                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }

                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);
                }
                else{

                    log("Type message ERROR: "+message.getType());

                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.tour_bar_red));
                    ((ViewHolderMessageChat)holder).contentContactMessageText.setText(context.getString(R.string.error_message_unrecognizable));

                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

                    ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbLand.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbLand.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).gradientContactMessageThumbPort.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoIconContactMessageThumbPort.setVisibility(View.GONE);
                    ((ViewHolderMessageChat) holder).videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);
                }
            }
        }

        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(-14, outMetrics), Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
//        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);

        if(((ChatActivityLollipop)context).lastIdMsgSeen !=-1){

            if(((ChatActivityLollipop)context).lastIdMsgSeen == message.getMsgId()){

                log("onBindViewHolder:Last message id match!");

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) ((ViewHolderMessageChat)holder).newMessagesLayout.getLayoutParams();

                if((message.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) || (message.getType()==MegaChatMessage.TYPE_PRIV_CHANGE)){
                    if(message.getHandleOfAction()==myUserHandle){
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_own_message_layout);
                        ((ViewHolderMessageChat)holder).newMessagesLayout.setLayoutParams(params);
                    }else{
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_contact_message_layout);
                        ((ViewHolderMessageChat)holder).newMessagesLayout.setLayoutParams(params);
                    }

                }else{
                    if(message.getUserHandle()==megaChatApi.getMyUserHandle()){
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_own_message_layout);
                        ((ViewHolderMessageChat)holder).newMessagesLayout.setLayoutParams(params);
                    }else{
                        params.addRule(RelativeLayout.BELOW, R.id.message_chat_contact_message_layout);
                        ((ViewHolderMessageChat)holder).newMessagesLayout.setLayoutParams(params);
                    }
                }

                String numberString;
                long unreadMessages = Math.abs(((ChatActivityLollipop)context).generalUnreadCount);
                if(((ChatActivityLollipop)context).generalUnreadCount<0){
                    numberString = "+"+unreadMessages;
                }
                else{
                    numberString = unreadMessages+"";
                }

                String contentUnreadText = context.getResources().getQuantityString(R.plurals.number_unread_messages, (int)unreadMessages, numberString);
                ((ViewHolderMessageChat)holder).newMessagesText.setText(contentUnreadText);

                ((ViewHolderMessageChat)holder).newMessagesLayout.setVisibility(View.VISIBLE);
//                ((ChatActivityLollipop)context).showJumpMessage();
                ((ChatActivityLollipop)context).setNewVisibility(true);

            }
            else{
                ((ViewHolderMessageChat)holder).newMessagesLayout.setVisibility(View.GONE);

            }
        }
        else{
            ((ViewHolderMessageChat)holder).newMessagesLayout.setVisibility(View.GONE);

        }
    }

    public void setUserAvatar(ViewHolderMessageChat holder, MegaChatMessage message){
        log("setUserAvatar");

        String name = "";
        name = message.getUserName(0);
        if(name.trim().isEmpty()){
            name = message.getUserEmail(0);
        }
        String email = message.getUserEmail(0);
        log("Contact Name: "+name);
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(message.getUserHandle(0));

        ChatAttachmentAvatarListener listener;
        if(myUserHandle==message.getUserHandle()){
            createDefaultAvatar(holder, userHandleEncoded, name, true);
            listener = new ChatAttachmentAvatarListener(context, holder, this, true);
        }
        else{
            createDefaultAvatar(holder, userHandleEncoded, name, false);
            listener = new ChatAttachmentAvatarListener(context, holder, this, false);
        }

        File avatar = null;
        if (context.getExternalCacheDir() != null){
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), email + ".jpg");
        }
        else{
            avatar = new File(context.getCacheDir().getAbsolutePath(), email + ".jpg");
        }
        Bitmap bitmap = null;
        if (avatar.exists()){
            if (avatar.length() > 0){
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();

                    if(megaApi==null){
                        log("setUserAvatar: megaApi is Null in Offline mode");
                        return;
                    }

                    if (context.getExternalCacheDir() != null){
                        megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                    }
                    else{
                        megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                    }
                }
                else{
                    if(myUserHandle==message.getUserHandle()){
                        holder.contentOwnMessageContactInitialLetter.setVisibility(View.GONE);
                        holder.contentOwnMessageContactThumb.setImageBitmap(bitmap);
                    }
                    else{
                        holder.contentContactMessageContactInitialLetter.setVisibility(View.GONE);
                        holder.contentContactMessageContactThumb.setImageBitmap(bitmap);
                    }
                }
            }
            else{
                if(megaApi==null){
                    log("setUserAvatar: megaApi is Null in Offline mode");
                    return;
                }

                if (context.getExternalCacheDir() != null){
                    megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                }
                else{
                    megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
                }
            }
        }
        else{
            if(megaApi==null){
                log("setUserAvatar: megaApi is Null in Offline mode");
                return;
            }

            if (context.getExternalCacheDir() != null){
                megaApi.getUserAvatar(email, context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
            }
            else{
                megaApi.getUserAvatar(email, context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg", listener);
            }
        }
    }
    public void setContactAvatar(ViewHolderMessageChat holder, long userHandle, String name){
        log("setContactAvatar");

        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
        createDefaultAvatarContact(holder, userHandleEncoded, name);
        ChatAttachmentAvatarListener listener = new ChatAttachmentAvatarListener(context, holder, this, false);

        File avatar = null;
        if (context.getExternalCacheDir() != null){
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), userHandleEncoded + ".jpg");
        }else{
            avatar = new File(context.getCacheDir().getAbsolutePath(), userHandleEncoded + ".jpg");
        }

        Bitmap bitmap = null;
        if (avatar.exists()){
            if (avatar.length() > 0){
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();

                    if(megaApi==null){
                        log("setUserAvatar: megaApi is Null in Offline mode");
                        return;
                    }

                    if (context.getExternalCacheDir() != null){
                        megaApi.getUserAvatar(userHandleEncoded, context.getExternalCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                    }else{
                        megaApi.getUserAvatar(userHandleEncoded, context.getCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                    }
                }
                else{
                    holder.contactInitialLetter.setVisibility(View.GONE);
                    holder.contactImageView.setImageBitmap(bitmap);
                }
            }
            else{
                if(megaApi==null){
                    log("setUserAvatar: megaApi is Null in Offline mode");
                    return;
                }

                if (context.getExternalCacheDir() != null){
                    megaApi.getUserAvatar(userHandleEncoded, context.getExternalCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                }
                else{
                    megaApi.getUserAvatar(userHandleEncoded, context.getCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
                }
            }
        }
        else{

            if(megaApi==null){
                log("setUserAvatar: megaApi is Null in Offline mode");
                return;
            }

            if (context.getExternalCacheDir() != null){
                megaApi.getUserAvatar(userHandleEncoded, context.getExternalCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
            }
            else{
                megaApi.getUserAvatar(userHandleEncoded, context.getCacheDir().getAbsolutePath() + "/" + userHandleEncoded + ".jpg", listener);
            }
        }
    }

    public void createDefaultAvatarContact(ViewHolderMessageChat holder, String userHandle, String name){
        log("createDefaultAvatarContact");
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        String color = megaApi.getUserAvatarColor(userHandle);
        if(userHandle!=null){
            if(color!=null){
                log("The color to set the avatar is "+color);
                p.setColor(Color.parseColor(color));
            }
            else{
                log("Default color to the avatar");
                p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
            }
        }
        else{
            p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        holder.contactImageView.setImageBitmap(defaultAvatar);

        if (name != null){
            if (name.trim().length() > 0){
                String firstLetter = name.charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                holder.contactInitialLetter.setText(firstLetter);
                holder.contactInitialLetter.setTextColor(Color.WHITE);
                holder.contactInitialLetter.setVisibility(View.VISIBLE);
            }
        }
    }

    public void createDefaultAvatar(ViewHolderMessageChat holder, String userHandle, String name, boolean isMyMsg){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        String color = megaApi.getUserAvatarColor(userHandle);
        if(userHandle!=null){
            if(color!=null){
                log("The color to set the avatar is "+color);
                p.setColor(Color.parseColor(color));
            }
            else{
                log("Default color to the avatar");
                p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
            }
        }
        else{
            p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
        }


        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        if(isMyMsg){
            holder.contentOwnMessageContactThumb.setImageBitmap(defaultAvatar);

            if (name != null){
                if (name.trim().length() > 0){
                    String firstLetter = name.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    holder.contentOwnMessageContactInitialLetter.setText(firstLetter);
                    holder.contentOwnMessageContactInitialLetter.setTextColor(Color.WHITE);
                    holder.contentOwnMessageContactInitialLetter.setVisibility(View.VISIBLE);
                }
            }
            holder.contentOwnMessageContactInitialLetter.setTextSize(24);
        }
        else{
            holder.contentContactMessageContactThumb.setImageBitmap(defaultAvatar);
            if (name != null){
                if (name.trim().length() > 0){
                    String firstLetter = name.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    holder.contentContactMessageContactInitialLetter.setText(firstLetter);
                    holder.contentContactMessageContactInitialLetter.setTextColor(Color.WHITE);
                    holder.contentContactMessageContactInitialLetter.setVisibility(View.VISIBLE);
                }
            }
            holder.contentContactMessageContactInitialLetter.setTextSize(24);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size()+1;
    }

    @Override
    public int getItemViewType(int position) {
        if(position==0)
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
        if(this.multipleSelect)
        {
            selectedItems = new SparseBooleanArray();
        }
    }

    public void toggleSelection(int pos) {
        log("toggleSelection");

        if (selectedItems.get(pos, false)) {
            log("delete pos: "+pos);
            selectedItems.delete(pos);
        }
        else {
            log("PUT pos: "+pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    public void selectAll(){
        for (int i= 0; i<this.getItemCount();i++){
            if(!isItemChecked(i)){
                toggleSelection(i);
            }
        }
    }

    public void updateSelectionOnScroll(){
        log("updateSelectionOnScroll");

        List<Integer> selected = getSelectedItems();
        selectedItems.clear();

        for(int i=0;i<selected.size();i++){
            int pos = selected.get(i);
            selectedItems.put(pos+1, true);
            notifyItemChanged(pos);
            notifyItemChanged(pos+1);
        }
    }

    public void clearSelections() {
        log("clearSelection");
        if(selectedItems!=null){
            selectedItems.clear();
        }
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
                return messages.get(position-1);
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

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {

                AndroidMegaChatMessage m = getMessageAt(selectedItems.keyAt(i));
                if (m != null){
                    messages.add(m);
                }
            }
        }
        return messages;
    }

    public Object getItem(int position) {
        return messages.get(position-1);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setMessages (ArrayList<AndroidMegaChatMessage> messages){
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void modifyMessage(ArrayList<AndroidMegaChatMessage> messages, int position){
        this.messages = messages;
        notifyItemChanged(position);
    }

    public void appendMessage(ArrayList<AndroidMegaChatMessage> messages){
        this.messages = messages;
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessage(ArrayList<AndroidMegaChatMessage> messages, int position){
        log("addMessage: "+position);
        this.messages = messages;
        notifyItemInserted(position);
        if(position==messages.size()){
            log("No need to update more");
        }
        else{
            log("Update until end");
            int itemCount = messages.size()-position;
            log("itemCount: "+itemCount);
            notifyItemRangeChanged(position, itemCount+1);
        }
    }

    public void removeMessage(int position, ArrayList<AndroidMegaChatMessage> messages){
        log("removeMessage: size: "+messages.size());
        this.messages = messages;
        notifyItemRemoved(position);

        if(position==messages.size()-1){
            log("No need to update more");
        }
        else{
            log("Update until end");
            int itemCount = messages.size()-position;
            log("itemCount: "+itemCount);
            notifyItemRangeChanged(position, itemCount);
        }
    }

    public void loadPreviousMessage(ArrayList<AndroidMegaChatMessage> messages){
        this.messages = messages;
        notifyItemInserted(0);
    }

    public void loadPreviousMessages(ArrayList<AndroidMegaChatMessage> messages, int counter){
        log("loadPreviousMessages: "+counter);
        this.messages = messages;
        notifyItemRangeInserted(0, counter);
    }

    private void setErrorStateOnPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap){
        log("setErrorStateOnPreview");
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

            if(MimeTypeList.typeForName(name).isVideo()){
                holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);
            }else{
               holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
               holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
            }

            holder.errorUploadingPortrait.setVisibility(View.VISIBLE);
            holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);
            holder.retryAlert.setVisibility(View.VISIBLE);


        }else{
            log("Landscape");
            holder.errorUploadingPortrait.setVisibility(View.GONE);
            holder.transparentCoatingPortrait.setVisibility(View.GONE);
            holder.ownTriangleIconFile.setVisibility(View.GONE);

            holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

            if(MimeTypeList.typeForName(name).isVideo()){
                holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);
            }else{
                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
            }
            holder.errorUploadingLandscape.setVisibility(View.VISIBLE);
            holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);

            holder.retryAlert.setVisibility(View.VISIBLE);
        }
    }

    private void setOwnPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap, MegaNode node){
        log("setOwnPreview");

        if(holder!=null) {
            if (bitmap.getWidth() < bitmap.getHeight()) {
                log("Portrait");

                holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                if (MimeTypeList.typeForName(node.getName()).isPdf()){
                    log("Is pfd preview");
                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                }else if (MimeTypeList.typeForName(node.getName()).isVideo()){
                    log("Is video preview");
                    holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                    holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);
                    holder.videoTimecontentOwnMessageThumbPort.setText(timeVideo(node));
                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.VISIBLE);

                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

                }else{
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

                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);


            } else {
                log("Landscape");

                holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                if (MimeTypeList.typeForName(node.getName()).isPdf()){
                    log("Is pfd preview");
                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);

                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                }else if (MimeTypeList.typeForName(node.getName()).isVideo()){
                    log("Is video preview");
                    holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                    holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);
                    holder.videoTimecontentOwnMessageThumbLand.setText(timeVideo(node));
                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.VISIBLE);

                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);

                }else{
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

                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
            }
        }
    }

    private void setContactPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap, MegaNode node) {
        log("setContactPreview");
        if (bitmap.getWidth() < bitmap.getHeight()) {
            log("Portrait");
            holder.contentContactMessageThumbPort.setImageBitmap(bitmap);
            holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (MimeTypeList.typeForName(node.getName()).isPdf()){
                log("Contact message - Is pfd preview");
                holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

            }else if(MimeTypeList.typeForName(node.getName()).isVideo()){
                log("Contact message - Is video preview");
                holder.gradientContactMessageThumbPort.setVisibility(View.VISIBLE);
                holder.videoIconContactMessageThumbPort.setVisibility(View.VISIBLE);
                holder.videoTimecontentContactMessageThumbPort.setText(timeVideo(node));
                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.VISIBLE);

                holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

            }else{
                holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);
                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
            }

            holder.contentContactMessageThumbPort.setVisibility(View.VISIBLE);
//                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.VISIBLE);

            holder.contentContactMessageThumbLand.setVisibility(View.GONE);
            holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
//                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);


            holder.contentContactMessageFileThumb.setVisibility(View.GONE);
            holder.contentContactMessageFileName.setVisibility(View.GONE);
            holder.contentContactMessageFileSize.setVisibility(View.GONE);

            RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
            contactThumbParams.setMargins(0, 0 ,0, 0);
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

            if (MimeTypeList.typeForName(node.getName()).isPdf()){
                log("Contact message - Is pfd preview");
                holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);

                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

            }else if(MimeTypeList.typeForName(node.getName()).isVideo()){
                log("Contact message - Is video preview");
                holder.gradientContactMessageThumbLand.setVisibility(View.VISIBLE);
                holder.videoIconContactMessageThumbLand.setVisibility(View.VISIBLE);
                holder.videoTimecontentContactMessageThumbLand.setText(timeVideo(node));
                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.VISIBLE);

                holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);

            }else{
                holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);
                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
            }

            holder.contentContactMessageThumbLand.setVisibility(View.VISIBLE);
//                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.VISIBLE);
            holder.contentContactMessageThumbPort.setVisibility(View.GONE);
            holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
            holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);
            holder.contentContactMessageFileThumb.setVisibility(View.GONE);
            holder.contentContactMessageFileName.setVisibility(View.GONE);
            holder.contentContactMessageFileSize.setVisibility(View.GONE);

            RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
            contactThumbParams.setMargins(0, 0 ,0, 0);
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

    private void setPreview(long handle, MegaChatLollipopAdapter.ViewHolderMessageChat holder){
        log("setPreview: "+handle);

        if(holder!=null){
            File previewDir = PreviewUtils.getPreviewFolder(context);
            String base64 = MegaApiJava.handleToBase64(handle);
            File preview = new File(previewDir, base64+".jpg");
            if (preview.exists()) {

                if (preview.length() > 0) {
                    Bitmap bitmap = PreviewUtils.getBitmapForCache(preview, context);

                    if(bitmap!=null){

                        PreviewUtils.previewCache.put(handle, bitmap);

                        if(holder.userHandle == megaChatApi.getMyUserHandle()){

                            String name = holder.contentOwnMessageFileName.getText().toString();
                            MegaNode node = megaApi.getNodeByHandle(handle);

                            log("Update my preview: "+name);
                            if (bitmap.getWidth() < bitmap.getHeight()) {
                                log("Portrait");

                                holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                                holder.contentOwnMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(name).isPdf()){
                                    log("Is pfd preview");
                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                    holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);

                                }else if (MimeTypeList.typeForName(name).isVideo()){
                                    log("Is video preview");

                                    holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);

                                    if(node!=null){
                                       holder.videoTimecontentOwnMessageThumbPort.setText(timeVideo(node));
                                       holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                    }else{
                                       holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                                    }

                                    holder.iconOwnTypeDocPortraitPreview.setVisibility(View.GONE);

                                }else{
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

                                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                            }
                            else {
                                log("Landscape");
                                holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                                holder.contentOwnMessageThumbLand.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(name).isPdf()){
                                    log("Is pfd preview");
                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.VISIBLE);

                                    holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);

                                }else if (MimeTypeList.typeForName(name).isVideo()){
                                    log("Is video preview");
                                    holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);

                                    if(node!=null){
                                        holder.videoTimecontentOwnMessageThumbLand.setText(timeVideo(node));
                                        holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                    }else{
                                        holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);
                                    }

                                    holder.iconOwnTypeDocLandPreview.setVisibility(View.GONE);

                                }else{
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

                                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoTimecontentOwnMessageThumbPort.setVisibility(View.GONE);
                            }
                        }
                        else{
                            log("Update my contacts preview");
                            String name = holder.contentContactMessageFileName.getText().toString();
                            MegaNode node = megaApi.getNodeByHandle(handle);

                            if (bitmap.getWidth() < bitmap.getHeight()) {

                                log("Portrait");
                                holder.contentContactMessageThumbPort.setImageBitmap(bitmap);
                                holder.contentContactMessageThumbPort.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                if (MimeTypeList.typeForName(name).isPdf()){
                                    log("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.VISIBLE);

                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);

                                }else if(MimeTypeList.typeForName(name).isVideo()){
                                    log("Contact message - Is video preview");

                                    holder.gradientContactMessageThumbPort.setVisibility(View.VISIBLE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.VISIBLE);

                                    if(node!=null){
                                        holder.videoTimecontentContactMessageThumbPort.setText(timeVideo(node));
                                        holder.videoTimecontentContactMessageThumbPort.setVisibility(View.VISIBLE);
                                    }else{
                                        holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                                    }

                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);

                                }else{
                                    holder.iconContactTypeDocPortraitPreview.setVisibility(View.GONE);
                                    holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
                                }

                                holder.contentContactMessageThumbPort.setVisibility(View.VISIBLE);
//                                holder.contentContactMessageThumbPortFramework.setVisibility(View.VISIBLE);

                                holder.contentContactMessageThumbLand.setVisibility(View.GONE);
                                holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

//                                holder.contentContactMessageThumbLandFramework.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbPort.getLayoutParams();
                                contactThumbParams.setMargins(0, 0 ,0, 0);
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

                                if (MimeTypeList.typeForName(name).isPdf()){
                                    log("Contact message - Is pfd preview");
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.VISIBLE);

                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);

                                }else if(MimeTypeList.typeForName(name).isVideo()){
                                    log("Contact message - Is video preview");
                                    holder.gradientContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.VISIBLE);

                                    if(node!=null){
                                        holder.videoTimecontentContactMessageThumbLand.setText(timeVideo(node));
                                        holder.videoTimecontentContactMessageThumbLand.setVisibility(View.VISIBLE);
                                    }else{
                                        holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
                                    }

                                    holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);

                                }else{
                                    holder.iconContactTypeDocLandPreview.setVisibility(View.GONE);
                                    holder.gradientContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoIconContactMessageThumbLand.setVisibility(View.GONE);
                                    holder.videoTimecontentContactMessageThumbLand.setVisibility(View.GONE);
                                }

                                holder.contentContactMessageThumbLand.setVisibility(View.VISIBLE);
//                                holder.contentContactMessageThumbLandFramework.setVisibility(View.VISIBLE);

                                holder.contentContactMessageThumbPort.setVisibility(View.GONE);
                                holder.gradientContactMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconContactMessageThumbPort.setVisibility(View.GONE);
                                holder.videoTimecontentContactMessageThumbPort.setVisibility(View.GONE);
//                                holder.contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbLand.getLayoutParams();
                                contactThumbParams.setMargins(0, 0 ,0, 0);
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
            }
            else{
                log("Preview not exists");
            }
        }
    }

    private void setUploadingPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap){
        log("setUploadingPreview: "+holder.filePathUploading);

        if(holder!=null){

            holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
            holder.ownManagementMessageLayout.setVisibility(View.GONE);
            holder.titleOwnMessage.setGravity(Gravity.RIGHT);
            holder.timeOwnText.setGravity(Gravity.RIGHT);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.timeOwnText.getLayoutParams();
            params.rightMargin = Util.scaleWidthPx(16, outMetrics);
            params.leftMargin = Util.scaleWidthPx(0, outMetrics);
            holder.timeOwnText.setLayoutParams(params);

            if(bitmap!=null){
                log("Bitmap not null - Update uploading my preview");

                int currentPosition = holder.getLayoutPosition();
                log("currentPosition holder: "+currentPosition);

                if(currentPosition==-1){
                    log("The position cannot be recovered - had changed");
                    for(int i=messages.size()-1;i>=0;i--){
                        AndroidMegaChatMessage message = messages.get(i);
                        if(message.isUploading()){
                            String path = message.getPendingMessage().getFilePath();
                            if(path.equals(holder.filePathUploading)){
                                currentPosition = i+1;
                                log("Found current position: "+currentPosition);
                                break;
                            }
                        }
                    }
                }
                log("Messages size: "+messages.size());
                if(currentPosition>messages.size()){
                    log("Position not valid");
                    return;
                }

                AndroidMegaChatMessage message = messages.get(currentPosition-1);
                if(message.getPendingMessage()!=null) {
                    log("State of the message: " + message.getPendingMessage().getState());
                    log("Attachment: "+message.getPendingMessage().getFilePath());

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

                    if (message.getPendingMessage().getState() == PendingMessage.STATE_ERROR) {
                        log("Message is on ERROR state");

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

                            if (MimeTypeList.typeForName(name).isVideo()){
                                holder.gradientOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.VISIBLE);

                            }else{
                                holder.gradientOwnMessageThumbPort.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbPort.setVisibility(View.GONE);
                            }

                            holder.errorUploadingPortrait.setVisibility(View.VISIBLE);
                            holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);

                        }else{
                            log("Landscape");
                            holder.transparentCoatingPortrait.setVisibility(View.GONE);
                            holder.errorUploadingPortrait.setVisibility(View.GONE);
                            holder.videoTimecontentOwnMessageThumbLand.setVisibility(View.GONE);

                            if (MimeTypeList.typeForName(name).isVideo()){
                                holder.gradientOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.VISIBLE);

                            }else{
                                holder.gradientOwnMessageThumbLand.setVisibility(View.GONE);
                                holder.videoIconOwnMessageThumbLand.setVisibility(View.GONE);
                            }

                            holder.errorUploadingLandscape.setVisibility(View.VISIBLE);
                            holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);


                        }

                    } else {
                        log("Message is in progress state");
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
                        }else{
                            log("Landscape");
                            holder.transparentCoatingPortrait.setVisibility(View.GONE);
                            holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarLand.setVisibility(View.VISIBLE);
                            holder.uploadingProgressBarPort.setVisibility(View.GONE);
                        }
                    }
                }else{

                    log("The pending message is NULL-- cannot set preview");
                }
            }else{
                log("Bitmap is NULL");
            }
        }else{
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

        PreviewDownloadListener(Context context, MegaChatLollipopAdapter.ViewHolderMessageChat holder, MegaChatLollipopAdapter adapter) {
            this.context = context;
            this.holder = holder;
            this.adapter = adapter;
        }

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {

        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

            log("onRequestFinish: "+request.getType() + "__" + request.getRequestString());
            log("onRequestFinish: Node: " + request.getNodeHandle() + "_" + request.getName());

            if (request.getType() == MegaRequest.TYPE_GET_ATTR_FILE){
                if (e.getErrorCode() == MegaError.API_OK){

                    long handle = request.getNodeHandle();
                    pendingPreviews.remove(handle);

                    setPreview(handle, holder);
                }
                else {
                    log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }

        @Override
        public void onRequestTemporaryError (MegaApiJava api, MegaRequest request, MegaError e){

        }

        @Override
        public void onRequestUpdate (MegaApiJava api, MegaRequest request){

        }
    }

    public String timeVideo(MegaNode n){
        int duration = n.getDuration();
        String timeString = "";

        if(duration>0){
            int hours = duration / 3600;
            int minutes = (duration % 3600) / 60;
            int seconds = duration % 60;

            if(hours>0){
                timeString = String.format("%d:%d:%02d", hours, minutes, seconds);
            }
            else{
                timeString = String.format("%d:%02d", minutes, seconds);
            }

            log("The duration is: "+hours+" "+minutes+" "+seconds);
        }
        return timeString;
    }

}
