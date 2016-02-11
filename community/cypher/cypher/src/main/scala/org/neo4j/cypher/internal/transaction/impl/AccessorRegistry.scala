/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.transaction.impl

import scala.collection.mutable

class AccessorRegistry[T](val accessorType: String) {
  private val accessors: mutable.Map[String, T] = mutable.Map.empty

  def +=(entry: (String, T)) = {
    val (name, value) = entry
    if (accessors.contains(name))
      throw new IllegalArgumentException(s"Already registered an $accessorType with name `$name`")
    else
      accessors += entry
  }

  def -=(name: String) = {
    if (accessors.contains(name))
      accessors -= name
    else
      throw new IllegalArgumentException(s"Cannot unregister unknown $accessorType with name `$name`")
  }

  def isEmpty = size == 0
  def nonEmpty = size > 0

  def size = accessors.size

  override def toString = s"$accessorType(accessors=$accessors)"
}
