package mega.privacy.android.app.meeting.fragments

import android.graphics.*
import android.os.Build.VERSION_CODES.P
import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatRoom


class SelfFeedFragment : MeetingBaseFragment(), TextureView.SurfaceTextureListener {

    private var chatId: Long? = null
    private var clientId: Long? = null
    private var chat: MegaChatRoom? = null

    private lateinit var video: TextureView

    var isDrawing = true

    private var surfaceWidth = 0
    private var surfaceHeight = 0

    private val paint = Paint()
    private val srcRect = Rect()
    private val dstRect = Rect()
    private val modeSrcOver = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    private val modeSrcIn = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID)
            clientId = it.getLong(Constants.CLIENT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = SelfFeedFloatingWindowFragmentBinding.inflate(inflater, container, false).root
        video = root.findViewById(R.id.video)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (parentFragment as InMeetingFragment).bottomFloatingPanelViewHolder.propertyUpdaters.add {
            view.alpha = 1 - it
        }

        video.surfaceTextureListener = this
    }

    // TODO test start
    val onChatVideoData = fun(width: Int, height: Int, bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        val canvas = video.lockCanvas() ?: return

        srcRect.top = 0
        srcRect.left = 0
        srcRect.right = width
        srcRect.bottom = height

        paint.color = Color.parseColor("#abcdef")
        canvas.drawRoundRect(
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            Util.dp2px(16f, outMetrics).toFloat(),
            Util.dp2px(16f, outMetrics).toFloat(),
            paint
        )

        canvas.drawBitmap(bitmap, srcRect, dstRect, null)

        video.unlockCanvasAndPost(canvas)
    }

    @RequiresApi(P)
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        dstRect.top = 0
        dstRect.left = 0
        dstRect.right = width
        dstRect.bottom = height

        (parentFragment as InMeetingFragment).inMeetingViewModel.frames.observeForever {
            GlobalScope.launch(Dispatchers.IO) {
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

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        // TODO changeDestRect
    }

    @RequiresApi(P)
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        isDrawing = false
        return true
    }
    // TODO test code end

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

    }

    companion object {

        const val TAG = "SelfFeedFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Parameter 1.
         * @param clientId Parameter 2.
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance(chatId: Long, clientId: Long) =
            SelfFeedFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.CLIENT_ID, clientId)
                }
            }
    }
}