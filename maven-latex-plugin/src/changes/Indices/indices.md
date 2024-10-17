<!-- markdownlint-disable no-trailing-spaces -->
<!-- markdownlint-disable no-inline-html -->

# Index processing 

This document discusses advantages, disadvantages and ways 
to replace `makeindex` by newer index processors. 
The need comes from the fact, 
that `makeindex` is not really internationalized. 
The first trial to replace `makeindex` was `xindy`, 
but this is mostly abandoned. 
The two surviving successors are `upmendex` and `xindex` 
which both promise to be compatible with `makeindex` while supporting Unicode. 

Compatibility refers to the input-output behavior, i.e. the interface. 
The options are much different, so changing the program requires adapting options. 
The rest of the interface refers to files: 
The input is the IDX file containing the raw entries of the index 
not yet unified and not yet sorted, 
and the output is the according IND file essentially consisting of a `theindex` environment 
ready to be included into the document. 
As a side effect, errors, warnings and further info are logged into an ILG file. 
The form of the IDX file does not depend on the tool used 
and the IND files coincide essentially, i.e. neglecting styling. 

This analysis focuses on the analysis of the ILG file written. 

## The torture IDX 

The file [tort.idx](./tort.idx) is extracted from the `makeindex` package 
and serves as a test case. 
It illustrates what is written into the ILG file in various failure cases. 
We apply `makeindex` and the other candidates 
to `tort.idx` and compare the resulting ILG files and IND files. 

### `makeindex` on torture 

The original `makeindex` writes index file [tort-m.ind](./tort-m.ind) 
and log file [tort-m.ilg](./tort-m.ilg). 
This can be done invoking `makeindex -t tort-m.ilg -o tort-m.ind`. 
The log file starts with a banner 

```
This is makeindex, version 2.17 [TeX Live 2024] (kpathsea + Thai support).
```

followed by info that scanning 

```
Scanning input file tort.idx...
```

Then optionally there are errors. 
They all look similar to the following: 

```
!! Input index error (file = tort.idx, line = 1):
   -- Illegal null field.
```

Each entry contains the following pieces of information: 

- The leading `!! Input index error` indicates that this is an error. 
- Then follows the location of the error with 
  file name and line number within that file. 
  Note that `makeindex` can be run with various files at once. 
- Finally, the kind of error is given. 

After specifying each error individually, there is a statistics 

```
.done (29 entries accepted, 5 rejected).
```

The number of rejected entries is the number of errors. 
So if more than `0` entries are rejected, an error is found. 

The above statistics is on a separate line if and only if an error is found. 

Then follows indication of sorting maybe also including unification 
followed by generating the according IND file. 
It is something like 

```
Sorting entries....done (135 comparisons).
Generating output file tort.ind....
```

Seemingly, in this step no error or warning can occur. 
Then follow generation of the index file which may include warnings. 
These look something like 

```
## Warning (input = tort.idx, line = 23; output = tort.ind, line = 3):
   -- Range closing operator has an inconsistent encapsulator iii.
```

Warnings are separated by a blank line. 
They consist of 

- `## Warning ` indicating that it is a warning. 
- Indication of the location both in input and in output, 
  each given by name of file and line number in that file. 
- Finally, indication of the kind of warning on a separate line. 


Like for errors, also for warning a statistics follows like so: 

```
done (24 lines written, 10 warnings).
```

If the number of warnings is not zero, a warning is found. 
Caution: For one warning, we have singular. 

Finally, the written files are indicated: 

```
Output written in tort-m.ind.
Transcript written in tort-m.ilg.
```

### `upmendex` on torture


Analogously to `makeindex` 
the Unicode aware `upmendex` 
can be told to write an index file `tort-u.ind` 
and log file [tort-u.ilg](./tort-u.ilg). 
This can be done invoking `upmendex -t tort-m.ilg -o tort-m.ind`. 
The log file starts with a banner 

```
This is upmendex version 1.08 [ICU 75.1] (TeX Live 2024).
```

