package mega.privacy.android.app.interfaces

import mega.privacy.android.app.objects.GiphyResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GiphyEndPointsInterface {
    @GET("v1/gifs/search?")
    fun getGiphySearch(@Query("q") q: String? = null,
                       @Query("limit") limit: Int? = null,
                       @Query("offset") offset: Int? = null,
                       @Query("rating") rating: String? = null,
                       @Query("lang") lang: String? = null,
                       @Query("random_id") random_id: String? = null): Call<GiphyResponse>

    @GET("v1/gifs/trending")
    fun getGiphyTrending(@Query("limit") limit: Int? = null,
                         @Query("offset") offset: Int? = null,
                         @Query("rating") rating: String? = null,
                         @Query("random_id") random_id: String? = null): Call<GiphyResponse>
}