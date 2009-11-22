/*
 * Copyright (c) 2007-2009 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package cascading.tap;

import java.io.IOException;

import cascading.scheme.Scheme;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.mapred.JobConf;

/**
 * Class GlobHfs is a type of {@link MultiSourceTap} that accepts Hadoop style 'file globbing' expressions so
 * multiple files that match the given pattern may be used as the input sources for a given {@link Flow}.
 * <p/>
 * See {@link FileSystem#globStatus(org.apache.hadoop.fs.Path)} for details on the globbing syntax. But in short
 * it is similiar to standard regular expressions except alternation is done via {foo,bar} instead of (foo|bar).
 *
 * @see Hfs
 * @see MultiSourceTap
 * @see FileSystem
 */
public class GlobHfs extends MultiSourceTap
  {
  /** Field pathPattern */
  private String pathPattern;
  /** Field pathFilter */
  private PathFilter pathFilter;

  /**
   * Constructor GlobHfs creates a new GlobHfs instance.
   *
   * @param scheme      of type Scheme
   * @param pathPattern of type String
   */
  public GlobHfs( Scheme scheme, String pathPattern )
    {
    this( scheme, pathPattern, null );
    }

  /**
   * Constructor GlobHfs creates a new GlobHfs instance.
   *
   * @param scheme      of type Scheme
   * @param pathPattern of type String
   * @param pathFilter  of type PathFilter
   */
  public GlobHfs( Scheme scheme, String pathPattern, PathFilter pathFilter )
    {
    super( scheme );
    this.pathPattern = pathPattern;
    this.pathFilter = pathFilter;
    }

  @Override
  protected Tap[] getTaps()
    {
    if( taps != null )
      return taps;

    try
      {
      taps = makeTaps( new JobConf() );
      }
    catch( IOException exception )
      {
      throw new TapException( "unable to resolve taps for globbing path: " + pathPattern );
      }

    return taps;
    }

  private Tap[] makeTaps( JobConf conf ) throws IOException
    {
    FileStatus[] statusList = null;

    Path path = new Path( pathPattern );

    FileSystem fileSystem = path.getFileSystem( conf );

    if( pathFilter == null )
      statusList = fileSystem.globStatus( path );
    else
      statusList = fileSystem.globStatus( path, pathFilter );

    if( statusList == null || statusList.length == 0 )
      throw new TapException( "unable to find paths matching path pattern: " + pathPattern );

    Tap[] taps = new Tap[statusList.length];

    for( int i = 0; i < statusList.length; i++ )
      taps[ i ] = new Hfs( getScheme(), statusList[ i ].getPath().toString() );

    return taps;
    }

  @Override
  public void sourceInit( JobConf conf ) throws IOException
    {
    taps = makeTaps( conf );
    super.sourceInit( conf );
    }

  @Override
  public boolean equals( Object object )
    {
    if( this == object )
      return true;
    if( object == null || getClass() != object.getClass() )
      return false;
    if( !super.equals( object ) )
      return false;

    GlobHfs globHfs = (GlobHfs) object;

    if( pathFilter != null ? !pathFilter.equals( globHfs.pathFilter ) : globHfs.pathFilter != null )
      return false;
    if( pathPattern != null ? !pathPattern.equals( globHfs.pathPattern ) : globHfs.pathPattern != null )
      return false;

    return true;
    }

  @Override
  public int hashCode()
    {
    int result = pathPattern != null ? pathPattern.hashCode() : 0;
    result = 31 * result + ( pathFilter != null ? pathFilter.hashCode() : 0 );
    return result;
    }
  }
