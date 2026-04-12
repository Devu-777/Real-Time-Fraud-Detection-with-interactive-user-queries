package com.example

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.WindowedSerdes
import java.time.Duration
import jakarta.inject.Singleton
import org.apache.kafka.streams.kstream.Produced.with

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
            .to("fraud-alerts", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String::class.java), Serdes.String()))
    }
}