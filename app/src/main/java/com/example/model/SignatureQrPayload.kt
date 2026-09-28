package com.example.model

import org.json.JSONObject

/**
 * QR Code payload data format:
 * { "doc": ..., "signer": ..., "ts": ..., "id": ..., "hash": ... }
 */
data class SignatureQrPayload(
    val doc: String,
    val signer: String,
    val ts: Long,
    val id: String,
    val hash: String
) {
    fun toJson(): String {
        return "{\"doc\":\"${escapeJson(doc)}\",\"signer\":\"${escapeJson(signer)}\",\"ts\":$ts,\"id\":\"${escapeJson(id)}\",\"hash\":\"${escapeJson(hash)}\"}"
    }

    fun toPrettyJson(): String {
        return "{\n  \"doc\": \"${escapeJson(doc)}\",\n  \"signer\": \"${escapeJson(signer)}\",\n  \"ts\": $ts,\n  \"id\": \"${escapeJson(id)}\",\n  \"hash\": \"${escapeJson(hash)}\"\n}"
    }

    companion object {
        fun escapeJson(value: String): String {
            return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        fun fromJson(jsonString: String): SignatureQrPayload {
            return try {
                val json = JSONObject(jsonString)
                SignatureQrPayload(
                    doc = json.optString("doc", ""),
                    signer = json.optString("signer", ""),
                    ts = json.optLong("ts", 0L),
                    id = json.optString("id", ""),
                    hash = json.optString("hash", "")
                )
            } catch (e: Throwable) {
                // Fallback regex parser if org.json is not mocked in basic JVM test environments
                parseSimpleJson(jsonString)
            }
        }

        private fun parseSimpleJson(jsonString: String): SignatureQrPayload {
            fun extractField(fieldName: String): String {
                val regex = "\"$fieldName\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1) ?: ""
            }
            fun extractLongField(fieldName: String): Long {
                val regex = "\"$fieldName\"\\s*:\\s*(\\d+)".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            }

            return SignatureQrPayload(
                doc = extractField("doc"),
                signer = extractField("signer"),
                ts = extractLongField("ts"),
                id = extractField("id"),
                hash = extractField("hash")
            )
        }
    }
}
