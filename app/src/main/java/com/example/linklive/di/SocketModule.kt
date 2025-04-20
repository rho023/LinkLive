package com.example.linklive.di

import com.example.linklive.data.SocketHandler
import com.example.linklive.domain.SocketService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SocketModule {
    @Binds
    @Singleton
    abstract fun bindSocketService(socketHandler: SocketHandler): SocketService
}