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

class SubscribeToWaitingListUseCaseTest {

    private val gymClassRepository = mockk<OutboundPorts.GymClassRepository>(relaxed = true)
    private val memberFinder = mockk<OutboundPorts.MemberFinder>()
    private val eventPublisher = mockk<OutboundPorts.EventPublisher>(relaxed = true)

    private val subscribeToWaitingList = SubscribeToWaitingListUseCase(
        gymClassRepository,
        memberFinder,
        eventPublisher,
        FakeExecuteUsecase,
    )

    @Test
    fun `should orchestrate successfully the subscription to a waiting list of a gym class`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val buildGymClass = buildGymClass(
            maxCapacity = 1,
            bookings = listOf(Booking(BookingId(UUID.randomUUID()), MemberId(UUID.randomUUID()))),
        )
        every { gymClassRepository.findBy(ClassId(classId)) } returns buildGymClass.right()
        every { memberFinder.findBy(MemberId(memberId)) } returns buildMember().right()

        val result = subscribeToWaitingList(classId, memberId)

        result.isRight() shouldBe true
        verify { eventPublisher.publish(any(MemberSubscribedToWaitingList::class)) }
        verify { gymClassRepository.save(any(GymClass::class)) }
    }

    @Test
    fun `should fail orchestrating the subscription to a waiting list when gym class does not exists`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { gymClassRepository.findBy(ClassId(classId)) } returns GymClassNotFound.left()

        val result = subscribeToWaitingList(classId, memberId)

        result shouldBe GymClassNotFound.left()
    }

    @Test
    fun `should fail orchestrating the subscription to a waiting list  when the member does not exists`() {
        val classId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        every { gymClassRepository.findBy(ClassId(classId)) } returns buildGymClass().right()
        every { memberFinder.findBy(MemberId(memberId)) } returns MemberNotFound.left()

        val result = subscribeToWaitingList(classId, memberId)

        result shouldBe MemberNotFound.left()
    }
}
