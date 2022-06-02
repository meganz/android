package mega.privacy.android.app.gallery.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemGalleryCardBinding
import mega.privacy.android.app.gallery.data.GalleryCard

/**
 * Adapter to show Camera Uploads cards organized by days, months or years.
 *
 * @param cardViewType Type of view:
 *                          - DAYS_VIEW if days.
 *                          - MONTHS_VIEW if months
 *                          - YEARS_VIEW if years.
 * @param cardWidth    Size to set as card view width.
 * @param cardMargin   Size to set as card view margin.
 * @param listener     Callback used to manage card events.
 */
class GalleryCardAdapter(
    private val cardViewType: Int,
    private val cardWidth: Int,
    private val cardMargin: Int,
    private val listener: Listener,
) : ListAdapter<GalleryCard, GalleryCardViewHolder>(GalleryCard.DiffCallback()),
    SectionTitleProvider {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryCardViewHolder =
        GalleryCardViewHolder(
            cardViewType,
            ItemGalleryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            cardWidth,
            cardMargin
        )

    override fun onBindViewHolder(holder: GalleryCardViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }

    override fun getSectionTitle(position: Int): String =
        if (position in 0 until itemCount) {
            getItem(position).date
        } else {
            ""
        }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    interface Listener {
        fun onCardClicked(card: GalleryCard)
    }
}
