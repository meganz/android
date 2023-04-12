package mega.privacy.android.data.mapper.recentactions

import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import java.io.File
import javax.inject.Inject

/**
 * The mapper class for converting [MegaRecentActionBucket] to [RecentActionBucket]
 */
internal class RecentActionBucketMapper @Inject constructor(private val nodeMapper: NodeMapper) {
    suspend operator fun invoke(
        megaRecentActionBucket: MegaRecentActionBucket,
        thumbnailPath: suspend () -> File?,
        previewPath: suspend () -> File?,
        fullSizePath: suspend () -> File?,
        hasVersion: suspend (MegaNode) -> Boolean,
        numberOfChildFolders: suspend (MegaNode) -> Int,
        numberOfChildFiles: suspend (MegaNode) -> Int,
        fileTypeInfoMapper: FileTypeInfoMapper,
        isPendingShare: suspend (MegaNode) -> Boolean,
        isInRubbish: suspend (MegaNode) -> Boolean
    ) = RecentActionBucketUnTyped(
        timestamp = megaRecentActionBucket.timestamp,
        userEmail = megaRecentActionBucket.userEmail,
        parentHandle = megaRecentActionBucket.parentHandle,
        isUpdate = megaRecentActionBucket.isUpdate,
        isMedia = megaRecentActionBucket.isMedia,
        nodes = (0 until megaRecentActionBucket.nodes.size()).map {
            nodeMapper(
                megaRecentActionBucket.nodes.get(it),
                thumbnailPath,
                previewPath,
                fullSizePath,
                hasVersion,
                numberOfChildFolders,
                numberOfChildFiles,
                fileTypeInfoMapper,
                isPendingShare,
                isInRubbish,
            )
        },
    )
}