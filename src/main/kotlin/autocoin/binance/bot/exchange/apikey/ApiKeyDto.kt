package autocoin.binance.bot.exchange.apikey

import java.security.MessageDigest


fun String.md5() = MessageDigest.getInstance("MD5").digest(this.toByteArray()).joinToString("") { "%02x".format(it) }

data class ApiKeyDto(
    val publicKey: String,
    val secretKey: String,
) {
    override fun toString(): String {
        return "ApiKeyDto(publicKey.md5()='${publicKey.md5()}', secretKey.md5()='${secretKey.md5()}')"
    }
}
