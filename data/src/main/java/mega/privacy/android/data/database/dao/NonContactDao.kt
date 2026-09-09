package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import mega.privacy.android.data.database.entity.NonContactEntity

@Dao
internal interface NonContactDao {
    @Query("SELECT * FROM non_contacts WHERE handle = :handle")
    suspend fun getNonContactByHandle(handle: Long): NonContactEntity?

    @Query(
        "INSERT INTO non_contacts (handle, first_name) VALUES (:handle, :firstName) " +
                "ON CONFLICT(handle) DO UPDATE SET first_name = :firstName"
    )
    suspend fun insertOrUpdateFirstName(handle: Long, firstName: String?)

    @Query(
        "INSERT INTO non_contacts (handle, last_name) VALUES (:handle, :lastName) " +
                "ON CONFLICT(handle) DO UPDATE SET last_name = :lastName"
    )
    suspend fun insertOrUpdateLastName(handle: Long, lastName: String?)

    @Query(
        "INSERT INTO non_contacts (handle, email) VALUES (:handle, :email) " +
                "ON CONFLICT(handle) DO UPDATE SET email = :email"
    )
    suspend fun insertOrUpdateEmail(handle: Long, email: String?)

    @Query("DELETE FROM non_contacts")
    suspend fun deleteAllNonContacts()
}
