package mega.privacy.android.app.providers.documentprovider

import android.app.AuthenticationRequiredException
import android.content.ContentProvider
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.app.providers.documentprovider.model.ChildrenSlot
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentRow
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveSessionState
import mega.privacy.android.app.providers.documentprovider.model.DocumentSlot
import mega.privacy.android.app.R
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Field

/**
 * Robolectric-based tests for [CloudDriveDocumentProvider].
 * Uses a mock [CloudDriveDocumentDataProvider] so only the provider's behaviour is tested;
 * no use case dependencies are required.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class CloudDriveDocumentProviderTest {

    private val mockDataProvider: CloudDriveDocumentDataProvider = mock()
    private val mockEntryPoint: CloudDriveDocumentProviderEntryPoint = mock()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val sessionState =
        MutableStateFlow<CloudDriveSessionState>(CloudDriveSessionState.Initialising)
    private val documentState = MutableStateFlow<DocumentSlot>(DocumentSlot.Idle)
    private val childrenState = MutableStateFlow<ChildrenSlot>(ChildrenSlot.Idle)

    private lateinit var testScheduler: TestCoroutineScheduler
    private lateinit var testScope: TestScope
    private lateinit var underTest: CloudDriveDocumentProvider

    private companion object {
        private const val CLOUD_DRIVE_ROOT_ID = "mega_cloud_drive_root"
        private const val ROOT_NODE_DOCUMENT_ID = "mega_cloud_drive_root:1"
    }

    @Before
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        sessionState.value = CloudDriveSessionState.Initialising
        documentState.value = DocumentSlot.Idle
        childrenState.value = ChildrenSlot.Idle
        whenever(mockDataProvider.sessionState).thenReturn(sessionState)
        whenever(mockDataProvider.documentState).thenReturn(documentState)
        whenever(mockDataProvider.childrenState).thenReturn(childrenState)
        whenever(mockDataProvider.findCachedChildRow(any())).thenReturn(null)
        whenever(mockDataProvider.getPendingChildrenForParent(any())).thenReturn(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(mockDataProvider, mockEntryPoint)
    }

    private fun createProvider(): CloudDriveDocumentProvider {
        whenever(mockEntryPoint.cloudDriveDocumentDataProvider()).thenReturn(mockDataProvider)
        whenever(mockEntryPoint.applicationScope()).thenReturn(testScope)
        val provider = CloudDriveDocumentProvider()
        injectDependencyContainer(provider, lazy { mockEntryPoint })
        attachContext(provider, ApplicationProvider.getApplicationContext())
        underTest = provider
        return provider
    }

    private fun injectDependencyContainer(instance: Any, value: Any) {
        val field: Field = instance.javaClass.getDeclaredField("dependencyContainer\$delegate")
        field.isAccessible = true
        field.set(instance, value)
    }

    private fun attachContext(provider: ContentProvider, context: Context) {
        val field: Field = ContentProvider::class.java.getDeclaredField("mContext")
        field.isAccessible = true
        field.set(provider, context)
    }

    private fun setReadySession(accountName: String = "test@mega.co.nz") {
        sessionState.value = CloudDriveSessionState.Ready(
            accountName = accountName,
            rootNodeDocumentId = ROOT_NODE_DOCUMENT_ID,
        )
    }

    @Test
    fun `test that onCreate returns true`() = runTest {
        createProvider()
        assertThat(underTest.onCreate()).isTrue()
    }

    @Test
    fun `test that queryRoots returns one root row with account name when session is Ready`() =
        runTest {
            setReadySession()
            createProvider()

            val cursor: Cursor = underTest.queryRoots(null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_ROOT_ID)))
                .isEqualTo(CLOUD_DRIVE_ROOT_ID)
            assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_DOCUMENT_ID)))
                .isEqualTo(CLOUD_DRIVE_ROOT_ID)
            assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_SUMMARY)))
                .isEqualTo("test@mega.co.nz")
        }

    @Test
    fun `test that queryRoots advertises zero root flags so DocumentsUI does not show create action`() =
        runTest {
            setReadySession()
            createProvider()

            val cursor: Cursor = underTest.queryRoots(null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            // FLAG_SUPPORTS_CREATE on the root makes DocumentsUI replace the Get Info action
            // with a "save here" picker, which breaks Get Info access for AND-23569.
            assertThat(cursor.getInt(cursor.getColumnIndex(Root.COLUMN_FLAGS))).isEqualTo(0)
        }

    @Test
    fun `test that queryRoots returns login summary when session is NotLoggedIn`() = runTest {
        sessionState.value = CloudDriveSessionState.NotLoggedIn
        createProvider()

        val cursor: Cursor = underTest.queryRoots(null)
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        val expectedSummary = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.login_to_mega)
        assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_SUMMARY)))
            .isEqualTo(expectedSummary)
    }

    @Test
    fun `test that queryRoots returns account summary when session is Offline`() = runTest {
        sessionState.value = CloudDriveSessionState.Offline("test@mega.co.nz")
        createProvider()

        val cursor: Cursor = underTest.queryRoots(null)
        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_SUMMARY)))
            .isEqualTo("test@mega.co.nz")
    }

    @Test
    fun `test that queryRoots returns account summary when session is PasscodeLockEnabled`() =
        runTest {
            sessionState.value =
                CloudDriveSessionState.PasscodeLockEnabled("test@mega.co.nz")
            createProvider()

            val cursor: Cursor = underTest.queryRoots(null)
            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_SUMMARY)))
                .isEqualTo("test@mega.co.nz")
        }

    @Test
    fun `test that queryDocument throws exception when document id null`() = runTest {
        setReadySession()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.queryDocument(null, null)
        }
        assertThat(e).hasMessageThat().contains("Invalid document id")
    }

    @Test
    fun `test that queryDocument throws when document id empty`() = runTest {
        setReadySession()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.queryDocument("", null)
        }
        assertThat(e).hasMessageThat().contains("Invalid document id")
    }

    @Test
    fun `test that queryDocument returns root folder row when documentId is CLOUD_DRIVE_ROOT_ID`() =
        runTest {
            createProvider()

            val cursor: Cursor = underTest.queryDocument(CLOUD_DRIVE_ROOT_ID, null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo(CLOUD_DRIVE_ROOT_ID)
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)))
                .isEqualTo(Document.MIME_TYPE_DIR)
            val expectedRootName = ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.app_name)
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
                .isEqualTo(expectedRootName)
            // FLAG_DIR_SUPPORTS_CREATE on the root folder row makes DocumentsUI swap Get Info
            // for the "save here" picker.
            assertThat(cursor.getInt(cursor.getColumnIndex(Document.COLUMN_FLAGS))).isEqualTo(0)
        }

    @Test
    fun `test that queryDocument returns root folder row when documentId is root even when session is Offline`() =
        runTest {
            sessionState.value = CloudDriveSessionState.Offline("test@mega.co.nz")
            createProvider()

            val cursor: Cursor = underTest.queryDocument(CLOUD_DRIVE_ROOT_ID, null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo(CLOUD_DRIVE_ROOT_ID)
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)))
                .isEqualTo(Document.MIME_TYPE_DIR)
        }

    @Test
    fun `test that queryDocument returns error cursor when session is PasscodeLockEnabled`() =
        runTest {
            sessionState.value =
                CloudDriveSessionState.PasscodeLockEnabled("test@mega.co.nz")
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:123", null)

            assertThat(cursor.count).isEqualTo(0)
            val passcodeLockEnabledMessage = ApplicationProvider.getApplicationContext<Context>()
                .getString(sharedR.string.saf_passcode_lock_enabled_message)
            assertThat(cursor.extras?.getString(DocumentsContract.EXTRA_ERROR))
                .isEqualTo(passcodeLockEnabledMessage)
        }

    @Test
    fun `test that queryDocument returns error cursor with offline message when session is Offline and documentId is not root`() =
        runTest {
            sessionState.value = CloudDriveSessionState.Offline("test@mega.co.nz")
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:123", null)

            assertThat(cursor.count).isEqualTo(0)
            val noInternetMessage = ApplicationProvider.getApplicationContext<Context>()
                .getString(sharedR.string.saf_no_internet_message)
            assertThat(cursor.extras?.getString(DocumentsContract.EXTRA_ERROR))
                .isEqualTo(noInternetMessage)
        }

    @Test
    fun `test that queryDocument returns folder row when documentState is Loaded and documentId matches`() =
        runTest {
            setReadySession()
            val folderRow = CloudDriveDocumentRow(
                documentId = "$CLOUD_DRIVE_ROOT_ID:999",
                displayName = "Test Folder",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 2000L,
                flags = 0,
            )
            documentState.value = DocumentSlot.Loaded(
                documentId = "$CLOUD_DRIVE_ROOT_ID:999",
                row = folderRow,
            )
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:999", null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo("$CLOUD_DRIVE_ROOT_ID:999")
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
                .isEqualTo("Test Folder")
            assertThat(cursor.getLong(cursor.getColumnIndex(Document.COLUMN_LAST_MODIFIED)))
                .isEqualTo(2000L)
        }

    @Test
    fun `test that queryDocument returns file row when documentState is Loaded and node is file`() =
        runTest {
            setReadySession()
            val fileRow = CloudDriveDocumentRow(
                documentId = "$CLOUD_DRIVE_ROOT_ID:888",
                displayName = "test.pdf",
                mimeType = "application/pdf",
                size = 1024L,
                lastModified = 3000L,
                flags = 0,
            )
            documentState.value = DocumentSlot.Loaded(
                documentId = "$CLOUD_DRIVE_ROOT_ID:888",
                row = fileRow,
            )
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:888", null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)))
                .isEqualTo("application/pdf")
            assertThat(cursor.getLong(cursor.getColumnIndex(Document.COLUMN_SIZE))).isEqualTo(1024L)
            assertThat(cursor.getLong(cursor.getColumnIndex(Document.COLUMN_LAST_MODIFIED)))
                .isEqualTo(3000L)
        }

    @Test
    fun `test that queryDocument returns cached row when documentState mismatches but cache hits`() =
        runTest {
            // Get Info flow: a child listing was previously loaded, then SAF queries the child
            // doc directly. documentState may not yet be Loaded for the requested id, but the
            // cached row from the prior listing should serve immediately without a loading cursor.
            setReadySession()
            val requestedId = "$CLOUD_DRIVE_ROOT_ID:42"
            val cachedRow = CloudDriveDocumentRow(
                documentId = requestedId,
                displayName = "cached.txt",
                mimeType = "text/plain",
                size = 42L,
                lastModified = 5000L,
                flags = Document.FLAG_SUPPORTS_RENAME,
            )
            whenever(mockDataProvider.findCachedChildRow(requestedId)).thenReturn(cachedRow)
            createProvider()

            val cursor: Cursor = underTest.queryDocument(requestedId, null)

            assertThat(cursor.count).isEqualTo(1)
            assertThat(cursor.extras?.getBoolean(DocumentsContract.EXTRA_LOADING) ?: false)
                .isFalse()
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo(requestedId)
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
                .isEqualTo("cached.txt")
            // Cache hit still warms documentState for subsequent queries.
            verify(mockDataProvider).loadDocumentInBackground(requestedId)
        }

    @Test
    fun `test that queryDocument returns loading cursor when documentState is Loading and no cache hit`() =
        runTest {
            setReadySession()
            documentState.value = DocumentSlot.Loading("$CLOUD_DRIVE_ROOT_ID:777")
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:777", null)
            assertThat(cursor.extras?.getBoolean(DocumentsContract.EXTRA_LOADING)).isTrue()
            assertThat(cursor.count).isEqualTo(0)
        }

    @Test
    fun `test that queryDocument with documentState Loaded for a different id loads requested in background`() =
        runTest {
            setReadySession()
            documentState.value = DocumentSlot.Loaded(
                documentId = "$CLOUD_DRIVE_ROOT_ID:999",
                row = CloudDriveDocumentRow(
                    documentId = "$CLOUD_DRIVE_ROOT_ID:999",
                    displayName = "Other",
                    mimeType = Document.MIME_TYPE_DIR,
                    size = 0L,
                    lastModified = 0L,
                    flags = 0,
                ),
            )
            createProvider()

            underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:777", null)
            verify(mockDataProvider).loadDocumentInBackground("$CLOUD_DRIVE_ROOT_ID:777")
        }

    @Test
    fun `test that queryDocument throws when documentState is NotFound for requested documentId`() =
        runTest {
            setReadySession()
            documentState.value = DocumentSlot.NotFound("$CLOUD_DRIVE_ROOT_ID:12345")
            createProvider()

            val e = assertThrows<FileNotFoundException> {
                underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:12345", null)
            }
            assertThat(e).hasMessageThat().contains("Node not found")
        }

    @Test
    fun `test that queryDocument uses default document projection when projection null`() =
        runTest {
            createProvider()

            val cursor: Cursor = underTest.queryDocument(CLOUD_DRIVE_ROOT_ID, null)
            assertThat(cursor.columnNames).asList().containsAtLeast(
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_MIME_TYPE,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_SIZE,
                Document.COLUMN_LAST_MODIFIED,
                Document.COLUMN_FLAGS
            )
        }

    @Test
    fun `test that queryDocument throws AuthenticationRequiredException when NotLoggedIn and documentID is not root`() =
        runTest {
            sessionState.value = CloudDriveSessionState.NotLoggedIn
            createProvider()

            assertThrows<AuthenticationRequiredException> {
                underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:123", null)
            }
        }

    @Test
    fun `test that queryChildDocuments throws exception when parent document id empty`() = runTest {
        setReadySession()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.queryChildDocuments(parentDocumentId = "", null, null)
        }
        assertThat(e).hasMessageThat().contains("Invalid parent document id")
    }

    @Test
    fun `test that queryChildDocuments returns error cursor when session is Offline`() = runTest {
        sessionState.value = CloudDriveSessionState.Offline("test@mega.co.nz")
        createProvider()

        val cursor: Cursor =
            underTest.queryChildDocuments(parentDocumentId = CLOUD_DRIVE_ROOT_ID, null, null)

        assertThat(cursor.count).isEqualTo(0)
        val noInternetMessage = ApplicationProvider.getApplicationContext<Context>()
            .getString(sharedR.string.saf_no_internet_message)
        assertThat(cursor.extras?.getString(DocumentsContract.EXTRA_ERROR))
            .isEqualTo(noInternetMessage)
    }

    @Test
    fun `test that queryChildDocuments returns error cursor when session is PasscodeLockEnabled`() =
        runTest {
            sessionState.value =
                CloudDriveSessionState.PasscodeLockEnabled("test@mega.co.nz")
            createProvider()

            val cursor: Cursor =
                underTest.queryChildDocuments(parentDocumentId = CLOUD_DRIVE_ROOT_ID, null, null)

            assertThat(cursor.count).isEqualTo(0)
            val passcodeLockEnabledMessage = ApplicationProvider.getApplicationContext<Context>()
                .getString(sharedR.string.saf_passcode_lock_enabled_message)
            assertThat(cursor.extras?.getString(DocumentsContract.EXTRA_ERROR))
                .isEqualTo(passcodeLockEnabledMessage)
        }

    @Test
    fun `test that queryChildDocuments calls loadChildrenInBackground when no cache and Idle`() =
        runTest {
            setReadySession()
            createProvider()

            underTest.queryChildDocuments(parentDocumentId = CLOUD_DRIVE_ROOT_ID, null, null)
            verify(mockDataProvider).loadChildrenInBackground(CLOUD_DRIVE_ROOT_ID)
        }

    @Test
    fun `test that queryChildDocuments returns loading cursor when childrenState is Loading for same parent`() =
        runTest {
            setReadySession()
            childrenState.value = ChildrenSlot.Loading(CLOUD_DRIVE_ROOT_ID)
            createProvider()

            val cursor: Cursor =
                underTest.queryChildDocuments(parentDocumentId = CLOUD_DRIVE_ROOT_ID, null, null)
            assertThat(cursor.extras?.getBoolean(DocumentsContract.EXTRA_LOADING)).isTrue()
            assertThat(cursor.count).isEqualTo(0)
        }

    @Test
    fun `test that queryChildDocuments throws AuthenticationRequiredException when NotLoggedIn`() =
        runTest {
            sessionState.value = CloudDriveSessionState.NotLoggedIn
            createProvider()

            assertThrows<AuthenticationRequiredException> {
                underTest.queryChildDocuments(
                    parentDocumentId = CLOUD_DRIVE_ROOT_ID,
                    null,
                    null
                )
            }
        }

    @Test
    fun `test that queryChildDocuments returns child rows when childrenState is Loaded and parentId matches`() =
        runTest {
            setReadySession()
            val childRow = CloudDriveDocumentRow(
                documentId = "$CLOUD_DRIVE_ROOT_ID:100",
                displayName = "Child Folder",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 1500L,
                flags = 0,
            )
            childrenState.value = ChildrenSlot.Loaded(
                parentDocumentId = CLOUD_DRIVE_ROOT_ID,
                children = listOf(childRow),
                hasMore = false,
            )
            createProvider()

            val cursor: Cursor =
                underTest.queryChildDocuments(parentDocumentId = CLOUD_DRIVE_ROOT_ID, null, null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo("$CLOUD_DRIVE_ROOT_ID:100")
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
                .isEqualTo("Child Folder")
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)))
                .isEqualTo(Document.MIME_TYPE_DIR)
        }

    @Test
    fun `test that queryChildDocuments returns non-loading empty cursor for cached folder Info screen fast path`() =
        runTest {
            // AND-23569 Get Info flow: when SAF queries children for a folder whose row is
            // already cached, return a non-loading empty cursor so the Info screen renders
            // the folder's metadata immediately while the actual children load in background.
            setReadySession()
            val folderId = "$CLOUD_DRIVE_ROOT_ID:50"
            val folderRow = CloudDriveDocumentRow(
                documentId = folderId,
                displayName = "FolderForInfo",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 1000L,
                flags = Document.FLAG_SUPPORTS_RENAME,
            )
            whenever(mockDataProvider.findCachedChildRow(folderId)).thenReturn(folderRow)
            createProvider()

            val cursor: Cursor =
                underTest.queryChildDocuments(parentDocumentId = folderId, null, null)

            assertThat(cursor.count).isEqualTo(0)
            assertThat(cursor.extras?.getBoolean(DocumentsContract.EXTRA_LOADING) ?: false)
                .isFalse()
            verify(mockDataProvider).loadChildrenInBackground(folderId)
        }

    @Test
    fun `test that queryChildDocuments still returns Loaded children when cached row matches an already-loaded parent`() =
        runTest {
            // The fast path is only taken when childrenState has not yet caught up. Once the
            // load completes and childrenState becomes Loaded for the same parent, the cursor
            // should serve the real rows instead of returning empty.
            setReadySession()
            val folderId = "$CLOUD_DRIVE_ROOT_ID:60"
            val folderRow = CloudDriveDocumentRow(
                documentId = folderId,
                displayName = "Folder",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            val childRow = CloudDriveDocumentRow(
                documentId = "$CLOUD_DRIVE_ROOT_ID:61",
                displayName = "child.txt",
                mimeType = "text/plain",
                size = 1L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(mockDataProvider.findCachedChildRow(folderId)).thenReturn(folderRow)
            childrenState.value = ChildrenSlot.Loaded(
                parentDocumentId = folderId,
                children = listOf(childRow),
                hasMore = false,
            )
            createProvider()

            val cursor: Cursor =
                underTest.queryChildDocuments(parentDocumentId = folderId, null, null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo("$CLOUD_DRIVE_ROOT_ID:61")
        }

    @Test
    fun `test that queryChildDocuments when childrenState Loaded with different parent calls loadChildrenInBackground`() =
        runTest {
            setReadySession()
            childrenState.value = ChildrenSlot.Loaded(
                parentDocumentId = CLOUD_DRIVE_ROOT_ID,
                children = emptyList(),
                hasMore = false,
            )
            createProvider()

            underTest.queryChildDocuments(parentDocumentId = "$CLOUD_DRIVE_ROOT_ID:999", null, null)
            verify(mockDataProvider).loadChildrenInBackground("$CLOUD_DRIVE_ROOT_ID:999")
        }

    @Test
    fun `test that openDocument throws FileNotFoundException when documentId is null`() = runTest {
        setReadySession()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.openDocument(null, "r", null)
        }
        assertThat(e).hasMessageThat().contains("Invalid document id")
    }

    @Test
    fun `test that openDocument throws FileNotFoundException when documentId is empty`() = runTest {
        setReadySession()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.openDocument("", "r", null)
        }
        assertThat(e).hasMessageThat().contains("Invalid document id")
    }

    @Test
    fun `test that openDocument throws FileNotFoundException when documentId is root`() = runTest {
        setReadySession()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.openDocument(CLOUD_DRIVE_ROOT_ID, "r", null)
        }
        assertThat(e).hasMessageThat().contains("root")
    }

    @Test
    fun `test that openDocument throws AuthenticationRequiredException when session is NotLoggedIn`() =
        runTest {
            sessionState.value = CloudDriveSessionState.NotLoggedIn
            createProvider()

            assertThrows<AuthenticationRequiredException> {
                underTest.openDocument("$CLOUD_DRIVE_ROOT_ID:123", "r", null)
            }
        }

    @Test
    fun `test that openDocument throws FileNotFoundException when session is PasscodeLockEnabled`() =
        runTest {
            sessionState.value =
                CloudDriveSessionState.PasscodeLockEnabled("test@mega.co.nz")
            createProvider()

            assertThrows<FileNotFoundException> {
                underTest.openDocument("$CLOUD_DRIVE_ROOT_ID:123", "r", null)
            }
        }

    @Test
    fun `test that openDocument throws FileNotFoundException when session is Offline`() = runTest {
        sessionState.value = CloudDriveSessionState.Offline("test@mega.co.nz")
        createProvider()

        assertThrows<FileNotFoundException> {
            underTest.openDocument("$CLOUD_DRIVE_ROOT_ID:123", "r", null)
        }
    }

    @Test
    fun `test that openDocument returns readable ParcelFileDescriptor when openDocumentFile succeeds`() =
        runTest {
            setReadySession()
            val tempFile = File.createTempFile("cloud_drive_doc", ".txt").apply {
                writeText("hello")
                deleteOnExit()
            }
            wheneverBlocking { mockDataProvider.openDocumentFile(any()) }.thenReturn(tempFile)
            createProvider()

            val pfd = underTest.openDocument("$CLOUD_DRIVE_ROOT_ID:123", "r", null)

            assertThat(pfd).isNotNull()
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                assertThat(String(input.readBytes())).isEqualTo("hello")
            }
            verifyBlocking(mockDataProvider) { openDocumentFile("$CLOUD_DRIVE_ROOT_ID:123") }
        }

    @Test
    fun `test that openDocument throws FileNotFoundException when openDocumentFile throws FileNotFoundException`() =
        runTest {
            setReadySession()
            wheneverBlocking { mockDataProvider.openDocumentFile(any()) }
                .thenAnswer { throw FileNotFoundException("Node not found") }
            createProvider()

            val e = assertThrows<FileNotFoundException> {
                underTest.openDocument("$CLOUD_DRIVE_ROOT_ID:123", "r", null)
            }
            assertThat(e).hasMessageThat().contains("Node not found")
            verifyBlocking(mockDataProvider) { openDocumentFile("$CLOUD_DRIVE_ROOT_ID:123") }
        }

    @Test
    fun `test that openDocument returns writable PFD for pending placeholder id`() = runTest {
        setReadySession()
        val pendingId = "mega_cloud_drive_pending:abc"
        val tempFile = File.createTempFile("scratch_", ".tmp").apply { deleteOnExit() }
        whenever(mockDataProvider.isPendingDocumentId(pendingId)).thenReturn(true)
        wheneverBlocking { mockDataProvider.prepareWriteScratchFile(pendingId) }
            .thenReturn(tempFile)
        createProvider()

        val pfd = underTest.openDocument(pendingId, "w", null)

        assertThat(pfd).isNotNull()
        pfd.close()
    }

    @Test
    fun `test that createDocument with MIME_TYPE_DIR returns placeholder id from registerPendingFolder`() =
        runTest {
            setReadySession()
            val pendingId = "mega_cloud_drive_pending:folder-uuid"
            wheneverBlocking { mockDataProvider.registerPendingFolder(any(), any()) }
                .thenReturn(pendingId)
            createProvider()

            val result = underTest.createDocument(
                CLOUD_DRIVE_ROOT_ID,
                Document.MIME_TYPE_DIR,
                "MyFolder",
            )

            assertThat(result).isEqualTo(pendingId)
            verifyBlocking(mockDataProvider) {
                registerPendingFolder(CLOUD_DRIVE_ROOT_ID, "MyFolder")
            }
        }

    @Test
    fun `test that createDocument with file mime delegates to registerPendingFile`() = runTest {
        setReadySession()
        val pendingId = "mega_cloud_drive_pending:xyz"
        wheneverBlocking { mockDataProvider.registerPendingFile(any(), any(), any()) }
            .thenReturn(pendingId)
        createProvider()

        val result = underTest.createDocument(
            CLOUD_DRIVE_ROOT_ID,
            "text/plain",
            "note.txt",
        )

        assertThat(result).isEqualTo(pendingId)
        verifyBlocking(mockDataProvider) {
            registerPendingFile(CLOUD_DRIVE_ROOT_ID, "note.txt", "text/plain")
        }
    }

    @Test
    fun `test that renameDocument delegates to dataProvider and returns document id`() = runTest {
        setReadySession()
        val documentId = "$CLOUD_DRIVE_ROOT_ID:123"
        wheneverBlocking { mockDataProvider.renameDocument(any(), any()) }
            .thenReturn(CLOUD_DRIVE_ROOT_ID)
        createProvider()

        val result = underTest.renameDocument(documentId, "renamed.txt")

        assertThat(result).isEqualTo(documentId)
        verifyBlocking(mockDataProvider) { renameDocument(documentId, "renamed.txt") }
    }

    @Test
    fun `test that queryDocument returns synthesized row for pending placeholder id`() = runTest {
        setReadySession()
        val pendingId = "mega_cloud_drive_pending:abc"
        val pendingRow = CloudDriveDocumentRow(
            documentId = pendingId,
            displayName = "draft.txt",
            mimeType = "text/plain",
            size = 0L,
            lastModified = 1L,
            flags = 0,
        )
        whenever(mockDataProvider.isPendingDocumentId(pendingId)).thenReturn(true)
        whenever(mockDataProvider.getPendingDocumentRow(pendingId)).thenReturn(pendingRow)
        createProvider()

        val cursor: Cursor = underTest.queryDocument(pendingId, null)

        assertThat(cursor.moveToFirst()).isTrue()
        assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
            .isEqualTo("draft.txt")
        assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
            .isEqualTo(pendingId)
    }
}
