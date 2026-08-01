package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.controller.auth.response.NicknameCandidate
import kr.hanchae.moyeotrip.controller.auth.response.NicknameCandidatesResponse
import kr.hanchae.moyeotrip.entity.user.AdjectiveProfile
import kr.hanchae.moyeotrip.entity.user.AnimalProfile
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
        val adjective = AdjectiveProfile.entries[random.nextInt(AdjectiveProfile.entries.size)]
        val animal = AnimalProfile.entries[random.nextInt(AnimalProfile.entries.size)]
        val color = NicknameColor.entries[random.nextInt(NicknameColor.entries.size)]
        val number = random.nextInt(10_000).toString().padStart(4, '0')
        val nickname = "${adjective.word} ${animal.word} $number"
        return NicknameCandidate(
            nickname = nickname,
            adjective = adjective.word,
            animal = animal.word,
            color = color,
            description = adjective.description,
        )
    }

    companion object {
        private const val CANDIDATE_COUNT = 3
        private const val MAX_GENERATION_ATTEMPTS = 1_000
        private val SELECTION_TTL = Duration.ofMinutes(10)
    }
}
