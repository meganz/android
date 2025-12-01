package mega.privacy.android.feature.photos.mapper

import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.feature.photos.model.Sort
import javax.inject.Inject

class LegacyPhotosSortMapper @Inject constructor() {
    operator fun invoke(sortDirection: SortDirection): Sort = when (sortDirection) {
        SortDirection.Ascending -> Sort.OLDEST
        SortDirection.Descending -> Sort.NEWEST
    }
}