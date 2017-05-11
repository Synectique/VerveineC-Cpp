# VerveineC-Cpp
A C/C++ to Famix extractor

## Description

This is a MSE (Famix) model extractor for C/C++. [Look here](https://en.wikipedia.org/wiki/Moose_(analysis)) to understand what are Famix and Moose.

This exporter works on top of the CDT plugin from Eclipse and is itself an Eclipse plugin. It is intended to run in an "headless Eclipse" (Eclipse running in "batch" mode).

It is originally developped on Eclipse 4.5.2 (Mars.2), build 20160218-0600 and CDT 8.8.1.201602051005

## Getting started: Get the Eclipse Distribution loccaly

The first step is to install [Eclipse](https://www.eclipse.org/downloads/). 

In order to build the parser you'll need to have two modules of Eclipse:
- **Eclipse C/C++ IDE CDT**
- **Eclipse PDE (Plug-in Development Environment)**

Those modules can be installed via the Eclipse Marketplace (available directly in the latest Eclipses). (See bellow for the tested versions of those modules)

Once everything is installed you can import VerveineC into eclipse in order to build it. 

The current version of VerveineC is built on Java7. If you use Java8 you need to update the JRE in the libraries of the project (*RightClick on the project -> Build Path -> Configure Build Path -> Libraries -> Add Library -> JRE System Library*)

You can now export the project into your plugins. 
*RightClick on the project -> Export -> Plug-in Development -> Deployable plug-ins and fragments*

Choose "Install into host" and finish the export. This will put a new file `verveine.extractor.Cpp_1.0.0.{some-date}.jar` in the "plugins" sub-directory of the Eclipse distribution.

In your install directory, you must have something likes this :

 ```
 app Directory
└── [drwxr-xr-x  238]  Contents
    ├── [drwxr-xr-x  612]  Eclipse
    │   ├── [drwxr-xr-x  102]  .eclipseextension
    │   ├── [-rw-r--r--   60]  .eclipseproduct
    │   ├── [drwxr-xr-x  442]  configuration
    │   ├── [drwxr-xr-x   68]  dropins
    │   ├── [-rw-r--r--  643]  eclipse.ini
    │   ├── [drwxr-xr-x 7.4K]  features
    │   ├── [-rw-r--r-- 6.1K]  notice.html
    │   ├── [drwxr-xr-x  238]  p2
    │   ├── [drwxr-xr-x  36K]  plugins
    │   ├── [drwxr-xr-x  102]  readme
    ├── [-rw-r--r-- 2.1K]  Info.plist
    ├── [drwxr-xr-x  102]  MacOS
    ├── [drwxr-xr-x  102]  Resources
    └── [drwxr-xr-x  102]  _CodeSignature
```

Put the folder verveine-launcher into app Directory/Contents/Eclipse.

In some cases, you must include your GCC install's folders in verveine-launcher/includes.vconf

## Generate a MSE


To run the extractor, go into app Directory/Contents/Eclipse/verveine-launcher and execute the command "verveineC.sh pathToDirectoryToParse".

You can add version information by editing verveine-launcher/verveine.vers.

## Tested setups

Eclipse | CDT | PDE | 
:-------|:-------|:-------|
4.5.2 (Mars.2) | 8.8.1.201602051005 | 3.10 |
4.5.2 (Mars.2) | 8.8.1.201602051005 | 3.11 |
