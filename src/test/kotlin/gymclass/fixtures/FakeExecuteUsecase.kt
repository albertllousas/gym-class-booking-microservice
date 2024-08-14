package gymclass.fixtures

import gymclass.app.usecases.ExecuteUsecase
import io.mockk.mockk

val FakeExecuteUsecase: ExecuteUsecase = ExecuteUsecase.build(
    ExecuteUsecase::class, FakeWithinTransaction, mockk(relaxed = true)
)