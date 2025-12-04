package mega.privacy.android.app.myAccount.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CANCEL_ACCOUNT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.RESET_PASSWORD_LINK
import mega.privacy.android.domain.entity.RegexPatternType.VERIFY_CHANGE_MAIL_LINK
import mega.privacy.android.domain.exception.ResetPasswordLinkException
import mega.privacy.android.domain.usecase.QueryResetPasswordLinkUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.navigation.destination.WebSiteNavKey
import timber.log.Timber
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR

class MyAccountDeepLinkHandler @Inject constructor(
    private val queryResetPasswordLinkUseCase: QueryResetPasswordLinkUseCase,
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase,
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? {
        return when (regexPatternType) {
            CANCEL_ACCOUNT_LINK -> {
                listOf(
                    MyAccountNavKey(
                        action = Constants.ACTION_CANCEL_ACCOUNT,
                        link = uri.toString()
                    )
                )
            }

            VERIFY_CHANGE_MAIL_LINK -> {
                listOf(
                    MyAccountNavKey(
                        action = Constants.ACTION_CHANGE_MAIL,
                        link = uri.toString()
                    )
                )
            }

            RESET_PASSWORD_LINK -> runCatching {
                queryResetPasswordLinkUseCase(uri.toString())
            }.onFailure { Timber.e(it) }.let { result ->
                if (result.isSuccess) {
                    val linkInfo = result.getOrNull()
                    val emailForLink = linkInfo?.email
                    val (isLoggedIn, userEmail) = runCatching { getAccountCredentialsUseCase() }.getOrNull()
                        ?.let { credentials -> true to credentials.email } ?: (false to null)

                    when {
                        emailForLink != userEmail && isLoggedIn -> {
                            snackbarEventQueue.queueMessage(R.string.error_not_logged_with_correct_account)
                            emptyList()
                        }

                        linkInfo?.isRequiredRecoveryKey == true -> listOf(
                            if (isLoggedIn) {
                                MyAccountNavKey(
                                    action = Constants.ACTION_RESET_PASS,
                                    link = uri.toString()
                                )
                            } else {
                                LoginNavKey(
                                    action = Constants.ACTION_RESET_PASS,
                                    link = uri.toString()
                                )
                            }
                        )

                        else -> listOf(WebSiteNavKey(uri.toString()))
                    }
                } else {
                    when (result.exceptionOrNull()) {
                        ResetPasswordLinkException.LinkInvalid -> sharedR.string.general_invalid_link
                        ResetPasswordLinkException.LinkExpired -> R.string.recovery_link_expired
                        ResetPasswordLinkException.LinkAccessDenied -> R.string.error_not_logged_with_correct_account
                        else -> R.string.general_text_error
                    }.let { errorStringId -> snackbarEventQueue.queueMessage(errorStringId) }

                    emptyList()
                }
            }

            else -> null
        }
    }

    override fun loggedInRequiredMessage(
        navKeys: List<NavKey>,
        regexPatternType: RegexPatternType?,
    ) = if (regexPatternType == RESET_PASSWORD_LINK) {
        null
    } else {
        super.loggedInRequiredMessage(navKeys, regexPatternType)
    }
}