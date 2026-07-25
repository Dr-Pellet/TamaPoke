#!/usr/bin/env python3
"""Genera dex.json (tabla de la Pokedex para la app Android) desde dex_data.py.

Comparte la misma fuente de datos que gen_dex.py (dex.h del firmware), asi
que ambos artefactos no pueden divergir: dex_data.py sigue siendo la unica
fuente de verdad.

  python3 tools/gen_dex_json.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from dex_data import DEX, TYPE_ACCENTS, CLASSIC, RARE, LEGENDARY
from dex_stats import BASE_STATS
from gen_dex import TYPE_BIOME, BIOME_OVERRIDE, rgb565

RARITY_NAMES = {0: "EVO", 1: "COMMON", 2: "RARE", 3: "LEGENDARY"}


def main():
    evolved = {d[4] for d in DEX if d[4]} | {135, 136}
    entries = []
    for num, slug, display, typ, evo, lvl in DEX:
        if num in evolved:
            rarity = 0
        elif num in LEGENDARY:
            rarity = 3
        elif num in RARE:
            rarity = 2
        else:
            rarity = 1
        hp, atk, df, spe = BASE_STATS[num]
        bio = BIOME_OVERRIDE.get(num, TYPE_BIOME[typ])
        entries.append({
            "id": num,
            "slug": slug,
            "name": display,
            "type": typ,
            "accent": f"#{rgb565(TYPE_ACCENTS[typ]):04X}",
            "evolvesTo": evo,
            "evolveLevel": lvl,
            "rarity": RARITY_NAMES[rarity],
            "baseStats": {"hp": hp, "atk": atk, "def": df, "spe": spe},
            "biome": bio,
        })

    out = {
        "eeveeId": 133,
        "eeveeEvolutions": [134, 135, 136],
        "classicStarters": CLASSIC,
        "species": entries,
    }

    path = os.path.join(os.path.dirname(__file__), '..', 'android', 'app', 'src', 'main', 'assets', 'dex.json')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=2)
    print(f"guardado {os.path.normpath(path)} ({len(entries)} especies)")


if __name__ == '__main__':
    main()
