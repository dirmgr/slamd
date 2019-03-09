/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2010.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.tools.ldapdecoder.protocol;



/**
 * This class defines constants and a method for dealing with LDAP result codes.
 *
 *
 * @author   Neil A. Wilson
 */
public class LDAPResultCode
{
  /**
   * The result code that indicates the operation completed successfully.
   */
  public static final int SUCCESS = 0;



  /**
   * The result code that indicates there was an operations error.
   */
  public static final int OPERATIONS_ERROR = 1;



  /**
   * The result code that indicates there was a protocol error.
   */
  public static final int PROTOCOL_ERROR = 2;



  /**
   * The result code that indicates a configured time limit was exceeded.
   */
  public static final int TIME_LIMIT_EXCEEDED = 3;



  /**
   * The result code that indicates a configured size limit was exceeded.
   */
  public static final int SIZE_LIMIT_EXCEEDED = 4;



  /**
   * The result code that indicates that a compare operation did not match the
   * specified criteria.
   */
  public static final int COMPARE_FALSE = 5;



  /**
   * The result code that indicates that a compare operation matched the
   * specified criteria.
   */
  public static final int COMPARE_TRUE = 6;



  /**
   * The result code that indicates that the requested authentication method is
   * not supported.
   */
  public static final int AUTH_METHOD_NOT_SUPPORTED = 7;



  /**
   * The result code that indicates that strong authentication is required for
   * the requested operation.
   */
  public static final int STRONG_AUTH_REQUIRED = 8;



  /**
   * The result code that indicates that a referral has been returned.
   */
  public static final int REFERRAL = 10;



  /**
   * The result code that indicates that an administrative limit was exceeded.
   */
  public static final int ADMIN_LIMIT_EXCEEDED = 11;



  /**
   * The result code that indicates that the request contained an critical
   * control that is not supported.
   */
  public static final int UNAVAILABLE_CRITICAL_EXTENSION = 12;



  /**
   * The result code that indicates that confidentiality is required for the
   * requested operation.
   */
  public static final int CONFIDENTIALITY_REQUIRED = 13;



  /**
   * The result code that indicates that multi-stage SASL authentication is
   * in progress.
   */
  public static final int SASL_AUTHENTICATION_IN_PROGRESS = 14;



  /**
   * The result code that indicates that an operation referenced an attribute
   * that does not exist.
   */
  public static final int NO_SUCH_ATTRIBUTE = 16;



  /**
   * The result code that indicates the request contained an undefined attribute
   * type.
   */
  public static final int UNDEFINED_ATTRIBUTE_TYPE = 17;



  /**
   * The result code that indicates that an inappropriate type of matching
   * occurred.
   */
  public static final int INAPPROPRIATE_MATCHING = 18;



  /**
   * The result code that indicates that a constraint violation occurred.
   */
  public static final int CONSTRAINT_VIOLATION = 19;



  /**
   * The result code that indicates an attempt was made to add an attribute or
   * value that already exists in the entry.
   */
  public static final int ATTRIBUTE_OR_VALUE_EXISTS = 20;



  /**
   * The result code that indicates that the request attempted to use an
   * attribute with an invalid syntax.
   */
  public static final int INVALID_ATTRIBUTE_SYNTAX = 21;



  /**
   * The result code that indicates the user attempted to perform an operation
   * on an entry that doesn't exist.
   */
  public static final int NO_SUCH_OBJECT = 32;



  /**
   * The result code that indicates there was a problem with an alias.
   */
  public static final int ALIAS_PROBLEM = 33;



  /**
   * The result code that indicates the request contained a DN with an invalid
   * syntax.
   */
  public static final int INVALID_DN_SYNTAX = 34;



  /**
   * The result code that indicates a problem occurred while attempting to
   * dereference an alias.
   */
  public static final int ALIAS_DEREFERENCING_PROBLEM = 36;



  /**
   * The result code that indicates that an inappropriate form of
   * authentication has been requested.
   */
  public static final int INAPPROPRIATE_AUTHENTICATION = 48;



  /**
   * The result code that indicates that the user did not provide valid
   * credentials to perform the authentication.
   */
  public static final int INVALID_CREDENTIALS = 49;



  /**
   * The result code that indicates the authenticated user does not have
   * permission to perform the requested operation.
   */
  public static final int INSUFFICIENT_ACCESS_RIGHTS = 50;



  /**
   * The result code that indicates the server is too busy to perform the
   * requested operation.
   */
  public static final int BUSY = 51;



  /**
   * The result code that indicates the server is unavailable to perform the
   * requested operation.
   */
  public static final int UNAVAILABLE = 52;



  /**
   * The result code that indicates the server is unwilling to perform the
   * requested operation.
   */
  public static final int UNWILLING_TO_PERFORM = 53;



  /**
   * The result code that indicates that a referral loop has been detected.
   */
  public static final int LOOP_DETECT = 54;



  /**
   * The result code that indicates that a search request attempted to use a
   * VLV control without a server-side sorting control.
   */
  public static final int SORT_CONTROL_MISSING = 60;



  /**
   * The result code that indicates there was a naming violation.
   */
  public static final int NAMING_VIOLATION = 64;



  /**
   * The result code that indicates there was an objectclass violation.
   */
  public static final int OBJECTCLASS_VIOLATION = 65;



  /**
   * The result code that indicates that the requested operation is not allowed
   * on a non-leaf entry.
   */
  public static final int NOT_ALLOWED_ON_NONLEAF = 66;



