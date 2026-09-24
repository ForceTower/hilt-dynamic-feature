package dev.forcetower.hilt.example

import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import javax.inject.Named
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object Module {
    @Provides
    @Singleton
    fun provideToken(): Token {
        return Token("123456")
    }

    @Provides
    @Named("applicationName")
    fun applicationName(): String = "Hilt Dynamic"
}
