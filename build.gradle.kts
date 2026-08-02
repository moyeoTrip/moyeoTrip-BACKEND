plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("com.google.cloud.tools.jib") version "3.5.3"
}

group = "kr.hanchae"
version = "0.0.1-SNAPSHOT"
description = "MoyeoTrip"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

jib {
    from {
        image = "eclipse-temurin:21-jre"
        platforms {
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "ghcr.io/moyeotrip/moyeotrip_backend:latest"
        auth {
            username = System.getenv("DOCKER_USERNAME") ?: ""
            password = System.getenv("DOCKER_PASSWORD") ?: ""
        }
    }
    container {
        environment = mapOf("TZ" to "Asia/Seoul")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.0"))
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // OAuth2 & JWT
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Secret & Config
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.1.0"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-parameter-store:4.0.2")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap:4.3.2")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.3")

    // Infrastructures
    runtimeOnly("com.oracle.database.jdbc:ojdbc11:23.26.1.0.0")
    implementation("org.redisson:redisson-spring-boot-starter:4.3.1")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:4.0.2")
    runtimeOnly("org.flywaydb:flyway-database-oracle")

    // Documents
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Dynamic Query (kotlin-jdsl — 공식 BOM 미발행, 변수로 통일)
    val kotlinJdslVersion = "3.9.0"
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:$kotlinJdslVersion")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-boot4-support:$kotlinJdslVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-oracle-free")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    //Firebase
    implementation("com.google.firebase:firebase-admin:9.8.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
}
