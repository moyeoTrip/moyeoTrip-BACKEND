package kr.hanchae.moyeotrip.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class UserWithdrawalDataRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    data class StoredObjectKeys(
        val feedImages: List<String>,
        val chatImages: List<String>,
    ) {
        val all: List<String>
            get() = feedImages + chatImages
    }

    fun removePersonalActivity(
        userId: Long,
        nickname: String?,
        withdrawnAt: LocalDateTime,
    ): StoredObjectKeys {
        snapshotPublicCourseCreatorNickname(userId, nickname)
        delete("DELETE FROM travel_course_likes WHERE user_id = ?", userId)
        markCoursesRetainedByOtherUsers(userId, withdrawnAt)
        cancelHostedRooms(userId, withdrawnAt)

        val storedObjectKeys =
            StoredObjectKeys(
                feedImages = findAuthoredFeedImageKeys(userId),
                chatImages = findSentChatImageKeys(userId),
            )

        delete("DELETE FROM feed_likes WHERE user_id = ?", userId)
        delete("DELETE FROM feed_comments WHERE author_id = ?", userId)
        delete("DELETE FROM feeds WHERE author_id = ?", userId)
        delete("DELETE FROM travel_companions WHERE owner_id = ? OR companion_id = ?", userId, userId)
        delete("DELETE FROM friendships WHERE first_user_id = ? OR second_user_id = ?", userId, userId)
        delete("DELETE FROM friend_requests WHERE requester_id = ? OR receiver_id = ?", userId, userId)
        delete("DELETE FROM user_blocks WHERE blocker_id = ? OR blocked_id = ?", userId, userId)
        delete("DELETE FROM travel_course_ratings WHERE user_id = ?", userId)
        delete("DELETE FROM chat_poll_votes WHERE user_id = ?", userId)
        delete("DELETE FROM chat_message_mentions WHERE user_id = ?", userId)
        delete("DELETE FROM chat_messages WHERE sender_id = ?", userId)
        delete("DELETE FROM chat_room_notices WHERE author_id = ?", userId)
        delete("DELETE FROM chat_room_kick_histories WHERE kicked_user_id = ? OR kicked_by_id = ?", userId, userId)
        delete("DELETE FROM chat_room_notification_settings WHERE user_id = ?", userId)
        delete("DELETE FROM chat_room_favorites WHERE user_id = ?", userId)
        delete("DELETE FROM chat_room_join_applications WHERE user_id = ?", userId)
        delete("DELETE FROM chat_room_participants WHERE user_id = ?", userId)
        delete("DELETE FROM notifications WHERE recipient_id = ?", userId)

        deleteUnreferencedOwnedCourses(userId)
        return storedObjectKeys
    }

    fun preparePermanentDeletion(
        userId: Long,
        nickname: String?,
    ): StoredObjectKeys {
        snapshotPublicCourseCreatorNickname(userId, nickname)
        val roomObjectKeys = findHostedRoomObjectKeys(userId)

        delete("DELETE FROM chat_rooms WHERE host_id = ?", userId)
        delete(
            "DELETE FROM travel_courses WHERE owner_id = ? AND type = 'CUSTOM' " +
                "AND NOT EXISTS (SELECT 1 FROM chat_rooms WHERE chat_rooms.travel_course_id = travel_courses.id)",
            userId,
        )
        delete(
            "DELETE FROM travel_courses WHERE owner_id = ? AND type = 'PUBLIC' " +
                "AND retained_after_owner_withdrawal = 0 " +
                "AND NOT EXISTS (SELECT 1 FROM travel_course_likes WHERE travel_course_likes.travel_course_id = travel_courses.id) " +
                "AND NOT EXISTS (SELECT 1 FROM chat_rooms WHERE chat_rooms.travel_course_id = travel_courses.id)",
            userId,
        )
        jdbcTemplate.update(
            "UPDATE travel_courses SET owner_id = NULL WHERE owner_id = ? AND type = 'PUBLIC'",
            userId,
        )
        return roomObjectKeys
    }

    private fun snapshotPublicCourseCreatorNickname(
        userId: Long,
        nickname: String?,
    ) {
        if (nickname == null) return
        jdbcTemplate.update(
            "UPDATE travel_courses SET creator_nickname = ? " +
                "WHERE owner_id = ? AND type = 'PUBLIC' AND creator_nickname IS NULL",
            nickname,
            userId,
        )
    }

    private fun cancelHostedRooms(
        userId: Long,
        withdrawnAt: LocalDateTime,
    ) {
        jdbcTemplate.update(
            "UPDATE chat_rooms SET status = 'CANCELLED', chat_closed_datetime = ?, deletion_scheduled_date = ? " +
                "WHERE host_id = ? AND status <> 'CANCELLED'",
            Timestamp.valueOf(withdrawnAt),
            Date.valueOf(withdrawnAt.toLocalDate().plusDays(CHAT_ROOM_RETENTION_DAYS)),
            userId,
        )
    }

    private fun markCoursesRetainedByOtherUsers(
        userId: Long,
        withdrawnAt: LocalDateTime,
    ) {
        jdbcTemplate.update(
            "UPDATE travel_courses SET retained_after_owner_withdrawal = 1 " +
                "WHERE owner_id = ? AND type = 'PUBLIC' AND (" +
                "EXISTS (SELECT 1 FROM travel_course_likes " +
                "WHERE travel_course_likes.travel_course_id = travel_courses.id) OR " +
                "EXISTS (SELECT 1 FROM chat_rooms " +
                "WHERE chat_rooms.travel_course_id = travel_courses.id " +
                "AND chat_rooms.status = 'CONFIRMED' " +
                "AND COALESCE(chat_rooms.end_date, chat_rooms.start_date) < ?)" +
                ")",
            userId,
            Date.valueOf(withdrawnAt.toLocalDate()),
        )
    }

    private fun deleteUnreferencedOwnedCourses(userId: Long) {
        delete(
            "DELETE FROM travel_courses WHERE owner_id = ? AND type = 'PUBLIC' " +
                "AND retained_after_owner_withdrawal = 0 " +
                "AND NOT EXISTS (SELECT 1 FROM travel_course_likes WHERE travel_course_likes.travel_course_id = travel_courses.id) " +
                "AND NOT EXISTS (SELECT 1 FROM chat_rooms WHERE chat_rooms.travel_course_id = travel_courses.id)",
            userId,
        )
        delete(
            "DELETE FROM travel_courses WHERE owner_id = ? AND type = 'CUSTOM' " +
                "AND NOT EXISTS (SELECT 1 FROM chat_rooms WHERE chat_rooms.travel_course_id = travel_courses.id)",
            userId,
        )
    }

    private fun findAuthoredFeedImageKeys(userId: Long): List<String> =
        jdbcTemplate
            .queryForList(
                "SELECT feed_images.file_name FROM feed_images " +
                    "JOIN feeds ON feeds.id = feed_images.feed_id WHERE feeds.author_id = ?",
                String::class.java,
                userId,
            ).filterNotNull()

    private fun findSentChatImageKeys(userId: Long): List<String> =
        jdbcTemplate
            .queryForList(
                "SELECT image_url FROM chat_messages WHERE sender_id = ? AND image_url IS NOT NULL",
                String::class.java,
                userId,
            ).filterNotNull()

    private fun findHostedRoomObjectKeys(userId: Long): StoredObjectKeys =
        StoredObjectKeys(
            feedImages =
                jdbcTemplate
                    .queryForList(
                        "SELECT feed_images.file_name FROM feed_images " +
                            "JOIN feeds ON feeds.id = feed_images.feed_id " +
                            "JOIN chat_rooms ON chat_rooms.id = feeds.chat_room_id WHERE chat_rooms.host_id = ?",
                        String::class.java,
                        userId,
                    ).filterNotNull(),
            chatImages =
                jdbcTemplate
                    .queryForList(
                        "SELECT chat_messages.image_url FROM chat_messages " +
                            "JOIN chat_rooms ON chat_rooms.id = chat_messages.chat_room_id " +
                            "WHERE chat_rooms.host_id = ? AND chat_messages.image_url IS NOT NULL",
                        String::class.java,
                        userId,
                    ).filterNotNull(),
        )

    private fun delete(
        sql: String,
        vararg args: Any,
    ) {
        jdbcTemplate.update(sql, *args)
    }

    private companion object {
        const val CHAT_ROOM_RETENTION_DAYS = 14L
    }
}
