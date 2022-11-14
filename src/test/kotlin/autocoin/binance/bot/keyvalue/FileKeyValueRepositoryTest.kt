package autocoin.binance.bot.keyvalue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

class FileKeyValueRepositoryTest {

    @TempDir
    lateinit var tempFolder: File

    private lateinit var tested: FileKeyValueRepository

    @BeforeEach
    fun setup() {
        tested = FileKeyValueRepository(clock = Clock.systemDefaultZone())
    }

    @Test
    fun shouldReturnNullWhenNothingSavedBefore() {
        // when
        val latestVersion = tested.getLatestVersion(tempFolder, key = "test")
        // then
        assertThat(latestVersion).isNull()
    }

    @Test
    fun shouldReadLatestVersionFromFileWhenAfterOneSave() {
        // given
        tested.saveNewVersion(directory = tempFolder, key = "test", value = "value1")
        // when
        val latestVersion = tested.getLatestVersion(directory = tempFolder, key = "test")
        // then
        assertThat(latestVersion).isNotNull
        assertThat(latestVersion!!.value).isEqualTo("value1")
    }


    @Test
    fun shouldReadLatestVersionFromFileAfterTwoSaves() {
        // given
        tested.saveNewVersion(directory = tempFolder, key = "test", value = "value1")
        tested.saveNewVersion(directory = tempFolder, key = "test", value = "value2")
        // when
        val latestVersion = tested.getLatestVersion(directory = tempFolder, key = "test")
        // then
        assertThat(latestVersion).isNotNull
        assertThat(latestVersion!!.value).isEqualTo("value2")
    }

    @Test
    fun shouldCreateFileWithProperNameAndContent() {
        // given
        val currentTimeMillis = 19L
        val tested = FileKeyValueRepository(clock = Clock.fixed(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault()), fileExtension = ".json")
        val currentTimeMillisAsDateTimeString = "19700101010000019"
        // when
        tested.saveNewVersion(directory = tempFolder, key = "test", value = "value1")
        // then
        val savedFile = tempFolder.resolve("test_$currentTimeMillisAsDateTimeString.json")
        assertThat(savedFile).exists()
        assertThat(savedFile.readText()).isEqualTo("value1")
    }

    private class QueueClock(timeMillisList: List<Long>) : Clock() {
        private val timeMillisQueue: ArrayDeque<Long> = ArrayDeque(timeMillisList)

        override fun getZone(): ZoneId {
            TODO("Not yet implemented")
        }

        override fun withZone(zone: ZoneId?): Clock {
            TODO("Not yet implemented")
        }

        override fun instant(): Instant {
            return Instant.ofEpochMilli(timeMillisQueue.poll())
        }

    }

    @Test
    fun shouldKeepLastNVersions() {
        val tested = FileKeyValueRepository(clock = QueueClock(listOf(1L, 2L, 3L, 4L)), fileExtension = ".json")
        tested.saveNewVersion(directory = tempFolder, key = "test", "value1")
        tested.saveNewVersion(directory = tempFolder, key = "test", "value2")
        tested.saveNewVersion(directory = tempFolder, key = "test", "value3")
        tested.saveNewVersion(directory = tempFolder, key = "test", "value4")
        // when
        tested.keepLastNVersions(tempFolder, key = "test", maxVersions = 2)
        // then
        assertThat(tempFolder.resolve("test_19700101010000001.json")).doesNotExist()
        assertThat(tempFolder.resolve("test_19700101010000002.json")).doesNotExist()
        assertThat(tempFolder.resolve("test_19700101010000003.json")).exists()
        assertThat(tempFolder.resolve("test_19700101010000004.json")).exists()
    }

    @Test
    fun shouldNotRemoveOtherKeysWhenKeepLastNVersions() {
        val tested = FileKeyValueRepository(clock = QueueClock(listOf(1L, 2L, 3L)), fileExtension = ".json")
        tested.saveNewVersion(directory = tempFolder, key = "test", "value1")
        tested.saveNewVersion(directory = tempFolder, key = "test", "value2")
        tested.saveNewVersion(directory = tempFolder, key = "test2", "value3")
        // when
        tested.keepLastNVersions(tempFolder, key = "test", maxVersions = 1)
        // then
        assertThat(tempFolder.resolve("test_19700101010000001.json")).doesNotExist()
        assertThat(tempFolder.resolve("test_19700101010000002.json")).exists()
        assertThat(tempFolder.resolve("test2_19700101010000003.json")).exists()
    }

}
