package dev.forcetower.hilt.example

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorthlessDependencies {
    @Named("applicationName")
    fun applicationName(): String
}
