#!/bin/bash -

# run as root 

###############################################################################
# instTexlive.sh — Install, update or activate a TeX Live yearly installation
#
# SYNOPSIS
#   instTexlive.sh <YEAR> <MODE>
#
# DESCRIPTION
#   TeX Live is released once per year. Each yearly release remains active from
#   may of that year until april of the next year, after which it is frozen
#   and archived. Each release is identified by the <YEAR> of publishing given with 4 digits. 
#   Various releases can coexist, each in a folder given by 
#
#       /usr/local/texlive/<YEAR>
#
#   Executables of texlive are collected in 
#
#       /usr/local/texlive/<YEAR>/bin
#
#   Unlike LaTeX classes and styles executables may depend on the so called texlive platform. 
#   Thus for each platform supported 
#   This script performs the installation of binaries for the platform 
#This script must be invoked as root. 
#   It installs, updates and patches a TeX Live installation for a given <YEAR>. 
#   The script supports two modes:
#
#     MODE = nup
#         "new or update":
#         - Installs TeX Live <YEAR> if not present (scheme=small, paper=a4).
#         - Checks whether the architecture is supported; else exits. 
#         - Updates the installation if it already exists.
#         - Installs packages listed in file `packagesTexlive<YEAR>.txt` expected in current folder.
#         - Applies custom patches for PythonTeX, tex4ht, xindy, etc.. 
#         - Activates release <YEAR> by adjusting PATH.
#
#     MODE = org
#         "original / use existing":
#         - No installation, update or patching is performed.
#         - Checks whether the release <YEAR> is installed and the architecture is supported; else exits. 
#         - IActivates <YEAR> by adjusting PATH.
#   Note that setting the path to the release <YEAR> 
#   includes eliminating other releases from the path by need. 
#
# PACKAGE LIST
#   Optional file: packagesTexlive<YEAR>.txt
#   Format:
#       - One or more package names per line.
#       - Inline comments allowed:  <pkg> <pkg>  # comment
#       - Full-line comments allowed:  # ...
#       - Blank lines allowed.
#       - No line continuation (“\”) supported.
#
#       The file is filtered using:
#           sed 's/#.*//' | sed '/^\s*$/d'
#
# REQUIREMENTS
#   - linux, unix, WSL, no mac.
#   - Must be executed as root.
#   - Network access required for installation and updates.
#   - wget, tar, unzip must be available.
#
# ACTIONS PERFORMED
#   - Installation from CTAN mirrors (tlnet-final/<YEAR>).
#   - tlmgr configuration (repository auto-selection).
#   - tlmgr update --self and update --all.
#   - Installation of user-defined packages.
#   - Application of custom patches into texmf-dist.
#   - Regeneration of TeX Live filename database (texhash).
#   - PATH cleanup and activation of the selected version.
#
# EXIT CODES
#   0  success
#   1  problem at start 
#      - running not as root, 
#      - invalid arguments, 
#      - revision year not found 
#   2  failure downloading the installer 
#   3  installation error 
#      - unexpected folder structure, 
#      - cannot enter installation folder, 
#      - installer itself failed
#   4  architecture/platform not supported 
#   5  missing package list
#   6  patching error
#   7  installation not present (org mode)
#
# EXAMPLES
#   Install or update TeX Live 2025:
#       sudo ./instTexlive.sh 2025 nup
#
#   Switch to an existing TeX Live 2023:
#       sudo ./instTexlive.sh 2023 org
#
# AUTHOR
#   Your Name <ernst.reissner@simuline.eu>
#
###############################################################################

# folder where all texlive releases are to be installed ... 
ROOT="/usr/local/texlive"
# list of servers with historic texlive builds: https://www.tug.org/historic/
SERVER_HIST="https://ftp.math.utah.edu/pub/tex/historic/systems/texlive"
# SERVER_HIST="https://mirror.nju.edu.cn/tex-historic/systems/texlive$"
#SERVER_HIST="https://ftp.tu-chemnitz.de/pub/tug/historic/systems/texlive"

# server for current texlive not available on historic servers 
SERVER_CURR="https://mirror.ctan.org/systems/texlive/tlnet"
# my personal server providing patches 
SERVER_MYPATCH="https://www.simuline.eu/TexLive"

