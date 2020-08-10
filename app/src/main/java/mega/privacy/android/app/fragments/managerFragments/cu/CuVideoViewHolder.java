package mega.privacy.android.app.fragments.managerFragments.cu;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding;

import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
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

    @Override protected void bind(CuNode node, RequestManager requestManager) {
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

        RequestBuilder<Drawable> request = requestManager.load(node.getThumbnail())
                .error(R.drawable.ic_image_thumbnail)
                .transition(DrawableTransitionOptions.withCrossFade());

        if (node.isSelected()) {
            request.transform(new RoundedCorners(mItemSizeConfig.getRoundCornerRadius()));
        } else {
            request.placeholder(R.drawable.ic_image_thumbnail);
        }

        request.into(mBinding.thumbnail);
    }

    public ItemCameraUploadsVideoBinding binding() {
        return mBinding;
    }
}
