#!/bin/sh

help()
{
	echo "Generate and add eclipse help from yocto's documentation"
	echo -e "Usage: $0 BRANCH_NAME | TAG_NAME PLUGIN_FOLDER\n"
	echo "Options:"
	echo "-h - display this help and exit"
	echo "TAG_NAME - tag to build the documentation upon"
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

safe_sed()
{
  if [ "$1" ] && [ "$2" ]; then
     sed -e "$1" $2 > $2.tmp && mv $2.tmp $2 || fail $? "safe_sed $1 $2"
  else
     fail 1 "usage: safe_sed \"s/from/to/\" /path/to/file"
  fi
}

CHECKOUT_TAG=0
while getopts ":h" opt; do
	case $opt in
		h)
			help
			;;
	esac
done
shift $(($OPTIND - 1))

if [ $# -ne 2 ]; then
	help
fi

PLUGIN_FOLDER=`readlink -f $2`

TOP=`pwd`

DOC_DIR=${PLUGIN_FOLDER}/docs
rm -rf ${DOC_DIR}
DOC_PLUGIN_DIR=${PLUGIN_FOLDER}/plugins/org.yocto.doc.user
DOC_HTML_DIR=${DOC_PLUGIN_DIR}/html/

# git clone
DOC_GIT=http://git.yoctoproject.org/git/yocto-docs
git clone ${DOC_GIT} ${DOC_DIR} || fail $? "git clone ${DOC_GIT}"
cd ${DOC_DIR}

git checkout $1 || fail $? "git checkout $1"
COMMIT_ID=`git rev-parse HEAD`

# check yocto-docs branch/tag and build sdk-manual or adt-manual accordingly
# [YOCTO #9622]
if [ "$1" = "master" ] || [ "$1" = "krogoth" ] || [ "$1" = "yocto-2.1" ]; then
  DOCS="yocto-project-qs sdk-manual kernel-dev \
        bsp-guide ref-manual dev-manual profile-manual"
else
  DOCS="yocto-project-qs adt-manual kernel-dev \
        bsp-guide ref-manual dev-manual profile-manual"
  safe_sed "s/sdk-manual/adt-manual/" ${DOC_PLUGIN_DIR}/plugin.xml
  safe_sed "s/sdk-manual/adt-manual/" ${DOC_PLUGIN_DIR}/about.html.in
  safe_sed "s/Software Development Kit (SDK)/Application/" ${DOC_PLUGIN_DIR}/about.html.in
  safe_sed "s/(SDK)/(ADT)/" ${DOC_PLUGIN_DIR}/about.html.in
  safe_sed "s/sdk-manual/adt-manual/" ${DOC_PLUGIN_DIR}/toc.xml
  safe_sed "s/Software Development Kit (SDK)/Application/" ${DOC_PLUGIN_DIR}/toc.xml
fi

# build and copy
cd documentation
for DOC in ${DOCS}; do
	make DOC=${DOC} eclipse
	cp -rf ${DOC}/eclipse/html/* ${DOC_HTML_DIR}
	cp -f ${DOC}/eclipse/${DOC}-toc.xml ${DOC_HTML_DIR}
done

sed -e "s/@.*@/${COMMIT_ID}/" < ${DOC_PLUGIN_DIR}/about.html.in > ${DOC_PLUGIN_DIR}/about.html

cd ${TOP}
