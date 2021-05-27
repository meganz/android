package mega.privacy.android.app.fragments.managerFragments.cu

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemCuCardBinding
import mega.privacy.android.app.fragments.managerFragments.cu.CameraUploadsFragment.DAYS_VIEW
import nz.mega.sdk.MegaNode

class CUCardViewHolder(
    private val viewType: Int,
    private val binding: ItemCuCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(card: Pair<CUCard, MegaNode>) {
        binding.numberItemsText.isVisible = viewType == DAYS_VIEW
    }
}