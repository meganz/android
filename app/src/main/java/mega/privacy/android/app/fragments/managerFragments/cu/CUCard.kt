package mega.privacy.android.app.fragments.managerFragments.cu

import androidx.recyclerview.widget.DiffUtil
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.LocalDate

data class CUCard(
    val node: MegaNode,
    var preview: File?,
    var day: String? = null,
    var month: String? = null,
    val year: String?,
    val date: String,
    val localDate: LocalDate,
    var numItems: Long? = null
) {
    class DiffCallback : DiffUtil.ItemCallback<CUCard>() {
        override fun areItemsTheSame(oldItem: CUCard, newItem: CUCard) =
            oldItem.node.handle == newItem.node.handle

        override fun areContentsTheSame(oldItem: CUCard, newItem: CUCard) =
            oldItem == newItem
    }
}
