package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant.TABLE_LAST_PAGE_VIEWED_IN_PDF
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf

/**
 * Last page viewed in pdf database entity for [LastPageViewedInPdf]
 *
 * @property nodeHandle The unique identifier for the node associated with the PDF.
 * @property lastPageViewed The last page number that was viewed in the PDF.
 */
@Entity(tableName = TABLE_LAST_PAGE_VIEWED_IN_PDF)
data class LastPageViewedInPdfEntity(
    @PrimaryKey
    @ColumnInfo("nodeHandle") val nodeHandle: Long = -1,
    @ColumnInfo("lastPageViewed") val lastPageViewed: Long = 1,
)
