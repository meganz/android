package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.view.PasscodeDialog
import mega.privacy.android.app.utils.AlertDialogUtil.enableOrDisableDialogButton
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.REQUIRE_PASSCODE_INVALID
import mega.privacy.android.app.utils.TextUtil.removeFormatPlaceholder
import mega.privacy.android.app.utils.wrapper.PasscodePreferenceWrapper
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeLockStateUseCase
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class PasscodeUtil @Inject constructor(
    @ActivityContext private val context: Context,
    private val passcodePreferenceWrapper: PasscodePreferenceWrapper,
    @ApplicationScope private val scope: CoroutineScope,
    private val passcodeManagement: PasscodeManagement,
    private val monitorPasscodeLockStateUseCase: MonitorPasscodeLockStateUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getThemeMode: GetThemeMode,
) {

    companion object {
        const val REQUIRE_PASSCODE_IMMEDIATE = 500
        const val REQUIRE_PASSCODE_AFTER_5S = 5 * 1000
        const val REQUIRE_PASSCODE_AFTER_10S = 10 * 1000
        const val REQUIRE_PASSCODE_AFTER_30S = 30 * 1000
        const val REQUIRE_PASSCODE_AFTER_1M = 60 * 1000
        const val REQUIRE_PASSCODE_AFTER_2M = 60 * 2 * 1000
        const val REQUIRE_PASSCODE_AFTER_5M = 60 * 5 * 1000

        const val FIVE_SECONDS_OR_MINUTES = 5
        const val TEN_SECONDS = 10
        const val THIRTY_SECONDS = 30
        const val ONE_MINUTE = 1
        const val TWO_MINUTES = 2
    }

    /**
     * Shows a dialog to choose the required time to ask for passcode.
     * @param itemChecked The option to set as selected if after rotation, invalid option otherwise.
     * @return The AlertDialog.
     */
    suspend fun showRequirePasscodeDialog(itemChecked: Int, context: Context): AlertDialog {
        val dialogBuilder =
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
        dialogBuilder.setTitle(context.getString(R.string.settings_require_passcode))

        val options = ArrayList<String>()
        options.add(context.getString(R.string.action_immediately))

        options.add(
            removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_seconds,
                    FIVE_SECONDS_OR_MINUTES,
                    FIVE_SECONDS_OR_MINUTES
                )
            )
        )

        options.add(
            removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_seconds,
                    TEN_SECONDS,
                    TEN_SECONDS
                )
            )
        )

        options.add(
            removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_seconds,
                    THIRTY_SECONDS,
                    THIRTY_SECONDS
                )
            )
        )

        options.add(
            removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_minutes,
                    ONE_MINUTE,
                    ONE_MINUTE
                )
            )
        )

        options.add(
            removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_minutes,
                    TWO_MINUTES,
                    TWO_MINUTES
                )
            )
        )

        options.add(
            removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_minutes,
                    FIVE_SECONDS_OR_MINUTES,
                    FIVE_SECONDS_OR_MINUTES
                )
            )
        )

        val optionsAdapter = ArrayAdapter(
            context,
            R.layout.checked_text_view_dialog_button,
            options
        )
        val listView = ListView(context)
        listView.adapter = optionsAdapter
        val itemClicked = AtomicReference<Int>()
        if (itemChecked != INVALID_POSITION) {
            itemClicked.set(itemChecked)
        }

        val initialRequiredTime =
            getPasscodeRequireTimeOption(passcodePreferenceWrapper.getPasscodeTimeOut())

        dialogBuilder.setSingleChoiceItems(
            optionsAdapter,
            if (itemChecked != INVALID_POSITION) itemChecked else initialRequiredTime
        ) { dialog: DialogInterface, item: Int ->
            itemClicked.set(item)
            enableOrDisableDialogButton(
                context,
                initialRequiredTime != item,
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            )
        }

        dialogBuilder.setPositiveButton(
            context.getString(R.string.general_ok)
        ) { _: DialogInterface, _: Int ->
            if (itemClicked.get() != initialRequiredTime) {
                scope.launch {
                    passcodePreferenceWrapper.setPasscodeTimeOut(
                        getPasscodeRequireTime(
                            itemClicked.get()
                        )
                    )
                }
            }
        }

        dialogBuilder.setNegativeButton(context.getString(R.string.general_cancel), null)

        val requirePasscodeDialog = dialogBuilder.create()
        requirePasscodeDialog.show()

        enableOrDisableDialogButton(
            context,
            itemChecked != INVALID_POSITION && itemChecked != initialRequiredTime,
            requirePasscodeDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        )

        return requirePasscodeDialog
    }

    /**
     * Gets the time chosen depending on the clicked option.
     *
     * @param optionClicked The option clicked.
     * @return The time chosen.
     */
    private fun getPasscodeRequireTime(optionClicked: Int): Int {
        return when (optionClicked) {
            1 -> REQUIRE_PASSCODE_AFTER_5S
            2 -> REQUIRE_PASSCODE_AFTER_10S
            3 -> REQUIRE_PASSCODE_AFTER_30S
            4 -> REQUIRE_PASSCODE_AFTER_1M
            5 -> REQUIRE_PASSCODE_AFTER_2M
            6 -> REQUIRE_PASSCODE_AFTER_5M
            else -> REQUIRE_PASSCODE_IMMEDIATE
        }
    }

    /**
     * Gets the option value depending on the required time set.
     *
     * @param requiredTime The required time set.
     * @return The option value.
     */
    private fun getPasscodeRequireTimeOption(requiredTime: Int): Int {
        return when (requiredTime) {
            REQUIRE_PASSCODE_IMMEDIATE -> 0
            REQUIRE_PASSCODE_AFTER_5S -> 1
            REQUIRE_PASSCODE_AFTER_10S -> 2
            REQUIRE_PASSCODE_AFTER_30S -> 3
            REQUIRE_PASSCODE_AFTER_1M -> 4
            REQUIRE_PASSCODE_AFTER_2M -> 5
            REQUIRE_PASSCODE_AFTER_5M -> 6
            else -> INVALID_POSITION
        }
    }

    /**
     * Gets the text to show in the Preference depending on the time set.
     *
     * @param requiredTime The required time set.
     * @return The string to show.
     */
    fun getRequiredPasscodeText(requiredTime: Int): String {
        val requiredTimeValue = getPasscodeRequireTimeValue(requiredTime)

        return when (requiredTime) {
            REQUIRE_PASSCODE_AFTER_5S, REQUIRE_PASSCODE_AFTER_10S, REQUIRE_PASSCODE_AFTER_30S -> removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_seconds,
                    requiredTimeValue,
                    requiredTimeValue
                )
            )

            REQUIRE_PASSCODE_AFTER_1M, REQUIRE_PASSCODE_AFTER_2M, REQUIRE_PASSCODE_AFTER_5M -> removeFormatPlaceholder(
                context.resources.getQuantityString(
                    R.plurals.plural_call_ended_messages_minutes,
                    requiredTimeValue,
                    requiredTimeValue
                )
            )

            else -> context.getString(R.string.action_immediately)
        }
    }

    /**
     * Gets the value to set in the string which will be shown in the Preference.
     *
     * @param requiredTime The required time set.
     * @return The value to set in the string.
     */
    private fun getPasscodeRequireTimeValue(requiredTime: Int): Int {
        return when (requiredTime) {
            REQUIRE_PASSCODE_AFTER_5S -> FIVE_SECONDS_OR_MINUTES
            REQUIRE_PASSCODE_AFTER_10S -> TEN_SECONDS
            REQUIRE_PASSCODE_AFTER_30S -> THIRTY_SECONDS
            REQUIRE_PASSCODE_AFTER_1M -> ONE_MINUTE
            REQUIRE_PASSCODE_AFTER_2M -> TWO_MINUTES
            REQUIRE_PASSCODE_AFTER_5M -> FIVE_SECONDS_OR_MINUTES
            else -> REQUIRE_PASSCODE_IMMEDIATE
        }
    }

    fun enablePasscode(passcodeType: String, passcode: String) {
        updatePasscode(true, passcodeType, passcode, REQUIRE_PASSCODE_AFTER_30S)
        pauseUpdate()
    }

    fun disablePasscode() {
        updatePasscode(false, "", "", REQUIRE_PASSCODE_INVALID)
    }

    private fun updatePasscode(enable: Boolean, type: String, passcode: String, requiredTime: Int) {
        scope.launch {
            passcodePreferenceWrapper.setPasscodeEnabled(enable)
            passcodePreferenceWrapper.setPasscodeLockType(type)
            passcodePreferenceWrapper.setPasscode(passcode)
            if (enable && passcodePreferenceWrapper.getPasscodeTimeOut() == REQUIRE_PASSCODE_INVALID) {
                passcodePreferenceWrapper.setPasscodeTimeOut(requiredTime)
            }
        }
    }

    /**
     * Method to get the time set for passcode lock
     *
     * @return time set for passcode lock
     */
    suspend fun timeRequiredForPasscode(): Int {
        val enabled = passcodePreferenceWrapper.isPasscodeEnabled()
        val code = passcodePreferenceWrapper.getPasscode()
        return if (enabled
            && code != null
        ) {
            passcodePreferenceWrapper.getPasscodeTimeOut()
        } else REQUIRE_PASSCODE_INVALID
    }

    /**
     * Checks if should lock the app and show the passcode screen.
     *
     * @return True if should lock the app, false otherwise.
     */
    suspend fun shouldLock(): Boolean {
        val enabled = passcodePreferenceWrapper.isPasscodeEnabled()
        val code = passcodePreferenceWrapper.getPasscode()
        val timeOut = passcodePreferenceWrapper.getPasscodeTimeOut()
        return if (enabled
            && code != null
            && timeOut != REQUIRE_PASSCODE_INVALID
        ) {
            val currentTime = System.currentTimeMillis()
            val lastPaused = passcodeManagement.lastPause

            Timber.d("Time: $currentTime lastPause: $lastPaused")

            currentTime - lastPaused > timeOut
        } else false
    }

    /**
     * Called after resume some activity to check if should lock or not the app.
     */
    fun resume() {
        runBlocking {
            if (shouldLock()) {
                showLockScreen()
            }
        }
    }

    /**
     * Called when some activity is paused to update the lastPause value.
     */
    fun pauseUpdate() {
        passcodeManagement.lastPause = System.currentTimeMillis()
    }

    /**
     * Called when PasscodeLock activity is resumed to reset the lastPause value.
     */
    fun resetLastPauseUpdate() {
        passcodeManagement.lastPause = 0
    }

    /**
     * Launches an intent to show passcode screen when the app is locked
     */
    private suspend fun showLockScreen() {
        val uiFlag = getFeatureFlagValueUseCase(AppFeatures.Passcode)
        val backendFlag = getFeatureFlagValueUseCase(AppFeatures.PasscodeBackend)
        if (uiFlag && backendFlag) {
            val activity = context as Activity
            val themeMode = getThemeMode().first()
            val view = ComposeView(activity)
                .apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        val locked: Boolean by monitorPasscodeLockStateUseCase().collectAsStateWithLifecycle(
                            initialValue = true
                        )
                        if (locked) {
                            AndroidTheme(isDark = themeMode.isDarkMode()) {
                                PasscodeDialog()
                            }
                        }
                    }
                }

            activity.addContentView(
                view,
                (activity.findViewById(android.R.id.content) as ViewGroup).layoutParams
            )

        } else {
            context.startActivity(
                Intent(context, PasscodeLockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

    }
}