/**
*  We include the serial and the lithne library.
**/
import processing.serial.*;
import ili.lithne.*;


/**
*  We first create a Lithne object (uppercase), with the name lithne (lowercase)
*  Then we create a new object called FunctionTable, which is on the next tab, and we name it ft
*  We also create a new NodeManager that you can use to keep track of which nodes are in your network
**/
Lithne lithne;
FunctionTable ft = new FunctionTable();
NodeManager nm   = new NodeManager();

// Just for example purposes, we keep track of whether the remote LED is on or off
int ledValue = 0; 

void setup()
{
  /**
  *  Here we create the Lithne object, attach it to 'this' (our current sketch) with the specified 
  *  com-port and the specified baudrate. Once we have done this, we inform the lithne library that
  *  it can begin its connection.
  **/
  lithne  =  new Lithne( this, "COM22", 115200 );
  lithne.begin();
  
  /* Add some nodes to your nodeManager using their XBeeAddress64 and a user-defined name */
  nm.addNode("00 13 a2 00 40 79 ce e7", "Node 1" );   // Match the XBeeAddress to the number on the back of your XBee Radio
  nm.addNode("00 13 a2 00 40 79 ce b7", "DeskLamp" ); // Preferrably, give the nodes a meaningful name; it helps =]
  
  /**
  *  This is an important step. Here we inform our FunctionTable object, that we want it to listen
  *  to 'MessageEvent'-s of the Lithne object. By doing so, messages will automatically be forwarded
  *  to this class. (more instructions on the next tab)
  **/
  lithne.addMessageListener( ft );

  /**
  *  These are examples of some static references you can make, for example for broadcast
  *  addresses, or for unknown addresses.
  **/
  println("16B DEFAULT:\t"+Lithne.addressToString( Lithne.UNKNOWN_16 ) );
  println("64B DEFAULT:\t"+Lithne.addressToString( Lithne.UNKNOWN_64 ) );
  println("BROADCAST:\t\t"+Lithne.addressToString( Lithne.BROADCAST ) );
  println("COORDINATOR:\t"+Lithne.addressToString( Lithne.COORDINATOR ) );
}

/**
*  In the main draw function, we don't have to do anything. The Lithne library handles all messages
*  received and send internally. You can do whatever you like, (or dislike) in this code. No questions asked.
**/
void draw()
{
  background(0);
}

/**
*  This is a demonstration of how easily you can send messages. Trigger this by pressing a key
*/
void keyPressed()
{
  /**
  *  First, we create a new Message, named msg
  **/
  Message msg  =  new Message();
  
  /**
  *  We then tell this message that it should be send to XBeeAddress64 of the type BROADCAST (00 00 00 00 00 00 FF FF)
  *  That we want the message to call function "setLed" which is a user-defined function name that should be matched on receiving devices
  *  To conclude, we print the fully composed message, to check it in the console (the black thingy below where text appears)
  **/
  msg.toXBeeAddress64( Lithne.BROADCAST );
  /* Alternatively, you may send it to  
  msg.toXBeeAddress64( nm.getXBeeAddress64("DeskLamp") );
  */
  msg.setFunction( "setLed" );
  /* In case of a BROADCAST we may narrow the amount of nodes that receive the message by setting a scope for the message.  
  *  Nodes can be configured to check if they belong to this scope and ignore or process it.
  *  In this case we send the message to all nodes in the "Office"
  */
  msg.setScope( "Office" );    
  
  /* We add an argument to say whether the LED should go on or off
  *  We also change the local variable, so next time we will do the opposite
  */
  msg.addArgument( ledValue );
  ledValue = abs(ledValue - 1);  // This toggles ledValue between 0 and 1
  
  println("SENDING NEW MESSAGE: " +msg.toString());
  
  /**
  *  Finally, when we are satisfied with our message, we inform the Lithne library to transmit it.
  *  Consider the analogy of writing a postcard (traditional, huh):
  *  You write a message, add to whom you want to send it, and then hand it over to the mailman.
  **/
  lithne.send( msg );
}