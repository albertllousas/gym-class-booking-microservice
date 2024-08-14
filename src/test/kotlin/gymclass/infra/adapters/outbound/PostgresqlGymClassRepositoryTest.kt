package gymclass.infra.adapters.outbound

import arrow.core.right
import gymclass.app.domain.Booking
import gymclass.app.domain.BookingId
import gymclass.app.domain.MemberId
import gymclass.fixtures.TestBuilders.buildGymClass
import gymclass.fixtures.containers.Postgres
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Tag("integration")
class PostgresqlGymClassRepositoryTest {

    private val db = Postgres()

    private val jdbi = Jdbi.create(db.container.jdbcUrl, db.container.username, db.container.password)

    private val clock = Clock.fixed(Instant.parse("2020-01-02T00:00:00Z"), ZoneId.of("UTC"))

    private val currentHandle = jdbi.open()

    private val gymClassRepository =
        PostgresqlGymClassRepository(jdbi, getCurrentTransaction = { currentHandle }, clock)

    @AfterEach
    fun `tear down`() {
        db.container.stop()
    }

    @Test
    fun `should save and find an existant find a gym class`() {
        val booking = Booking(BookingId(UUID.randomUUID()), MemberId(UUID.randomUUID()))
        val gymClass = buildGymClass(
            startTime = LocalDateTime.parse("2020-01-02T01:00:00"),
            endTime = LocalDateTime.parse("2020-01-02T02:00:00"),
            bookings = listOf(booking)
        ).also(gymClassRepository::save)

        val result = gymClassRepository.findBy(gymClass.id)

        result shouldBe gymClass.right()
    }

    @Test
    fun `should increment version for an existant gym class when it is saved`() {
        val gymClass = buildGymClass().also(gymClassRepository::save)
        gymClassRepository.findBy(gymClass.id).onRight { gymClassRepository.save(it) }

        val result = gymClassRepository.findBy(gymClass.id)

        result.isRight() shouldBe true
        result.onRight { it.version shouldBe 1 }
    }

    @Test
    fun `should crash when there is a concurrent conflict saving a gym class`() {
        val gymClass = buildGymClass().also(gymClassRepository::save).also(gymClassRepository::save)

        shouldThrow<OptimisticLockException> { gymClassRepository.save(gymClass) }
    }
}
