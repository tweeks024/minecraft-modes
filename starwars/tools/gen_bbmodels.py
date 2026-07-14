#!/usr/bin/env python3
"""Generate Blockbench .bbmodel source files for the starwars mobs.

A .bbmodel is JSON. We programmatically build the modded_entity model with
the standard humanoid skeleton (head, body, arms, legs) plus per-mob
accessory cubes, or — for mobs with a fully custom skeleton — a cube table
that replaces the humanoid cubes outright. The generated .bbmodel files are
valid Blockbench projects — open them in Blockbench (File -> Open) for
visual editing.

Coord system: Blockbench's modded_entity uses world coords where y=0 is
feet and y=32 is top of head. The conversion from Java HumanoidModel
addBox() coords is:
    bbmodel_from = (bone_x + jx, bone_y - (jy + jh), bone_z + jz)
    bbmodel_to   = (bone_x + jx + jw, bone_y - jy, bone_z + jz + jd)
(Y is inverted because HumanoidModel measures cube y from bone-pivot-down.)

Re-run after editing the script with:
    python3 starwars/tools/gen_bbmodels.py starwars/tools/
"""
import base64
import json
import os
import sys
import uuid

# Deterministic UUIDs so re-runs produce byte-identical output.
NAMESPACE = uuid.UUID('00000000-0000-0000-0000-000000000001')

def det_uuid(name):
    return str(uuid.uuid5(NAMESPACE, name))

# Bones for a vanilla HumanoidModel. (origin = bone pivot in world coords)
HEAD_BONE  = (0,    24, 0)
BODY_BONE  = (0,    24, 0)
RARM_BONE  = (-5,   22, 0)
LARM_BONE  = (5,    22, 0)
RLEG_BONE  = (-1.9, 12, 0)
LLEG_BONE  = (1.9,  12, 0)

# Standard humanoid cubes: (name, parent_bone, java_addBox_args, uv_offset)
# java_addBox_args = (jx, jy, jz, jw, jh, jd)
HUMANOID_CUBES = [
    ('head',      HEAD_BONE,  (-4, -8, -4, 8, 8, 8),  (0, 0)),
    ('body',      BODY_BONE,  (-4,  0, -2, 8, 12, 4), (16, 16)),
    ('right_arm', RARM_BONE,  (-3, -2, -2, 4, 12, 4), (40, 16)),
    ('left_arm',  LARM_BONE,  (-1, -2, -2, 4, 12, 4), (32, 48)),
    ('right_leg', RLEG_BONE,  (-2,  0, -2, 4, 12, 4), (0, 16)),
    ('left_leg',  LLEG_BONE,  (-2,  0, -2, 4, 12, 4), (16, 48)),
]

# Per-mob accessory cubes. Each entry is the same shape as HUMANOID_CUBES,
# optionally with a 5th "inflate" element (default 0.0).
STORMTROOPER_ACCESSORIES = [
    # (name, parent_bone, (jx, jy, jz, jw, jh, jd), uv, inflate)
    ('helmet_shell', HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8), (32, 0), 0.6),
    ('chin_vent',    HEAD_BONE, (-1.5, -1.5, -5.0, 3, 2, 1), (56, 16), 0.0),
]

# Battle droid REPLACES the humanoid cubes (custom skeleton).
BATTLE_DROID_CUBES = [
    ('head',      HEAD_BONE, (-2.0, -6.0, -2.0, 4, 6, 4),  (0, 0)),
    ('snout',     HEAD_BONE, (-1.0, -3.0, -5.0, 2, 2, 3),  (16, 0)),
    ('body',      BODY_BONE, (-3.0,  0.0, -1.5, 6, 10, 3), (0, 16)),
    ('hip_block', BODY_BONE, (-1.0, 10.0, -1.0, 2, 2, 2),  (18, 16)),
    ('right_arm', RARM_BONE, (-1.0, -1.0, -1.0, 2, 12, 2), (32, 16)),
    ('left_arm',  LARM_BONE, (-1.0, -1.0, -1.0, 2, 12, 2), (40, 16)),
    ('right_leg', RLEG_BONE, (-1.0,  0.0, -1.0, 2, 12, 2), (48, 16)),
    ('left_leg',  LLEG_BONE, (-1.0,  0.0, -1.0, 2, 12, 2), (56, 16)),
]

JEDI_KNIGHT_ACCESSORIES = [
    ('robe_skirt', BODY_BONE, (-4.5, 12.0, -2.5, 9, 7, 5), (32, 32), 0.0),
    ('hood',       HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8), (32, 0), 0.5),
]

DARTH_VADER_ACCESSORIES = [
    ('helmet_dome',  HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8),  (32, 0), 0.7),
    ('helmet_flare', HEAD_BONE, (-5.0, -2.0, -5.0, 10, 2, 10),(32, 16), 0.0),
    ('cape',         BODY_BONE, (-4.5,  0.0,  2.1, 9, 20, 1), (44, 32), 0.0),
    ('chest_panel',  BODY_BONE, (-2.0,  3.0, -2.6, 4, 3, 1),  (56, 54), 0.0),
]

BOBA_FETT_ACCESSORIES = [
    ('helmet_shell', HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8),  (32, 0), 0.6),
    ('rangefinder',  HEAD_BONE, (3.8, -12.0, -0.5, 1, 4, 1),  (56, 16), 0.0),
    ('jetpack',      BODY_BONE, (-3.0,  0.5,  2.1, 6, 8, 3),  (44, 32), 0.0),
]

# Han Solo: black vest as an inflated layer over the torso's upper half.
# The vest is the silhouette feature (spec §3.5).
HAN_SOLO_ACCESSORIES = [
    ('vest', BODY_BONE, (-4.0, 0.0, -2.0, 8, 8, 4), (32, 32), 0.25),
]

# Princess Leia: side hair buns modeled as geometry (mandatory silhouette
# feature, spec §4.4) + the Jedi-style robe skirt for the senatorial gown.
PRINCESS_LEIA_ACCESSORIES = [
    ('bun_right',  HEAD_BONE, (-5.5, -5.0, -1.5, 2, 3, 3), (54, 0), 0.0),
    ('bun_left',   HEAD_BONE, ( 3.5, -5.0, -1.5, 2, 3, 3), (54, 6), 0.0),
    ('robe_skirt', BODY_BONE, (-4.5, 12.0, -2.5, 9, 7, 5), (32, 32), 0.0),
]

# Astromech droid: fully custom skeleton (body/head/legs only, no arms) at
# its own pivots — the exact PartPose.offset values from AstromechModel.java
# Step 3 — rather than the standard humanoid bone table above.
ASTRO_BODY_BONE = (0, 24, 0)
ASTRO_HEAD_BONE = (0, 10, 0)
ASTRO_RLEG_BONE = (-5, 12, 0)
ASTRO_LLEG_BONE = (5, 12, 0)

# (bone_name, origin) pairs, in outliner order — passed as build_bbmodel's
# bone_defs override since astromech's bone set/pivots don't match the
# standard humanoid HEAD_BONE/BODY_BONE/.../LLEG_BONE table above.
ASTROMECH_BONE_DEFS = [
    ('body', ASTRO_BODY_BONE),
    ('head', ASTRO_HEAD_BONE),
    ('right_leg', ASTRO_RLEG_BONE),
    ('left_leg', ASTRO_LLEG_BONE),
]

ASTROMECH_CUBES = [
    ('body',      ASTRO_BODY_BONE, (-4.0, -14.0, -4.0, 8, 10, 8), (0, 20)),
    ('head',      ASTRO_HEAD_BONE, (-4.0,  -4.0, -4.0, 8, 4, 8),  (0, 0)),
    ('eye_lens',  ASTRO_HEAD_BONE, (-1.5,  -3.0, -4.5, 3, 2, 1),  (32, 0)),
    ('right_leg', ASTRO_RLEG_BONE, (-1.0,   0.0, -1.5, 2, 12, 3), (32, 20)),
    ('left_leg',  ASTRO_LLEG_BONE, (-1.0,   0.0, -1.5, 2, 12, 3), (42, 20)),
]

