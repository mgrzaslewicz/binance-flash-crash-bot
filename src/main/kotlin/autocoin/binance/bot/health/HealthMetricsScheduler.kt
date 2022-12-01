package autocoin.binance.bot.health

import autocoin.metrics.MetricsService
import mu.KLogging
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class HealthMetricsScheduler(
    private val delayBetweenMetrics: Duration = Duration.of(1, ChronoUnit.MINUTES),
    private val healthService: HealthService,
    private val metricsService: MetricsService,
    private val scheduledExecutor: ScheduledExecutorService,
) {
    companion object : KLogging()

    private fun reportMemoryUsage() {
        metricsService.recordMemory()
    }

    private fun reportHealth(healthy: Boolean) {
        metricsService.recordHealth(healthy)
    }

    private fun reportDescriptorsUsage() {
        metricsService.recordDescriptors()
    }

    private fun reportThreadsUsage() {
        metricsService.recordThreadCount()
    }

    fun scheduleSendingMetrics() {
        logger.info { "Scheduling sending metrics every ${delayBetweenMetrics}: health, memory usage, threads count, open files count" }
        scheduledExecutor.scheduleAtFixedRate({
            try {
                val health = healthService.getHealth()
                reportHealth(health.healthy)
                reportMemoryUsage()
                reportThreadsUsage()
                reportDescriptorsUsage()
            } catch (e: Exception) {
                logger.error(e) { "Something went wrong when sending metrics" }
            }
        }, 0, delayBetweenMetrics.seconds, TimeUnit.SECONDS)

    }

}
