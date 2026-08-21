package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.ChatPollOption
import kr.hanchae.moyeotrip.entity.chat.ChatPollVote
import org.springframework.data.jpa.repository.JpaRepository

interface ChatPollOptionRepository : JpaRepository<ChatPollOption, Long> {
    fun findAllByMessageIdOrderBySequenceAsc(messageId: Long): List<ChatPollOption>

    fun findByIdAndMessageId(
        id: Long,
        messageId: Long,
    ): ChatPollOption?
}

interface ChatPollVoteRepository : JpaRepository<ChatPollVote, Long> {
    fun findAllByMessageId(messageId: Long): List<ChatPollVote>

    fun findByMessageIdAndUserId(
        messageId: Long,
        userId: Long,
    ): ChatPollVote?
}