# Landspeeder: single-bone static vehicle (no limb animation, no arms/legs at
# all — unlike astromech's body/head/legs skeleton, this is a single 'body'
# bone). Origin at entity center, Java model-space coords (y-down from the
# bone-pivot plane, same java_to_bbmodel conversion as every other mob).
#
# Y-COORDINATE DEVIATION FROM BRIEF: the brief's jy values (hull 17.0, nose
# 18.0, windshield 13.0, seats 19.0, turbine_c 15.0, turbine_l/r 16.0) put
# the model's lowest point (max jy+jh = hull/nose's 22.0) at
# bbmodel_from_y = 24 - 22.0 = 2.0 — i.e. the hull's underside would render
# 2.0 blocks above the entity origin, not the ~0.1 the brief's own geometry
# caution calls for. Verified against the ASTROMECH_CUBES donor: its legs
# (ASTRO_RLEG_BONE=(-5,12,0), java jy=0.0/jh=12) work out to
# bbmodel_from_y = 12 - (0+12) = 0 — i.e. astromech's feet sit exactly at
# the entity origin (y=0), confirming bbmodel y=0 is the ground-contact
# plane the caution means by "entity origin". To match that convention here,
# every LANDSPEEDER_CUBES jy below is the brief's jy + 1.9 (dimensions W/H/D
# and all other values unchanged) — this shifts the lowest point (hull/nose
# underside) from bbmodel y=2.0 down to bbmodel y=0.1, satisfying the "~0.1
# blocks above the entity origin" requirement while preserving every cube's
# relative position (windshield still sits flush on the hull's top, seats
# and turbines still recess into the hull by the same margins as the brief).
SPEEDER_BONE = (0, 24, 0)

LANDSPEEDER_BONE_DEFS = [
    ('body', SPEEDER_BONE),
]

LANDSPEEDER_CUBES = [
    # (name, bone, (jx, jy, jz, jw, jh, jd), uv[, inflate])
    # jy = brief value + 1.9 (see deviation note above).
    #
    # HULL SPLIT (Task 13 carry-over from Task 12 review): the original
    # single 16w x5h x26d hull cube has a box-uv footprint of
    # 2*(16+26)=84 wide x (5+26)=31 tall — 20px wider than the 64px canvas,
    # unfixable by repositioning. Fixed by splitting the hull along its
    # z-depth (the long/overflowing axis) into hull_front (negative z, nose
    # side) and hull_rear (positive z, turbine side), each 16w x5h x13d ->
    # footprint 2*(16+13)=58 wide x (5+13)=18 tall, which fits the 64px
    # canvas with 6px of horizontal slack. The two cubes are seamless
    # (hull_front's z range [-13,0) exactly abuts hull_rear's [0,13),
    # reconstructing the original hull's full [-13,13] extent with identical
    # x/y) — paint_landspeeder() paints both uv blocks with continuous
    # banding+rust so the seam is not visible in practice.
    #
    # Splitting costs 2*h=10 extra px of stacked height (36 for both hull
    # pieces vs. 31 for the one it replaced), which no longer leaves room to
    # simply stack the other 5 uv blocks below it (14+10+5=29 needed,
    # 64-36=28 available — 1px short). Fixed with a genuine 2-column pack
    # below the two hull rows (y=[36,64), the full remaining 28px band):
    # left column x=[0,28) stacks turbine_c then turbine_l/turbine_r
    # (28x14 each, exactly fills the 28px column); right column x=[28,64)
    # stacks nose (36 wide - exactly fills the column), then seat_left/
    # seat_right (24 wide), then windshield (30 wide), total 10+8+5=23 of
    # the 28px column (5px slack). Verified non-overlapping and fully
    # on-canvas below (all rects strictly within x<=64, y<=64).
    # seat_right/turbine_r deliberately reuse seat_left/turbine_l's uv
    # offset (both pairs are exact mirror-image geometry receiving identical
    # paint, so sharing the region is intentional, not an accidental clash).
    ('hull_front',  SPEEDER_BONE, (-8.0, 18.9, -13.0, 16, 5, 13), (0, 0)),
    ('hull_rear',   SPEEDER_BONE, (-8.0, 18.9,   0.0, 16, 5, 13), (0, 18)),
    ('nose',        SPEEDER_BONE, (-6.0, 19.9, -19.0, 12, 4, 6),  (28, 36)),
    ('windshield',  SPEEDER_BONE, (-7.0, 14.9,  -7.0, 14, 4, 1),  (28, 54)),
    ('seat_left',   SPEEDER_BONE, (-7.0, 20.9,  -4.0, 6, 2, 6),   (28, 46)),
    ('seat_right',  SPEEDER_BONE, ( 1.0, 20.9,  -4.0, 6, 2, 6),   (28, 46)),
    ('turbine_c',   SPEEDER_BONE, (-3.0, 16.9,  11.0, 6, 6, 8),   (0, 36)),
    ('turbine_l',   SPEEDER_BONE, (-10.0, 17.9, 10.0, 5, 5, 9),   (0, 50)),
    ('turbine_r',   SPEEDER_BONE, ( 5.0, 17.9,  10.0, 5, 5, 9),   (0, 50)),
]

# -----------------------------------------------------------------------------
# Task-14 mobs. Bone names + cube sizes are the hand-off contract with the
# Java entity models being written in parallel — do not rename/resize.
# tusken_raider + rebel_trooper reuse HUMANOID_CUBES outright (see MOBS);
# snowtrooper has NO rig at all (reuses the stormtrooper geometry + its own
# skin). All rigs below are custom skeletons with their own bone tables.
# -----------------------------------------------------------------------------

# Jawa: 3/4-height robed scavenger, grounded stack — legs 0..6, robe body
# 6..15, head 15..23 (same pivots and cube tuples as JawaModel.java, which
# was written against this rig's UV layout: compact blocks at head (0,0),
# body (0,16), arms (24,16)/(36,16), legs (0,32)/(12,32)).
JAWA_HEAD_BONE = (0, 15, 0)
JAWA_BODY_BONE = (0, 6, 0)
JAWA_RARM_BONE = (-5.5, 13, 0)
JAWA_LARM_BONE = (5.5, 13, 0)
JAWA_RLEG_BONE = (-1.5, 6, 0)
JAWA_LLEG_BONE = (1.5, 6, 0)

JAWA_BONE_DEFS = [
    ('head', JAWA_HEAD_BONE),
    ('body', JAWA_BODY_BONE),
    ('right_arm', JAWA_RARM_BONE),
    ('left_arm', JAWA_LARM_BONE),
    ('right_leg', JAWA_RLEG_BONE),
    ('left_leg', JAWA_LLEG_BONE),
]

JAWA_CUBES = [
    ('head',      JAWA_HEAD_BONE, (-4.0, -8.0, -4.0, 8, 8, 8),    (0, 0)),
    ('body',      JAWA_BODY_BONE, (-4.0, -9.0, -2.0, 8, 9, 4),    (0, 16)),
    ('right_arm', JAWA_RARM_BONE, (-1.5, -2.0, -1.5, 3, 9, 3),    (24, 16)),
    ('left_arm',  JAWA_LARM_BONE, (-1.5, -2.0, -1.5, 3, 9, 3),    (36, 16)),
    ('right_leg', JAWA_RLEG_BONE, (-1.5,  0.0, -1.5, 3, 6, 3),    (0, 32)),
    ('left_leg',  JAWA_LLEG_BONE, (-1.5,  0.0, -1.5, 3, 6, 3),    (12, 32)),
]

# Bantha (128x64 canvas): quadruped. Body 14x10x22 with its underside at
# y=14; wool_skirt overlay hangs 12..17 around the body's lower half; head
# at the front (z-) with two forward-running horn cubes on the head bone;
# leg0..leg3 at the body corners, feet on the ground.
BANTHA_BODY_BONE = (0, 24, 0)
BANTHA_HEAD_BONE = (0, 22, -11)
BANTHA_LEG0_BONE = (-5, 14, -8)
BANTHA_LEG1_BONE = (5, 14, -8)
BANTHA_LEG2_BONE = (-5, 14, 8)
BANTHA_LEG3_BONE = (5, 14, 8)

BANTHA_BONE_DEFS = [
    ('body', BANTHA_BODY_BONE),
    ('head', BANTHA_HEAD_BONE),
    ('leg0', BANTHA_LEG0_BONE),
    ('leg1', BANTHA_LEG1_BONE),
    ('leg2', BANTHA_LEG2_BONE),
    ('leg3', BANTHA_LEG3_BONE),
]

