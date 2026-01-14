#!/bin/bash -

# run as user

###############################################################################
# actTexlive.sh — activate a TeX Live release already installation
#
# SYNOPSIS
#   source actTexlive.sh <YEAR>
#
# DESCRIPTION
#   This script switches to given texlive release 
#   provided it is installed for the current platform 
#   by adapting the user PATH. 
#   Thus it must be invoked by user (not by root) via `source`. 
#
#   TeX Live is released once a year in may 
#   and each release is identified by its year of publication <YEAR> represented by 4 digits. 
#   It is recommended to install with `instTexlive,sh`. 
#   This script expects the TeX Live installation of release <YEAR> in 
#
#       /usr/local/texlive/<YEAR>
#
#   The specified release must be installed for the current platform, else an error is risen. 
#   The executables are collected in 
#
#       /usr/local/texlive/<YEAR>/bin
#
#   Partially, the executables depend on the TeX Live platform 
#   which is typically a combination of architecture and operating system 
#   like `x86_64-linux`. 
#   Thus they are collected in a subfolder named after the platform `<platform>` like 
#
#       /usr/local/texlive/<YEAR>/bin/<platform>
#
#   If the preconditions are satisfied, all elements of the form 
#
#       /usr/local/texlive/
#
#   are removed from the PATH and then 
#       /usr/local/texlive/<YEAR>/bin/<platform>
#
#   is added as first element. 
#   Thus PATH contains a unique pointer to a TeX Live installation fitting the curent version and platform. 
#
#   Please use `instTexlive.sh` to install a missing release or plattform and to add further packages. 
#
#   Note that the package manager `tlmgr` is among the executables and so the following is available to the user: 
#   - `tlmgr --list` lists the packages and marks the installed ones by `i`. 
#   - `tlmgr update --list` lists the packages for which an update is available. 
#   - `tlmgr info <package>` lists various pieces of information for <package>; see `tlmgr bug <package>`
#   - `tlmgr bug <package>` lists bug contact for <package>
#
# REQUIREMENTS
#   - linux, unix, WSL, no mac.
#   - Must not be executed as root.
#
# ACTIONS PERFORMED
#   PATH cleanup and activation of the selected version fitting the current platform.
#
# EXIT CODES
#   0  success
#   1  problem at start 
#      - invoker may be all but root, 
#      - invalid arguments (exactly one argument: release year shall be given by 4 digits)
#   2  release <YEAR> is not installed  (invoke `instTexlive/sh` before)
#   3  current platform (given by architecture and os) not installed (maybe not supported) 
#      although the given release is installed as such. 
#
# EXAMPLES
#   switch to TeX Live 2025:
#       sudo ./actTexlive.sh 2025
#
#   Switch to an existing TeX Live 2023:
#       sudo ./actTexlive.sh 2023
#
# AUTHOR
#   Your Name <ernst.reissner@simuline.eu>
#
###############################################################################


# Farbfunktionen
error() { echo -e "\e[91m✖ ERROR: $1\e[0m"; }
warn()  { echo -e "\e[93m⚠ WARN: $1\e[0m"; }
info()  { echo -e "\e[94mℹ INFO: $1\e[0m"; }
succ()  { echo -e "\e[92m✔ DONE: $1\e[0m"; }

[ "$EUID" -ne 0 ] || { error "Please run as user, not as root."; exit 1; }
[ "$#" -eq 1 ] || { error "Usage: $0 <year in 4 digits>."; exit 1; }
YEAR="$1"
[[ "$YEAR" =~ ^[0-9]{4}$ ]] || { error "Invalid argument: year must be exactly 4 digits."; exit 1; }

# folder where all texlive releases are to be installed ... 
ROOT="/usr/local/texlive"
# each release in the folder of its year 
INSTALL_DIR="$ROOT/$YEAR"
# The architecture for which to install (the current)
TL_PLATFORM=$(uname -m)-$(uname | tr '[:upper:]' '[:lower:]')
# The subfolder of the installation with the binaries for the given architecture 
BIN_DIR="$INSTALL_DIR/bin/$TL_PLATFORM"

[ -d "$INSTALL_DIR" ] || { error "Texlive $YEAR not (properly) installed."; exit 2; }
[ -d "$BIN_DIR" ] || { error "Seemingly texlive $YEAR is not installed for your architecture $TL_PLATFORM."; exit 3; }

info "switching to path including $BIN_DIR..."
# switch the path to $YEAR
OLD_IFS="$IFS"
IFS=":"
read -ra ADDR <<< "$PATH"
NEW_PATH="$BIN_DIR"
for p in "${ADDR[@]}"; do
  # match else append 
  [[ "$p" =~ ^$ROOT ]] || NEW_PATH="$NEW_PATH:$p"
done
IFS="$OLD_IFS"

export PATH="$NEW_PATH"
info "switching done"

