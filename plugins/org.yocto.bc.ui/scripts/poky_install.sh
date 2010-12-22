#!/bin/bash
# Installation script for Poky Pinky development branch
# Adapted from instructions available at http://wiki.openembedded.net/index.php/Getting_started
# This script can be executable as shell script after substituting {| |} variables with real values.
# 4/3/2009 Ken Gilmer

# These are the variables that are queried in the UI.  The following lines are parsed by the install wizard.
# {|D|Install Directory|R|~/yocto||}
# {|T|Init Script|R|poky-init-build-env||}

# System Check
echo "which git"
which git
echo "which svn"
which svn
echo "which python"
which python

echo "Install Directory: "$1
# Directory Setup
#[ -d ${Install Directory} ] || mkdir -p ${Install Directory}  
echo "[ -d $1 ] || mkdir -p $1"  
[ -d $1 ] || mkdir -p $1  
#if [ -d "${Install Directory}/poky" ] 
#if [ -d "${Install Directory}/poky" ]; then 
#echo "directory already exist"
#exit 1
#else
#mkdir -p ${Install Directory}  
#cd ${Install Directory}
echo "cd $1"
cd $1
#fi

# Installing from Poky pinky branch
#[ -d poky ] && echo "poky directory already exist! Install aborted!" 1>&2; exit 
echo "git clone git://git.pokylinux.org/poky.git"
git clone git://git.pokylinux.org/poky.git
#svn export -r HEAD http://svn.o-hand.com/repos/poky/branches/pinky/
#mv pinky/* .
#rm -Rf pinky
