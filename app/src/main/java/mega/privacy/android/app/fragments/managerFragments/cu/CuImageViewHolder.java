package mega.privacy.android.app.fragments.managerFragments.cu;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
class CuImageViewHolder extends CuViewHolder {

    private final ItemCameraUploadsImageBinding mBinding;
    private final CuItemSizeConfig mItemSizeConfig;

    public CuImageViewHolder(ItemCameraUploadsImageBinding binding,
            CuItemSizeConfig itemSizeConfig) {
        super(binding.getRoot());
        mBinding = binding;
        mItemSizeConfig = itemSizeConfig;

        setViewSize(binding.getRoot(), binding.icSelected, itemSizeConfig);
    }

    @Override protected void bind(CuNode node, RequestManager requestManager) {
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

    public ItemCameraUploadsImageBinding binding() {
        return mBinding;
    }
}
