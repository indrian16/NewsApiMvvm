package com.pratamawijaya.newsapimvvm.di

import android.arch.persistence.room.Room
import com.google.gson.GsonBuilder
import com.pratamawijaya.newsapimvvm.data.NewsApiServices
import com.pratamawijaya.newsapimvvm.data.db.NewsAppDb
import com.pratamawijaya.newsapimvvm.data.db.mapper.ArticleTableMapper
import com.pratamawijaya.newsapimvvm.data.mapper.ArticleMapper
import com.pratamawijaya.newsapimvvm.data.repository.NewsRepository
import com.pratamawijaya.newsapimvvm.data.repository.NewsRepositoryImpl
import com.pratamawijaya.newsapimvvm.ui.topheadline.TopHeadlineViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    val context by lazy { androidApplication() }
    single { createOkHttpClient() }
    single { createWebService<NewsApiServices>(get(), "https://newsapi.org/v2/") }
    // db
    single { Room.databaseBuilder(context, NewsAppDb::class.java, "newsapi.db").build() }
    single { get<NewsAppDb>().articleDao() }
    // mapper
    single { ArticleMapper() }
    single { ArticleTableMapper() }
    single { NewsRepositoryImpl(get(), get(), get(), get()) as NewsRepository }
    viewModel { TopHeadlineViewModel(get()) }
}

fun createOkHttpClient(): OkHttpClient {
    val httpLoggingInterceptor = HttpLoggingInterceptor()
    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

    val API_KEY = "4b4df2ea3a154950852b6fda536cfb7f"
    return OkHttpClient.Builder()
            .connectTimeout(60L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().addHeader("X-Api-Key", API_KEY).build()
                chain.proceed(req)
            }
            .build()
}

inline fun <reified T> createWebService(okHttpClient: OkHttpClient, url: String): T {
    val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create()

    val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build()
    return retrofit.create(T::class.java)
}