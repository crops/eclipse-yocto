#! /bin/sh
[ -e config.cache ] && rm -f config.cache

libtoolize --automake
aclocal ${POKY_ACLOCAL_OPTS}
autoconf
autoheader
automake -a
./configure $@
exit
