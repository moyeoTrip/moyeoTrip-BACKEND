package kr.hanchae.moyeotrip.support

import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.oracle.OracleContainer
import org.testcontainers.utility.DockerImageName

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = "moyeotrip-shared-testcontainers", mode = ResourceAccessMode.READ_WRITE)
abstract class ContainerIntegrationTestSupport {
    companion object {
        @JvmStatic
        @ServiceConnection
        val oracle: OracleContainer =
            OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23.26.1-faststart"))
                .withReuse(true)
                .apply { start() }

        @JvmStatic
        @ServiceConnection
        val redis: RedisContainer =
            RedisContainer(DockerImageName.parse("redis:8-alpine"))
                .withReuse(true)
                .apply { start() }
    }
}
