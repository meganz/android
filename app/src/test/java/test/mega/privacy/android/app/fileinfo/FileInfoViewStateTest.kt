package test.mega.privacy.android.app.fileinfo

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.fileinfo.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.FileInfoViewState.Companion.MAX_NUMBER_OF_CONTACTS_IN_LIST
import nz.mega.sdk.MegaShare
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class FileInfoViewStateTest {


    private lateinit var underTest: FileInfoViewState

    @Test
    fun `test outSharesCoerceMax is returning all outShares if maximum is not surpassed`() {
        val outShares = List<MegaShare>(MAX_NUMBER_OF_CONTACTS_IN_LIST) { mock() }
        underTest = FileInfoViewState(outShares = outShares)
        Truth.assertThat(underTest.outSharesCoerceMax.size)
            .isEqualTo(MAX_NUMBER_OF_CONTACTS_IN_LIST)
    }

    @Test
    fun `test outSharesCoerceMax is returning no more than maximum out shares`() {
        val outShares = List<MegaShare>(MAX_NUMBER_OF_CONTACTS_IN_LIST + 1) { mock() }
        underTest = FileInfoViewState(outShares = outShares)
        Truth.assertThat(underTest.outSharesCoerceMax.size)
            .isEqualTo(MAX_NUMBER_OF_CONTACTS_IN_LIST)
    }
}