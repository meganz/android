package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;

import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
class CuVideoViewHolder extends CuViewHolder {

    private final ItemCameraUploadsVideoBinding binding;
    private final CuItemSizeConfig itemSizeConfig;

    public CuVideoViewHolder(ItemCameraUploadsVideoBinding binding,
            CuItemSizeConfig itemSizeConfig) {
        super(binding.getRoot());
        this.binding = binding;
        this.itemSizeConfig = itemSizeConfig;

        setViewSize(binding.getRoot(), binding.icSelected, itemSizeConfig);
    }

    @Override protected void bind(CuNode node) {
        updateThumbnailDisplay(binding.thumbnail, node, itemSizeConfig);

        if (itemSizeConfig.isSmallGrid()) {
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

    public ItemCameraUploadsVideoBinding binding() {
        return binding;
    }
}
