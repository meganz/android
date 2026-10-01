package mega.privacy.android.app

import android.os.SystemClock
import android.util.Log
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.navigation.destination.NameCollisionNavKey
import mega.privacy.android.shared.nodes.namecollision.NameCollisionArgs
import mega.privacy.android.shared.nodes.namecollision.NameCollisionOutcome
import nz.mega.sdk.MegaNode
import java.io.ByteArrayOutputStream

/**
 * The fake Cloud Drive the name-collision screen tests run against: a source folder and a target
 * folder where every item of the source already exists by name in the target, plus the collision
 * batches built from it.
 */
internal object NameCollisionFixtures {
    const val LAUNCH_TIMEOUT = 60_000L
    const val LOAD_TIMEOUT = 30_000L

    const val LAUNCH_ID = "name-collision-test"
    const val SOURCE_FOLDER_HANDLE = 10L
    const val TARGET_FOLDER_HANDLE = 20L
    const val FILE_SIZE = 2_048L
    const val TIMESTAMP = 1_695_600_000L

    const val HOME_NAV_ITEM_TAG = "main_navigation:navigation_item_Home"

    val REPORT = CollidingItem(
        sourceHandle = 101L,
        collisionHandle = 201L,
        name = "report.txt",
        isFile = true,
        renameName = "report (1).txt",
    )
    val NOTES = CollidingItem(
        sourceHandle = 102L,
        collisionHandle = 202L,
        name = "notes.txt",
        isFile = true,
        renameName = "notes (1).txt",
    )
    val PHOTOS = CollidingItem(
        sourceHandle = 111L,
        collisionHandle = 211L,
        name = "Photos",
        isFile = false,
        renameName = "Photos (1)",
    )
    val VIDEOS = CollidingItem(
        sourceHandle = 112L,
        collisionHandle = 212L,
        name = "Videos",
        isFile = false,
        renameName = "Videos (1)",
    )
    val ITEMS = listOf(REPORT, NOTES, PHOTOS, VIDEOS)

    fun mixedBatch() = NameCollisionArgs.Multiple(
        collisions = listOf(
            REPORT.collision(),
            PHOTOS.collision(),
            NOTES.collision(),
            VIDEOS.collision(),
        ),
        isFolderUploadContext = false,
    )

    fun seedNodeTree(fakeMegaApi: FakeMegaApiGateway) {
        val rootHandle = fakeMegaApi.nodeTree.rootNode.handle
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = SOURCE_FOLDER_HANDLE,
                name = "Source",
                parentHandle = rootHandle,
                isFolder = true,
            ),
            parentHandle = rootHandle,
        )
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = TARGET_FOLDER_HANDLE,
                name = "Target",
                parentHandle = rootHandle,
                isFolder = true,
            ),
            parentHandle = rootHandle,
        )
        ITEMS.forEach { item ->
            fakeMegaApi.nodeTree.addNode(
                item.node(handle = item.sourceHandle, parentHandle = SOURCE_FOLDER_HANDLE),
                parentHandle = SOURCE_FOLDER_HANDLE,
            )
            fakeMegaApi.nodeTree.addNode(
                item.node(handle = item.collisionHandle, parentHandle = TARGET_FOLDER_HANDLE),
                parentHandle = TARGET_FOLDER_HANDLE,
            )
        }
    }
}

/**
 * An item of the source folder that already exists by name in the target folder.
 *
 * @property sourceHandle    Handle of the node being copied or moved.
 * @property collisionHandle Handle of the same-named node already in the target folder.
 * @property renameName      Name the screen offers to keep both.
 */
internal data class CollidingItem(
    val sourceHandle: Long,
    val collisionHandle: Long,
    val name: String,
    val isFile: Boolean,
    val renameName: String,
) {
    fun node(handle: Long, parentHandle: Long): MegaNode = StubMegaNode(
        handle = handle,
        name = name,
        parentHandle = parentHandle,
        isFolder = !isFile,
        size = if (isFile) NameCollisionFixtures.FILE_SIZE else 0L,
        creationTime = NameCollisionFixtures.TIMESTAMP,
        modificationTime = NameCollisionFixtures.TIMESTAMP,
    )

    fun collision(
        type: NodeNameCollisionType = NodeNameCollisionType.COPY,
    ) = NodeNameCollision.Default(
        collisionHandle = collisionHandle,
        nodeHandle = sourceHandle,
        name = name,
        size = if (isFile) NameCollisionFixtures.FILE_SIZE else 0L,
        childFolderCount = 0,
        childFileCount = 0,
        lastModified = NameCollisionFixtures.TIMESTAMP,
        parentHandle = NameCollisionFixtures.TARGET_FOLDER_HANDLE,
        isFile = isFile,
        type = type,
    )
}

