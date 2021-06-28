package dev.forcetower.hilt.example

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object Module {
    @Provides
    @Reusable
    fun provideToken(): Token {
        return Token("123456")
    }
}