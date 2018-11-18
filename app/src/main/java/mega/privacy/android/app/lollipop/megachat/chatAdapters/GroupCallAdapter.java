package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.listeners.GroupCallListener;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.lollipop.megachat.calls.InfoPeerGroupCall;
import mega.privacy.android.app.lollipop.megachat.calls.MegaSurfaceRendererGroup;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;
import nz.mega.sdk.MegaNode;

import static android.view.View.GONE;
import static mega.privacy.android.app.utils.Util.context;
import static mega.privacy.android.app.utils.Util.deleteFolderAndSubfolders;
import static mega.privacy.android.app.utils.Util.percScreenLogin;

public class GroupCallAdapter extends RecyclerView.Adapter<GroupCallAdapter.ViewHolderGroupCall> implements MegaSurfaceRendererGroup.MegaSurfaceRendererGroupListener {

    public static final int ITEM_VIEW_TYPE_LIST = 0;
    public static final int ITEM_VIEW_TYPE_GRID = 1;

    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi = null;
    Display display;
    DisplayMetrics outMetrics;
    float density;
    float scaleW;
    float scaleH;
    float widthScreenPX, heightScreenPX;
    boolean isCallInProgress = false;
//    int adapterType;

    RecyclerView recyclerViewFragment;

    ArrayList<InfoPeerGroupCall> peers;
    long chatId;

