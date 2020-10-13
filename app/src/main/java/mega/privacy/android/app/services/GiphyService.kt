package mega.privacy.android.app.services

import mega.privacy.android.app.interfaces.GiphyEndPointsInterface
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GiphyService {
    const val TEST_URL = "https://giphy-sandbox3.developers.mega.co.nz/"
    const val BASE_URL = "https://giphy.mega.nz/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(TEST_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun buildService(): GiphyEndPointsInterface {
        return retrofit.create(GiphyEndPointsInterface::class.java)
    }
}