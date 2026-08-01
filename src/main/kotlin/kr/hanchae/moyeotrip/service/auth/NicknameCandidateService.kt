package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.controller.auth.response.NicknameCandidate
import kr.hanchae.moyeotrip.controller.auth.response.NicknameCandidatesResponse
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.repository.NicknameCandidateRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.util.UUID

@Service
class NicknameCandidateService(
    private val userRepository: UserRepository,
    private val nicknameCandidateRepository: NicknameCandidateRepository,
) {
    private val random = SecureRandom()

    fun generateCandidates(): NicknameCandidatesResponse {
        val candidates = linkedMapOf<String, NicknameCandidate>()
        var attempts = 0
        while (candidates.size < CANDIDATE_COUNT && attempts < MAX_GENERATION_ATTEMPTS) {
            attempts++
            val candidate = generateNickname()
            if (!userRepository.existsByInformationNickname(candidate.nickname)) {
                candidates[candidate.nickname] = candidate
            }
        }
        check(candidates.size == CANDIDATE_COUNT) { "고유 닉네임 후보를 생성하지 못했습니다." }

        val selectionToken = UUID.randomUUID().toString()
        nicknameCandidateRepository.save(
            selectionToken,
            candidates.values.associate { it.nickname to it.color },
            SELECTION_TTL,
        )
        return NicknameCandidatesResponse(
            selectionToken = selectionToken,
            candidates = candidates.values.toList(),
            expiresInSeconds = SELECTION_TTL.seconds,
        )
    }

    private fun generateNickname(): NicknameCandidate {
        val adjective = ADJECTIVES[random.nextInt(ADJECTIVES.size)]
        val animal = ANIMALS[random.nextInt(ANIMALS.size)]
        val color = NicknameColor.entries[random.nextInt(NicknameColor.entries.size)]
        val number = random.nextInt(10_000).toString().padStart(4, '0')
        val nickname = "${adjective.word} $animal $number"
        return NicknameCandidate(
            nickname = nickname,
            adjective = adjective.word,
            animal = animal,
            color = color,
            description = adjective.description,
        )
    }

    companion object {
        private const val CANDIDATE_COUNT = 3
        private const val MAX_GENERATION_ATTEMPTS = 1_000
        private val SELECTION_TTL = Duration.ofMinutes(10)

        private val ADJECTIVES =
            listOf(
                AdjectiveProfile("따스한", "처음 만난 사람에게도 다정하게 말을 건네요"),
                AdjectiveProfile("빠른", "빠른 템포로 여행을 알차게 즐겨요"),
                AdjectiveProfile("느긋한", "서두르지 않고 여행의 순간을 천천히 음미해요"),
                AdjectiveProfile("용감한", "낯선 장소와 새로운 경험에도 씩씩하게 도전해요"),
                AdjectiveProfile("다정한", "함께하는 사람을 세심하게 살피고 마음을 나눠요"),
                AdjectiveProfile("신나는", "어디서든 즐거움을 발견해 여행 분위기를 북돋아요"),
                AdjectiveProfile("빛나는", "자신만의 취향으로 여행의 특별한 순간을 만들어요"),
                AdjectiveProfile("씩씩한", "예상치 못한 상황도 긍정적으로 헤쳐 나가요"),
                AdjectiveProfile("포근한", "편안한 분위기로 동행에게 안정감을 줘요"),
                AdjectiveProfile("명랑한", "밝은 에너지로 여행 내내 웃음을 더해요"),
                AdjectiveProfile("차분한", "꼼꼼하게 살피며 여유로운 여행을 즐겨요"),
                AdjectiveProfile("귀여운", "소소한 장면에도 호기심을 보이며 즐거워해요"),
                AdjectiveProfile("즐거운", "매 순간 좋은 점을 찾아 행복하게 여행해요"),
                AdjectiveProfile("산뜻한", "가볍고 상쾌한 기분으로 새로운 하루를 시작해요"),
                AdjectiveProfile("든든한", "계획과 준비를 챙겨 동행이 믿고 의지할 수 있어요"),
                AdjectiveProfile("영리한", "상황에 맞는 좋은 선택으로 여행을 효율적으로 즐겨요"),
                AdjectiveProfile("수줍은", "조용히 주변을 관찰하며 천천히 여행에 스며들어요"),
                AdjectiveProfile("재빠른", "좋은 기회를 놓치지 않고 민첩하게 움직여요"),
                AdjectiveProfile("행복한", "작은 경험에서도 기쁨을 발견하고 함께 나눠요"),
                AdjectiveProfile("자유로운", "정해진 틀보다 마음이 이끄는 대로 여행해요"),
            )
        private val ANIMALS =
            listOf(
                "사슴",
                "거북이",
                "토끼",
                "여우",
                "수달",
                "다람쥐",
                "고양이",
                "강아지",
                "판다",
                "펭귄",
                "돌고래",
                "부엉이",
                "참새",
                "알파카",
                "코알라",
                "두루미",
                "해달",
                "고슴도치",
                "너구리",
                "기린",
            )

        private data class AdjectiveProfile(
            val word: String,
            val description: String,
        )
    }
}
