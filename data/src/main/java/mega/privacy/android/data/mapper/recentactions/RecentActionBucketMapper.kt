package mega.privacy.android.data.mapper.recentactions

import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket

/**
 * The mapper class for converting [MegaRecentActionBucket] to [RecentActionBucket]
 */
internal fun interface RecentActionBucketMapper {
    suspend operator fun invoke(
        megaRecentActionBucket: MegaRecentActionBucket,
        thumbnailPath: suspend (MegaNode) -> String?,
        hasVersion: suspend (MegaNode) -> Boolean,
        numberOfChildFolders: suspend (MegaNode) -> Int,
        numberOfChildFiles: suspend (MegaNode) -> Int,
        fileTypeInfoMapper: FileTypeInfoMapper,
        isPendingShare: suspend (MegaNode) -> Boolean,
        isInRubbish: suspend (MegaNode) -> Boolean
    ): RecentActionBucketUnTyped
}