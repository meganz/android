package mega.privacy.android.app.globalmanagement

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import timber.log.Timber
import javax.inject.Inject

class ChatLogoutHandler @Inject constructor(
    @MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
    @ApplicationScope
    private val sharingScope: CoroutineScope,
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    @ApplicationContext
    private val context: Context,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    private val localLogoutUseCase: LocalLogoutUseCase,
) {
    fun handleChatLogout(isLoggingIn: Boolean) {
        sharingScope.launch {
            runCatching { localLogoutAppUseCase() }
                .onFailure { Timber.d(it) }

            withContext(mainDispatcher) {
                if (isLoggingIn) {
                    Timber.d("Already in Login Activity, not necessary to launch it again")
                    return@withContext
                }
                localLogoutUseCase(disableChatApi = true)
                activityLifecycleHandler.getCurrentActivity()?.let { activity ->
                    if (activity !is MegaActivity) {
                        activity.startActivity(Intent(context, MegaActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                        })
                    }
                }
            }
        }
    }
}