package mega.privacy.android.app.presentation.documentsection.model

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.sync.ui.mapper.FileTypeIconMapper
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert the TypedFileNode to DocumentUIEntity
 */
class DocumentUiEntityMapper @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
) {

    /**
     * Convert the TypedFileNode to DocumentUIEntity
     *
     * @param typedFileNode TypedFileNode
     * @return DocumentUiEntity
     */
    operator fun invoke(
        typedFileNode: TypedFileNode,
    ) = DocumentUiEntity(
        id = typedFileNode.id,
        name = typedFileNode.name,
        size = typedFileNode.size,
        thumbnail = typedFileNode.thumbnailPath?.let { File(it) },
        icon = fileTypeIconMapper(typedFileNode.type.extension),
        fileTypeInfo = typedFileNode.type,
        isFavourite = typedFileNode.isFavourite,
        isExported = typedFileNode.exportedData != null,
        isTakenDown = typedFileNode.isTakenDown,
        hasVersions = typedFileNode.hasVersion,
        modificationTime = typedFileNode.modificationTime,
        label = typedFileNode.label,
        nodeAvailableOffline = typedFileNode.isAvailableOffline
    )
}