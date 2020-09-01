package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding;
import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;

public class CameraUploadsAdapter extends RecyclerView.Adapter<CuViewHolder>
        implements SectionTitleProvider {

    private final Listener mListener;
    private final List<CuNode> mNodes = new ArrayList<>();
    private final int mSpanCount;
    private final CuItemSizeConfig mItemSizeConfig;

    public CameraUploadsAdapter(Listener listener, int spanCount, CuItemSizeConfig itemSizeConfig) {
        mListener = listener;
        mSpanCount = spanCount;
        mItemSizeConfig = itemSizeConfig;
    }

    @Override public long getItemId(int position) {
        switch (getItemViewType(position)) {
            case CuNode.TYPE_TITLE:
                return mNodes.get(position).getModifyDate().hashCode();
            case CuNode.TYPE_IMAGE:
            case CuNode.TYPE_VIDEO:
            default:
                return mNodes.get(position).getNode().getHandle();
        }
    }

    @Override public int getItemViewType(int position) {
        return mNodes.get(position).getType();
    }

    @NonNull @Override
    public CuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case CuNode.TYPE_TITLE:
                return new CuTitleViewHolder(
                        ItemCameraUploadsTitleBinding.inflate(inflater, parent, false));
            case CuNode.TYPE_VIDEO:
                return new CuVideoViewHolder(
                        ItemCameraUploadsVideoBinding.inflate(inflater, parent, false),
                        mItemSizeConfig);
            case CuNode.TYPE_IMAGE:
            default:
                return new CuImageViewHolder(
                        ItemCameraUploadsImageBinding.inflate(inflater, parent, false),
                        mItemSizeConfig);
        }
    }

    @Override public void onBindViewHolder(@NonNull CuViewHolder holder, int position) {
        holder.bind(position, mNodes.get(position), mListener);
    }

    @Override public int getItemCount() {
        return mNodes.size();
    }

    public void setNodes(List<CuNode> nodes) {
        this.mNodes.clear();
        this.mNodes.addAll(nodes);
        notifyDataSetChanged();
    }

    public int getSpanSize(int position) {
        if (position < 0 || position >= mNodes.size()) {
            return 1;
        }
        switch (mNodes.get(position).getType()) {
            case CuNode.TYPE_TITLE:
                return mSpanCount;
            case CuNode.TYPE_IMAGE:
            case CuNode.TYPE_VIDEO:
            default:
                return 1;
        }
    }

    public int[] getThumbnailLocationOnScreen(RecyclerView.ViewHolder holder) {
        View thumbnail = null;
        if (holder instanceof CuImageViewHolder) {
            thumbnail = ((CuImageViewHolder) holder).binding().thumbnail;
        } else if (holder instanceof CuVideoViewHolder) {
            thumbnail = ((CuVideoViewHolder) holder).binding().thumbnail;
        }

        if (thumbnail == null) {
            return null;
        }

        int[] topLeft = new int[2];
        thumbnail.getLocationOnScreen(topLeft);

        return new int[] {
                topLeft[0], topLeft[1], thumbnail.getWidth(), thumbnail.getHeight()
        };
    }

    public void setThumbnailVisibility(RecyclerView.ViewHolder holder, int visibility) {
        if (holder instanceof CuImageViewHolder) {
            ((CuImageViewHolder) holder).binding().thumbnail.setVisibility(visibility);
        } else if (holder instanceof CuVideoViewHolder) {
            ((CuVideoViewHolder) holder).binding().thumbnail.setVisibility(visibility);
        }
    }

    public int getNodePosition(long handle) {
        for (int i = 0, n = mNodes.size(); i < n; i++) {
            MegaNode node = mNodes.get(i).getNode();
            if (node != null && node.getHandle() == handle) {
                return i;
            }
        }

        return INVALID_POSITION;
    }

    public void showSelectionAnimation(int position, CuNode node, RecyclerView.ViewHolder holder) {
        if (holder == null || position < 0 || position >= mNodes.size()
                || mNodes.get(position).getNode() == null
                || mNodes.get(position).getNode().getHandle() != node.getNode().getHandle()) {
            return;
        }

        if (holder instanceof CuImageViewHolder) {
            showSelectionAnimation(((CuImageViewHolder) holder).binding().icSelected, position,
                    node.isSelected());
        } else if (holder instanceof CuVideoViewHolder) {
            showSelectionAnimation(((CuVideoViewHolder) holder).binding().icSelected, position,
                    node.isSelected());
        }
    }

    private void showSelectionAnimation(View view, int position, boolean showing) {
        if (showing) {
            view.setVisibility(View.VISIBLE);
        }
        Animation flipAnimation = AnimationUtils.loadAnimation(view.getContext(),
                R.anim.multiselect_flip);
        flipAnimation.setDuration(200);
        flipAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {
            }

            @Override public void onAnimationEnd(Animation animation) {
                if (!showing) {
                    view.setVisibility(View.GONE);
                }
                notifyItemChanged(position);
            }

            @Override public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(flipAnimation);
    }

    @Override public String getSectionTitle(int position) {
        if (position < 0 || position >= mNodes.size()) {
            return "";
        }
        return mNodes.get(position).getModifyDate();
    }

    public interface Listener {
        void onNodeClicked(int position, CuNode node);

        void onNodeLongClicked(int position, CuNode node);
    }
}
