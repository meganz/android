package mega.privacy.android.app.meeting.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemCameraGroupCallBinding
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class VideoListViewHolder(
    private val binding: ItemCameraGroupCallBinding,
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(participant: Participant) {
        val layoutParams = binding.general.layoutParams
        layoutParams.width = Util.dp2px(110f)
        layoutParams.height = Util.dp2px(110f)

        binding.general.background = ColorDrawable(Color.parseColor(participant.avatarBackground))
    }

}
