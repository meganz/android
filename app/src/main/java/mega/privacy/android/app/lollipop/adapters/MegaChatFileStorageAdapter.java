package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactFileListFragmentLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFileStorageFragment;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;


public class MegaChatFileStorageAdapter extends RecyclerView.Adapter<MegaChatFileStorageAdapter.ViewHolderBrowser> implements OnClickListener, View.OnLongClickListener{

    Context context;
    MegaApiAndroid megaApi;
    ActionBar aB;
    ArrayList<Bitmap> thumbimages;
    Object fragment;
    DisplayMetrics outMetrics;
    private SparseBooleanArray selectedItems;
    boolean multipleSelect;
    DatabaseHandler dbH = null;

    /* public static view holder class */
    public static class ViewHolderBrowser extends ViewHolder {

        public ViewHolderBrowser(View v) {
            super(v);
        }
        public RelativeLayout itemLayout;
    }

    public static class ViewHolderBrowserGrid extends ViewHolderBrowser {

        public ViewHolderBrowserGrid(View v){
            super(v);
        }
        public ImageView imageViewThumb;
        public RelativeLayout thumbLayout;
        public ImageView imageSelected;
        public RelativeLayout imageUnselected;
    }

    public void toggleSelection(int pos) {
        log("*********ADAPTER: toggleSelection-> "+pos);

        if (selectedItems.get(pos, false)) {
            log("delete pos: "+pos);
            selectedItems.delete(pos);
        }else {
            log("PUT pos: "+pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);

        if (selectedItems.size() <= 0){
            ((ChatFileStorageFragment) fragment).hideMultipleSelect();
        }
    }

    public void toggleAllSelection(int pos) {
        log("*********ADAPTER: toggleAllSelection-> "+pos);
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            log("delete pos: "+pos);
            selectedItems.delete(pos);
        }
        else {
            log("PUT pos: "+pos);
            selectedItems.put(pos, true);
        }
        log("adapter type is GRID");
        if (selectedItems.size() <= 0){
            ((ChatFileStorageFragment) fragment).hideMultipleSelect();
        }
        notifyItemChanged(positionToflip);
    }

    public void selectAll(){
        log("*********ADAPTER: selectAll");

        for (int i= 0; i<this.getItemCount();i++){
            if(!isItemChecked(i)){
                toggleAllSelection(i);
            }
        }
    }

    public void clearSelections() {
        log("*********ADAPTER: clearSelections");
        for (int i= 0; i<this.getItemCount();i++){
            if(isItemChecked(i)){
                toggleAllSelection(i);
            }
        }
    }

    private boolean isItemChecked(int position) {
        log("*********ADAPTER: isItemChecked-> "+position);

        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        log("*********ADAPTER: getSelectedItemCount");

        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        log("*********ADAPTER: getSelectedItems");

        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public List<Bitmap> getSelectedNodes() {
        log("*********ADAPTER: getSelectedNodes");

        ArrayList<Bitmap> nodes = new ArrayList<Bitmap>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {
                Bitmap document = getNodeAt(selectedItems.keyAt(i));
                if (document != null){
                    nodes.add(document);
                }
            }
        }
        return nodes;
    }


