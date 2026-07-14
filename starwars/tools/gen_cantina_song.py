#!/usr/bin/env python3
"""Synthesize the cantina jukebox tune (WAV, then ffmpeg -> OGG).

An ORIGINAL up-tempo swing number in D minor — the famous cantina song is
copyrighted, so this is our own melody in the same jazzy mood: squawky
square-wave lead, walking bass, swung hats. Fully deterministic (pure math,
no random).

Usage: python3 starwars/tools/gen_cantina_song.py
Writes: starwars/src/main/resources/assets/starwars/sounds/music/cantina_band.ogg
"""
import math
import os
import struct
import subprocess
import sys
import tempfile
import wave

RATE = 44100
BPM = 168.0
BEAT = 60.0 / BPM               # quarter note seconds
SWING = (2.0 / 3.0, 1.0 / 3.0)  # swung eighth pair, in beats

# ---------------------------------------------------------------------------
# Note tables. Melody/bass phrases as (midi_or_None, eighths) pairs.
# The tune: AABA form, jaunty call-and-response — an original composition.

A_MELODY = [
    (62, 1), (65, 1), (69, 1), (65, 1), (64, 1), (67, 1), (70, 1), (67, 1),
    (69, 1), (72, 1), (69, 1), (65, 1), (64, 2), (60, 1), (64, 1),
    (62, 1), (65, 1), (69, 1), (72, 1), (74, 1), (72, 1), (69, 1), (65, 1),
    (64, 1), (67, 1), (64, 1), (60, 1), (62, 3), (None, 1),
]

B_MELODY = [
    (65, 1), (69, 1), (72, 1), (69, 1), (70, 1), (74, 1), (77, 1), (74, 1),
    (72, 1), (76, 1), (72, 1), (69, 1), (67, 2), (64, 1), (67, 1),
    (69, 1), (72, 1), (76, 1), (72, 1), (74, 1), (77, 1), (74, 1), (69, 1),
    (67, 1), (70, 1), (67, 1), (64, 1), (69, 3), (None, 1),
]

A_BASS = [
    (38, 2), (45, 2), (36, 2), (43, 2), (38, 2), (45, 2), (40, 2), (45, 2),
    (38, 2), (45, 2), (36, 2), (43, 2), (40, 2), (43, 2), (38, 2), (38, 2),
]

B_BASS = [
    (41, 2), (48, 2), (46, 2), (41, 2), (36, 2), (43, 2), (40, 2), (43, 2),
    (41, 2), (48, 2), (38, 2), (45, 2), (40, 2), (43, 2), (45, 2), (45, 2),
]


def midi_hz(midi):
    return 440.0 * 2.0 ** ((midi - 69) / 12.0)


def swung_times(phrase):
    """Yield (start_beats, dur_beats, midi) honouring swing pairs."""
    pos_eighths = 0
    for midi, eighths in phrase:
        start = 0.0
        for i in range(pos_eighths):
            start += SWING[i % 2]
        dur = 0.0
        for i in range(pos_eighths, pos_eighths + eighths):
            dur += SWING[i % 2]
        if midi is not None:
            yield start, dur, midi
        pos_eighths += eighths


def add_tone(buf, t0, dur, freq, vol, kind):
    """Additive render into the float buffer."""
    start = int(t0 * RATE)
    n = int(dur * RATE)
    for i in range(n):
        t = i / RATE
        env = min(1.0, t / 0.012) * math.exp(-2.6 * t / dur)
        vib = 1.0 + 0.006 * math.sin(TAU * 5.4 * t)
        ph = TAU * freq * vib * t
        if kind == 'lead':
            # square-ish with softened harmonics: clarinet-from-space
            s = (math.sin(ph) + 0.45 * math.sin(3 * ph) + 0.18 * math.sin(5 * ph))
            s = math.tanh(1.7 * s) * 0.62
        elif kind == 'bass':
            s = math.sin(ph) + 0.28 * math.sin(2 * ph)
            env = min(1.0, t / 0.008) * math.exp(-3.4 * t / dur)
            s *= 0.8
        else:
            s = math.sin(ph)
        j = start + i
        if 0 <= j < len(buf):
            buf[j] += vol * env * s


def add_hat(buf, t0, vol):
    start = int(t0 * RATE)
    n = int(0.045 * RATE)
    for i in range(n):
        t = i / RATE
        # deterministic "noise": dense inharmonic partial stack
        s = 0.0
        for k, f in enumerate((6113.0, 7877.0, 9241.0, 10937.0)):
            s += math.sin(TAU * f * t + k * 1.7)
        env = math.exp(-55.0 * t)
        j = start + i
        if 0 <= j < len(buf):
            buf[j] += vol * env * s * 0.22


TAU = 2.0 * math.pi


def render():
    # Form: A A B A over the phrase tables; 8 bars each + 2 bar tail = 34 bars.
    form = [(A_MELODY, A_BASS), (A_MELODY, A_BASS), (B_MELODY, B_BASS), (A_MELODY, A_BASS)]
    bars_per_section = 8
    total_beats = 4 * bars_per_section * 4 + 8
    total = total_beats * BEAT + 1.5
    buf = [0.0] * int(total * RATE)

    section_start = 0.0
    for melody, bass in form:
        for start_b, dur_b, midi in swung_times(melody):
            add_tone(buf, section_start + start_b * BEAT, dur_b * BEAT * 0.92,
                     midi_hz(midi), 0.30, 'lead')
        for start_b, dur_b, midi in swung_times(bass):
            add_tone(buf, section_start + start_b * BEAT, dur_b * BEAT * 0.85,
                     midi_hz(midi), 0.34, 'bass')
        section_beats = bars_per_section * 4
        eighth_pos = 0.0
        for i in range(section_beats * 2):
            add_hat(buf, section_start + eighth_pos * BEAT, 0.16 if i % 2 == 0 else 0.09)
            eighth_pos += SWING[i % 2]
        section_start += section_beats * BEAT

    # Tail: hold the tonic with a little fifth flourish.
    add_tone(buf, section_start, 1.6, midi_hz(62), 0.30, 'lead')
    add_tone(buf, section_start + 0.4, 1.2, midi_hz(69), 0.18, 'lead')
    add_tone(buf, section_start, 1.8, midi_hz(38), 0.34, 'bass')

    peak = max(1e-9, max(abs(s) for s in buf))
    scale = 0.86 / peak
    return b''.join(struct.pack('<h', int(max(-1.0, min(1.0, s * scale)) * 32767)) for s in buf)


def main():
    out = 'starwars/src/main/resources/assets/starwars/sounds/music/cantina_band.ogg'
    os.makedirs(os.path.dirname(out), exist_ok=True)
    pcm = render()
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        wav_path = tmp.name
    try:
        with wave.open(wav_path, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm)
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav_path,
                        '-c:a', 'libvorbis', '-qscale:a', '4', out], check=True)
    finally:
        os.unlink(wav_path)
    print(out, os.path.getsize(out), 'bytes')


if __name__ == '__main__':
    sys.exit(main())
