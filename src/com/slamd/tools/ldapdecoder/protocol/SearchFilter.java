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



import java.util.ArrayList;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;
import com.slamd.asn1.ASN1Set;



/**
 * This class defines an LDAP search filter, which specifies criteria to use for
 * finding entries in a directory server.
 *
 *
 * @author   Neil A. Wilson
 */
public class SearchFilter
{
  /**
   * The ASN.1 type for AND filters.
   */
  public static final byte AND_FILTER_TYPE = (byte) 0xA0;



  /**
   * The ASN.1 type for OR filters.
   */
  public static final byte OR_FILTER_TYPE = (byte) 0xA1;



  /**
   * The ASN.1 type for NOT filters.
   */
  public static final byte NOT_FILTER_TYPE = (byte) 0xA2;



  /**
   * The ASN.1 type for equality filters.
   */
  public static final byte EQUALITY_FILTER_TYPE = (byte) 0xA3;



  /**
   * The ASN.1 type for substring filters.
   */
  public static final byte SUBSTRING_FILTER_TYPE = (byte) 0xA4;



  /**
   * The ASN.1 type for greater or equal filters.
   */
  public static final byte GREATER_OR_EQUAL_FILTER_TYPE = (byte) 0xA5;



  /**
   * The ASN.1 type for less or equal filters.
   */
  public static final byte LESS_OR_EQUAL_FILTER_TYPE = (byte) 0xA6;



  /**
   * The ASN.1 type for presence filters.
   */
  public static final byte PRESENCE_FILTER_TYPE = (byte) 0x87;



  /**
   * The ASN.1 type for approximate filters.
   */
  public static final byte APPROXIMATE_FILTER_TYPE = (byte) 0xA8;



  /**
   * The ASN.1 type for extensible matching filters.
   */
  public static final byte EXTENSIBLE_MATCH_FILTER_TYPE = (byte) 0xA9;



  /**
   * The ASN.1 type for subInitial value in a substring filter.
   */
  public static final byte SUBINITIAL_TYPE = (byte) 0x80;



  /**
   * The ASN.1 type for subAny values in a substring filter.
   */
  public static final byte SUBANY_TYPE = (byte) 0x81;



  /**
   * The ASN.1 type for subFinal value in a substring filter.
   */
  public static final byte SUBFINAL_TYPE = (byte) 0x82;



  /**
   * The ASN.1 type for the matching rule ID in an extensible match filter.
   */
  public static final byte EXTENSIBLE_MATCH_RULE_ID_TYPE = (byte) 0x81;



  /**
   * The ASN.1 type for the attribute type ID in an extensible match filter.
   */
  public static final byte EXTENSIBLE_MATCH_ATTRIBUTE_TYPE = (byte) 0x82;



  /**
   * The ASN.1 type for the match value ID in an extensible match filter.
   */
  public static final byte EXTENSIBLE_MATCH_VALUE_TYPE = (byte) 0x83;



  /**
   * The ASN.1 type for the indicator of whether to include DN attributes for an
   * extensible match filter.
   */
  public static final byte EXTENSIBLE_MATCH_INCLUDE_DN_TYPE = (byte) 0x84;



  // Indicates whether to include DN attributes in the extensible matching.
  private boolean includeDNAttributes;

  // The filter type for this search filter.
  private byte filterType;

  // The set of subfilters for this search filter.
  private SearchFilter[] subFilters;

  // The attribute type for this search filter.
  private String attributeType;

  // The assertion value for this search filter.
  private String assertionValue;

  // The OID of the matching rule for the extensible matching.
  private String matchingRuleID;

  // The subFinal portion of the substring filter.
  private String subFinalValue;

  // The subInitial portion of the substring filter.
  private String subInitialValue;

  // The subAny portion of the substring filter.
  private String[] subAnyValues;



