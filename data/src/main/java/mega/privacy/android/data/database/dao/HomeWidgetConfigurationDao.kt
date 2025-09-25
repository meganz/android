package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.database.entity.HomeWidgetConfigurationEntity

@Dao
internal interface HomeWidgetConfigurationDao {

    /**
     * Get all widget configurations ordered by widget order
     */
    @Query("SELECT * FROM home_widget_configuration")
    fun monitorAllWidgetConfigurations(): Flow<List<HomeWidgetConfigurationEntity>>

    /**
     * Get all widget configurations ordered by widget order
     */
    @Query("SELECT * FROM home_widget_configuration")
    suspend fun getAllWidgetConfigurations(): List<HomeWidgetConfigurationEntity>

    /**
     * Get widget configuration by identifier
     */
    @Query("SELECT * FROM home_widget_configuration WHERE widget_identifier = :widgetIdentifier")
    fun monitorWidgetConfigurationByIdentifier(widgetIdentifier: String): Flow<HomeWidgetConfigurationEntity>

    /**
     * Insert or update a single widget configuration
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWidgetConfiguration(entity: HomeWidgetConfigurationEntity)

    /**
     * Insert or update multiple widget configurations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWidgetConfigurations(entities: List<HomeWidgetConfigurationEntity>)

    /**
     * Get count of widget configurations
     */
    @Query("SELECT COUNT(*) FROM home_widget_configuration")
    suspend fun getWidgetConfigurationCount(): Int

    /**
     * Delete widget configuration by identifier
     */
    @Query("DELETE FROM home_widget_configuration WHERE widget_identifier = :widgetIdentifier")
    fun deleteWidgetConfigurationById(widgetIdentifier: String)
}
