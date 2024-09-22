<!-- markdownlint-disable no-trailing-spaces -->
<!-- markdownlint-disable no-inline-html -->

# Features

The features consist in support of various [contexts](#contexts), 
input and ability to create [various output](#input-and-output), 
various [checks](#checks) and [orchestration and document development](#orchestration-and-document-development). 


## Contexts 

This piece of software consists of a 

- an ant task and 
- a maven plugin 
  
with a common source core. 


## Input and Output 

Supports 

- many output formats like PDF, DVI, HTML, DOCX, RTF, TXT and others 
- many graphical input formats like PNG, MP, FIG, gnuplot 
  and provides a separate goal creating them, `grp` 
- document classes including `book`, `article`, `leaflet`, `scrlttr2` and `beamer`; 
  for the latter also `beamerarticle` 
- bibliography, index/indices, glossary/glossaries and embedded code 


## Checks 

Checks 

- sources with `chktex` and logs the result in target and goal `chk` 
- versions of used tools 
- log files detecting errors and warnings 
- Check of return codes 
- further excessive logging of warnings and errors 
- whether a document could have been reproduced, by demand 


## Orchestration and Document Development 

Orchestrates various tools, detecting need for execution 
e.g. of `bibtex` including `rerunfilecheck` for `lualatex` and friends. 

Supports document development. 

- Provides goals `grp` to create graphic files and `clr` for cleaning up. 
- Cooperates by cooperating with editor, viewer 
  and with other tools in the build chain. 
  - Offers [installation script](./fromTex/instVScode4tex.sh) 
    for extensions of VS Code. 
  - Offers configuration file [`.chktexrc`](./fromTex/.chktexrc) 
    for `chktex`. 
  - Can create configuration file [`.latexmkrc`](./fromTex/.latexmkrc) 
    for `latexmk` synchronized with the configuration. 
  - Offers a common header file [`header.tex`](./fromTex/header.tex) 
    to unify mainly packages loaded by latex main files. 
  - Offers a common header file [`headerSuppressMetaPDF.tex`](./fromTex/headerSuppressMetaPDF.tex) 
    to keep up security, i.e. privacy quite independent of the tool chain. 


# Planned Features

- Support `biber` replacing `bibtex` as preferred tool
- Support `xindy` replacing `makeindex` as preferred tool
- Support `bib2gls` replacing `makeglossary` as preferred tool
- Execute `glosstex` if needed
- Usage of the `multibib` macros
- ...

# Feature Requests 

Feature requests from users are always welcome: 
[write me](mailto:rei3ner@arcor.de). 
