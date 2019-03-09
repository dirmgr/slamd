# SLAMD Distributed Load Generation Engine

![Image](resources/slamd_logo.gif)

This repository holds the source code for the SLAMD Distributed Load Generation Engine, which is a Java-based tool designed for benchmarking and performance analysis of network-based applications.  It provides a Web-based interface for scheduling and running jobs and viewing the results, and clients that can be installed on the systems you want to use to generate the load against whatever you're testing.  You can use multiple clients simultaneously to generate more load against the target server or service.

SLAMD is especially good at benchmarking LDAP directory servers, but it can be used for other purposes as well, and includes minimal out-of-the-box support for HTTP, IMAP, POP3, SMTP, and SQL.  It offers an API for creating custom jobs, so you can write your own support for interacting with whatever you want.

SLAMD was primarily written by Neil A. Wilson.  It was first created at Sun Microsystems in 2002, under the direction of Steve Shoaff who suggested both the basic concept and the name.  The name SLAMD is a play on "slapd", which is used in the binary name of some types of LDAP directory servers (it kind of stands for "standalone LDAP daemon").

The original SLAMD code was maintained until about 2010, and then it just sort of went dormant for a long period of time.  It has been dusted off and many of its components updated.  Only minimal testing has been performed thus far, but it seems to be working okay.  No guarantee is made about backward compatibility with older versions, and you now need to use Java 8 or later to run it.

SLAMD is open source under the terms of the [Sun Public License version 1.0](LICENSE.txt).  That's the license under which it was originally released, and since no single entity holds the copyright to everything, that's the license that it's always going to have.
