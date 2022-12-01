package autocoin.binance.bot.httpserver

import io.undertow.server.HttpHandler
import io.undertow.util.HttpString

interface ApiHandler {
    val method: HttpString
    val urlTemplate: String
    val httpHandler: HttpHandler
}
