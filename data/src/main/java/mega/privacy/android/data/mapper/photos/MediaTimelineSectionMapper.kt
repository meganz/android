package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineSection
import nz.mega.sdk.MegaDateSectionList
import javax.inject.Inject

/**
 * Mapper to convert a [MegaDateSectionList] from the SDK into a list of [MediaTimelineSection].
 */
internal class MediaTimelineSectionMapper @Inject constructor() {

    operator fun invoke(sectionList: MegaDateSectionList): List<MediaTimelineSection> =
        buildList {
            for (i in 0..<sectionList.size()) {
                val item = sectionList[i]
                add(
                    MediaTimelineSection(
                        groupId = item.groupId,
                        count = item.count,
                        startDate = item.startDate,
                        endDate = item.endDate,
                    )
                )
            }
        }
}
