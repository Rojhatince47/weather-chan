package io.github.rojhatince47.weatherchan.core.common.di

import io.github.rojhatince47.weatherchan.core.common.dispatcher.DefaultDispatcher
import io.github.rojhatince47.weatherchan.core.common.dispatcher.IoDispatcher
import io.github.rojhatince47.weatherchan.core.common.error.DefaultErrorMapper
import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {
    @Binds
    abstract fun bindErrorMapper(impl: DefaultErrorMapper): ErrorMapper

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
