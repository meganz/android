package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.PlaylistFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.OfflineUtils.getOfflineFile;

public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.ViewHolderBrowser> implements View.OnClickListener, SectionTitleProvider {

    MegaApiAndroid megaApi;

    Context context;
    ArrayList<Long> handles;
    ArrayList<MegaNode> nodes;
    ArrayList<MegaOffline> offNodes;
    ArrayList<File> zipFiles;
    long parentHandle;
    String parentPath;
    RecyclerView recyclerView;
    Fragment fragment;
    int adapterType;

    int itemChecked;
    MegaNode nodeChecked;
    MegaOffline offNodeChecked;
    File zipChecked;

    @Override
    public String getSectionTitle(int position) {
        return getItemNode(position).substring(0, 1);
    }

    private String getItemNode(int position) {
        return nodes.get(position).getName();
    }

    public static class ViewHolderBrowser extends RecyclerView.ViewHolder {

        public ViewHolderBrowser(View v) {
            super(v);
        }

        public ImageView imageView;
        public TextView textViewFileName;
        public TextView textViewFileSize;
        public TextView textViewState;
        public RelativeLayout itemLayout;
    }

    public PlayListAdapter(Context _context, Fragment _fragment, ArrayList<Long> _handles, long _parentHandle, RecyclerView _recyclerView, int adapterType) {

        this.context = _context;
        this.fragment = _fragment;
        this.handles = _handles;
        this.parentHandle = _parentHandle;
        this.recyclerView = _recyclerView;
        this.adapterType = adapterType;

        if (megaApi == null) {
            if (((AudioVideoPlayerLollipop) context).isFolderLink()){
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApiFolder();
            }
            else {
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
            }
        }

        nodes = new ArrayList<>();
        for (int i =0; i<handles.size(); i++){
            nodes.add(megaApi.getNodeByHandle(handles.get(i)));
        }
        setNodes(nodes);
    }

    public PlayListAdapter(Context _context, Fragment _fragment, ArrayList<MegaOffline> _offNodes, String _parentPath, RecyclerView _recyclerView, int adapterType) {

        this.context = _context;
        this.fragment = _fragment;
        this.offNodes =  new ArrayList<>();
        this.parentPath = _parentPath;
        this.recyclerView = _recyclerView;
        this.adapterType = adapterType;

        setOffNodes(_offNodes);
    }

    public PlayListAdapter(Context _context, Fragment _fragment, ArrayList<File> _zipFiles, RecyclerView _recyclerView, int adapterType) {

        this.context = _context;
        this.fragment = _fragment;
        this.zipFiles =  new ArrayList<>();
        this.recyclerView = _recyclerView;
        this.adapterType = adapterType;

        setZipNodes(_zipFiles);
    }

    public void setZipNodes (ArrayList<File> files) {
        LogUtil.logDebug("setZipNodes");
        this.zipFiles = files;
        notifyDataSetChanged();
    }

    public void setOffNodes(ArrayList<MegaOffline> nodes) {
        LogUtil.logDebug("setOffNodes");
        this.offNodes = nodes;
        notifyDataSetChanged();
    }

    public void setNodes(ArrayList<MegaNode> nodes) {
        LogUtil.logDebug("Size: " + nodes.size());
        this.nodes = nodes;
        notifyDataSetChanged();
    }

    @Override
    public PlayListAdapter.ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
        LogUtil.logDebug("onCreateViewHolder");

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_playlist, parent, false);
        PlayListAdapter.ViewHolderBrowser holderList = new PlayListAdapter.ViewHolderBrowser(v);

        holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.file_list_item_layout_playlist);
        holderList.imageView = (ImageView) v.findViewById(R.id.file_list_thumbnail);
        holderList.textViewFileName = (TextView) v.findViewById(R.id.file_list_filename);
        holderList.textViewFileSize = (TextView) v.findViewById(R.id.file_list_filesize);
        holderList.textViewState = (TextView) v.findViewById(R.id.file_list_filestate);

        holderList.itemLayout.setTag(holderList);
        holderList.itemLayout.setOnClickListener(this);

        return holderList;
    }

    @Override
    public void onBindViewHolder(ViewHolderBrowser holder, int position) {
        LogUtil.logDebug("Position: " + position);

        MegaOffline offNode = null;
        File mediaFile = null;
        File zipFile = null;
        MegaNode node = null;

        if (adapterType == Constants.OFFLINE_ADAPTER){
            offNode = (MegaOffline) getItem(position);
            mediaFile = getOfflineFile(context, offNode);
        }
        else if (adapterType == Constants.ZIP_ADAPTER) {
            zipFile = (File) getItem(position);
        }
        else {
            node = (MegaNode) getItem(position);
        }

        Bitmap thumb = null;

        if (adapterType == Constants.OFFLINE_ADAPTER){
            holder.textViewFileName.setText(offNode.getName());
            holder.textViewFileSize.setText(Util.getSizeString(mediaFile.length()));

            if ((position == itemChecked && !((PlaylistFragment) fragment).isSearchOpen()) && ((AudioVideoPlayerLollipop) context).querySearch.equals("")
                    || (((PlaylistFragment) fragment).isSearchOpen() || !((AudioVideoPlayerLollipop) context).querySearch.equals("")) && (offNode.getName().equals(offNodeChecked.getName()))){
                holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.file_playlist_playing));
                holder.textViewFileSize.setVisibility(View.GONE);
                holder.textViewState.setVisibility(View.VISIBLE);
                if (((PlaylistFragment) fragment).getPlayer().getPlayWhenReady()){
                    holder.textViewState.setText(context.getString(R.string.playlist_state_playing));
                }
                else {
                    holder.textViewState.setText(context.getString(R.string.playlist_state_paused));
                }
            }
            else{
                holder.itemLayout.setBackgroundColor(Color.WHITE);
                holder.textViewFileSize.setVisibility(View.VISIBLE);
                holder.textViewState.setVisibility(View.GONE);
            }
            holder.imageView.setImageResource(MimeTypeList.typeForName(offNode.getName()).getIconResourceId());
        }
        else if (adapterType == Constants.ZIP_ADAPTER) {
            holder.textViewFileName.setText(zipFile.getName());
            holder.textViewFileSize.setText(Util.getSizeString(zipFile.length()));

            if ((position == itemChecked && !((PlaylistFragment) fragment).isSearchOpen()) && ((AudioVideoPlayerLollipop) context).querySearch.equals("")
                    || (((PlaylistFragment) fragment).isSearchOpen() || !((AudioVideoPlayerLollipop) context).querySearch.equals("")) && (zipFile.getName().equals(zipChecked.getName()))) {
                holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.file_playlist_playing));
                holder.textViewFileSize.setVisibility(View.GONE);
                holder.textViewState.setVisibility(View.VISIBLE);
                if (((PlaylistFragment) fragment).getPlayer().getPlayWhenReady()){
                    holder.textViewState.setText(context.getString(R.string.playlist_state_playing));
                }
                else {
                    holder.textViewState.setText(context.getString(R.string.playlist_state_paused));
                }
            }
            else {
                holder.itemLayout.setBackgroundColor(Color.WHITE);
                holder.textViewFileSize.setVisibility(View.VISIBLE);
                holder.textViewState.setVisibility(View.GONE);
            }
            holder.imageView.setImageResource(MimeTypeList.typeForName(zipFile.getName()).getIconResourceId());
        }
        else {
            holder.textViewFileName.setText(node.getName());
            holder.textViewFileSize.setText(Util.getSizeString(node.getSize()));

            if ((position == itemChecked && !((PlaylistFragment) fragment).isSearchOpen()) && ((AudioVideoPlayerLollipop) context).querySearch.equals("")
                    || (((PlaylistFragment) fragment).isSearchOpen() || !((AudioVideoPlayerLollipop) context).querySearch.equals("")) && (node.getName().equals(nodeChecked.getName()))){
                holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.file_playlist_playing));
                holder.textViewFileSize.setVisibility(View.GONE);
                holder.textViewState.setVisibility(View.VISIBLE);
                if (((PlaylistFragment) fragment).getPlayer().getPlayWhenReady()){
                    holder.textViewState.setText(context.getString(R.string.playlist_state_playing));
                }
                else {
                    holder.textViewState.setText(context.getString(R.string.playlist_state_paused));
                }
            }
            else{
                holder.itemLayout.setBackgroundColor(Color.WHITE);
                holder.textViewFileSize.setVisibility(View.VISIBLE);
                holder.textViewState.setVisibility(View.GONE);
            }
            holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        params.setMargins(0, 0, 0, 0);
        holder.imageView.setLayoutParams(params);

        LogUtil.logDebug("Check the thumb");

        if (adapterType == Constants.OFFLINE_ADAPTER || adapterType == Constants.ZIP_ADAPTER){

        }
        else{
            if (node.hasThumbnail()) {
                LogUtil.logDebug("Node has thumbnail");
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
                params1.setMargins(left, 0, 0, 0);

                holder.imageView.setLayoutParams(params1);

                thumb = ThumbnailUtils.getThumbnailFromCache(node);
                if (thumb != null) {

                    holder.imageView.setImageBitmap(thumb);

                } else {
                    thumb = ThumbnailUtils
                            .getThumbnailFromFolder(node, context);
                    if (thumb != null) {
                        holder.imageView.setImageBitmap(thumb);

                    } else {
                        try {
                            thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node, context, holder, megaApi, this);
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
                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
                    params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                    params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                    int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
                    params1.setMargins(left, 0, 0, 0);

                    holder.imageView.setLayoutParams(params1);
                    holder.imageView.setImageBitmap(thumb);


                } else {
                    thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                    if (thumb != null) {
                        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
                        params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                        params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                        int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
                        params1.setMargins(left, 0, 0, 0);

                        holder.imageView.setLayoutParams(params1);
                        holder.imageView.setImageBitmap(thumb);

                    } else {
                        try {
                            ThumbnailUtilsLollipop.createThumbnailList(context, node,holder, megaApi, this);
                        } catch (Exception e) {
                        } // Too many AsyncTasks
                    }
                }
            }
        }
    }

    public Object getItem(int position) {

        if (adapterType == Constants.OFFLINE_ADAPTER){
            if (offNodes != null){
                return offNodes.get(position);
            }
        }
        else if (adapterType == Constants.ZIP_ADAPTER) {
            if (zipFiles != null) {
                return zipFiles.get(position);
            }
        }
        else {
            if (nodes != null){
                return nodes.get(position);
            }
        }

        return null;
    }

    @Override
    public int getItemCount() {
        if (adapterType == Constants.OFFLINE_ADAPTER){
            if (offNodes != null){
                return offNodes.size();
            }
        }
        else if (adapterType == Constants.ZIP_ADAPTER) {
            if (zipFiles != null) {
                return zipFiles.size();
            }
        }
        else{
            if (nodes != null){
                return nodes.size();
            }
        }
        return 0;
    }

    @Override
    public void onClick(View v) {

        ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();
        int currentPosition = holder.getAdapterPosition();

        if (!((PlaylistFragment) fragment).isSearchOpen()) {
            if(!((AudioVideoPlayerLollipop) context).querySearch.equals("")) {
                ((AudioVideoPlayerLollipop) context).onBackPressed();
            }
            if (itemChecked == currentPosition) {
                if (((PlaylistFragment) fragment).getPlayer().getPlayWhenReady()) {
                    ((PlaylistFragment) fragment).getPlayer().setPlayWhenReady(false);
                } else {
                    ((PlaylistFragment) fragment).getPlayer().setPlayWhenReady(true);
                }
                ((PlaylistFragment) fragment).showController(false);
            }
            else {
                setItemChecked(currentPosition);

                switch (v.getId()) {
                    case R.id.file_list_item_layout_playlist: {
                        ((PlaylistFragment) fragment).itemClick(currentPosition);
                        ((PlaylistFragment) fragment).getPlayer().setPlayWhenReady(true);
                        ((PlaylistFragment) fragment).showController(false);
                    }
                }
            }

            notifyDataSetChanged();
        }
        else {
            ((AudioVideoPlayerLollipop) context).searchMenuItem.collapseActionView();
            String name = holder.textViewFileName.getText().toString();
            if (adapterType == Constants.OFFLINE_ADAPTER){
                if (name.equals(offNodeChecked.getName())){
                    playPauseItem();
                }
                else {
                    for (int i=0; i<offNodes.size(); i++){
                        if (offNodes.get(i).getName().equals(name)){
                            changeItem(i);
                            break;
                        }
                    }
                }
            }
            else if (adapterType == Constants.ZIP_ADAPTER) {
                if (name.equals(zipChecked.getName())) {
                    playPauseItem();
                }
                else {
                    for (int i=0; i<zipFiles.size(); i++){
                        if (zipFiles.get(i).getName().equals(name)){
                            changeItem(i);
                            break;
                        }
                    }
                }
            }
            else {
                if (name.equals(nodeChecked.getName())){
                    playPauseItem();
                }
                else {
                    for (int i=0; i<nodes.size(); i++){
                        if (nodes.get(i).getName().equals(name)){
                            changeItem(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void playPauseItem(){
        if (((PlaylistFragment) fragment).getPlayer().getPlayWhenReady()) {
            ((PlaylistFragment) fragment).getPlayer().setPlayWhenReady(false);
        } else {
            ((PlaylistFragment) fragment).getPlayer().setPlayWhenReady(true);
        }
        ((PlaylistFragment) fragment).showController(false);
        ((PlaylistFragment) fragment).scrollTo(itemChecked);
    }

    public void changeItem(int i){
        setItemChecked(i);
        ((PlaylistFragment) fragment).itemClick(i);
        ((PlaylistFragment) fragment).scrollTo(i);
        ((PlaylistFragment) fragment).showController(false);
        ((PlaylistFragment) fragment).getPlayer().setPlayWhenReady(true);
    }

    public void setItemChecked (int position){
        if (position >=0) {
            this.itemChecked = position;
            if (adapterType == Constants.OFFLINE_ADAPTER) {
                if (offNodes != null && position < offNodes.size()) {
                    offNodeChecked = offNodes.get(position);
                }
            }
            else if (adapterType == Constants.ZIP_ADAPTER) {
                if (zipFiles != null && position < zipFiles.size()) {
                    zipChecked = zipFiles.get(position);
                }
            }
            else {
                if (nodes != null && position < nodes.size()) {
                    nodeChecked = nodes.get(position);
                }
            }
        }
    }

    public int getItemChecked (){
        return itemChecked;
    }
}
