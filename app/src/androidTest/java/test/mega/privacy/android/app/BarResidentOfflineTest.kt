package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Test
import javax.inject.Inject

/**
 * Full-app instrumented test for AND-24955 — the Offline section resident in the customised
 * bottom navigation bar is a bar root, but only at its section root:
 *
 * 1. The offline root's top bar shows no back affordance while Offline sits in the bar.
 * 2. A pushed offline sub-folder keeps its back affordance even though it shares the bar root's
 *    nav key class, and back returns to the root — which again shows none.
 * 3. The Menu shows no static "Offline files" row while Offline is in the bar.
 *
 * Offline content is seeded straight through [NodeRepository.saveOfflineNodeInformation] — the
 * production persistence path of the offline Room table — with a root folder and one child file.
 * The rows do not need backing files on disk: the offline list is built from the table, and file
 * sizes/thumbnails degrade gracefully for missing files. Built on the `:data-test` fake-SDK
 * framework via [BottomNavigationTestBase]; the back affordance is the top bar's
 * "Navigation Icon" content description, asserted absent only after the top bar itself is
 * confirmed on screen.
 */
@HiltAndroidTest
class BarResidentOfflineTest : BottomNavigationTestBase() {

    @Inject
    lateinit var nodeRepository: NodeRepository

    private val offlineFilesLabel
        get() = targetContext.getString(sharedR.string.general_section_offline_files)

    private val rubbishBinLabel
        get() = targetContext.getString(sharedR.string.general_section_rubbish_bin)

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking {
            setCustomiseNavigationTooltipShownUseCase()
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(
                    orderedVisibleItemIds = listOf(HOME_ID, DRIVE_ID, OFFLINE_ID),
                )
            )
        }
    }

    private fun seedOfflineFolderWithChildFile() = runBlocking {
        val folderRowId = nodeRepository.saveOfflineNodeInformation(
            offlineNodeInformation = OtherOfflineNodeInformation(
                id = -1,
                path = "/",
                name = FOLDER_NAME,
                handle = FOLDER_HANDLE,
                isFolder = true,
                lastModifiedTime = null,
                parentId = -1,
            ),
            parentOfflineInformationId = null,
        )
        nodeRepository.saveOfflineNodeInformation(
            offlineNodeInformation = OtherOfflineNodeInformation(
                id = -1,
                path = "/$FOLDER_NAME/",
                name = FILE_NAME,
                handle = FILE_HANDLE,
                isFolder = false,
                lastModifiedTime = null,
                parentId = folderRowId.toInt(),
            ),
            parentOfflineInformationId = folderRowId,
        )
    }

    @Test
    fun offlineRootShowsNoBackWhileAPushedSubFolderKeepsItAndTheMenuRowIsAbsent() {
        seedOfflineFolderWithChildFile()

        launchApp()

        // Offline sits in the customised bar; entering it lands on the seeded root folder list.
        awaitObject(By.res(OFFLINE_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "offline-nav-item")
        clickAndAwait(By.res(OFFLINE_NAV_ITEM_TAG), By.text(FOLDER_NAME), "offline-nav-item")

        // The bar root's top bar renders with no back affordance.
        awaitObject(By.res(OFFLINE_SEARCH_TOP_APP_BAR_TAG), LOAD_TIMEOUT, "offline-top-bar")
        assertNotPresent(backArrow, "offline-root-back-arrow")

        // A sub-folder is pushed on the section's own stack and keeps its back affordance even
        // though it shares the bar root's nav key class.
        clickAndAwait(By.text(FOLDER_NAME), backArrow, "offline-folder-row")
        awaitObject(By.text(FILE_NAME), LOAD_TIMEOUT, "offline-child-file")

        // Back returns to the section root, which again shows no back affordance. The child
        // file name is only listed in the sub-folder, so its disappearance confirms the pop —
        // the folder name is also the sub-folder's title and cannot.
        awaitObject(backArrow, LOAD_TIMEOUT, "offline-back-arrow").click()
        awaitGone(By.text(FILE_NAME), "offline-child-file")
        awaitObject(By.res(OFFLINE_SEARCH_TOP_APP_BAR_TAG), LOAD_TIMEOUT, "offline-top-bar")
        assertNotPresent(backArrow, "offline-root-back-arrow-after-return")

        // The Menu shows no static "Offline files" row while Offline is in the bar; the anchor
        // row below its slot is scrolled into view first so the absence is asserted against
        // composed rows.
        openMenu()
        scrollMenuUntil(By.text(rubbishBinLabel), "rubbish-bin-row")
        assertNotPresent(menuAccountRow(offlineFilesLabel), "offline-files-menu-row")
    }

    private companion object {
        const val FOLDER_NAME = "Offline test folder"
        const val FILE_NAME = "offline-test-file.txt"
        const val FOLDER_HANDLE = "1000"
        const val FILE_HANDLE = "1001"
    }
}
