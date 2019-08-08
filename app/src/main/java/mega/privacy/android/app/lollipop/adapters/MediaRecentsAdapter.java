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
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRecentActionBucket;

public class MediaRecentsAdapter extends RecyclerView.Adapter<MediaRecentsAdapter.ViewHolderMediaBucket> implements View.OnClickListener {

    private MegaApiAndroid megaApi;
    private Context context;
    private Object fragment;
    private MegaRecentActionBucket bucket;

    private DisplayMetrics outMetrics;

    private MegaNodeList nodes;

    public MediaRecentsAdapter(Context context, Object fragment, MegaNodeList nodeList, MegaRecentActionBucket bucket) {
        this.context = context;
        this.fragment = fragment;
        this.nodes = nodeList;
        this.bucket = bucket;

        megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
    }

    public class ViewHolderMediaBucket extends RecyclerView.ViewHolder {

        private long document;
        private ImageView thumbnail;
        private RelativeLayout videoLayout;
        private TextView videoDuration;

        public void setImage(Bitmap image) {
            this.thumbnail.setImageBitmap(image);
        }

        public ImageView getThumbnail() {
            return thumbnail;
        }

        public long getDocument() {
            return document;
        }

        public ViewHolderMediaBucket(View itemView) {
            super(itemView);
        }
    }

    @Override
    public MediaRecentsAdapter.ViewHolderMediaBucket onCreateViewHolder(ViewGroup parent, int viewType) {
        outMetrics = context.getResources().getDisplayMetrics();

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_bucket, parent, false);
        ViewHolderMediaBucket holder = new ViewHolderMediaBucket(v);

        holder.thumbnail = v.findViewById(R.id.thumbnail_view);
        holder.videoLayout = v.findViewById(R.id.video_layout);
        holder.videoDuration = v.findViewById(R.id.duration_text);
        holder.itemView.setOnClickListener(this);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(MediaRecentsAdapter.ViewHolderMediaBucket holder, int position) {
        MegaNode node = getItemAtPosition(position);
        if (node == null) return;

        holder.document = node.getHandle();
        holder.thumbnail.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

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
            holder.setImage(ThumbnailUtilsLollipop.getRoundedBitmap(context, thumbnail, Util.px2dp(1, outMetrics)));
        }

        if (MimeTypeList.typeForName(node.getName()).isVideo()) {
            holder.videoLayout.setVisibility(View.VISIBLE);
            holder.videoDuration.setText(TimeUtils.getVideoDuration(node.getDuration()));
        } else {
            holder.videoLayout.setVisibility(View.GONE);
        }
    }

    private MegaNode getItemAtPosition(int pos) {
        if (nodes == null || nodes.size() == 0 || pos >= nodes.size()) return null;

        return nodes.get(pos);
    }

    @Override
    public int getItemCount() {
        if (nodes == null) return 0;

        return nodes.size();
    }

    @Override
    public void onClick(View v) {

        ViewHolderMediaBucket holder = (ViewHolderMediaBucket) v.getTag();
        if (holder == null) return;

        MegaNode node = getItemAtPosition(holder.getAdapterPosition());
        if (node == null) return;

        ((RecentsFragment) fragment).setBucketSelected(bucket);
        ((RecentsFragment) fragment).openFile(node, true);

    }

    private static void log(String log) {
        Util.log("MediaRecentsAdapter", log);
    }
}
