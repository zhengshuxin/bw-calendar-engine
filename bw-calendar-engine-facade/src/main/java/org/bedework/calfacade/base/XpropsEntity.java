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
package org.bedework.calfacade.base;

import org.bedework.calfacade.BwXproperty;
import org.bedework.calfacade.exc.CalFacadeException;

import java.util.List;

/** An entity that can have one or more x-properties will implement this interface.
 *
 * @author douglm
 */
public interface XpropsEntity {
  /** Set the x-props
   *
   * @param val    List of x-props
   */
  void setXproperties(final List<BwXproperty> val);

  /**
   * @return List<BwXproperty>
   */
  List<BwXproperty> getXproperties();

  /**
   * @return int
   */
  int getNumXproperties();

  /**
   *
   * @param val - name to match
   * @return list of matching properties - never null
   */
  List<BwXproperty> getXproperties(final String val);

  /** Find x-properties storing the value of the named ical property
   *
   * @param val - name to match
   * @return list of matching properties - never null
   *  @throws org.bedework.calfacade.exc.CalFacadeException
   */
  List<BwXproperty> getXicalProperties(final String val) throws CalFacadeException;

  /** Remove all instances of the named property.
   *
   * @param val - name to match
   * @return number of removed proeprties
   */
  int removeXproperties(final String val);

  /**
   * @param val
   */
  void addXproperty(final BwXproperty val);

  /**
   * @param val
   */
  void removeXproperty(final BwXproperty val);

  /**
   * @return List of x-properties
   */
  List<BwXproperty> cloneXproperty();
}
