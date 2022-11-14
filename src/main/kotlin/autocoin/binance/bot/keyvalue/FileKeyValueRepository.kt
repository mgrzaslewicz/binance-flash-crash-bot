package autocoin.binance.bot.keyvalue

import mu.KLogging
import java.io.File
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class LatestVersion(
    val file: Path,
    val value: String,
)

class FileKeyValueRepository(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val fileExtension: String = ".json",
) {
    private companion object : KLogging()

    /**
     * Avoid writing files at the same millisecond
     */
    private val saveLocks = WeakHashMap<String, Any>()

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    private fun getCurrentDateTimeAsString() = getDateTimeAsString(clock.millis())

    private fun getDateTimeAsString(millis: Long) = dateTimeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime())

    private fun getNumberFrom(fileName: String): Long {
        val exchangeNameAndDateTime = fileName.split("_", fileExtension)
        return exchangeNameAndDateTime[1].toLong()
    }

    fun getLatestVersion(directory: File, key: String): LatestVersion? {
        val fileName = directory.ensureDirectory().list()!!.filter {
            it.startsWith(key) && it.endsWith(fileExtension)
        }.maxByOrNull { getNumberFrom(it) }
        return if (fileName != null) {
            val file = directory.resolve(fileName)
            return LatestVersion(file = directory.resolve(fileName).toPath(), value = file.readText())
        } else {
            null
        }
    }

    private fun File.ensureDirectory(): File {
        if (!(exists() || mkdirs())) {
            throw IllegalStateException("Could not create directory $this")
        }
        return this
    }

    fun keepLastNVersions(directory: File, key: String, maxVersions: Int) {
        logger.debug { "Keeping max $maxVersions in $directory" }
        val allFiles = directory.ensureDirectory().list()
            ?.filter { it.startsWith(key) && it.endsWith(fileExtension) }
            ?.sortedBy { getNumberFrom(it) }
        if ((allFiles?.size ?: 0) > maxVersions) {
            allFiles?.subList(0, maxVersions)
                ?.forEach { directory.resolve(it).delete() }
        }
    }

    /**
     * Saves value with file name as key + version (timestamp)
     * @return path of file in directory to which value was saved
     */
    fun saveNewVersion(directory: File, key: String, value: String): Path {
        val version = getCurrentDateTimeAsString()

        val newFileName = "${key}_$version$fileExtension"
        synchronized(saveLocks) {
            saveLocks.computeIfAbsent(key) { Object() }
        }
        synchronized(saveLocks.getValue(key)) {
            val newFile = directory.ensureDirectory().resolve(newFileName)
            newFile.createNewFile()
            newFile.writeText(value)
            return newFile.toPath()
        }
    }

}
