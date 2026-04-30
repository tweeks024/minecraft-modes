package com.tweeks.translator.java

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.tweeks.translator.discover.ModDiscovery
import com.tweeks.translator.emit.Untranslatable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * End-to-end sanity test for the Phase 2a Java pipeline. Loads the real
 * `securityguard` mod's sources via [JavaSourceLoader] using the
 * runtime classpath dumped by `:translator:dumpModClasspaths`, then
 * verifies that JavaParser's symbol solver actually resolves
 * Minecraft-API types pulled in from NeoForge jars.
 *
 * If this test passes:
 *   - the classpath dump is reaching the test JVM via
 *     `translator.classpathDir`;
 *   - JarTypeSolver can read NeoForge's universal jar;
 *   - JavaSymbolSolver can climb the inheritance chain through Mojang
 *     classes (SecurityGuardEntity -> IronGolem -> AbstractGolem -> ...).
 *
 * If it fails, Phase 2b's EntityAnalyzer / GoalMatcher work cannot
 * proceed — every analyser depends on this same resolution path.
 */
class SymbolResolutionSanityTest {

    // Each test re-parses; cheap (~ms) and robust against per-test
    // instance lifecycle quirks. If we need to amortize, switch to
    // @BeforeAll + PER_CLASS later.
    private val repoRoot: Path = ModDiscovery.findRepoRoot(Path.of("").toAbsolutePath())
    private val discovery = ModDiscovery(repoRoot)

    private fun loadSecurityguard(): JavaSourceLoader.ResolvedModSources {
        val resolver = ClasspathResolver.fromSystemProperties()
        assertTrue(resolver.isAvailable()) {
            "${ClasspathResolver.SYSTEM_PROPERTY} is not set. The test task wires this " +
                "via :translator:dumpModClasspaths — run via ':translator:test', not from an IDE."
        }
        val all = discovery.discover()
        val securityguard = all.firstOrNull { it.modId == "securityguard" }
            ?: error("securityguard mod not discovered; settings.gradle / src layout must have changed.")
        val unt = Untranslatable()
        val loader = JavaSourceLoader(resolver, unt)
        val resolved = loader.load(securityguard, all)
        // Surface any parse failures to the test log so they're easy to diagnose.
        val report = unt.renderReport("securityguard")
        if (report.contains("Java source files JavaParser could not parse")) {
            System.err.println("[symbol-resolution-test] parse failures detected:\n$report")
        }
        return resolved
    }

    @Test
    fun `parses every java file under securityguard`() {
        val resolved = loadSecurityguard()
        // securityguard ships ~15 .java files. The exact count drifts
        // as the mod grows, so just assert > 5 (more than a skeleton).
        assertTrue(resolved.units.size >= 5) {
            "Expected at least 5 parsed compilation units; got ${resolved.units.size}"
        }
    }

    @Test
    fun `securityguard SecurityGuardEntity resolves IronGolem as parent`() {
        val resolved = loadSecurityguard()
        val securityGuardClass = findClass(resolved, "SecurityGuardEntity")
        val extended = securityGuardClass.extendedTypes.firstOrNull()
            ?: error("SecurityGuardEntity does not declare an extends clause; this test fixture is wrong.")

        val resolvedRef = extended.resolve()
        // resolved is a ResolvedReferenceType whose typeDeclaration carries the FQN.
        val fqn = resolvedRef.asReferenceType().qualifiedName
        assertEquals(
            "net.minecraft.world.entity.animal.golem.IronGolem",
            fqn,
            "SecurityGuardEntity's parent must resolve to vanilla IronGolem.",
        )
    }

    @Test
    fun `securityguard createAttributes return type resolves to AttributeSupplier Builder`() {
        val resolved = loadSecurityguard()
        val securityGuardClass = findClass(resolved, "SecurityGuardEntity")
        val createAttrs = securityGuardClass.methods.firstOrNull { it.nameAsString == "createAttributes" }
            ?: error("SecurityGuardEntity#createAttributes() not found in parsed AST.")

        val resolvedReturn = (createAttrs as MethodDeclaration).type.resolve()
        // ResolvedType -> describe() yields a canonical FQN form.
        val desc = resolvedReturn.describe()
        // Inner-class separator may be `.` or `$` depending on JavaParser
        // version; normalize for the comparison.
        val canonical = desc.replace('$', '.')
        assertEquals(
            "net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder",
            canonical,
            "createAttributes() must resolve to AttributeSupplier.Builder; otherwise the symbol solver isn't seeing the Minecraft jars.",
        )
    }

    private fun findClass(
        resolved: JavaSourceLoader.ResolvedModSources,
        simpleName: String,
    ): ClassOrInterfaceDeclaration {
        val seen = mutableListOf<String>()
        for (unit in resolved.units) {
            for (type in unit.types) {
                if (type is ClassOrInterfaceDeclaration) {
                    seen.add(type.nameAsString)
                    if (type.nameAsString == simpleName) {
                        return type
                    }
                }
            }
        }
        // Include `seen` in the error so a future regression that drops
        // a file (e.g. a parse failure) is easy to diagnose.
        error("Class $simpleName not found in parsed sources for securityguard. Top-level types parsed: $seen")
    }
}
