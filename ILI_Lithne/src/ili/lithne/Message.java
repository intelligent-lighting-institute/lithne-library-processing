
package ili.lithne;

import java.util.ArrayList;

import processing.core.PApplet;

import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;

public class Message implements LithneListener, XBeeListener, MessageListener
{
	public static final float REVISION				=	(float) 1.02;
	
	public static final int ERROR_MESSAGE			=	255;
	public static final int NO_SCOPE              	=  	1;
	public static final int NO_ARGUMENT				=	-1;
	
	public static final int ADDRESS_COMPLETE		=	2;
	public static final int ADDRESS_INCOMPLETE		=	1;
	public static final int ADDRESS_EMPTY			=	0;
	
	//Payload constants
	public static final int SCOPE_MSB				=	0;
	public static final int SCOPE_LSB				=	1;
	public static final int FUNCTION_BYTE			=	2;
	public static final int FUNCTION_MSB			=	2;
	public static final int FUNCTION_LSB			=	3;
	public static final int ARGUMENTS_START			=	4;
	public static final int MAXIMUM_PAYLOAD_BYTES 	=  	72;
	public static final int MAXIMUM_ARGUMENTS     	=  	(int) (MAXIMUM_PAYLOAD_BYTES-ARGUMENTS_START)/2;
	
	private ArrayList<MessageListener> _messageListeners	=  new ArrayList<MessageListener>();
  
	private XBeeAddress64 _to64 		= Lithne.UNKNOWN_64;  //Initialize XBeeAddress at UNKNOWN
	private XBeeAddress16 _to16 	 	= Lithne.UNKNOWN_16;  //Initialize XBeeAddress at UNKNOWN
	private XBeeAddress64 _from64		= Lithne.UNKNOWN_64;  //Initialize XBeeAddress at UNKNOWN
	private XBeeAddress16 _from16 		= Lithne.UNKNOWN_16;  //Initialize XBeeAddress at UNKNOWN
  
	private boolean _messageSent       	=	false;	//Is the message in the air?
	private boolean _messageDelivered  	=	false;	//Is the message delivered? (Did we get an acknowledgement)
	private boolean _messageSuccess    	=	false;	//Was the delivery successful?
	private boolean _isBroadcast		=	false;	//Is this message a broadcast, especially interesting when receiving
  
	/*	The payload always has the following format (in bytes
	 *	| [0][1] | 	[2][3] 	| [2+n][2+n][2+n]... |
	 *  | scope	 | function	| arguments	n ...	 |
	 */
	private int[] _payload				=	new int[ARGUMENTS_START];
	
	private int[] _appendArguments		=	new int[0];
	private int[] _overwriteArguments	=	new int[0];
	
//	PApplet _parent						=	null;	//This is only set once it is send by a Lithne object
	private Lithne _lithne				=	null;
  
//  Timestamp dateSend			=	null;	//perhaps for later implementation
//  Timestamp dateDelivered		=	null;	//perhaps for later implementation
  
	int _sentMillis           =  0;				//When was the message send?
	int _deliveredMillis      =  0;				//When was the message delivered?
	int _receivedMillis       =  0;       			//Initialize the time the message was received
	int _maximumDeliveryTime  =  5000;             //Sets the maximum time a message waits for a delivery status
	int _attempts             =  0;                //Times the message has been attempted to be send

  /*                    _                   _             
                       | |                 | |            
     ___ ___  _ __  ___| |_ _ __ _   _  ___| |_ ___  _ __ 
    / __/ _ \| '_ \/ __| __| '__| | | |/ __| __/ _ \| '__|
   | (_| (_) | | | \__ \ |_| |  | |_| | (__| || (_) | |   
    \___\___/|_| |_|___/\__|_|   \__,_|\___|\__\___/|_|
   */
	
	public Message() { }
	public Message( Message _msg )
	{
		this.toXBeeAddress64( _msg.toXBeeAddress64() );
		this.toXBeeAddress16( _msg.toXBeeAddress16() );
		this.setScope(         _msg.getScope() );
		this.setFunction(      _msg.getFunction() );
		this.addArguments(     _msg.getArguments() );
	}
	public Message( XBeeAddress64 _receiver64, XBeeAddress16 _receiver16 )
	{
		this.toXBeeAddress64( _receiver64 );
		this.toXBeeAddress16( _receiver16 );
	}
	public Message( XBeeAddress64 _receiver64, int _scope, int _function, int[] _arguments )
	{
		this.toXBeeAddress64( _receiver64 );
		this.setScope( _scope );
		this.setFunction( _function );
		this.addArguments( _arguments );
	}
	public Message( XBeeAddress64 _receiver64, XBeeAddress16 _receiver16, int _scope, int _function, int[] _arguments )
	{
		this.toXBeeAddress64( _receiver64 );
		this.toXBeeAddress16( _receiver16 );
		this.setScope( _scope );
		this.setFunction( _function );
		this.addArguments( _arguments );
	}
	public Message( Node node, int scope, int function, int[] arguments  )
	{
		this.toXBeeAddress64( node.getXBeeAddress64() );
		this.toXBeeAddress16( node.getXBeeAddress16() );
		this.setScope( scope );
		this.setFunction( function );
		this.addArguments( arguments );
	}

