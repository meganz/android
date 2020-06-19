package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
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

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.NewGridRecyclerView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.fragments.managerFragments.LinksFragment;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactFileListFragmentLollipop;
import mega.privacy.android.app.lollipop.ContactSharedFolderFragment;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;

import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.NodeTakenDownDialogHandler.*;

public class MegaNodeAdapter extends RecyclerView.Adapter<MegaNodeAdapter.ViewHolderBrowser> implements OnClickListener, View.OnLongClickListener, SectionTitleProvider, RotatableAdapter, nodeTakenDownDialogListener {

    public static final int ITEM_VIEW_TYPE_LIST = 0;
    public static final int ITEM_VIEW_TYPE_GRID = 1;

    private Context context;
    private MegaApiAndroid megaApi;

    private ArrayList<MegaNode> nodes;

    private Object fragment;
    private long parentHandle = -1;
    private DisplayMetrics outMetrics;

    private int placeholderCount;

    private SparseBooleanArray selectedItems;

    /** the flag to store the node position where still remained unhandled*/
    private int unHandledItem = -1;

    /** the dialog to show taken down message */
    private AlertDialog takenDownDialog;

    private RecyclerView listFragment;
    private DatabaseHandler dbH = null;
    private boolean multipleSelect;
    private int type = FILE_BROWSER_ADAPTER;
    private int adapterType;

    public static class ViewHolderBrowser extends RecyclerView.ViewHolder {

        private ViewHolderBrowser(View v) {
            super(v);
        }

        public ImageView savedOffline;
        public ImageView publicLinkImage;
        public ImageView takenDownImage;
        public TextView textViewFileName;
        public EmojiTextView textViewFileSize;
        public long document;
        public RelativeLayout itemLayout;
    }

    public static class ViewHolderBrowserList extends MegaNodeAdapter.ViewHolderBrowser {

        public ViewHolderBrowserList(View v) {
            super(v);
        }
        public ImageView imageView;
        public ImageView permissionsIcon;
        public ImageView versionsIcon;
        public RelativeLayout threeDotsLayout;
    }

    public static class ViewHolderBrowserGrid extends MegaNodeAdapter.ViewHolderBrowser {

        public ViewHolderBrowserGrid(View v) {
            super(v);
        }

        public ImageView imageViewThumb;
        public ImageView imageViewIcon;
        public RelativeLayout thumbLayout;
        public View separator;
        public ImageView imageViewVideoIcon;
        public TextView videoDuration;
        public RelativeLayout videoInfoLayout, bottomContainer;
        public ImageButton imageButtonThreeDots;

        public View folderLayout;
        public View fileLayout;
        public RelativeLayout thumbLayoutForFile;
        public ImageView fileGridIconForFile;
        public ImageButton imageButtonThreeDotsForFile;
        public TextView textViewFileNameForFile;
        public ImageView takenDownImageForFile;
        public ImageView fileGridSelected;
    }

    @Override
    public int getPlaceholderCount() {
        return placeholderCount;
    }

    @Override
    public int getUnhandledItem() {
        return unHandledItem;
    }

    public void toggleAllSelection(int pos) {
        logDebug("Position: " + pos);
        startAnimation(pos, putOrDeletePostion(pos));
    }

    public void toggleSelection(int pos) {
        logDebug("Position: " + pos);
        startAnimation(pos, putOrDeletePostion(pos));
    }

    boolean putOrDeletePostion(int pos) {
        if (selectedItems.get(pos,false)) {
            logDebug("Delete pos: " + pos);
            selectedItems.delete(pos);
            return true;
        } else {
            logDebug("PUT pos: " + pos);
            selectedItems.put(pos,true);
            return false;
        }
    }

