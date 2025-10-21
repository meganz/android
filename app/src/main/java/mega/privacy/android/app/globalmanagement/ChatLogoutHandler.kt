package mega.privacy.android.app.globalmanagement

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.BroadcastFinishActivityUseCase
import mega.privacy.android.domain.usecase.login.DisableChatApiUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber
import javax.inject.Inject

class ChatLogoutHandler @Inject constructor(
    @MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
    @ApplicationScope
    private val sharingScope: CoroutineScope,
    private val broadcastFinishActivityUseCase: BroadcastFinishActivityUseCase,
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @ApplicationContext
    private val context: Context,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    private val localLogoutUseCase: LocalLogoutUseCase,
    private val disableChatApiUseCase: DisableChatApiUseCase,
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
                val isSingleActivityEnable = runCatching {
                    getFeatureFlagValueUseCase(AppFeatures.SingleActivity)
                }.getOrDefault(false)
                if (isSingleActivityEnable) {
                    localLogoutUseCase(disableChatApiUseCase)
                } else {
                    //Need to finish ManagerActivity to avoid unexpected behaviours after forced logouts.
                    broadcastFinishActivityUseCase()
                    val loginIntent = Intent(context, LoginActivity::class.java).apply {
                        putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                        if (MegaApplication.urlConfirmationLink != null) {
                            putExtra(
                                Constants.EXTRA_CONFIRMATION,
                                MegaApplication.urlConfirmationLink
                            )
                            if (activityLifecycleHandler.isActivityVisible) {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            } else {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            action = Constants.ACTION_CONFIRM
                            MegaApplication.urlConfirmationLink = null
                        } else {
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    }
                    context.startActivity(loginIntent)
                }
            }
        }
    }
}