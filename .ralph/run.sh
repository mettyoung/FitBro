#!/bin/bash
# Ralph loop for FitBro — runs Claude Code autonomously, one PRD story per iteration.
# Usage: .ralph/run.sh [max_iterations]   (default 8)
# Reads: prd.json + progress.txt at repo root; prompt at .ralph/prompt.md
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
PROMPT="$ROOT/.ralph/prompt.md"
PRD="$ROOT/prd.json"
MAX_ITERATIONS="${1:-8}"

command -v jq >/dev/null || { echo "jq required"; exit 1; }
command -v claude >/dev/null || { echo "claude CLI required"; exit 1; }

echo "Ralph start — max $MAX_ITERATIONS iterations — branch $(jq -r .branchName "$PRD")"

for i in $(seq 1 "$MAX_ITERATIONS"); do
  remaining=$(jq '[.userStories[] | select(.passes == false)] | length' "$PRD")
  echo ""
  echo "=== Iteration $i/$MAX_ITERATIONS — $remaining stories remaining ==="
  if [ "$remaining" -eq 0 ]; then
    echo "All stories pass. Done."
    exit 0
  fi

  OUTPUT=$(claude --dangerously-skip-permissions --print < "$PROMPT" 2>&1 | tee /dev/stderr) || true

  if echo "$OUTPUT" | grep -q "<promise>COMPLETE</promise>"; then
    echo ""
    echo "Ralph signalled COMPLETE at iteration $i."
    exit 0
  fi
  echo "--- iteration $i done ---"
  sleep 2
done

echo "Reached max iterations without COMPLETE. See progress.txt."
exit 1
