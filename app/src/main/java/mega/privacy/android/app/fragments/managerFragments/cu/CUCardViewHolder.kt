package mega.privacy.android.app.fragments.managerFragments.cu

import android.annotation.SuppressLint
import android.net.Uri
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemCuCardBinding
import mega.privacy.android.app.fragments.managerFragments.cu.CameraUploadsFragment.*
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getString

class CUCardViewHolder(
    private val viewType: Int,
    private val binding: ItemCuCardBinding,
    cardWidth: Int,
    cardMargin: Int
) : RecyclerView.ViewHolder(binding.root) {

    init {
        val params = binding.root.layoutParams as GridLayoutManager.LayoutParams
        params.width = cardWidth
        params.height = cardWidth
        params.setMargins(cardMargin, cardMargin, cardMargin, cardMargin)
        binding.root.layoutParams = params
    }

    @SuppressLint("SetTextI18n")
    fun bind(position: Int, card: CUCard, listener: CUCardViewAdapter.Listener) {
        itemView.setOnClickListener { listener.onCardClicked(position, card) }

        var date = when (viewType) {
            YEARS_VIEW -> "[B]" + card.year + "[/B]"
            MONTHS_VIEW -> if (card.year == null) "[B]" + card.month + "[/B]" else getString(
                R.string.cu_month_year_date,
                card.month,
                card.year
            )
            DAYS_VIEW -> if (card.year == null) "[B]" + card.date + "[/B]" else getString(
                R.string.cu_day_month_year_date,
                card.day,
                card.month,
                card.year
            )
            else -> null
        }

        if (date != null) {
            try {
                date = date.replace("[B]", "<b><font face=\"sans-serif\">")
                    .replace("[/B]", "</font></b>")
            } catch (e: Exception) {
                LogUtil.logWarning("Exception formatting text.", e)
            }

            binding.dateText.text = HtmlCompat.fromHtml(date!!, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        val numItems = card.numItems?.minus(1)
        if (numItems != null) {
            binding.numberItemsText.isVisible = viewType == DAYS_VIEW && numItems > 0
            binding.numberItemsText.text = "+${numItems}"
        }

        val preview = card.preview

        if (preview != null) {
            binding.preview.setImageURI(Uri.fromFile(preview))
        } else {
            binding.preview.setActualImageResource(R.drawable.ic_image_thumbnail)
        }
    }
}