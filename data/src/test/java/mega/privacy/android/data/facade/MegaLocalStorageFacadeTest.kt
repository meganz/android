package mega.privacy.android.data.facade

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.model.chat.NonContactInfo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaLocalStorageFacadeTest {

    private lateinit var underTest: MegaLocalStorageFacade

    private val dbHandler = mock<DatabaseHandler>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()

    @BeforeAll
    fun setUp() {
        underTest = MegaLocalStorageFacade(
            dbHandler = { dbHandler },
            megaLocalRoomGateway = megaLocalRoomGateway,
            databaseDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(dbHandler, megaLocalRoomGateway)
    }

    @Test
    fun `test that getNonContactByHandle returns the value from the room gateway`() = runTest {
        val expected = mock<NonContactInfo>()
        whenever(megaLocalRoomGateway.getNonContactByHandle(HANDLE)).thenReturn(expected)

        assertThat(underTest.getNonContactByHandle(HANDLE)).isEqualTo(expected)
        verifyNoInteractions(dbHandler)
    }

    @Test
    fun `test that getNonContactByHandle returns null when the room gateway has no entry`() =
        runTest {
            whenever(megaLocalRoomGateway.getNonContactByHandle(HANDLE)).thenReturn(null)

            assertThat(underTest.getNonContactByHandle(HANDLE)).isNull()
        }

    @Test
    fun `test that setNonContactFirstName delegates to the room gateway`() = runTest {
        underTest.setNonContactFirstName(HANDLE, FIRST_NAME)

        verify(megaLocalRoomGateway).setNonContactFirstName(HANDLE, FIRST_NAME)
        verifyNoInteractions(dbHandler)
    }

    @Test
    fun `test that setNonContactLastName delegates to the room gateway`() = runTest {
        underTest.setNonContactLastName(HANDLE, LAST_NAME)

        verify(megaLocalRoomGateway).setNonContactLastName(HANDLE, LAST_NAME)
        verifyNoInteractions(dbHandler)
    }

    @Test
    fun `test that setNonContactEmail delegates to the room gateway`() = runTest {
        underTest.setNonContactEmail(HANDLE, EMAIL)

        verify(megaLocalRoomGateway).setNonContactEmail(HANDLE, EMAIL)
        verifyNoInteractions(dbHandler)
    }

    @Test
    fun `test that clearNonContacts deletes every row through the room gateway`() = runTest {
        underTest.clearNonContacts()

        verify(megaLocalRoomGateway).deleteAllNonContacts()
        verifyNoInteractions(dbHandler)
    }

    private companion object {
        const val HANDLE = 1234L
        const val FIRST_NAME = "Hai"
        const val LAST_NAME = "Luong"
        const val EMAIL = "lh@mega.co.nz"
    }
}
