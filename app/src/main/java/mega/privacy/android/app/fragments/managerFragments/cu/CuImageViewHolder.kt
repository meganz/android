package mega.privacy.android.app.fragments.managerFragments.cu

import android.view.View
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig

class CuImageViewHolder(
    private val mBinding: ItemCameraUploadsImageBinding,
    private val mItemSizeConfig: GalleryItemSizeConfig
) : CuGridViewHolder(mBinding.root) {

    init {
        setViewSize(mBinding.root, mBinding.icSelected, mItemSizeConfig)
    }

    override fun bind(node: CuNode) {
        mBinding.icSelected.visibility = if (node.isSelected) View.VISIBLE else View.GONE

        updateThumbnailDisplay(
            mBinding.thumbnail,
            node,
            mItemSizeConfig
        )
    }

    fun binding() = mBinding
}