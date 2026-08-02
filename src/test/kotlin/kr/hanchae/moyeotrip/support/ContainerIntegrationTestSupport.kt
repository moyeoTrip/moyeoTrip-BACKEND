package kr.hanchae.moyeotrip.support

import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.utility.DockerImageName

@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
abstract class ContainerIntegrationTestSupport {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val oracle: OracleContainer =
            OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23.26.1-faststart"))
                .withReuse(true)

        @JvmStatic
        @Container
        @ServiceConnection
        val redis: RedisContainer =
            RedisContainer(DockerImageName.parse("redis:8-alpine"))
                .withReuse(true)
    }
}
