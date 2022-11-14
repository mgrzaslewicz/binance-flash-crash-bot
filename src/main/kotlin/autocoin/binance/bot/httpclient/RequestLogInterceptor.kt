package autocoin.binance.bot.httpclient

import mu.KLogging
import okhttp3.Interceptor
import okhttp3.Response


class RequestLogInterceptor : Interceptor {
    companion object : KLogging()


    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val t1 = System.nanoTime()
        logger.debug { "Sending request ${request.url} ${chain.connection()} on ${request.headers}" }
        val response = chain.proceed(request)
        val t2 = System.nanoTime()
        logger.debug { "Received response for ${response.request.url} in ${t2 - t1 / 1e6} ms, ${response.headers}" }
        return response
    }
}
