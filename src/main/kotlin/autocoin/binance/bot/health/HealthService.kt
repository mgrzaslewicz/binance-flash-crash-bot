package autocoin.binance.bot.health

import autocoin.binance.bot.exchange.BinancePriceStream
import autocoin.binance.bot.strategy.StrategyExecutorService
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class UserOrder(
    val exchangeOrderId: String,
    val price: String,
    val amount: String,
    val createdTimestamp: String,
)

data class RunningStrategy(
    val type: String,
    val currencyPair: String,
    val user: String,
    val orders: List<UserOrder>,
    val ordersAmountSum: String,
)

data class Process(
    val startedTimestamp: String,
    val runningDuration: String,
)

data class PriceStream(
    private val startedTimestamp: String,
    val priceWebSocketReconnectCount: Int,
    val priceWebSocketFailureCount: Int,
    val runningDuration: String,
)

data class Health(
    val healthy: Boolean,
    val healthCalculatedAt: String,
    val unhealthyReasons: List<String>,
    val priceStream: PriceStream?,
    val runningStrategies: List<RunningStrategy>,
    val process: Process,
)

class HealthService(
    private val binancePriceStream: BinancePriceStream,
    private val strategyExecutorService: StrategyExecutorService,
    private val clock: Clock,
    private val startedAt: Instant,
) {
    private val startedAtString: String by lazy { startedAt.toString() }

    fun getHealth(): Health {
        val isConnectedToPriceStream = binancePriceStream.isConnected()
        val runningStrategies = strategyExecutorService.getRunningStrategies()
            .asSequence()
            .map { strategy ->
                RunningStrategy(
                    currencyPair = strategy.baseCurrencyCode + "/" + strategy.counterCurrencyCode,
                    user = strategy.userId,
                    type = strategy.strategyType.name,
                    orders = strategy.orders.asSequence()
                        .sortedByDescending { it.createTimeMillis }
                        .map { order ->
                            UserOrder(
                                exchangeOrderId = order.exchangeOrderId,
                                price = order.price.toPlainString(),
                                amount = order.amount.toPlainString(),
                                createdTimestamp = Instant.ofEpochMilli(order.createTimeMillis).toString(),
                            )
                        }.toList(),
                    ordersAmountSum = strategy.orders.sumOf { order -> order.amount }.toPlainString(),
                )
            }
            .sortedBy { it.user }
            .toList()

        val priceStreamStartTimestamp = binancePriceStream.getWebsocketConnectionStartedTimestamp()

        val health = Health(
            healthy = isConnectedToPriceStream,
            unhealthyReasons = listOfNotNull(
                if (isConnectedToPriceStream) null else "Not connected to price websocket stream",
            ),
            priceStream = if (priceStreamStartTimestamp != null) {
                PriceStream(
                    startedTimestamp = priceStreamStartTimestamp.toString(),
                    runningDuration = Duration.between(priceStreamStartTimestamp, Instant.now(clock)).toString(),
                    priceWebSocketReconnectCount = binancePriceStream.getWebsocketReconnectCount(),
                    priceWebSocketFailureCount = binancePriceStream.getWebsocketFailureCount(),
                )
            } else null,
            runningStrategies = runningStrategies,
            process = Process(
                startedTimestamp = startedAtString,
                runningDuration = Duration.between(startedAt, Instant.now(clock)).toString(),
            ),
            healthCalculatedAt = clock.instant().toString(),
        )
        return health
    }
}