  /**
   * Creates a new search filter with the provided information.
   *
   * @param  filterType           The filter type for this filter.
   * @param  subFilters           The set of subfilters for this filter.
   * @param  attributeType        The attribute type for this filter.
   * @param  assertionValue       The assertion value for this filter.
   * @param  subInitialValue      The subInitial value for this substring
   *                              filter.
   * @param  subAnyValues         The subAny values for this substring filter.
   * @param  subFinalValue        The subFinal value for this substring filter.
   * @param  matchingRuleID       The matching rule ID for this extensible
   *                              matching filter.
   * @param  includeDNAttributes  Indicates whether to include DN attributes in
   *                              this extensible matching filter.
   */
  private SearchFilter(byte filterType, SearchFilter[] subFilters,
                       String attributeType, String assertionValue,
                       String subInitialValue, String[] subAnyValues,
                       String subFinalValue, String matchingRuleID,
                       boolean includeDNAttributes)
  {
    this.filterType          = filterType;
    this.subFilters          = subFilters;
    this.attributeType       = attributeType;
    this.assertionValue      = assertionValue;
    this.subInitialValue     = subInitialValue;
    this.subAnyValues        = subAnyValues;
    this.subFinalValue       = subFinalValue;
    this.matchingRuleID      = matchingRuleID;
    this.includeDNAttributes = includeDNAttributes;
  }



  /**
   * Creates a new AND search filter containing the provided subfilters.
   *
   * @param  subFilters  The subfilters to contain in the AND filter.
   *
   * @return  An AND search filter containing the provided subfilters.
   */
  public static SearchFilter createANDFilter(SearchFilter[] subFilters)
  {
    return new SearchFilter(AND_FILTER_TYPE, subFilters, null, null, null, null,
                            null, null, false);
  }



  /**
   * Creates a new OR search filter containing the provided subfilters.
   *
   * @param  subFilters  The subfilters to contain in the OR filter.
   *
   * @return  An OR search filter containing the provided subfilters.
   */
  public static SearchFilter createORFilter(SearchFilter[] subFilters)
  {
    return new SearchFilter(OR_FILTER_TYPE, subFilters, null, null, null, null,
                            null, null, false);
  }



  /**
   * Creates a new NOT search filter containing the provided subfilter.
   *
   * @param  subFilter  The subfilter to contain in the NOT filter.
   *
   * @return  A NOT search filter containing the provided subfilter.
   */
  public static SearchFilter createNOTFilter(SearchFilter subFilter)
  {
    return new SearchFilter(NOT_FILTER_TYPE, new SearchFilter[] { subFilter },
                            null, null, null, null, null, null, false);
  }



  /**
   * Creates a new equality search filter containing the specified attribute
   * type and assertion value.
   *
   * @param  attributeType   The attribute type for the equality search filter.
   * @param  assertionValue  The assertion value for the equality search filter.
   *
   * @return  The requested equality search filter.
   */
  public static SearchFilter createEqualityFilter(String attributeType,
                                                  String assertionValue)
  {
    return new SearchFilter(EQUALITY_FILTER_TYPE, null, attributeType,
                            assertionValue, null, null, null, null, false);
  }



  /**
   * Creates a new substring search filter with the provided information.
   *
   * @param  attributeType    The attribute type for the substring filter.
   * @param  subInitialValue  The subInitial value for the substring filter.
   * @param  subAnyValues     The subAny values for the substring filter.
   * @param  subFinalValue    The subFinal value for the substring filter.
   *
   * @return  The requested substring search filter.
   */
  public static SearchFilter createSubstringFilter(String attributeType,
                                                   String subInitialValue,
                                                   String[] subAnyValues,
                                                   String subFinalValue)
  {
    return new SearchFilter(SUBSTRING_FILTER_TYPE, null, attributeType, null,
                            subInitialValue, subAnyValues, subFinalValue, null,
                            false);
  }



  /**
   * Creates a new greater or equal search filter containing the specified
   * attribute type and assertion value.
   *
   * @param  attributeType   The attribute type for the greater or equal search
   *                         filter.
   * @param  assertionValue  The assertion value for the greater or equal search
   *                         filter.
   *
   * @return  The requested greater or equal search filter.
   */
  public static SearchFilter createGreaterOrEqualFilter(String attributeType,
                                                        String assertionValue)
  {
    return new SearchFilter(GREATER_OR_EQUAL_FILTER_TYPE, null, attributeType,
                            assertionValue, null, null, null, null, false);
  }



