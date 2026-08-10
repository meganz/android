package mega.privacy.android.app.listeners.global

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.chat.UpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.chat.link.IsRichPreviewsEnabledUseCase
import mega.privacy.android.domain.usecase.chat.link.ShouldShowRichLinkWarningUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

class GlobalOnUserUpdateHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val updatePushNotificationSettingsUseCase: UpdatePushNotificationSettingsUseCase,
    private val shouldShowRichLinkWarningUseCase: ShouldShowRichLinkWarningUseCase,
    private val isRichPreviewsEnabledUseCase: IsRichPreviewsEnabledUseCase,
){
    operator fun invoke(
        users: ArrayList<MegaUser>?,
        api: MegaApiJava,
    ){
        users?.toList()?.forEach { user ->
            val myUserHandle = api.myUserHandle
            val isMyChange =
                myUserHandle != null && myUserHandle == MegaApiJava.userHandleToBase64(user.handle)
            if (user.hasChanged(MegaUser.CHANGE_TYPE_PUSH_SETTINGS.toLong()) && user.isOwnChange == 0) {
                applicationScope.launch {
                    runCatching {
                        updatePushNotificationSettingsUseCase()
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER.toLong()) && isMyChange) {
                api.getMyChatFilesFolder(GetAttrUserListener(appContext, true))
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS.toLong()) && isMyChange) {
                applicationScope.launch {
                    runCatching {
                        shouldShowRichLinkWarningUseCase()
                        isRichPreviewsEnabledUseCase()
                    }.onFailure {
                        Timber.e(it, "Error checking rich link settings")
                    }
                }
                return@forEach
            }
            if (user.hasChanged(MegaUser.CHANGE_TYPE_RUBBISH_TIME.toLong()) && isMyChange) {
                api.getRubbishBinAutopurgePeriod(GetAttrUserListener(appContext))
                return@forEach
            }
        }
    }
}