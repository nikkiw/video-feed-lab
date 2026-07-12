#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)
REPOSITORY_URL=${MEDIA_LAB_REPOSITORY_URL:-https://github.com/nikkiw/universal-media-lab.git}
MEDIA_LAB_REF=${MEDIA_LAB_REF:-9215010ecc4730f47e5c810a2dabae43ffc36dac}
TARGET_DIR=${MEDIA_LAB_DIR:-$ROOT_DIR/.local/universal-media-lab}
PATCH_FILE=$ROOT_DIR/tools/media-lab/patches/upstream-gateway.patch
ROUTE_SOURCE=$ROOT_DIR/tools/media-lab/overlays/video-feed-lab.conf
MARKER_FILE=$TARGET_DIR/.git/video-feed-lab-customization

fail() {
  printf 'media-lab setup: %s\n' "$*" >&2
  exit 1
}

if [ ! -d "$TARGET_DIR/.git" ]; then
  [ ! -e "$TARGET_DIR" ] || fail "$TARGET_DIR exists but is not a Git checkout"
  mkdir -p "$(dirname -- "$TARGET_DIR")"
  printf 'Cloning %s into %s\n' "$REPOSITORY_URL" "$TARGET_DIR"
  git clone "$REPOSITORY_URL" "$TARGET_DIR"
fi

ACTUAL_REMOTE=$(git -C "$TARGET_DIR" remote get-url origin)
[ "$ACTUAL_REMOTE" = "$REPOSITORY_URL" ] ||
  fail "origin is $ACTUAL_REMOTE; expected $REPOSITORY_URL"

if [ -f "$MARKER_FILE" ]; then
  APPLIED_REF=$(cat "$MARKER_FILE")
  [ "$APPLIED_REF" = "$MEDIA_LAB_REF" ] ||
    fail "checkout is customized for $APPLIED_REF; use a new MEDIA_LAB_DIR for $MEDIA_LAB_REF"
  git -C "$TARGET_DIR" apply --reverse --check "$PATCH_FILE" ||
    fail "the upstream gateway patch is missing or was modified"
  if ! cmp -s "$ROUTE_SOURCE" "$TARGET_DIR/gateway/project-routes/video-feed-lab.conf"; then
    cp "$ROUTE_SOURCE" "$TARGET_DIR/gateway/project-routes/video-feed-lab.conf"
    printf 'Refreshed video-feed-lab Nginx route overlay.\n'
  fi
  printf 'GitHub checkout is already customized: %s\n' "$TARGET_DIR"
else
  [ -z "$(git -C "$TARGET_DIR" status --porcelain)" ] ||
    fail "checkout has local changes before customization"

  git -C "$TARGET_DIR" fetch --depth 1 origin "$MEDIA_LAB_REF"
  git -C "$TARGET_DIR" checkout --detach FETCH_HEAD

  git -C "$TARGET_DIR" apply --check "$PATCH_FILE"
  git -C "$TARGET_DIR" apply "$PATCH_FILE"
  mkdir -p "$TARGET_DIR/gateway/project-routes"
  cp "$ROUTE_SOURCE" "$TARGET_DIR/gateway/project-routes/video-feed-lab.conf"
  printf '%s\n' "$MEDIA_LAB_REF" > "$MARKER_FILE"
  printf 'Applied video-feed-lab Nginx customization.\n'
fi

if [ ! -f "$TARGET_DIR/.env" ]; then
  cp "$TARGET_DIR/.env.example" "$TARGET_DIR/.env"
  printf 'Created %s/.env from .env.example.\n' "$TARGET_DIR"
fi

cat <<EOF

Universal Media Lab is ready at:
  $TARGET_DIR

Next:
  1. Put a licensed source video in:
     $TARGET_DIR/media/inbox/
  2. Run:
     make media-lab-bootstrap
EOF
