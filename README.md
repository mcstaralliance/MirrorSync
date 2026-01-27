# MirrorSync

一个用于 Minecraft Forge 客户端的自动文件同步 Mod，支持从远程服务器自动下载、更新和清理游戏文件。

## 📋 功能特性

- ✅ **自动文件同步**：根据远程清单自动下载和更新文件
- ✅ **软更新/硬更新**：支持软更新（仅添加）和硬更新（强制同步）两种模式
- ✅ **智能增量更新**：只下载变更的文件（通过 MD5 校验）
- ✅ **自动补全缺失文件**：本地不存在的文件会自动下载
- ✅ **文件清理**：自动删除过时的旧版本文件（硬更新模式）
- ✅ **并发下载**：最多 2 个并发下载，避免 CDN QPS 限制
- ✅ **重试机制**：失败自动重试最多 3 次
- ✅ **完整性校验**：MD5 哈希校验确保文件完整性
- ✅ **远程开关控制**：通过远程开关灵活控制是否启用同步

## 🔧 工作原理

### 软更新 vs 硬更新

MirrorSync 支持两种更新模式，通过配置 `update_dirs` 来决定：

#### 🟢 软更新（默认推荐）

**特点**：只添加/更新文件，**不删除**玩家目录中的额外文件

**配置**：
```python
update_dirs = []  # 空列表 = 软更新
```

**适用场景**：
- 日常更新，添加新 mod 或更新现有 mod
- 允许玩家保留自己添加的客户端 mod
- 降低误删风险

**行为**：
- ✅ 添加清单中新增的文件
- ✅ 更新 MD5 不匹配的文件
- ❌ 不删除清单中没有的文件

#### 🔴 硬更新（强制同步）

**特点**：完全同步到清单状态，**删除**所有不在清单中的文件

**配置**：
```python
update_dirs = ["mods", "kubejs"]  # 指定需要强制同步的目录
```

**适用场景**：
- 大版本更新，需要清理旧版本文件
- 移除废弃的 mod
- 确保所有玩家环境完全一致

**行为**：
- ✅ 添加清单中新增的文件
- ✅ 更新 MD5 不匹配的文件
- ✅ 删除指定目录中不在清单里的文件

#### 🟡 混合模式（部分硬更新）

可以只对部分目录使用硬更新：

```python
sync_dirs = ["kubejs", "ldlib", "mods", "config"]
update_dirs = ["mods"]  # 只对 mods 目录硬更新，其他目录软更新
```

**重要提示**：
- 软硬更新模式在生成清单时确定，**无运行时开关**
- 切换模式需要重新运行 `mirror_sync.py` 并上传新的清单文件
- `dir_manifest.json` 为空或不存在 = 软更新
- `dir_manifest.json` 包含目录规则 = 硬更新

### 两个清单文件

**`manifest.json` - 文件同步清单**
- 记录所有需要同步的文件及其 MD5 哈希值
- Mod 会检查本地文件：
  - 文件不存在 → 自动下载
  - 文件 MD5 不匹配 → 下载覆盖
  - 文件 MD5 匹配 → 跳过（已是最新）
- 每个文件包含：
  - `filename`: 文件名
  - `hash`: MD5 哈希值
  - `savePath`: 客户端保存路径（如 `.minecraft/mods/xxx.jar`）
  - `downloadUrl`: 远程下载地址

**`dir_manifest.json` - 目录清理清单**
- 记录需要清理的目录及应保留的文件列表
- Mod 会删除目录中所有不在 `children` 列表中的文件（清理旧版本 mod 等）
- 每个目录包含：
  - `dirPath`: 目录路径
  - `children`: 该目录下应保留的文件/子目录名称列表（数组）
- **为空或不存在时跳过清理步骤**（软更新）

### 同步流程

1. **启动检查**：游戏启动时，Mod 检查远程 `switch.txt` 是否为 `true`
2. **文件同步**（第一阶段）：
   - 下载 `manifest.json` 获取文件清单
   - 对每个文件检查：
     - 文件不存在 → 下载
     - 文件存在但 MD5 不匹配 → 下载覆盖
     - MD5 匹配 → 跳过
   - 验证下载文件的完整性
   - 最多 2 个文件并发下载，失败重试最多 3 次
3. **文件清理**（第二阶段，仅硬更新）：
   - 下载 `dir_manifest.json` 获取目录清单
   - 删除指定目录中不在 `children` 列表的文件
   - 软更新模式跳过此步骤
4. **完成**：所有任务完成后进入游戏

## 🚀 使用指南

### 服务端配置

#### 1. 准备文件结构

在工作目录中创建 `.minecraft` 目录结构：

```
.minecraft/
├── kubejs/          # KubeJS 脚本文件
├── ldlib/           # LDLib 库文件
└── mods/            # Mod 文件
```

