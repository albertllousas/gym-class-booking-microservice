package gymclass.infra.adapters.outbound

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import gymclass.app.domain.Member
import gymclass.app.domain.MemberId
import gymclass.app.domain.OutboundPorts
import gymclass.app.domain.OutboundPortsErrors.MemberNotFound
import io.helidon.webclient.api.WebClient
import java.util.UUID


class ExternalHttpMemberFinder(private val client: WebClient) : OutboundPorts.MemberFinder {

    override fun findBy(memberId: MemberId): Either<MemberNotFound, Member> =
        client.get()
            .path("/members/${memberId.value}")
            .request()
            .let {
                when (it.status().code()) {
                    200 -> it.entity().`as`(MemberHttpResponse::class.java).let { m -> Member(MemberId(m.memberId)) }
                        .right()

                    404 -> MemberNotFound.left()
                    else -> throw HttpCallNonSucceededException(
                        "ExternalHttpMemberFinder", it.entity().`as`(String::class.java), it.status().code()
                    )
                }
            }
}

data class MemberHttpResponse(val memberId: UUID, val fullName: String)

data class HttpCallNonSucceededException(
    val httpClient: String,
    val errorBody: String?,
    val httpStatus: Int
) : RuntimeException("Http call with '$httpClient' failed with status '$httpStatus' and body '$errorBody' ")