    int maxScreenWidth, maxScreenHeight;
    boolean avatarRequested = false;

//    public static class ViewHolderBrowserList extends GroupCallAdapter.ViewHolderGroupCall.ViewHolderBrowserList {
//
//        public ViewHolderBrowserList(View v) {
//            super(v);
//        }
//    }
//
//    public static class ViewHolderBrowserGrid extends MegaBrowserLollipopAdapter.ViewHolderBrowserGrid {
//
//        public ViewHolderBrowserGrid(View v) {
//            super(v);
//        }
//
//    }

public GroupCallAdapter(Context context, RecyclerView recyclerView, ArrayList<InfoPeerGroupCall> peers, long chatId, boolean isCallInProgress) {

    log("GroupCallAdapter(peers: "+peers.size()+")");

        this.context = context;
        this.recyclerViewFragment = recyclerView;
        this.peers = peers;
        this.chatId = chatId;
        this.isCallInProgress = isCallInProgress;
//        this.adapterType = adapterType;

        MegaApplication app = (MegaApplication) ((Activity) context).getApplication();
        if (megaApi == null) {
            megaApi = app.getMegaApi();
        }

        log("retryPendingConnections()");
        if (megaApi != null) {
            log("---------retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        if (megaChatApi == null) {
            megaChatApi = app.getMegaChatApi();
        }
    }

    public class ViewHolderGroupCall extends RecyclerView.ViewHolder{

        RelativeLayout rlGeneral;
        RelativeLayout greenLayer;
        RelativeLayout avatarMicroLayout;
        RelativeLayout avatarLayout;
        RoundedImageView avatarImage;
        ImageView microAvatar;
        ImageView microSurface;
        TextView avatarInitialLetter;
        RelativeLayout parentSurfaceView;
        RelativeLayout surfaceMicroLayout;

        public ViewHolderGroupCall(View itemView) {
            super(itemView);
        }

    }

    public class ViewHolderGroupCallGrid extends ViewHolderGroupCall{
        public ViewHolderGroupCallGrid(View v) {
            super(v);
        }
    }

    ViewHolderGroupCallGrid holderGrid = null;

    @Override public GroupCallAdapter.ViewHolderGroupCall onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder()");

        display = ((ChatCallActivity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        widthScreenPX = outMetrics.widthPixels;
        heightScreenPX = outMetrics.heightPixels;
        density = context.getResources().getDisplayMetrics().density;
        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        maxScreenHeight = parent.getMeasuredHeight();
        maxScreenWidth = parent.getMeasuredWidth();

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera_group_call, parent, false);
//        int numPeersOnCall = getItemCount();
//        CustomizedGridRecyclerView.LayoutParams lp = (CustomizedGridRecyclerView.LayoutParams) v.getLayoutParams();

//        if(numPeersOnCall == 1){
//            lp.height = maxScreenHeight;
//            lp.width = maxScreenWidth;
//        }else if(numPeersOnCall == 2){
//            lp.height = maxScreenHeight/numPeersOnCall;
//            lp.width = maxScreenWidth;
//        }else if(numPeersOnCall == 3){
//            lp.height = maxScreenHeight/numPeersOnCall;
//            lp.width = maxScreenWidth;
//        }else if((numPeersOnCall >= 4) && (numPeersOnCall < 7)){
//            lp.height = Util.scaleWidthPx(180, outMetrics);
//            lp.width = maxScreenWidth/2;
//        }else{
//
//            lp.height = Util.scaleWidthPx(90, outMetrics);
//            lp.width = Util.scaleWidthPx(90, outMetrics);
//        }
//        v.setLayoutParams(lp);



//        if(numPeersOnCall < 4) {
//            lp.height = maxScreenHeight/numPeersOnCall;
//            lp.width = maxScreenWidth;
//
//        }else if((numPeersOnCall >= 4) && (numPeersOnCall < 7)){
//            lp.height = Util.scaleWidthPx(180, outMetrics);
//            lp.width = maxScreenWidth/2;
//        }else{
//            lp.height = Util.scaleWidthPx(90, outMetrics);
//            lp.width = Util.scaleWidthPx(90, outMetrics);
//        }



//        CustomizedGridRecyclerView.LayoutParams lp = (CustomizedGridRecyclerView.LayoutParams) v.getLayoutParams();
//
//        if(numPeersOnCall < 7){
//            lp.height = Util.scaleWidthPx(180, outMetrics);
////            lp.width = maxScreenWidth/2;
//            lp.width = Util.scaleWidthPx(180, outMetrics);
//        }else{
//            lp.height = Util.scaleWidthPx(90, outMetrics);
//            lp.width = Util.scaleWidthPx(90, outMetrics);
//        }
//
//        v.setLayoutParams(lp);

        holderGrid = new ViewHolderGroupCallGrid(v);

        holderGrid.rlGeneral = (RelativeLayout) v.findViewById(R.id.general);

        holderGrid.greenLayer = (RelativeLayout) v.findViewById(R.id.green_layer);
        holderGrid.surfaceMicroLayout = (RelativeLayout) v.findViewById(R.id.rl_surface_and_micro);

        holderGrid.parentSurfaceView = (RelativeLayout) v.findViewById(R.id.parent_surface_view);
        holderGrid.parentSurfaceView.removeAllViews();
        holderGrid.parentSurfaceView.removeAllViewsInLayout();
        holderGrid.avatarMicroLayout = (RelativeLayout) v.findViewById(R.id.layout_avatar_micro);

        holderGrid.avatarLayout = (RelativeLayout) v.findViewById(R.id.avatar_rl);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)holderGrid.avatarLayout.getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        holderGrid.avatarLayout.setLayoutParams(layoutParams);

        holderGrid.microAvatar = (ImageView) v.findViewById(R.id.micro_avatar);
        holderGrid.microSurface = (ImageView) v.findViewById(R.id.micro_surface_view);

        holderGrid.avatarImage = (RoundedImageView) v.findViewById(R.id.avatar_image);
        holderGrid.avatarInitialLetter = (TextView) v.findViewById(R.id.avatar_initial_letter);

        v.setTag(holderGrid);
        return holderGrid;
    }

    @Override
    public void onBindViewHolder(ViewHolderGroupCall holder, int position) {
        ViewHolderGroupCallGrid holderGrid2 = (ViewHolderGroupCallGrid) holder;
        onBindViewHolderGrid(holderGrid2, position);
    }

