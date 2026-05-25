package kr.hanchae.moyeotrip.logging

import kr.hanchae.moyeotrip.utils.MoyeoTripJsonMappers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * [HttpExchangeRepository] 구현 — Spring Boot 가 매 요청마다 add() 호출.
 *
 * - 외부 노출은 안 함 — actuator `httpexchanges` endpoint 는 exposure 에서 제외했고 [HttpExchangesEndpoint
 *   extension] 도 등록 안 함 (영구 Bearer UUID credential leak 위험).
 * - 운영은 표준 출력 → ELK/CloudWatch 로 수집.
 * - 민감 정보는 [TraceManager.getTrace] 단계에서 redact.
 * - synchronized list 의 락 경합을 피해 [ConcurrentLinkedDeque] + size cap.
 */
@Component
class TraceRepository(
    private val traceManager: TraceManager,
) : HttpExchangeRepository {
    private val contents: ConcurrentLinkedDeque<Trace> = ConcurrentLinkedDeque()

    // `ConcurrentLinkedDeque.size` 는 O(N) 순회 — 매 요청마다 size cap 체크하는 경로에서 비용 큼.
    // AtomicInteger 로 O(1) 카운터 유지. concurrent oversize / undersize 1~2 정도는 허용 (cap 목적엔 충분).
    private val sizeCounter: AtomicInteger = AtomicInteger(0)

    private val errorLogger: Logger = LoggerFactory.getLogger("error")
    private val protocolLogger: Logger = LoggerFactory.getLogger("protocol")
    private val matcher = AntPathMatcher()

    override fun add(httpTrace: HttpExchange) {
        traceManager.httpTrace = httpTrace
        val trace = traceManager.getTrace() ?: return

        contents.addLast(trace)
        sizeCounter.incrementAndGet()
        while (sizeCounter.get() > MAX_BUFFER_SIZE) {
            if (contents.pollFirst() != null) {
                sizeCounter.decrementAndGet()
            } else {
                break
            }
        }

        if (traceManager.isErrorLog()) {
            errorLogger.error(MoyeoTripJsonMappers.default.writeValueAsString(trace))
        } else if (!filterTrace(trace)) {
            protocolLogger.info(MoyeoTripJsonMappers.default.writeValueAsString(trace))
        }
    }

    private fun filterTrace(trace: Trace): Boolean = FILTERED_PATHS.any { matcher.match(it, trace.path) }

    override fun findAll(): List<HttpExchange> = Collections.emptyList()

    fun findAllTrace(): List<Trace> = contents.toList().reversed()

    companion object {
        /** 메모리 보호용 buffer 상한. 운영에선 외부 로그 수집(ELK/CloudWatch) 사용 가정. */
        const val MAX_BUFFER_SIZE = 100

        /** trace 에서 protocol 로그를 남기지 않을 path 패턴 — actuator / swagger 류 노이즈 제거. */
        private val FILTERED_PATHS =
            listOf(
                "/actuator/**",
                "/**/swagger-ui/**",
                "/**/swagger-resources/**",
                "/api-docs/**",
            )
    }
}
