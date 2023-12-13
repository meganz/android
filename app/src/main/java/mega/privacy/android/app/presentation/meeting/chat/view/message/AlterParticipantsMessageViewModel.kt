package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFullNameUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Alter participants message view model
 *
 */
@HiltViewModel
class AlterParticipantsMessageViewModel @Inject constructor(
    private val getParticipantFullNameUseCase: GetParticipantFullNameUseCase,
    private val getMyFullNameUseCase: GetMyFullNameUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
) : ViewModel() {
    /**
     * Get participant full name
     *
     */
    suspend fun getParticipantFullName(handle: Long): String? {
        return runCatching {
            getParticipantFullNameUseCase(handle)
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
    }

    /**
     * Get my full name
     *
     */
    suspend fun getMyFullName(): String? {
        return runCatching {
            getMyFullNameUseCase()
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
    }

    /**
     * Get my user handle
     *
     */
    suspend fun getMyUserHandle(): Long {
        return runCatching {
            getMyUserHandleUseCase()
        }.onFailure {
            Timber.e(it)
        }.getOrElse { -1L }
    }
}