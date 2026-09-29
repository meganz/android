package mega.privacy.android.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.gateway.api.GiphyApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideGiphyApi(): GiphyApi = Retrofit.Builder()
        .baseUrl(GIPHY_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GiphyApi::class.java)

    private const val GIPHY_BASE_URL = "https://giphy.mega.nz/"
}
