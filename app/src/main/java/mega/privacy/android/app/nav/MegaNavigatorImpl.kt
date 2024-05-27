package mega.privacy.android.app.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.ManageChatHistoryActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivityV2
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserComposeActivity
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EXTRA_HANDLE_ZIP
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.app.zippreview.ui.ZipBrowserActivity
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Mega navigator impl
 * Centralized navigation logic instead of call navigator separately
 * We will replace with navigation component in the future
 */
internal class MegaNavigatorImpl @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : MegaNavigator,
    AppNavigatorImpl {
    override fun openSettingsCameraUploads(activity: Activity) {
        applicationScope.launch {
            activity.startActivity(
                Intent(
                    activity,
                    SettingsCameraUploadsActivity::class.java,
                )
            )
        }
    }

    override fun openChat(
        context: Context,
        chatId: Long,
        action: String?,
        link: String?,
        text: String?,
        messageId: Long?,
        isOverQuota: Int?,
        flags: Int,
    ) {
        val intent = getChatActivityIntent(
            context = context,
            action = action,
            link = link,
            text = text,
            chatId = chatId,
            messageId = messageId,
            isOverQuota = isOverQuota,
            flags = flags
        )
        context.startActivity(intent)
    }

    override fun openUpgradeAccount(context: Context) {
        applicationScope.launch {
            val intent = Intent(context, UpgradeAccountActivity::class.java)
            context.startActivity(intent)
        }
    }

    private fun getChatActivityIntent(
        context: Context,
        action: String?,
        link: String?,
        text: String?,
        chatId: Long,
        messageId: Long?,
        isOverQuota: Int?,
        flags: Int,
    ): Intent {
        val intent = Intent(context, ChatHostActivity::class.java).apply {
            this.action = action
            putExtra(EXTRA_ACTION, action)
            text?.let { putExtra(Constants.SHOW_SNACKBAR, text) }
            putExtra(Constants.CHAT_ID, chatId)
            messageId?.let { putExtra("ID_MSG", messageId) }
            isOverQuota?.let { putExtra("IS_OVERQUOTA", isOverQuota) }
            if (flags > 0) setFlags(flags)
        }
        link?.let {
            intent.putExtra(EXTRA_LINK, it)
        }
        return intent
    }

    override fun openManageChatHistoryActivity(
        context: Context,
        chatId: Long,
        email: String?,
    ) {
        applicationScope.launch {
            val activity =
                if (getFeatureFlagValueUseCase(AppFeatures.NewManageChatHistoryActivity)) {
                    ManageChatHistoryActivityV2::class.java
                } else {
                    ManageChatHistoryActivity::class.java
                }
            val intent = Intent(context, activity).apply {
                putExtra(Constants.CHAT_ID, chatId)
                email?.let { putExtra(Constants.EMAIL, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun openZipBrowserActivity(
        context: Context,
        zipFilePath: String,
        nodeHandle: Long?,
        onError: () -> Unit,
    ) {
        applicationScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.NewZipBrowser)) {
                if (ZipBrowserComposeActivity.zipFileFormatCheck(context, zipFilePath)) {
                    ZipBrowserComposeActivity::class.java
                } else null
            } else {
                if (ZipBrowserActivity.zipFileFormatCheck(context, zipFilePath)) {
                    ZipBrowserActivity::class.java
                } else null
            }?.let { activity ->
                context.startActivity(Intent(context, activity).apply {
                    putExtra(EXTRA_PATH_ZIP, zipFilePath)
                    putExtra(EXTRA_HANDLE_ZIP, nodeHandle)
                })
            } ?: run {
                onError()
            }
        }
    }
}
