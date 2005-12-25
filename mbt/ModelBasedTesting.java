//This file is part of the Model-based Testing java package
//Copyright (C) 2005  Kristian Karl
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

package mbt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

/**
 * @author Kristian Karl
 */
public class ModelBasedTesting
{

	private SparseGraph _graph          = new SparseGraph();
	private SAXBuilder  _parser         = new SAXBuilder();
	private Random      _radomGenerator = new Random();

	private String  START_NODE           = "Start";
	private String  ID_KEY               = "id";
	private String  FILE_KEY             = "file";
	private String  LABEL_KEY            = "label";
	private String  VISITED_KEY          = "visited";
	private String  WEIGHT_KEY           = "weight";
	private String  STATE_KEY            = "state";
	private String  CONDITION_KEY        = "condition";
	private String  VARIABLE_KEY         = "variable";
	private String  BACK_KEY             = "back";
	private String  NO_HISTORY	         = "no history";
	private String  NO_MERGE	         = "no merge";

	private Document 			 _doc;
	private String   			 _graphmlFileName;
	private Object   			 _object;
	private Logger   			 _logger;
	private Object[] 			 _vertices     = null;
	private Object[]             _edges        = null;
	private DirectedSparseVertex _nextVertex   = null;
	private DirectedSparseVertex _prevVertex   = null;
	private DirectedSparseEdge 	 _rejectedEdge = null;
	private LinkedList 			 _history      = new LinkedList();
	private long				 _start_time;
	private long				 _end_time;
	private boolean				 _runUntilAllEdgesVisited = false;
	private List				 _shortestPathToVertex = null;

	public ModelBasedTesting( String graphmlFileName_,
							  Object object_,
							  Logger logger_ )
	{
		_graphmlFileName = graphmlFileName_;
		_object          = object_;
		_logger          = logger_;

		readFiles();
	}

	public ModelBasedTesting( String graphmlFileName_,
							  Logger logger_ )
	{
		_graphmlFileName = graphmlFileName_;
		_object          = null;
		_logger          = logger_;

		readFiles();
	}

	public ModelBasedTesting( String graphmlFileName_,
			  Object object_ )
	{
		_graphmlFileName = graphmlFileName_;
		_object          = object_;
		_logger          = null;
		
		readFiles();
	}

