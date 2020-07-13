package mega.privacy.android.app.lollipop.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding;
import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;
import mega.privacy.android.app.lollipop.managerSections.cu.CuNode;

import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;

public class CameraUploadsAdapter extends RecyclerView.Adapter<CameraUploadsAdapter.CuViewHolder>
    implements SectionTitleProvider {

  private final List<CuNode> nodes = new ArrayList<>();
  private final int spanCount;
  private final boolean smallGrid;
  private final int gridWidth;
  private final int gridMargin;

  public CameraUploadsAdapter(int spanCount, boolean smallGrid, int gridWidth, int gridMargin) {
    this.spanCount = spanCount;
    this.smallGrid = smallGrid;
    this.gridWidth = gridWidth;
    this.gridMargin = gridMargin;
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
            inflater.inflate(R.layout.item_camera_uploads_video, parent, false),
            smallGrid, gridWidth, gridMargin);
      case CuNode.TYPE_IMAGE:
      default:
        return new CuImageViewHolder(
            inflater.inflate(R.layout.item_camera_uploads_image, parent, false),
            gridWidth, gridMargin);
    }
  }

  @Override public void onBindViewHolder(@NonNull CuViewHolder holder, int position) {
    holder.bind(nodes.get(position));
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

  @Override public String getSectionTitle(int position) {
    if (position < 0 || position >= nodes.size()) {
      return "";
    }
    return nodes.get(position).getModifyDate();
  }

  static abstract class CuViewHolder extends RecyclerView.ViewHolder {

    public CuViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    protected abstract void bind(CuNode node);
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
  }

  static class CuImageViewHolder extends CuViewHolder {

    private final ItemCameraUploadsImageBinding binding;

    public CuImageViewHolder(@NonNull View itemView, int gridWidth, int gridMargin) {
      super(itemView);
      binding = ItemCameraUploadsImageBinding.bind(itemView);

      GridLayoutManager.LayoutParams params =
          (GridLayoutManager.LayoutParams) binding.getRoot().getLayoutParams();
      params.width = gridWidth;
      params.height = gridWidth;
      params.topMargin = gridMargin;
      params.bottomMargin = gridMargin;
      params.leftMargin = gridMargin;
      params.rightMargin = gridMargin;
      binding.getRoot().setLayoutParams(params);
    }

    @Override protected void bind(CuNode node) {
      if (node.getThumbnail() != null) {
        binding.thumbnail.setImageURI(Uri.fromFile(node.getThumbnail()));
      } else {
        binding.thumbnail.setImageResource(R.drawable.ic_image_thumbnail);
      }
    }
  }

  static class CuVideoViewHolder extends CuViewHolder {

    private final ItemCameraUploadsVideoBinding binding;
    private final boolean smallGrid;

    public CuVideoViewHolder(@NonNull View itemView, boolean smallGrid, int gridWidth,
        int gridMargin) {
      super(itemView);
      binding = ItemCameraUploadsVideoBinding.bind(itemView);
      this.smallGrid = smallGrid;

      GridLayoutManager.LayoutParams params =
          (GridLayoutManager.LayoutParams) binding.getRoot().getLayoutParams();
      params.width = gridWidth;
      params.height = gridWidth;
      params.topMargin = gridMargin;
      params.bottomMargin = gridMargin;
      params.leftMargin = gridMargin;
      params.rightMargin = gridMargin;
      binding.getRoot().setLayoutParams(params);
    }

    @Override protected void bind(CuNode node) {
      if (node.getThumbnail() != null) {
        binding.thumbnail.setImageURI(Uri.fromFile(node.getThumbnail()));
      } else {
        binding.thumbnail.setImageResource(R.drawable.ic_image_thumbnail);
      }

      if (smallGrid) {
        binding.videoDuration.setVisibility(View.GONE);
      } else {
        binding.videoDuration.setVisibility(View.VISIBLE);
        binding.videoDuration.setText(getVideoDuration(node.getNode().getDuration()));
      }
    }
  }
}