	/**
	 * Sets the reference to the object that send this message
	 * @param lithne
	 */
	public void setLithne( Lithne lithne )
	{
		if( lithne != null )
		{
			this._lithne	=	lithne;
		}
	}
	
	/**
	 * Returns the Lithne object that sends this message. Is only available when the message
	 * has been transmitted.
	 * @return
	 */
	public Lithne getLithne()
	{
		return this._lithne;
	}
  /*	META LEVEL INFORMATION: TRANSMISSION STATUS	*/
  /**
   * Clears all meta level information regarding the transmission of the message
   */
  public void clearTransmissionStatus()
  {
	  this._messageSent       	=  false;	//Is the message in the air?
	  this._messageDelivered  	=  false;	//Is the message delivered? (Did we get an acknowledgement)
	  this._messageSuccess    	=  false;	//Was the delivery successful?
	  
	  this._sentMillis          =  0;				//When was the message send?
	  this._deliveredMillis     =  0;				//When was the message delivered?
	  this._receivedMillis      =  0;       			//Initialize the time the message was received
	  this._maximumDeliveryTime =  5000;             //Sets the maximum time a message waits for a delivery status
	  this._attempts            =  0;                //Times the message has been attempted to be send
  }

  /**
   * Indicates when the message was received. Returns the   
   * @param int
   * @return int
   */
  public int receivedAt( int millis )
  {
    return this._receivedMillis  =  millis;
  }
  /*  Returns the millis the message was received  */
  public int receivedAt()
  {
    return this._receivedMillis;
  }
  
  /**  Increases the number of attempts to transmit this message  **/
  public void retry()
  {
    this._attempts++;
  }
  /**  Returns the current retry count  **/
  public int getRetries()
  {
    return this.attempts();
  }
  /**  Same as getRetries()  **/
  public int attempts()
  {
    return this._attempts;
  }
  /**  Same as getRetries()  **/
  public int retries()
  {
    return this._attempts;
  }
  /**
   * Indicates whether this received message was a broadcast
   * @return
   */
  public boolean isBroadcast()
  {
	  return _isBroadcast;
  }
  /**
   * Indicates whether the received message was broadcasted
   * @param boolean
   * @return boolean
   */
  public boolean isBroadcast( boolean isBroadcast )
  {
	  this._isBroadcast	=	isBroadcast;
	  return this._isBroadcast;
  }
  /*
  public int getChecksum()
  {
    return this._checksum;
  }
  public void setChecksum( int checksum )
  {
    this._checksum  =  checksum;
  }
  */
  
  /**  Whenever this message should not listen to a Lithne sender, call this function  **/
  public void send( Lithne lithne )
  {
	  if( lithne != null )
	  {
		  this.setLithne( lithne );	//Set the reference to the object that send this message
		  this._messageSent  =  true;	//set the flag to true
		  this.sentAt( this.getLithne().getParent().millis() );	//Indicate transmission time start
		  this.getLithne().addXBeeListener( this );	//Add this object as listener to XBee events (SUCCES or FAIL)
	  }
  }
  
  /**
   * Calling this function indicates the message is send via the XBee. This message is added as
   * listener to the Lithne object. The millis indicate the time the message was sent.
   * @param Lithne
   * @param int
   */
  public void send( Lithne lithne, int millis )
  {
	  this.setLithne( lithne );	//Set the reference to the object that send this message
	  this._messageSent  =  true;	//set the flag to true
	  this.sentAt( millis );
	  this.getLithne().addXBeeListener( this );    //Add this message as listener to the sending class
  }
  
  /**
   * Indicates the delivery status of the message (true/false) and the milliseconds the message was delivered.
   * This allows to calculate the delivery time.
   * @param deliveryState
   */
  public void delivered( boolean deliveryState )
  {
	  this._messageDelivered	=	true;
	  this._messageSuccess		=  deliveryState;
	  
	  if( this.getLithne() != null )
	  {
		  this.getLithne().removeXBeeListener( this );
		  this._deliveredMillis	=	this.getLithne().getParent().millis();
	  }
  }
  /**  
   * Indicates the delivery status of the message (true/false) and the milliseconds the message was delivered.
   * This allows to calculate the delivery time.
   * @param deliveryState
   */
  public void delivered( Lithne lithne, boolean deliveryState, int millis )
  {
	  this._messageDelivered  =  true;
	  this._messageSuccess    =  deliveryState;
	  if( lithne != null )
	  {
		  this.deliveredAt( millis );
		  lithne.removeMessageListener(this);
	  }
  }
  
