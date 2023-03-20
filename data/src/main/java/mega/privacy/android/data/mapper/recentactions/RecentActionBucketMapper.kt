package mega.privacy.android.data.mapper.recentactions

import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.MapHasVersion
import mega.privacy.android.data.mapper.MapInRubbish
import mega.privacy.android.data.mapper.MapNumberOfChildFiles
import mega.privacy.android.data.mapper.MapNumberOfChildFolders
import mega.privacy.android.data.mapper.MapPendingShare
import mega.privacy.android.data.mapper.MapThumbnail
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaRecentActionBucket

/**
 * The mapper class for converting [MegaRecentActionBucket] to [RecentActionBucket]
 */
internal fun interface RecentActionBucketMapper {
    suspend operator fun invoke(
        megaRecentActionBucket: MegaRecentActionBucket,
        thumbnailPath: MapThumbnail,
        hasVersion: MapHasVersion,
        numberOfChildFolders: MapNumberOfChildFolders,
        numberOfChildFiles: MapNumberOfChildFiles,
        fileTypeInfoMapper: FileTypeInfoMapper,
        isPendingShare: MapPendingShare,
        isInRubbish: MapInRubbish
    ): RecentActionBucketUnTyped
}