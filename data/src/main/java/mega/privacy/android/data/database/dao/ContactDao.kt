package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.ContactEntity

@Dao
internal interface ContactDao {
    @Query("SELECT * FROM contacts")
    fun getAllContact(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(entity: ContactEntity)

    @Query("SELECT * FROM contacts WHERE mail = :email")
    suspend fun getContactByEmail(email: String?): ContactEntity?

    @Query("SELECT * FROM contacts WHERE handle = :handle")
    suspend fun getContactByHandle(handle: String?): ContactEntity?

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContact()

    @Query("SELECT COUNT(id) FROM contacts")
    suspend fun getContactCount(): Int
}