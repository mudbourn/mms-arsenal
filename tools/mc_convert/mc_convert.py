import argparse
import json
import numpy as np
import trimesh
from PIL import Image
from scipy import ndimage

NETHERITE = [
    (0x1a, 0x16, 0x17), (0x26, 0x20, 0x21), (0x31, 0x29, 0x2a), (0x3b, 0x33, 0x34),
    (0x46, 0x3e, 0x3f), (0x5a, 0x52, 0x55), (0x72, 0x6a, 0x6e), (0x8c, 0x84, 0x88),
]

ap = argparse.ArgumentParser()
ap.add_argument("model")
ap.add_argument("albedo")
ap.add_argument("out_geo")
ap.add_argument("out_tex")
ap.add_argument("out_icon")
ap.add_argument("--height", type=int, default=56)
ap.add_argument("--axes", default="auto", help="mesh axes for depth,up,side e.g. 1,2,0")
ap.add_argument("--flip", action="store_true", help="turn the model upside down")
ap.add_argument("--palette", default="netherite", choices=["netherite", "source"])
ap.add_argument("--min-depth", type=float, default=3.0, help="thinnest a part may be front to back")
ap.add_argument("--chunk", type=float, default=0.8, help="part depth as a share of its width, at least")
ap.add_argument("--min-width", type=float, default=4.0, help="thinnest a part with a free end may be across")
ap.add_argument("--bulk", type=float, default=1.3, help="widen parts with a free end by this factor")
ap.add_argument("--taper", type=float, default=0.7, help="how much free-ended parts shrink from their root to their tip")
ap.add_argument("--depth-taper", type=float, default=0.8, help="how much free-ended parts thin front to back from their root to their tip")
ap.add_argument("--ring-scale", type=float, default=0.75, help="size of the square that replaces a loop, as a share of the loop")
ap.add_argument("--reach", type=float, default=4.0, help="pixels an arm's long run reaches past its corner toward the tip")
ap.add_argument("--style", default="clean", choices=["clean", "source"], help="clean shades each box from its average tone; source copies the model's texture pixel by pixel")
ap.add_argument("--detail", type=float, default=1.5, help="straightening tolerance in pixels; higher means fewer, longer boxes")
ap.add_argument("--symmetric", action="store_true")
ap.add_argument("--sword", action="store_true", help="build a straight sword: handle, guard, blade, and a bent tip")
ap.add_argument("--tip-bend", type=float, default=20.0, help="sword mode: further degrees the tip turns back toward the spine")
ap.add_argument("--mid-bend", type=float, default=10.0, help="sword mode: degrees the blade turns back toward the spine near the middle")
ap.add_argument("--mid-share", type=float, default=0.5, help="sword mode: share of the blade before the middle bend")
ap.add_argument("--tip-share", type=float, default=0.15, help="sword mode: share of the blade taken by the bent tip")
ap.add_argument("--guard-palette", default="silver", choices=["netherite", "silver", "red"], help="sword mode: palette of the guard")
ap.add_argument("--handle-palette", default="red", choices=["netherite", "silver", "red"], help="sword mode: palette of the wrapped handle")
ap.add_argument("--handle-colour", default="", help="sword mode: hex colour such as b02e26 for the handle, in place of the red ramp")
ap.add_argument("--ss", type=int, default=3)
a = ap.parse_args()

scene = trimesh.load(a.model, force="scene")
mesh = trimesh.util.concatenate(list(scene.geometry.values()))
uv = mesh.visual.uv
tex = np.asarray(Image.open(a.albedo).convert("RGB")).astype(float)
V = mesh.vertices
if a.axes == "auto":
    ext = V.max(0) - V.min(0)
    order = np.argsort(ext)
    axes = [order[0], order[2], order[1]]
else:
    axes = [int(c) for c in a.axes.split(",")]
