package ili.lithne;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

import org.apache.log4j.PropertyConfigurator;

import com.rapplogic.xbee.api.ApiId;
//import com.rapplogic.xbee.api.*;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.RemoteAtRequest;
import com.rapplogic.xbee.api.RemoteAtResponse;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.XBeeTimeoutException;
import com.rapplogic.xbee.api.zigbee.ZNetNodeIdentificationResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxBaseResponse.Option;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;
import com.rapplogic.xbee.api.zigbee.ZNetTxStatusResponse;

import processing.core.PApplet;
import processing.serial.Serial;

public class Lithne implements LithneListener 
{
	/*	Static variables	*/
	public static final int STD_DEBUG_DEPTH			=	3;
	public static final int UNKNOWN_ID				=	0xFE;
	public static final XBeeAddress16 UNKNOWN_16	=	new XBeeAddress16( 0x0, 0xFE );	
	public static final XBeeAddress64 UNKNOWN_64	=	new XBeeAddress64( "00 00 00 00 00 00 00 FE");
	public static final XBeeAddress16 UNKNOWN_16B	=	new XBeeAddress16( 0x0, 0xFE );	
	public static final XBeeAddress64 UNKNOWN_64B	=	new XBeeAddress64( "00 00 00 00 00 00 00 FE");
	public static final XBeeAddress64 BROADCAST		=	new XBeeAddress64( "00 00 00 00 00 00 FF FF");
	public static final XBeeAddress64 COORDINATOR	=	new XBeeAddress64( "00 00 00 00 00 00 00 00");
	public static final int[] BAUDRATES  			=  { 2400, 4800, 9600, 19200, 38400, 57600, 115200 };
	
	
	private XBee _xbee;
	XBeeResponse response;
	Queue<XBeeResponse> queue = new ConcurrentLinkedQueue<XBeeResponse>();
	
	private String	_myName            	=  "LithneXBee";
	private String	_xbeePort           =  null;
	private int		_xbeeBaudrate       =  115200;
	
	private Message _incomingMessage			=	new Message();
	private Message _outgoingMessage			=	new Message();
	
	private ArrayList<Message> _inbox          	=  new ArrayList<Message>();  //This contains a list of message that are received
	private ArrayList<Message> _outbox         	=  new ArrayList<Message>();  //This contains a list of messages waiting to be send
	// Lists for the different event listeners
	private ArrayList<LithneListener> _lithneListeners		=  new ArrayList<LithneListener>();
	private ArrayList<MessageListener> _messageListeners	=  new ArrayList<MessageListener>();
	private ArrayList<NodeListener> _nodeListeners      	=  new ArrayList<NodeListener>();
	private ArrayList<XBeeListener> _xbeeListeners      	=  new ArrayList<XBeeListener>();
	
	private boolean _connectSuccess		=	false;
	private boolean _registered 		=	false;
	
	private PApplet _parent				=	null;
	
	private static boolean _debugMode	=	false;
	private static int _debugDepth		=	0;
	
//	private Serial xbeeSerial;
	
	/**
	 * Default constructor in which the reference to the parent sketch is specified
	 * @param parent
	 */
	public Lithne( PApplet parent )
	{
		this._parent	=	parent;
		this.traceln("Created Lithne object. Please initialize the XBee port and baudrate (standard: 115200).");
		this.register();
	}
	public Lithne( PApplet _parent, String _port )
	{
		this.setXBeePort( _port );
		this._parent  =  _parent;
		this.traceln("Created Lithne object on XBee port '"+this.getXBeePort()+"'. Please initialize baudrate (standard: 115200).");
		this.register();
	}
	public Lithne( PApplet _parent, String _port, int _baudrate )
	{
		this.setXBeePort( _port );
		this.setXBeeBaudrate( _baudrate );
		this._parent  =  _parent;
		this.traceln("Created Lithne object on XBee port '"+this.getXBeePort()+"' with baudrate "+this.getXBeeBaudrate()+".");
		
		this.register();
	}
	
	private void register()
	{
		if( !this._registered )
		{
	    	this.getParent().registerPre( this );
	    	this.getParent().registerDraw( this );
	    	this.getParent().registerPost( this );
	    	this._registered	=	true;
		}
	}
	
	private void unregister()
	{
		if( this._registered )
		{
			this.getParent().unregisterPre( this );
	    	this.getParent().unregisterDraw( this );
	    	this.getParent().unregisterPost( this );
	    	this._registered	=	false;
		}
	}
	
	public void pre()
	{
		//Here we can do some preparations, and/or sending
	    this.readXBee();  	//We first see if there are new messages available from our XBee
	    if( this.getMessagesInOutbox() > 0 ) this.cleanOutbox();	//Then we clean our outbox.
	}
	
	public void draw()
	{
	}
	
	public void post()
	{
		//At the end of the loop, we attempt to send all the messages that were created
		this.processOutbox();
	}
	
