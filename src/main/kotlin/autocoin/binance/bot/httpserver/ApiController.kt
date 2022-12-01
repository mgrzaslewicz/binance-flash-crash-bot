package autocoin.binance.bot.httpserver

interface ApiController {
    fun apiHandlers(): List<ApiHandler>
}
