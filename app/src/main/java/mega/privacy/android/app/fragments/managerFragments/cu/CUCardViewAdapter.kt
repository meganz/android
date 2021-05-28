package mega.privacy.android.app.fragments.managerFragments.cu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemCuCardBinding
import mega.privacy.android.app.fragments.managerFragments.cu.CameraUploadsFragment.*

class CUCardViewAdapter(
    private val cardViewType: Int,
    private val cardWidth: Int,
    private val cardMargin: Int
) : RecyclerView.Adapter<CUCardViewHolder>(), SectionTitleProvider {

    private var cards: ArrayList<android.util.Pair<CUCard, CuNode>> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CUCardViewHolder {
        return CUCardViewHolder(
            cardViewType,
            ItemCuCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            cardWidth,
            cardMargin
        )
    }

    override fun onBindViewHolder(holder: CUCardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    override fun getItemId(position: Int): Long {
        return cards[position].second.node.handle
    }

    override fun getSectionTitle(position: Int): String {
        return if (position < 0 || position >= cards.size) ""
        else {
            val date = cards[position].first

            when (cardViewType) {
                YEARS_VIEW -> date.year
                MONTHS_VIEW -> date.month + " " + date.year
                DAYS_VIEW -> date.day + " " + date.month + " " + date.year
                else -> ""
            }
        }
    }

    fun setCards(cards: List<android.util.Pair<CUCard, CuNode>>) {
        this.cards.clear()
        this.cards.addAll(cards)
        notifyDataSetChanged()
    }
}