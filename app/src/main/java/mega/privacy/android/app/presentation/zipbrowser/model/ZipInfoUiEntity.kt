package mega.privacy.android.app.presentation.zipbrowser.model

import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType

/**
 * The zip info ui entity
 *
 * @property name zip file entry name
 * @property path zip file entry path
 * @property info zip file entry info
 * @property zipEntryType ZipEntryType
 */
data class ZipInfoUiEntity(
    val name: String,
    val path: String,
    val info: String,
    val zipEntryType: ZipEntryType
)