P = V[:, axes]
# sword mode: line the sword up on its principal axes, handle down, flat side to the camera
if a.sword:
    C = V - V.mean(0)
    pc = np.linalg.svd(C, full_matrices=False)[2]
    Q = C @ pc.T
    t = (Q[:, 0] - Q[:, 0].min()) / np.ptp(Q[:, 0])
    bins = np.minimum((t * 50).astype(int), 49)
    thick = np.array([np.ptp(Q[bins == k, 2]) if (bins == k).any() else 0.0 for k in range(50)])
    if thick.argmax() > 25:
        Q[:, 0] = -Q[:, 0]
    P = Q[:, [2, 0, 1]]
if a.flip:
    P[:, 1] = -P[:, 1]
    P[:, 2] = -P[:, 2]
lo = P.min(0)
hi = P.max(0)
pitch = (hi[1] - lo[1]) / a.height
W = int(np.ceil((hi[2] - lo[2]) / pitch))
H = a.height
S = a.ss
gw, gh = W * S, H * S

# rasterize every triangle in the side-up plane, tracking depth range and front-most uv
zmin = np.full((gh, gw), np.inf)
zmax = np.full((gh, gw), -np.inf)
fuv = np.zeros((gh, gw, 2))
gx = (P[:, 2] - lo[2]) / pitch * S
gy = (hi[1] - P[:, 1]) / pitch * S
dz = P[:, 0]
for f in mesh.faces:
    x = gx[f]
    y = gy[f]
    x0, x1 = int(max(0, np.floor(x.min()))), int(min(gw - 1, np.ceil(x.max())))
    y0, y1 = int(max(0, np.floor(y.min()))), int(min(gh - 1, np.ceil(y.max())))
    if x1 < x0 or y1 < y0:
        continue
    xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
    d = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2])
    if abs(d) < 1e-12:
        continue
    w0 = ((y[1] - y[2]) * (xs - x[2]) + (x[2] - x[1]) * (ys - y[2])) / d
    w1 = ((y[2] - y[0]) * (xs - x[2]) + (x[0] - x[2]) * (ys - y[2])) / d
    w2 = 1 - w0 - w1
    inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
    if not inside.any():
        continue
    depth = w0 * dz[f[0]] + w1 * dz[f[1]] + w2 * dz[f[2]]
    sl = (slice(y0, y1 + 1), slice(x0, x1 + 1))
    np.minimum(zmin[sl], np.where(inside, depth, np.inf), out=zmin[sl])
    front = inside & (depth > zmax[sl])
    zmax[sl] = np.where(front, depth, zmax[sl])
    t = uv[f]
    u_ = w0 * t[0, 0] + w1 * t[1, 0] + w2 * t[2, 0]
    v_ = w0 * t[0, 1] + w1 * t[1, 1] + w2 * t[2, 1]
    fuv[sl][..., 0] = np.where(front, u_, fuv[sl][..., 0])
    fuv[sl][..., 1] = np.where(front, v_, fuv[sl][..., 1])

hit = np.isfinite(zmax)
th, tw = tex.shape[:2]
tu = np.clip((fuv[..., 0] % 1) * (tw - 1), 0, tw - 1).astype(int)
tv = np.clip((1 - fuv[..., 1] % 1) * (th - 1), 0, th - 1).astype(int)
col = tex[tv, tu]
thick = np.where(hit, zmax - zmin, 0) / pitch

# downsample the supersampled planes to one value per pixel
def block(x):
    return x.reshape(H, S, W, S, *x.shape[2:]).swapaxes(1, 2).reshape(H, W, S * S, *x.shape[2:])
cover = block(hit.astype(float)).mean(2)
mask = cover >= 0.4
hb = block(hit)
cnt = np.maximum(hb.sum(2), 1)
rgb = (block(col) * hb[..., None]).sum(2) / cnt[..., None]
depth = (block(thick) * hb).sum(2) / cnt
if a.symmetric:
    mask = mask | mask[:, ::-1]
    rgb = (rgb + rgb[:, ::-1]) / 2
    depth = (depth + depth[:, ::-1]) / 2
