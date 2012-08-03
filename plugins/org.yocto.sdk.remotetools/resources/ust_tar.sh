#!/bin/sh

#set PATH to include sbin dirs
export PATH="$PATH:/usr/local/sbin:/usr/sbin:/sbin"

if [ ! -d "$@" ] || [ -z "$@" ]; then
  exit 1
fi

DATESTRING="$(date +%Y%m%d%H%M%S%N)"
BASENAME=`basename $@`
DATAFILE=/tmp/${BASENAME}-${DATESTRING}.tar
cd $@
cd ..

tar -cf  ${DATAFILE} ${BASENAME} &> /dev/null || exit $?

echo -e "ustfile:$DATAFILE\n"

