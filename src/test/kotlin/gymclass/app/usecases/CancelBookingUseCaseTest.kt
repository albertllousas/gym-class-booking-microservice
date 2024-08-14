package gymclass.app.usecases

import arrow.core.left
import arrow.core.right
import gymclass.app.domain.Booking
import gymclass.app.domain.BookingId
import gymclass.app.domain.ClassId
import gymclass.app.domain.GymClass
import gymclass.app.domain.MemberId
import gymclass.app.domain.MemberSubscribedToWaitingList
import gymclass.app.domain.OutboundPorts
import gymclass.app.domain.OutboundPortsErrors.GymClassNotFound
import gymclass.app.domain.OutboundPortsErrors.MemberNotFound
import gymclass.fixtures.FakeExecuteUsecase
import gymclass.fixtures.TestBuilders.buildGymClass
import gymclass.fixtures.TestBuilders.buildMember
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class CancelBookingUseCaseTest {

    private val gymClassRepository = mockk<OutboundPorts.GymClassRepository>(relaxed = true)
    private val memberFinder = mockk<OutboundPorts.MemberFinder>()
    private val eventPublisher = mockk<OutboundPorts.EventPublisher>(relaxed = true)

    private val cancelBooking = CancelBookingUseCase(
        gymClassRepository,
        memberFinder,
        eventPublisher,
        FakeExecuteUsecase,
    )

    @Test
    fun `should orchestrate successfully the cancellation of a booking for a gym class`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val buildGymClass = buildGymClass(
            maxCapacity = 1,
            bookings = listOf(Booking(BookingId(UUID.randomUUID()), MemberId(memberId))),
        )
        every { gymClassRepository.findBy(ClassId(classId)) } returns buildGymClass.right()
        every { memberFinder.findBy(MemberId(memberId)) } returns buildMember(id = MemberId(memberId)).right()

        val result = cancelBooking(classId, memberId)

        result.isRight() shouldBe true
        verify { eventPublisher.publish(any(MemberSubscribedToWaitingList::class)) }
        verify { gymClassRepository.save(any(GymClass::class)) }
    }

    @Test
    fun `should fail orchestrating the cancellation when gym class does not exists`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { gymClassRepository.findBy(ClassId(classId)) } returns GymClassNotFound.left()

        val result = cancelBooking(classId, memberId)

        result shouldBe GymClassNotFound.left()
    }

    @Test
    fun `should fail orchestrating the cancellation when the member does not exists`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { gymClassRepository.findBy(ClassId(classId)) } returns buildGymClass().right()
        every { memberFinder.findBy(MemberId(memberId)) } returns MemberNotFound.left()

        val result = cancelBooking(classId, memberId)

        result shouldBe MemberNotFound.left()
    }
}