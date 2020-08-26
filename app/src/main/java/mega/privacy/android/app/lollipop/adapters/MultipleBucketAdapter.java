package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.fragments.recent.RecentsBucketFragment;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class MultipleBucketAdapter extends RecyclerView.Adapter<MultipleBucketAdapter.ViewHolderMultipleBucket> implements View.OnClickListener, SectionTitleProvider {

    Context context;
    Object fragment;
    MegaApiAndroid megaApi;

    private DisplayMetrics outMetrics;

    List<MegaNode> nodes;
    boolean isMedia;

    public MultipleBucketAdapter(Context context, Object fragment, List<MegaNode> nodes, boolean isMedia) {
        this.context = context;
        this.fragment = fragment;
        this.isMedia = isMedia;
        setNodes(nodes);

        megaApi = MegaApplication.getInstance().getMegaApi();

        outMetrics = context.getResources().getDisplayMetrics();
    }

    public class ViewHolderMultipleBucket extends RecyclerView.ViewHolder {

        private LinearLayout multipleBucketLayout;
        private long document;
        private RelativeLayout mediaView;
        private ImageView thumbnailMedia;
        private RelativeLayout videoLayout;
        private TextView videoDuration;
        private RelativeLayout listView;
        private ImageView thumbnailList;
        private TextView nameText;
        private TextView infoText;
        private ImageView threeDots;

        public ViewHolderMultipleBucket(View itemView) {
            super(itemView);
        }

        public long getDocument() {
            return document;
        }

        public void setImageThumbnail(Bitmap image) {
            if (isMedia) {
                this.thumbnailMedia.setImageBitmap(image);
            } else {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.thumbnailList.getLayoutParams();
                params.width = params.height = px2dp(36, outMetrics);
                int margin = px2dp(18, outMetrics);
                params.setMargins(margin, margin, margin, 0);

                this.thumbnailList.setLayoutParams(params);
                this.thumbnailList.setImageBitmap(image);
            }
        }

        public ImageView getThumbnailList() {
            return thumbnailList;
        }

        public ImageView getThumbnailMedia() {
            return thumbnailMedia;
        }
    }

    public boolean isMedia() {
        return isMedia;
    }

    @Override
    public ViewHolderMultipleBucket onCreateViewHolder(ViewGroup parent, int viewType) {
        logDebug("onCreateViewHolder");
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multiple_bucket, parent, false);
        ViewHolderMultipleBucket holder = new ViewHolderMultipleBucket(v);

        holder.multipleBucketLayout = v.findViewById(R.id.multiple_bucket_layout);
        holder.multipleBucketLayout.setTag(holder);
        holder.multipleBucketLayout.setOnClickListener(this);
        holder.mediaView = v.findViewById(R.id.media_layout);
        holder.thumbnailMedia = v.findViewById(R.id.thumbnail_media);
        holder.videoLayout = v.findViewById(R.id.video_layout);
        holder.videoDuration = v.findViewById(R.id.duration_text);
        holder.listView = v.findViewById(R.id.list_layout);
        holder.thumbnailList = v.findViewById(R.id.thumbnail_list);
        holder.nameText = v.findViewById(R.id.name_text);
        holder.infoText = v.findViewById(R.id.info_text);
        holder.threeDots = v.findViewById(R.id.three_dots);
        holder.threeDots.setTag(holder);
        holder.threeDots.setOnClickListener(this);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderMultipleBucket holder, int position) {
        logDebug("onBindViewHolder");
        MegaNode node = getItemAtPosition(position);
        if (node == null) return;

        holder.document = node.getHandle();

        Bitmap thumbnail = getThumbnailFromCache(node);
        if (thumbnail == null) {
            thumbnail = getThumbnailFromFolder(node, context);
            if (thumbnail == null) {
                try {
                    if (node.hasThumbnail() || isMedia) {
                        thumbnail = getThumbnailFromMegaList(node, context, holder, megaApi, this);
                    } else {
                        createThumbnailList(context, node, holder, megaApi, this);
                    }
                } catch (Exception e) {
                    logError("Error getting or creating node thumbnail", e);
                    e.printStackTrace();
                }
            }
        }

        if (isMedia) {
            holder.mediaView.setVisibility(View.VISIBLE);
            holder.listView.setVisibility(View.GONE);
            if (isAudioOrVideo(node)) {
                holder.videoLayout.setVisibility(View.VISIBLE);
                holder.videoDuration.setText(getVideoDuration(node.getDuration()));
            } else {
                holder.videoLayout.setVisibility(View.GONE);
            }

            int size;
            if (isScreenInPortrait(context)) {
                size = outMetrics.widthPixels / 4;
            } else {
                size = outMetrics.widthPixels / 6;
            }
            size -= px2dp(2, outMetrics);

            holder.thumbnailMedia.getLayoutParams().width = size;
            holder.thumbnailMedia.getLayoutParams().height = size;

            if (thumbnail != null) {
                holder.setImageThumbnail(thumbnail);
            } else {
                holder.thumbnailMedia.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            }
        } else {
            holder.mediaView.setVisibility(View.GONE);
            holder.listView.setVisibility(View.VISIBLE);
            holder.nameText.setText(node.getName());
            holder.infoText.setText(getSizeString(node.getSize()) + " · " + formatTime(node.getCreationTime()));

            if (thumbnail != null) {
                holder.setImageThumbnail(thumbnail);
            } else {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.thumbnailList.getLayoutParams();
                params.width = params.height = px2dp(48, outMetrics);
                int margin = px2dp(12, outMetrics);
                params.setMargins(margin, margin, margin, 0);
                holder.thumbnailList.setLayoutParams(params);
                holder.thumbnailList.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            }
        }
    }

    private MegaNode getItemAtPosition(int pos) {
        if (nodes == null || nodes.isEmpty() || pos >= nodes.size()) return null;

        return nodes.get(pos);
    }

    @Override
    public int getItemCount() {
        if (nodes == null) return 0;

        return nodes.size();
    }

    private void setNodes(List<MegaNode> nodes) {
        this.nodes = nodes;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");
        MultipleBucketAdapter.ViewHolderMultipleBucket holder = (MultipleBucketAdapter.ViewHolderMultipleBucket) v.getTag();
        if (holder == null) return;

        MegaNode node = getItemAtPosition(holder.getAdapterPosition());
        if (node == null) return;
        switch (v.getId()) {
            case R.id.three_dots: {
                if (!isOnline(context)) {
                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                    break;
                }
                ((ManagerActivityLollipop) context).showNodeOptionsPanel(node);
                break;
            }
            case R.id.multiple_bucket_layout: {
                if(ManagerActivityLollipop.getDrawerItem() == ManagerActivityLollipop.DrawerItem.HOMEPAGE) {
                    ((RecentsBucketFragment) fragment).openFile(node, true,
                            v.findViewById(isMedia ? R.id.thumbnail_media : R.id.thumbnail_list),
                            RecentsFragment.OPEN_FROM_SUB);
                } else {
                    ((RecentsFragment) fragment).openFile(node, true,
                            v.findViewById(isMedia ? R.id.thumbnail_media : R.id.thumbnail_list),
                            RecentsFragment.OPEN_FROM_SUB);
                }
                break;
            }
        }
    }

    @Override
    public String getSectionTitle(int position) {
        MegaNode node = getItemAtPosition(position);
        if (node == null) return "";

        String name = node.getName();
        if (!isTextEmpty(name)) return name.substring(0, 1);

        return "";
    }

    public int getNodePosition(long handle) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getHandle() == handle) {
                return i;
            }
        }
        return INVALID_POSITION;
    }

    public ImageView getThumbnailView(RecyclerView recyclerView, long handle) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        int position = getNodePosition(handle);
        if (position == INVALID_POSITION) {
            return null;
        }

        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForLayoutPosition(position);
        if (holder instanceof ViewHolderMultipleBucket) {
            return isMedia ? ((ViewHolderMultipleBucket) holder).thumbnailMedia
                : ((ViewHolderMultipleBucket) holder).thumbnailList;
        }

        return null;
    }
}