	public void writeGraph( String mergedGraphml_ )
	{
		StringBuffer sourceFile = new StringBuffer();
		try {
			FileWriter file = new FileWriter( mergedGraphml_ );
			
			sourceFile.append( "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" );
			sourceFile.append( "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns/graphml\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns/graphml http://www.yworks.com/xml/schema/graphml/1.0/ygraphml.xsd\" xmlns:y=\"http://www.yworks.com/xml/graphml\">\n" );
			sourceFile.append( "  <key id=\"d0\" for=\"node\" yfiles.type=\"nodegraphics\"/>\n" );
			sourceFile.append( "  <key id=\"d1\" for=\"edge\" yfiles.type=\"edgegraphics\"/>\n" );
			sourceFile.append( "  <graph id=\"G\" edgedefault=\"directed\">\n" );

	        int numVertices = _graph.getVertices().size();
	        Indexer id = Indexer.getIndexer( _graph );
	        for ( int i = 0; i < numVertices; i++ )
	        {
	            Vertex v = (Vertex) id.getVertex(i);
	            int vId = i+1;

				sourceFile.append( "    <node id=\"n" + vId + "\">\n" );
				sourceFile.append( "      <data key=\"d0\" >\n" );
				sourceFile.append( "        <y:ShapeNode >\n" );
				sourceFile.append( "          <y:Geometry  x=\"241.875\" y=\"158.701171875\" width=\"95.0\" height=\"30.0\"/>\n" );
				sourceFile.append( "          <y:Fill color=\"#CCCCFF\"  transparent=\"false\"/>\n" );
				sourceFile.append( "          <y:BorderStyle type=\"line\" width=\"1.0\" color=\"#000000\" />\n" );
				sourceFile.append( "          <y:NodeLabel x=\"1.5\" y=\"5.6494140625\" width=\"92.0\" height=\"18.701171875\" visible=\"true\" alignment=\"center\" fontFamily=\"Dialog\" fontSize=\"12\" fontStyle=\"plain\" textColor=\"#000000\" modelName=\"internal\" modelPosition=\"c\" autoSizePolicy=\"content\">" + v.getUserDatum( LABEL_KEY ) + "</y:NodeLabel>\n" );
				sourceFile.append( "          <y:Shape type=\"rectangle\"/>\n" );
				sourceFile.append( "        </y:ShapeNode>\n" );
				sourceFile.append( "      </data>\n" );
				sourceFile.append( "    </node>\n" );
			}

	        for ( Iterator edgeIterator = _graph.getEdges().iterator(); edgeIterator.hasNext(); ) 
	        {
	            Edge e = (Edge) edgeIterator.next();
	            Pair p = e.getEndpoints();
	            Vertex src = (Vertex) p.getFirst();
	            Vertex dest = (Vertex) p.getSecond();
	            int srcId = id.getIndex(src)+1;
	            int destId = id.getIndex(dest)+1;
	            
	            sourceFile.append( "    <edge source=\"n" + srcId + "\" target=\"n" + destId + "\">\n" );
	            sourceFile.append( "      <data key=\"d1\" >\n" );
	            sourceFile.append( "        <y:PolyLineEdge >\n" );
	            sourceFile.append( "          <y:Path sx=\"-23.75\" sy=\"15.0\" tx=\"-23.75\" ty=\"-15.0\">\n" );
	            sourceFile.append( "            <y:Point x=\"273.3125\" y=\"95.0\"/>\n" );
	            sourceFile.append( "            <y:Point x=\"209.5625\" y=\"95.0\"/>\n" );
	            sourceFile.append( "            <y:Point x=\"209.5625\" y=\"143.701171875\"/>\n" );
	            sourceFile.append( "            <y:Point x=\"265.625\" y=\"143.701171875\"/>\n" );
	            sourceFile.append( "          </y:Path>\n" );
	            sourceFile.append( "          <y:LineStyle type=\"line\" width=\"1.0\" color=\"#000000\" />\n" );
	            sourceFile.append( "          <y:Arrows source=\"none\" target=\"standard\"/>\n" );
	            sourceFile.append( "          <y:EdgeLabel x=\"-148.25\" y=\"30.000000000000014\" width=\"169.0\" height=\"18.701171875\" visible=\"true\" alignment=\"center\" fontFamily=\"Dialog\" fontSize=\"12\" fontStyle=\"plain\" textColor=\"#000000\" modelName=\"free\" modelPosition=\"anywhere\" preferredPlacement=\"on_edge\" distance=\"2.0\" ratio=\"0.5\">" + e.getUserDatum( LABEL_KEY ) + "</y:EdgeLabel>\n" );
	            sourceFile.append( "          <y:BendStyle smoothed=\"false\"/>\n" );
	            sourceFile.append( "        </y:PolyLineEdge>\n" );
	            sourceFile.append( "      </data>\n" );
	            sourceFile.append( "    </edge>\n" );
	            
	        }
	        
	        sourceFile.append( "  </graph>\n" );
	        sourceFile.append( "</graphml>\n" );
	        
			file.write( sourceFile.toString() );
			file.flush();
			_logger.debug( "Wrote: " +  mergedGraphml_ );
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	private void readFiles()
	{
		File file = new File( _graphmlFileName );
		if ( file.isFile() )
		{
			parseFile( _graphmlFileName );
		}
		else if ( file.isDirectory() )
		{
		    // Only accpets files which suffix is .graphml
		    FilenameFilter filter = new FilenameFilter()
		    {
		        public boolean accept( File dir, String name )
		        {
		            return name.endsWith( ".graphml" );
		        }
		    };


		    File [] allChildren = file.listFiles( filter );
		    for ( int i = 0; i < allChildren.length; ++i )
		    {
		    	parseFile( allChildren[ i ].getAbsolutePath() );
		    }

		    mergeVertices();

		    /*Layout l = new FRLayout( _graph );
		    Renderer r = new PluggableRenderer();
		    VisualizationViewer vv = new VisualizationViewer( l, r );
		    JFrame jf = new JFrame();
		    jf.getContentPane().add( vv );
		    jf.show();*/
		}
		else
		{
			throw new RuntimeException( "\"" + _graphmlFileName + "\" is not a file or a directory. Please specify a valid .graphml file or a directory containing .graphml files" );
		}
	}

	/**
	 * @param runningTime
	 * The time, in seconds, to run this test.
	 */
	public void runRandomWalk( long runningTime ) throws FoundNoEdgeException
	{
		findStartingVertex();

		long startTime = System.currentTimeMillis();
		long currentTime = startTime;
		_start_time = startTime;


		runningTime *= 1000;

		// Start the execution which is random.
		while ( ( currentTime - startTime ) < runningTime )
		{
			executeMethod( false );
			currentTime = System.currentTimeMillis();
			_end_time = currentTime;
		}
	}


	/**
	 * Run the test untill all vertices (nodes) are visited.
	 */
	public void runUntilAllVerticesVisited() throws FoundNoEdgeException
	{
		findStartingVertex();

		_start_time = System.currentTimeMillis();
		while ( true )
		{
			executeMethod( true );
			_end_time = System.currentTimeMillis();
			if ( isAllVerticesVisited() )
			{
				break;
			}
		}
	}


	/**
	 * Run the test untill all edges (arrows, or transistions) are visited.
	 */
	public void runUntilAllEdgesVisited() throws FoundNoEdgeException
	{
		_runUntilAllEdgesVisited = true;

		findStartingVertex();

		_start_time = System.currentTimeMillis();
		while ( true )
		{
			executeMethod( true );
			_end_time = System.currentTimeMillis();
			if ( isAllEdgesVisited() )
			{
				break;
			}
		}
	}


	/**
	 * Run the test untill all edges (arrows, or transistions), and
	 * all vertices (nodes) are visited
	 */
	public void runUntilAllVerticesAndEdgesVisited() throws FoundNoEdgeException
	{
		findStartingVertex();

		_start_time = System.currentTimeMillis();
		while ( true )
		{
			executeMethod( true );
			_end_time = System.currentTimeMillis();
			if ( isAllVerticesVisited() && isAllEdgesVisited() )
			{
				break;
			}
		}
	}


	/**
	 * Returns true if all vertices (nodes) are visited
	 */
	private boolean isAllVerticesVisited()
	{
		_vertices = _graph.getVertices().toArray();

		for ( int i = 0; i < _edges.length; i++ )
		{
			DirectedSparseVertex vertex = (DirectedSparseVertex)_vertices[ i ];

			Integer vistited = (Integer)vertex.getUserDatum( VISITED_KEY );
			if ( vistited.intValue() == 0 )
			{
				return false;
			}
		}
		return true;
	}


	/**
	 * Returns true if all edges (arrows or transitions) are visited
	 */
	private boolean isAllEdgesVisited()
	{
		_edges = _graph.getEdges().toArray();

		for ( int i = 0; i < _edges.length; i++ )
		{
			DirectedSparseEdge edge = (DirectedSparseEdge)_edges[ i ];

			Integer vistited = (Integer)edge.getUserDatum( VISITED_KEY );
			if ( vistited.intValue() == 0 )
			{
				return false;
			}
		}
		return true;
	}


	/**
	 * Parses the graphml file, and load into the internal graph structure _graph
	 */
	private void parseFile( String fileName )
	{
		try
		{
			_logger.info( "Parsing file: " + fileName );
			_doc = _parser.build( fileName );

			// Parse all vertices (nodes)
			Iterator iter = _doc.getDescendants( new ElementFilter( "node" ) );
			while ( iter.hasNext() )
			{
				Object o = iter.next();
				if ( o instanceof Element )
				{
					Element element = (Element)o;
					if ( element.getAttributeValue( "yfiles.foldertype" ) != null )
					{
						_logger.debug( "Excluded node: " + element.getAttributeValue( "yfiles.foldertype" ) );
						continue;
					}
					_logger.debug( "id: " + element.getAttributeValue( "id" ) );

					Iterator iter2 = element.getDescendants( new ElementFilter( "NodeLabel" ) );
					while ( iter2.hasNext() )
					{
						Object o2 = iter2.next();
						if ( o2 instanceof Element )
						{
							Element nodeLabel = (Element)o2;
							_logger.debug( "Full name: " + nodeLabel.getQualifiedName() );
							_logger.debug( "Name: " + nodeLabel.getTextTrim() );

							DirectedSparseVertex v = (DirectedSparseVertex) _graph.addVertex( new DirectedSparseVertex() );

							v.addUserDatum( ID_KEY, 	 element.getAttributeValue( "id" ), UserData.SHARED );
							v.addUserDatum( VISITED_KEY, new Integer( 0 ), 					UserData.SHARED );
							v.addUserDatum( FILE_KEY, 	 fileName, 							UserData.SHARED );

							String str = nodeLabel.getTextTrim();
							Pattern p = Pattern.compile( "(.*)", Pattern.MULTILINE );
							Matcher m = p.matcher( str );
							String label;
							if ( m.find( ))
							{
								label = m.group( 1 );
								v.addUserDatum( LABEL_KEY, label, UserData.SHARED );
							}
							else
							{
								throw new RuntimeException( "Label must be defined." );
							}
							_logger.debug( "Added node: " + v.getUserDatum( LABEL_KEY ) );





							// If no merge is defined, find it...
							// If defined, it means that when merging graphs, this specific vertex will not be merged.
							p = Pattern.compile( "(no merge)", Pattern.MULTILINE );
							m = p.matcher( str );
							if ( m.find() )
							{
								v.addUserDatum( NO_MERGE, m.group( 1 ), UserData.SHARED );
								_logger.debug( "Found no merge for edge: " + label );
							}

							
							
							// NOTE: Only for html applications
							// In browsers, the usage of the 'Back'-button can be used.
							// If defined, with a value value, which depicts the probability for the edge
							// to be executed, tha back-button will be pressed in the browser.
							// A value of 0.05 is the same as 5% chance of going down this road.
							p = Pattern.compile( "(back=(.*))", Pattern.MULTILINE );
							m = p.matcher( str );
							if ( m.find( ) )
							{
								Float probability;
								String value = m.group( 2 );
								try
								{
									probability = Float.valueOf( value.trim() );
								}
								catch ( NumberFormatException error )
								{
									throw new RuntimeException( "For label: " + label + ", back is not a correct float value: " + error.toString() );
								}
								v.addUserDatum( BACK_KEY, probability, UserData.SHARED );
							}
						}
					}
				}
			}

			Object[] vertices = _graph.getVertices().toArray();

			// Parse all edges (arrows or transtitions)
			iter = _doc.getDescendants( new ElementFilter( "edge" ) );
			while ( iter.hasNext() )
			{
				Object o = iter.next();
				if ( o instanceof Element )
				{
					Element element = (Element)o;
					_logger.debug( "id: " + element.getAttributeValue( "id" ) );

					Iterator iter2 = element.getDescendants( new ElementFilter( "EdgeLabel" ) );
					Element edgeLabel = null;
					if ( iter2.hasNext() )
					{
						Object o2 = iter2.next();
						if ( o2 instanceof Element )
						{
							edgeLabel = (Element)o2;
							_logger.debug( "Full name: " + edgeLabel.getQualifiedName() );
							_logger.debug( "Name: " + edgeLabel.getTextTrim() );
						}
					}
					_logger.debug( "source: " + element.getAttributeValue( "source" ) );
					_logger.debug( "target: " + element.getAttributeValue( "target" ) );

					DirectedSparseVertex source = null;
					DirectedSparseVertex dest = null;

					for ( int i = 0; i < vertices.length; i++ )
					{
						DirectedSparseVertex vertex = (DirectedSparseVertex)vertices[ i ];

						// Find source vertex
						if ( vertex.getUserDatum( ID_KEY ).equals( element.getAttributeValue( "source" ) ) &&
							 vertex.getUserDatum( FILE_KEY ).equals( fileName ) )
						{
							source = vertex;
						}
						if ( vertex.getUserDatum( ID_KEY ).equals( element.getAttributeValue( "target" ) ) &&
							 vertex.getUserDatum( FILE_KEY ).equals( fileName ) )
						{
							dest = vertex;
						}
					}
					if ( source == null )
					{
						String msg = "Could not find starting node for edge. Name: " + element.getAttributeValue( "source" );
						_logger.error( msg );
						throw new RuntimeException( msg );
					}
					if ( dest == null )
					{
						String msg = "Could not find end node for edge. Name: " + element.getAttributeValue( "target" );
						_logger.error( msg );
						throw new RuntimeException( msg );
					}


					DirectedSparseEdge e = new DirectedSparseEdge( source, dest );
					_graph.addEdge( e );
					e.addUserDatum( ID_KEY,   element.getAttributeValue( "id" ), UserData.SHARED );
					e.addUserDatum( FILE_KEY, fileName, 						 UserData.SHARED );


					if ( edgeLabel != null )
					{
						String str = edgeLabel.getTextTrim();
						Pattern p = Pattern.compile( "(.*)", Pattern.MULTILINE );
						Matcher m = p.matcher( str );
						String label = null;
						if ( m.find() )
						{
							label = m.group( 1 );
							e.addUserDatum( LABEL_KEY, label, UserData.SHARED );
							_logger.debug( "Found label= " + label + " for edge id: " + edgeLabel.getQualifiedName() );
						}
						else
						{
							throw new RuntimeException( "Label for edge must be defined." );
						}



						// If weight is defined, find it...
						// weight must be associated with a value, which depicts the probability for the edge
						// to be executed.
						// A value of 0.05 is the same as 5% chance of going down this road.
						p = Pattern.compile( "(weight=(.*))", Pattern.MULTILINE );
						m = p.matcher( str );
						if ( m.find() )
						{
							Float weight;
							String value = m.group( 2 );
							try
							{
								weight = Float.valueOf( value.trim() );
								_logger.debug( "Found weight= " + weight + " for edge: " + label );
							}
							catch ( NumberFormatException error )
							{
								throw new RuntimeException( "For label: " + label + ", weight is not a correct float value: " + error.toString() );
							}
							e.addUserDatum( WEIGHT_KEY, weight, UserData.SHARED );
						}



						// If No_history is defined, find it...
						// If defined, it means that when executing this edge, it shall not
						// be added to the history list of passed edgses.
						p = Pattern.compile( "(No_history)", Pattern.MULTILINE );
						m = p.matcher( str );
						if ( m.find() )
						{
							e.addUserDatum( NO_HISTORY, m.group( 1 ), UserData.SHARED );
							_logger.debug( "Found No_history for edge: " + label );
						}



						// If condition used defined, find it...
						p = Pattern.compile( "(if: (.*)=(.*))", Pattern.MULTILINE );
						m = p.matcher( str );
						HashMap conditions = null;
						while ( m.find( ) )
						{
							if ( conditions == null )
							{
								conditions = new HashMap();
							}
							String variable = m.group( 2 );
							Boolean state   = Boolean.valueOf( m.group( 3 ) );
							conditions.put( variable, state );
							_logger.debug( "Condition: " + variable + " = " +  state );
						}
						if ( conditions != null )
						{
							e.addUserDatum( CONDITION_KEY, conditions, UserData.SHARED );
						}



						// If state are defined, find them...
						HashMap states = null;
						p = Pattern.compile( "(state: (.*)=(.*))", Pattern.MULTILINE );
						m = p.matcher( str );
						while ( m.find( ) )
						{
							if ( states == null )
							{
								states = new HashMap();
							}
							String variable = m.group( 2 );
							Boolean state   = Boolean.valueOf( m.group( 3 ) );
							states.put( variable, state );
							_logger.debug( "State: " + variable + " = " +  state );
						}
						if ( states != null )
						{
							e.addUserDatum( STATE_KEY, states, UserData.SHARED );
						}



						// If string variables are defined, find them...
						HashMap variables = null;
						p = Pattern.compile( "(string: (.*)=(.*))", Pattern.MULTILINE );
						m = p.matcher( str );
						while ( m.find( ) )
						{
							if ( variables == null )
								{
									variables = new HashMap();
								}
								String variableLabel = m.group( 2 );
								String variable = m.group( 3 );
								variables.put( variableLabel, variable );
								_logger.debug( "String variable: " + variableLabel + " = " +  variable );
							}



							// If integer variables are defined, find them...
							p = Pattern.compile( "(integer: (.*)=(.*))", Pattern.MULTILINE );
							m = p.matcher( str );
							while ( m.find( ) )
							{
								if ( variables == null )
								{
									variables = new HashMap();
								}
								String variableLabel = m.group( 2 );
								Integer variable   = Integer.valueOf( m.group( 3 ) );
								variables.put( variableLabel, variable );
								_logger.debug( "Integer variable: " + variableLabel + " = " +  variable );
							}



							// If integer variables are defined, find them...
							p = Pattern.compile( "(float: (.*)=(.*))", Pattern.MULTILINE );
							m = p.matcher( str );
							while ( m.find( ) )
							{
								if ( variables == null )
								{
									variables = new HashMap();
								}
								String variableLabel = m.group( 2 );
								Float variable   = Float.valueOf( m.group( 3 ) );
								variables.put( variableLabel, variable );
								_logger.debug( "Float variable: " + variableLabel + " = " +  variable );
							}
							if ( variables != null )
							{
								e.addUserDatum( VARIABLE_KEY, variables, UserData.SHARED );
							}

						}
						e.addUserDatum( VISITED_KEY, new Integer( 0 ), UserData.SHARED );
						_logger.debug( "Adderade edge: \"" + e.getUserDatum( LABEL_KEY ) + "\"" );
					}
				}
		}
		catch ( JDOMException e )
		{
			_logger.error( e );
			throw new RuntimeException( "Kunde inte skanna filen: " + fileName );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			throw new RuntimeException( "Kunde inte skanna filen: " + fileName );
		}
	}


	// When multiple graps are read from several files, cgances are that there are vertices that has
	// no out edges, which are continued in a different file. These has to be merged.
	private void mergeVertices()
	{
		Vector A = new Vector();
		Vector B = new Vector();
		Vector C = new Vector();
		
		
		// Find all vertices, which has a in-edge,
		// with an empty label, and comes from
		// a Start node.
		// Store the Start nodes in a temporary array A.
		// Store the found vertices in a temporary array B.		
		Object[] vertices = _graph.getVertices().toArray();
		for ( int i = 0; i < vertices.length; i++ )
		{
			DirectedSparseVertex startVertex = (DirectedSparseVertex)vertices[ i ];

			// Find all vertices that are start nodes (START_NODE)
			if ( startVertex.getUserDatum( LABEL_KEY ).equals( START_NODE ) )
			{
				// we are only interested of START_NODE's with 1 out edge
				Object[] edges = startVertex.getOutEdges().toArray();
				if ( edges.length == 1 )
				{
					// The out edge should have no label
					DirectedSparseEdge edge = (DirectedSparseEdge)edges[ 0 ];
					if ( !edge.containsUserDatumKey( LABEL_KEY ) )
					{
						A.add( startVertex );
						B.add( (DirectedSparseVertex)edge.getDest() );
					}
				}
			}
		}
		_logger.debug( "Found " + B.size() + " vertices to merge." );
		

		
		
		// Remove all Start nodes stored in the
		// array A found above.
		for ( Iterator iter = A.iterator(); iter.hasNext(); )
		{
			DirectedSparseVertex vertex = (DirectedSparseVertex) iter.next();
			_graph.removeVertex( vertex );
		}
		_logger.debug( "Removed " + A.size() + " vertices (Start) from graph." );

		
		
		// Pick a vertex V1 from the array B
		for ( Iterator iter = B.iterator(); iter.hasNext(); )
		{
			boolean mergeSuccessfull = false;
			DirectedSparseVertex v1 = (DirectedSparseVertex) iter.next();
			_logger.debug( "Investigating vertex V1(" + v1.hashCode() + "): " + v1.getUserDatum( LABEL_KEY ) );

			vertices = _graph.getVertices().toArray();
			for ( int i = 0; i < vertices.length; i++ )
			{
				DirectedSparseVertex v2 = (DirectedSparseVertex)vertices[ i ];
				
				if ( v2.getUserDatum( LABEL_KEY ).equals( v1.getUserDatum( LABEL_KEY ) ) == false )
				{
					continue;
				}
				if ( v2.containsUserDatumKey( NO_MERGE ) )
				{
					continue;
				}
				if ( v1.hashCode() == v2.hashCode() )
				{
					continue;
				}
				if ( v1.getGraph() == null )
				{
					continue;
				}

				_logger.debug( "Found vertex V2(" + v2.hashCode() + "): " + v2.getUserDatum( LABEL_KEY ) );
					
					
				// For all in-edges for vertex V2, change
				// the dest vertex (for that edge) from V2 to V1
				Object[] in_edges = v2.getInEdges().toArray();
				for ( int j = 0; j < in_edges.length; j++ )
				{
					DirectedSparseEdge e2 = (DirectedSparseEdge)in_edges[ j ];
					DirectedSparseEdge e1 = new DirectedSparseEdge( e2.getSource(), v1 );
					e1.importUserData( e2 );
					_graph.addEdge( e1 );
				}
				
				// For all out-edges for vertex V2, change
				// the source vertex (for that edge) from V2 to V1
				Object[] out_edges = v2.getOutEdges().toArray();
				for ( int j = 0; j < out_edges.length; j++ )
				{
					DirectedSparseEdge e2 = (DirectedSparseEdge)out_edges[ j ];
					DirectedSparseEdge e1 = new DirectedSparseEdge( v1, e2.getDest() );
					e1.importUserData( e2 );
					_graph.addEdge( e1 );
				}
								
				_logger.debug( "Merging vertex V2(" + v2.hashCode() + ") with V1(" + v1.hashCode()+ "): " + v1.getUserDatum( LABEL_KEY ) );
				_graph.removeVertex( v2 );
			}
			
			if ( mergeSuccessfull == false )
			{
				_logger.warn( "Could not merge vertex: " + v1.getUserDatum( LABEL_KEY ) + "(" + v1.hashCode() + ")" );
			}
		}

		
		// Find all vertices which has no
		// out-edges and NO_MERGE not defined.
		// Store the vertices in array C		
		vertices = _graph.getVertices().toArray();
		for ( int i = 0; i < vertices.length; i++ )
		{
			DirectedSparseVertex v = (DirectedSparseVertex)vertices[ i ];

			if ( v.getOutEdges().isEmpty() &&
				 v.containsUserDatumKey( NO_MERGE ) == false )
			{
				_logger.debug( "Found vertex V(" + v.hashCode() + "): " + v.getUserDatum( LABEL_KEY ) + ", with no out-edges" );
				C.add( v );
			}
		}

		
		// Pick a vertex V1 from the array C
		for ( Iterator iter = C.iterator(); iter.hasNext(); )
		{
			DirectedSparseVertex v1 = (DirectedSparseVertex) iter.next();

			vertices = _graph.getVertices().toArray();
			for ( int i = 0; i < vertices.length; i++ )
			{
				DirectedSparseVertex v2 = (DirectedSparseVertex)vertices[ i ];

				// Find a vertex V2 with an identical label,
				// and NO_MERGE not defined, and has out-edges
				if ( v2.getUserDatum( LABEL_KEY ).equals( v1.getUserDatum( LABEL_KEY ) ) == false )
				{
					continue;
				}
				if ( v2.containsUserDatumKey( NO_MERGE ) )
				{
					continue;
				}
				if ( v1.hashCode() == v2.hashCode() )
				{
					continue;
				}
				if ( v1.getGraph() == null )
				{
					continue;
				}
				if ( v2.getOutEdges().isEmpty() )
				{
					continue;
				}

				_logger.debug( "Found vertex V2(" + v2.hashCode() + "): " + v2.getUserDatum( LABEL_KEY ) );

				// For all in-edges for vertex V1, change
				// the dest vertex (for that edge) from V1 to V2
				Object[] in_edges = v1.getInEdges().toArray();
				for ( int j = 0; j < in_edges.length; j++ )
				{
					DirectedSparseEdge e2 = (DirectedSparseEdge)in_edges[ j ];
					DirectedSparseEdge e1 = new DirectedSparseEdge( e2.getSource(), v2 );
					e1.importUserData( e2 );
					_graph.addEdge( e1 );
				}

				// Remove vertex V1
				_logger.debug( "Merging vertex V1(" + v1.hashCode() + ") with V2(" + v2.hashCode()+ "): " + v2.getUserDatum( LABEL_KEY ) );
				_graph.removeVertex( v1 );
			}
		}
		
		
		// Check for vertices that has no out-edges
		vertices = _graph.getVertices().toArray();
		for ( int i = 0; i < vertices.length; i++ )
		{
			DirectedSparseVertex v = (DirectedSparseVertex)vertices[ i ];
			if ( v.getOutEdges().isEmpty() )
			{
				_logger.warn( "Found a vertex acting as a Cul-de-Sac: " + v.getUserDatum( LABEL_KEY ) + "(" + v.hashCode() + ")" );
			}
		}
		
		_logger.debug( "Done merging" );
}


	public String get_statistics()
	{
		String stat = new String();
		String new_line = new String( "\n" );

		Object[] vertices = _graph.getVertices().toArray();
		Object[] edges    = _graph.getEdges().toArray();

		int numOfVisitedVertices      = 0;
		int numOfVisitedEdges         = 0;
		int totalNumOfVisitedVertices = 0;
		int totalNumOfVisitedEdges    = 0;

		// Log which edges are not visited.
		for ( int i = 0; i < edges.length; i++ )
		{
			DirectedSparseEdge edge = (DirectedSparseEdge)edges[ i ];

			Integer vistited = (Integer)edge.getUserDatum( VISITED_KEY );
			if ( vistited.intValue() == 0 )
			{
				stat += "Not tested (Edge): " + (String)edge.getUserDatum( LABEL_KEY ) + ", from: " + (String)edge.getSource().getUserDatum( LABEL_KEY ) + ", to: " + (String)edge.getDest().getUserDatum( LABEL_KEY ) + new_line;
			}
			else
			{
				numOfVisitedEdges++;
			}
			totalNumOfVisitedEdges += vistited.intValue();
		}

		// Logga vilka noder som inte ?r bes?kta.
		for ( int i = 0; i < vertices.length; i++ )
		{
			DirectedSparseVertex vertex = (DirectedSparseVertex)vertices[ i ];

			Integer vistited = (Integer)vertex.getUserDatum( VISITED_KEY );
			if ( vistited.intValue() == 0 )
			{
				stat += "Not tested (Vertex): " + (String)vertex.getUserDatum( LABEL_KEY ) + new_line;
			}
			else
			{
				numOfVisitedVertices++;
			}
			totalNumOfVisitedVertices += vistited.intValue();
		}
		stat += "Test coverage edges: " + numOfVisitedEdges + "/" + edges.length + " => " +  (numOfVisitedEdges / (float)edges.length * 100) + "%" + new_line;
		stat += "Test coverage vertices: " + numOfVisitedVertices + "/" + vertices.length + " => " + (numOfVisitedVertices / (float)vertices.length * 100)  + "%" + new_line;
		stat += "Number of visited edges: " + totalNumOfVisitedEdges + new_line;
		stat += "Number of visited vertices: " + totalNumOfVisitedVertices + new_line;
		stat += "Execution time: " + ( ( _end_time - _start_time ) / 1000 ) + " seconds" + new_line;

		return stat;
	}

	/**
	 * Return the instance of the graph
	 */
	public SparseGraph get_graph() {
		return _graph;
	}

	public void generateJavaCode_XDE( String fileName )
	{
		boolean _existBack = false;

		_vertices = _graph.getVertices().toArray();
		_edges    = _graph.getEdges().toArray();

		ArrayList writtenVertices = new ArrayList();
		ArrayList writtenEdges    = new ArrayList();

		StringBuffer sourceFile = new StringBuffer();

		try
		{
			/**
			 * Read the original file first. If the methods already are defined in the file,
			 * leave those methods alone.
			 */
			BufferedReader input = null;
			try
			{
				_logger.debug( "Try to open file: " + fileName );
				input = new BufferedReader( new FileReader( fileName ) );
				String line = null;
				while ( ( line = input.readLine() ) != null )
				{
					sourceFile.append( line );
					sourceFile.append( System.getProperty( "line.separator" ) );
				}
			}
			catch ( FileNotFoundException e )
			{
				_logger.error( "File not found exception: " + e.getMessage() );
			}
			catch ( IOException e )
			{
				_logger.error( "IO exception: " + e.getMessage() );
			}
			finally
			{
				try
				{
					if ( input != null )
					{
						input.close();
					}
				}
				catch ( IOException e )
				{
					_logger.error( "IO exception: " + e.getMessage() );
				}
			}

			_logger.debug( sourceFile.toString() );


			FileWriter file = new FileWriter( fileName );

			for ( int i = 0; i < _vertices.length; i++ )
			{
				DirectedSparseVertex vertex = (DirectedSparseVertex)_vertices[ i ];

				boolean duplicated = false;
				for ( Iterator iter = writtenVertices.iterator(); iter.hasNext(); )
				{
					String str = (String) iter.next();
					if ( str.equals( (String)vertex.getUserDatum( LABEL_KEY ) ) == true )
					{
						duplicated = true;
						break;
					}
				}

				if ( _existBack == false )
				{
					_existBack = true;

					Pattern p = Pattern.compile( "public void PressBackButton\\(\\)(.|[\\n\\r])*?\\{(.|[\\n\\r])*?\\}", Pattern.MULTILINE );
					Matcher m = p.matcher( sourceFile );

					if ( m.find() == false )
					{
						sourceFile.append( "/**\n" );
						sourceFile.append( " * This method implements the edge: PressBackButton\n" );
						sourceFile.append( " */\n" );
						sourceFile.append( "public void PressBackButton()\n" );
						sourceFile.append( "{\n" );
						sourceFile.append( "	_logger.info( \"Edge: PressBackButton\" );\n" );
						sourceFile.append( "	throw new RuntimeException( \"Not implemented. This line can be removed.\" );\n" );
						sourceFile.append( "}\n\n" );
					}
				}

				if ( duplicated == false )
				{
					Pattern p = Pattern.compile( "public void " + (String)vertex.getUserDatum( LABEL_KEY ) + "\\(\\)(.|[\\n\\r])*?\\{(.|[\\n\\r])*?\\}", Pattern.MULTILINE );
					Matcher m = p.matcher( sourceFile );

					if ( m.find() == false )
					{
						sourceFile.append( "/**\n" );
						sourceFile.append( " * This method implements the verification of the vertex: " + (String)vertex.getUserDatum( LABEL_KEY ) + "\n" );
						sourceFile.append( " */\n" );
						sourceFile.append( "public void " + (String)vertex.getUserDatum( LABEL_KEY ) + "()\n" );
						sourceFile.append( "{\n" );
						sourceFile.append( "	_logger.info( \"Vertex: " + (String)vertex.getUserDatum( LABEL_KEY ) + "\" );\n" );
						sourceFile.append( "	throw new RuntimeException( \"Not implemented. This line can be removed.\" );\n" );
						sourceFile.append( "}\n\n" );
					}
				}

				writtenVertices.add( (String)vertex.getUserDatum( LABEL_KEY ) );
			}

			for ( int i = 0; i < _edges.length; i++ )
			{
				DirectedSparseEdge edge = (DirectedSparseEdge)_edges[ i ];

				boolean duplicated = false;
				for ( Iterator iter = writtenEdges.iterator(); iter.hasNext(); )
				{
					String str = (String) iter.next();
					if ( str.equals( (String)edge.getUserDatum( LABEL_KEY ) ) == true )
					{
						duplicated = true;
						break;
					}
				}

				if ( duplicated == false )
				{
					Pattern p = Pattern.compile( "public void " + (String)edge.getUserDatum( LABEL_KEY ) + "\\(\\)(.|[\\n\\r])*?\\{(.|[\\n\\r])*?\\}", Pattern.MULTILINE );
					Matcher m = p.matcher( sourceFile );

					if ( m.find() == false )
					{
						sourceFile.append( "/**\n" );
						sourceFile.append( " * This method implemets the edge: " + (String)edge.getUserDatum( LABEL_KEY ) + "\n" );
						sourceFile.append( " */\n" );
						sourceFile.append( "public void " + (String)edge.getUserDatum( LABEL_KEY ) + "()\n" );
						sourceFile.append( "{\n" );
						sourceFile.append( "	_logger.info( \"Edge: " + (String)edge.getUserDatum( LABEL_KEY ) + "\" );\n" );

						if ( edge.containsUserDatumKey( STATE_KEY ) )
						{
							HashMap map = (HashMap)edge.getUserDatum( STATE_KEY );
							Set variables = map.keySet();
							for ( Iterator iter = variables.iterator(); iter.hasNext();)
							{
								String  variable = (String) iter.next();
								Boolean value	 = (Boolean)map.get( variable );
								sourceFile.append( "	boolean " + variable + " = " + value + ";\n" );
							}
						}

						if ( edge.containsUserDatumKey( CONDITION_KEY ) )
						{
							HashMap map = (HashMap)edge.getUserDatum( CONDITION_KEY );
							Set variables = map.keySet();
							for ( Iterator iter = variables.iterator(); iter.hasNext();)
							{
								String  variable = (String) iter.next();
								Boolean value	 = (Boolean)map.get( variable );
								sourceFile.append( "	if ( " + variable + " != " + value + " )\n" );
								sourceFile.append( "	{\n" );
								sourceFile.append( "	  _logger.info( \"Not a valid path until condition is fullfilled\" );\n" );
								sourceFile.append( "	  throw new GoBackToPreviousVertexException();\n" );
								sourceFile.append( "	}\n" );
							}
						}

						if ( edge.containsUserDatumKey( VARIABLE_KEY ) )
						{
							HashMap map = (HashMap)edge.getUserDatum( VARIABLE_KEY );
							Set variables = map.keySet();
							for ( Iterator iter = variables.iterator(); iter.hasNext();)
							{
								String variable = (String) iter.next();
								Object object = map.get( variable );
								if ( object == null )
								{
									throw new RuntimeException( "An object in the hash map was null!" );
								}
								if ( object instanceof String )
								{
									String value  = (String)map.get( variable );
									sourceFile.append( "	String " + variable + " = \"" + value + "\";\n" );
								}
								if ( object instanceof Integer )
								{
									Integer value = (Integer)map.get( variable );
									sourceFile.append( "	String " + variable + " = " + value + ";\n" );
								}
								if ( object instanceof Float )
								{
									Float value	= (Float)map.get( variable );
									sourceFile.append( "	String " + variable + " = " + value + ";\n" );
								}
							}
						}

						sourceFile.append( "	throw new RuntimeException( \"Not implemented\" );\n" );
						sourceFile.append( "}\n\n" );
					}
				}

				writtenEdges.add( (String)edge.getUserDatum( LABEL_KEY ) );
			}
			file.write( sourceFile.toString() );
			file.flush();
		}
		catch ( IOException e )
		{
			_logger.error( e.getMessage() );
		}
	}

	private void findStartingVertex()
	{
		_vertices = _graph.getVertices().toArray();
		_logger.info( "Number of vertices = " + _vertices.length );

		_edges = _graph.getEdges().toArray();
		_logger.info( "Number of edges = " + _edges.length );


		_nextVertex = null;
		for ( int i = 0; i < _vertices.length; i++ )
		{
			DirectedSparseVertex vertex = (DirectedSparseVertex)_vertices[ i ];

			// Find Start vertex
			if ( vertex.getUserDatum( LABEL_KEY ).equals( START_NODE ) )
			{
				_nextVertex = vertex;
				vertex.setUserDatum( VISITED_KEY, new Integer( 1 ), UserData.SHARED );
				break;
			}
		}

		if ( _nextVertex == null )
		{
			String msg = "Did not found the starting vertex in the graph.";
			_logger.error( msg );
			throw new RuntimeException( msg );
		}
	}

	private void executeMethod( boolean optimize ) throws FoundNoEdgeException
	{
		DirectedSparseEdge edge 	= null;
		Object[] 		   outEdges = null;

		if ( _nextVertex.containsUserDatumKey( BACK_KEY ) && _history. size() >= 3 )
		{
			Float probability = (Float)_nextVertex.getUserDatum( BACK_KEY );
			int index = _radomGenerator.nextInt( 100 );
			if ( index < ( probability.floatValue() * 100 ) )
			{
				String str =  (String)_history.removeLast();
				_logger.debug( "Remove from history: " + str );
				String  nodeLabel = (String)_history.getLast();
				_logger.debug( "Reversing a vertex. From: " + (String)_nextVertex.getUserDatum( LABEL_KEY ) + ", to: " + nodeLabel );

				Object[] vertices = _graph.getVertices().toArray();
				for ( int i = 0; i < vertices.length; i++ )
				{
					DirectedSparseVertex vertex = (DirectedSparseVertex)vertices[ i ];
					if ( nodeLabel == (String)vertex.getUserDatum( LABEL_KEY ) )
					{
						try
						{
							_nextVertex = vertex;
							String label = "PressBackButton";
							_logger.debug( "Invoke method for edge: \"" + label + "\"" );
							invokeMethod( label );

							label = nodeLabel;
							_logger.debug( "Invoke method for vertex: \"" + label + "\"" );
							invokeMethod( label );
						}
						catch( GoBackToPreviousVertexException e )
						{
							throw new RuntimeException( "An GoBackToPreviousVertexException was thrown where it should not be thrown." );
						}

						return;
					}
				}
				throw new RuntimeException( "An attempt was made to reverse to vertex: " + nodeLabel + ", and did not find it." );
			}
		}


		_logger.debug( "Vertex = " + (String)_nextVertex.getUserDatum( LABEL_KEY ) );

		outEdges = _nextVertex.getOutEdges().toArray();
		_logger.debug( "Number of outgoing edges = " + outEdges.length );

		outEdges = shuffle( outEdges );

		if ( _shortestPathToVertex == null && _runUntilAllEdgesVisited == true )
		{
			Vector unvisitedEdges = returnUnvisitedEdge();
			if ( unvisitedEdges.size() == 0)
			{
				_logger.debug( "All edges has been visited!" );
				return;
			}
			_logger.info( "Found " + unvisitedEdges.size() + " unvisited edges." );


			Object[] shuffledList = shuffle( unvisitedEdges.toArray() );
			DirectedSparseEdge e = (DirectedSparseEdge)shuffledList[ 0 ];
			if ( e == null )
			{
				throw new RuntimeException( "Found an empty edge!" );
			}
			_logger.info( "Selecting edge: " + getCompleteEdgeName( e ) );
			_shortestPathToVertex = new DijkstraShortestPath( _graph ).getPath( _nextVertex, e.getSource() );
			_shortestPathToVertex.add( e );
			_logger.info( "Intend to take the shortest path between: " + (String)_nextVertex.getUserDatum( LABEL_KEY ) + " ==> " + (String)e.getDest().getUserDatum( LABEL_KEY ) + " (from: " + (String)e.getSource().getUserDatum( LABEL_KEY ) + "), using " + _shortestPathToVertex.size() + " hops." );

			String paths = "";
			for (Iterator iter = _shortestPathToVertex.iterator(); iter.hasNext();)
			{
				DirectedSparseEdge item = (DirectedSparseEdge) iter.next();
				paths += " ==> " + getCompleteEdgeName( item );
			}
			_logger.info( paths );
		}

		if ( _shortestPathToVertex != null && _shortestPathToVertex.size() > 0 )
		{
			edge = (DirectedSparseEdge)_shortestPathToVertex.get( 0 );
			_shortestPathToVertex.remove( 0 );
			_logger.debug( "Removed edge: " + getCompleteEdgeName( edge ) + " from the shortest path list, " + _shortestPathToVertex.size() + " hops remaining." );

			if ( _shortestPathToVertex.size() == 0 )
			{
				_shortestPathToVertex = null;
			}
		}
		else if ( optimize )
		{
			// Look for an edge that has not been visited yet.
			for ( int i = 0; i < outEdges.length; i++ )
			{
				edge = (DirectedSparseEdge)outEdges[ i ];

				Integer vistited = (Integer)edge.getUserDatum( VISITED_KEY );
				if ( vistited.intValue() == 0 )
				{
					if ( _rejectedEdge == edge )
					{
						// This edge has been rejected, because some condition was not fullfilled.
						// Try with the next edge in the for-loop.
						// _rejectedEdge has to be set to null, because it can be valid next time.
						_rejectedEdge = null;
					}
					else
					{
						_logger.debug( "Found an edge which has not been visited yet: " + getCompleteEdgeName( edge ) );
						break;
					}
				}
				edge = null;
			}
			if ( edge == null )
			{
				_logger.debug( "All edges has been visited (" + outEdges.length + ")" );
				edge = getWeightedEdge( _nextVertex );
			}
		}
		else
		{
			edge = getWeightedEdge( _nextVertex );
		}

		if ( edge == null  )
		{
			throw new RuntimeException( "Did not find any edge." );
		}
		_logger.debug( "Edge = \"" + getCompleteEdgeName( edge ) + "\"" );

		_prevVertex = _nextVertex;
		_nextVertex = (DirectedSparseVertex)edge.getDest();

		try
		{
			String label = (String)edge.getUserDatum( LABEL_KEY );
			_logger.debug( "Invoke method for edge: \"" + label + "\"" );
			invokeMethod( label );
			Integer vistited = (Integer)edge.getUserDatum( VISITED_KEY );
			vistited = new Integer( vistited.intValue() + 1 );
			edge.setUserDatum( VISITED_KEY, vistited, UserData.SHARED );

			label = (String)edge.getDest().getUserDatum( LABEL_KEY );
			_logger.debug( "Invoke method for vertex: \"" + label + "\"" );
			invokeMethod( label );
			vistited = (Integer)edge.getDest().getUserDatum( VISITED_KEY );
			vistited = new Integer( vistited.intValue() + 1 );
			edge.getDest().setUserDatum( VISITED_KEY, vistited, UserData.SHARED );

			if ( ((String)edge.getDest().getUserDatum( LABEL_KEY )).equals( "Stop" ) )
			{
				_logger.debug( "Clearing the history" );
				_history.clear();
			}
			if ( edge.containsUserDatumKey( NO_HISTORY ) == false )
			{
				_logger.debug( "Add to history: " +  getCompleteEdgeName( edge ) );
				_history.add( (String)edge.getDest().getUserDatum( LABEL_KEY ) );
			}
		}
		catch( GoBackToPreviousVertexException e )
		{
			_logger.debug( "The edge: " + getCompleteEdgeName( edge ) + " can not be run due to unfullfilled conditions." );
			_logger.debug( "Trying from vertex: " + (String)_prevVertex.getUserDatum( LABEL_KEY ) + " again." );
			_rejectedEdge = edge;
			_nextVertex   = _prevVertex;
		}
	}

	private void invokeMethod( String method ) throws GoBackToPreviousVertexException
	{
		Class cls = _object.getClass();

		try
		{
			if ( method.compareTo( "" ) != 0 )
			{
				Method meth = cls.getMethod( method, null );
				meth.invoke( _object, null  );
			}
		}
		catch( NoSuchMethodException e )
		{
			_logger.error( e );
			_logger.error( "Try to invoke method: " + method );
			throw new RuntimeException( "The methoden is not defined: " + method );
		}
		catch( java.lang.reflect.InvocationTargetException e )
		{
			if ( e.getTargetException().getClass() == GoBackToPreviousVertexException.class )
			{
				throw new GoBackToPreviousVertexException();
			}

			_logger.error( e.getCause().getMessage() );
			e.getCause().printStackTrace();
			throw new RuntimeException( e.getCause().getMessage() );
		}
		catch( Exception e )
		{
			_logger.error( e );
			e.printStackTrace();
			throw new RuntimeException( "Abrupt end of execution: " + e.getMessage() );
		}
	}

	/**
	 * Returns a random edge from the vertex's list of outgoing edges.
	 * If any edge is weighted, this will be taken in consideration.
	 *
	 * @param DirectedSparseVertex
	 * @return DirectedSparseEdge
	 */
	DirectedSparseEdge getWeightedEdge( DirectedSparseVertex vertex ) throws FoundNoEdgeException
	{
		Object[] edges = vertex.getOutEdges().toArray();
		DirectedSparseEdge edge = null;
		float probabilities[]   = new float[ edges.length ];
		int   numberOfZeros     = 0;
		float sum               = 0;

		for ( int i = 0; i < edges.length; i++ )
		{
			edge = (DirectedSparseEdge)edges[ i ];

			if ( edge.containsUserDatumKey( WEIGHT_KEY ) )
			{
				Float weight = (Float)edge.getUserDatum( WEIGHT_KEY );
				probabilities[ i ] = weight.floatValue();
				sum += probabilities[ i ];
			}
			else
			{
				numberOfZeros++;
				probabilities[ i ] = 0;
			}
		}

		if ( sum > 1 )
		{
			throw new RuntimeException( "The sum of all weight from vertex: " + (String)vertex.getUserDatum( LABEL_KEY ) + " adds to more than 1" );
		}

		float rest = ( 1 - sum ) / numberOfZeros;
		int index = _radomGenerator.nextInt( 100 );
		_logger.debug( "Randomized integer index = " + index );

		float weight = 0;
		for ( int i = 0; i < edges.length; i++ )
		{
			if ( probabilities[ i ] == 0 )
			{
				probabilities[ i ] = rest;
			}
			_logger.debug( "The edge: " + (String)((DirectedSparseEdge)edges[ i ]).getUserDatum( LABEL_KEY ) + " is given the probability of " + probabilities[ i ] * 100 + "%"  );

			weight = weight + probabilities[ i ] * 100;
			_logger.debug( "Current weight is: " + weight  );
			if ( index < weight )
			{
				edge = (DirectedSparseEdge)edges[ i ];
				_logger.debug( "Selected edge is: " + getCompleteEdgeName( edge ) );
				break;
			}
		}

		if ( edge == null )
		{
			_logger.error( "Vertex: " + (String)vertex.getUserDatum( LABEL_KEY ) + ", has no out edges. Test ends here!" );
			throw new FoundNoEdgeException();
		}

		return edge;
	}

	/**
	 * This functions shuffle the array, and returns the shuffled array
	 * @param array
	 * @return
	 */
	private Object[] shuffle( Object[] array )
	{
		for ( int i = 0; i < array.length; i++ )
		{
			Object leftObject = array[ i ];
			int index = _radomGenerator.nextInt( array.length );
			Object rightObject = array[ index ];

			array[ i ]     = rightObject;
			array[ index ] = leftObject;
		}
		return array;
	}



	/**
	 * This functions returns a list of edges, which has not yet been visited
	 * @return DirectedSparseEdge
	 */
	private Vector returnUnvisitedEdge()
	{
		Vector edgesNotVisited = new Vector();

		for ( int i = 0; i < _edges.length; i++ )
		{
			DirectedSparseEdge edge = (DirectedSparseEdge)_edges[ i ];

			Integer vistited = (Integer)edge.getUserDatum( VISITED_KEY );
			if ( vistited.intValue() == 0 )
			{
				edgesNotVisited.add( edge );
				_logger.debug( "Unvisited: " +  getCompleteEdgeName( edge ) );
			}
		}

		return edgesNotVisited;
	}

	private String getCompleteEdgeName( DirectedSparseEdge edge )
	{
		String str = (String)edge.getUserDatum( LABEL_KEY ) + " (" + (String)edge.getSource().getUserDatum( LABEL_KEY ) + " -> " + (String)edge.getDest().getUserDatum( LABEL_KEY ) + ") " + edge.hashCode() + "(" + edge.getSource().hashCode() + " -> " + edge.getDest().hashCode() + ")";
		return str;
	}
}