package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
class CuImageViewHolder extends CuViewHolder {

    private final ItemCameraUploadsImageBinding binding;
    private final CuItemSizeConfig itemSizeConfig;

    public CuImageViewHolder(ItemCameraUploadsImageBinding binding,
            CuItemSizeConfig itemSizeConfig) {
        super(binding.getRoot());
        this.binding = binding;
        this.itemSizeConfig = itemSizeConfig;

        setViewSize(binding.getRoot(), binding.icSelected, itemSizeConfig);
    }

    @Override protected void bind(CuNode node) {
        updateThumbnailDisplay(binding.thumbnail, node, itemSizeConfig);

        binding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);
    }

    public ItemCameraUploadsImageBinding binding() {
        return binding;
    }
}
