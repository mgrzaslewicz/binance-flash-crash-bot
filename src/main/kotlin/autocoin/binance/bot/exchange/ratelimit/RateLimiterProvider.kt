package autocoin.binance.bot.exchange.ratelimit

import autocoin.binance.bot.exchange.apikey.ApiKeyId
import com.google.common.util.concurrent.RateLimiter

interface RateLimiterProvider {
    operator fun invoke(apiKeyId: ApiKeyId): RateLimiter
}
