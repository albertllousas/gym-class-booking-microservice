package gymclass.infra.adapters.outbound

import gymclass.app.domain.DomainFailures
import gymclass.fixtures.TestBuilders.DomainEvents.buildClassBooked
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import io.micrometer.core.instrument.Tag as MetricTag

@Tag("integration")
class MetricsEventPublisherTest {

    private val metrics = SimpleMeterRegistry()

    private val metricsEventPublisher = MetricsEventPublisher(metrics)

    @Test
    fun `publish a metric for a domain event`() {
        val event = buildClassBooked()

        metricsEventPublisher.publish(event)

        metrics.counter("domain.event", listOf(MetricTag.of("type", "ClassBooked"))).count() shouldBe 1.0
    }

    @Test
    fun `publish a metric for a domain error`() {
        val error = DomainFailures.MaxCapacityReached

        metricsEventPublisher.publish(error, this::class)

        metrics.counter(
            "domain.error",
            listOf(
                MetricTag.of("clazz", "MetricsEventPublisherTest"),
                MetricTag.of("type", "MaxCapacityReached")
            )
        ).count() shouldBe 1.0
    }

    @Test
    fun `publish a metric for an application crash`() {
        val boom = IllegalArgumentException("boom")

        metricsEventPublisher.publish(boom)

        metrics.counter(
            "app.crash",
            listOf(
                MetricTag.of("exception", "IllegalArgumentException")
            )
        ).count() shouldBe 1.0
    }
}
