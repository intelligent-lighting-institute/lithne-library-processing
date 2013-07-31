package ili.lithne;

import processing.core.*;
import processing.xml.*;

import java.util.ArrayList;
import java.util.Iterator;

import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;

public class Node implements XBeeListener, LithneListener
{
	public static int RESET_DELAY					=	10000;
	public static int UNKNOWN_ID					=	0xFE;
	public static final String UNKNOWN_NAME 		= 	"Unnamed Node";
	public static final XBeeAddress16 UNKNOWN_16	=	new XBeeAddress16( 0x0, 0xFE );	
	public static final XBeeAddress64 UNKNOWN_64	=	new XBeeAddress64( "00 00 00 00 00 00 00 FE");
	public static final XBeeAddress64 BROADCAST		=	new XBeeAddress64( "00 00 00 00 00 00 FF FF");
	public static final XBeeAddress64 COORDINATOR	=	new XBeeAddress64( "00 00 00 00 00 00 00 00");
	
	private XBeeAddress16 _xbeeAddress16	=	UNKNOWN_16;
	private XBeeAddress64 _xbeeAddress64	=	UNKNOWN_64;
	private int _rssi						=	0;			  //Initialize the signal strength at 0
	private int _lastContact				=	0;
	private int _id							=	UNKNOWN_ID;	  //Initialize the id of the node as UNKNOWN 
	private String _name					=	UNKNOWN_NAME; //Initialize the name of the node as unknown
	private int _fails						=	0;
	private boolean _offline				=	false;		  //Indicates whether this node is offline (not present in the network)
	
	private int _lastReset					=	0;
	
	private ArrayList<NodeListener> _nodeListeners  		=  new ArrayList<NodeListener>();
	
	public Node(){}
	public Node( XBeeAddress64 xbeeAddress64 )
	{
		this.setXBeeAddress64(xbeeAddress64);
	}
	public Node( String xbeeAddress64 )
	{
		this.setXBeeAddress64( xbeeAddress64 );
	}
	public Node ( XBeeAddress64 xbeeAddress64, XBeeAddress16 xbeeAddress16 )
	{
		this.setXBeeAddress64(xbeeAddress64);
		this.setXBeeAddress16(xbeeAddress16);
	}
	public Node ( XBeeAddress64 xbeeAddress64, XBeeAddress16 xbeeAddress16, String name )
	{
		this.setXBeeAddress64(xbeeAddress64);
		this.setXBeeAddress16(xbeeAddress16);
		this.setName(name);
	}
	public Node ( XBeeAddress64 xbeeAddress64, XBeeAddress16 xbeeAddress16, String name, int id )
	{
		this.setXBeeAddress64(xbeeAddress64);
		this.setXBeeAddress16(xbeeAddress16);
		this.setName(name);
		this.setID(id);
	}
	public Node ( XBeeAddress64 xbeeAddress64, String name )
	{
		this.setXBeeAddress64(xbeeAddress64);
		this.setName(name);
	}
	public Node ( XBeeAddress64 xbeeAddress64, String name, int id )
	{
		this.setXBeeAddress64(xbeeAddress64);
		this.setName(name);
		this.setID(id);
	}
	public Node ( XMLElement node )
	{
		String xbeeAddress64	=	node.getString("XBeeAddress64");
		String xbeeAddress16	=	node.getString("XBeeAddress16");
		this.setName( node.getString("name", Node.UNKNOWN_NAME) );
		this.setID( node.getInt("id", Node.UNKNOWN_ID) );
		
		if( xbeeAddress64 != null )
		{
			this.setXBeeAddress64( new XBeeAddress64(xbeeAddress64) );
		}
		if( xbeeAddress16 != null )
		{
			this.setXBeeAddress16(xbeeAddress16);
		}
		
	}
	
	/**
	 * Returns the information of this node as an XMLElement. Easy to store somewhere or pass around.
	 * The format is <Node XBeeAddres64="XX XX XX XX XX XX XX XX" XBeeAddress16="XX XX" name="name" id="XX" />
	 * @return XMLElement
	 */
	public XMLElement getAsXML()
	{
		XMLElement xml	=	new XMLElement("Node");
		xml.setString("XBeeAddress64", Lithne.addressToString( this.getXBeeAddress64() ) );
		xml.setString("XBeeAddress16", Lithne.addressToString( this.getXBeeAddress16() ) );
		xml.setString("name", this.getName() );
		xml.setInt("id", this.getID() );
		return xml;
	}
	
