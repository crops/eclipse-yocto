#!/bin/sh

#set PATH to include sbin dirs
export PATH="$PATH:/usr/local/sbin:/usr/sbin:/sbin"

DATESTRING="$(date +%Y%m%d%H%M%S%N)"
TEMPFILE="/tmp/yocto-ust-tmp-$DATESTRING"

rm -f ${TEMPFILE}
usttrace $@ &> ${TEMPFILE}
ret=$?

if [ $ret -ne 0 ]; then
  cat $TEMPFILE
  rm -f $TEMPFILE
  exit $ret
fi

#search for output dir
USTDIR=`cat ${TEMPFILE} | awk '/^Trace was output in:/ { print $5}'`
rm -f ${TEMPFILE}

if [ -z "$USTDIR" ]; then
  exit 1
fi

BASENAME=`basename $USTDIR`
DATAFILE=/tmp/${BASENAME}.tar
cd $USTDIR
cd ..

tar -cf  ${DATAFILE} ${BASENAME} &> /dev/null || exit $?

echo -e "ustfile:$DATAFILE\n"