mask = ndimage.binary_opening(mask, np.ones((2, 1))) | ndimage.binary_opening(mask, np.ones((1, 2)))

# centre lines: thin the outline to a one-pixel skeleton at double resolution
K = 2
big = ndimage.zoom(mask.astype(float), K, order=1) > 0.5
dist = ndimage.distance_transform_edt(big) / K

def thin(img):
    img = img.copy()
    while True:
        changed = False
        for step in (0, 1):
            p = np.pad(img, 1)
            n = [p[:-2, 1:-1], p[:-2, 2:], p[1:-1, 2:], p[2:, 2:], p[2:, 1:-1], p[2:, :-2], p[1:-1, :-2], p[:-2, :-2]]
            b = sum(x.astype(int) for x in n)
            t = sum(((~n[i]) & n[(i + 1) % 8]).astype(int) for i in range(8))
            if step == 0:
                c = ~(n[0] & n[2] & n[4]) & ~(n[2] & n[4] & n[6])
            else:
                c = ~(n[0] & n[2] & n[6]) & ~(n[0] & n[4] & n[6])
            kill = img & (b >= 2) & (b <= 6) & (t == 1) & c
            if kill.any():
                img &= ~kill
                changed = True
        if not changed:
            return img

def neighbours(sk, r, c):
    out = []
    for dr in (-1, 0, 1):
        for dc in (-1, 0, 1):
            if (dr or dc) and 0 <= r + dr < sk.shape[0] and 0 <= c + dc < sk.shape[1] and sk[r + dr, c + dc]:
                if dr and dc and (sk[r + dr, c] or sk[r, c + dc]):
                    continue
                out.append((r + dr, c + dc))
    return out

def trace(sk):
    pts = list(zip(*np.nonzero(sk)))
    adj = {q: neighbours(sk, *q) for q in pts}
    nodes = {q for q in pts if len(adj[q]) != 2}
    seen = set()
    paths = []
    def walk(start, nxt):
        path = [start, nxt]
        seen.add((start, nxt))
        seen.add((nxt, start))
        while path[-1] not in nodes:
            step = [q for q in adj[path[-1]] if q != path[-2]]
            if not step or step[0] == path[0]:
                if step:
                    path.append(step[0])
                break
            seen.add((path[-1], step[0]))
            seen.add((step[0], path[-1]))
            path.append(step[0])
        return path
    for n in nodes:
        for q in adj[n]:
            if (n, q) not in seen:
                paths.append(walk(n, q))
    for q in pts:
        for r in adj[q]:
            if (q, r) not in seen:
                paths.append(walk(q, r))
    return paths, adj

sk = thin(big)
for _ in range(3):
    paths, adj = trace(sk)
    pruned = False
    for path in paths:
        ends = [q for q in (path[0], path[-1]) if len(adj[q]) == 1]
        joins = [q for q in (path[0], path[-1]) if len(adj[q]) > 2]
        if ends and joins and len(path) / K < 1.5 * dist[joins[0]]:
            for q in path:
                if q != joins[0]:
                    sk[q] = False
            pruned = True
    if not pruned:
        break
paths, adj = trace(sk)

def rdp(pts, eps, lo=0, hi=None):
    hi = len(pts) - 1 if hi is None else hi
    if hi - lo < 2:
        return [lo, hi]
    a0, a1 = np.array(pts[lo], float), np.array(pts[hi], float)
    d = a1 - a0
    n = np.hypot(*d)
    arr = np.array(pts[lo:hi + 1], float)
    if n < 1e-9:
        dev = np.hypot(*(arr - a0).T)
    else:
        dev = np.abs(d[0] * (arr[:, 1] - a0[1]) - d[1] * (arr[:, 0] - a0[0])) / n
    i = int(dev.argmax())
    if dev[i] <= eps:
        return [lo, hi]
    return rdp(pts, eps, lo, lo + i)[:-1] + rdp(pts, eps, lo + i, hi)

