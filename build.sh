#!/bin/sh

###############################################################################
#                             Sun Public License
#
# The contents of this file are subject to the Sun Public License Version
# 1.0 (the "License").  You may not use this file except in compliance with
# the License.  A copy of the License is available at http://www.sun.com/
#
# The Original Code is the SLAMD Distributed Load Generation Engine.
# The Initial Developer of the Original Code is Neil A. Wilson.
# Portions created by Neil A. Wilson are Copyright (C) 2004-2010.
# Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
# All Rights Reserved.
#
# Contributor(s):  Neil A. Wilson
###############################################################################


# Change to the location of this build script.
cd `dirname $0`


# See if JAVA_HOME is set.  If not, then see if there is a java executable in
# the path and try to figure out JAVA_HOME from that.
if test -z "${JAVA_HOME}"
then
  CLASSPATH="ext"
  JAVA_HOME=`java -cp ${CLASSPATH} FindJavaHome`
  if test -z "${JAVA_HOME}"
  then
    echo "ERROR:  Unable to determine the path to the JDK installation."
    echo "        Please specify it using the JAVA_HOME environment variable."
    exit 1
  fi
fi


# Execute the ant build script and pass it any user-provided arguments.
ANT_HOME=`pwd`/ext/ant
export ANT_HOME
ext/ant/bin/ant -quiet --noconfig ${*}