BANTHA_CUBES = [
    ('body',       BANTHA_BODY_BONE, (-7.0,  0.0, -11.0, 14, 10, 22), (0, 0)),
    ('wool_skirt', BANTHA_BODY_BONE, (-8.0,  7.0, -12.0, 16, 5, 24),  (0, 32)),
    ('head',       BANTHA_HEAD_BONE, (-5.0, -2.0, -10.0, 10, 9, 10),  (72, 0)),
    ('right_horn', BANTHA_HEAD_BONE, (-7.0,  1.0,  -8.0, 2, 2, 6),    (112, 0)),
    ('left_horn',  BANTHA_HEAD_BONE, ( 5.0,  1.0,  -8.0, 2, 2, 6),    (112, 8)),
    ('leg0',       BANTHA_LEG0_BONE, (-2.0,  0.0,  -2.0, 4, 14, 4),   (96, 19)),
    ('leg1',       BANTHA_LEG1_BONE, (-2.0,  0.0,  -2.0, 4, 14, 4),   (112, 19)),
    ('leg2',       BANTHA_LEG2_BONE, (-2.0,  0.0,  -2.0, 4, 14, 4),   (96, 37)),
    ('leg3',       BANTHA_LEG3_BONE, (-2.0,  0.0,  -2.0, 4, 14, 4),   (112, 37)),
]

# Tauntaun: bipedal snow lizard. Body 12..22 with the neck rising off its
# front top into the forward-jutting head; small arms hang off the front
# flanks; hind legs carry the ground contact; tail juts rearward.
TAUN_BODY_BONE  = (0, 22, 0)
TAUN_NECK_BONE  = (0, 22, -5)
TAUN_HEAD_BONE  = (0, 26, -5)
TAUN_RARM_BONE  = (-4, 19, -5)
TAUN_LARM_BONE  = (4, 19, -5)
TAUN_RHIND_BONE = (-2, 12, 4)
TAUN_LHIND_BONE = (2, 12, 4)
TAUN_TAIL_BONE  = (0, 15, 7)

TAUNTAUN_BONE_DEFS = [
    ('body', TAUN_BODY_BONE),
    ('neck', TAUN_NECK_BONE),
    ('head', TAUN_HEAD_BONE),
    ('right_arm', TAUN_RARM_BONE),
    ('left_arm', TAUN_LARM_BONE),
    ('right_hind', TAUN_RHIND_BONE),
    ('left_hind', TAUN_LHIND_BONE),
    ('tail', TAUN_TAIL_BONE),
]

TAUNTAUN_CUBES = [
    ('body',       TAUN_BODY_BONE,  (-4.0,  0.0, -7.0, 8, 10, 14), (0, 0)),
    ('neck',       TAUN_NECK_BONE,  (-2.0, -4.0, -2.0, 4, 6, 4),   (44, 0)),
    ('head',       TAUN_HEAD_BONE,  (-3.0, -5.0, -8.0, 6, 6, 8),   (0, 24)),
    ('right_arm',  TAUN_RARM_BONE,  (-3.0,  0.0, -1.5, 3, 6, 3),   (44, 10)),
    ('left_arm',   TAUN_LARM_BONE,  ( 0.0,  0.0, -1.5, 3, 6, 3),   (44, 19)),
    ('right_hind', TAUN_RHIND_BONE, (-2.0,  0.0, -2.0, 4, 12, 4),  (0, 40)),
    ('left_hind',  TAUN_LHIND_BONE, (-2.0,  0.0, -2.0, 4, 12, 4),  (16, 40)),
    ('tail',       TAUN_TAIL_BONE,  (-1.5, -1.5,  0.0, 3, 3, 8),   (28, 28)),
]

# Wampa (128x64 canvas): hulking biped. Body 12..24, head 24..32 with two
# upward horn stubs; long 14-tall arms reach from the shoulders (y22) down
# past the body's underside; stout legs 0..10 under a 2px shag overhang.
WAMPA_BODY_BONE = (0, 12, 0)
WAMPA_HEAD_BONE = (0, 24, 0)
WAMPA_RARM_BONE = (-9.5, 22, 0)
WAMPA_LARM_BONE = (9.5, 22, 0)
WAMPA_RLEG_BONE = (-3.5, 12, 0)
WAMPA_LLEG_BONE = (3.5, 12, 0)

WAMPA_BONE_DEFS = [
    ('body', WAMPA_BODY_BONE),
    ('head', WAMPA_HEAD_BONE),
    ('right_arm', WAMPA_RARM_BONE),
    ('left_arm', WAMPA_LARM_BONE),
    ('right_leg', WAMPA_RLEG_BONE),
    ('left_leg', WAMPA_LLEG_BONE),
]

WAMPA_CUBES = [
    ('body',       WAMPA_BODY_BONE, (-7.0, -12.0, -4.0, 14, 12, 8), (0, 0)),
    ('head',       WAMPA_HEAD_BONE, (-5.0,  -8.0, -4.0, 10, 8, 8),  (44, 0)),
    ('right_horn', WAMPA_HEAD_BONE, (-6.0, -10.0, -1.0, 2, 3, 2),   (80, 0)),
    ('left_horn',  WAMPA_HEAD_BONE, ( 4.0, -10.0, -1.0, 2, 3, 2),   (88, 0)),
    ('right_arm',  WAMPA_RARM_BONE, (-2.5,  -2.0, -2.5, 5, 14, 5),  (0, 20)),
    ('left_arm',   WAMPA_LARM_BONE, (-2.5,  -2.0, -2.5, 5, 14, 5),  (20, 20)),
    ('right_leg',  WAMPA_RLEG_BONE, (-2.5,   2.0, -2.5, 5, 10, 5),  (40, 20)),
    ('left_leg',   WAMPA_LLEG_BONE, (-2.5,   2.0, -2.5, 5, 10, 5),  (60, 20)),
]

# Probe droid: levitating pod (20..28) with a front eye cube + top antenna
# on the pod bone, and four 8-tall manipulator legs dangling from the pod's
# corners to y=12 — it hovers, so nothing reaches the ground.
PROBE_POD_BONE  = (0, 24, 0)
PROBE_LEG0_BONE = (-4, 20, -4)
PROBE_LEG1_BONE = (4, 20, -4)
PROBE_LEG2_BONE = (-4, 20, 4)
PROBE_LEG3_BONE = (4, 20, 4)

PROBE_DROID_BONE_DEFS = [
    ('pod', PROBE_POD_BONE),
    ('leg0', PROBE_LEG0_BONE),
    ('leg1', PROBE_LEG1_BONE),
    ('leg2', PROBE_LEG2_BONE),
    ('leg3', PROBE_LEG3_BONE),
]

PROBE_DROID_CUBES = [
    ('pod',     PROBE_POD_BONE,  (-5.0,  -4.0, -5.0, 10, 8, 10), (0, 0)),
    ('eye',     PROBE_POD_BONE,  (-2.0,  -2.0, -7.0, 4, 4, 2),   (40, 0)),
    ('antenna', PROBE_POD_BONE,  (-0.5, -10.0, -0.5, 1, 6, 1),   (52, 0)),
    ('leg0',    PROBE_LEG0_BONE, (-0.5,   0.0, -0.5, 1, 8, 1),   (0, 18)),
    ('leg1',    PROBE_LEG1_BONE, (-0.5,   0.0, -0.5, 1, 8, 1),   (4, 18)),
    ('leg2',    PROBE_LEG2_BONE, (-0.5,   0.0, -0.5, 1, 8, 1),   (8, 18)),
    ('leg3',    PROBE_LEG3_BONE, (-0.5,   0.0, -0.5, 1, 8, 1),   (12, 18)),
]

# Dragonsnake: ground-hugging swamp serpent, head + 4 segments chained
# straight behind along +z (total z extent -20..20, height 0..4). Segment
# pivots sit at each joint's front edge for slither articulation; the head
# pivots at the neck joint, mid-height.
DSNAKE_HEAD_BONE = (0, 2, -12)
DSNAKE_SEG0_BONE = (0, 4, -12)
DSNAKE_SEG1_BONE = (0, 4, -4)
DSNAKE_SEG2_BONE = (0, 4, 4)
DSNAKE_SEG3_BONE = (0, 4, 12)

DRAGONSNAKE_BONE_DEFS = [
    ('head', DSNAKE_HEAD_BONE),
    ('seg0', DSNAKE_SEG0_BONE),
    ('seg1', DSNAKE_SEG1_BONE),
    ('seg2', DSNAKE_SEG2_BONE),
    ('seg3', DSNAKE_SEG3_BONE),
]

