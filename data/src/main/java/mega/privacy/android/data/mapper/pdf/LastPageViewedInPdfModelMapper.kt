package mega.privacy.android.data.mapper.pdf

import mega.privacy.android.data.database.entity.LastPageViewedInPdfEntity
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import javax.inject.Inject

internal class LastPageViewedInPdfModelMapper @Inject constructor() {

    operator fun invoke(lastPageViewedInPdfEntity: LastPageViewedInPdfEntity) =
        with(lastPageViewedInPdfEntity) {
            LastPageViewedInPdf(
                nodeHandle = nodeHandle,
                lastPageViewed = lastPageViewed,
            )
        }
}