package gymclass.infra.config

import gymclass.infra.adapters.outbound.LogEventWriter
import gymclass.infra.adapters.outbound.MetricsEventPublisher
import io.micrometer.core.instrument.MeterRegistry

object ObservabilityConfig {

    fun metricsEventPublisher(meterRegistry: MeterRegistry) = MetricsEventPublisher(meterRegistry)

    fun logEventWriter(): LogEventWriter = LogEventWriter()
}
