package kr.hanchae.moyeotrip.exception

open class BaseException(
    val errorCode: ErrorCode,
    message: String? = errorCode.errorMessage,
) : RuntimeException(message)
