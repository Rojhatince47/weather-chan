package io.github.rojhatince47.weatherchan.core.network.di

import io.github.rojhatince47.weatherchan.core.network.GeocodingRetrofit
import io.github.rojhatince47.weatherchan.core.network.OpenMeteoRetrofit
import io.github.rojhatince47.weatherchan.core.network.WttrInRetrofit
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoGeocodingApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    @Provides
    @Singleton
    @OpenMeteoRetrofit
    fun provideOpenMeteoRetrofit(client: OkHttpClient): Retrofit = retrofit(client, "https://api.open-meteo.com/")

    @Provides
    @Singleton
    @WttrInRetrofit
    fun provideWttrInRetrofit(client: OkHttpClient): Retrofit = retrofit(client, "https://wttr.in/")

    @Provides
    @Singleton
    fun provideOpenMeteoApi(@OpenMeteoRetrofit retrofit: Retrofit): OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)

    @Provides
    @Singleton
    @GeocodingRetrofit
    fun provideGeocodingRetrofit(client: OkHttpClient): Retrofit = retrofit(client, "https://geocoding-api.open-meteo.com/")

    @Provides
    @Singleton
    fun provideOpenMeteoGeocodingApi(@GeocodingRetrofit retrofit: Retrofit): OpenMeteoGeocodingApi =
        retrofit.create(OpenMeteoGeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideWttrInApi(@WttrInRetrofit retrofit: Retrofit): WttrInApi = retrofit.create(WttrInApi::class.java)

    private fun retrofit(client: OkHttpClient, baseUrl: String): Retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
