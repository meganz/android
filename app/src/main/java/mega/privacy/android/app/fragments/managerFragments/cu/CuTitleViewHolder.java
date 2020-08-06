package mega.privacy.android.app.fragments.managerFragments.cu;

import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding;

class CuTitleViewHolder extends CuViewHolder {

    private final ItemCameraUploadsTitleBinding mBinding;

    public CuTitleViewHolder(ItemCameraUploadsTitleBinding binding) {
        super(binding.getRoot());

        mBinding = binding;
    }

    @Override protected void bind(CuNode node) {
        mBinding.gridTitle.setText(node.getModifyDate());
    }

    @Override protected boolean handleClick() {
        return false;
    }
}
