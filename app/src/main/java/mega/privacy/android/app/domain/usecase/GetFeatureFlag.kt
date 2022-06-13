package mega.privacy.android.app.domain.usecase

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface GetFeatureFlag {

    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(key: String): Flow<Boolean>
}