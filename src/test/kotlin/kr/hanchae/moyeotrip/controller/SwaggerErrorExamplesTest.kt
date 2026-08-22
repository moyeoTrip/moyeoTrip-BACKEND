package kr.hanchae.moyeotrip.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kr.hanchae.moyeotrip.controller.auth.AuthAPISpec
import kr.hanchae.moyeotrip.controller.auth.TestTokenAPISpec
import kr.hanchae.moyeotrip.controller.chat.ChatRoomAPISpec
import kr.hanchae.moyeotrip.controller.chat.request.MyChatRoomFilter
import kr.hanchae.moyeotrip.controller.chat.response.ApprovalResult
import kr.hanchae.moyeotrip.controller.chat.response.JoinResult
import kr.hanchae.moyeotrip.controller.chat.response.LeaveResult
import kr.hanchae.moyeotrip.controller.chat.response.TravelRoadmapProgress
import kr.hanchae.moyeotrip.controller.feed.FeedAPISpec
import kr.hanchae.moyeotrip.controller.feed.request.FeedTab
import kr.hanchae.moyeotrip.controller.notification.NotificationAPISpec
import kr.hanchae.moyeotrip.controller.terms.TermsAPISpec
import kr.hanchae.moyeotrip.controller.tour.TourismContentAPISpec
import kr.hanchae.moyeotrip.controller.tour.TravelCourseAPISpec
import kr.hanchae.moyeotrip.controller.user.FriendAPISpec
import kr.hanchae.moyeotrip.controller.user.TravelCompanionAPISpec
import kr.hanchae.moyeotrip.controller.user.UserAPISpec
import kr.hanchae.moyeotrip.controller.user.UserBlockAPISpec
import kr.hanchae.moyeotrip.entity.chat.ChatMessageType
import kr.hanchae.moyeotrip.entity.chat.ChatParticipantRole
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.chat.GenderRestriction
import kr.hanchae.moyeotrip.entity.chat.JoinApplicationStatus
import kr.hanchae.moyeotrip.entity.chat.JoinApprovalMode
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode
import kr.hanchae.moyeotrip.entity.tour.CoursePublicationStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SwaggerErrorExamplesTest {
    @Nested
    inner class Auth {
        @Test
        fun `회원가입 400은 모든 도메인 검증 오류를 구분한다`() {
            assertEquals(
                setOf("요청 검증 실패", "지원하지 않는 Firebase 제공자", "최소 가입 연령 미달", "닉네임 선택 오류", "사용할 수 없는 약관 포함", "필수 약관 미동의"),
                examples(AuthAPISpec::class.java, "signup", "400"),
            )
        }

        @Test
        fun `카카오 인증 오류와 가입 완료 인증 수단 중복을 구분한다`() {
            assertEquals(
                setOf("유효하지 않은 카카오 access token", "다른 카카오 앱 토큰"),
                examples(AuthAPISpec::class.java, "createKakaoCustomToken", "401"),
            )
            assertTrue(examples(AuthAPISpec::class.java, "signup", "409").contains("이미 가입 완료된 인증 수단"))
        }
    }

    @Nested
    inner class ChatRoom {
        @Test
        fun `참가 신청의 400과 404를 원인별로 구분한다`() {
            assertEquals(
                setOf("요청 본문 검증 실패", "수동 승인 모임의 신청 메시지 누락"),
                examples(ChatRoomAPISpec::class.java, "applyToJoin", "400"),
            )
            assertEquals(
                setOf(40302),
                errorCodes(ChatRoomAPISpec::class.java, "applyToJoin", "403"),
            )
            assertEquals(
                setOf("로그인 사용자 없음", "채팅방 없음"),
                examples(ChatRoomAPISpec::class.java, "applyToJoin", "404"),
            )
        }

        @Test
        fun `채팅 종료와 메시지 없는 방 오류를 문서화한다`() {
            assertTrue(examples(ChatRoomAPISpec::class.java, "applyToJoin", "409").contains("모집 종료"))
            assertEquals(setOf(40407), errorCodes(ChatRoomAPISpec::class.java, "getMyRooms", "404"))
            assertEquals(setOf(40911), errorCodes(ChatRoomAPISpec::class.java, "sendMessage", "409"))
        }

        @Test
        fun `채팅방과 하위 리소스를 함께 찾는 404는 각각 예시를 제공한다`() {
            assertEquals(
                setOf("채팅방 없음", "참가 신청 없음"),
                examples(ChatRoomAPISpec::class.java, "cancelJoinApplication", "404"),
            )
            assertEquals(
                setOf("채팅방 없음", "승인 대기 참가 신청 없음"),
                examples(ChatRoomAPISpec::class.java, "approveApplication", "404"),
            )
            assertEquals(
                setOf("채팅방 없음", "승인 대기 참가 신청 없음"),
                examples(ChatRoomAPISpec::class.java, "rejectApplication", "404"),
            )
            assertEquals(
                setOf("채팅방 없음", "강퇴할 일반 멤버 없음"),
                examples(ChatRoomAPISpec::class.java, "kickMember", "404"),
            )
            assertEquals(
                setOf("채팅방 없음", "공지 없음"),
                examples(ChatRoomAPISpec::class.java, "updateNotice", "404"),
            )
            assertEquals(
                setOf("투표 메시지 없음", "투표 선택지 없음"),
                examples(ChatRoomAPISpec::class.java, "votePoll", "404"),
            )
        }
    }

    @Nested
    inner class Feed {
        @Test
        fun `피드 작성과 댓글 작성의 400과 404를 원인별로 구분한다`() {
            assertEquals(
                setOf("사진 또는 요청 본문 검증 실패", "동일 여행 피드 중복"),
                examples(FeedAPISpec::class.java, "createFeed", "400"),
            )
            assertEquals(
                setOf("로그인 사용자 없음", "채팅방 없음"),
                examples(FeedAPISpec::class.java, "createFeed", "404"),
            )
            assertEquals(
                setOf("피드 없음", "부모 댓글 없음", "로그인 사용자 없음"),
                examples(FeedAPISpec::class.java, "createComment", "404"),
            )
        }
    }

    @Nested
    inner class User {
        @Test
        fun `프로필과 사용자 관계 API의 다중 오류 예시를 구분한다`() {
            assertEquals(
                setOf("요청 본문 또는 선택 ID 오류", "최소 가입 연령 미달"),
                examples(UserAPISpec::class.java, "updateProfile", "400"),
            )
            assertEquals(
                setOf("로그인 사용자 없음", "선택할 프로필 이미지 후보 없음"),
                examples(UserAPISpec::class.java, "selectProfileImage", "404"),
            )
            assertEquals(
                setOf("자기 자신에게 친구 요청", "이미 친구인 사용자", "상대방이 보낸 요청이 이미 있음"),
                examples(FriendAPISpec::class.java, "sendRequest", "400"),
            )
            assertEquals(
                setOf("로그인 사용자 없음", "친구 요청 대상 없음"),
                examples(FriendAPISpec::class.java, "sendRequest", "404"),
            )
            assertEquals(
                setOf("로그인 사용자 없음", "차단 대상 사용자 없음"),
                examples(UserBlockAPISpec::class.java, "block", "404"),
            )
            assertEquals(
                setOf("채팅방 없음", "함께 여행한 동행 기록 없음", "평가할 동행자 없음"),
                examples(TravelCompanionAPISpec::class.java, "reviewCompanion", "404"),
            )
        }
    }

    @Nested
    inner class TravelCourse {
        @Test
        fun `커스텀 코스 수정의 400과 404를 원인별로 구분한다`() {
            assertEquals(
                setOf("요청 본문 검증 실패", "일차별 장소·순서 구성 오류"),
                examples(TravelCourseAPISpec::class.java, "updateRoomCourse", "400"),
            )
            assertEquals(
                setOf("채팅방 없음", "관광 장소 없음"),
                examples(TravelCourseAPISpec::class.java, "updateRoomCourse", "404"),
            )
        }
    }

    @Nested
    inner class DocumentationCompleteness {
        @Test
        fun `공개 API의 모든 메서드에 작업과 응답 문서 및 입력 설명이 있다`() {
            apiSpecs.forEach { apiSpec ->
                apiSpec.declaredMethods.forEach { method ->
                    assertTrue(method.isAnnotationPresent(Operation::class.java), "${apiSpec.simpleName}.${method.name}에 @Operation이 없습니다.")
                    assertTrue(
                        method.isAnnotationPresent(ApiResponses::class.java),
                        "${apiSpec.simpleName}.${method.name}에 @ApiResponses가 없습니다.",
                    )
                    method.parameters
                        .filterNot { parameter -> parameter.getAnnotation(Parameter::class.java)?.hidden == true }
                        .forEach { parameter ->
                            assertTrue(
                                parameter.isAnnotationPresent(Parameter::class.java) ||
                                    parameter.isAnnotationPresent(RequestBody::class.java),
                                "${apiSpec.simpleName}.${method.name}의 ${parameter.name}에 Swagger 입력 설명이 없습니다.",
                            )
                        }
                }
            }
        }

        @Test
        fun `내부 관광지 동기화 오류를 제외한 ErrorCode는 Swagger 오류 예시로 노출한다`() {
            val documentedCodes =
                apiSpecs
                    .flatMap { spec ->
                        spec.declaredMethods
                            .flatMap { method -> method.getAnnotation(ApiResponses::class.java).value.toList() }
                            .flatMap { response -> response.content.flatMap { it.examples.toList() } }
                            .mapNotNull { example ->
                                CODE_PATTERN
                                    .find(example.value)
                                    ?.groupValues
                                    ?.get(1)
                                    ?.toInt()
                            }
                    }.toSet()

            val expectedCodes =
                ErrorCode.entries
                    .filterNot { it == ErrorCode.TOURISM_CONTENT_TYPE_NOT_FOUND }
                    .map { it.code }
                    .toSet()

            assertEquals(expectedCodes, documentedCodes)
        }

        @Test
        fun `공개 요청과 응답에 쓰는 enum은 각 값의 의미를 설명한다`() {
            publicEnums.forEach { enumType ->
                val schema = enumType.getAnnotation(Schema::class.java)

                assertTrue(schema.description.isNotBlank(), "${enumType.simpleName}에 enum 설명이 없습니다.")
                assertTrue(schema.description.contains('='), "${enumType.simpleName}의 값별 의미가 없습니다.")
            }
        }
    }

    private fun examples(
        spec: Class<*>,
        methodName: String,
        responseCode: String,
    ): Set<String> {
        val method = spec.declaredMethods.single { it.name == methodName }
        val responses = method.getAnnotation(ApiResponses::class.java)
        val response = responses.value.single { it.responseCode == responseCode }

        return response.content
            .single()
            .examples
            .map { it.name }
            .toSet()
    }

    private fun errorCodes(
        spec: Class<*>,
        methodName: String,
        responseCode: String,
    ): Set<Int> =
        spec.declaredMethods
            .single { it.name == methodName }
            .getAnnotation(ApiResponses::class.java)
            .value
            .single { it.responseCode == responseCode }
            .content
            .flatMap { it.examples.toList() }
            .mapNotNull { example ->
                CODE_PATTERN
                    .find(example.value)
                    ?.groupValues
                    ?.get(1)
                    ?.toInt()
            }.toSet()

    private companion object {
        val apiSpecs =
            listOf(
                AuthAPISpec::class.java,
                TestTokenAPISpec::class.java,
                ChatRoomAPISpec::class.java,
                FeedAPISpec::class.java,
                NotificationAPISpec::class.java,
                TermsAPISpec::class.java,
                TourismContentAPISpec::class.java,
                TravelCourseAPISpec::class.java,
                FriendAPISpec::class.java,
                TravelCompanionAPISpec::class.java,
                UserAPISpec::class.java,
                UserBlockAPISpec::class.java,
            )

        val publicEnums =
            listOf(
                AgreementTermCode::class.java,
                ApprovalResult::class.java,
                ChatMessageType::class.java,
                ChatNotificationMode::class.java,
                ChatParticipantRole::class.java,
                ChatRoomStatus::class.java,
                CoursePublicationStatus::class.java,
                FeedTab::class.java,
                FeedVisibility::class.java,
                Gender::class.java,
                GenderRestriction::class.java,
                JoinApplicationStatus::class.java,
                JoinApprovalMode::class.java,
                JoinResult::class.java,
                LeaveResult::class.java,
                MyChatRoomFilter::class.java,
                NicknameColor::class.java,
                NotificationType::class.java,
                ProviderType::class.java,
                SignupState::class.java,
                TravelCourseType::class.java,
                TravelRoadmapProgress::class.java,
                TripType::class.java,
            )

        val CODE_PATTERN = Regex("\\\"code\\\":(\\d+)")
    }
}
