---
name: commit-and-push
description: Commit all current changes and push immediately after implementation. Use when the user says "commit", "commit and push", "save the changes", "push the changes", "let's commit", or similar after finishing work. Generates a clean, minimal single-line commit message with no co-author attribution, no footer, and no verbosity — just a precise imperative summary of what changed.
---

# Commit and Push

Stage every changed file, write one clean commit message, and push. Nothing more.

## The commit message rule

One line. Imperative mood. ≤72 characters. No period. No footer. No co-author attribution. No preamble.

Good: `Add JWT refresh token rotation`
Good: `Fix balance calculation for split transactions`
Good: `Remove deprecated UserSession middleware`

Bad: `This commit adds...`
Bad: `Update stuff`
Bad: `feat: implement auth (see details below)\n\nCo-Authored-By: Claude`

The message captures the essential *what* — not the how, not the why, not who wrote it.

## Steps

1. Run `git status` and `git diff HEAD` to read every change. If nothing is staged yet, use `git diff` instead.

2. Write the commit message. One line, imperative, ≤72 chars. If the changes span several unrelated areas, pick the most significant one and note the others briefly in the same line (e.g., `Add budget alerts and fix category seed order`).

3. Stage everything: `git add -A`

4. Commit — pass the message directly with `-m`, never via heredoc:
   ```bash
   git commit -m "Your message here"
   ```

5. Push on the current branch:
   ```bash
   git push
   ```

## Hard rules

- Never append `Co-Authored-By:` or any footer
- Never write a multi-line message body
- Never use `--no-verify` unless the user explicitly asks
- If the push is rejected (non-fast-forward), tell the user — do not force-push without explicit permission
