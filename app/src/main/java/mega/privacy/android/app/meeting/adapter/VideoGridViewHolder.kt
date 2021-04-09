package mega.privacy.android.app.meeting.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemCameraGroupCallBinding
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class VideoGridViewHolder(
    private val binding: ItemCameraGroupCallBinding,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(participant: Participant, itemCount: Int, isFirstPage: Boolean) {
        layout(isFirstPage, itemCount)

        binding.general.background = ColorDrawable(Color.parseColor(participant.avatarBackground))
    }

    private fun layout(isFirstPage: Boolean, itemCount: Int) {
        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.general.layoutParams as GridLayoutManager.LayoutParams

        val veriticalMargin = ((screenHeight - screenWidth / 2 * 3) / 2)

        if (isFirstPage) {
            when (itemCount) {
                2 -> {
                    w = screenWidth
                    h = screenHeight / 2
                    layoutParams.setMargins(0, 0, 0, 0)
                }
                3 -> {
                    w = (screenWidth * 0.8).toInt()
                    h = screenHeight / 3
                    layoutParams.setMargins((screenWidth - w) / 2, 0, (screenWidth - w) / 2, 0)
                }
                5 -> {
                    w = screenWidth / 2
                    h = w

                    when (adapterPosition) {
                        0, 1 -> {
                            layoutParams.setMargins(0, veriticalMargin, 0, 0)
                        }
                        4 -> {
                            layoutParams.setMargins((screenWidth - w) / 2, 0, (screenWidth - w) / 2, 0)
                        }
                        else -> {
                            layoutParams.setMargins(0, 0, 0, 0)
                        }
                    }
                }
                4, 6 -> {
                    val pair = layout46(layoutParams, veriticalMargin)
                    h = pair.first
                    w = pair.second
                }
            }
        } else {
            val pair = layout46(layoutParams, veriticalMargin)
            h = pair.first
            w = pair.second
        }

        layoutParams.width = w
        layoutParams.height = h
    }

    private fun layout46(
        layoutParams: GridLayoutManager.LayoutParams,
        veriticalMargin: Int
    ): Pair<Int, Int> {
        val w = screenWidth / 2
        when (adapterPosition) {
            0, 1 -> {
                layoutParams.setMargins(0, veriticalMargin, 0, 0)
            }
            else -> {
                layoutParams.setMargins(0, 0, 0, 0)
            }
        }
        return Pair(w, w)
    }
}
