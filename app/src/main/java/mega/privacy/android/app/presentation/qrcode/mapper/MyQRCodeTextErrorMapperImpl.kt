package mega.privacy.android.app.presentation.qrcode.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.MyQRCodeException
import javax.inject.Inject

/**
 * Implementation of [MyQRCodeTextErrorMapper]
 */
class MyQRCodeTextErrorMapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MyQRCodeTextErrorMapper {
    override fun invoke(error: Throwable): String {
        return when (error) {
            is MyQRCodeException.ResetFailed -> context.getString(R.string.qrcode_reset_not_successfully)
            is MyQRCodeException.DeleteFailed -> context.getString(R.string.qrcode_delete_not_successfully)
            else -> context.getString(R.string.general_error)
        }
    }
}