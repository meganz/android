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
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import java.nio.ByteBuffer

object TestTool {

//    fun getTestParticipants(context: Context): List<Participant> {
//        val megaApi = MegaApplication.getInstance().megaApi
//        val avatar =
//            CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + FileUtil.JPG_EXTENSION)
//        return listOf(
//            Participant("Joanna Zhao", avatar, "#abcdef", false, true, false, false),
//            Participant("Yeray Rosales", avatar, "#bcd111", true, false, true, false),
//            Participant("Harmen Porter", avatar, "#ccddee", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#123456", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#ff2312", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#1223ee", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#1266ee", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#1223ff", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#1223dd", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#1223ee", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#ff23ee", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#12ffee", false, false, false, true),
//            Participant("Katayama Fumiki", avatar, "#b323ee", false, false, false, true),
//        )
//    }
//
    fun testData() = listOf(
        Participant(1L,2L,"Joanna Zhao", null,  false, true, false, false),
        Participant(2L,2L,"Yeray Rosales", null,  true, false, true, false),
        Participant(3L,2L,"Harmen Porter", null,  false, false, false, true),
        Participant(4L,2L,"Katayama Fumiki", null,  false, false, false, true),
        Participant(5L,2L,"1 Fumiki", null,  false, false, false, true),
        Participant(6L,2L,"2 Fumiki", null, false, false, false, true),
        Participant(7L,2L,"3 Fumiki", null, false, false, false, true),
        Participant(8L,2L,"4 Fumiki", null, false, false, false, true),
        Participant(9L,2L,"5 Fumiki", null,  false, false, false, true),
        Participant(10L,2L,"6 Fumiki", null,  false, false, false, true),
        Participant(11L,2L,"7 Fumiki", null,  false, false, false, true),
        Participant(12L,2L,"8 Fumiki", null,  false, false, false, true),
        Participant(13L,2L,"9 Fumiki", null,  false, false, false, true),
    )

    fun View.showHide() {
        isVisible = !isVisible
    }

    // TODO test start: Change the video path to point a local video on your test device.(Need storage read permission)
    @RequiresApi(Build.VERSION_CODES.P)
    class FrameProducer {

        val frames = mutableListOf<Bitmap>()

        //processor: ((width: Int, height: Int, bitmap: Bitmap) -> Unit)
        suspend fun load() {
            val videoPath =
                Environment.getExternalStorageDirectory().absolutePath + "/DCIM/photos/20190819_111725.mp4"
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)

            val frameCount = retriever.extractMetadata(METADATA_KEY_VIDEO_FRAME_COUNT).toInt()

            // In test, the bitmap won't be recycled, so don't put in too many bitmaps.
            for (i in 0 until 300.coerceAtMost(frameCount)) frames.add(retriever.getFrameAtIndex(i))
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

