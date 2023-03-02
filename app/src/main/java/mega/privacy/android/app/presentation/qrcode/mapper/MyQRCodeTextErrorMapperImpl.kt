package mega.privacy.android.app.presentation.qrcode.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.QRCodeException
import javax.inject.Inject

/**
 * Implementation of [MyQRCodeTextErrorMapper]
 */
class MyQRCodeTextErrorMapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MyQRCodeTextErrorMapper {
    override fun invoke(error: Throwable): String {
        return when (error) {
            is QRCodeException.ResetFailed -> context.getString(R.string.qrcode_reset_not_successfully)
            is QRCodeException.DeleteFailed -> context.getString(R.string.qrcode_delete_not_successfully)
            else -> context.getString(R.string.general_error)
        }
    }
}