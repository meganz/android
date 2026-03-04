package mega.privacy.android.app.providers.documentprovider

import android.content.ContentProvider
import android.content.Context
import android.database.Cursor
import android.os.Build
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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

    private val dataProviderState = MutableStateFlow<CloudDriveDocumentProviderUiState>(
        CloudDriveDocumentProviderUiState.LoadingRoot
    )

    private lateinit var testScheduler: TestCoroutineScheduler
    private lateinit var testScope: TestScope
    private lateinit var underTest: CloudDriveDocumentProvider

    private companion object {
        private const val CLOUD_DRIVE_ROOT_ID = "mega_cloud_drive_root"
        private const val ROOT_DOCUMENT_ID = "mega_cloud_drive_root:1"
    }

    @Before
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        dataProviderState.value = CloudDriveDocumentProviderUiState.LoadingRoot
        whenever(mockDataProvider.state).thenReturn(dataProviderState)
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

    private fun setRootState(
        accountName: String = "test@mega.co.nz",
        rootNodeDocumentId: String = ROOT_DOCUMENT_ID,
    ) {
        dataProviderState.value = CloudDriveDocumentProviderUiState.Root(
            accountName = accountName,
            rootNodeDocumentId = rootNodeDocumentId,
        )
    }

    @Test
    fun `test that onCreate returns true`() = runTest {
        createProvider()
        assertThat(underTest.onCreate()).isTrue()
    }

    @Test
    fun `test that queryRoots returns one root row when state is Root`() = runTest {
        setRootState()
        createProvider()

        val cursor: Cursor = underTest.queryRoots(null)

        assertThat(cursor.count).isEqualTo(1)
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_ROOT_ID)))
            .isEqualTo(CLOUD_DRIVE_ROOT_ID)
        assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_DOCUMENT_ID)))
            .isEqualTo(ROOT_DOCUMENT_ID)
        assertThat(cursor.getString(cursor.getColumnIndex(Root.COLUMN_SUMMARY)))
            .isEqualTo("test@mega.co.nz")
    }

    @Test
    fun `test that queryRoots returns empty cursor when user not logged in`() = runTest {
        dataProviderState.value = CloudDriveDocumentProviderUiState.NotLoggedIn
        createProvider()

        val cursor: Cursor = underTest.queryRoots(null)
        assertThat(cursor.count).isEqualTo(0)
    }

    @Test
    fun `test that queryDocument throws exception when document id null`() = runTest {
        setRootState()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.queryDocument(null, null)
        }
        assertThat(e).hasMessageThat().contains("Invalid document id")
    }

    @Test
    fun `test that queryDocument throws when document id empty`() = runTest {
        setRootState()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.queryDocument("", null)
        }
        assertThat(e).hasMessageThat().contains("Invalid document id")
    }

    @Test
    fun `test that queryDocument returns root folder row when Root state and documentId matches rootNodeDocumentId`() =
        runTest {
            setRootState()
            createProvider()

            val cursor: Cursor = underTest.queryDocument(ROOT_DOCUMENT_ID, null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo(ROOT_DOCUMENT_ID)
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)))
                .isEqualTo(Document.MIME_TYPE_DIR)
            val expectedRootName = ApplicationProvider.getApplicationContext<Context>()
                .getString(R.string.app_name) ?: "MEGA"
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
                .isEqualTo(expectedRootName)
        }

    @Test
    fun `test that queryDocument returns folder row when state is DocumentData and documentId matches`() =
        runTest {
            val folderRow = CloudDriveDocumentRow(
                documentId = "$CLOUD_DRIVE_ROOT_ID:999",
                displayName = "Test Folder",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 2000L,
                flags = 0,
            )
            dataProviderState.value = CloudDriveDocumentProviderUiState.DocumentData(
                accountName = "test@mega.co.nz",
                documentId = "$CLOUD_DRIVE_ROOT_ID:999",
                document = folderRow,
                rootNodeDocumentId = ROOT_DOCUMENT_ID,
            )
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:999", null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo("$CLOUD_DRIVE_ROOT_ID:999")
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
                .isEqualTo("Test Folder")
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)))
                .isEqualTo(Document.MIME_TYPE_DIR)
            assertThat(cursor.getLong(cursor.getColumnIndex(Document.COLUMN_LAST_MODIFIED)))
                .isEqualTo(2000L)
        }

    @Test
    fun `test that queryDocument returns file row when state is DocumentData and node is file`() =
        runTest {
            val fileRow = CloudDriveDocumentRow(
                documentId = "$CLOUD_DRIVE_ROOT_ID:888",
                displayName = "test.pdf",
                mimeType = "application/pdf",
                size = 1024L,
                lastModified = 3000L,
                flags = 0,
            )
            dataProviderState.value = CloudDriveDocumentProviderUiState.DocumentData(
                accountName = "test@mega.co.nz",
                documentId = "$CLOUD_DRIVE_ROOT_ID:888",
                document = fileRow,
                rootNodeDocumentId = ROOT_DOCUMENT_ID,
            )
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:888", null)

            assertThat(cursor.count).isEqualTo(1)
            cursor.moveToFirst()
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)))
                .isEqualTo("$CLOUD_DRIVE_ROOT_ID:888")
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)))
                .isEqualTo("test.pdf")
            assertThat(cursor.getString(cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)))
                .isEqualTo("application/pdf")
            assertThat(cursor.getLong(cursor.getColumnIndex(Document.COLUMN_SIZE))).isEqualTo(1024L)
            assertThat(cursor.getLong(cursor.getColumnIndex(Document.COLUMN_LAST_MODIFIED)))
                .isEqualTo(3000L)
        }

    @Test
    fun `test that queryDocument returns loading cursor when LoadingDocument`() =
        runTest {
            dataProviderState.value = CloudDriveDocumentProviderUiState.LoadingDocument(
                accountName = "test@mega.co.nz",
                currentDocumentId = "$CLOUD_DRIVE_ROOT_ID:777",
                rootNodeDocumentId = ROOT_DOCUMENT_ID,
            )
            createProvider()

            val cursor: Cursor = underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:777", null)
            assertThat(cursor.extras?.getBoolean(DocumentsContract.EXTRA_LOADING)).isTrue()
            assertThat(cursor.count).isEqualTo(0)
        }

    @Test
    fun `test that queryDocument when Root and documentId mismatch calls loadDocumentInBackground`() =
        runTest {
            setRootState()
            createProvider()

            underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:777", null)
            verify(mockDataProvider).loadDocumentInBackground("$CLOUD_DRIVE_ROOT_ID:777")
        }

    @Test
    fun `test that queryDocument throws exception when state is FileNotFound for requested documentId`() =
        runTest {
            dataProviderState.value = CloudDriveDocumentProviderUiState.FileNotFound(
                accountName = "test@mega.co.nz",
                documentId = "$CLOUD_DRIVE_ROOT_ID:12345",
                rootNodeDocumentId = ROOT_DOCUMENT_ID,
            )
            createProvider()

            val e = assertThrows<FileNotFoundException> {
                underTest.queryDocument("$CLOUD_DRIVE_ROOT_ID:12345", null)
            }
            assertThat(e).hasMessageThat().contains("Node not found")
        }

    @Test
    fun `test that queryDocument uses default document projection when projection null`() =
        runTest {
            setRootState()
            createProvider()

            val cursor: Cursor = underTest.queryDocument(ROOT_DOCUMENT_ID, null)
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
    fun `test that queryChildDocuments throws exception when parent document id empty`() = runTest {
        setRootState()
        createProvider()

        val e = assertThrows<FileNotFoundException> {
            underTest.queryChildDocuments(parentDocumentId = "", null, null)
        }
        assertThat(e).hasMessageThat().contains("Invalid parent document id")
    }

    @Test
    fun `test that queryChildDocuments when Root state calls loadChildrenInBackground`() = runTest {
        setRootState()
        createProvider()

        underTest.queryChildDocuments(parentDocumentId = ROOT_DOCUMENT_ID, null, null)
        verify(mockDataProvider).loadChildrenInBackground(ROOT_DOCUMENT_ID)
    }

    @Test
    fun `test that queryChildDocuments returns loading cursor when state is LoadingChildren`() =
        runTest {
            dataProviderState.value = CloudDriveDocumentProviderUiState.LoadingChildren(
                accountName = "test@mega.co.nz",
                currentParentDocumentId = ROOT_DOCUMENT_ID,
                rootNodeDocumentId = ROOT_DOCUMENT_ID,
            )
            createProvider()

            val cursor: Cursor =
                underTest.queryChildDocuments(parentDocumentId = ROOT_DOCUMENT_ID, null, null)
            assertThat(cursor.extras?.getBoolean(DocumentsContract.EXTRA_LOADING)).isTrue()
            assertThat(cursor.count).isEqualTo(0)
        }

    @Test
    fun `test that queryChildDocuments returns empty cursor when NotLoggedIn`() = runTest {
        dataProviderState.value = CloudDriveDocumentProviderUiState.NotLoggedIn
        createProvider()

        val cursor: Cursor =
            underTest.queryChildDocuments(parentDocumentId = ROOT_DOCUMENT_ID, null, null)
        assertThat(cursor.count).isEqualTo(0)
    }

    @Test
    fun `test that queryChildDocuments returns child rows when state is ChildData and parentId matches`() =
        runTest {
            val childRow = CloudDriveDocumentRow(
                documentId = "$CLOUD_DRIVE_ROOT_ID:100",
                displayName = "Child Folder",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 1500L,
                flags = 0,
            )
            dataProviderState.value = CloudDriveDocumentProviderUiState.ChildData(
                accountName = "test@mega.co.nz",
                parentId = ROOT_DOCUMENT_ID,
                children = listOf(childRow),
                hasMore = false,
                rootNodeDocumentId = ROOT_DOCUMENT_ID,
            )
            createProvider()

            val cursor: Cursor =
                underTest.queryChildDocuments(parentDocumentId = ROOT_DOCUMENT_ID, null, null)

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
    fun `test that queryChildDocuments when ChildData parentId mismatch calls loadChildrenInBackground`() =
        runTest {
            dataProviderState.value = CloudDriveDocumentProviderUiState.ChildData(
                accountName = "test@mega.co.nz",
                parentId = ROOT_DOCUMENT_ID,
                children = emptyList(),
                hasMore = false,
                rootNodeDocumentId = ROOT_DOCUMENT_ID,
            )
            createProvider()

            underTest.queryChildDocuments(parentDocumentId = "$CLOUD_DRIVE_ROOT_ID:999", null, null)
            verify(mockDataProvider).loadChildrenInBackground("$CLOUD_DRIVE_ROOT_ID:999")
        }

    @Test
    fun `test that openDocument throws not yet implemented`() = runTest {
        setRootState()
        createProvider()

        val e = assertThrows<NotImplementedError> {
            underTest.openDocument("$CLOUD_DRIVE_ROOT_ID:123", "r", null)
        }
        assertThat(e).hasMessageThat().contains("Not yet implemented")
    }
}
