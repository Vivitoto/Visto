# GitHub API Push Workflow

Visto keeps `scripts/push-via-api.py` as the preferred push path from this
Hermes workstation. Normal `git push` / `git fetch` over GitHub HTTPS can time
out on this machine, while `gh api` usually works.

## When to use it

Use this after local changes are reviewed, committed, and explicitly approved for
push/release:

```bash
cd /home/vito/.hermes/workspace/Visto
scripts/push-via-api.py
```

The script pushes the committed local `HEAD` to `Vivitoto/Visto` `main` through
GitHub's Git Data API. It requires a clean working tree by default, so uncommitted
changes are not accidentally published.

## Verification

Avoid using `git fetch` as the primary verification path on this workstation; it
may hit the same HTTPS timeout as `git push`. Prefer API checks:

```bash
gh api repos/Vivitoto/Visto/branches/main --jq .commit.sha
gh run list --repo Vivitoto/Visto --branch main --limit 5
gh release view latest --repo Vivitoto/Visto --json tagName,name,assets,url
```

API pushes may create a remote commit SHA that differs from the local commit SHA
while keeping the same Git tree. In that case local `git status` can still show
`ahead`, but the remote content is already published if the local tree and remote
tree match.

## Dry run

```bash
scripts/push-via-api.py --dry-run
```

If the GitHub API is temporarily unavailable, wait and retry rather than falling
back to `git push`.
