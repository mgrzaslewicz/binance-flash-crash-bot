package autocoin.binance.bot.exchange

interface SkippingConsumer {
    fun run(
        runnable: Runnable, onSkipped: Runnable = Runnable { }
    )
}
