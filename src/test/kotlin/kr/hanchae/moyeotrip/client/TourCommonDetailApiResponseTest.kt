package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TourCommonDetailApiResponseTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `detailCommon2 실제 성공 응답을 역직렬화한다`() {
        val response = objectMapper.readValue<TourCommonDetailApiResponse>(SUCCESS_RESPONSE)
        val item =
            response.response
                ?.body
                ?.items
                ?.item
                ?.single()

        assertNotNull(response.response)
        assertEquals("0000", response.response?.header?.resultCode)
        assertEquals("547853", item?.contentid)
        assertEquals("12", item?.contenttypeid)
        assertEquals("35", item?.areacode)
        assertEquals("9", item?.sigungucode)
        assertEquals("EX030100", item?.lclsSystm3)
        assertEquals("A02030100", item?.cat3)
        assertEquals("https://gosg.my.canva.site", item?.homepage?.substringAfter('>')?.substringBefore('<'))
    }

    companion object {
        private const val SUCCESS_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{"contentid":"547853","contenttypeid":"12","title":"승곡체험휴양마을","createdtime":"20080418181422","modifiedtime":"20251023143000","tel":"","telname":"","homepage":"<a href=\"https://gosg.my.canva.site/sg34/\" target=\"_blank\">https://gosg.my.canva.site</a>","firstimage":"http://tong.visitkorea.or.kr/image.jpg","firstimage2":"http://tong.visitkorea.or.kr/thumbnail.jpg","cpyrhtDivCd":"Type1","areacode":"35","sigungucode":"9","lDongRegnCd":"47","lDongSignguCd":"250","lclsSystm1":"EX","lclsSystm2":"EX03","lclsSystm3":"EX030100","cat1":"A02","cat2":"A0203","cat3":"A02030100","addr1":"경상북도 상주시 낙동면 승곡1길 34","addr2":"","zipcode":"37252","mapx":"128.2100856976","mapy":"36.3656593428","mlevel":"6","overview":"상세 설명","futureField":"추가 필드"}]},"numOfRows":1,"pageNo":1,"totalCount":1}}}"""
    }
}
