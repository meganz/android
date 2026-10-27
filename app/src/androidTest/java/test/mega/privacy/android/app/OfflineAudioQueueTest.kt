package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/**
 * Full-app instrumented test for SAO-2027 / T21460436: the audio player queue lists the same
 * audio files as the Offline folder that the user opens a file from.
 *
 * Making a file available offline from inside a cloud folder also stores every folder above it,
 * so that the offline files mirror the cloud folder structure on disk. The test seeds two audio
 * files made available offline from different cloud folders: `Music/Album A/Morning Walk.wav` and
 * `Podcasts/Episode 12.wav`. Both files are offline roots, so both show at the top level of
 * Offline, while their folders are stored only as ancestors. The journey opens one file from the
 * top level and checks that the player queue also lists the other one.
 *
 * The revamped audio player is used ([ApiFeatures.AudioPlayerRevamp]) because it opens as a
 * Compose route in [MegaActivity]. The legacy `AudioPlayerActivity` is a `BaseActivity` and cannot
 * run on the fake-SDK framework. Both players build the offline queue from the same parent id.
 *
 * Offline rows are seeded through [NodeRepository.saveOfflineNodeInformation], the production
 * persistence path, after [NodeRepository.clearOffline] removes rows left by earlier runs. Each
 * audio file is written as 1 s of silent WAV audio at the path that [GetOfflineFileUseCase]
 * resolves, so that the player can open it. The `hold` calls pause the journey at each state so
 * that a screen recording for the MR shows every state.
 */
