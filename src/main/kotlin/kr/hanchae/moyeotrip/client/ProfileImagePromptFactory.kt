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
        userId: Long,
        generationNumber: Int,
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
        val scene = selectScene(userId, generationNumber)

        return """
            Create one finished square profile illustration for the MoyeoTrip travel community.

            STYLE REFERENCE
            - Use the attached image only as the canonical reference for character anatomy, facial language, rounded proportions, smooth digital-gouache texture, soft edge treatment, and polished storybook finish.
            - Do not copy its species, clothing, accessories, number of characters, pose, composition, or background.
            - Ignore and completely replace the attached image's background.

            CHARACTER
            - Draw exactly one cute anthropomorphic $animal with a $personality personality.
            - Show one complete compact body, including both ears, hands, and feet, with a large rounded head, short limbs, glossy dark eyes, tiny nose and mouth, and soft cheek blush.
            - Express the personality through the face and a simple welcoming pose. Keep the natural fur color recognizable.
            - Give the character simple cozy clothing or one clear main accessory. No prescribed hiking outfit is required.

            COLOR IDENTITY
            - Requested identity color: ${color.imageColorName}.
            - Exact palette: primary ${color.imagePrimaryHex}, shadow ${color.imageShadowHex}, highlight ${color.imageHighlightHex}.
            - The requested color must cover at least 60% of the main clothing or primary visible accessory and remain unmistakable at thumbnail size.
            - Repeat the identity color in only two or three small environmental accents. Keep natural fur and skin colors unchanged.
            - Stay inside this hue family; do not turn it into a neighboring color, muddy neutral, pastel wash, fluorescent color, or neon color.

            COMPOSITION
            - Square 1:1 canvas. Center the character and make it occupy about 58-64% of the canvas height.
            - Keep at least 10% clear space around the silhouette. Make the face and identity color readable as a small circular avatar crop.

            ORIGINAL BACKGROUND FOR THIS CANDIDATE
            - Scene: $scene.
            - Build a fresh background independently from the reference image, with a clear foreground, midground, and distant layer.
            - Keep the scenery charming and recognizable but less detailed and lower-contrast than the character.

            RENDERING
            - Polished 2D digital storybook mascot illustration with smooth digital gouache, rounded shapes, controlled texture, warm soft lighting, and clean color separation.
            - Do not use rough watercolor paper, sketch lines, photorealism, 3D rendering, plastic toy styling, anime styling, or flat vector icon styling.
            - Do not include text, letters, digits, captions, speech bubbles, logos, signatures, borders, frames, or watermarks.
            """.trimIndent()
    }

    private fun selectScene(
        userId: Long,
        generationNumber: Int,
    ): String {
        val seed = userId.hashCode() * SCENE_SEED_MULTIPLIER + generationNumber
        return BACKGROUND_SCENES[Math.floorMod(seed, BACKGROUND_SCENES.size)]
    }

    companion object {
        private const val SCENE_SEED_MULTIPLIER = 31

        private val BACKGROUND_SCENES =
            listOf(
                "a peaceful early-autumn lakeside walking path with reeds and distant layered hills",
                "a bright spring wildflower meadow with a gently winding path and soft green mountains",
                "a quiet forest trail beside a clear shallow stream with filtered morning light",
                "a breezy coastal path overlooking a calm blue sea, low cliffs, and distant islands",
                "a snowy field with rounded footprints, bare trees, and pale layered mountains",
                "a warm sunset hill with tall grass, a winding path, and small trees in the distance",
                "a calm riverside promenade with stepping stones, willow branches, and hazy hills",
                "a cheerful orchard path with scattered blossoms, low stone walls, and distant farmland",
                "a misty dawn trail through soft pine woods with a small wooden signpost",
                "a quiet moonlit flower field with a curved path and layered dark-blue mountains",
            )
    }
}