  /**
   * Returns when the message was sent (in millis(); Processing)
   * @return int
   */
  public int sentAt() 
  {
	  return this._sentMillis;
  }
  /**
   * Set the time at which the message was transmitted.
   * @param time
   * @return int
   */
  public int sentAt( int time )
  {
	  if( !this.isSent() )
	  {
		  this._messageSent	=	true;
	  }
	  return this._sentMillis	=	time;
  }
  /**
   * Returns when the message was delivered (in millis(); Processing)
   * @return int
   */
  public int deliveredAt()
  {
	  return this._deliveredMillis;
  }
  
  /**
   * Sets the time at which the message was delivered and returns this value
   * @param time
   * @return int
   */
  public int deliveredAt( int time )
  {
	  if( !this.isDelivered() )
	  {
		  this._messageDelivered	=	true;
	  }
	  return this._deliveredMillis	=	time;
  }
  
  /**  Calculates how long the message took to be delivered  **/
  public int getDeliveryTime()
  {
    int deliveryTime  =  0;
    if( this.deliveredAt() > 0 )
    {
      deliveryTime  =  (int) this.deliveredAt() - this.sentAt();
    }
    
    return PApplet.max( deliveryTime, 0 );
  }
  
  /**  Returns the maximum time we wait before considering the message failed  **/
  public int maximumDeliveryTime()
  {
    return this._maximumDeliveryTime;
  }
  /**
   * Returns the maximum time to wait before considering the message as timed out. Sets the time out to the specified time.
   * @param int
   * @return int
   */
  public int maximumDeliveryTime( int time )
  {
	  return this._maximumDeliveryTime	=	time;
  }
  
  /**
   * Based on the provided millis, this calculates the interval between this moment and the time the message was sent.
   * @param millis
   * @return
   */
  public int transmissionTime( int millis )
  {
	  int transmissionTime	=	0;
	  if( this.sentAt() > 0 )
	  {
		  transmissionTime	=	millis - this.sentAt();
	  }
	  return transmissionTime;
  }
  
  /**  Clears the internally stored data, to ready the message for new data  **/
  public void clear()
  {
	  this.clearPayload();
//	  this.clearArguments();	- Commented out
//	  this.clearFunction();
	  this.clearScope();
	  this.clearTo();
	  this.clearFrom();
	  this.clearTransmissionStatus();
  }
  
  /**  
   * This function checks whether the message has a valid 64-bit XBeeAddress or valid 16-bit XBeeAddress.
   * Returns 'true' if the message can be send. Returns 'false' if the message can't be send.  
   **/
  public boolean isValid()
  {
    boolean validMessage  =  true;
    if( this.toXBeeAddress64().equals( Lithne.UNKNOWN_64 ) && this.toXBeeAddress16().equals( Lithne.UNKNOWN_16 ) )
    {
      validMessage  =  false;
    }
    return validMessage;
  }
  
  /**
   * This function checks whether the specified message and this message
   * have an equal destination address. If this is the case, returns 'true'
   * otherwise this function returns 'false'
   * @param Message
   * @return boolean
   */
  public boolean equalsDestination( Message msg )
  {
	  boolean equal	=	false;
	  //If both the 64-bit and 16-bit address are equal, they have the same destination
	  if( this.toXBeeAddress16().equals( msg.toXBeeAddress16() ) && 
		  this.toXBeeAddress64().equals( msg.toXBeeAddress64() ) )
	  {
		  equal	=	true;
	  }
	  //If this message does not have a valid 16-bit address, but the 64-bit addresses
	  //are equals, we consider it to be the same destination, and we immediately update
	  //our local 16-bit address to that of the message given to us.
	  if( this.toXBeeAddress16().equals( Lithne.UNKNOWN_16 ) &&
		  this.toXBeeAddress64().equals( msg.toXBeeAddress64() ) )
	  {
		  equal	=	true;
		  if( !msg.toXBeeAddress16().equals( Lithne.UNKNOWN_16 ) )
		  {
			  this.toXBeeAddress16(msg.toXBeeAddress16());
		  }
	  }
	  //If, for some strange reason, this message does not have a 64-bit address,
	  //but the 16-bit addresses match, we consider the messages to have the same
	  //destination. We also update our local 64-bit address. This situation is unlikely
	  if( this.toXBeeAddress64().equals(Lithne.UNKNOWN_64) && 
		  this.toXBeeAddress16().equals( msg.toXBeeAddress16() ) )
	  {
		  equal	=	true;
		  if( !msg.toXBeeAddress64().equals(Lithne.UNKNOWN_64))
		  {
			  this.toXBeeAddress64(msg.toXBeeAddress64());
		  }
	  }
	  return equal;
  }
  

  /*  Returns whether the message is sent or not  */
  public boolean isSent()
  {
    return this._messageSent;
  }
  /*  Returns whether the message is delivered or not  */
  public boolean isDelivered()
  {
    return this._messageDelivered;
  }
  /**  Returns whether the message delivery was successful or not  **/
  public boolean deliverySuccess()
  {
    return this._messageSuccess;
  }
  

