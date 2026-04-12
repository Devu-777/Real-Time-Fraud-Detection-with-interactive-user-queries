package com.example

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import java.time.Duration
import javax.inject.Singleton

@Singleton
class FraudStream {
    fun buildTopology(builder: StreamsBuilder) {
        builder.stream("raw-transactions", Consumed.with(Serdes.String(), Serdes.String()))
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.`as`("swipe-counts"))
            // If count >= 3, it's fraud
            .toStream()
            .filter { _, count -> count >= 3 }
            .mapValues { _, _ -> "BLOCKED" }
            .to("fraud-alerts", Produced.with(Serdes.WindowedSerdes.timeWindowedSerdeFrom(String::class.java), Serdes.String()))
    }
}