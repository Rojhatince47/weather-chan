package io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

internal interface OverpassApi {
    /** [data] Overpass QL sorgusu; [url] mirror depo katmanında seçilir. */
    @GET
    suspend fun query(@Url url: String, @Query("data") data: String): OverpassResponseDto
}
