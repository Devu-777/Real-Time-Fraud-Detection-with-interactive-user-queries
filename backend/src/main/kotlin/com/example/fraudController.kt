package com.example

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.client.HttpClient
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.QueryableStoreTypes
import java.net.URL
import javax.inject.Inject

@Controller("/api")
class FraudController(
    @Inject private val streams: KafkaStreams,
    @Inject private val httpClient: HttpClient // Used to call other nodes
) {

    private val storeName = "swipe-counts"

    @Post("/transactions")
    fun produceTransaction(@Body payload: Map<String, String>): HttpResponse<String> {
        // In a real app, use a KafkaProducer here. For simplicity, assuming it's sent.
        return HttpResponse.accepted()
    }

    @Get("/status/{userId}")
    fun getStatus(userId: String): HttpResponse<Map<String, Any>> {
        // 1. Ask Kafka Streams: "Who owns this user's data?"
        val metadata = streams.queryMetadataForKey(storeName, userId, Serdes.String().serializer())
        
        if (metadata == null || metadata === org.apache.kafka.streams.KeyQueryMetadata.NOT_AVAILABLE) {
            return HttpResponse.serverError(mapOf("error" to "System rebalancing"))
        }

        val activeHost = metadata.activeHost()
        val currentHost = System.getenv("APP_HOST") ?: "localhost"

        // 2. If I own it, query my local RocksDB
        if (activeHost.host() == currentHost) {
            val store = streams.store(storeName, QueryableStoreTypes.windowStore<String, Long>())
            val iterator = store.backwardFetch(userId, 0L, System.currentTimeMillis())
            val count = if (iterator.hasNext()) iterator.next().value else 0L
            
            return HttpResponse.ok(mapOf("userId" to userId, "count" to count, "node" to currentHost))
        } 
        
        // 3. RPC Proxy: If another node owns it, forward the HTTP request to them
        val remoteUrl = "http://${activeHost.host()}:${activeHost.port()}/api/status/$userId"
        val proxyResponse = httpClient.toBlocking().retrieve(remoteUrl, Map::class.java)
        
        return HttpResponse.ok(proxyResponse as Map<String, Any>)
    }
}