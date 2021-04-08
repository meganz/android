package mega.privacy.android.app.meeting.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.grid_view_call_fragment.*
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemCameraGroupCallBinding
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class ParticipantVideoViewHolder(
    private val binding: ItemCameraGroupCallBinding,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(participant: Participant, itemCount: Int) {
        var w = 0
        var h = 0

        val lp: GridLayoutManager.LayoutParams =
            binding.general.layoutParams as GridLayoutManager.LayoutParams

        when (itemCount) {
            2 -> {
                w = screenWidth
                h = screenHeight / 2
                lp.setMargins(0, 0, 0, 0)
            }
            3 -> {
                w = (screenWidth * 0.8).toInt()
                h = screenHeight / 3
                lp.setMargins((screenWidth - w) / 2, 0, (screenWidth - w) / 2, 0)
            }
            5 -> {
                w = screenWidth / 2
                h = screenHeight / 3
                if(adapterPosition == 4) {
                    lp.setMargins((screenWidth - w) / 2, 0, (screenWidth - w) / 2, 0)
                } else {
                    lp.setMargins(0, 0, 0, 0)
                }
            }
            4, 6 -> {
                w = screenWidth / 2
                h = screenHeight / 3
                lp.setMargins(0, 0, 0, 0)
            }
        }

        lp.width = w
        lp.height = h

        binding.general.background = ColorDrawable(Color.parseColor(participant.avatarBackground))
    }
}
