#!/bin/bash
MODEL="$1"
PROMPT="$2"
PATH=$HOME/.npm-global/bin:$PATH opencode run --attach http://127.0.0.1:36361 --password d242bac4-b1c9-49f0-a292-a84a4b685c2b --model "$MODEL" "$PROMPT"