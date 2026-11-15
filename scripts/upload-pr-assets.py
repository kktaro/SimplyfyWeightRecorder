#!/usr/bin/env python3
"""PR 本文に貼る画像を `pr-assets` ブランチへ上げ、埋め込み用の Markdown を出力する。

usage: scripts/upload-pr-assets.py <PR番号> <画像>...

ローカルの git 操作はせず、GitHub API（gh）だけで `pr-assets/<PR番号>/` にコミットする。
`pr-assets` はアプリのコードと履歴を共有しない孤立ブランチで、無ければ作る。
消すと過去の PR 本文の画像が表示されなくなるので、削除しないこと。
"""
import base64
import json
import os
import subprocess
import sys

BRANCH = "pr-assets"


def gh(*args, body=None):
    out = subprocess.run(
        ["gh", *args], input=None if body is None else json.dumps(body),
        capture_output=True, text=True,
    )
    if out.returncode != 0:
        return None, out.stderr
    return (json.loads(out.stdout) if out.stdout.strip() else {}), None


def api(method, path, body):
    result, err = gh("api", "--method", method, path, "--input", "-", body=body)
    if result is None:
        sys.exit(f"{method} {path} failed: {err}")
    return result


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    pr, files = sys.argv[1], sys.argv[2:]
    repo, err = gh("repo", "view", "--json", "nameWithOwner")
    if repo is None:
        sys.exit(err)
    repo = repo["nameWithOwner"]

    ref, _ = gh("api", f"repos/{repo}/git/ref/heads/{BRANCH}")
    parent = ref["object"]["sha"] if ref else None
    base_tree = None
    if parent:
        commit_info, err = gh("api", f"repos/{repo}/git/commits/{parent}")
        if commit_info is None:
            sys.exit(err)
        base_tree = commit_info["tree"]["sha"]

    tree = []
    for path in files:
        with open(path, "rb") as f:
            blob = api("POST", f"repos/{repo}/git/blobs",
                       {"content": base64.b64encode(f.read()).decode(), "encoding": "base64"})
        tree.append({"path": f"pr-{pr}/{os.path.basename(path)}", "mode": "100644",
                     "type": "blob", "sha": blob["sha"]})

    tree_body = {"tree": tree}
    if base_tree:
        tree_body["base_tree"] = base_tree
    tree_sha = api("POST", f"repos/{repo}/git/trees", tree_body)["sha"]
    commit = api("POST", f"repos/{repo}/git/commits", {
        "message": f"PR #{pr} の画像", "tree": tree_sha, "parents": [parent] if parent else [],
    })["sha"]
    if parent:
        api("PATCH", f"repos/{repo}/git/refs/heads/{BRANCH}", {"sha": commit})
    else:
        api("POST", f"repos/{repo}/git/refs", {"ref": f"refs/heads/{BRANCH}", "sha": commit})

    for path in files:
        name = os.path.basename(path)
        print(f'<img src="https://raw.githubusercontent.com/{repo}/{BRANCH}/pr-{pr}/{name}" width="270">')


if __name__ == "__main__":
    main()
