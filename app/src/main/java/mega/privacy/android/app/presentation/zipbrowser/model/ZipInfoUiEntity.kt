package mega.privacy.android.app.presentation.zipbrowser.model

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType

/**
 * The zip info ui entity
 *
 * @property icon zip file entry icon
 * @property name zip file entry name
 * @property path zip file entry path
 * @property info zip file entry info
 * @property zipEntryType ZipEntryType
 */
data class ZipInfoUiEntity(
    @DrawableRes val icon: Int,
    val name: String,
    val path: String,
    val info: String,
    val zipEntryType: ZipEntryType
)