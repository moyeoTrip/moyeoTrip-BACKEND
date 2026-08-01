package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.entity.user.AdjectiveProfile
import kr.hanchae.moyeotrip.entity.user.AnimalProfile
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class ProfileImagePromptFactory {
    fun create(
        nickname: String,
        color: NicknameColor,
    ): String {
        val nicknameParts = nickname.split(" ")
        val personality =
            AdjectiveProfile.fromWord(nicknameParts.getOrNull(0))?.imagePersonality
                ?: "friendly, warm, and adventurous"
        val animal =
            AnimalProfile
                .fromWord(nicknameParts.getOrNull(1))
                ?.name
                ?.lowercase(Locale.ROOT)
                ?.replace('_', ' ')
                ?: "adorable travel mascot animal"
        val dominantColor = color.name.lowercase(Locale.ROOT).replace('_', ' ')
        return listOf(
            "Create a square 1:1 profile avatar illustration for a travel community.",
            "Draw exactly one cute, charming anthropomorphic $animal with a $personality personality.",
            "Use a centered waist-up portrait composition with comfortable margins, showing the head, face, upper torso, and expressive arm pose clearly.",
            "Use a polished, soft, whimsical cartoon illustration style with simple rounded shapes and expressive features, not photorealistic or 3D-rendered.",
            "Use $dominantColor as the dominant color palette, with a clean harmonious background and subtle travel-inspired details.",
            "Keep the composition readable at small profile-image size and avoid clutter.",
            "Do not include any text, letters, digits, captions, speech bubbles, logos, symbols, signatures, or watermarks.",
            "Ignore the numeric suffix in the nickname and focus only on the adjective's personality and the animal species.",
        ).joinToString("\n")
    }
}