    public void onBindViewHolderGrid (final ViewHolderGroupCallGrid holder, final int position){
        log("onBindViewHolderGrid() - position: "+position);

        final InfoPeerGroupCall peer = getNodeAt(position);
        if (peer == null){
            return;
        }

        int numPeersOnCall = getItemCount();

        CustomizedGridRecyclerView.LayoutParams lp = (CustomizedGridRecyclerView.LayoutParams) holder.rlGeneral.getLayoutParams();

        if(numPeersOnCall < 4){
            lp.height = maxScreenHeight/numPeersOnCall;
            lp.width = maxScreenWidth;
        }else if((numPeersOnCall >= 4) && (numPeersOnCall < 7)){
            lp.height = Util.scaleWidthPx(180, outMetrics);
            lp.width = maxScreenWidth/2;
        }else{
            lp.height = Util.scaleWidthPx(90, outMetrics);
            lp.width = Util.scaleWidthPx(90, outMetrics);
        }
        holder.rlGeneral.setLayoutParams(lp);
        holder.rlGeneral.setGravity(RelativeLayout.CENTER_HORIZONTAL);

        if(isCallInProgress){
            holder.rlGeneral.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(getItemCount() < 7){
                        ((ChatCallActivity) context).remoteCameraClick();
                    }else{
                        ((ChatCallActivity) context).itemClicked(peer);
                    }
                }
            });
        }else{
            holder.rlGeneral.setOnClickListener(null);
        }

        if(peer.isVideoOn()) {
            holder.avatarMicroLayout.setVisibility(GONE);
            holder.microAvatar.setVisibility(View.GONE);

            if(numPeersOnCall == 1){
                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = maxScreenWidth;
                layoutParamsSurface.height = maxScreenWidth;
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

            }else if(numPeersOnCall == 2){
                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = Util.scaleWidthPx(320, outMetrics);
                layoutParamsSurface.height = Util.scaleWidthPx(320, outMetrics);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

            }else if(numPeersOnCall == 3){
                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = Util.scaleWidthPx(212, outMetrics);
                layoutParamsSurface.height = Util.scaleWidthPx(212, outMetrics);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

            }else if(numPeersOnCall == 4){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = Util.scaleWidthPx(180, outMetrics);
                layoutParamsSurface.height = Util.scaleWidthPx(180, outMetrics);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

                if((position < 2)){
                    layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                }else{
                    layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

                }
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

            }else if(numPeersOnCall == 5){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = Util.scaleWidthPx(180, outMetrics);
                layoutParamsSurface.height = Util.scaleWidthPx(180, outMetrics);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

                if(peer.getHandle().equals(megaChatApi.getMyUserHandle())){
                    ViewGroup.LayoutParams layoutParamsPeer = (ViewGroup.LayoutParams) holder.rlGeneral.getLayoutParams();
                    layoutParamsPeer.width = maxScreenWidth;
                    layoutParamsPeer.height = Util.scaleWidthPx(180, outMetrics);
                    holder.rlGeneral.setLayoutParams(layoutParamsPeer);
                }else{
                    ViewGroup.LayoutParams layoutParamsPeer = (ViewGroup.LayoutParams) holder.rlGeneral.getLayoutParams();
                    layoutParamsPeer.width = (maxScreenWidth/2);
                    layoutParamsPeer.height = Util.scaleWidthPx(180, outMetrics);
                    holder.rlGeneral.setLayoutParams(layoutParamsPeer);
                }

            }else if(numPeersOnCall == 6){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = Util.scaleWidthPx(180, outMetrics);
                layoutParamsSurface.height = Util.scaleWidthPx(180, outMetrics);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);
            }



