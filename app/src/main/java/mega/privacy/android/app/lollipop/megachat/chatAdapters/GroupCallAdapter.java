package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import java.io.File;
import java.util.ArrayList;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.listeners.CallNonContactNameListener;
import mega.privacy.android.app.lollipop.listeners.GroupCallListener;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.lollipop.megachat.calls.InfoPeerGroupCall;
import mega.privacy.android.app.lollipop.megachat.calls.MegaSurfaceRendererGroup;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static android.view.View.GONE;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class GroupCallAdapter extends RecyclerView.Adapter<GroupCallAdapter.ViewHolderGroupCall> implements MegaSurfaceRendererGroup.MegaSurfaceRendererGroupListener {

    public static final int ITEM_VIEW_TYPE_LIST = 0;
    public static final int ITEM_VIEW_TYPE_GRID = 1;
    private static final int MARGIN_MUTE_ICON_SMALL = 7;
    private static final int MARGIN_MUTE_ICON_LARGE = 15;
    private static final int SIZE_MUTE_ICON_SMALL = 15;
    private static final int SIZE_MUTE_ICON_LARGE = 24;

    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi = null;
    Display display;
    DisplayMetrics outMetrics;
    float density;
    float scaleW;
    float scaleH;
    float widthScreenPX, heightScreenPX;

    RecyclerView recyclerViewFragment;

    ArrayList<InfoPeerGroupCall> peers;
    long chatId;

    int maxScreenWidth, maxScreenHeight;
    boolean isGrid = true;
    boolean isManualMode = false;
    int statusBarHeight = 0;

    public GroupCallAdapter(Context context, RecyclerView recyclerView, ArrayList<InfoPeerGroupCall> peers, long chatId, boolean isGrid) {

        if(peers!=null){
            logDebug("peers: " + peers.size());
        }
        this.context = context;
        this.recyclerViewFragment = recyclerView;
        this.peers = peers;
        this.chatId = chatId;
        this.isGrid = isGrid;

        MegaApplication app = (MegaApplication) ((Activity) context).getApplication();
        if (megaApi == null) {
            megaApi = app.getMegaApi();
        }

        if (megaApi != null) {
            logDebug("retryPendingConnections");
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
        RelativeLayout qualityLayout;
        ImageView qualityIcon;
        EmojiTextView avatarInitialLetter;
        RelativeLayout parentSurfaceView;
        RelativeLayout surfaceMicroLayout;
        long peerId;
        public void setImageView(Bitmap bitmap){
            avatarImage.setImageBitmap(bitmap);
            avatarImage.setVisibility(View.VISIBLE);
            avatarInitialLetter.setVisibility(View.GONE);
        }


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
        logDebug("onCreateViewHolder()");

        display = ((ChatCallActivity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        widthScreenPX = outMetrics.widthPixels;
        heightScreenPX = outMetrics.heightPixels;
        density = context.getResources().getDisplayMetrics().density;
        scaleW = getScaleW(outMetrics, density);
        scaleH = getScaleH(outMetrics, density);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_camera_group_call, parent, false);

        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        maxScreenHeight = (int)heightScreenPX - statusBarHeight;
        maxScreenWidth = (int)widthScreenPX;

        holderGrid = new ViewHolderGroupCallGrid(v);

        holderGrid.rlGeneral = (RelativeLayout) v.findViewById(R.id.general);
        holderGrid.greenLayer = (RelativeLayout) v.findViewById(R.id.green_layer);
        holderGrid.surfaceMicroLayout = (RelativeLayout) v.findViewById(R.id.rl_surface_and_micro);

        holderGrid.parentSurfaceView = (RelativeLayout) v.findViewById(R.id.parent_surface_view);
        holderGrid.avatarMicroLayout = (RelativeLayout) v.findViewById(R.id.layout_avatar_micro);

        holderGrid.avatarLayout = (RelativeLayout) v.findViewById(R.id.avatar_rl);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)holderGrid.avatarLayout.getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        holderGrid.avatarLayout.setLayoutParams(layoutParams);

        holderGrid.microAvatar = (ImageView) v.findViewById(R.id.micro_avatar);
        holderGrid.microSurface = (ImageView) v.findViewById(R.id.micro_surface_view);
        holderGrid.qualityLayout = (RelativeLayout) v.findViewById(R.id.rl_quality);
        holderGrid.qualityIcon = (ImageView) v.findViewById(R.id.quality_icon);
        holderGrid.avatarImage = (RoundedImageView) v.findViewById(R.id.avatar_image);
        holderGrid.avatarInitialLetter = v.findViewById(R.id.avatar_initial_letter);
        holderGrid.avatarImage.setImageBitmap(null);
        holderGrid.avatarInitialLetter.setText("");

        v.setTag(holderGrid);
        return holderGrid;
    }

    @Override
    public void onBindViewHolder(ViewHolderGroupCall holder, int position) {
        ViewHolderGroupCallGrid holderGrid2 = (ViewHolderGroupCallGrid) holder;
        onBindViewHolderGrid(holderGrid2, position);
    }

    public void onBindViewHolderGrid (final ViewHolderGroupCallGrid holder, final int position){
        logDebug("position: " + position);

        final InfoPeerGroupCall peer = getNodeAt(position);
        if (peer == null){
            return;
        }

        holder.peerId = peer.getPeerId();
        int numPeersOnCall = getItemCount();
        logDebug("peerId = " + peer.getPeerId() + ", clientId = " + peer.getClientId() + ") of numPeersOnCall: " + numPeersOnCall);


        if(isGrid){
            CustomizedGridRecyclerView.LayoutParams lp = (CustomizedGridRecyclerView.LayoutParams) holder.rlGeneral.getLayoutParams();

            if(numPeersOnCall < 4){
                lp.height = maxScreenHeight/numPeersOnCall;
                lp.width = maxScreenWidth;

            }else if((numPeersOnCall >= 4) && (numPeersOnCall < 7)){
                lp.height = maxScreenWidth/2;
                lp.width = maxScreenWidth/2;
                if((peers.size()==5)  && (peer.getPeerId() == megaChatApi.getMyUserHandle()) && (peer.getClientId() == megaChatApi.getMyClientidHandle(chatId))){
                    ViewGroup.LayoutParams layoutParamsPeer = (ViewGroup.LayoutParams) holder.rlGeneral.getLayoutParams();
                    layoutParamsPeer.width = maxScreenWidth;
                    layoutParamsPeer.height = maxScreenWidth/2;
                    holder.rlGeneral.setLayoutParams(layoutParamsPeer);
                }
            }else{
                lp.height = scaleWidthPx(90, outMetrics);
                lp.width = scaleWidthPx(90, outMetrics);
            }
            holder.rlGeneral.setLayoutParams(lp);

        }else{

            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.rlGeneral.getLayoutParams();

            if(numPeersOnCall < 4){
                lp.height = maxScreenHeight/numPeersOnCall;
                lp.width = maxScreenWidth;

            }else if((numPeersOnCall >= 4) && (numPeersOnCall < 7)){
                lp.height = maxScreenWidth/2;
                lp.width = maxScreenWidth/2;
                if((peers.size()==5)  && (peer.getPeerId() == megaChatApi.getMyUserHandle()) && (peer.getClientId() == megaChatApi.getMyClientidHandle(chatId))){
                    ViewGroup.LayoutParams layoutParamsPeer = (ViewGroup.LayoutParams) holder.rlGeneral.getLayoutParams();
                    layoutParamsPeer.width = maxScreenWidth;
                    layoutParamsPeer.height = maxScreenWidth/2;
                    holder.rlGeneral.setLayoutParams(layoutParamsPeer);
                }
            }else{
                lp.height = scaleWidthPx(90, outMetrics);
                lp.width = scaleWidthPx(90, outMetrics);
            }
            holder.rlGeneral.setLayoutParams(lp);
        }

        if(isEstablishedCall(chatId)){
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

        holder.avatarImage.setImageBitmap(null);
        holder.avatarInitialLetter.setText("");

        if(peer.isVideoOn()) {
            logDebug("(peerid = " + peer.getPeerId() + ", clientId = " + peer.getClientId() + ") VIDEO ON pos: " + position);

            holder.avatarMicroLayout.setVisibility(GONE);
            holder.microAvatar.setVisibility(View.GONE);

            if(numPeersOnCall == 1){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = maxScreenWidth;
                layoutParamsSurface.height = maxScreenWidth;
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

                RelativeLayout.LayoutParams paramsQualityLayout = new RelativeLayout.LayoutParams(holder.qualityLayout.getLayoutParams());
                paramsQualityLayout.height = maxScreenWidth;
                paramsQualityLayout.width = maxScreenWidth;
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_TOP, R.id.parent_surface_view);
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_LEFT, R.id.parent_surface_view);
                holder.qualityLayout.setLayoutParams(paramsQualityLayout);

            }else if(numPeersOnCall == 2){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = maxScreenHeight/numPeersOnCall;
                layoutParamsSurface.height = maxScreenHeight/numPeersOnCall;
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL,RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

                RelativeLayout.LayoutParams paramsQualityLayout = new RelativeLayout.LayoutParams(holder.qualityLayout.getLayoutParams());
                paramsQualityLayout.height = maxScreenHeight/numPeersOnCall;
                paramsQualityLayout.width = maxScreenHeight/numPeersOnCall;
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_TOP, R.id.parent_surface_view);
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_LEFT, R.id.parent_surface_view);
                holder.qualityLayout.setLayoutParams(paramsQualityLayout);

            }else if(numPeersOnCall == 3){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = maxScreenHeight/numPeersOnCall;
                layoutParamsSurface.height = maxScreenHeight/numPeersOnCall;
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

                RelativeLayout.LayoutParams paramsQualityLayout = new RelativeLayout.LayoutParams(holder.qualityLayout.getLayoutParams());
                paramsQualityLayout.height = maxScreenHeight/numPeersOnCall;
                paramsQualityLayout.width = maxScreenHeight/numPeersOnCall;
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_TOP, R.id.parent_surface_view);
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_LEFT, R.id.parent_surface_view);
                holder.qualityLayout.setLayoutParams(paramsQualityLayout);

            }else if(numPeersOnCall == 4){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = maxScreenWidth/2;
                layoutParamsSurface.height = maxScreenWidth/2;
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

                if((position < 2)){
                    layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                }else{
                    layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

                }
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

                RelativeLayout.LayoutParams paramsQualityLayout = new RelativeLayout.LayoutParams(holder.qualityLayout.getLayoutParams());
                paramsQualityLayout.height = maxScreenWidth/2;
                paramsQualityLayout.width = maxScreenWidth/2;
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_TOP, R.id.parent_surface_view);
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_LEFT, R.id.parent_surface_view);
                holder.qualityLayout.setLayoutParams(paramsQualityLayout);

            }else if(numPeersOnCall == 5){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = maxScreenWidth/2;
                layoutParamsSurface.height = maxScreenWidth/2;
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

                RelativeLayout.LayoutParams paramsQualityLayout = new RelativeLayout.LayoutParams(holder.qualityLayout.getLayoutParams());
                paramsQualityLayout.height = maxScreenWidth/2;
                paramsQualityLayout.width = maxScreenWidth/2;
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_TOP, R.id.parent_surface_view);
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_LEFT, R.id.parent_surface_view);
                holder.qualityLayout.setLayoutParams(paramsQualityLayout);

            }else if(numPeersOnCall == 6){

                //Surface Layout:
                RelativeLayout.LayoutParams layoutParamsSurface = (RelativeLayout.LayoutParams) holder.parentSurfaceView.getLayoutParams();
                layoutParamsSurface.width = maxScreenWidth/2;
                layoutParamsSurface.height = maxScreenWidth/2;
                layoutParamsSurface.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                layoutParamsSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                holder.parentSurfaceView.setLayoutParams(layoutParamsSurface);

                RelativeLayout.LayoutParams paramsQualityLayout = new RelativeLayout.LayoutParams(holder.qualityLayout.getLayoutParams());
                paramsQualityLayout.height = maxScreenWidth/2;
                paramsQualityLayout.width = maxScreenWidth/2;
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_TOP, R.id.parent_surface_view);
                paramsQualityLayout.addRule(RelativeLayout.ALIGN_LEFT, R.id.parent_surface_view);
                holder.qualityLayout.setLayoutParams(paramsQualityLayout);
            }

            //Listener && SurfaceView
            if(peer.getListener() == null){
                logDebug("(peerId = " + peer.getPeerId() + ", clientId = " + peer.getClientId() + ") VIDEO ON- listener == null");
                holder.parentSurfaceView.removeAllViews();
                TextureView myTexture = new TextureView(context);
                myTexture.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                myTexture.setAlpha(1.0f);
                myTexture.setRotation(0);
                boolean isLocal;
                if((peer.getPeerId() == megaChatApi.getMyUserHandle()) && (peer.getClientId() == megaChatApi.getMyClientidHandle(chatId))){
                    isLocal = true;
                }else{
                    isLocal = false;
                }

                GroupCallListener listenerPeer = new GroupCallListener(context, myTexture, peer.getPeerId(), peer.getClientId(), isLocal);
                peer.setListener(listenerPeer);

                if((peer.getPeerId() == megaChatApi.getMyUserHandle()) && (peer.getClientId() == megaChatApi.getMyClientidHandle(chatId))){

                    megaChatApi.addChatLocalVideoListener(chatId, peer.getListener());
                } else {
                    megaChatApi.addChatRemoteVideoListener(chatId, peer.getPeerId(), peer.getClientId(), peer.getListener());
                }

                if(numPeersOnCall < 7){
                    peer.getListener().getLocalRenderer().addListener(null);
                }else{
                    peer.getListener().getLocalRenderer().addListener(this);
                }

                holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());

            }else{

                logDebug("(peerId = " + peer.getPeerId() + ", clientId = " + peer.getClientId() + ") VIDEO ON - listener != null");
                if(holder.parentSurfaceView.getChildCount() == 0){
                    if(peer.getListener().getSurfaceView().getParent()!=null){
                        if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                            ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeView(peer.getListener().getSurfaceView());
                            holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                        }else{
                            holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                        }
                    }else{
                        holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                    }
                }else{
                    if(holder.parentSurfaceView.getChildAt(0).equals(peer.getListener().getSurfaceView())){
                    }else{
                        //Remove items of parent
                        holder.parentSurfaceView.removeAllViews();
                        //Remove parent of Surface
                        if(peer.getListener().getSurfaceView().getParent()!=null){
                            if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                                ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeView(peer.getListener().getSurfaceView());
                                holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                            }else{
                                holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                            }
                        }else{
                            holder.parentSurfaceView.addView(peer.getListener().getSurfaceView());
                        }
                    }
                }

                if(peer.getListener().getHeight() != 0){
                    peer.getListener().setHeight(0);
                }
                if(peer.getListener().getWidth() != 0){
                    peer.getListener().setWidth(0);
                }
            }

            holder.surfaceMicroLayout.setVisibility(View.VISIBLE);

            //Audio icon:
            displayMuteIcon(holder.microSurface, peer);
            if(peers.size() < 7){
                RelativeLayout.LayoutParams paramsQuality = new RelativeLayout.LayoutParams(holder.qualityIcon.getLayoutParams());
                paramsQuality.height = scaleWidthPx(24, outMetrics);
                paramsQuality.width = scaleWidthPx(24, outMetrics);
                paramsQuality.setMargins(scaleWidthPx(15, outMetrics), 0,  0, scaleWidthPx(15, outMetrics));
                paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_LEFT,  RelativeLayout.TRUE);
                holder.qualityIcon.setLayoutParams(paramsQuality);
            }else{
                RelativeLayout.LayoutParams paramsQuality = new RelativeLayout.LayoutParams(holder.qualityIcon.getLayoutParams());
                paramsQuality.height = scaleWidthPx(20, outMetrics);
                paramsQuality.width = scaleWidthPx(20, outMetrics);
                paramsQuality.setMargins(scaleWidthPx(7, outMetrics), 0,  0, scaleWidthPx(7, outMetrics));
                paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_LEFT,  RelativeLayout.TRUE);
                holder.qualityIcon.setLayoutParams(paramsQuality);
            }

            if(peer.isAudioOn()){
                holder.microSurface.setVisibility(View.GONE);
            }else{
                if(isEstablishedCall(chatId)) {
                    holder.microSurface.setVisibility(View.VISIBLE);
                }else{
                    holder.microSurface.setVisibility(View.GONE);
                }
            }

            if(peer.isGoodQuality()){
                holder.qualityLayout.setVisibility(View.GONE);
            }else{
                if(isEstablishedCall(chatId)) {
                    holder.qualityLayout.setVisibility(View.VISIBLE);
                }else{
                    holder.qualityLayout.setVisibility(View.GONE);
                }
            }

            //Green Layer:
            if(numPeersOnCall < 7){
                holder.greenLayer.setVisibility(View.GONE);
                peer.setGreenLayer(false);
            }else{
                if(peer.hasGreenLayer()){
                    if(isManualMode){
                        holder.greenLayer.setBackground(ContextCompat.getDrawable(context, R.drawable.border_green_layer_selected));
                    }else {
                        holder.greenLayer.setBackground(ContextCompat.getDrawable(context, R.drawable.border_green_layer));
                    }
                    holder.greenLayer.setVisibility(View.VISIBLE);
                }else{
                    holder.greenLayer.setVisibility(View.GONE);
                }
            }

        }else{
            logDebug("(peerId = " + peer.getPeerId() + ", clientId = " + peer.getPeerId() + ") VIDEO OFF");
            //Avatar:
            setProfile(peer.getPeerId(), peer.getName(), null, holder, position);
            holder.qualityLayout.setVisibility(GONE);

            //Remove SurfaceView && Listener:
            holder.surfaceMicroLayout.setVisibility(GONE);
            if(peer.getListener() != null){
                if((peer.getPeerId() == megaChatApi.getMyUserHandle()) && (peer.getClientId() == megaChatApi.getMyClientidHandle(chatId))){
                    megaChatApi.removeChatVideoListener(chatId, -1, -1, peer.getListener());
                }else{
                    megaChatApi.removeChatVideoListener(chatId, peer.getPeerId(), peer.getClientId(), peer.getListener());
                }
                if(holder.parentSurfaceView.getChildCount() == 0){
                    if(peer.getListener().getSurfaceView().getParent()!=null){
                        if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                            ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeView(peer.getListener().getSurfaceView());
                        }
                    }
                }else{
                    holder.parentSurfaceView.removeAllViews();

                    if(peer.getListener().getSurfaceView().getParent()!=null){
                        if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                            ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeView(peer.getListener().getSurfaceView());

                        }
                    }
                }
                peer.setListener(null);
            }
            holder.avatarMicroLayout.setVisibility(View.VISIBLE);


            //Micro icon:
            if(numPeersOnCall < 7){
                RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                paramsMicroAvatar.height = scaleWidthPx(24, outMetrics);
                paramsMicroAvatar.width = scaleWidthPx(24, outMetrics);
                paramsMicroAvatar.setMargins(scaleWidthPx(10, outMetrics), 0, 0, 0);
                paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                holder.microAvatar.setLayoutParams(paramsMicroAvatar);

                ViewGroup.LayoutParams paramsAvatarImage = (ViewGroup.LayoutParams) holder.avatarImage.getLayoutParams();
                paramsAvatarImage.width = scaleWidthPx(88, outMetrics);
                paramsAvatarImage.height = scaleWidthPx(88, outMetrics);
                holder.avatarImage.setLayoutParams(paramsAvatarImage);
                holder.avatarInitialLetter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f);
                holder.avatarInitialLetter.setEmojiSize(px2dp(EMOJI_AVATAR_CALL_SMALL, outMetrics));
            }else{
                RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                paramsMicroAvatar.height = scaleWidthPx(15, outMetrics);
                paramsMicroAvatar.width = scaleWidthPx(15, outMetrics);
                paramsMicroAvatar.setMargins(0, 0, 0, 0);
                paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                holder.microAvatar.setLayoutParams(paramsMicroAvatar);

                ViewGroup.LayoutParams paramsAvatarImage = (ViewGroup.LayoutParams) holder.avatarImage.getLayoutParams();
                paramsAvatarImage.width = scaleWidthPx(60, outMetrics);
                paramsAvatarImage.height = scaleWidthPx(60, outMetrics);
                holder.avatarImage.setLayoutParams(paramsAvatarImage);
                holder.avatarInitialLetter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
                holder.avatarInitialLetter.setEmojiSize(px2dp(EMOJI_SIZE_EXTRA_HIGH, outMetrics));

            }
            if(peer.isAudioOn()){
                holder.microAvatar.setVisibility(View.GONE);
            }else{

                if(isEstablishedCall(chatId)) {
                    holder.microAvatar.setVisibility(View.VISIBLE);
                }else{
                    holder.microAvatar.setVisibility(View.GONE);
                }
            }

            //Green Layer:
            if(numPeersOnCall < 7){
                holder.greenLayer.setVisibility(View.GONE);
                peer.setGreenLayer(false);
            }else{
                if(peer.hasGreenLayer()){
                    if(isManualMode){
                        holder.greenLayer.setBackground(ContextCompat.getDrawable(context, R.drawable.border_green_layer_selected));
                    }else {
                        holder.greenLayer.setBackground(ContextCompat.getDrawable(context, R.drawable.border_green_layer));
                    }
                    holder.greenLayer.setVisibility(View.VISIBLE);
                }else{
                    holder.greenLayer.setVisibility(View.GONE);
                }
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

    //Group call: avatar
    public void setProfile(long peerId, String fullName, String peerEmail, ViewHolderGroupCall holder, int position) {
        logDebug("setProfile");

        if(holder == null){
            holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(position);
        }
        if(holder!=null){
            CallNonContactNameListener listener = new CallNonContactNameListener(context, holder, this, peerId, fullName, position);

            if (peerId == megaChatApi.getMyUserHandle()) {
                //My peer, other client
                peerEmail = megaChatApi.getMyEmail();
            } else if((peerEmail == null) || (peerId != holder.peerId)){
                peerEmail = megaChatApi.getContactEmail(peerId);
                if (peerEmail == null) {
                    megaChatApi.getUserEmail(peerId, listener);
                }
            }

            if (peerEmail != null) {
                File avatar = null;
                Bitmap bitmap = null;
                if (context != null) {
                    if (megaChatApi != null) {
                        avatar = buildAvatarFile(context,peerEmail + ".jpg");
                    }
                }
                if ((isFileAvailable(avatar)) && (avatar.length() > 0)) {
                    BitmapFactory.Options bOpts = new BitmapFactory.Options();
                    bOpts.inPurgeable = true;
                    bOpts.inInputShareable = true;
                    bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
//                    bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, bitmap, 3);
                    if (bitmap != null) {
                        holder.avatarImage.setImageBitmap(bitmap);
                        holder.avatarImage.setVisibility(View.VISIBLE);
                        holder.avatarInitialLetter.setVisibility(GONE);

                    }else{
                        createDefaultAvatar(holder, peerId, fullName, peerEmail);
                        if(peerId !=  megaChatApi.getMyUserHandle()){
                            avatar.delete();
                            if (megaApi == null) {
                                return;
                            }
                            megaApi.getUserAvatar(peerEmail,buildAvatarFile(context,peerEmail + ".jpg").getAbsolutePath(),listener);
                        }
                    }
                } else {
                    createDefaultAvatar(holder, peerId, fullName, peerEmail);
                    if(peerId != megaChatApi.getMyUserHandle()){
                        if (megaApi == null) {
                            return;
                        }
                        megaApi.getUserAvatar(peerEmail,buildAvatarFile(context,peerEmail + ".jpg").getAbsolutePath(),listener);
                    }
                }
            }

        }else{
            notifyItemChanged(position);
        }
    }

    //Group call: default my avatar
    private void createDefaultAvatar(ViewHolderGroupCall holder, long peerId, String peerName, String peerEmail){
        String name = null;
        if(peerName != null && peerName.trim().length() > 0){
            name = peerName;
        }else if(peerEmail != null && peerEmail.length() > 0) {
            name = peerEmail;
        }
        holder.avatarImage.setImageBitmap(getDefaultAvatar(getColorAvatar(peerId), name, AVATAR_SIZE, true));
    }

    public RecyclerView getListFragment() {
        return recyclerViewFragment;
    }

    public void setListFragment(RecyclerView recyclerViewFragment) {
        this.recyclerViewFragment = recyclerViewFragment;
    }

    public void changesInQuality(int position, ViewHolderGroupCall holder){

        if(holder == null){
            holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(position);
        }
        if(holder!=null){
            InfoPeerGroupCall peer = getNodeAt(position);
            if (peer == null){
                return;
            }

            if(peer.isGoodQuality()){
                holder.qualityLayout.setVisibility(View.GONE);
            }else{
                if(peer.isVideoOn()){
                    if(peers.size() < 7){
                        RelativeLayout.LayoutParams paramsQuality = new RelativeLayout.LayoutParams(holder.qualityIcon.getLayoutParams());
                        paramsQuality.height = scaleWidthPx(24, outMetrics);
                        paramsQuality.width = scaleWidthPx(24, outMetrics);
                        paramsQuality.setMargins(scaleWidthPx(15, outMetrics), 0,  0, scaleWidthPx(15, outMetrics));
                        paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_LEFT,  RelativeLayout.TRUE);
                        holder.qualityIcon.setLayoutParams(paramsQuality);
                    }else{
                        RelativeLayout.LayoutParams paramsQuality = new RelativeLayout.LayoutParams(holder.qualityIcon.getLayoutParams());
                        paramsQuality.height = scaleWidthPx(20, outMetrics);
                        paramsQuality.width = scaleWidthPx(20, outMetrics);
                        paramsQuality.setMargins(scaleWidthPx(7, outMetrics), 0,  0, scaleWidthPx(7, outMetrics));
                        paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        paramsQuality.addRule(RelativeLayout.ALIGN_PARENT_LEFT,  RelativeLayout.TRUE);
                        holder.qualityIcon.setLayoutParams(paramsQuality);
                    }
                    holder.qualityLayout.setVisibility(View.VISIBLE);
                }else{
                    holder.qualityLayout.setVisibility(View.GONE);
                }
            }
        }else{
            notifyItemChanged(position);
        }
    }

    private ViewHolderGroupCall getHolder(int position) {
        return (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(position);
    }

    private void displayMuteIcon(final ImageView microSurface, InfoPeerGroupCall peer) {
        int peerPosition = peers.indexOf(peer);
        boolean smallIcon = !(peers.size() < 7);
        int iconSize = smallIcon ? SIZE_MUTE_ICON_SMALL : SIZE_MUTE_ICON_LARGE;
        int iconRightMargin = scaleWidthPx(smallIcon ? MARGIN_MUTE_ICON_SMALL : MARGIN_MUTE_ICON_LARGE, outMetrics);
        int iconTopMargin = scaleWidthPx(smallIcon ? MARGIN_MUTE_ICON_SMALL : MARGIN_MUTE_ICON_LARGE, outMetrics);

        if (!smallIcon && ((ChatCallActivity) context).isActionBarShowing() && (peers.size() == 2 || peers.size() == 5 || peers.size() == 6)) {
            if (peerPosition == 0 || (peerPosition == 1 && peers.size() != 2)) {
                iconTopMargin += getActionBarHeight();
            }
        }

        RelativeLayout.LayoutParams paramsMicroSurface = new RelativeLayout.LayoutParams(microSurface.getLayoutParams());
        paramsMicroSurface.height = scaleWidthPx(iconSize, outMetrics);
        paramsMicroSurface.width = scaleWidthPx(iconSize, outMetrics);
        paramsMicroSurface.setMargins(0, iconTopMargin, iconRightMargin, 0);
        paramsMicroSurface.addRule(RelativeLayout.ALIGN_TOP, R.id.parent_surface_view);
        paramsMicroSurface.addRule(RelativeLayout.ALIGN_RIGHT, R.id.parent_surface_view);
        microSurface.setLayoutParams(paramsMicroSurface);
    }

    public void updateMuteIcon() {
        for (InfoPeerGroupCall peer : peers) {
            ViewHolderGroupCall holder = getHolder(peers.indexOf(peer));
            if (!peer.isAudioOn() && peer.isVideoOn()) {
                displayMuteIcon(holder.microSurface, peer);
            }
        }
    }

    public void changesInAudio(int position){
        ViewHolderGroupCall holder = getHolder(position);

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
                    //Micro icon:
                    if(peers.size() < 7){
                        RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                        paramsMicroAvatar.height = scaleWidthPx(24, outMetrics);
                        paramsMicroAvatar.width = scaleWidthPx(24, outMetrics);
                        paramsMicroAvatar.setMargins(scaleWidthPx(10, outMetrics), 0, 0, 0);
                        paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                        paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                        holder.microAvatar.setLayoutParams(paramsMicroAvatar);

                        ViewGroup.LayoutParams paramsAvatarImage = (ViewGroup.LayoutParams) holder.avatarImage.getLayoutParams();
                        paramsAvatarImage.width = scaleWidthPx(88, outMetrics);
                        paramsAvatarImage.height = scaleWidthPx(88, outMetrics);
                        holder.avatarImage.setLayoutParams(paramsAvatarImage);
                        holder.avatarInitialLetter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f);
                        holder.avatarInitialLetter.setEmojiSize(px2dp(EMOJI_AVATAR_CALL_SMALL, outMetrics));

                    }else{
                        RelativeLayout.LayoutParams paramsMicroAvatar = new RelativeLayout.LayoutParams(holder.microAvatar.getLayoutParams());
                        paramsMicroAvatar.height = scaleWidthPx(15, outMetrics);
                        paramsMicroAvatar.width = scaleWidthPx(15, outMetrics);
                        paramsMicroAvatar.setMargins(0, 0, 0, 0);
                        paramsMicroAvatar.addRule(RelativeLayout.RIGHT_OF, R.id.avatar_rl);
                        paramsMicroAvatar.addRule(RelativeLayout.ALIGN_TOP, R.id.avatar_rl);
                        holder.microAvatar.setLayoutParams(paramsMicroAvatar);

                        ViewGroup.LayoutParams paramsAvatarImage = (ViewGroup.LayoutParams) holder.avatarImage.getLayoutParams();
                        paramsAvatarImage.width = scaleWidthPx(60, outMetrics);
                        paramsAvatarImage.height = scaleWidthPx(60, outMetrics);
                        holder.avatarImage.setLayoutParams(paramsAvatarImage);
                        holder.avatarInitialLetter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
                        holder.avatarInitialLetter.setEmojiSize(px2dp(EMOJI_SIZE_EXTRA_HIGH, outMetrics));
                    }

                }else{
                    holder.microAvatar.setVisibility(View.GONE);
                    holder.microSurface.setVisibility(View.VISIBLE);

                    //Audio icon:
                    displayMuteIcon(holder.microSurface, peer);
                }
            }
        }else{
            notifyItemChanged(position);
        }
    }

    private int getActionBarHeight() {
        int actionBarHeight = ((ChatCallActivity) context).getSupportActionBar().getHeight();
        if (actionBarHeight != 0) {
            return actionBarHeight;
        }

        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    public void updateMode(boolean flag){
        isManualMode = flag;
    }

    public void changesInGreenLayer(int position, ViewHolderGroupCall holder){
        logDebug("changesInGreenLayer()");
        if(holder == null){
            holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(position);
        }
        if(holder!=null){
            InfoPeerGroupCall peer = getNodeAt(position);
            if (peer == null){
                return;
            }
            if(peer.hasGreenLayer()){
                if(isManualMode){
                    holder.greenLayer.setBackground(ContextCompat.getDrawable(context, R.drawable.border_green_layer_selected));
                }else {
                    holder.greenLayer.setBackground(ContextCompat.getDrawable(context, R.drawable.border_green_layer));
                }
                holder.greenLayer.setVisibility(View.VISIBLE);
            }else{
                holder.greenLayer.setVisibility(View.GONE);
            }

        }else{
            notifyItemChanged(position);
        }
    }

    @Override
    public void resetSize(long peerid, long clientid) {
        logDebug("Peer ID: " + peerid + ", Client ID: " + clientid);
        if(getItemCount()!=0){

           if((peers!=null)&&(peers.size()>0)){
               for(InfoPeerGroupCall peer:peers){
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

    public void onDestroy(){
        logDebug("onDestroy()");
        ViewHolderGroupCall holder = null;
        if((peers!=null)&&(peers.size()>0)) {
            for(int i=0; i<peers.size(); i++){
                if(holder == null){
                    holder = (ViewHolderGroupCall) recyclerViewFragment.findViewHolderForAdapterPosition(i);
                }
                if(holder!=null){
                    logDebug("holder != null");
                    InfoPeerGroupCall peer = getNodeAt(i);
                    if (peer == null){
                        return;
                    }
                    //Remove SurfaceView && Listener:
                    if(peer.getListener() != null){
                        if((peer.getPeerId() == megaChatApi.getMyUserHandle()) && (peer.getClientId() == megaChatApi.getMyClientidHandle(chatId))){
                            megaChatApi.removeChatVideoListener(chatId, -1, -1, peer.getListener());
                        }else{
                            megaChatApi.removeChatVideoListener(chatId, peer.getPeerId(), peer.getClientId(), peer.getListener());
                        }
                        if(holder.parentSurfaceView.getChildCount() == 0){
                            if(peer.getListener().getSurfaceView().getParent()!=null){
                                if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                                    ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeView(peer.getListener().getSurfaceView());
                                }
                            }
                        }else{

                            holder.parentSurfaceView.removeAllViews();
                            holder.parentSurfaceView.removeAllViewsInLayout();
                            if(peer.getListener().getSurfaceView().getParent()!=null){
                                if(peer.getListener().getSurfaceView().getParent().getParent()!=null){
                                    ((ViewGroup)peer.getListener().getSurfaceView().getParent()).removeView(peer.getListener().getSurfaceView());
                                }
                            }
                        }
                        peer.getListener().getSurfaceView().setVisibility(GONE);
                        peer.setListener(null);
                    }

                }else{
                    logWarning("holder == null");

//                notifyItemChanged(i);
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
}