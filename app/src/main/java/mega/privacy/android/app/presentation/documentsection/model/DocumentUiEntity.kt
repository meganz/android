package mega.privacy.android.app.presentation.documentsection.model

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * The entity for the document is displayed in documents section
 *
 * @property id NodeId
 * @property name the document's name
 * @property size the document's size
 * @property thumbnail the document's thumbnail
 * @property icon the document's icon
 * @property fileTypeInfo the document's file type info
 * @property isFavourite the document if is Favourite
 * @property isExported the document if is Exported
 * @property isTakenDown the document if is TakenDown
 * @property hasVersions the document if has Versions
 * @property modificationTime the document's modification time
 * @property label the document's label
 * @property nodeAvailableOffline the document if is available for offline
 * @property isSelected the document if is selected
 */
data class DocumentUiEntity(
    val id: NodeId,
    val name: String,
    val size: Long,
    val thumbnail: File? = null,
    @DrawableRes val icon: Int,
    val fileTypeInfo: FileTypeInfo,
    val isFavourite: Boolean = false,
    val isExported: Boolean = false,
    val isTakenDown: Boolean = false,
    val hasVersions: Boolean = false,
    val modificationTime: Long,
    val label: Int,
    val nodeAvailableOffline: Boolean = false,
    val isSelected: Boolean = false,
)
