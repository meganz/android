package mega.privacy.android.app.presentation.settings

import androidx.preference.PreferenceDataStore

/**
 * Data store wrapper class for the settings view model
 *
 * @property viewModel
 */
class ViewModelPreferenceDataStore(
    private val viewModel: SettingsViewModel,
) : PreferenceDataStore() {

    override fun putString(key: String?, value: String?) = viewModel.putString(key, value)

    override fun putStringSet(key: String?, values: MutableSet<String>?) =
        viewModel.putStringSet(key, values)

    override fun putInt(key: String?, value: Int) = viewModel.putInt(key, value)

    override fun putLong(key: String?, value: Long) = viewModel.putLong(key, value)

    override fun putFloat(key: String?, value: Float) = viewModel.putFloat(key, value)

    override fun putBoolean(key: String?, value: Boolean) = viewModel.putBoolean(key, value)

    override fun getString(key: String?, defValue: String?): String? =
        viewModel.getString(key, defValue)

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        viewModel.getStringSet(key, defValues)

    override fun getInt(key: String?, defValue: Int) = viewModel.getInt(key, defValue)

    override fun getLong(key: String?, defValue: Long) = viewModel.getLong(key, defValue)

    override fun getFloat(key: String?, defValue: Float) = viewModel.getFloat(key, defValue)

    override fun getBoolean(key: String?, defValue: Boolean) =
        viewModel.getBoolean(key, defValue)
}