  /*
      _                         _     _                   
   | |                       | |   | |                  
   __  _| |__   ___  ___  __ _  __| | __| |_ __ ___  ___ ___ 
   \ \/ / '_ \ / _ \/ _ \/ _` |/ _` |/ _` | '__/ _ \/ __/ __|
   >  <| |_) |  __/  __/ (_| | (_| | (_| | | |  __/\__ \__ \
   /_/\_\_.__/ \___|\___|\__,_|\__,_|\__,_|_|  \___||___/___/
   
   */
  public void toNode( Node node )
  {
	  this.toXBeeAddress64( node.getXBeeAddress64() );
	  this.toXBeeAddress16( node.getXBeeAddress16()	);
  }

  /**
   * Clears the to-fields (destination's fields) of this message
   */
  public void clearTo()
  {
    this._to16  =  Lithne.UNKNOWN_16;
    this._to64  =  Lithne.UNKNOWN_64;
  }
  /**
   * Clears the from-fields (sender's fields) of this message
   */
  public void clearFrom()
  {
    this._from64  =  Lithne.UNKNOWN_64;
    this._from16  =  Lithne.UNKNOWN_16;
  }
  
  
  /*
   * 
   * TO-FIELD	-	TO FIELDS	-	TO-FIELDS
   * 
   */
  
  public int toAddressing()
  {
	  //Standard, the addressing is empty, meaning nothing is set.
	  int toAddressing	=	Message.ADDRESS_EMPTY;
	  
	  //If both the XBeeAddresses are set, the addressing is complete
	  if( !this.toXBeeAddress64().equals(Node.UNKNOWN_64) && !this.toXBeeAddress16().equals(Node.UNKNOWN_16) )
	  {
		  toAddressing	=	Message.ADDRESS_COMPLETE;
	  }
	  //Otherwise, if either the 16 bit, or the 64 bit is set, the addressing is incomplete.
	  else if( (this.toXBeeAddress16().equals(Node.UNKNOWN_16) && !this.toXBeeAddress64().equals(Node.UNKNOWN_64) )
			|| !(this.toXBeeAddress16().equals(Node.UNKNOWN_16) && this.toXBeeAddress64().equals(Node.UNKNOWN_64) ) )
	  {
		  toAddressing	=	Message.ADDRESS_INCOMPLETE;
	  }
	  return toAddressing;
  }
  /**
   * Returns the (hardware address) destination address for this message
   * @return XBeeAddress64
   */
  public XBeeAddress64 toXBeeAddress64( )
  {
	  return this._to64;
  }
  /** Set the recipient for the message using 64 bit Address **/
  public XBeeAddress64 toXBeeAddress64( XBeeAddress64 xbeeAddress64 )
  {
    if( !this.toXBeeAddress64().equals(xbeeAddress64) )
    {
      this._to64	=	xbeeAddress64;
    }
    return this._to64;
  }
  
  /**
   * Converts the specified String, which should be in the format
   * "XX XX XX XX XX XX XX XX" to an XBeeAddress64
   * @param xbeeAddress64
   * @return XBeeAddress64
   */
  public XBeeAddress64 toXBeeAddress64( String xbeeAddress64 )
  {
	  try
	  {
		  this.toXBeeAddress64( new XBeeAddress64( xbeeAddress64 ) );
	  }
	  catch(Exception e){ }
	  return this._to64;
  }

  /**
   * Returns the 16-bit (network address) destination address of this message 
   * @return XBeeAddress16
   */
  public XBeeAddress16 toXBeeAddress16( )
  {
	  return this._to16;
  }
  /** Set the recipient for the message using 16 bit Address **/
  public XBeeAddress16 toXBeeAddress16( XBeeAddress16 xbeeAddress16 )
  {
	  if( xbeeAddress16 != null )
	  {
		  this._to16	=	xbeeAddress16;
	  }
	  return this._to16;
  }
  

  
  /**  Returns the 64-bit address of the sender of the message and returns an
      unknown address if the 64-bit address is not entered  */
  public XBeeAddress64 fromXBeeAddress64()
  {
    XBeeAddress64 returnAddress  =  Lithne.UNKNOWN_64;
    if ( this._from64 != null )
    {
      returnAddress  =  this._from64;
    }
    return returnAddress;
  }
  /**
   * Sets the from-field to the specified hardware address (XBeeAddress64). Then returns the updated field value.
   * @param XBeeAddress64
   * @return XBeeAddress64
   */
  public XBeeAddress64 fromXBeeAddress64( XBeeAddress64 xbeeAddress64 )
  {
	  if( xbeeAddress64 != null )
	  {
		  this._from64	=	xbeeAddress64;
	  }
	  return this._from64;
  }

  /**
   * Returns the current from-field network address (XBeeAddress16).
   * @return XBeeAddress16
   */
  public XBeeAddress16 fromXBeeAddress16( )
  {
    XBeeAddress16 returnAddress  =  new XBeeAddress16( 0, 0 );
    if ( this._from16 != null )
    {
      returnAddress  =  this._from16;
    }
    return returnAddress;
  }
  
