# SLAMD Distributed Load Generation Engine

![Image](resources/slamd_logo.gif)

---
> **NOTE:** This code has undergone lots of changes and cleanup since I dusted it off after it sat dormant for many years.  I've updated many of the libraries that it uses (including Tomcat, the UnboundID LDAP SDK for Java, and the Apache Commons FileUpload), and I ripped out the ASN.1 parsing code in favor of the APIs provided by the UnboundID LDAP SDK.  I also performed a whole lot of code cleanup, which was sorely needed because the project was started in the Java 1.3 days, before generics and foreach and lots of other nice features.  I've done some basic testing and things seem to be working, but all of this overhauling may have introduced some bugs or instability.  If you find bugs, please report them.  I do know that some of the resource monitoring code doesn't work so well on Linux anymore, since it depends on being able to parse the output of commands run on the underlying system, and Linux doesn't care so much about preserving stable output.

> **ALSO NOTE:** Although the functionality should largely be the same, the new version should not be considered backward compatible with the version that I last touched almost a decated ago.  Most of the jobs have been given different class names, and some of them have been completely rewritten.  There are also different default values for some settings.  I have made some minor updates to the API that could require you to update custom jobs you may have written.  You should definitely not try to upgrade an existing installation with job data to this new version of the code.  And even if you just start fresh with the new version, it's possible that I could make additional changes that could introduce further incompatibilities.  So if you use a non-release version of the updated SLAMD (and at this time, that's all that's available), just know that an upgrade might break things.
---

## About SLAMD

This repository holds the source code for the SLAMD Distributed Load Generation Engine, which is a Java-based tool designed for benchmarking and performance analysis of network-based applications.  It provides a Web-based interface for scheduling and running jobs and viewing the results, and clients that can be installed on the systems you want to use to generate the load against whatever you're testing.  You can use multiple clients simultaneously to generate more load against the target server or service.

SLAMD is especially good at benchmarking LDAP directory servers, but it can be used for other purposes as well, and includes minimal out-of-the-box support for HTTP, IMAP, POP3, SMTP, and SQL.  It offers an API for creating custom jobs, so you can write your own support for interacting with whatever you want.

SLAMD was primarily written by Neil A. Wilson.  It was first created at Sun Microsystems in 2002, under the direction of Steve Shoaff who suggested both the basic concept and the name.  The name SLAMD is a play on "slapd", which is used in the binary name of some types of LDAP directory servers (it kind of stands for "standalone LDAP daemon").

The original SLAMD code was maintained until about 2010, and then it just sort of went dormant for a long period of time.  It has been dusted off and many of its components updated.  Only minimal testing has been performed thus far, but it seems to be working okay.  No guarantee is made about backward compatibility with older versions, and you now need to use Java 8 or later to run it.

SLAMD is open source under the terms of the [Sun Public License version 1.0](LICENSE.txt).  That's the license under which it was originally released, and since no single entity holds the copyright to everything, that's the license that it's always going to have.


## Building SLAMD

To build SLAMD from source check it out of the repository with a command like:

    git checkout https://github.com/dirmgr/slamd.git

Then change into the new `slamd` directory and run `build.sh` on Linux/UNIX-based systems, or `build.bat` on Windows.  Once the build completes, go into `build/package` directory and you will see zip files for the SLAMD server, the client, and the resource monitor client.


## Running the SLAMD Server

If you want to run the SLAMD server locally, you can just go into the `build/package/slamd` directory and run `bin/startup.sh` (or `bin/startup.bat` on Windows).  If you want to install it somewhere else, then just copy the `slamd-{timestamp}.zip` file to the desired system, unpack it, and run the startup script.  Then, go to "http://{address}:8080/slamd" in a browser (where {address} is the address of the server on which SLAMD is running, or "localhost" if you're running it on your workstation).  The first time you run it, you should see a page telling you that you need to create a new database, which you can do by simply clicking a button.


## Running the SLAMD Client

To actually be able to do anything with SLAMD, you'll also need to create one or more clients.

To run the SLAMD client, unpack the `slamd_client-{timestamp}.zip` file on whatever machines you want to use as the client.  You'll need to edit the `slamd_client.conf` file to at least specify the address of the SLAMD server in the `SLAMD_ADDRESS` property (you probably won't want to change the port numbers).  If you just want to manually run a single client instance on that machine, then that should be all that you need to change.  If you would rather run the client manager, which can allow you to create and destroy client instances from the SLAMD server's web interface, then you may also want to adjust the `AUTO_CREATE_CLIENTS` and `MAX_CLIENTS` options.  If you only want this client to run jobs for which you explicitly request this client, then you can change `RESTRICTED_MODE` to `on`.

Once you have finished editing `slamd_client.conf`, you can start a single instance of the client with the `start_client.sh` (or `start_client.bat`) script.  If you want to start the client manager, then run `start_client_manager.sh` (or `start_client_manager.bat`) instead.  If you do run the client manager, you may need to go to the SLAMD Server Status page in the web interface and request that it create the desired number of client instances.


## Running the SLAMD Resource Monitor Client

SLAMD also provides a resource monitor client, which can measure various performance metrics (CPU utilization, network traffic, disk I/O, etc.) on systems where you're running the SLAMD clients or the server under load.

