package autocoin.binance.bot.user.repository

import autocoin.binance.bot.keyvalue.FileKeyValueRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FileUserRepository(
    private val fileKeyValueRepository: FileKeyValueRepository,
    private val fileRepositoryDirectory: Path,
    private val objectMapper: ObjectMapper
) : UserRepository {

    private object UsersType : TypeReference<List<User>>()

    private fun getAllUsers(): List<User> {
        val file = fileRepositoryDirectory.toFile()
        val content = fileKeyValueRepository.getLatestVersion(file, "users")
        return if (content != null) {
            try {
                objectMapper.readValue(content.value, UsersType)
            } catch (e: Exception) {
                throw Exception("Could not deserialize users from ${content.file.absolutePathString()})", e)
            }
        } else {
            emptyList()
        }
    }

    override fun getUsers() = getAllUsers()

}