  /**
   * Updates the from-field network address (XBeeAddress16) to the specified value. Then returns the new field's value.
   * @param XBeeAddress16
   * @return XBeeAddress16
   */
  public XBeeAddress16 fromXBeeAddress16( XBeeAddress16 xbeeAddress16 )
  {
    if( xbeeAddress16 != null )
    {
    	this._from16	=	xbeeAddress16;
    }
    return this._from16;
  }
  
  /*
                                            _       
   | |      
   __ _ _ __ __ _ _   _ _ __ ___   ___ _ __ | |_ ___ 
   / _` | '__/ _` | | | | '_ ` _ \ / _ \ '_ \| __/ __|
   | (_| | | | (_| | |_| | | | | | |  __/ | | | |_\__ \
   \__,_|_|  \__, |\__,_|_| |_| |_|\___|_| |_|\__|___/
   __/ |                                   
   |___/                                    
   */
  /**
   *	This function clears the entire payload and reinitializes the standard scope and function
   **/
  public void clearPayload()
  {
	  this._payload	=	new int[Message.ARGUMENTS_START];
  }

  /**
   * This function resets the currently set function to the standard (Message.ERROR_MESSAGE = 255)
   */
  public void clearFunction()
  {
	  this._payload[Message.FUNCTION_MSB]	=	0;
	  this._payload[Message.FUNCTION_LSB]	=	0;
  }

  /**
   * Clears all arguments in the message, leaves the scope and function untouched
   */
  public void clearArguments() 
  {
	  int scopeBackup	=	this.getScope();		//We back up the scope
	  int funcBackup	=	this.getFunction();		//and the function
	  
	  this.clearPayload();							//Then we clear the entire payload
	  
	  this.setScope( scopeBackup );					//And we reset the scope
	  this.setFunction( funcBackup );				//and we reset the function
  }
  
  /**
   * Adds a byte to the end of the payload.
   * @param argument
   * @return 'true' if addition was successful, 'false' if the byte is not added
   */
  public boolean addByte( int argument )
  {
	  boolean success	=	false;
	  if( this.getPayloadSize() < MAXIMUM_PAYLOAD_BYTES )
	  {
		  this._payload	=	PApplet.append(this._payload, argument & 0xFF);
		  success		=	true;
	  }
	  return success;
  }
  
  /**
   * Sets an argument at a specified position in the payload
   * @param position
   * @param argument
   */
  public void setArgument( int position, int argument )
  {
	  if( position < this.getNumberOfArguments() )
	  {
		  
	  }
  }
  
  /**
   * Add a boolean as an argument. True becomes 1, false becomes 0
   * @param argument
   * @return boolean
   */
  public boolean addArgument( boolean argument )
  {
	  if( argument )
	  {
		  return this.addArgument( 1 );		  
	  }
	  else
	  {
		  return this.addArgument( 0 );
	  }
	  
  }
  /**
   * Adds the specific argument to the list of arguments
   * @param argument
   * @return true if successful
   */
  public boolean addArgument( float argument )
  {
    return this.addArgument( (int)argument );
  }
  /**
   * Adds the argument to the message at the end of the current message array. Returns whether the argument is succesfully appended.
   * @param int
   * @return boolean
   */
  public boolean addArgument( int argument ) 
  {
	  boolean success	=	false;
	  if( this.getNumberOfArguments() < Message.MAXIMUM_ARGUMENTS )
	  {
		  	int argMSB		=	argument >> 8 & 0xFF;	//We calculate the MSB of the argument..
	  		int argLSB		=	argument & 0xFF;		//..and the LSB of the argument
//	  		System.out.println("Converted "+argument+" to MSB: "+argMSB+" and LSB: "+argLSB);
	  		this._payload	=	PApplet.append( this._payload, argMSB );	//We first add the MSB to the array..
	  		this._payload	=	PApplet.append( this._payload, argLSB );	//..and secondly the LSB
//	  		this._arguments	=	PApplet.append( this._arguments, argument );
	  		success = true;
	  }
	  return success;
  }
  /**
   * Adds multiple arguments to the message at the end of the array. 
   * Returns 'true' if all arguments have successfully been appended. 
   * Returns 'false' if the payload is full.
   * @param int[]
   * @return boolean
   */
  public boolean addArguments( int arguments[] )
  {
    boolean added  =  false;
    if( (this.getNumberOfArguments() + arguments.length) <= MAXIMUM_ARGUMENTS )  //If there is enough space in the payload, we add the arguments
    {
      for ( int i=0; i<arguments.length; i++ )
      {
        added	=	this.addArgument( arguments[i] );
        if( !added )
        {
        	break;
        }
      }
    }
    return added;
  }
  
  /**
   * Adds a string of arguments to the message in the format "xx, xx, xx"
   * @param String
   */
  public void addArguments( String arguments )
  {
    String[] seperateArgs  =  PApplet.split( arguments, ",");
    for ( int i=0; i<seperateArgs.length; i++ )
    {
      this.addArgument( Integer.parseInt( PApplet.trim(seperateArgs[i]) ) );
    }
  }
  
