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
package org.bedework.indexer;

import org.bedework.calfacade.BwCalendar;
import org.bedework.calfacade.BwEvent;
import org.bedework.calfacade.RecurringRetrievalMode;
import org.bedework.calfacade.configs.AuthProperties;
import org.bedework.calfacade.configs.BasicSystemProperties;
import org.bedework.calfacade.configs.IndexProperties;
import org.bedework.calfacade.exc.CalFacadeAccessException;
import org.bedework.calfacade.exc.CalFacadeException;
import org.bedework.calfacade.svc.EventInfo;
import org.bedework.calsvci.CalSvcFactoryDefault;
import org.bedework.calsvci.CalSvcI;
import org.bedework.calsvci.CalSvcIPars;
import org.bedework.calsvci.EventsI;

import org.apache.log4j.Logger;

import java.util.Collection;

/** An interface to the calendar system for the indexer.
 *
 * @author Mike Douglass
 *
 */
public abstract class CalSys {
  protected String name;

  protected String adminAccount;

  protected String principal;

  protected boolean debug;

  private Logger log;

  private String publicCalendarRoot;

  private AuthProperties authpars;
  private AuthProperties unauthpars;
  private IndexProperties idxpars;
  private BasicSystemProperties basicSyspars;

  private int collectionBatchSize = 10;

  private int entityBatchSize = 50;

  private int principalBatchSize = 10;

  private CalSvcI svci;

  /* Who the svci object is for */
  private String curAccount;
  private boolean curPublicAdmin;

  protected static ThreadPool entityThreadPool;
  protected static ThreadPool principalThreadPool;

  /**
   * @param name
   * @param adminAccount
   * @param principal
   * @throws CalFacadeException
   */
  public CalSys(final String name,
                final String adminAccount,
                final String principal) throws CalFacadeException {
    this.name = name;
    this.adminAccount = adminAccount;
    this.principal = principal;
    debug = getLogger().isDebugEnabled();
  }

  protected void setThreadPools(final int maxEntityThreads,
                                final int maxPrincipalThreads) {
    entityThreadPool = new ThreadPool("Entity", maxEntityThreads);
    principalThreadPool = new ThreadPool("Principal", maxPrincipalThreads);
  }

  /**
   *
   */
  public void checkThreads() {
    entityThreadPool.checkThreads();
    principalThreadPool.checkThreads();
  }

  /**
   * @throws CalFacadeException
   *
   */
  public void join() throws CalFacadeException {
    entityThreadPool.waitForProcessors();
    principalThreadPool.waitForProcessors();
  }

  protected IndexerThread getEntityThread(final Processor proc) throws CalFacadeException {
    return entityThreadPool.getThread(proc);
  }

  protected IndexerThread getPrincipalThread(final Processor proc) throws CalFacadeException {
    return principalThreadPool.getThread(proc);
  }

  /**
   * @param principal
   */
  public void setCurrentPrincipal(final String principal) {
    this.principal = principal;
  }

  protected String getParentPath(final String href) {
    int pos = href.lastIndexOf("/");

    if (pos <= 0) {
      return null;
    }

    return href.substring(0, pos);
  }

  protected String getName(final String href) {
    int pos = href.lastIndexOf("/");

    if (pos <= 0) {
      return href;
    }

    if (pos == href.length() - 1) {
      return null;
    }

    return href.substring(pos + 1);
  }

  /** Get an svci object and return it. Also embed it in this object.
   *
   * @return svci object
   * @throws CalFacadeException
   */
  public CalSvcI getSvci() throws CalFacadeException {
    if ((svci != null) && svci.isOpen()) {
      // We shouldn't need to check if it's the same account.
      return svci;
    }

    String account = adminAccount;
    boolean publicAdmin = true;
    String userPrincipalPrefix = "/principals/users/";

    if (principal != null) {
      if (principal.startsWith(userPrincipalPrefix)) {
        account = principal.substring(userPrincipalPrefix.length());

        if (account.endsWith("/")) {
          account = account.substring(0, account.length() - 1);
        }
      }

      publicAdmin = false;
    }

    if ((svci == null) ||
        !account.equals(curAccount) ||
        (publicAdmin != curPublicAdmin)) {
      curAccount = account;
      curPublicAdmin = publicAdmin;

      CalSvcIPars pars = CalSvcIPars.getIndexerPars(account,
                                                    publicAdmin);   // Allow super user
      svci = new CalSvcFactoryDefault().getSvc(pars);
    }

    svci.open();
    svci.beginTransaction();

    return svci;
  }

