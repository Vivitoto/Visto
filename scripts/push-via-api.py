#!/usr/bin/env python3
"""Push the current local HEAD to Vivitoto/Visto main via the GitHub API.

Used as a workaround when normal `git push` over HTTPS hits TLS/connection
issues on this workstation. Reads files from the local working tree that
differ between the remote main commit and local HEAD.
"""
from __future__ import annotations

import base64
import json
import os
import stat
import subprocess
import sys
from pathlib import Path

REPO = os.environ.get("VISTO_REPO", "Vivitoto/Visto")


def gh_api(args: list[str], payload: bytes | None = None) -> str:
    res = subprocess.run(
        ["gh", "api", *args],
        input=payload,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if res.returncode != 0:
        sys.stderr.write(res.stderr.decode())
        sys.exit(res.returncode)
    return res.stdout.decode()


def main() -> int:
    parent = gh_api(
        [f"repos/{REPO}/branches/main", "--jq", ".commit.sha"],
    ).strip()
    base_tree = gh_api(
        [f"repos/{REPO}/git/commits/{parent}", "--jq", ".tree.sha"],
    ).strip()

    local_head_tree = (
        subprocess.check_output(["git", "rev-parse", "HEAD^{tree}"], text=True).strip()
    )
    local_head = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()

    if local_head_tree == base_tree:
        print(f"local tree already matches remote main ({local_head[:7]})")
        return 0

    # Pretend local previous commit equals remote parent, so we diff against it
    # via local working tree differences against what's on remote.
    diff_lines = subprocess.check_output(
        ["git", "diff", "--name-status", f"HEAD~1..HEAD"], text=True
    ).splitlines()
    changes: list[tuple[str, str]] = []
    for line in diff_lines:
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        status = parts[0]
        path = parts[-1]
        changes.append((status, path))

    tree_entries = []
    for status, path in changes:
        if status.startswith("D"):
            tree_entries.append({"path": path, "mode": "100644", "type": "blob", "sha": None})
            print(f"delete  {path}")
            continue
        p = Path(path)
        data = p.read_bytes()
        payload = json.dumps(
            {"content": base64.b64encode(data).decode(), "encoding": "base64"}
        ).encode()
        out = gh_api([f"repos/{REPO}/git/blobs", "--method", "POST", "--input", "-"], payload)
        sha = json.loads(out)["sha"]
        mode = "100755" if (p.stat().st_mode & stat.S_IXUSR) else "100644"
        tree_entries.append({"path": path, "mode": mode, "type": "blob", "sha": sha})
        print(f"blob {sha[:7]} {mode} {path}")

    payload = json.dumps({"base_tree": base_tree, "tree": tree_entries}).encode()
    out = gh_api(
        [f"repos/{REPO}/git/trees", "--method", "POST", "--input", "-"], payload
    )
    tree_sha = json.loads(out)["sha"]
    print("tree", tree_sha)

    msg = subprocess.check_output(["git", "log", "-1", "--pretty=%B"], text=True).strip()
    payload = json.dumps(
        {"message": msg, "tree": tree_sha, "parents": [parent]}
    ).encode()
    out = gh_api(
        [f"repos/{REPO}/git/commits", "--method", "POST", "--input", "-"], payload
    )
    commit_sha = json.loads(out)["sha"]
    print("commit", commit_sha)

    payload = json.dumps({"sha": commit_sha, "force": False}).encode()
    gh_api(
        [f"repos/{REPO}/git/refs/heads/main", "--method", "PATCH", "--input", "-"],
        payload,
    )
    print("refs/heads/main =>", commit_sha)
    return 0


if __name__ == "__main__":
    sys.exit(main())
