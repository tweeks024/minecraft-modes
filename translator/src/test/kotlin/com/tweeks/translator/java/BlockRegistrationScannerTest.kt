package com.tweeks.translator.java

import com.github.javaparser.JavaParser
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BlockRegistrationScannerTest {

    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())

    private fun sourcesOf(tmp: Path, vararg srcs: String): JavaSourceLoader.ResolvedModSources {
        val mod = ModDiscovery.DiscoveredMod(modId = "themod", rootDir = tmp.resolve("themodroot"))
        val parser = JavaParser()
        val units = srcs.map { parser.parse(it).result.orElseThrow() }
        return JavaSourceLoader.ResolvedModSources(
            mod = mod,
            units = units,
            typeSolver = CombinedTypeSolver(),
        )
    }

    @Test
    fun `registerBlock with a portal factory class records the portal honesty entry`(@TempDir tmp: Path) {
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object HYPERSPACE_PORTAL = BLOCKS.registerBlock("hyperspace_portal",
                    com.example.themod.world.gate.HyperspacePortalBlock::new,
                    () -> BlockBehaviour.Properties.of().noCollision().strength(-1.0F));
            }
        """.trimIndent()

        val unt = Untranslatable()
        BlockRegistrationScanner(unt).scan("themod", sourcesOf(tmp, registrationSrc))

        val report = unt.renderReport("themod")
        assertTrue(report.contains("## Custom blocks not translated")) { report }
        assertTrue(
            report.contains(
                "- `hyperspace_portal`: custom block 'hyperspace_portal' not translated — " +
                    "translator has no Bedrock block emitter; portal/teleport behavior impossible " +
                    "without a scripting harness (block class HyperspacePortalBlock)",
            ),
        ) { report }
    }

    @Test
    fun `registerSimpleBlock without portal semantics records the plain no-emitter entry`(@TempDir tmp: Path) {
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object CARGO_CRATE = BLOCKS.registerSimpleBlock("cargo_crate",
                    BlockBehaviour.Properties.of().strength(2.0F));
            }
        """.trimIndent()

        val unt = Untranslatable()
        BlockRegistrationScanner(unt).scan("themod", sourcesOf(tmp, registrationSrc))

        val report = unt.renderReport("themod")
        assertTrue(
            report.contains(
                "- `cargo_crate`: custom block 'cargo_crate' not translated — " +
                    "translator has no Bedrock block emitter",
            ),
        ) { report }
        assertFalse(report.contains("portal/teleport behavior")) { report }
        assertFalse(report.contains("block class")) { report }
    }

    @Test
    fun `non-BLOCKS scopes and non-literal ids are ignored`(@TempDir tmp: Path) {
        val registrationSrc = """
            package com.example.themod;
            class Registration {
                public static final Object THING = ITEMS.registerItem("thing", ThingItem::new, p -> p);
                public static final Object SIDE = OTHER.registerBlock("side_block", SideBlock::new);
                public static final Object DYN = BLOCKS.registerBlock(DYNAMIC_ID, DynBlock::new);
            }
        """.trimIndent()

        val unt = Untranslatable()
        BlockRegistrationScanner(unt).scan("themod", sourcesOf(tmp, registrationSrc))

        val report = unt.renderReport("themod")
        assertFalse(report.contains("Custom blocks not translated")) { report }
    }

    @Test
    fun `starwars hyperspace_portal registration is recorded end to end`() {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable())
        val all = ModDiscovery(repoRoot).discover()
        val mod = all.first { it.modId == "starwars" }
        val unt = Untranslatable()
        val sources = JavaSourceLoader(resolver, unt).load(mod, all)
        BlockRegistrationScanner(unt).scan(mod.modId, sources)

        val report = unt.renderReport("starwars")
        assertTrue(
            report.contains(
                "- `hyperspace_portal`: custom block 'hyperspace_portal' not translated — " +
                    "translator has no Bedrock block emitter; portal/teleport behavior impossible " +
                    "without a scripting harness (block class HyperspacePortalBlock)",
            ),
        ) { report }
    }
}
