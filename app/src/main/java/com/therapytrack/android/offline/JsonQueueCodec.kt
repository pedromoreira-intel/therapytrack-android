package com.therapytrack.android.offline

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class JsonQueueCodec<Item>(private val serializer: KSerializer<Item>) : QueueCodec<Item> {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    override fun encode(items: List<Item>): ByteArray = json.encodeToString(ListSerializer(serializer), items).toByteArray()
    override fun decode(bytes: ByteArray): List<Item> = json.decodeFromString(ListSerializer(serializer), bytes.decodeToString())
}
