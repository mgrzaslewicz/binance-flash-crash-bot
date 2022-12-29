package autocoin.binance.bot.strategy.execution.repository

import com.autocoin.exchangegateway.spi.keyvalue.FileKeyValueRepository
import java.io.File
import java.nio.file.Path
import java.util.function.Function

open class FileBackedMutableHashSet<T>(
    private val set: HashSet<T> = LinkedHashSet(),
    private val serializer: Function<Set<T>, String>,
    private val deserializer: Function<String, Set<T>>,
    private val valueKey: String,
    private val fileKeyValueRepository: FileKeyValueRepository,
    private val numberOfBackupsToKeep: Int = Int.MAX_VALUE,
    fileRepositoryDirectory: Path,
) : FileBackedMutableSet<T>, MutableSet<T> by set {

    private var isLoadedFromFile = false
    private val repositoryDirectory: File = fileRepositoryDirectory.toFile()


    override fun save(): FileOperationResult {
        val value: String?
        try {
            value = serializer.apply(set)
        } catch (e: Exception) {
            return FileOperationResult(file = null, exception = e)
        }
        val newVersionFile = fileKeyValueRepository.saveNewVersion(directory = repositoryDirectory, key = valueKey, value = value)
        cleanupBackups()
        return FileOperationResult(file = newVersionFile)
    }

    private fun cleanupBackups() {
        if (numberOfBackupsToKeep < Int.MAX_VALUE) {
            fileKeyValueRepository.keepLastNVersions(directory = repositoryDirectory, maxVersions = numberOfBackupsToKeep, key = valueKey)
        }
    }

    override fun load(): FileOperationResult {
        val fileWithValue = fileKeyValueRepository.getLatestVersion(directory = repositoryDirectory, valueKey)
        if (fileWithValue != null) {
            try {
                val newValues = deserializer.apply(fileWithValue.value)
                set.clear()
                set.addAll(newValues)
            } catch (e: Exception) {
                return FileOperationResult(file = fileWithValue.file, e)
            }
        }
        isLoadedFromFile = true
        return FileOperationResult(file = fileWithValue?.file)
    }

}
