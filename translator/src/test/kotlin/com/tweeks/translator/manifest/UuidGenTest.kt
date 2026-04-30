package com.tweeks.translator.manifest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class UuidGenTest {

    /**
     * RFC 4122 §A.5 reference test vector for UUIDv5 in the DNS namespace:
     *   v5(DNS, "www.example.com") = 2ed6657d-e927-568b-95e1-2665a8aea6a2
     * Cross-checked against Python's `uuid.uuid5(uuid.NAMESPACE_DNS, "www.example.com")`.
     */
    @Test
    fun `rfc test vector — v5(DNS, www_example_com)`() {
        val expected = UUID.fromString("2ed6657d-e927-568b-95e1-2665a8aea6a2")
        val actual = UuidGen.uuidV5(UuidGen.DNS_NAMESPACE, "www.example.com")
        assertEquals(expected, actual)
    }

    @Test
    fun `dns namespace constant equals canonical RFC 4122 value`() {
        assertEquals(
            UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
            UuidGen.DNS_NAMESPACE,
        )
    }

    @Test
    fun `same inputs produce same uuid (determinism)`() {
        val a = UuidGen.headerUuid("securityguard")
        val b = UuidGen.headerUuid("securityguard")
        assertEquals(a, b)

        val ns1 = UuidGen.PROJECT_NAMESPACE
        val ns2 = UuidGen.PROJECT_NAMESPACE
        assertEquals(ns1, ns2)
    }

    @Test
    fun `different mods produce different header uuids`() {
        val a = UuidGen.headerUuid("securityguard")
        val b = UuidGen.headerUuid("thief")
        assert(a != b) { "Distinct mod ids must yield distinct header UUIDs (got $a == $b)" }
    }

    @Test
    fun `header behavior and resource module uuids are all distinct for the same mod`() {
        val header = UuidGen.headerUuid("securityguard")
        val behavior = UuidGen.behaviorModuleUuid("securityguard")
        val resource = UuidGen.resourceModuleUuid("securityguard")
        val set = setOf(header, behavior, resource)
        assertEquals(3, set.size, "All three role UUIDs must be distinct")
    }

    @Test
    fun `version and variant bits are set per RFC 4122`() {
        val u = UuidGen.uuidV5(UuidGen.PROJECT_NAMESPACE, "anything")
        // version nibble in `time_hi_and_version` should be 5
        assertEquals(5, u.version())
        // variant should be RFC 4122 (the IETF variant), encoded as 2 in java.util.UUID
        assertEquals(2, u.variant())
    }
}
