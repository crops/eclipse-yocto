#!/bin/sh

#setup eclipse building environment for Indigo.
#comment out the following line if you want to using your own http proxy setting for eclipse update site
#PROXY=http://proxy.jf.intel.com:911

err_exit() 
{
  echo "[FAILED $1]$2"
  exit $1
}

curdir=`pwd`

uname_s=`uname -s`
uname_m=`uname -m`
case ${uname_s}${uname_m} in
  Linuxppc*) ep_arch=linux-gtk-ppc
             cdt_arch=linux.ppc
             ;;
  Linuxx86_64*) ep_arch=linux-gtk-x86_64 
                cdt_arch=linux.x86_64
                ;;
  Linuxi*86) ep_arch=linux-gtk
             cdt_arch=linux.x86
             ;;
  *)
    echo "Unknown ${uname_s}${uname_m}"
    exit 1
    ;;
esac

#parsing proxy URLS
url=${PROXY}
if [ "x$url" != "x" ]; then
    proto=`echo $url | grep :// | sed -e 's,^\(.*://\).*,\1,g'`
    url=`echo $url | sed s,$proto,,g`
    userpass=`echo $url | grep @ | cut -d@ -f1`
    user=`echo $userpass | cut -d: -f1`
    pass=`echo $userpass | grep : | cut -d: -f2`
    url=`echo $url | sed s,$userpass@,,g`
    host=`echo $url | cut -d: -f1`
    port=`echo $url | cut -d: -f2 | sed -e 's,[^0-9],,g'`
    [ "x$host" = "x" ] && err_exit 1 "Undefined proxy host"
    PROXY_PARAM="-Dhttp.proxySet=true -Dhttp.proxyHost=$host"
    [ "x$port" != "x" ] && PROXY_PARAM="${PROXY_PARAM} -Dhttp.proxyPort=$port"
fi


# prepare the base Eclipse installation in folder "eclipse"
ep_rel="R-"
ep_ver="4.2"
ep_date="-201206081400"
P2_disabled=false
P2_no_dropins=false
if [ ! -f eclipse/plugins/org.eclipse.swt_3.100.0.v4233d.jar ]; then
  curdir2=`pwd`
  if [ ! -d eclipse -o -h eclipse ]; then
    if [ -d eclipse-${ep_ver}-${ep_arch} ]; then
      rm -rf eclipse-${ep_ver}-${ep_arch}
    fi
    mkdir eclipse-${ep_ver}-${ep_arch}
    cd eclipse-${ep_ver}-${ep_arch}
  else
    rm -rf eclipse
  fi
  # Eclipse SDK: Need the SDK so we can link into docs
  echo "Getting Eclipse SDK..."
  # wget "http://download.eclipse.org/eclipse/downloads/drops4/${ep_rel}${ep_ver}${ep_date}/eclipse-SDK-${ep_ver}-${ep_arch}.tar.gz"
  # The eclipse site has moments where it is overloaded. Maintaining our own mirror solves this.
  wget "http://downloads.yoctoproject.org/eclipse/downloads/drops4/${ep_rel}${ep_ver}${ep_date}/eclipse-SDK-${ep_ver}-${ep_arch}.tar.gz"
  tar xfz eclipse-SDK-${ep_ver}-${ep_arch}.tar.gz || err_exit $? "extracting Eclipse SDK failed"
  rm eclipse-SDK-${ep_ver}-${ep_arch}.tar.gz
  cd "${curdir2}"
  if [ ! -d eclipse -o -h eclipse ]; then
    if [ -e eclipse ]; then 
      rm eclipse
    fi
    ln -s eclipse-${ep_ver}-${ep_arch}/eclipse eclipse
  fi
fi
if [ ! -f eclipse/startup.jar ]; then
  curdir2=`pwd`
  cd eclipse/plugins
  if [ -h ../startup.jar ]; then
    rm ../startup.jar
  fi
  LAUNCHER="`ls org.eclipse.equinox.launcher_*.jar | sort | tail -1`"
  if [ "x${LAUNCHER}" != "x" ]; then
    echo "eclipse LAUNCHER=${LAUNCHER}" 
    ln -s plugins/${LAUNCHER} ../startup.jar
  else
    echo "Eclipse: NO startup.jar LAUNCHER FOUND!"
  fi
  cd ${curdir2}
fi

LAUNCHER="eclipse/startup.jar"

