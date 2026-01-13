#!/usr/bin/env perl

# .latexmkrc adapted to the configurability of the latex-maven-plugin and its ant task 

# This is to check whether the pom really overwrites the settings in defaultSettingsAs.properties
# ${chkTexOptions}<chkTexOptions>-q -b0 -L -H1</chkTexOptions>

# to create pdf via lualatex 
#$pdflatex = 'lualatex -file-line-error %O %S';

# PDF-generating modes are:
# 0: do NOT generate a pdf version of the document. (default)
# 1: pdflatex, as specified by $pdflatex variable (still largely in use)
# 2: via postscript conversion, as specified by the $ps2pdf variable (useless)
# 3: via dvi conversion, as specified by the $dvipdf variable (useless)
# 4: lualatex, as specified by the $lualatex variable (best)
#    Note that we abuse that by replacing lualatex by a variable
# 5: xelatex, as specified by the $xelatex variable (second best)
$pdf_mode = 4;# specifies creation of pdf via lualatex 

# If additional parameters must be passed to lualatex 
#'svg' package.
# It converts raw SVG files to the PDF+PDF_TEX combo using InkScape.
# $lualatex = "lualatex --shell-escape";

# note that -recorder is implicitly added by latexmk, 
# so may be duplicated, but no disadvantage 
# %O is the options (additional options passed by latexmk)
# %S source file (maybe %A and %B more appropriate: without ending)
#$lualatex = "${getLatex2pdfCommand()} ${latex2pdfOptions} %O %S";
$lualatex = "internal run_latex %A %O";

# superfluous for perl >=5.36 according to documentation, but does not work for me (perl 5.38?)
use feature 'signatures';
# the following desirable but currently not possible 
#use strict;
#use warnings;


sub parseTexFile($fileName) {
  # The pattern is used to read magic comments. 
  # Double quotes because the pattern contains single quotes; 
  # no interpolation 
  my $patternLatexMainFile = "${patternLatexMainFile}";
  print("patternLatexMainFile: \n$patternLatexMainFile\n");
  open my $info, $fileName or die "Could not open $fileName: $!";
  # the lines read so far (each line with newline)
  my $lines = "";
  while (my $line = <$info>) {
    print "$line\n";
    $lines = "$lines$line";
    if ($line =~ /$patternLatexMainFile/) {
      #print("line matches |$+{programMagic}|\n");
      #print("line matches |$+{docClass}|\n");
      #print("lines: \n$lines\n");
      close $info;
      if ($lines !~ /$patternLatexMainFile/) {
        die("$fileName is no latex main file: preamble does not match\n");
      }
      print("preamble matches: \n");
      {
        no warnings;
        # for compatibility with other IDEs 
        print("programMagic=$+{programMagic}\n");
        # reflect settings for LatexBuilder 
        print("chkDiffMagic=$+{chkDiffMagic} value=$+{chkDiffMagicVal}\n");
        # intentionally ignored 
        print("latexmkMagic=$+{latexmkMagic} value=$+{latexmkMagicVal}\n");
        print("targetsMagic=$+{targetsMagic}\n");
        # parse argument of \DocumentMetadata{...}
        print("docMetadata=$+{docMetadata}\n");
      }
      print("docClass: $+{docClass}\n");
      # Make default value explicit 
      my $chkDiffMagic = ($+{chkDiffMagic} and not $+{chkDiffMagicVal})
        ? 'true' : $+{chkDiffMagicVal};
      # return the magic comments relevant in this context 
      return ($+{programMagic}, $chkDiffMagic);
    }
    # Here, the line does not match: go on 
  } # while 
  close $info;
  die("$fileName is no latex main file: no line match\n");
}

#parseTexFile($ARGV[0]);

use Cwd qw(getcwd);
use File::Spec::Functions qw(catfile);
use Capture::Tiny qw(capture_stdout);

#use DateTime;
use DateTime::Format::ISO8601; # for ->parse_datetime

