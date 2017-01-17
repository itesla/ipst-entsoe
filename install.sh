#!/bin/bash

# Copyright (c) 2016, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

sourceDir=$(dirname $(readlink -f $0))


## install default settings
###############################################################################
ipst_prefix=$HOME/itesla
ipst_package_version=` mvn -f "$sourceDir/pom.xml" org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version | grep -v "Download" | grep -v "\["`
ipst_package_name=ipst-$ipst_package_version
ipst_package_type=zip

# Targets
ipst_clean=false
ipst_compile=false
ipst_docs=false
ipst_package=false
ipst_install=false


## read settings from configuration file
###############################################################################
settings="$sourceDir/install.cfg"
if [ -f "${settings}" ]; then
     source "${settings}"
fi


## Usage/Help
###############################################################################
cmd=$0
usage() {
    echo "usage: $cmd [options] [target...]"
    echo ""
    echo "Available targets:"
    echo "  clean                    Clean iPST modules"
    echo "  compile                  Compile iPST modules"
    echo "  package                  Compile iPST modules and create a distributable package"
    echo "  install                  Compile iPST modules and install it (default target)"
    echo "  help                     Display this help"
    echo "  docs                     Generate the documentation (Javadoc)"
    echo ""
    echo "iPST options:"
    echo "  --help                   Display this help"
    echo "  --prefix                 Set the installation directory (default is $HOME/itesla)"
    echo "  --package-type           Set the package format. The supported formats are zip, tar, tar.gz and tar.bz2 (default is zip)"
    echo ""
}


## Write Settings functions
###############################################################################
writeSetting() {
    if [[ $# -lt 2 || $# -gt 3 ]]; then
        echo "WARNING: writeSetting <setting> <value> [comment (true|false)]"
        exit 1
    fi

    SETTING=$1
    VALUE=$2
    if [[ $# -eq 3 ]]; then
        echo -ne "# "
    fi
    echo "${SETTING}=${VALUE}"

    return 0
}

writeComment() {
    echo "# $*"
    return 0
}

writeEmptyLine() {
    echo ""
    return 0
}

writeSettings() {
    writeComment " -- iPST global options --"
    writeSetting "ipst_prefix" ${ipst_prefix}
    writeSetting "ipst_package_type" ${ipst_package_type}

    writeEmptyLine

    writeComment " -- iPST C++ modules options --"
    writeSetting "eurostag_build" ${eurostag_build}
    writeSetting "eurostag_home" "${eurostag_home}"
    writeSetting "dymola_build" ${dymola_build}
    writeSetting "dymola_home" "${dymola_home}"
    writeSetting "matlab_build" ${matlab_build}
    writeSetting "matlab_home" "${matlab_home}"

    writeEmptyLine

    writeComment " -- iPST thirdparty libraries --"
    writeSetting "thirdparty_build" ${thirdparty_build}
    writeSetting "thirdparty_prefix" ${thirdparty_prefix}
    writeSetting "thirdparty_download" ${thirdparty_download}
    writeSetting "thirdparty_packs" ${thirdparty_packs}

    return 0
}


## Build Java Modules
###############################################################################
ipst_java()
{
    if [[ $ipst_clean = true || $ipst_compile = true || $ipst_docs = true ]]; then
        echo "** Building iPST Java modules"

        mvn_options=""
        [ $ipst_clean = true ] && mvn_options="$mvn_options clean"
        [ $ipst_compile = true ] && mvn_options="$mvn_options install"
        if [ ! -z "$mvn_options" ]; then
            mvn -f "$sourceDir/pom.xml" $mvn_options || exit $?
        fi

        if [ $ipst_docs = true ]; then
            echo "**** Generating Javadoc documentation"
            mvn -f "$sourceDir/pom.xml" javadoc:javadoc || exit $?
            mvn -f "$sourceDir/distribution/pom.xml" install || exit $?
        fi
    fi
}

## Package iPST
###############################################################################
ipst_package()
{
    if [ $ipst_package = true ]; then
        echo "** Packaging iPST"

        case "$ipst_package_type" in
            zip)
                [ -f "${ipst_package_name}.zip" ] && rm -f "${ipst_package_name}.zip"
                $(cd "$sourceDir/distribution/target/distribution-${ipst_package_version}-full" && zip -rq "$sourceDir/${ipst_package_name}.zip" "itesla")
                zip -qT "${ipst_package_name}.zip" > /dev/null 2>&1 || exit $?
                ;;

            tar)
                [ -f "${ipst_package_name}.tar" ] && rm -f "${ipst_package_name}.tar"
                tar -cf "${ipst_package_name}.tar" -C "$sourceDir/distribution/target/distribution-${ipst_package_version}-full" . || exit $?
                ;;

            tar.gz | tgz)
                [ -f "${ipst_package_name}.tar.gz" ] && rm -f "${ipst_package_name}.tar.gz"
                [ -f "${ipst_package_name}.tgz" ] && rm -f "${ipst_package_name}.tgz"
                tar -czf "${ipst_package_name}.tar.gz" -C "$sourceDir/distribution/target/distribution-${ipst_package_version}-full" . || exit $?
                ;;

            tar.bz2 | tbz)
                [ -f "${ipst_package_name}.tar.bz2" ] && rm -f "${ipst_package_name}.tar.bz2"
                [ -f "${ipst_package_name}.tbz" ] && rm -f "${ipst_package_name}.tbz"
                tar -cjf "${ipst_package_name}.tar.bz2" -C "$sourceDir/distribution/target/distribution-${ipst_package_version}-full" . || exit $?
                ;;

            *)
                echo "Invalid package format: zip, tar, tar.gz, tar.bz2 are supported."
                exit 1;
                ;;
        esac
    fi
}

## Install iPST
###############################################################################
ipst_install()
{
    if [ $ipst_install = true ]; then
        echo "** Installing iPST"

        echo "**** Copying files"
        mkdir -p "$ipst_prefix" || exit $?
        cp -Rp "$sourceDir/distribution/target/distribution-${ipst_package_version}-full/itesla"/* "$ipst_prefix" || exit $?
    fi
}

## Parse command line
###############################################################################
ipst_options="prefix:,package-type:"

opts=`getopt -o '' --long "help,$ipst_options" -n 'install.sh' -- "$@"`
eval set -- "$opts"
while true; do
    case "$1" in
        # iPST options
        --prefix) ipst_prefix=$2 ; shift 2 ;;
        --package-type) ipst_package_type=$2 ; shift 2 ;;

        # Help
        --help) usage ; exit 0 ;;

        --) shift ; break ;;
        *) usage ; exit 1 ;;
    esac
done

if [ $# -ne 0 ]; then
    for command in $*; do
        case "$command" in
            clean) ipst_clean=true ;;
            compile) ipst_compile=true ;;
            docs) ipst_docs=true ;;
            package) ipst_package=true ; ipst_compile=true ;;
            install) ipst_install=true ; ipst_compile=true ;;
            help) usage; exit 0 ;;
            *) usage ; exit 1 ;;
        esac
    done
else
    ipst_compile=true
    ipst_install=true
fi

## Build iPST platform
###############################################################################

# Build Java modules
ipst_java

# Package iPST
ipst_package

# Install iPST
ipst_install

# Save settings
writeSettings > "${settings}"

