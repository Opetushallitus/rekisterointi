#!/usr/bin/env bash
set -o errexit -o nounset -o pipefail

function require_command {
  if ! command -v "$1" > /dev/null; then
    fatal "I require $1 but it's not installed. Aborting."
  fi
}

require_command tmux
require_command mvn
require_command npm

session="rekisterointi"

tmux kill-session -t $session || true
tmux start-server
tmux new-session -d -s $session

tmux select-pane -t 0 -T server
echo "starting server"
tmux send-keys "mvn spring-boot:run" C-m

tmux splitw -h

tmux select-pane -t 1 -T frontend
echo "starting frontend"
tmux send-keys "cd rekisterointi-ui && npm run build:watch" C-m

tmux select-pane -t 0
tmux attach-session -t $session
