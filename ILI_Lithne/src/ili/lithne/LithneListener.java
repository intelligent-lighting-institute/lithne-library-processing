
package ili.lithne;

/**  This class deals with all events that are related to the Lithne
     library internals. Anything that falls out of the scope of the
     Messages and Node, is thrown through this Object  **/
public interface LithneListener 
{
  public void lithneEventReceived( LithneEvent event ); 
}