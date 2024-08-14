plugins {
    alias(libs.plugins.jvm)
    application
}

object Versions {
    const val HELIDON = "4.0.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.helidon.common:helidon-common:${Versions.HELIDON}")
    implementation("io.helidon.webserver:helidon-webserver:${Versions.HELIDON}")
    implementation("io.helidon.http.media:helidon-http-media-jackson:${Versions.HELIDON}")
    implementation("io.helidon.metrics:helidon-metrics:${Versions.HELIDON}")
    implementation("io.helidon.messaging.kafka:helidon-messaging-kafka:${Versions.HELIDON}")
    implementation("io.helidon.metrics:helidon-metrics:${Versions.HELIDON}")
    implementation("io.helidon.webserver.observe:helidon-webserver-observe-metrics:${Versions.HELIDON}")
    implementation("io.helidon.metrics.providers:helidon-metrics-providers-micrometer:${Versions.HELIDON}")
    implementation("io.helidon.integrations.micrometer:helidon-integrations-micrometer:${Versions.HELIDON}")
    implementation("io.helidon.webclient:helidon-webclient:${Versions.HELIDON}")
    implementation("io.helidon.http.media:helidon-http-media-jackson:${Versions.HELIDON}")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("io.arrow-kt:arrow-core:2.0.0-alpha.2")
    implementation("org.jdbi:jdbi3-core:3.45.1")
    implementation("org.jdbi:jdbi3-kotlin:3.45.1")
    implementation("org.flywaydb:flyway-core:9.22.3")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.postgresql:postgresql:42.7.1")

    testImplementation("io.helidon.webserver.testing.junit5:helidon-webserver-testing-junit5:4.0.8")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.0")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:kafka:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("io.debezium:debezium-testing-testcontainers:2.4.1.Final")
    testImplementation("com.github.tomakehurst:wiremock:3.0.1")
    testImplementation("io.rest-assured:rest-assured:5.5.0")

}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "gymclass.RunnerKt"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