  /**
   * Clears the existing arguments that should be considered for appending
   */
  public void clearAppendArguments()
  {
	  this._appendArguments	=	new int[0];
  }
  
  /**
   * This function indicates whether the Message has argument appending turned on.
   * @return boolean
   */
  public boolean hasAppendArguments()
  {
	  boolean appArg	=	false;
	  if( this.getAppendArguments().length > 0 )
	  {
		  appArg	=	true;
	  }
	  return appArg;
  }
  
  /**
   * This returns the array of arguments that should be considered when appending
   * this message is appended to another message. When setting an appending argument
   * 
   * @return int[]
   */
  public int[] getAppendArguments()
  {
	  return this._appendArguments;
  }
  /**
   * This function adds the argument number that should be considered when appending
   * messages to each other.
   * @param argNumber
   */
  public void appendIfDifferent( int argNumber )
  {
	  this._appendArguments	=	PApplet.append(this._appendArguments, argNumber);
  }
  
  /**
   * This clears the existing overwrite arguments specified
   */
  public void resetOverwriteArguments()
  {
	  this._overwriteArguments	=	new int[0];
  }
  
  /**
   * This returns the array of arguments that should be considered when overwriting
   * this message is appended to another message. When setting an appending argument
   * 
   * @return int[]
   */
  public int[] getOverwriteArguments()
  {
	  return this._overwriteArguments;
  }
  
  /**
   * Indicates whether arguments are set as overwriting arguments
   * @return
   */
  public boolean hasOverwriteArguments()
  {
	  boolean appArg	=	false;
	  if( this.getOverwriteArguments().length > 0 )
	  {
		  appArg	=	true;
	  }
	  return appArg;
  }
  
  /**
   * Use this function to indicate specific arguments that should be checked
   * before this message can be overwritten. By specifying an argument number
   * you indicate that the message arguments may be overwritten if that specific
   * argument is similar
   * @param argNumber
   */
  public void overwriteIfEqual( int argNumber )
  {
	  this._overwriteArguments	=	PApplet.append( this._overwriteArguments, argNumber );
  }
  

  /**  
   *	Returns the arguments at the specified position. 
   *	Returns 'NO_ARGUMENT' (-1) if there is no argument at that position.
   *	@return int
   **/
  public int getArgument( int argPos )
  {
    int arg  =  Message.NO_ARGUMENT;
    if ( argPos < this.getNumberOfArguments() )
    {
    	int argMSB	=	this._payload[ Message.ARGUMENTS_START + (argPos*2)  ] << 8;
    	int argLSB	=	this._payload[ Message.ARGUMENTS_START + (argPos*2)+1];
    	arg 		=	argMSB + argLSB;
    }
    return arg;
  }
  /**
   * Returns an array list of all arguments.
   * @return int[]
   */
  public int[] getArguments()
  {
	  int arguments[]	=	new int[this.getNumberOfArguments()];
	  for( int i=0; i<this.getNumberOfArguments(); i++ )
	  {
		  arguments[i]	=	this.getArgument(i);
	  }
	  return arguments;
  }
  
  /**
   * Returns the number of arguments currently in this message.
   * @return
   */
  public int getNumberOfArguments()
  {
    return (this._payload.length - Message.ARGUMENTS_START)/2;
  }
  
  /**
   * Indicates whether the maximum number of arguments has been reached. Indicates 'true' when no more arguments can be added (thus, array is full)
   * @return boolean
   */
  public boolean argumentsFull()
  {
    boolean full  =  false;
    if( this.getNumberOfArguments() >= MAXIMUM_ARGUMENTS )
    {
      full  =  true;
    }
    return full;
  }

  /**  
   * Change the function specified in the message using a String.
   * The String is first hashed to a number, before being set.
   * Please note that you can use maximum of ~250 characters. 
   */
  public void setFunction( String function )
  {
    this.setFunction( Lithne.hash( function ) );
  }
  /**
   * Sets the function to be called by this message to the specified value. Automatically converts to integer.
   * @param function
   */
  public void setFunction( float function )
  {
	  this.setFunction( (int) function );
  }
  /**  
   * Sets the function to be called by this message to the specified value. Returns the new function. 
   */
  public void setFunction ( int function )
  {
      this._payload[Message.FUNCTION_MSB]	=	(function >> 8) & 0xFF;
      this._payload[Message.FUNCTION_LSB]	=	 function 		& 0xFF;
  }
  
  /**
   * Returns the current function called by the message
   * @return int
   */
  public int getFunction()
  {
	  int f_msb		=	this._payload[Message.FUNCTION_MSB] << 8;
	  int f_lsb		=	this._payload[Message.FUNCTION_LSB];
	  return ( f_msb + f_lsb );
  }
  