    public MegaChatFileStorageAdapter(Context _context, Object fragment, RecyclerView recyclerView, ActionBar aB, ArrayList<Bitmap> _thumbimages) {

        log("*********ADAPTER: MegaChatFileStorageAdapter");
        this.context = _context;
        this.fragment = fragment;
        this.thumbimages = _thumbimages;

        dbH = DatabaseHandler.getDbHandler(context);

        this.aB = aB;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication())
                    .getMegaApi();
        }
    }

    public void setNodes(ArrayList<Bitmap> thumbImages) {
        log("*********ADAPTER: setNodes");
        this.thumbimages = thumbImages;
        notifyDataSetChanged();
    }



    @Override
    public ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
        log("*********ADAPTER: onCreateViewHolder");
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_storage_grid, parent, false);
        ViewHolderBrowserGrid holderGrid = new ViewHolderBrowserGrid(v);

        holderGrid.itemLayout = (RelativeLayout) v.findViewById(R.id.file_storage_grid_item_layout);
        holderGrid.thumbLayout = (RelativeLayout) v.findViewById(R.id.file_storage_grid_thumbnail_layout);
        holderGrid.imageViewThumb = (ImageView) v.findViewById(R.id.file_storage_grid_thumbnail);
        holderGrid.imageSelected = (ImageView) v.findViewById(R.id.thumbnail_selected);
        holderGrid.imageUnselected = (RelativeLayout) v.findViewById(R.id.long_click_unselected);

        holderGrid.itemLayout.setTag(holderGrid);
        holderGrid.itemLayout.setOnClickListener(this);
        holderGrid.itemLayout.setOnLongClickListener(this);

        v.setTag(holderGrid);

        return holderGrid;
    }

    @Override
    public void onBindViewHolder(ViewHolderBrowser holder, int position) {
        log("*********ADAPTER: onBindViewHolder");
        ViewHolderBrowserGrid holderGrid = (ViewHolderBrowserGrid) holder;
        onBindViewHolderGrid(holderGrid, position);
    }

    public void onBindViewHolderGrid(ViewHolderBrowserGrid holder, int position){
        log("*********ADAPTER: onBindViewHolderGrid");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Bitmap image = (Bitmap) getItem(position);
        if(image == null){
            return;
        }

        holder.imageViewThumb.setVisibility(View.VISIBLE);

        holder.imageViewThumb.setImageBitmap(image);
        if (!multipleSelect) {
            holder.imageSelected.setVisibility(View.GONE);
            holder.imageUnselected.setVisibility(View.GONE);

        }else {

            if(this.isItemChecked(position)){
                holder.imageSelected.setVisibility(View.VISIBLE);
                holder.imageUnselected.setVisibility(View.GONE);

            }else{
                holder.imageSelected.setVisibility(View.GONE);
                holder.imageUnselected.setVisibility(View.VISIBLE);
            }
        }

    }

    private Bitmap getItemNode(int position) {
        log("*********ADAPTER: getItemNode");
        return thumbimages.get(position);
    }


    @Override
    public int getItemCount() {
        log("*********ADAPTER: getItemCount");

        if (thumbimages != null){
            return thumbimages.size();
        }else{
            return 0;
        }
    }

    public Object getItem(int position) {
        log("*********ADAPTER: getItem-> "+position);

        if (thumbimages != null){
            return thumbimages.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        log("*********ADAPTER: getItemCount-> "+position);

        return position;
    }


    public Bitmap getNodeAt(int position) {
        log("*********ADAPTER: getNodeAt-> "+position);

        try {
            if (thumbimages != null) {
                return thumbimages.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    private static void log(String log) {
        Util.log("MegaChatFileStorageAdapter", log);
    }

    @Override
    public void onClick(View v) {
        log("*********ADAPTER: onClick");
        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();
        int currentPosition = holder.getAdapterPosition();
        log("onClick -> Current position: "+currentPosition);

        if(currentPosition<0){
            log("Current position error - not valid value");
            return;
        }
        ((ChatFileStorageFragment) fragment).itemClick(currentPosition);

    }

    @Override
    public boolean onLongClick(View view) {
        log("*******ADAPTER: OnLongCLick");
        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        ViewHolderBrowser holder = (ViewHolderBrowser) view.getTag();
        int currentPosition = holder.getAdapterPosition();
        if (!isMultipleSelect()){
            setMultipleSelect(true);
        }
//        getSelectedNodes();
        ((ChatFileStorageFragment) fragment).itemClick(currentPosition);


        return true;
    }

    public boolean isMultipleSelect() {
        log("*********ADAPTER: isMultipleSelect");

        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        log("*********ADAPTER: setMultipleSelect-> "+multipleSelect);

        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if(this.multipleSelect){
            selectedItems = new SparseBooleanArray();
        }
    }

}