//            if(holder.parentSurfaceView.getChildCount()!=0){
//                holder.parentSurfaceView.removeAllViewsInLayout();
//            }
//            holder.surfaceMicroLayout.setVisibility(GONE);

            //Listener && SurfaceView
            if(peer.getListener() == null){

                SurfaceView surfaceView = new SurfaceView(context);
                surfaceView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                surfaceView.setZOrderMediaOverlay(true);
                GroupCallListener listenerPeer = new GroupCallListener(context, surfaceView, peer.getHandle());
                peer.setListener(listenerPeer);

                if (peer.getHandle().equals(megaChatApi.getMyUserHandle())) {
                    megaChatApi.addChatLocalVideoListener(chatId, peer.getListener());
                } else {
                    megaChatApi.addChatRemoteVideoListener(chatId, peer.getHandle(), peer.getListener());
                }
                if(numPeersOnCall < 7){
                    peer.getListener().getLocalRenderer().addListener(null);
                }else{
                    peer.getListener().getLocalRenderer().addListener(this);
                }
                holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                peer.getListener().getSurfaceView().getHolder().setSizeFromLayout();

            }else{

//                if(peer.getListener().getHeight() != 0){
//                    peer.getListener().setHeight(0);
//                }
//                if(peer.getListener().getWidth() != 0){
//                    peer.getListener().setWidth(0);
//                }
                if(holder.parentSurfaceView.getChildCount() == 0){
                    if(peer.getListener().getSurfaceView().getParent()!=null){
                        if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                            ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeAllViewsInLayout();
                            if(peer.getListener().getHeight() != 0){
                                peer.getListener().setHeight(0);
                            }
                            if(peer.getListener().getWidth() != 0){
                                peer.getListener().setWidth(0);
                            }
                            holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                            peer.getListener().getSurfaceView().getHolder().setSizeFromLayout();

                        }else{

                        }
                    }else{

                        if(peer.getListener().getHeight() != 0){
                            peer.getListener().setHeight(0);
                        }
                        if(peer.getListener().getWidth() != 0){
                            peer.getListener().setWidth(0);
                        }
                        holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                        peer.getListener().getSurfaceView().getHolder().setSizeFromLayout();

                    }
                }else{
                    peer.getListener().getSurfaceView().getHolder().setSizeFromLayout();


//                    if(peer.getListener().getHeight() != 0){
//                        peer.getListener().setHeight(0);
//                    }
//                    if(peer.getListener().getWidth() != 0){
//                        peer.getListener().setWidth(0);
//                    }
//                    holder.parentSurfaceView.setGravity(RelativeLayout.CENTER_HORIZONTAL);
                }