# color functions 
error() { echo -e "\e[91m✖ ERROR: $1\e[0m"; }
warn()  { echo -e "\e[93m⚠ WARN: $1\e[0m"; }
info()  { echo -e "\e[94mℹ INFO: $1\e[0m"; }
succ()  { echo -e "\e[92m✔ DONE: $1\e[0m"; }

# ensuring correct usage 
[ "$EUID" -eq 0 ] || { error "Please run as root or via sudo."; exit 1; }
[ "$#" -eq 1 ] || { error "Usage: $0 <year in 4 digits>"; exit 1; }
YEAR="$1"
[[ "$YEAR" =~ ^[0-9]{4}$ ]] || { error "Year must be exactly 4 digits"; exit 1; }

# folder for this script and for packagesTexlive<year>.txt
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# folder to install texlive of the given year 
INSTALL_DIR="$ROOT/$YEAR"
# the architecture for which to install (the current one)
ARCH=$(uname -m)-$(uname | tr '[:upper:]' '[:lower:]')
# the subfolder of the installation with the binaries for the given architecture 
BIN_DIR="$INSTALL_DIR/bin/$ARCH"
# truely the minimal scheme which makes sense 
SCHEME="small"


# find out whether the revision $YEAR exists, and whether it is frozen already 
wget --spider --quiet "$SERVER_HIST/$YEAR" || { error "Revision $YEAR not found."; exit 1; }
info "Found revision $YEAR on server."
wget --spider --quiet "$SERVER_HIST/$YEAR/tlnet-final"
IS_FROZEN=$?
if [ "$IS_FROZEN" -eq 0 ]; then
  info "Revision $YEAR on server is frozen."
  repo="$SERVER_HIST/$YEAR/tlnet-final"
else
  info "Revision $YEAR on server is live."
  repo="$SERVER_CURR"
fi

# new or update 
if [ ! -d "$BIN_DIR" ]; then
  # Here, the revision $YEAR is not yet installed, 
  # at least not for the current architecture $ARCH 
  if [ -d "$INSTALL_DIR" ]; then
    # Here, the revision $YEAR is principially installed, 
    # it is only the architecture which is missing. 
    info "Reinstalling existing release $YEAR for architecture $ARCH..."
  else
    # Here, the revision $YEAR is not yet installed at all 
    info "Installing texlive $YEAR for architecture $ARCH..."
  fi



  FILE="install-tl-unx.tar.gz"
  rm -f "$FILE"
  # try URL for life release 
  info "Try downloading final release..."
  if [ "$IS_FROZEN" -eq 0 ]; then
    # is frozen 
    URL="$SERVER_HIST/$YEAR/tlnet-final/$FILE"
  else
    # TBD: clarify whether "$SERVER_CURR"/$FILE works also, then $repo/$FILE is the better choice 
    URL="$SERVER_HIST/$YEAR/$FILE"
  fi

  # URL="https://mirror.ctan.org/systems/texlive/$YEAR/tlnet-final/$FILE"
  # URL="https://ftp.tu-chemnitz.de/pub/tug/historic/systems/texlive/$YEAR/$FILE"
  wget "$URL" || { error "Downloading installer $FILE failed."; exit 2; }
  # if [ $? -ne 0 ]; then 
  #   info "Try downloading archived release..."
  #   # try URL for archived release 

  #   # list of historic servers: https://www.tug.org/historic/
  #   URL="https://ftp.math.utah.edu/pub/tex/historic/systems/texlive/$YEAR/$FILE"
  #   # worked for 2024, 2023
  #   URL="https://ftp.tu-chemnitz.de/pub/tug/historic/systems/texlive/$YEAR/tlnet-final/$FILE"
  #   #URL="https://mirror.nju.edu.cn/tex-historic/systems/texlive/$YEAR/$FILE"
  #   wget "$URL" || { error "Downloading $FILE failed, both as live release and as archived one."; exit 2; }
  # fi
  succ "Download installer succeeded."

  INSTALLER="install-tl"
  INSTALLER_DIR=$(tar -tf "$FILE" | grep -E "^[^/]+/$INSTALLER" | cut -d/ -f1 | head -1)
  [ -n "$INSTALLER_DIR" ] || { error "Unexpected folder structure of $FILE."; exit 3; }

  rm -rf "$INSTALLER_DIR"
  tar -xzf "$FILE"

  pushd "$INSTALLER_DIR" > /dev/null || { error "Could not enter installation folder $INSTALLER_DIR."; exit 3; }
    info "installing texlive $YEAR scheme $SCHEME into $INSTALL_DIR"
    chmod +x "$INSTALLER"
    # ./$INSTALLER --scheme "$SCHEME" --no-interaction -paper "a4" --texdir "$INSTALL_DIR" \
    #   || { error "Installer failed with exit code $?."; exit 3; }

    # if [ "$IS_FROZEN" -eq 0 ]; then
    #   repo="$SERVER_HIST/$YEAR/tlnet-final"
    #  else
    #   repo="$SERVER_CURR"
    # fi
    ./$INSTALLER -repository "$repo" \
      --scheme "$SCHEME" --no-interaction -paper "a4" --texdir "$INSTALL_DIR" \
      || { error "Installer failed with exit code $?."; exit 3; }
      
  popd > /dev/null
  [ -d "$BIN_DIR" ] || { error "Unexpected: Architecture $ARCH is not accessible."; exit 4; }
  
  # schema small is installed even with a reinstall with scheme minimal
  succ "Installed texlive $YEAR with scheme $SCHEME for architecture $ARCH."
