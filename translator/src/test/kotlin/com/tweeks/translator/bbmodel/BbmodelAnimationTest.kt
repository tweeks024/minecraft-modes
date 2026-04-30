package com.tweeks.translator.bbmodel

import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import com.tweeks.translator.manifest.BedrockTarget
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Synthetic-animation tests for [BbmodelConverter]. None of the real
 * `.bbmodel` files in this repo carry animations yet, so this class
 * exercises the animation code path with hand-crafted JSON fixtures.
 */
class BbmodelAnimationTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val target = BedrockTarget.load(repoRoot.resolve("translator/bedrock-target.json"))

    /**
     * A minimal bbmodel with one bone and one walk animation. The animator
     * targets the bone's uuid and carries position+rotation keyframes at
     * three time points (0, 0.5, 1).
     */
    private val syntheticBbmodel = """
        {
          "name": "walker",
          "model_identifier": "",
          "resolution": { "width": 64, "height": 64 },
          "elements": [
            {
              "uuid": "AAAA",
              "type": "cube",
              "name": "leg_cube",
              "from": [-1, 0, -1],
              "to": [1, 6, 1],
              "origin": [0, 0, 0],
              "uv_offset": [0, 0]
            }
          ],
          "outliner": [
            {
              "name": "left_leg",
              "uuid": "BONE-1",
              "origin": [0, 6, 0],
              "children": ["AAAA"]
            }
          ],
          "animations": [
            {
              "name": "walk",
              "loop": "true",
              "length": 1.0,
              "animators": {
                "BONE-1": {
                  "name": "left_leg",
                  "type": "bone",
                  "keyframes": [
                    {
                      "channel": "rotation",
                      "time": 0.0,
                      "data_points": [{ "x": 0, "y": 0, "z": 0 }]
                    },
                    {
                      "channel": "rotation",
                      "time": 0.5,
                      "data_points": [{ "x": 30, "y": 0, "z": 0 }]
                    },
                    {
                      "channel": "rotation",
                      "time": 1.0,
                      "data_points": [{ "x": 0, "y": 0, "z": 0 }]
                    },
                    {
                      "channel": "position",
                      "time": 0.0,
                      "data_points": [{ "x": 0, "y": 0, "z": 0 }]
                    },
                    {
                      "channel": "position",
                      "time": 0.5,
                      "data_points": [{ "x": 0, "y": 0, "z": -2 }]
                    },
                    {
                      "channel": "position",
                      "time": 1.0,
                      "data_points": [{ "x": 0, "y": 0, "z": 0 }]
                    }
                  ]
                }
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `synthetic walk animation matches golden`() {
        val converter = BbmodelConverter(target, Untranslatable())
        val bb = BbmodelConverter.JSON.decodeFromString(Bbmodel.serializer(), syntheticBbmodel)
        val actual = converter.buildAnimationJson(bb, "testmod", "walker")
        val expected = repoRoot.resolve(
            "translator/src/test/resources/goldens/animation/synthetic_walk.animation.json",
        ).readText()
        assertJsonEquals(expected, actual)
    }

    @Test
    fun `convertOne writes animation file when bbmodel has animations`(@TempDir out: Path) {
        // Drop the synthetic JSON to disk so we can run the public `convertOne` path.
        val toolsDir = out.resolve("input")
        toolsDir.toFile().mkdirs()
        val input = toolsDir.resolve("walker.bbmodel")
        input.toFile().writeText(syntheticBbmodel)

        val converter = BbmodelConverter(target, Untranslatable())
        converter.convertOne(input, "testmod", out)

        val animFile = out.resolve("testmod/resource_pack/animations/walker.animation.json")
        val geoFile = out.resolve("testmod/resource_pack/models/entity/walker.geo.json")
        assertTrue(geoFile.exists()) { "expected geometry file" }
        assertTrue(animFile.exists()) { "expected animation file" }
    }

    @Test
    fun `multi-data-point keyframe records to untranslatable and uses first point`() {
        val unt = Untranslatable()
        val converter = BbmodelConverter(target, unt)
        val multi = """
            {
              "name": "walker",
              "resolution": { "width": 16, "height": 16 },
              "elements": [],
              "outliner": [
                { "name": "head", "uuid": "G1", "origin": [0,0,0], "children": [] }
              ],
              "animations": [
                {
                  "name": "wave",
                  "loop": "true",
                  "length": 1.0,
                  "animators": {
                    "G1": {
                      "name": "head",
                      "type": "bone",
                      "keyframes": [
                        {
                          "channel": "rotation",
                          "time": 0.5,
                          "data_points": [
                            { "x": 10, "y": 0, "z": 0 },
                            { "x": 99, "y": 0, "z": 0 }
                          ]
                        }
                      ]
                    }
                  }
                }
              ]
            }
        """.trimIndent()
        val bb = BbmodelConverter.JSON.decodeFromString(Bbmodel.serializer(), multi)
        val text = converter.buildAnimationJson(bb, "testmod", "walker")
        // First data point's x=10 should be in the output, not 99.
        assertTrue(text.contains("10")) { "expected first datapoint x=10 in: $text" }
        // Untranslatable report should mention the multi-data-point keyframe.
        val report = unt.renderReport("testmod")
        assertTrue(report.contains("multi-data-point keyframes")) { report }
    }

    private fun assertJsonEquals(expected: String, actual: String) {
        val e = Json.parseToJsonElement(expected)
        val a = Json.parseToJsonElement(actual)
        assertEquals(e, a)
    }
}
