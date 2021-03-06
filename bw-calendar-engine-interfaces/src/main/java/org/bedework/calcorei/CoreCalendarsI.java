/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.calcorei;

import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwPrincipal;
import org.bedework.calfacade.CollectionSynchInfo;
import org.bedework.calfacade.exc.CalFacadeException;

import org.bedework.access.Ace;
import org.bedework.access.AceWho;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** This is the calendars section of the low level interface to the calendar
 * database. Calendars is something of a misnomer - we're really talking
 * collections here. The type property defines which type of collection we are
 * dealing with. The CalDAV spec defines what is allowable, e.g. no collections
 * inside a calendar collection.
 *
 * <p>To allow us to enforce access checks we wrap the object inside a wrapper
 * class which blocks access to the getChildren method. To retrieve the children
 * of a calendar object call the getCalendars(BwCalendar) method. The resulting
 * collection is a set of access checked, wrapped objects. Only accessible
 * children will be returned.
 *
 * @author Mike Douglass
 */
public interface CoreCalendarsI extends Serializable {

  /* ====================================================================
   *                   Calendars
   * ==================================================================== */

  /** Called whenever we start running under a new principal. May require a
   * flush of some cached information.
   *
   * @throws CalFacadeException
   */
  public void principalChanged() throws CalFacadeException;

  /**
   * @param path
   * @param token or null if first call
   * @return CollectionSynchInfo
   * @throws CalFacadeException
   */
  public CollectionSynchInfo getSynchInfo(String path,
                                          String token) throws CalFacadeException;

  /** Returns children of the given calendar to which the current user has
   * some access.
   *
   * @param  cal          parent calendar
   * @return Collection   of BwCalendar
   * @throws CalFacadeException
   */
  public Collection<BwCalendar> getCalendars(BwCalendar cal) throws CalFacadeException;

  /** Attempt to get calendar referenced by the alias. For an internal alias
   * the result will also be set in the aliasTarget property of the parameter.
   *
   * @param val
   * @param resolveSubAlias - if true and the alias points to an alias, resolve
   *                  down to a non-alias.
   * @param freeBusy
   * @return BwCalendar
   * @throws CalFacadeException
   */
  public BwCalendar resolveAlias(BwCalendar val,
                                 boolean resolveSubAlias,
                                 boolean freeBusy) throws CalFacadeException;

  /** Find any aliases to the given collection.
   *
   * @param val - the alias
   * @return list of aliases
   * @throws CalFacadeException
   */
  public List<BwCalendar> findAlias(String val) throws CalFacadeException;

  /** Get a calendar given the path. If the path is that of a 'special'
   * calendar, for example the deleted calendar, it may not exist if it has
   * not been used.
   *
   * @param  path          String path of calendar
   * @param  desiredAccess int access we need
   * @param  alwaysReturnResult  false to raise access exceptions
   *                             true to return only those we have access to
   * @return BwCalendar null for unknown calendar
   * @throws CalFacadeException
   */
  public BwCalendar getCalendar(String path,
                                int desiredAccess,
                                boolean alwaysReturnResult) throws CalFacadeException;

  /** Returned by getSpecialCalendar
   */
  public static class GetSpecialCalendarResult {
    /** True if user does not exist
     */
    public boolean noUserHome;

    /** True if calendar was created
     */
    public boolean created;

    /**
     */
    public BwCalendar cal;
  }
  /** Get a special calendar (e.g. Trash) for the given user. If it does not
   * exist and is supported by the target system it will be created.
   *
   * @param  owner
   * @param  calType   int special calendar type.
   * @param  create    true if we should create it if non-existant.
   * @param  access    int desired access - from PrivilegeDefs
   * @return GetSpecialCalendarResult null for unknown calendar
   * @throws CalFacadeException
   */
  public GetSpecialCalendarResult getSpecialCalendar(BwPrincipal owner,
                                                     int calType,
                                                     boolean create,
                                                     int access) throws CalFacadeException;

  /** Add a calendar object
   *
   * <p>The new calendar object will be added to the db. If the indicated parent
   * is null it will be added as a root level calendar.
   *
   * <p>Certain restrictions apply, mostly because of interoperability issues.
   * A calendar cannot be added to another calendar which already contains
   * entities, e.g. events etc.
   *
   * <p>Names cannot contain certain characters - (complete this)
   *
   * <p>Name must be unique at this level, i.e. all paths must be unique
   *
   * @param  val     BwCalendar new object
   * @param  parentPath  String path to parent.
   * @return BwCalendar object as added. Parameter val MUST be discarded
   * @throws CalFacadeException
   */
  public BwCalendar add(BwCalendar val,
                                String parentPath) throws CalFacadeException;

