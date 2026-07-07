package kr.hanchae.moyeotrip.exception

class AlreadyExistedProviderUserIdException :
    BaseException(
        ErrorCode.ALREADY_EXIST_PROVIDER_USER_ID,
        "이미 존재하는 유저입니다.",
    )