sub getTimestampDiff($fileName) {
  # The following is to determing PDF file to diff if chkDiff is set 
  my $pdfFileOrg = catfile(getcwd(), $fileName);

  my $baseDirectory   = '${baseDirectory}/'; # trailing '/' for concatenation 
  my $texSrcDirectory = '${texSrcDirectory}/';
  my $diffDirectory   = '${diffDirectory}/';

  $pdfFileOrg =~ s/\Q$baseDirectory$texSrcDirectory//;
  my $pdfFileDiff = "$baseDirectory$diffDirectory$pdfFileOrg";
  if (!-e $pdfFileDiff) {
    # Here, $epoch_timestamp is not defined 
    return undef;
  }
  #die("File $pdfFileDiff to diff does not exist ") unless ;
  # epoch time of last modification # TBD: avoid magic number 9 
  my $epoch_timestamp = int((stat($pdfFileDiff))[9]);
  my $creationDateEpoch = getCreationTimeMetaEpoch($pdfFileDiff);
  # my ($stdout, $res) = capture_stdout { system("${getPdfMetainfoCommand()} ${pdfMetainfoOptions} $pdfFileDiff") };
  # print ("metainfo ok: $res\n");
  # print ("metainfos: \n$stdout\n");
  # $stdout =~ /CreationDate:\s*(?<creationDate>.*)\R/ or die("${getPdfMetainfoCommand()} did not get CreationDate. ");
  # print ("CreationDate: $+{creationDate}");
  # my $dt = DateTime::Format::ISO8601->parse_datetime($+{creationDate});
  print("+++meta epoch time: $creationDateEpoch\n");
  print("+++file epoch time: $epoch_timestamp\n");
  return $creationDateEpoch;
}

sub getCreationTimeMetaEpoch($pdfFile) {
  my ($stdout, $res) = capture_stdout {
      system("${getPdfMetainfoCommand()} ${pdfMetainfoOptions} $pdfFile"); # TBD: evaluate return value 
  };
  print("metainfo ok: $res\n");
  print("metainfos: \n$stdout\n");
  $stdout =~ /CreationDate:\s*(?<creationDate>.*)\R/
    or die("${getPdfMetainfoCommand()} did not get CreationDate. ");
  print("CreationDate: $+{creationDate}\n");
  my $dt = DateTime::Format::ISO8601->parse_datetime($+{creationDate});
  my $creationDateEpoch = $dt->epoch();
  print("internal epoch time: $creationDateEpoch\n");
  return $creationDateEpoch;
}