	/**
	 * Returns the identifier of this node. Standard set to UNKOWN_ID (0xFE or 254)  
	 * @return int
	 */
	public int getID()
	{
		return this._id;
	}
	/**
	 * @param _id
	 *          the desired identifier between 0-255 (254 is reserved for UNKNOWN_ID)
	 * 
	 * Sets the identifier of the node to the specified id
	 * 
	 * @return boolean
	 */
	public boolean setID( String _id )
	{
		return this.setID( Integer.parseInt(_id) );
	}
	/**
	 * Sets the ID to the specified value. Converts the float to an int.
	 * @param float
	 * @return boolean
	 */
	public boolean setID( float id )
	{
		return this.setID( (int) id );
	}
	/**
	 * Sets the identifier of the node to the specified id. Should be between (0-255, excluding 254)
	 * 
	 * @param _id
	 *          the desired identifier between 0-255 (254 is reserved for UNKNOWN_ID)
	 * @return boolean
	 * 			Returns true if the node identifier was changed succesfully
	 */
	public boolean setID( int _id )
	{
		boolean success  =  false;
		if( _id >= 0 && _id <= 255 && _id != UNKNOWN_ID )  //This is limited by the storage in the Lithne nodes
		{
			this._id  =  _id;
			success 	=  true;
		}
		return success;
	}
	
	/**
	 * Returns the number of failed messages to this Node
	 * @return int
	 */
	public int getFailCount()
	{
		return this._fails;
	}
	
	/**
	 * Sets the number of failed messages to the specified integer value.
	 * Returns the new fail count
	 * @param int
	 * @return int
	 */
	public int setFailCount( int count )
	{
		return this._fails	=	count;
	}
	
	/**
	 * Resets the number of failed messages to this Node to 0
	 * @return
	 */
	public int resetFailCount()
	{
		return this._fails	=	0;
	}
	
	/**
	 * Indicates whether the node is online
	 * @return boolean
	 */
	public boolean isOnline()
	{
		return !this._offline;
	}
	
	/**
	 * Indicates whether the node is offline
	 * @return boolean
	 */
	public boolean isOffline()
	{
		return this._offline;
	}
	
	/**
	 * Toggles the state of the node to the specified state and returns the new state
	 * @param state
	 * @return boolean
	 */
	public boolean setOffline( boolean state )
	{
		return this._offline	=	state;
	}
	
	/**
	 * Returns the last millis() when there was contact with the node (either a successful delivery, or a message) 
	 * @return int
	 */
	public int lastContactAt()
	{
		return this._lastContact;
	}
	
	/**
	 * Returns the last millis() when there was contact with the node (either a successful delivery, or a message) 
	 * @return int
	 */
	public int lastContactAt( int lastContact )
	{
		return this._lastContact	=	lastContact;
	}
	
	/**
	 * (Not operational at the moment). Returns the last know RSSI value. 
	 * @return int
	 */
	public int getRSSI()
	{
		return this._rssi;
	}
	
	/**
	 * Returns the name of this node. Standard set to "Unnamed Node"
	 * @return String
	 */
	public String getName()
	{
		return this._name;
	}
	/**
	 * Sets the name of this node to the specified String. Checks whether the name is not null, an empty String or "Unnamed Node". Returns 'true' if name changed succesfully.
	 * @param _name
	 * @return boolean
	 */
	public boolean setName( String _name )
	{
//	    this.traceln("SETNAME( STRING ): Changing name from "+this.getName()+" to "+_name, 2);
	    boolean success  =  false;
	    if( _name != null && !_name.equals("") &&  !_name.equals(UNKNOWN_NAME) )
	    {
//	      this.traceln("SETNAME( STRING ): Setting name of Node to "+_name, 3);
	      this._name  	=  _name;
	      success 		=  true;
	    }
	    return success;
	} 
	/**
	 * Returns the 64-bit address (Hardware Address) of the node.
	 * @return
	 */
	public XBeeAddress64 getXBeeAddress64() 
	{
		return this._xbeeAddress64;
	}
	/**
	 * Sets the 64-bit address of this node to the specified value
	 * @param _xbeeAddress64
	 */
	public void setXBeeAddress64( XBeeAddress64 xbeeAddress64 ) 
	{
		if( xbeeAddress64 != null )
		{
			this._xbeeAddress64 = xbeeAddress64;
		}
	}
	
	public void setXBeeAddress64( String xbeeAddress64 )
	{
		if( xbeeAddress64 != null )
		{
			try
			{
				this._xbeeAddress64 = new XBeeAddress64( xbeeAddress64 );
			}
			catch( Exception e )
			{
				System.err.println("[ERROR]. Incorrect format of the XBeeAddress64. Please use the format 'XX XX XX XX XX XX XX XX XX'.");
			}
		}
	}
	