  /**
   * Checks whether the function in this message equals the
   * provided String. Hashes the String first. 
   * @param function
   * @return boolean
   */
  public boolean functionIs( String function )
  {
	  return functionIs( Lithne.hash(function) );
  }
  
  /**
   * Checks whether the function equals the provided integer
   * @param function
   * @return boolean
   */
  public boolean functionIs( int function )
  {
	  boolean equal	=	false;
	  if( this.getFunction() == function )
	  {
		  equal	=	true;
	  }
	  return equal;
  }
  
  /**
   * This function can be used to transmit a String in the message.
   * Please note: This function REMOVES the existing arguments!
   * @param argument
   */
  public void setStringArgument( String argument )
  {
//	  System.out.println("Converting argument '"+argument+"'");
	  if( argument != null && argument.length() != 0 )
	  {
		  this.clearArguments();	//First we clear the arguments
		  for( int i=0; i < argument.length(); i++  )	//Loop through the entire argument
		  {
			  this._payload	=	PApplet.append( this._payload, argument.charAt(i) & 0xFFFF);
			  
//			  System.out.println("Compiled "+i+" '"+argument.charAt(i)+" to "+this._payload[3+i]);
		  }
	  }
  }
  
  /**
   * Returns a String based on the current payload
   * @return
   */
  public String getStringArgument( )
  {
	  char characterArr[] = new char[ this.getPayload().length - Message.ARGUMENTS_START ];
//	  System.out.println("Converting arguments to String of length "+characterArr.length);
      for (int i = 0; i < characterArr.length; i++ )
      {
        characterArr[(i)]   =	  (char) PApplet.constrain(this.getPayload()[Message.ARGUMENTS_START+i], 32, 127 );
//        System.out.println("Converting "+this.getPayload()[3+i]+" to "+characterArr[i]);
      }
      return new String(characterArr);
	  
  }
  
  /**
   * Returns the current size of the payload
   * @return int
   */
  public int getPayloadSize()
  {
	  return this.getPayload().length;
  }

  /**
   * Returns the payload to be transmitted via the Zigbee message, standard including the scope.
   * @return
   */
  public int[] getPayload( )
  {
    return getPayload( true );
  }
  /**
   * Returns the payload for a ZigBee message, based on the specified information in this message.
   * @param boolean 
   * @return int[]
   */
  public int[] getPayload( boolean withScope ) 
  {
	  int payload[]	=	this._payload;
	  if( !withScope )
	  {
	  }
	  return payload;
//    int[] payload  =  new int[0];
//    if ( _withScope )
//    {
//      int payLength = 3 + (this.getNumberOfArguments()*2);
//      payload = new int[payLength];
//      payload[0] = this.getScope() >> 8 & 0xff;
//      payload[1] = this.getScope() & 0xff;
//      payload[2] = this.getFunction() & 0xff;
//      
//      for (int i = 0; i < this.getNumberOfArguments(); i++) 
//      {
//        payload[(i*2)+3] = (this.getArgument(i) >> 8 ) & 0xFF ;  //Here we need to split the int in two bytes
//        payload[(i*2)+4] = this.getArgument(i) & 0xFF;  //Here we need to split the int in two bytes
//      }
//    }
//    else
//    {
//      int payLength = 1 + this.getNumberOfArguments();
//      payload = new int[payLength];
//      payload[0] = this.getFunction();
//      for (int i = 0; i < this.getNumberOfArguments(); i++) 
//      {
//    	  payload[(i*2)+1] = (this.getArgument(i) >> 8 ) & 0xFF ; //Here we need to split the int in two bytes
//          payload[(i*2)+2] = this.getArgument(i) & 0xFF;  		  //Here we need to split the int in two bytes
//      }
//    }
  }

  /**  
   * You can send a message payload to this function and it 
   * overwrites all the internal data with the data in the payload  
   */
  public void setPayload( int[] payload )
  {
	  if( payload.length >= Message.ARGUMENTS_START )
	  {
		  this._payload	=	payload;
	  }
  }

  /*
                        _             
   (_)            
   ___  ___ ___  _ __  _ _ __   __ _ 
   / __|/ __/ _ \| '_ \| | '_ \ / _` |
   \__ \ (_| (_) | |_) | | | | | (_| |
   |___/\___\___/| .__/|_|_| |_|\__, |
    		   | |             __/ |
			   |_|            |___/ 
   */
  
