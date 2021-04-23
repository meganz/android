package mega.privacy.android.app.meeting.adapter

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.TestTool.showHide
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class VideoListViewHolder(
    private val binding: ItemParticipantVideoBinding,
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    lateinit var holder: SurfaceHolder

    var job: Job? = null

    val callback = object : SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder?) {
            isDrawing = true

            dstRect.top = 0
            dstRect.left = 0
            dstRect.right = binding.video.width
            dstRect.bottom = binding.video.height

            inMeetingViewModel.frames.observeForever {
                job = GlobalScope.launch(Dispatchers.IO) {
                    while (isDrawing) {
                        it.forEach {
                            delay(50)
                            onChatVideoData(it.width, it.height, it)

                            if (!isDrawing) return@forEach
                        }
                    }
                }
            }
        }

        override fun surfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            onRecycle()
        }
    }

    var isDrawing = true

    private val srcRect = Rect()
    private val dstRect = Rect()

    fun onRecycle() {
        isDrawing = false

        holder.removeCallback(callback)

        if (job != null && job!!.isActive) {
            GlobalScope.launch(Dispatchers.IO) {
                job!!.cancelAndJoin()
            }
        }
    }

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        itemClickViewModel: ItemClickViewModel,
        participant: Participant
    ) {
        this.inMeetingViewModel = inMeetingViewModel

        val layoutParams = binding.root.layoutParams
        layoutParams.width = Util.dp2px(ITEM_WIDTH)
        layoutParams.height = Util.dp2px(ITEM_HEIGHT)

        binding.name.text = participant.name

        binding.root.setOnClickListener {
            itemClickViewModel.onItemClick(participant)
            binding.selectedForeground.showHide()
        }

        holder = binding.video.holder
        holder.addCallback(callback)
    }

    // TODO test start
    val onChatVideoData = fun(width: Int, height: Int, bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        srcRect.top = 0
        srcRect.left = 0
        srcRect.right = width
        srcRect.bottom = height

        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas() ?: return
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    companion object {

        const val ITEM_WIDTH = 90f
        const val ITEM_HEIGHT = 90f
    }
}
