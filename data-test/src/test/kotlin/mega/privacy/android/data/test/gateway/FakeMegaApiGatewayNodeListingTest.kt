package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaCancelToken
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaSearchFilter
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Covers the node listings [FakeMegaApiGateway] resolves from its node tree by reading the
 * `MegaSearchFilter` it is given: `getChildren` (direct children of the filter's location) and
 * `searchWithFilter` (the whole scope, recursively).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayNodeListingTest {

    private lateinit var underTest: FakeMegaApiGateway

    private val cancelToken = StubMegaCancelToken()

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
    }

    @Test
    fun `test that getChildren returns the direct children of the filter location`() = runTest {
        seedTree()

        val children = underTest.getChildren(filterOf(locationHandle = FOLDER_HANDLE))

        assertThat(children.map { it.name })
            .containsExactly("nested.txt", "deep")
            .inOrder()
    }

    @Test
    fun `test that getChildren returns an empty list when the filter has no location handle`() =
        runTest {
            seedTree()

            assertThat(underTest.getChildren(filterOf())).isEmpty()
        }

    @Test
    fun `test that getChildren applies the filter node type`() = runTest {
        seedTree()

        val folders = underTest.getChildren(
            filterOf(locationHandle = ROOT_HANDLE, nodeType = MegaNode.TYPE_FOLDER),
        )

        assertThat(folders.map { it.name }).containsExactly("folder")
    }

    @Test
    fun `test that searchWithFilter returns the whole subtree of the filter location`() = runTest {
        seedTree()

        val results = underTest.searchWithFilter(filterOf(locationHandle = FOLDER_HANDLE))

        assertThat(results.map { it.name })
            .containsExactly("nested.txt", "deep", "buried.txt")
    }

    @Test
    fun `test that searchWithFilter scopes to the Cloud Drive when the target is the root node`() =
        runTest {
            seedTree()
            underTest.nodeTree.addNode(
                StubMegaNode(handle = 90L, name = "trashed.txt", parentHandle = RUBBISH_HANDLE),
                parentHandle = RUBBISH_HANDLE,
            )

            val results = underTest.searchWithFilter(
                filterOf(location = MegaApiJava.SEARCH_TARGET_ROOTNODE),
            )

            assertThat(results.map { it.name }).doesNotContain("trashed.txt")
            assertThat(results.map { it.name }).contains("buried.txt")
        }

    @Test
    fun `test that searchWithFilter returns every added node when the target is all`() = runTest {
        seedTree()
        underTest.nodeTree.addNode(
            StubMegaNode(handle = 90L, name = "trashed.txt", parentHandle = RUBBISH_HANDLE),
            parentHandle = RUBBISH_HANDLE,
        )

        val results = underTest.searchWithFilter(filterOf())

        assertThat(results.map { it.name })
            .containsExactly("photo.jpg", "folder", "nested.txt", "deep", "buried.txt", "trashed.txt")
    }

    @Test
    fun `test that searchWithFilter matches the name term case insensitively`() = runTest {
        seedTree()

        val results = underTest.searchWithFilter(filterOf(name = "NEST"))

        assertThat(results.map { it.name }).containsExactly("nested.txt")
    }

    @Test
    fun `test that searchWithFilter matches the description when the text operator is or`() =
        runTest {
            underTest.nodeTree.addNode(
                StubMegaNode(
                    handle = 40L,
                    name = "notes.txt",
                    parentHandle = ROOT_HANDLE,
                    description = "holiday plans",
                ),
                parentHandle = ROOT_HANDLE,
            )

            val results = underTest.searchWithFilter(
                filterOf(name = "no-such-name", description = "holiday", useAndForText = false),
            )

            assertThat(results.map { it.name }).containsExactly("notes.txt")
        }

    @Test
    fun `test that searchWithFilter applies the favourite filter`() = runTest {
        seedTree()
        underTest.nodeTree.setFavourite(FILE_HANDLE, favourite = true)

        val results = underTest.searchWithFilter(
            filterOf(favourite = MegaSearchFilter.BOOL_FILTER_ONLY_TRUE),
        )

        assertThat(results.map { it.name }).containsExactly("photo.jpg")
    }

    @Test
    fun `test that searchWithFilter applies the sensitivity filter`() = runTest {
        seedTree()
        underTest.nodeTree.addNode(
            StubMegaNode(
                handle = 50L,
                name = "hidden.txt",
                parentHandle = ROOT_HANDLE,
                isMarkedSensitive = true,
            ),
            parentHandle = ROOT_HANDLE,
        )

        val visible = underTest.searchWithFilter(
            filterOf(sensitivity = MegaSearchFilter.BOOL_FILTER_ONLY_FALSE),
        )

        assertThat(visible.map { it.name }).doesNotContain("hidden.txt")
    }

    @Test
    fun `test that searchWithFilter applies the modification time window`() = runTest {
        underTest.nodeTree.addNode(
            StubMegaNode(
                handle = 60L,
                name = "old.txt",
                parentHandle = ROOT_HANDLE,
                modificationTime = 100L,
            ),
            parentHandle = ROOT_HANDLE,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(
                handle = 61L,
                name = "recent.txt",
                parentHandle = ROOT_HANDLE,
                modificationTime = 900L,
            ),
            parentHandle = ROOT_HANDLE,
        )

        val results = underTest.searchWithFilter(
            filterOf(modificationTimeLowerLimit = 500L, modificationTimeUpperLimit = 1_000L),
        )

        assertThat(results.map { it.name }).containsExactly("recent.txt")
    }

    @Test
    fun `test that searchWithFilter scopes to the incoming share subtree`() = runTest {
        val shareRoot = StubMegaNode(
            handle = 70L,
            name = "shared folder",
            isFolder = true,
            isInShare = true,
        )
        underTest.shares.addIncomingShare(
            node = shareRoot,
            ownerEmail = "owner@mega.nz",
            ownerHandle = 555L,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(handle = 71L, name = "shared.txt", parentHandle = 70L),
            parentHandle = 70L,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(handle = 72L, name = "mine.txt", parentHandle = ROOT_HANDLE),
            parentHandle = ROOT_HANDLE,
        )

        val results = underTest.searchWithFilter(
            filterOf(location = MegaApiJava.SEARCH_TARGET_INSHARE),
        )

        assertThat(results.map { it.name }).containsExactly("shared folder", "shared.txt")
    }

    @Test
    fun `test that an explicit getChildren stub overrides the tree backed default`() = runTest {
        seedTree()
        val stubbed = StubMegaNode(handle = 80L, name = "stubbed.txt")
        underTest.stub(MegaApiGateway::getChildren) { listOf(stubbed) }

        val children = underTest.getChildren(filterOf(locationHandle = ROOT_HANDLE))

        assertThat(children).containsExactly(stubbed)
    }

    @Test
    fun `test that an explicit searchWithFilter stub overrides the tree backed default`() =
        runTest {
            seedTree()
            val stubbed = StubMegaNode(handle = 81L, name = "stubbed.txt")
            underTest.stub(MegaApiGateway::searchWithFilter) { listOf(stubbed) }

            val results = underTest.searchWithFilter(filterOf(name = "nest"))

            assertThat(results).containsExactly(stubbed)
        }

    private suspend fun FakeMegaApiGateway.getChildren(filter: MegaSearchFilter) =
        getChildren(filter, MegaApiJava.ORDER_DEFAULT_ASC, cancelToken, null)

    private suspend fun FakeMegaApiGateway.searchWithFilter(filter: MegaSearchFilter) =
        searchWithFilter(filter, MegaApiJava.ORDER_DEFAULT_ASC, cancelToken, null)

    private fun filterOf(
        locationHandle: Long = MegaApiJava.INVALID_HANDLE,
        location: Int = MegaApiJava.SEARCH_TARGET_ALL,
        name: String = "",
        description: String? = null,
        nodeType: Int = MegaNode.TYPE_UNKNOWN,
        favourite: Int = MegaSearchFilter.BOOL_FILTER_DISABLED,
        sensitivity: Int = MegaSearchFilter.BOOL_FILTER_DISABLED,
        modificationTimeLowerLimit: Long = 0L,
        modificationTimeUpperLimit: Long = 0L,
        useAndForText: Boolean = true,
    ) = StubMegaSearchFilter(
        name = name,
        nodeType = nodeType,
        favourite = favourite,
        sensitivity = sensitivity,
        locationHandle = locationHandle,
        location = location,
        modificationTimeLowerLimit = modificationTimeLowerLimit,
        modificationTimeUpperLimit = modificationTimeUpperLimit,
        description = description,
        useAndForText = useAndForText,
    )

    /**
     * Cloud Drive → photo.jpg, folder → (nested.txt, deep → buried.txt).
     */
    private fun seedTree() {
        underTest.nodeTree.addNode(
            StubMegaNode(handle = FILE_HANDLE, name = "photo.jpg", parentHandle = ROOT_HANDLE),
            parentHandle = ROOT_HANDLE,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(
                handle = FOLDER_HANDLE,
                name = "folder",
                parentHandle = ROOT_HANDLE,
                isFolder = true,
            ),
            parentHandle = ROOT_HANDLE,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(handle = 12L, name = "nested.txt", parentHandle = FOLDER_HANDLE),
            parentHandle = FOLDER_HANDLE,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(
                handle = 13L,
                name = "deep",
                parentHandle = FOLDER_HANDLE,
                isFolder = true,
            ),
            parentHandle = FOLDER_HANDLE,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(handle = 14L, name = "buried.txt", parentHandle = 13L),
            parentHandle = 13L,
        )
    }

    private companion object {
        const val ROOT_HANDLE = 1L
        const val RUBBISH_HANDLE = 2L
        const val FILE_HANDLE = 10L
        const val FOLDER_HANDLE = 11L
    }
}
