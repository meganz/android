package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;

import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;

class CuVideoViewHolder extends CuGridViewHolder {

    private final ItemCameraUploadsVideoBinding mBinding;
    private final CuItemSizeConfig mItemSizeConfig;

    public CuVideoViewHolder(ItemCameraUploadsVideoBinding binding,
            CuItemSizeConfig itemSizeConfig) {
        super(binding.getRoot());
        mBinding = binding;
        mItemSizeConfig = itemSizeConfig;

        setViewSize(binding.getRoot(), binding.icSelected, itemSizeConfig);
    }

    @Override protected void bind(CuNode node) {
        if (mItemSizeConfig.isSmallGrid()) {
            mBinding.videoDuration.setVisibility(View.GONE);
            mBinding.playIcon.setVisibility(View.VISIBLE);
        } else {
            mBinding.playIcon.setVisibility(View.GONE);

            if (node.getNode() != null) {
                mBinding.videoDuration.setVisibility(View.VISIBLE);
                mBinding.videoDuration.setText(getVideoDuration(node.getNode().getDuration()));
            } else {
                mBinding.videoDuration.setVisibility(View.GONE);
            }
        }

        mBinding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);

        updateThumbnailDisplay(mBinding.thumbnail, node, mItemSizeConfig);
    }

    public ItemCameraUploadsVideoBinding binding() {
        return mBinding;
    }
}