# solid parts: one rotated box per straight run, one cube per bend or junction
def world(q):
    return np.array([H - q[0] / K, q[1] / K - W / 2])

def part_depth(width, pts):
    rr = [min(H - 1, int(q[0] / K)) for q in pts]
    cc = [min(W - 1, int(q[1] / K)) for q in pts]
    measured = float(np.median(depth[rr, cc]))
    return round(max(a.min_depth, measured, a.chunk * width) * 2) / 2

parts = []
joints = {}
square = set()
tips = []
for path in paths:
    if len(adj[path[0]]) == 1 and len(adj[path[-1]]) != 1:
        path = path[::-1]
    free = len(adj[path[-1]]) == 1
    idx = rdp(path, a.detail * K)
    # bent free parts become a long run to the corner, a shorter run toward the tip, and a cube at the tip
    bent = False
    if free:
        arr = np.array(path, float)
        chord = arr[-1] - arr[0]
        dev = np.abs(chord[0] * (arr[:, 1] - arr[0, 1]) - chord[1] * (arr[:, 0] - arr[0, 0])) / max(1e-9, np.hypot(*chord))
        corner = int(dev.argmax())
        bent = dev[corner] > 2 * a.detail * K
        idx = [0, len(path) - 1]
    keep = [path[i] for i in idx]
    if bent:
        keep = [path[0], path[corner], path[-1]]
        idx = [0, corner, len(path) - 1]
    # loops such as the ring become a square around their centre, starting and ending at the attachment
    if path[0] == path[-1] and len(path) > 8:
        arr = np.array(path, float)
        mid = (arr.min(0) + arr.max(0)) / 2
        half = (arr.max(0) - arr.min(0)).max() / 2 * a.ring_scale
        start = np.array(path[0], float)
        off = start - mid
        if abs(off[0]) >= abs(off[1]):
            start = np.array([mid[0] + np.sign(off[0]) * half, mid[1]])
        else:
            start = np.array([mid[0], mid[1] + np.sign(off[1]) * half])
        corners = [mid + half * np.array(c) for c in ((1, 1), (1, -1), (-1, -1), (-1, 1))]
        a0 = np.arctan2(*(start - mid))
        corners.sort(key=lambda c: (np.arctan2(*(c - mid)) - a0) % (2 * np.pi))
        keep = [tuple(start)] + [tuple(c) for c in corners] + [tuple(start)]
        joints[tuple(start)] = 2 * float(dist[path[0]])
        adj[tuple(start)] = adj[path[0]]
        square.update(tuple(c) for c in corners)
        idx = [0] * len(keep)
        idx[-1] = len(path) - 1
    n = len(keep) - 1
    for i in range(n):
        seg = path if idx[-1] == len(path) - 1 and idx[1] == 0 else path[idx[i]:idx[i + 1] + 1]
        p0, p1 = world(keep[i]), world(keep[i + 1])
        width = 2 * float(np.median([dist[q] for q in seg]))
        # only bent free parts (arms) are widened; straight ones such as a stock keep their true width
        if not bent:
            width = max(1.0, round(width))
            d = part_depth(width, seg)
        else:
            if i == 0:
                base = max(a.min_width, round(width * a.bulk))
                base_d = part_depth(base, seg)
            # the long run keeps full size; the shorter run and the tip cube shrink by half the taper
            width = base if i == 0 else max(2.0, round(base * (1 - a.taper / 2) * 2) / 2)
            d = base_d if i == 0 else max(1.5, round(base_d * (1 - a.depth_taper / 2) * 2) / 2)
            if i == 1:
                size = width + 1
                heading = np.array(keep[2], float) - keep[1]
                heading /= max(1e-9, np.hypot(*heading))
                cube = np.array(keep[2], float) - heading * size / 2 * K
                # the shorter run and tip cube slide out to sit a quarter of their width inside the long run's end
                along = world(keep[1]) - world(keep[0])
                shift = along / max(1e-9, np.hypot(*along)) * (base / 2 + a.reach - width / 4)
                p0 = p0 + shift
                p1 = world(cube) + shift
                deg = float(np.degrees(np.arctan2(*(p1 - p0))))
                tips.append((p1, size, deg, d + 0.5))
        # free ends reach the outline by their true half width, not the widened one
        ext0 = float(dist[keep[i]]) if len(adj.get(keep[i], "xx")) == 1 else 0.0
        ext1 = float(dist[keep[i + 1]]) if len(adj.get(keep[i + 1], "xx")) == 1 and not bent else 0.0
        # an arm's long run reaches past its corner in place of a joint cube
        if bent and i == 0:
            ext1 = width / 2 + a.reach
        # an arm's shorter run reaches back to just inside the long run's top edge so the turn stays closed
        if bent and i == 1:
            ext0 = base / 2 - 1
        parts.append({"a": p0, "b": p1, "w": width, "d": d, "ext": (ext0, ext1)})
    for j, q in enumerate(keep):
        if 0 < j < n and not bent:
            joints[q] = joints.get(q, 0.0)
        elif len(adj[q]) > 2:
            joints[q] = max(joints.get(q, 0.0), 2 * float(dist[tuple(int(round(v)) for v in q)]))

