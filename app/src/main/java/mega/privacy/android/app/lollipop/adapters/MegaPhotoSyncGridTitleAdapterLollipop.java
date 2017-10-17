package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaMonthPic;
import mega.privacy.android.app.MegaStreamingService;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MegaMonthPicLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

/**
 * Created by mega on 4/08/17.
 */

public class MegaPhotoSyncGridTitleAdapterLollipop extends RecyclerView.Adapter<MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle>{

    private class Media {
        public String filePath;
        public long timestamp;
    }

    public static int PADDING_GRID_LARGE = 6;
    public static int PADDING_GRID_SMALL = 3;

    public static final int TYPE_ITEM_TITLE = 3;
    public static final int TYPE_ITEM_IMAGE = 1;
    public static final int TYPE_ITEM_VIDEO = 2;
    public static final int TYPE_NO_TYPE = 0;

    private MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder = null;

    private Context context;
    private MegaApplication app;
    private MegaApiAndroid megaApi;

    private ArrayList<MegaMonthPicLollipop> monthPics;
    private ArrayList<MegaNode> nodes;

    private long photosyncHandle = -1;

    private RecyclerView listFragment;
    private ImageView emptyImageViewFragment;
    private TextView emptyTextViewFragment;
    private ActionBar aB;

    private int numberOfCells;
    private int gridWidth;

    private boolean multipleSelect;

    private SparseBooleanArray checkedItems = new SparseBooleanArray();

    private int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;

    private Object fragment;
    private int type = Constants.CAMERA_UPLOAD_ADAPTER;

    private ActionMode actionMode;

    private int count;
    private int countTitles;

    private List<ItemInformation> itemInformationList;

    public static class ItemInformation{
        public int type = -1;
        public String name = null;
        public MegaNode n = null;
        public MegaMonthPicLollipop megaMonthPic = null;

        public ItemInformation(int type, String name, MegaMonthPicLollipop megaMonthPic){
            this.type = type;
            this.name = name;
            this.megaMonthPic = megaMonthPic;
        }

        public ItemInformation(int type, MegaNode n, MegaMonthPicLollipop megaMonthPic){
            this.type = type;
            this.n = n;
            this.megaMonthPic = megaMonthPic;
        }
    }

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            log("onActionItemClicked");
            List<MegaNode> documents = getSelectedDocuments();