@HiltAndroidTest
class OfflineAudioQueueTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var nodeRepository: NodeRepository

    @Inject
    lateinit var getOfflineFileUseCase: GetOfflineFileUseCase

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val offlineFilesLabel
        get() = targetContext.getString(sharedR.string.general_section_offline_files)

    private var scenario: ActivityScenario<MegaActivity>? = null

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

        TestAppBoot.runCoreInitializers()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }

        fakeFlags.set(ApiFeatures.AudioPlayerRevamp, true)
    }

    @After
    fun tearDown() {
        // Opening the audio player delivers a new intent to the running MegaActivity. After that,
        // ActivityScenario no longer receives its lifecycle changes, so close() waits 45 s for a
        // destroy that it does not see. The activity is finished directly instead.
        runCatching { scenario?.onActivity { it.finish() } }
            .onFailure { Timber.w(it, "Could not finish MegaActivity") }
        runBlocking { nodeRepository.clearOffline() }
        fakeFlags.clear()
    }

    @Test
    fun theQueueListsEveryAudioFileOfTheOfflineFolderTheFileWasOpenedFrom() {
        seedOfflineAudioFiles()

        launchOffline()
        awaitObject(titleOf(PODCAST_FILE_NAME), "podcast-file-row")
        hold(CAMERA_HOLD_LONG_MS)

        click(titleOf(MUSIC_FILE_NAME), "music-file-row")
        awaitObject(By.desc(PLAYLIST_BUTTON_DESCRIPTION), "playlist-button")
        hold(CAMERA_HOLD_SHORT_MS)

        click(By.desc(PLAYLIST_BUTTON_DESCRIPTION), "playlist-button")
        awaitObject(queueItem(MUSIC_TITLE), "queue-music-item")
        hold(CAMERA_HOLD_LONG_MS)

        val hasPodcast = device.hasObject(queueItem(PODCAST_TITLE))
        if (!hasPodcast) dumpHierarchy("queue-podcast-item")
        assertWithMessage("$PODCAST_TITLE in the queue").that(hasPodcast).isTrue()
    }

    // region seeding helpers

    /**
     * Stores the offline rows that making the two audio files available offline produces: the
     * cloud folders above each file as ancestors, and each file as an offline root. The audio
     * data is written to the offline path of each file.
     */
    private fun seedOfflineAudioFiles() = runBlocking {
        nodeRepository.clearOffline()

        val musicRowId = saveOfflineRow(
            MUSIC_FOLDER_HANDLE, MUSIC_FOLDER_NAME, "/", parentRowId = null,
        )
        val albumRowId = saveOfflineRow(
            ALBUM_FOLDER_HANDLE, ALBUM_FOLDER_NAME, "/$MUSIC_FOLDER_NAME/", parentRowId = musicRowId,
        )
        saveOfflineAudioFile(
            MUSIC_FILE_HANDLE, MUSIC_FILE_NAME, "/$MUSIC_FOLDER_NAME/$ALBUM_FOLDER_NAME/",
            parentRowId = albumRowId,
        )

        val podcastsRowId = saveOfflineRow(
            PODCASTS_FOLDER_HANDLE, PODCASTS_FOLDER_NAME, "/", parentRowId = null,
        )
        saveOfflineAudioFile(
            PODCAST_FILE_HANDLE, PODCAST_FILE_NAME, "/$PODCASTS_FOLDER_NAME/",
            parentRowId = podcastsRowId,
        )
    }

    private suspend fun saveOfflineRow(
        handle: Long,
        name: String,
        path: String,
        parentRowId: Long?,
    ): Long = nodeRepository.saveOfflineNodeInformation(
        offlineNodeInformation = offlineInformation(handle, name, path, true, parentRowId),
        parentOfflineInformationId = parentRowId,
        isOfflineRoot = false,
    )

    private suspend fun saveOfflineAudioFile(
        handle: Long,
        name: String,
        path: String,
        parentRowId: Long,
    ) {
        val information = offlineInformation(handle, name, path, false, parentRowId)
        nodeRepository.saveOfflineNodeInformation(
            offlineNodeInformation = information,
            parentOfflineInformationId = parentRowId,
            isOfflineRoot = true,
        )
        val file = getOfflineFileUseCase(information)
        file.parentFile?.mkdirs()
        file.writeBytes(silentWav())
    }

    private fun offlineInformation(
        handle: Long,
        name: String,
        path: String,
        isFolder: Boolean,
        parentRowId: Long?,
    ) = OtherOfflineNodeInformation(
        id = -1,
        path = path,
        name = name,
        handle = handle.toString(),
        isFolder = isFolder,
        lastModifiedTime = null,
        parentId = parentRowId?.toInt() ?: -1,
    )

    /** 1 s of 16-bit mono PCM silence at 8 kHz, with a 44-byte WAV header. */
    private fun silentWav(): ByteArray {
        val dataSize = WAV_SAMPLE_RATE * WAV_BYTES_PER_SAMPLE
        return ByteBuffer.allocate(WAV_HEADER_SIZE + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray())
            .putInt(WAV_HEADER_SIZE - 8 + dataSize)
            .put("WAVE".toByteArray())
            .put("fmt ".toByteArray())
            .putInt(16)
            .putShort(1)
            .putShort(1)
            .putInt(WAV_SAMPLE_RATE)
            .putInt(WAV_SAMPLE_RATE * WAV_BYTES_PER_SAMPLE)
            .putShort(WAV_BYTES_PER_SAMPLE.toShort())
            .putShort((WAV_BYTES_PER_SAMPLE * 8).toShort())
            .put("data".toByteArray())
            .putInt(dataSize)
            .array()
    }

    // endregion

    // region navigation helpers

    /**
     * Launches the logged-in app and opens the root of the Offline section from the Menu.
     *
     * The app is launched as [MegaActivity] and not with an intent built by
     * `MegaActivity.getIntentWithExtraDestinations`, because that intent starts a different
     * activity class, and opening the audio player then starts [MegaActivity] on top of it. The
     * scenario cannot close the covered activity.
     */
    private fun launchOffline() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        click(By.res(MENU_NAV_ITEM_TAG), "menu-nav-item")
        awaitObject(By.res(MENU_TOOLBAR_TAG), "menu-toolbar")
        val offlineRow = By.res(MENU_ACCOUNT_ITEM_TAG).hasDescendant(By.text(offlineFilesLabel))
        scrollUntil(offlineRow, "offline-files-row")
        click(offlineRow, "offline-files-row")
        awaitObject(titleOf(MUSIC_FILE_NAME), "music-file-row")
    }

    // endregion

    // region UiAutomator helpers

    private fun titleOf(name: String): BySelector = By.res(NODE_TITLE_TAG).text(name)

    private fun queueItem(title: String): BySelector =
        By.res(QUEUE_CONTENT_TAG).hasDescendant(By.textContains(title))

    private fun hold(durationMs: Long) = Thread.sleep(durationMs)

    /**
     * Scrolls the first scrollable list until [selector] is present, because a LazyColumn does not
     * compose the rows below the visible area.
     */
    private fun scrollUntil(selector: BySelector, name: String) {
        repeat(MAX_SCROLL_ATTEMPTS) {
            if (device.hasObject(selector)) return
            val list = device.findObject(By.scrollable(true)) ?: return@repeat
            list.setGestureMargin(list.visibleBounds.height() / 4)
            list.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, name)
    }

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, name)
        device.findObject(selector).click()
    }

    private fun awaitObject(selector: BySelector, name: String) {
        if (device.wait(Until.hasObject(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
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
        const val LOAD_TIMEOUT = 30_000L
        const val MAX_SCROLL_ATTEMPTS = 10
        const val SCROLL_PERCENT = 0.8f
        const val CAMERA_HOLD_SHORT_MS = 1_000L
        const val CAMERA_HOLD_LONG_MS = 2_000L

        const val MUSIC_FOLDER_HANDLE = 300L
        const val ALBUM_FOLDER_HANDLE = 301L
        const val MUSIC_FILE_HANDLE = 302L
        const val PODCASTS_FOLDER_HANDLE = 303L
        const val PODCAST_FILE_HANDLE = 304L
        const val MUSIC_FOLDER_NAME = "Music"
        const val ALBUM_FOLDER_NAME = "Album A"
        const val PODCASTS_FOLDER_NAME = "Podcasts"
        const val MUSIC_TITLE = "Morning Walk"
        const val PODCAST_TITLE = "Episode 12"
        const val MUSIC_FILE_NAME = "$MUSIC_TITLE.wav"
        const val PODCAST_FILE_NAME = "$PODCAST_TITLE.wav"

        const val WAV_HEADER_SIZE = 44
        const val WAV_SAMPLE_RATE = 8_000
        const val WAV_BYTES_PER_SAMPLE = 2

        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"
        const val MENU_TOOLBAR_TAG = "menu_home_screen:toolbar"
        const val MENU_ACCOUNT_ITEM_TAG = "menu_home_screen:account_item"
        const val NODE_TITLE_TAG = "node_list_view_item:title"
        const val QUEUE_CONTENT_TAG = "audio_player:queue_content"

        /** Content description of the playlist button in `AudioPlayerPrimaryActions`. */
        const val PLAYLIST_BUTTON_DESCRIPTION = "Playlist"
    }
}
