import json, os
for path in [
    'D:/CC/ChaosCraft/src/main/resources/models/devilsdream/nightmare_eye_projectile.bbmodel',
    'D:/CC/ChaosCraft/src/main/resources/models/devilsdream/blood_comet.bbmodel',
]:
    sz = os.path.getsize(path)
    with open(path) as f: m = json.load(f)
    el = len(m['elements']); ol = len(m['outliner']); an = len(m['animations'])
    print(f"{path.split('/')[-1]}: size={sz/1024:.1f}KB el={el} outliner={ol} anims={an}")
    for a in m['animations']:
        ks = sum(len(v['keyframes']) for v in a['animators'].values())
        ab = len(a['animators'])
        mn = min(len(v['keyframes']) for v in a['animators'].values())
        print(f"  {a['name']}: bones_animated={ab} total_kf={ks} min_kf_per_bone={mn} len={a['length']} loop={a['loop']}")
    # x-axis spread of element bboxes
    xs_lo = min(e['from'][0] for e in m['elements'])
    xs_hi = max(e['to'][0] for e in m['elements'])
    ys_lo = min(e['from'][1] for e in m['elements'])
    ys_hi = max(e['to'][1] for e in m['elements'])
    zs_lo = min(e['from'][2] for e in m['elements'])
    zs_hi = max(e['to'][2] for e in m['elements'])
    print(f"  bbox x[{xs_lo:.2f},{xs_hi:.2f}] y[{ys_lo:.2f},{ys_hi:.2f}] z[{zs_lo:.2f},{zs_hi:.2f}]")