# TBD: not ideal foor pdfViaDvi=true: conversion dvi to pdf is needed only once at the end, 
# whereas this method does conversion dvi to pdf each time also tex to dvi is performed. 
# Thus in the long run only run dvi2pdf; the rest is done with rules. 
sub run_latex($fileName, @opts) {

  # transforms string representations from pom to perl specific representations 
  # maybe there are alternative: yes for true and no for false. Clarify. 
  my %boolStrToVal = (true => 1, false => 0);

  # This presupposes that latexmk is invoked with the filename without extension 
  ($programMagic, $chkDiffMagic) = parseTexFile("$fileName.tex");

  # override settings if magic comment is present 
  my $latexCommand = ($programMagic ? $programMagic : "${getLatex2pdfCommand()}");
  my $chkDiffB     = ($chkDiffMagic ? $chkDiffMagic : "${chkDiff}");
  $chkDiffB = $boolStrToVal{$chkDiffB};

  my $timeEnv = "";
  my $epoch_timestamp;
  if ($chkDiffB) {
    # # The following is to determing PDF file to diff if chkDiff is set 
    # my $pdfFileOrg=catfile(getcwd, "$fileName.pdf");

    # my $baseDirectory='${baseDirectory}/';# trailing '/' for concatenation 
    # my $texSrcDirectory='${texSrcDirectory}/';
    # my $diffDirectory='${diffDirectory}/';

    # $pdfFileOrg =~ s/\Q$baseDirectory$texSrcDirectory//;
    # my $pdfFileDiff = "$baseDirectory$diffDirectory$pdfFileOrg";
    # die("File $pdfFileDiff to diff does not exist ") unless (-e $pdfFileDiff);

    $epoch_timestamp = getTimestampDiff("$fileName.pdf");
    if (defined($epoch_timestamp)) {
      # Here, the reference file exists 
      # For lualatex setting TZ=UTC is needed but FORCE_SOURCE_DATE is ignored 
      # For pdflatex setting TZ=UTC is superfluous but FORCE_SOURCE_DATE is needed; 
      # the same for xelatex 
      $timeEnv = "TZ=UTC SOURCE_DATE_EPOCH=$epoch_timestamp FORCE_SOURCE_DATE=1 ";
    } else {
      # Here, the reference PDF file does not exist, so local time but with GMT timezone 
      $timeEnv = "TZ=UTC ";
    }
    # in both cases note the trailing blank 
    # The settings are required both for direct compilation into PDF and for compilation via DVI 
  }

  my $pdfViaDvi = $boolStrToVal{'${pdfViaDvi}'};
  # note that exactly one of the two options -no-pdf -output-format=dvi applies; 
  # the other is ignored. 
  # TBD: eliminate: xelatex emits a warning because -output-format is unknown 
  my $addArgs = $pdfViaDvi ? "-no-pdf -output-format=dvi " : "";
  # TBD: evaluate return values properly.  
  my $res = system("$timeEnv$latexCommand ${latex2pdfOptions} $addArgs @opts $fileName");
  if ($pdfViaDvi) {
    # Note that $timeEnv is first of all suitable for the latex compiler. 
    # strictly speaking FORCE_SOURCE_DATE is not needed; the other variables are needed 
    # to set up 
    # TBC: ignores options %O from latexmk, acceptable 
    $res = $res or
      system("$timeEnv${getDvi2pdfCommand()} ${dvi2pdfOptions} $fileName");
  }
  #print("invoke: ${getLatex2pdfCommand()} ${latex2pdfOptions} @opts $fileName\n");
  #return system("${getLatex2pdfCommand()} ${latex2pdfOptions} @opts $fileName");
  if ($chkDiffB) {
    if (not defined($epoch_timestamp)) {
      $epoch_timestamp = getCreationTimeMetaEpoch("$fileName.pdf");
    }
    $res = $res or utime($epoch_timestamp, $epoch_timestamp, "$fileName.pdf");
  }
  return $res;
}

#$postscript_mode = $dvi_mode = 0;

# to configure bibtex 
# bbl files are never precious 
$bibtex_use = 2;
$bibtex = "${bibtexCommand} ${bibtexOptions} %O %S";# default: bibtex %O %S

# this cannot be done according to the according latex maven plugin, 
# because the according parameter maxNumReRunsLatex may be set to -1 
# which signifies an infinite number of runs. 
$max_repeat = 30;

# default are tex and eps, but could also be pdf and ptx and mps
# Currently, all those files are given with explicit endings, 
# so no extensions to be added. 
#add_input_ext('');

# It is what it seems to be: clean inludes what was generated by cus 
$cleanup_includes_cusdep_generated = 1;
$cleanup_includes_generated = 1;

# TBD: clarify: xdv and dvi seem to be internal. 
# maybe missing other extensions in conjunction with synctex 
# maybe better @generated_exts see below 
push @generated_exts, "%R.synctex.gz", "%R.synctex";;

# bbl does not work
#@generated_exts = (@generated_exts, 'lol', 'bbl', 'glo', 'ist')
#print "Hello!"
#foreach (@generated_exts) {
#print "Generated exts: $_\n";
#}
#print "clean_ext\n";
# Here @generated_exts is ('aux', 'fls', 'log', # generated by latex already
# 'toc', 'lof', 'lot', 'out', # generated by latex conditionally 
# 'idx', 'ind', 'blg', 'ilg', # concerning indices 
# # strange enough: nothing for bibtex 
# 'xdv', 'bcf'