  /**
   * The result code that indicates that the requested operation is not allowed
   * on the RDN attribute.
   */
  public static final int NOT_ALLOWED_ON_RDN = 67;



  /**
   * The result code that indicates that the resulting entry already exists.
   */
  public static final int ENTRY_ALREADY_EXISTS = 68;



  /**
   * The result code that indicates that objectClass modifications are
   * prohibited.
   */
  public static final int OBJECTCLASS_MODS_PROHIBITED = 69;



  /**
   * The result code that indicates that the operation could not be completed
   * because it affects multiple DSAs.
   */
  public static final int AFFECTS_MULTIPLE_DSAS = 71;



  /**
   * The result code that indicates that the associated operation was cancelled.
   */
  public static final int CANCELLED = 72;



  /**
   * The result code that indicates that a request was made to cancel an unknown
   * operation.
   */
  public static final int NO_SUCH_OPERATION = 73;



  /**
   * The result code that indicates that a cancel request was received too late
   * in the operation processing.
   */
  public static final int TOO_LATE = 74;



  /**
   * The result code that indicates that the requested cancel operation cannot
   * be performed on the target operation.
   */
  public static final int CANNOT_CANCEL = 75;



  /**
   * The result code that indicates that some other problem prevented the
   * operation from completed successfully.
   */
  public static final int OTHER = 80;



  /**
   * The result code that indicates that a proxied authorization request was
   * refused.
   */
  public static final int PROXIED_AUTHORIZATION_REFUSED = 123;



  /**
   * Retrieves a string representation of the provided result code.
   *
   * @param  resultCode  The result code for which to retrieve the string
   *                     representation.
   *
   * @return  The string representation of the provided result code.
   */
  public static String resultCodeToString(int resultCode)
  {
    switch (resultCode)
    {
      case SUCCESS:
        return "Success";
      case OPERATIONS_ERROR:
        return "Operations Error";
      case PROTOCOL_ERROR:
        return "Protocol Error";
      case TIME_LIMIT_EXCEEDED:
        return "Time Limit Exceeded";
      case SIZE_LIMIT_EXCEEDED:
        return "Size Limit Exceeded";
      case COMPARE_FALSE:
        return "Compare False";
      case COMPARE_TRUE:
        return "Compare True";
      case AUTH_METHOD_NOT_SUPPORTED:
        return "Auth Method Not Supported";
      case STRONG_AUTH_REQUIRED:
        return "Strong Auth Required";
      case REFERRAL:
        return "Referral";
      case ADMIN_LIMIT_EXCEEDED:
        return "Admin Limit Exceeded";
      case UNAVAILABLE_CRITICAL_EXTENSION:
        return "Unavailable Critical Extension";
      case CONFIDENTIALITY_REQUIRED:
        return "Confidentiality Required";
      case SASL_AUTHENTICATION_IN_PROGRESS:
        return "SASL Authentication in Progress";
      case NO_SUCH_ATTRIBUTE:
        return "No Such Attribute";
      case UNDEFINED_ATTRIBUTE_TYPE:
        return "Undefined Attribute Type";
      case INAPPROPRIATE_MATCHING:
        return "Inappropriate Matching";
      case CONSTRAINT_VIOLATION:
        return "Constraint Violation";
      case ATTRIBUTE_OR_VALUE_EXISTS:
        return "Attribute or Value Exists";
      case INVALID_ATTRIBUTE_SYNTAX:
        return "Invalid Attribute Syntax";
      case NO_SUCH_OBJECT:
        return "No Such Object";
      case ALIAS_PROBLEM:
        return "Alias Problem";
      case INVALID_DN_SYNTAX:
        return "Invalid DN Syntax";
      case ALIAS_DEREFERENCING_PROBLEM:
        return "Alias Dereferencing Problem";
      case INAPPROPRIATE_AUTHENTICATION:
        return "Inappropriate Authentication";
      case INVALID_CREDENTIALS:
        return "Invalid Credentials";
      case INSUFFICIENT_ACCESS_RIGHTS:
        return "Insufficient Access Rights";
      case BUSY:
        return "Busy";
      case UNAVAILABLE:
        return "Unavailable";
      case UNWILLING_TO_PERFORM:
        return "Unwilling to Perform";
      case LOOP_DETECT:
        return "Loop Detect";
      case SORT_CONTROL_MISSING:
        return "Sort Control Missing";
      case NAMING_VIOLATION:
        return "Naming Violation";
      case OBJECTCLASS_VIOLATION:
        return "ObjectClass Violation";
      case NOT_ALLOWED_ON_NONLEAF:
        return "Not Allowed on Nonleaf";
      case NOT_ALLOWED_ON_RDN:
        return "Not Allowed on RDN";
      case ENTRY_ALREADY_EXISTS:
        return "Entry Already Exists";
      case OBJECTCLASS_MODS_PROHIBITED:
        return "ObjectClass Mods Prohibited";
      case AFFECTS_MULTIPLE_DSAS:
        return "Affects Multiple DSAs";
      case CANCELLED:
        return "Cancelled";
      case NO_SUCH_OPERATION:
        return "No Such Operation";
      case TOO_LATE:
        return "Too Late";
      case CANNOT_CANCEL:
        return "Cannot Cancel";
      case OTHER:
        return "Other";
      case PROXIED_AUTHORIZATION_REFUSED:
        return "Proxied Authorization Refused";
      default:
        return "Unknown Result Code";
    }
  }
}

