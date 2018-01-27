Note: PART I and PART II can be skipped if you have already done that.

Part I: Base environment setup

I-1. Install Build Dependencies

I-1.1 Fedora

  sudo dnf install java-1.8.0-openjdk xsltproc wget tar make git

I-1.2 Ubuntu

  sudo apt-get install openjdk-8-jdk xsltproc wget tar make git

I-1.3 OpenSUSE

  sudo zypper install java-1_8_0-openjdk xsltproc wget tar make git


I-3. Proxy Settings (If you're behind some firewall)

  The currently recommended solution is to use Chameleonsocks:

    https://github.com/crops/chameleonsocks

  The legacy method of handling proxy settings is included in Appendix A.


I-4. Get the Source Code for the Yocto Project Eclipse Plugin

  git clone git://git.yoctoproject.org/eclipse-poky.git

  The scripts used for building, including on the auto builder, are under
  the "scripts" directory.

  The repo has several branches, for different releases of Eclipse (e.g.
  "neon-master") and also for difference releases of the Yocto Project
  (e.g. "neon/morty").


Part II: Setup the Build Environment

II-1. Modify the scripts/setup.sh to set appropriate proxy settings.

  If you are not using Chameleonsocks, set PROXY to a http proxy URL
  and PORT to the port number. Comment out these 2 variables if proxy
  is not required.


II-2. Run setup.sh Script

  Run <path to>/scripts/setup.sh to set up the build environment.
  This will install the Eclipse SDK and relevant plugins required to build
  the Yocto Project Eclipse plug-ins. It will also create a convenience
  symbolic link to the Eclipse executable.

  If you are working on a newer Eclipse release, where the upstream has not
  yet been mirrored to http://downloads.yoctoproject.org/eclipse/, you
  should instead run:

  <path to>/scripts/setup.sh --upstream


Part III: Build & Install Yocto Eclipse plug-in

III.1 Build the Yocto Eclipse Plug-in

  To build the Yocto Eclipse plug-in, simply run

"ECLIPSE_HOME=<eclipse path> <path to>/scripts/build.sh <plugin branch or tag name> <documentation branch or tag name> <release name> 2>&1 | tee -a build.log"

  Note that this will clone the indicated branch of yocto-docs from
  git.yoctoproject.org. If you pass in an invalid branch/tag, the build
  will fail.

  The <eclipse path> is the absolute path where you installed Eclipse
  in step II-2.

  If successful, three files will be generated in the directory where
  you invoked the "build.sh" script:
    org.yocto.doc-<release name>-<date>.zip,
    org.yocto.sdk-<release name>-<date>.zip and
    org.yocto.sdk-<release name>-<date>-archive.zip

  If you are planning to build multiple versions of the plugins (for
  different Eclipse releases for instance), you can run the setup and build
  scripts from any directory, as long as you use the proper paths.

  When switching between branches, you may need to run the setup.sh script
  again to properly setup a different Eclipse environment. You may want to
  change the "eclipse" symbolic link to point to the desired Eclipse
  installation so that the already downloaded and installed files will be
  recognized.


III.2 Building Local Changes to Yocto Eclipse Plug-in

  If you are building from a local branch, you should instead run:

"ECLIPSE_HOME=<eclipse path> <path to>/scripts/build.sh -l <plugin branch or tag name> <documentation branch or tag name> <release name> 2>&1 | tee -a build.log"


III.3 Install the Yocto Eclipse Plug-in

  The file with the "-archive" in its name is the archive zip used for Eclipse
  update manager (the "p2 repository"). The user can use the Eclipse update
  manager to install it:
    Help -> Install New Software... -> Add... -> Archive...
  This is the recommended way to install the plugin during development.

  The files without the "-archive" in their names are the zip containing
  only the plugins/features. The user can unzip them into their target
  eclipse "plugins" folder to install it.


Part IV. Updating and Keeping Up-to-Date with Upstream Eclipse

IV.1 Updating setup.sh script

  When you would like to update the plugin to build for a new Eclipse release,
  you need to make a few changes in the setup.sh script.

  1. If you are building for a non "Release" version of Eclipse, set the
     appropriate value for "ep_rel", for instance "S-" for stable.

  2. Set "ep_ver" to the numeric version of the release, for instance "4.6"
     for the original Neon release or "4.5.2" for the Mars.2 release or
     "4.7M2" for the second Oxygen milestone.

  3. Set the "ep_date" to the date stamp on the tarball. The easiest way to
     determine this is to go the main Eclipse website, click on the Downloads
     button, click on the Download Packages link, click on the "Other builds"
     link in the "More Downloads" sidebar and then hover over the version you
     would like to download to see its fully qualified name. The "Other builds"
     link currently resolves to http://download.eclipse.org/eclipse/downloads/
     but this may change in the future.

  4. Set the MAIN_SITE to point to the desired release URL.

  5. Set the UPDATE_SITE to point to the correct update site for the release
     in question. Note that for milestone releases (pre-release), the URL is
     different since the numeric version has "milestones" appended to it (e.g.
     "4.7milestones" for Oxygen pre-release).

  6. Set the versions of the various components to the desired version,
     for instance CDTFEAT="9.0.0" for the original Neon release, but
     became "9.1.0" for the Neon.1 release and will be "9.2.0" for the
     upcoming Oxygen release.

  7. If any new components need to be added (new build dependencies), the
     name of the feature must match exactly in order for the convenience
     "update_feature_remote" function to work properly. A recommended way
     to explore P2 repositories is included in the Oomph Setup SDK.
     It can be found under "Window -> Show View -> Other... -> Oomph ->
     Repository Explorer".


IV.2 Staying Informed

  Eclipse has either a major release or a point release approximately every
  three months. At times, plugins and features may be moved to a different
  update site. As an example, between the Mars release and the Mars.1 release,
  the RSE plugin was officially deprecated and moved to a separate update site.

  In order to stay on top of these changes, it is highly recommended that you
  subscribe to at least the following mailing lists:

    1. C/C++ Development Tooling:
        https://dev.eclipse.org/mailman/listinfo/cdt-dev

    2. Eclipse Packaging Project:
        https://dev.eclipse.org/mailman/listinfo/epp-dev

    3. Cross Project Issues:
        https://dev.eclipse.org/mailman/listinfo/cross-project-issues-dev

  It is on these mailing lists that major API and ABI breaking changes will be
  announced. As an example, the minimum required JDK version for Neon is 1.8.0,
  which was announced on both the "EPP" and the "Cross Project" lists. These
  mailing lists are also a place to discuss planned new features and commun-
  icate with other Eclipse developers. It is not particularly time-consuming
  to stay up to date, but if you wait for a year or two you might find that
  upstream Eclipse has changed dramatically and it can be difficult to sync.


Part V. Sharing with the Community

V.1 Sending Patches to the Mailing List

  Any changes to the eclipse-poky plugin should be sent as patches to
  the Eclipse Poky  mailing list (eclipse-poky@yoctoproject.org), with

    --subject-prefix="<eclipse-revision>][PATCH"

  where <eclipse-revision> might be "neon" or "oxygen". Also if, the bug
  affects a particular Yocto Project release, include the release name
  in the subject prefix:

    --subject-prefix="<eclipse-revision>][<yocto-project-release-name>][PATCH"

  where <yocto-project-release-name> might be "morty", "pyro" or "rocko" for
  instance.

  This is a subscriber only list, so you will need to sign up for access at:

    https://lists.yoctoproject.org/listinfo/eclipse-poky

  Patches should follow the same guidelines that are used for other parts of
  the Yocto Project and Open Embedded code-base:

    http://www.openembedded.org/wiki/How_to_submit_a_patch_to_OpenEmbedded

  Note that code included in the eclipse-poky plugin should be under the EPL
  (Eclipse Public License) or a compatible license.

V.2 Sharing Code in the eclipse-poky-contrib Repository

  Similar to other -contrib repositories in the Yocto Project, there is
  an eclipse-poky-contrib repository created for community members to be
  able to share work-in-progress code or code for pull-requests.

    http://git.yoctoproject.org/cgit/cgit.cgi/eclipse-poky-contrib/

  To request access, follow the same instructions as for poky-contrib:

    https://wiki.yoctoproject.org/wiki/Poky_Contributions#Request_Access


Appendix A. Proxy Settings without Chameleonsocks

A-1. using git through a SOCKS proxy(If you're behind some firewall)

A-1.1 Create a wrapper script for netcat
  cat > ~/bin/proxy-wrapper

  #!/bin/sh
  PROXY=proxy.example.com
  PORT=8080
  METHOD="-X 5 -x ${PROXY}:${PORT}"

  nc $METHOD $*

  Then Ctlr+D to save the file and "chmod +x ~/bin/proxy-wrapper"

  Note: if netcat is not installed, please "sudo zypper install netcat-openbsd".

A-1.2 set git proxy environment
  add the following line to your ~/.bashrc and "source ~/.bashrc"

  export GIT_PROXY_COMMAND="/somepath/bin/proxy-wrapper"

  Please be noted that it should be absolute path, since "~" is not supported.

A-2. using svn through a http_proxy(If you're behind some firewall)
  Modify the ~/.subversion/servers

  http-proxy-host = proxy.example.com
  http-proxy-port = 8888
