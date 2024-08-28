package mega.privacy.android.app.presentation.mapper.file

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.file.FolderInfoStringMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderInfoStringMapperTest {
    private lateinit var underTest: FolderInfoStringMapper

    private var context = mock<Context>()
    private val resources = mock<Resources>()

    @BeforeAll
    fun setUp() {
        whenever(context.resources).thenReturn(resources)
        underTest = FolderInfoStringMapper(
            context = context,
        )
    }

    @ParameterizedTest(name = "when folder number is {0}, file number is {1}")
    @MethodSource("provideFolderInfoStringMapperParameters")
    fun `test that mapper returns the correct formatted string`(numFolders: Int, numFiles: Int) {
        val emptyString = "Empty Folder"
        val folderString = "$numFolders folders"
        val fileString = "$numFiles files"
        val result = "$folderString · $fileString"

        whenever(context.getString(R.string.file_browser_empty_folder)).thenReturn(emptyString)
        initQuantityReturned(
            stringId = R.plurals.num_folders_with_parameter,
            quantity = numFolders,
            content = numFolders,
            returnValue = folderString
        )
        initQuantityReturned(
            stringId = R.plurals.num_files_with_parameter,
            quantity = numFiles,
            content = numFiles,
            returnValue = fileString
        )
        initQuantityReturned(
            stringId = R.plurals.num_folders_num_files,
            quantity = numFolders,
            content = numFolders,
            returnValue = "$folderString · "
        )
        initQuantityReturned(
            stringId = R.plurals.num_folders_num_files_2,
            quantity = numFiles,
            content = numFiles,
            returnValue = fileString
        )

        val expected = when {
            numFolders == 0 && numFiles == 0 -> emptyString
            numFolders > 0 && numFiles == 0 -> folderString
            numFolders == 0 && numFiles > 0 -> fileString
            else -> result
        }

        val actual = underTest(numFolders = numFolders, numFiles = numFiles)

        assertThat(actual).isEqualTo(expected)
    }

    private fun initQuantityReturned(
        @PluralsRes stringId: Int,
        quantity: Int,
        content: Int,
        returnValue: String,
    ) {
        whenever(resources.getQuantityString(stringId, quantity, content)).thenReturn(returnValue)
    }

    private fun provideFolderInfoStringMapperParameters() = Stream.of(
        Arguments.of(0, 0),
        Arguments.of(0, 10),
        Arguments.of(10, 0),
        Arguments.of(10, 10)
    )
}