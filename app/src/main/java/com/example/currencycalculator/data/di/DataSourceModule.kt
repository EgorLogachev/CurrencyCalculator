package com.example.currencycalculator.data.di

import android.content.Context
import com.example.currencycalculator.data.Repository
import com.example.currencycalculator.data.RepositoryImpl
import com.example.currencycalculator.data.networking.ApiService
import com.example.currencycalculator.data.storage.ConversionsStorage
import com.example.currencycalculator.data.storage.ConversionsStorageImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    private const val BASE_URL = "https://bank.gov.ua/"

    @Singleton
    @Provides
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .build()

    @Singleton
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Singleton
    @Provides
    fun provideHistory(@ApplicationContext context: Context): ConversionsStorage =
        ConversionsStorageImpl(context)

    @Singleton
    @Provides
    fun provideRepository(
        apiService: ApiService,
        conversionsStorage: ConversionsStorage
    ): Repository = RepositoryImpl(apiService, conversionsStorage)


}