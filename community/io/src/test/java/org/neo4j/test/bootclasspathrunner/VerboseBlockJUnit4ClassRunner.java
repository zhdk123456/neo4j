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
package org.neo4j.test.bootclasspathrunner;

import org.junit.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

public class VerboseBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner
{
    public VerboseBlockJUnit4ClassRunner( Class<?> klass ) throws InitializationError
    {
        super( klass );
    }

    @Override
    protected Statement withPotentialTimeout( FrameworkMethod method, Object test, Statement next )
    {
        long timeout = getTimeout( method.getAnnotation( Test.class ) );
        if ( timeout <= 0 )
        {
            return next;
        }
        return new VerboseFailOnTimeout( next, timeout, TimeUnit.MILLISECONDS );
    }

    private static long getTimeout( Test annotation )
    {
        if ( annotation == null )
        {
            return 0;
        }
        return annotation.timeout();
    }
}
