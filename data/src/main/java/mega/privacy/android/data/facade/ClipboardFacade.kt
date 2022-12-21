package mega.privacy.android.data.facade

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.ClipboardGateway
import timber.log.Timber
import javax.inject.Inject

/**
 * [ClipboardGateway] implementation
 *
 * @property context context to get the clipboard service
 */
class ClipboardFacade @Inject constructor(
    @ApplicationContext val context: Context,
) : ClipboardGateway {

    override fun setClip(label: String, text: String) {
        Timber.d("set label($label) into clipboard")
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText(label, text)
        clipboardManager?.setPrimaryClip(clip)
    }
}