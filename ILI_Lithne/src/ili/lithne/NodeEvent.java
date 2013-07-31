
package ili.lithne;

import java.util.EventObject;


public class NodeEvent extends EventObject
{
  /**
	 * 
	 */
	private static final long serialVersionUID = 8954132728474964291L;
	
	public static final int NODE_ADDED       =  1; 
	public static final int NODEREMOVED      =  2;
	public static final int NODEUPDATED      =  3;
	public static final int NODE_RST_16B     =  4;
	public static final int NODE_INFO        =  5;
	public static final int NODEJOINED       =  10;
	public static final int NODEJOIN         =  10;
	public static final int NODE_IDENTIFIER  =  10;

	private Node _node                       =  null;
	private int _eventType;
  
  public NodeEvent( Object source, int type )
  {
    super( source );
    _eventType  =  type;
    this._node  =  (Node) source;
  }
  
  public int getEventType()
  {
    return _eventType;
  }
  public int getType()
  {
    return _eventType;
  }
  
  /**  Returns the message that has triggered the even.
       Returns NULL when it was not a message that triggered this event.  **/
  public Node getNode()
  {
    return this._node;
  }
}