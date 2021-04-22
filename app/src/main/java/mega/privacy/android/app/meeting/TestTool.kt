package mega.privacy.android.app.meeting

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT
import android.os.Build
import android.os.Environment
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import ash.TL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import java.nio.ByteBuffer

object TestTool {

    fun getTestParticipants(context: Context): List<Participant> {
        val megaApi = MegaApplication.getInstance().megaApi
        val avatar =
            CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + FileUtil.JPG_EXTENSION)
        return listOf(
            Participant("Joanna Zhao", avatar, "#abcdef", false, true, false, false),
            Participant("Yeray Rosales", avatar, "#bcd111", true, false, true, false),
            Participant("Harmen Porter", avatar, "#ccddee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#123456", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#ff2312", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1266ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223ff", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223dd", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#1223ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#ff23ee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#12ffee", false, false, false, true),
            Participant("Katayama Fumiki", avatar, "#b323ee", false, false, false, true),
        )
    }

    fun testData() = listOf(
        Participant("Joanna Zhao", null, "#abcdef", false, true, false, false),
        Participant("Yeray Rosales", null, "#bcd111", true, false, true, false),
        Participant("Harmen Porter", null, "#ccddee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#123456", false, false, false, true),
        Participant("Katayama Fumiki", null, "#ff2312", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1266ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223ff", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223dd", false, false, false, true),
        Participant("Katayama Fumiki", null, "#1223ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#ff23ee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#12ffee", false, false, false, true),
        Participant("Katayama Fumiki", null, "#b323ee", false, false, false, true),
    )

    fun View.showHide() {
        isVisible = !isVisible
    }

    // TODO test start: Change the video path to point a local video on your test device.(Need storage read permission)
    @RequiresApi(Build.VERSION_CODES.P)
    class FrameProducer {

        var running = true

        val frames = mutableListOf<Bitmap>()

        init {
//            GlobalScope.launch(Dispatchers.IO) {
//                init()
//            }
            val videoPath =
                Environment.getExternalStorageDirectory().absolutePath + "/DCIM/photos/20190819_111725.mp4"
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)

            val frameCount = retriever.extractMetadata(METADATA_KEY_VIDEO_FRAME_COUNT).toInt()

            for (i in 0 until frameCount) frames.add(retriever.getFrameAtIndex(i))
        }

        suspend fun init() {
            // TODO Change the video path to point a local video on your test device.(Need storage read permission)
            val videoPath =
                Environment.getExternalStorageDirectory().absolutePath + "/DCIM/photos/20190819_111725.mp4"
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)

            val frameCount = retriever.extractMetadata(METADATA_KEY_VIDEO_FRAME_COUNT).toInt()

            for (i in 0 until frameCount) frames.add(retriever.getFrameAtIndex(i))
        }


        suspend fun getFrame(processor: ((width: Int, height: Int, bitmap: Bitmap) -> Unit)) {
            while (running) {
                frames.forEach {
                    delay(100)
                    processor(it.width, it.height, it)

                    if (!running) return@forEach
                }
            }
        }

        private fun convertToByteBuffer(b: Bitmap): ByteArray {
            val capacity = b.byteCount
            val buffer = ByteBuffer.allocate(capacity)
            b.copyPixelsToBuffer(buffer)

            return buffer.array()
        }
    }
    // TODO test end
}

