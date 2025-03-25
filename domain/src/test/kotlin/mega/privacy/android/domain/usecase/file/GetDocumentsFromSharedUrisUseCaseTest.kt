package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.transfers.uploads.IsMalformedPathFromExternalAppUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDocumentsFromSharedUrisUseCaseTest {

    private lateinit var underTest: GetDocumentsFromSharedUrisUseCase

    private val filePrepareUseCase = mock<FilePrepareUseCase>()
    private val isMalformedPathFromExternalAppUseCase =
        mock<IsMalformedPathFromExternalAppUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = GetDocumentsFromSharedUrisUseCase(
            filePrepareUseCase,
            isMalformedPathFromExternalAppUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            filePrepareUseCase,
            isMalformedPathFromExternalAppUseCase,
        )
    }

    @Test
    fun `test that filePrepareUseCase result is returned when isMalformedPathFromExternalAppUseCase returns false`() =
        runTest {
            val uriPaths = (0..10).map { UriPath("content:uri.file$it.txt") }
            val action = "action"
            val expected = mock<List<DocumentEntity>>()
            whenever(isMalformedPathFromExternalAppUseCase(any(), any())) doReturn false
            whenever(filePrepareUseCase(uriPaths)) doReturn expected

            val actual = underTest.invoke(action, uriPaths)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that empty list is returned when isMalformedPathFromExternalAppUseCase returns true`() =
        runTest {
            val uriPaths = (0..10).map { UriPath("content:uri.file$it.txt") }
            val action = "action"
            whenever(isMalformedPathFromExternalAppUseCase(any(), any())) doReturn true
            whenever(filePrepareUseCase(emptyList())) doReturn emptyList()

            val actual = underTest.invoke(action, uriPaths)

            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that uris are filtered out when isMalformedPathFromExternalAppUseCase returns true`() =
        runTest {
            val uriPaths = (0..10).map { UriPath("content:uri.file$it.txt") }
            val action = "action"
            val expected = mock<List<DocumentEntity>>()
            val filteredIn = uriPaths.filterIndexed { index, _ ->
                index.mod(2) == 0
            }
            uriPaths.forEach {
                whenever(
                    isMalformedPathFromExternalAppUseCase("action", it.value)
                ) doReturn !filteredIn.contains(it)
            }
            whenever(filePrepareUseCase(eq(filteredIn))) doReturn expected

            val actual = underTest.invoke(action, uriPaths)

            assertThat(actual).isEqualTo(expected)
        }
}