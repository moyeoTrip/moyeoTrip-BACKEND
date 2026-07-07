package kr.hanchae.moyeotrip.exception

class InvalidRefreshTokenException :
    BaseException(
        ErrorCode.INVALID_REFRESH_TOKEN,
        ErrorCode.INVALID_REFRESH_TOKEN.errorMessage,
    )