DRAGONSNAKE_CUBES = [
    ('head', DSNAKE_HEAD_BONE, (-3.0, -2.0, -8.0, 6, 4, 8), (0, 0)),
    ('seg0', DSNAKE_SEG0_BONE, (-2.5,  0.0,  0.0, 5, 4, 8), (28, 0)),
    ('seg1', DSNAKE_SEG1_BONE, (-2.5,  0.0,  0.0, 5, 4, 8), (0, 12)),
    ('seg2', DSNAKE_SEG2_BONE, (-2.5,  0.0,  0.0, 5, 4, 8), (26, 12)),
    ('seg3', DSNAKE_SEG3_BONE, (-2.5,  0.0,  0.0, 5, 4, 8), (0, 24)),
]

# Bogwing (32x32 canvas): small swamp flyer hovering at y~14-19; flat wing
# panels flap from the body's flanks (pivots at x=+/-2).
BOG_BODY_BONE  = (0, 17, 0)
BOG_HEAD_BONE  = (0, 17, -3)
BOG_RWING_BONE = (-2, 17, 0)
BOG_LWING_BONE = (2, 17, 0)

BOGWING_BONE_DEFS = [
    ('body', BOG_BODY_BONE),
    ('head', BOG_HEAD_BONE),
    ('right_wing', BOG_RWING_BONE),
    ('left_wing', BOG_LWING_BONE),
]

BOGWING_CUBES = [
    ('body',       BOG_BODY_BONE,  ( -2.0,  0.0, -3.0, 4, 3, 6),  (0, 0)),
    ('head',       BOG_HEAD_BONE,  ( -1.5, -2.0, -4.0, 3, 3, 4),  (0, 9)),
    ('right_wing', BOG_RWING_BONE, (-10.0,  0.0, -3.0, 10, 1, 6), (0, 16)),
    ('left_wing',  BOG_LWING_BONE, (  0.0,  0.0, -3.0, 10, 1, 6), (0, 23)),
]

# Yoda: knee-high. Head pivot (0,12,0) per the brief (head 12..20, wider
# than tall at 9x8x8) with ear cubes flaring off the sides; robe body 5..12;
# stubby arms + 4-tall legs (0..4 — the 1px robe-hem gap up to the body
# reads as shadow).
YODA_HEAD_BONE = (0, 12, 0)
YODA_BODY_BONE = (0, 5, 0)
YODA_RARM_BONE = (-4, 11, 0)
YODA_LARM_BONE = (4, 11, 0)
YODA_RLEG_BONE = (-1.5, 4, 0)
YODA_LLEG_BONE = (1.5, 4, 0)

YODA_BONE_DEFS = [
    ('head', YODA_HEAD_BONE),
    ('body', YODA_BODY_BONE),
    ('right_arm', YODA_RARM_BONE),
    ('left_arm', YODA_LARM_BONE),
    ('right_leg', YODA_RLEG_BONE),
    ('left_leg', YODA_LLEG_BONE),
]

YODA_CUBES = [
    ('head',      YODA_HEAD_BONE, (-4.5, -8.0, -4.0, 9, 8, 8), (0, 0)),
    ('right_ear', YODA_HEAD_BONE, (-7.5, -6.0, -0.5, 3, 2, 1), (34, 0)),
    ('left_ear',  YODA_HEAD_BONE, ( 4.5, -6.0, -0.5, 3, 2, 1), (42, 0)),
    ('body',      YODA_BODY_BONE, (-3.0, -7.0, -2.0, 6, 7, 4), (0, 16)),
    ('right_arm', YODA_RARM_BONE, (-1.0, -1.0, -1.0, 2, 6, 2), (20, 16)),
    ('left_arm',  YODA_LARM_BONE, (-1.0, -1.0, -1.0, 2, 6, 2), (28, 16)),
    ('right_leg', YODA_RLEG_BONE, (-1.0,  0.0, -1.0, 2, 4, 2), (36, 16)),
    ('left_leg',  YODA_LLEG_BONE, (-1.0,  0.0, -1.0, 2, 4, 2), (44, 16)),
]

# -----------------------------------------------------------------------------
# Wave-3 vehicle/creature rigs. Bone names + cube sizes are the hand-off
# contract with the Java entity models being written in parallel — verbatim
# from the wave-3 art brief, do not rename/resize. All are custom skeletons
# (their own bone tables) with one cube per bone except band_droid (antenna
# rides the head bone, horn rides the right_arm bone). Vehicle bones use the
# same java_to_bbmodel convention as every other rig: bbmodel y=0 is the
# ground-contact plane. Mirror-image parts that receive identical paint
# (at_at's four legs, four feet) deliberately share one box-UV offset, exactly
# as the landspeeder's seat_left/seat_right + turbine_l/turbine_r pairs do.
# -----------------------------------------------------------------------------

# speeder_bike (64x64): 74-Z-style swoop bike. Long low chassis, a rear seat,
# two forward steering vanes, two low footrails.
SPEEDER_BIKE_BONE_DEFS = [
    ('chassis',    (0, 6, 0)),
    ('seat',       (0, 10, 4)),
    ('vane_left',  (-2, 8, -9)),
    ('vane_right', (2, 8, -9)),
    ('rail_left',  (-3, 6, 0)),
    ('rail_right', (3, 6, 0)),
]

SPEEDER_BIKE_CUBES = [
    ('chassis',    (0, 6, 0),   (-3, -4, -9, 6, 4, 18), (0, 0)),
    ('seat',       (0, 10, 4),  (-2, -2, -3, 4, 2, 6),  (0, 22)),
    ('vane_left',  (-2, 8, -9), (-2, 0, -2, 2, 2, 6),   (20, 22)),
    ('vane_right', (2, 8, -9),  (0, 0, -2, 2, 2, 6),    (36, 22)),
    ('rail_left',  (-3, 6, 0),  (-1, 0, -2, 1, 1, 8),   (0, 30)),
    ('rail_right', (3, 6, 0),   (0, 0, -2, 1, 1, 8),    (18, 30)),
]

# xwing (128x128): T-65 fighter. Central fuselage + nose + cockpit; four S-foil
# wings splayed off the rear (top pair rotated +12.5 deg, bottom pair -12.5 deg
# about Z per the brief) with an engine at each wing root.
XWING_BONE_DEFS = [
    ('fuselage',  (0, 8, 0)),
    ('nose',      (0, 9, -13)),
    ('cockpit',   (0, 14, 0)),
    ('wing_tl',   (-3, 13.5, 8), (0, 0, 12.5)),
    ('wing_tr',   (3, 13.5, 8),  (0, 0, 12.5)),
    ('wing_bl',   (-3, 8.5, 8),  (0, 0, -12.5)),
    ('wing_br',   (3, 8.5, 8),   (0, 0, -12.5)),
    ('engine_tl', (-3, 12, 3)),
    ('engine_tr', (3, 12, 3)),
    ('engine_bl', (-3, 7, 3)),
    ('engine_br', (3, 7, 3)),
]

XWING_CUBES = [
    ('fuselage',  (0, 8, 0),     (-3, -6, -13, 6, 6, 26), (0, 0)),
    ('nose',      (0, 9, -13),   (-2, -4, -10, 4, 4, 10), (64, 0)),
    ('cockpit',   (0, 14, 0),    (-2, -3, -3, 4, 3, 6),   (64, 14)),
    ('wing_tl',   (-3, 13.5, 8), (-14, -0.5, -5, 14, 1, 10), (0, 32)),
    ('wing_tr',   (3, 13.5, 8),  (0, -0.5, -5, 14, 1, 10),   (0, 43)),
    ('wing_bl',   (-3, 8.5, 8),  (-14, -0.5, -5, 14, 1, 10), (0, 54)),
    ('wing_br',   (3, 8.5, 8),   (0, -0.5, -5, 14, 1, 10),   (0, 65)),
    ('engine_tl', (-3, 12, 3),   (-3, -3, 0, 3, 3, 8),    (0, 76)),
    ('engine_tr', (3, 12, 3),    (0, -3, 0, 3, 3, 8),     (22, 76)),
    ('engine_bl', (-3, 7, 3),    (-3, -3, 0, 3, 3, 8),    (44, 76)),
    ('engine_br', (3, 7, 3),     (0, -3, 0, 3, 3, 8),     (66, 76)),
]

