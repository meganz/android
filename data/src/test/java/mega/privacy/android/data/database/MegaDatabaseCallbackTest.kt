package mega.privacy.android.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaDatabaseCallbackTest {
    private lateinit var underTest: MegaDatabaseCallback

    private val delegate = mock<SupportSQLiteOpenHelper.Callback>()
    private val legacyDatabaseMigration = mock<LegacyDatabaseMigration>()
    private val db = mock<SupportSQLiteDatabase>()

    @BeforeEach
    fun setUp() {
        reset(delegate, legacyDatabaseMigration, db)
        underTest = MegaDatabaseCallback(delegate, legacyDatabaseMigration)
    }

    @Test
    fun `test that onOpen delegates without repairing when Room verifies the database`() {
        underTest.onOpen(db)

        verify(delegate).onOpen(db)
        verify(delegate, never()).onCreate(any())
        verify(db, never()).beginTransaction()
    }

    @Test
    fun `test that onOpen rethrows when the delegate fails with an exception that is not a schema error`() {
        doThrow(RuntimeException("boom")).whenever(delegate).onOpen(db)

        assertThrows<RuntimeException> { underTest.onOpen(db) }

        verify(delegate, never()).onCreate(any())
        verify(db, never()).beginTransaction()
    }

    @Test
    fun `test that onOpen restores the identity through onCreate and reopens when Room cannot verify the database`() {
        doThrow(IllegalStateException(MISSING_IDENTITY_MESSAGE)).doNothing()
            .whenever(delegate).onOpen(db)

        underTest.onOpen(db)

        inOrder(db, delegate) {
            verify(delegate).onOpen(db)
            verify(db).beginTransaction()
            verify(delegate).onCreate(db)
            verify(db).setTransactionSuccessful()
            verify(db).endTransaction()
            verify(delegate).onOpen(db)
        }
        verify(delegate, never()).onUpgrade(any(), any(), any())
        verify(db, never()).execSQL(any())
    }

    @Test
    fun `test that onOpen rolls back and rethrows the original error when the schema is still invalid`() {
        val original = IllegalStateException(MISSING_IDENTITY_MESSAGE)
        doThrow(original).whenever(delegate).onOpen(db)
        doThrow(IllegalStateException("Pre-packaged database has an invalid schema"))
            .whenever(delegate).onCreate(db)

        val error = assertThrows<IllegalStateException> { underTest.onOpen(db) }

        assertThat(error).isSameInstanceAs(original)
        verify(db).endTransaction()
        verify(db, never()).setTransactionSuccessful()
        verify(delegate, times(1)).onOpen(db)
    }

    @Test
    fun `test that onOpen rolls back and rethrows when restoring the identity fails with another error`() {
        doThrow(IllegalStateException(MISSING_IDENTITY_MESSAGE)).whenever(delegate).onOpen(db)
        doThrow(RuntimeException("disk full")).whenever(delegate).onCreate(db)

        val error = assertThrows<RuntimeException> { underTest.onOpen(db) }

        assertThat(error).hasMessageThat().isEqualTo("disk full")
        verify(db).endTransaction()
        verify(db, never()).setTransactionSuccessful()
        verify(delegate, times(1)).onOpen(db)
    }

    @Test
    fun `test that onOpen rethrows when the database still fails verification after restoring the identity`() {
        doThrow(IllegalStateException(MISSING_IDENTITY_MESSAGE)).whenever(delegate).onOpen(db)

        assertThrows<IllegalStateException> { underTest.onOpen(db) }

        verify(delegate, times(2)).onOpen(db)
        verify(delegate, times(1)).onCreate(db)
    }

    private companion object {
        const val MISSING_IDENTITY_MESSAGE =
            "Room cannot verify the data integrity. Looks like you've changed schema but " +
                    "forgot to update the version number. You can simply fix this by increasing " +
                    "the version number. Expected identity hash: 1edc1a99c1b94ea59d517349bc22931e, " +
                    "found: null"
    }
}
