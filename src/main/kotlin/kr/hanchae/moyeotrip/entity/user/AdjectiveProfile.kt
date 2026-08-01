package kr.hanchae.moyeotrip.entity.user

enum class AdjectiveProfile(
    val word: String,
    val description: String,
    val imagePersonality: String,
) {
    WARM("따스한", "처음 만난 사람에게도 다정하게 말을 건네요", "warm-hearted, gentle, and welcoming"),
    FAST("빠른", "빠른 템포로 여행을 알차게 즐겨요", "quick, energetic, and lively"),
    RELAXED("느긋한", "서두르지 않고 여행의 순간을 천천히 음미해요", "relaxed, calm, and easygoing"),
    BRAVE("용감한", "낯선 장소와 새로운 경험에도 씩씩하게 도전해요", "brave, confident, and adventurous"),
    KIND("다정한", "함께하는 사람을 세심하게 살피고 마음을 나눠요", "kind, affectionate, and considerate"),
    EXCITED("신나는", "어디서든 즐거움을 발견해 여행 분위기를 북돋아요", "excited, cheerful, and energetic"),
    RADIANT("빛나는", "자신만의 취향으로 여행의 특별한 순간을 만들어요", "radiant, optimistic, and inspiring"),
    SPIRITED("씩씩한", "예상치 못한 상황도 긍정적으로 헤쳐 나가요", "spirited, resilient, and positive"),
    COZY("포근한", "편안한 분위기로 동행에게 안정감을 줘요", "cozy, comforting, and peaceful"),
    CHEERFUL("명랑한", "밝은 에너지로 여행 내내 웃음을 더해요", "bright, joyful, and sociable"),
    CALM("차분한", "꼼꼼하게 살피며 여유로운 여행을 즐겨요", "composed, thoughtful, and serene"),
    CUTE("귀여운", "소소한 장면에도 호기심을 보이며 즐거워해요", "adorable, playful, and curious"),
    JOYFUL("즐거운", "매 순간 좋은 점을 찾아 행복하게 여행해요", "happy, upbeat, and fun-loving"),
    FRESH("산뜻한", "가볍고 상쾌한 기분으로 새로운 하루를 시작해요", "fresh, lighthearted, and refreshing"),
    RELIABLE("든든한", "계획과 준비를 챙겨 동행이 믿고 의지할 수 있어요", "reliable, prepared, and reassuring"),
    CLEVER("영리한", "상황에 맞는 좋은 선택으로 여행을 효율적으로 즐겨요", "clever, resourceful, and observant"),
    SHY("수줍은", "조용히 주변을 관찰하며 천천히 여행에 스며들어요", "shy, gentle, and quietly curious"),
    NIMBLE("재빠른", "좋은 기회를 놓치지 않고 민첩하게 움직여요", "nimble, alert, and enthusiastic"),
    HAPPY("행복한", "작은 경험에서도 기쁨을 발견하고 함께 나눠요", "happy, grateful, and warm"),
    FREE_SPIRITED("자유로운", "정해진 틀보다 마음이 이끄는 대로 여행해요", "free-spirited, curious, and adventurous"),
    ;

    companion object {
        private val BY_WORD = entries.associateBy(AdjectiveProfile::word)

        fun fromWord(word: String?): AdjectiveProfile? = BY_WORD[word]
    }
}
