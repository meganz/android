package mega.privacy.android.app.presentation.recentactions.recentactionbucket;

import static mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.RECENTS_MODE;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.FileUtil.isAudioOrVideo;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TimeUtils.formatTime;
import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

import coil.Coil;
import coil.transform.RoundedCornersTransformation;
import coil.util.CoilUtils;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.dragger.DragThumbnailGetter;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.fragments.homepage.NodeItem;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class RecentActionBucketAdapter
        extends ListAdapter<NodeItem, RecentActionBucketAdapter.ViewHolderMultipleBucket>
        implements View.OnClickListener, View.OnLongClickListener, SectionTitleProvider, DragThumbnailGetter {

    Context context;
    Object fragment;
    MegaApiAndroid megaApi;

    private final DisplayMetrics outMetrics;

    List<NodeItem> nodes;
    boolean isMedia;
    boolean isIncomingShare;

    public RecentActionBucketAdapter(Context context, Object fragment, List<NodeItem> nodes, boolean isMedia, boolean isIncomingShare, RecentActionBucketDiffCallback diffCallback) {
        super(diffCallback);
        this.context = context;
        this.fragment = fragment;
        this.isMedia = isMedia;
        this.isIncomingShare = isIncomingShare;
        setNodes(nodes);

        megaApi = MegaApplication.getInstance().getMegaApi();

        outMetrics = context.getResources().getDisplayMetrics();
    }

    // View Holder
    public class ViewHolderMultipleBucket extends RecyclerView.ViewHolder {
        private LinearLayout multipleBucketLayout;
        private long document;
        private RelativeLayout mediaView;
        private ImageView thumbnailMedia;
        private RelativeLayout videoLayout;
        private TextView videoDuration;
        private RelativeLayout listView;
        private ImageView selectedIcon;
        private ImageView thumbnailList;
        private TextView nameText;
        private TextView infoText;
        private ImageView imgLabel;
        private ImageView imgFavourite;
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
                RelativeLayout.LayoutParams params =
                        (RelativeLayout.LayoutParams) this.thumbnailList.getLayoutParams();
                params.width = params.height = dp2px(ITEM_WIDTH, outMetrics);
                int margin = dp2px(ITEM_MARGIN, outMetrics);
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
    public int getNodePosition(long handle) {
        for (int i = 0; i < nodes.size(); i++) {
            NodeItem node = nodes.get(i);
            if (node != null && node.getNode().getHandle() == handle) {
                return i;
            }
        }

        return INVALID_POSITION;
    }

    @Nullable
    @Override
    public ImageView getThumbnail(@NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ViewHolderMultipleBucket) {
            return isMedia ? ((ViewHolderMultipleBucket) viewHolder).thumbnailMedia
                    : ((ViewHolderMultipleBucket) viewHolder).thumbnailList;
        }

        return null;
    }

    @Override
    public ViewHolderMultipleBucket onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multiple_bucket, parent, false);
        ViewHolderMultipleBucket holder = new ViewHolderMultipleBucket(v);

        holder.multipleBucketLayout = v.findViewById(R.id.multiple_bucket_layout);
        holder.multipleBucketLayout.setTag(holder);
        holder.multipleBucketLayout.setOnClickListener(this);
        holder.multipleBucketLayout.setOnLongClickListener(this);
        holder.mediaView = v.findViewById(R.id.media_layout);
        holder.thumbnailMedia = v.findViewById(R.id.thumbnail_media);
        holder.videoLayout = v.findViewById(R.id.video_layout);
        holder.videoDuration = v.findViewById(R.id.duration_text);
        holder.listView = v.findViewById(R.id.list_layout);
        holder.thumbnailList = v.findViewById(R.id.thumbnail_list);
        holder.nameText = v.findViewById(R.id.name_text);
        holder.infoText = v.findViewById(R.id.info_text);
        holder.imgLabel = v.findViewById(R.id.img_label);
        holder.imgFavourite = v.findViewById(R.id.img_favourite);
        holder.selectedIcon = v.findViewById(R.id.icon_selected);
        holder.threeDots = v.findViewById(R.id.three_dots);
        holder.threeDots.setTag(holder);
        holder.threeDots.setOnClickListener(this);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderMultipleBucket holder, int position) {
        Timber.d("onBindViewHolder");
        NodeItem node = getItemAtPosition(position);
        MegaNode megaNode = node.getNode();
        if (megaNode == null) return;
        holder.document = megaNode.getHandle();

        if (isMedia) {
            holder.mediaView.setVisibility(View.VISIBLE);
            holder.mediaView.setAlpha(node.isSensitive() && !isIncomingShare ? 0.5f : 1f);
            holder.listView.setVisibility(View.GONE);
            holder.imgLabel.setVisibility(View.GONE);
            holder.imgFavourite.setVisibility(View.GONE);
            holder.selectedIcon.setVisibility(View.GONE);

            if (isAudioOrVideo(megaNode)) {
                holder.videoLayout.setVisibility(View.VISIBLE);
                holder.videoDuration.setText(getVideoDuration(megaNode.getDuration()));
            } else {
                holder.videoLayout.setVisibility(View.GONE);
            }

            holder.thumbnailMedia.setVisibility(View.VISIBLE);
            CoilUtils.dispose(holder.thumbnailMedia);

            int size;
            if (isScreenInPortrait(context)) {
                size = outMetrics.widthPixels / 4;
            } else {
                size = outMetrics.widthPixels / 6;
            }
            size -= dp2px(2, outMetrics);

            holder.thumbnailMedia.getLayoutParams().width = size;
            holder.thumbnailMedia.getLayoutParams().height = size;

            if (megaNode.hasThumbnail()) {
                Coil.imageLoader(context).enqueue(
                        new coil.request.ImageRequest.Builder(context)
                                .placeholder(MimeTypeList.typeForName(megaNode.getName()).getIconResourceId())
                                .data(ThumbnailRequest.fromHandle(megaNode.getHandle()))
                                .target(holder.thumbnailMedia)
                                .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                                .build()
                );
            } else {
                holder.thumbnailMedia.setImageResource(
                        MimeTypeList.typeForName(megaNode.getName()).getIconResourceId()
                );
            }

            if (node.getSelected()) {
                holder.selectedIcon.setVisibility(View.VISIBLE);
            } else {
                holder.selectedIcon.setVisibility(View.GONE);
            }
        } else {
            holder.mediaView.setVisibility(View.GONE);
            holder.listView.setVisibility(View.VISIBLE);
            holder.listView.setAlpha(node.isSensitive() && !isIncomingShare ? 0.5f : 1f);
            holder.nameText.setText(megaNode.getName());
            holder.infoText.setText(getSizeString(megaNode.getSize(), context) + " · " + formatTime(megaNode.getCreationTime()));

            holder.thumbnailList.setVisibility(View.VISIBLE);
            CoilUtils.dispose(holder.thumbnailList);

            if (megaNode.getLabel() != MegaNode.NODE_LBL_UNKNOWN) {
                Drawable drawable = MegaNodeUtil.getNodeLabelDrawable(megaNode.getLabel(), holder.itemView.getResources());
                holder.imgLabel.setImageDrawable(drawable);
                holder.imgLabel.setVisibility(View.VISIBLE);
            } else {
                holder.imgLabel.setVisibility(View.GONE);
            }

            holder.imgFavourite.setVisibility(megaNode.isFavourite() ? View.VISIBLE : View.GONE);

            if (node.getSelected()) {
                holder.thumbnailList.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder);
            } else {
                holder.thumbnailList.setImageDrawable(null);

                int placeHolderRes = MimeTypeList.typeForName(node.getNode().getName()).getIconResourceId();

                if (megaNode.hasThumbnail()) {
                    Coil.imageLoader(context).enqueue(
                            new coil.request.ImageRequest.Builder(context)
                                    .placeholder(placeHolderRes)
                                    .data(ThumbnailRequest.fromHandle(megaNode.getHandle()))
                                    .target(holder.thumbnailList)
                                    .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                                    .build()
                    );
                } else {
                    int imgResource;
                    if (megaNode.isFolder()) {
                        imgResource = mega.privacy.android.icon.pack.R.drawable.ic_folder_medium_solid;
                    } else {
                        imgResource = placeHolderRes;
                    }
                    holder.thumbnailList.setImageResource(imgResource);
                }
            }
        }

        node.setUiDirty(false);
    }

    private NodeItem getItemAtPosition(int pos) {
        if (nodes == null || nodes.isEmpty() || pos >= nodes.size() || pos < 0) return null;

        return nodes.get(pos);
    }

    @Override
    public int getItemCount() {
        if (nodes == null) return 0;

        return nodes.size();
    }

    public void setNodes(List<NodeItem> nodes) {
        this.nodes = nodes;
        notifyDataSetChanged();
    }

    public void setIsIncomingShare(boolean isIncomingShare) {
        this.isIncomingShare = isIncomingShare;
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");
        RecentActionBucketAdapter.ViewHolderMultipleBucket holder = (RecentActionBucketAdapter.ViewHolderMultipleBucket) v.getTag();
        if (holder == null) return;

        List<NodeItem> selectedNodes = this.nodes.stream().filter(NodeItem::getSelected).collect(Collectors.toList());
        NodeItem node = getItemAtPosition(holder.getAbsoluteAdapterPosition());
        if (node == null) return;
        int id = v.getId();
        if (id == R.id.three_dots) {
            if (selectedNodes.isEmpty()) {
                if (!isOnline(context)) {
                    ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                    return;
                }
                ((ManagerActivity) context).showNodeOptionsPanel(node.getNode(), RECENTS_MODE);
            } else {
                if (fragment instanceof RecentActionBucketFragment) {
                    ((RecentActionBucketFragment) fragment).handleItemClick(holder.getAdapterPosition(), node, true);
                }
            }
        } else if (id == R.id.multiple_bucket_layout) {
            if (fragment instanceof RecentActionBucketFragment) {
                ((RecentActionBucketFragment) fragment).handleItemClick(holder.getAdapterPosition(), node, true);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onLongClick(View v) {
        Timber.d("onClick");
        RecentActionBucketAdapter.ViewHolderMultipleBucket holder = (RecentActionBucketAdapter.ViewHolderMultipleBucket) v.getTag();
        if (holder == null) return false;

        NodeItem node = getItemAtPosition(holder.getAbsoluteAdapterPosition());
        if (node == null) return false;

        if (fragment instanceof RecentActionBucketFragment) {
            ((RecentActionBucketFragment) fragment).onNodeLongClicked(
                    holder.getAdapterPosition(),
                    node
            );
        }

        return true;
    }

    @Override
    public String getSectionTitle(int position, Context context) {
        NodeItem node = getItemAtPosition(position);
        if (node == null) return "";

        String name = node.getNode().getName();
        if (!isTextEmpty(name)) return name.substring(0, 1);

        return "";
    }

    private static final int ITEM_WIDTH = 48;
    private static final int ITEM_MARGIN = 12;
}