internal fun FakeMegaApiGateway.awaitCopy(item: CollidingItem, newName: String?) {
    awaitInvocation("copyNode") { arguments ->
        (arguments[0] as MegaNode).handle == item.sourceHandle &&
                (arguments[1] as MegaNode).handle == NameCollisionFixtures.TARGET_FOLDER_HANDLE &&
                arguments[2] == newName
    }
}

/**
 * The copy and move commands are dispatched from a `viewModelScope.launch`, so their invocations
 * are not recorded synchronously after the tap that triggered them.
 */
internal fun FakeMegaApiGateway.awaitInvocation(
    methodName: String,
    matches: (List<Any?>) -> Boolean,
) {
    val deadline = SystemClock.uptimeMillis() + NameCollisionFixtures.LOAD_TIMEOUT
    while (SystemClock.uptimeMillis() < deadline) {
        val matched = invocations.any { it.methodName == methodName && matches(it.arguments) }
        if (matched) return
        Thread.sleep(POLL_INTERVAL_MS)
    }
    throw AssertionError(
        "Timed out after ${NameCollisionFixtures.LOAD_TIMEOUT}ms waiting for a matching " +
                "$methodName; recorded=${invocations.filter { it.methodName == methodName }}"
    )
}

internal fun NavigationResultManager.awaitOutcome(): NameCollisionOutcome = runBlocking {
    withTimeout(NameCollisionFixtures.LOAD_TIMEOUT) {
        monitorResult<NameCollisionOutcome>(NameCollisionNavKey.RESULT).first { it != null }
    }
}!!

internal fun NavigationResultManager.currentOutcome(): NameCollisionOutcome? = runBlocking {
    monitorResult<NameCollisionOutcome>(NameCollisionNavKey.RESULT).first()
}

/**
 * The options are laid out in a vertically scrolling column, so on shorter screens the rows at
 * the bottom are clipped out of the accessibility hierarchy until the content is scrolled.
 */
internal fun UiDevice.scrollUntilObject(selector: BySelector, name: String) {
    repeat(MAX_SCROLL_ATTEMPTS) {
        if (hasObject(selector)) return
        val scrollable = findObject(By.scrollable(true)) ?: return@repeat
        scrollable.setGestureMargin(scrollable.visibleBounds.height() / 4)
        scrollable.scroll(Direction.DOWN, SCROLL_PERCENT)
    }
    awaitObject(selector, NameCollisionFixtures.LOAD_TIMEOUT, name)
}

/**
 * Waits for [selector]; on timeout, writes the window hierarchy to logcat (tag `UiDump`,
 * chunked, so it survives the post-test uninstall) before failing.
 */
internal fun UiDevice.awaitObject(selector: BySelector, timeout: Long, name: String) {
    if (wait(Until.hasObject(selector), timeout)) return
    dumpHierarchy(name)
    throw AssertionError(
        "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
    )
}

internal fun UiDevice.awaitGone(selector: BySelector, name: String) {
    if (wait(Until.gone(selector), NameCollisionFixtures.LOAD_TIMEOUT)) return
    dumpHierarchy(name)
    throw AssertionError(
        "Timed out after ${NameCollisionFixtures.LOAD_TIMEOUT}ms waiting for $name ($selector) " +
                "to be gone; hierarchy in logcat tag UiDump"
    )
}

private fun UiDevice.dumpHierarchy(name: String) {
    val stream = ByteArrayOutputStream()
    dumpWindowHierarchy(stream)
    stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
        Log.d("UiDump", "[$name#$index] $chunk")
    }
}

private const val POLL_INTERVAL_MS = 100L
private const val MAX_SCROLL_ATTEMPTS = 5
private const val SCROLL_PERCENT = 0.8f
