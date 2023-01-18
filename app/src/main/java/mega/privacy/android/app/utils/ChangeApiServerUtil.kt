package mega.privacy.android.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.AlertDialogUtil.enableOrDisableDialogButton
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object ChangeApiServerUtil {

    const val API_SERVER_PREFERENCES = "API_SERVER_PREFERENCES"
    const val API_SERVER = "API_SERVER"
    const val API_SERVER_CHECKED = "API_SERVER_CHECKED"

    const val PRODUCTION_SERVER = "https://g.api.mega.co.nz/"
    const val STAGING_SERVER = "https://staging.api.mega.co.nz/"
    const val STAGING_444_SERVER = "https://staging.api.mega.co.nz:444/"
    const val SANDBOX3_SERVER = "https://api-sandbox3.developers.mega.co.nz/"

    const val PRODUCTION_SERVER_VALUE = 0
    const val STAGING_SERVER_VALUE = 1
    const val STAGING_444_SERVER_VALUE = 2
    const val SANDBOX3_SERVER_VALUE = 3

    /**
     * Shows a dialog to point to a different api server than the current one.
     *
     * @param activity Current Activity.
     * @param megaApi  MegaApiJava to point launch the requests if needed.
     * @return The dialog.
     */
    @SuppressLint("InflateParams")
    @JvmStatic
    fun showChangeApiServerDialog(
        activity: Activity,
        megaApi: MegaApiJava
    ): AlertDialog {
        val preferences =
            activity.getSharedPreferences(API_SERVER_PREFERENCES, MODE_PRIVATE)
        val currentApiServerValue = preferences.getInt(API_SERVER, PRODUCTION_SERVER_VALUE)
        val apiServerValueChecked = preferences.getInt(API_SERVER_CHECKED, INVALID_VALUE)

        val apiServerValue: Int =
            if (apiServerValueChecked != INVALID_VALUE) apiServerValueChecked
            else currentApiServerValue

        val stringsArray = ArrayList<String>()
        stringsArray.add(PRODUCTION_SERVER_VALUE, getString(R.string.production_api_server))
        stringsArray.add(STAGING_SERVER_VALUE, getString(R.string.staging_api_server))
        stringsArray.add(STAGING_444_SERVER_VALUE, getString(R.string.staging444_api_server))
        stringsArray.add(SANDBOX3_SERVER_VALUE, getString(R.string.sandbox3_api_server))

        val itemsAdapter =
            ArrayAdapter(activity, R.layout.checked_text_view_dialog_button, stringsArray)

        val listView = ListView(activity)
        listView.adapter = itemsAdapter
        val itemClicked = AtomicReference<Int>()
        itemClicked.set(apiServerValue)
        val customTitle: View =
            activity.layoutInflater.inflate(R.layout.dialog_api_server, null)

        val builder = MaterialAlertDialogBuilder(activity)
            .setCustomTitle(customTitle)
            .setPositiveButton(getString(R.string.general_ok), null)
            .setNegativeButton(getString(R.string.general_cancel), null)
            .setOnDismissListener {
                preferences.edit().putInt(API_SERVER_CHECKED, INVALID_VALUE).apply()
            }
            .setSingleChoiceItems(
                itemsAdapter,
                apiServerValue
            ) { dialog: DialogInterface, item: Int ->
                itemClicked.set(item)
                preferences.edit().putInt(API_SERVER_CHECKED, item).apply()
                enableOrDisableDialogButton(
                    activity, item != currentApiServerValue,
                    (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                )
            }

        val changeApiServerDialog = builder.create()
        changeApiServerDialog.show()
        changeApiServerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            changeApiServer(activity, megaApi, preferences, changeApiServerDialog)
        }

        enableOrDisableDialogButton(
            activity, apiServerValue != currentApiServerValue,
            changeApiServerDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        )

        return changeApiServerDialog
    }

    /**
     * Points to the new api server selected.
     *
     * @param activity              Current Activity.
     * @param megaApi               MegaApiJava to point launch the requests.
     * @param preferences           Shared preferences to get/set api values.
     * @param changeApiServerDialog AlertDialog where the new api server was selected.
     */
    private fun changeApiServer(
        activity: Activity,
        megaApi: MegaApiJava,
        preferences: SharedPreferences,
        changeApiServerDialog: AlertDialog
    ) {
        val currentApiServerValue = preferences.getInt(API_SERVER, PRODUCTION_SERVER_VALUE)
        val newApiServerValue = preferences.getInt(API_SERVER_CHECKED, currentApiServerValue)

        changeApiServerDialog.dismiss()

        if (newApiServerValue == currentApiServerValue) {
            return
        }

        if (currentApiServerValue == SANDBOX3_SERVER_VALUE) {
            megaApi.setPublicKeyPinning(true)
        } else if (newApiServerValue == SANDBOX3_SERVER_VALUE) {
            megaApi.setPublicKeyPinning(false)
        }

        val apiServer = getApiServerFromValue(newApiServerValue)
        megaApi.changeApiUrl(apiServer)
        preferences.edit().putInt(API_SERVER, getApiServerValue(apiServer))
            .apply()

        val intent = Intent(activity, LoginActivity::class.java)
            .putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
            .setAction(ACTION_REFRESH_API_SERVER)

        activity.startActivityForResult(intent, REQUEST_CODE_REFRESH_API_SERVER)
    }

    /**
     * Gets the preferences value from URL api server.
     *
     * @param apiServer URL api server.
     * @return The api server preferences value.
     */
    @JvmStatic
    fun getApiServerValue(apiServer: String?): Int {
        return when (apiServer) {
            PRODUCTION_SERVER -> PRODUCTION_SERVER_VALUE
            STAGING_SERVER -> STAGING_SERVER_VALUE
            STAGING_444_SERVER -> STAGING_444_SERVER_VALUE
            SANDBOX3_SERVER -> SANDBOX3_SERVER_VALUE
            else -> PRODUCTION_SERVER_VALUE
        }
    }

    /**
     * Gets the URL api server from its preferences value.
     *
     * @param apiServerValue Api server preferences value.
     * @return The URL api server.
     */
    @JvmStatic
    fun getApiServerFromValue(apiServerValue: Int): String {
        return when (apiServerValue) {
            PRODUCTION_SERVER_VALUE -> PRODUCTION_SERVER
            STAGING_SERVER_VALUE -> STAGING_SERVER
            STAGING_444_SERVER_VALUE -> STAGING_444_SERVER
            SANDBOX3_SERVER_VALUE -> SANDBOX3_SERVER
            else -> PRODUCTION_SERVER
        }
    }
}