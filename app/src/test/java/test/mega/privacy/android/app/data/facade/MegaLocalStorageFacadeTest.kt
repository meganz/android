package test.mega.privacy.android.app.data.facade

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.facade.MegaLocalStorageFacade
import mega.privacy.android.domain.entity.SortOrder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MegaLocalStorageFacadeTest {

    private lateinit var underTest: MegaLocalStorageFacade
    private val dbHandler = mock<DatabaseHandler>()

    @Before
    fun setUp() {
        underTest = MegaLocalStorageFacade(dbHandler)
        whenever(dbHandler.preferences).thenReturn(mock())
    }

    @Test
    fun test_getCloudSortOrder_ReturnsDefaultValue_WhenPreferencesIsNotSet() =
        runTest {
            val expected = SortOrder.ORDER_DEFAULT_ASC.value
            whenever(dbHandler.preferences?.preferredSortCloud).thenReturn(null)
            assertThat(underTest.getCloudSortOrder()).isEqualTo(expected)
        }

    @Test
    fun test_getCloudSortOrder_ReturnsValueInPreferences_WhenPreferencesIsSet() =
        runTest {
            val expected = SortOrder.ORDER_SIZE_DESC.value
            whenever(dbHandler.preferences?.preferredSortCloud).thenReturn(expected.toString())
            assertThat(underTest.getCloudSortOrder()).isEqualTo(expected)
        }

    @Test
    fun test_getCameraSortOrder_ReturnsDefaultValue_WhenPreferencesIsNotSet() =
        runTest {
            val expected = SortOrder.ORDER_MODIFICATION_DESC.value
            whenever(dbHandler.preferences?.preferredSortCameraUpload).thenReturn(null)
            assertThat(underTest.getCameraSortOrder()).isEqualTo(expected)
        }

    @Test
    fun test_getCameraSortOrder_ReturnsValueInPreferences_WhenPreferencesIsSet() =
        runTest {
            val expected = SortOrder.ORDER_ALPHABETICAL_ASC.value
            whenever(dbHandler.preferences?.preferredSortCameraUpload).thenReturn(expected.toString())
            assertThat(underTest.getCameraSortOrder()).isEqualTo(expected)
        }


    @Test
    fun test_getOthersSortOrder_ReturnsDefaultValue_WhenPreferencesIsNotSet() =
        runTest {
            val expected = SortOrder.ORDER_DEFAULT_ASC.value
            whenever(dbHandler.preferences?.preferredSortOthers).thenReturn(null)
            assertThat(underTest.getOthersSortOrder()).isEqualTo(expected)
        }

    @Test
    fun test_getOthersSortOrder_ReturnsValueInPreferences_WhenPreferencesIsSet() =
        runTest {
            val expected = SortOrder.ORDER_ALPHABETICAL_DESC.value
            whenever(dbHandler.preferences?.preferredSortOthers).thenReturn(expected.toString())
            assertThat(underTest.getOthersSortOrder()).isEqualTo(expected)
        }


    @Test
    fun test_getLinksSortOrder_ReturnsDefaultASCValue_WhenCloudSortOrderIsOrderModificationASC() =
        runTest {
            whenever(dbHandler.preferences?.preferredSortCloud).thenReturn(SortOrder.ORDER_MODIFICATION_ASC.value.toString())
            val expected = SortOrder.ORDER_LINK_CREATION_ASC.value
            assertThat(underTest.getLinksSortOrder()).isEqualTo(expected)
        }

    @Test
    fun test_getLinksSortOrder_ReturnsDefaultDESCValue_WhenCloudSortOrderIsOrderModificationDESC() =
        runTest {
            whenever(dbHandler.preferences?.preferredSortCloud).thenReturn(SortOrder.ORDER_MODIFICATION_DESC.value.toString())
            val expected = SortOrder.ORDER_LINK_CREATION_DESC.value
            assertThat(underTest.getLinksSortOrder()).isEqualTo(expected)
        }

    @Test
    fun test_getLinksSortOrder_ReturnsCloudSortOrder_WhenCloudSortOrderIsNotOrderModification() =
        runTest {
            val expected = SortOrder.ORDER_DEFAULT_ASC.value
            whenever(dbHandler.preferences?.preferredSortCloud).thenReturn(expected.toString())
            assertThat(underTest.getLinksSortOrder()).isEqualTo(expected)
        }
}