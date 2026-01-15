package mega.privacy.android.app.deeplinks

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.deeplinks.model.DeepLinksUIState
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import timber.log.Timber

/**
 * Deep link view model.
 */
@HiltViewModel(assistedFactory = DeepLinksViewModel.Factory::class)
class DeepLinksViewModel @AssistedInject constructor(
    private val deepLinkHandlers: List<@JvmSuppressWildcards DeepLinkHandler>,
    private val getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
    private val getAccountCredentials: GetAccountCredentialsUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    @Assisted val args: Args,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeepLinksUIState())
    val uiState: StateFlow<DeepLinksUIState> = _uiState

    init {
        consumeDestination()
    }

    private fun consumeDestination() {
        viewModelScope.launch {
            val (regexPatternType, isLoggedIn) = if (args.regexPatternType == null) {
                val regexPatternType = getDecodedUrlRegexPatternTypeUseCase(args.uri.toString())
                val isLoggedIn = getAccountCredentials() != null

                regexPatternType to isLoggedIn
            } else {
                args.regexPatternType to true
            }

            deepLinkHandlers.firstNotNullOfOrNull { deepLinkHandler ->
                deepLinkHandler.getNavKeysInternal(args.uri, regexPatternType, isLoggedIn)
            }?.let { navKeys ->
                navKeys.forEach {
                    Timber.d("Adding NavKey from deep link: $it")
                }
                _uiState.update { state -> state.copy(navKeys = navKeys) }
            } ?: run {
                // This should only happen in case the link is not supported by MEGA app
                Timber.e("Deep link not handled: $args.uri")
                _uiState.update { state -> state.copy(navKeys = emptyList()) }
                snackbarEventQueue.queueMessage(R.string.open_link_not_valid_link)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): DeepLinksViewModel
    }

    data class Args(
        val uri: Uri,
        val regexPatternType: RegexPatternType? = null,
    )
}