# tie_fighter (128x64): a central cockpit ball with a front window, two short
# pylons, and two big outboard hexagonal solar panels (tall+deep, 1 thick).
TIE_FIGHTER_BONE_DEFS = [
    ('ball',        (0, 10, 0)),
    ('window',      (0, 12, -4)),
    ('pylon_left',  (-4, 13, 0)),
    ('pylon_right', (4, 13, 0)),
    ('panel_left',  (-8, 4, 0)),
    ('panel_right', (8, 4, 0)),
]

TIE_FIGHTER_CUBES = [
    ('ball',        (0, 10, 0),  (-4, -8, -4, 8, 8, 8),   (0, 0)),
    ('window',      (0, 12, -4), (-2, -4, -1, 4, 4, 1),   (32, 0)),
    ('pylon_left',  (-4, 13, 0), (-4, -2, -1, 4, 2, 2),   (32, 5)),
    ('pylon_right', (4, 13, 0),  (0, -2, -1, 4, 2, 2),    (44, 5)),
    ('panel_left',  (-8, 4, 0),  (-1, -16, -7, 1, 16, 14), (0, 16)),
    ('panel_right', (8, 4, 0),   (0, -16, -7, 1, 16, 14),  (30, 16)),
]

# at_at (256x128, true scale): armored body high off the ground, an up-angled
# neck to the head, four tall legs to the ground with foot pads. All four legs
# are identical mirror geometry and share one box-UV offset; likewise the four
# feet (same convention as the landspeeder's mirrored seat/turbine pairs).
AT_AT_BONE_DEFS = [
    ('body',    (0, 88, 0)),
    ('neck',    (0, 110, -32), (45, 0, 0)),
    ('head',    (0, 118, -40)),
    ('leg_fl',  (-14, 88, -26)),
    ('leg_fr',  (14, 88, -26)),
    ('leg_bl',  (-14, 88, 26)),
    ('leg_br',  (14, 88, 26)),
    ('foot_fl', (-14, 0, -26)),
    ('foot_fr', (14, 0, -26)),
    ('foot_bl', (-14, 0, 26)),
    ('foot_br', (14, 0, 26)),
]

AT_AT_CUBES = [
    ('body',    (0, 88, 0),     (-18, -28, -32, 36, 28, 64), (0, 0)),
    ('neck',    (0, 110, -32),  (-4, -3, -20, 8, 6, 20),     (64, 92)),
    ('head',    (0, 118, -40),  (-7, -10, -18, 14, 10, 18),  (0, 92)),
    ('leg_fl',  (-14, 88, -26), (-4, 0, -4, 8, 88, 8),       (200, 0)),
    ('leg_fr',  (14, 88, -26),  (-4, 0, -4, 8, 88, 8),       (200, 0)),
    ('leg_bl',  (-14, 88, 26),  (-4, 0, -4, 8, 88, 8),       (200, 0)),
    ('leg_br',  (14, 88, 26),   (-4, 0, -4, 8, 88, 8),       (200, 0)),
    ('foot_fl', (-14, 0, -26),  (-6, -6, -6, 12, 6, 12),     (120, 92)),
    ('foot_fr', (14, 0, -26),   (-6, -6, -6, 12, 6, 12),     (120, 92)),
    ('foot_bl', (-14, 0, 26),   (-6, -6, -6, 12, 6, 12),     (120, 92)),
    ('foot_br', (14, 0, 26),    (-6, -6, -6, 12, 6, 12),     (120, 92)),
]

# band_droid (64x64): Bith-band-meets-protocol-droid. Domed head with a stub
# antenna, boxy body, two arms (the right one carrying a forward-jutting horn
# instrument), two short legs. Antenna rides the head bone; horn rides the
# right_arm bone — so head and right_arm each hold two cubes.
BAND_DROID_BONE_DEFS = [
    ('head',      (0, 14, 0)),
    ('body',      (0, 6, 0)),
    ('right_arm', (-3, 14, 0)),
    ('left_arm',  (3, 14, 0)),
    ('right_leg', (-1, 6, 0)),
    ('left_leg',  (1, 6, 0)),
]

BAND_DROID_CUBES = [
    ('head',      (0, 14, 0),  (-3, -6, -3, 6, 6, 6),  (0, 0)),
    ('antenna',   (0, 14, 0),  (-0.5, -9, -0.5, 1, 3, 1), (24, 0)),
    ('body',      (0, 6, 0),   (-3, -8, -2, 6, 8, 4),  (0, 16)),
    ('right_arm', (-3, 14, 0), (-2, 0, -1, 2, 8, 2),   (24, 16)),
    ('horn',      (-3, 14, 0), (-2, 7, -6, 2, 2, 5),   (40, 0)),
    ('left_arm',  (3, 14, 0),  (0, 0, -1, 2, 8, 2),    (32, 16)),
    ('right_leg', (-1, 6, 0),  (-2, 0, -1, 2, 6, 2),   (0, 32)),
    ('left_leg',  (1, 6, 0),   (0, 0, -1, 2, 6, 2),    (12, 32)),
]

# -----------------------------------------------------------------------------
# Companion mobs. Bone names + cube sizes are the hand-off contract with the
# Java entity models being written in parallel — verbatim from the brief, do
# not rename/resize. Both are custom skeletons with their own bone tables.
# snout/ear_left/ear_right (Chewbacca) and ear_left/ear_right/robe_skirt
# (Grogu) are accessory cubes riding the head/body bones (like yoda's ears).
# -----------------------------------------------------------------------------

# Chewbacca (64x64): tall Wookiee. Legs 0..8, body 6..20 (14 tall), head
# 20..28 with a forward-jutting snout + small top-side ears; long 14-tall arms
# hang from the shoulders down past the hips.
CHEW_HEAD_BONE = (0, 20, 0)
CHEW_BODY_BONE = (0, 6, 0)
CHEW_RARM_BONE = (-5.5, 18, 0)
CHEW_LARM_BONE = (5.5, 18, 0)
CHEW_RLEG_BONE = (-2, 8, 0)
CHEW_LLEG_BONE = (2, 8, 0)

CHEWBACCA_BONE_DEFS = [
    ('head', CHEW_HEAD_BONE),
    ('body', CHEW_BODY_BONE),
    ('right_arm', CHEW_RARM_BONE),
    ('left_arm', CHEW_LARM_BONE),
    ('right_leg', CHEW_RLEG_BONE),
    ('left_leg', CHEW_LLEG_BONE),
]

CHEWBACCA_CUBES = [
    ('head',      CHEW_HEAD_BONE, (-4.0,  -8.0, -4.0, 8, 8, 8),  (0, 0)),
    ('snout',     CHEW_HEAD_BONE, (-2.0,  -3.0, -7.0, 4, 3, 4),  (32, 0)),
    ('ear_left',  CHEW_HEAD_BONE, ( 4.0,  -8.0, -0.5, 1, 2, 1),  (48, 0)),
    ('ear_right', CHEW_HEAD_BONE, (-5.0,  -8.0, -0.5, 1, 2, 1),  (52, 0)),
    ('body',      CHEW_BODY_BONE, (-4.0, -14.0, -2.5, 8, 14, 5), (0, 16)),
    ('right_arm', CHEW_RARM_BONE, (-1.5,   0.0, -1.5, 3, 14, 3), (28, 16)),
    ('left_arm',  CHEW_LARM_BONE, (-1.5,   0.0, -1.5, 3, 14, 3), (40, 16)),
    ('right_leg', CHEW_RLEG_BONE, (-1.5,   0.0, -1.5, 3, 8, 3),  (0, 36)),
    ('left_leg',  CHEW_LLEG_BONE, (-1.5,   0.0, -1.5, 3, 8, 3),  (14, 36)),
]

# Grogu (32x32): tiny foundling. Legs 0..2, body 2..7, oversized head 6..12
# (sunk onto the body, no neck) with big ears flaring straight out at head
# mid-height; stubby arms; a robe_skirt overlay wrapping wider+under the body.
GROGU_HEAD_BONE = (0, 6, 0)
GROGU_BODY_BONE = (0, 2, 0)
GROGU_RARM_BONE = (-2.5, 6, 0)
GROGU_LARM_BONE = (2.5, 6, 0)
GROGU_RLEG_BONE = (-1, 2, 0)
GROGU_LLEG_BONE = (1, 2, 0)

