package kr.hanchae.moyeotrip.utils.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import kr.hanchae.moyeotrip.config.properties.JwtProperties
import org.redisson.api.RedissonClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.MILLIS
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtUtil(
    private val jwtProperties: JwtProperties,
    private val redissonClient: RedissonClient,
) {
    val accessKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.accessKey))
    val refreshKey: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.refreshKey))
    val accessTokenExpirationTime: Long = jwtProperties.accessTokenExpirationTime
    val refreshTokenExpirationTime: Long = jwtProperties.refreshTokenExpirationTime

    val log: Logger = LoggerFactory.getLogger(JwtUtil::class.java)

    fun generateAccessToken(
        userId: Long,
        nickName: String,
        expirationTime: Long = accessTokenExpirationTime,
    ): String =
        generateJwtToken(
            mapOf(
                "userId" to userId.toString(),
                "nickName" to nickName,
            ),
            expirationTime,
            accessKey,
        )

    fun generateRefreshToken(
        userId: Long,
        rotateId: String,
        expirationTime: Long = refreshTokenExpirationTime,
    ): String =
        generateJwtToken(
            mapOf(
                "userId" to userId.toString(),
                "rotateId" to rotateId,
            ),
            expirationTime,
            refreshKey,
        )

    fun storeCachedRefreshTokenRotateId(
        userId: Long,
        rotateId: String,
    ) {
        redissonClient
            .getBucket<String>(
                getRefreshTokenCacheKey(userId),
            ).set(rotateId, Duration.ofMillis(jwtProperties.refreshTokenExpirationTime))
    }

    companion object {
        private const val REFRESH_TOKEN_CACHE_PREFIX = "MoyeoTrip:refresh-token-cache:"

        fun getRefreshTokenCacheKey(userId: Long): String = REFRESH_TOKEN_CACHE_PREFIX.plus(userId)
    }

    private fun generateJwtToken(
        claims: Map<String, String>,
        expirationTime: Long,
        key: SecretKey,
    ): String =
        Jwts
            .builder()
            .claims(claims)
            .expiration(afterMillis(expirationTime))
            .signWith(key)
            .compact()

    private fun afterMillis(ms: Long): Date =
        Date.from(
            ZonedDateTime.now().plus(ms, MILLIS).toInstant(),
        )

    fun generateRotateId(): String = UUID.randomUUID().toString()

    fun validateCachedRefreshTokenRotateId(token: String): Boolean {
        val bucket = redissonClient.getBucket<String>(getRefreshTokenCacheKey(getUserId(refreshKey, token)))
        if (!bucket.isExists) {
            return false
        }

        val cachedRotateId = bucket.get()
        val rotateIdInToken = getRotateId(token)
        return cachedRotateId == rotateIdInToken
    }

    fun getRotateId(token: String): String = parseClaims(refreshKey, token).get("rotateId", String::class.java)

    fun getUserId(
        key: SecretKey,
        token: String,
    ): Long = parseClaims(key, token).get("userId", String::class.java).toLong()

    fun validateToken(
        key: SecretKey,
        token: String,
    ): Boolean {
        try {
            val expirationTime = parseClaims(key, token).expiration
            return expirationTime.after(Date.from(ZonedDateTime.now().toInstant()))
        } catch (ex: JwtException) {
            log.debug("JWT 검증에 실패했습니다: {}", ex.message)
        } catch (ex: IllegalArgumentException) {
            log.debug("올바르지 않은 JWT입니다: {}", ex.message)
        }
        return false
    }

    private fun parseClaims(
        key: SecretKey,
        token: String,
    ) = Jwts
        .parser()
        .verifyWith(key)
        .decryptWith(key)
        .build()
        .parseSignedClaims(token)
        .payload
}

fun String.isBearerToken(): Boolean = this.startsWith("Bearer ")

fun String.removeBearer(): String = this.removePrefix("Bearer ")
