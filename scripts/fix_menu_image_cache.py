#!/usr/bin/env python3
"""
One-off: re-uploads every existing menu image (menu/ and menu/thumbs/) in place
with a long-lived Cache-Control header.

Images uploaded before this fix were stored with "no-cache", so browsers
revalidated them on every page view. New uploads already get the long header
(see SupabaseStorageService.CACHE_IMMUTABLE). Object paths are unchanged, so no
database update is needed.

Downloads go through the authenticated endpoint (bypasses the CDN), so this
costs roughly the bucket size once in uncached egress.

Usage (from the project root):
    python3 scripts/fix_menu_image_cache.py            # dry run: list what would change
    python3 scripts/fix_menu_image_cache.py --apply    # re-upload
"""
import json
import sys
import urllib.request
from pathlib import Path

CACHE_CONTROL = "max-age=31536000, immutable"
PREFIXES = ["menu", "menu/thumbs"]


def load_env(path=".env"):
    env = {}
    for line in Path(path).read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def request(method, url, key, body=None, headers=None):
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Authorization", "Bearer " + key)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read()


def list_files(base, bucket, key, prefix):
    files, offset = [], 0
    while True:
        body = json.dumps({"prefix": prefix, "limit": 100, "offset": offset}).encode()
        page = json.loads(request("POST", f"{base}/storage/v1/object/list/{bucket}", key, body,
                                  {"Content-Type": "application/json"}))
        files += [o for o in page if o.get("metadata")]  # entries without metadata are folders
        if len(page) < 100:
            return files
        offset += 100


def main():
    apply = "--apply" in sys.argv
    env = load_env()
    base, key = env["SUPABASE_URL"].rstrip("/"), env["SUPABASE_SERVICE_ROLE_KEY"]
    bucket = env.get("SUPABASE_BUCKET") or "bulletbeats"

    todo = []
    for prefix in PREFIXES:
        for o in list_files(base, bucket, key, prefix):
            if o["metadata"].get("cacheControl") != CACHE_CONTROL:
                todo.append((f"{prefix}/{o['name']}", o["metadata"].get("mimetype"), o["metadata"]["size"]))

    total_mb = sum(s for _, _, s in todo) / 1e6
    print(f"{len(todo)} objects need updating ({total_mb:.1f} MB)")
    if not apply:
        for path, _, _ in todo:
            print("  " + path)
        print("Dry run — re-run with --apply to update.")
        return

    failed = 0
    for i, (path, mime, _) in enumerate(todo, 1):
        try:
            data = request("GET", f"{base}/storage/v1/object/authenticated/{bucket}/{path}", key)
            request("POST", f"{base}/storage/v1/object/{bucket}/{path}", key, data, {
                "x-upsert": "true",
                "cache-control": CACHE_CONTROL,
                "Content-Type": mime or "application/octet-stream",
            })
            print(f"[{i}/{len(todo)}] ok   {path}")
        except Exception as e:  # keep going; re-running only retries what's left
            failed += 1
            print(f"[{i}/{len(todo)}] FAIL {path}: {e}")
    print(f"Done. {len(todo) - failed} updated, {failed} failed.")


if __name__ == "__main__":
    main()
