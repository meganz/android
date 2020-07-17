package mega.privacy.android.app.adapters;

import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding;
import mega.privacy.android.app.fragments.managerFragments.cu.CuNode;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
class CuTitleViewHolder extends CuViewHolder {

  private final ItemCameraUploadsTitleBinding binding;

  public CuTitleViewHolder(ItemCameraUploadsTitleBinding binding) {
    super(binding.getRoot());

    this.binding = binding;
  }

  @Override protected void bind(CuNode node) {
    binding.gridTitle.setText(node.getModifyDate());
  }

  @Override protected boolean handleClick() {
    return false;
  }
}
