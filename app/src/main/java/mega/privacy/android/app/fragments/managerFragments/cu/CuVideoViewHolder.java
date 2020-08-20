package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;

import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;

class CuVideoViewHolder extends CuViewHolder {

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
        } else {
            mBinding.videoDuration.setVisibility(View.VISIBLE);
            if (node.getNode() != null) {
                mBinding.videoDuration.setText(getVideoDuration(node.getNode().getDuration()));
            } else {
                mBinding.videoDuration.setVisibility(View.GONE);
            }
        }
        mBinding.videoInfo.setBackgroundResource(
                node.isSelected() ? R.drawable.gradient_cam_uploads_rounded
                        : R.drawable.gradient_cam_uploads);

        mBinding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);

        updateThumbnailDisplay(mBinding.thumbnail, node, mItemSizeConfig);
    }

    public ItemCameraUploadsVideoBinding binding() {
        return mBinding;
    }
}