            switch(item.getItemId()){
                case R.id.cab_menu_download:{
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i=0;i<documents.size();i++){
                        handleList.add(documents.get(i).getHandle());
                    }
                    clearSelections();
                    hideMultipleSelect();
                    NodeController nC = new NodeController(context);
                    nC.prepareForDownload(handleList);
                    break;
                }
                case R.id.cab_menu_copy:{
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i=0;i<documents.size();i++){
                        handleList.add(documents.get(i).getHandle());
                    }
                    clearSelections();
                    hideMultipleSelect();
                    NodeController nC = new NodeController(context);
                    nC.chooseLocationToCopyNodes(handleList);
                    break;
                }
                case R.id.cab_menu_move:{
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i=0;i<documents.size();i++){
                        handleList.add(documents.get(i).getHandle());
                    }
                    clearSelections();
                    hideMultipleSelect();
                    NodeController nC = new NodeController(context);
                    nC.chooseLocationToMoveNodes(handleList);
                    break;
                }
                case R.id.cab_menu_share_link:{
                    clearSelections();
                    hideMultipleSelect();
                    if (documents.size()==1){
                        //NodeController nC = new NodeController(context);
                        //nC.exportLink(documents.get(0));
                        if(documents.get(0)==null){
                            log("The selected node is NULL");
                            break;
                        }
                        ((ManagerActivityLollipop) context).showGetLinkActivity(documents.get(0).getHandle());
                    }
                    break;
                }
                case R.id.cab_menu_share_link_remove:{

                    clearSelections();
                    hideMultipleSelect();
                    if (documents.size()==1){
                        //NodeController nC = new NodeController(context);
                        //nC.removeLink(documents.get(0));
                        if(documents.get(0)==null){
                            log("The selected node is NULL");
                            break;
                        }
                        ((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(documents.get(0));

                    }
                    break;

                }
                case R.id.cab_menu_trash:{
                    ArrayList<Long> handleList = new ArrayList<Long>();
                    for (int i=0;i<documents.size();i++){
                        handleList.add(documents.get(i).getHandle());
                    }
                    clearSelections();
                    hideMultipleSelect();
                    ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                    break;
                }
                case R.id.cab_menu_select_all:{
                    ((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
                    selectAll();
                    break;
                }
                case R.id.cab_menu_unselect_all:{
                    clearSelections();
                    hideMultipleSelect();
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            log("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.file_browser_action, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            log("onDestroyActionMode");
            multipleSelect = false;
            clearSelections();
            actionMode = null;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            log("onPrepareActionMode");
            List<MegaNode> selected = getSelectedDocuments();

            log("the num of items selected is "+selected.size());

            boolean showDownload = false;
            boolean showRename = false;
            boolean showCopy = false;
            boolean showMove = false;
            boolean showLink = false;
            boolean showTrash = false;
            boolean showRemoveLink = false;


            // Link
            if ((selected.size() == 1) && (megaApi.checkAccess(selected.get(0), MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK)) {
                if(selected.get(0).isExported()){
                    //Node has public link
                    showRemoveLink=true;
                    showLink=false;

                }
                else{
                    showRemoveLink=false;
                    showLink=true;
                }			}

            if (selected.size() != 0) {
                showDownload = true;
                showTrash = true;
                showMove = true;
                showCopy = true;

                for(int i=0; i<selected.size();i++)	{
                    if(megaApi.checkMove(selected.get(i), megaApi.getRubbishNode()).getErrorCode() != MegaError.API_OK)	{
                        showTrash = false;
                        showMove = false;
                        break;
                    }
                }

                if(selected.size() >= count - countTitles){
                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);
                }
                else{
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(true);
                }
            }
            else{
                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
            }

            if(showCopy){
                menu.findItem(R.id.cab_menu_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            if(showDownload){
                menu.findItem(R.id.cab_menu_download).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            if(showLink){
                menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            if(showRemoveLink){
                menu.findItem(R.id.cab_menu_share_link).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                menu.findItem(R.id.cab_menu_share_link_remove).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            if(showMove){
                if(selected.size()==1){
                    menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                }else{
                    menu.findItem(R.id.cab_menu_move).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
            }

            menu.findItem(R.id.cab_menu_download).setVisible(showDownload);
            menu.findItem(R.id.cab_menu_rename).setVisible(showRename);
            menu.findItem(R.id.cab_menu_copy).setVisible(showCopy);
            menu.findItem(R.id.cab_menu_move).setVisible(showMove);
            menu.findItem(R.id.cab_menu_share_link).setVisible(showLink);
            menu.findItem(R.id.cab_menu_share_link_remove).setVisible(showRemoveLink);

            menu.findItem(R.id.cab_menu_trash).setVisible(showTrash);
            menu.findItem(R.id.cab_menu_leave_multiple_share).setVisible(false);

            return false;
        }

    }

    @Override
    public ViewHolderPhotoTitleSyncGridTitle onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onBindViewHolder");

        log("onCreateViewHolder");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = ((Activity) context).getResources().getDisplayMetrics().density;

        float scaleW = Util.getScaleW(outMetrics, density);
        float scaleH = Util.getScaleH(outMetrics, density);

        float dpHeight = outMetrics.heightPixels / density;
        float dpWidth  = outMetrics.widthPixels / density;

//		Toast.makeText(context, "W: " + dpWidth + "__H: " + dpHeight, Toast.LENGTH_SHORT).show();
//		Toast.makeText(context, "Wpx: " + outMetrics.widthPixels + "__H: " + outMetrics.heightPixels, Toast.LENGTH_SHORT).show();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.cell_photosync_grid_title, parent, false);

        int padding = 0;
        if(numberOfCells == CameraUploadFragmentLollipop.GRID_LARGE){
            log("numOfCells is GRID_LARGE");

            padding = PADDING_GRID_LARGE;
        }
        else if (numberOfCells == CameraUploadFragmentLollipop.GRID_SMALL){
            log("numOfCells is GRID_SMALL");
            padding = PADDING_GRID_SMALL;
        }
        else{
            log("numOfCells is "+numberOfCells);
            padding = 2;
//				iV.setPadding(padding, padding, padding, padding);
        }
        holder = new MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle(v, gridWidth, padding, this);
        holder.setDocument(-1l);
//		//Margins
//		RelativeLayout.LayoutParams contentTextParams = (RelativeLayout.LayoutParams)holder.textView.getLayoutParams();
//		contentTextParams.setMargins(Util.scaleWidthPx(63, outMetrics), Util.scaleHeightPx(5, outMetrics), 0, Util.scaleHeightPx(5, outMetrics));
//		holder.textView.setLayoutParams(contentTextParams);


        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderPhotoTitleSyncGridTitle holder, int position) {
//        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        display.getMetrics(outMetrics);
//        float density = ((Activity) context).getResources().getDisplayMetrics().density;
//
//        float scaleW = Util.getScaleW(outMetrics, density);
//        float scaleH = Util.getScaleH(outMetrics, density);

        ItemInformation itemInformation = getInformationOfPosition(position);
        switch (itemInformation.type){
            case TYPE_ITEM_TITLE:{
                holder.setDataTitle(itemInformation.name);
                break;
            }
            case TYPE_ITEM_IMAGE:{
                holder.setMegaMonthPicLollipop(itemInformation.megaMonthPic);
                int positionNodes = (int) itemInformation.megaMonthPic.getPosition(itemInformation.n);
                holder.setDataImage(itemInformation.n, isChecked(positionNodes), multipleSelect, position, positionNodes);
                holder.setGridWidth(gridWidth, numberOfCells);

                break;
            }
            case TYPE_ITEM_VIDEO:{
                holder.setMegaMonthPicLollipop(itemInformation.megaMonthPic);
                int positionNodes = (int) itemInformation.megaMonthPic.getPosition(itemInformation.n);
                holder.setDataVideo(itemInformation.n, isChecked(positionNodes), multipleSelect, numberOfCells, position, positionNodes);
                holder.setGridWidth(gridWidth, numberOfCells);
                break;
            }
            case TYPE_NO_TYPE:
                log("Error, NO TYPE");
                break;
        }


    }

    @Override
    public int getItemCount() {
        return count;
    }

    public class ViewHolderPhotoTitleSyncGridTitle extends RecyclerView.ViewHolder implements ThumbnailUtilsLollipop.ThumbnailInterface{

        private RelativeLayout layout_title;
        private TextView title;
        private ImageView photo;
        private RelativeLayout layout_videoInfo;
        private TextView videoDuration;
        private ImageView videoIcon;
        private RelativeLayout gradient_effect;
        private ImageView click_icon;
        private RelativeLayout click_unselected;
        private RelativeLayout content_layout;
        private long document;
        private MegaMonthPicLollipop megaMonthPicLollipop;
        private int positionAdapter;
        private int positionNodes;
        private RecyclerView.Adapter adapter;
        private int type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_NO_TYPE;
        private int margins = 0;
        private int gridWidth = 0;

        public ViewHolderPhotoTitleSyncGridTitle(View itemView, int gridWidth, int margins, RecyclerView.Adapter adapter) {
            super(itemView);
            layout_title = (RelativeLayout) itemView.findViewById(R.id.cell_photosync_grid_title_layout);
            layout_title.setVisibility(View.GONE);
            title = (TextView) itemView.findViewById(R.id.cell_photosync_grid_title_title);
            photo = (ImageView) itemView.findViewById(R.id.cell_photosync_grid_title_thumbnail);
            layout_videoInfo = (RelativeLayout) itemView.findViewById(R.id.cell_item_videoinfo_layout);
            layout_videoInfo.setVisibility(View.GONE);
            videoDuration = (TextView) itemView.findViewById(R.id.cell_photosync_grid_title_video_duration);
            gradient_effect = (RelativeLayout) itemView.findViewById(R.id.cell_photosync_title_gradient_effect);
            gradient_effect.setVisibility(View.GONE);
            click_icon = (ImageView) itemView.findViewById(R.id.cell_photosync_title_menu_long_click_select);
            click_icon.setVisibility(View.GONE);
            click_unselected = (RelativeLayout) itemView.findViewById(R.id.cell_photosync_title_menu_long_click_unselected);
            click_unselected.setVisibility(View.GONE);
            videoIcon = (ImageView) itemView.findViewById(R.id.cell_photosync_grid_title_video_icon);
            content_layout = (RelativeLayout) itemView.findViewById(R.id.cell_item_grid_title_layout);

            this.margins = margins;
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) content_layout.getLayoutParams();
            marginParams.setMargins(margins, margins, margins, margins);
            content_layout.setLayoutParams(marginParams);

            this.gridWidth = gridWidth;
            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) content_layout.getLayoutParams();
            params.height = gridWidth;
            params.width = gridWidth;
            content_layout.setLayoutParams(params);

            click_icon.setMaxHeight(gridWidth);
            click_icon.setMaxWidth(gridWidth);

            content_layout.setVisibility(View.GONE);

            this.adapter = adapter;

            positionAdapter = -1;
            positionNodes = -1;
            final MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle thisClass = this;
            photo.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if(type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_NO_TYPE || type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE){
                        return;
                    }
                    long handle = document;

                    MegaNode n = megaApi.getNodeByHandle(handle);
                    if (n != null){
                        onNodeClick(thisClass, thisClass.getPositionNodes());
                    }
                }
            } );

            photo.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    if(type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_NO_TYPE || type == MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE){
                        return true;
                    }
                    long handle = document;

                    MegaNode n = megaApi.getNodeByHandle(handle);
                    if (n != null){
                        int positionInNodes = thisClass.getPositionNodes();
                        onNodeLongClick(thisClass, positionInNodes);
                    }

                    return true;
                }
            });
        }

        public void setDataTitle(String txt){
            layout_title.setVisibility(View.VISIBLE);
            content_layout.setVisibility(View.GONE);
            title.setText(txt);

            type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_TITLE;
        }

        public void setDataImage(MegaNode n, boolean checked, boolean multipleSelect, int positionAdapter, int positionNodes){
            layout_title.setVisibility(View.GONE);
            content_layout.setVisibility(View.VISIBLE);
            layout_videoInfo.setVisibility(View.GONE);
            gradient_effect.setVisibility(View.GONE);

            this.positionAdapter = positionAdapter;
            this.positionNodes = positionNodes;


            type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_IMAGE;

            if (multipleSelect){
                if (checked){
                    click_icon.setVisibility(View.VISIBLE);
                    click_unselected.setVisibility(View.GONE);
                }
                else{
                    click_icon.setVisibility(View.GONE);
                    click_unselected.setVisibility(View.VISIBLE);
                }
            }
            else{
                click_icon.setVisibility(View.GONE);
                click_unselected.setVisibility(View.GONE);
            }

            if(n == null){
                log("n is null");
                return;
            }

            document = n.getHandle();

//            LoadImage task = new LoadImage(n, type, 0);
//            task.execute(holder);

            Bitmap thumb = null;
            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
            postSetImageView();

            if (n.hasThumbnail()){
                thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
                if (thumb != null){
                    photo.setImageBitmap(thumb);
                    postSetImageView();
                }
                else{
                    thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
                    if (thumb != null){
                        photo.setImageBitmap(thumb);
                        postSetImageView();
                    }
                    else{
                        try{
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromThumbnailInterface(n, context, this, megaApi, adapter);
                        }
                        catch(Exception e){} //Too many AsyncTasks

                        if (thumb != null){
                            photo.setImageBitmap(thumb);
                            postSetImageView();
                        }
                        else{
                            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
                            postSetImageView();
                        }
                    }
                }
            }
            else{
                log(n.getName()+" NO ThUMB!!");
            }

        }

        public void setDataVideo(MegaNode n, boolean checked, boolean multipleSelect, int numberOfColumns, int positionAdapter, int positionNodes){
            layout_title.setVisibility(View.GONE);
            content_layout.setVisibility(View.VISIBLE);
            layout_videoInfo.setVisibility(View.VISIBLE);
            videoDuration.setVisibility(View.VISIBLE);
            gradient_effect.setVisibility(View.VISIBLE);

            this.positionAdapter = positionAdapter;
            this.positionNodes = positionNodes;

            type = MegaPhotoSyncGridTitleAdapterLollipop.TYPE_ITEM_VIDEO;

            if (multipleSelect){
                if (checked){
                    click_icon.setVisibility(View.VISIBLE);
                    click_unselected.setVisibility(View.GONE);
                }
                else{
                    click_icon.setVisibility(View.GONE);
                    click_unselected.setVisibility(View.VISIBLE);
                }
            }
            else{
                click_icon.setVisibility(View.GONE);
                click_unselected.setVisibility(View.GONE);
            }

            if(n == null){
                log("n is null");
                return;
            }

            document = n.getHandle();

//            LoadImage task = new LoadImage(n, type, numberOfColumns);
//            task.execute(this);

            Bitmap thumb = null;
            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
            postSetImageView();
            if (n.hasThumbnail()){
                thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
                if (thumb != null){
                    photo.setImageBitmap(thumb);
                    postSetImageView();
                }
                else{
                    thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
                    if (thumb != null){
                        photo.setImageBitmap(thumb);
                    }
                    else{
                        try{
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromThumbnailInterface(n, context, this, megaApi, adapter);
                        }
                        catch(Exception e){} //Too many AsyncTasks

                        if (thumb != null){
                            photo.setImageBitmap(thumb);
                        }
                        else{
                            photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
                        }
                        postSetImageView();
                    }
                }
            }
            else{
                log(n.getName()+" NO ThUMB!!");
            }

            if(numberOfColumns == CameraUploadFragmentLollipop.GRID_LARGE){
                videoIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                log(n.getName()+" DURATION: "+n.getDuration());
                int duration = n.getDuration();
                if(duration>0){
                    int hours = duration / 3600;
                    int minutes = (duration % 3600) / 60;
                    int seconds = duration % 60;

                    String timeString;
                    if(hours>0){
                        timeString = String.format("%d:%d:%02d", hours, minutes, seconds);
                    }
                    else{
                        timeString = String.format("%d:%02d", minutes, seconds);
                    }

                    log("The duration is: "+hours+" "+minutes+" "+seconds);

                    videoDuration.setText(timeString);
                }
                else{
                    videoDuration.setVisibility(View.GONE);
                }
            }
            else{
                videoIcon.setImageResource(R.drawable.ic_play_arrow_white_18dp);
                videoDuration.setVisibility(View.GONE);
            }
        }

        public long getDocument() {
            return document;
        }

        @Override
        public ImageView getImageView() {
            return photo;
        }

        @Override
        public int getPositionOnAdapter() {
            return positionAdapter;
        }

        @Override
        public void postSetImageView() {
        }

        @Override
        public void preSetImageView() {

        }

        @Override
        public void setBitmap(Bitmap bitmap) {
            photo.setImageBitmap(bitmap);
        }

        public void setDocument(long document) {
            this.document = document;
        }

        public void setMegaMonthPicLollipop(MegaMonthPicLollipop megaMonthPicLollipop) {
            this.megaMonthPicLollipop = megaMonthPicLollipop;
        }

        public int getPositionNodes() {
            return positionNodes;
        }

        public void setPositionNodes(int positionNodes) {
            this.positionNodes = positionNodes;
        }

        public int getGridWidth() {
            return gridWidth;
        }

        public void setGridWidth(int gridWidth, int numberOfCells) {
            if(this.gridWidth != gridWidth){
                this.gridWidth = gridWidth;
                ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) content_layout.getLayoutParams();
                params.height = gridWidth;
                params.width = gridWidth;
                content_layout.setLayoutParams(params);
                int padding = 0;
                if(numberOfCells == CameraUploadFragmentLollipop.GRID_LARGE){
                    log("numOfCells is GRID_LARGE");

                    padding = PADDING_GRID_LARGE;
                }
                else if (numberOfCells == CameraUploadFragmentLollipop.GRID_SMALL){
                    log("numOfCells is GRID_SMALL");
                    padding = PADDING_GRID_SMALL;
                }
                else{
                    log("numOfCells is "+numberOfCells);
                    padding = 2;
                }
                setMargins(padding);
            }
        }

        public int getMargins() {
            return margins;
        }

        public void setMargins(int margins) {
            if(this.margins != margins){
                this.margins = margins;
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) content_layout.getLayoutParams();
                marginParams.setMargins(margins, margins, margins, margins);
                content_layout.setLayoutParams(marginParams);
            }
        }

