package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.WrapTextView;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatAttachmentAvatarListener;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.PendingMessage;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
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
import nz.mega.sdk.MegaUser;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    public static int MAX_WIDTH_NAME_SENDER_GROUP_THUMB_LAND_PICTURE=194;
    public static int MAX_WIDTH_NAME_SENDER_GROUP_THUMB_PORTRAIT_PICTURE=136;

    public static int MAX_WIDTH_FILENAME_LAND=491;
    public static int MAX_WIDTH_FILENAME_PORT=220;

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

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap preview = BitmapFactory.decodeFile(currentFile.getAbsolutePath(), options);

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

        RelativeLayout previewFrameContactPort;


        RelativeLayout dateLayout;
        TextView dateText;
        RelativeLayout ownMessageLayout;
        RelativeLayout titleOwnMessage;
//        TextView meText;
        TextView timeOwnText;
        RelativeLayout contentOwnMessageLayout;
        TextView contentOwnMessageText;

        ImageView contentOwnMessageThumbLand;
        ImageView contentOwnMessageThumbPort;
        RelativeLayout contentOwnMessageFileLayout;
        ImageView contentOwnMessageFileThumb;
        TextView contentOwnMessageFileName;
        TextView contentOwnMessageFileSize;

        RelativeLayout contentOwnMessageContactLayout;
        RelativeLayout contentOwnMessageContactLayoutAvatar;
        RoundedImageView contentOwnMessageContactThumb;
        TextView contentOwnMessageContactName;
        public TextView contentOwnMessageContactEmail;
        TextView contentOwnMessageContactInitialLetter;

        RelativeLayout transparentCoatingLandscape;
        RelativeLayout transparentCoatingPortrait;
        ProgressBar uploadingProgressBar;

        RelativeLayout errorUploadingLayout;

        TextView retryAlert;
        ImageView triangleIcon;

        RelativeLayout contactMessageLayout;
        RelativeLayout titleContactMessage;
//        TextView contactText;
        TextView timeContactText;
        RelativeLayout contentContactMessageLayout;
        TextView contentContactMessageText;

        ImageView contentContactMessageThumbLand;
        ImageView contentContactMessageThumbLandFramework;
        ImageView contentContactMessageThumbPort;
        ImageView contentContactMessageThumbPortFramework;

        RelativeLayout contentContactMessageFileLayout;
        ImageView contentContactMessageFileThumb;
        TextView contentContactMessageFileName;
        TextView contentContactMessageFileSize;
        TextView contentContactMessageFileSender;

        RelativeLayout contentContactMessageContactLayout;
        RelativeLayout contentContactMessageContactLayoutAvatar;
        RoundedImageView contentContactMessageContactThumb;
        TextView contentContactMessageContactName;
        public TextView contentContactMessageContactEmail;
        TextView contentContactMessageContactInitialLetter;
        TextView contentContactMessageContactSender;

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

            //Own messages
            holder.ownMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_own_message_layout);
            holder.titleOwnMessage = (RelativeLayout) v.findViewById(R.id.title_own_message_layout);
