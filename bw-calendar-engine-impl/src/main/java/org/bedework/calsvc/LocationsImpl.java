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
package org.bedework.calsvc;

import org.bedework.calfacade.BwLocation;
import org.bedework.calfacade.BwString;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calsvci.Locations;
import org.bedework.util.caching.FlushMap;
import org.bedework.util.calendar.PropertyIndex.PropertyInfoIndex;

import java.util.Collection;

/** Class which handles manipulation of Locations.
 *
 * @author Mike Douglass   douglm - rpi.edu
 */
public class LocationsImpl
        extends EventPropertiesImpl<BwLocation>
        implements Locations {
  /* We'll cache lists of entities by principal href - flushing them
    every so often.
   */
  private static FlushMap<String, Collection<BwLocation>> cached =
          new FlushMap<>(60 * 1000 * 5, // 5 mins
                         2000);  // max size

  private FlushMap<String, BwLocation> cachedByUid =
          new FlushMap<>(60 * 1000 * 5, // 5 mins
                         2000);  // max size

  /** Constructor
  *
  * @param svci calsvc object
  */
  public LocationsImpl(final CalSvc svci) {
    super(svci);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void init(final boolean adminCanEditAllPublic) {
    super.init(BwLocation.class.getCanonicalName(),
               adminCanEditAllPublic);
  }

  @Override
  Collection<BwLocation> getCached(final String ownerHref) {
    return cached.get(ownerHref);
  }

  @Override
  void putCached(final String ownerHref,
                 final Collection<BwLocation> vals) {
    cached.put(ownerHref, vals);
  }

  @Override
  void removeCached(final String ownerHref) {
    cached.remove(ownerHref);
  }

  @Override
  BwLocation getCachedByUid(final String uid) {
    return cachedByUid.get(uid);
  }

  @Override
  void putCachedByUid(final String uid, final BwLocation val) {
    cachedByUid.put(uid, val);
  }

  @Override
  void removeCachedByUid(final String uid) {
    cachedByUid.remove(uid);
  }

  @Override
  Collection<BwLocation> fetchAllIndexed(final boolean publick,
                                         final String ownerHref)
          throws CalFacadeException {
    return getIndexer(publick, ownerHref).fetchAllLocations();
  }

  @Override
  BwLocation fetchIndexedByUid(String uid) throws CalFacadeException {
    return getIndexer().fetchLocation(uid, PropertyInfoIndex.UID);
  }

  BwLocation findPersistent(final BwLocation val,
                            final String ownerHref) throws CalFacadeException {
    return findPersistent(val.getAddress(), ownerHref);
  }

  @Override
  public boolean exists(BwLocation val) throws CalFacadeException {
    return findPersistent(val.getFinderKeyValue(), val.getOwnerHref()) != null;
  }

  @Override
  public BwLocation find(final BwString val) throws CalFacadeException {
    return getIndexer().fetchLocation("address.value",
                                      PropertyInfoIndex.ADDRESS,
                                      PropertyInfoIndex.VALUE);
  }
}