    void startAnimation (final int pos, final boolean delete) {

        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            logDebug("Adapter type is LIST");
            ViewHolderBrowserList view = (ViewHolderBrowserList)listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                logDebug("Start animation: " + pos);
                Animation flipAnimation = AnimationUtils.loadAnimation(context,R.anim.multiselect_flip);
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (!delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        hideMultipleSelect();
                        if (delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.imageView.startAnimation(flipAnimation);
            }
            else {
                logDebug("View is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        } else {
            logDebug("Adapter type is GRID");
            MegaNode node = (MegaNode)getItem(pos);
            boolean isFile = false;
            if (node != null) {
                if (node.isFolder()) {
                    isFile = false;
                }
                else {
                    isFile = true;
                }
            }
            ViewHolderBrowserGrid view = (ViewHolderBrowserGrid)listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                logDebug("Start animation: " + pos);
                Animation flipAnimation = AnimationUtils.loadAnimation(context,R.anim.multiselect_flip);
                if (!delete && isFile) {
                    notifyItemChanged(pos);
                    flipAnimation.setDuration(250);
                }
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (!delete) {
                            notifyItemChanged(pos);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        hideMultipleSelect();
                        notifyItemChanged(pos);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                if (isFile) {
                    view.fileGridSelected.startAnimation(flipAnimation);
                }
                else {
                    view.imageViewIcon.startAnimation(flipAnimation);
                }
            }
            else {
                logDebug("View is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        }
    }

    void hideMultipleSelect () {
        if (selectedItems.size() <= 0) {
            if (type == RUBBISH_BIN_ADAPTER) {
                ((RubbishBinFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == INBOX_ADAPTER) {
                ((InboxFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == INCOMING_SHARES_ADAPTER) {
                ((IncomingSharesFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == OUTGOING_SHARES_ADAPTER) {
                ((OutgoingSharesFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragmentLollipop)fragment).hideMultipleSelect();
            } else if(type==CONTACT_SHARED_FOLDER_ADAPTER){
                ((ContactSharedFolderFragment) fragment).hideMultipleSelect();
            } else if (type == FOLDER_LINK_ADAPTER) {
                ((FolderLinkActivityLollipop)context).hideMultipleSelect();
            } else if (type == SEARCH_ADAPTER) {
                ((SearchFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == LINKS_ADAPTER) {
                ((LinksFragment) fragment).hideMultipleSelect();
            } else {
                ((FileBrowserFragmentLollipop)fragment).hideMultipleSelect();
            }
        }
    }

    public void selectAll() {
        for (int i = 0;i < nodes.size();i++) {
            if (!isItemChecked(i)) {
                //Exlude placeholder.
                if (nodes.get(i) != null) {
                    toggleAllSelection(i);
                }
            }
        }
    }

    public void clearSelections() {
        logDebug("clearSelections");
        if(nodes == null){
            return;
        }
        for (int i = 0;i < nodes.size();i++) {
            if (isItemChecked(i)) {
                //Exlude placeholder.
                if (nodes.get(i) != null) {
                    toggleAllSelection(i);
                }
            }
        }
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    @Override
    public List<Integer> getSelectedItems() {
        if (selectedItems != null) {
            List<Integer> items = new ArrayList<Integer>(selectedItems.size());
            for (int i = 0; i < selectedItems.size(); i++) {
                items.add(selectedItems.keyAt(i));
            }
            return items;
        } else {
            return null;
        }
    }

    /*
     * Get list of all selected nodes
     */
    public List<MegaNode> getSelectedNodes() {
        ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();

        for (int i = 0;i < selectedItems.size();i++) {
            if (selectedItems.valueAt(i) == true) {
                MegaNode document = getNodeAt(selectedItems.keyAt(i));
                if (document != null) {
                    nodes.add(document);
                }
            }
        }
        return nodes;
    }

    public ArrayList<MegaNode> getArrayListSelectedNodes() {
        ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();

        for (int i = 0;i < selectedItems.size();i++) {
            if (selectedItems.valueAt(i) == true) {
                MegaNode document = getNodeAt(selectedItems.keyAt(i));
                if (document != null) {
                    nodes.add(document);
                }
            }
        }
        return nodes;
    }

    /*
     * The method to return how many folders in this adapter
     */
    @Override
    public int getFolderCount() {
        return getNumberOfFolders(nodes);
    }

    /**
     * In grid view.
     * For folder count is odd. Insert null element as placeholder.
     *
     * @param nodes Origin nodes to show.
     * @return Nodes list with placeholder.
     */
    private ArrayList<MegaNode> insertPlaceHolderNode(ArrayList<MegaNode> nodes) {
        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            placeholderCount = 0;
            return nodes;
        }

        int folderCount = getNumberOfFolders(nodes);
        int spanCount = 2;

        if (listFragment instanceof NewGridRecyclerView) {
            spanCount = ((NewGridRecyclerView)listFragment).getSpanCount();
        }

        placeholderCount = (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);

        if (folderCount > 0 && placeholderCount != 0 && adapterType == ITEM_VIEW_TYPE_GRID) {
            //Add placeholder at folders' end.
            for (int i = 0;i < placeholderCount;i++) {
                try {
                    nodes.add(folderCount + i,null);
                } catch (IndexOutOfBoundsException e) {
                    logError("Inserting placeholders [nodes.size]: " + nodes.size() + " [folderCount+i]: " + (folderCount + i), e);
                }
            }
        }

        return nodes;
    }

    public MegaNodeAdapter(Context _context, Object fragment, ArrayList<MegaNode> _nodes, long _parentHandle, RecyclerView recyclerView, ActionBar aB, int type, int adapterType) {

        this.context = _context;
        this.nodes = _nodes;
        this.parentHandle = _parentHandle;
        this.type = type;
        this.adapterType = adapterType;
        this.fragment = fragment;

        dbH = DatabaseHandler.getDbHandler(context);

        switch (type) {
            case CONTACT_FILE_ADAPTER: {
                ((ContactFileListActivityLollipop)context).setParentHandle(parentHandle);
                break;
            }
            case FOLDER_LINK_ADAPTER: {
                megaApi = ((MegaApplication)((Activity)context).getApplication()).getMegaApiFolder();
                break;
            }
            case SEARCH_ADAPTER: {
                ((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
                break;
            }
            case INBOX_ADAPTER: {
                logDebug("onCreate INBOX_ADAPTER");
                ((ManagerActivityLollipop)context).setParentHandleInbox(parentHandle);
                break;
            }
            default: {
                break;
            }
        }

        this.listFragment = recyclerView;
        this.type = type;

        if (megaApi == null) {
            megaApi = ((MegaApplication)((Activity)context).getApplication())
                    .getMegaApi();
        }
    }

    public void setNodes(ArrayList<MegaNode> nodes) {
        this.nodes = insertPlaceHolderNode(nodes);
        logDebug("setNodes size: " + this.nodes.size());
        notifyDataSetChanged();
    }

    /**
     * Method to update an item when some contact information has changed.
     *
     * @param contactHandle Contact ID.
     */
    public void updateItem(long contactHandle) {
        for (MegaNode node : nodes) {
            if (node.isFolder()) {
                if (type == INCOMING_SHARES_ADAPTER || type == OUTGOING_SHARES_ADAPTER) {
                    ArrayList<MegaShare> shares = type == INCOMING_SHARES_ADAPTER ?
                            megaApi.getInSharesList() : megaApi.getOutShares(node);
                    if (shares != null && !shares.isEmpty()) {
                        for (MegaShare share : shares) {
                            MegaUser user = megaApi.getContact(share.getUser());
                            if (user != null && user.getHandle() == contactHandle) {
                                int position = nodes.indexOf(node);
                                notifyItemChanged(position);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    public int getAdapterType() {
        return adapterType;
    }

    public MegaNodeAdapter.ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");
        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (viewType == ITEM_VIEW_TYPE_LIST) {
            logDebug("type: ITEM_VIEW_TYPE_LIST");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
            ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
            holderList.itemLayout = v.findViewById(R.id.file_list_item_layout);
            holderList.imageView = v.findViewById(R.id.file_list_thumbnail);
            holderList.savedOffline = v.findViewById(R.id.file_list_saved_offline);

            holderList.publicLinkImage = v.findViewById(R.id.file_list_public_link);
            holderList.takenDownImage = v.findViewById(R.id.file_list_taken_down);
            holderList.permissionsIcon = v.findViewById(R.id.file_list_incoming_permissions);

            holderList.versionsIcon = v.findViewById(R.id.file_list_versions_icon);

            holderList.textViewFileName = v.findViewById(R.id.file_list_filename);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holderList.textViewFileName.setMaxWidth(scaleWidthPx(275, outMetrics));
            } else {
                holderList.textViewFileName.setMaxWidth(scaleWidthPx(190, outMetrics));
            }

            holderList.textViewFileSize = v.findViewById(R.id.file_list_filesize);
            if (isScreenInPortrait(context)) {
                holderList.textViewFileSize.setMaxWidthEmojis(px2dp(MAX_WIDTH_CONTACT_NAME_PORT, outMetrics));
            } else {
                holderList.textViewFileSize.setMaxWidthEmojis(px2dp(MAX_WIDTH_CONTACT_NAME_LAND, outMetrics));
            }

            holderList.threeDotsLayout = v.findViewById(R.id.file_list_three_dots_layout);

            holderList.savedOffline.setVisibility(View.INVISIBLE);

            holderList.publicLinkImage.setVisibility(View.INVISIBLE);

            holderList.takenDownImage.setVisibility(View.GONE);

            holderList.textViewFileSize.setVisibility(View.VISIBLE);

            holderList.itemLayout.setTag(holderList);
            holderList.itemLayout.setOnClickListener(this);
            holderList.itemLayout.setOnLongClickListener(this);

            holderList.threeDotsLayout.setTag(holderList);
            holderList.threeDotsLayout.setOnClickListener(this);

            v.setTag(holderList);
            return holderList;
        } else if (viewType == ITEM_VIEW_TYPE_GRID) {
            logDebug("type: ITEM_VIEW_TYPE_GRID");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_grid, parent, false);
            ViewHolderBrowserGrid holderGrid = new ViewHolderBrowserGrid(v);

            holderGrid.folderLayout = v.findViewById(R.id.item_file_grid_folder);
            holderGrid.fileLayout = v.findViewById(R.id.item_file_grid_file);
            holderGrid.itemLayout = v.findViewById(R.id.file_grid_item_layout);
            holderGrid.imageViewThumb = v.findViewById(R.id.file_grid_thumbnail);
            holderGrid.imageViewIcon = v.findViewById(R.id.file_grid_icon);
            holderGrid.fileGridIconForFile = v.findViewById(R.id.file_grid_icon_for_file);
            holderGrid.thumbLayout = v.findViewById(R.id.file_grid_thumbnail_layout);
            holderGrid.thumbLayoutForFile = v.findViewById(R.id.file_grid_thumbnail_layout_for_file);
            holderGrid.textViewFileName = v.findViewById(R.id.file_grid_filename);
            holderGrid.textViewFileNameForFile = v.findViewById(R.id.file_grid_filename_for_file);
            holderGrid.imageButtonThreeDotsForFile = v.findViewById(R.id.file_grid_three_dots_for_file);
            holderGrid.textViewFileSize = v.findViewById(R.id.file_grid_filesize);
            holderGrid.imageButtonThreeDots = v.findViewById(R.id.file_grid_three_dots);
            holderGrid.savedOffline = v.findViewById(R.id.file_grid_saved_offline);
            holderGrid.separator = v.findViewById(R.id.file_grid_separator);
            holderGrid.publicLinkImage = v.findViewById(R.id.file_grid_public_link);
            holderGrid.takenDownImage = v.findViewById(R.id.file_grid_taken_down);
            holderGrid.takenDownImageForFile = v.findViewById(R.id.file_grid_taken_down_for_file);
            holderGrid.imageViewVideoIcon = v.findViewById(R.id.file_grid_video_icon);
            holderGrid.videoDuration = v.findViewById(R.id.file_grid_title_video_duration);
            holderGrid.videoInfoLayout = v.findViewById(R.id.item_file_videoinfo_layout);
            holderGrid.fileGridSelected = v.findViewById(R.id.file_grid_selected);
            holderGrid.bottomContainer = v.findViewById(R.id.grid_bottom_container);
            holderGrid.bottomContainer.setTag(holderGrid);
            holderGrid.bottomContainer.setOnClickListener(this);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holderGrid.textViewFileSize.setMaxWidth(scaleWidthPx(70, outMetrics));
            } else {
                holderGrid.textViewFileSize.setMaxWidth(scaleWidthPx(130, outMetrics));
            }
            if (holderGrid.textViewFileSize != null) {
                holderGrid.textViewFileSize.setVisibility(View.VISIBLE);
            } else {
                logWarning("textViewMessageInfo is NULL");
            }

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holderGrid.textViewFileNameForFile.setMaxWidth(scaleWidthPx(70, outMetrics));
            } else {
                holderGrid.textViewFileNameForFile.setMaxWidth(scaleWidthPx(140, outMetrics));
            }


            holderGrid.savedOffline.setVisibility(View.INVISIBLE);
            holderGrid.publicLinkImage.setVisibility(View.GONE);
            holderGrid.takenDownImage.setVisibility(View.GONE);
            holderGrid.takenDownImageForFile.setVisibility(View.GONE);

            holderGrid.itemLayout.setTag(holderGrid);
            holderGrid.itemLayout.setOnClickListener(this);
            holderGrid.itemLayout.setOnLongClickListener(this);

            holderGrid.imageButtonThreeDots.setTag(holderGrid);
            holderGrid.imageButtonThreeDots.setOnClickListener(this);
            holderGrid.imageButtonThreeDotsForFile.setTag(holderGrid);
            holderGrid.imageButtonThreeDotsForFile.setOnClickListener(this);
            v.setTag(holderGrid);

            return holderGrid;
        } else {
            return null;
        }
    }

    public void onBindViewHolder(ViewHolderBrowser holder, int position) {
        logDebug("Position: " + position);

        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            ViewHolderBrowserList holderList = (ViewHolderBrowserList)holder;
            onBindViewHolderList(holderList,position);
        } else if (adapterType == ITEM_VIEW_TYPE_GRID) {
            ViewHolderBrowserGrid holderGrid = (ViewHolderBrowserGrid)holder;
            onBindViewHolderGrid(holderGrid,position);
        }
        reSelectUnhandledNode();
    }

    public void onBindViewHolderGrid(ViewHolderBrowserGrid holder,int position) {
        logDebug("Position: " + position);
        MegaNode node = (MegaNode)getItem(position);
        //Placeholder for folder when folder count is odd.
        if (node == null) {
            holder.folderLayout.setVisibility(View.INVISIBLE);
            holder.fileLayout.setVisibility(View.GONE);
            holder.itemLayout.setVisibility(View.INVISIBLE);
            return;
        }

        holder.document = node.getHandle();
        logDebug("Node : " + position + " " + node.getHandle());

        holder.textViewFileName.setText(node.getName());
        holder.textViewFileSize.setText("");
        holder.videoInfoLayout.setVisibility(View.GONE);

        if (node.isExported()) {
            //Node has public link
            holder.publicLinkImage.setVisibility(View.VISIBLE);
            if (node.isExpired()) {
                logWarning("Node exported but expired!!");
            }
        } else {
            holder.publicLinkImage.setVisibility(View.INVISIBLE);
        }

        if (node.isTakenDown()) {
            holder.textViewFileNameForFile.setTextColor(context.getResources().getColor(R.color.dark_primary_color));
            holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.dark_primary_color));
            holder.takenDownImage.setVisibility(View.VISIBLE);
            holder.takenDownImageForFile.setVisibility(View.VISIBLE);
        } else {
            holder.textViewFileNameForFile.setTextColor(context.getResources().getColor(R.color.black));
            holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.black));
            holder.takenDownImage.setVisibility(View.GONE);
            holder.takenDownImageForFile.setVisibility(View.GONE);
        }

        if (node.isFolder()) {
            holder.itemLayout.setVisibility(View.VISIBLE);
            holder.folderLayout.setVisibility(View.VISIBLE);
            holder.fileLayout.setVisibility(View.GONE);
            holder.textViewFileSize.setVisibility(View.VISIBLE);

            setFolderGridSelected(holder, position, getFolderIcon(node, type == OUTGOING_SHARES_ADAPTER ? ManagerActivityLollipop.DrawerItem.SHARED_ITEMS : ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE));

            if (type == FOLDER_LINK_ADAPTER) {
                holder.textViewFileSize.setText(getInfoFolder(node,context,megaApi));
            } else {
                holder.textViewFileSize.setText(getInfoFolder(node,context));
            }
            holder.imageViewIcon.setVisibility(View.VISIBLE);
            holder.imageViewThumb.setVisibility(View.GONE);
            holder.thumbLayout.setBackgroundColor(Color.TRANSPARENT);

            if (type == INCOMING_SHARES_ADAPTER) {
                //Show the owner of the shared folder
                ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                for (int j = 0;j < sharesIncoming.size();j++) {
                    MegaShare mS = sharesIncoming.get(j);
                    if (mS.getNodeHandle() == node.getHandle()) {
                        MegaUser user = megaApi.getContact(mS.getUser());
                        if (user != null) {
                            holder.textViewFileSize.setText(getMegaUserNameDB(user));
                        } else {
                            holder.textViewFileSize.setText(mS.getUser());
                        }
                    }
                }
            } else if (type == OUTGOING_SHARES_ADAPTER) {
                //Show the number of contacts who shared the folder if more than one contact and name of contact if that is not the case
                holder.textViewFileSize.setText(getOutgoingSubtitle(holder.textViewFileSize.getText().toString(), node));
            }
        } else if (node.isFile()) {
            //TODO file
            holder.itemLayout.setVisibility(View.VISIBLE);
            holder.folderLayout.setVisibility(View.GONE);
            holder.imageViewThumb.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            holder.imageViewThumb.setVisibility(View.GONE);
            holder.fileLayout.setVisibility(View.VISIBLE);
            holder.textViewFileName.setVisibility(View.VISIBLE);
            holder.textViewFileSize.setVisibility(View.GONE);

            holder.textViewFileNameForFile.setText(node.getName());
            long nodeSize = node.getSize();
            holder.textViewFileSize.setText(getSizeString(nodeSize));

            holder.fileGridIconForFile.setVisibility(View.VISIBLE);
            holder.fileGridIconForFile.setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());

            if (isVideoFile(node.getName())) {
                holder.videoInfoLayout.setVisibility(View.VISIBLE);
                holder.videoDuration.setVisibility(View.GONE);

                String duration = getVideoDuration(node.getDuration());
                if (duration != null && !duration.isEmpty()) {
                    holder.videoDuration.setText(duration);
                    holder.videoDuration.setVisibility(View.VISIBLE);
                }
            }

            if (node.hasThumbnail()) {
                Bitmap temp = ThumbnailUtilsLollipop.getThumbnailFromCache(node);

                if (temp != null) {
                    setImageThumbnail(holder, temp);
                }
                else {
                    temp = ThumbnailUtilsLollipop.getThumbnailFromFolder(node,context);

                    if (temp != null) {
                        setImageThumbnail(holder, temp);
                    }
                    else {
                        try {
                            temp = ThumbnailUtilsLollipop.getThumbnailFromMegaGrid(node,context,holder,megaApi,this);

                        } catch (Exception e) {} // Too many AsyncTasks

                        if (temp != null) {
                            setImageThumbnail(holder, temp);
                        }
                    }
                }
            }
            else {
                Bitmap temp = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
                if (temp != null) {
                    setImageThumbnail(holder, temp);
                }
                else {
                    temp = ThumbnailUtilsLollipop.getThumbnailFromFolder(node,context);

                    if (temp != null) {
                        setImageThumbnail(holder, temp);
                    }
                    else {
                        try {
                            ThumbnailUtilsLollipop.createThumbnailGrid(context,node,holder,megaApi,this);
                        } catch (Exception e) {} // Too many AsyncTasks
                    }
                }
            }

            if (isMultipleSelect() && isItemChecked(position)) {
                holder.itemLayout.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid_selected));
                holder.fileGridSelected.setImageResource(R.drawable.ic_select_folder);

            } else {
                holder.itemLayout.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid));
                holder.fileGridSelected.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }

        //Check if is an offline file to show the red arrow
        if (availableOffline(context, node)) {
            holder.savedOffline.setVisibility(View.VISIBLE);
        }
        else {
            holder.savedOffline.setVisibility(View.INVISIBLE);
        }
    }

    private void setImageThumbnail (ViewHolderBrowserGrid holder, Bitmap temp) {
        Bitmap thumb = ThumbnailUtilsLollipop.getRoundedRectBitmap(context,temp,2);
        holder.fileGridIconForFile.setVisibility(View.GONE);
        holder.imageViewThumb.setVisibility(View.VISIBLE);
        holder.imageViewThumb.setImageBitmap(thumb);
    }

    private void setFolderGridSelected(ViewHolderBrowserGrid holder, int position, int folderDrawableResId) {
        if (isMultipleSelect() && isItemChecked(position)) {
            RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams)holder.imageViewIcon.getLayoutParams();
            paramsMultiselect.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,24,context.getResources().getDisplayMetrics());
            paramsMultiselect.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,24,context.getResources().getDisplayMetrics());
            holder.imageViewIcon.setLayoutParams(paramsMultiselect);
            holder.itemLayout.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid_selected));
            holder.imageViewIcon.setImageResource(R.drawable.ic_select_folder);
        } else {
            holder.itemLayout.setBackground(ContextCompat.getDrawable(context,R.drawable.background_item_grid));
            holder.imageViewIcon.setImageResource(folderDrawableResId);
        }
    }

    private void setFolderListSelected (ViewHolderBrowserList holder, int position, int folderDrawableResId) {
        if (isMultipleSelect() && isItemChecked(position)) {
            RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
            paramsMultiselect.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
            paramsMultiselect.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
            paramsMultiselect.setMargins(0,0,0,0);
            holder.imageView.setLayoutParams(paramsMultiselect);
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context,R.color.new_multiselect_color));
            holder.imageView.setImageResource(R.drawable.ic_select_folder);
        }
        else {
            holder.itemLayout.setBackgroundColor(Color.WHITE);
            holder.imageView.setImageResource(folderDrawableResId);
        }
    }

    public void onBindViewHolderList(ViewHolderBrowserList holder,int position) {
        logDebug("Position: " + position);

        MegaNode node = (MegaNode)getItem(position);
        if (node == null) {
            return;
        }
        holder.document = node.getHandle();
        Bitmap thumb = null;

        holder.textViewFileName.setText(node.getName());
        holder.textViewFileSize.setText("");

        holder.publicLinkImage.setVisibility(View.INVISIBLE);
        holder.permissionsIcon.setVisibility(View.GONE);

        if (node.isExported() && type != LINKS_ADAPTER) {
            //Node has public link
            holder.publicLinkImage.setVisibility(View.VISIBLE);
            if (node.isExpired()) {
                logWarning("Node exported but expired!!");
            }
        } else {
            holder.publicLinkImage.setVisibility(View.INVISIBLE);
        }

        if (node.isTakenDown()) {
            holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.dark_primary_color));
            holder.takenDownImage.setVisibility(View.VISIBLE);
        } else {
            holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.black));
            holder.takenDownImage.setVisibility(View.GONE);
        }

        if (node.isFolder()) {

            logDebug("Node is folder");
            holder.itemLayout.setBackgroundColor(Color.WHITE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
            params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
            params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
            params.setMargins(0,0,0,0);
            holder.imageView.setLayoutParams(params);

            holder.textViewFileSize.setVisibility(View.VISIBLE);
            holder.textViewFileSize.setText(getInfoFolder(node,context));

            holder.versionsIcon.setVisibility(View.GONE);

            setFolderListSelected(holder, position, getFolderIcon(node, type == OUTGOING_SHARES_ADAPTER ? ManagerActivityLollipop.DrawerItem.SHARED_ITEMS : ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE));

            if (type == FOLDER_LINK_ADAPTER) {
                holder.textViewFileSize.setText(getInfoFolder(node,context,megaApi));
            } else if (type == CONTACT_FILE_ADAPTER|| type == CONTACT_SHARED_FOLDER_ADAPTER){
                boolean firstLevel;
                if(type == CONTACT_FILE_ADAPTER){
                    firstLevel = ((ContactFileListFragmentLollipop) fragment).isEmptyParentHandleStack();
                } else{
                    firstLevel = true;
                }

                if (firstLevel) {
                    int accessLevel = megaApi.getAccess(node);

                    if (accessLevel == MegaShare.ACCESS_FULL) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                    } else if (accessLevel == MegaShare.ACCESS_READWRITE) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
                    } else {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read);
                    }
                    holder.permissionsIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.permissionsIcon.setVisibility(View.GONE);
                }
            } else if (type == INCOMING_SHARES_ADAPTER) {
                holder.publicLinkImage.setVisibility(View.INVISIBLE);

                if (node.isTakenDown()) {
                    holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.dark_primary_color));
                    holder.takenDownImage.setVisibility(View.VISIBLE);
                } else {
                    holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.black));
                    holder.takenDownImage.setVisibility(View.GONE);
                }

                //Show the owner of the shared folder
                ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                for (int j = 0;j < sharesIncoming.size();j++) {
                    MegaShare mS = sharesIncoming.get(j);
                    if (mS.getNodeHandle() == node.getHandle()) {
                        MegaUser user = megaApi.getContact(mS.getUser());
                        if (user != null) {
                            holder.textViewFileSize.setText(getMegaUserNameDB(user));
                        } else {
                            holder.textViewFileSize.setText(mS.getUser());
                        }
                    }
                }

                if (((ManagerActivityLollipop) context).getDeepBrowserTreeIncoming() == 0) {
                    int accessLevel = megaApi.getAccess(node);

                    if (accessLevel == MegaShare.ACCESS_FULL) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                    } else if (accessLevel == MegaShare.ACCESS_READWRITE) {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
                    } else {
                        holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read);
                    }
                    holder.permissionsIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.permissionsIcon.setVisibility(View.GONE);
                }

            } else if (type == OUTGOING_SHARES_ADAPTER) {
                //Show the number of contacts who shared the folder if more than one contact and name of contact if that is not the case
                holder.textViewFileSize.setText(getOutgoingSubtitle(holder.textViewFileSize.getText().toString(), node));
            }
        } else {
            logDebug("Node is file");
            boolean isLinksRoot = type == LINKS_ADAPTER && ((ManagerActivityLollipop) context).getDeepBrowserTreeLinks() == 0;
            holder.textViewFileSize.setText(String.format("%s . %s", getSizeString(node.getSize()),
                    formatLongDateTime(isLinksRoot ? node.getPublicLinkCreationTime() : node.getModificationTime())));

            if(megaApi.hasVersions(node)){
                holder.versionsIcon.setVisibility(View.VISIBLE);
            }
            else{
                holder.versionsIcon.setVisibility(View.GONE);
            }

            if (!isMultipleSelect()) {
                logDebug("Not multiselect");
                holder.itemLayout.setBackgroundColor(Color.WHITE);
                holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
                params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
                params.setMargins(0,0,0,0);
                holder.imageView.setLayoutParams(params);

                logDebug("Check the thumb");

                if (node.hasThumbnail()) {
                    logDebug("Node has thumbnail");
                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                    params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                    params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                    int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                    params1.setMargins(left,0,0,0);

                    holder.imageView.setLayoutParams(params1);

                    thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
                    if (thumb != null) {

                        holder.imageView.setImageBitmap(thumb);

                    } else {
                        thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node,context);
                        if (thumb != null) {
                            holder.imageView.setImageBitmap(thumb);

                        } else {
                            try {
                                thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node,context,holder,megaApi,this);
                            } catch (Exception e) {
                            } // Too many AsyncTasks

                            if (thumb != null) {
                                holder.imageView.setImageBitmap(thumb);
                            }
                        }
                    }
                } else {
                    logDebug("Node NOT thumbnail");
                    thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
                    if (thumb != null) {
                        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                        params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                        params1.setMargins(left,0,0,0);

                        holder.imageView.setLayoutParams(params1);
                        holder.imageView.setImageBitmap(thumb);


                    } else {
                        thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node,context);
                        if (thumb != null) {
                            RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                            params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                            params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                            int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                            params1.setMargins(left,0,0,0);

                            holder.imageView.setLayoutParams(params1);
                            holder.imageView.setImageBitmap(thumb);

                        } else {
                            try {
                                ThumbnailUtilsLollipop.createThumbnailList(context,node,holder,megaApi,this);
                            } catch (Exception e) {
                            } // Too many AsyncTasks
                        }
                    }
                }
            } else {
                logDebug("Multiselection ON");
                if (this.isItemChecked(position)) {
                    holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context,R.color.new_multiselect_color));
                    RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                    paramsMultiselect.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
                    paramsMultiselect.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
                    paramsMultiselect.setMargins(0,0,0,0);
                    holder.imageView.setLayoutParams(paramsMultiselect);
                    holder.imageView.setImageResource(R.drawable.ic_select_folder);
                } else {
                    holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context,R.color.white));

                    logDebug("Check the thumb");

                    if (node.hasThumbnail()) {
                        logDebug("Node has thumbnail");
                        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                        params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                        params1.setMargins(left,0,0,0);

                        holder.imageView.setLayoutParams(params1);

                        thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
                        if (thumb != null) {

                            holder.imageView.setImageBitmap(thumb);

                        } else {
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node,context);
                            if (thumb != null) {
                                holder.imageView.setImageBitmap(thumb);

                            } else {
                                try {
                                    thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node,context,holder,megaApi,this);
                                } catch (Exception e) {
                                } // Too many AsyncTasks

                                if (thumb != null) {
                                    holder.imageView.setImageBitmap(thumb);
                                }
                            }
                        }
                    } else {
                        logDebug("Node NOT thumbnail");

                        thumb = ThumbnailUtilsLollipop.getThumbnailFromCache(node);
                        if (thumb != null) {
                            RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                            params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                            params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                            int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                            params1.setMargins(left,0,0,0);

                            holder.imageView.setLayoutParams(params1);
                            holder.imageView.setImageBitmap(thumb);


                        } else {
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromFolder(node,context);
                            if (thumb != null) {
                                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                                params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                                params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                                int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                                params1.setMargins(left,0,0,0);

                                holder.imageView.setLayoutParams(params1);
                                holder.imageView.setImageBitmap(thumb);

                            } else {
                                logDebug("NOT thumbnail");
                                holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                                try {
                                    ThumbnailUtilsLollipop.createThumbnailList(context,node,holder,megaApi,this);
                                } catch (Exception e) {
                                } // Too many AsyncTasks
                            }
                        }
                    }
                }
            }
        }

        //Check if is an offline file to show the red arrow
        if (availableOffline(context, node)) {
            holder.savedOffline.setVisibility(View.VISIBLE);
        }
        else {
            holder.savedOffline.setVisibility(View.INVISIBLE);
        }
    }

    private String getItemNode(int position) {
        if (nodes.get(position) != null) {
            return nodes.get(position).getName();
        }
        return null;
    }


    @Override
    public int getItemCount() {
        if (nodes != null) {
            return nodes.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return adapterType;
    }

    public Object getItem(int position) {
        if (nodes != null) {
            return nodes.get(position);
        }

        return null;
    }

    @Override
    public String getSectionTitle(int position) {
        if (getItemNode(position) != null && !getItemNode(position).equals("")) {
            return getItemNode(position).substring(0,1);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        ViewHolderBrowser holder = (ViewHolderBrowser)v.getTag();
        int currentPosition = holder.getAdapterPosition();

        logDebug("Current position: " + currentPosition);

        if (currentPosition < 0) {
            logError("Current position error - not valid value");
            return;
        }

        final MegaNode n = (MegaNode)getItem(currentPosition);
        if (n == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.grid_bottom_container:
            case R.id.file_list_three_dots_layout:
            case R.id.file_grid_three_dots:
            case R.id.file_grid_three_dots_for_file: {
                threeDotsClicked(currentPosition,n);
                break;
            }
            case R.id.file_list_item_layout:
            case R.id.file_grid_item_layout: {
                if (n.isTakenDown() && !isMultipleSelect()) {
                    takenDownDialog = showTakenDownDialog(n.isFolder(), v, currentPosition, this, context);
                    unHandledItem = currentPosition;
                } else {
                    fileClicked(currentPosition, v);
                }
                break;
            }
        }
    }

    public void filClicked(int currentPosition) {
        notifyItemChanged(currentPosition);
        unHandledItem = currentPosition;
    }

    private void fileClicked(int currentPosition, View view) {
        int[] screenPosition = new int[2];
        ImageView imageView;
        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            imageView = view.findViewById(R.id.file_list_thumbnail);
        } else {
            imageView = view.findViewById(R.id.file_grid_thumbnail);
        }
        imageView.getLocationOnScreen(screenPosition);

        int[] dimens = new int[4];
        dimens[0] = screenPosition[0];
        dimens[1] = screenPosition[1];
        dimens[2] = imageView.getWidth();
        dimens[3] = imageView.getHeight();
        if (type == RUBBISH_BIN_ADAPTER) {
            ((RubbishBinFragmentLollipop) fragment).itemClick(currentPosition, dimens, imageView);
        } else if (type == INBOX_ADAPTER) {
            ((InboxFragmentLollipop) fragment).itemClick(currentPosition, dimens, imageView);
        } else if (type == INCOMING_SHARES_ADAPTER) {
            ((IncomingSharesFragmentLollipop) fragment).itemClick(currentPosition, dimens, imageView);
        } else if (type == OUTGOING_SHARES_ADAPTER) {
            ((OutgoingSharesFragmentLollipop) fragment).itemClick(currentPosition, dimens, imageView);
        } else if (type == CONTACT_FILE_ADAPTER) {
            ((ContactFileListFragmentLollipop) fragment).itemClick(currentPosition, dimens, imageView);
        } else if (type == CONTACT_SHARED_FOLDER_ADAPTER) {
            ((ContactSharedFolderFragment) fragment).itemClick(currentPosition, dimens, imageView);
        } else if (type == FOLDER_LINK_ADAPTER) {
            ((FolderLinkActivityLollipop) context).itemClick(currentPosition, dimens, imageView);
        } else if (type == SEARCH_ADAPTER) {
            ((SearchFragmentLollipop) fragment).itemClick(currentPosition, dimens, imageView);
        } else if (type == LINKS_ADAPTER) {
            ((LinksFragment) fragment).itemClick(currentPosition, dimens, imageView);
        } else {
            logDebug("layout FileBrowserFragmentLollipop!");
            ((FileBrowserFragmentLollipop) fragment).itemClick(currentPosition, dimens, imageView);
        }
    }

    private void threeDotsClicked(int currentPosition,MegaNode n) {
        logDebug("onClick: file_list_three_dots: " + currentPosition);
        if (!isOnline(context)) {
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            } else if (context instanceof FolderLinkActivityLollipop) {
                ((FolderLinkActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
            } else if (context instanceof ContactFileListActivityLollipop) {
                ((ContactFileListActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
            }
            return;
        }

        if (isMultipleSelect()) {
            if (type == RUBBISH_BIN_ADAPTER) {
                ((RubbishBinFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == INBOX_ADAPTER) {
                ((InboxFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == INCOMING_SHARES_ADAPTER) {
                ((IncomingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == OUTGOING_SHARES_ADAPTER) {
                ((OutgoingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if(type==CONTACT_SHARED_FOLDER_ADAPTER){
                ((ContactSharedFolderFragment) fragment).itemClick(currentPosition,null,null);
            } else if (type == FOLDER_LINK_ADAPTER) {
                ((FolderLinkActivityLollipop)context).itemClick(currentPosition,null,null);
            } else if (type == SEARCH_ADAPTER) {
                ((SearchFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == LINKS_ADAPTER) {
                ((LinksFragment) fragment).itemClick(currentPosition, null, null);
            } else {
                logDebug("click layout FileBrowserFragmentLollipop!");
                ((FileBrowserFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            }
        } else {
            if (type == CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragmentLollipop)fragment).showOptionsPanel(n);
            } else if (type == FOLDER_LINK_ADAPTER) {
                ((FolderLinkActivityLollipop)context).showOptionsPanel(n);
            } else if(type==CONTACT_SHARED_FOLDER_ADAPTER){
                ((ContactSharedFolderFragment) fragment).showOptionsPanel(n);
            } else {
                ((ManagerActivityLollipop)context).showNodeOptionsPanel(n);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        logDebug("OnLongCLick");

        ViewHolderBrowser holder = (ViewHolderBrowser)view.getTag();
        int currentPosition = holder.getAdapterPosition();
        if (type == RUBBISH_BIN_ADAPTER) {
            ((RubbishBinFragmentLollipop)fragment).activateActionMode();
            ((RubbishBinFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == INBOX_ADAPTER) {
            ((InboxFragmentLollipop)fragment).activateActionMode();
            ((InboxFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == INCOMING_SHARES_ADAPTER) {
            ((IncomingSharesFragmentLollipop)fragment).activateActionMode();
            ((IncomingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if(type==CONTACT_SHARED_FOLDER_ADAPTER){
            ((ContactSharedFolderFragment) fragment).activateActionMode();
            ((ContactSharedFolderFragment) fragment).itemClick(currentPosition, null, null);
        } else if (type == OUTGOING_SHARES_ADAPTER) {
            ((OutgoingSharesFragmentLollipop)fragment).activateActionMode();
            ((OutgoingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == CONTACT_FILE_ADAPTER) {
            ((ContactFileListFragmentLollipop)fragment).activateActionMode();
            ((ContactFileListFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == FOLDER_LINK_ADAPTER) {
            logDebug("FOLDER_LINK_ADAPTER");
            ((FolderLinkActivityLollipop)context).activateActionMode();
            ((FolderLinkActivityLollipop)context).itemClick(currentPosition,null,null);
        } else if (type == SEARCH_ADAPTER) {
            if (((SearchFragmentLollipop)fragment).isAllowedMultiselect()) {
                ((SearchFragmentLollipop)fragment).activateActionMode();
                ((SearchFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            }
        } else if (type == LINKS_ADAPTER) {
            logDebug("FOLDER_LINK_ADAPTER");
            ((LinksFragment)fragment).activateActionMode();
            ((LinksFragment)fragment).itemClick(currentPosition,null,null);
        } else {
            logDebug("click layout FileBrowserFragmentLollipop!");
            ((FileBrowserFragmentLollipop)fragment).activateActionMode();
            ((FileBrowserFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        }

        return true;
    }

    /*
     * Get document at specified position
     */
    public MegaNode getNodeAt(int position) {
        try {
            if (nodes != null) {
                return nodes.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    public long getParentHandle() {
        return parentHandle;
    }

    public void setParentHandle(long parentHandle) {
        this.parentHandle = parentHandle;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        logDebug("multipleSelect: " + multipleSelect);
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        } else if (selectedItems != null) {
            selectedItems.clear();
        }
    }

    public void setListFragment (RecyclerView listFragment) {
        this.listFragment = listFragment;
    }

    /**
     * Gets the subtitle of a Outgoing item.
     * If it is shared with only one contact it should return the name or email of it.
     * If it is shared with more than one contact it should return the number of contacts.
     * If it is not a root outgoing folder it should return the content of the folder.
     *
     * @param currentSubtitle the current content of the folder (number of files and folders).
     * @param node outgoing folder.
     * @return the string to show in the subtitle of an outgoing item.
     */
    private String getOutgoingSubtitle(String currentSubtitle, MegaNode node) {
        String subtitle = currentSubtitle;

        ArrayList<MegaShare> sl = megaApi.getOutShares(node);
        if (sl != null && sl.size() != 0) {
            if (sl.size() == 1 && sl.get(0).getUser() != null) {
                subtitle = sl.get(0).getUser();
                MegaContactDB contactDB = dbH.findContactByEmail(subtitle);
                if (contactDB != null) {
                    String fullName = getContactNameDB(contactDB);
                    if (fullName != null) {
                        subtitle = fullName;
                    }
                }
            } else {
                subtitle = context.getResources().getString(R.string.file_properties_shared_folder_select_contact) + " " + sl.size() + " " + context.getResources().getQuantityString(R.plurals.general_num_users, sl.size());
            }
        }

        return subtitle;
    }

    /**
     * This is the method to click unhandled taken down dialog again,
     * after the recycler view finish binding adapter
     */
    private void reSelectUnhandledNode() {
        // if there is no un handled item
        if (unHandledItem == -1) {
            return;
        }

        listFragment.postDelayed(
                () -> {
                    if (takenDownDialog != null && takenDownDialog.isShowing()) {
                        return;
                    }

                    try {
                        listFragment.scrollToPosition(unHandledItem);
                        listFragment.findViewHolderForAdapterPosition(unHandledItem).itemView.performClick();
                    } catch (Exception ex) {
                        logError("Exception happens: " + ex.toString());
                    }
                }, 100
        );
    }

    /**
     * This is the method to clear existence dialog to prevent window leak,
     * after the rotation of the screen
     */
    public void clearTakenDownDialog() {
        if (takenDownDialog != null) {
            takenDownDialog.dismiss();
        }
    }

    @Override
    public void onOpenClicked(int currentPosition, View view) {
        unHandledItem = -1;
        fileClicked(currentPosition, view);
    }

    @Override
    public void onDisputeClicked() {
        unHandledItem = -1;
    }

    @Override
    public void onCancelClicked() {
        unHandledItem = -1;
    }
}