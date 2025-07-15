package mega.privacy.android.app.uploadFolder.list.adapter

import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil3.asImage
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.databinding.ItemFolderContentBinding
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * RecyclerView's ViewHolder to show FolderContent Data info in a list view.
 *
 * @property binding    Item's view binding
 */
class FolderContentListHolder(
    private val binding: ItemFolderContentBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: FolderContent.Data) {
        binding.apply {
            val isSelected = item.isSelected
            selectedIcon.isVisible = isSelected

            CoilUtils.dispose(thumbnail)
            thumbnail.apply {
                isVisible = !isSelected

                if (!isSelected) {
                    if (item.isFolder) {
                        setImageURI(null as Uri?)
                        setImageResource(IconPackR.drawable.ic_folder_medium_solid)
                    } else {
                        if (item.isSelected) {
                            setImageResource(CoreUiR.drawable.ic_select_folder)
                        } else {
                            load(item.uri) {
                                val iconResId = MimeTypeList.typeForName(item.name).iconResourceId
                                val placeholder = ContextCompat.getDrawable(
                                    binding.root.context,
                                    iconResId
                                )?.asImage()
                                placeholder(placeholder)
                                transformations(
                                    RoundedCornersTransformation(
                                        dp2px(THUMB_CORNER_RADIUS_DP).toFloat()
                                    )
                                )
                                listener(
                                    onError = { _, _ ->
                                        setImageResource(iconResId)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            name.text = item.name
            fileInfo.text = if (item.isFolder) {
                TextUtil.getFolderInfo(
                    item.numberOfFolders,
                    item.numberOfFiles,
                    binding.root.context
                );
            } else {
                TextUtil.getFileInfo(
                    Util.getSizeString(item.size, binding.root.context),
                    TimeUtils.formatLongDateTime(item.lastModified / 1000)
                )
            }
        }
    }
}