  /**
   * @return svci object
   * @throws CalFacadeException
   */
  public CalSvcI getAdminSvci() throws CalFacadeException {
    CalSvcIPars pars = CalSvcIPars.getIndexerPars(adminAccount,
                                                  true);   // publicAdmin,
    CalSvcI svci = new CalSvcFactoryDefault().getSvc(pars);

    svci.open();
    svci.beginTransaction();

    return svci;
  }

  /**
   * @throws CalFacadeException
   */
  public void close() throws CalFacadeException {
    if ((svci == null) || !svci.isOpen()) {
      return;
    }

    close(svci);

    svci = null;
  }

  /**
   * @param svci
   * @throws CalFacadeException
   */
  public void close(final CalSvcI svci) throws CalFacadeException {
    if ((svci == null) || !svci.isOpen()) {
      return;
    }

    try {
      svci.endTransaction();
    } catch (Throwable t) {
    }

    try {
      svci.close();
    } catch (Throwable t) {
    }
  }

  protected String getPublicCalendarRoot() throws CalFacadeException {
    if (publicCalendarRoot == null) {
      publicCalendarRoot = getBasicSyspars().getPublicCalendarRoot();
    }

    return publicCalendarRoot;
  }

  protected AuthProperties getAuthpars(boolean auth) throws CalFacadeException {
    CalSvcI svci = null;
    AuthProperties apars;
    if (auth) {
      apars = authpars;
    } else {
      apars = unauthpars;
    }

    if (apars == null) {
      try {
        svci = getAdminSvci();
        apars = svci.getAuthProperties(auth);
        if (auth) {
          authpars = apars;
        } else {
          unauthpars = apars;
        }
      } finally {
        if (svci != null) {
          try {
            close(svci);
          } finally {
          }
        }
      }
    }

    return apars;
  }

  protected IndexProperties getIdxpars() throws CalFacadeException {
    CalSvcI svci = null;
    if (idxpars == null) {
      try {
        svci = getAdminSvci();
        idxpars = svci.getIndexProperties();
      } finally {
        if (svci != null) {
          try {
            close(svci);
          } finally {
          }
        }
      }
    }

    return idxpars;
  }

  protected BasicSystemProperties getBasicSyspars() throws CalFacadeException {
    CalSvcI svci = null;
    if (basicSyspars == null) {
      try {
        svci = getAdminSvci();
        basicSyspars = svci.getBasicSystemProperties();
      } finally {
        if (svci != null) {
          try {
            close(svci);
          } finally {
          }
        }
      }
    }

    return basicSyspars;
  }

  protected boolean hasAccess(final BwCalendar col) throws CalFacadeException {
    // XXX This should do a real access check so we can index subscriptions.

    if (col.getPublick()) {
      return true;
    }

    //if (publick) {
    //  return false;
    //}

    if (principal == null) {
      // We aren't handling a principal yet.
      return true;
    }

    return col.getOwnerHref().equals(principal);
  }

  protected boolean hasAccess(final BwEvent ent) throws CalFacadeException {
    // XXX This should do a real access check so we can index subscriptions.

    if (ent.getPublick()) {
      return true;
    }

    //if (publick) {
    //  return false;
    //}

    return ent.getOwnerHref().equals(principal);
  }

  protected BwCalendar getCollection(final String path) throws CalFacadeException {
    BwCalendar col = svci.getCalendarsHandler().get(path);

    if ((col == null) || !hasAccess(col)) {
      if (debug) {
        if (col == null) {
          debugMsg("No collection");
        } else {
          debugMsg("No access to " + path + " for " + showPrincipal());
        }
      }
      throw new CalFacadeAccessException();
    }

    return col;
  }