  /**
   * Creates a new less or equal search filter containing the specified
   * attribute type and assertion value.
   *
   * @param  attributeType   The attribute type for the less or equal search
   *                         filter.
   * @param  assertionValue  The assertion value for the less or equal search
   *                         filter.
   *
   * @return  The requested less or equal search filter.
   */
  public static SearchFilter createLessOrEqualFilter(String attributeType,
                                                     String assertionValue)
  {
    return new SearchFilter(LESS_OR_EQUAL_FILTER_TYPE, null, attributeType,
                            assertionValue, null, null, null, null, false);
  }



  /**
   * Creates a new presence search filter with the specified attribute type.
   *
   * @param  attributeType  The attribute type for the presence search filter.
   *
   * @return  The requested presence search filter.
   */
  public static SearchFilter createPresenceFilter(String attributeType)
  {
    return new SearchFilter(PRESENCE_FILTER_TYPE, null, attributeType, null,
                            null, null, null, null, false);
  }



  /**
   * Creates a new approximate search filter containing the specified attribute
   * type and assertion value.
   *
   * @param  attributeType   The attribute type for the approximate search
   *                         filter.
   * @param  assertionValue  The assertion value for the approximate search
   *                         filter.
   *
   * @return  The requested approximate search filter.
   */
  public static SearchFilter createApproximateFilter(String attributeType,
                                                     String assertionValue)
  {
    return new SearchFilter(APPROXIMATE_FILTER_TYPE, null, attributeType,
                            assertionValue, null, null, null, null, false);
  }



  /**
   * Creates a new extensible matching filter with the provided information.
   *
   * @param  matchingRuleID       The OID of the matching rule to use for the
   *                              extensible matching filter.
   * @param  attributeType        The attribute type to use for the extensible
   *                              matching filter.
   * @param  assertionValue       The assertion value to use for the extensible
   *                              matching filter.
   * @param  includeDNAttributes  Includes whether to consider DN attributes
   *                              when performing the matching.
   *
   * @return  The requested extensible matching search filter.
   */
  public static SearchFilter createExtensibleFilter(String matchingRuleID,
                                                    String attributeType,
                                                    String assertionValue,
                                                    boolean includeDNAttributes)
  {
    return new SearchFilter(EXTENSIBLE_MATCH_FILTER_TYPE, null, attributeType,
                            assertionValue, null, null, null, matchingRuleID,
                            includeDNAttributes);
  }



  /**
   * Retrieves the filter type for this search filter.
   *
   * @return  The filter type for this search filter.
   */
  public byte getFilterType()
  {
    return filterType;
  }



  /**
   * Retrieves the set of subfilters for this search filter.
   *
   * @return  The set of subfilters for this search filter.
   */
  public SearchFilter[] getSubFilters()
  {
    return subFilters;
  }



  /**
   * Retrieves the attribute type for this search filter.
   *
   * @return  The attribute type for this search filter.
   */
  public String getAttributeType()
  {
    return attributeType;
  }



  /**
   * Retrieves the assertion value for this search filter.
   *
   * @return  The assertion value for this search filter.
   */
  public String getAssertionValue()
  {
    return assertionValue;
  }



  /**
   * Retrieves the subInitial value for this search filter.
   *
   * @return  The subInitial value for this search filter.
   */
  public String getSubInitialValue()
  {
    return subInitialValue;
  }



  /**
   * Retrieves the set of subAny values for this search filter.
   *
   * @return  The subAny values for this search filter.
   */
  public String[] getSubAnyValues()
  {
    return subAnyValues;
  }



  /**
   * Retrieves the subFinal value for this search filter.
   *
   * @return  The subFinal value for this search filter.
   */
  public String getSubFinalValue()
  {
    return subFinalValue;
  }



