package com.tweeks.starwars.world;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private ModStructures() {}

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_TYPE, StarWarsMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
        DeferredRegister.create(Registries.STRUCTURE_PIECE, StarWarsMod.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<EscapePodStructure>> ESCAPE_POD_TYPE =
        STRUCTURE_TYPES.register("escape_pod", () -> () -> EscapePodStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ESCAPE_POD_PIECE =
        STRUCTURE_PIECES.register("escape_pod",
            () -> (StructurePieceType) EscapePodPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<ImperialOutpostStructure>> IMPERIAL_OUTPOST_TYPE =
        STRUCTURE_TYPES.register("imperial_outpost", () -> () -> ImperialOutpostStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> IMPERIAL_OUTPOST_PIECE =
        STRUCTURE_PIECES.register("imperial_outpost",
            () -> (StructurePieceType) ImperialOutpostPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<JediRuinStructure>> JEDI_RUIN_TYPE =
        STRUCTURE_TYPES.register("jedi_ruin", () -> () -> JediRuinStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> JEDI_RUIN_PIECE =
        STRUCTURE_PIECES.register("jedi_ruin",
            () -> (StructurePieceType) JediRuinPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<MoistureFarmStructure>> MOISTURE_FARM_TYPE =
        STRUCTURE_TYPES.register("moisture_farm", () -> () -> MoistureFarmStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> MOISTURE_FARM_PIECE =
        STRUCTURE_PIECES.register("moisture_farm",
            () -> (StructurePieceType) MoistureFarmPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<SandcrawlerStructure>> SANDCRAWLER_TYPE =
        STRUCTURE_TYPES.register("sandcrawler", () -> () -> SandcrawlerStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SANDCRAWLER_PIECE =
        STRUCTURE_PIECES.register("sandcrawler",
            () -> (StructurePieceType) SandcrawlerPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<FerrixTownStructure>> FERRIX_TOWN_TYPE =
        STRUCTURE_TYPES.register("ferrix_town", () -> () -> FerrixTownStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> FERRIX_TOWN_PIECE =
        STRUCTURE_PIECES.register("ferrix_town",
            () -> (StructurePieceType) FerrixTownPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<KraytSkeletonStructure>> KRAYT_SKELETON_TYPE =
        STRUCTURE_TYPES.register("krayt_skeleton", () -> () -> KraytSkeletonStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> KRAYT_SKELETON_PIECE =
        STRUCTURE_PIECES.register("krayt_skeleton",
            () -> (StructurePieceType) KraytSkeletonPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<MosEisleyStructure>> MOS_EISLEY_TYPE =
        STRUCTURE_TYPES.register("mos_eisley", () -> () -> MosEisleyStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> MOS_EISLEY_PIECE =
        STRUCTURE_PIECES.register("mos_eisley",
            () -> (StructurePieceType) MosEisleyPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<YodaHutStructure>> YODA_HUT_TYPE =
        STRUCTURE_TYPES.register("yoda_hut", () -> () -> YodaHutStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> YODA_HUT_PIECE =
        STRUCTURE_PIECES.register("yoda_hut",
            () -> (StructurePieceType) YodaHutPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<XwingWreckStructure>> XWING_WRECK_TYPE =
        STRUCTURE_TYPES.register("xwing_wreck", () -> () -> XwingWreckStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> XWING_WRECK_PIECE =
        STRUCTURE_PIECES.register("xwing_wreck",
            () -> (StructurePieceType) XwingWreckPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<EchoBaseStructure>> ECHO_BASE_TYPE =
        STRUCTURE_TYPES.register("echo_base", () -> () -> EchoBaseStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ECHO_BASE_PIECE =
        STRUCTURE_PIECES.register("echo_base",
            () -> (StructurePieceType) EchoBasePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<WampaCaveStructure>> WAMPA_CAVE_TYPE =
        STRUCTURE_TYPES.register("wampa_cave", () -> () -> WampaCaveStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> WAMPA_CAVE_PIECE =
        STRUCTURE_PIECES.register("wampa_cave",
            () -> (StructurePieceType) WampaCavePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<EwokVillageStructure>> EWOK_VILLAGE_TYPE =
        STRUCTURE_TYPES.register("ewok_village", () -> () -> EwokVillageStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> EWOK_VILLAGE_PIECE =
        STRUCTURE_PIECES.register("ewok_village",
            () -> (StructurePieceType) EwokVillagePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<VaderCastleStructure>> VADER_CASTLE_TYPE =
        STRUCTURE_TYPES.register("vader_castle", () -> () -> VaderCastleStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> VADER_CASTLE_PIECE =
        STRUCTURE_PIECES.register("vader_castle",
            () -> (StructurePieceType) VaderCastlePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<JabbaPalaceStructure>> JABBA_PALACE_TYPE =
        STRUCTURE_TYPES.register("jabba_palace", () -> () -> JabbaPalaceStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> JABBA_PALACE_PIECE =
        STRUCTURE_PIECES.register("jabba_palace",
            () -> (StructurePieceType) JabbaPalacePiece::new);

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
    }
}