# extensions ext to be deleted by latexmk -c 
# Note that the file names are %R.ext. 
# this may cause problems with extensions containing a dot. 
# Also this is not general enough 
# if the generated file deviates from %R by more than an extension. 
# In this case, use the form with explicit '%R'. 
# list of listings, whereas lof and lot are already present. 
push @generated_exts, "lol";
push @generated_exts, "dvi", "xdv";
# for beamer class 
push @generated_exts, "nav", "snm", "vrb", 'run.xml';
push @generated_exts, "clg";# log file for chktex: specific for latex builder LMP 
push @generated_exts, "soc";# package changes, also adding toc if not present 
push @generated_exts, "sil";# package silence
push @generated_exts, "tdo";# package todonotes
push @generated_exts, "xmpdata";# written by filecontents for pdfx 

# why are 'ist' and 'xdy' not under generated_exts? 
# note that currently, either %R or what is present is the extension only! 
# this does not make sense very much. 
# $clean_ext .= " stateMachine.log"; does not work, because stateMachine.log is the extension! 

# should be under indexing 
push @generated_exts, '%ist', '%xdy', '%R-*.ind', '%R-*.idx', '%R-*.ilg', '%R-*.ind';

# many arguments shall be quoted 
# but in many cases it is immaterial; except in metapost 
sub quote {
  $inString = $_[0];
  #print "in: $inString\n";
  $outString = $inString;
  $outString =~ s/^ */'/;
  $outString =~ s/ *$/'/;
  $outString =~ s/ +/' '/g;
  $outString =~ s/''//;# empty if "''"
  
  #print "out: $outString\n";
  return $outString;
}


add_cus_dep('fig', 'ptx', 0, 'fig2dev');
sub fig2dev($file) {
  print("create 'ptx' from '$file.fig'\n");
  rdb_add_generated("$file.pdf", "$file.eps");
  #fig2dev -L pstex    <fig2devGenOptions> <fig2devPdfEpsOptions>        xxx.fig xxx.eps   
  #fig2dev -L pdftex   <fig2devGenOptions> <fig2devPdfEpsOptions>        xxx.fig xxx.pdf   
  #fig2dev -L pdftex_t <fig2devGenOptions> <fig2devPtxOptions>    -p xxx xxx.fig xxx.ptx
  # TBD: evaluate return values properly 
  my $ret1 = system(qq/${getFig2devCommand()} -L  pstex   ${fig2devGenOptions} ${fig2devPdfEpsOptions}       $file.fig $file.eps/);
  my $ret2 = system(qq/${getFig2devCommand()} -L pdftex   ${fig2devGenOptions} ${fig2devPdfEpsOptions}       $file.fig $file.pdf/);
  my $ret3 = system(qq/${getFig2devCommand()} -L pdftex_t ${fig2devGenOptions} ${fig2devPtxOptions} -p $file $file.fig $file.ptx/);

  return ($ret1 or $ret2 or $ret3);
}

my $gnuplotOptions = "";
add_cus_dep('gp', 'ptx', 0, 'gnuplot');
sub gnuplot($file) {
  print("create 'ptx' from '$file.gp'\n");
  rdb_add_generated("$file.pdf", "$file.eps");
  # here in the java code no quoting occurs 
  #my $gnuplotOptionsQ = quote(qq/${gnuplotOptions}/);
  my $ret1 = system(qq/${getGnuplotCommand()} -e "set terminal cairolatex pdf ${gnuplotOptions};\
            set output '$file.ptx';\
            load '$file.gp'"/);
  # my $ret2 = system("gnuplot -e \"set terminal cairolatex eps ${gnuplotOptions};\
  #           set output '$file.ptx';\
  #           load '$file.gp'\"");
  $res >>= 8; # reconstruct return value of the application 
  return $ret;
}