GROGU_BONE_DEFS = [
    ('head', GROGU_HEAD_BONE),
    ('body', GROGU_BODY_BONE),
    ('right_arm', GROGU_RARM_BONE),
    ('left_arm', GROGU_LARM_BONE),
    ('right_leg', GROGU_RLEG_BONE),
    ('left_leg', GROGU_LLEG_BONE),
]

GROGU_CUBES = [
    ('head',       GROGU_HEAD_BONE, (-3.5, -6.0, -3.0, 7, 6, 6), (0, 0)),
    ('ear_left',   GROGU_HEAD_BONE, ( 3.5, -3.5, -1.5, 5, 1, 3), (0, 14)),
    ('ear_right',  GROGU_HEAD_BONE, (-8.5, -3.5, -1.5, 5, 1, 3), (0, 18)),
    ('body',       GROGU_BODY_BONE, (-2.0, -5.0, -1.5, 4, 5, 3), (0, 22)),
    ('right_arm',  GROGU_RARM_BONE, (-0.5,  0.0, -0.5, 1, 4, 1), (16, 12)),
    ('left_arm',   GROGU_LARM_BONE, (-0.5,  0.0, -0.5, 1, 4, 1), (20, 12)),
    ('right_leg',  GROGU_RLEG_BONE, (-0.5,  0.0, -0.5, 1, 2, 1), (24, 12)),
    ('left_leg',   GROGU_LLEG_BONE, (-0.5,  0.0, -0.5, 1, 2, 1), (28, 12)),
    ('robe_skirt', GROGU_BODY_BONE, (-2.5, -2.0, -2.0, 5, 3, 4), (14, 22)),
]

# Ewok (32x32): small furry forest native. Feet at world-y 0; legs 0..5,
# body 5..11, head/hood 10..17 (sunk onto the body, no neck), two ear bumps
# at world-y 14..16. HEAD/BODY bone origins honor the brief (head @ y=13,
# body @ y=7); Java bone-y = 24 - world-y (head 11, body 17, arms 13, legs 19).
#
# HEAD+HOOD MERGED: the brief lists a 7x6x6 head plus an 8x7x7 hood cube, but
# two head-region cubes that big cannot both be box-UV-packed onto a 32x32
# sheet alongside body+limbs (head net alone is 26x12; a second ~26-30-wide
# net leaves no room for the body's 9-tall net). So the 'head' bone carries a
# single 8x7x7 cube painted as the furry cowl with a tan face on its front —
# the natural way to render a hooded face on a small sheet. The repo's other
# hooded humanoids (jedi_knight/obi_wan) keep a discrete hood cube only
# because they are 64x64. Ears stay as two 1x2x2 bumps on the head bone.
EWOK_HEAD_BONE = (0, 13, 0)
EWOK_BODY_BONE = (0, 7, 0)
EWOK_RARM_BONE = (-3.5, 11, 0)
EWOK_LARM_BONE = (3.5, 11, 0)
EWOK_RLEG_BONE = (-1, 5, 0)
EWOK_LLEG_BONE = (1, 5, 0)

EWOK_BONE_DEFS = [
    ('head', EWOK_HEAD_BONE),
    ('body', EWOK_BODY_BONE),
    ('right_arm', EWOK_RARM_BONE),
    ('left_arm', EWOK_LARM_BONE),
    ('right_leg', EWOK_RLEG_BONE),
    ('left_leg', EWOK_LLEG_BONE),
]

EWOK_CUBES = [
    ('head',      EWOK_HEAD_BONE, (-4.0, -4.0, -3.5, 8, 7, 7), (0, 0)),
    ('ear_right', EWOK_HEAD_BONE, (-5.0, -3.0, -1.0, 1, 2, 2), (24, 14)),
    ('ear_left',  EWOK_HEAD_BONE, ( 4.0, -3.0, -1.0, 1, 2, 2), (24, 18)),
    ('body',      EWOK_BODY_BONE, (-2.5, -4.0, -1.5, 5, 6, 3), (0, 14)),
    ('right_arm', EWOK_RARM_BONE, (-1.0,  0.0, -1.0, 2, 6, 2), (16, 14)),
    ('left_arm',  EWOK_LARM_BONE, (-1.0,  0.0, -1.0, 2, 6, 2), (0, 23)),
    ('right_leg', EWOK_RLEG_BONE, (-1.0,  0.0, -1.0, 2, 5, 2), (8, 23)),
    ('left_leg',  EWOK_LLEG_BONE, (-1.0,  0.0, -1.0, 2, 5, 2), (16, 23)),
]

# mob_name -> bone_defs override (only needed for mobs whose bone set/pivots
# aren't the standard humanoid table).
MOB_BONE_DEFS = {
    'astromech': ASTROMECH_BONE_DEFS,
    'landspeeder': LANDSPEEDER_BONE_DEFS,
    'jawa': JAWA_BONE_DEFS,
    'bantha': BANTHA_BONE_DEFS,
    'tauntaun': TAUNTAUN_BONE_DEFS,
    'wampa': WAMPA_BONE_DEFS,
    'probe_droid': PROBE_DROID_BONE_DEFS,
    'dragonsnake': DRAGONSNAKE_BONE_DEFS,
    'bogwing': BOGWING_BONE_DEFS,
    'yoda': YODA_BONE_DEFS,
    'speeder_bike': SPEEDER_BIKE_BONE_DEFS,
    'xwing': XWING_BONE_DEFS,
    'tie_fighter': TIE_FIGHTER_BONE_DEFS,
    'at_at': AT_AT_BONE_DEFS,
    'band_droid': BAND_DROID_BONE_DEFS,
    'chewbacca': CHEWBACCA_BONE_DEFS,
    'grogu': GROGU_BONE_DEFS,
    'ewok': EWOK_BONE_DEFS,
}

# mob_name -> (tex_width, tex_height) for non-64x64 texture canvases.
MOB_TEX_SIZES = {
    'bantha': (128, 64),
    'wampa': (128, 64),
    'bogwing': (32, 32),
    'xwing': (128, 128),
    'tie_fighter': (128, 64),
    'at_at': (256, 128),
    'grogu': (32, 32),
    'ewok': (32, 32),
    # speeder_bike + band_droid + chewbacca use the default 64x64 canvas.
}

MOBS = {
    'stormtrooper': HUMANOID_CUBES + STORMTROOPER_ACCESSORIES,
    'battle_droid': BATTLE_DROID_CUBES,
    'jedi_knight': HUMANOID_CUBES + JEDI_KNIGHT_ACCESSORIES,
    'darth_vader': HUMANOID_CUBES + DARTH_VADER_ACCESSORIES,
    # Plain humanoid — no accessory cubes. Luke's black tunic, blond hair,
    # and glove stripe are painted directly onto the standard UV layout.
    'luke_skywalker': HUMANOID_CUBES,
    # Obi-Wan reuses the Jedi Knight's robe_skirt + hood accessory geometry
    # exactly — only the paint differs.
    'obi_wan': HUMANOID_CUBES + JEDI_KNIGHT_ACCESSORIES,
    # Boba Fett: helmet shell (stormtrooper-style inflated head box),
    # rangefinder stalk, and a back-mounted jetpack.
    'boba_fett': HUMANOID_CUBES + BOBA_FETT_ACCESSORIES,
    # Astromech: fully custom skeleton (see ASTROMECH_BONE_DEFS above) —
    # body/head/legs only, no arms.
    'astromech': ASTROMECH_CUBES,
    # Han Solo: humanoid + black vest layer over the white shirt.
    'han_solo': HUMANOID_CUBES + HAN_SOLO_ACCESSORIES,
    # Princess Leia: humanoid + side buns (geometry silhouette) + robe skirt.
    'princess_leia': HUMANOID_CUBES + PRINCESS_LEIA_ACCESSORIES,
    # Landspeeder: fully custom single-bone skeleton (see LANDSPEEDER_BONE_DEFS
    # above) — a static vehicle, no arms/legs/animation.
    'landspeeder': LANDSPEEDER_CUBES,
    # Task-14 mobs. Tusken Raider + Rebel Trooper are humanoids at the
    # stormtrooper's proportions, and their Java models reuse the
    # stormtrooper's accessory cubes verbatim (inflated helmet_shell @(32,0)
    # + chin_vent @(56,16)) as wrap-shell/breather and helmet-shell/strap.
    # (Snowtrooper deliberately absent: it reuses the stormtrooper rig.)
    'tusken_raider': HUMANOID_CUBES + STORMTROOPER_ACCESSORIES,
    'rebel_trooper': HUMANOID_CUBES + STORMTROOPER_ACCESSORIES,
    'jawa': JAWA_CUBES,
    'bantha': BANTHA_CUBES,
    'tauntaun': TAUNTAUN_CUBES,
    'wampa': WAMPA_CUBES,
    'probe_droid': PROBE_DROID_CUBES,
    'dragonsnake': DRAGONSNAKE_CUBES,
    'bogwing': BOGWING_CUBES,
    'yoda': YODA_CUBES,
    # Wave-3 vehicles/creatures (custom skeletons, see *_BONE_DEFS above).
    'speeder_bike': SPEEDER_BIKE_CUBES,
    'xwing': XWING_CUBES,
    'tie_fighter': TIE_FIGHTER_CUBES,
    'at_at': AT_AT_CUBES,
    'band_droid': BAND_DROID_CUBES,
    # Companions: custom skeletons (see *_BONE_DEFS above).
    'chewbacca': CHEWBACCA_CUBES,
    'grogu': GROGU_CUBES,
    # Forest native: small humanoid-skeleton rig (see EWOK_BONE_DEFS above).
    'ewok': EWOK_CUBES,
}