//        holder.meText = (TextView) v.findViewById(R.id.message_chat_me_text);

            holder.previewFramePort = (RelativeLayout) v.findViewById(R.id.preview_frame_portrait);
            holder.previewFrameLand = (RelativeLayout) v.findViewById(R.id.preview_frame_landscape);

            // holder.previewFrameContactPort = (RelativeLayout) v.findViewById(R.id.preview_frame_contact_portrait);

            holder.timeOwnText = (TextView) v.findViewById(R.id.message_chat_time_text);

            holder.contentOwnMessageLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_layout);

            holder.contentOwnMessageText = (WrapTextView) v.findViewById(R.id.content_own_message_text);

            holder.contentOwnMessageThumbLand = (ImageView)  v.findViewById(R.id.content_own_message_thumb_landscape);
            holder.contentOwnMessageThumbPort = (ImageView)  v.findViewById(R.id.content_own_message_thumb_portrait);

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

            holder.retryAlert = (TextView) v.findViewById(R.id.not_sent_own_message_text);

            holder.triangleIcon = (ImageView)  v.findViewById(R.id.own_triangle_icon);

            holder.transparentCoatingPortrait = (RelativeLayout) v.findViewById(R.id.transparent_coating_portrait);
            holder.transparentCoatingPortrait.setVisibility(View.GONE);

            holder.transparentCoatingLandscape = (RelativeLayout) v.findViewById(R.id.transparent_coating_landscape);
            holder.transparentCoatingLandscape.setVisibility(View.GONE);

            holder.uploadingProgressBar = (ProgressBar) v.findViewById(R.id.uploadingProgressBar);
            holder.uploadingProgressBar.setVisibility(View.GONE);

            holder.errorUploadingLayout = (RelativeLayout) v.findViewById(R.id.error_uploading_relative_layout);
            holder.errorUploadingLayout.setVisibility(View.GONE);

            holder.ownManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.own_management_message_layout);
            //Margins
            RelativeLayout.LayoutParams ownManagementParams = (RelativeLayout.LayoutParams)holder.ownManagementMessageLayout.getLayoutParams();
            ownManagementParams.addRule(RelativeLayout.ALIGN_RIGHT);
            ownManagementParams.setMargins(0, 0, 0, Util.scaleHeightPx(4, outMetrics));
            holder.ownManagementMessageLayout.setLayoutParams(ownManagementParams);

            holder.ownManagementMessageText = (TextView) v.findViewById(R.id.own_management_message_text);

            //Contact messages////////////////////////////////////////
            holder.contactMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_contact_message_layout);
            holder.titleContactMessage = (RelativeLayout) v.findViewById(R.id.title_contact_message_layout);
