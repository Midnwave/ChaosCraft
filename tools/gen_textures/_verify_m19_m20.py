import json
for fn in ['silver_mirror_portal','devils_constellation']:
    with open(f'D:/CC/ChaosCraft/src/main/resources/models/devilsdream/{fn}.bbmodel') as f:
        m = json.load(f)
    print(f'{fn}: elems={len(m["elements"])} bones={len(m["outliner"])} anims={len(m["animations"])}')
    for a in m['animations']:
        kfs = [len(an['keyframes']) for an in a['animators'].values()]
        print(f'  {a["name"]}: bones={len(a["animators"])} min_kf={min(kfs)} max_kf={max(kfs)} len={a["length"]}')
    xs = [c for e in m['elements'] for c in (e['from'][0], e['to'][0])]
    ys = [c for e in m['elements'] for c in (e['from'][1], e['to'][1])]
    zs = [c for e in m['elements'] for c in (e['from'][2], e['to'][2])]
    print(f'  X[{min(xs):.1f},{max(xs):.1f}] Y[{min(ys):.1f},{max(ys):.1f}] Z[{min(zs):.1f},{max(zs):.1f}]')