def java_to_bbmodel(bone, java):
    """Convert Java HumanoidModel addBox args to bbmodel from/to world coords."""
    bx, by, bz = bone
    jx, jy, jz, jw, jh, jd = java
    from_pt = [bx + jx, by - (jy + jh), bz + jz]
    to_pt   = [bx + jx + jw, by - jy, bz + jz + jd]
    return from_pt, to_pt


def make_cube(name, mob_name, bone, java, uv_offset, inflate=0.0):
    """Build the bbmodel `elements` entry for one cube."""
    from_pt, to_pt = java_to_bbmodel(bone, java)
    cube = {
        "name": name,
        "rescale": False,
        "locked": False,
        "mirror_uv": False,
        "from": from_pt,
        "to": to_pt,
        "autouv": 0,
        "color": 0,
        "origin": list(bone),
        "uv_offset": list(uv_offset),
        "type": "cube",
        "uuid": det_uuid(f"{mob_name}/cube/{name}"),
    }
    if inflate:
        cube["inflate"] = inflate
    return cube


def make_bone_group(mob_name, name, origin, child_uuids, rotation=None):
    """Build the outliner entry for one bone (head/body/arm/leg/etc).

    `rotation` (optional [x, y, z] degrees) is the bone's rest-pose rotation
    — used by the Task-19 vehicle rigs (xwing S-foils, at_at neck). Omitted
    entirely for unrotated bones so every pre-existing mob's outliner entry
    stays byte-identical."""
    group = {
        "name": name,
        "origin": list(origin),
    }
    if rotation is not None:
        group["rotation"] = list(rotation)
    group.update({
        "color": 0,
        "uuid": det_uuid(f"{mob_name}/bone/{name}"),
        "export": True,
        "isOpen": True,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": child_uuids,
    })
    return group


# Default (standard humanoid) bone table, as (bone_name, origin) pairs in
# outliner order — same order as the original hardcoded if/elif chain this
# replaces, so lookups for the existing mobs are byte-for-byte unchanged
# (including that HEAD_BONE and BODY_BONE share the same (0, 24, 0) value:
# 'head' is checked first and wins the match for both, exactly as before).
DEFAULT_BONE_DEFS = [
    ('head', HEAD_BONE),
    ('body', BODY_BONE),
    ('right_arm', RARM_BONE),
    ('left_arm', LARM_BONE),
    ('right_leg', RLEG_BONE),
    ('left_leg', LLEG_BONE),
]


def _parent_bone_name(bone_origin, bone_defs):
    for entry in bone_defs:
        name, origin = entry[0], entry[1]
        if bone_origin == origin:
            return name
    raise ValueError(bone_origin)


