package org.apache.ctakes.dictionary.creator.gui.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
public class DirChooser extends JPanel {

   static private final Logger LOGGER = LogManager.getLogger( "DirChooser" );

   public DirChooser( final String name, final String defaultDirectory, final ActionListener dirChangeListener ) {
      super( new BorderLayout( 10, 10 ) );
      setBorder( new EmptyBorder( 2, 10, 2, 10 ) );
      final JLabel label = new JLabel( name );
      label.setPreferredSize( new Dimension( 100, 0 ) );
      label.setHorizontalAlignment( SwingConstants.TRAILING );
      final JTextField textField = new JTextField( defaultDirectory );
      textField.setEditable( false );
      final JButton openChooserButton = new JButton( new OpenDirAction( textField, dirChangeListener ) );
      add( label, BorderLayout.WEST );
      add( textField, BorderLayout.CENTER );
      add( openChooserButton, BorderLayout.EAST );

      textField.setDropTarget( new DirDropTarget( textField, dirChangeListener ) );
      textField.addActionListener( dirChangeListener );
   }

   /**
    * Opens the JFileChooser
    */
   private class OpenDirAction extends AbstractAction {
      private final JFileChooser __chooser;
      private final JTextComponent __textComponent;
      private final ActionListener __dirChangeListener;

      private OpenDirAction( final JTextComponent textComponent, final ActionListener dirChangeListener ) {
         super( "Select Directory" );
         __textComponent = textComponent;
         __chooser = new JFileChooser();
         __chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
         __dirChangeListener = dirChangeListener;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         final String startDirPath = __textComponent.getText();
         if ( startDirPath != null && !startDirPath.isEmpty() ) {
            final File startingDir = new File( startDirPath );
            if ( startingDir.exists() ) {
               __chooser.setCurrentDirectory( startingDir );
            }
         }
         final int option = __chooser.showOpenDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         final File file = __chooser.getSelectedFile();
         __textComponent.setText( file.getAbsolutePath() );
         final ActionEvent dirEvent = new ActionEvent( this, ActionEvent.ACTION_FIRST, file.getAbsolutePath() );
         __dirChangeListener.actionPerformed( dirEvent );
      }
   }


   private class DirDropTarget extends DropTarget {
      private final JTextComponent __textComponent;
      private final ActionListener __dirChangeListener;
      private DirDropTarget( final JTextComponent textComponent, final ActionListener dirChangeListener ) {
         __textComponent = textComponent;
         __dirChangeListener = dirChangeListener;
      }
      @Override
      public synchronized void drop( final DropTargetDropEvent event ) {
         event.acceptDrop( DnDConstants.ACTION_COPY );
         try {
            final Object values = event.getTransferable().getTransferData( DataFlavor.javaFileListFlavor );
            if ( !(values instanceof Iterable) ) {
               return;
            }
            for ( Object value : (Iterable)values ) {
               if ( !(value instanceof File) ) {
                  continue;
               }
               final File file = (File)value;
               if ( !file.isDirectory() ) {
                  continue;
               }
               __textComponent.setText( file.getAbsolutePath() );
               final ActionEvent dirEvent
                     = new ActionEvent( this, ActionEvent.ACTION_FIRST, file.getAbsolutePath() );
               __dirChangeListener.actionPerformed( dirEvent );
               return;
            }
         } catch ( UnsupportedFlavorException | IOException multE ) {
            LOGGER.warn( multE.getMessage() );
         }
      }
   }



}