	  /*	META LEVEL STUFF	*/
	  /**
	   * Hashes the given word to our own nifty internal Lithne hasher
	   * @param word
	   * @return int
	   */
	  public static int hash( String word )
	  {
		  int wordValue  =  0;
		  for( int i=0; i < word.length(); i++ )
		  {
			  wordValue  +=  word.charAt(i) & 0xFF;
		  }
		  wordValue += word.length();
		  wordValue += word.charAt(0) & 0xFF;
		  wordValue += word.charAt( word.length() - 1 ) & 0xFF;
	    
		  return wordValue;
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
	   * Converts the provided. NOT IMPLEMENTED YET!
	   * @param xbeeAddress16
	   * @return XBeeAddress16
	   */
	  public static XBeeAddress16 stringToXBeeAddress16( String xbeeAddress16 )
	  {
		  XBeeAddress16 xbAdd	=	Lithne.UNKNOWN_16;
		  try
		  {
			  
		  }
		  catch( Exception e)
		  {
			  
		  }
		  
		  return xbAdd;
	  }
	
	  /**
	   * Returns the XBee object of this Lithne instance
	   * @return
	   */
	  private XBee getXBee()
	  {
		  return this._xbee;
	  }
	  
	/**
	 * Returns the reference to the main sketch
	 * @return PApplet
	 */
	public PApplet getParent()
	{
		return this._parent;
	}
	
	/**
	 * Sets the reference to the main sketch and returns the new reference
	 * @param parent
	 * @return
	 */
	private PApplet setParent( PApplet parent )
	{
		return this._parent	=	parent;
	}
	
	/**
	 * Returns the name of this object
	 * @return String
	 */
	public String getName()
	{
	  return this._myName;
	}
	
	/**
	 * Sets the name of this object to the specified name if it is not "" or null.
	 * @param String
	 */
	public void setName( String newName )
	{
	  if( !newName.equals("") || newName  ==  null )
	  {
	    this._myName  =  newName;
	  }
	}
	
	/*	XBEE CONNECTION */
	/**  
	 * Returns 'true' when a connection to the XBee is established, 'false if not
	 * @return booleans
     */
	public boolean isConnected()
	{
		boolean connected	=	false;
		if( this._xbee != null )
		{
			if( this._xbee.isConnected() && this._connectSuccess )
			{
				connected	=	true;
			}
		}
		return connected;
	}
	/**
	 * Returns the current baud rate at which the XBee communicates
	 * @return
	 */
	public int getXBeeBaudrate()
	{
	  return this._xbeeBaudrate;
	}
	/**  Updates the local baudrate to the specified baudrate  **/
	public boolean setXBeeBaudrate( int _baudrate )
	{
		boolean success = false;
		if( !this.isConnected() )
		{
			this._xbeeBaudrate  =  _baudrate;
			success	=	true;
		}
		this.fireEvent( LithneEvent.XBEE_BAUDRATE );
		return success;
	}
		
	/**  
	 * Returns the port the XBee is connected to. This can be 'null' if there is no connection established yet.
	 * @return String  
	 */
	public String getXBeePort()
	{
	  return this._xbeePort;
	}
	
	/**
	 * Sets the port of the XBee to the specified port
	 * @param _port
	 * @return boolean
	 */
	public boolean setXBeePort( String port )
	{
//		this._xbeePort	=	port;
//		return true;
		//This was the old code, but couldn't get it to run
		boolean success	=	false;
		if( port != null && !port.equals("") && !this.isConnected() )
		{
			for( int i=0; i<Serial.list().length; i++ )
			{
				if( port.equalsIgnoreCase( Serial.list()[i] ) )
				{
					this._xbeePort = Serial.list()[i];
					success	=	true;
					break;
				}
			}
		}
		return success;
	}
	
	/**
	 * Scans through all the ports and attempts to automatically connect to the XBee port.
	 */
	public void beginAuto() 
	{
		this.traceln("Attemtping to automatically connect to the XBee port.");
		
		int currentPort	=	0;
		  
		while( !this.isConnected() && currentPort < Serial.list().length ) 
		{
			String portAttempt	=	Serial.list()[currentPort];
			if( !portAttempt.contains("Bluetooth") )
		    {
		        if( this.begin(portAttempt) )
		        {
		        	break;
		        }
		    }
			currentPort++;
		}
	}
	
	/**
	 * Attempts to open the connection to the XBee with default or earlier set values. This will not run by default, as no port is specified
	 */
	public boolean begin()
	{
		boolean success	=	false;
		if( this.getXBeePort() != null && this.getXBeeBaudrate() > 0 )
		{
			success	=	this.begin( this.getXBeePort(), this.getXBeeBaudrate() );
		}
		return success;
	}
	  /**  
	   * Opens the connection to the XBee using the standard baudrate of 115200
	   * @param String
	   * @return boolean  
	   */
	  public boolean begin( String port)
	  {
		  boolean success	=	false;
		  this.setXBeePort(port);
		  if( this.getXBeePort() != null && this.getXBeeBaudrate() > 0 )
		  {
			  success	=	this.begin( this.getXBeePort(), this.getXBeeBaudrate() );
		  }
		  return success;
	  }
	  
	  /**  
	   * Opens the connection to the XBee using the specified port and the specified baudrate
	   * @param String
	   * @param int
	   * @return boolean  
	   */
	  public boolean begin( String _port, int _baudrate )
	  {
		  if( this.portAvailable(_port) )
		  {
			  this.setXBeePort(_port);
			  this.setXBeeBaudrate(_baudrate);
			  try 
			  {
				  this.traceln("Attempting to connect to XBee at "+_port+" with baudrate "+_baudrate);
				  PropertyConfigurator.configure(this.getParent().dataPath("")+"log4j.properties");
				  this._xbee = new XBee();
				  this._xbee.open( _port, _baudrate );
				  this._connectSuccess	=	true;
			  } 
			  catch (Exception e) 
			  {
				  e.printStackTrace();
				  this._connectSuccess	=	false;
				  this.traceln("Failed to connect to XBee at "+_port+" with baudrate "+_baudrate+". Please retry to connect.");
			  }
	    
		      this._xbee.addPacketListener(new PacketListener() 
		      {
		        public void processResponse(XBeeResponse response) 
		        {
		          queue.offer(response);
		        }
		      }  
		      );
		  }
	    
	    if( this._connectSuccess )
	    {
	      this.addLithneListener( this );   //Make this class listen to its own events
//	      this.addNodeListener( this );     //Make this class listen to its own events
//	      this.addMessageListener( this );  //Make this class listen to its own events
	      this.fireEvent( LithneEvent.CONNECTED );
	      this.traceln("Connection succesfully established");
	    }
	    
	    return this._connectSuccess;
	  }

	  /**
	   * Terminates the connection to the XBee. Then fires a LithneEvent.DISCONNECTED.
	   */
	  public void end()
	  {
		  this.getXBee().close();
		  this.fireEvent( LithneEvent.DISCONNECTED );
	  }
	  
	/**  
	 * This function loops through the Serial list to see whether the specified port is available
	 * @return boolean  
	 */
	public boolean portAvailable()
	{
	  return this.portAvailable( this.getXBeePort() );
	}
	/**
	 * Function checks whether the specified port is available
	 * @param port 
	 * @return boolean
	 */
	public boolean portAvailable( String port )
	{
//		return true;
		//This was the old code, but couldn't get it to run. Issue with Serial library import
	  boolean portReady  =  false;
	  for( int i=0; i<Serial.list().length; i++ )
	  {
	    if( Serial.list()[i].equalsIgnoreCase( port ) )
	    {
	      portReady  =  true;
	      break;
	    }
	  }
	  return portReady;
	}
	
	/**
   * Turns on the debug mode. This will print feedback to the console of the functions called.
   */
	public void enableDebug()
	{
		Lithne._debugMode	=	true;
		this.traceln("Debug mode enabled. To disable call 'disableDebug()'.");
	}
	  
	public void enableDebug( int depthLevel )
	{
		Lithne._debugMode		=	true;
		Lithne._debugDepth	=	depthLevel;
	}
	  
	/**
	* Disables debug output. No more message will be printed to the console.
	*/
	public void disableDebug()
	{
		this.traceln("Debug mode disabled. To enable call 'enableDebug()'.");
		Lithne._debugMode	=	false;
	}
	
	
	/*	NODE STUFF	*/
	/**
	 * Registers the specified node as a listener to Lithne and XBee events
	 * @param Node 
	 * @return boolean
	 * 			Returns true if the node was succesfully registered, false if there was an error.
	 */
	public boolean registerNode( Node node )
	{
		boolean registered  =  false;
	    if( !this.getLithneListeners().contains( node ) )
	    {
//	      this.getNodes().add( node );     //Add the node to the internal ArrayList
	      this.addLithneListener( node );  //Add the node as listener to LithneEvents
	      this.addXBeeListener( node );    //Add the node as listener to XBeeEvents
	      registered  =  true;
	    }
	    return registered;
	}
	/**
	 * This function removes the node as a listener to Lithne and XBee events of this class
	 * @param Node
	 * @return boolean
	 * 			Returns true if the node was successfully unregistered. False if there was an error.
	 */
	public boolean unregisterNode( Node node )
	{
		boolean unregistered  =  false;
//	    if( this.getNodes().indexOf( node ) >= 0 )
	    {
//	      this.getNodes().remove( this.getNodes().indexOf( node ) );  //Remove the node from the internal list
	      this.removeLithneListener( node );                          //Remove the node as listener to LithneEvents
	      this.removeXBeeListener( node );                            //Remove the node as listener to XBeeEvents
	      unregistered  =  true;
	    }
	    return unregistered;
	}
	  
//	/**
//	 * @deprecated Function has become obsolete as we made the library event-based.
//	 * @return boolean
//	 */
//	public boolean messageAvailable()
//	{
//		return false;
//		if ( !eventsActive )  //If events are not activated, we manually have to read the XBee
//		{
//			this.readXBee();
//		}
//		return newInboxMessage;
//	}
	
	

	  






	  /*  RECEIVING MESSAGES  */

	  public Message getIncomingMessage()
	  {
	    return this._incomingMessage;
	  }

	  /*  Returns the scope of the incoming message  */
	  public int getScope()
	  {
	    return this.getIncomingMessage().getScope();
	  }
	  /*  Returns the function of the incoming message  */
	  public int getFunction()
	  {
	    return this.getIncomingMessage().getFunction();
	  }
	  /*  Returns the argument of the incoming message at the specified position  */
	  public int getArgument( int _argumentPosition )
	  {
	    return this.getIncomingMessage().getArgument( _argumentPosition );
	  }
	  /*  Returns the number of arguments  */
	  public int getNumberOfArguments( )
	  {
	    return this.getIncomingMessage().getNumberOfArguments();
	  } 




	  /*  SENDING MESSAGES
	   _ _             
	   | (_)            
	   ___  ___ _ __   __| |_ _ __   __ _ 
	   / __|/ _ \ '_ \ / _` | | '_ \ / _` |
	   \__ \  __/ | | | (_| | | | | | (_| |
	   |___/\___|_| |_|\__,_|_|_| |_|\__, |
							   	   __/ |
								   |___/      
	   SENDING MESSAGES  */


	  public ArrayList<Message> getOutbox()
	  {
		  return this._outbox;
	  }
	  /**
	   * Returns the standard outgoing message that is used to simplify sending of messages.
	   * @return Message
	   */
	  public Message getOutgoingMessage()
	  {
		  return this._outgoingMessage;
	  }
	  
	  /**
	   * Sets the 64-bit address of the standard outgoing message to the specified XBeeAddress64
	   * @param XBeeAddress64
	   */
	  public void sendToXBeeAddress64( XBeeAddress64 _address64 )
	  {
	    this.getOutgoingMessage().toXBeeAddress64( _address64 );
	  }
	  
	  /**
	   * Sets the 16-bit address of the standard outgoing message to the specified XBeeAddress16
	   * @param XBeeAddress16
	   */
	  public void sendToXBeeAddress16( XBeeAddress16 _address16 )
	  {
		  this.getOutgoingMessage().toXBeeAddress16( _address16 );
	  }

	  /**
	   * Sets the scope of the standard outgoing message to the specified value 
	   * @param int
	   */
	  public void setScope( int _scope )
	  {
		  this.getOutgoingMessage().setScope( _scope );
	  }
	  /**
	   * Sets the scope of the standard outgoing message to the specified String. This string is hashed first to an integer value.
	   * @param String
	   */
	  public void setScope( String _scope )
	  {
		  this.getOutgoingMessage().setScope( _scope );
	  }

	  /*  Sets the function of the outgoing message to the specified value  */
	  public void setFunction( int _function )
	  {
		  this.getOutgoingMessage().setFunction( _function );
	  }
	  
	  /**
	   * Add an argument to the standard outgoing message
	   * @param _argument
	   */
	  public void addArgument( int _argument )
	  {
		  this.getOutgoingMessage().addArgument( _argument );
	  }
	  /**
	   * Add a list of arguments to the standard outgoing message
	   * @param _arguments
	   */
	  public void addArguments( int _arguments[] )
	  {
		  this.getOutgoingMessage().addArguments( _arguments );
	  }
	  public void addArguments( String _arguments )
	  {
		  this.getOutgoingMessage().addArguments( _arguments );
	  }

	  /**
	   * Get the number of messages currently in the outbox
	   * @return int
	   */
	  public int getMessagesInOutbox()
	  {
	    return this.getOutbox().size();
	  }
	  
	  /**
	   * Retrieves the specified message in the outbox and returns it
	   * @param Position of the message. Should be smaller than getMessagesInOutbox().
	   * @return Message
	   */
	  public Message getMessageInOutbox( int pos )
	  {
		  return this.getOutbox().get(pos);
	  }

	  /**  
	   * Transmits the specified data as a packet  
	   **/ 
	  public void sendPacket( XBeeAddress64 _address64, XBeeAddress16 _address16, int _scope, int _function, int[] _arguments )
	  {
	    this.addToOutbox( new Message( _address64, _address16, _scope, _function, _arguments ) );
	  }
	  
	  /**
	   * Gives the command to the Lithne library to send the message as it has been composed at that moment.
	   */
	  public void send()
	  {
	    this.send( true );
	  }
	  
	  /**
	   * Gives the command to the object to start transmitting the composed message. 
	   * Use the parameter to specify whether you would like overwrite messages to the same address
	   * @param boolean
	   * 			true  - overwrites messages to the same address, with the same function.
	   * 			false - adds a new message to the outbox. 
	   */
	  public void send( boolean overwrite )
	  {
	    Message sendMessage  =  new Message( this.getOutgoingMessage() );
	    this.send( sendMessage );
	    this._outgoingMessage =  new Message();
	  }
	  
	  /**  
	   * Add the specified message to the outbox, waiting to be send. 
	   * Automatically overwrites messages to the same address with the same scope and function (thus only different arguments)
	   * @param Message 
	  **/
	  public void send( Message m )
	  {
	    this.addToOutbox( m  );
	  }

//	  /**  Here, we update our outbox with the specific message.
//	   If there is an exisiting message with the same recipient and the same function and scope, we change the arguments  **/
//	  private void addToOutbox( Message msg )
//	  {
//	    this.addToOutbox( msg, true );
//	  }

	  /**
	   * This function sorts the outbox, based on the destination information of the messages.
	   * By sorting the outbox the messages that are less likely to be received don't clog the outbox
	   * and messages that are more likely to be successfully sent are transmitted first.
	   * Messages that have both a 16-bit and 64-bit address are transmitted first.
	   * Messages that have only a 64-bit address are transmitted after this
	   * Any other messages are moved to the end of the outbox
	   */
	  public synchronized void sortOutbox()
	  {
		  /**
		   * The procedure for sorting is as follows.
		   * We loop through the outbox. When we encounter a message
		   * with a 64 and 16-bit address, we do nothing, that is ok.
		   * Whenever we encounter a message with only a 64-bit, we move it to the back.
		   */
		  int insertAt	=	0;
		  for( int i=0; i<this.getMessagesInOutbox(); i++ )
		  {
			  Message checkingMsg	=	this.getOutbox().get(i);
			  //If the address is correctly set, we want to insert new messages AFTER this message
			  if( checkingMsg.toAddressing() ==	Message.ADDRESS_COMPLETE )
			  {
				  insertAt	=	i+1;
			  }
			  else if( checkingMsg.toAddressing() == Message.ADDRESS_INCOMPLETE )
			  {
				  
			  }
			  else
			  {
				  //Move the message to the of the array, this definitely clogs our outbox 
			  }
		  }
	  }
	  /**
	   * This function switches the two specified messages in the outbox from their position
	   * @param pos1
	   * @param pos2
	   */
	  public boolean switchMessages( int pos1, int pos2 )
	  {
		  boolean success	=	false;
		  if( pos1 <= this.getMessagesInOutbox() && pos2 <= this.getMessagesInOutbox() )
		  {
			  Collections.swap(this.getOutbox(), pos1, pos2);
		  }
		  return success;
	  }
	  /**
	   * This function adds the specified message to the outbox and checks whether data should be overwritten
	   * @param newMessage
	   */
	  private synchronized void addToOutbox( Message newMessage )
	  {
//	    this.traceln("ADDTOOUTBOX( MESSAGE, BOOLEAN ): Message - "+newMessage.toString() + ", enabled overwrite: "+overwriteOlderMessage, 1 );
	    boolean msgSet  =  false;

	    //If there are more message in the outbox, we check if we should overwrite older messages
	    if( this.getMessagesInOutbox() > 0  )
	    {
	      //Search through the outbox, whether there is a message with the same data
	      for ( int i=0; i<this.getMessagesInOutbox(); i ++ )
	      {
	        Message currentMessage  =  (Message) this.getOutbox().get(i);
	        //We only want to overwrite, if the message is not yet sent (otherwise, it makes no sense, it is gone)
	        if( !currentMessage.isSent() )
	        {

	//	        this.traceln("ADDTOOUTBOX( MESSAGE, BOOLEAN ): 1. Comparing ("+currentMessage.getRecipient64().toString()+") to ("+newMessage.getRecipient64().toString()+"), status: "+currentMessage.getRecipient64().equals( newMessage.getRecipient64() ), 5 );
	//	        this.traceln("ADDTOOUTBOX( MESSAGE, BOOLEAN ): 2. Comparing ("+currentMessage.getRecipient16().toString()+") to ("+newMessage.getRecipient16().toString()+"), status: "+currentMessage.getRecipient16().equals( newMessage.getRecipient16() ), 5 );
	//	        this.traceln("ADDTOOUTBOX( MESSAGE, BOOLEAN ): 3. Comparing ("+currentMessage.getScope()+") to ("+newMessage.getScope()+"), status: "+( currentMessage.getScope() == newMessage.getScope() ), 5 );
	//	        this.traceln("ADDTOOUTBOX( MESSAGE, BOOLEAN ): 4. Comparing ("+currentMessage.getFunction()+") to ("+newMessage.getFunction()+"), status: "+ (currentMessage.getFunction() == newMessage.getFunction() ), 5 );
	
		        if ( currentMessage.equalsDestination( newMessage )				 //If the messages have equal recipients, 64-bit...
		        &&   currentMessage.getScope()       == newMessage.getScope()     //and they have equals scopes
		        &&   currentMessage.getFunction()    == newMessage.getFunction() )//and an equal function
		        {
		        	//If we arrived here, the messages have the same destination, scope and function.
		        	//Now we need to check whether we need to overwrite older messages
		        	
		        	boolean overwrite	=	true;
		        	//If both messages have the same overwrite 'mask' (same arguments that should be considered)
		        	if( Arrays.equals( currentMessage.getOverwriteArguments(), newMessage.getOverwriteArguments() ) )
		        	{
		        		for( int j=0; j<newMessage.getOverwriteArguments().length; j++ )
		        		{
		        			if( currentMessage.getArgument( newMessage.getOverwriteArguments()[j] ) != newMessage.getArgument( newMessage.getOverwriteArguments()[j] ) )
		        			{
		        				overwrite	=	false;
		        				traceln("addToOutbox(): Overwrite Argument ("+j+") not equal "+currentMessage.getArgument( newMessage.getOverwriteArguments()[j] )+" vs. "+newMessage.getArgument( newMessage.getOverwriteArguments()[j] ), 5);
		        				break;
		        			}
		        		}
		        	}
		        	else
		        	{
		        		overwrite	=	false;
		        		traceln("addToOutbox(): Message overwriting set to false. getOverwriteArguments not equal.",5);
		        	}
		        	
		        	if( overwrite )
		        	{
		        		currentMessage.clearArguments();
				        currentMessage.addArguments( newMessage.getArguments() );
				        msgSet  =  true;
				        break;
		        	}
		        }
	        }
	      }
	      if ( !msgSet )
	      {
	        this.getOutbox().add( newMessage );
//	        this.traceln("ADDTOOUTBOX( MESSAGE, BOOLEAN ): Not a similar message found. Adding as new message.", 3);
	      }
	    }
	    else
	    {
	      this.getOutbox().add( newMessage );
	    }
	  }

	  
	  /**  This function checks whether there are messages in the outbox, and sends the first one  **/

	  /**  
	   * Function reviews the messages in the outbox, appends messages with similar destinations and checks for
	   * messages that are not delivered. 
	   **/
	  private synchronized void processOutbox()
	  {
	    if( this.getMessagesInOutbox() > 0 )  //If there are messages in the outbox, waiting to be send...
	    {
	      Message nextMsg  =  (Message) this.getOutbox().get(0);  //We retrieve the message that is first in line

	      this.traceln("processOutbox(): There is/are "+this.getMessagesInOutbox()+" message(s) in the outbox.", 4);

	      if ( !nextMsg.isSent() )  //If the next message in the outbox has not been sent yet, we will send it.
	      {
	    	  this.traceln("processOutbox(): The next message is not transmitted yet. We transmit it now.", 4);
	        //Here we will append any message with the same stuff
	    	  if( nextMsg.hasAppendArguments() ) nextMsg	=	this.appendMessage( nextMsg );
	    	  boolean msgSent  =  this.sendMessage( nextMsg );
//	        this.traceln("PROCESSOUTBOX(): After sending, there are "+this.getMessagesInOutbox()+" message(s) waiting to be transmitted.", 3);
	      }
	      else this.traceln("processOutbox(): The next message in line is currently being transmitted. We wait for a delivery notification.", 5);
	    }
	  }
	  
	  /**
	   * This function checks whether there are similar messages in the outbox that have to be appended.
	   * Returns a composed message consisting of all similar messages.
	   * @param Message
	   * @return Message
	   */
	  private synchronized Message appendMessage( Message originalMessage )
	  {
//	    this.traceln("APPENDMESSAGE( MESSAGE ): Appending any similar messages to "+originalMessage.toString() );
	    for( int i=0; i < this.getMessagesInOutbox(); i++ )
	    {
	      Message comparingMessage  =  (Message) this.getOutbox().get(i);
	      
	      if( !originalMessage.equals( comparingMessage )                           && //If the original message equals the message we are looking at, we are looking in a mirror
	          originalMessage.equalsDestination( comparingMessage )  				&& //..and they have equal destinations 
	          originalMessage.getScope()       == comparingMessage.getScope()       && //..and they have equals scopes
	          originalMessage.getFunction()    == comparingMessage.getFunction() )             //...and they have equal functions
	      {
	    	  boolean append	=	true;
	    	  traceln("appendMessage(): Header information similar. Checking appendArguments.",5);
	    	  if( Arrays.equals( originalMessage.getAppendArguments(), comparingMessage.getAppendArguments()) )
	    	  {
	    		  traceln("appendMessage(): appendArguments similar.",5);
	    		  for( int j=0; j<originalMessage.getAppendArguments().length; j++)
	    		  {
	    			  traceln("appendMessage(): Comparing argument ("+originalMessage.getAppendArguments()[j]+"): "+originalMessage.getArgument(originalMessage.getAppendArguments()[j]) + " vs. "+comparingMessage.getArgument(originalMessage.getAppendArguments()[j]), 5);
	    			  if( originalMessage.getArgument(originalMessage.getAppendArguments()[j]) == comparingMessage.getArgument(originalMessage.getAppendArguments()[j]) )
	    			  {
	    				  append	=	false;
	    					  
	    				  break;
	    			  }
	    		  }
	    	  }

	    	  
	    	  if( append )
		      {
	    		  if( originalMessage.addArguments( comparingMessage.getArguments() ) )
	    		  {
	    			  comparingMessage.send( this );  //Indicate that our compared message is appended and send...
	    			  comparingMessage.delivered( true );  //...and indicate our message has been delivered, so it will be cleaned
//		          this.traceln("Found a similar message to append to the first one in outbox at: "+i);
	    		  }
	    		  else  //If there is no more space in this message, we break from appending. This boat is full and is leaving, captain!
			      {
			        break;
			      }
		      }
	        
	      }
	    }
	    return originalMessage;
	  }
	  
	  /**  
	   * Checks whether the next message in the outbox has been delivered and then deletes it  
	   **/
	  private synchronized void cleanOutbox()
	  {
//	    this.traceln("CLEANOUTBOX(): Cleaning up outbox.", 1);
//	    this.printOutbox();

	    Iterator<Message> outboxMessages = this.getOutbox().iterator();

	    this.traceln("cleanOutbox(): Cleaning outbox ("+this.getOutbox().size()+")", 5);
	    while ( outboxMessages.hasNext () ) 
	    {
	      Message nextMessage  =  (Message) outboxMessages.next();
	      this.traceln("cleanOutbox(): Message: "+nextMessage.toString(), 5);
	      		//If the message has been sent and is delivered
	      if ( (nextMessage.isSent() && nextMessage.isDelivered()) ||
	    		//Or the message has been sent and is in the air for too long
	    	   (nextMessage.isSent() && (this.getParent().millis() - nextMessage.sentAt()) >= nextMessage.maximumDeliveryTime() )  )	
	      {
	        this.traceln("cleanOutbox(): Message has been sent ("+nextMessage.isSent()+") and is delivered ("+nextMessage.isDelivered()+", success:"+nextMessage.deliverySuccess()+"). We remove the message from the outbox.", 5);
//	        nextMessage.stopListeningTo( this );  //Stop listening to my events
	    	  this.removeLithneListener( nextMessage );	//Remove the message as listener to Lithne Events
	    	  
	    	  if( nextMessage.deliverySuccess() ) this.fireXBeeEvent( new XBeeEvent(this, XBeeEvent.DELIVERY_SUCCESS, nextMessage.toXBeeAddress64(), nextMessage.toXBeeAddress16()) );
	    	  else this.fireXBeeEvent( new XBeeEvent(this, XBeeEvent.DELIVERY_FAILED, nextMessage.toXBeeAddress64(), nextMessage.toXBeeAddress16()) );
	    	  
	    	  outboxMessages.remove();   //Remove the message from the outbox
	    	  nextMessage  =  null;      //Set the reference to null, for the garbage cleaner to remove it.
	      }
	    }
	  }


	  /**  
	   * This function prints an overview of the messages in the outbox  
	  **/
//	  private void printOutbox()
//	  {
//	    int msgCount  =  0;
//	    Iterator outboxMessages = outbox.iterator();
//	    while ( outboxMessages.hasNext () ) 
//	    {
//	      this.traceln("[OUTBOX ("+this.getMessagesInOutbox()+")]\t["+(msgCount+1)+"] " + ((Message) outboxMessages.next()).toString() );
//	      msgCount++;
//	    }
//	  }

	  /**
	   * Requests the information of the node specified with the XBeeAddress64
	   * @param xbeeAddress64
	   */
	  public void requestNodeInfo( XBeeAddress64 xbeeAddress64 )
	  {
	    if ( this.isConnected() )
	    {
	    	this.traceln("requestNodeInfo("+Lithne.addressToString( xbeeAddress64 )+")", 3 );
	    	try
	    	{
//	        	this.traceln("REQUESTING NODE INFORMATION FROM");
	    		RemoteAtRequest request = new RemoteAtRequest( xbeeAddress64, "ND" );
	    		this.getXBee().sendAsynchronous(request);
	    	}
	    	catch( Exception e ) 
	    	{
	    		this.traceln("[ERROR] - There was an error when requesting node information for node ("+Lithne.addressToString(xbeeAddress64)+")", 1);
	    	}
	    }
	  }
	  
	  /**
	   * Discover which nodes are present in the network. You have to listen to XBeeEvents
	   * for NODE_IDENTIFICATION to catch the responses.
	   */
	  public void discoverNodes()
	  {
		  this.requestNodeInfo(BROADCAST);
	  }


	  /**
	   * This is the function that finally sends a message via the XBee module
	   * @param message
	   * @return boolean
	   */
	  public boolean sendMessage( Message message ) 
	  {
		  this.traceln("sendMessage(). Sending the following message: "+message.toString(), 5);

		  boolean messageFailed   =  false;  //Flag to indicate whether the message was successfully sent
		  boolean sendingTo64B    =  false;  //Flag to indicate whether the message will be send to 64bit address

		  if ( this.isConnected() && message.isValid() )  //If we are connected and the message has one valid recipient
		  {
			  ZNetTxRequest request;  //Create a new request
			  
			  try 
			  {
	        //        XBeeAddress64 addr64  =  m.getRecipient64();
	        //        XBeeAddress16 addr16  =  m.getRecipient16();

	        // Check whether we will send a BROAD or UNI cast to determine the type
	        //        XBeeAddress64 broadcastAddress =  new XBeeAddress64( "00 00 00 00 00 00 FF FF" );
	        //        XBeeAddress16 unknown16B       =  new XBeeAddress16( 0x0, 0xfe );

		        /*  We assume the message is a UNICAST message.
		         If this is not the case, thus the message has a 64-bit broadcast address, we change this.  */
		        ZNetTxRequest.Option cast = ZNetTxRequest.Option.UNICAST;
		        if ( message.toXBeeAddress64().equals(BROADCAST) )
		        { //If the address is a broadcast address, we set it to a BROADCAST address
		          cast = ZNetTxRequest.Option.BROADCAST;
		          this.traceln( "sendMessage(): The address in the message is a broadcast address. Mode set to BROADCAST.", 5);
		        }

	
		        if ( message.toXBeeAddress16().equals( Lithne.UNKNOWN_16 ) ) 
		        { //If the 16 bit address is unknown, we use the 64 bit address
		          this.traceln( "sendMessage(): The 16-bit address ("+ message.toXBeeAddress16().toString()+") is UNKNOWN, so we use the 64-bit address ("+message.toXBeeAddress64().toString()+").", 4);
		          request       =  new ZNetTxRequest(  message.toXBeeAddress64(), message.getPayload() );
		          sendingTo64B  =  true;
		        }
		        else 
		        { //If we know the 16 bit address, we use this to send a message
		          this.traceln( "sendMessage(): The 16-bit address is KNOWN. We are sending the message to the specified 16-bit address ("+message.toXBeeAddress16().toString()+").", 4);
		          request = new ZNetTxRequest( 0x01, // frameID
		        		  					   message.toXBeeAddress64(), // 64b Address
		        		  					   message.toXBeeAddress16(), // 16b Address
		        		  					   0, // broadcastRadius
		        		  					   cast, // ZNetTxRequest.Option (UNICAST or BROADCAST)
		        		  					   message.getPayload() );     // int[] payload
		        }
		        this.traceln("sendMessage(): Compiled request is "+request.toString(), 5 );
	
		        //Store the time the message was sent, so we can track how long it takes to get a response
		        //        long start = System.currentTimeMillis();  //Obsolete, we now do this in the message
	
		        try 
		        {
		          //Attempt to send the message, time out after 'x' ms
		        	message.send( this ); //Indicate to the message that we send it now. We add a reference to this class to listen to its events
		        	XBeeResponse xbResponse       = this.getXBee().sendSynchronous( request, 50 );  //We send the message and wait for a response for 50 ms.
		        	ZNetTxStatusResponse response = (ZNetTxStatusResponse) xbResponse;
		        	if ( !xbResponse.isError() )  //If the message has not thrown an error
		        	{
//		        		message.delivered( true );  //Indicate that this message was delivered
	          		}
		        	
		        	/* I have commented out this code on december 4th 2012. I attempt to catch all delivery
		        	 * and failure messages using events. I have also made the wait time a lot lower (50 ms)
		        	 * which means that this code should run much faster. We don't wait for a delivery notification
		        	 * anymore, this will be captured in the 'readXBee()' function.
		        	 */

//		        	XBeeResponse xbResponse       = this.getXBee().sendSynchronous( request, 500 );  //We send the message and wait for a response for 500 ms.
//		        	
	
		        	/*  Here we check if the request has been successfully transmitted within the time-out time  */
		        	
		        }
		        catch( XBeeTimeoutException e )
		        {
//		        	message.send( this );
//		        	message.delivered(false);
		        	this.traceln("sendMessage(): [WARNING] - Message not delivered within 50 ms.", 5);
		          
	
		          /*  Here we can check whether the message was not delivered because the 16-bit address was wrong.
		           If this is the case we can reset the 16-bit address of the corresponding node.
		           If the message was not send, we will reset the 16-bit address to UNKNOWN_16B, 
		           and send the message again, using the 64-bit address.
		           The corresponding 16-bit address will then automatically be updated, if the node is known.  */
		        }
			  }
			  catch (Exception e) 
			  {
	        	this.errorln("sendMessage(): [ERROR] - There was an unexpected error when attempting to send the message. "+e);
			  }

	      /*  If we somehow failed to send the message, then we attempt to retransmit it  */
	      if ( messageFailed && message.retries() < 3  )
	      {
	        //        this.traceln("SEND( MESSAGE ): The message has FAILED to be transmitted. But we'll give it another shot. ("+(m.retries()+1)+"/3)");
	        //        this.traceln("SEND( MESSAGE ): First, we reset the 16-bit address of the message, since this might be wrong or not up to date.");
	        //        m.setRecipientBy16( UNKNOWN_16B );  //Overwrite the 16-bit address of the message with the UNKNOWN_16B address
	        //        m.retry();                          //Tell the message that we retry to send it.
	        //        this.addToOutbox( m );              //Add this message to the outbox again.
	      }
	      else
	      {
	        //        m  =  new Message();
	      }
	    }

	    /*  This code only runs when the XBee is NOT connected or the message is invalid  */
	    else
	    {
	      if ( this.isConnected() )
	      {
	        this.errorln("sendMessage(): [ERROR] - The XBeeAddress64 ("+message.toXBeeAddress64().toString()+") and/or XBeeAddress16 ("+message.toXBeeAddress16().toString()+") is invalid, so we don't send this message.");
	      }
	      else
	      {
//	        this.traceln("SENDMESSAGE( MESSAGE ): XBee NOT connected. Simulating sending and delivery.", 2 );
	      }
	      message.send( this );	//Simulate that the message is sent
	      this.getParent().delay( (int) this.getParent().random(13, 25) );	//wait for a random time	
	      message.delivered( true );	//inform the message it is delivered
	    }

	    this.traceln("sendMessage(). Message at end:" + message.toString(), 5 );

	    return message.isDelivered();  //Return whether the message was failed or not
	  }

	  /**
	                      _       _             
	                     (_)     (_)            
	   _ __ ___  ___ ___ ___   ___ _ __   __ _ 
	   | '__/ _ \/ __/ _ \ \ \ / / | '_ \ / _` |
	   | | |  __/ (_|  __/ |\ V /| | | | | (_| |
	   |_|  \___|\___\___|_| \_/ |_|_| |_|\__, |
	                                       __/ |
	                                       |___/ 
	   **/


	  /**
	   * This function checks whether there is data available in the XBee and parses it correctly.
	   * @return
	   */
	  private synchronized boolean readXBee()
	  {
		  boolean newMessage	=	false;
		  XBeeResponse xbeeResponse;
		  //  If data is received in the response
		  while ( (response = queue.poll ()) != null ) 
		  {
			  xbeeResponse  =  response;

			  this.traceln("readXBee(): [PACKET RECEIVED] - Raw packet information: "+xbeeResponse.toString()+"", 5);

			  try 
			  {
				  //  A complete list of ApiId can be found: http://xbee-api.googlecode.com/svn/trunk/docs/api/com/rapplogic/xbee/api/ApiId.html */
				  int msgApiId  =  xbeeResponse.getApiId().getValue();  //Store the API ID, we use this in our following switch-case
				  Message msg   =  new Message();
				  
				  if( 		xbeeResponse.getApiId() == ApiId.AT_COMMAND ) {}
				  else if(	xbeeResponse.getApiId() == ApiId.AT_COMMAND_QUEUE ){}
				  else if(	xbeeResponse.getApiId() == ApiId.AT_RESPONSE ){}
				  else if(	xbeeResponse.getApiId() == ApiId.REMOTE_AT_REQUEST ){}
				  else if(	xbeeResponse.getApiId() == ApiId.REMOTE_AT_RESPONSE )
				  {
					  RemoteAtResponse at_rx = (RemoteAtResponse) xbeeResponse;
			          if( at_rx.getCommand().equals("ND") )
			          {
			            this.fireXBeeEvent( new XBeeEvent( this, XBeeEvent.NODE_IDENTIFICATION, at_rx.getRemoteAddress64(), at_rx.getRemoteAddress16() ) );  //We fire a NODE_IDENTIFIER event
			          }
				  }
				  else if(	xbeeResponse.getApiId() == ApiId.ZNET_RX_RESPONSE )
				  {
					  /*  We first parse the response to a type of ZNetRxResponse. This can be done safely, because the API-ID tells us it is
		              a message of this type. More info on: http://xbee-api.googlecode.com/svn/trunk/docs/api/com/rapplogic/xbee/api/zigbee/ZNetRxResponse.html  */
					  ZNetRxResponse rx = (ZNetRxResponse) xbeeResponse;  //First, we parse the response to a ZNetRxResponse.
					  this.traceln( "readXBee(): [ZNET_RX_RESPONSE] - from "+Lithne.addressToString( rx.getRemoteAddress64() )+" - "+Lithne.addressToString( rx.getRemoteAddress16() ), 4);
//					  this.traceln("readXBee(). Received a packet of type ZNET_RX_RESPONSE (0x90). This is a message from another node. We try to parse this to a message object and throw events to MessageListeners.", 3);
			          try
			          {
			        	  if( msg != null && rx != null )
			        	  {
				            msg.fromXBeeAddress64(	rx.getRemoteAddress64() );  //Store the 64-bit sender in the incoming message
				            msg.fromXBeeAddress16(	rx.getRemoteAddress16() );  //Store the 16-bit sender in the incoming message
				            msg.setPayload(  		rx.getData() );             //Store the payload to the incoming message
				            if( rx.getOption() == Option.BROADCAST_PACKET ) msg.isBroadcast( true );
				            newMessage  =  true;
				            this.traceln("readXBee(): [RX_RESPONSE] - Received the following message: "+msg.toString(), 3 );
				            this.fireMessageEvent( MessageEvent.MESSAGE_RECEIVED, msg );
				            this.fireXBeeEvent( new XBeeEvent( this, XBeeEvent.NODE_IDENTIFICATION, rx.getRemoteAddress64(), rx.getRemoteAddress16() ) );  //We fire a NODE_IDENTIFIER event
			        	  }
			          } 
			          catch( Exception e ) 
			          {
			        	  this.errorln("readXBee(): [ZNET_RX_RESPONSE] [ERROR] - "+e.toString());
			        	  newMessage  =  false;
			          }
				  }
				  else if(	xbeeResponse.getApiId() == ApiId.ZNET_TX_STATUS_RESPONSE )
				  {
//					  this.traceln("readXBee(): [OUTBOX ("+this.getOutbox().size()+")]");
					  XBeeAddress64 _dest64	=	Lithne.UNKNOWN_64;
					  XBeeAddress16 _dest16	=	Lithne.UNKNOWN_16;
					  if( this.getOutbox().size() > 0 )
					  {
						  _dest64	=	this.getOutbox().get(0).toXBeeAddress64();
						  _dest16	=	this.getOutbox().get(0).toXBeeAddress16();
					  }
		        	  
					  ZNetTxStatusResponse z_tx_sr = (ZNetTxStatusResponse) xbeeResponse;
					  this.traceln( "readXBee(): [TX_STATUS_RESPONSE] "+z_tx_sr.toString(), 3);
//					  this.traceln( "readXBee(): [TX_STATUS_RESPONSE] - to "+z_tx_sr.getRemoteAddress16()+", delivery: "+z_tx_sr.getDeliveryStatus(), 1);
//			          this.traceln("READXBEE(): The previously send message to 16B: "+z_tx_sr.getRemoteAddress16()+" delivery status: "+z_tx_sr.getDeliveryStatus()+". Retries: "+z_tx_sr.getRetryCount(), 3 );
//			          msg.toXBeeAddress16( z_tx_sr.getRemoteAddress16() );
			          /*  At this place we can also check whether the message we have send is succesfully received  */
			          if ( z_tx_sr.getDeliveryStatus() == ZNetTxStatusResponse.DeliveryStatus.SUCCESS )
			          {
			        	  _dest16	=	z_tx_sr.getRemoteAddress16();
			        	  //We fire an event to XBeeEvent Listeners where we take the 64-bit address of the message in the outbox.
			        	  this.fireXBeeEvent( new XBeeEvent(this, XBeeEvent.DELIVERY_SUCCESS, _dest64, _dest16) );
			        	  this.traceln("readXBee(): [TX_STATUS_RESPONSE] - Delivery to "+_dest64+"/"+_dest16+" was succesful!", 3);
			          }
			          else
			          {
			        	  //We fire an event to XBeeEvent Listeners where we take the 64-bit address of the message in the outbox.
			        	  this.fireXBeeEvent( new XBeeEvent(this, XBeeEvent.DELIVERY_FAILED, this.getOutbox().get(0).toXBeeAddress64(), this.getOutbox().get(0).toXBeeAddress16()) );
			        	  this.errorln("readXBee(): [TX_STATUS_RESPONSE] - Delivery to "+_dest64+"/"+_dest16+" failed");
			          }
				  }
				  else if(	xbeeResponse.getApiId() == ApiId.ZNET_IO_NODE_IDENTIFIER_RESPONSE )
				  {
					  ZNetNodeIdentificationResponse ni_rx = (ZNetNodeIdentificationResponse) xbeeResponse;
//			          this.traceln("READXBEE(): New Node ('"+ni_rx.getNodeIdentifier()+"') of type ("+ni_rx.getDeviceType()+") in the network.");
//			          this.traceln("READXBEE(): 64B: ("+ni_rx.getRemoteAddress64()+"), 16B: ("+ni_rx.getRemoteAddress16()+")." );

			          this.fireXBeeEvent( new XBeeEvent( this, XBeeEvent.NODE_IDENTIFICATION, ni_rx.getRemoteAddress64(), ni_rx.getRemoteAddress16() ) );  //We fire a NODE_IDENTIFIER event
				  }
				  
			  }
			  catch (Exception e) 
			  {
				  this.traceln("readXBee(): [ERROR] - Error while attempting to process the incoming packet " + e.toString(), 1);
			  }
		  }

	    return newMessage;
	  }





	  /*  EVENT HANDLING
	   
	   EVENT HANDLING
	   
	   EVENT HANDLING  */

	public synchronized void lithneEventReceived( LithneEvent event )
	  {
	    //Here we can handle all Lithne Events
//	    this.traceln("LITHNEEVENTRECEIVED( LITHNEEVENT ): Received a LithneEvent ("+event.getType()+")" );
	    switch( event.getEventType() )
	    {
	    case LithneEvent.CONNECTED:
//	      this.traceln("LITHNEEVENTRECEIVED( LITHNEEVENT ): Lithne got CONNECTED, registering Pre, Draw and Post ("+event.getType()+")", 1 );
//	      this.traceln("parent is: "+this.getParent() );
	    	this.traceln("lithneEvent. Lithne is CONNECTED. Registering pre(), draw(), post() functions to main sketch.", 3);
	    	this.register();
	    break;
	    case LithneEvent.DISCONNECTED:
	    	this.traceln("lithneEvent. Lithne is DISCONNECTED. Unregistering pre(), draw(), post() functions from main sketch.", 3);
	    	this.unregister();
	    break;
	    }
	  }

	  public synchronized void messageEventReceived( MessageEvent event )
	  {
	    //Here we can handle all Message Events
//	    this.traceln("LITHNEMESSAGEEVENTRECEIVED( LITHNEMESSAGEEVENT ): Received a LithneMessageEvent ("+event.getType()+")" );
	    switch( event.getEventType() )
	    {
	    case MessageEvent.MESSAGE_RECEIVED:
//	      this.addNode( new Node( event.getMessage().fromXBeeAddress64(), event.getMessage().fromXBeeAddress16() ) );
	      break;
	    }
	  }

	  public synchronized void nodeEventReceived( NodeEvent event )
	  {
	    //Here we can handle all Node Events
//	    this.traceln("LITHNENODEEVENTRECEIVED( LITHNENODEEVENT ): Received a LithneNodeEvent ("+event.getType()+")" );
	    switch( event.getEventType() )
	    {
	    case NodeEvent.NODEJOIN:
//	      if ( this.autoAdd() )
//	      {
//	        this.addNode( new Node( this.getFreeID(), event.getNode().getXBeeAddress64(), event.getNode().getXBeeAddress16() ) );
//	      }
	      break;
	    }
	  }

	  /**
	   * Returns the current LithneListeners of this Lithne Object.
	   * @return ArrayList<LithneListener>
	   */
	  public ArrayList<LithneListener> getLithneListeners()
	  {
	    return this._lithneListeners;
	  }
	  /**  
	   * Registers the specified object as a listener to events of this Lithne object  
	   **/
	  public synchronized void addLithneListener( LithneListener l ) 
	  {
	    if( !this.getLithneListeners().contains( l ) )
	    {
	      this.getLithneListeners().add( l );
	    }
	  }
	  /**  
	   * Removes the specified object as a listener to this object  
	   **/
	  public synchronized void removeLithneListener( LithneListener l ) 
	  {
	    //    this.traceln("REMOVELITHNELISTENER( LITHNELISTENER ): There is/are currently "+_lithneListeners.size()+" lithne listener(s). We now remove one listener ("+l.getName()+").", 1);
	    _lithneListeners.remove( l );
	  }

	  /**  
	   * Fires an event that has to do with Lithne actions  
	   **/
	  private synchronized void fireEvent( int eventType ) 
	  {
	    //Create a new LithneEvent, with the specific details
	    LithneEvent event = new LithneEvent( this, eventType );

	    Iterator<LithneListener> listeners = _lithneListeners.iterator();
	    while ( listeners.hasNext () ) 
	    {
	      ( (LithneListener) listeners.next() ).lithneEventReceived( event );
	    }
	  }
	  
	  
	  
	  
	  
	  /**
	   * Returns the current XBee Event Listeners
	   * @return ArrayList<XBeeListener>
	   */
	  public ArrayList<XBeeListener> getXBeeListeners()
	  {
	    return this._xbeeListeners;
	  }
	  /**  
	   * Registers the specified parameter as a listener to events of this object. 
	   * The parameter should be of the type 'XBeeListener' and should implement the function 'xbeeEventReceived( XBeeEvent e )'  
	   **/
	  public synchronized void addXBeeListener( XBeeListener l ) 
	  {
	    if( !this.getXBeeListeners().contains( l ) )
	    {
//	    	this.traceln("addXBeeListener(): Added new XBee Listener, total "+this.getXBeeListeners().size());
	    	this.getXBeeListeners().add( l );
	    }
	  }
	  /**  
	   * Removes the specified XBeeListener as a listener to XBee Events passed by this Lithne object  
	   **/
	  public synchronized void removeXBeeListener( XBeeListener l ) 
	  {
	    //    this.traceln("REMOVELITHNELISTENER( LITHNELISTENER ): There is/are currently "+_lithneListeners.size()+" lithne listener(s). We now remove one listener ("+l.getName()+").", 1);
	    	this.getXBeeListeners().remove( l );
	  }
	  /**
	   * Fires an XBee event to all XBeeListeners 
	   * @param XBeeEvent
	   */
	  private synchronized void fireXBeeEvent( XBeeEvent event )
	  {
//	    this.traceln("Firing XBeeEvent to "+this.getXBeeListeners().size()+" listeners");
	    for( int i=0; i<this.getXBeeListeners().size(); i++ )
	    {
	      ( (XBeeListener) this.getXBeeListeners().get(i) ).xbeeEventReceived( event );
	    } 
	  }
	  
	  
	  
	  
	  /**
	   * Returns the current list of MessageListeners registered to this Lithne object
	   * @return ArrayList<MessageListener>
	   */
	  public ArrayList<MessageListener> getMessageListeners()
	  {
	    return this._messageListeners;
	  }
	  /**  
	   * Registers the specified object as a listener to events of this object  
	   **/
	  public synchronized void addMessageListener( MessageListener ml ) 
	  {
		  this.traceln("addMessageListener(): Registering new MessageListener to listen to '"+this.getName()+"'. Current listeners: "+this.getMessageListeners().size());
	    if( !this.getMessageListeners().contains( ml ) )
	    {
	      this.getMessageListeners().add( ml );
	    }
	  }
	  /**  
	   * Removes the specified object as a listener to this object  
	   **/
	  public synchronized void removeMessageListener( MessageListener ml ) 
	  {
		  this.traceln("removeMessageListener(): Removing MessageListener from '"+this.getName()+"'. Current listeners: "+this.getMessageListeners().size());
		  this.getMessageListeners().remove( ml );
	  }
	  /**  
	   * Fire a LithneMessageEvent to all listeners of the specified event type MessageEvent.XXXXX and with the specified message.
	   **/
	  private synchronized void fireMessageEvent( int eventType, Message _message ) 
	  {
		  try
		  {
			  this.traceln("fireMessageEvent(). Firing a message event (type:"+eventType+") to "+this.getMessageListeners().size()+" listeners.", 5);
			  //Create a new HyvveEvent, with the specific details
			  MessageEvent event = new MessageEvent( this, eventType, _message );
		   		
			  for( int i=0; i< this.getMessageListeners().size(); i++ )
			  {
				  this.getMessageListeners().get(i).messageEventReceived( event );
			  }
		  }
		  catch( Exception e )
		  {
			  errorln("[ERROR]: Firing MessageEvent to MessageListeners failed." );
		  }
	  }
	  
	  
	  /*  DEBUG OUTPUT  */
	  protected void traceln( String _msg )
	  {
		  this.traceln(_msg, STD_DEBUG_DEPTH);
	  }
	  /**
	   * Prints the message to the console and prepends date information and appends a new line.
	   * @param _msg
	   */
	  protected void traceln( String msg, int msgDepth )
	  {
		  if( Lithne._debugMode && msgDepth <= Lithne._debugDepth)
		  {
			  System.out.print("["+timestamp()+ " ("+PApplet.nf(this.getParent().millis(), 8)+")]\t" ); //Print the time and the amount of millis()
			  System.out.print("[Lithne ("+this.getXBeePort()+")]\t");              //Print the object type and the name of the object
			  if (this.getName().length() <= 9) System.out.print("\t");        //If it is a small name, add a tab
			  System.out.println( msg );                                      //Print the message
		  }
	  }
	  protected void trace( String msg )
	  {
		  this.trace(msg, STD_DEBUG_DEPTH);
	  }
	  /**
	   * Prints the specified message to the console, without additional information.
	   * @param _msg
	   */
	  protected void trace( String msg, int msgDepth )
	  {
		  if( Lithne._debugMode && msgDepth <= Lithne._debugDepth )
		  {
			  System.out.print(msg);
		  }
	  }
	  
	  /**
	   * Prints an error message with a time stamp and name information prepended
	   * @param String
	   */
	  protected void errorln( String errormsg )
	  {
		  System.err.print("["+this.datetimestamp()+ " ("+PApplet.nf(this.getParent().millis(), 8)+")]\t" ); //Print the time and the amount of millis()
		  System.err.print("["+this.getName()+"]\t");              //Print the object type and the name of the object
		  System.err.println( errormsg );
	  }
	  /**
	   * Returns a stamp of the current date "XXXX-XX-XX" in the format year-month-day
	   * @return String
	   */
	  public String datestamp()
	  {
		  return PApplet.nf(PApplet.year(), 4) +"-"+
				 PApplet.nf(PApplet.month(), 2) +"-"+
				 PApplet.nf(PApplet.day(), 2);
		  	
	  }
	  /**
	   * Returns a timestamp of the current time: hours-minutes-seconds in the format "XX:XX:XX"
	   * @return String
	   */
	  public String timestamp()
	  {
	    return  PApplet.nf(PApplet.hour(), 2)+":"+
	    		PApplet.nf(PApplet.minute(), 2)+":"+
	    		PApplet.nf(PApplet.second(), 2);
	  }
	  /**
	   * Returns a composed date and time stamp of the current date and time in the format
	   * 'XXXX-XX-XX XX:XX:XX' with year-month-day hours:minutes:seconds
	   * @return
	   */
	  public String datetimestamp()
	  {
		  return this.datestamp()+" "+this.timestamp(); 
	  }
}