//        holder.contactText = (TextView) v.findViewById(R.id.message_chat_contact_text);

            holder.timeContactText = (TextView) v.findViewById(R.id.contact_message_chat_time_text);

            holder.contentContactMessageLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_layout);

            holder.contentContactMessageText = (WrapTextView) v.findViewById(R.id.content_contact_message_text);

            holder.contentContactMessageThumbLand = (ImageView)  v.findViewById(R.id.content_contact_message_thumb_landscape);
            holder.contentContactMessageThumbLandFramework = (ImageView)  v.findViewById(R.id.content_contact_message_thumb_landscape_framework);

            holder.contentContactMessageThumbPort = (ImageView)  v.findViewById(R.id.content_contact_message_thumb_portrait);
            holder.contentContactMessageThumbPortFramework = (ImageView)  v.findViewById(R.id.content_contact_message_thumb_portrait_framework);


            holder.contentContactMessageFileLayout = (RelativeLayout)  v.findViewById(R.id.content_contact_message_file_layout);
            holder.contentContactMessageFileSender = (TextView)  v.findViewById(R.id.content_contact_message_file_sender);
            holder.contentContactMessageFileThumb = (ImageView)  v.findViewById(R.id.content_contact_message_file_thumb);
            holder.contentContactMessageFileName = (TextView)  v.findViewById(R.id.content_contact_message_file_name);
            holder.contentContactMessageFileSize = (TextView)  v.findViewById(R.id.content_contact_message_file_size);

            holder.contentContactMessageContactLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout);
            holder.contentContactMessageContactLayoutAvatar = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout_avatar);
            holder.contentContactMessageContactThumb = (RoundedImageView) v.findViewById(R.id.content_contact_message_contact_thumb);
            holder.contentContactMessageContactName = (TextView)  v.findViewById(R.id.content_contact_message_contact_name);
            holder.contentContactMessageContactEmail = (TextView)  v.findViewById(R.id.content_contact_message_contact_email);
            holder.contentContactMessageContactInitialLetter = (TextView)  v.findViewById(R.id.content_contact_message_contact_initial_letter);
            holder.contentContactMessageContactSender = (TextView)  v.findViewById(R.id.content_contact_message_contact_sender);

            holder.contactManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.contact_management_message_layout);
            //Margins
            RelativeLayout.LayoutParams contactManagementParams = (RelativeLayout.LayoutParams)holder.contactManagementMessageLayout.getLayoutParams();
            contactManagementParams.addRule(RelativeLayout.ALIGN_LEFT);
            holder.contactManagementMessageLayout.setLayoutParams(contactManagementParams);

            holder.contactManagementMessageText = (TextView) v.findViewById(R.id.contact_management_message_text);

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
        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

        AndroidMegaChatMessage message = messages.get(position-1);

        if(messages.get(position-1).getInfoToShow()!=-1){
            switch (messages.get(position-1).getInfoToShow()){
                case Constants.CHAT_ADAPTER_SHOW_ALL:{
                    ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getPendingMessage().getUploadTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                    ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message.getPendingMessage().getUploadTimestamp()));
                    break;
                }
                case Constants.CHAT_ADAPTER_SHOW_TIME:{
                    log("CHAT_ADAPTER_SHOW_TIME");
                    ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message.getPendingMessage().getUploadTimestamp()));
                    break;
                }
                case Constants.CHAT_ADAPTER_SHOW_NOTHING:
                case Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME:{
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



        ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.VISIBLE);

        ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.VISIBLE);
        ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.VISIBLE);
        ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.VISIBLE);

        ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

        ArrayList<String> paths = message.getPendingMessage().getFilePaths();
        ArrayList<String> names = message.getPendingMessage().getNames();
        if(paths != null){

            if(paths.size()==1) {
                log("One attachment in uploading message");

                Bitmap preview = null;
                ((ViewHolderMessageChat)holder).filePathUploading = paths.get(0);
                log("Path of the file: "+paths.get(0));
                long fingerprintCache = MegaApiAndroid.base64ToHandle(megaApi.getFingerprint(paths.get(0)));

                if (MimeTypeList.typeForName(paths.get(0)).isImage()){

                    try{
                        preview = PreviewUtils.getPreviewFromCache(fingerprintCache);
                        if (preview != null){
                            setUploadingPreview((ViewHolderMessageChat)holder, preview);
                        }

                        try{
                            new MegaChatLollipopAdapter.ChatUploadingPreviewAsyncTask(((ViewHolderMessageChat)holder)).execute(paths.get(0));
                        }
                        catch(Exception e){
                            //Too many AsyncTasks
                        }

                    }
                    catch(Exception e){}
                }

                String name = names.get(0);
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

            }
            else{
                log("Several attachments in uploading message");

                ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(names.get(0)).getIconResourceId());

                ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, paths.size(), paths.size()));
            }

            log("State of the message: "+message.getPendingMessage().getState());
            if(message.getPendingMessage().getState()== PendingMessage.STATE_ERROR){
                ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_error);
            }
            else{
                ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setText(R.string.attachment_uploading_state_uploading);
            }
        }
    }


    public void onBindViewHolderMessage(RecyclerView.ViewHolder holder, int position) {
        log("onBindViewHolderMessage: " + position);

        ((ViewHolderMessageChat) holder).currentPosition = position;

        ((ViewHolderMessageChat) holder).triangleIcon.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).retryAlert.setVisibility(View.GONE);

        ((ViewHolderMessageChat) holder).transparentCoatingLandscape.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).transparentCoatingPortrait.setVisibility(View.GONE);
        ((ViewHolderMessageChat) holder).uploadingProgressBar.setVisibility(View.GONE);

        MegaChatMessage message = messages.get(position-1).getMessage();
        ((ViewHolderMessageChat)holder).userHandle = message.getUserHandle();

        if(message.getType()==MegaChatMessage.TYPE_ALTER_PARTICIPANTS){
            log("ALTER PARTICIPANT MESSAGE!!");

            if(message.getHandleOfAction()==myUserHandle){
                log("me alter participant");

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME:{
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
                        log("Selected: "+message.getContent());
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
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME:{
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
                        log("Selected: " + message.getContent());
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
            log("PRIVILEGE CHANGE message");
            if(message.getHandleOfAction()==myUserHandle){
                log("a moderator change my privilege");
                int privilege = message.getPrivilege();
                log("Privilege of the user: "+privilege);

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME:{
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
                        log("Selected: " + message.getContent());
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                    } else {
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }
            }
            else{
                log("Participant privilege change!");
                log("Message type PRIVILEGE CHANGE: "+message.getContent());

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME:{
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
                        log("Selected: " + message.getContent());
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
            if(message.getUserHandle()==myUserHandle) {
                log("MY message!!");
                log("MY message handle!!: "+message.getMsgId());

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME:{
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

                        ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageContactLayout.setVisibility(View.GONE);

                        Spannable content = new SpannableString(messageContent);
                        int status = message.getStatus();
                        if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                            log("Show triangle retry!");
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.mail_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                        }
                        else if((status==MegaChatMessage.STATUS_SENDING)){
                            log("Status not received by server: "+message.getStatus());
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.mail_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        else{
                            log("Status: "+message.getStatus());
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(content+" ");

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        ((ViewHolderMessageChat)holder).contentOwnMessageText.append(edited);
                        ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.GONE);

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
                                log("Selected: "+message.getContent());
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

                        ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

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
                                log("Selected: " + message.getContent());
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

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

                            if(message.getContent()!=null){
                                messageContent = message.getContent();
                            }

                            int status = message.getStatus();

                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                                log("Show triangle retry!");
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                                ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                            }
                            else if((status==MegaChatMessage.STATUS_SENDING)){
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                            }
                            else{
                                log("Status: "+message.getStatus());
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                            }

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);


                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.VISIBLE);

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

                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileName.setText(node.getName());
                                    long nodeSize = node.getSize();
                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileSize.setText(Util.getSizeString(nodeSize));

                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                                    if (node.hasPreview()){
                                        log("Get preview of node");
                                        Bitmap preview = null;
                                        preview = PreviewUtils.getPreviewFromCache(node);
                                        if (preview != null) {

                                            PreviewUtils.previewCache.put(node.getHandle(), preview);
                                            if (preview.getWidth() < preview.getHeight()) {
                                                log("Portrait");

                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setImageBitmap(preview);
                                                ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);

                                            } else {
                                                log("Landcape");

                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setImageBitmap(preview);
                                                ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);
                                            }

                                        } else {

                                            preview = PreviewUtils.getPreviewFromFolder(node, context);
                                            if (preview != null) {
                                                PreviewUtils.previewCache.put(node.getHandle(), preview);
                                                if (preview.getWidth() < preview.getHeight()) {
                                                    log("Portrait");


                                                    ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setImageBitmap(preview);
                                                    ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.GONE);

                                                } else {
                                                    log("Landcape");

                                                    ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setImageBitmap(preview);
                                                    ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                                    ((ViewHolderMessageChat) holder).contentOwnMessageFileLayout.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).previewFramePort.setVisibility(View.GONE);
                                                    ((ViewHolderMessageChat) holder).contentOwnMessageThumbPort.setVisibility(View.GONE);
                                                }

                                            } else {

                                                if (pendingPreviews.contains(node.getHandle())) {
                                                    log("the preview is already downloaded or added to the list");

                                                } else {

                                                    File previewFile = new File(PreviewUtils.getPreviewFolder(context), node.getBase64Handle() + ".jpg");

                                                    PreviewDownloadListener listener = new PreviewDownloadListener(context, (ViewHolderMessageChat) holder, this);
                                                    //                                                listenersGrid.put(node.getHandle(), listener);
                                                    log("To download here: " + previewFile.getAbsolutePath());

                                                    pendingPreviews.add(node.getHandle());
                                                    megaApi.getPreview(node, previewFile.getAbsolutePath(), listener);
                                                }

                                            }
                                        }
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setElevation(0);
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


                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            messageContent = "Attachment revoked";
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);

                        }
                        else if(message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFrameLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).previewFramePort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);


                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.VISIBLE);

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
                                log("Selected: "+message.getContent());
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
                            log("Selected: " + message.getContent());
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
                    log("Message type TITLE CHANGE: "+message.getContent());

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
                            log("Selected: " + message.getContent());
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

                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

