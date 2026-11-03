package io.github.rojhatince47.weatherchan.core.common.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.rojhatince47.weatherchan.core.common.dispatcher.DefaultDispatcher
import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import io.github.rojhatince47.weatherchan.core.network.NetworkErrorMapper
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {
    @Binds
    abstract fun bindErrorMapper(impl: NetworkErrorMapper): ErrorMapper

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