# sword mode: a pommel cap, a wrapped handle, a flat guard, a straight blade, and a short tip bent back toward the spine
if a.sword:
    length = np.ptp(P[:, 1])
    px = H / length
    t = (P[:, 1] - lo[1]) / length
    bins = np.minimum((t * 50).astype(int), 49)
    def span(k, col):
        sel = bins == k
        return float(np.ptp(P[sel, col])) if sel.any() else 0.0
    def centre(k):
        sel = bins == k
        return float(P[sel, 2].mean()) if sel.any() else 0.0
    thick = np.array([span(k, 0) for k in range(50)])
    wide = np.array([span(k, 2) for k in range(50)])
    g = int(thick.argmax())
    guard_bins = [k for k in range(max(0, g - 2), min(50, g + 3)) if thick[k] > 0.5 * thick[g]]
    g0, g1 = min(guard_bins), max(guard_bins) + 1
    handle_w = max(2.0, round(float(np.median(wide[:g0])) * px))
    handle_d = max(2.0, round(float(np.median(thick[:g0])) * px))
    blade_w = max(2.5, round(float(np.median(wide[g1:])) * px * 2) / 2)
    blade_d = max(1.0, round(float(np.median(thick[g1:])) * px * 2) / 2)
    guard_w = max(handle_w + 3, round(float(wide[g0:g1].max()) * px))
    guard_d = max(handle_d + 1, round(float(thick[g0:g1].max()) * px * 2) / 2)
    guard_len = max(1.5, round((g1 - g0) / 50 * H * 2) / 2)
    y_guard = g0 / 50 * H
    spine = -np.sign(centre(49) - centre(g1)) or 1.0
    def piece(start, end, width, depth, caps=(0.0, 0.0), palette="netherite", wrap=False):
        return {
            "a": np.array(start),
            "b": np.array(end),
            "w": width,
            "d": depth,
            "ext": caps,
            "palette": palette,
            "wrap": wrap,
        }
    top = y_guard + guard_len
    parts = [
        piece([0.0, 0.0], [1.0, 0.0], handle_w + 1, handle_d + 0.5, palette=a.guard_palette),
        piece([1.0, 0.0], [y_guard, 0.0], handle_w, handle_d, palette=a.handle_palette, wrap=True),
        piece([y_guard, 0.0], [top, 0.0], guard_w, guard_d, palette=a.guard_palette),
    ]
    # the blade bends back toward the spine near the middle and again near the tip
    blade = H - top
    runs = [
        (blade * a.mid_share, 0.0),
        (blade * (1 - a.mid_share - a.tip_share), a.mid_bend),
        (blade * a.tip_share, a.mid_bend + a.tip_bend),
    ]
    # mitred joints: each run reaches past a bend by half its width times tan of half the turn, so outer edges meet
    point = np.array([top, 0.0])
    for k, (run, angle) in enumerate(runs):
        heading = np.radians(angle)
        end = point + run * np.array([np.cos(heading), spine * np.sin(heading)])
        before = abs(angle - runs[k - 1][1]) if k > 0 else 0.0
        after = abs(runs[k + 1][1] - angle) if k + 1 < len(runs) else 0.0
        caps = (blade_w / 2 * np.tan(np.radians(before) / 2), blade_w / 2 * np.tan(np.radians(after) / 2))
        parts.append(piece(list(point), list(end), blade_w, blade_d, caps))
        point = end
    joints = {}
    tips = []

