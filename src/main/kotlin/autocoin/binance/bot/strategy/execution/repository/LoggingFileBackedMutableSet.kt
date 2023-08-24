package autocoin.binance.bot.strategy.execution.repository

import mu.KLogging
import kotlin.io.path.absolutePathString

class LoggingFileBackedMutableSet<T>(private val decorated: FileBackedMutableSet<T>, private val logPrefix: String) :
    FileBackedMutableSet<T> by decorated {
    private companion object : KLogging()

    override fun save(): FileOperationResult {
        return decorated.save().also {
            if (it.exception != null) {
                if (it.file != null) {
                    logger.error(it.exception) { "[$logPrefix] Failed to save to ${it.file.absolutePathString()}" }
                } else {
                    logger.error(it.exception) { "[$logPrefix] Failed to save" }
                }
            } else {
                logger.info { "[$logPrefix] Saved ${this.size} items to ${it.file?.absolutePathString()}" }
            }
        }
    }

    override fun load(): FileOperationResult {
        return decorated.load().also {
            if (it.exception != null) {
                if (it.file != null) {
                    logger.error(it.exception) { "[$logPrefix] Failed to load items from ${it.file.absolutePathString()}" }
                } else {
                    logger.error(it.exception) { "[$logPrefix] Failed to load items" }
                }
            } else if (it.file != null) {
                logger.info { "[$logPrefix] Loaded ${this.size} items from ${it.file.absolutePathString()}" }
            } else {
                logger.info { "[$logPrefix] Loaded ${this.size} items, no previously saved set found" }
            }
        }
    }
}

fun <T> FileBackedMutableSet<T>.logging(logPrefix: String) =
    LoggingFileBackedMutableSet(decorated = this, logPrefix = logPrefix)
