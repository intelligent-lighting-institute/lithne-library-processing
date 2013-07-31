
package ili.lithne;

import java.util.EventObject;

public class LithneEvent extends EventObject
{
  /**
	 * 
	 */
	private static final long serialVersionUID = -4710915432397000388L;
	public static final int CONNECTED         =  1;
	public static final int DISCONNECTED      =  2;
	public static final int XBEE_BAUDRATE     =  3;
	public static final int XBEE_PORT	      =  4;

	public static final int PACKET_SUCCESS    =  10;
	public static final int PACKET_FAILED     =  11;
    
	private int _eventType;
  
  public LithneEvent( Object source, int type ) 
  {
    super( source );
    _eventType  =  type;
  }  
    
  /**  Returns the type of event   **/
  public int getEventType()
  {
    return _eventType;
  }
  public int getType()
  {
    return _eventType;
  }
  
  public Lithne getLithne()
  {
    return (Lithne) this.getSource();
  }
  
  public int getMillis()
  {
	  return this.getLithne().getParent().millis();
  }
}