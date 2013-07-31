
package ili.lithne;

import java.util.EventObject;

public class MessageEvent extends EventObject
{
  /**
	 * 
	 */
	private static final long serialVersionUID = -5882263171900674864L;

	public static final int NEW_MESSAGE       =  1;
	public static final int MESSAGE_RECEIVED  =  1;
	public static final int SEND_MESSAGE      =  2;
	public static final int MESSAGE_SEND      =  2;
	public static final int MESSAGE_SUCCESS   =  8;
	public static final int MESSAGE_FAILED    =  9;

	private Message _msg                      =  null;
	private int _eventType;
  
  public MessageEvent( Object source, int type, Message _message )
  {
    super( source );
    _eventType  =  type;
    this._msg  =  _message;
  }
  
  public int getEventType()
  {
    return _eventType;
  }
  public int getType()
  {
    return _eventType;
  }
  
  /**
   * Returns the milliseconds when this event was generated
   * @return
   */
  public int getMillis()
  {
	  return 0;
  }
  
  /**  Returns the message that has triggered the even.
       Returns NULL when it was not a message that triggered this event.  **/
  public Message getMessage()
  {
    return this._msg;
  }
  
  public Lithne getLithne()
  {
    return (Lithne) this.getSource();
  }
}