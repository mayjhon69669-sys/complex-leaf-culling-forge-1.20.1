# Complex Leaf Culling

A multi-tier leaf culling mod for Minecraft 1.21.1 (Fabric + Sodium), designed for modpacks with dense foliage from mods like William Wyther's Overhauled Overworld (WWOO) where vanilla culling leaves significant performance on the table.

## What it does

Complex Leaf Culling applies three independent culling strategies that complement each other:

**Standard Leaf Culling** (Tier 1 - all renderers)
Marks shared faces between adjacent leaf blocks as invisible during mesh building. Works for all leaf blocks and cooperates directly with Sodium's occlusion system. This is what most culling mods do.

**Extended Leaf Culling** (Tier A - Sodium only)
Cancels rendering entirely for interior leaf blocks that are fully surrounded by other leaves. Zero geometry is generated for these blocks. Most effective in dense WWOO canopies and other modded forests where standard culling misses blocks it doesn't recognise.

**Edge Pass Demotion** (Tier B - Sodium only)
Leaf blocks on the edge of a canopy that partially qualify (one neighbour below the cancel threshold) are demoted from expensive translucent rendering to cheaper cutout rendering. Leaf shapes are fully preserved - only the render pass changes. Most noticeable with shader packs like Iris where translucent geometry is disproportionately expensive.

## Requirements

- Minecraft 1.21.1
- Fabric Loader >= 0.16.0
- Fabric API
- [Sodium](https://modrinth.com/mod/sodium)

## Recommended

- [Iris Shaders](https://modrinth.com/mod/iris) - Tier B pass demotion gives the most visible benefit with shaders active
- [ModMenu](https://modrinth.com/mod/modmenu) - provides access to the in-game config screen

## Configuration

Open the config screen via ModMenu, or use the `/clc` command in-game:

```
/clc status
/clc reload
/clc set enabled true|false
/clc set mode CONSERVATIVE|BALANCED|AGGRESSIVE
/clc set threshold 0-6          (0 = auto from mode)
/clc set standardCulling true|false
/clc set extendedCulling true|false
/clc set passDemotion true|false
/clc set extendedDetect true|false
```

### Culling Modes

| Mode | Neighbours Required | Description |
|---|---|---|
| CONSERVATIVE | 6/6 | Only perfectly buried blocks. Safest visuals. |
| BALANCED | 5/6 | Good balance of performance and appearance. Default. |
| AGGRESSIVE | 4/6 | Maximum performance. Slight edge thinning possible. |

**Extended Leaf Detection** - when enabled, blocks whose registry path contains `leaf`, `leaves`, or `foliage` are treated as leaves even if not tagged `minecraft:leaves`. Required for full WWOO compatibility.

## Compatibility

| Mod | Status |
|---|---|
| Sodium 0.6.x | Required |
| Iris | Full compatibility, Tier B most effective here |
| Distant Horizons | No interaction (DH handles its own LOD geometry) |
| C2ME | Compatible |
| EntityCulling | Separate concern, no conflict |
| MoreCulling | Complementary - covers different block types |
| SodiumLeafCulling | Redundant - both target the same pipeline. Use one or the other. |
| WWOO | Primary use case |

## Performance notes

All culling work happens during chunk mesh building on Sodium's worker threads - not during rendering. There is no per-frame CPU cost. The performance benefit is realised when chunks are built or rebuilt (world load, block changes, F3+A, or saving settings).

## Building from source

```bash
git clone https://github.com/YOUR_GITHUB_HERE/complex-leaf-culling
cd complex-leaf-culling

# Place sodium-fabric-0.6.x+mc1.21.1.jar in libs/
./gradlew build
```

The built jar will be in `build/libs/`.

## License

MIT - free to fork, modify, and redistribute with attribution. See [LICENSE](LICENSE).