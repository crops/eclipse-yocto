#comment out the following 2 lines if you don't want to use the http proxy for update site
PROXY=proxy-shz.intel.com
PORT=911

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
  Linuxx86*) ep_arch=linux-gtk
             cdt_arch=linux.x86
             ;;
esac

# prepare the base Eclipse installation in folder "eclipse"
ep_rel="R-"
ep_ver=3.6
ep_date="-201006080911"
P2_disabled=false
P2_no_dropins=false
if [ ! -f eclipse/plugins/org.eclipse.swt_3.6.0.v3650b.jar ]; then
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
  wget "http://download.eclipse.org/eclipse/downloads/drops/${ep_rel}${ep_ver}${ep_date}/eclipse-SDK-${ep_ver}-${ep_arch}.tar.gz"
  tar xfvz eclipse-SDK-${ep_ver}-${ep_arch}.tar.gz
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
  LAUNCHER=`ls org.eclipse.equinox.launcher_*.jar | sort | tail -1`
  if [ "${LAUNCHER}" != "" ]; then
    echo "eclipse LAUNCHER=${LAUNCHER}" 
    ln -s plugins/${LAUNCHER} ../startup.jar
  else
    echo "Eclipse: NO startup.jar LAUNCHER FOUND!"
  fi
  cd ${curdir2}
fi

if ${P2_no_dropins} ; then
  #P2 disabled?
  DROPIN=.
  DROPUP=.
else
  #P2 enabled
  DROPIN=eclipse/dropins
  DROPUP=../..
fi

LAUNCHER=`ls eclipse/plugins/org.eclipse.equinox.launcher_*.jar | sort | tail -1`
update_feature_remote()
{
#$1: remote_site_url
#$2: featureId
#$3: desired version (optional)
  if [ "x${PROXY}" != "x" ]; then
     PROXY_PARAM="-Dhttp.proxySet=true -Dhttp.proxyHost=${PROXY} -Dhttp.proxyPort=${PORT}"
  fi
  
  local remote_ver=`java ${PROXY_PARAM} \
    -jar ${LAUNCHER} \
    -application org.eclipse.update.core.standaloneUpdate \
    -command search \
    -from $1 \
    | sed 's/".*" //g' | awk '/'$2' / { print $2 }'`
  
  [ "x${remote_ver}" = "x" ] && return 1;
  
  if [ "x$3" != "x" ]; then
    if [[ "${remote_ver}" < "$3" ]]; then
      return 2
    fi
  fi
  
  echo "installing $2 ${remote_ver} ..."
set -x
  java ${PROXY_PARAM} -jar ${LAUNCHER} \
    -application org.eclipse.update.core.standaloneUpdate \
    -command install \
    -from $1 \
    -featureId $2 \
    -version ${remote_ver}
set +x
  return 0;
}

#RSE SDK
RSEREL="R-"
RSEVER="3.2"
RSEDATE="-201006071030"
RSENAME=RSE-SDK-${RSEVER}.zip
if [ ! -f eclipse/plugins/org.eclipse.rse.sdk_3.2.0.v201003151933.jar ]; then
  echo "Getting RSE SDK..."
  wget "http://download.eclipse.org/dsdp/tm/downloads/drops/${RSEREL}${RSEVER}${RSEDATE}/${RSENAME}"
  unzip -o ${RSENAME}
  rm ${RSENAME}
fi

# CDT Runtime
CDTREL=7.0.0
CDTFEAT=7.0.0
CDTVER=201006141710
CDTNAME=cdt-master-${CDTREL}-I${CDTVER}.zip
CDTLOC=builds/${CDTREL}/I.I${CDTVER}/${CDTNAME}
if [ ! -f eclipse/plugins/org.eclipse.cdt_${CDTFEAT}.${CDTVER}.jar ]; then
  echo "Install CDT..."
  UPDATE_SITE="http://download.eclipse.org/tools/cdt/releases/helios"
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.platform ${CDTFEAT} || exit $?
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt ${CDTFEAT} || exit $?
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.sdk ${CDTFEAT} || exit $?
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.launch.remote || exit $?
fi

#AUTOTOOL
ATVER=2.0.0
if [ ! -f eclipse/plugins/org.eclipse.linux.cdt.autotools_v${ATVER}.*.jar ]; then
  echo "Install AutoTool..."
  UPDATE_SITE="http://download.eclipse.org/technology/linuxtools/update"
  update_feature_remote ${UPDATE_SITE} org.eclipse.linuxtools.cdt.autotools ${ATVER} || exit $?
fi

echo "Your build environment is now created."
echo ""
echo "Run ECLIPSE_HOME=${cur_dir}/eclipse `dirname $0`/build.sh <branch name> <release name> to build"
echo ""

exit 0
