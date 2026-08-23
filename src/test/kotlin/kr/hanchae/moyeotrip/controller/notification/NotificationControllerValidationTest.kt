package kr.hanchae.moyeotrip.controller.notification

import kr.hanchae.moyeotrip.service.notification.NotificationService
import kr.hanchae.moyeotrip.support.LoginUserIdStubResolver
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class NotificationControllerValidationTest {
    private val notificationService = mock(NotificationService::class.java)
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(NotificationController(notificationService))
            .setCustomArgumentResolvers(LoginUserIdStubResolver())
            .build()

    @Test
    fun `FCM 토큰은 공백일 수 없다`() {
        mockMvc
            .perform(
                put("/api/v1/notifications/fcm-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"fcmToken":" "}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `FCM 토큰은 DB 컬럼 허용 길이를 넘을 수 없다`() {
        val token = "a".repeat(256)
        mockMvc
            .perform(
                put("/api/v1/notifications/fcm-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"fcmToken":"$token"}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(notificationService)
    }
}
