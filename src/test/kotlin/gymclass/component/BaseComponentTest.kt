package gymclass.component

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import gymclass.fixtures.containers.Debezium
import gymclass.fixtures.containers.Kafka
import gymclass.fixtures.containers.Postgres
import gymclass.runServer
import io.helidon.config.Config
import io.helidon.config.ConfigSources
import io.helidon.webserver.WebServer
import io.helidon.webserver.testing.junit5.ServerTest
import io.restassured.RestAssured
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.testcontainers.containers.Network
import java.time.Instant.now
import java.util.UUID

@Tag("component")
@ServerTest
abstract class BaseComponentTest {

    protected lateinit var server: WebServer

    protected val network: Network = Network.newNetwork()

    protected val db = Postgres(network)

    protected val kafka = Kafka(network)

    protected val debezium = Debezium(kafka, db)

    protected val consumer = kafka.buildConsumer().also { it.subscribe(listOf(kafka.topic)) }

    protected val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    protected val externalMemberService = WireMockRule(wireMockConfig().dynamicPort()).also { it.start() }

    @BeforeEach
    fun setUp() {
        val overriddenProperties = mapOf(
            "database.jdbc-url" to db.container.jdbcUrl,
            "database.username" to db.container.username,
            "database.password" to db.container.password,
            "client.members-api.base-url" to externalMemberService.baseUrl(),
            "mp.messaging.connector.helidon-kafka.bootstrap.servers" to kafka.container.bootstrapServers,
        )
        val customConfigSource = ConfigSources.create(overriddenProperties).build()
        val defaultConfigSource = ConfigSources.classpath("application.yaml").build()
        val config: Config = Config.builder().sources(customConfigSource, defaultConfigSource).build()
        RestAssured.baseURI = "http://localhost:8080"

        server = runServer(config)
    }

    protected fun givenAClassExists(id: UUID, capacity: Int, waitingMember: UUID? = null) {
        val waitingList = waitingMember?.let { "'{${it}}'" } ?: "'{}'"
        Jdbi.create(db.container.jdbcUrl, db.container.username, db.container.password).open()
            .execute(
                """ INSERT INTO gym_classes ( id, max_capacity, name, start_time, end_time, created, version, cancelled, waiting_list) 
                VALUES ('$id', $capacity, 'crossfit', '${now()}', '${now()}', '${now()}', 0, false, $waitingList)  """
            )
    }

    protected fun givenABookingExists(classId: UUID, memberId: UUID) =
        Jdbi.create(db.container.jdbcUrl, db.container.username, db.container.password).open()
            .execute(
                """ INSERT INTO gym_class_bookings ( id, gym_class_id, member_id, created) 
                VALUES ('${UUID.randomUUID()}','$classId' ,'$memberId', '${now()}')  """
            )

    @AfterEach
    fun tearDown() {
        server.stop()
        kafka.container.stop()
        db.container.stop()
        debezium.container.stop()
    }

}