package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;

import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;
import static mega.privacy.android.app.utils.ZoomUtil.*;

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
        switch (mItemSizeConfig.getZoom()) {
            case ZOOM_IN_1X:
            case ZOOM_DEFAULT  :
                mBinding.playIcon.setVisibility(View.GONE);

                if (node.getNode() != null) {
                    mBinding.videoDuration.setVisibility(View.VISIBLE);
                    mBinding.videoDuration.setText(getVideoDuration(node.getNode().getDuration()));
                } else {
                    mBinding.videoDuration.setVisibility(View.GONE);
                }

                break;
            case ZOOM_OUT_1X:
                mBinding.videoDuration.setVisibility(View.GONE);
                mBinding.playIcon.setVisibility(View.VISIBLE);

                break;
            default:
                mBinding.videoDuration.setVisibility(View.GONE);
                mBinding.playIcon.setVisibility(View.GONE);
        }

        mBinding.videoInfo.setBackgroundResource(
                node.isSelected() ? R.drawable.grid_cam_uploads_rounded
                        : R.color.grey_alpha_032);


        mBinding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);

        updateThumbnailDisplay(mBinding.thumbnail, node, mItemSizeConfig);
    }

    public ItemCameraUploadsVideoBinding binding() {
        return mBinding;
    }
}