followed by info that scanning 

```
Scanning input file tort.idx.
```

Then optionally there are errors. 
They all look similar to the following: 

```
Error: Extra `@' in tort.idx, line 3.
```

The pieces of information given are essentially the same as for `makeindex`, 
but the form is more compact. 

After specifying each error individually, there is a statistics 

```
...done (125 entries accepted, 0 rejected).
125 entries accepted, 0 rejected.
```

Strange enough, this is essentially twice 
and the first is not on a separate line. 

As for `makeindex`, 
the number of rejected entries is the number of errors. 
So if more than `0` entries are rejected, an error is found. 


Compared to `makeindex`, fewer errors are recognized: 
Whereas extra `@` and extra `!` are recognized, extra `|` is not. 
Also, null fields are not recognized. 

Seemingly, if errors occur, no IND file is written and so consequently, 
no warnings (`makeindex` emits only writing an IND file), can occur. 

In presence of an error, the ILG file ends on something like 

```
2 errors, written in tort-u.ilg.
Nothing written in output file.
```

If no errors occur, 
then the ILG file goes on like that of `makeindex` with sorting. 
But this is divided into sorting index and sorting pages, probably within index entry. 
This is something like 

```
Sorting index....done(699 comparisons).
Sorting pages....done(55 comparisons).
```

As for `makeindex`, also for `upmendex` during writing the IND file, 
warnings may occur. 
If none occur the ILG file goes on and ends like so: 

```
Making index file....done.
0 warnings, written in test.ilg.
Output written in test.ind.
```

As you can see, we used index `test.idx` 
created from [test.tex](./test.tex). 
This is `makeindex`s manual which we consider [below](#index-of-manual). 

To analyze warnings of `upmendex`, 
we just eliminated lines in `tort.idx` causing an error 
which results in a 'weak torture index` [tortW.idx](./tortW.idx). 


Then the resulting ILG file [tortW-u.ilg] is something like 

```
Making index file.Warning: Unmatched range closing operator ')',missing(.
Warning: Unmatched range opening operator '(',missing).
...done.
2 warnings, written in tortW-u.ilg.
Output written in tortW-u.ind.
```

This shows that after `Making index file.` a newline is missing. 
Between `Making index file.` and `...done.` come the warnings 
each with a specific form and finally a statistics indicating the number of warnings. 

To be able to compare the warnings in the ILG file and the IND file created by `upmendex`, 
with those created by `makeindex`, we first apply makeindex on `tortW.idx`. 

As expected, `makeindex tortW -t tortW-m.ilg -o tortW-m.ind` 
yields an ILG file [tortW-m.ilg](./tortW-m.ilg) with fewer errors compared with [tort-m.ilg](./tort-m.ilg) 
and different files and line numbers, 
whereas [tortW-m.ind](./tortW-m.ind) equals [tort-m.ind](./tort-m.ind), which is reasonable. 


If we compare the warnings `upmendex` emits on [tortW.idx](./tortW.idx), 
to those emitted by `makeindex`, we see that `upmendex` does not specify the location of the warning, 
neither in the IDX file nor in the IND file. 
It gives no indication on the file nor on the line. 

Also, as for errors `upmendex` is less strict for warnings as well, 
resulting in much fewer warnings compared to `makeindex`. 

Finally, the IND files deviate: 

- `upmendex` includes entries `makeindex` rejects. 
  In a sense, `upmendex`' IND file is consistent with the fewer errors. 
- There are differences in ranges 
  when non-Arabic page numbers are involved. 
  This is consistent with the fewer warnings. 





### `xindex` on torture 

Even on the heavily wrong [tort.idx](./tort.idx), 
`xindex` does not give any indication that something may be wrong 
by emitting errors or warnings into `tort.ilg` 
or by not creating `tort.ind`. 
Instead, the latter is just empty. 