get_version()
{
#$1: repository_url
#$2: featureId
#$3: 'all' or 'max' or 'min', 'max' if not specified
  local remote_vers="`java ${PROXY_PARAM} \
    -jar ${LAUNCHER} \
    -application org.eclipse.equinox.p2.director \
    -destination ${curdir}/eclipse \
    -profile SDKProfile \
    -repository $1 \
    -list $2\
    | awk 'BEGIN { FS="=" } { print $2 }'`"

  #find larget remote vers
  local remote_ver="`echo ${remote_vers} | cut -d ' ' -f1`"
  case $3 in
    all)
      remote_ver=${remote_vers}
      ;;
    min)
      for i in ${remote_vers}; do
        [ "${remote_ver}" \> "$i" ] && remote_ver="$i"
      done
      ;;
    *)
      for i in ${remote_vers}; do
        [ "${remote_ver}" \< "$i" ] && remote_ver="$i"
      done
      ;;
  esac

  echo ${remote_ver}
}

check_local_version()
{
# $1 unitId
# $2 min version
# $3 max version (optional)
  version="`get_version file:///${curdir}/eclipse/p2/org.eclipse.equinox.p2.engine/profileRegistry/SDKProfile.profile $1`"
  [ "$version" \< "$2" ] && return 1
  if [ "x$3" != "x" ]; then
    [ "$version" \> "$3" ] && return -1
  fi
  return 0
}

update_feature_remote()
{
# install a feature of with version requirement [min, max)
#$1: reporsitory url
#$2: featureId
#$3: min version
#$4: max version(optional)
  [ $# -lt 3 ] && err_exit 1 "update_feature_remote: invalid parameters, $*"
  check_local_version $2 $3 $4 && echo "skip installed feature $2" && return 0
  local installIU=""
  if [ "x$4" != "x" ]; then
      #has max version requirement
      for i in "`get_version $1 $2 'all'`"; do
        if [ "$i" \> "$3" ] || [ "$i" = "$3" ] && [ "$i" \< "$4" ]; then
          [ "$i" \> "$installIU" ] && installIU=$i
        fi
      done
  else
      #only has minimum version requirement
      local max_remote_ver="`get_version $1 $2 'max'`"
      [ "$max_remote_ver" \> "$3" ] || [ "$max_remote_ver" = "$3" ] && installIU=$max_remote_ver
  fi

  [ "x$installIU" = "x" ] && err_exit 1 "Can NOT find candidates of $2 version($3, $4) at $1!"
  installIU="$2/$installIU"
  echo "try to install $installIU ..."
  java ${PROXY_PARAM} -jar ${LAUNCHER} \
    -application org.eclipse.equinox.p2.director \
    -destination ${curdir}/eclipse \
    -profile SDKProfile \
    -repository $1 \
    -installIU ${installIU} || err_exit $? "installing ${installIU} failed"
}

#Eclipse Update Site
#MAIN_UPDATE_SITE="http://download.eclipse.org/releases/juno"
# The main eclipse download site is unreliable at times. For now, we're going to
# maintain a mirror of just what we need.
MAIN_UPDATE_SITE="http://downloads.yoctoproject.org/eclipse/juno/ftp.osuosl.org/pub/eclipse/releases/juno"

UPDATE_SITE="${MAIN_UPDATE_SITE}"

#CDT related
CDTFEAT="8.1.0"
echo "Installing CDT..."
update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.sdk.feature.group ${CDTFEAT}
CDTREMOTEVER="6.0.0"
update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.launch.remote.feature.group ${CDTREMOTEVER}

#RSE SDK
RSEVER="3.4.0"
TCFVER="1.0.0"
TMVER="3.3.0"
echo "Installing RSE/TCF/TM related component..."
update_feature_remote ${UPDATE_SITE} org.eclipse.rse.feature.group ${RSEVER}
update_feature_remote ${UPDATE_SITE} org.eclipse.tcf.rse.feature.feature.group ${TCFVER}
update_feature_remote ${UPDATE_SITE} org.eclipse.tm.terminal.sdk.feature.group ${TMVER}

#AUTOTOOL
ATVER="3.0.1"
echo "Install AutoTool..."
update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.autotools.feature.group ${ATVER}

#Lttng legacy
TMFREL="1.0.0"
echo "Install TMF..."
update_feature_remote ${UPDATE_SITE} org.eclipse.linuxtools.tmf.feature.group ${TMFREL}

echo ""
echo "Your build environment is successfully created."
echo "Run ECLIPSE_HOME=${curdir}/eclipse `dirname $0`/build.sh <branch name> <release name> to build"
echo ""

exit 0
