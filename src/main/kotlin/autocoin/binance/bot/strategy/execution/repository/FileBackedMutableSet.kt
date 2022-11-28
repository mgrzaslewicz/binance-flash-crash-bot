package autocoin.binance.bot.strategy.execution.repository

import java.nio.file.Path

data class FileOperationResult(
    val file: Path?,
    val exception: Exception? = null
)

interface FileBackedMutableSet<T> : MutableSet<T> {
    fun save(): FileOperationResult
    fun load(): FileOperationResult
}
