package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
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


public class MegaChatFileStorageAdapter extends RecyclerView.Adapter<MegaChatFileStorageAdapter.ViewHolderBrowser>{

    Context context;
    MegaApiAndroid megaApi;
    ActionBar aB;

    ArrayList<String> imagesPath;

    Object fragment;

    DisplayMetrics outMetrics;

    private SparseBooleanArray selectedItems;

    DatabaseHandler dbH = null;
    boolean multipleSelect;

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
        public View separator;
    }

    public MegaChatFileStorageAdapter(Context _context, Object fragment, ArrayList<String> _imagesPath, RecyclerView recyclerView, ActionBar aB) {

        log("MegaChatFileStorageAdapter");
        this.context = _context;
        this.imagesPath = _imagesPath;
        this.fragment = fragment;

        dbH = DatabaseHandler.getDbHandler(context);

        this.aB = aB;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication())
                    .getMegaApi();
        }
    }

    public void setNodes(ArrayList<String> imagesPath) {
        log("setNodes");
        this.imagesPath = imagesPath;
        notifyDataSetChanged();
    }



    @Override
    public ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_storage_grid, parent, false);
        ViewHolderBrowserGrid holderGrid = new ViewHolderBrowserGrid(v);

        holderGrid.itemLayout = (RelativeLayout) v.findViewById(R.id.file_storage_grid_item_layout);
        holderGrid.thumbLayout = (RelativeLayout) v.findViewById(R.id.file_storage_grid_thumbnail_layout);
        holderGrid.imageViewThumb = (ImageView) v.findViewById(R.id.file_storage_grid_thumbnail);
        holderGrid.separator = (View) v.findViewById(R.id.file_grid_separator);

        holderGrid.itemLayout.setTag(holderGrid);
        //holderGrid.itemLayout.setOnClickListener(this);
        // holderGrid.itemLayout.setOnLongClickListener(this);

        v.setTag(holderGrid);

        return holderGrid;
    }

    @Override
    public void onBindViewHolder(ViewHolderBrowser holder, int position) {
        log("onBindViewHolder");
        ViewHolderBrowserGrid holderGrid = (ViewHolderBrowserGrid) holder;
        onBindViewHolderGrid(holderGrid, position);
    }

    public void onBindViewHolderGrid(ViewHolderBrowserGrid holder, int position){
        log("onBindViewHolderGrid");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();

        String image = (String) getItem(position);
        if (image == null){
            return;
        }

        Bitmap thumb = null;
        holder.itemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.background_item_grid));
        holder.separator.setBackgroundColor(context.getResources().getColor(R.color.new_background_fragment));

        holder.imageViewThumb.setVisibility(View.VISIBLE);
    }

    private String getItemNode(int position) {
        return imagesPath.get(position);
    }


    @Override
    public int getItemCount() {
        if (imagesPath != null){
            return imagesPath.size();
        }else{
            return 0;
        }
    }

    public Object getItem(int position) {
        if (imagesPath != null){
            return imagesPath.get(position);
        }
        return null;
    }

    public String getNodeAt(int position) {
        try {
            if (imagesPath != null) {
                return imagesPath.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    private static void log(String log) {
        Util.log("MegaChatFileStorageAdapter", log);
    }

//    public Bitmap StringToBitMap(String encodedString){
//        try{
//            byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
//            Bitmap bitmap=BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
//            return bitmap;
//        }catch(Exception e){
//            e.getMessage();
//            return null;
//        }
//    }

}
