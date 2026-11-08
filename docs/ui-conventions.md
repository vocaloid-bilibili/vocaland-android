# UI 设计约定

## 圆角 token（Corners / MaterialTheme.shapes）

新代码必须使用 `MaterialTheme.shapes` 或 `xyz.lisfox.vocaland.ui.theme.Corners`，禁止新增 `RoundedCornerShape(N.dp)` 字面量。存量逐步收敛。

| 场景 | token | 半径 |
|---|---|---|
| 缩略图、小封面 | `Corners.sm` / `shapes.small` | 12dp |
| 内嵌行、chip 容器 | `Corners.md` / `shapes.medium` | 16dp |
| 普通卡片 | `Corners.lg` / `shapes.large` | 18dp |
| 大卡（轮播、Hero、占位卡） | `Corners.hero` / `shapes.extraLarge` | 28dp |
| 状态 pill / 头像 | `CircleShape` | — |

## 嵌套卡片色阶

外层卡片用 `surfaceContainerLow`；内层需要区分时升一级用 `surfaceContainerHigh`（跳级到 `Highest` 反差过大，仅在禁用态背景使用）。

## 图片

- 装饰性封面 `contentDescription = null`，避免 TalkBack 重复播报。
- 非共享元素场景不给 `decodeWidth`，`NetworkImage` 会自动按布局尺寸降采样。
- 小于 96dp 的图片自动关闭 crossfade。
