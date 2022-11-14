package autocoin.binance.bot.user.repository

import mu.KLogging

class LoggingUserRepository(private val decorated: UserRepository) : UserRepository {
    private companion object : KLogging()

    override fun getUsers(): List<User> {
        return try {
            val users = decorated.getUsers()
            logger.info { "There are ${users.size} users" }
            return users
        } catch (e: Exception) {
            logger.error(e) { "Could not get users" }
            emptyList()
        }
    }
}

fun UserRepository.logging() = LoggingUserRepository(this)