	/**
	 * Returns the current 16-bit network address of this node
	 * @return XBeeAddress16
	 */
	public XBeeAddress16 getXBeeAddress16() 
	{
		return this._xbeeAddress16;
	}
	/**
	 * Resets the 16-bit address of this node to an UNKNOWN_16 value.
	 */
	public void resetXBeeAddress16()
	{
		System.out.print("Resetting XBeeAddress of "+this.getXBeeAddress16());
		this.setXBeeAddress16( Node.UNKNOWN_16 );
		System.out.println(" to "+this.getXBeeAddress16());
	}
	/**
	 * Sets the 16-bit network address of this node to the specified XBeeAddress16
	 * @param XBeeAddress16
	 */
	public boolean setXBeeAddress16( XBeeAddress16 xbeeAddress16 ) 
	{
		boolean success	=	false;
		if( xbeeAddress16 != null )
		{
			this._xbeeAddress16 = xbeeAddress16;
			success	=	true;
		}
		return success;
	}
	/**
	 * Attempts to set the 16-bit network address of this node to the specified values. The first value should represent the most significant byte (MSB) the second the least significant byte (LSB)
	 * @param int
	 * @param int
	 * @return boolean
	 * 			Returns true if the address has successfully been updated.
	 */
	public boolean setXBeeAddress16( int msb, int lsb )
	{
		boolean success	=	false;
		XBeeAddress16 temp	=	null;
		try
		{
			temp	=	new XBeeAddress16( msb, lsb );
			success	=	true;
		}
		catch( Exception e)
		{
			success	=	false;
		}
		
		if( success )
		{
			this.setXBeeAddress16( temp );
		}
		
		return success;
	}
	/**
	 * Sets the 16-bit network address of this node to the specified string. The string can have the format "XX XX" or "0xXX,0xXX".
	 * @param XBeeAddress16
	 * @return boolean
	 * 			Returns true if the 16-bit address has been updated succesfully.
	 */
	public boolean setXBeeAddress16( String xbeeAddress16 )
	{
	    boolean success  =  false;
	    XBeeAddress16 temp	=	null;
	    
	    xbeeAddress16  =  PApplet.trim( xbeeAddress16 );    //First, we remove any erroneous spaces from the String
	    xbeeAddress16.replaceAll("0x", "");      //Then, we replace all 0x, by nothing, thus removing it
	    String[] splitAddress  =  PApplet.splitTokens( xbeeAddress16, ", ");  //Then, we split the String into pieces by either spaces or commas

	    if( splitAddress.length == 1 )
	    {
	    	int complete  =  PApplet.unhex( splitAddress[0] );
		      try
		      {
		    	temp 	 =  new XBeeAddress16( (complete>>8) & 0xFF, complete & 0xFF);
		        success  =  true;
		      }
		      catch( Exception e ){ success  =  false; }
	      
	    }
	    else if( splitAddress.length == 2 )
	    {
	    	int msb  =  PApplet.unhex( splitAddress[0] );
			int lsb  =  PApplet.unhex( splitAddress[1] );
			try
			{
				temp  	=  new XBeeAddress16(msb, lsb);
				success =  true;
			}
			catch( Exception e ){ success  =  false; }
	    }
	    
	    if( success )
	    {
	    	this.setXBeeAddress16( temp );
	    }
	    
	    return success;
	}
	
	
	