Whereas the other tools allow specifying IND file and ILG file separately, 
`xindex` does this in a sense synchronously: 
Invoking `xindex tort -o tort-x.ind` yields `tortng.ilg` (which is a bug) 
and empty `tort-x.ind`. 
Also, `xindex tort -o tort-x` yields `tort-x.ilg` and empty `tort-x`, 
which is not better. 

All in all, `xindex` seems bad quality. 


## Comparison

From the point of view of indication of possible failures, 
the ILG file of `xindex` is useless. 
In its manual it promises full compatibility to `makeindex` while supporting Unicode, 
but it fails to keep the promise. 

In contrast, the ILG of `upmendex` is not ideal compared with `makeindex`. 
Its manual promises compatibility to a wide extent without specifying the details. 
This promise is kept. 
It is worth contributing to recover from the slight deficiencies. 
These can be summarized as follows. 

- Whereas errors on extra `@` and `!` are recognized, extra `|` are not. 
- Errors on illegal null fields are not recognized. 
- Statistics on errors are written into the ILG file twice. 
- There are problems with newline. 
  For errors the symptom is that `...done` is on the same line as the last error. 
  This is because errors are written as `\n Error...` and before `...done` a newline is missing. 
  In contrast, for warnings the symptom is that the first warning is on the same line as 
  `Making index file.`. 
  This is because warnings are written as `Warning: ...\n` and after `Making index file.` 
  a newline is missing. 
  I would suggest inserting newline after `Scanning....` and after `Making index file`. 
  Both Warnings and errors shall end with newline; there shall be no leading newline. 
- Warnings are not located, neither in source nor in target. 
  The behavior of `makeindex` indicating file and line number both in source and in target. 
- Whereas unmatching range opening and range closing operators are recognized, 
  at least the following inconsistencies are not recognized: 
  - Range closing operator has an inconsistent encapsulator
  - Inconsistent page encapsulator ii within range
  - Conflicting entries: multiple encaps for the same page under same key.
  - Illegal range formation: starting & ending pages are of different types. 

## Unifying regular expressions 

The aim is to recognize errors and warnings. 
This is impossible for `xindex`, but there are ways to implement this for `makeindex` and `upmendex` 
in a unified manner. 

Essentially, there are two ways to do it: Either relying on 
- individual errors/warnings 
- or on the statistics. 

For individual errors we need 

```
^(!! Input index error |Error: )
```

whereas for individual warnings it would be 

```
(## Warning |Warning: )
```

Based on statistics it is for errors 

```
([0-9]+ entries accepted, [1-9][0-9]*+* rejected). 
```

and for warnings 

```
([0-9]+ lines written, [1-9][0-9]* warning(s?)| [1-9][0-9]* warning(s?), written in .+\.)
```

## Index of manual 

The [manual (test.tex)](./test.tex) does not compile because the IND file is missing. 
Here, only scroll mode and creating that file helps, 
i.e. invoking 

```
lualatex --interaction=scrollmode test. 
```

This creates the IDX file. 
We created the IND file with 

```
makeindex test -t test-m.ilg -o test-m.ind
```

as intended resulting in ILG file [test-m.ilg](./test-m.ilg) 
but also with `upmendex` resulting accordingly in [test-u.ilg](./test-u.ilg). 

We observe, that even for its own manual, 
`makeindex` emits a warning into the according ILG file: 

```
## Warning (input = test.idx, line = 85; output = test-m.ind, line = 121):
   -- Conflicting entries: multiple encaps for the same page under same key.
```

As for [torture tests](#the-torture-idx), 
also for the manual, `makeindex` is stricter than `upmendex` 
which emits no warnings for the manual. 

Note that the according IND files [test-m.ind](./test-m.ind) 
and [test-u.ind](./test-u.ind) coincide 
up to the location of the entry `\index` 
which `makeindex` places after `index`, 
whereas `upmendex` places it in front of `index`. 

This difference may be overcome just by using different indices 
for LaTeX commands. 

For `xindex` we only state that the differences with `makeindex` 
in the IND files are numerous so that we can say, 
`xindex` is incompatible. 
