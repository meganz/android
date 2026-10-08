package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsSyncTransferSearchMatchUseCaseTest {

    private lateinit var underTest: IsSyncTransferSearchMatchUseCase

    @BeforeAll
    fun setUp() {
        underTest = IsSyncTransferSearchMatchUseCase()
    }

    @ParameterizedTest(name = "{0} in {1} matches {2}")
    @MethodSource("textCases")
    fun `test that invoke matches the text anywhere in the file name ignoring case`(
        text: String,
        fileName: String,
        expected: Boolean,
    ) {
        val actual = underTest(entry(fileName), SyncTransferFilter.Search(text))

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} search, {1} entry, any direction {2} matches {3}")
    @MethodSource("directionCases")
    fun `test that invoke narrows the match to the search direction`(
        searchDirection: SyncTransferDirection?,
        entryDirection: SyncTransferDirection,
        anyDirection: Boolean,
        expected: Boolean,
    ) {
        val actual = underTest(
            entry("report.pdf", entryDirection),
            SyncTransferFilter.Search("report", searchDirection),
            anyDirection = anyDirection,
        )

        assertThat(actual).isEqualTo(expected)
    }

    private fun textCases(): Stream<Arguments> = Stream.of(
        Arguments.of("rePort", "Report.PDF", true),
        Arguments.of("REPORT", "report.pdf", true),
        Arguments.of("2024", "report-2024-final.pdf", true),
        Arguments.of("pdf", "report.pdf", true),
        Arguments.of("notes", "report.pdf", false),
        Arguments.of("report ", "report.pdf", false),
    )

    private fun directionCases(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncTransferDirection.Download, SyncTransferDirection.Download, false, true),
        Arguments.of(SyncTransferDirection.Download, SyncTransferDirection.Upload, false, false),
        Arguments.of(SyncTransferDirection.Upload, SyncTransferDirection.Download, false, false),
        Arguments.of(SyncTransferDirection.Download, SyncTransferDirection.Upload, true, true),
        Arguments.of(SyncTransferDirection.Upload, SyncTransferDirection.Download, true, true),
        Arguments.of(null, SyncTransferDirection.Upload, false, true),
        Arguments.of(null, SyncTransferDirection.Download, false, true),
    )

    private fun entry(
        fileName: String,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
    ) = SyncTransferEntry(
        tag = 1,
        direction = direction,
        state = TransferState.STATE_QUEUED,
        fileName = fileName,
        totalBytes = 100,
        transferredBytes = 0,
        speed = 0,
        meanSpeed = 0,
        priority = 1,
        notificationNumber = 1,
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )
}