def build_bbmodel(mob_name, cubes, texture_path, tex_height=64, bone_defs=None,
                  tex_width=64):
    """Return the full bbmodel dict for one mob.

    `cubes` is a flat list of (name, bone, java, uv[, inflate]) tuples —
    the full cube table for the mob (humanoid cubes + accessories, or a
    fully custom skeleton). `bone_defs` is an optional (name, origin) list
    overriding the standard humanoid 6-bone table — required for mobs whose
    skeleton doesn't match it (see ASTROMECH_BONE_DEFS). `tex_width`/
    `tex_height` set the texture canvas (bantha/wampa 128x64, bogwing 32x32).
    """
    if bone_defs is None:
        bone_defs = DEFAULT_BONE_DEFS

    elements = []
    bone_children = {entry[0]: [] for entry in bone_defs}

    for entry in cubes:
        if len(entry) == 5:
            name, bone, java, uv, inflate = entry
        else:
            name, bone, java, uv = entry
            inflate = 0.0
        cube = make_cube(name, mob_name, bone, java, uv, inflate)
        elements.append(cube)
        bone_children[_parent_bone_name(bone, bone_defs)].append(cube["uuid"])

    # bone_defs entries are (name, origin) or (name, origin, rotation) — the
    # optional rotation ([x,y,z] deg) is emitted only when present, so every
    # pre-existing (2-tuple) mob's outliner stays byte-identical.
    outliner = [
        make_bone_group(mob_name, entry[0], entry[1], bone_children[entry[0]],
                        rotation=(entry[2] if len(entry) > 2 else None))
        for entry in bone_defs
    ]

    # Embed the texture as base64.
    tex_data = ''
    if os.path.exists(texture_path):
        with open(texture_path, 'rb') as f:
            tex_data = 'data:image/png;base64,' + base64.b64encode(f.read()).decode('ascii')
    else:
        print(f"WARNING: texture missing for {mob_name}, embedding empty", file=sys.stderr)

    texture = {
        "path": os.path.abspath(texture_path),
        "name": f"{mob_name}.png",
        "folder": "entity",
        "namespace": "starwars",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": det_uuid(f"{mob_name}/texture"),
        "relative_path": f"../src/main/resources/assets/starwars/textures/entity/{mob_name}.png",
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": tex_width,
        "height": tex_height,
        "uv_width": tex_width,
        "uv_height": tex_height,
        "source": tex_data,
    }

    return {
        "meta": {
            "format_version": "4.5",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": mob_name,
        "model_identifier": "",
        "visible_box": [4, 4, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "unhandled_root_fields": {},
        "resolution": {"width": tex_width, "height": tex_height},
        "elements": elements,
        "outliner": outliner,
        "textures": [texture],
    }


def write_bbmodel(out_dir, mob_name, cubes, texture_dir, tex_height=64, bone_defs=None,
                  tex_width=64):
    texture_path = os.path.join(texture_dir, f"{mob_name}.png")
    bbmodel = build_bbmodel(mob_name, cubes, texture_path, tex_height, bone_defs=bone_defs,
                            tex_width=tex_width)
    out_path = os.path.join(out_dir, f"{mob_name}.bbmodel")
    with open(out_path, 'w') as f:
        json.dump(bbmodel, f, indent=2)
        f.write('\n')
    print(f"  wrote {out_path}")


# -----------------------------------------------------------------------------
# Armor bbmodels — standard armor-overlay geometry (helmet/chestplate/
# leggings/boots), one bbmodel per piece, following the repo convention set
# by craftee/tools/craftee_armor_{helmet,chestplate,leggings,boots}.bbmodel:
# format_version "5.0" (not "4.5"), explicit modded_entity_* root fields,
# per-face "faces" UV (not the simplified "uv_offset" the mob cubes above
# use), a flat outliner (no bone groups — armor cubes have no parent bone
# hierarchy), and a 64x32 resolution (the worn-armor equipment sheet size,
# not the mob's 64x64 skin). Cube from/to sizes match the vanilla humanoid
# armor pivots exactly (same coords craftee's own generator uses); unlike
# craftee, each cube also carries the standard vanilla ArmorModel "inflate"
# value for its layer (helmet/chestplate/boots outer layer ~1.0, leggings
# inner layer ~0.5) so the preview matches how the game actually renders
# worn armor.
# -----------------------------------------------------------------------------

HEAD_ARMOR_ORIGIN  = [0, 24, 0]
BODY_ARMOR_ORIGIN  = [0, 24, 0]
RARM_ARMOR_ORIGIN  = [-5, 22, 0]
LARM_ARMOR_ORIGIN  = [5, 22, 0]
RLEG_ARMOR_ORIGIN  = [-1.9, 12, 0]
LLEG_ARMOR_ORIGIN  = [1.9, 12, 0]

# (name, from, to, origin, uv_offset, inflate)
STORMTROOPER_HELMET_CUBES = [
    ("head", [-4, 24, -4], [4, 32, 4], HEAD_ARMOR_ORIGIN, (0, 0), 1.0),
]

STORMTROOPER_CHESTPLATE_CUBES = [
    ("body",      [-4, 12, -2], [4, 24, 2],  BODY_ARMOR_ORIGIN, (16, 16), 1.01),
    ("right_arm", [-8, 12, -2], [-4, 24, 2], RARM_ARMOR_ORIGIN, (40, 16), 1.0),
    ("left_arm",  [4, 12, -2],  [8, 24, 2],  LARM_ARMOR_ORIGIN, (40, 16), 1.0),
]

# Leggings cover both the waist (body cube) and both legs, per vanilla
# ArmorModel LEGGINGS geometry — unlike craftee's own leggings bbmodel
# (legs only), the task brief calls for the body cube too.
STORMTROOPER_LEGGINGS_CUBES = [
    ("body",      [-4, 12, -2],   [4, 24, 2],   BODY_ARMOR_ORIGIN, (16, 16), 0.51),
    ("right_leg", [-3.9, 0, -2],  [0.1, 12, 2], RLEG_ARMOR_ORIGIN, (0, 16),  0.5),
    ("left_leg",  [-0.1, 0, -2],  [3.9, 12, 2], LLEG_ARMOR_ORIGIN, (0, 16),  0.5),
]

STORMTROOPER_BOOTS_CUBES = [
    ("right_leg", [-3.9, 0, -2], [0.1, 12, 2], RLEG_ARMOR_ORIGIN, (0, 16), 1.0),
    ("left_leg",  [-0.1, 0, -2], [3.9, 12, 2], LLEG_ARMOR_ORIGIN, (0, 16), 1.0),
]

# piece_name -> (cubes, equipment texture subfolder, texture file)
ARMOR_PIECES = {
    'stormtrooper_armor_helmet':     (STORMTROOPER_HELMET_CUBES, 'humanoid', 'stormtrooper.png'),
    'stormtrooper_armor_chestplate': (STORMTROOPER_CHESTPLATE_CUBES, 'humanoid', 'stormtrooper.png'),
    'stormtrooper_armor_leggings':   (STORMTROOPER_LEGGINGS_CUBES, 'humanoid_leggings', 'stormtrooper.png'),
    'stormtrooper_armor_boots':      (STORMTROOPER_BOOTS_CUBES, 'humanoid', 'stormtrooper.png'),
    # Han Solo set reuses the stormtrooper cube tables (same worn
    # silhouette) with the han_solo equipment texture.
    'han_solo_armor_helmet':     (STORMTROOPER_HELMET_CUBES, 'humanoid', 'han_solo.png'),
    'han_solo_armor_chestplate': (STORMTROOPER_CHESTPLATE_CUBES, 'humanoid', 'han_solo.png'),
    'han_solo_armor_leggings':   (STORMTROOPER_LEGGINGS_CUBES, 'humanoid_leggings', 'han_solo.png'),
    'han_solo_armor_boots':      (STORMTROOPER_BOOTS_CUBES, 'humanoid', 'han_solo.png'),
}


def armor_box_faces(size_w, size_h, size_d, uv_u, uv_v):
    """Box-UV 6-face map for a cube of size (W, H, D) at uv_offset (U, V).
    Mirrors craftee/tools/generate_bbmodels.py's box_faces()."""
    u, v, w, h, d = uv_u, uv_v, size_w, size_h, size_d
    return {
        "north": {"uv": [u + d,         v + d, u + d + w,         v + d + h], "texture": 0},
        "east":  {"uv": [u,             v + d, u + d,             v + d + h], "texture": 0},
        "south": {"uv": [u + d + w + d, v + d, u + d + w + d + w, v + d + h], "texture": 0},
        "west":  {"uv": [u + d + w,     v + d, u + d + w + d,     v + d + h], "texture": 0},
        "up":    {"uv": [u + d + w,     v + d, u + d,             v],         "texture": 0},
        "down":  {"uv": [u + d + w + w, v + d, u + d + w,         v],         "texture": 0},
    }


def make_armor_cube(piece_name, name, fr, to, origin, uv_offset, inflate=0.0):
    w = to[0] - fr[0]
    h = to[1] - fr[1]
    d = to[2] - fr[2]
    cube = {
        "name": name,
        "from": fr,
        "to": to,
        "origin": origin,
        "autouv": 0,
        "box_uv": True,
        "uv_offset": list(uv_offset),
        "rotation": None,
        "rescale": None,
        "locked": False,
        "render_order": "default",
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "color": 0,
        "type": "cube",
        "uuid": det_uuid(f"{piece_name}/cube/{name}"),
        "faces": armor_box_faces(w, h, d, *uv_offset),
    }
    if inflate:
        cube["inflate"] = inflate
    return cube


def armor_texture_record(piece_name, texture_path, folder, texture_basename='stormtrooper.png'):
    tex_data = ''
    if os.path.exists(texture_path):
        with open(texture_path, 'rb') as f:
            tex_data = 'data:image/png;base64,' + base64.b64encode(f.read()).decode('ascii')
    else:
        print(f"WARNING: texture missing for {piece_name}, embedding empty", file=sys.stderr)
    rel_path = os.path.relpath(texture_path, start=os.path.dirname(os.path.abspath(__file__)))
    return {
        "path": None,
        "name": texture_basename,
        "folder": folder,
        "namespace": "starwars",
        "id": "0",
        "particle": False,
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": det_uuid(f"{piece_name}/texture"),
        "relative_path": rel_path.replace("\\", "/"),
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "width": 64,
        "height": 32,
        "uv_width": 64,
        "uv_height": 32,
        "source": tex_data,
    }


def build_armor_bbmodel(piece_name, cube_specs, texture_path, folder, texture_basename='stormtrooper.png'):
    elements = [make_armor_cube(piece_name, *spec) for spec in cube_specs]
    outliner = [e["uuid"] for e in elements]
    return {
        "meta": {
            "format_version": "5.0",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": piece_name,
        "model_identifier": "",
        "modded_entity_entity_class": "",
        "modded_entity_version": "1.21",
        "modded_entity_flip_y": True,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 64, "height": 32},
        "elements": elements,
        "groups": [],
        "outliner": outliner,
        "textures": [armor_texture_record(piece_name, texture_path, folder, texture_basename)],
    }


def write_armor_bbmodel(out_dir, piece_name, cube_specs, texture_dir, folder, texture_basename='stormtrooper.png'):
    texture_path = os.path.join(texture_dir, 'equipment', folder, texture_basename)
    bbmodel = build_armor_bbmodel(piece_name, cube_specs, texture_path, folder=f"entity/equipment/{folder}",
                                   texture_basename=texture_basename)
    out_path = os.path.join(out_dir, f"{piece_name}.bbmodel")
    with open(out_path, 'w') as f:
        json.dump(bbmodel, f, indent=2)
        f.write('\n')
    print(f"  wrote {out_path}")


if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    # Look for textures relative to the tools/ dir.
    tools_dir = os.path.dirname(os.path.abspath(out_dir.rstrip('/')))
    texture_dir = os.path.join(tools_dir, 'src/main/resources/assets/starwars/textures/entity')
    if not os.path.isdir(texture_dir):
        # Fallback if invoked from worktree root.
        texture_dir = os.path.abspath(
            'starwars/src/main/resources/assets/starwars/textures/entity')
    print(f"Texture dir: {texture_dir}")

    for mob_name, cubes in MOBS.items():
        tw, th = MOB_TEX_SIZES.get(mob_name, (64, 64))
        write_bbmodel(out_dir, mob_name, cubes, texture_dir,
                       tex_height=th, bone_defs=MOB_BONE_DEFS.get(mob_name),
                       tex_width=tw)

    for piece_name, (cubes, folder, texture_basename) in ARMOR_PIECES.items():
        write_armor_bbmodel(out_dir, piece_name, cubes, texture_dir, folder, texture_basename)
    print('OK')
