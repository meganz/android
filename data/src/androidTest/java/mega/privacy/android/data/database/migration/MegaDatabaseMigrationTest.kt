package mega.privacy.android.data.database.migration

import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.spec.AutoMigrationSpec73to74
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MegaDatabaseMigrationTest {
    private val testDatabaseName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MegaDatabase::class.java,
        listOf<AutoMigrationSpec>(AutoMigrationSpec73to74()),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrate68To69() {
        helper.createDatabase(testDatabaseName, 68).apply {
            val CREATE_COMPLETED_TRANSFER_TABLE =
                "CREATE TABLE IF NOT EXISTS ${TABLE_COMPLETED_TRANSFERS}(" +
                        "${KEY_ID} INTEGER PRIMARY KEY, " +                      //0
                        "${KEY_TRANSFER_FILENAME} TEXT, " +                      //1
                        "${KEY_TRANSFER_TYPE} TEXT, " +                          //2
                        "${KEY_TRANSFER_STATE} TEXT, " +                         //3
                        "${KEY_TRANSFER_SIZE} TEXT, " +                          //4
                        "${KEY_TRANSFER_HANDLE} TEXT, " +                        //5
                        "${KEY_TRANSFER_PATH} TEXT, " +                          //6
                        "${KEY_TRANSFER_OFFLINE} BOOLEAN, " +                    //7
                        "${KEY_TRANSFER_TIMESTAMP} TEXT, " +                     //8
                        "${KEY_TRANSFER_ERROR} TEXT, " +                         //9
                        "${KEY_TRANSFER_ORIGINAL_PATH} TEXT, " +                 //10
                        "${KEY_TRANSFER_PARENT_HANDLE} TEXT)"                    //11
            execSQL(CREATE_COMPLETED_TRANSFER_TABLE) // refer onCreate LegacyDatabaseMigration
            execSQL(
                """
                INSERT INTO completedtransfers (
                    transferfilename,
                    transfertype,
                    transferstate,
                    transfersize,
                    transferhandle,
                    transferpath,
                    transferoffline,
                    transfertimestamp,
                    transfererror,
                    transferoriginalpath,
                    transferparenthandle
                ) VALUES (
                    'example.txt',
                    'Document',
                    'Completed',
                    '1024 KB',
                    'xyz123',
                    '/documents',
                    'false',
                    '2023-09-25',
                    NULL,
                    '/downloads/example.txt',
                    'parent_xyz'
                );
            """.trimIndent()
            )
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        val db =
            helper.runMigrationsAndValidate(testDatabaseName, 69, true, *MegaDatabase.MIGRATIONS)
        db.query("SELECT * FROM completedtransfers").use {
            it.moveToFirst()
            assert(it.count == 1)
            assert(it.getString(1) == "example.txt")
            assert(it.getString(2) == "Document")
            assert(it.getString(3) == "Completed")
            assert(it.getString(4) == "1024 KB")
            assert(it.getString(5) == "xyz123")
            assert(it.getString(6) == "/documents")
            assert(it.getString(7) == "false")
            assert(it.getString(8) == "2023-09-25")
            assert(it.getString(9) == null)
            assert(it.getString(10) == "/downloads/example.txt")
            assert(it.getString(11) == "parent_xyz")
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate69To70() {
        helper.createDatabase(testDatabaseName, 69).apply {
            close()
        }
        val db =
            helper.runMigrationsAndValidate(testDatabaseName, 70, true, *MegaDatabase.MIGRATIONS)
        try {
            db.query("SELECT * FROM megacontacts").use {
                it.moveToFirst()
                assert(it.count == 0)
            }
        } catch (e: Exception) {
            assert(e.message.orEmpty().contains("no such table: megacontacts"))
        }
    }

    companion object {
        const val TABLE_COMPLETED_TRANSFERS = "completedtransfers"
        const val KEY_TRANSFER_FILENAME = "transferfilename"
        const val KEY_TRANSFER_TYPE = "transfertype"
        const val KEY_TRANSFER_STATE = "transferstate"
        const val KEY_TRANSFER_SIZE = "transfersize"
        const val KEY_TRANSFER_HANDLE = "transferhandle"
        const val KEY_TRANSFER_PATH = "transferpath"
        const val KEY_TRANSFER_OFFLINE = "transferoffline"
        const val KEY_TRANSFER_TIMESTAMP = "transfertimestamp"
        const val KEY_TRANSFER_ERROR = "transfererror"
        const val KEY_TRANSFER_ORIGINAL_PATH = "transferoriginalpath"
        const val KEY_TRANSFER_PARENT_HANDLE = "transferparenthandle"
        const val KEY_ID = "id"
    }
}