#!/bin/bash
# Installation script for BUG R1.4
# Adapted from instructions available at http://wiki.openembedded.net/index.php/Getting_started
# This script can be executable as shell script after substituting {| |} variables with real values.
# 4/3/2009 Ken Gilmer

# These are the variables that are queried in the UI.  The following lines are parsed by the install wizard.
# {|D|Install Directory|R|~/oe||}
# {|T|Init Script|R|reinstate-build-env||}

# System Check
which git
which svn
which python

# Directory Setup
[ -d ${Install Directory} ] || mkdir -p ${Install Directory}  
cd ${Install Directory}

# Installing from Bug Labs SVN repository
svn export -r HEAD svn://svn.buglabs.net/bug/tags/releases/R1.4/com.buglabs.build.oe
mv com.buglabs.build.oe/* .
rm -Rf com.buglabs.build.oe

