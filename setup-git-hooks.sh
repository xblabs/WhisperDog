#!/bin/bash
#
# Setup script to install git hooks for WhisperDog
# Run this after cloning to enable automatic CHANGELOG.md updates
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_SOURCE="$SCRIPT_DIR/.githooks"
HOOKS_DEST="$SCRIPT_DIR/.git/hooks"

echo "📂 Installing WhisperDog git hooks..."

if [ ! -d "$HOOKS_SOURCE" ]; then
    echo "❌ Error: .githooks directory not found"
    exit 1
fi

if [ ! -d "$HOOKS_DEST" ]; then
    echo "❌ Error: .git/hooks directory not found - is this a git repository?"
    exit 1
fi

# Install each hook
for hook in "$HOOKS_SOURCE"/*; do
    if [ -f "$hook" ]; then
        hook_name=$(basename "$hook")
        dest_path="$HOOKS_DEST/$hook_name"

        # Backup existing hook if present
        if [ -f "$dest_path" ] && [ ! -f "$dest_path.bak" ]; then
            echo "  📦 Backing up existing $hook_name to $hook_name.bak"
            cp "$dest_path" "$dest_path.bak"
        fi

        # Copy and make executable
        cp "$hook" "$dest_path"
        chmod +x "$dest_path"
        echo "  ✅ Installed: $hook_name"
    fi
done

echo ""
echo "🎉 Git hooks installed successfully!"
echo ""
echo "The following hooks are now active:"
echo "  • post-commit: Auto-updates CHANGELOG.md for conventional commits"
echo ""
echo "Commit message format for auto-changelog:"
echo "  feat: description  → Added section"
echo "  fix: description   → Fixed section"
echo "  docs: description  → Documentation section"
echo "  refactor: desc     → Changed section"
echo "  perf: description  → Changed section"