# palette: map luminance onto the target ramp
lum = rgb @ np.array([0.299, 0.587, 0.114])
lv = lum[mask]
l0, l1 = np.percentile(lv, 5), np.percentile(lv, 95)
tone = np.clip(np.round((lum - l0) / max(1e-6, l1 - l0) * 5) + 1, 0, 7).astype(int)

# region values carry a palette in the hundreds and a tone 0-7 in the units
PALETTES = {"netherite": 0, "silver": 1, "red": 2}
SILVER = [
    (0x3a, 0x3c, 0x42), (0x55, 0x58, 0x5f), (0x70, 0x74, 0x7c), (0x8c, 0x90, 0x98),
    (0xa8, 0xac, 0xb3), (0xc2, 0xc5, 0xcb), (0xd8, 0xdb, 0xdf), (0xee, 0xf0, 0xf2),
]
RED = [
    (0x1c, 0x05, 0x07), (0x2b, 0x08, 0x0b), (0x3b, 0x0c, 0x10), (0x4c, 0x10, 0x15),
    (0x5e, 0x15, 0x1b), (0x72, 0x1b, 0x22), (0x88, 0x24, 0x2b), (0x9e, 0x2e, 0x35),
]

# a custom handle colour replaces the red ramp with eight shades of that colour
if a.handle_colour:
    tint = np.array([int(a.handle_colour[k:k + 2], 16) for k in (0, 2, 4)], float)
    RED = [tuple(int(v) for v in np.clip(tint * (0.3 + k * 0.11), 0, 255)) for k in range(8)]

def colour(i):
    pal, i = divmod(int(i), 100)
    if pal == 1:
        return SILVER[i]
    if pal == 2:
        return RED[i]
    if a.palette == "netherite":
        return NETHERITE[i]
    return tuple(int(v) for v in np.round(rgb[mask].mean(0) * (0.4 + i * 0.12)))

# boxes widened past the outline borrow the nearest outline pixel instead of a flat fill
near_r, near_c = ndimage.distance_transform_edt(~mask, return_distances=False, return_indices=True)

def sample(z, y):
    r = int(np.clip(H - y, 0, H - 1))
    c = int(np.clip(z + W / 2, 0, W - 1))
    return tone[near_r[r, c], near_c[r, c]]

# atlas: each box gets a front region sampled from the source, and side regions resampled from that front
regions = []
cubes = []

# clean style: one body tone for the whole model, each box allowed one step either way
body = int(np.median(tone[mask]))

# clean style: a lit top edge, a shaded bottom edge, a soft vertical ramp and sparse speckle around one tone
rng = np.random.default_rng(7)

def speckle(reg):
    hits = rng.random(reg.shape)
    return reg + (hits < 0.08).astype(int) - (hits > 0.9).astype(int)

# shade runs from light at the top left of the model to dark at the bottom right, like a lit metal block
def shade(z, y):
    return 1.6 * (y / H - 0.5) - 0.6 * (z / W)