  /**
   * Retrieves the matching rule ID for this search filter.
   *
   * @return  The matching rule ID for this search filter.
   */
  public String getMatchingRuleID()
  {
    return matchingRuleID;
  }



  /**
   * Indicates whether to include DN attributes when performing the extensible
   * matching.
   *
   * @return  <CODE>true</CODE> if DN attributes should be considered when
   *          performing the extensible matching, or <CODE>false</CODE> if not.
   */
  public boolean includeDNAttributes()
  {
    return includeDNAttributes;
  }



  /**
   * Encodes this search filter to an ASN.1 element.
   *
   * @return  The ASN.1 element containing the encoded search filter.
   */
  public ASN1Element encode()
  {
    switch (filterType)
    {
      case AND_FILTER_TYPE:
      case OR_FILTER_TYPE:
        ASN1Element[] elements = new ASN1Element[subFilters.length];
        for (int i=0; i < subFilters.length; i++)
        {
          elements[i] = subFilters[i].encode();
        }
        return new ASN1Set(filterType, elements);
      case NOT_FILTER_TYPE:
        ASN1Element subElement = subFilters[0].encode();
        return new ASN1OctetString(filterType, subElement.encode());
      case EQUALITY_FILTER_TYPE:
      case GREATER_OR_EQUAL_FILTER_TYPE:
      case LESS_OR_EQUAL_FILTER_TYPE:
      case APPROXIMATE_FILTER_TYPE:
        elements = new ASN1Element[]
        {
            new ASN1OctetString(attributeType),
            new ASN1OctetString(assertionValue)
        };
        return new ASN1Sequence(filterType, elements);
      case SUBSTRING_FILTER_TYPE:
        ArrayList<ASN1Element> valueElementList = new ArrayList<ASN1Element>();
        if ((subInitialValue != null) && (subInitialValue.length() > 0))
        {
          valueElementList.add(new ASN1OctetString(SUBINITIAL_TYPE,
                                                   subInitialValue));
        }
        for (int i=0; ((subAnyValues != null) && (i < subAnyValues.length));
             i++)
        {
          valueElementList.add(new ASN1OctetString(SUBANY_TYPE,
                                                   subAnyValues[i]));
        }
        if ((subFinalValue != null) && (subFinalValue.length() > 0))
        {
          valueElementList.add(new ASN1OctetString(SUBFINAL_TYPE,
                                                   subFinalValue));
        }

        ASN1Element[] valueElements = new ASN1Element[valueElementList.size()];
        valueElementList.toArray(valueElements);

        elements = new ASN1Element[]
        {
          new ASN1OctetString(attributeType),
          new ASN1Sequence(valueElements)
        };

        return new ASN1Sequence(filterType, elements);
      case PRESENCE_FILTER_TYPE:
        return new ASN1OctetString(filterType, attributeType);
      case EXTENSIBLE_MATCH_FILTER_TYPE:
        ArrayList<ASN1Element> elementList = new ArrayList<ASN1Element>(4);
        if ((matchingRuleID != null) && (matchingRuleID.length() > 0))
        {
          elementList.add(new ASN1OctetString(EXTENSIBLE_MATCH_RULE_ID_TYPE,
                                              matchingRuleID));
        }
        if ((attributeType != null) && (attributeType.length() > 0))
        {
          elementList.add(new ASN1OctetString(EXTENSIBLE_MATCH_ATTRIBUTE_TYPE,
                                              attributeType));
        }
        elementList.add(new ASN1OctetString(EXTENSIBLE_MATCH_VALUE_TYPE,
                                            assertionValue));
        if (includeDNAttributes)
        {
          elementList.add(new ASN1Boolean(EXTENSIBLE_MATCH_INCLUDE_DN_TYPE,
                                          includeDNAttributes));
        }

        elements = new ASN1Element[elementList.size()];
        elementList.toArray(elements);
        return new ASN1Sequence(filterType, elements);
      default:
        return null;
    }
  }