//                peer.getListener().getSurfaceView().getHolder().setSizeFromLayout();

            }
            holder.surfaceMicroLayout.setVisibility(View.VISIBLE);


            //Audio icon:
            if(numPeersOnCall < 7){
                RelativeLayout.LayoutParams paramsMicroSurface = new RelativeLayout.LayoutParams(holder.microSurface.getLayoutParams());
                paramsMicroSurface.height = Util.scaleWidthPx(24, outMetrics);
                paramsMicroSurface.width = Util.scaleWidthPx(24, outMetrics);
                paramsMicroSurface.setMargins(0, Util.scaleWidthPx(15, outMetrics),  Util.scaleWidthPx(15, outMetrics), 0);
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.microSurface.setLayoutParams(paramsMicroSurface);
            }else{
                RelativeLayout.LayoutParams paramsMicroSurface = new RelativeLayout.LayoutParams(holder.microSurface.getLayoutParams());
                paramsMicroSurface.height = Util.scaleWidthPx(15, outMetrics);
                paramsMicroSurface.width = Util.scaleWidthPx(15, outMetrics);
                paramsMicroSurface.setMargins(0,  Util.scaleWidthPx(7, outMetrics),  Util.scaleWidthPx(7, outMetrics), 0);
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                holder.microSurface.setLayoutParams(paramsMicroSurface);
            }
            if(peer.isAudioOn()){
                holder.microSurface.setVisibility(View.GONE);
            }else{
                if(isCallInProgress) {
                    holder.microSurface.setVisibility(View.VISIBLE);
                }else{
                    holder.microSurface.setVisibility(View.GONE);
                }
            }

            //Green Layer:
            if(numPeersOnCall < 7){
                holder.greenLayer.setVisibility(View.GONE);
            }else{
                if(peer.hasGreenLayer()){
                    holder.greenLayer.setVisibility(View.VISIBLE);
                }else{
                    holder.greenLayer.setVisibility(View.GONE);
                }
            }

        }else{

            //Remove Surface view && Listener:
            holder.surfaceMicroLayout.setVisibility(GONE);

            holder.avatarMicroLayout.setVisibility(View.VISIBLE);

            if(peer.getListener() != null){
                if (peer.getHandle().equals(megaChatApi.getMyUserHandle())) {
                    megaChatApi.removeChatVideoListener(chatId, -1, peer.getListener());
                }else{
                    megaChatApi.removeChatVideoListener(chatId, peer.getHandle(), peer.getListener());
                }

                if(holder.parentSurfaceView.getChildCount() == 0){
                    if(peer.getListener().getSurfaceView().getParent()!=null){
                        if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                            ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeAllViewsInLayout();
                        }else{ }
                    }else{ }
                }else{
                    holder.parentSurfaceView.removeAllViews();
                    holder.parentSurfaceView.removeAllViewsInLayout();
                }
                peer.setListener(null);
            }else{ }

            //Create Avatar:
            if (peer.getHandle().equals(megaChatApi.getMyUserHandle())) {
                setProfileMyAvatar(holder);
            }else{
                setProfileContactAvatar(peer.getHandle(), peer.getName(), holder);
            }

            //Micro icon:
            if(numPeersOnCall < 7){
                RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                paramsMicroAvatar.height = Util.scaleWidthPx(24, outMetrics);
                paramsMicroAvatar.width = Util.scaleWidthPx(24, outMetrics);
                paramsMicroAvatar.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, 0);
                paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                holder.microAvatar.setLayoutParams(paramsMicroAvatar);

                ViewGroup.LayoutParams paramsAvatarImage = (ViewGroup.LayoutParams) holder.avatarImage.getLayoutParams();
                paramsAvatarImage.width = Util.scaleWidthPx(88, outMetrics);
                paramsAvatarImage.height = Util.scaleWidthPx(88, outMetrics);
                holder.avatarImage.setLayoutParams(paramsAvatarImage);
                holder.avatarInitialLetter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f);
            }else{
                RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                paramsMicroAvatar.height = Util.scaleWidthPx(15, outMetrics);
                paramsMicroAvatar.width = Util.scaleWidthPx(15, outMetrics);
                paramsMicroAvatar.setMargins(0, 0, 0, 0);
                paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                holder.microAvatar.setLayoutParams(paramsMicroAvatar);

                ViewGroup.LayoutParams paramsAvatarImage = (ViewGroup.LayoutParams) holder.avatarImage.getLayoutParams();
                paramsAvatarImage.width = Util.scaleWidthPx(60, outMetrics);
                paramsAvatarImage.height = Util.scaleWidthPx(60, outMetrics);
                holder.avatarImage.setLayoutParams(paramsAvatarImage);
                holder.avatarInitialLetter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
            }
            if(peer.isAudioOn()){
                holder.microAvatar.setVisibility(View.GONE);
            }else{
                if(isCallInProgress){
                    holder.microAvatar.setVisibility(View.VISIBLE);
                }else{
                    holder.microAvatar.setVisibility(View.GONE);
                }
            }

            //Green Layer:
            if(numPeersOnCall >= 7){
                if(peer.hasGreenLayer()){
                    holder.greenLayer.setVisibility(View.VISIBLE);
                }else{
                    holder.greenLayer.setVisibility(View.GONE);
                }
            }else{
                holder.greenLayer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (peers != null){
            return peers.size();
        }else{
            return 0;
        }
    }

    public Object getItem(int position) {
        if (peers != null){
            return peers.get(position);
        }
        return null;
    }

    public InfoPeerGroupCall getNodeAt(int position) {
        try {
            if (peers != null) {
                return peers.get(position);
            }
        } catch (IndexOutOfBoundsException e) {}
        return null;
    }

   //My AVATAR
   public void setProfileMyAvatar(ViewHolderGroupCall holder) {
       Bitmap myBitmap = null;
       File avatar = null;
       if (context != null) {
           if (context.getExternalCacheDir() != null) {
               avatar = new File(context.getExternalCacheDir().getAbsolutePath(), megaChatApi.getMyEmail() + ".jpg");
           } else {
               avatar = new File(context.getCacheDir().getAbsolutePath(), megaChatApi.getMyEmail() + ".jpg");
           }
       }
       if (avatar.exists()) {
           if (avatar.length() > 0) {
               BitmapFactory.Options bOpts = new BitmapFactory.Options();
               bOpts.inPurgeable = true;
               bOpts.inInputShareable = true;
               myBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
               myBitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, myBitmap, 3);
               if (myBitmap != null) {
                       holder.avatarImage.setImageBitmap(myBitmap);
                       holder.avatarInitialLetter.setVisibility(GONE);
               }
               else{
                   createMyDefaultAvatar(holder);
               }
           }
           else {
               createMyDefaultAvatar(holder);
           }
       } else {
           createMyDefaultAvatar(holder);
       }
   }
    //My Default AVATAR
    public void createMyDefaultAvatar(ViewHolderGroupCall holder) {
        String myFullName = megaChatApi.getMyFullname();
        String myFirstLetter=myFullName.charAt(0) + "";
        myFirstLetter = myFirstLetter.toUpperCase(Locale.getDefault());
        long userHandle = megaChatApi.getMyUserHandle();

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels,outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);

        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(userHandle));

        if(color!=null){
            p.setColor(Color.parseColor(color));
        }else{
            p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        holder.avatarImage.setImageBitmap(defaultAvatar);
        holder.avatarInitialLetter.setText(myFirstLetter);
        holder.avatarInitialLetter.setVisibility(View.VISIBLE);
    }

    //CONTACT AVATAR
    public void setProfileContactAvatar(long userHandle,  String fullName, ViewHolderGroupCall holder){
        Bitmap bitmap = null;
        File avatar = null;
        String contactMail = megaChatApi.getContactEmail(userHandle);
        if (context.getExternalCacheDir() != null) {
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contactMail + ".jpg");
        } else {
            avatar = new File(context.getCacheDir().getAbsolutePath(), contactMail + ".jpg");
        }

        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, bitmap, 3);
                if (bitmap != null) {
                    holder.avatarImage.setVisibility(View.VISIBLE);
                    holder.avatarImage.setImageBitmap(bitmap);
                    holder.avatarInitialLetter.setVisibility(GONE);
                }else{
                    UserAvatarListener listener = new UserAvatarListener(context);
                    avatar.delete();
                    if(!avatarRequested){
                        avatarRequested = true;
                        if (context.getExternalCacheDir() != null){
                            megaApi.getUserAvatar(contactMail, context.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
                        }
                        else{
                            megaApi.getUserAvatar(contactMail, context.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
                        }
                    }
                    createDefaultAvatar(userHandle, fullName, holder);
                }
            }else{
                UserAvatarListener listener = new UserAvatarListener(context);

                if(!avatarRequested){
                    avatarRequested = true;
                    if (context.getExternalCacheDir() != null){
                        megaApi.getUserAvatar(contactMail, context.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
                    }
                    else{
                        megaApi.getUserAvatar(contactMail, context.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
                    }
                }
                createDefaultAvatar(userHandle, fullName, holder);
            }
        }else{
            UserAvatarListener listener = new UserAvatarListener(context);

            if(!avatarRequested){
                avatarRequested = true;
                if (context.getExternalCacheDir() != null){
                    megaApi.getUserAvatar(contactMail, context.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
                }
                else{
                    megaApi.getUserAvatar(contactMail, context.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
                }
            }

            createDefaultAvatar(userHandle, fullName, holder);
        }
    }
    //CONTACT Default AVATAR
    public void createDefaultAvatar(long userHandle,  String fullName, ViewHolderGroupCall holder) {
        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);

        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(userHandle));
        if (color != null) {
            p.setColor(Color.parseColor(color));
        } else {
            p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        }else {
            radius = defaultAvatar.getHeight() / 2;
        }
        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        holder.avatarImage.setVisibility(View.VISIBLE);
        holder.avatarImage.setImageBitmap(defaultAvatar);
        String contactFirstLetter = fullName.charAt(0) + "";
        contactFirstLetter = contactFirstLetter.toUpperCase(Locale.getDefault());
        holder.avatarInitialLetter.setText(contactFirstLetter);
        holder.avatarInitialLetter.setVisibility(View.VISIBLE);
    }

//    public void setAdapterType(int adapterType){
//        this.adapterType = adapterType;
//    }

    public RecyclerView getListFragment() {
        return recyclerViewFragment;
    }

    public void setListFragment(RecyclerView recyclerViewFragment) {
        this.recyclerViewFragment = recyclerViewFragment;
    }

    public void removeSurfacesView(){
        if((peers!=null)&&(peers.size())>0){
            for(int i=0;i<peers.size();i++){
                ViewHolderGroupCall holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(i);
                if(holder!=null){
                    holder.parentSurfaceView.removeAllViewsInLayout();
                    holder.parentSurfaceView.setVisibility(GONE);
                }
            }
        }
    }
//    public void changesInVideo(int position, ViewHolderGroupCall holder){
//        log("++++++ changesInVideo");
//
//        if(holder == null){
//            holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(position);
//        }
//        if(holder!=null){
//            InfoPeerGroupCall peer = getNodeAt(position);
//            if (peer == null){
//                return;
//            }
//            log("++++++ changesInVideo()  holder != NULL");
//
//            if(peer.isVideoOn()){
//                log("++++++ changesInVideo()  isVideoOn");
//
//                //Create Surface View
//                holder.surfaceView = new SurfaceView(context);
//                holder.surfaceView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
//                holder.surfaceView.setZOrderMediaOverlay(true);
//                holder.localSurfaceHolder = holder.surfaceView.getHolder();
//                holder.localSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
//                holder.localRenderer = new MegaSurfaceRendererGroup(holder.surfaceView, peer.getHandle());
//                holder.surfaceViewLayout.addView(holder.surfaceView);
//                if(peers.size() < 7){
//                    log("remove listener in local Renderer");
//                    holder.localRenderer.addListener(null);
//                }else{
//                    log("add listener in local Renderer");
//                    holder.localRenderer.addListener(this);
//                }
//                //Create listener
//                if(peer.getListener() == null){
//                    log("++++ create New listener");
//                    GroupCallListener listenerPeer = new GroupCallListener(context, holder);
//                    peer.setListener(listenerPeer);
//                    if (peer.getHandle().equals(megaChatApi.getMyUserHandle())) {
//                        log("Video ON-> addChatLocalVideoListener() ");
//                        megaChatApi.addChatLocalVideoListener(chatId, peer.getListener());
//                    } else {
//                        log("Video ON-> addChatRemoteVideoListener()");
//                        megaChatApi.addChatRemoteVideoListener(chatId, peer.getHandle(), peer.getListener());
//                    }
//                }else{
//                    log("++++ Listener-> height y widht 0");
//
//                    peer.getListener().setHeight(0);
//                    peer.getListener().setWidth(0);
//                }
//
//                holder.surfaceViewLayout.setVisibility(View.VISIBLE);
//                holder.avatarMicroLayout.setVisibility(View.GONE);
//
//            }else{
//                log("++++++ changesInVideo()  isVideoOff");
//
//                //Remove surface view
//                holder.surfaceView.setVisibility(GONE);
//                holder.surfaceViewLayout.removeAllViewsInLayout();
//                if (peer.getHandle().equals(megaChatApi.getMyUserHandle())) {
//                    log("Video OFF-> removeChatMyVideoListener()");
//                    megaChatApi.removeChatVideoListener(chatId, -1, peer.getListener());
//                }else{
//                    log("Video OFF-> removeChatVideoListener()");
//                    megaChatApi.removeChatVideoListener(chatId, peer.getHandle(), peer.getListener());
//                }
//                peer.setListener(null);
//                holder.surfaceViewLayout.setVisibility(View.GONE);
//                holder.avatarMicroLayout.setVisibility(View.VISIBLE);
//
//            }
//        }else{
//            log("++++++ changesInVideo()  holder == NULL -> notifyItemChanged");
//
//            notifyItemChanged(position);
//        }
//    }

    public void changesInAudio(int position, ViewHolderGroupCall holder){
        log("changesInAudio");

        if(holder == null){
            holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(position);
        }
        if(holder!=null){
            InfoPeerGroupCall peer = getNodeAt(position);
            if (peer == null){
                return;
            }

            if(peer.isAudioOn()){
                holder.microAvatar.setVisibility(View.GONE);
                holder.microSurface.setVisibility(View.GONE);

            }else{
                if(!peer.isVideoOn()){
                    holder.microSurface.setVisibility(View.GONE);

                    holder.microAvatar.setVisibility(View.VISIBLE);
                    if(peers.size() < 7){
                        RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                        paramsMicroAvatar.height = Util.scaleWidthPx(24, outMetrics);
                        paramsMicroAvatar.width = Util.scaleWidthPx(24, outMetrics);
                        paramsMicroAvatar.setMargins(Util.scaleWidthPx(10, outMetrics), 0, 0, 0);
                        paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                        paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                        holder.microAvatar.setLayoutParams(paramsMicroAvatar);
                    }else{
                        RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                        paramsMicroAvatar.height = Util.scaleWidthPx(15, outMetrics);
                        paramsMicroAvatar.width = Util.scaleWidthPx(15, outMetrics);
                        paramsMicroAvatar.setMargins(0, 0, 0, 0);
                        paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                        paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                        holder.microAvatar.setLayoutParams(paramsMicroAvatar);
                    }

                }else{
                    holder.microAvatar.setVisibility(View.GONE);

                    holder.microSurface.setVisibility(View.VISIBLE);
                    if(peers.size() < 7){
                        RelativeLayout.LayoutParams paramsMicroSurface = new RelativeLayout.LayoutParams(holder.microSurface.getLayoutParams());
                        paramsMicroSurface.height = Util.scaleWidthPx(24, outMetrics);
                        paramsMicroSurface.width = Util.scaleWidthPx(24, outMetrics);
                        paramsMicroSurface.setMargins(0, Util.scaleWidthPx(15, outMetrics),  Util.scaleWidthPx(15, outMetrics), 0);
                        paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        holder.microSurface.setLayoutParams(paramsMicroSurface);
                    }else{
                        RelativeLayout.LayoutParams paramsMicroSurface = new RelativeLayout.LayoutParams(holder.microSurface.getLayoutParams());
                        paramsMicroSurface.height = Util.scaleWidthPx(15, outMetrics);
                        paramsMicroSurface.width = Util.scaleWidthPx(15, outMetrics);
                        paramsMicroSurface.setMargins(0,  Util.scaleWidthPx(7, outMetrics),  Util.scaleWidthPx(7, outMetrics), 0);
                        paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        holder.microSurface.setLayoutParams(paramsMicroSurface);
                    }

                }
            }
        }else{
            notifyItemChanged(position);
        }
    }

    public void changesInGreenLayer(int position, ViewHolderGroupCall holder){
        log("changesInGreenLayer()");
//        notifyItemChanged(position);
        if(holder == null){
            holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(position);
        }
        if(holder!=null){
            InfoPeerGroupCall peer = getNodeAt(position);
            if (peer == null){
                return;
            }
            if(peer.hasGreenLayer()){
                holder.greenLayer.setVisibility(View.VISIBLE);
            }else{
                holder.greenLayer.setVisibility(View.GONE);
            }
        }else{
            notifyItemChanged(position);
        }
    }

    @Override
    public void resetSize(Long userHandle) {
        log("resetSize");
        if(getItemCount()!=0){
            for(InfoPeerGroupCall peer:peers){
                if(peer.getHandle() == userHandle){
                    if(peer.getListener()!=null){
                        if(peer.getListener().getWidth()!=0){
                            peer.getListener().setWidth(0);
                        }
                        if(peer.getListener().getHeight()!=0){
                            peer.getListener().setHeight(0);
                        }
                    }
                }
            }
        }
    }

    public ArrayList<InfoPeerGroupCall> getPeers() {
        return peers;
    }

    public void setPeers(ArrayList<InfoPeerGroupCall> peers) {
        this.peers = peers;
    }

    private static void log(String log) {
        Util.log("GroupCallAdapter", log);
    }


}