package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
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
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactFileListFragmentLollipop;
import mega.privacy.android.app.lollipop.ContactSharedFolderFragment;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;

public class MegaNodeAdapter extends RecyclerView.Adapter<MegaNodeAdapter.ViewHolderBrowser> implements OnClickListener, View.OnLongClickListener, SectionTitleProvider, RotatableAdapter {

    public static final int ITEM_VIEW_TYPE_LIST = 0;
    public static final int ITEM_VIEW_TYPE_GRID = 1;

    Context context;
    MegaApiAndroid megaApi;

    //	int positionClicked;
    ArrayList<MegaNode> nodes;

    Object fragment;
    long parentHandle = -1;
    DisplayMetrics outMetrics;

    private int placeholderCount;

    private SparseBooleanArray selectedItems;

    RecyclerView listFragment;
    //	ImageView emptyImageViewFragment;
//	TextView emptyTextViewFragment;
    boolean incoming = false;
    boolean inbox = false;
    DatabaseHandler dbH = null;
    boolean multipleSelect;
    int type = Constants.FILE_BROWSER_ADAPTER;
    int adapterType;

    public static class ViewHolderBrowser extends RecyclerView.ViewHolder {

        public ViewHolderBrowser(View v) {
            super(v);
        }

        public ImageView savedOffline;
        public ImageView publicLinkImage;
        public TextView textViewFileName;
        public TextView textViewFileSize;
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
        public ImageView fileGridSelected;
    }

    @Override
    public int getPlaceholderCount() {
        return placeholderCount;
    }

    public void toggleAllSelection(int pos) {
        LogUtil.logDebug("Position: " + pos);
        startAnimation(pos, putOrDeletePostion(pos));
    }

    public void toggleSelection(int pos) {
        LogUtil.logDebug("Position: " + pos);
        startAnimation(pos, putOrDeletePostion(pos));
    }

    boolean putOrDeletePostion(int pos) {
        if (selectedItems.get(pos,false)) {
            LogUtil.logDebug("Delete pos: " + pos);
            selectedItems.delete(pos);
            return true;
        } else {
            LogUtil.logDebug("PUT pos: " + pos);
            selectedItems.put(pos,true);
            return false;
        }
    }

    void startAnimation (final int pos, final boolean delete) {

        if (adapterType == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
            LogUtil.logDebug("Adapter type is LIST");
            MegaNodeAdapter.ViewHolderBrowserList view = (MegaNodeAdapter.ViewHolderBrowserList)listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                LogUtil.logDebug("Start animation: " + pos);
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
                LogUtil.logDebug("View is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        } else {
            LogUtil.logDebug("Adapter type is GRID");
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
            MegaNodeAdapter.ViewHolderBrowserGrid view = (MegaNodeAdapter.ViewHolderBrowserGrid)listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                LogUtil.logDebug("Start animation: " + pos);
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
                LogUtil.logDebug("View is null - not animation");
                hideMultipleSelect();
                notifyItemChanged(pos);
            }
        }
    }

