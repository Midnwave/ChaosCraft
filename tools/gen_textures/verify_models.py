import json, os
for fn in ['fallen_angel_wings_summon.bbmodel', 'nightmare_cathedral.bbmodel']:
    path = f'D:/CC/ChaosCraft/src/main/resources/models/devilsdream/{fn}'
    with open(path) as f:
        m = json.load(f)
    print(f'=== {fn} ===')
    print(f'Elements: {len(m["elements"])}, Bones: {len(m["outliner"])}, Anims: {len(m["animations"])}')
    bones_ids = set(m['outliner'])
    for a in m['animations']:
        anim_keys = set(a['animators'].keys())
        missing = bones_ids - anim_keys
        kf_counts = [len(a['animators'][k]['keyframes']) for k in anim_keys]
        min_kf = min(kf_counts) if kf_counts else 0
        max_kf = max(kf_counts) if kf_counts else 0
        print(f'  {a["name"]}: {len(anim_keys)} animators, missing={len(missing)}, kfs/bone min={min_kf} max={max_kf}')
    print(f'  Size: {os.path.getsize(path):,} bytes')
