package autocoin.binance.bot.exchange.ratelimit

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.autocoin.exchangegateway.spi.ratelimiter.RateLimiter
import com.autocoin.exchangegateway.spi.ratelimiter.RateLimiterProvider
import java.util.concurrent.ConcurrentHashMap
import com.google.common.util.concurrent.RateLimiter as GoogleRateLimiter

class PerApiKeyRateLimiterProvider(
    private val permitsPerSecondPerApiKey: Double = 5.0,
) : RateLimiterProvider<ApiKeyId> {
    /**
     * https://www.binance.com/en/support/announcement/notice-on-adjusting-order-rate-limits-to-the-spot-exchange-2188a59425384e2082b79d9beccf669c
     */
    private val rateLimitersPerApiKey = ConcurrentHashMap<ApiKeyId, RateLimiter>()
    override fun invoke(identifier: ApiKeyId): RateLimiter {
        return rateLimitersPerApiKey.computeIfAbsent(identifier) {
            object : RateLimiter {
                private val googleRateLimiter = GoogleRateLimiter.create(permitsPerSecondPerApiKey)
                override fun tryAcquire() = googleRateLimiter.tryAcquire()
                override fun acquire() = googleRateLimiter.acquire()
            }
        }
    }
}
