package mega.privacy.android.app.lollipop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.utils.Constants;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

public class AudioVideoPlayerLollipop extends PinActivityLollipop implements VideoRendererEventListener, MegaChatRequestListenerInterface, MegaTransferListenerInterface{

    public static int REQUEST_CODE_SELECT_CHAT = 1005;

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    private AlertDialog alertDialogTransferOverquota;

    Handler handler;
    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer player;
    private Uri uri;

    private AppBarLayout appBarLayout;
    private Toolbar tB;
    private ActionBar aB;

    private MenuItem shareIcon;
    private MenuItem propertiesIcon;
    private MenuItem chatIcon;

    private RelativeLayout audioContainer;
    private long handle;
    int countChat = 0;
    int successSent = 0;
    int errorSent = 0;
    boolean transferOverquota = false;

    private boolean video = false;
    private boolean loading = true;
    private ProgressDialog statusDialog = null;
    private String fileName = null;

    private RelativeLayout containerAudioVideoPlayer;

    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        setContentView(R.layout.activity_audiovideoplayer);

        Intent intent = getIntent();
        if (intent == null){
            log("intent null");
            finish();
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            handle = bundle.getLong("HANDLE");
            fileName = bundle.getString("FILENAME");
        }

        uri = intent.getData();
        //Uri uri = Uri.parse("https://youtu.be/upUpVb3cLfk");
        if (uri == null){
            log("uri null");
            finish();
            return;
        }
        log("uri: "+uri);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }

        tB = (Toolbar) findViewById(R.id.call_toolbar);
        if (tB == null) {
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        log("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        if (fileName != null) {
            aB.setTitle(fileName);
        }
        else {
            aB.setTitle(getFileName(uri));
        }

        audioContainer = (RelativeLayout) findViewById(R.id.audio_container);
        audioContainer.setVisibility(View.GONE);

        handler = new Handler();

        MegaApplication app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();
        if(megaApi==null||megaApi.getRootNode()==null){
            log("Refresh session - sdk");
            Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
            intentLogin.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
            intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intentLogin);
            finish();
            return;
        }

        if(mega.privacy.android.app.utils.Util.isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
            }

            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
                log("Refresh session - karere");
                Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
                intentLogin.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
                intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentLogin);
                finish();
                return;
            }
        }

        log("Overquota delay: "+megaApi.getBandwidthOverquotaDelay());
        if(megaApi.getBandwidthOverquotaDelay()>0){
            if(alertDialogTransferOverquota==null){
                showTransferOverquotaDialog();
            }
            else {
                if (!(alertDialogTransferOverquota.isShowing())) {
                    showTransferOverquotaDialog();
                }
            }
        }

        log("Add transfer listener");
        megaApi.addTransferListener(this);

        //Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        //Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        //Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);

        //Set media controller
        simpleExoPlayerView.setUseController(true);
        simpleExoPlayerView.requestFocus();

        //Bind the player to the view
        simpleExoPlayerView.setPlayer(player);
        simpleExoPlayerView.setControllerAutoShow(false);
        simpleExoPlayerView.setControllerShowTimeoutMs(999999999);
        simpleExoPlayerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event){

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (aB.isShowing()) {
                        simpleExoPlayerView.hideController();
                        hideActionStatusBar();
                    }
                    else {
                        showActionStatusBar();
                    }
                }
                return true;
            }
        });
        /*simpleExoPlayerView.setControllerVisibilityListener(new PlaybackControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if(aB.isShowing()){
                    simpleExoPlayerView.showController();
                }
            }
        });*/
        //Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
        //Produces DataSource instances through which meida data is loaded
        //DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "android2"), defaultBandwidthMeter);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "android2"), defaultBandwidthMeter);
        //Produces Extractor instances for parsing the media data
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        MediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
        //MediaSource mediaSource = new HlsMediaSource(uri, dataSourceFactory, handler, null);
        //DashMediaSource mediaSource = new DashMediaSource(uri, dataSourceFactory, new DefaultDashChunkSource.Factory(dataSourceFactory), null, null);


        final LoopingMediaSource loopingMediaSource = new LoopingMediaSource(mediaSource);

        player.prepare(loopingMediaSource);

        statusDialog = new ProgressDialog(AudioVideoPlayerLollipop.this);
        statusDialog.setMessage(getString(R.string.general_loading));
        statusDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                showActionStatusBar();
            }
        });
        statusDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (loading) {
                    finish();
                }
            }
        });

        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                log("onTimelineChanged");
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                log("onTracksChanged");
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                log("onLoadingChanged");
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                log("onPlayerStateChanged");

                if (playbackState == ExoPlayer.STATE_BUFFERING){
                    audioContainer.setVisibility(View.GONE);

                    if (loading && !transferOverquota){
                        try {
                            statusDialog.setCanceledOnTouchOutside(false);
                            statusDialog.show();
                        }
                        catch(Exception e){
                            return;
                        }
                    }
                }
                else {
                    statusDialog.hide();

                    if (!video) {
                        audioContainer.setVisibility(View.VISIBLE);
                    }
                    else {
                        audioContainer.setVisibility(View.GONE);
                    }
                }
                log("loading: "+loading);
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                log("onRepeatModeChanged");
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                log("onShuffleModeEnabledChanged");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                log("onPlayerError");
                player.stop();
                player.prepare(loopingMediaSource);
                player.setPlayWhenReady(true);
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                log("onPositionDiscontinuity");
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                log("onPlaybackParametersChanged");
            }

            @Override
            public void onSeekProcessed() {
                log("onSeekProcessed");
            }
        });
        player.setPlayWhenReady(true);
        player.setVideoDebugListener(this);
        simpleExoPlayerView.showController();
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    protected void hideActionStatusBar(){
        if (aB != null && aB.isShowing()) {
            if(tB != null) {
                tB.animate().translationY(-220).setDuration(400L)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                aB.hide();
                            }
                        }).start();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            else {
                aB.hide();
            }
            simpleExoPlayerView.hideController();
        }
    }
    protected void showActionStatusBar(){
        if (aB != null && !aB.isShowing()) {
            aB.show();
            if(tB != null) {
                tB.animate().translationY(0).setDuration(400L).start();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            simpleExoPlayerView.showController();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_audiovideoplayer, menu);

        shareIcon = menu.findItem(R.id.full_video_viewer_share);

        Drawable share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        shareIcon.setIcon(share);

        propertiesIcon = menu.findItem(R.id.full_image_viewer_properties);
        chatIcon = menu.findItem(R.id.full_image_viewer_chat);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        log("onPrepareOptionsMenu");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.full_video_viewer_chat:{
                log("Chat option");
                long[] longArray = new long[1];

                longArray[0] = handle;

                Intent i = new Intent(this, ChatExplorerActivity.class);
                i.putExtra("NODE_HANDLES", longArray);
                startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
                break;
            }
            case R.id.full_video_viewer_share: {
                log("Share option");

                intentToSendFile(uri);

                break;
            }
            case R.id.full_video_viewer_properties: {
                log("Info option");

                MegaNode node = megaApi.getNodeByHandle(handle);
                Intent i = new Intent(this, FileInfoActivityLollipop.class);
                i.putExtra("handle", node.getHandle());
                i.putExtra("imageId", MimeTypeMime.typeForName(node.getName()).getIconResourceId());
                i.putExtra("name", node.getName());
                startActivity(i);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void intentToSendFile(Uri uri){
        log("intentToSendFile");

        if(uri!=null){
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.setType("application/pdf");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                log("Use provider to share");
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            else{
                share.putExtra(Intent.EXTRA_STREAM, uri);
            }
            startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
        }
        else{
            Snackbar.make(this.getCurrentFocus(), getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (intent == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
            long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
            log("Send to "+chatHandles.length+" chats");

            long[] nodeHandles = intent.getLongArrayExtra("NODE_HANDLES");
            log("Send "+nodeHandles.length+" nodes");

            countChat = chatHandles.length;
            if(countChat==1){
                megaChatApi.attachNode(chatHandles[0], nodeHandles[0], this);
            }
            else if(countChat>1){

                for(int i=0; i<chatHandles.length; i++){
                    megaChatApi.attachNode(chatHandles[i], nodeHandles[0], this);
                }
            }
        }
    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
        log("onVideoEnabled");
        video = true;
        loading = false;
        audioContainer.setVisibility(View.GONE);
    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        log("onVideoDecoderInitialized");
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        log("onVideoInputFormatChanged");
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
        log("onDroppedFrames");
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        log("onVideoSizeChanged");
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        log("onRenderedFirstFrame");
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
        log("onVideoDisabled");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
    }

    @Override
    protected void onDestroy() {
        log("onDestroy()");

        if (megaApi != null) {
            megaApi.removeTransferListener(this);
        }
        player.release();

        super.onDestroy();
    }

    public static void log(String message) {
        mega.privacy.android.app.utils.Util.log("AudioVideoPlayerLollipop", message);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish");
        if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("File sent correctly");
                successSent++;

            }
            else{
                log("File NOT sent: "+e.getErrorCode()+"___"+e.getErrorString());
                errorSent++;
            }

            if(countChat==errorSent+successSent){
                if(successSent==countChat){
                    if(countChat==1){
                        long handle = request.getChatHandle();
                        MegaChatListItem chatItem = megaChatApi.getChatListItem(handle);
                        if(chatItem!=null){
                            Intent intent = new Intent(this, ManagerActivityLollipop.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
                            intent.putExtra("CHAT_ID", handle);
                            startActivity(intent);
                            finish();
                        }
                    }
                    else{
                        showSnackbar(getString(R.string.success_attaching_node_from_cloud_chats, countChat));
                    }
                }
                else if(errorSent==countChat){
                    showSnackbar(getString(R.string.error_attaching_node_from_cloud));
                }
                else{
                    showSnackbar(getString(R.string.error_attaching_node_from_cloud_chats));
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(getCurrentFocus(), s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    @Override
    public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

    }

    @Override
    public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {

    }

    @Override
    public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {

    }

    @Override
    public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        log("onTransferTemporaryError");

        if(e.getErrorCode() == MegaError.API_EOVERQUOTA){
            log("API_EOVERQUOTA error!!");

            if(alertDialogTransferOverquota==null){
                showTransferOverquotaDialog();
            }
            else {
                if (!(alertDialogTransferOverquota.isShowing())) {
                    showTransferOverquotaDialog();
                }
            }
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return false;
    }


    public void showTransferOverquotaDialog(){
        log("showTransferOverquotaDialog");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.transfer_overquota_layout, null);
        dialogBuilder.setView(dialogView);

        TextView title = (TextView) dialogView.findViewById(R.id.transfer_overquota_title);
        title.setText(getString(R.string.title_depleted_transfer_overquota));

        ImageView icon = (ImageView) dialogView.findViewById(R.id.image_transfer_overquota);
        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.transfer_quota_empty));

        TextView text = (TextView) dialogView.findViewById(R.id.text_transfer_overquota);
        text.setText(getString(R.string.text_depleted_transfer_overquota));

        Button continueButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_dissmiss);

        Button paymentButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_payment);
        paymentButton.setText(getString(R.string.action_upgrade_account));

        alertDialogTransferOverquota = dialogBuilder.create();

        alertDialogTransferOverquota.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                transferOverquota = true;
                statusDialog.hide();
                showActionStatusBar();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                alertDialogTransferOverquota.dismiss();
                transferOverquota = false;
                if (loading) {
                    statusDialog.setCanceledOnTouchOutside(false);
                    statusDialog.show();
                }
            }

        });

        paymentButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                alertDialogTransferOverquota.dismiss();
                transferOverquota = false;
                showUpgradeAccount();
            }
        });

        alertDialogTransferOverquota.setCancelable(false);
        alertDialogTransferOverquota.setCanceledOnTouchOutside(false);
        alertDialogTransferOverquota.show();
    }

    public void showUpgradeAccount(){
        log("showUpgradeAccount");
        Intent upgradeIntent = new Intent(this, ManagerActivityLollipop.class);
        upgradeIntent.setAction(Constants.ACTION_SHOW_UPGRADE_ACCOUNT);
        startActivity(upgradeIntent);
    }

    private void showOverquotaNotification(){
        log("showOverquotaNotification");

        PendingIntent pendingIntent = null;

        String info = "Streaming";
        Notification notification = null;

        String contentText = getString(R.string.download_show_info);
        String message = getString(R.string.title_depleted_transfer_overquota);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify_download)
                    .setOngoing(false).setContentTitle(message).setSubText(info)
                    .setContentText(contentText)
                    .setOnlyAlertOnce(true);

            if(pendingIntent!=null){
                mBuilder.setContentIntent(pendingIntent);
            }
            notification = mBuilder.build();
        }
        else if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify_download)
                    .setOngoing(false).setContentTitle(message).setContentInfo(info)
                    .setContentText(contentText)
                    .setOnlyAlertOnce(true);

            if(pendingIntent!=null){
                mBuilder.setContentIntent(pendingIntent);
            }
            notification = mBuilder.getNotification();
        }
        else
        {
            notification = new Notification(R.drawable.ic_stat_notify_download, null, 1);
            notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
            if(pendingIntent!=null){
                notification.contentIntent = pendingIntent;
            }
            notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify_download);
            notification.contentView.setTextViewText(R.id.status_text, message);
            notification.contentView.setTextViewText(R.id.progress_text, info);
        }

        mNotificationManager.notify(Constants.NOTIFICATION_STREAMING_OVERQUOTA, notification);
    }
}