//                log("Content: "+message.getContent());
                }
//                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
            }
            else{
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
                        }
                        else{
                            log("4-Name already asked and no name received: "+ message.getUserHandle());
                        }
                    }
                }
                else{
//                    ((ViewHolderMessageChat)holder).fullNameTitle = getContactFullName(((ViewHolderMessageChat)holder).userHandle);
                    ((ViewHolderMessageChat)holder).fullNameTitle = chatRoom.getTitle();
                }

                if(messages.get(position-1).getInfoToShow()!=-1){
                    switch (messages.get(position-1).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            log("CHAT_ADAPTER_SHOW_TIME--");
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            ((ViewHolderMessageChat)holder).timeContactText.setVisibility(View.VISIBLE);
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME:{
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

                    if(((ChatActivityLollipop) context).isGroup()){

//                    name.setSpan(new RelativeSizeSpan(0.85f), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        String handleString = megaApi.userHandleToBase64(((ViewHolderMessageChat)holder).userHandle);
                        String color = megaApi.getUserAvatarColor(handleString);

                        if(message.getType()==MegaChatMessage.TYPE_NORMAL){

                            if(messages.get(position-1).getInfoToShow()==Constants.CHAT_ADAPTER_SHOW_NOTHING_NO_NAME){
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setText("");
                            }else{
                                Spannable name = new SpannableString(((ViewHolderMessageChat)holder).fullNameTitle+"\n");
                                if(color!=null){
                                    log("The color to set the avatar is "+color);
                                    name.setSpan(new ForegroundColorSpan(Color.parseColor(color)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                else{
                                    log("Default color to the avatar");
                                    name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                ((ViewHolderMessageChat)holder).contentContactMessageText.setText(name);
                                ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.GONE);

                            }


                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.VISIBLE);
                            if(color!=null){
                                log("TYPE_NODE_ATTACHMENT The color to set the avatar is "+color);
                                ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setTextColor(Color.parseColor(color));
                            }
                            else{
                                log("TYPE_NODE_ATTACHMENT Default color to the avatar");
                                ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            }
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setText(((ViewHolderMessageChat)holder).fullNameTitle);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.VISIBLE);
                            if(color!=null){
                                log("The color to set the avatar is "+color);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setTextColor(Color.parseColor(color));
                            }
                            else{
                                log("Default color to the avatar");
                                ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            }
                            ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setText(((ViewHolderMessageChat)holder).fullNameTitle);
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contentContactMessageText.setText("");
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
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).previewFrameContactPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageContactLayout.setVisibility(View.GONE);

                        Spannable content = new SpannableString(messageContent);
                        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ((ViewHolderMessageChat)holder).contentContactMessageText.append(content+" ");
//                    ((ViewHolderMessageChat)holder).contentContactMessageText.setText(content);

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ((ViewHolderMessageChat)holder).contentContactMessageText.append(edited);
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
                                log("Selected: "+message.getContent());
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

                        ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).previewFrameContactPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                        ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

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
                                log("Selected: " + message.getContent());

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
                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);

                            if(message.getContent()!=null){
                                messageContent = message.getContent();
                            }
                            ((ViewHolderMessageChat)holder).contentContactMessageText.append(messageContent);

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);


                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.VISIBLE);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setElevation(0);
                            }

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
                                    preview = PreviewUtils.getPreviewFromCache(node);
                                    if (preview != null){
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setElevation(2);
                                        }

                                        log("Success -> getPreviewFromCache");
                                        PreviewUtils.previewCache.put(node.getHandle(), preview);
                                        if (preview.getWidth() < preview.getHeight()) {
                                            log("Portrait");

                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setImageBitmap(preview);

                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.VISIBLE);
                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.VISIBLE);

                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);

                                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                                            if(chatRoom.isGroup()){
                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
                                                contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setBackgroundResource(R.drawable.shape_images_chat_3);



                                                log("Max width to MAX_WIDTH_NAME_SENDER_GROUP_THUMB");
                                                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_NAME_SENDER_GROUP_THUMB_PORTRAIT_PICTURE, context.getResources().getDisplayMetrics());
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setMaxWidth((int) width);
                                            }
                                            else{
                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
                                                contactThumbParams.setMargins(0, 0 ,0, 0);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setBackgroundResource(R.drawable.shape_images_chat);


                                            }

                                        } else {
                                            log("Landcape");

                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setImageBitmap(preview);
                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.VISIBLE);
                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.VISIBLE);

                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                                            ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);


                                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                                            if(chatRoom.isGroup()){
                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
                                                contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setBackgroundResource(R.drawable.shape_images_chat_3);



                                                log("Max width to 220");
                                                float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_NAME_SENDER_GROUP_THUMB_LAND_PICTURE, context.getResources().getDisplayMetrics())- 8;
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setMaxWidth((int) width);
                                            }
                                            else{
                                                RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
                                                contactThumbParams.setMargins(0, 0 ,0, 0);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
                                                ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setBackgroundResource(R.drawable.shape_images_chat);


                                            }
                                        }
                                    }
                                    else {


                                        log("Fail -> getPreviewFromCache");
                                        preview = PreviewUtils.getPreviewFromFolder(node, context);
                                        if (preview != null) {

                                            log("SUCCESS -> getPreviewFromFolder");
                                            PreviewUtils.previewCache.put(node.getHandle(), preview);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setElevation(2);
                                            }

                                            if (preview.getWidth() < preview.getHeight()) {
                                                log("Portrait");
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setImageBitmap(preview);

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.VISIBLE);

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);


                                                ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                                                if(chatRoom.isGroup()){
                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
                                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setBackgroundResource(R.drawable.shape_images_chat_3);



                                                    log("Max width to MAX_WIDTH_NAME_SENDER_GROUP_THUMB");
                                                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_NAME_SENDER_GROUP_THUMB_PORTRAIT_PICTURE, context.getResources().getDisplayMetrics());
                                                    ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setMaxWidth((int) width);
                                                }
                                                else{
                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbPort.getLayoutParams();
                                                    contactThumbParams.setMargins(0, 0 ,0, 0);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setBackgroundResource(R.drawable.shape_images_chat);


                                                }

                                            } else {
                                                log("Landcape");

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setImageBitmap(preview);
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLand.setVisibility(View.VISIBLE);
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbLandFramework.setVisibility(View.VISIBLE);

                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat) holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                                                ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                                                if(chatRoom.isGroup()){
                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
                                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics),0, 0);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setBackgroundResource(R.drawable.shape_images_chat_3);



                                                    log("Max width to 220");
                                                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_NAME_SENDER_GROUP_THUMB_LAND_PICTURE, context.getResources().getDisplayMetrics())- 8;
                                                    ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setMaxWidth((int) width);
                                                }
                                                else{
                                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageThumbLand.getLayoutParams();
                                                    contactThumbParams.setMargins(0, 0 ,0, 0);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
                                                    ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setBackgroundResource(R.drawable.shape_images_chat);


                                                }
                                            }

                                        } else {

                                            log("Fail -> getPreviewFromFolder");
                                            if (node.hasPreview()){
                                                log("Node has preview");

                                                if (pendingPreviews.contains(node.getHandle())){
                                                    log("the preview is already downloaded or added to the list");
                                                }
                                                else{
                                                    File previewFile = new File(PreviewUtils.getPreviewFolder(context), node.getBase64Handle()+".jpg");

                                                    PreviewDownloadListener listener = new PreviewDownloadListener(context, (ViewHolderMessageChat)holder, this);
//                                                listenersGrid.put(node.getHandle(), listener);
                                                    log("To download here: " + previewFile.getAbsolutePath());

                                                    pendingPreviews.add(node.getHandle());
                                                    megaApi.getPreview(node, previewFile.getAbsolutePath(), listener);
                                                }
                                            }
                                            else{
                                                log("Node has NO preview!!!");
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                    ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setElevation(0);
                                                }
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
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);


                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

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
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLandFramework.setVisibility(View.GONE);


                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPortFramework.setVisibility(View.GONE);

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
                                log("Selected: "+message.getContent());
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
                            log("Selected: " + message.getContent());
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                    if(((ChatActivityLollipop) context).isGroup()){
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
                    else{
                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(context.getString(R.string.history_cleared_message));
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
                    log("Message type CHANGE TITLE "+message.getContent());

                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);

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
                            log("Selected: " + message.getContent());
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
                }
            }
        }

        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(-14, outMetrics), Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