    void hideMultipleSelect () {
        if (selectedItems.size() <= 0) {
            if (type == Constants.RUBBISH_BIN_ADAPTER) {
                ((RubbishBinFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == Constants.INBOX_ADAPTER) {
                ((InboxFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == Constants.INCOMING_SHARES_ADAPTER) {
                ((IncomingSharesFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == Constants.OUTGOING_SHARES_ADAPTER) {
                ((OutgoingSharesFragmentLollipop)fragment).hideMultipleSelect();
            } else if (type == Constants.CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragmentLollipop)fragment).hideMultipleSelect();
            } else if(type==Constants.CONTACT_SHARED_FOLDER_ADAPTER){
                ((ContactSharedFolderFragment) fragment).hideMultipleSelect();
            } else if (type == Constants.FOLDER_LINK_ADAPTER) {
                ((FolderLinkActivityLollipop)context).hideMultipleSelect();
            } else if (type == Constants.SEARCH_ADAPTER) {
                ((SearchFragmentLollipop)fragment).hideMultipleSelect();
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
        LogUtil.logDebug("clearSelections");
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
        return getFolderCount(nodes);
    }

    /*
     * The method to calculate how many nodes are folders in array list
     */
    public int getFolderCount(ArrayList<MegaNode> nodes) {
        int folderCount = 0;
        if (nodes == null) return folderCount;
        for (MegaNode node : nodes) {
            if (node == null) {
                continue;
            }
            if (node.isFolder()) {
                folderCount++;
            }
        }
        return folderCount;
    }

    /**
     * In grid view.
     * For folder count is odd. Insert null element as placeholder.
     *
     * @param nodes Origin nodes to show.
     * @return Nodes list with placeholder.
     */
    private ArrayList<MegaNode> insertPlaceHolderNode(ArrayList<MegaNode> nodes) {
        int folderCount = getFolderCount(nodes);
        int spanCount = 2;
        if (listFragment instanceof NewGridRecyclerView) {
            spanCount = ((NewGridRecyclerView)listFragment).getSpanCount();
        }
        placeholderCount = (folderCount % spanCount) == 0 ? 0 : spanCount - (folderCount % spanCount);

        if (folderCount > 0 && placeholderCount != 0 && adapterType == ITEM_VIEW_TYPE_GRID) {
            //Add placeholder at folders' end.
            for (int i = 0;i < placeholderCount;i++) {
                nodes.add(folderCount + i,null);
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
            case Constants.FILE_BROWSER_ADAPTER: {
//				((ManagerActivityLollipop) context).setParentHandleBrowser(parentHandle);
                break;
            }
            case Constants.CONTACT_FILE_ADAPTER: {
                ((ContactFileListActivityLollipop)context).setParentHandle(parentHandle);
                break;
            }
            case Constants.RUBBISH_BIN_ADAPTER: {
//				((ManagerActivityLollipop) context).setParentHandleRubbish(parentHandle);
                break;
            }
            case Constants.FOLDER_LINK_ADAPTER: {
                megaApi = ((MegaApplication)((Activity)context).getApplication()).getMegaApiFolder();
                break;
            }
            case Constants.SEARCH_ADAPTER: {
                ((ManagerActivityLollipop)context).setParentHandleSearch(parentHandle);
                break;
            }
            case Constants.OUTGOING_SHARES_ADAPTER: {
//				((ManagerActivityLollipop) context).setParentHandleOutgoing(parentHandle);
                break;
            }
            case Constants.INCOMING_SHARES_ADAPTER: {
                incoming = true;
//				((ManagerActivityLollipop) context).setParentHandleIncoming(parentHandle);
                break;
            }
            case Constants.INBOX_ADAPTER: {
                LogUtil.logDebug("onCreate INBOX_ADAPTER");
                inbox = true;
                ((ManagerActivityLollipop)context).setParentHandleInbox(parentHandle);
                break;
            }
            default: {
                //			((ManagerActivityLollipop) context).setParentHandleCloud(parentHandle);
                break;
            }
        }

        this.listFragment = recyclerView;
//		this.emptyImageViewFragment = emptyImageView;
//		this.emptyTextViewFragment = emptyTextView;
        this.type = type;

        if (megaApi == null) {
            megaApi = ((MegaApplication)((Activity)context).getApplication())
                    .getMegaApi();
        }
    }

    public void setNodes(ArrayList<MegaNode> nodes) {
        this.nodes = insertPlaceHolderNode(nodes);
        LogUtil.logDebug("setNodes size: " + this.nodes.size());
//		contentTextFragment.setText(getInfoFolder(node));
        notifyDataSetChanged();
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    public int getAdapterType() {
        return adapterType;
    }

    public MegaNodeAdapter.ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
        LogUtil.logDebug("onCreateViewHolder");
        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (viewType == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
            LogUtil.logDebug("type: ITEM_VIEW_TYPE_LIST");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list,parent,false);
            ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
            holderList.itemLayout = (RelativeLayout)v.findViewById(R.id.file_list_item_layout);
            holderList.imageView = (ImageView)v.findViewById(R.id.file_list_thumbnail);
            holderList.savedOffline = (ImageView)v.findViewById(R.id.file_list_saved_offline);

            holderList.publicLinkImage = (ImageView)v.findViewById(R.id.file_list_public_link);
            holderList.permissionsIcon = (ImageView)v.findViewById(R.id.file_list_incoming_permissions);

            holderList.versionsIcon = (ImageView) v.findViewById(R.id.file_list_versions_icon);

            holderList.textViewFileName = (TextView)v.findViewById(R.id.file_list_filename);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holderList.textViewFileName.setMaxWidth(Util.scaleWidthPx(275,outMetrics));
            } else {
                holderList.textViewFileName.setMaxWidth(Util.scaleWidthPx(210,outMetrics));
            }

            holderList.textViewFileSize = (TextView)v.findViewById(R.id.file_list_filesize);

            holderList.threeDotsLayout = (RelativeLayout)v.findViewById(R.id.file_list_three_dots_layout);

            holderList.savedOffline.setVisibility(View.INVISIBLE);

            holderList.publicLinkImage.setVisibility(View.INVISIBLE);

            holderList.textViewFileSize.setVisibility(View.VISIBLE);

            holderList.itemLayout.setTag(holderList);
            holderList.itemLayout.setOnClickListener(this);
            holderList.itemLayout.setOnLongClickListener(this);

            holderList.threeDotsLayout.setTag(holderList);
            holderList.threeDotsLayout.setOnClickListener(this);

            v.setTag(holderList);
            return holderList;
        } else if (viewType == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
            LogUtil.logDebug("type: ITEM_VIEW_TYPE_GRID");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_grid_new,parent,false);
            MegaNodeAdapter.ViewHolderBrowserGrid holderGrid = new MegaNodeAdapter.ViewHolderBrowserGrid(v);

            holderGrid.folderLayout = v.findViewById(R.id.item_file_grid_folder);
            holderGrid.fileLayout = v.findViewById(R.id.item_file_grid_file);
            holderGrid.itemLayout = (RelativeLayout)v.findViewById(R.id.file_grid_item_layout);
            holderGrid.imageViewThumb = (ImageView)v.findViewById(R.id.file_grid_thumbnail);
            holderGrid.imageViewIcon = (ImageView)v.findViewById(R.id.file_grid_icon);
            holderGrid.fileGridIconForFile = (ImageView)v.findViewById(R.id.file_grid_icon_for_file);
            holderGrid.thumbLayout = (RelativeLayout)v.findViewById(R.id.file_grid_thumbnail_layout);
            holderGrid.thumbLayoutForFile = (RelativeLayout)v.findViewById(R.id.file_grid_thumbnail_layout_for_file);
            holderGrid.textViewFileName = (TextView)v.findViewById(R.id.file_grid_filename);
            holderGrid.textViewFileNameForFile = (TextView)v.findViewById(R.id.file_grid_filename_for_file);
            holderGrid.imageButtonThreeDotsForFile = (ImageButton)v.findViewById(R.id.file_grid_three_dots_for_file);
            holderGrid.textViewFileSize = (TextView)v.findViewById(R.id.file_grid_filesize);
            holderGrid.imageButtonThreeDots = (ImageButton)v.findViewById(R.id.file_grid_three_dots);
            holderGrid.savedOffline = (ImageView)v.findViewById(R.id.file_grid_saved_offline);
            holderGrid.separator = (View)v.findViewById(R.id.file_grid_separator);
            holderGrid.publicLinkImage = (ImageView)v.findViewById(R.id.file_grid_public_link);
            holderGrid.imageViewVideoIcon = (ImageView)v.findViewById(R.id.file_grid_video_icon);
            holderGrid.videoDuration = (TextView)v.findViewById(R.id.file_grid_title_video_duration);
            holderGrid.videoInfoLayout = (RelativeLayout)v.findViewById(R.id.item_file_videoinfo_layout);
            holderGrid.fileGridSelected = (ImageView)v.findViewById(R.id.file_grid_selected);
            holderGrid.bottomContainer = v.findViewById(R.id.grid_bottom_container);
            holderGrid.bottomContainer.setTag(holderGrid);
            holderGrid.bottomContainer.setOnClickListener(this);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                holderGrid.textViewFileSize.setMaxWidth(Util.scaleWidthPx(70,outMetrics));
            } else {
                holderGrid.textViewFileSize.setMaxWidth(Util.scaleWidthPx(130,outMetrics));
            }
            if (holderGrid.textViewFileSize != null) {
                holderGrid.textViewFileSize.setVisibility(View.VISIBLE);
            } else {
                LogUtil.logWarning("textViewMessageInfo is NULL");
            }

            holderGrid.savedOffline.setVisibility(View.INVISIBLE);
            holderGrid.publicLinkImage.setVisibility(View.GONE);

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

    public void onBindViewHolder(MegaNodeAdapter.ViewHolderBrowser holder, int position) {
        LogUtil.logDebug("Position: " + position);

        if (adapterType == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
            MegaNodeAdapter.ViewHolderBrowserList holderList = (MegaNodeAdapter.ViewHolderBrowserList)holder;
            onBindViewHolderList(holderList,position);
        } else if (adapterType == MegaNodeAdapter.ITEM_VIEW_TYPE_GRID) {
            MegaNodeAdapter.ViewHolderBrowserGrid holderGrid = (MegaNodeAdapter.ViewHolderBrowserGrid)holder;
            onBindViewHolderGrid(holderGrid,position);
        }
    }

    public void onBindViewHolderGrid(ViewHolderBrowserGrid holder,int position) {
        LogUtil.logDebug("Position: " + position);
        MegaNode node = (MegaNode)getItem(position);
        //Placeholder for folder when folder count is odd.
        if (node == null) {
            holder.folderLayout.setVisibility(View.INVISIBLE);
            holder.fileLayout.setVisibility(View.GONE);
            holder.itemLayout.setVisibility(View.INVISIBLE);
            return;
        }

        holder.document = node.getHandle();
        LogUtil.logDebug("Node : " + position + " " + node.getHandle());

        holder.textViewFileName.setText(node.getName());
        holder.textViewFileSize.setText("");
        holder.videoInfoLayout.setVisibility(View.GONE);

        if (node.isExported()) {
            //Node has public link
            holder.publicLinkImage.setVisibility(View.VISIBLE);
            if (node.isExpired()) {
                LogUtil.logWarning("Node exported but expired!!");
            }
        } else {
            holder.publicLinkImage.setVisibility(View.INVISIBLE);
        }

        if (node.isFolder()) {
            holder.itemLayout.setVisibility(View.VISIBLE);
            holder.folderLayout.setVisibility(View.VISIBLE);
            holder.fileLayout.setVisibility(View.GONE);
            holder.textViewFileSize.setVisibility(View.VISIBLE);

            if (type == Constants.FOLDER_LINK_ADAPTER) {
                holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node,context,megaApi));
                setFolderGridSelected(holder,position,R.drawable.ic_folder_list);
            } else {
                holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node,context));
            }
            holder.imageViewIcon.setVisibility(View.VISIBLE);
            holder.imageViewThumb.setVisibility(View.GONE);
            holder.thumbLayout.setBackgroundColor(Color.TRANSPARENT);

            if (type == Constants.INCOMING_SHARES_ADAPTER) {
                if (node.isInShare()) {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder_incoming);
                }
                else {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder);
                }
                //Show the owner of the shared folder
                ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                for (int j = 0;j < sharesIncoming.size();j++) {
                    MegaShare mS = sharesIncoming.get(j);
                    if (mS.getNodeHandle() == node.getHandle()) {
                        MegaUser user = megaApi.getContact(mS.getUser());
                        if (user != null) {
                            MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
                            if (contactDB != null) {
                                if (!contactDB.getName().equals("")) {
                                    holder.textViewFileSize.setText(contactDB.getName() + " " + contactDB.getLastName());
                                } else {
                                    holder.textViewFileSize.setText(user.getEmail());
                                }
                            } else {
                                LogUtil.logWarning("The contactDB is null: ");
                                holder.textViewFileSize.setText(user.getEmail());
                            }
                        } else {
                            holder.textViewFileSize.setText(mS.getUser());
                        }
                    }
                }
            } else if (type == Constants.OUTGOING_SHARES_ADAPTER) {
                if (node.isOutShare() || megaApi.isPendingShare(node)) {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder_outgoing);
                }
                else {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder);
                }
                //Show the number of contacts who shared the folder
                ArrayList<MegaShare> sl = megaApi.getOutShares(node);
                if (sl != null) {
                    if (sl.size() != 0) {
                        holder.textViewFileSize.setText(context.getResources().getString(R.string.file_properties_shared_folder_select_contact) + " " + sl.size() + " " + context.getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
                    }
                }
            } else if (type == Constants.FILE_BROWSER_ADAPTER) {
                if (node.isOutShare() || megaApi.isPendingShare(node)) {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder_outgoing);
                } else if (node.isInShare()) {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder_incoming);
                } else {
                    if (((ManagerActivityLollipop)context).isCameraUploads(node)) {
                        setFolderGridSelected(holder,position,R.drawable.ic_folder_image);
                    } else {
                        setFolderGridSelected(holder,position,R.drawable.ic_folder);
                    }
                }
            } else {
                if (node.isOutShare() || megaApi.isPendingShare(node)) {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder_outgoing);
                } else if (node.isInShare()) {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder_incoming);
                } else {
                    setFolderGridSelected(holder,position,R.drawable.ic_folder);
                }
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
            holder.textViewFileSize.setText(Util.getSizeString(nodeSize));

            holder.fileGridIconForFile.setVisibility(View.VISIBLE);
            holder.fileGridIconForFile.setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());

            if (isVideoFile(node.getName())) {
                holder.videoInfoLayout.setVisibility(View.VISIBLE);
                holder.videoDuration.setVisibility(View.GONE);
                LogUtil.logDebug(node.getHandle() + " DURATION: " + node.getDuration());
                int duration = node.getDuration();
                if (duration > 0) {
                    int hours = duration / 3600;
                    int minutes = (duration % 3600) / 60;
                    int seconds = duration % 60;

                    String timeString;
                    if (hours > 0) {
                        timeString = String.format("%d:%d:%02d",hours,minutes,seconds);
                    } else {
                        timeString = String.format("%d:%02d",minutes,seconds);
                    }

                    LogUtil.logDebug("The duration is: " + hours + " " + minutes + " " + seconds);

                    holder.videoDuration.setText(timeString);
                    holder.videoDuration.setVisibility(View.VISIBLE);
                }
            }

            if (node.hasThumbnail()) {
                Bitmap temp = ThumbnailUtils.getThumbnailFromCache(node);

                if (temp != null) {
                    setImageThumbnail(holder, temp);
                }
                else {
                    temp = ThumbnailUtils.getThumbnailFromFolder(node,context);

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
                Bitmap temp = ThumbnailUtils.getThumbnailFromCache(node);
                if (temp != null) {
                    setImageThumbnail(holder, temp);
                }
                else {
                    temp = ThumbnailUtils.getThumbnailFromFolder(node,context);

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
        LogUtil.logDebug("Position: " + position);

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

        if (node.isExported()) {
            //Node has public link
            holder.publicLinkImage.setVisibility(View.VISIBLE);
            if (node.isExpired()) {
                LogUtil.logWarning("Node exported but expired!!");
            }
        } else {
            holder.publicLinkImage.setVisibility(View.INVISIBLE);
        }

        if (node.isFolder()) {

            LogUtil.logDebug("Node is folder");
            holder.itemLayout.setBackgroundColor(Color.WHITE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
            params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
            params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
            params.setMargins(0,0,0,0);
            holder.imageView.setLayoutParams(params);

            holder.textViewFileSize.setVisibility(View.VISIBLE);
//			holder.propertiesText.setText(R.string.general_folder_info);
            holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node,context));

            holder.versionsIcon.setVisibility(View.GONE);

            if (type == Constants.FOLDER_LINK_ADAPTER) {
                holder.textViewFileSize.setText(MegaApiUtils.getInfoFolder(node,context,megaApi));
                setFolderListSelected(holder, position, R.drawable.ic_folder_list);
            } else if (type == Constants.CONTACT_FILE_ADAPTER|| type == Constants.CONTACT_SHARED_FOLDER_ADAPTER){
                setFolderListSelected(holder, position, R.drawable.ic_folder_incoming_list);

                boolean firstLevel;
                if(type == Constants.CONTACT_FILE_ADAPTER){
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
            } else if (type == Constants.INCOMING_SHARES_ADAPTER) {
                holder.publicLinkImage.setVisibility(View.INVISIBLE);

                if (node.isInShare()) {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_incoming_list);
                }
                else {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_list);
                }

                //Show the owner of the shared folder
                ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                for (int j = 0;j < sharesIncoming.size();j++) {
                    MegaShare mS = sharesIncoming.get(j);
                    if (mS.getNodeHandle() == node.getHandle()) {
                        MegaUser user = megaApi.getContact(mS.getUser());
                        if (user != null) {
                            MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
                            if (contactDB != null) {
                                if (!contactDB.getName().equals("")) {
                                    holder.textViewFileSize.setText(contactDB.getName() + " " + contactDB.getLastName());
                                } else {
                                    holder.textViewFileSize.setText(user.getEmail());
                                }
                            } else {
                                LogUtil.logWarning("The contactDB is null: ");
                                holder.textViewFileSize.setText(user.getEmail());
                            }
                        } else {
                            holder.textViewFileSize.setText(mS.getUser());
                        }
                    }
                }

                int dBT = ((IncomingSharesFragmentLollipop)fragment).getDeepBrowserTree();

                if (dBT == 0) {
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

            } else if (type == Constants.OUTGOING_SHARES_ADAPTER) {
                if (node.isOutShare() || megaApi.isPendingShare(node)) {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_outgoing_list);
                }
                else {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_list);
                }
                //Show the number of contacts who shared the folder
                ArrayList<MegaShare> sl = megaApi.getOutShares(node);
                if (sl != null) {
                    if (sl.size() != 0) {
                        holder.textViewFileSize.setText(context.getResources().getString(R.string.file_properties_shared_folder_select_contact) + " " + sl.size() + " " + context.getResources().getQuantityString(R.plurals.general_num_users,sl.size()));
                    }
                }
            } else if (type == Constants.FILE_BROWSER_ADAPTER) {
                if (node.isOutShare() || megaApi.isPendingShare(node)) {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_outgoing_list);
                }
                else if (node.isInShare()) {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_incoming_list);
                }
                else {
                    if (((ManagerActivityLollipop) context).isCameraUploads(node)) {
                        setFolderListSelected(holder, position, R.drawable.ic_folder_image_list);
                    }
                    else {
                        setFolderListSelected(holder, position, R.drawable.ic_folder_list);
                    }
                }

            } else {
                if (node.isOutShare() || megaApi.isPendingShare(node)) {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_outgoing_list);
                }
                else if (node.isInShare()) {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_incoming_list);
                }
                else {
                    setFolderListSelected(holder, position, R.drawable.ic_folder_list);
                }
            }
        } else {
            LogUtil.logDebug("Node is file");
//			holder.propertiesText.setText(R.string.general_file_info);
            long nodeSize = node.getSize();
            holder.textViewFileSize.setText(Util.getSizeString(nodeSize));

            if(megaApi.hasVersions(node)){
                holder.versionsIcon.setVisibility(View.VISIBLE);
            }
            else{
                holder.versionsIcon.setVisibility(View.GONE);
            }

            if (!isMultipleSelect()) {
                LogUtil.logDebug("Not multiselect");
                holder.itemLayout.setBackgroundColor(Color.WHITE);
                holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
                params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,48,context.getResources().getDisplayMetrics());
                params.setMargins(0,0,0,0);
                holder.imageView.setLayoutParams(params);

                LogUtil.logDebug("Check the thumb");

                if (node.hasThumbnail()) {
                    LogUtil.logDebug("Node has thumbnail");
                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                    params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                    params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                    int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                    params1.setMargins(left,0,0,0);

                    holder.imageView.setLayoutParams(params1);

                    thumb = ThumbnailUtils.getThumbnailFromCache(node);
                    if (thumb != null) {

                        holder.imageView.setImageBitmap(thumb);

                    } else {
                        thumb = ThumbnailUtils
                                .getThumbnailFromFolder(node,context);
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
                    LogUtil.logDebug("Node NOT thumbnail");
                    thumb = ThumbnailUtils.getThumbnailFromCache(node);
                    if (thumb != null) {
                        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                        params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                        params1.setMargins(left,0,0,0);

                        holder.imageView.setLayoutParams(params1);
                        holder.imageView.setImageBitmap(thumb);


                    } else {
                        thumb = ThumbnailUtils.getThumbnailFromFolder(node,context);
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
                LogUtil.logDebug("Multiselection ON");
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

                    LogUtil.logDebug("Check the thumb");

                    if (node.hasThumbnail()) {
                        LogUtil.logDebug("Node has thumbnail");
                        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                        params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                        int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                        params1.setMargins(left,0,0,0);

                        holder.imageView.setLayoutParams(params1);

                        thumb = ThumbnailUtils.getThumbnailFromCache(node);
                        if (thumb != null) {

                            holder.imageView.setImageBitmap(thumb);

                        } else {
                            thumb = ThumbnailUtils
                                    .getThumbnailFromFolder(node,context);
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
                        LogUtil.logDebug("Node NOT thumbnail");

                        thumb = ThumbnailUtils.getThumbnailFromCache(node);
                        if (thumb != null) {
                            RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                            params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                            params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                            int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                            params1.setMargins(left,0,0,0);

                            holder.imageView.setLayoutParams(params1);
                            holder.imageView.setImageBitmap(thumb);


                        } else {
                            thumb = ThumbnailUtils.getThumbnailFromFolder(node,context);
                            if (thumb != null) {
                                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)holder.imageView.getLayoutParams();
                                params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                                params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
                                int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
                                params1.setMargins(left,0,0,0);

                                holder.imageView.setLayoutParams(params1);
                                holder.imageView.setImageBitmap(thumb);

                            } else {
                                LogUtil.logDebug("NOT thumbnail");
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

//	public boolean isEnabled(int position) {
//		// if (position == 0){
//		// return false;
//		// }
//		// else{
//		// return true;
//		// }
//		return super.isEnabled(position);
//	}


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
        LogUtil.logDebug("onClick");

        ViewHolderBrowser holder = (ViewHolderBrowser)v.getTag();
        int currentPosition = holder.getAdapterPosition();

        LogUtil.logDebug("Current position: " + currentPosition);

        if (currentPosition < 0) {
            LogUtil.logError("Current position error - not valid value");
            return;
        }

        final MegaNode n = (MegaNode)getItem(currentPosition);
        if (n == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.grid_bottom_container:
            case R.id.file_list_three_dots_layout:
            case R.id.file_grid_three_dots: {
                threeDotsClicked(currentPosition,n);
                break;
            }
            case R.id.file_grid_three_dots_for_file: {
                threeDotsClicked(currentPosition,n);
                break;
            }
            case R.id.file_list_item_layout:
            case R.id.file_grid_item_layout: {
                int[] screenPosition = new int[2];
                ImageView imageView;
                if (adapterType == MegaNodeAdapter.ITEM_VIEW_TYPE_LIST) {
                    imageView = (ImageView)v.findViewById(R.id.file_list_thumbnail);
                } else {
                    imageView = (ImageView)v.findViewById(R.id.file_grid_thumbnail);
                }
                imageView.getLocationOnScreen(screenPosition);

                int[] dimens = new int[4];
                dimens[0] = screenPosition[0];
                dimens[1] = screenPosition[1];
                dimens[2] = imageView.getWidth();
                dimens[3] = imageView.getHeight();
                if (type == Constants.RUBBISH_BIN_ADAPTER) {
                    ((RubbishBinFragmentLollipop)fragment).itemClick(currentPosition,dimens,imageView);
                } else if (type == Constants.INBOX_ADAPTER) {
                    ((InboxFragmentLollipop)fragment).itemClick(currentPosition,dimens,imageView);
                } else if (type == Constants.INCOMING_SHARES_ADAPTER) {
                    ((IncomingSharesFragmentLollipop)fragment).itemClick(currentPosition,dimens,imageView);
                } else if (type == Constants.OUTGOING_SHARES_ADAPTER) {
                    ((OutgoingSharesFragmentLollipop)fragment).itemClick(currentPosition,dimens,imageView);
                } else if (type == Constants.CONTACT_FILE_ADAPTER) {
                    ((ContactFileListFragmentLollipop)fragment).itemClick(currentPosition,dimens,imageView);
                } else if(type==Constants.CONTACT_SHARED_FOLDER_ADAPTER){
                    ((ContactSharedFolderFragment) fragment).itemClick(currentPosition, dimens, imageView);
                }else if (type == Constants.FOLDER_LINK_ADAPTER) {
                    ((FolderLinkActivityLollipop)context).itemClick(currentPosition,dimens,imageView);
                } else if (type == Constants.SEARCH_ADAPTER) {
                    ((SearchFragmentLollipop)fragment).itemClick(currentPosition,dimens,imageView);
                } else {
                    LogUtil.logDebug("layout FileBrowserFragmentLollipop!");
                    ((FileBrowserFragmentLollipop)fragment).itemClick(currentPosition,dimens,imageView);
                }
                break;
            }
        }
    }


    private void threeDotsClicked(int currentPosition,MegaNode n) {
        LogUtil.logDebug("onClick: file_list_three_dots: " + currentPosition);
        if (!Util.isOnline(context)) {
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            } else if (context instanceof FolderLinkActivityLollipop) {
                ((FolderLinkActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
            } else if (context instanceof ContactFileListActivityLollipop) {
                ((ContactFileListActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
            }
            return;
        }

        if (isMultipleSelect()) {
            if (type == Constants.RUBBISH_BIN_ADAPTER) {
                ((RubbishBinFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == Constants.INBOX_ADAPTER) {
                ((InboxFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == Constants.INCOMING_SHARES_ADAPTER) {
                ((IncomingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == Constants.OUTGOING_SHARES_ADAPTER) {
                ((OutgoingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if (type == Constants.CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else if(type==Constants.CONTACT_SHARED_FOLDER_ADAPTER){
                ((ContactSharedFolderFragment) fragment).itemClick(currentPosition,null,null);
            } else if (type == Constants.FOLDER_LINK_ADAPTER) {
                ((FolderLinkActivityLollipop)context).itemClick(currentPosition,null,null);
            } else if (type == Constants.SEARCH_ADAPTER) {
                ((SearchFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            } else {
                LogUtil.logDebug("click layout FileBrowserFragmentLollipop!");
                ((FileBrowserFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            }
        } else {
            if (type == Constants.CONTACT_FILE_ADAPTER) {
                ((ContactFileListFragmentLollipop)fragment).showOptionsPanel(n);
            } else if (type == Constants.FOLDER_LINK_ADAPTER) {
                ((FolderLinkActivityLollipop)context).showOptionsPanel(n);
            } else if(type==Constants.CONTACT_SHARED_FOLDER_ADAPTER){
                ((ContactSharedFolderFragment) fragment).showOptionsPanel(n);
            } else {
                ((ManagerActivityLollipop)context).showNodeOptionsPanel(n);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        LogUtil.logDebug("OnLongCLick");

        ViewHolderBrowser holder = (ViewHolderBrowser)view.getTag();
        int currentPosition = holder.getAdapterPosition();
//        Toast.makeText(context,"pos:" + currentPosition ,Toast.LENGTH_SHORT ).show();
        if (type == Constants.RUBBISH_BIN_ADAPTER) {
            ((RubbishBinFragmentLollipop)fragment).activateActionMode();
            ((RubbishBinFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == Constants.INBOX_ADAPTER) {
            ((InboxFragmentLollipop)fragment).activateActionMode();
            ((InboxFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == Constants.INCOMING_SHARES_ADAPTER) {
            ((IncomingSharesFragmentLollipop)fragment).activateActionMode();
            ((IncomingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if(type==Constants.CONTACT_SHARED_FOLDER_ADAPTER){
            ((ContactSharedFolderFragment) fragment).activateActionMode();
            ((ContactSharedFolderFragment) fragment).itemClick(currentPosition, null, null);
        } else if (type == Constants.OUTGOING_SHARES_ADAPTER) {
            ((OutgoingSharesFragmentLollipop)fragment).activateActionMode();
            ((OutgoingSharesFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == Constants.CONTACT_FILE_ADAPTER) {
            ((ContactFileListFragmentLollipop)fragment).activateActionMode();
            ((ContactFileListFragmentLollipop)fragment).itemClick(currentPosition,null,null);
        } else if (type == Constants.FOLDER_LINK_ADAPTER) {
            LogUtil.logDebug("FOLDER_LINK_ADAPTER");
            ((FolderLinkActivityLollipop)context).activateActionMode();
            ((FolderLinkActivityLollipop)context).itemClick(currentPosition,null,null);
        } else if (type == Constants.SEARCH_ADAPTER) {
            if (((SearchFragmentLollipop)fragment).isAllowedMultiselect()) {
                ((SearchFragmentLollipop)fragment).activateActionMode();
                ((SearchFragmentLollipop)fragment).itemClick(currentPosition,null,null);
            }
        } else {
            LogUtil.logDebug("click layout FileBrowserFragmentLollipop!");
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
        LogUtil.logDebug("multipleSelect: " + multipleSelect);
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }

    public void setListFragment (RecyclerView listFragment) {
        this.listFragment = listFragment;
    }

    public void allowMultiselect() {

    }
}