# Family-filter targeting (cross-mod entities)

Java marker interfaces like `com.tweeks.securitycore.api.SecurityAlly` and
`com.tweeks.securitycore.api.SecurityHostile` have no Bedrock equivalent. The
translator replaces them with **type-family tags** on the Bedrock entity JSON.

Whenever entity Java code does `instanceof SecurityHostile` (or similar) for
**targeting** purposes, the Bedrock translation is a family filter on the
relevant `minecraft:behavior.*` component. Use this pattern verbatim — do NOT
hard-code mod-specific entity ids, since the family is the cross-mod join key.

## Family tags emitted by the translator

| Java marker                                          | Bedrock family       |
|------------------------------------------------------|----------------------|
| `com.tweeks.securitycore.api.SecurityAlly`           | `security_ally`      |
| `com.tweeks.securitycore.api.SecurityHostile`        | `security_hostile`   |

Both tags appear in `minecraft:type_family.family` on the Bedrock entity JSON
emitted by `EntityAnalyzer`, alongside the entity id and `mob` / `monster`.

## Pattern: target nearest attackable enemy by family

**Java source:**
```java
this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
    this, Mob.class, 10, true, false,
    (target, level) -> target instanceof SecurityHostile sh && sh.isCurrentlyHostile()
));
```

**Bedrock equivalent (component on the targeting entity's JSON):**
```json
"minecraft:behavior.nearest_attackable_target": {
  "priority": 2,
  "within_radius": 16,
  "must_see": true,
  "entity_types": [
    {
      "filters": {
        "test": "is_family",
        "subject": "other",
        "value": "security_hostile"
      },
      "max_dist": 16
    }
  ]
}
```

Notes:
- `subject: "other"` is the candidate target. `"self"` would test the
  *targeting* entity, which is not what we want.
- `value` must match a family tag the translator emits — see the table above.
- The `isCurrentlyHostile()` runtime gate (e.g. a disguised Thief) does NOT
  have a family-filter equivalent. Use a `dataDrivenEntityTrigger` or a
  dynamic-property check inside the targeting JS to suppress targeting while
  the would-be target is in its non-hostile state. Alternatively, swap the
  family on/off via an event when state changes:
  - Bedrock supports adding/removing family tags via component-group events:
    define one component group with `security_hostile` and one without,
    and trigger between them when the entity's "hostile" state flips.

## Pattern: never target an ally

**Java source:**
```java
target -> !(target instanceof SecurityAlly)
```

**Bedrock equivalent:** add a negated `is_family` filter:
```json
"minecraft:behavior.nearest_attackable_target": {
  "entity_types": [
    {
      "filters": {
        "all_of": [
          { "test": "is_family", "subject": "other", "value": "monster" },
          { "test": "is_family", "subject": "other", "value": "security_ally", "operator": "not" }
        ]
      },
      "max_dist": 16
    }
  ]
}
```

`"operator": "not"` inverts an `is_family` test in Bedrock filter grammar.

## When NOT to use families

If the Java check is on a concrete sibling-mod class (e.g. `instanceof
ThiefEntity`) for targeting, prefer the family tag the translator emits for
that entity (`thief` is automatically in `minecraft:type_family.family` via
the entity id) over the explicit `minecraft:thief` typeId. Family tags survive
mod-id renames and are the cross-mod-correct way to reference enemies.
