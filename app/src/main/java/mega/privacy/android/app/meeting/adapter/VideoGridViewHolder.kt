package mega.privacy.android.app.meeting.adapter

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * When use DataBinding here, when user fling the RecyclerView, the bottom sheet will have
 * extra top offset. Not use DataBinding could avoid this bug.
 */
class VideoGridViewHolder(
    private val binding: ItemParticipantVideoBinding,
    private val gridView: CustomizedGridCallRecyclerView,
    private val screenWidth: Int,
    private val screenHeight: Int
) : RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    lateinit var inMeetingViewModel: InMeetingViewModel

    lateinit var holder: SurfaceHolder

    var isDrawing = true

    private val srcRect = Rect()
    private val dstRect = Rect()

    // TODO test start
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

    val onChatVideoData = fun(width: Int, height: Int, bitmap: Bitmap) {
        if (bitmap.isRecycled || !holder.surface.isValid) return

        val canvas = holder.lockCanvas() ?: return

        srcRect.top = 0
        srcRect.left = 0
        srcRect.right = width
        srcRect.bottom = height

        canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        holder.unlockCanvasAndPost(canvas)
    }
    // TODO test end

    fun bind(
        inMeetingViewModel: InMeetingViewModel,
        participant: Participant,
        itemCount: Int,
        isFirstPage: Boolean
    ) {
        this.inMeetingViewModel = inMeetingViewModel

        layout(isFirstPage, itemCount)

        binding.name.text = participant.name

        holder = binding.video.holder
        holder.addCallback(callback)
    }

    fun onRecycle() {
        isDrawing = false

        holder.removeCallback(callback)

        if (job != null && job!!.isActive) {
            GlobalScope.launch(Dispatchers.IO) {
                job!!.cancelAndJoin()
            }
        }
    }

    private fun layout(isFirstPage: Boolean, itemCount: Int) {
        var w = 0
        var h = 0

        val layoutParams: GridLayoutManager.LayoutParams =
            binding.root.layoutParams as GridLayoutManager.LayoutParams

        val verticalMargin = ((screenHeight - screenWidth / 2 * 3) / 2)

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
                            layoutParams.setMargins(0, verticalMargin, 0, 0)
                        }
                        4 -> {
                            layoutParams.setMargins(
                                (screenWidth - w) / 2,
                                0,
                                (screenWidth - w) / 2,
                                0
                            )
                        }
                        else -> {
                            layoutParams.setMargins(0, 0, 0, 0)
                        }
                    }
                }
                4, 6 -> {
                    val pair = layout46(layoutParams, verticalMargin)
                    h = pair.first
                    w = pair.second
                }
            }
        } else {
            val pair = layout46(layoutParams, verticalMargin)
            h = pair.first
            w = pair.second
        }

        layoutParams.width = w
        layoutParams.height = h
    }

    private fun layout46(
        layoutParams: GridLayoutManager.LayoutParams,
        verticalMargin: Int
    ): Pair<Int, Int> {
        val w = screenWidth / 2
        when (adapterPosition) {
            0, 1 -> {
                layoutParams.setMargins(0, verticalMargin, 0, 0)
            }
            else -> {
                layoutParams.setMargins(0, 0, 0, 0)
            }
        }
        return Pair(w, w)
    }
}
