package autocoin.binance.bot.app.config

import autocoin.binance.bot.httpclient.RequestLogInterceptor
import autocoin.binance.bot.keyvalue.FileKeyValueRepository
import autocoin.binance.bot.strategy.repository.FileStrategyExecutionRepository
import autocoin.binance.bot.strategy.repository.logging
import autocoin.binance.bot.user.repository.FileUserRepository
import autocoin.binance.bot.user.repository.logging
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KLogging
import okhttp3.OkHttpClient
import java.util.concurrent.Executors.newSingleThreadScheduledExecutor
import java.util.concurrent.TimeUnit

val objectMapper = ObjectMapper().registerKotlinModule()

class AppContext(private val appConfig: AppConfig) {
    companion object : KLogging()

    val httpClient = OkHttpClient().newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(RequestLogInterceptor())
        .build()

    val webSocketClient = OkHttpClient().newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .addInterceptor(RequestLogInterceptor())
        .build()


    val nonCriticalJobsScheduledExecutorService = newSingleThreadScheduledExecutor()

    val fileKeyValueRepository = FileKeyValueRepository()

    val strategyExecutionRepository = FileStrategyExecutionRepository(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    ).logging()

    val userRepository = FileUserRepository(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    ).logging()


}
