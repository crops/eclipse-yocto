Note: PART I and PART II can be skipped if you have already done that.

Part I: Base environment setup
I-1. install JDK
  sudo zypper install java-1_6_0-openjdk

I-2. install X11 related packages for eclipse running
  sudo zypper install xorg-x11-xauth

I-3. using git through a SOCKS proxy(If you're behind some firewall)
I-3.1 Create a wrapper script for netcat
  cat > ~/bin/proxy-wrapper

  #!/bin/sh
  PROXY=proxy-jf.intel.com
  PORT=1080
  METHOD="-X 5 -x ${PROXY}:${PORT}"

  nc $METHOD $*

  Then Ctlr+D to save the file and "chmod +x ~/bin/proxy-wrapper"

  Note: if netcat is not installed, please "sudo zypper install netcat-openbsd".

I-3.2 set git proxy environment
  add the following line to your ~/.bashrc and "source ~/.bashrc"

  export GIT_PROXY_COMMAND="/somepath/bin/proxy-wrapper"

  Please be noted that it should be absolute path, since "~" is not supported.

I-4. using svn through a http_proxy(If you're behind some firewall)
  Modify the ~/.subversion/servers

  http-proxy-host = proxy.jf.intel.com
  http-proxy-port = 911

I-5. Get the scripts from eclipse-poky git
  git clone git://git.pokylinux.org/eclipse-poky.git

  The scripts used for auto builder is under the directory of "scripts".


Part II: Setup the build environment
II-1. Modify the scripts/setup.sh to set appropriate proxy settings.
  Set PROXY to a http proxy URL and PORT to the port number. Comment out 
  these 2 variables if proxy is not required.

II-2. Run scripts/setup.sh to set up the build environment. 
  This will install the eclipse and relevant plugins required to build 
  Yocto eclipse plug-in.

Part III: Build & Install Yocto Eclipse plug-in

To build the Yocto Eclipse plug-in, simply run 
"ECLIPSE_HOME=<eclipse path> scripts/build.sh <branch name> <release name>".

The <eclipse install path> is the absolute path where you installed the 
eclipse in step II-2.

The <branch name> is the git branch name you build based on.

If successful, 2 files org.yocto.sdk-<release name>-<date>.zip and 
org.yocto.sdk.-<release name>-<date>-archive.zip will be genereated under the
directory where you invoked the "build.sh" script.

The file with the "-archive" in its name is the archive zip used for eclipse
update manager. User should use eclipse update manager to install it.

The file without the "-archive" in its name is the zip containing only the 
plugins/features. User should unzip it to the their target eclipse to install it.
