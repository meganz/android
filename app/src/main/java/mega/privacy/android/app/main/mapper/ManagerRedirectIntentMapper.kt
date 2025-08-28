package mega.privacy.android.app.main.mapper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.scopes.ActivityScoped
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants
import timber.log.Timber
import javax.inject.Inject

/**
 * Manager redirect intent mapper
 *
 * @property activity
 */
@ActivityScoped
class ManagerRedirectIntentMapper @Inject constructor(private val activity: Activity) {
    /**
     * Invoke
     *
     * @param intent
     */
    operator fun invoke(intent: Intent): Intent? {
        Timber.d("Handle redirect intent action ${intent.action}")
        return when (intent.action) {
            Constants.ACTION_IMPORT_LINK_FETCH_NODES -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_IMPORT_LINK_FETCH_NODES
                    data = intent.dataString?.let { Uri.parse(it) } ?: return null
                }

            Constants.ACTION_OPEN_MEGA_LINK -> {
                Intent(activity, FileLinkComposeActivity::class.java).apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_IMPORT_LINK_FETCH_NODES
                    data = intent.dataString?.let { Uri.parse(it) } ?: return null
                }
            }

            Constants.ACTION_OPEN_MEGA_FOLDER_LINK -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK
                    data = intent.dataString?.let { Uri.parse(it) } ?: return null
                }

            Constants.ACTION_OPEN_CHAT_LINK -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_OPEN_CHAT_LINK
                    data = intent.dataString?.let { Uri.parse(it) } ?: return null
                }

            Constants.ACTION_EXPORT_MASTER_KEY -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_EXPORT_MASTER_KEY
                }

            Constants.ACTION_LOCATE_DOWNLOADED_FILE -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_LOCATE_DOWNLOADED_FILE
                    putExtra(
                        Constants.INTENT_EXTRA_IS_OFFLINE_PATH,
                        intent.getBooleanExtra(Constants.INTENT_EXTRA_IS_OFFLINE_PATH, false)
                    )
                    intent.getStringExtra(FileStorageActivity.EXTRA_PATH)?.let {
                        putExtra(FileStorageActivity.EXTRA_PATH, it)
                    }
                    intent.getStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES)?.let {
                        putStringArrayListExtra(FileStorageActivity.EXTRA_FILE_NAMES, it)
                    }
                }

            Constants.ACTION_SHOW_WARNING -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_SHOW_WARNING
                    intent.getStringExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE)?.let {
                        putExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE, it)
                    }
                }

            Constants.ACTION_EXPLORE_ZIP -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_EXPLORE_ZIP
                    intent.getStringExtra(Constants.EXTRA_PATH_ZIP)?.let {
                        putExtra(Constants.EXTRA_PATH_ZIP, it)
                    }
                }

            Constants.ACTION_IPC -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_IPC
                }

            Constants.ACTION_CHAT_NOTIFICATION_MESSAGE -> Intent(
                activity,
                LoginActivity::class.java
            ).apply {
                putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = Constants.ACTION_CHAT_NOTIFICATION_MESSAGE
            }

            Constants.ACTION_CHAT_SUMMARY -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_CHAT_SUMMARY
                }

            Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION -> Intent(
                activity,
                LoginActivity::class.java
            )
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION
                }

            Constants.ACTION_OPEN_HANDLE_NODE -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_OPEN_HANDLE_NODE
                    data = intent.dataString?.let { Uri.parse(it) } ?: return null
                }

            Constants.ACTION_OVERQUOTA_STORAGE -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_OVERQUOTA_STORAGE
                }

            Constants.ACTION_OPEN_CONTACTS_SECTION -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(
                        Constants.CONTACT_HANDLE,
                        intent.getLongExtra(Constants.CONTACT_HANDLE, -1)
                    )
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_OPEN_CONTACTS_SECTION
                }

            Constants.ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE -> Intent(
                activity,
                LoginActivity::class.java
            )
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_SHOW_SNACKBAR_SENT_AS_MESSAGE
                }

            Constants.ACTION_SHOW_UPGRADE_ACCOUNT -> Intent(activity, LoginActivity::class.java)
                .apply {
                    putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = Constants.ACTION_SHOW_UPGRADE_ACCOUNT
                }

            else -> null
        }
    }
}