  private String showPrincipal() {
    if (principal != null) {
      return "principal=" + principal;
    }

    return "account=" + adminAccount;
  }

  /**
   *
   */
  public static class Refs {
    /** Where we are in the list */
    public int index;

    /** How many to request */
    public int batchSize;

    /** List of references - names or hrefs depending on context */
    public Collection<String> refs;
  }

  /** Get the next batch of principal hrefs.
   *
   * @param refs - null on first call.
   * @return next batch of hrefs or null for no more.
   * @throws CalFacadeException
   */
  protected Refs getPrincipalHrefs(final Refs refs) throws CalFacadeException {
    CalSvcI svci = getAdminSvci();
    Refs r = refs;

    if (r == null) {
      r = new Refs();
      r.batchSize = principalBatchSize;
    }

    try {
      r.refs = svci.getUsersHandler().getPrincipalHrefs(r.index, r.batchSize);

      if (debug) {
        if (r.refs == null) {
          debugMsg("getPrincipalHrefs(" + r.index + ") found none");
        } else {
          debugMsg("getPrincipalHrefs(" + r.index + ") found " +
                   r.refs.size());
        }
      }

      if (r.refs == null) {
        return null;
      }

      r.index += r.refs.size();

      return r;
    } finally {
      close(svci);
    }
  }

  /** Get the next batch of child collection paths.
   *
   * @param path
   * @param refs - null on first call.
   * @return next batch of hrefs or null for no more.
   * @throws CalFacadeException
   */
  protected Refs getChildCollections(final String path,
                                     final Refs refs) throws CalFacadeException {
    if (debug) {
      debugMsg("getChildCollections(" + path + ")");
    }

    Refs r = refs;

    if (r == null) {
      r = new Refs();
      r.batchSize = collectionBatchSize;
    }

    CalSvcI svci = getAdminSvci();

    try {
      BwCalendar col = svci.getCalendarsHandler().get(path);

      if ((col == null) || !hasAccess(col)) {
        if (debug) {
          if (col == null) {
            debugMsg("No collection");
          } else {
            debugMsg("No access");
          }
        }
        throw new CalFacadeAccessException();
      }

      r.refs = svci.getAdminHandler().getChildCollections(path, r.index, r.batchSize);

      if (debug) {
        if (r.refs == null) {
          debugMsg("getChildCollections(" + path + ") found none");
        } else {
          debugMsg("getChildCollections(" + path + ") found " + r.refs.size());
        }
      }

      if (r.refs == null) {
        return null;
      }

      r.index += r.refs.size();

      return r;
    } finally {
      close(svci);
    }
  }

  /** Get the next batch of child entity names.
   *
   * @param path
   * @param refs - null on first call.
   * @return next batch of hrefs or null for no more.
   * @throws CalFacadeException
   */
  protected Refs getChildEntities(final String path,
                                  final Refs refs) throws CalFacadeException {
    if (debug) {
      debugMsg("getChildEntities(" + path + ")");
    }

    CalSvcI svci = getAdminSvci();

    Refs r = refs;

    if (r == null) {
      r = new Refs();
      r.batchSize = entityBatchSize;
    }

    try {
      BwCalendar col = svci.getCalendarsHandler().get(path);

      if ((col == null) || !hasAccess(col)) {
        throw new CalFacadeAccessException();
      }

      r.refs = svci.getAdminHandler().getChildEntities(path, r.index, r.batchSize);

      if (debug) {
        if (r.refs == null) {
          debugMsg("getChildEntities(" + path + ") found none");
        } else {
          debugMsg("getChildEntities(" + path + ") found " + r.refs.size());
        }
      }

      if (r.refs == null) {
        return null;
      }

      r.index += r.refs.size();

      return r;
    } finally {
      close(svci);
    }
  }

  protected EventInfo getEvent(final String colPath,
                               final String name) throws CalFacadeException {
    EventsI evhandler = svci.getEventsHandler();

    return evhandler.get(colPath, name,
                         RecurringRetrievalMode.overrides);
  }

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }
}
