package kr.hanchae.moyeotrip.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val code: Int,
    val errorMessage: String,
) {
    // 요청을 잘못했을 때는 40000부터 시작
    BAD_REQUEST(HttpStatus.BAD_REQUEST, 40000, "잘못된 요청입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, 40001, "유효하지 않은 RefreshToken 입니다."),

    // UNAUTHORIZED는 40100부터 시작
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40100, "인증되지 않은 사용자입니다."),

    // FORBIDDEN는 40300부터 시작
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "접근 권한이 없습니다."),

    // 리소스 NOT FOUND는 40400부터 시작
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 40400, "해당 유저를 찾을 수 없습니다."),

    // 리소스 충돌은 40900부터 시작
    ALREADY_EXIST_NICKNAME(HttpStatus.CONFLICT, 40900, "이미 사용중인 닉네임입니다."),
    ALREADY_EXIST_PROVIDER_USER_ID(HttpStatus.CONFLICT, 40901, "이미 존재하는 유저입니다."),

    // 요청크기에 대한 에러는 41300부터 시작
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, 41300, "파일 크기가 너무 큽니다."),

    // 서버에러는 50000부터 시작
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "서버에러입니다."),
    KAKAO_CLIENT_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, 50001, "카카오 클라이언트 에러입니다."),
}