#### 2. 配置生成脚本

编辑 `mirror_sync.py`：

```python
# 需要同步的目录（会递归扫描所有文件）
sync_dirs = ["kubejs", "ldlib", "mods"]

# 需要清理的目录（硬更新模式）
# 软更新：update_dirs = []  （推荐，不删除额外文件）
# 硬更新：update_dirs = ["mods", "kubejs"]  （强制同步，删除额外文件）
update_dirs = []  # 默认软更新

# 文件下载基础 URL
base_url = "https://resource.mcstaralliance.com/lastupdate/"

# 需要同步的单个文件（可选）
sync_files = []  # 例如: ["config/xxx.json"]
```

#### 3. 生成清单文件

运行脚本生成清单：

```bash
python mirror_sync.py
```

这会生成：
- `manifest.json` - 文件同步清单
- `dir_manifest.json` - 目录清理清单（如果配置了 `update_dirs`）

#### 4. 上传到 OSS/CDN

将以下文件上传到 `https://resource.mcstaralliance.com/lastupdate/`：

| 文件 | 说明 | 示例路径 |
|------|------|----------|
| `manifest.json` | 文件同步清单 | `lastupdate/manifest.json` |
| `dir_manifest.json` | 目录清理清单 | `lastupdate/dir_manifest.json` |
| `switch.txt` | 同步开关（内容为 `true` 或 `false`） | `lastupdate/switch.txt` |
| 游戏文件 | 所有需要同步的文件 | `lastupdate/mods/xxx.jar`<br>`lastupdate/kubejs/xxx.js` |

**注意**：游戏文件上传路径需与清单中 `downloadUrl` 保持一致。

#### 5. 控制同步开关

创建 `switch.txt` 文件：
- 内容为 `true`：启用自动同步
- 内容为 `false`：禁用自动同步

### 客户端使用

#### 1. 安装 Mod

将编译好的 `mirrorsync-x.x.x.jar` 放入 `.minecraft/mods/` 目录。

#### 2. 启动游戏

Mod 会在游戏启动时自动运行：
1. 检查 `switch.txt` 是否为 `true`
2. 下载/更新所有变更的文件
3. 清理过时文件（仅硬更新模式）
4. 完成后进入游戏

#### 3. 日志查看

查看 `logs/latest.log` 或游戏控制台，可以看到同步进度和详细信息。

## 📦 构建项目

```bash
# Windows
.\gradlew.bat build "-Dnet.minecraftforge.gradle.check.certs=false"

# Linux/Mac
./gradlew build -Dnet.minecraftforge.gradle.check.certs=false
```

构建产物位于 `build/libs/` 目录。

## 💡 典型使用场景

### 场景1：日常更新（软更新）

**配置**：`update_dirs = []`

1. 管理员添加新 mod 或更新现有 mod
2. 运行 `mirror_sync.py` 生成新清单
3. 上传到 CDN
4. 玩家启动游戏，自动获取更新
5. 玩家的自定义 mod 得以保留

### 场景2：大版本更新（硬更新）

**配置**：`update_dirs = ["mods", "kubejs"]`

1. 管理员重构整合包，移除废弃 mod
2. 运行 `mirror_sync.py` 生成新清单（包含清理规则）
3. 上传到 CDN
4. 玩家启动游戏，自动同步并清理旧文件
5. 所有玩家环境完全一致

### 场景3：混合更新

**配置**：`update_dirs = ["mods"]`

- 对 `mods` 目录强制同步（硬更新）
- 对 `kubejs`、`config` 等目录软更新
- 既保证核心 mod 一致性，又允许玩家自定义脚本

### 优势

- 玩家无需手动下载整合包
- 只下载变更的文件，节省流量和时间
- 自动补全缺失文件，新玩家首次启动即可同步所有文件
- 灵活控制软硬更新，平衡一致性与自由度
- MD5 校验确保文件完整性
- 限制并发数避免触发 CDN QPS 限制

## 🔒 安全注意事项

- 确保 OSS/CDN 的访问权限配置正确
- 定期检查 `manifest.json` 中的哈希值，防止文件被篡改
- 使用 HTTPS 传输以保证安全性

## 📝 远程资源地址

| 资源 | 用途 | URL |
|------|------|-----|
| 同步开关 | 控制是否启用同步 | `https://resource.mcstaralliance.com/lastupdate/switch.txt` |
| 文件清单 | 文件同步列表 | `https://resource.mcstaralliance.com/lastupdate/manifest.json` |
| 目录清单 | 目录清理列表 | `https://resource.mcstaralliance.com/lastupdate/dir_manifest.json` |

## 📄 License

本项目采用 MIT 许可证。
