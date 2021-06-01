package mega.privacy.android.app.fragments.managerFragments.cu

import android.annotation.SuppressLint
import android.net.Uri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemCuCardBinding
import mega.privacy.android.app.fragments.managerFragments.cu.CameraUploadsFragment.DAYS_VIEW

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
    fun bind(card: CUCard) {
        binding.dateText.text = card.date
        binding.numberItemsText.isVisible = viewType == DAYS_VIEW
        binding.numberItemsText.text = "+${card.numItems}"

        val preview = card.preview

        if (preview != null) {
            binding.preview.setImageURI(Uri.fromFile(preview))
        } else {
            binding.preview.setActualImageResource(R.drawable.ic_image_thumbnail)
        }
    }
}