  /**
   * Decodes the provided ASN.1 element as an LDAP search filter.
   *
   * @param  element  The ASN.1 element to be decoded as a search filter.
   *
   * @return  The decoded search filter.
   *
   * @throws  ProtocolException  If a problem occurs while decoding the search
   *                             filter.
   */
  public static SearchFilter decode(ASN1Element element)
         throws ProtocolException
  {
    switch (element.getType())
    {
      case AND_FILTER_TYPE:
        ASN1Element[] elements;
        try
        {
          elements = element.decodeAsSet().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode element as a set of " +
                                      "search filters", e);
        }

        SearchFilter[] subFilters = new SearchFilter[elements.length];
        for (int i=0; i < elements.length; i++)
        {
          try
          {
            subFilters[i] = decode(elements[i]);
          }
          catch (Exception e)
          {
            throw new ProtocolException("Unable to decode AND subfilter", e);
          }
        }

        return createANDFilter(subFilters);
      case OR_FILTER_TYPE:
        try
        {
          elements = element.decodeAsSet().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode element as a set of " +
                                      "search filters", e);
        }

        subFilters = new SearchFilter[elements.length];
        for (int i=0; i < elements.length; i++)
        {
          try
          {
            subFilters[i] = decode(elements[i]);
          }
          catch (Exception e)
          {
            throw new ProtocolException("Unable to decode OR subfilter", e);
          }
        }

