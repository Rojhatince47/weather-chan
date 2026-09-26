package io.github.rojhatince47.weatherchan.core.network

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class NetworkErrorMapper @Inject constructor() : ErrorMapper {
    override fun map(throwable: Throwable): AppError = when (throwable) {
        is IOException -> AppError.Network
        is HttpException -> AppError.Server(throwable.code())
        is SerializationException -> AppError.Parse
        else -> AppError.Unknown(throwable)
    }
}
