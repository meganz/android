package mega.privacy.android.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatMessage
import mega.privacy.android.data.test.stub.StubMegaChatRequest
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaFolderInfo
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaNodeList
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.nodes.R as nodesR
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaSearchFilter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm05 ManageFileFolderMenu` — managing a file /
 * folder through its Cloud Drive context menu (the node options bottom sheet). Built on the
 * `:data-test` fake-SDK framework and following [RenameNodeTest] / [HomeScreenTest] /
 * [ChatRoomTest]: the whole app runs as in production with only the SDK gateways faked, the UI is
 * driven with UiAutomator (no Compose rule), and Compose test tags are matched as resource ids
 * because the activity sets `testTagsAsResourceId = true`. Surfaces rendered in their own window
 * (the rename dialog, the move-to-rubbish confirmation, the change-label sheet) do not carry
 * resource ids there, so they are driven by [By.text] with strings resolved from resources.
 *
 * The SDK side-effect of each mutating command is applied with the `:data-test`
 * [mega.privacy.android.data.test.state.FakeNodeTree] helpers (`rename` / `copy` / `move` /
 * `moveToRubbish` / `setFavourite` / `setLabel`), which change the fake tree and broadcast the
 * matching `OnNodesUpdate` in one call, after polling for the gateway invocation the UI action
 * enqueues from a coroutine.
 *
 * Ported: tc01/tc02 (file/folder info), tc03/tc04 (favourite toggling), tc05/tc06 (labels),
 * tc11/tc12 (get link, against the revamped Share link screen behind `ShareLinkRevamp`),
 * tc18 (send file to chat), tc20 (rename folder — tc19's file rename is already covered by
 * [RenameNodeTest]), tc21/tc22 (copy), tc23/tc24 (move), tc25/tc26 (move to Rubbish bin).
 *
 * Adaptations and skips:
 * - tc07–tc10 (save to device / available offline) are NOT ported: they assert real transferred
 *   bytes and offline artifacts on the device, out of scope for the fake-SDK framework batch.
 * - tc13–tc17 (share via the system share sheet / share folder to a contact) are NOT ported: they
 *   run through cross-app share sheets or assert second-account outcomes.
 * - The copy/move destination picker ([FileExplorerActivity]) and the send-to-chat picker
 *   ([ChatExplorerActivity]) are legacy activities that talk to the SDK directly, so — like the
 *   pickers in [CloudDriveUploadTest] and [ChatRoomTest] — they are stubbed with Espresso-Intents
 *   to immediately return the selection; everything around the picker runs the production flow.
 * - tc01/tc02 open the revamped File info screen (`FileInfoRevamp` forced on); the get-link /
 *   remove-link round trip inside the info screen is covered by tc11/tc12 on the Share link
 *   screen instead.
 */
@HiltAndroidTest
class FileFolderMenuTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeMegaChat: FakeMegaChatApiGateway

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private var scenario: ActivityScenario<*>? = null

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

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Node lists resolve through getChildren/searchWithFilter with an SDK filter object;
        // answer from the fake tree using the filter's location handle so navigating into a
        // folder (copy/move targets, the Rubbish bin) lists that folder's children.
        fakeMegaApi.stub(MegaApiGateway::getChildren) { arguments -> childrenFor(arguments) }
        fakeMegaApi.stub(MegaApiGateway::searchWithFilter) { arguments -> childrenFor(arguments) }

        // Persist a logged-in session through the app's real credentials path; account details are
        // fetched eagerly because account-detail dependent features wait for a non-empty value.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    // region tests

    /** tc03/tc04 — favouriting a file and a folder shows the heart on their rows; unfavouriting removes it. */
    @Test
    fun favouriteToggleShowsHeartOnRows() {
        seedFile()
        seedFolder()

        launchCloudDrive()

        // Favourite the file: heart shows on its row.
        clickNodeOptionsAction(FILE_NAME, FAVOURITE_ACTION_TAG)
        awaitFavouriteInvocation(count = 1, favourite = true)
        runBlocking { fakeMegaApi.nodeTree.setFavourite(FILE_HANDLE, true) }
        awaitObjectCount(By.res(FAVOURITE_ICON_TAG), 1, "file-favourite-heart")

        // Favourite the folder: a second heart shows.
        clickNodeOptionsAction(FOLDER_NAME, FAVOURITE_ACTION_TAG)
        awaitFavouriteInvocation(count = 2, favourite = true)
        runBlocking { fakeMegaApi.nodeTree.setFavourite(FOLDER_HANDLE, true) }
        awaitObjectCount(By.res(FAVOURITE_ICON_TAG), 2, "folder-favourite-heart")

        // Unfavourite the file through its now-shown "Remove favourite" option: back to one heart.
        clickNodeOptionsAction(FILE_NAME, REMOVE_FAVOURITE_ACTION_TAG)
        awaitFavouriteInvocation(count = 1, favourite = false)
        runBlocking { fakeMegaApi.nodeTree.setFavourite(FILE_HANDLE, false) }
        awaitObjectCount(By.res(FAVOURITE_ICON_TAG), 1, "file-heart-removed")
    }

    /** tc05/tc06 — applying a Red label to a file and a folder shows the label dot; removing clears it. */
    @Test
    fun labelApplyAndRemoveShowsDotOnRows() {
        seedFile()
        seedFolder()

        launchCloudDrive()

        // Label the file Red; the change-label sheet renders in its own window, so it is driven
        // by text.
        clickNodeOptionsAction(FILE_NAME, LABEL_ACTION_TAG)
        clickText(str(sharedR.string.label_red), "red-label-option")
        awaitInvocations("setNodeLabel") {
            fakeMegaApi.invocations.filter { it.methodName == "setNodeLabel" }
        }
        runBlocking { fakeMegaApi.nodeTree.setLabel(FILE_HANDLE, MegaNode.NODE_LBL_RED) }
        awaitObjectCount(By.res(LABEL_TAG), 1, "file-label-dot")

        // Label the folder Red: a second dot shows.
        clickNodeOptionsAction(FOLDER_NAME, LABEL_ACTION_TAG)
        clickText(str(sharedR.string.label_red), "red-label-option-folder")
        awaitInvocations("setNodeLabel x2") {
            fakeMegaApi.invocations.filter { it.methodName == "setNodeLabel" }
                .takeIf { it.size >= 2 }.orEmpty()
        }
        runBlocking { fakeMegaApi.nodeTree.setLabel(FOLDER_HANDLE, MegaNode.NODE_LBL_RED) }
        awaitObjectCount(By.res(LABEL_TAG), 2, "folder-label-dot")

        // Remove the file's label: back to one dot.
        clickNodeOptionsAction(FILE_NAME, LABEL_ACTION_TAG)
        clickText(str(nodesR.string.action_remove_label), "remove-label-option")
        awaitInvocations("resetNodeLabel") {
            fakeMegaApi.invocations.filter { it.methodName == "resetNodeLabel" }
        }
        runBlocking { fakeMegaApi.nodeTree.setLabel(FILE_HANDLE, 0) }
        awaitObjectCount(By.res(LABEL_TAG), 1, "file-label-removed")
    }

    /**
     * tc11/tc12 — Get link for a file and a folder exports the node and shows the generated link
     * on the revamped Share link screen (agreeing to the copyright notice on first use).
     */
    @Test
    fun getLinkShowsGeneratedLinkForFileAndFolder() {
        fakeFlags.set(ApiFeatures.ShareLinkRevamp, true)
        seedFile()
        seedFolder()
        stubExportedLink(FILE_HANDLE, FILE_LINK)
        stubExportedLink(FOLDER_HANDLE, FOLDER_LINK)

        launchCloudDrive()

        clickNodeOptionsAction(FILE_NAME, GET_LINK_ACTION_TAG)
        agreeToCopyrightIfShown()
        awaitObject(By.text(FILE_LINK), LOAD_TIMEOUT, "file-share-link")

        device.pressBack()
        clickNodeOptionsAction(FOLDER_NAME, GET_LINK_ACTION_TAG)
        agreeToCopyrightIfShown()
        awaitObject(By.text(FOLDER_LINK), LOAD_TIMEOUT, "folder-share-link")
    }

    /**
     * tc20 — renaming a folder through the node options menu shows the new name on the row.
     * tc19's file variant of the same journey is already covered by [RenameNodeTest].
     */
    @Test
    fun renamedFolderRowShowsTheNewName() {
        seedFolder()
        fakeMegaApi.stubRequest(MegaApiGateway::renameNode)

        launchCloudDrive()

        clickNodeOptionsAction(FOLDER_NAME, RENAME_ACTION_TAG)

        // The rename dialog renders in its own window without resource ids: match the input by
        // class + current text and the confirm button by its localized label.
        val renameText = str(sharedR.string.context_rename)
        awaitObject(By.clazz("android.widget.EditText").text(FOLDER_NAME), LOAD_TIMEOUT, "rename-input")
        device.findObject(By.clazz("android.widget.EditText").text(FOLDER_NAME)).text = RENAMED_FOLDER_NAME
        click(By.clickable(true).hasDescendant(By.text(renameText)), "rename-confirm")

        awaitInvocations("renameNode") {
            fakeMegaApi.invocations.filter { it.methodName == "renameNode" }
        }
        runBlocking { fakeMegaApi.nodeTree.rename(FOLDER_HANDLE, RENAMED_FOLDER_NAME) }

        awaitObject(By.res(NODE_TITLE_TAG).text(RENAMED_FOLDER_NAME), LOAD_TIMEOUT, "renamed-folder-row")
    }

    /** tc21/tc22 — copying a file and a folder into a target folder shows them under the target. */
    @Test
    fun copiedFileAndFolderShowUnderTargetFolder() {
        seedFile()
        seedFolder()
        seedTargetFolder()

        withStubbedFolderPicker(
            action = FileExplorerActivity.ACTION_PICK_COPY_FOLDER,
            sourceExtra = Constants.INTENT_EXTRA_KEY_COPY_FROM,
            handlesExtra = Constants.INTENT_EXTRA_KEY_COPY_HANDLES,
            targetExtra = Constants.INTENT_EXTRA_KEY_COPY_TO,
        ) {
            launchCloudDrive()

            clickNodeOptionsAction(FILE_NAME, COPY_ACTION_TAG)
            awaitInvocations("copyNode") {
                fakeMegaApi.invocations.filter { it.methodName == "copyNode" }
            }
            runBlocking { fakeMegaApi.nodeTree.copy(FILE_HANDLE, TARGET_FOLDER_HANDLE) }

            clickNodeOptionsAction(FOLDER_NAME, COPY_ACTION_TAG)
            awaitInvocations("copyNode x2") {
                fakeMegaApi.invocations.filter { it.methodName == "copyNode" }
                    .takeIf { it.size >= 2 }.orEmpty()
            }
            runBlocking { fakeMegaApi.nodeTree.copy(FOLDER_HANDLE, TARGET_FOLDER_HANDLE) }

            // The originals stay in place; the copies are listed when opening the target folder.
            awaitObject(By.res(NODE_TITLE_TAG).text(FILE_NAME), LOAD_TIMEOUT, "copied-source-file-row")
            openFolderRow(TARGET_FOLDER_NAME)
            awaitObject(By.res(NODE_TITLE_TAG).text(FILE_NAME), LOAD_TIMEOUT, "copied-file-in-target")
            awaitObject(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), LOAD_TIMEOUT, "copied-folder-in-target")
        }
    }

    /** tc23/tc24 — moving a file and a folder removes them from the source and shows them in the target. */
    @Test
    fun movedFileAndFolderShowUnderTargetFolderOnly() {
        seedFile()
        seedFolder()
        seedTargetFolder()

        withStubbedFolderPicker(
            action = FileExplorerActivity.ACTION_PICK_MOVE_FOLDER,
            sourceExtra = Constants.INTENT_EXTRA_KEY_MOVE_FROM,
            handlesExtra = Constants.INTENT_EXTRA_KEY_MOVE_HANDLES,
            targetExtra = Constants.INTENT_EXTRA_KEY_MOVE_TO,
        ) {
            launchCloudDrive()

            clickNodeOptionsAction(FILE_NAME, MOVE_ACTION_TAG)
            awaitInvocations("moveNode") {
                fakeMegaApi.invocations.filter { it.methodName == "moveNode" }
            }
            runBlocking { fakeMegaApi.nodeTree.move(FILE_HANDLE, TARGET_FOLDER_HANDLE) }
            awaitGone(By.res(NODE_TITLE_TAG).text(FILE_NAME), "moved-file-gone-from-source")

            clickNodeOptionsAction(FOLDER_NAME, MOVE_ACTION_TAG)
            awaitInvocations("moveNode x2") {
                fakeMegaApi.invocations.filter { it.methodName == "moveNode" }
                    .takeIf { it.size >= 2 }.orEmpty()
            }
            runBlocking { fakeMegaApi.nodeTree.move(FOLDER_HANDLE, TARGET_FOLDER_HANDLE) }
            awaitGone(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), "moved-folder-gone-from-source")

            openFolderRow(TARGET_FOLDER_NAME)
            awaitObject(By.res(NODE_TITLE_TAG).text(FILE_NAME), LOAD_TIMEOUT, "moved-file-in-target")
            awaitObject(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), LOAD_TIMEOUT, "moved-folder-in-target")
        }
    }

    /**
     * tc25/tc26 — moving a file and a folder to the Rubbish bin (with the confirmation dialog)
     * removes them from the Cloud Drive list and shows them under the Rubbish bin section.
     *
     * This test is the demo-recording candidate: it briefly holds each state a viewer should
     * register (list with both rows, the confirmation dialog, the emptied list, the Rubbish bin).
     */
    @Test
    fun movedToRubbishRowsDisappearFromDriveAndShowInRubbishBin() {
        seedFile()
        seedFolder()

        launchCloudDrive()
        awaitObject(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), LOAD_TIMEOUT, "seeded-rows")
        Thread.sleep(CAMERA_HOLD_SHORT_MS)

        // File → Move to Rubbish bin → confirm; the dialog renders in its own window (By.text).
        clickNodeOptionsAction(FILE_NAME, TRASH_ACTION_TAG)
        awaitObject(By.text(str(nodesR.string.confirmation_move_to_rubbish)), LOAD_TIMEOUT, "rubbish-dialog")
        Thread.sleep(CAMERA_HOLD_SHORT_MS)
        clickText(str(sharedR.string.general_move), "rubbish-confirm")
        awaitInvocations("moveNode") {
            fakeMegaApi.invocations.filter { it.methodName == "moveNode" }
        }
        runBlocking { fakeMegaApi.nodeTree.moveToRubbish(FILE_HANDLE) }
        awaitGone(By.res(NODE_TITLE_TAG).text(FILE_NAME), "file-gone-from-drive")

        // Folder → Move to Rubbish bin → confirm.
        clickNodeOptionsAction(FOLDER_NAME, TRASH_ACTION_TAG)
        clickText(str(sharedR.string.general_move), "rubbish-confirm-folder")
        awaitInvocations("moveNode x2") {
            fakeMegaApi.invocations.filter { it.methodName == "moveNode" }
                .takeIf { it.size >= 2 }.orEmpty()
        }
        runBlocking { fakeMegaApi.nodeTree.moveToRubbish(FOLDER_HANDLE) }
        awaitGone(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), "folder-gone-from-drive")
        Thread.sleep(CAMERA_HOLD_SHORT_MS)

        // Both nodes are listed under the Rubbish bin (Menu tab → Rubbish bin).
        click(By.res(MENU_NAV_ITEM_TAG), "menu-nav-item")
        val rubbishBinLabel = str(sharedR.string.general_section_rubbish_bin)
        scrollUntilText(rubbishBinLabel)
        clickText(rubbishBinLabel, "rubbish-bin-menu-row")
        awaitObject(By.res(NODE_TITLE_TAG).text(FILE_NAME), LOAD_TIMEOUT, "file-in-rubbish")
        awaitObject(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), LOAD_TIMEOUT, "folder-in-rubbish")
        Thread.sleep(CAMERA_HOLD_LONG_MS)
    }

    /** tc01 — the file info screen shows the file's name and its type/size subtitle. */
    @Test
    fun fileInfoShowsNameAndSize() {
        fakeFlags.set(ApiFeatures.FileInfoRevamp, true)
        seedFile()

        launchCloudDrive()
        clickNodeOptionsAction(FILE_NAME, INFO_ACTION_TAG)

        awaitObject(By.res(FILE_INFO_NAME_TAG).text(FILE_NAME), LOAD_TIMEOUT, "file-info-name")
        awaitObject(By.res(FILE_INFO_SUBTITLE_TAG), LOAD_TIMEOUT, "file-info-subtitle")
        awaitObject(By.res(FILE_INFO_ADDED_TAG), LOAD_TIMEOUT, "file-info-added-row")
    }

    /** tc02 — the folder info screen shows the folder's name and its contents ("1 file"). */
    @Test
    fun folderInfoShowsNameAndContents() {
        fakeFlags.set(ApiFeatures.FileInfoRevamp, true)
        seedFolder()
        seedNode(CHILD_FILE_HANDLE, CHILD_FILE_NAME, isFolder = false, parentHandle = FOLDER_HANDLE)
        fakeMegaApi.stubRequest(
            MegaApiGateway::getFolderInfo,
            request = StubMegaRequest(
                type = MegaRequest.TYPE_FOLDER_INFO,
                megaFolderInfo = StubMegaFolderInfo(numFiles = 1, currentSize = FILE_SIZE),
            ),
        )

        launchCloudDrive()
        clickNodeOptionsAction(FOLDER_NAME, INFO_ACTION_TAG)

        awaitObject(By.res(FILE_INFO_NAME_TAG).text(FOLDER_NAME), LOAD_TIMEOUT, "folder-info-name")
        val oneFile = targetContext.resources.getQuantityString(
            sharedR.plurals.num_of_files_with_parameter, 1, 1,
        )
        awaitObject(
            By.res(FILE_INFO_SUBTITLE_TAG).textContains(oneFile),
            LOAD_TIMEOUT,
            "folder-info-contents",
        )
    }

    /**
     * tc18 — sending a file to a chat attaches the node through the chat gateway and renders the
     * attachment message in the chat room. The chat picker is stubbed with Espresso-Intents to
     * return the seeded chat; the attachment message is then delivered through the fake chat
     * history and asserted as a rendered row in the chat.
     */
    @Test
    fun sentFileRendersAsChatAttachment() {
        val node = seedFile()
        fakeMegaChat.chatState.chatRooms[CHAT_ID] = StubMegaChatRoom(
            chatId = CHAT_ID,
            title = CONTACT_NAME,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = false,
            isActive = true,
        )
        // The production attach path reads the echoed message's tempId from the request.
        fakeMegaChat.stubChatRequest(
            MegaChatApiGateway::attachNode,
            request = StubMegaChatRequest(
                type = MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE,
                megaChatMessage = StubMegaChatMessage(
                    msgId = ATTACHMENT_MSG_ID,
                    tempId = ATTACHMENT_MSG_ID,
                    userHandle = fakeMegaChat.chatState.myUserHandle,
                    type = MegaChatMessage.TYPE_NODE_ATTACHMENT,
                    status = MegaChatMessage.STATUS_SENDING,
                    megaNodeList = StubMegaNodeList(listOf(node)),
                ),
            ),
        )

        withStubbedChatPicker {
            launchCloudDrive()
            clickNodeOptionsAction(FILE_NAME, SEND_TO_CHAT_ACTION_TAG)

            awaitInvocations("attachNode($CHAT_ID, $FILE_HANDLE)") {
                fakeMegaChat.invocations.filter {
                    it.methodName == "attachNode" &&
                            it.arguments.getOrNull(0) == CHAT_ID &&
                            it.arguments.getOrNull(1) == FILE_HANDLE
                }
            }
            // The success snackbar is the user-visible confirmation of the send.
            val sentMessage = targetContext.resources.getQuantityString(
                nodesR.plurals.files_send_to_chat_success, 1,
            )
            awaitObject(By.text(sentMessage), LOAD_TIMEOUT, "sent-to-chat-snackbar")
        }

        // Open the chat room with the attachment in its history: the attachment row renders the
        // attached file's name.
        fakeMegaChat.chatState.addChatMessage(
            CHAT_ID,
            StubMegaChatMessage(
                msgId = ATTACHMENT_MSG_ID,
                userHandle = fakeMegaChat.chatState.myUserHandle,
                type = MegaChatMessage.TYPE_NODE_ATTACHMENT,
                status = MegaChatMessage.STATUS_DELIVERED,
                timestamp = System.currentTimeMillis() / 1000,
                megaNodeList = StubMegaNodeList(listOf(node)),
            ),
        )
        scenario?.close()
        scenario = ActivityScenario.launch<ChatActivity>(
            Intent(targetContext, ChatActivity::class.java)
                .putExtra(ChatNavKey.LEGACY_CHAT_ID, CHAT_ID),
        )
        awaitObject(By.text(CONTACT_NAME), LAUNCH_TIMEOUT, "chat-room-loaded")
        awaitObject(By.text(FILE_NAME), LOAD_TIMEOUT, "attachment-message-row")
    }

    // endregion

    // region seeding & stubbing helpers

    private fun seedFile(): StubMegaNode =
        seedNode(FILE_HANDLE, FILE_NAME, isFolder = false, parentHandle = rootHandle)

    private fun seedFolder(): StubMegaNode =
        seedNode(FOLDER_HANDLE, FOLDER_NAME, isFolder = true, parentHandle = rootHandle)

    private fun seedTargetFolder(): StubMegaNode =
        seedNode(TARGET_FOLDER_HANDLE, TARGET_FOLDER_NAME, isFolder = true, parentHandle = rootHandle)

    /**
     * Seeds a node owned by the logged-in account (send-to-chat attaches a node directly only
     * when it is owned; a foreign owner would trigger a copy into the chat-files folder).
     */
    private fun seedNode(
        handle: Long,
        name: String,
        isFolder: Boolean,
        parentHandle: Long,
    ): StubMegaNode {
        val node = StubMegaNode(
            handle = handle,
            name = name,
            parentHandle = parentHandle,
            isFolder = isFolder,
            size = if (isFolder) 0L else FILE_SIZE,
            modificationTime = System.currentTimeMillis() / 1000,
            owner = fakeMegaApi.account.myUserHandle,
        )
        fakeMegaApi.nodeTree.addNode(node, parentHandle = parentHandle)
        return node
    }

    /** Stubs the export request for the node with [handle] to report [link] as the public link. */
    private fun stubExportedLink(handle: Long, link: String) {
        fakeMegaApi.stubRequest(
            MegaApiGateway::exportNode,
            request = StubMegaRequest(type = MegaRequest.TYPE_EXPORT, link = link),
            matcher = { arguments -> (arguments.getOrNull(0) as? MegaNode)?.handle == handle },
        )
    }

    /** Resolves the fake tree children for a getChildren/searchWithFilter gateway call. */
    private fun childrenFor(arguments: List<Any?>): List<MegaNode> {
        val locationHandle = (arguments.firstOrNull() as? MegaSearchFilter)
            ?.byLocationHandle()
            ?.takeIf { it != -1L }
            ?: rootHandle
        return fakeMegaApi.nodeTree.childrenOf(locationHandle)
    }

    /**
     * The copy/move destination picker ([FileExplorerActivity]) is a legacy activity that talks to
     * the SDK directly, so it cannot run against the fakes; stub its result with Espresso-Intents
     * as if the user had picked the seeded target folder for the launched node handles.
     */
    private fun withStubbedFolderPicker(
        action: String,
        sourceExtra: String,
        handlesExtra: String,
        targetExtra: String,
        block: () -> Unit,
    ) {
        Intents.init()
        try {
            intending(hasAction(action)).respondWithFunction { intent ->
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent()
                        .putExtra(handlesExtra, intent.getLongArrayExtra(sourceExtra))
                        .putExtra(targetExtra, TARGET_FOLDER_HANDLE),
                )
            }
            block()
        } finally {
            Intents.release()
        }
    }

    /**
     * The send-to-chat picker ([ChatExplorerActivity]) is a legacy activity that talks to the SDK
     * directly; stub its result as if the user had picked the seeded chat.
     */
    private fun withStubbedChatPicker(block: () -> Unit) {
        Intents.init()
        try {
            intending(hasComponent(ChatExplorerActivity::class.java.name)).respondWithFunction { intent ->
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent()
                        .putExtra(
                            Constants.NODE_HANDLES,
                            intent.getLongArrayExtra(Constants.NODE_HANDLES),
                        )
                        .putExtra(Constants.SELECTED_CHATS, longArrayOf(CHAT_ID)),
                )
            }
            block()
        } finally {
            Intents.release()
        }
    }

    // endregion

    // region navigation helpers

    /** Launches the logged-in app and opens the Cloud Drive section. */
    private fun launchCloudDrive() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        click(By.res(DRIVE_NAV_ITEM_TAG), "drive-nav-item")
        awaitObject(By.res(NODE_TITLE_TAG), LOAD_TIMEOUT, "cloud-drive-rows")
    }

    /**
     * Opens the node options bottom sheet for the row titled [nodeName] and clicks the action
     * tagged [actionTag], scrolling the sheet's lazy list when the action sits below the visible
     * area.
     */
    private fun clickNodeOptionsAction(nodeName: String, actionTag: String) {
        clickMoreIconForRow(nodeName)
        awaitObject(By.res(SHEET_LIST_TAG), LOAD_TIMEOUT, "node-options-sheet")
        scrollSheetUntil(By.res(actionTag), actionTag)
        device.findObject(By.res(actionTag)).click()
    }

    /**
     * Clicks the overflow (more) icon of the row titled [rowTitle]. Rows carry no per-row tag, so
     * the icon is resolved by vertical alignment with the row's title node.
     */
    private fun clickMoreIconForRow(rowTitle: String) {
        awaitObject(By.res(NODE_TITLE_TAG).text(rowTitle), LOAD_TIMEOUT, "row-$rowTitle")
        val titleCenterY = device.findObject(By.res(NODE_TITLE_TAG).text(rowTitle))
            .visibleBounds.centerY()
        val moreIcon = device.findObjects(By.res(MORE_ICON_TAG))
            .minByOrNull { abs(it.visibleBounds.centerY() - titleCenterY) }
            ?: run {
                dumpHierarchy("more-icon-$rowTitle")
                throw AssertionError("No more icon found for row $rowTitle")
            }
        moreIcon.click()
    }

    /** Opens the folder row titled [folderName] by clicking its title. */
    private fun openFolderRow(folderName: String) {
        click(By.res(NODE_TITLE_TAG).text(folderName), "open-folder-$folderName")
    }

    /** Agrees to the first-use copyright notice when the Share link screen shows it. */
    private fun agreeToCopyrightIfShown() {
        if (device.wait(Until.hasObject(By.res(SHARE_LINK_COPYRIGHT_AGREE_TAG)), COPYRIGHT_TIMEOUT)) {
            device.findObject(By.res(SHARE_LINK_COPYRIGHT_AGREE_TAG)).click()
        }
    }

    // endregion

    // region UiAutomator helpers

    private fun str(resId: Int): String = targetContext.getString(resId)

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
    }

    private fun clickText(text: String, name: String) = click(By.text(text), name)

    /**
     * Scrolls the node options sheet's lazy list until [selector] is present — entries below the
     * visible area are not composed and cannot be matched until scrolled into view.
     */
    private fun scrollSheetUntil(selector: BySelector, name: String) {
        repeat(MAX_SCROLL_ATTEMPTS) {
            if (device.hasObject(selector)) return
            val container = device.findObject(By.res(SHEET_LIST_TAG)) ?: return@repeat
            container.setGestureMargin(container.visibleBounds.height() / 4)
            container.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
    }

    /** Scrolls the visible scrollable container until a node with [text] is on screen. */
    private fun scrollUntilText(text: String) {
        if (device.hasObject(By.text(text))) return
        runCatching {
            UiScrollable(UiSelector().scrollable(true))
                .apply { setMaxSearchSwipes(MAX_SCROLL_ATTEMPTS) }
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
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to be gone; " +
                    "hierarchy in logcat tag UiDump",
        )
    }

    private fun awaitObjectCount(selector: BySelector, count: Int, name: String) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.findObjects(selector).size >= count) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $count objects matching $name " +
                    "($selector), found ${device.findObjects(selector).size}; " +
                    "hierarchy in logcat tag UiDump",
        )
    }

    private fun awaitFavouriteInvocation(count: Int, favourite: Boolean) {
        awaitInvocations("setNodeFavourite(favourite=$favourite) x$count") {
            fakeMegaApi.invocations.filter {
                it.methodName == "setNodeFavourite" && it.arguments.getOrNull(1) == favourite
            }.takeIf { it.size >= count }.orEmpty()
        }
    }

    /**
     * Gateway commands are dispatched from coroutine launches, so their invocations are not
     * recorded synchronously after the UI action that triggers them. Poll [supplier] until at
     * least one invocation matches, or fail with what was actually recorded.
     */
    private fun <T> awaitInvocations(name: String, supplier: () -> List<T>): List<T> {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for an invocation of $name; " +
                    "recorded api=${fakeMegaApi.invocations.map { it.methodName }} " +
                    "chat=${fakeMegaChat.invocations.map { it.methodName }}",
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
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val COPYRIGHT_TIMEOUT = 5_000L
        const val POLL_INTERVAL_MS = 100L
        const val MAX_SCROLL_ATTEMPTS = 10
        const val SCROLL_PERCENT = 0.8f
        const val CAMERA_HOLD_SHORT_MS = 1_000L
        const val CAMERA_HOLD_LONG_MS = 2_000L

        const val FILE_HANDLE = 100L
        const val FOLDER_HANDLE = 101L
        const val TARGET_FOLDER_HANDLE = 200L
        const val CHILD_FILE_HANDLE = 150L
        const val FILE_SIZE = 1_024L
        const val FILE_NAME = "sm5file.txt"
        const val FOLDER_NAME = "f-sm5folder"
        const val RENAMED_FOLDER_NAME = "renamed-sm5folder"
        const val TARGET_FOLDER_NAME = "folderTarget-sm5"
        const val CHILD_FILE_NAME = "sm5child.txt"
        const val FILE_LINK = "https://mega.nz/file/sm5file#filekey"
        const val FOLDER_LINK = "https://mega.nz/folder/sm5folder#folderkey"

        const val CHAT_ID = 1_001L
        const val PEER_HANDLE = 2_002L
        const val CONTACT_NAME = "sm5 contact"
        const val ATTACHMENT_MSG_ID = 10L

        /** Main navigation item test tags (Drive/Sync section and Menu tab). */
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Mirrors the internal tags of NodeListViewItem rows. */
        const val NODE_TITLE_TAG = "node_list_view_item:title"
        const val MORE_ICON_TAG = "node_list_view_item:more_icon"
        const val FAVOURITE_ICON_TAG = "node_list_view_item:favourite_icon"
        const val LABEL_TAG = "node_list_view_item:label"

        /** Mirrors NODE_OPTIONS_LAZY_COLUMN_TEST_TAG of the node options bottom sheet. */
        const val SHEET_LIST_TAG = "node_options_bottom_sheet:lazy_column"

        /** Mirror the testTags of the node options menu actions. */
        const val FAVOURITE_ACTION_TAG = "menu_action:favourite"
        const val REMOVE_FAVOURITE_ACTION_TAG = "menu_action:remove_favourite"
        const val LABEL_ACTION_TAG = "menu_action:label"
        const val GET_LINK_ACTION_TAG = "menu_action:get_link"
        const val INFO_ACTION_TAG = "menu_action:info"
        const val RENAME_ACTION_TAG = "menu_action:rename"
        const val COPY_ACTION_TAG = "menu_action:copy"
        const val MOVE_ACTION_TAG = "menu_action:move"
        const val TRASH_ACTION_TAG = "menu_action:rubbish_bin"
        const val SEND_TO_CHAT_ACTION_TAG = "menu_action:send_to_chat"

        /** Mirrors SHARE_LINK_COPYRIGHT_AGREE_TAG of the revamped Share link screen. */
        const val SHARE_LINK_COPYRIGHT_AGREE_TAG = "share_link_screen:copyright_agree"

        /** Mirror the tags of the revamped File info screen. */
        const val FILE_INFO_NAME_TAG = "file_info_screen:name"
        const val FILE_INFO_SUBTITLE_TAG = "file_info_screen:subtitle"
        const val FILE_INFO_ADDED_TAG = "file_info_screen:added"
    }
}
