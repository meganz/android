package mega.privacy.android.data.filewrapper

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FileWrapperTest {

    @Test
    fun `test isPath returns false for uri path`() {
        val uriPath = "file:///path/to/file"
        assertThat(FileWrapper.isPath(uriPath)).isFalse()
    }

    @Test
    fun `test isPath returns true for file path`() {
        Mockito.mockStatic(Uri::class.java).use {
            val filePath = "/path/to/file"
            val uri = mock<Uri> {
                on { it.scheme } doReturn null
            }
            val fileUri = mock<Uri> {
                on { it.scheme } doReturn "file"
            }
            whenever(Uri.parse(filePath)) doReturn uri
            whenever(Uri.fromFile(any())) doReturn fileUri
            assertThat(FileWrapper.isPath(filePath)).isTrue()
        }
    }

    @Test
    fun `test isPath returns false for content uri`() {
        Mockito.mockStatic(Uri::class.java).use {
            val contentUri = "content://path/to/file"
            val uri = mock<Uri> {
                on { it.scheme } doReturn "content"
            }
            whenever(Uri.parse(contentUri)) doReturn uri
            assertThat(FileWrapper.isPath(contentUri)).isFalse()
        }
    }

    @Test
    fun `test that getChildrenWithMetadata returns result from function`() {
        val expected = listOf(
            ChildMetadata(
                uri = "content://child1",
                name = "file1.txt",
                isFolder = false,
                size = 100L,
                lastModified = 200L,
                path = "/storage/emulated/0/file1.txt"
            )
        )
        val wrapper = createFileWrapper(
            getChildrenWithMetadataFunction = { expected }
        )

        val actual = wrapper.getChildrenWithMetadata()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that getChildrenWithMetadata returns null when function throws`() {
        val wrapper = createFileWrapper(
            getChildrenWithMetadataFunction = { throw RuntimeException("test error") }
        )

        val actual = wrapper.getChildrenWithMetadata()

        assertThat(actual).isNull()
    }

    @Test
    fun `test that getChildrenWithMetadata returns empty list from function`() {
        val wrapper = createFileWrapper(
            getChildrenWithMetadataFunction = { emptyList() }
        )

        val actual = wrapper.getChildrenWithMetadata()

        assertThat(actual).isEmpty()
    }

    @Test
    fun `test that getChildrenWithMetadata propagates null from function`() {
        // Empty list and null are different signals: empty means "directory has zero
        // children", null means "lookup failed". The wrapper must preserve null so the
        // C++ JNI side can return SCAN_INACCESSIBLE — coercing to emptyList() would
        // tell the sync engine to delete every cached child of this directory.
        val wrapper = createFileWrapper(
            getChildrenWithMetadataFunction = { null }
        )

        val actual = wrapper.getChildrenWithMetadata()

        assertThat(actual).isNull()
    }

    private fun createFileWrapper(
        getChildrenWithMetadataFunction: () -> List<ChildMetadata>? = { emptyList() },
    ) = FileWrapper(
        uri = "content://test",
        name = "test",
        isFolder = true,
        getChildrenUrisFunction = { emptyList() },
        getDetachedFileDescriptorFunction = { null },
        childFileExistsFunction = { false },
        getChildByNameFunction = { null },
        createChildFileFunction = { _, _ -> null },
        getPathFunction = { null },
        deleteFileFunction = { false },
        deleteFolderIfEmptyFunction = { false },
        setModificationTimeFunction = { false },
        renameFunction = { null },
        renameOverwriteFunction = { _, _, _ -> null },
        moveDocumentFunction = { _, _ -> null },
        createNestedPathFunction = { _, _, _ -> null },
        getChildrenWithMetadataFunction = getChildrenWithMetadataFunction,
    )
}
