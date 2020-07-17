package mega.privacy.android.app.adapters;

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
import mega.privacy.android.app.fragments.managerFragments.cu.CuItemSizeConfig;
import mega.privacy.android.app.fragments.managerFragments.cu.CuNode;
import nz.mega.sdk.MegaNode;

public class CameraUploadsAdapter extends RecyclerView.Adapter<CuViewHolder>
    implements SectionTitleProvider {

  private final Listener listener;
  private final List<CuNode> nodes = new ArrayList<>();
  private final int spanCount;
  private final CuItemSizeConfig itemSizeConfig;

  public CameraUploadsAdapter(Listener listener, int spanCount, CuItemSizeConfig itemSizeConfig) {
    this.listener = listener;
    this.spanCount = spanCount;
    this.itemSizeConfig = itemSizeConfig;
  }

  @Override public int getItemViewType(int position) {
    return nodes.get(position).getType();
  }

  @NonNull @Override
  public CuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    switch (viewType) {
      case CuNode.TYPE_TITLE:
        return new CuTitleViewHolder(
            ItemCameraUploadsTitleBinding.inflate(inflater, parent, false));
      case CuNode.TYPE_VIDEO:
        return new CuVideoViewHolder(ItemCameraUploadsVideoBinding.inflate(inflater, parent, false),
            itemSizeConfig);
      case CuNode.TYPE_IMAGE:
      default:
        return new CuImageViewHolder(ItemCameraUploadsImageBinding.inflate(inflater, parent, false),
            itemSizeConfig);
    }
  }

  @Override public void onBindViewHolder(@NonNull CuViewHolder holder, int position) {
    holder.bind(position, nodes.get(position), listener);
  }

  @Override public int getItemCount() {
    return nodes.size();
  }

  public void setNodes(List<CuNode> nodes) {
    this.nodes.clear();
    this.nodes.addAll(nodes);
    notifyDataSetChanged();
  }

  public int getSpanSize(int position) {
    if (position < 0 || position >= nodes.size()) {
      return 1;
    }
    switch (nodes.get(position).getType()) {
      case CuNode.TYPE_TITLE:
        return spanCount;
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
    for (int i = 0, n = nodes.size(); i < n; i++) {
      MegaNode node = nodes.get(i).getNode();
      if (node != null && node.getHandle() == handle) {
        return i;
      }
    }

    return -1;
  }

  public void showSelectionAnimation(int position, CuNode node, RecyclerView.ViewHolder holder) {
    if (holder == null || position < 0 || position >= nodes.size()
        || nodes.get(position).getNode() == null
        || nodes.get(position).getNode().getHandle() != node.getNode().getHandle()) {
      return;
    }

    notifyItemChanged(position);

    if (holder instanceof CuImageViewHolder) {
      showSelectionAnimation(((CuImageViewHolder) holder).binding().icSelected, position,
          node.isSelected());
    } else if (holder instanceof CuVideoViewHolder) {
      showSelectionAnimation(((CuVideoViewHolder) holder).binding().icSelected, position,
          node.isSelected());
    }
  }

  private void showSelectionAnimation(View view, int position, boolean showing) {
    Animation flipAnimation = AnimationUtils.loadAnimation(view.getContext(),
        R.anim.multiselect_flip);
    flipAnimation.setDuration(200);
    flipAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override public void onAnimationStart(Animation animation) {
        if (showing) {
          notifyItemChanged(position);
        }
      }

      @Override public void onAnimationEnd(Animation animation) {
        if (!showing) {
          notifyItemChanged(position);
        }
      }

      @Override public void onAnimationRepeat(Animation animation) {
      }
    });
    view.startAnimation(flipAnimation);
  }

  @Override public String getSectionTitle(int position) {
    if (position < 0 || position >= nodes.size()) {
      return "";
    }
    return nodes.get(position).getModifyDate();
  }

  public interface Listener {
    void onNodeClicked(int position, CuNode node);

    void onNodeLongClicked(int position, CuNode node);
  }
}
