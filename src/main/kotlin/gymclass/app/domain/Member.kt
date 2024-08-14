package gymclass.app.domain

import java.util.UUID

data class Member(val id: MemberId)

data class MemberId(val value: UUID)