# metapost rule from http://tex.stackexchange.com/questions/37134
#add_cus_dep('mp', 'mps', 0, 'mpost');
add_cus_dep('mp', 'mps', 0, 'mpost');
sub mpost($file) {
  print("create 'mps' from '$file.mp'\n");
  rdb_add_generated("$file.mpx", "$file.fls", "$file.log");
  my ($name, $path) = fileparse($file);
  pushd($path);
  my $metapostOptionsQ = quote(qq/${metapostOptions}/);
  #print "quoted: $metapostOptionsQ\n";
  my $return = system(qq/${getMetapostCommand()} $metapostOptionsQ $name/);
  popd();
  $res >>= 8; # reconstruct return value of the application 
  return $return;
}


add_cus_dep('svg', 'ptx', 0, 'inkscape');
sub inkscape($file) {
  print("create  'ptx'from '$file.svg'\n");
  rdb_add_generated("$file.pdf", "$file.eps");
  my $ret1 = system(qq/${getSvg2devCommand()} --export-filename=$file.pdf ${svg2devOptions} $file.svg/);
  #my $ret2 = system("inkscape --export-filename=$file.eps -D --export-latex $file.svg ");
  #use File::Copy;
  # This works only for pdf, not for eps. 
  #unlink($file.pdf_tex) or die "cannot unlink $file.pdf_tex";
  rename("$file.pdf_tex", "$file.ptx");# or die "cannot move $file.pdf_tex";
  $res >>= 8; # reconstruct return value of the application 
  return $ret1;# or $ret2;
}


# graphics for xfig (not appropriate for mixed tex/pdf)

# add_cus_dep('fig', 'pdf', 0, 'fig2pdf');

# sub fig2pdf {
# system( "fig2dev -Lpdf \"$_[0].fig\" \"$_[0].pdf\"" );
# }

$makeindex = 'internal run_makeSplitindex %A %O';

# if used \sindex[idx]{} and no other index name, this is misleading: is a single multi-index
sub parseIdxFileForMultiIdx($fileName) {
  $fileName = "$fileName.idx";
  # since the patter is likely to end in $ 
  my $patternMultiIndex = '${patternMultiIndex}';
  open my $info, $fileName or die "Could not open $fileName: $!";
  my $foundDefaultEntry = 0;
  my %res = ();
  while (my $line = <$info>) {
    if ($line =~ /$patternMultiIndex/) {
      # Here, it is clear that we have a multi-index 
      $res{$2} = 1;
    } else {
      $foundDefaultEntry = 1;
    }
  }

  if ($foundDefaultEntry && %res) {
    # Here, both \index and \sindex are used 
    $res{"idx"} = 1;
  }
  close $info;
  # Here, it is clear that we have no multi-index 
  return keys %res;
}

sub run_makeSplitindex($fileName, @opts) {
  # $fileName is without ending 

  my @indexLabels = parseIdxFileForMultiIdx($fileName);
  if (@indexLabels) {
    # create dummy ind file 
    my $ind_fh = 'This is a dummy file. ';
    open($ind_fh, '>>', "$fileName.ind");
    close $ind_fh;

    foreach (@indexLabels) {
      # splitindex splits up the idx file into many 
      # and then generates ind file each 
      rdb_add_generated("$fileName-$_.idx", "$fileName-$_.ind");
    }

    $res = system("${splitIndexCommand} --makeindex ${makeIndexCommand} ${splitIndexOptions} $fileName -- ${makeIndexOptions} @opts");
  } else {
    $res = system("${makeIndexCommand} ${makeIndexOptions} @opts $fileName");
  }
  $res >>= 8; # reconstruct return value of the application 
  return $res
}

