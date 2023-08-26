package autocoin.binance.bot.strategy.execution.repository

import com.autocoin.exchangegateway.api.keyvalue.FileKeyValueRepository
import java.util.*

class FileBackedMutableHashSet<T>(
    private val comparator: Comparator<T> = Comparator { o1, o2 -> o1.hashCode().compareTo(o2.hashCode()) },
    private val set: MutableSet<T> = TreeSet<T>(comparator),
    private val keyValueRepository: FileKeyValueRepository<String, Set<T>>,
    private val valueKey: String,
    private val numberOfBackupsToKeep: Int = Int.MAX_VALUE,
) : FileBackedMutableSet<T>, MutableSet<T> by set {

    private var isLoadedFromFile = false


    override fun save(): FileOperationResult {
        return try {
            val newVersion = keyValueRepository.saveNewVersion(key = valueKey, value = set)
            cleanupBackups()
            FileOperationResult(file = newVersion.file)
        } catch (e: Exception) {
            FileOperationResult(file = null, exception = e)
        }
    }

    private fun cleanupBackups() {
        if (numberOfBackupsToKeep < Int.MAX_VALUE) {
            keyValueRepository.keepLastNVersions(maxVersions = numberOfBackupsToKeep, key = valueKey)
        }
    }

    override fun load(): FileOperationResult {
        val latestVersion = keyValueRepository.getLatestVersion(valueKey)
        if (latestVersion != null) {
            try {
                set.clear()
                set.addAll(latestVersion.value)
            } catch (e: Exception) {
                return FileOperationResult(file = latestVersion.file, e)
            }
        }
        isLoadedFromFile = true
        return FileOperationResult(file = latestVersion?.file)
    }

}
