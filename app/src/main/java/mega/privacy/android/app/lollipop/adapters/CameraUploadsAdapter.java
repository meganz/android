package mega.privacy.android.app.lollipop.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;
import java.util.ArrayList;
import java.util.List;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding;
import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;
import mega.privacy.android.app.lollipop.managerSections.cu.CuNode;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;

public class CameraUploadsAdapter extends RecyclerView.Adapter<CameraUploadsAdapter.CuViewHolder>
    implements SectionTitleProvider {

  private final Listener listener;
  private final List<CuNode> nodes = new ArrayList<>();
  private final int spanCount;
  private final ItemSizeConfig itemSizeConfig;

  public CameraUploadsAdapter(Listener listener, int spanCount, ItemSizeConfig itemSizeConfig) {
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
            inflater.inflate(R.layout.item_camera_uploads_title, parent, false));
      case CuNode.TYPE_VIDEO:
        return new CuVideoViewHolder(
            inflater.inflate(R.layout.item_camera_uploads_video, parent, false), itemSizeConfig);
      case CuNode.TYPE_IMAGE:
      default:
        return new CuImageViewHolder(
            inflater.inflate(R.layout.item_camera_uploads_image, parent, false), itemSizeConfig);
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
      thumbnail = ((CuImageViewHolder) holder).binding.thumbnail;
    } else if (holder instanceof CuVideoViewHolder) {
      thumbnail = ((CuVideoViewHolder) holder).binding.thumbnail;
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
      ((CuImageViewHolder) holder).binding.thumbnail.setVisibility(visibility);
    } else if (holder instanceof CuVideoViewHolder) {
      ((CuVideoViewHolder) holder).binding.thumbnail.setVisibility(visibility);
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
      showSelectionAnimation(((CuImageViewHolder) holder).binding.icSelected, position,
          node.isSelected());
    } else if (holder instanceof CuVideoViewHolder) {
      showSelectionAnimation(((CuVideoViewHolder) holder).binding.icSelected, position,
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

  public static class ItemSizeConfig {
    final boolean smallGrid;
    final int gridSize;
    final int gridMargin;
    final int icSelectedSize;
    final int icSelectedMargin;
    final int roundCornerRadius;
    final int selectedPadding;

    public ItemSizeConfig(boolean smallGrid, int gridSize, int gridMargin, int icSelectedSize,
        int icSelectedMargin, int roundCornerRadius, int selectedPadding) {
      this.smallGrid = smallGrid;
      this.gridSize = gridSize;
      this.gridMargin = gridMargin;
      this.icSelectedSize = icSelectedSize;
      this.icSelectedMargin = icSelectedMargin;
      this.roundCornerRadius = roundCornerRadius;
      this.selectedPadding = selectedPadding;
    }
  }

  static abstract class CuViewHolder extends RecyclerView.ViewHolder {

    public CuViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    void bind(int position, CuNode node, Listener listener) {
      if (handleClick() && node.getNode() != null) {
        itemView.setOnClickListener(v -> listener.onNodeClicked(position, node));
        itemView.setOnLongClickListener(v -> {
          listener.onNodeLongClicked(position, node);
          return true;
        });
      }
      bind(node);
    }

    protected abstract void bind(CuNode node);

    protected boolean handleClick() {
      return true;
    }

    static void setViewSize(View grid, View icSelected, ItemSizeConfig itemSizeConfig) {
      GridLayoutManager.LayoutParams params =
          (GridLayoutManager.LayoutParams) grid.getLayoutParams();
      params.width = itemSizeConfig.gridSize;
      params.height = itemSizeConfig.gridSize;
      params.topMargin = itemSizeConfig.gridMargin;
      params.bottomMargin = itemSizeConfig.gridMargin;
      params.setMarginStart(itemSizeConfig.gridMargin);
      params.setMarginEnd(itemSizeConfig.gridMargin);
      grid.setLayoutParams(params);

      FrameLayout.LayoutParams icSelectedParams =
          (FrameLayout.LayoutParams) icSelected.getLayoutParams();
      icSelectedParams.width = itemSizeConfig.icSelectedSize;
      icSelectedParams.height = itemSizeConfig.icSelectedSize;
      icSelectedParams.topMargin = itemSizeConfig.icSelectedMargin;
      icSelectedParams.setMarginStart(itemSizeConfig.icSelectedMargin);
      icSelected.setLayoutParams(icSelectedParams);
    }

    static void updateThumbnailDisplay(SimpleDraweeView thumbnail, CuNode node,
        ItemSizeConfig itemSizeConfig) {
      if (node.getThumbnail() != null) {
        thumbnail.setImageURI(Uri.fromFile(node.getThumbnail()));
      } else {
        thumbnail.setImageResource(R.drawable.ic_image_thumbnail);
      }

      thumbnail.getHierarchy()
          .setRoundingParams(RoundingParams.fromCornersRadius(
              node.isSelected() ? itemSizeConfig.roundCornerRadius : 0));

      int padding = node.isSelected() ? itemSizeConfig.selectedPadding : 0;
      thumbnail.setPadding(padding, padding, padding, padding);
    }
  }

  static class CuTitleViewHolder extends CuViewHolder {

    private final ItemCameraUploadsTitleBinding binding;

    public CuTitleViewHolder(@NonNull View itemView) {
      super(itemView);

      binding = ItemCameraUploadsTitleBinding.bind(itemView);
    }

    @Override protected void bind(CuNode node) {
      binding.gridTitle.setText(node.getModifyDate());
    }

    @Override protected boolean handleClick() {
      return false;
    }
  }

  static class CuImageViewHolder extends CuViewHolder {

    private final ItemCameraUploadsImageBinding binding;
    private final ItemSizeConfig itemSizeConfig;

    public CuImageViewHolder(@NonNull View itemView, ItemSizeConfig itemSizeConfig) {
      super(itemView);
      binding = ItemCameraUploadsImageBinding.bind(itemView);
      this.itemSizeConfig = itemSizeConfig;

      setViewSize(binding.getRoot(), binding.icSelected, itemSizeConfig);
    }

    @Override protected void bind(CuNode node) {
      updateThumbnailDisplay(binding.thumbnail, node, itemSizeConfig);

      binding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);
    }
  }

  static class CuVideoViewHolder extends CuViewHolder {

    private final ItemCameraUploadsVideoBinding binding;
    private final ItemSizeConfig itemSizeConfig;

    public CuVideoViewHolder(@NonNull View itemView, ItemSizeConfig itemSizeConfig) {
      super(itemView);
      binding = ItemCameraUploadsVideoBinding.bind(itemView);
      this.itemSizeConfig = itemSizeConfig;

      setViewSize(binding.getRoot(), binding.icSelected, itemSizeConfig);
    }

    @Override protected void bind(CuNode node) {
      updateThumbnailDisplay(binding.thumbnail, node, itemSizeConfig);

      if (itemSizeConfig.smallGrid) {
        binding.videoDuration.setVisibility(View.GONE);
      } else {
        binding.videoDuration.setVisibility(View.VISIBLE);
        if (node.getNode() != null) {
          binding.videoDuration.setText(getVideoDuration(node.getNode().getDuration()));
        } else {
          binding.videoDuration.setVisibility(View.GONE);
        }
      }
      binding.videoInfo.setBackgroundResource(
          node.isSelected() ? R.drawable.gradient_cam_uploads_rounded
              : R.drawable.gradient_cam_uploads);

      binding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);
    }
  }
}
