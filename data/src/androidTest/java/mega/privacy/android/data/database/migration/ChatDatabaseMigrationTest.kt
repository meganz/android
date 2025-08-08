package mega.privacy.android.data.database.migration

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.database.chat.ChatDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ChatDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ChatDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val testDbName = "chat_db_migration_test"

    @Test
    fun migrate6To7_addsNodeLabelColumn_asNullableText() {
        // Create database at version 6 using the exported schema and close it.
        helper.createDatabase(testDbName, 6).apply {
            // No data required for this migration test
            close()
        }

        // Open the database with the latest schema (7). This will trigger auto-migration.
        val db = Room.databaseBuilder(context, ChatDatabase::class.java, testDbName)
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .build()

        // Validate the table schema contains the new column with correct type and nullability.
        val cursor = db.openHelper.readableDatabase.query("PRAGMA table_info('chat_node')")
        var found = false
        var type: String? = null
        var notNull: Int? = null
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            val typeIndex = it.getColumnIndex("type")
            val notNullIndex = it.getColumnIndex("notnull")
            while (it.moveToNext()) {
                val columnName = it.getString(nameIndex)
                if (columnName == "nodeLabel") {
                    found = true
                    type = it.getString(typeIndex)
                    notNull = it.getInt(notNullIndex)
                    break
                }
            }
        }

        assertThat(found).isTrue()
        assertThat(type).isEqualTo("TEXT")
        assertThat(notNull).isEqualTo(0)

        db.close()
    }
}