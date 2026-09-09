package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NonContactDaoTest {
    private lateinit var nonContactDao: NonContactDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        nonContactDao = db.nonContactDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test_that_getNonContactByHandle_returns_null_when_the_handle_is_unknown`() = runTest {
        assertThat(nonContactDao.getNonContactByHandle(HANDLE)).isNull()
    }

    @Test
    fun `test_that_insertOrUpdateFirstName_inserts_a_new_row_when_the_handle_is_unknown`() =
        runTest {
            nonContactDao.insertOrUpdateFirstName(HANDLE, "Hai")

            val actual = nonContactDao.getNonContactByHandle(HANDLE)
            assertThat(actual?.handle).isEqualTo(HANDLE)
            assertThat(actual?.firstName).isEqualTo("Hai")
            assertThat(actual?.lastName).isNull()
            assertThat(actual?.email).isNull()
        }

    @Test
    fun `test_that_the_setters_update_a_single_row_when_the_handle_is_known`() = runTest {
        nonContactDao.insertOrUpdateFirstName(HANDLE, "Hai")
        nonContactDao.insertOrUpdateLastName(HANDLE, "Luong")
        nonContactDao.insertOrUpdateEmail(HANDLE, "lh@mega.co.nz")
        nonContactDao.insertOrUpdateFirstName(HANDLE, "Jayce")

        val actual = nonContactDao.getNonContactByHandle(HANDLE)
        assertThat(actual?.firstName).isEqualTo("Jayce")
        assertThat(actual?.lastName).isEqualTo("Luong")
        assertThat(actual?.email).isEqualTo("lh@mega.co.nz")
    }

    @Test
    fun `test_that_deleteAllNonContacts_removes_every_row`() = runTest {
        nonContactDao.insertOrUpdateFirstName(HANDLE, "Hai")
        nonContactDao.insertOrUpdateFirstName(OTHER_HANDLE, "Jayce")

        nonContactDao.deleteAllNonContacts()

        assertThat(nonContactDao.getNonContactByHandle(HANDLE)).isNull()
        assertThat(nonContactDao.getNonContactByHandle(OTHER_HANDLE)).isNull()
    }

    private companion object {
        const val HANDLE = 1234L
        const val OTHER_HANDLE = 5678L
    }
}
