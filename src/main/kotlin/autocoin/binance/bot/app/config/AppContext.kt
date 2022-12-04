package autocoin.binance.bot.app.config

import autocoin.binance.bot.eventbus.DefaultEventBus
import autocoin.binance.bot.exchange.BinancePriceStream
import autocoin.binance.bot.exchange.LoggingOnlyWalletService
import autocoin.binance.bot.exchange.PriceWebSocketConnectionKeeper
import autocoin.binance.bot.exchange.addingBinanceMarketOrderWithCounterCurrencyAmountBehavior
import autocoin.binance.bot.exchange.addingDelay
import autocoin.binance.bot.exchange.addingTestBinanceMarketOrderWithCounterCurrencyAmountBehavior
import autocoin.binance.bot.exchange.logging
import autocoin.binance.bot.exchange.measuringTime
import autocoin.binance.bot.exchange.mockingLimitBuyOrder
import autocoin.binance.bot.exchange.rateLimiting
import autocoin.binance.bot.health.HealthMetricsScheduler
import autocoin.binance.bot.health.HealthService
import autocoin.binance.bot.httpserver.HealthController
import autocoin.binance.bot.httpserver.ServerBuilder
import autocoin.binance.bot.strategy.ExchangeStrategyExecutorService
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionFileBackedMutableSet
import autocoin.binance.bot.strategy.execution.repository.logging
import autocoin.binance.bot.strategy.executor.BinanceStrategyExecutorProvider
import autocoin.binance.bot.strategy.loggingStrategyExecutor
import autocoin.binance.bot.strategy.parameters.repository.StrategyParametersFileBackedMutableSet
import autocoin.binance.bot.user.repository.FileUserRepository
import autocoin.binance.bot.user.repository.logging
import autocoin.metrics.JsonlFileStatsDClient
import autocoin.metrics.MetricsService
import automate.profit.autocoin.exchange.CachingXchangeProvider
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.XchangeFactoryWrapper
import automate.profit.autocoin.exchange.XchangeSpecificationApiKeyAssigner
import automate.profit.autocoin.exchange.apikey.ExchangeApiKey
import automate.profit.autocoin.exchange.apikey.ExchangeDto
import automate.profit.autocoin.exchange.apikey.ExchangeKeyDto
import automate.profit.autocoin.exchange.apikey.ExchangeKeyService
import automate.profit.autocoin.exchange.apikey.ExchangeService
import automate.profit.autocoin.exchange.apikey.ServiceApiKeysProvider
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.DemoOrderCreator
import automate.profit.autocoin.exchange.order.XchangeOrderService
import automate.profit.autocoin.exchange.peruser.ExchangeSpecificationVerifier
import automate.profit.autocoin.exchange.peruser.UserExchangeServicesFactory
import automate.profit.autocoin.exchange.peruser.XchangeUserExchangeServicesFactory
import automate.profit.autocoin.exchange.ratelimiter.ExchangeRateLimiters
import automate.profit.autocoin.exchange.wallet.ExchangeCurrencyPairsInWalletService
import automate.profit.autocoin.exchange.wallet.ExchangeWalletService
import automate.profit.autocoin.exchange.wallet.XchangeExchangeWalletService
import automate.profit.autocoin.keyvalue.FileKeyValueRepository
import com.binance.api.client.BinanceApiClientFactory
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.timgroup.statsd.StatsDClient
import mu.KLogging
import org.knowm.xchange.ExchangeFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.Executors.newSingleThreadScheduledExecutor

class CurrencyPairDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        return CurrencyPair.of(key)
    }
}


val objectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(
        SimpleModule()
            .addKeyDeserializer(CurrencyPair::class.java, CurrencyPairDeserializer())
    )

class AppContext(private val appConfig: AppConfig) {
    private companion object : KLogging()

    val clock = Clock.systemDefaultZone()
    val startedAt: Instant = clock.instant()

    val eventBus = DefaultEventBus()

    val binancePriceStream = BinancePriceStream(
        eventBus = eventBus,
        binanceApiWebSocketClient = BinanceApiClientFactory.newInstance().newWebSocketClient(),
        clock = clock,
    )

    val priceWebSocketConnectionKeeper = PriceWebSocketConnectionKeeper(
        clock = clock,
        binancePriceStream = binancePriceStream,
        scheduledExecutor = newSingleThreadScheduledExecutor(),
    )


    val fileKeyValueRepository = FileKeyValueRepository()

    val strategyParameters = StrategyParametersFileBackedMutableSet(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    ).logging(logPrefix = "StrategyParameters")

    val strategyExecutions = StrategyExecutionFileBackedMutableSet(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    ).logging(logPrefix = "StrategyExecutions")

    val exchangeRateLimiters = ExchangeRateLimiters(
        defaultPermitsPerDuration = 300L,
        defaultDuration = Duration.of(1L, ChronoUnit.MINUTES),
        permitAcquireTimeout = Duration.of(250L, ChronoUnit.MILLIS),
    )

