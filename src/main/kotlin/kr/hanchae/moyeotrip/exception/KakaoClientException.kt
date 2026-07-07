package kr.hanchae.moyeotrip.exception

class KakaoClientException(errorMessage: String?): BaseException(ErrorCode.KAKAO_CLIENT_EXCEPTION, errorMessage)
