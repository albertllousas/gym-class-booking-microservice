package gymclass.app.usecases

import arrow.core.left
import arrow.core.right
import gymclass.app.domain.ClassBooked
import gymclass.app.domain.ClassId
import gymclass.app.domain.GymClass
import gymclass.app.domain.MemberId
import gymclass.app.domain.OutboundPorts
import gymclass.app.domain.OutboundPortsErrors.GymClassNotFound
import gymclass.fixtures.FakeExecuteUsecase
import gymclass.fixtures.TestBuilders.buildGymClass
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class BookForForWaitingMemberUseCaseTest {

    private val gymClassRepository = mockk<OutboundPorts.GymClassRepository>(relaxed = true)
    private val eventPublisher = mockk<OutboundPorts.EventPublisher>(relaxed = true)

    private val bookForForWaitingMember = BookForForWaitingMemberUseCase(
        gymClassRepository,
        eventPublisher,
        FakeExecuteUsecase,
    )

    @Test
    fun `should orchestrate successfully the booking for a waiting member of a gym class`() {
        val classId = UUID.randomUUID()
        val gymClass = buildGymClass(
            maxCapacity = 1,
            waitingList = listOf(MemberId(UUID.randomUUID())),
        )
        every { gymClassRepository.findBy(ClassId(classId)) } returns gymClass.right()

        val result = bookForForWaitingMember(classId)

        result.isRight() shouldBe true
        verify { eventPublisher.publish(any(ClassBooked::class)) }
        verify { gymClassRepository.save(any(GymClass::class)) }
    }

    @Test
    fun `should fail orchestrating the booking for a waiting member when gym class does not exists`() {
        val classId = UUID.randomUUID()
        every { gymClassRepository.findBy(ClassId(classId)) } returns GymClassNotFound.left()

        val result = bookForForWaitingMember(classId)

        result shouldBe GymClassNotFound.left()
    }

    @Test
    fun `should fail orchestrating the booking for a waiting member when action fails`() {
        val classId = UUID.randomUUID()
        every { gymClassRepository.findBy(ClassId(classId)) } returns buildGymClass().right()

        val result = bookForForWaitingMember(classId)

        result.isLeft() shouldBe true
    }
}
