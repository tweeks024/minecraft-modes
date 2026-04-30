package com.tweeks.translator.json

import kotlinx.serialization.json.Json

/**
 * Shared pretty-printing [Json] singleton for every JSON-pipeline transform.
 *
 * Centralizing this guarantees byte-identical formatting across every emitted
 * file in `bedrock-out/`. If a transform genuinely needs a different config,
 * leave it inline with a comment explaining why.
 */
internal object JsonFormat {
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    val PRETTY: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }
}
