package kr.hanchae.moyeotrip.entity.chat

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TravelCourseTest {
    @Test
    fun `관리자가 구성한 코스에는 호스트가 장소를 추가할 수 없다`() {
        val course = TravelCourse(id = 1L, type = TravelCourseType.MANAGED, title = "관리 코스")

        assertThrows(IllegalStateException::class.java) {
            course.addCustomPlace("임의 장소", null, 1)
        }
    }
}
