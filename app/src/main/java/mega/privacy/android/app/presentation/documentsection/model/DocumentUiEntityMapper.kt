package mega.privacy.android.app.presentation.documentsection.model

import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.shares.AccessPermission
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
        accessPermission: AccessPermission,
        canBeMovedToRubbishBin: Boolean,
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
        nodeAvailableOffline = typedFileNode.isAvailableOffline,
        isMarkedSensitive = typedFileNode.isMarkedSensitive,
        isSensitiveInherited = typedFileNode.isSensitiveInherited,
        isIncomingShare = typedFileNode.isIncomingShare,
        accessPermission = accessPermission,
        canBeMovedToRubbishBin = canBeMovedToRubbishBin
    )
}