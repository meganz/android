package mega.privacy.android.data.gateway.api

import mega.privacy.android.data.model.giphy.GiphyResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API of the MEGA Giphy proxy.
 */
internal interface GiphyApi {

    @GET("v1/gifs/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): GiphyResponse?

    @GET("v1/gifs/trending")
    suspend fun getTrending(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): GiphyResponse?
}
