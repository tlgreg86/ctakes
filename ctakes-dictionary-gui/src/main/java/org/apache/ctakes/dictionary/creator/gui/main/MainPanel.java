package org.apache.ctakes.dictionary.creator.gui.main;

import org.apache.ctakes.dictionary.creator.gui.ctakes.DictionaryBuilder;
import org.apache.ctakes.dictionary.creator.gui.umls.MrconsoIndex;
import org.apache.ctakes.dictionary.creator.gui.umls.SourceTableModel;
import org.apache.ctakes.dictionary.creator.gui.umls.Tui;
import org.apache.ctakes.dictionary.creator.gui.umls.TuiTableModel;
import org.apache.ctakes.dictionary.creator.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/10/2015
 */
final public class MainPanel extends JPanel {

   static private final Logger LOGGER = LogManager.getLogger( "MainPanel" );

   private String _umlsDirPath = System.getProperty( "user.dir" );
   private String _ctakesPath = System.getProperty( "user.dir" );
   final TuiTableModel _tuiModel = new TuiTableModel();
   final SourceTableModel _sourceModel = new SourceTableModel();

   public MainPanel() {
      super( new BorderLayout() );

      final JComponent sourceDirPanel = new JPanel( new GridLayout( 2, 1 ) );
      sourceDirPanel.add( new DirChooser( "cTAKES Installation:", _umlsDirPath, new CtakesDirListener() ) );
      sourceDirPanel.add( new DirChooser( "UMLS Installation:", _ctakesPath, new UmlsDirListener() ) );
      add( sourceDirPanel, BorderLayout.NORTH );

      final JComponent centerPanel = new JPanel( new GridLayout( 1, 2 ) );
      centerPanel.add( createSourceTable( _sourceModel ) );
      centerPanel.add( createTuiTable( _tuiModel ) );
      add( centerPanel, BorderLayout.CENTER );
      add( createGoPanel(), BorderLayout.SOUTH );
   }

   private JComponent createTuiTable( final TableModel tuiModel ) {
      final JTable tuiTable = new JTable( tuiModel );
      tuiTable.setCellSelectionEnabled( false );
      tuiTable.setShowVerticalLines( false );
      tuiTable.setAutoCreateRowSorter( true );
      tuiTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      tuiTable.getColumnModel().getColumn( 0 ).setMaxWidth( 50 );
      tuiTable.getColumnModel().getColumn( 1 ).setMaxWidth( 50 );
      return new JScrollPane( tuiTable );
   }

   private JComponent createSourceTable( final TableModel sourceModel ) {
      final JTable tuiTable = new JTable( sourceModel );
      tuiTable.setCellSelectionEnabled( false );
      tuiTable.setShowVerticalLines( false );
      tuiTable.setAutoCreateRowSorter( true );
      tuiTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      tuiTable.getColumnModel().getColumn( 0 ).setMaxWidth( 50 );
      tuiTable.getColumnModel().getColumn( 1 ).setMaxWidth( 50 );
      return new JScrollPane( tuiTable );
   }

   private JComponent createGoPanel() {
      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBorder( new EmptyBorder( 2, 10, 2, 10 ) );
      final JLabel label = new JLabel( "Dictionary Name:" );
      label.setPreferredSize( new Dimension( 100, 0 ) );
      label.setHorizontalAlignment( SwingConstants.TRAILING );
      final JTextField textField = new JTextField( "custom" );
      final JButton buildButton = new JButton( new BuildDictionaryAction( textField ) );
      panel.add( label, BorderLayout.WEST );
      panel.add( textField, BorderLayout.CENTER );
      panel.add( buildButton, BorderLayout.EAST );
      return panel;
   }

   private String setUmlsDirPath( final String umlsDirPath ) {
      File mrConso = new File( umlsDirPath, "MRCONSO.RRF" );
      if ( mrConso.isFile() ) {
         _umlsDirPath = mrConso.getParentFile().getParent();
      }
      final String plusMetaPath = new File( umlsDirPath, "META" ).getPath();
      mrConso = new File( plusMetaPath, "MRCONSO.RRF" );
      if ( mrConso.isFile() ) {
         _umlsDirPath = umlsDirPath;
      } else {
         error( "Invalid UMLS Installation", umlsDirPath + " is not a valid path to a UMLS installation" );
      }
      return _umlsDirPath;
   }

   private void loadSources() {
      SwingUtilities.invokeLater( new SourceLoadRunner( _umlsDirPath ) );
   }