    val userExchangeServicesFactory: UserExchangeServicesFactory = XchangeUserExchangeServicesFactory(
        serviceApiKeysProvider = object : ServiceApiKeysProvider {
            override fun getApiKeys(supportedExchange: SupportedExchange): ExchangeApiKey? {
                TODO("Not yet implemented")
            }
        },
        cachingXchangeProvider = CachingXchangeProvider(
            xchangeSpecificationApiKeyAssigner = XchangeSpecificationApiKeyAssigner(ExchangeSpecificationVerifier()),
            xchangeFactoryWrapper = XchangeFactoryWrapper(ExchangeFactory.INSTANCE),
        ),
        exchangeRateLimiters = exchangeRateLimiters,
        clock = clock,
    )
    val exchangeService = object : ExchangeService {
        override fun getExchangeIdByName(exchangeName: String): String {
            TODO("Not yet implemented")
        }

        override fun getExchangeNameById(exchangeId: String): String {
            TODO("Not yet implemented")
        }

        override fun getExchanges(): List<ExchangeDto> {
            TODO("Not yet implemented")
        }
    }
    val exchangeKeyService = object : ExchangeKeyService {
        override fun getExchangeKey(exchangeUserId: String, exchangeId: String): ExchangeKeyDto? {
            TODO("Not yet implemented")
        }

        override fun getExchangeKeys(): List<ExchangeKeyDto> {
            TODO("Not yet implemented")
        }

        override fun getExchangeKeys(exchangeUserId: String): List<ExchangeKeyDto> {
            TODO("Not yet implemented")
        }
    }

    val exchangeWalletService: ExchangeWalletService = if (appConfig.shouldMakeRealOrders) {
        XchangeExchangeWalletService(
            exchangeService = exchangeService,
            exchangeKeyService = exchangeKeyService,
            userExchangeServicesFactory = userExchangeServicesFactory,
        )
    } else {
        LoggingOnlyWalletService()
    }

    val exchangeCurrencyPairsInWalletService = object : ExchangeCurrencyPairsInWalletService {
        override fun generateFromWalletIfGivenEmpty(exchangeName: String, exchangeKey: ExchangeKeyDto, currencyPairs: List<CurrencyPair>): List<CurrencyPair> {
            TODO("Not yet implemented")
        }

        override fun generateFromWalletIfGivenEmpty(exchangeName: String, exchangeUserId: String, currencyPairs: List<CurrencyPair>): List<CurrencyPair> {
            TODO("Not yet implemented")
        }
    }

    val exchangeOrderService = if (appConfig.shouldMakeRealOrders) {
        logger.info { "Will make real orders" }
        XchangeOrderService(
            exchangeService = exchangeService,
            exchangeKeyService = exchangeKeyService,
            userExchangeServicesFactory = userExchangeServicesFactory,
            exchangeCurrencyPairsInWallet = exchangeCurrencyPairsInWalletService,
            demoOrderCreator = DemoOrderCreator(clock),
        )
            .addingBinanceMarketOrderWithCounterCurrencyAmountBehavior(clock = clock)
            .measuringTime()
            .rateLimiting()
            .logging()

    } else {
        logger.warn { "Will NOT make real orders, just test market order at binance and return mock limit orders instead" }
        XchangeOrderService(
            exchangeService = exchangeService,
            exchangeKeyService = exchangeKeyService,
            userExchangeServicesFactory = userExchangeServicesFactory,
            exchangeCurrencyPairsInWallet = exchangeCurrencyPairsInWalletService,
            demoOrderCreator = DemoOrderCreator(clock),
        )
            .addingTestBinanceMarketOrderWithCounterCurrencyAmountBehavior(clock = clock)
            .mockingLimitBuyOrder(clock = clock)
            .rateLimiting()
            .addingDelay(Duration.ofSeconds(1))
            .logging()
            .measuringTime()

    }


    val strategyExecutorProvider = BinanceStrategyExecutorProvider(
        exchangeOrderService = exchangeOrderService,
        strategyExecutions = strategyExecutions,
        javaExecutorService = Executors.newCachedThreadPool(),
    )

    val strategyExecutionsService =
        ExchangeStrategyExecutorService(
            strategyExecutions = strategyExecutions,
            strategyExecutorProvider = strategyExecutorProvider,
        ).loggingStrategyExecutor(minDelayBetweenLogs = Duration.ofSeconds(1))

    val userRepository = FileUserRepository(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory,
        objectMapper = objectMapper,
        fileKeyValueRepository = fileKeyValueRepository,
    ).logging()

    val healthService = HealthService(
        binancePriceStream = binancePriceStream,
        strategyExecutorService = strategyExecutionsService,
        clock = clock,
        startedAt = startedAt,
    )
    val healthController = HealthController(
        healthService = healthService,
        objectMapper = objectMapper,
    )

    val controllers = listOf(healthController)

    val statsdClient: StatsDClient = JsonlFileStatsDClient(appConfig.botHomeFolder.resolve("metrics.jsonl").toFile())

    val metricsService: MetricsService = MetricsService(statsdClient)

    val server = ServerBuilder(appConfig.serverPort, controllers, metricsService).build()

    val healthMetricsScheduler = HealthMetricsScheduler(
        healthService = healthService,
        metricsService = metricsService,
        scheduledExecutor = newSingleThreadScheduledExecutor(),
        delayBetweenMetrics = Duration.ofSeconds(30),
    )
}
