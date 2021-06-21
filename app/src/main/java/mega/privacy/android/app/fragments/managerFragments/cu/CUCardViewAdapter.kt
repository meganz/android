package mega.privacy.android.app.fragments.managerFragments.cu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemCuCardBinding

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
class CUCardViewAdapter(
    private val cardViewType: Int,
    private val cardWidth: Int,
    private val cardMargin: Int,
    private val listener: Listener
) : ListAdapter<CUCard, CUCardViewHolder>(CUCard.DiffCallback()), SectionTitleProvider {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CUCardViewHolder =
        CUCardViewHolder(
            cardViewType,
            ItemCuCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            cardWidth,
            cardMargin
        )

    override fun onBindViewHolder(holder: CUCardViewHolder, position: Int) {
        holder.bind(position, getItem(position), listener)
    }

    override fun getSectionTitle(position: Int): String =
        if (position in 0 until itemCount) {
            getItem(position).date
        } else {
            ""
        }

    override fun getItemId(position: Int): Long =
        getItem(position).node.handle

    interface Listener {
        fun onCardClicked(position: Int, card: CUCard)
    }
}
