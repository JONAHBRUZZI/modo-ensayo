#!/bin/bash
if [ "$GIT_AUTHOR_EMAIL" = "jonat@modoensayo.cl" ]; then
    export GIT_AUTHOR_EMAIL="jon.guerra@duocuc.cl"
    export GIT_AUTHOR_NAME="Jonathan Guerra"
fi
if [ "$GIT_COMMITTER_EMAIL" = "jonat@modoensayo.cl" ]; then
    export GIT_COMMITTER_EMAIL="jon.guerra@duocuc.cl"
    export GIT_COMMITTER_NAME="Jonathan Guerra"
fi
