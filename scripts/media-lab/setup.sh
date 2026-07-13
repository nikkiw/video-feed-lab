#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)
REPOSITORY_URL=${MEDIA_LAB_REPOSITORY_URL:-https://github.com/nikkiw/universal-media-lab.git}
MEDIA_LAB_REF=${MEDIA_LAB_REF:-b1c7da8210774d32c88b37172443499705204fbb}
TARGET_DIR=${MEDIA_LAB_DIR:-$ROOT_DIR/.local/universal-media-lab}
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

CURRENT_REF=""
if [ -f "$MARKER_FILE" ]; then
  CURRENT_REF=$(cat "$MARKER_FILE")
fi

if [ "$CURRENT_REF" != "$MEDIA_LAB_REF" ]; then
  printf 'Checking out commit %s (previous: %s)...\n' "$MEDIA_LAB_REF" "${CURRENT_REF:-none}"
  
  # Check if there are local modifications to tracked files
  if [ -n "$(git -C "$TARGET_DIR" status --porcelain -uno)" ]; then
    fail "checkout at $TARGET_DIR has local modifications to tracked files; please discard them first"
  fi

  git -C "$TARGET_DIR" fetch --depth 1 origin "$MEDIA_LAB_REF"
  git -C "$TARGET_DIR" checkout --detach FETCH_HEAD
  
  mkdir -p "$TARGET_DIR/gateway/project-routes"
  cp "$ROUTE_SOURCE" "$TARGET_DIR/gateway/project-routes/video-feed-lab.conf"
  printf '%s\n' "$MEDIA_LAB_REF" > "$MARKER_FILE"
  printf 'Configured Nginx route overlay.\n'
else
  # Just refresh route configuration if it changed
  if ! cmp -s "$ROUTE_SOURCE" "$TARGET_DIR/gateway/project-routes/video-feed-lab.conf"; then
    cp "$ROUTE_SOURCE" "$TARGET_DIR/gateway/project-routes/video-feed-lab.conf"
    printf 'Refreshed video-feed-lab Nginx route overlay.\n'
  fi
fi

if [ ! -f "$TARGET_DIR/.env" ]; then
  cp "$TARGET_DIR/.env.example" "$TARGET_DIR/.env"
  printf 'Created %s/.env from .env.example.\n' "$TARGET_DIR"
fi

# Ensure MEDIA_URL_PREFIX is set to /vfl/media in the .env file
if grep -q '^MEDIA_URL_PREFIX=' "$TARGET_DIR/.env"; then
  tmp_env="$TARGET_DIR/.env.tmp"
  awk '
    BEGIN { FS=OFS="=" }
    $1 == "MEDIA_URL_PREFIX" { $2 = "/vfl/media" }
    { print }
  ' "$TARGET_DIR/.env" > "$tmp_env" && mv "$tmp_env" "$TARGET_DIR/.env"
else
  printf '\nMEDIA_URL_PREFIX=/vfl/media\n' >> "$TARGET_DIR/.env"
fi
printf 'Configured MEDIA_URL_PREFIX=/vfl/media in .env\n'

cat <<EOF

Universal Media Lab is ready at:
  $TARGET_DIR

Next:
  1. Put a licensed source video in:
     $TARGET_DIR/media/inbox/
  2. Run:
     make media-lab-bootstrap
EOF
