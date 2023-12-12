package mega.privacy.android.app.presentation.meeting.chat.view.message

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.domain.usecase.GetBitmapFromStringUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat rich link view model
 *
 */
@HiltViewModel
class ChatRichLinkViewModel @Inject constructor(
    private val getBitmapFromStringUseCase: GetBitmapFromStringUseCase,
) : ViewModel() {
    /**
     * Get bitmap
     *
     */
    suspend fun getBitmap(value: String): Bitmap? =
        runCatching {
            getBitmapFromStringUseCase(value)
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
}