	/**
	 * Returns the information of this node as a String
	 * @return	String 
	 */
	public String toString()
	{
		String nodeToString	=	"(type: Node)";
		nodeToString		+=	" [64: "+this.getXBeeAddress64().toString()+"]";
		nodeToString		+=	" [16: "+this.getXBeeAddress16().toString()+"]";
		nodeToString		+=	" [name: '"+this.getName()+"']";
		nodeToString		+=	" [id: "+this.getID()+"]";
		nodeToString		+=	" [last contact: "+this.lastContactAt()+"]";
		
		return nodeToString;
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
	   * Checks whether the specified 64-bit and 16-bit address equal the information
	   * of this node. This also takes into account whether broadcast or unknown addressess
	   * are specified and corrects for this.
	   * @param xb64
	   * @return
	   */
	  public boolean equals( XBeeAddress64 xb64, XBeeAddress16 xb16 )
	  {
		  //We start out with stating that both are equal and are going to describe
		  //when the addresses are not equal in the remainder of this function
		  boolean isEqual	=	false;
		  
		  //If the 64-bit address we are comparing against is not UNKNOWN,
		  //we can compare the 64-bit addresses
		  if( !xb64.equals(Lithne.UNKNOWN_64) )
		  {
			  //1. If the 64 bit addresses are equal, the nodes are similar (64-bit is unique)
			  if( this.getXBeeAddress64().equals(xb64) ) isEqual =	true;
			  //2. If our own 64-bit is unknown, but the 16-bit addresses are equal, this is the same node
			  else if( this.getXBeeAddress64().equals(Lithne.UNKNOWN_64) && 
					   this.getXBeeAddress16().equals(xb16) ) isEqual	=	true;
		  }
		  //Otherwise, if the 64-bit address we compare against is UNKNOWN,
		  //There are still some conditions in which the addresses might be similar
		  else if( !this.getXBeeAddress16().equals(Lithne.UNKNOWN_16) )
		  {
			  if( this.getXBeeAddress16().equals(xb16) ) isEqual	=	true;
		  }
		  
//		  System.out.println("Compared ("+xb64.toString()+" vs "+this.getXBeeAddress64()+" and ("+xb16.toString()+" vs. "+this.getXBeeAddress16()+"), result is "+isEqual);
			  
		  return isEqual;
	  }
	
	//	EVENT HANDLING
	
	/**
	 * This function is called by the Lithne class whenever something occurs on the XBee line.
	 * @param event
	 */
	public void xbeeEventReceived( XBeeEvent event )
	{
		/* First we check whether this is not a broadcast node, or the event is not passed by a broadcast node.
		 * Then we check whether the 64-bit XBeeAddress or 16-bit XBeeAddresses are similar. If so, this event is for us.
		 */
				//If this address or the event address is not a broadcast address
		if( this.equals( event.getXBeeAddress64(), event.getXBeeAddress16() ) )
		{
			//If this is a node identification event, we update our internal 16-bit address
			if( event.getEventType() == XBeeEvent.NODE_IDENTIFICATION )
			{
//				System.out.println("Received NODE_IDENTIFICATION for "+event.getXBeeAddress64()+". Updating 16-bit from"+this.getXBeeAddress16()+" to "+event.getXBeeAddress16());
				this.setXBeeAddress16( event.getXBeeAddress16() );
			}
			//If we receive a message that the delivery has failed to an address that resembles my address, I reset my 16-bit address
			else if( event.getEventType() == XBeeEvent.DELIVERY_FAILED )
			{
				//If the XBeeAddress16 is not UNKNOWN and we have a failed message, we reset the 16-bit address. 
				//If there is an incorrect 16-bit address messages will never arrive
				if( !this.getXBeeAddress16().equals(Lithne.UNKNOWN_16) ) this.resetXBeeAddress16();
//				this.setXBeeAddress16( Node.UNKNOWN_16 );
				this.setFailCount( this.getFailCount()+1 );
//				System.err.println("[NODE ("+this.getXBeeAddress64().toString()+")] [ERROR]: Delivery to "+this.getXBeeAddress64()+"/"+this.getXBeeAddress16()+" failed");
//				if( event.getLithne().getParent().millis() - this._lastReset >= Node.RESET_DELAY )
//				{
					//In this case, we also want to rediscover our 16-bit address. We use this opportunity to transmit a node discover via the Lithne reference
//					event.getLithne().requestNodeInfo( this.getXBeeAddress64() );
//					event.getLithne().discoverNodes();	/*TEMPORARY FIX!*/
//				}
			}
			else if( event.getEventType() == XBeeEvent.DELIVERY_SUCCESS )
			{
				//When messages arrive succesfully to this node, we can reset the fail count
				this.resetFailCount();
				this.setOffline( false );
			}
			
			this.lastContactAt( event.getLithne().getParent().millis() );
			
//			System.out.println("Received XBeeEvent for me!\n"+this.toString());
		}
	}
	
	
	
	
	/**
	 * Returns the listeners to this Node's events
	 * @return
	 */
	public ArrayList<NodeListener> getNodeListeners()
	{
	  return this._nodeListeners;
	}
	/**
	 * Add the specific object as a NodeListener to this node. The object must implement the NodeListener interface
	 * @param listener
	 */
	public void addNodeListener( NodeListener listener )
	{
	  if( !this.getNodeListeners().contains( listener ) )
	  {
	    this.getNodeListeners().add( listener );
	  }
	}
	/**
	 * Removes the specific node listener from the list, and thus unregister for receiving events from this node
	 * @param listener
	 */
	public void removeNodeListener( NodeListener listener )
	{
	    this.getNodeListeners().remove( listener );
	}
	/**
	 * Throws an event from this node to all its listeners
	 * @param int
	 */
	private synchronized void fireEvent( int eventType )
	{
	  NodeEvent event = new NodeEvent( this, eventType );
	  Iterator<NodeListener> listeners = this.getNodeListeners().iterator();
	  while( listeners.hasNext () ) 
	  {
	    ( (NodeListener) listeners.next() ).nodeEventReceived( event );
	  }
	}
	
	
	public void lithneEventReceived(LithneEvent event) 
	{
		//Here we have to handle lithne events
	}	
}