else
  info "Found texlive $YEAR for architecture $ARCH installed locally."
fi

# Here, texlive $YEAR is installed with scheme small at least and with the current architecture $ARCH 

#pushd "$BIN_DIR" > /dev/null || { error "Unexpected: Architecture $ARCH is not accessible."; exit 4; }
PATH="$BIN_DIR:$PATH"

#tlmgr option repository https://ctan.uni-freiburg.de/systems/texlive/tlnet/
# ./tlmgr option repository https://mirror.ctan.org/systems/texlive/tlnet \
#   || { warn "Setting tlmgr mirror failed; skipping.";  }
# worked for 2024, 2023
# worked for 2025: drop altogether 
# ./tlmgr option repository https://ftp.tu-chemnitz.de/pub/tug/historic/systems/texlive/$YEAR \
#   || { warn "Setting tlmgr mirror failed; skipping.";  }
tlmgr option repository "$repo" \
  || { warn "Setting tlmgr mirror failed; skipping.";  }

# TBD: UPDATE only for live releases 
#   tlmgr: Remote database at https://ftp.mpi-inf.mpg.de/pub/tex/mirror/ftp.dante.de/pub/tex/systems/texlive/tlnet
# (revision 77141 of the texlive-scripts package)
# seems to be older than the local installation
# (revision 77146 of texlive-scripts);
# please use a different mirror and/or wait a day or two.


# CAUTION: updating tlmgr and packages also if frozen. 
# This is very much restricted on security and other major issues 
info "Updating tlmgr ..."
tlmgr update --self || { warn "Updating tlmgr failed; skipping."; }

#tlmgr --repository=/usr/local/texlive/2025/tlpkg list --only-installed > installed-packages.txt
# xargs tlmgr install < installed-packages.txt

info "Updating packages ..."
tlmgr update --all || { warn "Updating packages  failed; skipping."; }

#sudo tlmgr install latexmk rubber epstool latex2rtf


info "Adding packages to installation..."
# made directory of this scirpt $(dirname "${BASH_SOURCE[0]} an absolute path 
PKG_FILE="$SCRIPT_DIR/packagesTexlive$YEAR.txt"
[ -f "$PKG_FILE" ] || { error "Required package list $PKG_FILE not found. Aborting update."; exit 5; }
tlmgr install $(sed 's/#.*//' "$PKG_FILE" | sed '/^\s*$/d') \
  || { warn "Installing packages in $PKG_FILE failed; skipping."; }

info "Patching ..."



# TBD: download only if the folder exists. 
# TBD: what if the year is not patched? 
# This seems to happen also for 2023 although packagesTexlive2023.txt exists 


