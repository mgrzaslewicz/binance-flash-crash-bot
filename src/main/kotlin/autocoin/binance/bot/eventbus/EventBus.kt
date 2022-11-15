package autocoin.binance.bot.eventbus

import mu.KLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface EventType<T> {
    fun isAsync(): Boolean = false
}

interface EventBus {
    fun <T> register(eventType: EventType<T>, eventHandler: (event: T) -> Unit)
    fun <T> publish(eventType: EventType<T>, event: T)
}

class DefaultEventBus(private val executorService: ExecutorService = Executors.newCachedThreadPool()) : EventBus {
    private companion object : KLogging()

    private val eventListeners = ConcurrentHashMap<EventType<*>, MutableList<(payload: Any) -> Unit>>()

    override fun <T> register(eventType: EventType<T>, eventListener: (payload: T) -> Unit) {
        val listeners: MutableList<(payload: Any) -> Unit> = eventListeners.computeIfAbsent(eventType as EventType<*>) { ArrayList() }
        @Suppress("UNCHECKED_CAST")
        listeners += eventListener as (event: Any) -> Unit
    }

    override fun <T> publish(eventType: EventType<T>, event: T) {
        if (eventType.isAsync()) {
            eventListeners[eventType]?.forEach {
                executorService.submit {
                    tryInvokeEventListener(it, event as Any)
                }
            }
        } else {
            eventListeners[eventType]?.forEach {
                tryInvokeEventListener(it, event as Any)
            }
        }
    }

    private fun tryInvokeEventListener(eventListener: (payload: Any) -> Unit, event: Any) {
        try {
            eventListener(event)
        } catch (e: Exception) {
            logger.error(e) { "Handling event by listener failed" }
        }
    }

}
