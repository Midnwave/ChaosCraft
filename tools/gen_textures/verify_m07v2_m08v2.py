import json, os

for path in [
    'D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_static_field.bbmodel',
    'D:/CC/ChaosCraft/src/main/resources/models/devilsdream/infernal_crown_burst.bbmodel',
]:
    size = os.path.getsize(path)
    bb = json.load(open(path))
    print(f'=== {os.path.basename(path)} ===')
    print(f'  size: {size:,} bytes  (>=300000: {size>=300000})')
    print(f'  elements: {len(bb["elements"])}  (>=80: {len(bb["elements"])>=80})')
    print(f'  bones:    {len(bb["outliner"])}  (>=55: {len(bb["outliner"])>=55})')
    print(f'  textures: {len(bb["textures"])}')
    print(f'  animations: {len(bb["animations"])}')

    xs = []; zs = []
    for e in bb['elements']:
        xs += [e['from'][0], e['to'][0]]
        zs += [e['from'][2], e['to'][2]]
    print(f'  X range: {min(xs):.2f} .. {max(xs):.2f}')
    print(f'  Z range: {min(zs):.2f} .. {max(zs):.2f}')

    bone_uuids = {b['uuid'] for b in bb['outliner']}
    for anim in bb['animations']:
        anim_bones = set(anim['animators'].keys())
        missing = bone_uuids - anim_bones
        kf_counts = [len(a['keyframes']) for a in anim['animators'].values()]
        print(f'  anim {anim["name"]}: loop={anim["loop"]} len={anim["length"]} bones_in_anim={len(anim_bones)} missing={len(missing)} min_kf={min(kf_counts)} max_kf={max(kf_counts)}')
    print()
