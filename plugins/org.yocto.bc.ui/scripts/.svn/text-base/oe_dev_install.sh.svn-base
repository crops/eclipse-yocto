#!/bin/bash
# Installation script for OpenEmbedded dev branch.
# Adapted from instructions available at http://wiki.openembedded.net/index.php/Getting_started
# This script should be executable after substituting ${} variables with real values.
# 4/3/2009 Ken Gilmer

# These are the variables that are queried in the UI.  The following lines are parsed by the install wizard.
# {|D|Install Directory|R|~/oe||}
# {|T|Repository URL|R|git://git.openembedded.net/openembedded||}
# {|T|Init Script|R|init.sh||}
# {|T|Distribution|R|angstrom-2008.1||}
# {|T|Machine|R|om-gta01||}
# {|T|Package Cache Directory|R|${HOME}/sources||}

# System Check
which git
which svn
which python

# Directory Setup
[ -d ${Install Directory} ] || mkdir -p ${Install Directory} 
cd ${Install Directory}
mkdir -p build/conf

# Bitbake Setup
svn export -r HEAD http://svn.berlios.de/svnroot/repos/bitbake/branches/bitbake-1.8 bitbake

# OpenEmbedded Setup
git clone ${Repository URL}
echo "BBFILES = \"${Install Directory}/openembedded/recipes/*/*.bb\"" > build/conf/local.conf
echo "DISTRO = \"${Distribution}\"" >> build/conf/local.conf
echo "MACHINE = \"${Machine}\"" >> build/conf/local.conf
echo "DL_DIR = \"${Package Cache Directory}\"" >> build/conf/local.conf

# Environment Setup Script
echo "export BBPATH=${Install Directory}/build:${Install Directory}/openembedded" > ${Init Script}
echo "export PATH=${Install Directory}/bitbake/bin:$PATH" >> ${Init Script}
chmod u+x ${Init Script}
