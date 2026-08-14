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
    INVALID_AUTH_PROVIDER(HttpStatus.BAD_REQUEST, 40002, "지원하지 않거나 유효하지 않은 Firebase 로그인 제공자입니다."),
    INVALID_NICKNAME_SELECTION(HttpStatus.BAD_REQUEST, 40003, "닉네임 선택이 만료되었거나 발급된 후보와 일치하지 않습니다."),
    INVALID_KAKAO_REDIRECT_URI(HttpStatus.BAD_REQUEST, 40004, "허용되지 않은 카카오 redirect URI입니다."),
    TOURISM_COURSE_CONTENT_NOT_LISTED(HttpStatus.BAD_REQUEST, 40005, "코스 관광 콘텐츠는 여행지 목록에서 조회할 수 없습니다."),

    // UNAUTHORIZED는 40100부터 시작
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40100, "인증되지 않은 사용자입니다."),
    INVALID_FIREBASE_TOKEN(HttpStatus.UNAUTHORIZED, 40101, "유효하지 않은 Firebase ID 토큰입니다."),
    KAKAO_CLIENT_EXCEPTION(HttpStatus.UNAUTHORIZED, 40102, "유효하지 않은 카카오 액세스 토큰입니다."),
    INVALID_KAKAO_APP(HttpStatus.UNAUTHORIZED, 40103, "다른 카카오 애플리케이션에서 발급된 액세스 토큰입니다."),
    INVALID_KAKAO_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, 40104, "유효하지 않거나 만료된 카카오 인가 코드입니다."),

    // FORBIDDEN는 40300부터 시작
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "접근 권한이 없습니다."),
    CHAT_ROOM_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, 40301, "사용자가 채팅방에 참여하고 있지 않습니다."),

    // 리소스 NOT FOUND는 40400부터 시작
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 40400, "해당 유저를 찾을 수 없습니다."),
    PROFILE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "선택할 수 있는 프로필 이미지를 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 40402, "요청한 리소스를 찾을 수 없습니다."),
    TRAVEL_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, 40403, "관리자가 등록한 여행 코스를 찾을 수 없습니다."),
    CHAT_JOIN_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, 40404, "참가 신청을 찾을 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 40405, "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, 40406, "채팅방 멤버를 찾을 수 없습니다."),
    CHAT_ROOM_NO_MESSAGES(HttpStatus.NOT_FOUND, 40407, "채팅방에 메시지가 없습니다."),
    TOURISM_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, 40408, "관광 콘텐츠를 찾을 수 없습니다."),

    // 리소스 충돌은 40900부터 시작
    ALREADY_EXIST_NICKNAME(HttpStatus.CONFLICT, 40900, "이미 사용중인 닉네임입니다."),
    ALREADY_EXIST_PROVIDER_USER_ID(HttpStatus.CONFLICT, 40901, "이미 존재하는 유저입니다."),
    USER_INFO_REQUIRED(HttpStatus.CONFLICT, 40902, "추가 정보 입력이 필요합니다.(닉네임, 성별, 생년월일)"),
    AUTH_IDENTITY_ALREADY_LINKED(HttpStatus.CONFLICT, 40903, "이미 다른 계정으로 가입이 되어있습니다"),
    AUTH_PROVIDER_ALREADY_LINKED(HttpStatus.CONFLICT, 40904, "해당 로그인 제공자가 이미 연결되어 있습니다."),
    CHAT_ROOM_CLOSED(HttpStatus.CONFLICT, 40905, "모집이 종료된 채팅방입니다."),
    CHAT_ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, 40906, "이미 참가했거나 대기 중인 채팅방입니다."),
    CHAT_ROOM_NOT_JOINED(HttpStatus.CONFLICT, 40907, "참가하거나 대기 중인 채팅방이 아닙니다."),
    INVALID_TRAVEL_COURSE_SELECTION(HttpStatus.CONFLICT, 40909, "관리 코스 하나 또는 직접 구성한 코스 중 하나만 선택해야 합니다."),
    INVALID_CHAT_ROOM_STATUS(HttpStatus.CONFLICT, 40910, "변경할 수 없는 여행 상태입니다."),
    CHAT_DISABLED(HttpStatus.CONFLICT, 40911, "종료된 방에서는 채팅할 수 없습니다."),

    // 요청 한도 초과는 42900부터 시작
    PROFILE_IMAGE_GENERATION_LIMIT(HttpStatus.TOO_MANY_REQUESTS, 42900, "프로필 이미지는 사용자당 최대 3번까지 생성할 수 있습니다."),

    // 서버에러는 50000부터 시작
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "서버에러입니다."),
    FIREBASE_AUTH_ERROR(HttpStatus.BAD_GATEWAY, 50200, "Firebase 인증 처리에 실패했습니다."),
    PROFILE_IMAGE_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, 50201, "프로필 이미지 생성에 실패했습니다."),
    KAKAO_AUTH_UNAVAILABLE(HttpStatus.BAD_GATEWAY, 50202, "카카오 인증 서버와 통신하지 못했습니다."),
}
