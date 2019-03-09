@echo off
setlocal

rem ###########################################################################
rem #                             Sun Public License
rem #
rem # The contents of this file are subject to the Sun Public License Version
rem # 1.0 (the "License").  You may not use this file except in compliance with
rem # the License.  A copy of the License is available at http://www.sun.com/
rem #
rem # The Original Code is the SLAMD Distributed Load Generation Engine.
rem # The Initial Developer of the Original Code is Neil A. Wilson.
rem # Portions created by Neil A. Wilson are Copyright (C) 2004-2010.
rem # Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
rem # All Rights Reserved.
rem #
rem # Contributor(s):  Neil A. Wilson
rem ###########################################################################


if "%JAVA_HOME%" == "" goto noJavaHome
goto runAnt


:noJavaHome
echo ERROR:  Unable to determine the path to the JDK installation.
echo         Please specify it using the JAVA_HOME environment variable.
goto end


:runAnt
%~dp0\ext\ant\bin\ant %*


:end