def clean_faces(base, L, Wd, Dd, grad):
    ramp = np.linspace(0.5, -0.5, Wd)[:, None] if Wd > 2 else np.zeros((Wd, 1))
    front = speckle(np.round(base + grad + ramp).astype(int))
    front[0, :] = np.round(base + grad[0] + 2)
    front[-1, :] = np.round(base + grad[-1] - 2)
    edge = np.round(base + np.repeat(grad[:, :1], Dd, 1)).astype(int)
    edge[0, :] += 2
    edge[-1, :] -= 2
    end = np.round(base + np.repeat(grad[:, -1:], Dd, 1)).astype(int)
    end[0, :] += 2
    end[-1, :] -= 2
    return {
        "front": front,
        "up": speckle(np.round(base + 1 + np.repeat(grad[0][:, None], Dd, 1)).astype(int)),
        "down": speckle(np.round(base - 1 + np.repeat(grad[-1][:, None], Dd, 1)).astype(int)),
        "north": speckle(edge),
        "south": speckle(end),
    }

# handle wrap: crossing diagonal cords, lit where they cross over and dark in the gaps between
def wrapped(reg):
    v, u = np.indices(reg.shape)
    cord = ((u + v) % 4 == 0) | ((u - v) % 4 == 0)
    return np.where(cord, reg + 1, reg - 1)

def add_box(center, length, width, deg, d, front_tone, palette="netherite", wrap=False):
    # overlapping boxes of equal depth z-fight, so every box gets a slightly different depth
    d += 0.015 * (len(cubes) % 8)
    L = max(1, int(np.ceil(length)))
    Wd = max(1, int(np.ceil(width)))
    Dd = max(1, int(np.ceil(d)))
    front = np.zeros((Wd, L), int)
    grad = np.zeros((Wd, L))
    t = np.radians(deg)
    for v in range(Wd):
        for u in range(L):
            along = u + 0.5 - L / 2
            across = (Wd / 2 - v - 0.5)
            z = center[1] + along * np.cos(t) - across * np.sin(t)
            y = center[0] + along * np.sin(t) + across * np.cos(t)
            front[v, u] = front_tone(z, y)
            grad[v, u] = shade(z, y)
    if a.style == "clean":
        faces = clean_faces(int(np.clip(np.median(front), body - 1, body + 1)), L, Wd, Dd, grad)
    else:
        rows = np.round(np.linspace(0, Wd - 1, Dd)).astype(int)
        cols = np.round(np.linspace(0, L - 1, Dd)).astype(int)
        faces = {
            "front": front,
            "up": front[rows, :].T,
            "down": front[rows[::-1], :].T,
            "north": front[:, :Dd] if L >= Dd else front[:, cols],
            "south": front[:, -Dd:] if L >= Dd else front[:, cols[::-1]],
        }
    ids = {}
    for name, reg in faces.items():
        if wrap:
            reg = wrapped(reg)
        regions.append(np.clip(reg, 0, 7) + 100 * PALETTES[palette])
        ids[name] = len(regions) - 1
    cubes.append({
        "origin": [-d / 2, round(center[0] - width / 2, 3), round(center[1] - length / 2, 3)],
        "size": [d, width, round(length, 3)],
        "pivot": [0, round(center[0], 3), round(center[1], 3)],
        "rotation": [round(deg, 2), 0, 0],
        "regions": ids,
    })

for p in parts:
    d = p["b"] - p["a"]
    n = float(np.hypot(*d))
    if n < 1e-6:
        continue
    u = d / n
    a0 = p["a"] - u * p["ext"][0]
    b0 = p["b"] + u * p["ext"][1]
    center = (a0 + b0) / 2
    deg = float(np.degrees(np.arctan2(u[0], u[1])))
    add_box(
        center,
        float(np.hypot(*(b0 - a0))),
        p["w"],
        deg,
        p["d"],
        sample,
        p.get("palette", "netherite"),
        p.get("wrap", False),
    )

