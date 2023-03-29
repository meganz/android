package mega.privacy.android.data.mapper.recentactions

import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

/**
 * Implementation of [RecentActionBucketMapper].
 */
internal class RecentActionBucketMapperImpl @Inject constructor(private val nodeMapper: NodeMapper) : RecentActionBucketMapper {
    override suspend fun invoke(
        megaRecentActionBucket: MegaRecentActionBucket,
        thumbnailPath: suspend (MegaNode) -> String?,
        hasVersion: suspend (MegaNode) -> Boolean,
        numberOfChildFolders: suspend (MegaNode) -> Int,
        numberOfChildFiles: suspend (MegaNode) -> Int,
        fileTypeInfoMapper: FileTypeInfoMapper,
        isPendingShare: suspend (MegaNode) -> Boolean,
        isInRubbish: suspend (MegaNode) -> Boolean,
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
