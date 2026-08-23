package kr.hanchae.moyeotrip.controller.notification

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.chat.response.ChatRoomKickHistoryResponse
import kr.hanchae.moyeotrip.controller.notification.request.UpdateChatRoomNotificationSettingRequest
import kr.hanchae.moyeotrip.controller.notification.request.UpdateFcmTokenRequest
import kr.hanchae.moyeotrip.controller.notification.request.UpdateNotificationSettingRequest
import kr.hanchae.moyeotrip.controller.notification.response.ChatRoomNotificationSettingResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationPageResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationSettingResponse
import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.utils.LoginUserId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService,
) : NotificationAPISpec {
    @GetMapping
    override fun getNotifications(
        @LoginUserId userId: Long,
        @RequestParam(required = false) lastId: Long?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") unreadOnly: Boolean,
    ): NotificationPageResponse = notificationService.getNotifications(userId, lastId, size.coerceIn(1, 100), unreadOnly)

    @GetMapping("/{notificationId}/kick-history")
    override fun getKickHistory(
        @LoginUserId userId: Long,
        @PathVariable notificationId: Long,
    ): ChatRoomKickHistoryResponse = notificationService.getKickHistory(userId, notificationId)

    @PutMapping("/{notificationId}/read")
    override fun markRead(
        @LoginUserId userId: Long,
        @PathVariable notificationId: Long,
    ): ResponseEntity<Void> {
        notificationService.markRead(userId, notificationId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/read-all")
    override fun markAllRead(
        @LoginUserId userId: Long,
    ): ResponseEntity<Void> {
        notificationService.markAllRead(userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/fcm-token")
    override fun updateFcmToken(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: UpdateFcmTokenRequest,
    ): ResponseEntity<Void> {
        notificationService.updateFcmToken(userId, request.fcmToken)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/fcm-token")
    override fun deleteFcmToken(
        @LoginUserId userId: Long,
    ): ResponseEntity<Void> {
        notificationService.deleteFcmToken(userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/settings")
    override fun getSetting(
        @LoginUserId userId: Long,
    ): NotificationSettingResponse = notificationService.getSetting(userId)

    @PutMapping("/settings")
    override fun updateSetting(
        @LoginUserId userId: Long,
        @RequestBody request: UpdateNotificationSettingRequest,
    ): NotificationSettingResponse =
        notificationService.updateSetting(
            userId,
            request.chatNotificationMode,
            request.recruitmentDeadlineEnabled,
            request.socialActivityEnabled,
            request.marketingEnabled,
            request.doNotDisturbEnabled,
            request.doNotDisturbStartTime,
            request.doNotDisturbEndTime,
            request.doNotDisturbDays,
        )

    @GetMapping("/settings/chat-rooms/{roomId}")
    override fun getChatRoomSetting(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
    ): ChatRoomNotificationSettingResponse = notificationService.getChatRoomSetting(userId, roomId)

    @PutMapping("/settings/chat-rooms/{roomId}")
    override fun updateChatRoomSetting(
        @LoginUserId userId: Long,
        @PathVariable roomId: Long,
        @RequestBody request: UpdateChatRoomNotificationSettingRequest,
    ): ChatRoomNotificationSettingResponse = notificationService.updateChatRoomSetting(userId, roomId, request.enabled)
}
