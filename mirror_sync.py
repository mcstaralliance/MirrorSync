import os
import json
import hashlib
from qcloud_cos import CosConfig, CosS3Client
from tencentcloud.common import credential
from tencentcloud.common.profile.client_profile import ClientProfile
from tencentcloud.common.profile.http_profile import HttpProfile
from tencentcloud.cdn.v20180606 import cdn_client, models

# 配置开关
upload_to_cos = False  # 如果设置为 True，开启自动上传和 CDN 刷新；False 时只生成 manifest.json
update_dirs = []

# 腾讯云 COS 配置
secret_id = 'your-secret-id'  # 替换为您的 SecretId
secret_key = 'your-secret-key'  # 替换为您的 SecretKey
region = 'ap-beijing'  # 替换为您的 region
bucket_name = 'your-bucket-name'  # 替换为您的存储桶名称
base_url = "https://resource.mcstaralliance.com/lastupdate/"


# 初始化 COS 客户端
config = CosConfig(Region=region, SecretId=secret_id, SecretKey=secret_key)
client = CosS3Client(config)

# 初始化 CDN 客户端
cred = credential.Credential(secret_id, secret_key)
http_profile = HttpProfile()
http_profile.endpoint = "cdn.tencentcloudapi.com"
client_profile = ClientProfile()
client_profile.httpProfile = http_profile
cdn_client = cdn_client.CdnClient(cred, region, client_profile)

def find_all_file(base):
    """遍历目录中的所有文件"""
    for root, dirs, files in os.walk(base):
        for file in files:
            for dir in dirs:
                filepath = os.path.join(root, file)
                yield file, filepath, os.path.join(root, dir)

def upload_file_to_cos(local_path, cos_path):
    """上传文件到腾讯云 COS，如果文件已存在且不同则覆盖"""
    # 计算文件的 MD5
    with open(local_path, 'rb') as fp:
        data = fp.read()
        local_md5 = hashlib.md5(data).hexdigest()

    # 检查 COS 中是否已存在该文件
    try:
        response = client.head_object(Bucket=bucket_name, Key=cos_path)
        cos_md5 = response['ETag'].strip('"')  # COS 的文件 MD5 是在 ETag 中
        if cos_md5 == local_md5:
            print(f"文件已存在且内容相同，跳过上传: {local_path}")
            return
        else:
            print(f"文件已存在但内容不同，覆盖上传: {local_path}")
    except Exception as e:
        if 'NoSuchKey' in str(e):
            print(f"文件不存在，准备上传: {local_path}")
        else:
            raise e

    # 上传文件
    with open(local_path, 'rb') as f:
        response = client.put_object(Bucket=bucket_name, Key=cos_path, Body=f)
    print(f"上传成功: {local_path} -> {cos_path}")

    # 刷新 CDN 缓存
    if upload_to_cos:
        refresh_cdn(cos_path)

def refresh_cdn(file_path):
    """刷新文件的 CDN 缓存"""
    refresh_url = f"{base_url}{file_path}"
    try:
        # 创建请求对象
        req = models.PurgePathCacheRequest()
        params = {
            "Paths": [refresh_url],
            "FlushType": "flush"  # 可选值：flush（刷新全部资源），delete（刷新变更资源）
        }
        req.from_json_string(json.dumps(params))

        # 调用接口
        resp = cdn_client.PurgePathCache(req)
        print(f"CDN 刷新成功: {refresh_url}")
    except Exception as e:
        print(f"CDN 刷新失败: {e}")

if __name__ == '__main__':
    sync_dirs = ["resources"]  # 需要同步的目录
    sync_files = []  # 需要同步的单个文件（如果有的话）
    file_list = []
    dirs_to_update = []

    # 遍历目录同步文件
    for sync_dir in sync_dirs:
        for name, path, current_dir in find_all_file(".minecraft/" + sync_dir):
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

            for dir_to_update in update_dirs:
                if dir_to_update in current_dir:
                    dirs_to_update.append({
                        "dirPath": current_dir,
                        "children": None # Need help
                    })
                    

            if upload_to_cos:  # 如果开关开启，上传文件
                cos_path = "lastupdate/" + path.replace(".minecraft/", "")
                upload_file_to_cos(path, cos_path)

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

        if upload_to_cos:  # 如果开关开启，上传文件
            cos_path = "lastupdate/" + path.replace(".minecraft/", "")
            upload_file_to_cos(path, cos_path)

    # 打印 file_list 以供预览
    print(json.dumps(file_list, ensure_ascii=False, indent=4))

    # 将文件列表写入 manifest.json
    manifest_path = 'manifest.json'
    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(file_list, f, ensure_ascii=False, indent=4)

    # 提示文件已经生成
    print("manifest.json 文件已生成")

    # 仅上传 manifest.json 当开关开启时
    if upload_to_cos:
        upload_file_to_cos(manifest_path, "lastupdate/manifest.json")
        print("manifest.json 文件已上传到 COS")

    # 将软硬更新表写入 update.json
    update_path = 'update.json'
    with open(update_path, 'w', encoding='utf-8') as f:
        json.dump(dirs_to_update, f, ensure_ascii=False, indent=4)
    print("update.json 文件已生成")
