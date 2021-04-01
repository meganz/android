package mega.privacy.android.app.meeting.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemCameraGroupCallBinding
import mega.privacy.android.app.utils.ColorUtils
import nz.mega.sdk.MegaApiAndroid
import java.util.*
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class ParticipantVideoViewHolder(
    private val binding: ItemCameraGroupCallBinding
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(participant: Participant) {
        participant.name
        binding.general.background = ColorDrawable(Color.parseColor(participant.avatarBackground))
    }
}
