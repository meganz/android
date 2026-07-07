package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.data.database.dao.FolderPreferenceDao
import mega.privacy.android.data.mapper.FolderPreferenceEntityMapper
import mega.privacy.android.data.mapper.FolderPreferenceMapper
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.repository.FolderPreferenceRepository
import javax.inject.Inject

/**
 * Implementation of [FolderPreferenceRepository] backed by the folder_preference Room table.
 */
internal class FolderPreferenceRepositoryImpl @Inject constructor(
    private val folderPreferenceDao: FolderPreferenceDao,
    private val folderPreferenceMapper: FolderPreferenceMapper,
    private val folderPreferenceEntityMapper: FolderPreferenceEntityMapper,
) : FolderPreferenceRepository {

    override fun monitorFolderPreference(folderKey: String): Flow<FolderPreference?> =
        folderPreferenceDao.monitorByFolderKey(folderKey)
            .map { entity -> entity?.let(folderPreferenceMapper::invoke) }

    override suspend fun setFolderPreference(preference: FolderPreference) =
        folderPreferenceDao.insertOrUpdate(folderPreferenceEntityMapper(preference))

    override suspend fun clearFolderPreferences() =
        folderPreferenceDao.deleteAll()
}