for q, size in joints.items():
    near = [p for p in parts if any(np.allclose(world(q), x) for x in (p["a"], p["b"]))]
    pick = max if len(adj.get(q, "xx")) > 2 else np.mean
    d = float(pick([p["d"] for p in near] or [a.min_depth])) + 0.5
    angs = [np.arctan2(*(p["b"] - p["a"])) for p in near]
    # square corners stay aligned with their sides instead of turning to the mean angle
    deg = float(np.degrees(angs[0] if q in square else np.mean(angs))) if angs else 0.0
    size = max(size, float(pick([p["w"] for p in near] or [a.min_width]))) + 0.5
    add_box(world(q), size, size, deg, d, sample)

for center, size, deg, d in tips:
    add_box(center, size, size, deg, d, sample)

# pack the regions on shelves
TW = 64
while True:
    x = y = row = 0
    place = []
    for reg in regions:
        h, w = reg.shape
        if x + w > TW:
            x, y, row = 0, y + row, 0
        place.append((x, y))
        x += w
        row = max(row, h)
    if y + row <= TW:
        break
    TW *= 2
atlas = np.zeros((TW, TW, 4), np.uint8)
for reg, (x, y) in zip(regions, place):
    for v in range(reg.shape[0]):
        for u in range(reg.shape[1]):
            atlas[y + v, x + u] = list(colour(reg[v, u])) + [255]
Image.fromarray(atlas).save(a.out_tex)

for c in cubes:
    ids = c.pop("regions")
    def face(name, mirror=False):
        x, y = place[ids[name]]
        h, w = regions[ids[name]].shape
        if mirror:
            return {"uv": [x + w, y], "uv_size": [-w, h]}
        return {"uv": [x, y], "uv_size": [w, h]}
    c["uv"] = {
        "west": face("front"),
        "east": face("front", True),
        "north": face("north"),
        "south": face("south"),
        "up": face("up"),
        "down": face("down"),
    }

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [{
        "description": {
            "identifier": "geometry.converted",
            "texture_width": TW,
            "texture_height": TW,
            "visible_bounds_width": 4,
            "visible_bounds_height": 5,
            "visible_bounds_offset": [0, 2, 0],
        },
        "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": cubes}],
    }],
}
with open(a.out_geo, "w") as f:
    json.dump(geo, f, indent="\t")

# icon: the front of the boxes drawn flat, nearest box last
side_px = max(H, W) + 2
icon = np.zeros((side_px, side_px, 4), np.uint8)
oy, oz = (side_px - H) // 2, (side_px - W) // 2
for c in sorted(cubes, key=lambda c: c["size"][0]):
    t = np.radians(c["rotation"][0])
    py, pz = c["pivot"][1], c["pivot"][2]
    L, Wd = c["size"][2], c["size"][1]
    x, y = c["uv"]["west"]["uv"]
    for r in range(side_px):
        for col in range(side_px):
            wy = H - (r - oy) - 0.5
            wz = (col - oz) + 0.5 - W / 2
            dy, dz = wy - py, wz - pz
            along = dz * np.cos(t) + dy * np.sin(t)
            across = -dz * np.sin(t) + dy * np.cos(t)
            if abs(along) <= L / 2 and abs(across) <= Wd / 2:
                u = int(np.clip(along + L / 2, 0, max(0, np.ceil(L) - 1)))
                v = int(np.clip(Wd / 2 - across, 0, max(0, np.ceil(Wd) - 1)))
                icon[r, col] = atlas[y + v, x + u]
flat = Image.fromarray(icon)
# swords lie on the diagonal like vanilla sword icons
if a.sword:
    flat = flat.rotate(-45, Image.NEAREST, expand=True)
    flat = flat.crop(flat.getbbox())
    edge = max(flat.size)
    square = Image.new("RGBA", (edge, edge))
    square.paste(flat, ((edge - flat.size[0]) // 2, (edge - flat.size[1]) // 2))
    flat = square
flat.resize((32, 32), Image.NEAREST).save(a.out_icon)
print("boxes", len(parts), "joints", len(joints), "atlas", TW)
