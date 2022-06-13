package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.model.PreferenceResource

@Module
@InstallIn(SingletonComponent::class)
class QAModule {
    @Provides
    @IntoSet
    fun bindPreferenceResource(): PreferenceResource =
        PreferenceResource(R.xml.preferences_qa_entry)

}