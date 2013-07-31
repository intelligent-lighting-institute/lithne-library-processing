
package ili.lithne;

import java.util.ArrayList;

import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;


public class NodeManager implements XBeeListener
//public class NodeManager
{
  private ArrayList<Node> _nodes  =  new ArrayList<Node>();
 
  public NodeManager()
  {
  }
  
  /**
   * Adds a new Node to the NodeManager with the specified 64-bit address
   * and specified name.
   * @param String - addr64
   * @param String - name
   */
  public void addNode( String addr64, String name)
  {
    this.getNodeList().add( new Node( new XBeeAddress64(addr64), name ) );
  }
  
  /**
   * Adds the specified node to the NodeManager
   * @param node
   */
  public void addNode( Node node )
  {
	  this.getNodeList().add( node );
  }
  
  /**
   * Returns a reference to the complete Node arraylist
   * @return ArrayList<Node>
   */
  public ArrayList<Node> getNodeList()
  {
	  return this._nodes;
  }
  
  /**
   * Provides the number of nodes currently in the Node Manager.
   * @return int
   */
  public int getNumberOfNodes()
  {
	  return this.getNodeList().size();
  }
 
  
  /**
   * Returns the Node at the given position in the NodeManager.
   * Returns null if this position does not exist.
   * Function is mainly used internally for Node finding.
   * @param position
   * @return Node
   */
  public Node getNodeAt( int position )
  {
	  Node node	=	null;
	  if( position >= 0 && position < this.getNumberOfNodes() )
	  {
		  node	=	this.getNodeList().get( position );
	  }
	  return node;
  }
  
  /**
   * Searches the nodes stored for the specific name and returns
   * this node if it matches. Otherwise, returns null
   * @param name
   * @return Node
   */
  public Node getNode( String name )
  {
    Node node = null;

    for (int i = 0; i < this.getNumberOfNodes(); i++)
    {
      if( this.getNodeAt(i).getName().equals( name ) )
      {
        node	=	this.getNodeAt(i);
        break;
      }
    }
    return node;
  }
  
  /**
   * Searches the NodeManager for the specified 64-bit address
   * and returns the corresponding Node.
   * If no matching address is found, returns null
   * @param addr64
   * @return Node
   */
  public Node getNode( XBeeAddress64 addr64 )
  {
    Node node = null;
    
    for (int i = 0; i < this.getNumberOfNodes(); i++)
    {
      if( this.getNodeAt(i).getXBeeAddress64().equals(addr64) ) 
      {
        node = this.getNodeAt(i);
        break;
      }
    }
    return node;
  }
  
  /**
   * Searches the NodeManager for a node with the specified 16-bit
   * address and returns it. If there is no such node, function 
   * returns null.
   * @param XBeeAddress16 - addr16
   * @return Node
   */
  public Node getNode( XBeeAddress16 addr16 )
  {
	  Node node	=	null;
	  for (int i = 0; i < this.getNumberOfNodes(); i++)
	    {
	      if( this.getNodeAt(i).getXBeeAddress16().equals(addr16) ) 
	      {
	        node = this.getNodeAt(i);
	        break;
	      }
	    }
	    return node;
  }
  
  /**
   * Searches the NodeManager for the specified ID and
   * returns the first node it finds. Returns null if
   * no matching Node is found.
   * @param int - id
   * @return Node
   */
  public Node getNode( int id )
  {
	  Node node	=	null;
	  for (int i = 0; i < this.getNumberOfNodes(); i++)
	    {
	      if( this.getNodeAt(i).getID() == id ) 
	      {
	        node = this.getNodeAt(i);
	        break;
	      }
	    }
	    return node;
  }
  
  /**
   * Clears all the nodes in the node manager
   */
  public void clear()
  {
	  this._nodes.clear();
  }
  
  /**
   * Clears all the nodes in this manager. Equal to NodeManager.clear()
   */
  public void removeAllNodes()
  {
	  this.clear();
  }
  
  /**
   * Removes the Node at the specific position from the NodeManager
   * @param position
   */
  public void removeNode ( int position )
  {
    if( position >= 0 && position < this.getNumberOfNodes() )
    {
      this.getNodeList().remove( position );
    }
  }
  
  /**
   * Removes the Node with the specified name (if it is found) from the NodeManager
   * @param name
   */
  public void removeNode( String name )
  {
	  for (int i = 0; i < this.getNumberOfNodes(); i++)
	  {
		  if( this.getNodeAt(i).getName().equals(name) )
		  {
			  this.removeNode( i );
			  break;
		  }
	  }
  }
  
  /**
   * Searches all the nodes for the request 64-bit address and
   * returns the corresponding 16-bit address
   * @param _addr64
   * @return
   */
  public XBeeAddress16 getXBeeAddress16( XBeeAddress64 _addr64 )
  {
    XBeeAddress16 addr16 = Lithne.UNKNOWN_16;
    for (int i = 0; i < this.getNumberOfNodes(); i++) 
    {
      if ( this.getNodeAt(i).getXBeeAddress64().equals(_addr64) ) 
      {
        addr16 = this.getNodeAt(i).getXBeeAddress16();
        break;
      }
    }
    return addr16;
  }
 
  /**
   * Searches the NodeManager for the specified name
   * and returns the corresponding 64-bit address.
   * @param name
   * @return XBeeAddress64
   */
  public XBeeAddress64 getXBeeAddress64( String name )
  {
    XBeeAddress64 addr64 = Node.UNKNOWN_64;
    for (int i = 0; i < this.getNumberOfNodes(); i++) 
    {
      if ( this.getNodeAt(i).getName().equals(name) )
      {
        addr64 = this.getNodeAt(i).getXBeeAddress64();
        break;
      }
    }
    return addr64;
  }
 
  /**
   * Searches the NodeManager for the specified 64-bit address
   * and returns the corresponding name.
   * Returns Node.UNKNOWN_NAME if no Node is found.
   * @param addr64
   * @return
   */
  public String getNodeName( XBeeAddress64 addr64 )
  {
    String nodeName = Node.UNKNOWN_NAME;
    for (int i = 0; i < this.getNumberOfNodes(); i++)
    {
      if (this.getNodeAt(i).getXBeeAddress64().equals(addr64))
      {
        nodeName = this.getNodeAt(i).getName();
      }
    }
    return nodeName;
  }
 
  /**
   * Sets the name of the node with the given 64-bit address to the
   * specified value.
   * @param nodeName
   * @param sender
   */
  public void setNodeName( XBeeAddress64 addr64, String nodeName )
  {
	  for (int i = 0; i < this.getNumberOfNodes(); i++)
	  {
		  if( this.getNodeAt(i).getXBeeAddress64().equals( addr64) )
		  {
			  this.getNodeAt(i).setName(nodeName);
		  }
	  }
  }
 
  /**
   * Returns the Node ID, based on the provided name.
   * Returns Lithne.UNKNOWN_ID if no node with this ID is found.
   * @param name
   * @return int
   */
  public int getNodeID( String name )
  {
    int nodeId = Node.UNKNOWN_ID;
    //XBeeAddress64 n = new XBeeAddress64( s ) ;
    for (int i = 0; i < this.getNumberOfNodes(); i++)
    {
      if( this.getNodeAt(i).getName().equals(name) ) {
        nodeId = i;
      }
    }
    return nodeId;
  }
  
  
  /**
   * Converts the provided 64-bit address to an understandable format
   * @param XBeeAddress64
   * @return String
   * 			- Returns a string in the format "XX XX XX XX XX XX XX XX"
   */	
  public static String addressToString( XBeeAddress64 xbeeAddress64 )
  {
    String returnAddress  =  xbeeAddress64.toString();
    returnAddress  =  returnAddress.replace(",", " ");
    returnAddress  =  returnAddress.replace("0x", "");
    returnAddress  =  returnAddress.toUpperCase();
    return returnAddress;
  }
  
  
  /**
   * Converts the provided 16-bit address to an understandable format
   * @param XBeeAddress16
   * @return String
   * 			- Returns a string in the format "XX XX"
   */	

  public static String addressToString( XBeeAddress16 xbeeAddress16 )
  {
    String returnAddress  =  xbeeAddress16.toString();
    returnAddress  =  returnAddress.replace(",", " ");
    returnAddress  =  returnAddress.replace("0x", "");
    returnAddress  =  returnAddress.toUpperCase();
    return returnAddress;
  }
  
  /**
   * Handles all XBeeEvents received. If you register this
   * class as a listener to a Lithne object, it will automatically
   * attempt to maintain the correct information.
   */
  public void xbeeEventReceived( XBeeEvent event )
  {
	  
  }
  
  
}