# Model converter

`tools/mc_convert/mc_convert.py` turns a textured high-res model (glTF or any
format trimesh reads) into a Minecraft-style geo model for `mms_render_common:geo`.

It builds the model the way hand-made Blockbench weapons are built: solid, rotated
boxes. It draws the model's outline from the front, thins it to centre lines,
straightens each line into a few runs, and turns each run into a box as wide as
that part of the model and at least as deep as `--chunk` times its width. Bends and
junctions get a cube: at a junction it is half a pixel larger than the boxes it
joins, and at a bend it sits between its two neighbours in size, so joints stay
closed without z-fighting. Every box gets a slightly different depth so overlapping
faces never fight. With `--style clean` (the default) every box is shaded from one
body tone, allowed a step either way from the source: a lit top edge, a dark bottom
edge, a soft ramp between and sparse speckle, with lighter tops and darker bottoms,
all over a gradient from light at the top left of the model to dark at the bottom right.
With `--style source` each front is painted pixel by pixel from the source texture
and its other faces are resampled from that front. Loops such as rings become a
square frame (`--ring-scale`). A bent free-ended part becomes a long full-size run
out past its corner (no joint cube there), then a shorter tapered run and a tip cube flush with its end, the run reaching back to its top edge; a
nearly straight one stays a single run at its true width.
The output is a `.geo.json`, a texture atlas, and an inventory icon.

## Setup

```bash
python3 -m venv .venv
```

```bash
.venv/bin/pip install -r tools/mc_convert/requirements.txt
```

## Usage

```bash
.venv/bin/python tools/mc_convert/mc_convert.py model.gltf albedo.png out.geo.json out.png out_icon.png --symmetric
```

| Option | Default | Effect |
| --- | --- | --- |
| `--height` | 56 | Model height in pixels; raise it to keep thin details |
| `--ring-scale` | 0.75 | Size of the square that replaces a loop, as a share of the loop |
| `--reach` | 4 | Pixels an arm's long run reaches past its corner toward the tip |
| `--style` | clean | `clean` shades boxes from one body tone; `source` copies the model's texture pixel by pixel |
| `--sword` | off | Build a straight sword instead: pommel cap, wrapped handle, flat guard, blade and a bent tip, sized from the model's proportions |
| `--guard-palette` | silver | Sword mode: palette of the guard and pommel cap (`netherite`, `silver`, `red`) |
| `--handle-palette` | red | Sword mode: palette of the handle, drawn with a crossing cord wrap |
| `--handle-colour` | none | Sword mode: hex colour that replaces the red ramp, such as `b02e26` |
| `--mid-bend` | 10 | Sword mode: degrees the blade turns back toward the spine near the middle |
| `--mid-share` | 0.5 | Sword mode: share of the blade before the middle bend |
| `--tip-bend` | 20 | Sword mode: further degrees the tip turns back toward the spine |
| `--tip-share` | 0.15 | Sword mode: share of the blade taken by the bent tip |
| `--detail` | 1.5 | Straightening tolerance in pixels; higher gives fewer, longer boxes |
| `--min-width` | 4 | Least width of a part with a free end (arms, tips), in pixels |
| `--bulk` | 1.3 | Widens parts with a free end by this factor; loops such as rings keep their width so holes stay open |
| `--taper` | 0.7 | Twice how much an arm's shorter run and tip cube narrow |
| `--depth-taper` | 0.8 | Twice how much an arm's shorter run and tip cube thin front to back |
| `--chunk` | 0.8 | Least depth of a box as a share of its width |
| `--min-depth` | 3 | Least depth of any box, in pixels |
| `--palette` | `netherite` | `netherite` remaps brightness onto netherite tones; `source` keeps posterized source colours |
| `--symmetric` | off | Mirrors left and right to even out scan noise |
| `--axes` | `auto` | Mesh axes for depth, up and side, e.g. `1,2,0`; auto picks thinnest, longest, middle |
| `--flip` | off | Turns the model upside down |
| `--ss` | 3 | Samples per pixel along each axis |

For example, a past model was made with:

```bash
.venv/bin/python tools/mc_convert/mc_convert.py scene.gltf Material_baseColor.jpeg heavy_anchor.geo.json heavy_anchor.png heavy_anchor_item.png --axes 1,2,0 --symmetric --bulk 1.4 --min-width 5 --detail 0.8
```

## Limits

- Shapes come from the front outline, so it suits weapons that read from the
  side: blades, axes, hammers, anchors.
- Wide flat areas such as spade tips come out as a box along their centre line;
  lower `--detail` to keep more of their outline.
- Parts thinner than a pixel drop out unless `--height` goes up.
- Set the item's held model `texture_size` to the atlas size the script prints.

Odachi command (source zip extracted to `in/`):

Run once per wool colour, writing `textures/item/odachi/odachi_<colour>.png` and
`odachi_<colour>_item.png` with that colour's hex (red is `b02e26`, the fallback):

```bash
.venv/bin/python tools/mc_convert/mc_convert.py in/scene.gltf in/textures/Odachi1_baseColor.png odachi.geo.json odachi_red.png odachi_red_item.png --sword --height 64 --handle-colour b02e26
```

Blade joints are mitred: each run reaches past a bend by half its width times the
tangent of half the turn, so the outer edges meet without a gap or overhang.
