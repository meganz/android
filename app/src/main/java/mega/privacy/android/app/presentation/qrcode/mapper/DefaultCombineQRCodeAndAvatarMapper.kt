package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Implementation of [CombineQRCodeAndAvatarMapper]
 */
class DefaultCombineQRCodeAndAvatarMapper @Inject constructor(
    @IoDispatcher private val iODispatcher: CoroutineDispatcher,
) : CombineQRCodeAndAvatarMapper {
    override suspend fun invoke(
        qrCodeBitmap: Bitmap,
        qrCodeWidth: Int,
        @ColorInt qrCodeBgColor: Int,
        avatarBitmap: Bitmap,
        avatarWidth: Int,
        avatarBorderWidth: Int,
        @ColorInt avatarBorderColor: Int,
    ): Bitmap = withContext(iODispatcher) {
        val qrCode = Bitmap.createBitmap(qrCodeWidth, qrCodeWidth, Bitmap.Config.ARGB_8888)
        val offset = (avatarWidth / 2).toFloat()
        val avatarPos = (qrCodeWidth - avatarWidth) / 2
        val c = Canvas(qrCode)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = avatarBorderColor
        val scaledAvatar =
            Bitmap.createScaledBitmap(avatarBitmap, avatarWidth, avatarWidth, false)
        c.drawBitmap(qrCodeBitmap, 0f, 0f, null)
        c.drawCircle(
            avatarPos + offset,
            avatarPos + offset,
            offset + Util.dp2px(avatarBorderWidth.toFloat()),
            paint
        )
        c.drawBitmap(
            scaledAvatar,
            avatarPos.toFloat(),
            avatarPos.toFloat(),
            null
        )
        qrCodeBitmap.recycle()
        avatarBitmap.recycle()
        return@withContext qrCode
    }

}