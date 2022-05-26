package mega.privacy.android.app.gallery.data

import androidx.recyclerview.widget.DiffUtil
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.LocalDate

/**
 * Data class used to manage Camera Uploads and Media Uploads content as cards.
 *
 * @param node      MegaNode representing the card item.
 * @param preview   Preview of the node if exists, null otherwise.
 * @param day       Day of the modified date if a day card, null otherwise.
 * @param month     Month of the modified date if a day or month card, null otherwise.
 * @param year      Year of the modified date, null if the year is the current year and not a year card.
 * @param date      Modification date as String, formatted:
 *                      - Day card:   "dd MMMM" if same year, "dd MMMM yyyy" otherwise.
 *                      - Month card: month if same year, "MMMM yyyy" otherwise.
 *                      - Year card:  year.
 * @param localDate Modification date as LocalDate, without formatting.
 * @param numItems  Number of items contained in a card. Should be used only on day cards as per design.
 */
data class GalleryCard(
        val id :Long,
        val name: String,
        var preview: File?,
        var day: String? = null,
        var month: String? = null,
        val year: String?,
        val date: String,
        val localDate: LocalDate,
        var numItems: Long = 0
) {


    fun incrementNumItems() {
        numItems++
    }

    class DiffCallback : DiffUtil.ItemCallback<GalleryCard>() {
        override fun areItemsTheSame(oldItem: GalleryCard, newItem: GalleryCard) =
                oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GalleryCard, newItem: GalleryCard) =
                oldItem == newItem
    }
}
