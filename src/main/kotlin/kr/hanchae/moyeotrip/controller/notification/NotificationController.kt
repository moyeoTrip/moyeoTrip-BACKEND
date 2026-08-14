package kr.hanchae.moyeotrip.controller.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.notification.response.NotificationPageResponse
import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "알림", description = "모임 및 채팅 알림 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) {
    @Operation(summary = "내 알림 목록")
    @GetMapping
    fun getNotifications(
        @LoginUserId userId: Long,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") unreadOnly: Boolean,
    ): NotificationPageResponse = notificationService.getNotifications(userId, cursor, size.coerceIn(1, 100), unreadOnly)

    @Operation(summary = "알림 읽음 처리")
    @PutMapping("/{notificationId}/read")
    fun markRead(
        @LoginUserId userId: Long,
        @PathVariable notificationId: Long,
    ): ResponseEntity<Void> {
        notificationService.markRead(userId, notificationId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "알림 모두 읽음 처리")
    @PutMapping("/read-all")
    fun markAllRead(
        @LoginUserId userId: Long,
    ): ResponseEntity<Void> {
        notificationService.markAllRead(userId)
        return ResponseEntity.noContent().build()
    }
}
