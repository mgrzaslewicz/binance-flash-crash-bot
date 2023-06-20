package autocoin.binance.bot.app.config

import autocoin.binance.bot.eventbus.DefaultEventBus
import autocoin.binance.bot.exchange.BinancePriceStream
import autocoin.binance.bot.exchange.PriceWebSocketConnectionKeeper
import autocoin.binance.bot.exchange.apikey.ApiKeyId
import autocoin.binance.bot.exchange.binance.AddingBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService
import autocoin.binance.bot.exchange.binance.AddingTestBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService
import autocoin.binance.bot.exchange.binance.BinanceAuthorizedOrderServiceFactory
import autocoin.binance.bot.exchange.order.*
import autocoin.binance.bot.exchange.ratelimit.PerApiKeyRateLimiterProvider
import autocoin.binance.bot.exchange.wallet.LoggingOnlyWalletServiceGateway
import autocoin.binance.bot.exchange.wallet.measuringDuration
import autocoin.binance.bot.exchange.wallet.preLogging
import autocoin.binance.bot.exchange.wallet.rateLimiting
import autocoin.binance.bot.health.HealthMetricsScheduler
import autocoin.binance.bot.health.HealthService
import autocoin.binance.bot.httpserver.HealthController
import autocoin.binance.bot.httpserver.ServerBuilder
import autocoin.binance.bot.strategy.ExchangeStrategyExecutorService
import autocoin.binance.bot.strategy.execution.repository.StrategyExecutionFileBackedMutableSetBuilder
import autocoin.binance.bot.strategy.execution.repository.logging
import autocoin.binance.bot.strategy.executor.BinanceStrategyExecutorProvider
import autocoin.binance.bot.strategy.loggingStrategyExecutor
import autocoin.binance.bot.strategy.parameters.repository.StrategyParametersFileBackedMutableSetBuilder
import autocoin.metrics.JsonlFileStatsDClient
import autocoin.metrics.MetricsService
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.api.exchange.order.DemoOrderServiceGateway
import com.autocoin.exchangegateway.api.exchange.order.service.authorized.XchangeAuthorizedOrderServiceFactory
import com.autocoin.exchangegateway.api.exchange.wallet.service.authorized.XchangeAuthorizedWalletServiceFactory
import com.autocoin.exchangegateway.api.exchange.xchange.*
import com.autocoin.exchangegateway.spi.exchange.ExchangeName
import com.autocoin.exchangegateway.spi.exchange.apikey.ApiKeySupplier
import com.autocoin.exchangegateway.spi.exchange.order.gateway.OrderServiceGatewayUsingAuthorizedOrderService
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGateway
import com.autocoin.exchangegateway.spi.exchange.wallet.gateway.WalletServiceGatewayUsingAuthorizedWalletService
import com.binance.api.client.BinanceApiClientFactory
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.timgroup.statsd.StatsDClient
import mu.KLogging
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.Executors.newSingleThreadScheduledExecutor

class CurrencyPairDeserializer : KeyDeserializer() {
    override fun deserializeKey(
        key: String,
        ctxt: DeserializationContext,
    ): Any {
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


    val strategyParameters = StrategyParametersFileBackedMutableSetBuilder(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory.toFile(),
        objectMapper = objectMapper,
        clock = clock,
    )
        .build()
        .logging(logPrefix = "StrategyParameters")

    val strategyExecutions = StrategyExecutionFileBackedMutableSetBuilder(
        fileRepositoryDirectory = appConfig.fileRepositoryDirectory.toFile(),
        objectMapper = objectMapper,
        clock = clock,
    )
        .build()
        .logging(logPrefix = "StrategyExecutions")


    val xchangeProvider = CachingXchangeProvider(
        apiKeyToCacheKey = object : ApiKeyToCacheKeyProvider<ApiKeyId, String> {
            override operator fun invoke(
                exchangeName: ExchangeName,
                apiKeySupplier: ApiKeySupplier<ApiKeyId>,
            ): String {
                return apiKeySupplier.id.keyHash
            }
        },
        decorated = DefaultXchangeProvider(
            xchangeInstanceProvider = XchangeInstanceWrapper(),
            xchangeSpecificationApiKeyAssigner = XchangeSpecificationApiKeyAssigner(apiKeyVerifierGateway = XchangeApiKeyVerifierGateway()),
        )
    )

    val authorizedWalletServiceFactory = XchangeAuthorizedWalletServiceFactory(
        xchangeProvider = xchangeProvider,
    )

    val perApiKeyRateLimiterProvider = PerApiKeyRateLimiterProvider()

    val walletServiceGateway: WalletServiceGateway<ApiKeyId> = if (appConfig.shouldMakeRealOrders) {
        WalletServiceGatewayUsingAuthorizedWalletService(
            authorizedWalletServiceFactory = authorizedWalletServiceFactory,
        )
            .preLogging()
            .measuringDuration()
            .rateLimiting(rateLimiterProvider = perApiKeyRateLimiterProvider)
            .measuringDuration("with rate limit, ")
    } else {
        LoggingOnlyWalletServiceGateway()
    }

    val authorizedOrderServiceFactory = XchangeAuthorizedOrderServiceFactory(
        xchangeProvider = xchangeProvider,
        clock = clock,
    ).let {
        if (appConfig.shouldMakeRealOrders) {
            BinanceAuthorizedOrderServiceFactory(
                decorated = it,
                wrapper = { orderService ->
                    AddingBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService(
                        clock = clock,
                        decorated = orderService,
                    )
                },
            )
        } else {
            BinanceAuthorizedOrderServiceFactory(
                decorated = it,
                wrapper = { orderService ->
                    AddingTestBinanceMarketOrderWithCounterCurrencyAmountAuthorizedOrderService(
                        clock = clock,
                        decorated = orderService,
                    )
                },
            )
        }
    }

    val orderServiceGateway = if (appConfig.shouldMakeRealOrders) {
        logger.info { "Will operate on real orders" }
        OrderServiceGatewayUsingAuthorizedOrderService(
            authorizedOrderServiceFactory = authorizedOrderServiceFactory,
        )
            .preLogging()
            .measuringDuration()
            .rateLimiting(rateLimiterProvider = perApiKeyRateLimiterProvider)
            .measuringDuration("with rate limit, ")

    } else {
        logger.warn { "Will NOT operate on real orders, just test market order at binance and return mock limit orders instead" }
        DemoOrderServiceGateway<ApiKeyId>(
            clock = clock,
        )
            .mockingLimitBuyOrder(clock = clock)
            .preLogging()
            .addingDelay(Duration.ofSeconds(1))
            .rateLimiting(PerApiKeyRateLimiterProvider(permitsPerSecondPerApiKey = 0.1))
            .measuringDuration("including rate limit permit duration, ")
    }

    val strategyExecutorProvider = BinanceStrategyExecutorProvider(
        orderServiceGateway = orderServiceGateway,
        walletServiceGateway = walletServiceGateway,
        strategyExecutions = strategyExecutions,
        javaExecutorService = Executors.newCachedThreadPool(),
    )

    val strategyExecutionsService =
        ExchangeStrategyExecutorService(
            strategyExecutions = strategyExecutions,
            strategyExecutorProvider = strategyExecutorProvider,
        ).loggingStrategyExecutor(minDelayBetweenLogs = Duration.ofSeconds(1))

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