patchRec() {
  # Recursively copies folder structure and files from a patch server to a local folder.
  #
  # INPUT:
  #   $1  - SERVER_DIR : URL of the patch server folder (must provide an HTML index with <tr> table rows)
  #   $2  - LOCAL_DIR  : Local target folder where the files/folders will be copied
  #
  # BEHAVIOR:
  #   - Ensures LOCAL_DIR exists (mkdir -p)
  #   - Downloads the HTML index from SERVER_DIR
  #   - Iterates over all <tr> lines in the index to extract <a href="..."> entries
  #   - Ignores entries with href="/" (Parent Directory)
  #   - Recursively processes subdirectories (href ending with '/')
  #   - Downloads files (wget -N) to the corresponding LOCAL_DIR path
  #
  # ERROR HANDLING (Return Value as bitmask):
  #   - Bit 0 (value 1) : Index fetch failure (could not read SERVER_DIR)
  #   - Bit 1 (value 2) : Any file download failure
  #   - Bits are OR-combined recursively, so RET will reflect errors in subfolders too
  #
  # RETURN:
  #   - 0  : All files and folders copied successfully
  #   - 1  : Index fetch failed
  #   - 2  : One or more files could not be downloaded
  #   - 3  : Combination of index and file errors (bitmask)

  local SERVER_DIR="$1"
  local LOCAL_DIR="$2"
  #info "in patching1 $SERVER_DIR  $LOCAL_DIR"

  # ensure existence of $LOCAL_DIR (in top level normally superfluous but necessary in deeper nestings)
  mkdir -p "$LOCAL_DIR"

  # assume server provides the folder entries in a table, each entry corresponds with a row 
  local html
  html=$(wget -qO- "$SERVER_DIR/") || { warn "Could not fetch $SERVER_DIR"; return 1; }

  # The elements of the folder $SERVER_DIR are the lines in $html containing '<tr>', 
  # i.e. lines of a table 
  local lines
  lines=$(echo "$html" | grep '<tr>')

  # feed lines into the while loop 
  local RET=0
  while read -r line; do
    local href
    # href is all quoted in <a href="....">; all other lines skip 
    href=$(echo "$line" | grep -oP '(?<=<a href=")[^"]+') || continue

    # skip href="/..." and href="?..."
    [[ "$href" == /* ]] && continue
    [[ "$href" == \?* ]] && continue

    if [[ "$href" == */ ]]; then
      # folder, call recursively 
      # eliminate trailing slash 
      href="${href%/}"
      echo "folder $SERVER_DIR/$href"
      patchRec "$SERVER_DIR/$href" "$LOCAL_DIR/$href"
      RET=$(( RET | $? ))
    else
      # file, try to download 
      echo "file $SERVER_DIR/$href"
      wget -N "$SERVER_DIR/$href" -P "$LOCAL_DIR" || { warn "Downloading $href failed"; RET=$((RET | 2)); }
     fi
  done <<< "$lines"
  return $RET
}


#                                  INSTALL_DIR="$ROOT/$YEAR"
patchRec "$SERVER_MYPATCH/$YEAR" "$INSTALL_DIR/texmf-local"
# return value is ignored. patchRec emits warnings or errors if something went wrong. 

# This is a hack 
#set -x  # debug on 

