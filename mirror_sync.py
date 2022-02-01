import os
import json
import hashlib


def find_all_file(base):
    for root, ds, fs in os.walk(base):
        for f in fs:
            filepath = os.path.join(root, f)
            yield f, filepath


if __name__ == '__main__':
    base_url = "https://resource.mcstaralliance.com/lastupdate/"
    sync_dirs = ["scripts", "resources"]
    sync_files = []
    file_list = []
    for sync_dir in sync_dirs:
        for name, path in find_all_file(".minecraft/" + sync_dir):
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
    print(json.dumps(file_list, ensure_ascii=False))
