package kr.hanchae.moyeotrip.exception

class UserNotFoundException(
    userId: Long,
) : BaseException(ErrorCode.USER_NOT_FOUND, "ID($userId)에 해당하는 유저를 찾을 수 없습니다.")
