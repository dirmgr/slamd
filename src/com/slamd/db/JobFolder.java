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
package com.slamd.db;



import java.util.Arrays;

import com.slamd.asn1.ASN1Boolean;
import com.slamd.asn1.ASN1Element;
import com.slamd.asn1.ASN1OctetString;
import com.slamd.asn1.ASN1Sequence;



/**
 * This class defines a structure for storing information about a job folder.
 * This includes the name of the folder, the jobs and optimizing jobs that it
 * contains, the parent and child folders, and the permissions associated with
 * it.
 *
 *
 * @author  Neil A. Wilson
 */
public class JobFolder
{
  /**
   * The name of the encoded element that holds the name of this job folder.
   */
  public static final String ELEMENT_NAME = "name";



  /**
   * The name of the encoded element that indicates whether this folder should
   * be visible in restricted read-only mode.
   */
  public static final String ELEMENT_DISPLAY_IN_READ_ONLY =
       "display_in_read_only";



  /**
   * The name of the encoded element that indicates whether this is a virtual
   * folder.
   */
  public static final String ELEMENT_IS_VIRTUAL = "is_virtual";



  /**
   * The name of the encoded element that holds the name of the parent folder.
   */
  public static final String ELEMENT_PARENT = "parent";



  /**
   * The name of the encoded element that holds the names of the child folders.
   */
  public static final String ELEMENT_CHILDREN = "children";



  /**
   * The name of the encoded element that holds the description for this folder.
   */
  public static final String ELEMENT_DESCRIPTION = "description";



  /**
   * The name of the encoded element that holds the job IDs.
   */
  public static final String ELEMENT_JOB_IDS = "jobs";



  /**
   * The name of the encoded element that holds the optimizing job IDs.
   */
  public static final String ELEMENT_OPTIMIZING_JOB_IDS = "optimizing_jobs";



  /**
   * The name of the encoded element that holds the names of the uploaded files.
   */
  public static final String ELEMENT_FILE_NAMES = "files";



  /**
   * The name of the encoded element that holds the permissions.
   */
  public static final String ELEMENT_PERMISSIONS = "permissions";



  // Indicates whether this folder should be displayed in restricted read-only
  // mode.
  boolean displayInReadOnly;

  // Indicates whether this is a virtual job folder.
  boolean isVirtual;

  // The set of permissions associated with this job folder.
  SLAMDPermission[] permissions;

  // The description for this job folder.
  String description;

  // The name of this job folder.
  String folderName;

  // The name of the owner of this job folder.
  String ownerName;

  // The name of the parent folder.
  String parentName;

  // The names of all the child folders below this folder.
  String[] childNames;

  // The names of the uploaded files associated with this folder.
  String[] fileNames;

  // The job IDs of the jobs stored in this folder.
  String[] jobIDs;

  // The optimizing job IDs of the optimizing jobs stored in this folder.
  String[] optimizingJobIDs;



  /**
   * Creates a new job folder with the provided information.
   *
   * @param  folderName         The name of this job folder.
   * @param  displayInReadOnly  Indicates whether the contents of the folder
   *                            should be displayed in restricted read-only
   *                            mode.
   * @param  isVirtual          Indicates whether this is a virtual job folder.
   * @param  parentName         The name of the folder that is the parent of
   *                            this job folder.
   * @param  childNames         The names of the folders that are children of
   *                            this job folder.
   * @param  description        The description for this job folder.
   * @param  jobIDs             The job IDs of the jobs stored in this folder.
   * @param  optimizingJobIDs   The job IDs of the optimizing jobs stored in
   *                            this folder.
   * @param  fileNames          The names of the uploaded files associated with
   *                            this folder.
   * @param  permissions        The set of permissions that have been granted
   *                            for this folder.
   */
  public JobFolder(String folderName, boolean displayInReadOnly,
                   boolean isVirtual, String parentName, String[] childNames,
                   String description, String[] jobIDs,
                   String[] optimizingJobIDs, String[] fileNames,
                   SLAMDPermission[] permissions)
  {
    this.folderName        = folderName;
    this.displayInReadOnly = displayInReadOnly;
    this.isVirtual         = isVirtual;
    this.parentName        = parentName;
    this.description       = description;

    if (childNames == null)
    {
      this.childNames = new String[0];
    }
    else
    {
      this.childNames = childNames;
    }

    if (jobIDs == null)
    {
      this.jobIDs = new String[0];
    }
    else
    {
      this.jobIDs = jobIDs;
    }

    if (optimizingJobIDs == null)
    {
      this.optimizingJobIDs = new String[0];
    }
    else
    {
      this.optimizingJobIDs = optimizingJobIDs;
    }

    if (fileNames == null)
    {
      this.fileNames = new String[0];
    }
    else
    {
      this.fileNames = fileNames;
    }

    if (permissions == null)
    {
      this.permissions = new SLAMDPermission[0];
    }
    else
    {
      this.permissions = permissions;
    }
  }



