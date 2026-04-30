package com.tweeks.translator.manifest

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * Deterministic UUIDv5 generation for Bedrock Add-On manifests.
 *
 * Bedrock identifies installed packs by header UUID. Non-deterministic UUIDs
 * would break existing world saves on every translator rerun, so all UUIDs in
 * emitted manifests are derived from stable inputs (mod id, role) via UUIDv5
 * (RFC 4122 §4.3, SHA-1 namespace+name).
 *
 * Same input → same UUID forever. Stable across reruns, machines, and clones.
 */
object UuidGen {

    /** RFC 4122 well-known DNS namespace UUID. */
    val DNS_NAMESPACE: UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

    /**
     * Project-level namespace for all UUIDs the translator emits.
     * `UUIDv5(DNS_NAMESPACE, "minecraft-mods.tweeks.dev")`.
     */
    val PROJECT_NAMESPACE: UUID = uuidV5(DNS_NAMESPACE, "minecraft-mods.tweeks.dev")

    /** UUID for the pack header (the user-visible pack identity). */
    fun headerUuid(modId: String): UUID = uuidV5(PROJECT_NAMESPACE, "$modId:header")

    /** UUID for the behavior-pack module. */
    fun behaviorModuleUuid(modId: String): UUID = uuidV5(PROJECT_NAMESPACE, "$modId:modules.behavior")

    /** UUID for the resource-pack module. */
    fun resourceModuleUuid(modId: String): UUID = uuidV5(PROJECT_NAMESPACE, "$modId:modules.resource")

    /** Header UUID of the securitycore pack — used by sibling mods that depend on it. */
    fun coreDependencyUuid(): UUID = headerUuid("securitycore")

    /**
     * Compute UUIDv5 per RFC 4122 §4.3:
     *   1. Compute SHA-1 of (namespace bytes ‖ name bytes UTF-8).
     *   2. Take first 16 bytes.
     *   3. Set version bits (octet 6, high nibble = 0x5).
     *   4. Set variant bits (octet 8, top two bits = 10).
     *   5. Reassemble as UUID.
     *
     * `java.util.UUID` has no v5 factory — `nameUUIDFromBytes` produces v3 (MD5).
     */
    fun uuidV5(namespace: UUID, name: String): UUID {
        val sha1 = MessageDigest.getInstance("SHA-1")
        sha1.update(uuidToBytes(namespace))
        sha1.update(name.toByteArray(Charsets.UTF_8))
        val hash = sha1.digest()

        // Truncate SHA-1's 20-byte output to 16 bytes.
        val out = hash.copyOf(16)

        // Set version to 5 in octet 6 (high 4 bits).
        out[6] = ((out[6].toInt() and 0x0F) or 0x50).toByte()
        // Set variant to RFC 4122 in octet 8 (top 2 bits = 10).
        out[8] = ((out[8].toInt() and 0x3F) or 0x80).toByte()

        return bytesToUuid(out)
    }

    private fun uuidToBytes(uuid: UUID): ByteArray {
        val buf = ByteBuffer.allocate(16)
        buf.putLong(uuid.mostSignificantBits)
        buf.putLong(uuid.leastSignificantBits)
        return buf.array()
    }

    private fun bytesToUuid(bytes: ByteArray): UUID {
        require(bytes.size == 16) { "UUID requires exactly 16 bytes, got ${bytes.size}" }
        val buf = ByteBuffer.wrap(bytes)
        val msb = buf.long
        val lsb = buf.long
        return UUID(msb, lsb)
    }
}
