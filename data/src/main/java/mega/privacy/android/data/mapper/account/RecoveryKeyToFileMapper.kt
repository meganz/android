package mega.privacy.android.data.mapper.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

/**
 * Mapper to convert recovery key string to file
 */
internal class RecoveryKeyToFileMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Invoke
     */
    operator fun invoke(key: String): File {
        val rKBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(rKBitmap)
        val paint = Paint()
        paint.textSize = 40f
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        val height = paint.measureText("yY")
        val width = paint.measureText(key)
        val x = (rKBitmap.width - width) / 2
        canvas.drawText(key, x, height + 25f, paint)

        val file = File(context.filesDir, "TempRecoveryKey.png")
        val stream: OutputStream = FileOutputStream(file)
        stream.use {
            rKBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        return file
    }
}