  /**
   * Retrieves the name of this job folder.
   *
   * @return  The name of this job folder.
   */
  public String getFolderName()
  {
    return folderName;
  }



  /**
   * Indicates whether this folder should be visible in restricted read-only
   * mode.
   *
   * @return  <CODE>true</CODE> if this folder should be visible in restricted
   *          read-only mode, or <CODE>false</CODE> if not.
   */
  public boolean displayInReadOnlyMode()
  {
    return displayInReadOnly;
  }



  /**
   * Specifies whether this folder should be visible in restricted read-only
   * mode.
   *
   * @param  displayInReadOnly  Indicates whether this folder should be visible
   *                            in restricted read-only mode.
   */
  public void setDisplayInReadOnlyMode(boolean displayInReadOnly)
  {
    this.displayInReadOnly = displayInReadOnly;
  }



  /**
   * Indicates whether this is a virtual job folder.
   *
   * @return  <CODE>true</CODE> if this is a virtual job folder, or
   *          <CODE>false</CODE> if it is a real job folder.
   */
  public boolean isVirtual()
  {
    return isVirtual;
  }



  /**
   * Retrieves the name of the job folder that is the parent of this job folder.
   *
   * @return  The name of the parent job folder, or <CODE>null</CODE> if it is a
   *          top-level folder.
   */
  public String getParentName()
  {
    return parentName;
  }



  /**
   * Retrieves the names of the job folders that are children of this folder.
   *
   * @return  The names of the job folders that are children of this folder.
   */
  public String[] getChildNames()
  {
    return childNames;
  }