To run the SLAMD resource monitor client, unpack the `slamd_monitor_client-{timestamp}.zip` file on whatever systems you want to run it on.  Then, edit the `slamd_monitor_client.conf` file to at least specify the SLAMD server address and run the `start_monitor_client.sh` shell script (or `start_monitor_client.bat` batch file).


## Scheduling a Job

To schedule a job, click the "Schedule a Job" link in the navigation sidebar of the SLAMD server's web interface.  From there, you can choose the type of job you want to schedule, and then you'll see the parameters that are available for that job.  Some parameters are common to all types of jobs, including:

* Place in Folder -- The name of the folder in which the job should be organized.  By default, SLAMD only provides a single "Unclassified" folder, but you can create new folders using the "Real Job Folders" link in the sidebar.  A job can only exist in a single "real" folder, but you can also use virtual folders to have the job appear elsewhere as well if you'd like.

* Description -- A brief, one-line description for the job.  If you want to provide more detailed information, you can use the "Job Comments" section at the bottom of the page.

* Start Time -- The time that the job should start running.  You can specify a start time if you'd like the job to kick off at a specified time in the future, but if you leave this empty, or if you specify a time in the past, then the job will start running as soon as possible.

* Stop Time -- The time that you want the job to stop running, if you want to specify an absolute stop time.  In many cases, you'll probably want to specify a duration rather than a stop time.

* Duration -- The maximum length of time that you want the job to run.  You can provide this as just an integer value, which will be interpreted as the number of seconds that the job should run, but you can also include units if you want to make it a more human-readable value (for example, "5 minutes" or "2 hours").

* Number of Clients -- The number of clients that should be used to run the job concurrently.  If you indicate that multiple clients should be used, then the SLAMD server will do its best to start the job as close to the same time on all the client systems.  By default, the SLAMD server will pick any clients that are available (although it will try to pick clients from different systems if possible) and not operating in restricted mode.  If you want to explicitly specify which clients should be used, then you can use the "Use Specific Clients" field in the advanced scheduling options.

* Monitor Client if Available -- Indicates whether the SLAMD server should automatically collect statistics from any resource monitor clients that happen to be running on the same systems as the clients that it uses to run the job.  If you want to perform resource monitoring on other systems besides the clients (for example, the server under load), you can do that with the "Resource Monitor Clients" field in the advanced scheduling options.

* Wait for Available Clients -- Indicates whether the SLAMD server should wait for the desired clients to become available if they're not already available when it's time to start the job.  In most cases, you'll probably want to leave this checked.  If it's not checked and there aren't an acceptable set of clients ready when the server wants to start the job, then the job will instead be canceled.

* Threads Per Client -- The number of concurrent threads that should be used to run the job on each client system.  This should be an integer value that is greater than or equal to one.

* Statistics Collection Interval -- This indicates the time period over which the client should aggregate its statistics.  The smaller the value, the more data points that you'll have, but also the more space that will be required to hold the job results.  The minimum you can specify is 1 second, and that's a good choice for jobs that run for a very short period of time.  If you're running jobs for longer, then you'll probably want to have a higher collection interval.

* Job Comments -- Arbitrary additional comments that you want to make available for the job.

There will also be additional parameters that you can specify based on the type of job that you're running.  For example, if you're testing an LDAP directory server, then there will be fields for things like the address of the server, whether to connect securely, the authentication credentials, the base DN, etc.

Many jobs also provide parameters for specifying warm-up and cool-down intervals.  If present, these parameters indicate how long the job should run before it starts collecting statistics, and how soon it should stop collecting statistics before it finishes.  It can often be helpful to specify a warm-up interval to ensure that all of the clients have had a chance to ramp up and are generating a consistent load against the server, and to specify a cool-down interval to ensure that all the clients are still generating consistent load when the job stops collecting statistics.  Note that if you specify a warm-up and cool-down time, they will be part of the overall job duration, so you should adjust the duration accordingly.  For example, if you want the job to have a 20-second warm-up, collect statistics for 5 minutes, and a 10-second cool down, then you'd want to specify a total duration of "5 minutes, 30 seconds" (or 330 seconds if you prefer specifying it that way).

Once you've provided appropriate values for all of the parameters, you can click the "Test Job Parameters" button to allow the SLAMD server to try to perform some validation on the values that you've specified.  The type of validation varies with the type of job that you're using.  For example, if you're testing an LDAP directory server, then it might ensure that it can establish an LDAP connection to the desired server, authenticate with the provided credentials, and make sure that the base entry exists.

Once you're confident that everything looks right, you can click the "Schedule Job" button and the server will either wait for the start time to arrive or send it out for immediate processing.  The job will then run until it completes, until it has run for the maximum specified duration, or until the stop time arrives.  After it's complete, then the clients and resource monitor clients will return the data they've collected to the SLAMD server, and the web interface will show you the results numerically and can visually plot them on graphs.


## Documentation

You can find the SLAMD documentation in the [docs](docs) directory of this repository, or using the "SLAMD Documentation" link in the navigation sidebar of the SLAMD web interface.  The [SLAMD Quick Start Guide](docs/slamd_quick_start_guide.pdf) should help you get up and running quickly but with more depth than what's listed in this README file.

The [SLAMD Tools Guide](docs/tools_guide.pdf) provides information about some additional tools that ship with SLAMD.  Some of the tools include commands for load testing LDAP servers, for generating and analyzing LDIF data, for decoding LDAP traffic, and for capturing and replying TCP traffic.
