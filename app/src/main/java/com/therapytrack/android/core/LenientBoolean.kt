package com.therapytrack.android.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A boolean the server may send as `true`/`false` (Postgres) or `1`/`0`
 * (the SQLite development path). A strict Boolean field silently emptied the
 * whole journal and conversation on the dev server, and a queued entry the
 * server had accepted was re-sent because its response would not decode.
 */
object LenientBoolean : KSerializer<Boolean> {
    override val descriptor = PrimitiveSerialDescriptor("LenientBoolean", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
    override fun deserialize(decoder: Decoder): Boolean {
        val json = decoder as? JsonDecoder ?: return decoder.decodeBoolean()
        val p = json.decodeJsonElement().jsonPrimitive
        return p.booleanOrNull ?: (p.intOrNull?.let { it != 0 } ?: false)
    }
}
