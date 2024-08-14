package gymclass.fixtures

import gymclass.app.domain.Booking
import gymclass.app.domain.BookingId
import gymclass.app.domain.ClassBooked
import gymclass.app.domain.ClassId
import gymclass.app.domain.GymClass
import gymclass.app.domain.Member
import gymclass.app.domain.MemberId
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

val DEFAULT_MEMBER_ID = UUID.randomUUID()

object TestBuilders {

    fun buildGymClass(
        id: ClassId = ClassId(UUID.randomUUID()),
        name: String = "CrossFit",
        maxCapacity: Int = 10,
        bookings: List<Booking> = emptyList(),
        waitingList: List<MemberId> = emptyList(),
        startTime: LocalDateTime = LocalDateTime.now().plusDays(1),
        endTime: LocalDateTime = LocalDateTime.now().plusDays(1).plusHours(1),
        version: Long = 0,
        clock: Clock = Clock.systemUTC(),
        generateBookingId: () -> BookingId = GymClass.GENERATE_BOOKING_ID
    ) = GymClass(
        id = id,
        name = name,
        maxCapacity = maxCapacity,
        startTime = startTime,
        endTime = endTime,
        bookings = bookings,
        waitingList = waitingList,
        clock = clock,
        generateBookingId = generateBookingId,
        version = version
    )

    fun buildMember(id: MemberId = MemberId(UUID.randomUUID())) = Member(id = id)

    object DomainEvents {

        fun buildClassBooked(
            on: LocalDateTime = LocalDateTime.now(),
            booking: Booking = Booking(BookingId(UUID.randomUUID()), MemberId(UUID.randomUUID())),
            gymClass: GymClass = buildGymClass()
        ) =
            ClassBooked(
                on = LocalDateTime.now(),
                booking = Booking(BookingId(UUID.randomUUID()), MemberId(UUID.randomUUID())),
                gymClass = buildGymClass()
            )
    }
}
