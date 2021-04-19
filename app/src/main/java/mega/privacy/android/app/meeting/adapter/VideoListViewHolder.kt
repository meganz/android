package mega.privacy.android.app.meeting.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.TestTool.showHide
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class VideoListViewHolder(
    private val binding: ItemParticipantVideoBinding,
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(participant: Participant, itemClickViewModel: ItemClickViewModel) {
        val layoutParams = binding.root.layoutParams
        layoutParams.width = Util.dp2px(90f)
        layoutParams.height = Util.dp2px(90f)

        binding.video.background = ColorDrawable(Color.parseColor(participant.avatarBackground))
        binding.name.text = participant.name

        binding.root.setOnClickListener {
            itemClickViewModel.onItemClick(participant)
            binding.selectedForeground.showHide()
        }
    }

}
