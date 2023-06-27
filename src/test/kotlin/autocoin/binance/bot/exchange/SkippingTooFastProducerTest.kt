package autocoin.binance.bot.exchange

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

class SkippingTooFastProducerTest {

    @Test
    fun shouldSkipJobWhenPreviousIsNotFinished() {
        // given
        val longRunningJobLock = ReentrantLock()
        val jobsAllowedToRunCounter = AtomicInteger(0)
        val tested = SkippingTooFastProducer()
        val job = Runnable {
            longRunningJobLock.lock()
            jobsAllowedToRunCounter.incrementAndGet()
        }
        // when
        val threadExecutor1 = Executors.newSingleThreadExecutor()
        threadExecutor1.submit {
            tested.run(job)
        }
        val threadExecutor2 = Executors.newSingleThreadExecutor()
        threadExecutor2.submit {
            tested.run(job)
        }
        threadExecutor1.awaitTermination(100, TimeUnit.MILLISECONDS)
        threadExecutor2.awaitTermination(100, TimeUnit.MILLISECONDS)
        // then
        assertThat(jobsAllowedToRunCounter.get()).isEqualTo(1)
    }
}
