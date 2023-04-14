package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.ContactEntity

@Dao
internal interface ContactDao {
    @Query("SELECT * FROM contacts")
    fun getAll(): Flow<List<ContactEntity>>

    @Insert
    suspend fun add(entity: ContactEntity)

    @Query("SELECT * FROM contacts WHERE mail = :email")
    suspend fun getByEmail(email: String?): ContactEntity?

    @Query("SELECT * FROM contacts WHERE handle = :handle")
    suspend fun getByHandle(handle: String?): ContactEntity?

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()
}