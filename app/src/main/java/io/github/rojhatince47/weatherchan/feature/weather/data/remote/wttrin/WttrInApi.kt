package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import retrofit2.http.GET
import retrofit2.http.Path

interface WttrInApi {

    @GET("{location}?format=j1")
    suspend fun weather(@Path("location") location: String): WttrInDto
}
