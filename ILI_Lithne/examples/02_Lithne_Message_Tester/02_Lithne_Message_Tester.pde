/**
*  We include Lithne library and a library to generate pop-up messages
**/
//import processing.serial.*;
import ili.lithne.*;
import javax.swing.JOptionPane;

/**
*  We first create a Lithne object (uppercase), with the name lithne (lowercase)
*  Then we create a new object called FunctionTable, which is on the next tab, and we name it ft
**/
Lithne lithne;
FunctionTable ft = new FunctionTable();

void setup()
{
  /**
  *  Here we create the Lithne object, attach it to 'this' (our current sketch) with the specified 
  *  com-port and the specified baudrate. Once we have done this, we inform the lithne library that
  *  it can begin its connection.
  **/
  lithne  =  new Lithne( this, "/dev/tty.usbserial-A600KMDQ", 115200 );
  lithne.enableDebug();
  lithne.begin();
  
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
  *  That we want the message to call function 255 (we use this for error or network messages)
  *  Then we add the argument of the mouse X (horizontal) position.
  *  And we finally add the mouse Y (vertical) position as an argument.)
  *  To conclude, we print the fully composed message, to check it in the console (the black thingy below where text appears)
  **/
  noLoop();
  msg.toXBeeAddress64( Lithne.BROADCAST );
  
  String destination = JOptionPane.showInputDialog("Specify the 64-bit destination address in the format 'XX XX XX XX XX XX XX XX'", "00 13 A2 00 XX XX XX XX");
  
  /**
  *  From the specified information, we try to make a 64-bit address.
  *  If we can't succesfully make a 64-bit address, we set it to BROADCAST
  **/
  try
  {
    msg.toXBeeAddress64( destination );
  }
  catch( Exception e )
  {
    println("Could not create a XBeeAddress64 from the specified information. Setting the destination to Lithne.BROADCAST");
  }
  
  /**
  *  Next, we ask for a function to call. The String that is entered is converted to
  *  a number internally. If you want to, you can also specify an integer value
  **/
  String targetFunction = JOptionPane.showInputDialog("Please specify which remote function you wish to call.", "ERROR");
  if( int(targetFunction) != 0 )
  {
    msg.setFunction( int(targetFunction) );
  }
  else
  {
    msg.setFunction( targetFunction );
  }
  
  
  String arguments = JOptionPane.showInputDialog("Please specify any arguments you wish to add to the message, or clear if you do not wish to add argument.\nUse comma's to seperate between arguments.", "1,1,2,3,5,8");
  String[] separateArguments  =  split( arguments, "," );
  
  for( int i=0; i<separateArguments.length; i++ )
  {
    msg.addArgument( int( trim( separateArguments[i] ) ) );
  }  
  
  println("This is the final message you composed:" +msg.toString());
  
  /**
  *  Finally, when we are satisfied with our message, we inform the Lithne library to transmit it.
  *  Consider the analogy of writing a postcard (traditional, huh):
  *  You write a message, add to whom you want to send it, and then hand it over to the mailman.
  **/
  lithne.send( msg );
  
  loop();
}