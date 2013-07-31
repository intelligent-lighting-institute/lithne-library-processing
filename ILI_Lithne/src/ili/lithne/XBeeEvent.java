
package ili.lithne;

import java.util.EventObject;

import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;

public class XBeeEvent extends EventObject
{
	/**
	 * 
	 */
	private static final long serialVersionUID 	 = -4017983963629528479L;

	public final static int NODE_IDENTIFICATION  =  0x95;
	public final static int DELIVERY_SUCCESS	 =	0x01;
	public final static int DELIVERY_FAILED		 =	0x02;
	
	private int _eventType;
	private XBeeAddress64  _address64	=  Lithne.UNKNOWN_64;
	private XBeeAddress16  _address16	=  Lithne.UNKNOWN_16;
	private boolean _broadcast			=	false;
  
  public XBeeEvent( Object source, int type )
  {
    super( source );
    this.setEventType( type );
  }
  public XBeeEvent( Object source, int type, XBeeAddress64 address64 )
  {
    super( source );
    this.setEventType( type );
    this.setXBeeAddress64( address64 );
  }
  public XBeeEvent( Object source, int type, XBeeAddress64 address64 , XBeeAddress16 address16 )
  {
    super( source );
    this.setEventType( type );
    this.setXBeeAddress64( address64 );
    this.setXBeeAddress16( address16 );
  }
  public XBeeEvent( Object source, int type, XBeeAddress16 address16 )
  {
    super( source );
    this.setEventType( type );
    this.setXBeeAddress16( address16 );
  }
  
  /**
   * Set the XBeeAddress64  
   **/
  public void setXBeeAddress64( XBeeAddress64 address64 )
  {
    this._address64  =  address64;
  }
  public XBeeAddress64 getXBeeAddress64()
  {
    return this._address64;
  }
  
  /**
   * Returns the reference to the Lithne object that passed fired this event
   * @return Lithne
   */
  public Lithne getLithne()
  {
	  return (Lithne) this.getSource();
  }
  
  /**
   * Sets the 16-bit XBeeAddress of this XBeeEvent
   * @param address16
   */
  public void setXBeeAddress16( XBeeAddress16 address16 )
  {
    this._address16  =  address16;
  }
  /**
   * Returns the 16-bit XBeeAddress for this XBeeEvent
   * @return XBeeAddress16
   */
  public XBeeAddress16 getXBeeAddress16()
  {
    return this._address16;
  }
  
  /**  Returns the type of event   **/
  public int getEventType()
  {
    return _eventType;
  }
  public void setEventType( int type )
  {
    this._eventType  =  type;
  }
}