   private class SourceLoadRunner implements Runnable {
      private final String __umlsDirPath;
      private SourceLoadRunner( final String umlsDirPath ) {
         __umlsDirPath = umlsDirPath;
      }
      public void run() {
         SwingUtilities.getRoot( MainPanel.this ).setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         final File mrConso = new File( __umlsDirPath + "/META", "MRCONSO.RRF" );
         final String mrConsoPath = mrConso.getPath();
         LOGGER.info( "Parsing vocabulary types from " + mrConsoPath );
         final Collection<String> sources = new HashSet<>();
         try ( final BufferedReader reader = FileUtil.createReader( mrConsoPath ) ) {
            int lineCount = 0;
            java.util.List<String> tokens = FileUtil.readBsvTokens( reader, mrConsoPath );
            while ( tokens != null ) {
               lineCount++;
               if ( tokens.size() > MrconsoIndex.SOURCE._index ) {
                  sources.add( tokens.get( MrconsoIndex.SOURCE._index ) );
               }
               if ( lineCount % 100000 == 0 ) {
                  LOGGER.info( "File Line " + lineCount + "\t Vocabularies " + sources.size() );
               }
               tokens = FileUtil.readBsvTokens( reader, mrConsoPath );
            }
            LOGGER.info( "Parsed " + sources.size() + " vocabulary types" );
            _sourceModel.setSources( sources );
         } catch ( IOException ioE ) {
            error( "Vocabulary Parse Error", ioE.getMessage() );
         }
         SwingUtilities.getRoot( MainPanel.this ).setCursor( Cursor.getDefaultCursor() );
      }
   }

   private void buildDictionary( final String dictionaryName ) {
      SwingUtilities.invokeLater(
            new DictionaryBuildRunner( _umlsDirPath, _ctakesPath, dictionaryName, _sourceModel.getWantedSources(),
                  _sourceModel.getWantedTargets(), _tuiModel.getWantedTuis() ) );
   }

   private void error( final String title, final String message ) {
      LOGGER.error( message );
      JOptionPane.showMessageDialog( MainPanel.this, message, title, JOptionPane.ERROR_MESSAGE );
   }



   private class DictionaryBuildRunner implements Runnable {
      private final String __umlsDirPath;
      private final String __ctakesDirPath;
      private final String __dictionaryName;
      private final Collection<String> __wantedSources;
      private final Collection<String> __wantedTargets;
      private final Collection<Tui> __wantedTuis;
      private DictionaryBuildRunner( final String umlsDirPath, final String ctakesDirPath, final String dictionaryName,
                                     final Collection<String> wantedSources,
                                     final Collection<String> wantedTargets,
                                     final Collection<Tui> wantedTuis ) {
         __umlsDirPath = umlsDirPath;
         __ctakesDirPath = ctakesDirPath;
         __dictionaryName = dictionaryName;
         __wantedSources = wantedSources;
         __wantedTargets = new ArrayList<>( wantedTargets );
         __wantedTuis = new ArrayList<>( wantedTuis );
      }

      public void run() {
         SwingUtilities.getRoot( MainPanel.this ).setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         if ( DictionaryBuilder.buildDictionary( __umlsDirPath, __ctakesDirPath, __dictionaryName,
               Collections.singletonList( "ENG" ),
               __wantedSources, __wantedTargets, __wantedTuis ) ) {
            final String message = "Dictionary " + __dictionaryName + " successfully built in " + __ctakesDirPath;
            LOGGER.info( message );
            JOptionPane.showMessageDialog( MainPanel.this, message, "Dictionary Built", JOptionPane.INFORMATION_MESSAGE );
         } else {
            error( "Build Failure", "Dictionary " + __dictionaryName + " could not be built in " + __ctakesDirPath );
         }
         SwingUtilities.getRoot( MainPanel.this ).setCursor( Cursor.getDefaultCursor() );
      }
   }



   private class UmlsDirListener implements ActionListener {
      public void actionPerformed( final ActionEvent event ) {
         final String oldPath = _umlsDirPath;
         final String newPath = setUmlsDirPath( event.getActionCommand() );
         if ( !oldPath.equals( newPath ) ) {
            loadSources();
         }
      }
   }


   private class CtakesDirListener implements ActionListener {
      public void actionPerformed( final ActionEvent event ) {
         _ctakesPath = event.getActionCommand();
      }
   }


   /**
    * Opens the JFileChooser
    */
   private class BuildDictionaryAction extends AbstractAction {
      private final JTextComponent __textComponent;

      private BuildDictionaryAction( final JTextComponent textComponent ) {
         super( "Build Dictionary" );
         __textComponent = textComponent;
      }

      @Override
      public void actionPerformed( final ActionEvent event ) {
         final String dictionaryName = __textComponent.getText();
         if ( dictionaryName != null && !dictionaryName.isEmpty() ) {
            buildDictionary( dictionaryName.toLowerCase() );
         } else {
            error( "Invalid Dictionary Name", "Please Specify a Dictionary Name" );
         }
      }
   }

}
