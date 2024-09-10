package gymclass.app.domain

import arrow.core.left
import arrow.core.right
import gymclass.app.domain.DomainFailures.*
import gymclass.fixtures.TestBuilders.buildGymClass
import gymclass.fixtures.TestBuilders.buildMember
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class GymClassTest {

    private val clock = Clock.fixed(Instant.parse("2020-01-02T00:00:00Z"), ZoneId.of("UTC"))

    private val fixedBookingId = BookingId(UUID.randomUUID())

    private val generateBookingId = { fixedBookingId }

    @Nested
    inner class Booking {

        @Test
        fun `should book a class`() {
            val gymClass = buildGymClass(
                maxCapacity = 10,
                startTime = LocalDateTime.parse("2020-01-03T00:00:00"),
                clock = clock,
                generateBookingId = generateBookingId
            )
            val member = buildMember()

            val result = gymClass.bookFor(member)

            result shouldBe ClassBooked(
                on = LocalDateTime.parse("2020-01-02T00:00:00"),
                booking = Booking(fixedBookingId, member.id),
                gymClass = gymClass.copy(bookings = listOf(Booking(fixedBookingId, member.id)))
            ).right()
        }

        @Test
        fun `should fail booking a class if a booking for this member already exists`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                maxCapacity = 10,
                clock = clock,
                startTime = LocalDateTime.parse("2020-01-03T00:00:00"),
                bookings = listOf(Booking(fixedBookingId, member.id)),
                generateBookingId = generateBookingId
            )

            val result = gymClass.bookFor(member)

            result shouldBe BookingAlreadyExists.left()
        }

        @Test
        fun `should fail booking a class if max capacity has been reached`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                maxCapacity = 1,
                clock = clock,
                startTime = LocalDateTime.parse("2020-01-03T00:00:00"),
                bookings = listOf(Booking(fixedBookingId, MemberId(UUID.randomUUID()))),
                generateBookingId = generateBookingId
            )

            val result = gymClass.bookFor(member)

            result shouldBe MaxCapacityReached.left()
        }

        @Test
        fun `should fail booking a class if it already started`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                startTime = LocalDateTime.parse("2020-01-02T00:00:00"),
                maxCapacity = 10,
                clock = clock
            )

            val result = gymClass.bookFor(member)

            result shouldBe TooLateToBook.left()
        }

        @Test
        fun `should fail booking a class if it already ended`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                clock = clock,
                startTime = LocalDateTime.parse("2020-01-01T00:00:00"),
                endTime = LocalDateTime.parse("2020-01-01T01:00:00"),
                maxCapacity = 10,
            )

            val result = gymClass.bookFor(member)

            result shouldBe TooLateToBook.left()
        }
    }

    @Nested
    inner class SubscribeToWaitingList {

        @Test
        fun `should subscribe to waiting list`() {
            val gymClass = buildGymClass(
                maxCapacity = 1,
                clock = clock,
                bookings = listOf(Booking(BookingId(UUID.randomUUID()), MemberId(UUID.randomUUID()))),
            )
            val member = buildMember()

            val result = gymClass.subscribeToWaitingList(member)

            result shouldBe MemberSubscribedToWaitingList(
                on = LocalDateTime.parse("2020-01-02T00:00:00"),
                member = member,
                gymClass = gymClass.copy(waitingList = listOf(member.id))
            ).right()
        }

        @Test
        fun `should fail subscribing to waiting list if member has booked the class`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                maxCapacity = 1,
                bookings = listOf(Booking(BookingId(UUID.randomUUID()), member.id)),
            )

            val result = gymClass.subscribeToWaitingList(member)

            result shouldBe AlreadySubscribedToClass.left()
        }

        @Test
        fun `should fail subscribing to waiting list if max capacity has not been reached`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                maxCapacity = 10,
                clock = clock
            )

            val result = gymClass.subscribeToWaitingList(member)

            result shouldBe MaxCapacityNotReached.left()
        }

        @Test
        fun `should fail subscribing to waiting list if member is already subscribed`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                maxCapacity = 1,
                bookings = listOf(Booking(BookingId(UUID.randomUUID()), MemberId(UUID.randomUUID()))),
                waitingList = listOf(member.id)
            )

            val result = gymClass.subscribeToWaitingList(member)

            result shouldBe AlreadySubscribedToWaitingList.left()
        }
    }

    @Nested
    inner class CancelBooking {

        @Test
        fun `should cancel booking`() {
            val member = buildMember()
            val bookingId = BookingId(UUID.randomUUID())
            val gymClass = buildGymClass(
                clock = clock,
                bookings = listOf(Booking(bookingId, member.id))
            )

            val result = gymClass.cancelBookingFor(member)

            result shouldBe BookingCancelled(
                on = LocalDateTime.parse("2020-01-02T00:00:00"),
                booking = Booking(bookingId, member.id),
                gymClass = gymClass.copy(bookings = emptyList())
            ).right()
        }

        @Test
        fun `should fail cancelling booking if booking does not exists`() {
            val member = buildMember()
            val gymClass = buildGymClass(
                clock = clock,
                bookings = emptyList()
            )

            val result = gymClass.cancelBookingFor(member)

            result shouldBe BookingNotFound.left()
        }

        @Test
        fun `should fail cancelling booking if class already started`() {
            val member = buildMember()
            val bookingId = BookingId(UUID.randomUUID())
            val gymClass = buildGymClass(
                clock = clock,
                startTime = LocalDateTime.parse("2020-01-01T00:00:00"),
                bookings = listOf(Booking(bookingId, member.id))
            )

            val result = gymClass.cancelBookingFor(member)

            result shouldBe TooLateToCancel.left()
        }
    }

    @Nested
    inner class TryToBookForMemberInWaitingList {

        @Test
        fun `should book for waiting member`() {
            val memberId = MemberId(UUID.randomUUID())
            val gymClass = buildGymClass(
                clock = clock,
                maxCapacity = 1,
                waitingList = listOf(memberId),
                generateBookingId = generateBookingId
            )

            val result = gymClass.tryTobookForMemberInWaitingList()

            result shouldBe ClassBooked(
                on = LocalDateTime.parse("2020-01-02T00:00:00"),
                booking = Booking(fixedBookingId, memberId),
                promotedFromWaitingList = true,
                gymClass = gymClass.copy(
                    bookings = listOf(Booking(fixedBookingId, memberId)),
                    waitingList = emptyList()
                )
            ).right()
        }

        @Test
        fun `should fail booking for waiting member if waiting list is empty`() {
            val gymClass = buildGymClass(
                clock = clock,
                maxCapacity = 1,
                waitingList = emptyList()
            )

            val result = gymClass.tryTobookForMemberInWaitingList()

            result shouldBe WaitingListEmpty.left()
        }

        @Test
        fun `should fail booking for waiting member if max capacity has been reached`() {
            val memberId = MemberId(UUID.randomUUID())
            val gymClass = buildGymClass(
                clock = clock,
                maxCapacity = 0,
                waitingList = listOf(memberId)
            )

            val result = gymClass.tryTobookForMemberInWaitingList()

            result shouldBe MaxCapacityReached.left()
        }
    }
}