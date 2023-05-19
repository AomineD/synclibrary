package com.aomined.synclibrary.di

import com.aomine.playdedeapi.api.ApiInterface
import com.aomined.synclibrary.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServicesModule {
    @Singleton
    @Provides
    fun provideInterfaceApi(apiInterface: ApiInterface): ApiService {
        return ApiService(apiInterface)
    }
}