        return createORFilter(subFilters);
      case NOT_FILTER_TYPE:
        try
        {
          ASN1Element filterElement = ASN1Element.decode(element.getValue());
          return createNOTFilter(decode(filterElement));
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode NOT filter element", e);
        }
      case EQUALITY_FILTER_TYPE:
        try
        {
          elements = element.decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode equality filter " +
                                      "sequence", e);
        }

        if (elements.length != 2)
        {
          throw new ProtocolException("There must be exactly two elements in " +
                                      "an equality filter sequence");
        }

        String attributeType;
        try
        {
          attributeType = elements[0].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode equality filter " +
                                      "attribute type", e);
        }

        String assertionValue;
        try
        {
          assertionValue = elements[1].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode equality filter " +
                                      "assertion value", e);
        }

        return createEqualityFilter(attributeType, assertionValue);
      case SUBSTRING_FILTER_TYPE:
        try
        {
          elements = element.decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode substring filter " +
                                      "sequence", e);
        }

        if (elements.length != 2)
        {
          throw new ProtocolException("There must be exactly two elements in " +
                                      "a substring filter sequence");
        }

        try
        {
          attributeType = elements[0].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode attribute type for " +
                                      "substring search filter", e);
        }

        ASN1Element[] subValueElements;
        try
        {
          subValueElements = elements[1].decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode substring value " +
                                      "elements", e);
        }

        ArrayList<String> subAnyList = new ArrayList<String>();
        String    subInitialValue = null;
        String    subFinalValue   = null;
        String[]  subAnyValues    = null;

        for (int i=0; i < subValueElements.length; i++)
        {
          switch (subValueElements[i].getType())
          {
            case SUBINITIAL_TYPE:
              try
              {
                subInitialValue =
                     subValueElements[i].decodeAsOctetString().getStringValue();
              }
              catch (Exception e)
              {
                throw new ProtocolException("Unable to decode subInitial value",
                                            e);
              }
              break;
            case SUBANY_TYPE:
              try
              {
                String subAnyValue =
                     subValueElements[i].decodeAsOctetString().getStringValue();
                subAnyList.add(subAnyValue);
              }
              catch (Exception e)
              {
                throw new ProtocolException("Unable to decode subAny value", e);
              }
              break;
            case SUBFINAL_TYPE:
              try
              {
                subFinalValue =
                     subValueElements[i].decodeAsOctetString().getStringValue();
              }
              catch (Exception e)
              {
                throw new ProtocolException("Unable to decode subFinal value",
                                            e);
              }
              break;
            default:
              throw new ProtocolException("Unrecognized substring filter " +
                                          "type " +
                                          subValueElements[i].getType());
          }
        }

        if (! subAnyList.isEmpty())
        {
          subAnyValues = new String[subAnyList.size()];
          subAnyList.toArray(subAnyValues);
        }

        return createSubstringFilter(attributeType, subInitialValue,
                                     subAnyValues, subFinalValue);
      case GREATER_OR_EQUAL_FILTER_TYPE:
        try
        {
          elements = element.decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode greater or equal " +
                                      "filter sequence", e);
        }

        if (elements.length != 2)
        {
          throw new ProtocolException("There must be exactly two elements in " +
                                      "a greater or equal filter sequence");
        }

        try
        {
          attributeType = elements[0].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode greater or equal " +
                                      "filter attribute type", e);
        }

        try
        {
          assertionValue = elements[1].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode greater or equal " +
                                      "filter assertion value", e);
        }

        return createGreaterOrEqualFilter(attributeType, assertionValue);
      case LESS_OR_EQUAL_FILTER_TYPE:
        try
        {
          elements = element.decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode less or equal " +
                                      "filter sequence", e);
        }

        if (elements.length != 2)
        {
          throw new ProtocolException("There must be exactly two elements in " +
                                      "a less or equal filter sequence");
        }

        try
        {
          attributeType = elements[0].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode less or equal " +
                                      "filter attribute type", e);
        }

        try
        {
          assertionValue = elements[1].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode less or equal " +
                                      "filter assertion value", e);
        }

        return createLessOrEqualFilter(attributeType, assertionValue);
      case PRESENCE_FILTER_TYPE:
        try
        {
          attributeType = element.decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode presence filter " +
                                      "attribute type", e);
        }

        return createPresenceFilter(attributeType);
      case APPROXIMATE_FILTER_TYPE:
        try
        {
          elements = element.decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode approximate " +
                                      "filter sequence", e);
        }

        if (elements.length != 2)
        {
          throw new ProtocolException("There must be exactly two elements in " +
                                      "an approximate filter sequence");
        }

        try
        {
          attributeType = elements[0].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode approximate " +
                                      "filter attribute type", e);
        }

        try
        {
          assertionValue = elements[1].decodeAsOctetString().getStringValue();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode approximate " +
                                      "filter assertion value", e);
        }

        return createApproximateFilter(attributeType, assertionValue);
      case EXTENSIBLE_MATCH_FILTER_TYPE:
        try
        {
          elements = element.decodeAsSequence().getElements();
        }
        catch (Exception e)
        {
          throw new ProtocolException("Unable to decode extensible matching " +
                                      "filter sequence", e);
        }

        if ((elements.length < 1) || (elements.length > 4))
        {
          throw new ProtocolException("There must be between 1 and 4 " +
                                      "elements in an extensible matching " +
                                      "filter sequence");
        }

        attributeType  = null;
        assertionValue = null;
        String  matchingRuleID      = null;
        boolean includeDNAttributes = false;
        for (int i=0; i < elements.length; i++)
        {
          switch (elements[i].getType())
          {
            case EXTENSIBLE_MATCH_RULE_ID_TYPE:
              try
              {
                matchingRuleID =
                     elements[i].decodeAsOctetString().getStringValue();
              }
              catch (Exception e)
              {
                throw new ProtocolException("Unable to decode matching rule " +
                                            "ID for extensible matching filter",
                                            e);
              }
              break;
            case EXTENSIBLE_MATCH_ATTRIBUTE_TYPE:
              try
              {
                attributeType =
                     elements[i].decodeAsOctetString().getStringValue();
              }
              catch (Exception e)
              {
                throw new ProtocolException("Unable to decode attribute type " +
                                            "for extensible matching filter",
                                            e);
              }
              break;
            case EXTENSIBLE_MATCH_VALUE_TYPE:
              try
              {
                assertionValue =
                     elements[i].decodeAsOctetString().getStringValue();
              }
              catch (Exception e)
              {
                throw new ProtocolException("Unable to decode assertion value" +
                                            "for extensible matching filter",
                                            e);
              }
              break;
            case EXTENSIBLE_MATCH_INCLUDE_DN_TYPE:
              try
              {
                includeDNAttributes =
                     elements[i].decodeAsBoolean().getBooleanValue();
              }
              catch (Exception e)
              {
                throw new ProtocolException("Unable to decode dnAttributes " +
                                            "for extensible matching filter",
                                            e);
              }
              break;
            default:
              throw new ProtocolException("Unrecognized extensible matching " +
                                          "filter element type " +
                                          elements[i].getType());
          }
        }

        return createExtensibleFilter(matchingRuleID, attributeType,
                                      assertionValue, includeDNAttributes);
      default:
        throw new ProtocolException("Invalid search filter element type " +
                                    element.getType());
    }
  }



  /**
   * Retrieves a string representation of this search filter in the format
   * specified by RFC 2254.
   *
   * @return  A string representation of this search filter in the format
   *          specified by RFC 2254.
   */
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    toStringBuilder(buffer);
    return buffer.toString();
  }



  /**
   * Writes an RFC 2254 representation of this search filter to the provided
   * string buffer.
   *
   * @param  buffer  The string buffer to which the filter is to be written.
   */
  public void toStringBuilder(StringBuilder buffer)
  {
    switch (filterType)
    {
      case AND_FILTER_TYPE:
        buffer.append("(&");
        for (int i=0; i < subFilters.length; i++)
        {
          subFilters[i].toStringBuilder(buffer);
        }
        buffer.append(')');
        break;
      case OR_FILTER_TYPE:
        buffer.append("(|");
        for (int i=0; i < subFilters.length; i++)
        {
          subFilters[i].toStringBuilder(buffer);
        }
        buffer.append(')');
        break;
      case NOT_FILTER_TYPE:
        buffer.append("(!");
        subFilters[0].toStringBuilder(buffer);
        buffer.append(')');
        break;
      case EQUALITY_FILTER_TYPE:
        buffer.append('(');
        buffer.append(attributeType);
        buffer.append('=');
        buffer.append(assertionValue);
        buffer.append(')');
        break;
      case SUBSTRING_FILTER_TYPE:
        buffer.append('(');
        buffer.append(attributeType);
        buffer.append('=');
        if ((subInitialValue != null) && (subInitialValue.length() > 0))
        {
          buffer.append(subInitialValue);
        }
        for (int i=0; ((subAnyValues != null) && (i < subAnyValues.length));
             i++)
        {
          buffer.append('*');
          buffer.append(subAnyValues[i]);
        }
        buffer.append('*');
        if ((subFinalValue != null) && (subFinalValue.length() > 0))
        {
          buffer.append(subFinalValue);
        }
        buffer.append(')');
        break;
      case GREATER_OR_EQUAL_FILTER_TYPE:
        buffer.append('(');
        buffer.append(attributeType);
        buffer.append(">=");
        buffer.append(assertionValue);
        buffer.append(')');
        break;
      case LESS_OR_EQUAL_FILTER_TYPE:
        buffer.append('(');
        buffer.append(attributeType);
        buffer.append("<=");
        buffer.append(assertionValue);
        buffer.append(')');
        break;
      case PRESENCE_FILTER_TYPE:
        buffer.append('(');
        buffer.append(attributeType);
        buffer.append("=*)");
        break;
      case APPROXIMATE_FILTER_TYPE:
        buffer.append('(');
        buffer.append(attributeType);
        buffer.append("~=");
        buffer.append(assertionValue);
        buffer.append(')');
        break;
      case EXTENSIBLE_MATCH_FILTER_TYPE:
        buffer.append('(');
        if ((attributeType != null) && (attributeType.length() > 0))
        {
          buffer.append(attributeType);
        }
        if (includeDNAttributes)
        {
          buffer.append(":dn");
        }
        if ((matchingRuleID != null) && (matchingRuleID.length() > 0))
        {
          buffer.append(':');
          buffer.append(matchingRuleID);
        }
        buffer.append(":=");
        buffer.append(assertionValue);
        buffer.append(')');
    }
  }
}

