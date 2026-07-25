#!/usr/bin/env python3
"""Empaqueta sprites PMD SpriteCollab para Android: a diferencia de
pack_pmd.py (que decodifica, recorta y re-empaqueta en el TPK2 binario del
firmware), aqui NO hace falta descodificar la imagen -- Android puede recortar
frames de la hoja de sprites original en tiempo de render (Canvas srcOffset/
srcSize), asi que basta con copiar el PNG tal cual y guardar los metadatos de
recorte (fila, tamano de frame, duraciones) en un .json junto a el.

Salida: android/app/src/main/assets/sprites/<dex>/<accion>.png
        android/app/src/main/assets/sprites/<dex>/<accion>.json

  python3 tools/pack_pmd_android.py                 # set por defecto (ver DEFAULT_DEX)
  python3 tools/pack_pmd_android.py 1 4 7            # dex concretos
  python3 tools/pack_pmd_android.py all              # los 151 (tarda; solo Idle)
"""
import json
import os
import sys
import urllib.request
import xml.etree.ElementTree as ET

BASE = 'https://raw.githubusercontent.com/PMDCollab/SpriteCollab/master/sprite'
CACHE = os.path.join(os.path.dirname(__file__), 'pmd_cache')
OUT = os.path.join(os.path.dirname(__file__), '..', 'android', 'app', 'src', 'main', 'assets', 'sprites')
SLOW = 1.4
MIN_MS = 70

# (nombre de salida, nombre de animacion en PMD, fila del sheet)
# direcciones: 0 abajo (frente), 2 derecha, 6 izquierda (igual que pack_pmd.py)
ACTIONS = [
    ('idle', 'Idle', 0),
    ('walk_l', 'Walk', 6),
    ('walk_r', 'Walk', 2),
    ('sleep', 'Sleep', 0),
    ('eat', 'Eat', 0),
    ('hurt', 'Hurt', 0),
    ('attack', 'Attack', 0),
    ('pose', 'Pose', 0),
]

# primer lote: iniciales + evoluciones + linea de Eevee, para validar el pipeline
# antes del barrido completo de las 151 especies (ver README de android/)
DEFAULT_DEX = [1, 2, 3, 4, 5, 6, 7, 8, 9, 133, 134, 135, 136]


def fetch_bytes(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    return urllib.request.urlopen(req, timeout=30).read()


def fetch_cached(url, dest):
    if os.path.exists(dest):
        with open(dest, 'rb') as f:
            return f.read()
    data = fetch_bytes(url)
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with open(dest, 'wb') as f:
        f.write(data)
    return data


def load_animdata(xml_bytes):
    root = ET.fromstring(xml_bytes)
    anims = {}
    for a in root.find('Anims'):
        name = a.find('Name').text
        if a.find('FrameWidth') is None:
            copy = a.find('CopyOf')
            if copy is not None:
                anims[name] = ('copy', copy.text)
            continue
        anims[name] = {
            'fw': int(a.find('FrameWidth').text),
            'fh': int(a.find('FrameHeight').text),
            'durations': [int(d.text) for d in a.find('Durations')],
            'src': name,
        }
    for k, v in list(anims.items()):
        if isinstance(v, tuple) and v[0] == 'copy':
            anims[k] = anims.get(v[1])
    return anims


def pack_species(dexnum):
    folder = os.path.join(CACHE, f'android_{dexnum:04d}')
    base = f'{BASE}/{dexnum:04d}'
    xml_bytes = fetch_cached(f'{base}/AnimData.xml', os.path.join(folder, 'AnimData.xml'))
    anims = load_animdata(xml_bytes)

    out_dir = os.path.join(OUT, str(dexnum))
    os.makedirs(out_dir, exist_ok=True)
    written = []

    for out_name, pmd_name, row in ACTIONS:
        anim = anims.get(pmd_name)
        if not anim:
            continue
        fw, fh, durations, src = anim['fw'], anim['fh'], anim['durations'], anim['src']
        png_url = f'{base}/{src}-Anim.png'
        try:
            png_bytes = fetch_cached(png_url, os.path.join(folder, f'{src}-Anim.png'))
        except Exception as e:
            print(f'  #{dexnum} {out_name}: sin PNG ({e})')
            continue

        # ancho/alto reales del PNG, para saber cuantas columnas/filas hay de verdad
        w = int.from_bytes(png_bytes[16:20], 'big')
        h = int.from_bytes(png_bytes[20:24], 'big')
        cols = max(1, w // fw)
        rows = max(1, h // fh)
        real_row = row if rows > row else 0
        n_frames = min(len(durations), cols)
        ms = [max(MIN_MS, round(d * 1000 / 60 * SLOW)) for d in durations[:n_frames]]

        with open(os.path.join(out_dir, f'{out_name}.png'), 'wb') as f:
            f.write(png_bytes)
        meta = {
            'frameWidth': fw, 'frameHeight': fh, 'row': real_row,
            'frameCount': n_frames, 'frameDurationsMs': ms,
        }
        with open(os.path.join(out_dir, f'{out_name}.json'), 'w') as f:
            json.dump(meta, f)
        written.append(out_name)

    print(f'#{dexnum:03d}: {", ".join(written) if written else "NADA"}')


def main():
    args = sys.argv[1:]
    if args == ['all']:
        nums = list(range(1, 152))
    elif args:
        nums = [int(a) for a in args]
    else:
        nums = DEFAULT_DEX

    fallos = []
    for n in nums:
        try:
            pack_species(n)
        except Exception as e:
            print(f'#{n:03d}: FALLO ({e})')
            fallos.append(n)
    print(f'FALLOS: {fallos}' if fallos else 'OK')


if __name__ == '__main__':
    main()
