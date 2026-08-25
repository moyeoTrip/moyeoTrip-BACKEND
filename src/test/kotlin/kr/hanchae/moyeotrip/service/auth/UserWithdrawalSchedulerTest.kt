package kr.hanchae.moyeotrip.service.auth

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class UserWithdrawalSchedulerTest {
    private val userService = mock(UserService::class.java)
    private val scheduler = UserWithdrawalScheduler(userService)

    @Test
    fun `매일 복구 기간이 지난 탈퇴 계정을 영구 삭제한다`() {
        `when`(userService.deleteExpiredWithdrawnUsers()).thenReturn(2)

        scheduler.deleteExpiredWithdrawnUsers()

        verify(userService).deleteExpiredWithdrawnUsers()
    }
}
