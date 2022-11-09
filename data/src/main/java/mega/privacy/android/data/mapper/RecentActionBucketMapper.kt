package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaRecentActionBucket

/**
 * The mapper class for converting [MegaRecentActionBucket] to [RecentActionBucket]
 */
typealias RecentActionBucketMapper = @JvmSuppressWildcards suspend (
    @JvmSuppressWildcards MegaRecentActionBucket,
    @JvmSuppressWildcards MapThumbnail,
    @JvmSuppressWildcards MapHasVersion,
    @JvmSuppressWildcards MapNumberOfChildFolders,
    @JvmSuppressWildcards MapNumberOfChildFiles,
    @JvmSuppressWildcards FileTypeInfoMapper,
    @JvmSuppressWildcards MapPendingShare,
    @JvmSuppressWildcards MapInRubbish,
) -> @JvmSuppressWildcards RecentActionBucketUnTyped

internal suspend fun toRecentActionBucket(
    megaRecentActionBucket: MegaRecentActionBucket,
    thumbnailPath: MapThumbnail,
    hasVersion: MapHasVersion,
    numberOfChildFolders: MapNumberOfChildFolders,
    numberOfChildFiles: MapNumberOfChildFiles,
    fileTypeInfoMapper: FileTypeInfoMapper,
    isPendingShare: MapPendingShare,
    isInRubbish: MapInRubbish,
) = RecentActionBucketUnTyped(
    timestamp = megaRecentActionBucket.timestamp,
    userEmail = megaRecentActionBucket.userEmail,
    parentHandle = megaRecentActionBucket.parentHandle,
    isUpdate = megaRecentActionBucket.isUpdate,
    isMedia = megaRecentActionBucket.isMedia,
    nodes = (0 until megaRecentActionBucket.nodes.size()).map {
        toNode(
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
