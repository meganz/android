package mega.privacy.android.app.presentation.settings

import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.usecase.GetPreference
import mega.privacy.android.domain.usecase.PutPreference

/**
 * Data store wrapper class for the settings view model
 *
 * @property viewModel
 */
class ViewModelPreferenceDataStore @AssistedInject constructor(
    @Assisted private val viewModel: SettingsViewModel,
    private val putStringPreference: PutPreference<String>,
    private val putStringSetPreference: PutPreference<MutableSet<String>>,
    private val putIntPreference: PutPreference<Int>,
    private val putLongPreference: PutPreference<Long>,
    private val putFloatPreference: PutPreference<Float>,
    private val putBooleanPreference: PutPreference<Boolean>,
    private val getStringPreference: GetPreference<String?>,
    private val getStringSetPreference: GetPreference<MutableSet<String>?>,
    private val getIntPreference: GetPreference<Int>,
    private val getLongPreference: GetPreference<Long>,
    private val getFloatPreference: GetPreference<Float>,
    private val getBooleanPreference: GetPreference<Boolean>,
) : PreferenceDataStore() {

    override fun putString(key: String?, value: String?) {
        viewModel.viewModelScope.launch { putStringPreference(key, value) }
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?) {
        viewModel.viewModelScope.launch { putStringSetPreference(key, values) }
    }

    override fun putInt(key: String?, value: Int) {
        viewModel.viewModelScope.launch { putIntPreference(key, value) }
    }

    override fun putLong(key: String?, value: Long) {
        viewModel.viewModelScope.launch { putLongPreference(key, value) }
    }

    override fun putFloat(key: String?, value: Float) {
        viewModel.viewModelScope.launch { putFloatPreference(key, value) }
    }

    override fun putBoolean(key: String?, value: Boolean) {
        viewModel.viewModelScope.launch { putBooleanPreference(key, value) }
    }

    override fun getString(key: String?, defValue: String?) =
        runBlocking { getStringPreference(key, defValue).firstOrNull() }

    override fun getStringSet(key: String?, defValue: MutableSet<String>?) =
        runBlocking { getStringSetPreference(key, defValue).firstOrNull() }

    override fun getInt(key: String?, defValue: Int) =
        runBlocking { getIntPreference(key, defValue).first() }

    override fun getLong(key: String?, defValue: Long) =
        runBlocking { getLongPreference(key, defValue).first() }

    override fun getFloat(key: String?, defValue: Float) =
        runBlocking { getFloatPreference(key, defValue).first() }

    override fun getBoolean(key: String?, defValue: Boolean) =
        runBlocking { getBooleanPreference(key, defValue).first() }
}