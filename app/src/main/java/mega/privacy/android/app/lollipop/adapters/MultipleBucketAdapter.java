package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.FileUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class MultipleBucketAdapter extends RecyclerView.Adapter<MultipleBucketAdapter.ViewHolderMultipleBucket> implements View.OnClickListener, SectionTitleProvider {

    Context context;
    Object fragment;
    MegaApiAndroid megaApi;

    private DisplayMetrics outMetrics;

    ArrayList<MegaNode> nodes;
    boolean isMedia;

    public MultipleBucketAdapter(Context context, Object fragment, ArrayList<MegaNode> nodes, boolean isMedia) {
        this.context = context;
        this.fragment = fragment;
        this.isMedia = isMedia;
        setNodes(nodes);

        megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
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
                params.width = params.height = Util.px2dp(36, outMetrics);
                int margin = Util.px2dp(18, outMetrics);
                params.setMargins(margin, margin, margin, 0);

                this.thumbnailList.setLayoutParams(params);
                this.thumbnailList.setImageBitmap(image);
            }
        }
    }

    @Override
    public ViewHolderMultipleBucket onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");
        outMetrics = context.getResources().getDisplayMetrics();
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
        log("onBindViewHolder");
        MegaNode node = getItemAtPosition(position);
        if (node == null) return;

        holder.document = node.getHandle();

        if (isMedia) {
            holder.mediaView.setVisibility(View.VISIBLE);
            holder.listView.setVisibility(View.GONE);
            if (FileUtils.isAudioOrVideo(node)) {
                holder.videoLayout.setVisibility(View.VISIBLE);
                holder.videoDuration.setText(TimeUtils.getVideoDuration(node.getDuration()));
            } else {
                holder.videoLayout.setVisibility(View.GONE);
            }

            int size;
            if (Util.isScreenInPortrait(context)) {
                size = outMetrics.widthPixels / 4;
            } else {
                size = outMetrics.widthPixels / 6;
            }
            size -= Util.px2dp(2, outMetrics);
            log("outMetrics.widthPixels: " + outMetrics.widthPixels + " final size: " + size);
            holder.thumbnailMedia.getLayoutParams().width = size;
            holder.thumbnailMedia.getLayoutParams().height = size;
            holder.thumbnailMedia.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
        } else {
            holder.mediaView.setVisibility(View.GONE);
            holder.listView.setVisibility(View.VISIBLE);
            holder.nameText.setText(node.getName());
            holder.infoText.setText(Util.getSizeString(node.getSize()) + " · " + TimeUtils.formatTime(node.getCreationTime()));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.thumbnailList.getLayoutParams();
            params.width = params.height = Util.px2dp(48, outMetrics);
            int margin = Util.px2dp(12, outMetrics);
            params.setMargins(margin, margin, margin, 0);
            holder.thumbnailList.setLayoutParams(params);
            holder.thumbnailList.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
        }

        Bitmap thumbnail = ThumbnailUtils.getThumbnailFromCache(node);
        if (thumbnail == null) {
            thumbnail = ThumbnailUtils.getThumbnailFromFolder(node, context);
            if (thumbnail == null) {
                try {
                    if (node.hasThumbnail()) {
                        thumbnail = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node, context, holder, megaApi, this);
                    } else {
                        ThumbnailUtilsLollipop.createThumbnailList(context, node, holder, megaApi, this);
                    }
                } catch (Exception e) {
                    log("Error getting or creating node thumbnail");
                    e.printStackTrace();
                }
            }
        }
        if (thumbnail != null) {
            holder.setImageThumbnail(thumbnail);
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

    private void setNodes(ArrayList<MegaNode> nodes) {
        this.nodes = nodes;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        log("onClick");
        MultipleBucketAdapter.ViewHolderMultipleBucket holder = (MultipleBucketAdapter.ViewHolderMultipleBucket) v.getTag();
        if (holder == null) return;

        MegaNode node = getItemAtPosition(holder.getAdapterPosition());
        if (node == null) return;
        switch (v.getId()) {
            case R.id.three_dots: {
                log("three_dots click");
                if (!Util.isOnline(context)) {
                    ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                    break;
                }
                ((ManagerActivityLollipop) context).showNodeOptionsPanel(node);
                break;
            }
            case R.id.multiple_bucket_layout: {
                ((RecentsFragment) fragment).openFile(node, true);
                break;
            }
        }
    }

    @Override
    public String getSectionTitle(int position) {
        MegaNode node = getItemAtPosition(position);
        if (node == null) return "";

        String name = node.getName();
        if (name != null && !name.isEmpty()) return name.substring(0, 1);

        return "";
    }

    private static void log(String log) {
        Util.log("MultipleBucketAdapter", log);
    }

}
