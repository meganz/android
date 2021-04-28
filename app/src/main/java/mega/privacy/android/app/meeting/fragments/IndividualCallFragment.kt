package mega.privacy.android.app.meeting.fragments

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_local_camera_call.*
import kotlinx.coroutines.*
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom


class IndividualCallFragment : MeetingBaseFragment() {

    private var chatId: Long? = null
    private var clientId: Long? = null
    private var chat: MegaChatRoom? = null
    private var isFloatingWindow = false

    lateinit var inMeetingViewModel: InMeetingViewModel

    val paint = Paint()

    var videoAlpha = 255

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
            dstRect.right = video.width
            dstRect.bottom = video.height

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

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        paint.alpha = videoAlpha

//        canvas.drawRoundRect(
//            RectF(0f, 0f, video.width.toFloat(), video.height.toFloat()),
//            Util.dp2px(16f, outMetrics).toFloat(),
//            Util.dp2px(16f, outMetrics).toFloat(),
//            paint
//        )

        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        holder.unlockCanvasAndPost(canvas)
    }
    // TODO test end

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID)
            clientId = it.getLong(Constants.CLIENT_ID)
            isFloatingWindow = it.getBoolean(Constants.IS_FLOATING_WINDOW)
        }

        this.inMeetingViewModel = (parentFragment as InMeetingFragment).inMeetingViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (isFloatingWindow)
            SelfFeedFloatingWindowFragmentBinding.inflate(inflater, container, false).root
        else
            IndividualCallFragmentBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isFloatingWindow) {
            holder = video.holder

            // Set background of SurfaceView as transparent after drew a round corner rect.
            video.setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSLUCENT)

            (parentFragment as InMeetingFragment).bottomFloatingPanelViewHolder.propertyUpdaters.apply {
                add {
                    view.alpha = 1 - it
                }
                add {
                    videoAlpha = ((1 - it) * 255).toInt()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Start drawing here, so that after a lock screen, the drawing can resume.
        if (isFloatingWindow) {
            // TODO test start
            holder.addCallback(callback)
            // TODO test code end
        }
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

    companion object {

        const val TAG = "IndividualCallFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Chat ID
         * @param peerId peer ID
         * @param isFloatingWindow True, if it's floating window. False, otherwise.
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance(chatId: Long, peerId: Long, isFloatingWindow: Boolean) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.PEER_ID, peerId)
                    putLong(Constants.CLIENT_ID, MEGACHAT_INVALID_HANDLE)

                    putBoolean(Constants.IS_FLOATING_WINDOW, isFloatingWindow)
                }
            }

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Chat ID
         * @param peerId peer ID
         * @param clientId client ID
         * @return A new instance of fragment MeetingFragment.
         */
        fun newInstance(chatId: Long, peerId: Long, clientId: Long) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.PEER_ID, peerId)
                    putLong(Constants.CLIENT_ID, clientId)
                    putBoolean(Constants.IS_FLOATING_WINDOW, false)

                }
            }
    }
}