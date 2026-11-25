#!/bin/sh
# Read-only access to the Venus containers for Claude, used as a forced SSH command:
# the key in authorized_keys can only run this script, whatever command the client asks for.
#
#   ssh venus-nas-logs ps                         -> status of the Venus containers
#   ssh venus-nas-logs logs venus-backend [lines] -> last lines of a container's logs (max 5000)
#
# Install a root-owned copy outside the git checkout (see ops/nas/README.md): the deployment pulls
# the repository, so a copy inside it could be changed by any commit.

DOCKER=/usr/local/bin/docker
set -f                       # no glob expansion of the client's words
set -- $SSH_ORIGINAL_COMMAND

case "$1" in
  ps)
    [ $# -eq 1 ] || { echo "usage: ps" >&2; exit 2; }
    exec sudo -n "$DOCKER" ps --all --filter "name=venus-" --format '{{.Names}}\t{{.Status}}\t{{.Image}}'
    ;;
  logs)
    [ $# -ge 2 ] && [ $# -le 3 ] || { echo "usage: logs <container> [lines]" >&2; exit 2; }
    case "$2" in
      venus-backend|venus-frontend|venus-db|venus-phpmyadmin) ;;
      *) echo "unknown container: only venus-backend, venus-frontend, venus-db, venus-phpmyadmin" >&2; exit 2 ;;
    esac
    lines="${3:-300}"
    case "$lines" in
      ''|*[!0-9]*) echo "lines must be a number" >&2; exit 2 ;;
    esac
    [ "$lines" -gt 5000 ] && lines=5000
    exec sudo -n "$DOCKER" logs --timestamps --tail "$lines" "$2" 2>&1
    ;;
  *)
    echo "usage: ps | logs <venus-backend|venus-frontend|venus-db|venus-phpmyadmin> [lines]" >&2
    exit 2
    ;;
esac
