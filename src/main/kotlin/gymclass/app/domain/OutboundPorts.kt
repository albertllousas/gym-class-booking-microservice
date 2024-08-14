package gymclass.app.domain

import arrow.core.Either
import gymclass.app.domain.OutboundPortsErrors.GymClassNotFound
import kotlin.reflect.KClass

object OutboundPorts {

    interface GymClassRepository {
        fun findBy(classId: ClassId): Either<GymClassNotFound, GymClass>
        fun save(gymClass: GymClass)
    }

    interface MemberFinder {
        fun findBy(memberId: MemberId): Either<OutboundPortsErrors.MemberNotFound, Member>
    }

    interface EventPublisher {
        fun publish(event: DomainEvent)
    }

    interface ErrorReporter {

        fun <T : Any> report(error: DomainError, origin: KClass<T>)

        fun report(crash: Throwable)
    }

    interface WithinTransaction {
        operator fun <T> invoke(transactionalBlock: () -> T): T
    }
}
