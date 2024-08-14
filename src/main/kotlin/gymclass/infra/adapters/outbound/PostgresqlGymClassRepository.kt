package gymclass.infra.adapters.outbound

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import gymclass.app.domain.Booking
import gymclass.app.domain.BookingId
import gymclass.app.domain.ClassId
import gymclass.app.domain.GymClass
import gymclass.app.domain.MemberId
import gymclass.app.domain.OutboundPorts
import gymclass.app.domain.OutboundPortsErrors.GymClassNotFound
import org.jdbi.v3.core.Jdbi
import java.time.Clock
import java.time.LocalDateTime.now
import java.util.UUID

class PostgresqlGymClassRepository(
    private val jdbi: Jdbi,
    private val getCurrentTransaction: GetCurrentTransaction,
    private val clock: Clock = Clock.systemUTC(),
) : OutboundPorts.GymClassRepository {

    override fun findBy(classId: ClassId): Either<GymClassNotFound, GymClass> =
        jdbi.open().use { handle ->
            val sql = """
                select
                    g.id, g.max_capacity, g.cancelled, g.name, g.start_time, g.end_time, g.version,
                    b.id as booking_id, b.member_id as booking_member_id, g.waiting_list
                from gym_classes g
                left join gym_class_bookings b on g.id = b.gym_class_id
                where g.id = :id
            """
            val result = handle.createQuery(sql)
                .bind("id", classId.value)
                .map { rs, _ ->
                    val gymClass = GymClass(
                        id = ClassId(UUID.fromString(rs.getString("id"))),
                        name = rs.getString("name"),
                        maxCapacity = rs.getInt("max_capacity"),
                        startTime = rs.getTimestamp("start_time").toLocalDateTime(),
                        cancelled = rs.getBoolean("cancelled"),
                        endTime = rs.getTimestamp("end_time").toLocalDateTime(),
                        bookings = emptyList(),
                        version = rs.getLong("version"),
                        waitingList = rs.getArray("waiting_list")?.let { it.array as Array<UUID> }?.map { MemberId(it) }
                            ?: emptyList()
                    )
                    val booking = if (rs.getString("booking_id") != null) {
                        Booking(
                            id = BookingId(UUID.fromString(rs.getString("booking_id"))),
                            memberId = MemberId(UUID.fromString(rs.getString("booking_member_id"))),
                        )
                    } else null
                    Pair(gymClass, booking)
                }
                .list()
            if (result.isNotEmpty()) {
                val gymClass = result[0].first
                val bookings = result.mapNotNull { it.second }
                gymClass.copy(bookings = bookings).right()
            } else {
                GymClassNotFound.left()
            }
        }

    override fun save(gymClass: GymClass) {
        getCurrentTransaction().createQuery(
            """INSERT INTO gym_classes ( id, max_capacity, name, start_time, end_time, created, version, cancelled, waiting_list) 
                VALUES (:id, :max_capacity, :name, :start_time, :end_time, :created, 0, :cancelled, :waiting_list)  
                ON CONFLICT (id) DO UPDATE SET 
                        max_capacity = :max_capacity,
                        name = :name,
                        start_time = :start_time,
                        end_time = :end_time,
                        created = :created,
                        cancelled = :cancelled,
                        waiting_list = :waiting_list,
                        version = gym_classes.version + 1 
                RETURNING version
            """
        )
            .bind("id", gymClass.id.value)
            .bind("max_capacity", gymClass.maxCapacity)
            .bind("cancelled", gymClass.cancelled)
            .bind("name", gymClass.name)
            .bind("start_time", gymClass.startTime)
            .bind("end_time", gymClass.endTime)
            .bind("created", now(clock))
            .bind("waiting_list", gymClass.waitingList.map { it.value }.toTypedArray())
            .mapTo(Long::class.java)
            .one()
            .also { version ->
                if (version > 0 && version != gymClass.version + 1) throw OptimisticLockException(gymClass.id.value)
            }
        getCurrentTransaction().execute(
            "DELETE FROM gym_class_bookings WHERE gym_class_id = ?",
            gymClass.id.value
        )
        gymClass.bookings.forEach { booking ->
            getCurrentTransaction().execute(
                """INSERT INTO gym_class_bookings ( id, gym_class_id, member_id, created )
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                """,
                booking.id.value,
                gymClass.id.value,
                booking.memberId.value,
                now(clock)
            )
        }
    }
}

data class OptimisticLockException(val id: UUID) : Exception("Optimistic lock exception for gym class '$id'")