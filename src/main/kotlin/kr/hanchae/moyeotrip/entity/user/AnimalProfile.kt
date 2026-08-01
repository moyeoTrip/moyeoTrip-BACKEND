package kr.hanchae.moyeotrip.entity.user

enum class AnimalProfile(
    val word: String,
) {
    DEER("사슴"),
    TURTLE("거북이"),
    RABBIT("토끼"),
    FOX("여우"),
    OTTER("수달"),
    SQUIRREL("다람쥐"),
    CAT("고양이"),
    PUPPY("강아지"),
    PANDA("판다"),
    PENGUIN("펭귄"),
    DOLPHIN("돌고래"),
    OWL("부엉이"),
    SPARROW("참새"),
    ALPACA("알파카"),
    KOALA("코알라"),
    CRANE("두루미"),
    SEA_OTTER("해달"),
    HEDGEHOG("고슴도치"),
    RACCOON("너구리"),
    GIRAFFE("기린"),
    ;

    companion object {
        private val BY_WORD = entries.associateBy(AnimalProfile::word)

        fun fromWord(word: String?): AnimalProfile? = BY_WORD[word]
    }
}
