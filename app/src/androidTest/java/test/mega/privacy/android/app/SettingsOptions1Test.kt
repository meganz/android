package mega.privacy.android.app

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaPushNotificationSettings
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.usecase.SetChatImageQuality
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import nz.mega.sdk.MegaChatApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm20 SettingOptions_1` — the *Portable* scenarios
 * that assert the Settings UI reflecting and persisting setting changes. Built on the `:data-test`
 * fake-SDK framework and following [CloudDriveUploadTest] / [TransferManagerTest]: the whole app runs
 * as in production with only the SDK gateways faked, the UI is driven with UiAutomator (not a Compose
 * rule), and Compose test tags are matched as resource ids where the screen sets
 * `testTagsAsResourceId = true`.
 *
 * Two constraints shape how the sm20 scenarios are ported here:
 *
 * 1. **Only screens that reach the SDK through `MegaApiGateway` / `MegaChatApiGateway` are
 *    fake-backed.** The chat settings presence flows (AND-24539) and the rubbish-bin "empty" action
 *    (AND-24540) are gateway-backed since those refactors landed, which unblocked tc05–tc12 and
 *    tc17 in this suite. Chat notifications reach the SDK through the push-notification settings
 *    attribute, so what the fake replies to each write is what the screen renders next.
 * 2. **Legacy `PreferenceFragment` screens carry no Compose test tags**, so their rows are driven by
 *    visible title text (`By.text`, resolved from the app's string resources). The Compose Camera
 *    Uploads screen and its radio dialogs are fully tagged, so those are driven by `By.res` — the
 *    dialog options are exposed as resource ids because the dialog sets `testTagsAsResourceId = true`
 *    on its own window.
 *
 * Ported: tc01 (theme), tc03 (CU how-to/file-upload options), tc04 (CU keep-file-names + secondary
 * media), tc05 (chat notifications and do not disturb), tc06 (auto-away with timeout),
 * tc07 (status persistence), tc08 (chat online status), tc09 (last-green visibility),
 * tc10 (chat image quality), tc11 (chat video quality), tc12 (rich URL previews),
 * tc13 (calls sound notifications), tc15 (clear offline files), tc17 (empty rubbish bin),
 * tc20 (file-versioning toggle).
 */
@HiltAndroidTest
class SettingsOptions1Test {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeMegaChat: FakeMegaChatApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase

    @Inject
    lateinit var setChatImageQuality: SetChatImageQuality

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private var scenario: ActivityScenario<MegaActivity>? = null

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        // The chat settings screens are legacy activities that recover the chat session through
        // the real MegaChatApi, which derives its database name from a fixed offset in the session
        // and aborts the process on anything shorter.
        fakeMegaApi.account.session = LONG_ENOUGH_SESSION

        // Pre-grant the permissions the settings flows request so their launchers return
        // synchronously instead of showing a system dialog (notifications for boot; media for
        // Camera Uploads).
        listOf(
            android.Manifest.permission.POST_NOTIFICATIONS,
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
        ).forEach { permission ->
            runCatching {
                instrumentation.uiAutomation.grantRuntimePermission(
                    targetContext.packageName,
                    permission,
                )
            }
        }

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Home may resolve Cloud Drive children on start; resolve them through the fake node tree.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        // File versioning is read from the request flag; report it enabled so the versioning toggle
        // starts checked and tc20 can exercise the disable-confirmation path.
        fakeMegaApi.stubRequest(
            MegaApiGateway::getFileVersionsOption,
            request = StubMegaRequest(flag = true),
        )

        fakeMegaApi.clearInvocations()
        fakeMegaChat.clearInvocations()

        // Persist a logged-in session through the app's real credentials path and seed Camera
        // Uploads as enabled so the CU option tiles (tc03/tc04) render without the enable flow's
        // media-permission prompt.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setupCameraUploadsSettingUseCase(true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        // Pop any legacy sub-activities / dialogs left on the stack so tests stay isolated in the
        // shared process.
        repeat(4) { runCatching { device.pressBack() } }
    }

    // region tests

    /** tc01 — choosing a colour theme updates the shown selection (Dark, then Light). */
    @Test
    fun themeSwitchUpdatesShownSelection() {
        openSettings()

        val colourTheme = str("set_color_theme_label")
        val dark = str("theme_dark")
        val light = str("theme_light")

        scrollToText(colourTheme)
        clickText(colourTheme, "colour-theme-row")
        clickText(dark, "dark-option")
        awaitObject(By.text(dark), LOAD_TIMEOUT, "theme-summary-dark")

        scrollToText(colourTheme)
        clickText(colourTheme, "colour-theme-row-reopen")
        clickText(light, "light-option")
        awaitObject(By.text(light), LOAD_TIMEOUT, "theme-summary-light")
    }

    /** tc03 — Camera Uploads how-to-upload and file-upload options update the shown value. */
    @Test
    fun cameraUploadsHowToAndFileUploadOptions() {
        openCameraUploadsSettings()

        click(By.res(HOW_TO_UPLOAD_TILE), LOAD_TIMEOUT, "how-to-upload-tile")
        click(By.res(HOW_TO_UPLOAD_OPTION_WIFI_OR_MOBILE), LOAD_TIMEOUT, "how-to-upload-option")
        awaitObject(By.text(str("cam_sync_data")), LOAD_TIMEOUT, "how-to-upload-summary")

        click(By.res(FILE_UPLOAD_TILE), LOAD_TIMEOUT, "file-upload-tile")
        click(By.res(FILE_UPLOAD_OPTION_VIDEOS_ONLY), LOAD_TIMEOUT, "file-upload-option")
        awaitObject(
            By.text(str("settings_camera_upload_only_videos")),
            LOAD_TIMEOUT,
            "file-upload-summary",
        )
    }

    /** tc04 — Camera Uploads keep-file-names toggles, and secondary media uploads can be enabled. */
    @Test
    fun cameraUploadsKeepFileNamesAndSecondaryMedia() {
        openCameraUploadsSettings()

        awaitObject(By.res(KEEP_FILE_NAMES_CHECKBOX), LOAD_TIMEOUT, "keep-file-names-checkbox")
        val initiallyChecked = device.findObject(By.res(KEEP_FILE_NAMES_CHECKBOX)).isChecked
        device.findObject(By.res(KEEP_FILE_NAMES_CHECKBOX)).click()
        awaitChecked(KEEP_FILE_NAMES_CHECKBOX, expected = !initiallyChecked, name = "keep-file-names")

        // Secondary media uploads start disabled; the row offers to enable, then flips to "Disable".
        click(By.text(str("settings_secondary_upload_on")), LOAD_TIMEOUT, "enable-secondary-media")
        awaitObject(
            By.text(str("settings_secondary_upload_off")),
            LOAD_TIMEOUT,
            "secondary-media-enabled",
        )
    }

    /**
     * tc05 — the chat notifications screen offers the do-not-disturb periods, muting for one of
     * them requests the SDK and reports the mute on both notification screens, and the master
     * chat-notifications switch hides and restores the do-not-disturb row.
     *
     * The SDK owns the resulting push-notification settings and the screens render from its reply,
     * so each step seeds the settings the fake replies with before triggering the write.
     */
    @Test
    fun chatNotificationsDoNotDisturbOptionsAndMasterSwitch() {
        stubPushNotificationSettings(dndEnabled = false, dndTimestamp = DND_UNSET)
        openChatSettings()

        val chatNotifications = str("title_properties_contact_notifications_for_chat")
        val doNotDisturb = str("title_dialog_mute_chat_notifications")
        val notificationsOn = str("mute_chat_notification_option_on")
        val doNotDisturbOff = str("mute_chatroom_notification_option_off")

        awaitObject(preferenceSummary(notificationsOn), LOAD_TIMEOUT, "chat-notifications-summary")
        clickChatPreference(chatNotifications, "chat-notifications-row")

        awaitObject(
            preferenceTitle(str("title_properties_chat_contact_message_sound")),
            LOAD_TIMEOUT,
            "message-sound-row",
        )
        awaitObject(preferenceTitle(str("settings_chat_vibration")), LOAD_TIMEOUT, "vibration-row")
        awaitObject(preferenceSummary(doNotDisturbOff), LOAD_TIMEOUT, "do-not-disturb-summary-off")

        click(preferenceTitle(doNotDisturb), LOAD_TIMEOUT, "do-not-disturb-row")
        awaitObject(
            By.text(str("subtitle_dialog_mute_chat_notifications")),
            LOAD_TIMEOUT,
            "do-not-disturb-dialog",
        )
        listOf(
            muteMinutesOption(30),
            muteHoursOption(1),
            muteHoursOption(6),
            muteHoursOption(24),
            muteUntilMorningOption(),
        ).forEachIndexed { index, option ->
            awaitObject(By.text(option), LOAD_TIMEOUT, "do-not-disturb-option-$index")
        }
        awaitObject(
            By.text(str("general_dialog_cancel_button")),
            LOAD_TIMEOUT,
            "do-not-disturb-dialog-cancel",
        )

        stubPushNotificationSettings(
            dndEnabled = true,
            dndTimestamp = System.currentTimeMillis() / 1000 + THIRTY_MINUTES_IN_SECONDS,
        )
        clickText(muteMinutesOption(30), "do-not-disturb-30-minutes-option")
        clickText(str("general_ok"), "do-not-disturb-dialog-ok")

        awaitInvocations("setPushNotificationSettings") {
            fakeMegaApi.invocations.filter { it.methodName == "setPushNotificationSettings" }
        }
        awaitObject(mutedUntilSummary(), LOAD_TIMEOUT, "do-not-disturb-muted-summary")

        device.pressBack()
        awaitObject(
            preferenceTitle(str("set_status_option_label")),
            LOAD_TIMEOUT,
            "back-on-chat-settings",
        )
        awaitObject(mutedUntilSummary(), LOAD_TIMEOUT, "chat-notifications-muted-summary")
        clickChatPreference(chatNotifications, "chat-notifications-row-reopen")

        stubPushNotificationSettings(dndEnabled = false, dndTimestamp = DND_UNSET)
        click(preferenceTitle(chatNotifications), LOAD_TIMEOUT, "chat-notifications-switch-on")
        awaitObject(
            preferenceSummary(doNotDisturbOff),
            LOAD_TIMEOUT,
            "do-not-disturb-summary-off-again",
        )

        stubPushNotificationSettings(dndEnabled = true, dndTimestamp = DND_INDEFINITE)
        click(preferenceTitle(chatNotifications), LOAD_TIMEOUT, "chat-notifications-switch-off")
        awaitGone(preferenceTitle(doNotDisturb), "do-not-disturb-row-hidden")

        stubPushNotificationSettings(dndEnabled = false, dndTimestamp = DND_UNSET)
        click(preferenceTitle(chatNotifications), LOAD_TIMEOUT, "chat-notifications-switch-on-again")
        awaitObject(preferenceTitle(doNotDisturb), LOAD_TIMEOUT, "do-not-disturb-row-restored")

        device.pressBack()
        awaitObject(
            preferenceTitle(str("set_status_option_label")),
            LOAD_TIMEOUT,
            "back-on-chat-settings-again",
        )
        awaitObject(
            preferenceSummary(notificationsOn),
            LOAD_TIMEOUT,
            "chat-notifications-summary-on-again",
        )
    }

    /**
     * tc08 — changing the chat online status through the status dialog requests the SDK with
     * the matching status value and updates the shown selection (away, busy, offline, then online).
     * The shown status comes from the presence config the screen reads when it opens, so each
     * change is confirmed on a freshly entered screen.
     *
     * Carries the demo-clip camera holds: short pauses on each state, ending on a 2s hold.
     */
    @Test
    fun chatOnlineStatusChangesUpdateSummaryAndRequestGateway() {
        openChatSettings()

        val statusRow = str("set_status_option_label")
        val dialogCancel = targetContext.getString(android.R.string.cancel)
        Thread.sleep(1_000)

        listOf(
            str("settings_chat_status_away") to MegaChatApi.STATUS_AWAY,
            str("settings_chat_status_busy") to MegaChatApi.STATUS_BUSY,
            str("settings_chat_status_offline") to MegaChatApi.STATUS_OFFLINE,
            str("settings_chat_status_online") to MegaChatApi.STATUS_ONLINE,
        ).forEach { (statusLabel, sdkStatus) ->
            clickText(statusRow, "status-row")
            // The status dialog renders in its own window; its Cancel button is the only element
            // not also present on the screen behind it.
            awaitObject(By.text(dialogCancel), LOAD_TIMEOUT, "status-dialog")
            Thread.sleep(1_000)
            clickText(statusLabel, "status-option-$sdkStatus")
            awaitGone(By.text(dialogCancel), "status-dialog-dismissed")

            awaitInvocations("setOnlineStatus($sdkStatus)") {
                fakeMegaChat.invocationsOf(MegaChatApiGateway::setOnlineStatus)
                    .filter { it.arguments.firstOrNull() == sdkStatus }
            }
            reopenChatSettings(statusLabel)
            Thread.sleep(1_000)
        }

        Thread.sleep(2_000)
    }

    /**
     * tc06 — enabling auto-away requests the default timeout, its dialog sets a custom one and
     * clamps out-of-range values, and turning auto-away back off hides the timeout row.
     *
     * The timeout summary is rendered from the presence config, which the screen reads when it
     * opens, so each clamped value is verified on a freshly entered screen.
     */
    @Test
    fun chatAutoAwayEnableAndTimeoutDialogRequestGateway() {
        openChatSettings()

        val autoAway = str("settings_autoaway_title")
        val autoAwayTimeout = str("settings_autoaway_subtitle")

        clickChatPreference(autoAway, "auto-away-switch")
        awaitInvocations("setPresenceAutoaway(true, 300)") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setPresenceAutoaway)
                .filter { it.arguments[0] == true && it.arguments[1] == 300 }
        }
        // The timeout row appears with the default 5-minute summary once auto-away is on.
        awaitObject(
            By.text(str("settings_autoaway_value", 5)),
            LOAD_TIMEOUT,
            "auto-away-default-summary",
        )

        setAutoAwayTimeout("10")
        awaitInvocations("setPresenceAutoaway(true, 600)") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setPresenceAutoaway)
                .filter { it.arguments[0] == true && it.arguments[1] == 600 }
        }

        setAutoAwayTimeout("9999")
        awaitInvocations("setPresenceAutoaway(true, ${MAX_AUTO_AWAY_MINUTES * 60})") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setPresenceAutoaway)
                .filter { it.arguments[0] == true && it.arguments[1] == MAX_AUTO_AWAY_MINUTES * 60 }
        }
        reopenChatSettings()
        awaitObject(
            preferenceSummary(str("settings_autoaway_value", MAX_AUTO_AWAY_MINUTES)),
            LOAD_TIMEOUT,
            "auto-away-clamped-to-maximum-summary",
        )

        setAutoAwayTimeout("0")
        awaitInvocations("setPresenceAutoaway(true, 60)") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setPresenceAutoaway)
                .filter { it.arguments[0] == true && it.arguments[1] == 60 }
        }
        reopenChatSettings()
        awaitObject(
            preferenceSummary(str("settings_autoaway_value", 1)),
            LOAD_TIMEOUT,
            "auto-away-clamped-to-minimum-summary",
        )

        clickChatPreference(autoAway, "auto-away-switch-off")
        awaitGone(preferenceTitle(autoAwayTimeout), "auto-away-timeout-row-hidden")
        awaitInvocations("setPresenceAutoaway(false, 0)") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setPresenceAutoaway)
                .filter { it.arguments[0] == false }
        }
    }

    /**
     * tc07 — the shown status follows the presence config, and persisting the status requests the
     * SDK and hides the auto-away rows until persistence is turned back off.
     */
    @Test
    fun chatPersistStatusToggleRequestsGateway() {
        openChatSettings()

        val persistence = str("settings_persistence_title")
        val autoAway = str("settings_autoaway_title")

        awaitObject(
            preferenceSummary(str("settings_chat_status_online")),
            LOAD_TIMEOUT,
            "status-summary-online",
        )
        awaitObject(
            preferenceSummary(str("settings_persistence_subtitle")),
            LOAD_TIMEOUT,
            "persistence-row",
        )
        awaitObject(preferenceTitle(autoAway), LOAD_TIMEOUT, "auto-away-row")

        clickChatPreference(persistence, "persistence-row-on")
        awaitInvocations("setPresencePersist(true)") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setPresencePersist)
                .filter { it.arguments.firstOrNull() == true }
        }

        // The presence rows are rendered from the presence config the screen reads when it opens.
        reopenChatSettings()
        awaitObject(
            preferenceSummary(str("settings_persistence_subtitle")),
            LOAD_TIMEOUT,
            "persistence-row-persisting",
        )
        awaitGone(preferenceTitle(autoAway), "auto-away-row-hidden")

        clickChatPreference(persistence, "persistence-row-off")
        awaitInvocations("setPresencePersist(false)") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setPresencePersist)
                .filter { it.arguments.firstOrNull() == false }
        }

        reopenChatSettings()
        awaitObject(preferenceTitle(autoAway), LOAD_TIMEOUT, "auto-away-row-restored")
    }

    /**
     * tc09 — toggling last-seen visibility requests the SDK. The row is interactable because the
     * fake's default presence config (Online status) has landed by the time [openChatSettings]
     * returns — the fragment disables it while the config is missing.
     */
    @Test
    fun chatLastGreenVisibilityToggleRequestsGateway() {
        openChatSettings()

        val lastGreen = str("option_enable_last_green_chat")
        scrollToText(lastGreen)
        clickChatPreference(lastGreen, "last-green-row")

        awaitInvocations("setLastGreenVisible(true)") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::setLastGreenVisible)
                .filter { it.arguments.firstOrNull() == true }
        }
    }

    /** tc10 — the chat image quality picked on its own screen becomes the Chat settings summary. */
    @Test
    fun chatImageQualitySelectionUpdatesSummary() {
        runBlocking { setChatImageQuality(ChatImageQuality.Automatic) }
        openChatSettings()

        val imageQuality = str("settings_chat_image_quality")
        scrollToText(imageQuality)
        awaitObject(
            preferenceSummary(str("automatic_image_quality")),
            LOAD_TIMEOUT,
            "image-quality-summary-automatic",
        )

        listOf("high_image_quality", "optimised_image_quality").forEach { quality ->
            scrollToText(imageQuality)
            clickChatPreference(imageQuality, "image-quality-row")
            awaitObject(
                By.text(str("automatic_image_quality_text")),
                LOAD_TIMEOUT,
                "image-quality-screen",
            )
            clickText(str(quality), "image-quality-option-$quality")

            device.pressBack()
            awaitObject(
                preferenceSummary(str(quality)),
                LOAD_TIMEOUT,
                "image-quality-summary-$quality",
            )
        }
    }

    /** tc11 — the chat video quality picked in its dialog becomes the Chat settings summary. */
    @Test
    fun chatVideoQualitySelectionUpdatesSummary() {
        openChatSettings()

        val videoQuality = str("settings_chat_upload_quality")
        val low = str("settings_chat_upload_quality_low")
        scrollToText(videoQuality)
        clickChatPreference(videoQuality, "video-quality-row")

        listOf(
            low,
            str("settings_chat_upload_quality_medium"),
            str("settings_chat_upload_quality_high"),
            str("settings_chat_upload_quality_original"),
        ).forEachIndexed { index, option ->
            awaitObject(By.text(option), LOAD_TIMEOUT, "video-quality-option-$index")
        }
        clickText(low, "video-quality-low-option")

        awaitObject(preferenceSummary(low), LOAD_TIMEOUT, "video-quality-summary-low")
    }

    /** tc12 — toggling rich URL previews requests the SDK to enable them. */
    @Test
    fun chatRichUrlPreviewsToggleRequestsGateway() {
        openChatSettings()

        val richPreviews = str("option_enable_chat_rich_preview")
        scrollToText(richPreviews)
        clickChatPreference(richPreviews, "rich-previews-row")

        awaitInvocations("enableRichPreviews(true)") {
            fakeMegaApi.invocations.filter {
                it.methodName == "enableRichPreviews" && it.arguments.firstOrNull() == true
            }
        }
    }

    /** tc13 — the Calls settings screen shows the sound-notifications option. */
    @Test
    fun callsSettingsShowsSoundNotifications() {
        openSettings()

        val calls = str("settings_calls_title")
        scrollToText(calls)
        clickText(calls, "calls-row")

        awaitObject(
            By.text(str("settings_calls_sound_notifications_title")),
            LOAD_TIMEOUT,
            "sound-notifications-label",
        )
    }

    /** tc15 — clearing offline files shows the confirmation dialog and completes. */
    @Test
    fun clearOfflineFilesConfirmationFlow() {
        openFileManagementSettings()

        val clearOffline = str("settings_advanced_features_offline")
        scrollToText(clearOffline)
        clickText(clearOffline, "clear-offline-row")

        val confirmation = str("clear_offline_confirmation")
        awaitObject(By.text(confirmation), LOAD_TIMEOUT, "clear-offline-dialog")
        clickText(str("general_clear"), "clear-offline-confirm")
        awaitGone(By.text(confirmation), "clear-offline-dialog-dismissed")
    }

    /** tc17 — emptying the rubbish bin confirms, requests the SDK and shows the success snackbar. */
    @Test
    fun emptyRubbishBinConfirmationRequestsGatewayAndShowsSnackbar() {
        openFileManagementSettings()

        val emptyRubbishBin = str("empty_rubbish_bin_menu")
        scrollToText(emptyRubbishBin)
        clickText(emptyRubbishBin, "empty-rubbish-bin-row")

        awaitObject(
            By.text(str("remove_all_rubbish_bin_confirmation")),
            LOAD_TIMEOUT,
            "empty-rubbish-bin-dialog",
        )
        clickText(str("general_clear"), "empty-rubbish-bin-confirm")

        awaitInvocations("cleanRubbishBin") {
            fakeMegaApi.invocations.filter { it.methodName == "cleanRubbishBin" }
        }
        awaitObject(
            By.text(str("empty_rubbish_bin_success_message")),
            LOAD_TIMEOUT,
            "empty-rubbish-bin-snackbar",
        )
    }

    /** tc20 — disabling file versioning confirms and requests the SDK to disable it. */
    @Test
    fun fileVersioningDisableRequestsGateway() {
        openFileManagementSettings()

        val versioning = str("settings_enable_file_versioning_title")
        scrollToText(versioning)
        clickText(versioning, "file-versioning-row")

        awaitObject(By.text(str("disable_versioning_label")), LOAD_TIMEOUT, "disable-versioning-dialog")
        clickText(str("verify_2fa_subtitle_diable_2fa"), "disable-versioning-confirm")

        awaitInvocations("setFileVersionsOption") {
            fakeMegaApi.invocations.filter { it.methodName == "setFileVersionsOption" }
        }
    }

    // endregion

    // region navigation helpers

    /** Launches the logged-in app and opens the legacy Settings screen via the Menu tab. */
    private fun openSettings() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)

        click(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "menu-nav-item")

        val settings = str("general_settings")
        scrollToText(settings)
        clickText(settings, "settings-menu-item")

        // The Appearance "Colour theme" row is near the top of the settings list; use it as the
        // "settings screen is up" signal.
        awaitObject(By.text(str("set_color_theme_label")), LOAD_TIMEOUT, "settings-screen")
    }

    private fun openFileManagementSettings() {
        openSettings()
        val fileManagement = str("settings_file_management_category")
        scrollToText(fileManagement)
        clickText(fileManagement, "file-management-row")
    }

    private fun openChatSettings() {
        openSettings()
        enterChatSettings(str("settings_chat_status_online"))
    }

    /**
     * Returns to Settings and enters Chat settings again. The presence rows are rendered from the
     * presence config the screen reads when it opens, so a presence change only shows on a screen
     * entered after it.
     */
    private fun reopenChatSettings(shownStatus: String = str("settings_chat_status_online")) {
        device.pressBack()
        // The Chat settings toolbar carries the same title as the Settings row that opens it, so
        // confirm the screen behind is up before entering again.
        awaitObject(By.text(str("set_color_theme_label")), LOAD_TIMEOUT, "back-on-settings-screen")
        enterChatSettings(shownStatus)
    }

    private fun enterChatSettings(shownStatus: String) {
        val chat = str("section_chat")
        scrollToText(chat)
        // The Chat row enables asynchronously once the chat session and connectivity land.
        awaitObject(By.text(chat).enabled(true), LOAD_TIMEOUT, "chat-row-enabled")
        clickText(chat, "chat-row")
        // The status row enables once the presence config, connectivity and root node all land.
        awaitObject(
            By.text(str("set_status_option_label")).enabled(true),
            LOAD_TIMEOUT,
            "chat-settings-screen",
        )
        // The shown status is only written once the presence config has been applied, so it marks
        // the point where the presence rows reflect the config rather than the layout defaults.
        awaitObject(preferenceSummary(shownStatus), LOAD_TIMEOUT, "chat-settings-shown-status")
    }

    /** Opens the auto-away timeout dialog and confirms [minutes] as the new value. */
    private fun setAutoAwayTimeout(minutes: String) {
        clickChatPreference(str("settings_autoaway_subtitle"), "auto-away-timeout-row")
        awaitObject(
            By.text(str("title_dialog_set_autoaway_value")),
            LOAD_TIMEOUT,
            "auto-away-dialog",
        )
        val input = By.res(viewId("autoaway_edittext"))
        awaitObject(input, LOAD_TIMEOUT, "auto-away-input")
        device.findObject(input).text = minutes
        click(By.res(viewId("autoaway_set_button")), LOAD_TIMEOUT, "auto-away-set-button")
        awaitGone(By.text(str("title_dialog_set_autoaway_value")), "auto-away-dialog-dismissed")
    }

    private fun openCameraUploadsSettings() {
        openSettings()
        val cameraUploads = str("section_photo_sync")
        scrollToText(cameraUploads)
        clickText(cameraUploads, "camera-uploads-row")
        // The option tiles render because Camera Uploads is seeded enabled in setUp().
        awaitObject(By.res(HOW_TO_UPLOAD_TILE), LOAD_TIMEOUT, "camera-uploads-options")
    }

    // endregion

    // region invocation helpers

    /**
     * Preference / gateway writes are dispatched from background scopes, so invocations are not
     * recorded synchronously after the UI action. Poll [supplier] until at least one match, or fail
     * with what was actually recorded. Mirrors [TransferManagerTest.awaitInvocations].
     */
    private fun <T> awaitInvocations(
        name: String,
        timeoutMs: Long = LOAD_TIMEOUT,
        supplier: () -> List<T>,
    ): List<T> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${timeoutMs}ms waiting for an invocation of $name; " +
                    "recorded api methods=${fakeMegaApi.invocations.map { it.methodName }}, " +
                    "recorded chat methods=${fakeMegaChat.invocations.map { it.methodName }}",
        )
    }

    // endregion

    // region push notification settings helpers

    /**
     * Seeds what the fake replies with for the next push-notification settings read or write. The
     * SDK reply is the single source the notification screens render from, and the stub cannot
     * derive a new mute state from the settings the app hands it, so tests state it up front.
     */
    private fun stubPushNotificationSettings(dndEnabled: Boolean, dndTimestamp: Long) {
        val settings = StubMegaPushNotificationSettings(
            globalChatsDndEnabled = dndEnabled,
            globalChatsDnd = dndTimestamp,
        )
        fakeMegaApi.stubRequest(
            MegaApiGateway::getPushNotificationSettings,
            request = StubMegaRequest(megaPushNotificationSettings = settings),
        )
        fakeMegaApi.stubRequest(
            MegaApiGateway::setPushNotificationSettings,
            request = StubMegaRequest(megaPushNotificationSettings = settings),
        )
    }

    private fun muteMinutesOption(minutes: Int) = TextUtil.removeFormatPlaceholder(
        plural("plural_call_ended_messages_minutes", minutes, minutes),
    )

    private fun muteHoursOption(hours: Int) = TextUtil.removeFormatPlaceholder(
        plural("plural_call_ended_messages_hours", hours, hours),
    )

    /** The last mute option, which names the next morning the mute would end on. */
    private fun muteUntilMorningOption() = str(
        if (TimeUtils.isUntilThisMorning()) "mute_chatroom_notification_option_until_this_morning"
        else "mute_chatroom_notification_option_until_tomorrow_morning"
    )

    /**
     * Matches a preference summary reporting a mute end time. The end time itself is a formatted
     * clock value, so only the leading part of the plural is matched.
     */
    private fun mutedUntilSummary(): BySelector = By.res(PREFERENCE_SUMMARY_ID)
        .textContains(plural("chat_notifications_muted_until_specific_time", 1, "").trim())

    // endregion

    // region UiAutomator helpers

    private fun str(name: String): String {
        val id = targetContext.resources.getIdentifier(name, "string", targetContext.packageName)
        require(id != 0) { "string resource not found: $name" }
        return targetContext.getString(id)
    }

    private fun str(name: String, vararg args: Any): String {
        val id = targetContext.resources.getIdentifier(name, "string", targetContext.packageName)
        require(id != 0) { "string resource not found: $name" }
        return targetContext.getString(id, *args)
    }

    private fun plural(name: String, quantity: Int, vararg args: Any): String {
        val id = targetContext.resources.getIdentifier(name, "plurals", targetContext.packageName)
        require(id != 0) { "plurals resource not found: $name" }
        return targetContext.resources.getQuantityString(id, quantity, *args)
    }

    /** Fully-qualified resource id for a legacy view id in the app package. */
    private fun viewId(name: String): String = "${targetContext.packageName}:id/$name"

    /**
     * Matches the title of a preference row. Scoping to the row's own text view keeps a match off
     * the toolbar, which on some settings screens carries the same title as one of its rows.
     */
    private fun preferenceTitle(title: String): BySelector =
        By.res(PREFERENCE_TITLE_ID).text(title)

    private fun preferenceSummary(summary: String): BySelector =
        By.res(PREFERENCE_SUMMARY_ID).text(summary)

    private fun click(selector: BySelector, timeout: Long, name: String) {
        awaitObject(selector, timeout, name)
        device.findObject(selector).click()
    }

    private fun clickText(text: String, name: String) = click(By.text(text), LOAD_TIMEOUT, name)

    /**
     * Clicks a chat-settings preference row once it is enabled. The presence rows are re-added and
     * re-enabled asynchronously as the presence config, connectivity and root node land, so a plain
     * text click can land on a still-disabled row and be dropped.
     */
    private fun clickChatPreference(title: String, name: String) =
        click(preferenceTitle(title).enabled(true), LOAD_TIMEOUT, name)

    private fun awaitChecked(tag: String, expected: Boolean, name: String) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.findObject(By.res(tag))?.isChecked == expected) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        dumpHierarchy(name)
        throw AssertionError("Timed out waiting for $name checkbox to be checked=$expected")
    }

    /**
     * Scrolls the first scrollable container until [text] is on screen. No-op when it is already
     * visible or nothing is scrollable — legacy preference lists and the Compose menu list both
     * expose a scrollable container.
     */
    private fun scrollToText(text: String) {
        if (device.hasObject(By.text(text))) return
        runCatching {
            UiScrollable(UiSelector().scrollable(true))
                .apply { setMaxSearchSwipes(MAX_SCROLL_SWIPES) }
                .scrollTextIntoView(text)
        }
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
        )
    }

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to disappear; hierarchy in logcat tag UiDump",
        )
    }

    private fun dumpHierarchy(name: String) {
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
            Log.d("UiDump", "[$name#$index] $chunk")
        }
    }

    // endregion

    private companion object {
        const val LAUNCH_TIMEOUT = 30_000L
        const val LOAD_TIMEOUT = 15_000L
        const val POLL_INTERVAL_MS = 100L
        const val MAX_SCROLL_SWIPES = 12

        /** A session as long as a real one, so the chat SDK can name its database from it. */
        const val LONG_ENOUGH_SESSION =
            "fakeSessionfakeSessionfakeSessionfakeSessionfakeSessionfakeSession"

        /** Auto-away timeout bound the app clamps user input to, in minutes. */
        const val MAX_AUTO_AWAY_MINUTES = 1457

        const val THIRTY_MINUTES_IN_SECONDS = 30 * 60L

        /** Global chat do-not-disturb values: never set, and muted until turned back on. */
        const val DND_UNSET = -1L
        const val DND_INDEFINITE = 0L

        /** Title and summary text views of a legacy preference row. */
        const val PREFERENCE_TITLE_ID = "android:id/title"
        const val PREFERENCE_SUMMARY_ID = "android:id/summary"

        /** Main navigation item test tag for the Menu tab (`MenuHomeScreen` destination). */
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Camera Uploads option tile tags (Compose, `testTagsAsResourceId`). */
        const val HOW_TO_UPLOAD_TILE = "how_to_upload_tile:generic_two_line_list_item"
        const val FILE_UPLOAD_TILE = "file_upload_tile:generic_two_line_list_item"
        const val KEEP_FILE_NAMES_CHECKBOX = "keep_file_names_tile:mega_checkbox"

        /**
         * Radio-dialog option tags. The dialog sets `testTagsAsResourceId = true` on its own window,
         * so these resolve by resource id even though the dialog renders in a separate window. The
         * suffix is the option enum's name.
         */
        const val HOW_TO_UPLOAD_OPTION_WIFI_OR_MOBILE =
            "confirmation_dialog_with_radio_buttons:text_option_WIFI_OR_MOBILE_DATA"
        const val FILE_UPLOAD_OPTION_VIDEOS_ONLY =
            "confirmation_dialog_with_radio_buttons:text_option_VideosOnly"
    }
}
