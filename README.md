# MirrorSync

使用方法：
1. 使用`mirror_sync.py` 生成 `manifest.json` 和 `update.json` 文件。
2. 将这两个文件上传到 oss 中，路径分别为 `lastupdate/manifest.json` 和 `lastupdate/update.json`，外网访问路径分别为 https://resource.mcstaralliance.com/lastupdate/manifest.json 和 https://resource.mcstaralliance.com/lastupdate/update.json 。
3. 创建并上传`lastupdate/swith.txt`，使用布尔值`true`或`false`表示是否开启 `MirroSync` 更新，外网访问路径为 https://resource.mcstaralliance.com/lastupdate/switch.txt 。
