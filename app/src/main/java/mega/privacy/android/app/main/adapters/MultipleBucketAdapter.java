package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment.RECENTS_MODE;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.FileUtil.isAudioOrVideo;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.ThumbnailUtils.createThumbnailList;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromCache;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromFolder;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbnailFromMegaList;
import static mega.privacy.android.app.utils.TimeUtils.formatTime;
import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.dragger.DragThumbnailGetter;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.fragments.homepage.NodeItem;
import mega.privacy.android.app.fragments.recent.RecentsBucketDiffCallback;
import mega.privacy.android.app.fragments.recent.RecentsBucketFragment;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.utils.MegaNodeUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class MultipleBucketAdapter
        extends ListAdapter<NodeItem, MultipleBucketAdapter.ViewHolderMultipleBucket>
        implements View.OnClickListener, View.OnLongClickListener, SectionTitleProvider, DragThumbnailGetter
{

    Context context;
    Object fragment;
    MegaApiAndroid megaApi;

    private final DisplayMetrics outMetrics;

    List<NodeItem> nodes;
    boolean isMedia;

    public MultipleBucketAdapter(Context context, Object fragment, List<NodeItem> nodes, boolean isMedia, RecentsBucketDiffCallback diffCallback) {
        super(diffCallback);
        this.context = context;
        this.fragment = fragment;
        this.isMedia = isMedia;
        setNodes(nodes);

        megaApi = MegaApplication.getInstance().getMegaApi();

        outMetrics = context.getResources().getDisplayMetrics();
    }

    // View Holder
    public class ViewHolderMultipleBucket extends RecyclerView.ViewHolder {
        private LinearLayout multipleBucketLayout;
        private long document;
        private RelativeLayout mediaView;
        private SimpleDraweeView thumbnailMedia;
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
                params.width = params.height = dp2px(48, outMetrics);
                int margin = dp2px(12, outMetrics);
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
        node.setUiDirty(false);
        holder.document = megaNode.getHandle();

        Bitmap thumbnail = getThumbnailFromCache(megaNode);
        if (thumbnail == null) {
            thumbnail = getThumbnailFromFolder(megaNode, context);
            if (thumbnail == null) {
                try {
                    if (megaNode.hasThumbnail() || isMedia) {
                        thumbnail = getThumbnailFromMegaList(megaNode, context, holder, megaApi, this);
                    } else {
                        createThumbnailList(context, megaNode, holder, megaApi, this);
                    }
                } catch (Exception e) {
                    Timber.e(e, "Error getting or creating megaNode thumbnail");
                }
            }
        }

        if (isMedia) {
            holder.mediaView.setVisibility(View.VISIBLE);
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

            int size;
            if (isScreenInPortrait(context)) {
                size = outMetrics.widthPixels / 4;
            } else {
                size = outMetrics.widthPixels / 6;
            }
            size -= dp2px(2, outMetrics);

            holder.thumbnailMedia.getLayoutParams().width = size;
            holder.thumbnailMedia.getLayoutParams().height = size;

            if (thumbnail != null) {
                ImageRequest request =
                        ImageRequestBuilder.newBuilderWithSource(Uri.fromFile(node.getThumbnail())).build();
                AbstractDraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setImageRequest(request)
                        .setOldController(holder.thumbnailMedia.getController())
                        .build();
                holder.thumbnailMedia.setController(controller);
            } else {
                holder.thumbnailMedia.setActualImageResource(R.drawable.ic_image_thumbnail);
            }

            if (node.getSelected()) {
                holder.selectedIcon.setVisibility(View.VISIBLE);
                holder.thumbnailMedia.getHierarchy().setRoundingParams(
                    RoundingParams.fromCornersRadius((float) context.getResources().getDimensionPixelSize(
                        R.dimen.cu_fragment_selected_round_corner_radius
                    ))
                );
                holder.thumbnailMedia.setBackground(ContextCompat.getDrawable(
                        holder.thumbnailMedia.getContext(),
                        R.drawable.background_item_grid_selected
                ));
            } else {
                holder.selectedIcon.setVisibility(View.GONE);
                holder.thumbnailMedia.getHierarchy().setRoundingParams(
                        RoundingParams.fromCornersRadius(0f)
                );
                holder.thumbnailMedia.setBackground(null);
            }
        } else {
            holder.mediaView.setVisibility(View.GONE);
            holder.listView.setVisibility(View.VISIBLE);
            holder.nameText.setText(megaNode.getName());
            holder.infoText.setText(getSizeString(megaNode.getSize()) + " · " + formatTime(megaNode.getCreationTime()));

            holder.thumbnailList.setVisibility(View.VISIBLE);

            if (megaNode.getLabel() != MegaNode.NODE_LBL_UNKNOWN) {
                Drawable drawable = MegaNodeUtil.getNodeLabelDrawable(megaNode.getLabel(), holder.itemView.getResources());
                holder.imgLabel.setImageDrawable(drawable);
                holder.imgLabel.setVisibility(View.VISIBLE);
            } else {
                holder.imgLabel.setVisibility(View.GONE);
            }

            holder.imgFavourite.setVisibility(megaNode.isFavourite() ? View.VISIBLE : View.GONE);

            if (thumbnail != null) {
                holder.setImageThumbnail(thumbnail);
            } else {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.thumbnailList.getLayoutParams();
                params.width = params.height = dp2px(48, outMetrics);
                int margin = dp2px(12, outMetrics);
                params.setMargins(margin, margin, margin, 0);
                holder.thumbnailList.setLayoutParams(params);
                holder.thumbnailList.setImageResource(MimeTypeList.typeForName(megaNode.getName()).getIconResourceId());
            }

            if (node.getSelected()) {
                holder.thumbnailList.setImageResource(R.drawable.ic_select_folder);
            } else {
                holder.thumbnailList.setImageDrawable(null);
                int placeHolderRes = MimeTypeList.typeForName(node.getNode().getName()).getIconResourceId();

                if (node.getThumbnail() != null) {
                    holder.thumbnailList.setImageURI(Uri.fromFile(node.getThumbnail()));
                } else {
                    int imgResource;
                    if (megaNode.isFolder()) {
                        imgResource = R.drawable.ic_folder_list;
                    } else {
                        imgResource = placeHolderRes;
                    }
                    holder.thumbnailList.setImageResource(imgResource);
                }
            }
        }
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

    @Override
    public void onClick(View v) {
        Timber.d("onClick");
        MultipleBucketAdapter.ViewHolderMultipleBucket holder = (MultipleBucketAdapter.ViewHolderMultipleBucket) v.getTag();
        if (holder == null) return;

        NodeItem node = getItemAtPosition(holder.getAbsoluteAdapterPosition());
        if (node == null) return;
        switch (v.getId()) {
            case R.id.three_dots: {
                if (!isOnline(context)) {
                    ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                    break;
                }
                ((ManagerActivity) context).showNodeOptionsPanel(node.getNode(), RECENTS_MODE);
                break;
            }
            case R.id.multiple_bucket_layout: {
                if (fragment instanceof RecentsBucketFragment) {
                    ((RecentsBucketFragment) fragment).handleItemClick(holder.getAdapterPosition(), node, true);
                }
                break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onLongClick(View v) {
        Timber.d("onClick");
        MultipleBucketAdapter.ViewHolderMultipleBucket holder = (MultipleBucketAdapter.ViewHolderMultipleBucket) v.getTag();
        if (holder == null) return false;

        NodeItem node = getItemAtPosition(holder.getAbsoluteAdapterPosition());
        if (node == null) return false;

        if (fragment instanceof RecentsBucketFragment) {
            ((RecentsBucketFragment) fragment).onNodeLongClicked(
                    holder.getAdapterPosition(),
                    node
            );
        }

        return true;
    }

    @Override
    public String getSectionTitle(int position) {
        NodeItem node = getItemAtPosition(position);
        if (node == null) return "";

        String name = node.getNode().getName();
        if (!isTextEmpty(name)) return name.substring(0, 1);

        return "";
    }
}
