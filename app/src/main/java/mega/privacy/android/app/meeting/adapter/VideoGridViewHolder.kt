package mega.privacy.android.app.meeting.adapter

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.databinding.ItemParticipantVideoBinding
import mega.privacy.android.app.meeting.TestTool
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
) : RecyclerView.ViewHolder(binding.root), TextureView.SurfaceTextureListener {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    private val srcRect = Rect()
    private val dstRect = Rect()

    // TODO test start
    lateinit var renderJob : Job
    private val frameProducer = TestTool.FrameProducer()
    // TODO test end

    fun bind(participant: Participant, itemCount: Int, isFirstPage: Boolean) {
        layout(isFirstPage, itemCount)

//        binding.video.surfaceTextureListener = this
        binding.name.text = participant.name
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

    // TODO test start
    val onChatVideoData = fun(width: Int, height: Int, bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        val canvas = binding.video.lockCanvas() ?: return

        srcRect.top = 0
        srcRect.left = 0
        srcRect.right = width
        srcRect.bottom = height


        canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        binding.video.unlockCanvasAndPost(canvas)

        bitmap.recycle()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        dstRect.top = 0
        dstRect.left = 0
        dstRect.right = width
        dstRect.bottom = height

        renderJob = GlobalScope.launch(Dispatchers.IO) {
            frameProducer.getFrame(onChatVideoData)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        // TODO changeDestRect
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        frameProducer.running = false
        GlobalScope.launch(Dispatchers.Main) {
            renderJob.cancelAndJoin()
        }
        return true
    }
    // TODO test code end

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }
}
