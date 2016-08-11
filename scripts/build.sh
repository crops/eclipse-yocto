#!/bin/bash

#setup Yocto Eclipse plug-in build environment for Neon
#comment out the following line if you wish to use your own http proxy settings
#export http_proxy=http://proxy.yourproxyinfo.com:8080

help ()
{
  echo "Build the Yocto Eclipse plugins"
  echo "Usage: $0 [OPTIONS] (PLUGIN_BRANCH_NAME | PLUGIN_TAG_NAME) && (DOC_BRANCH_NAME | DOC_TAG_NAME)
  RELEASE_NAME\n";
  echo "Example: $0 plugin_branch doc_tag my_release"
  echo "Options:"
  echo "-h - display this help and exit"
  echo "-l - use local git repository"
  echo "BRANCH_NAME - git branch name to build upon"
  echo "RELEASE_NAME - release name in the final output name"
  echo "TAG_NAME - git tag name to build upon\n"
  echo "Example [branch]: $0 luna master r0";
  exit 1;
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

find_eclipse_base ()
{
  [ -d ${ECLIPSE_HOME}/plugins ] &&  ECLIPSE_BASE=${ECLIPSE_HOME}
}

find_launcher ()
{
  local list="`ls ${ECLIPSE_BASE}/plugins/org.eclipse.equinox.launcher_*.jar`"
  for launcher in $list; do
    [ -f $launcher ] && LAUNCHER=${launcher}
  done
}

find_buildfile ()
{
  local list="`ls ${ECLIPSE_BASE}/plugins/org.eclipse.pde.build_*/scripts/build.xml`"
  for buildfile in $list; do
    [ -f $buildfile ] && BUILDFILE=${buildfile}
  done
}

check_env ()
{
  find_eclipse_base
  find_launcher
  find_buildfile
  
  local err=0
  [ "x${ECLIPSE_BASE}" = "x" -o "x${LAUNCHER}" = "x" -o "x${BUILDFILE}" = "x" ] && err=1
  if [ $err -eq 0 ]; then
    [ ! -d ${ECLIPSE_BASE} ] && err=1
    [ ! -f ${LAUNCHER} ] && err=1
    [ ! -f ${BUILDFILE} ] && err=1
  fi
  
  if [ $err -ne 0 ]; then
    echo "Please set env variable ECLIPSE_HOME to the eclipse installation directory!" 
    exit 1
  fi 
}

USE_LOCAL_GIT_REPO=0

while getopts ":lh" opt; do
	case $opt in
		h)
			help
			;;
		l)
			USE_LOCAL_GIT_REPO=1
			;;
	esac
done
shift $(($OPTIND - 1))


if [ $# -ne 3 ]; then
   help
fi


PLUGIN_REF=$1
DOC_REF=$2
RELEASE=$3

TOP=`pwd`

check_env

#create tmp dir for build
DATE=`date +%Y%m%d%H%M`
BUILD_TOP=`echo $1 | sed 's%/%-%'`
BUILD_TOP=${TOP}/${BUILD_TOP}_build_${DATE}
rm -rf ${BUILD_TOP}
mkdir ${BUILD_TOP} || fail $? "Create temporary build directory ${BUILD_TOP}"
BUILD_SRC=${BUILD_TOP}/src
BUILD_DIR=${BUILD_TOP}/build
mkdir ${BUILD_DIR} || fail $? "Create temporary build directory ${BUILD_DIR}"

#git clone
GIT_URL=http://git.yoctoproject.org/git/eclipse-poky
if [ $USE_LOCAL_GIT_REPO -eq 1 ]; then
	SCRIPT_DIR=`dirname $0`
	GIT_DIR=`readlink -f ${SCRIPT_DIR}/..`
	GIT_URL="file://${GIT_DIR}"
fi

GIT_DIR=${BUILD_SRC}

git clone ${GIT_URL} ${GIT_DIR} || fail $? "git clone ${GIT_URL}" 

cd ${GIT_DIR}

echo -e "\nChecking out ${PLUGIN_REF}\n"
git checkout ${PLUGIN_REF} || fail $? "git checkout ${PLUGIN_REF}"

cd ${TOP}

# generate and add documentation
echo -e "\nGenerate Yocto documentation\n"
${GIT_DIR}/scripts/generate-doc.sh ${DOC_REF} ${GIT_DIR} || fail $? "generate documentation"

#build 
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.bc.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} || fail $? "normal build"
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.doc.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} || fail $? "normal build"
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.sdk.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} || fail $? "normal build"

if [ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}.zip ] &&
	[ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.bc-${RELEASE}.zip ] &&
	[ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.doc-${RELEASE}.zip ]; then
  cp -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.bc-${RELEASE}.zip ./org.yocto.bc-${RELEASE}-${DATE}.zip
  cp -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.doc-${RELEASE}.zip ./org.yocto.doc-${RELEASE}-${DATE}.zip
  cp -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}.zip ./org.yocto.sdk-${RELEASE}-${DATE}.zip
  rm -rf ${BUILD_DIR}
else
  fail 1 "Not finding normal build output"
fi

#build archive for update site
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.bc.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} -Dp2.gathering=true || fail $? "archive build"
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.doc.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} -Dp2.gathering=true || fail $? "archive build"
java -jar ${LAUNCHER} -application org.eclipse.ant.core.antRunner -buildfile ${BUILDFILE} -DbaseLocation=${ECLIPSE_BASE} -Dbuilder=${GIT_DIR}/features/org.yocto.sdk.headless.build -DbuildDirectory=${BUILD_DIR} -DotherSrcDirectory=${GIT_DIR} -DbuildId=${RELEASE} -Dp2.gathering=true || fail $? "archive build"

#clean up
if [ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}-group.group.group.zip ] &&
	[ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.bc-${RELEASE}-group.group.group.zip ] &&
	[ -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.doc-${RELEASE}-group.group.group.zip ]; then
  cp -f ${BUILD_DIR}/I.${RELEASE}/org.yocto.sdk-${RELEASE}-group.group.group.zip ./org.yocto.sdk-${RELEASE}-${DATE}-archive.zip
  rm -rf ${BUILD_TOP}
else
  fail 1 "Not finding archive build output"
fi 

exit 0
