#!/bin/sh
cd "$(dirname $0)"
echo $*
exec ./driver $*
