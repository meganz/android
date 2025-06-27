package mega.privacy.android.data.mapper.pdf

import mega.privacy.android.data.database.entity.LastPageViewedInPdfEntity
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import javax.inject.Inject

internal class LastPageViewedInPdfEntityMapper @Inject constructor() {

    operator fun invoke(lastPageViewedInPdf: LastPageViewedInPdf) = with(lastPageViewedInPdf) {
        LastPageViewedInPdfEntity(
            nodeHandle = nodeHandle,
            lastPageViewed = lastPageViewed,
        )
    }
}