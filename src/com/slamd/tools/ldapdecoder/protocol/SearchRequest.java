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



import java.io.PrintStream;
import java.util.Date;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1Enumerated;
import com.slamd.asn1.ASN1Integer;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines an LDAP search request, which is used to locate entries
 * in the directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class SearchRequest
       extends ProtocolOp
{
  /**
   * The search scope that indicates that only the base object will be
   * evaluated.
   */
  public static final int SCOPE_BASE_OBJECT = 0;



  /**
   * The search scope that indicates that only entries exactly one level below
   * the search base will be evaluated.
   */
  public static final int SCOPE_SINGLE_LEVEL = 1;



  /**
   * The search scope that indicates that the base object and all entries below
   * it will be evaluated.
   */
  public static final int SCOPE_WHOLE_SUBTREE = 2;



  /**
   * The search scope that indicates that all entries below the base object (but
   * not the base entry itself) will be evaluated.
   */
  public static final int SCOPE_SUBORDINATE_SUBTREE = 3;



  /**
   * The alias policy that indicates that aliases should never be dereferenced.
   */
  public static final int DEREF_NEVER = 0;



  /**
   * The alias policy that indicates that any aliases found when performing the
   * search should be dereferenced.
   */
  public static final int DEREF_IN_SEARCHING = 1;



  /**
   * The alias policy that indicates that the search base should be dereferenced
   * if it is an alias.
   */
  public static final int DEREF_FINDING_BASE_OBJECT = 2;



  /**
   * The alias policy that indicates that all aliases encountered should be
   * dereferenced.
   */
  public static final int DEREF_ALWAYS = 3;



  // Indicates whether to include only the attribute types and no values in the
  // matching entries.
  private boolean typesOnly;

  // The policy that should be used for handling aliases encountered while
  // searching.
  private int derefPolicy;

  // The scope to use for the search.
  private int scope;

  // The maximum number of entries that should be returned from the search.
  private int sizeLimit;

  // The maximum length of time that should be spent processing the search.
  private int timeLimit;

  // The search filter that specifies the criteria for finding entries.
  private SearchFilter filter;

  // The base DN to use for the search.
  private String baseDN;

  // The list of attributes that should be included in matching entries.
  private String[] attributes;



  /**
   * Creates a new search filter with the provided information.
   *
   * @param  baseDN       The base DN to use for the search.
   * @param  scope        The scope to use for the search.
   * @param  derefPolicy  The policy to use when handling aliases encountered
   *                      while searching.
   * @param  sizeLimit    The size limit to use for the search.
   * @param  timeLimit    The time limit to use for the search.
   * @param  typesOnly    Indicates whether to include only attribute type and
   *                      no values in the matching entries.
   * @param  filter       The filter to use for the search.
   * @param  attributes   The list of attributes to include in matching entries.
   */
  public SearchRequest(String baseDN, int scope, int derefPolicy, int sizeLimit,
                       int timeLimit, boolean typesOnly, SearchFilter filter,
                       String[] attributes)
  {
    this.baseDN      = baseDN;
    this.scope       = scope;
    this.derefPolicy = derefPolicy;
    this.sizeLimit   = sizeLimit;
    this.timeLimit   = timeLimit;
    this.typesOnly   = typesOnly;
    this.filter      = filter;
    this.attributes  = attributes;
  }



  /**
   * Retrieves the base DN to use for the search.
   *
   * @return  The base DN to use for the search.
   */
  public String getBaseDN()
  {
    return baseDN;
  }



  /**
   * Retrieves the scope to use for the search.
   *
   * @return  The scope to use for the search.
   */
  public int getScope()
  {
    return scope;
  }



  /**
   * Retrieves the alias dereferencing policy for the search.
   *
   * @return  The alias dereferencing policy for the search.
   */
  public int getDerefPolicy()
  {
    return derefPolicy;
  }



  /**
   * Retrieves the size limit to use for the search.
   *
   * @return  The size limit to use for the search.
   */
  public int getSizeLimit()
  {
    return sizeLimit;
  }



  /**
   * Retrieves the time limit to use for the search.
   *
   * @return  The time limit to use for the search.
   */
  public int getTimeLimit()
  {
    return timeLimit;
  }



  /**
   * Indicates whether only attribute types and no values should be included in
   * matching entries.
   *
   * @return  <CODE>true</CODE> if only attribute types and no values should be
   *          included in matching entries, or <CODE>false</CODE> if not.
   */
  public boolean getTypesOnly()
  {
    return typesOnly;
  }



  /**
   * Retrieves the filter to use for the search.
   *
   * @return  The filter to use for the search.
   */
  public SearchFilter getFilter()
  {
    return filter;
  }



  /**
   * Retrieves the set of attributes that should be included in matching
   * entries.
   *
   * @return  The set of attributes that should be included in matching entries.
   */
  public String[] getAttributes()
  {
    return attributes;
  }



  /**
   * Encodes this protocol op to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    ASN1Element attrSequence;
    if ((attributes == null) || (attributes.length == 0))
    {
      attrSequence = new ASN1Sequence();
    }
    else
    {
      ASN1Element[] elements = new ASN1Element[attributes.length];
      for (int i=0; i < elements.length; i++)
      {
        elements[i] = new ASN1OctetString(attributes[i]);
      }

      attrSequence = new ASN1Sequence(elements);
    }

    ASN1Element[] searchElements = new ASN1Element[]
    {
      new ASN1OctetString(baseDN),
      new ASN1Enumerated(scope),
      new ASN1Enumerated(derefPolicy),
      new ASN1Integer(sizeLimit),
      new ASN1Integer(timeLimit),
      new ASN1Boolean(typesOnly),
      filter.encode(),
      attrSequence
    };

    return new ASN1Sequence(SEARCH_REQUEST_TYPE, searchElements);
  }



  /**
   * Decodes the provided ASN.1 element as a search request protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded search request.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the provided
   *                             ASN.1 element as a search request.
   */
  public static SearchRequest decodeSearchRequest(ASN1Element element)
         throws ProtocolException
  {
    ASN1Element[] searchElements;
    try
    {
      searchElements = element.decodeAsSequence().getElements();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request sequence",
                                  e);
    }


    if (searchElements.length != 8)
    {
      throw new ProtocolException("There must be exactly eight elements in a " +
                                  "search request sequence");
    }


    String baseDN;
    try
    {
      baseDN = searchElements[0].decodeAsOctetString().getStringValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request base DN", e);
    }


    int scope;
    try
    {
      scope = searchElements[1].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request scope", e);
    }


    int derefPolicy;
    try
    {
      derefPolicy = searchElements[2].decodeAsEnumerated().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request alias " +
                                  "dereferencing policy", e);
    }


    int sizeLimit;
    try
    {
      sizeLimit = searchElements[3].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request size limit",
                                  e);
    }


    int timeLimit;
    try
    {
      timeLimit = searchElements[4].decodeAsInteger().getIntValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request time limit",
                                  e);
    }


    boolean typesOnly;
    try
    {
      typesOnly = searchElements[5].decodeAsBoolean().getBooleanValue();
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request typesOnly",
                                  e);
    }


    SearchFilter filter = SearchFilter.decode(searchElements[6]);


    String[] attributes;
    try
    {
      ASN1Element[] attrElements =
           searchElements[7].decodeAsSequence().getElements();
      attributes = new String[attrElements.length];
      for (int i=0; i < attrElements.length; i++)
      {
        attributes[i] = attrElements[i].decodeAsOctetString().getStringValue();
      }
    }
    catch (Exception e)
    {
      throw new ProtocolException("Unable to decode search request attribute " +
                                  "list", e);
    }


    return new SearchRequest(baseDN, scope, derefPolicy, sizeLimit, timeLimit,
                             typesOnly, filter, attributes);
  }



  /**
   * Retrieves a user-friendly name for this protocol op.
   *
   * @return  A user-friendly name for this protocol op.
   */
  public String getProtocolOpType()
  {
    return "LDAP Search Request";
  }



  /**
   * Retrieves a string representation of this protocol op with the specified
   * indent.
   *
   * @param  indent  The number of spaces to indent the output.
   *
   * @return  A string representation of this protocol op with the specified
   *          indent.
   */
  public String toString(int indent)
  {
    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0; i < indent; i++)
    {
      indentBuf.append(' ');
    }


    StringBuilder buffer = new StringBuilder();
    buffer.append(indentBuf).append("Base DN:  ").
           append(baseDN).append(LDAPMessage.EOL);

    String scopeStr;
    switch (scope)
    {
      case SCOPE_BASE_OBJECT:
        scopeStr = " (baseObject)";
        break;
      case SCOPE_SINGLE_LEVEL:
        scopeStr = " (singleLevel)";
        break;
      case SCOPE_WHOLE_SUBTREE:
        scopeStr = " (wholeSubtree)";
        break;
      case SCOPE_SUBORDINATE_SUBTREE:
        scopeStr = " (subordinateSubtree)";
        break;
      default:
        scopeStr =" (Invalid Search Scope)";
        break;
    }
    buffer.append(indentBuf).append("Scope:  ").append(scope).
           append(scopeStr).append(LDAPMessage.EOL);

    String derefStr;
    switch (derefPolicy)
    {
      case DEREF_NEVER:
        derefStr = " (neverDerefAliases)";
        break;
      case DEREF_IN_SEARCHING:
        derefStr = " (derefInSearching)";
        break;
      case DEREF_FINDING_BASE_OBJECT:
        derefStr = " (derefFindingBaseObj)";
        break;
      case DEREF_ALWAYS:
        derefStr = " (derefAlways)";
        break;
      default:
        derefStr = " (Invalid Dereferencing Policy)";
        break;
    }
    buffer.append(indentBuf).append("Deref Aliases:  ").append(derefPolicy).
           append(derefStr).append(LDAPMessage.EOL);

    buffer.append(indentBuf).append("Size Limit:  ").append(sizeLimit).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("Time Limit:  ").append(timeLimit).
           append(LDAPMessage.EOL);
    buffer.append(indentBuf).append("Types Only:  ").append(typesOnly).
           append(LDAPMessage.EOL);

    buffer.append(indentBuf).append("Filter:  ");
    filter.toStringBuilder(buffer);
    buffer.append(LDAPMessage.EOL);

    buffer.append(indentBuf).append("Attributes:").append(LDAPMessage.EOL);
    for (int i=0; ((attributes != null) && (i < attributes.length)); i++)
    {
      buffer.append(indentBuf).append("    ").append(attributes[i]).
             append(LDAPMessage.EOL);
    }

    return buffer.toString();
  }



  /**
   * Constructs a string representation of this LDAP message in a form that can
   * be written to a SLAMD script.  It may be empty if this message isn't one
   * that would be generated as part of a client request.
   *
   * @param  scriptWriter  The print stream to which the script contents should
   *                       be written.
   */
  public void toSLAMDScript(PrintStream scriptWriter)
  {
    scriptWriter.println("#### Search request captured at " + new Date());
    scriptWriter.println("# Search Base:  " + baseDN);

    String scopeStr = String.valueOf(scope);
    switch(scope)
    {
      case SCOPE_BASE_OBJECT:
        scriptWriter.println("# Scope:  baseObject");
        scopeStr = "conn.scopeBase()";
        break;
      case SCOPE_SINGLE_LEVEL:
        scriptWriter.println("# Scope:  singleLevel");
        scopeStr = "conn.scopeOne()";
        break;
      case SCOPE_WHOLE_SUBTREE:
        scriptWriter.println("# Scope:  wholeSubtree");
        scopeStr = "conn.scopeSub()";
        break;
    }

    switch (derefPolicy)
    {
      case DEREF_NEVER:
        scriptWriter.println("# Deref Policy:  neverDerefAliases");
        break;
      case DEREF_IN_SEARCHING:
        scriptWriter.println("# Deref Policy:  derefInSearching");
        break;
      case DEREF_FINDING_BASE_OBJECT:
        scriptWriter.println("# Deref Policy:  derefFindingBaseObj");
        break;
      case DEREF_ALWAYS:
        scriptWriter.println("# Deref Policy:  derefAlways");
        break;
    }

    scriptWriter.println("# Size Limit:  " + sizeLimit);
    scriptWriter.println("# Time Limit:  " + timeLimit);
    scriptWriter.println("# Types Only:  " + typesOnly);

    String filterString = filter.toString();
    scriptWriter.println("# Filter:  " + filterString);

    if ((attributes != null) && (attributes.length > 0))
    {
      scriptWriter.println("# Attributes to Return:");
      for (int i=0; i < attributes.length; i++)
      {
        scriptWriter.println("#   " + attributes[i]);
      }
    }

    scriptWriter.println("searchAttrs.removeAll();");
    if ((attributes != null) && (attributes.length > 0))
    {
      for (int i=0; i < attributes.length; i++)
      {
        scriptWriter.println("searchAttrs.addValue(\"" + attributes[i] +
                             "\");");
      }
    }

    scriptWriter.println("resultCode = conn.search(\"" + baseDN + "\", " +
                         scopeStr + ", \"" + filterString +
                         "\", searchAttrs, " + timeLimit + ", " + sizeLimit +
                         ");");
    scriptWriter.println();
    scriptWriter.println();
  }
}

