package org.apache.ctakes.dictionary.creator.gui;


import org.apache.ctakes.dictionary.creator.gui.main.MainPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
public class CreatorGui {

   static private final Logger LOGGER = LogManager.getLogger( CreatorGui.class );


   static private JFrame createFrame() {
      final JFrame frame = new JFrame( "cTAKES Dictionary Creator" );
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
      final JMenuBar menuBar = new JMenuBar();
      final JMenu fileMenu = new JMenu( "File" );
      menuBar.add( fileMenu );

      frame.setJMenuBar( menuBar );
      System.setProperty( "apple.laf.useScreenMenuBar", "true" );
      return frame;
   }

   static private JComponent createMainPanel() {
      return new MainPanel();
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
   }

}