  /**
   * Indicates whether this job folder contains a child folder with the
   * specified name.
   *
   * @param  childName  The name of the child folder for which to make the
   *                    determination.
   *
   * @return  <CODE>true</CODE> if this job folder contains a child folder with
   *          the specified name, or <CODE>false</CODE> if not.
   */
  public boolean containsChildName(String childName)
  {
    if (childNames == null)
    {
      return false;
    }

    for (int i=0; i < childNames.length; i++)
    {
      if (childNames[i].equals(childName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Specifies the names of the job folders that are children of this folder.
   *
   * @param  childNames  The names of the job folders that are children of this
   *                     folder.
   */
  public void setChildNames(String[] childNames)
  {
    if (childNames == null)
    {
      childNames = new String[0];
    }

    Arrays.sort(childNames);
    this.childNames = childNames;
  }



  /**
   * Adds the specified folder as a child of this folder.
   *
   * @param  childName  The name of the job folder to add as a child of this
   *                    folder.
   */
  public void addChildName(String childName)
  {
    String[] newChildNames = new String[childNames.length+1];
    for (int i=0; i < childNames.length; i++)
    {
      if (childNames[i].equals(childName))
      {
        return;
      }
      else
      {
        newChildNames[i] = childNames[i];
      }
    }

    newChildNames[childNames.length] = childName;
    Arrays.sort(newChildNames);
    childNames = newChildNames;
  }



  /**
   * Removes the specified folder as a child of this folder.
   *
   * @param  childName  The name of the job folder to remove from the set of
   *                    children for this folder.
   */
  public void removeChildName(String childName)
  {
    int pos = -1;
    for (int i=0; i < childNames.length; i++)
    {
      if (childNames[i].equals(childName))
      {
        pos = i;
        break;
      }
    }

    if (pos == -1)
    {
      return;
    }

    String[] newChildNames = new String[childNames.length-1];
    System.arraycopy(childNames, 0, newChildNames, 0, pos);
    System.arraycopy(childNames, pos+1, newChildNames, pos,
                     (newChildNames.length - pos));
    childNames = newChildNames;
  }



  /**
   * Retrieves the description for this job folder.
   *
   * @return  The description for this job folder.
   */
  public String getDescription()
  {
    return description;
  }



  /**
   * Specifies the description for this job folder.
   *
   * @param  description  The description for this job folder.
   */
  public void setDescription(String description)
  {
    this.description = description;
  }



  /**
   * Retrieves the job IDs of all jobs associated with this folder.
   *
   * @return  The job IDs of all jobs associated with this folder.
   */
  public String[] getJobIDs()
  {
    return jobIDs;
  }



  /**
   * Indicates whether this job folder contains a job with the specified ID.
   *
   * @param  jobID  The job ID for which to make the determination.
   *
   * @return  <CODE>true</CODE> if this job folder contains a job with the
   *          specified job ID, or <CODE>false</CODE> if not.
   */
  public boolean containsJobID(String jobID)
  {
    if (jobIDs == null)
    {
      return false;
    }

    for (int i=0; i < jobIDs.length; i++)
    {
      if (jobIDs[i].equals(jobID))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Specifies the job IDs of all jobs associated with this folder.
   *
   * @param  jobIDs  The job IDs of all jobs associated with this folder.
   */
  public void setJobIDs(String[] jobIDs)
  {
    if (jobIDs == null)
    {
      jobIDs = new String[0];
    }

    Arrays.sort(jobIDs);
    this.jobIDs = jobIDs;
  }



  /**
   * Adds the provided job ID to the set of jobs associated with this folder.
   *
   * @param  jobID  The job ID to add to the set of jobs associated with this
   *                folder.
   */
  public void addJobID(String jobID)
  {
    String[] newIDs = new String[jobIDs.length+1];
    for (int i=0; i < jobIDs.length; i++)
    {
      if (jobIDs[i].equals(jobID))
      {
        return;
      }
      else
      {
        newIDs[i] = jobIDs[i];
      }
    }

    newIDs[jobIDs.length] = jobID;
    Arrays.sort(newIDs);
    jobIDs = newIDs;
  }



  /**
   * Removes the provided job ID from the set of jobs associated with this
   * folder.
   *
   * @param  jobID  The job ID to remove from the set of jobs associated with
   *                this folder.
   */
  public void removeJobID(String jobID)
  {
    int pos = -1;
    for (int i=0; i < jobIDs.length; i++)
    {
      if (jobIDs[i].equals(jobID))
      {
        pos = i;
        break;
      }
    }

    if (pos == -1)
    {
      return;
    }

    String[] newJobIDs = new String[jobIDs.length-1];
    System.arraycopy(jobIDs, 0, newJobIDs, 0, pos);
    System.arraycopy(jobIDs, pos+1, newJobIDs, pos,
                     (newJobIDs.length - pos));
    jobIDs = newJobIDs;
  }



  /**
   * Retrieves the optimizing job IDs of all optimizing jobs associated with
   * this folder.
   *
   * @return  The optimizing job IDs of all optimizing jobs associated with this
   *          folder.
   */
  public String[] getOptimizingJobIDs()
  {
    return optimizingJobIDs;
  }



  /**
   * Indicates whether this job folder contains an optimizing job with the
   * specified ID.
   *
   * @param  optimizingJobID  The optimizing job ID for which to make the
   *                          determination.
   *
   * @return  <CODE>true</CODE> if this job folder contains an optimizing job
   *          with the specified ID, or <CODE>false</CODE> if not.
   */
  public boolean containsOptimizingJobID(String optimizingJobID)
  {
    if (optimizingJobIDs == null)
    {
      return false;
    }

    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      if (optimizingJobIDs[i].equals(optimizingJobID))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Specifies the optimizing job IDs of all optimizing jobs associated with
   * this folder.
   *
   * @param  optimizingJobIDs  The optimizing job IDs of all optimizing jobs
   *                           associated with this folder.
   */
  public void setOptimzizingJobIDs(String[] optimizingJobIDs)
  {
    if (optimizingJobIDs == null)
    {
      optimizingJobIDs = new String[0];
    }

    Arrays.sort(optimizingJobIDs);
    this.optimizingJobIDs = optimizingJobIDs;
  }



  /**
   * Adds the provided optimizing job ID to the set of optimizing jobs
   * associated with this folder.
   *
   * @param  optimizingJobID  The optimizing job ID to add to the set of
   *                          optimizing jobs associated with this folder.
   */
  public void addOptimizingJobID(String optimizingJobID)
  {
    String[] newIDs = new String[optimizingJobIDs.length+1];
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      if (optimizingJobIDs[i].equals(optimizingJobID))
      {
        return;
      }
      else
      {
        newIDs[i] = optimizingJobIDs[i];
      }
    }

    newIDs[optimizingJobIDs.length] = optimizingJobID;
    Arrays.sort(newIDs);
    optimizingJobIDs = newIDs;
  }



  /**
   * Removes the provided optimizing job ID from the set of optimizing jobs
   * associated with this folder.
   *
   * @param  optimizingJobID  The optimizing job ID to remove from the set of
   *                          optimizing jobs associated with this folder.
   */
  public void removeOptimizingJobID(String optimizingJobID)
  {
    int pos = -1;
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      if (optimizingJobIDs[i].equals(optimizingJobID))
      {
        pos = i;
        break;
      }
    }

    if (pos == -1)
    {
      return;
    }

    String[] newOptimizingIDs = new String[optimizingJobIDs.length-1];
    System.arraycopy(optimizingJobIDs, 0, newOptimizingIDs, 0, pos);
    System.arraycopy(optimizingJobIDs, pos+1, newOptimizingIDs, pos,
                     (newOptimizingIDs.length - pos));
    optimizingJobIDs = newOptimizingIDs;
  }



  /**
   * Retrieves the names of the uploaded files associated with this job folder.
   *
   * @return  The names of the uploaded files associated with this job folder.
   */
  public String[] getFileNames()
  {
    return fileNames;
  }



  /**
   * Indicates whether this job folder contains an uploaded file with the
   * specified name.
   *
   * @param  fileName  The file name for which to make the determination.
   *
   * @return  <CODE>true</CODE> if this job folder contains an uploaded file
   *          with the specified name, or <CODE>false</CODE> if it does not.
   */
  public boolean containsFileName(String fileName)
  {
    if (fileNames == null)
    {
      return false;
    }

    for (int i=0; i < fileNames.length; i++)
    {
      if (fileNames[i].equals(fileName))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Specifies the names of the uploaded files associated with this job folder.
   *
   * @param  fileNames  The names of the uploaded files associated with this job
   *                    folder.
   */
  public void setFileNames(String[] fileNames)
  {
    if (fileNames == null)
    {
      fileNames = new String[0];
    }

    this.fileNames = fileNames;
  }



  /**
   * Adds the specified file name to the set of uploaded files for this job
   * folder.
   *
   * @param  fileName  The name of the file to add to the uploaded files for
   *                   this job folder.
   */
  public void addFileName(String fileName)
  {
    String[] newFiles = new String[fileNames.length+1];
    for (int i=0; i < fileNames.length; i++)
    {
      if (fileNames[i].equals(fileName))
      {
        return;
      }
      else
      {
        newFiles[i] = fileNames[i];
      }
    }

    newFiles[fileNames.length] = fileName;
    Arrays.sort(newFiles);
    fileNames = newFiles;
  }



  /**
   * Removes the specified file name from the set of uploaded files for this job
   * folder.
   *
   * @param  fileName  The name of the file to remove from the set of uploaded
   *                   files for this job folder.
   */
  public void removeFileName(String fileName)
  {
    int pos = -1;
    for (int i=0; i < fileNames.length; i++)
    {
      if (fileNames[i].equals(fileName))
      {
        pos = i;
        break;
      }
    }

    if (pos == -1)
    {
      return;
    }

    String[] newFileNames = new String[fileNames.length-1];
    System.arraycopy(fileNames, 0, newFileNames, 0, pos);
    System.arraycopy(fileNames, pos+1, newFileNames, pos,
                     (newFileNames.length - pos));
    fileNames = newFileNames;
  }



  /**
   * Retrieves the set of permissions associated with this job folder.
   *
   * @return  The set of permissions associated with this job folder.
   */
  public SLAMDPermission[] getPermissions()
  {
    return permissions;
  }



  /**
   * Determines whether the provided user has the specified permission for this
   * job folder.
   *
   * @param  user            The user for which to make the determination.
   * @param  permissionName  The name of the permission to check for the
   *                         provided user.
   *
   * @return  <CODE>true</CODE> if the user has the specified permission or
   *          <CODE>false</CODE> if not.
   */
  public boolean userHasPermission(SLAMDUser user, String permissionName)
  {
    for (int i=0; i < permissions.length; i++)
    {
      if (permissions[i].getName().equals(permissionName))
      {
        return permissions[i].appliesToUser(user);
      }
    }

    return false;
  }



  /**
   * Specifies the set of permissions to use for this folder.
   *
   * @param  permissions  The set of permissions to use for this folder.
   */
  public void setPermissions(SLAMDPermission[] permissions)
  {
    if (permissions == null)
    {
      permissions = new SLAMDPermission[0];
    }

    this.permissions = permissions;
  }



  /**
   * Sets the provided permission for this group.  If a permission already
   * exists with the provided name, then it will be replaced, otherwise it will
   * be added.
   *
   * @param  permission  The permission to set for this folder.
   */
  public void setPermission(SLAMDPermission permission)
  {
    for (int i=0; i < permissions.length; i++)
    {
      if (permissions[i].getName().equals(permission.getName()))
      {
        permissions[i] = permission;
        return;
      }
    }

    SLAMDPermission[] newPermissions =
         new SLAMDPermission[permissions.length+1];
    System.arraycopy(permissions, 0, newPermissions, 0, permissions.length);
    newPermissions[permissions.length] = permission;
    permissions = newPermissions;
  }



  /**
   * Removes the permission with the specified name from this job folder.
   *
   * @param  permissionName  The name of the permission to remove from this
   *                         folder.
   */
  public void removePermission(String permissionName)
  {
    int pos = -1;
    for (int i=0; i < permissions.length; i++)
    {
      if (permissions[i].getName().equals(permissionName))
      {
        pos = i;
        break;
      }
    }

    if (pos == -1)
    {
      return;
    }

    SLAMDPermission[] newPermissions =
         new SLAMDPermission[permissions.length-1];
    System.arraycopy(permissions, 0, newPermissions, 0, pos);
    System.arraycopy(permissions, pos+1, newPermissions, pos,
                     (newPermissions.length - pos));
    permissions = newPermissions;
  }



  /**
   * Encodes the information in this job folder to a byte array.
   *
   * @return  The byte array containing the encoded job folder.
   */
  public byte[] encode()
  {
    ASN1Element[] childElements = new ASN1Element[childNames.length];
    for (int i=0; i < childNames.length; i++)
    {
      childElements[i] = new ASN1OctetString(childNames[i]);
    }

    ASN1Element[] jobElements = new ASN1Element[jobIDs.length];
    for (int i=0; i < jobIDs.length; i++)
    {
      jobElements[i] = new ASN1OctetString(jobIDs[i]);
    }

    ASN1Element[] optimizingElements = new ASN1Element[optimizingJobIDs.length];
    for (int i=0; i < optimizingJobIDs.length; i++)
    {
      optimizingElements[i] = new ASN1OctetString(optimizingJobIDs[i]);
    }

    ASN1Element[] fileElements = new ASN1Element[fileNames.length];
    for (int i=0; i < fileNames.length; i++)
    {
      fileElements[i] = new ASN1OctetString(fileNames[i]);
    }

    ASN1Element[] permissionElements = new ASN1Element[permissions.length];
    for (int i=0; i < permissions.length; i++)
    {
      permissionElements[i] = permissions[i].encodeAsSequence();
    }

    ASN1Element[] folderElements = new ASN1Element[]
    {
      new ASN1OctetString(ELEMENT_NAME),
      new ASN1OctetString(folderName),
      new ASN1OctetString(ELEMENT_DISPLAY_IN_READ_ONLY),
      new ASN1Boolean(displayInReadOnly),
      new ASN1OctetString(ELEMENT_IS_VIRTUAL),
      new ASN1Boolean(isVirtual),
      new ASN1OctetString(ELEMENT_PARENT),
      new ASN1OctetString(parentName),
      new ASN1OctetString(ELEMENT_CHILDREN),
      new ASN1Sequence(childElements),
      new ASN1OctetString(ELEMENT_DESCRIPTION),
      new ASN1OctetString(description),
      new ASN1OctetString(ELEMENT_JOB_IDS),
      new ASN1Sequence(jobElements),
      new ASN1OctetString(ELEMENT_OPTIMIZING_JOB_IDS),
      new ASN1Sequence(optimizingElements),
      new ASN1OctetString(ELEMENT_FILE_NAMES),
      new ASN1Sequence(fileElements),
      new ASN1OctetString(ELEMENT_PERMISSIONS),
      new ASN1Sequence(permissionElements)
    };

    return new ASN1Sequence(folderElements).encode();
  }



  /**
   * Decodes the provided byte array as a job folder.
   *
   * @param  encodedFolder  The byte array containing the encoded folder data.
   *
   * @return  The decoded job folder.
   *
   * @throws  DecodeException  If a problem occurs while attempting to decode
   *                           the job folder.
   */
  public static JobFolder decode(byte[] encodedFolder)
         throws DecodeException
  {
    try
    {
      boolean           displayInReadOnly = false;
      boolean           isVirtual         = false;
      SLAMDPermission[] permissions       = new SLAMDPermission[0];
      String            description       = null;
      String            folderName        = null;
      String            parentName        = null;
      String[]          childNames        = new String[0];
      String[]          fileNames         = new String[0];
      String[]          jobIDs            = new String[0];
      String[]          optimizingJobIDs  = new String[0];

      ASN1Element   element  = ASN1Element.decode(encodedFolder);
      ASN1Element[] elements = element.decodeAsSequence().getElements();
      for (int i=0; i < elements.length; i += 2)
      {
        String elementName = elements[i].decodeAsOctetString().getStringValue();
        if (elementName.equals(ELEMENT_NAME))
        {
          folderName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_DISPLAY_IN_READ_ONLY))
        {
          displayInReadOnly = elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_IS_VIRTUAL))
        {
          isVirtual = elements[i+1].decodeAsBoolean().getBooleanValue();
        }
        else if (elementName.equals(ELEMENT_PARENT))
        {
          parentName = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_CHILDREN))
        {
          ASN1Element[] childElements =
               elements[i+1].decodeAsSequence().getElements();
          childNames = new String[childElements.length];
          for (int j=0; j < childNames.length; j++)
          {
            childNames[j] =
                 childElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_DESCRIPTION))
        {
          description = elements[i+1].decodeAsOctetString().getStringValue();
        }
        else if (elementName.equals(ELEMENT_JOB_IDS))
        {
          ASN1Element[] jobElements =
               elements[i+1].decodeAsSequence().getElements();
          jobIDs = new String[jobElements.length];
          for (int j=0; j < jobIDs.length; j++)
          {
            jobIDs[j] = jobElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_OPTIMIZING_JOB_IDS))
        {
          ASN1Element[] optimizingElements =
               elements[i+1].decodeAsSequence().getElements();
          optimizingJobIDs = new String[optimizingElements.length];
          for (int j=0; j < optimizingJobIDs.length; j++)
          {
            optimizingJobIDs[j] =
                 optimizingElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_FILE_NAMES))
        {
          ASN1Element[] fileElements =
               elements[i+1].decodeAsSequence().getElements();
          fileNames = new String[fileElements.length];
          for (int j=0; j < fileNames.length; j++)
          {
            fileNames[j] =
                 fileElements[j].decodeAsOctetString().getStringValue();
          }
        }
        else if (elementName.equals(ELEMENT_PERMISSIONS))
        {
          ASN1Element[] permissionElements =
               elements[i+1].decodeAsSequence().getElements();
          permissions = new SLAMDPermission[permissionElements.length];
          for (int j=0; j < permissions.length; j++)
          {
            permissions[j] =
                 SLAMDPermission.decodeSequence(permissionElements[j].
                                                     decodeAsSequence());
          }
        }
      }

      return new JobFolder(folderName, displayInReadOnly, isVirtual, parentName,
                           childNames, description, jobIDs, optimizingJobIDs,
                           fileNames, permissions);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw new DecodeException("Unable to decode job folder:  " + e, e);
    }
  }
}

