package gymclass.app.domain

import gymclass.app.domain.UseCaseErrors.BookClassError
import gymclass.app.domain.UseCaseErrors.BookForWaitingMemberError
import gymclass.app.domain.UseCaseErrors.CancelBookingError
import gymclass.app.domain.UseCaseErrors.SubscribeToWaitingListError

sealed interface DomainError

sealed interface UseCaseErrors {

    sealed interface BookClassError : DomainError

    sealed interface SubscribeToWaitingListError : DomainError

    sealed interface CancelBookingError : DomainError

    sealed interface BookForWaitingMemberError : DomainError
}

sealed interface OutboundPortsErrors {

    data object GymClassNotFound : BookClassError, SubscribeToWaitingListError, CancelBookingError, BookForWaitingMemberError

    data object MemberNotFound : BookClassError, SubscribeToWaitingListError, CancelBookingError
}

sealed interface DomainFailures {

    sealed interface BookClassFailure : BookClassError

    data object BookingAlreadyExists : BookClassFailure, BookForWaitingMemberFailure

    data object MaxCapacityReached : BookClassFailure, BookForWaitingMemberFailure

    data object TooLateToBook : BookClassFailure, BookForWaitingMemberFailure


    sealed interface SubscribeToWaitingListFailure : SubscribeToWaitingListError

    data object MaxCapacityNotReached : SubscribeToWaitingListFailure

    data object AlreadySubscribedToClass : SubscribeToWaitingListFailure

    data object AlreadySubscribedToWaitingList : SubscribeToWaitingListFailure


    sealed interface CancelBookingFailure : CancelBookingError

    data object BookingNotFound : CancelBookingFailure

    data object TooLateToCancel : CancelBookingFailure


    sealed interface BookForWaitingMemberFailure : BookForWaitingMemberError

    data object WaitingListEmpty : BookForWaitingMemberFailure
}
