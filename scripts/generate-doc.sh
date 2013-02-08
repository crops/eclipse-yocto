#!/bin/sh

help()
{
	echo "Generate and add eclipse help from yocto's documentation"
	echo "Usage: $0 BRANCH_NAME PLUGIN_FOLDER"
	echo "       $0 -t TAG_NAME PLUGIN_FOLDER"
	echo ""
	echo "Options:"
	echo "-h - display this help and exit"
	echo "-t TAG_NAME - tag to build the documentation upon"
	echo "BRANCH_NAME - branch to build the documentation upon"
	echo "PLUGIN_FOLDER - root folder of the eclipse-poky project"
	exit 1
}

fail ()
{
  local retval=$1
  shift $1
  echo "[Fail $retval]: $*"
  echo "BUILD_TOP=${BUILD_TOP}"
  cd ${TOP}
  exit ${retval}
}

CHECKOUT_TAG=0
while getopts ":ht" opt; do
	case $opt in
		h)
			help
			;;
		t)
			CHECKOUT_TAG=1
			;;
	esac
done
shift $(($OPTIND - 1))

if [ $# -ne 2 ]; then
	help
fi

if [ $CHECKOUT_TAG -eq 0 ]; then
	REFERENCE=origin/$1
else
	REFERENCE=$1
fi
PLUGIN_FOLDER=`readlink -f $2`

TOP=`pwd`

DOC_DIR=${PLUGIN_FOLDER}/docs
rm -rf ${DOC_DIR}
DOC_PLUGIN_DIR=${PLUGIN_FOLDER}/plugins/org.yocto.doc.user
DOC_HTML_DIR=${DOC_PLUGIN_DIR}/html/

# git clone
#DOC_GIT=git://git.yoctoproject.org/yocto-docs.git
DOC_GIT=file:///home/timo/_dev/oss/yocto/yocto-docs
git clone ${DOC_GIT} ${DOC_DIR} || fail $? "git clone ${DOC_GIT}"
cd ${DOC_DIR}
git checkout ${REFERENCE} || fail $? "git checkout ${REFERENCE}"
COMMIT_ID=`git rev-parse HEAD`

# build and copy
DOCS="yocto-project-qs adt-manual kernel-dev \
      bsp-guide ref-manual dev-manual profile-manual"

cd documentation
ECLIPSE_TARGET_AVAILABLE=`make -q eclipse &> /dev/null; echo $?`
if [ ${ECLIPSE_TARGET_AVAILABLE} -ne 1 ]; then
	echo "WARNING:"
	echo "This version does not support generating eclipse help"
	echo "Documentation will not be available in eclipse"
	exit 1
fi

for DOC in ${DOCS}; do
	make DOC=${DOC} eclipse
done

sed -e "s/@.*@/${COMMIT_ID}/" < ${DOC_PLUGIN_DIR}/about.html.in > ${DOC_PLUGIN_DIR}/about.html

cd ${TOP}