# This set of dependencies is only complete 
# if we restrict ourselves to types defined by options 
# without using \newglossary explicitly 
add_cus_dep( 'acn', 'acr', 0, 'run_makeglossaries' );
add_cus_dep( 'slo', 'sls', 0, 'run_makeglossaries' );
add_cus_dep( 'nlo', 'nls', 0, 'run_makeglossaries' );
#add_cus_dep( 'idx', 'ind', 0, 'run_makeglossaries' ); # would collide with indexing 
add_cus_dep( 'glo', 'gls', 0, 'run_makeglossaries' );
# TBD: add file endings for symbols, 
# not only here but also in the java code. 
push @generated_exts, 'glo', 'gls', 'glg';
push @generated_exts, 'acn', 'acr', 'alg';
push @generated_exts, 'slo', 'sls', 'slg';
push @generated_exts, 'nlo', 'nls', 'nlg';
# push @generated_exts, 'idx', 'ind', 'ilg'; # would collide with indexing 
push @generated_exts, "ist", "xdy"; # index stylefile created by the glossaries package 
# TBD: add xdy also in java code 

#$clean_ext .= " acr acn alg glo gls glg";# TBD: clarify: better in @generated_exts? 

sub run_makeglossaries($file) {
  my $options = "${makeGlossariesOptions}";
  if ($silent) {
    $options = "$options -q";
  }
  $res = system("${makeGlossariesCommand} $options $file");
  $res >>= 8; # reconstruct return value of the application 
  return $res;
}

# !!! ONLY WORKS WITH VERSION 4.54 or higher of latexmk
#TBD: take into account: modified:
#  '$_[0]'->

# #############
# # makeindex #
# #############
# @ist = glob("*.ist");
# if (scalar(@ist) > 0) {
#         $makeindex = "makeindex -s $ist[0] %O -o %D %S";
# }

# Implementing glossary with bib2gls and glossaries-extra, with the
#  log file (.glg) analyzed to get dependence on a .bib file.

# !!! ONLY WORKS WITH VERSION 4.54 or higher of latexmk

push @generated_exts, 'glg', '%R*.glstex';
# TBD: clarify 
#push @generated_exts, 'glg', '%R*.glstex';

# TBD: clarify treatment of encoding. 
# why we can avoid  
#        "--tex-encoding", "UTF-8",
#        "--log-encoding", "UTF-8",
# We need tex sources encoding in editor: utf8 
# in header 
# \iftutex%
#   \usepackage{fontspec}% sets utf8
# \else
#   % this seems to work with beamer also 
#   \usepackage[utf8]{inputenc}
#   \usepackage[T1]{fontenc}
# \fi
# in bib file: 1st line: 
# % Encoding: UTF-8


add_cus_dep('aux', 'glstex', 0, 'run_bib2gls');
# Explanation can be found in 
# https://tex.stackexchange.com/questions/400325/latexmkrc-for-bib2gls
sub run_bib2gls($file) {
  $options = "--group";
  if ($silent) {
    $options = "--silent $options";
  }
  my $ret = system("bib2gls $options $file");
  $ret >>= 8; # reconstruct return value of the application 
  if ($ret) {
    warn "Run_bib2gls: Error, running bib2gls; return value $ret.\n";
    return $ret;
  }

  # my ($base, $path) = fileparse($file);
  # if ($path && -e "$base.glstex") {
  #   rename "$base.glstex", "$path$base.glstex";
  # }

  # Analyze log file to find the bib-files.
  my $glg= "$file.glg";
  $isopen = open(my $glg_fh, '<', $glg);
  if (not $isopen) {
    warn "Run_bib2gls: Error opening log file '$glg'\n";
    return not @isopen;
  }
  rdb_add_generated($glg); 
 
  while (<$glg_fh>) {
    s/\s*$//;
    if (/^Reading\s+(.+)$/) {
      rdb_ensure_file($rule, $1);
    }
    if (/^Writing\s+(.+)$/) {
      rdb_add_generated($1);
    }
  }
  close $glg_fh;

  return $ret;
}