//        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);
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

    private void setPreview(long handle, MegaChatLollipopAdapter.ViewHolderMessageChat holder){
        log("setPreview: "+handle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setElevation(2);
        }

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

                            log("Update my preview");
                            if (bitmap.getWidth() < bitmap.getHeight()) {
                                log("Portrait");

                                holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);

                                holder.previewFramePort.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.previewFrameLand.setVisibility(View.GONE);
                                holder.contentOwnMessageThumbLand.setVisibility(View.GONE);
                            }
                            else {

                                log("Landcape");

                                holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);

                                holder.previewFrameLand.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                                holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                                holder.previewFramePort.setVisibility(View.GONE);
                                holder.contentOwnMessageThumbPort.setVisibility(View.GONE);
                            }
                        }
                        else{

                            log("Update my contacts preview");

                            if (bitmap.getWidth() < bitmap.getHeight()) {

                                log("Portrait");

                                holder.contentContactMessageThumbPort.setImageBitmap(bitmap);

                                holder.contentContactMessageThumbPort.setVisibility(View.VISIBLE);
                                holder.contentContactMessageThumbPortFramework.setVisibility(View.VISIBLE);

                                holder.contentContactMessageThumbLand.setVisibility(View.GONE);
                                holder.contentContactMessageThumbLandFramework.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                if(chatRoom.isGroup()){
                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbPort.getLayoutParams();
                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics) ,0, 0);
                                    holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbPortFramework.setBackgroundResource(R.drawable.shape_images_chat_3);

                                    log("Max width to MAX_WIDTH_NAME_SENDER_GROUP_THUMB");
                                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_NAME_SENDER_GROUP_THUMB_PORTRAIT_PICTURE, context.getResources().getDisplayMetrics());
                                    holder.contentContactMessageFileSender.setMaxWidth((int) width);
                                }
                                else{
                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbPort.getLayoutParams();
                                    contactThumbParams.setMargins(0, 0 ,0, 0);
                                    holder.contentContactMessageThumbPort.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbPortFramework.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbPortFramework.setBackgroundResource(R.drawable.shape_images_chat);
                                }

                            } else {

                                log("Landcape");

                                holder.contentContactMessageThumbLand.setImageBitmap(bitmap);

                                holder.contentContactMessageThumbLand.setVisibility(View.VISIBLE);
                                holder.contentContactMessageThumbLandFramework.setVisibility(View.VISIBLE);

                                holder.contentContactMessageThumbPort.setVisibility(View.GONE);
                                holder.contentContactMessageThumbPortFramework.setVisibility(View.GONE);

                                holder.contentContactMessageFileThumb.setVisibility(View.GONE);
                                holder.contentContactMessageFileName.setVisibility(View.GONE);
                                holder.contentContactMessageFileSize.setVisibility(View.GONE);

                                if(chatRoom.isGroup()){
                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbLand.getLayoutParams();
                                    contactThumbParams.setMargins(0, Util.scaleHeightPx(10, outMetrics),0, 0);
                                    holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbLandFramework.setBackgroundResource(R.drawable.shape_images_chat_3);

                                    log("Max width to 220");
                                    float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_WIDTH_NAME_SENDER_GROUP_THUMB_LAND_PICTURE, context.getResources().getDisplayMetrics());
                                    holder.contentContactMessageFileSender.setMaxWidth((int) width);
                                }
                                else{
                                    RelativeLayout.LayoutParams contactThumbParams = (RelativeLayout.LayoutParams)holder.contentContactMessageThumbLand.getLayoutParams();
                                    contactThumbParams.setMargins(0, 0 ,0, 0);
                                    holder.contentContactMessageThumbLand.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbLandFramework.setLayoutParams(contactThumbParams);
                                    holder.contentContactMessageThumbLandFramework.setBackgroundResource(R.drawable.shape_images_chat);

                                }

                            }
                        }
                    }
                }
            }
            else{
                log("Preview not exists");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setElevation(0);
                }
            }
        }
    }

    private void setUploadingPreview(MegaChatLollipopAdapter.ViewHolderMessageChat holder, Bitmap bitmap){
        log("setUploadingPreview: "+holder.filePathUploading);
        if(holder!=null){
            if(bitmap!=null){
                log("Update uploading my preview");

                int currentPosition = holder.getAdapterPosition()-1;
                AndroidMegaChatMessage message = messages.get(currentPosition);
                if(message.getPendingMessage()!=null) {
                    log("State of the message: " + message.getPendingMessage().getState());

                    if (message.getPendingMessage().getState() == PendingMessage.STATE_ERROR) {
                        holder.transparentCoatingPortrait.setVisibility(View.GONE);
                        holder.transparentCoatingLandscape.setVisibility(View.GONE);
                        holder.uploadingProgressBar.setVisibility(View.GONE);
                        holder.errorUploadingLayout.setVisibility(View.VISIBLE);
                    } else {
                        holder.transparentCoatingLandscape.setVisibility(View.VISIBLE);
                        holder.transparentCoatingPortrait.setVisibility(View.VISIBLE);
                        holder.uploadingProgressBar.setVisibility(View.VISIBLE);
                        holder.errorUploadingLayout.setVisibility(View.GONE);
                    }

                    if (bitmap.getWidth() < bitmap.getHeight()) {
                        log("Portrait");
                        holder.contentOwnMessageThumbPort.setImageBitmap(bitmap);
                        holder.previewFramePort.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageThumbPort.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                        holder.previewFrameLand.setVisibility(View.GONE);
                        holder.contentOwnMessageThumbLand.setVisibility(View.GONE);

                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.errorUploadingLayout.getLayoutParams();
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 152, context.getResources().getDisplayMetrics());

                        params.width = (int) width;
                        log("Preview ImageView width: " + width);
                        holder.errorUploadingLayout.setLayoutParams(params);
                    } else {
                        log("Landscape");
                        holder.contentOwnMessageThumbLand.setImageBitmap(bitmap);
                        holder.previewFrameLand.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageThumbLand.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageFileLayout.setVisibility(View.GONE);
                        holder.previewFramePort.setVisibility(View.GONE);
                        holder.contentOwnMessageThumbPort.setVisibility(View.GONE);

                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.errorUploadingLayout.getLayoutParams();
                        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 212, context.getResources().getDisplayMetrics());

                        params.width = (int) width;
                        log("Preview ImageView width: " + width);
                        holder.errorUploadingLayout.setLayoutParams(params);
                    }
                }
                else{
                    log("The pending message is NULL-- cannot set preview");
                }
            }
            else{
                log("Bitmap is NULL");
            }
        }
        else{
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

}
