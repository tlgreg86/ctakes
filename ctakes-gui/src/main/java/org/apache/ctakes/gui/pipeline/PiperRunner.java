package org.apache.ctakes.gui.pipeline;


import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/18/2017
 */
final public class PiperRunner {

   static private final Logger LOGGER = Logger.getLogger( "PiperRunner" );

   static private JFrame createFrame() {
      final JFrame frame = new JFrame( "cTAKES Piper File Submitter" );
      frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
      // Use 1024 x 768 as the minimum required resolution (XGA)
      // iPhone 3 : 480 x 320 (3:2, HVGA)
      // iPhone 4 : 960 x 640  (3:2, unique to Apple)
      // iPhone 5 : 1136 x 640 (under 16:9, unique to Apple)
      // iPad 3&4 : 2048 x 1536 (4:3, QXGA)
      // iPad Mini: 1024 x 768 (4:3, XGA)
      final Dimension size = new Dimension( 800, 600 );
      frame.setSize( size );
      frame.setMinimumSize( size );
//      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
      return frame;
   }


   static private JComponent createMainPanel() {
      return new PiperRunnerPanel();
   }


   public static void main( final String... args ) {
      try {
         UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
         UIManager.getDefaults().put( "SplitPane.border", BorderFactory.createEmptyBorder() );
      } catch ( ClassNotFoundException | InstantiationException
            | IllegalAccessException | UnsupportedLookAndFeelException multE ) {
         LOGGER.error( multE.getLocalizedMessage() );
      }
      final JFrame frame = createFrame();
      final JComponent mainPanel = createMainPanel();
      frame.add( mainPanel );
      frame.pack();
      frame.setVisible( true );
      DisablerPane.getInstance().initialize( frame );
      LOGGER.info( "1. Load a Piper File." );
      LOGGER.info( "   Once selected, the right panel will be display the piper file text." );
      LOGGER.info( "2. Edit Command Line Interface (cli) values for Pipe Bit Parameters." );
      LOGGER.info( "   Parameters and values are displayed in the left table." );
      LOGGER.info( "   Values can be edited directly on the table." );
      LOGGER.info( "   In the rightmost column is a button to open a Filechooser if needed." );
      LOGGER.info( "3. Alternatively, load a previously saved piper_cli parameter file." );
      LOGGER.info( "4. Save your Command Line Interface (cli) values to a piper_cli parameter file." );
      LOGGER.info( "5. Run the Pipeline." );
   }

}
