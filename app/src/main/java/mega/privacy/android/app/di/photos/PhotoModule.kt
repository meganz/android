package mega.privacy.android.app.di.photos

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.photos.widget.PhotoHomeWidgetProvider
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider

@Module
@InstallIn(SingletonComponent::class)
class PhotoModule {

    @Provides
    @IntoSet
    fun providePhotoWidgetProvider(widgetProvider: PhotoHomeWidgetProvider): HomeWidgetProvider =
        widgetProvider
}