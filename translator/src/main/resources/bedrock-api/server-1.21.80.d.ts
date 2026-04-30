// Minimal Bedrock @minecraft/server type declarations — Phase 3 stub.
//
// PHASE 3 STATUS: This is a hand-curated subset covering only the API surface
// the translator's worked examples reference. It is NOT vendored from the
// real npm package. Phase 4 hardening: replace with the full vendored
// @minecraft/server@<scripting_api_version>.d.ts so the LLM has full type
// fidelity. Phase 3's translations work because the patterns we ask for
// (event subscription, runInterval, dynamic properties, applyDamage) are all
// covered here.
//
// Reference: https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/

declare module "@minecraft/server" {

    /** A point in 3D space. */
    export interface Vector3 { x: number; y: number; z: number; }

    /** Identifies one of Bedrock's scripted dimensions. */
    export class Dimension {
        readonly id: string;
        getEntities(options?: EntityQueryOptions): Entity[];
    }

    /** Filter options for `Dimension.getEntities`. */
    export interface EntityQueryOptions {
        location?: Vector3;
        maxDistance?: number;
        type?: string;
        families?: string[];
        excludeFamilies?: string[];
    }

    /** A live entity in the world. */
    export class Entity {
        readonly id: string;
        readonly typeId: string;
        readonly dimension: Dimension;
        readonly location: Vector3;
        nameTag: string;

        /** Apply damage with optional cause + source attribution. */
        applyDamage(amount: number, options?: EntityApplyDamageOptions): boolean;

        /** Add a status effect (poison, regen, ...) for `duration` ticks. */
        addEffect(
            effectType: string,
            duration: number,
            options?: EntityEffectOptions,
        ): void;

        /** Remove the entity from the world. */
        kill(): boolean;

        /** Read a previously-set dynamic property; undefined if unset. */
        getDynamicProperty(identifier: string): boolean | number | string | Vector3 | undefined;

        /** Persist a small piece of per-entity state across ticks. */
        setDynamicProperty(
            identifier: string,
            value?: boolean | number | string | Vector3,
        ): void;

        /** Direct line-of-sight check against another entity. */
        getEntitiesFromViewDirection(options?: EntityRaycastOptions): EntityRaycastHit[];
    }

    export interface EntityApplyDamageOptions {
        damagingEntity?: Entity;
        cause?: string; // "entityAttack", "magic", "fall", etc.
    }

    export interface EntityEffectOptions {
        amplifier?: number;
        showParticles?: boolean;
    }

    export interface EntityRaycastOptions {
        maxDistance?: number;
    }

    export interface EntityRaycastHit {
        entity: Entity;
        distance: number;
    }

    /** Player-specific extensions. */
    export class Player extends Entity {
        readonly name: string;
        sendMessage(message: string): void;
    }

    /** The single live world. */
    export class World {
        readonly afterEvents: WorldAfterEvents;
        readonly beforeEvents: WorldBeforeEvents;
        getDimension(dimensionId: string): Dimension;
        getAllPlayers(): Player[];
        sendMessage(message: string): void;
    }

    /** Discrete after-events (read-only). */
    export interface WorldAfterEvents {
        entityHurt: EntityHurtAfterEventSignal;
        entityHitEntity: EntityHitEntityAfterEventSignal;
        entityDie: EntityDieAfterEventSignal;
        playerSpawn: PlayerSpawnAfterEventSignal;
        itemUse: ItemUseAfterEventSignal;
    }

    /** Discrete before-events (allow cancellation). */
    export interface WorldBeforeEvents {
        playerLeave: PlayerLeaveBeforeEventSignal;
        itemUse: ItemUseBeforeEventSignal;
    }

    /** Subscription handle for after-events. */
    export interface EventSignal<TEvent, TOptions = never> {
        subscribe(callback: (arg: TEvent) => void, options?: TOptions): (arg: TEvent) => void;
        unsubscribe(callback: (arg: TEvent) => void): void;
    }

    export interface EntityHurtAfterEvent {
        readonly hurtEntity: Entity;
        readonly damageSource: EntityDamageSource;
        readonly damage: number;
    }
    export type EntityHurtAfterEventSignal = EventSignal<EntityHurtAfterEvent>;

    export interface EntityHitEntityAfterEvent {
        readonly damagingEntity: Entity;
        readonly hitEntity: Entity;
    }
    export type EntityHitEntityAfterEventSignal = EventSignal<EntityHitEntityAfterEvent>;

    export interface EntityDieAfterEvent {
        readonly deadEntity: Entity;
        readonly damageSource: EntityDamageSource;
    }
    export type EntityDieAfterEventSignal = EventSignal<EntityDieAfterEvent>;

    export interface PlayerSpawnAfterEvent {
        readonly player: Player;
        readonly initialSpawn: boolean;
    }
    export type PlayerSpawnAfterEventSignal = EventSignal<PlayerSpawnAfterEvent>;

    export interface ItemUseAfterEvent {
        readonly source: Player;
        readonly itemStack: ItemStack;
    }
    export type ItemUseAfterEventSignal = EventSignal<ItemUseAfterEvent>;
    export type ItemUseBeforeEventSignal = EventSignal<ItemUseAfterEvent>;

    export interface PlayerLeaveBeforeEvent {
        readonly player: Player;
    }
    export type PlayerLeaveBeforeEventSignal = EventSignal<PlayerLeaveBeforeEvent>;

    export interface EntityDamageSource {
        readonly cause: string;
        readonly damagingEntity?: Entity;
    }

    export class ItemStack {
        readonly typeId: string;
        amount: number;
        nameTag?: string;
    }

    /** System scheduling — intervals, timeouts, and the per-tick handle. */
    export class System {
        /**
         * Run `callback` every `tickInterval` ticks. Returns a handle that can
         * be passed to `clearRun` to unschedule.
         *
         * PERF: prefer the largest interval that preserves behavior. 20 ticks
         * is one second.
         */
        runInterval(callback: () => void, tickInterval?: number): number;

        /** Run `callback` once, after `tickDelay` ticks. */
        runTimeout(callback: () => void, tickDelay?: number): number;

        /** Run `callback` once on the next tick (for fire-and-forget posts). */
        run(callback: () => void): number;

        clearRun(runId: number): void;

        readonly currentTick: number;
    }

    export const world: World;
    export const system: System;
}
