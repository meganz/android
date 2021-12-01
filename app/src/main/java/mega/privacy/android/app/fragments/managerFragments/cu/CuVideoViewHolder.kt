package mega.privacy.android.app.fragments.managerFragments.cu

import android.view.View
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemCameraUploadsVideoBinding
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_DEFAULT
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_IN_1X
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_1X

class CuVideoViewHolder(
    private val mBinding: ItemCameraUploadsVideoBinding,
    private val mItemSizeConfig: GalleryItemSizeConfig
) : CuGridViewHolder(mBinding.root) {

    init {
        setViewSize(mBinding.root, mBinding.icSelected, mItemSizeConfig)
    }

    override fun bind(node: GalleryItem) {
        when (mItemSizeConfig.zoom) {
            ZOOM_IN_1X, ZOOM_DEFAULT -> {
                mBinding.playIcon.visibility = View.GONE
                if (node.node != null) {
                    mBinding.videoDuration.visibility = View.VISIBLE
                    mBinding.videoDuration.text = TimeUtils.getVideoDuration(
                        node.node!!.duration
                    )
                } else {
                    mBinding.videoDuration.visibility = View.GONE
                }
            }
            ZOOM_OUT_1X -> {
                mBinding.videoDuration.visibility = View.GONE
                mBinding.playIcon.visibility = View.VISIBLE
            }
            else -> {
                mBinding.videoDuration.visibility = View.GONE
                mBinding.playIcon.visibility = View.GONE
            }
        }

        mBinding.videoInfo.setBackgroundResource(
            if (node.isSelected) R.drawable.grid_cam_uploads_rounded else R.color.grey_alpha_032
        )

        mBinding.icSelected.visibility = if (node.isSelected) View.VISIBLE else View.GONE

        updateThumbnailDisplay(mBinding.thumbnail, node, mItemSizeConfig)
    }

    fun binding() = mBinding
}