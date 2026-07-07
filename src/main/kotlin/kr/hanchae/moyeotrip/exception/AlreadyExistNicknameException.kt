package kr.hanchae.moyeotrip.exception

class AlreadyExistNicknameException :
    BaseException(
        ErrorCode.ALREADY_EXIST_NICKNAME,
        "이미 존재하는 닉네임입니다.",
    )
