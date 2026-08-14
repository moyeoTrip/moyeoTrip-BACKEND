package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.notification.Notification
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findAllByRecipientIdAndIdLessThanOrderByIdDesc(
        recipientId: Long,
        id: Long,
        pageable: Pageable,
    ): List<Notification>

    fun findAllByRecipientIdAndReadDateTimeIsNullAndIdLessThanOrderByIdDesc(
        recipientId: Long,
        id: Long,
        pageable: Pageable,
    ): List<Notification>

    fun findByIdAndRecipientId(
        id: Long,
        recipientId: Long,
    ): Notification?

    fun findAllByRecipientIdAndReadDateTimeIsNull(recipientId: Long): List<Notification>

    fun countByRecipientIdAndReadDateTimeIsNull(recipientId: Long): Long

    fun existsByRecipientIdAndTypeAndReferenceId(
        recipientId: Long,
        type: NotificationType,
        referenceId: Long,
    ): Boolean
}