//        private class LoadImage extends AsyncTask<ViewHolderPhotoTitleSyncGridTitle, Void, Bitmap> {
//
//            private MegaNode n;
//            private int type;
//            private int numberOfColumns;
//
//            public LoadImage(MegaNode n, int type, int numberOfColumns) {
//                this.n = n;
//                this.type = type;
//                this.numberOfColumns = numberOfColumns;
//            }
//
//            @Override
//            protected void onPreExecute(){
//                if(type == TYPE_ITEM_IMAGE){
//                    photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
//                }
//                else{
//                    photo.setImageResource(MimeTypeThumbnail.typeForName(n.getName()).getIconResourceId());
//                    if(numberOfColumns == CameraUploadFragmentLollipop.GRID_LARGE){
//                        videoIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
//                        log(n.getName()+" DURATION: "+n.getDuration());
//                        int duration = n.getDuration();
//                        if(duration>0){
//                            int hours = duration / 3600;
//                            int minutes = (duration % 3600) / 60;
//                            int seconds = duration % 60;
//
//                            String timeString;
//                            if(hours>0){
//                                timeString = String.format("%d:%d:%02d", hours, minutes, seconds);
//                            }
//                            else{
//                                timeString = String.format("%d:%02d", minutes, seconds);
//                            }
//
//                            log("The duration is: "+hours+" "+minutes+" "+seconds);
//
//                            videoDuration.setText(timeString);
//                        }
//                        else{
//                            videoDuration.setVisibility(View.GONE);
//                        }
//                    }
//                    else{
//                        videoIcon.setImageResource(R.drawable.ic_play_arrow_white_18dp);
//                        videoDuration.setVisibility(View.GONE);
//                    }
//                }
//            }
//
//            @Override
//            protected Bitmap doInBackground(ViewHolderPhotoTitleSyncGridTitle... holders) {
//                if (n.hasThumbnail()){
//                    Bitmap thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(n);
//                    if (thumb != null){
//                        return thumb;
//                    }
//                    else{
//                        thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(n, context);
//                        if (thumb != null){
//                            return thumb;
//                        }
//                        else{
//                            try{
//                                thumb = ThumbnailUtilsLollipop.getThumbnailFromThumbnailInterface(n, context, holders[0], megaApi, adapter);
//                            }
//                            catch(Exception e){} //Too many AsyncTasks
//
//                            if (thumb != null){
//                                return thumb;
//                            }
//                        }
//                    }
//                }
//                else{
//                    log(n.getName()+" NO ThUMB!!");
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Bitmap result){
//                if(result != null){
//                    photo.setImageBitmap(result);
//                    postSetImageView();
//                }
//            }
//        };

    }

    public MegaPhotoSyncGridTitleAdapterLollipop(Context _context, ArrayList<MegaMonthPicLollipop> _monthPics, long _photosyncHandle, RecyclerView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes, int numberOfCells, int gridWidth, Object fragment, int type, int count, int countTitles, List<ItemInformation> itemInformationList) {
        this.context = _context;
        this.monthPics = _monthPics;
        this.photosyncHandle = _photosyncHandle;
        this.nodes = _nodes;

        this.listFragment = listView;
        this.emptyImageViewFragment = emptyImageView;
        this.emptyTextViewFragment = emptyTextView;
        this.aB = aB;
        this.fragment = fragment;
        this.type = type;
        this.numberOfCells = numberOfCells;
        this.gridWidth = gridWidth;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        this.app = ((MegaApplication) ((Activity) context).getApplication());

        this.count = count;
        this.countTitles = countTitles;

        this.itemInformationList = itemInformationList;
    }

    public void setNumberOfCells(int numberOfCells, int gridWidth){
        this.numberOfCells = numberOfCells;
        this.gridWidth = gridWidth;
        notifyDataSetChanged();
    }

    public void setNodes(ArrayList<MegaMonthPicLollipop> monthPics, ArrayList<MegaNode> nodes, int count, int countTitles, List<ItemInformation> itemInformationList){
        this.monthPics = monthPics;
        this.nodes = nodes;
        this.count = count;
        this.countTitles = countTitles;
        this.itemInformationList = itemInformationList;
        notifyDataSetChanged();
    }

    public void setPhotoSyncHandle(long photoSyncHandle){
        this.photosyncHandle = photoSyncHandle;
        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        return monthPics.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect){
        this.multipleSelect = multipleSelect;
    }

    public void setOrder(int orderGetChildren){
        this.orderGetChildren = orderGetChildren;
    }

    public long getPhotoSyncHandle(){
        return photosyncHandle;
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        this.multipleSelect = false;
        ((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_TRANSPARENT_BLACK);
        clearSelections();

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void selectAll(){

        this.multipleSelect = true;
        if(nodes != null){
            for(int i=0; i<nodes.size(); i++){
                if (nodes.get(i).isFolder()){
                    continue;
                }

                if (!MimeTypeList.typeForName(nodes.get(i).getName()).isImage() && (!MimeTypeList.typeForName(nodes.get(i).getName()).isVideo())){
                    continue;
                }
                checkedItems.append(i, true);
            }
        }
        if (actionMode == null){
            actionMode = ((AppCompatActivity)context).startSupportActionMode(new MegaPhotoSyncGridTitleAdapterLollipop.ActionBarCallBack());
        }

        updateActionModeTitle();

        notifyDataSetChanged();
    }

    public void clearSelections() {
        log("clearSelections");
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.valueAt(i) == true) {
                int checkedPosition = checkedItems.keyAt(i);
                checkedItems.append(checkedPosition, false);
            }
        }
        this.multipleSelect = false;
        updateActionModeTitle();

        notifyDataSetChanged();
    }

    public boolean isChecked(int totalPosition){

        if (!multipleSelect){
            return false;
        }
        else{

            if (checkedItems.get(totalPosition, false) == false){
                return false;
            }
            else{
                return true;
            }
        }
    }

    public void onNodeClick(MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder, int positionInNodes){
        log("onNodeClick");

        if (!multipleSelect){
            MegaNode n = megaApi.getNodeByHandle(holder.getDocument());
            if (n != null){
                if (!n.isFolder()){
                    if (MimeTypeThumbnail.typeForName(n.getName()).isImage()){

                        Intent intent = new Intent(context, FullScreenImageViewerLollipop.class);
                        intent.putExtra("position", positionInNodes);
                        intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
                        intent.putExtra("adapterType", Constants.PHOTO_SYNC_ADAPTER);
                        intent.putExtra("orderGetChildren", orderGetChildren);
                        MyAccountInfo accountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
                        if(accountInfo!=null){
                            intent.putExtra("typeAccount", accountInfo.getAccountType());
                        }
                        log("Position in nodes: "+positionInNodes);
                        if (megaApi.getParentNode(nodes.get(positionInNodes)).getType() == MegaNode.TYPE_ROOT){
                            intent.putExtra("parentNodeHandle", -1L);
                        }
                        else{
                            intent.putExtra("parentNodeHandle", megaApi.getParentNode(nodes.get(positionInNodes)).getHandle());
                        }
                        context.startActivity(intent);
                    }
                    else if (MimeTypeThumbnail.typeForName(n.getName()).isVideo() || MimeTypeThumbnail.typeForName(n.getName()).isAudio() ){
                        MegaNode file = n;
                        Intent service = new Intent(context, MegaStreamingService.class);
                        context.startService(service);
                        String fileName = file.getName();
                        try {
                            fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
                        }
                        catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
                        String mimeType = MimeTypeThumbnail.typeForName(file.getName()).getType();
                        System.out.println("FILENAME: " + fileName);

                        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
                        mediaIntent.setDataAndType(Uri.parse(url), mimeType);
                        if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
                            context.startActivity(mediaIntent);
                        }
                        else{
                            Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
                            ArrayList<Long> handleList = new ArrayList<Long>();
                            handleList.add(n.getHandle());
                            NodeController nC = new NodeController(context);
                            nC.prepareForDownload(handleList);
                        }
                    }
                    else{
                        ArrayList<Long> handleList = new ArrayList<Long>();
                        handleList.add(n.getHandle());
                        NodeController nC = new NodeController(context);
                        nC.prepareForDownload(handleList);
                    }
                    notifyDataSetChanged();
                }
            }
        }
        else{
            if (checkedItems.get(positionInNodes, false) == false){
                checkedItems.append(positionInNodes, true);
            }
            else{
                checkedItems.append(positionInNodes, false);
            }
            List<MegaNode> selectedNodes = getSelectedDocuments();
            if (selectedNodes.size() > 0){
                updateActionModeTitle();
                notifyItemChanged(holder.getPositionOnAdapter());
            }
            else{
                hideMultipleSelect();
            }
        }
    }

    public void onNodeLongClick(MegaPhotoSyncGridTitleAdapterLollipop.ViewHolderPhotoTitleSyncGridTitle holder, int positionInNodes){
        log("onNodeLongClick");
        if (!multipleSelect){
            clearSelections();

            this.multipleSelect = true;
            checkedItems.append(positionInNodes, true);

            actionMode = ((AppCompatActivity)context).startSupportActionMode(new MegaPhotoSyncGridTitleAdapterLollipop.ActionBarCallBack());

            updateActionModeTitle();
            ((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);

            notifyItemChanged(holder.getPositionOnAdapter());
        }
        else{
            onNodeClick(holder, positionInNodes);
        }
    }

    private void updateActionModeTitle() {
        log("updateActionModeTitle");
        if (actionMode == null){
            log("actionMode null");
            return;
        }

        if (context == null){
            log("context null");
            return;
        }

        List<MegaNode> documents = getSelectedDocuments();
        int files = 0;
        int folders = 0;
        for (MegaNode document : documents) {
            if (document.isFile()) {
                files++;
            } else if (document.isFolder()) {
                folders++;
            }
        }
        Resources res = context.getResources();
        String title;
        int sum=files+folders;

        if (files == 0 && folders == 0) {
            title = Integer.toString(sum);
        } else if (files == 0) {
            title = Integer.toString(folders);
        } else if (folders == 0) {
            title = Integer.toString(files);
        } else {
            title = Integer.toString(sum);
        }
        actionMode.setTitle(title);
        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("oninvalidate error");
        }
        // actionMode.
    }

    /*
     * Get list of all selected documents
     */
    public List<MegaNode> getSelectedDocuments() {
        log("getSelectedDocuments");
        ArrayList<MegaNode> documents = new ArrayList<MegaNode>();
        for (int i = 0; i < checkedItems.size(); i++) {
            if (checkedItems.valueAt(i) == true) {
                MegaNode document = null;
                try {
                    if (nodes != null) {
                        document = nodes.get(checkedItems.keyAt(i));
                    }
                }
                catch (IndexOutOfBoundsException e) {}

                if (document != null){
                    documents.add(document);
                }
            }
        }

        return documents;
    }

    private int getTypeOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            log("size of temp: "+temp.nodeHandles);
            if(position == 0){
                return TYPE_ITEM_TITLE;
            }
            if(position > 0 && position-1 < temp.nodeHandles.size()){
                MegaNode n = megaApi.getNodeByHandle(temp.nodeHandles.get(position - 1));
                if(Util.isVideoFile(n.getName())){
                    return TYPE_ITEM_VIDEO;
                }
                else{
                    return TYPE_ITEM_IMAGE;
                }
            }
            position -= (temp.nodeHandles.size() + 1);
        }
        return TYPE_NO_TYPE;
    }

    @Nullable
    private Object getItemOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            if(position == 0){
                log("Title is "+temp.monthYearString);
                return temp.monthYearString;
            }
            if(position > 0 && position < temp.nodeHandles.size() + 1){
                return megaApi.getNodeByHandle(temp.nodeHandles.get(position - 1));
            }
            if(temp.nodeHandles.size() == 0)
                continue;
            position -= (temp.nodeHandles.size() + 1);
        }
        return null;
    }

    private ItemInformation getInformationOfPosition(int position){
        return itemInformationList.get(position);
    }

    private long getNodesPositionOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            if(position == 0){
                return -1;
            }
            if(position < temp.nodeHandles.size() + 1){
                return temp.getPosition(temp.nodeHandles.get(position - 1));
            }
            position -= (temp.nodeHandles.size() + 1);
        }
        return -1;
    }

    private MegaMonthPicLollipop getMegaMonthPicOfPosition(int position){
        for(MegaMonthPicLollipop temp : monthPics){
            if(position < temp.nodeHandles.size() + 1){
                return temp;
            }
            position -= (temp.nodeHandles.size() + 1);
        }
        return null;
    }

    public int getSpanSizeOfPosition(int position){
        int type = TYPE_NO_TYPE;
        try{
            type = itemInformationList.get(position).type;
        } catch (Exception e){
            e.printStackTrace();
        }
        switch (type){
            case TYPE_ITEM_TITLE:{
                return numberOfCells;
            }
            case TYPE_ITEM_IMAGE:
            case TYPE_ITEM_VIDEO:{
                return 1;
            }
            case TYPE_NO_TYPE:
                log("Error, NO TYPE");
                break;
        }
        return 0;
    }
    
    private static void log(String log) {
        Util.log("MegaPhotoSyncGridTitleAdapterLollipop", log);
    }
}
