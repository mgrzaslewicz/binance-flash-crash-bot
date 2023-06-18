package autocoin.binance.bot.exchange.ratelimit

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.google.common.util.concurrent.RateLimiter
import java.util.concurrent.ConcurrentHashMap

class PerApiKeyRateLimiterProvider(
    private val permitsPerSecondPerApiKey: Double = 5.0,
) : RateLimiterProvider {
    /**
     * https://www.binance.com/en/support/announcement/notice-on-adjusting-order-rate-limits-to-the-spot-exchange-2188a59425384e2082b79d9beccf669c
     */
    private val rateLimitersPerApiKey = ConcurrentHashMap<ApiKeyId, RateLimiter>()
    override fun invoke(apiKeyId: ApiKeyId): RateLimiter {
        return rateLimitersPerApiKey.computeIfAbsent(apiKeyId) { RateLimiter.create(permitsPerSecondPerApiKey) }
    }
}