  /** Change the name (path segment) of a calendar object.
   *
   * @param  val         BwCalendar object
   * @param  newName     String name
   * @throws CalFacadeException
   */
  public void renameCalendar(BwCalendar val,
                             String newName) throws CalFacadeException;

  /** Move a calendar object from one parent to another
   *
   * @param  val         BwCalendar object
   * @param  newParent   BwCalendar potential parent
   * @throws CalFacadeException
   */
  public void moveCalendar(BwCalendar val,
                           BwCalendar newParent) throws CalFacadeException;

  /** Mark collection as modified
   *
   * @param  path    String path for the collection
   * @throws CalFacadeException
   */
  public void touchCalendar(final String path) throws CalFacadeException;

  /** Mark collection as modified
   *
   * @param  col         BwCalendar object
   * @throws CalFacadeException
   */
  public void touchCalendar(final BwCalendar col) throws CalFacadeException;

  /** Update a calendar object
   *
   * @param  val     BwCalendar object
   * @throws CalFacadeException
   */
  public void updateCalendar(BwCalendar val) throws CalFacadeException;

  /** Change the access to the given calendar entity.
   *
   * @param cal      Bwcalendar
   * @param aces     Collection of ace
   * @param replaceAll true to replace the entire access list.
   * @throws CalFacadeException
   */
  public void changeAccess(BwCalendar cal,
                           Collection<Ace> aces,
                           boolean replaceAll) throws CalFacadeException;

  /** Remove any explicit access for the given who to the given calendar entity.
   *
   * @param cal      Bwcalendar
   * @param who      AceWho
   * @throws CalFacadeException
   */
  public abstract void defaultAccess(BwCalendar cal,
                                     AceWho who) throws CalFacadeException;

  /** Delete the given calendar
   *
   * <p>XXX Do we want a recursive flag or do we implement that higher up?
   *
   * @param val      BwCalendar object to be deleted
   * @param reallyDelete Really delete it - otherwise it's tombstoned
   * @return boolean false if it didn't exist, true if it was deleted.
   * @throws CalFacadeException
   */
  public boolean deleteCalendar(BwCalendar val,
                                boolean reallyDelete) throws CalFacadeException;

  /** Check to see if a collection is empty. A collection is not empty if it
   * contains other collections or calendar entities.
   *
   * @param val      BwCalendar object to check
   * @return boolean true if the calendar is empty
   * @throws CalFacadeException
   */
  public boolean isEmpty(BwCalendar val) throws CalFacadeException;

  /** Called after a principal has been added to the system.
   *
   * @param principal
   * @throws CalFacadeException
   */
  public void addNewCalendars(BwPrincipal principal) throws CalFacadeException;

  /** Return all collections on the given path with a lastmod GREATER
   * THAN that supplied. The path may not be null. A null lastmod will
   * return all collections in the collection.
   *
   * <p>Note that this is used only for synch reports and purging of tombstoned
   * collections. The returned objects are NOT to be delivered to clients.
   *
   * @param path - must be non-null
   * @param lastmod - limit search, may be null
   * @return list of collection paths.
   * @throws CalFacadeException
   */
  public Set<BwCalendar> getSynchCols(String path,
                                      String lastmod) throws CalFacadeException;

  /** Return the value to be used as the sync-token property for th egiven path.
   * This is effectively the max sync-token of the collection and any child
   * collections.
   *
   * @param path
   * @return a sync-token
   * @throws CalFacadeException
   */
  public String getSyncToken(String path) throws CalFacadeException;

  /* ====================================================================
   *                  Admin support
   * ==================================================================== */

  /** Obtain the next batch of children paths for the supplied path. A path of
   * null will return the system roots.
   *
   * @param parentPath
   * @param start start index in the batch - 0 for the first
   * @param count count of results we want
   * @return collection of String paths or null for no more
   * @throws CalFacadeException
   */
  public Collection<String> getChildCollections(String parentPath,
                                        int start,
                                        int count) throws CalFacadeException;
}
