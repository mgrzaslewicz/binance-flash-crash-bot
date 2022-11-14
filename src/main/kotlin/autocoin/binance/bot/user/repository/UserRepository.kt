package autocoin.binance.bot.user.repository

data class User(
    val userId: String,
)

interface UserRepository {
    fun getUsers(): List<User>
}


