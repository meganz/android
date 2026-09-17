# Commit signature gate

Every commit a merge request adds on top of its target branch must be GPG signed.
`commit_signature_gate.sh` enforces this in the Jenkins MR pipeline
(`jenkinsfile/android_build_status.groovy`, stage **Commit Signature Check**): on a violation the
pipeline fails and the report — the offending commits plus the commands to re-sign the branch and
to configure signing locally — is posted as an MR comment.

## Why "not N" and not "== G"

The Jenkins agents hold no contributor public keys, so a genuinely signed commit reports
`%G?` = `E` ("cannot check"), not `G`. The gate therefore asserts the absence of `N` (no
signature); asserting `== G` would fail every merge request.

`%G?` values: `G` good · `B` bad · `U` good/unknown validity · `X` good/expired ·
`Y` good/expired key · `R` good/revoked key · `E` cannot check · `N` no signature.

## Scope of the comparison

Only `origin/<target>..HEAD` is inspected. `develop` carries unsigned commits from before this
gate existed, so scanning full history would fail every merge request. Merge commits are excluded,
so a pipeline that checks out a merge result rather than the branch tip still reports only the
author's own commits.

## Manual run

```bash
bash tools/git/commit_signature_gate.sh "origin/develop"
```

Both arguments are optional — `commit_signature_gate.sh [<base-ref>] [<head-ref>]`, defaulting to
`origin/develop` and `HEAD`. Set `SIGNATURE_GATE_FETCH=1` to fetch the base ref first (CI does).

Exit codes: `0` pass (all signed, or no commits in the range) · `1` violation ·
`2` cannot verify (a ref could not be resolved; CI treats this as fail-open).

## Local setup

```bash
git config --global commit.gpgsign true
git config --global rebase.gpgSign true
```

`commit.gpgsign` is the setting that matters: on current git it is honoured by commit, amend,
cherry-pick, revert, merge and rebase alike (verified on git 2.39.1 with both rebase backends).
`rebase.gpgSign` adds nothing on top of it but states the intent explicitly.

If commits lose their signature despite the config, check that signing works in a
non-interactive shell — a `gpg-agent` that cannot prompt for a passphrase there fails silently
and the commit is still created.
