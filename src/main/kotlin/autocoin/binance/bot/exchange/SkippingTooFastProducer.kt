package autocoin.binance.bot.exchange

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Ensures that only one job is handled at a time.
 * If a new job arrives while the previous one is still in progress, the new one is skipped.
 * It's useful when producing speed is greater than consuming and jobs are run from different threads.
 */
class SkippingTooFastProducer(
    private val runInProgressLock: Lock = ReentrantLock(),
) : SkippingConsumer {
    private fun previousRunIsFinished(): Boolean {
        return runInProgressLock.tryLock()
    }

    private fun markRunAsFinished() {
        runInProgressLock.unlock()
    }

    override fun run(job: Runnable, onSkipped: Runnable) {
        if (previousRunIsFinished()) {
            try {
                job.run()
            } finally {
                markRunAsFinished()
            }
        } else {
            onSkipped.run()
        }
    }
}