  /**  Sets the scope for this message from an integer **/
  public void setScope( int scope )
  {
	  this.getPayload()[SCOPE_MSB] = (scope >> 8) & 0xFF;
	  this.getPayload()[SCOPE_LSB] = (scope     ) & 0xFF;
  }
  /**
   * Sets the scope of this message to the specific String value. The String will be hashed to an integer value.
   * @param _scope
   */
  public void setScope( String scope )
  {
    this.setScope( Lithne.hash(scope) );
  }
  /**  Removes the scope of the message  **/
  public void clearScope( )
  {
    this.setScope( NO_SCOPE );
  }  
  /**
   * Returns the current scope of this message.
   * @return
   */
  public int getScope( )
  {
	  int scopeMSB	=	this.getPayload()[SCOPE_MSB] << 8;
	  int scopeLSB	=	this.getPayload()[SCOPE_LSB];
	  return scopeMSB + scopeLSB;
  }
  
//  /**  
//   * Generates a hash of a word that represents the scope of the message. E.g. 'desk' will be hashed to ...
//   */
//  private int hashGroup( String word )
//  {
//    int wordValue  =  0;
//    for ( int i=0; i < word.length(); i++ )
//    {
//      wordValue  +=  (byte) word.charAt(i) ;
//    }
//    wordValue += word.length(); 
//    wordValue += (byte) word.charAt(0) ;
//    wordValue += (byte) word.charAt( word.length() - 1 );
//    //println("word value of "+word+" = "+wordValue);
//    return wordValue;
//  }



  /**
   * This handles the XBee events that are for this message
   */
  public void xbeeEventReceived( XBeeEvent event )
  {
	  if( event.getEventType() == XBeeEvent.DELIVERY_SUCCESS )
	  {
		  this.delivered( true );
	  }
	  if( event.getEventType() == XBeeEvent.DELIVERY_FAILED )
	  {
		  this.delivered( false );
	  }
  }  
  /**
   * This handles the Lithne events addressed to this message
   */
  public void lithneEventReceived( LithneEvent event )
  {
	  if( this.isSent() && !this.isDelivered() )
	  {
		  if( event.getEventType() == LithneEvent.PACKET_SUCCESS )
		  {
			  this.delivered( event.getLithne(), true, event.getMillis() );
//			  System.out.println("Message succesfully delivered. "+this.toString());
		  }
		  else if( event.getEventType() == LithneEvent.PACKET_FAILED )
		  {
			  this.delivered( event.getLithne(), false, event.getMillis() );
//			  System.out.println("Message NOT delivered. "+this.toString());
		  }
	  }
  }
  
  /**
   * Handles Message Events that are addressed to this message
   */
  public void messageEventReceived( MessageEvent event )
  {
  }
  
  
  public void nodeEventReceived( NodeEvent event )
  {
  }
  
  
  
  

  public String toString()
  {
	  return this.toString( false );
  }
  
  public String toString( boolean rawPayload )
  {
    String msgToString	=	"";
    
    if( !this.toXBeeAddress64().equals( Lithne.UNKNOWN_64) && !this.toXBeeAddress16().equals(Lithne.UNKNOWN_16) )
    {
	    msgToString	=	"[OUTGOING] [to 64B:"+Lithne.addressToString( this.toXBeeAddress64() )+
	    				", to 16B:"+Lithne.addressToString( this.toXBeeAddress16() )+"] ";
    }
    if( !this.fromXBeeAddress64().equals( Lithne.UNKNOWN_64) && !this.fromXBeeAddress16().equals(Lithne.UNKNOWN_16) )
    {
	    msgToString	+=	"[INCOMING] [from 64B:"+Lithne.addressToString( this.fromXBeeAddress64() )+
	    				", from 16B:"+Lithne.addressToString( this.fromXBeeAddress16() ) +"] ";
    }
    
    if( rawPayload )
    {
    	msgToString	+=	"[payload: ";
        for( int i=0; i<this.getPayloadSize(); i++ )
        {
        	msgToString	+= this.getPayload()[i];
        	if( i != this.getPayloadSize()-1 ) msgToString +=",";
        }
    }
    else
    {
    	msgToString	+=	"[scope:" +this.getScope()+", function:"+this.getFunction();
    	if( this.getNumberOfArguments() > 0 )
        {
        	if( this.getNumberOfArguments() > 1 ){ msgToString+= ", "+this.getNumberOfArguments()+" arguments:"; }
        	else{ msgToString	+=	", "+this.getNumberOfArguments()+" argument:"; }
        	
        	for( int i=0; i<this.getNumberOfArguments(); i++)
        	{
        		msgToString	+= this.getArgument(i);
        		if( i != this.getNumberOfArguments()-1 ) msgToString += ",";
        	}
        }
    }
    
    msgToString += "]";
    
    
    
    msgToString	+=	" (valid: "+this.isValid()+", sent:"+this.isSent();
                  
    if( this.isSent() && !this.isDelivered() )
    {
    	msgToString  +=  " ("+(this.getLithne().getParent().millis()-this.sentAt())+" of "+this.maximumDeliveryTime()+" ms)";
    }
    
    if( this.isDelivered() )
    {
    	msgToString  +=  ", message delivered in "+ this.getDeliveryTime()+" ms, success: "+this.deliverySuccess();
    }
    else
    {
    	msgToString  +=  ", message not delivered";
    }
    
    if( this.isBroadcast() )
    {
    	msgToString += ", is broadcast";
    }
    else
    {
    	msgToString += ", is unicast";
    }
    
    msgToString  +=  ")";

    return msgToString;
  }
 
}
