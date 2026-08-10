package mega.privacy.android.app.di.photos

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class PhotoModule {

    /**
     * Uncomment to enable multi-widget sample in home screen
     */
//    @Provides
//    @IntoSet
//    fun providePhotoWidgetProvider(widgetProvider: PhotoHomeWidgetProvider): HomeWidgetProvider =
//        widgetProvider
}