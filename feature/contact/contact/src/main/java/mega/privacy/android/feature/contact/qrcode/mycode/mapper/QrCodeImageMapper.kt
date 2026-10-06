package mega.privacy.android.feature.contact.qrcode.mycode.mapper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.core.qr.encodeQrCodeBitmap
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.shared.account.presentation.model.AvatarContent
import mega.privacy.android.shared.account.presentation.model.EmojiAvatarContent
import mega.privacy.android.shared.account.presentation.model.PhotoAvatarContent
import mega.privacy.android.shared.account.presentation.model.TextAvatarContent
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Renders the QR code image that is persisted for sharing and saving: the contact link as a
 * black-on-white QR code with the user's avatar over its centre, mirroring the screen's layout.
 *
 * The colours are fixed rather than taken from the theme so that the file scans reliably wherever
 * it ends up — a dark-theme capture would invert the code.
 */
class QrCodeImageMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke.
     *
     * @param contactLink the link encoded in the QR code
     * @param avatarContent the avatar drawn over the code
     * @return the JPEG-encoded image, or null if [contactLink] cannot be encoded.
     */
    suspend operator fun invoke(contactLink: String, avatarContent: AvatarContent): ByteArray? =
        withContext(defaultDispatcher) {
            val qrCode = encodeQrCodeBitmap(contactLink, QR_CODE_SIZE, QR_CODE_SIZE)
                ?: return@withContext null
            val image = createBitmap(IMAGE_SIZE, IMAGE_SIZE)
            val canvas = Canvas(image)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(qrCode, QUIET_ZONE.toFloat(), QUIET_ZONE.toFloat(), null)
            qrCode.recycle()
            canvas.drawAvatar(avatarContent)

            ByteArrayOutputStream().use { output ->
                image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                image.recycle()
                output.toByteArray()
            }
        }

    private fun Canvas.drawAvatar(avatarContent: AvatarContent) {
        val centre = IMAGE_SIZE / 2f
        val radius = AVATAR_SIZE / 2f
        drawCircle(centre, centre, radius + AVATAR_BORDER, fillPaint(Color.WHITE))

        when (avatarContent) {
            is TextAvatarContent -> {
                drawCircle(centre, centre, radius, fillPaint(avatarContent.backgroundColor))
                drawInitial(avatarContent.avatarText, centre)
            }

            is EmojiAvatarContent -> {
                drawCircle(centre, centre, radius, fillPaint(avatarContent.backgroundColor))
                ContextCompat.getDrawable(context, avatarContent.emojiContent)?.let { emoji ->
                    val inset = (centre - radius + EMOJI_PADDING).toInt()
                    emoji.setBounds(inset, inset, IMAGE_SIZE - inset, IMAGE_SIZE - inset)
                    emoji.draw(this)
                }
            }

            is PhotoAvatarContent -> {
                decodePhoto(avatarContent.path)?.let { photo ->
                    val scaled =
                        Bitmap.createScaledBitmap(photo, AVATAR_SIZE, AVATAR_SIZE, true)
                    photo.recycle()
                    val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    val offset = centre - radius
                    save()
                    translate(offset, offset)
                    drawCircle(radius, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.shader = shader
                    })
                    restore()
                    scaled.recycle()
                }
            }
        }
    }

    private fun Canvas.drawInitial(initial: String, centre: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = INITIAL_TEXT_SIZE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.SANS_SERIF
        }
        val metrics = paint.fontMetrics
        val baseline = centre - (metrics.ascent + metrics.descent) / 2f
        drawText(initial, centre, baseline, paint)
    }

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun decodePhoto(path: String): Bitmap? =
        runCatching { BitmapFactory.decodeFile(File(path.toUri().path ?: path).absolutePath) }
            .getOrNull()

    private companion object {
        const val IMAGE_SIZE = 600
        const val QUIET_ZONE = 40
        const val QR_CODE_SIZE = IMAGE_SIZE - 2 * QUIET_ZONE
        const val AVATAR_SIZE = 120
        const val AVATAR_BORDER = 6f
        const val EMOJI_PADDING = 15f
        const val INITIAL_TEXT_SIZE = 71f
        const val JPEG_QUALITY = 100
    }
}
