
package ili.lithne;

/**  Handles XBee Events as thrown by the Lithne class  **/
public interface XBeeListener
{
  public void xbeeEventReceived( XBeeEvent event );
}