pushd "$INSTALL_DIR" > /dev/null
  # do not overwrite what is already present, i.e. patched 
  cp -n texmf-dist/scripts/pythontex/* texmf-local/scripts/pythontex/
popd

pushd "$BIN_DIR" > /dev/null
  ln -sf ../../texmf-local/scripts/pythontex/pythontex.py     pythontex
  ln -sf ../../texmf-local/scripts/pythontex/depythontex.py depythontex
popd > /dev/null

#set +x  # debug off 

# # patch for (de-)pythontex
# DIR="$INSTALL_DIR/texmf-dist/scripts/pythontex"
# pushd "$DIR" > /dev/null || { error "Could not enter $DIR."; exit 6; }
#   FILE="pythontex3.py"
#   wget -N "$SERVER_MYPATCH/$YEAR/scripts/pythontex/$FILE" \
#     || warn "Downloading $FILE failed; skipping."
#   FILE="depythontex3.py"
#   wget -N "$SERVER_MYPATCH/$YEAR/scripts/pythontex/$FILE" \
#     || warn "Downloading $FILE failed; skipping."
#   FILE="pythontex_utils.py"
#   wget -N "$SERVER_MYPATCH/$YEAR/scripts/pythontex/$FILE" \
#     || warn "Downloading $FILE failed; skipping."
# popd > /dev/null

# # patch for tex4ht longtable 
# DIR="$INSTALL_DIR/texmf-dist/tex/generic/tex4ht"
# pushd "$DIR" > /dev/null || { error "Could not enter $DIR."; exit 6; }
#   FILE="longtable.4ht"
#   wget -N "$SERVER_MYPATCH/$YEAR/tex/generic/tex4ht/$FILE" \
#     || warn "Downloading $FILE failed; skipping."
# popd > /dev/null

# # patch for xindy hyperindex
# # DIR="$INSTALL_DIR/texmf-dist/xindy/tex"
# DIR="$INSTALL_DIR/texmf-dist/xindy"
# pushd "$DIR" > /dev/null || { error "Could not enter $DIR."; exit 6; }
#   FILE="hyperindex.xdy"
#   wget -N "$SERVER_MYPATCH/$YEAR/xindy/hyperindex.xdy" \
#     || warn "Downloading $FILE failed; skipping."
# popd > /dev/null

# TBD: this is something more generic: how to patch from file xxx.tds.zip 
# Here, tds stands for tex directory structure. 
# It is intended to be unpacked in DIR="$INSTALL_DIR/texmf-local/" and supplements the files already present. 
# This mechanism allows to apply quick patches without waiting for the texlive team to integrate. 
# Either texlive does not yet contain this package, or it contains a version which need to be patched. 
# One has to trace all files in xxx.tds.zip whether they are the same as in texml-dist. 
# If all are the same, a warning is needed that the patch is obsolete. 
# If at least one of the files in xxx.tds.zip differs from its counterpart in texml-dist, 
# as for example, does not exist in texml-dist or deviates, then the patch is still justified. 

# patch commutative-diagrams 
# PLEASE DO NOT REMOVE despite commutative-diagrams is now included in the base installation. 
# 
# Note that we install in tumbleweed installation of texlive only, not in the official one. 
# FILE="commutative-diagrams.tds.zip"
# URL="https://mirrors.ctan.org/install/graphics/pgf/contrib/$FILE"
# DIR="$INSTALL_DIR/texmf-local/"
# pushd "$DIR" > /dev/null || { error "Could not enter $DIR."; exit 6; }

#   # OLD_TS=0 if $FILE does not exist or timestamp cannot be found for some reason 
#   OLD_TS=$(stat -c %Y "$FILE" 2>/dev/null || echo 0)
#   # wget replaces only, if download succeeds 
#   wget -N --no-if-modified-since "$URL" \
#     || warn "Downloading $FILE failed, skip installation/update. "

#   NEW_TS=$(stat -c %Y "$FILE" 2>/dev/null || echo 0)
#   # If download failed, then FILE is not changed: either did not exist so after does not exist either. 
#   # or it is not changed. 
#   # In particular, the two timestamps coincide. 
#   if [ "$NEW_TS" -ne "$OLD_TS" ]; then
#   #if [ $(stat -c %Y "$FILE") -ne "$OLD_TS" ]; then
#     # only here do anything. 
#     unzip -o "$FILE"
#   fi
#popd > /dev/null

  # patch abstract.bst 

  # TBD: check all before removing this. 
  # FILE="abstract.bst"
  # DIR="$INSTALL_DIR/texmf-dist/bibtex/bst/abstract/"
  # pushd "$DIR" > /dev/null || { error "Could not enter $DIR."; exit 6; }
  #   wget -N "https://ctan.org/tex-archive/biblio/bibtex/utils/bibtools/$FILE" \
  #     || warn "Downloading $FILE failed; skipping."
  # popd > /dev/null


  #    TBD: It is not ok just to patch into an existing and clean texlive installation. 


info "Rehashing installation."
texhash || warn "Rehashing failed; skipping."



