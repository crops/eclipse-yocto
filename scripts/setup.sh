#setup eclipse building environment for Indigo.
#comment out the following 2 lines if you don't want to use the http proxy for update site
#PROXY=proxy.jf.intel.com
#PORT=911

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

# prepare the base Eclipse installation in folder "eclipse"
ep_rel="R-"
ep_ver=3.7
ep_date="-201106131736"
P2_disabled=false
P2_no_dropins=false
if [ ! -f eclipse/plugins/org.eclipse.swt_3.7.0.v3735b.jar ]; then
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
  echo "installing $2 ${remote_ver} ..."
  
  if [ "x${PROXY}" != "x" ]; then
     PROXY_PARAM="-Dhttp.proxySet=true -Dhttp.proxyHost=${PROXY} -Dhttp.proxyPort=${PORT}"
  fi
  
  local remote_ver=`java ${PROXY_PARAM} \
    -jar ${LAUNCHER} \
    -application org.eclipse.equinox.p2.director \
    -destination ${curdir}/eclipse \
    -profile SDKProfile \
    -repository $1 \
    -list \
    | awk 'BEGIN { FS="=" } /'$2'/ { print $2 }'`
  
  [ "x${remote_ver}" = "x" ] && err_exit 1 "unknown remote version"
  
  if [ "x$3" != "x" ]; then
    if [[ "${remote_ver}" < "$3" ]]; then
      err_exit 1 "unsatified remote version ${remote_ver}, required $3"
    fi
  fi
  
  java ${PROXY_PARAM} -jar ${LAUNCHER} \
    -application org.eclipse.equinox.p2.director \
    -destination ${curdir}/eclipse \
    -profile SDKProfile \
    -repository $1 \
    -installIU $2 || err_exit $? "installing $2 failed"
}

#RSE SDK
RSEREL="R-"
RSEVER="3.3"
RSEDATE="-201106080935"
RSENAME=RSE-SDK-${RSEVER}.zip
if [ ! -f eclipse/plugins/org.eclipse.rse.sdk_3.3.0.*.jar ]; then
  echo "Getting RSE SDK..."
  wget "http://download.eclipse.org/dsdp/tm/downloads/drops/${RSEREL}${RSEVER}${RSEDATE}/${RSENAME}"
  unzip -o ${RSENAME} || err_exit $? "extracting RSE SDK failed"
  rm ${RSENAME}
fi

# CDT Runtime
CDTREL="8.0.0"
CDTFEAT="8.0.0"
CDTVER="201106081058"
CDTNAME=cdt-master-${CDTREL}-I${CDTVER}.zip
CDTLOC=builds/${CDTREL}/I.I${CDTVER}/${CDTNAME}
if [ ! -f eclipse/plugins/org.eclipse.cdt_${CDTFEAT}.${CDTVER}.jar ]; then
  echo "Install CDT..."
  UPDATE_SITE="http://download.eclipse.org/releases/indigo"
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.platform.feature.group ${CDTFEAT}
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.feature.group ${CDTFEAT}
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.sdk.feature.group ${CDTFEAT}
  update_feature_remote ${UPDATE_SITE} org.eclipse.cdt.launch.remote.feature.group 
fi

#TMF
TMFREL="0.4.0"
TMFDATE="201111050234"
if [ ! -f eclipse/plugins/org.eclipse.linuxtools.tmf.core_${TMFREL}.${TMFDATE}.jar ]; then
  echo "Install TMF..."
  UPDATE_SITE="http://download.eclipse.org/technology/linuxtools/update"
  update_feature_remote ${UPDATE_SITE} org.eclipse.linuxtools.tmf.feature.group ${TMFREL}
fi

#AUTOTOOL
ATVER="3.0.0"
if [ ! -f eclipse/plugins/org.eclipse.linux.cdt.autotools_v${ATVER}.*.jar ]; then
  echo "Install AutoTool..."
  UPDATE_SITE="http://download.eclipse.org/technology/linuxtools/update"
  update_feature_remote ${UPDATE_SITE} org.eclipse.linuxtools.cdt.autotools.feature.group ${ATVER} 
fi

echo "Your build environment is now created."
echo ""
echo "Run ECLIPSE_HOME=${curdir}/eclipse `dirname $0`/build.sh <branch name> <release name> to build"
echo ""

exit 0