# The following code from John Collins is complementary to code in 
# changes/PythonTeXdep
# This code shall not be erased, it may only be deactivated. 
#$pythontex = 'pythontexW %R';#'pythontexW %O %R';
$pythontex = '${getPythontexCommand()} ${pythontexOptions} %R';#'pythontexW %O %R';

push @generated_exts, "pytxcode", "plg";
push @generated_exts, "depytx", "%R.depytx.tex", "dplg";

push @generated_exts, '${prefixPytexOutFolder}%R/*', '${prefixPytexOutFolder}%R';
#$extra_rule_spec{'pythontex'}  = [ 'internal', '', 'mypythontex', "%Y%R.pytxcode", "%Y${prefixPytexOutFolder}-%R/%R.pytxmcr", "%R", 1 ];
$extra_rule_spec{'pythontex'} = [
  'internal', '', 'mypythontex', 
  "%R.pytxcode", "${prefixPytexOutFolder}%R/%R.pytxmcr", "%R", 1
];

# Explanation for PythonTeX dependency 
# can be found in changes/PythonTeXdep 
# In fact, to make this work, 
# the code provided there must be included in package pythontex
# TBD: this function is invoked based on global variables. 
# For me, this is quite ugly... no parameters 
sub mypythontex {
  my $result_dir = $aux_dir1 . "${prefixPytexOutFolder}$$Pbase";
  my $ret        = Run_subst($pythontex, 2);
  rdb_add_generated(glob "$result_dir/*");

  my $fh = new FileHandle $$Pdest, "r";
  #open( my $fh, "<", $$Pdest );
  if ($fh) {
    #print "path: $ENV{PATH}";
    while (<$fh>) {
      if (/^%PythonTeX dependency:\s+'([^']+)';/) {
        print "Found pythontex dependency '$1'\n";
        rdb_ensure_file( $rule, $aux_dir1 . $1 );
      }
    }
    undef $fh;
  } else {
    warn "mypythontex: I could not read '$$Pdest'\n",
         "  to check dependencies\n";
  }
  return $ret;
}


# for htlatex 
push @generated_exts, "4tc", "4ct", "tmp", "xref", "css", "idv", "lg";
# TBD: for -C remove also html and xhtml
# TBD: check that this plugin also removes all these extensions.. think of lg. 


# biblatex
# push @generated_exts, "run.xml";# does run.xml work? 
# $clean_ext .= " %R-blx.bib";

$compiling_cmd = "internal run_preDecideCompilation %T %D";

sub run_preDecideCompilation {
  $source = $_[0];
  $target = $_[1];
  print("compiling $source to $target\n");
};

$success_cmd="internal run_onSuccess %D";

$warning_cmd="internal run_onWarn %D";

$failure_cmd="echo '...compilation with failure'";

sub run_onSuccess($target) {
  print("Compilation succeeded without warning.\n");
  run_onSuccessWarn($target);
}

sub run_onWarn($target) {
  print("Compilation succeeded with warning(s).\n");
  run_onSuccessWarn($target);
}

sub run_onSuccessWarn($target) {
  #$target = $_[0];

  if ($target !~ m/.pdf$/) {
    print("created no pdf\n");
    return;
  }
  # currently, postprocessing occurs for pdf files only 

  # Generalization needed for windows only 
  $bat="" ;
  $cmd="" ;
  if ($^O eq "Mswin32" ) {
    $bat=".bat";
    $cmd="cmd /c " ;
  } 
  my $res = system("$cmd" . "${verifyStdCommand}$bat ${verifyStdOptions} $target");
  $res >>= 8; # reconstruct return value of the application 
  print("${verifyStdCommand} return value: $res\n");
  if ($res == 0) {
    print("Conformance as requested. \n");
  } else {
    print("Conformance **not*** as requested. \n");
  }

};
