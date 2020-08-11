package mega.privacy.android.app.fragments.managerFragments.cu;

import com.bumptech.glide.RequestManager;

import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
class CuTitleViewHolder extends CuViewHolder {

    private final ItemCameraUploadsTitleBinding mBinding;

    public CuTitleViewHolder(ItemCameraUploadsTitleBinding binding) {
        super(binding.getRoot());

        mBinding = binding;
    }

    @Override protected void bind(CuNode node, RequestManager requestManager) {
        mBinding.gridTitle.setText(node.getModifyDate());
    }

    @Override protected boolean handleClick() {
        return false;
    }
}
