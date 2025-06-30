import os
import json
import hashlib


# 配置开关
update_dirs = []
base_url = "https://resource.mcstaralliance.com/lastupdate/"

def find_all_file(base):
    """遍历目录中的所有文件"""
    for root, dirs, files in os.walk(base):
        for file in files:
            filepath = os.path.join(root, file)
            yield file, filepath, root
        for dir in dirs:
            dirpath = os.path.join(root, dir)
            yield None, None, dirpath

def get_directory_contents(dir_path):
    """获取目录的所有子文件和子目录"""
    contents = []
    for root, dirs, files in os.walk(dir_path):
        # 只获取当前目录的内容，不递归
        if root == dir_path:
            for file in files:
                filepath = os.path.join(root, file)
                with open(filepath, 'rb') as fp:
                    data = fp.read()
                file_md5 = hashlib.md5(data).hexdigest()
                rel_path = filepath.replace(dir_path + os.sep, "")
                contents.append({
                    "type": "file",
                    "name": file,
                    "hash": file_md5,
                    "path": rel_path
                })

            for dir in dirs:
                dirpath = os.path.join(root, dir)
                contents.append({
                    "type": "directory",
                    "name": dir,
                    "path": dir
                })
            break
    return contents

if __name__ == '__main__':
    sync_dirs = ["kubejs", "ldlib", "mods"]  # 需要同步的目录
    sync_files = []  # 需要同步的单个文件（如果有的话）
    file_list = []
    dirs_to_update = []

    # 用于跟踪已处理的目录，避免重复
    processed_dirs = set()

    # 遍历目录同步文件
    for sync_dir in sync_dirs:
        for name, path, current_dir in find_all_file(".minecraft/" + sync_dir):
            # 处理文件
            if name and path:
                with open(path, 'rb') as fp:
                    data = fp.read()
                file_md5 = hashlib.md5(data).hexdigest()
                one = {
                    "filename": name,
                    "hash": file_md5,
                    "savePath": path.replace("\\", "/"),
                    "downloadUrl": base_url + path.replace("./", "").replace("\\", "/").replace(".minecraft/", "")
                }
                file_list.append(one)

            # 处理目录
            if current_dir:
                for dir_to_update in update_dirs:
                    if dir_to_update in current_dir and current_dir not in processed_dirs:
                        # 获取目录的子文件和子目录
                        children = get_directory_contents(current_dir)
                        dirs_to_update.append({
                            "dirPath": current_dir.replace("\\", "/"),
                            "children": children
                        })
                        processed_dirs.add(current_dir)

    # 遍历指定的单个文件
    for sync_file in sync_files:
        path = ".minecraft/" + sync_file.replace("\\", "/")
        name = path[path.rfind("/") + 1:]
        with open(path, 'rb') as fp:
            data = fp.read()
        file_md5 = hashlib.md5(data).hexdigest()
        one = {
            "filename": name,
            "hash": file_md5,
            "savePath": path.replace("\\", "/"),
            "downloadUrl": base_url + path.replace("./", "").replace("\\", "/").replace(".minecraft/", "")
        }
        file_list.append(one)

    # 打印 file_list 以供预览
    print(json.dumps(file_list, ensure_ascii=False, indent=4))

    # 打印 dirs_to_update 以供预览
    print("更新目录列表:")
    print(json.dumps(dirs_to_update, ensure_ascii=False, indent=4))

    # 将文件列表写入 manifest.json
    manifest_path = 'manifest.json'
    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(file_list, f, ensure_ascii=False, indent=4)

    # 可选：将目录更新信息写入另一个文件
    if dirs_to_update:
        dir_manifest_path = 'dir_manifest.json'
        with open(dir_manifest_path, 'w', encoding='utf-8') as f:
            json.dump(dirs_to_update, f, ensure_ascii=False, indent=4)
        print(f"dir_manifest.json 文件已生成")

    # 提示文件已经生成
    print("manifest.json 文件已生成")

