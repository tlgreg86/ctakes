package org.apache.ctakes.gui.pipeline;


import org.apache.ctakes.core.pipeline.PiperFileRunner;
import org.apache.ctakes.gui.component.*;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterCellRenderer;
import org.apache.ctakes.gui.pipeline.piper.PiperTextFilter;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileView;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 4/18/2017
 */
public class PiperRunnerPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "PiperRunnerPanel" );

   private final JFileChooser _piperChooser = new JFileChooser();
   private final JFileChooser _parmChooser = new JFileChooser();
   private DefaultStyledDocument _piperDocument;
   private PiperTextFilter _piperTextFilter;

   private String _piperPath = "";

   private JTextPane _textPane;
   private JTable _cliTable;

   private JButton _openButton;
   private JButton _saveButton;
   private JButton _parmButton;
   private JButton _runButton;

   private final java.util.List<String> _cliNames = new ArrayList<>();
   private final Map<String, Character> _cliChars = new HashMap<>();
   private final Map<String,String> _cliValues = new HashMap<>();
   private final String[] STANDARD_NAMES = { "InputDirectory (-i)", "OutputDirectory (-o)", "SubDirectory (-s)",
         "LookupXml (-l)", "UMLS Username (--user)", "UMLS Password (--pass)", "XMI Output (--xmiOut)" };
   private final Map<String,String> _standardChars = new HashMap<>( STANDARD_NAMES.length );
   private final Map<String,String> _standardValues = new HashMap<>( STANDARD_NAMES.length );


   PiperRunnerPanel() {
      super( new BorderLayout() );
      final String[] STANDARD_OPTIONS = { "-i", "-o", "-s", "-l", "--user", "--pass", "--xmiOut" };
      for ( int i=0; i<STANDARD_NAMES.length; i++ ) {
         _standardChars.put( STANDARD_NAMES[ i ], STANDARD_OPTIONS[ i ] );
      }
      final JSplitPane logSplit = new PositionedSplitPane( JSplitPane.VERTICAL_SPLIT );
      logSplit.setTopComponent( createMainPanel() );
      logSplit.setBottomComponent( LoggerPanel.createLoggerPanel() );
      logSplit.setDividerLocation( 0.6d );

      add( createToolBar(), BorderLayout.NORTH );

      add( logSplit, BorderLayout.CENTER );
      SwingUtilities.invokeLater( new ButtonIconLoader() );

      _piperChooser.setFileFilter( new FileNameExtensionFilter( "Pipeline Definition (Piper) File", "piper") );
      _piperChooser.setFileView( new PiperFileView() );
      _parmChooser.setFileFilter( new FileNameExtensionFilter( "Pipeline Definition (Piper) Parameter File", "piper_cli" ) );
      _parmChooser.setFileView( new PiperFileView() );
   }

   private JToolBar createToolBar() {
      final JToolBar toolBar = new JToolBar();
      toolBar.setFloatable( false );
      toolBar.setRollover( true );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      _openButton = addButton( toolBar, "Open Existing Piper File" );
      _openButton.addActionListener( new OpenPiperAction() );
      toolBar.addSeparator( new Dimension( 50, 0 ) );
      _parmButton = addButton( toolBar, "Open Parameter File" );
      _parmButton.addActionListener( new OpenParmAction() );
      _saveButton = addButton( toolBar, "Save Parameter File" );
      _saveButton.addActionListener( new SaveParmAction() );
      toolBar.add( Box.createHorizontalGlue() );
      _runButton = addButton( toolBar, "Run Current Piper File" );
      _runButton.addActionListener( new RunAction() );
      _runButton.setEnabled( false );
      return toolBar;
   }

   static private JButton addButton( final JToolBar toolBar, final String toolTip ) {
      final JButton button = new JButton();
      button.setFocusPainted( false );
      // prevents first button from having a painted border
      button.setFocusable( false );
      button.setToolTipText( toolTip );
      toolBar.add( button );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      return button;
   }

   private JComponent createMainPanel() {
      final JComponent westPanel = createWestPanel();
      final JComponent eastPanel = createEastPanel();
      final JSplitPane mainSplit = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, westPanel, eastPanel );
      mainSplit.setDividerLocation( 0.6 );
      return mainSplit;
   }

   private JComponent createEastPanel() {
      _piperDocument = new DefaultStyledDocument();
      _piperTextFilter = new PiperTextFilter( _piperDocument );
      _textPane = new JTextPane( _piperDocument );
      _textPane.putClientProperty( "caretWidth", 2 );
      _textPane.setCaretColor( Color.MAGENTA );
      _textPane.setEditable( false );
      final JScrollPane scroll = new JScrollPane( _textPane );
      final TextLineNumber lineNumber = new TextLineNumber( _textPane, 2 );
      scroll.setRowHeaderView( lineNumber );
      scroll.setMinimumSize( new Dimension( 100, 10 ) );
      return scroll;
   }

   private JComponent createWestPanel() {
      return new JScrollPane( createCliTable() );
   }

   private JComponent createCliTable() {
      _cliTable = new SmoothTipTable( new CliOptionModel() );
      _cliTable.setRowHeight( 20 );
      _cliTable.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      _cliTable.getColumnModel().getColumn( 0 ).setPreferredWidth( 100 );
      _cliTable.getColumnModel().getColumn( 2 ).setMaxWidth( 25 );
      _cliTable.setRowSelectionAllowed( true );
      _cliTable.setCellSelectionEnabled( true );
      _cliTable.setDefaultRenderer( ConfigurationParameter.class, new ParameterCellRenderer() );
      final FileTableCellEditor fileEditor = new FileTableCellEditor();
      fileEditor.getFileChooser().setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
      _cliTable.setDefaultRenderer( File.class, fileEditor );
      _cliTable.setDefaultEditor( File.class, fileEditor );
      ListSelectionModel selectionModel = _cliTable.getSelectionModel();
      selectionModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
      return _cliTable;
   }


   // -i, -o, -s, -l --user --pass --xmiOut
   private final class CliOptionModel implements TableModel {
      private final String[] COLUMN_NAMES = { "Parameter Name", "Value", "" };
      private final Class<?>[] COLUMN_CLASSES = { String.class, String.class, File.class };
      private final EventListenerList _listenerList = new EventListenerList();
      public int getRowCount() {
         return STANDARD_NAMES.length + _cliNames.size();
      }
      @Override
      public int getColumnCount() {
         return 3;
      }
      @Override
      public String getColumnName( final int column ) {
         return COLUMN_NAMES[ column ];
      }
      @Override
      public Class<?> getColumnClass( final int column ) {
         return COLUMN_CLASSES[ column ];
      }
      @Override
      public Object getValueAt( final int row, final int column ) {
         if ( column == 0 ) {
            if ( row < STANDARD_NAMES.length ) {
               return STANDARD_NAMES[ row ];
            }
            return _cliNames.get( row - STANDARD_NAMES.length );
         } else if ( column == 1 ) {
            if ( row < STANDARD_NAMES.length ) {
               return _standardValues.getOrDefault( STANDARD_NAMES[ row ], "" );
            }
            return _cliValues.getOrDefault( _cliNames.get( row - STANDARD_NAMES.length ), "" );
         } else if ( column == 2 ) {
            final String path = (String)getValueAt( row, 1 );
            return new File( path );
         }
         return "ERROR";
      }
      @Override
      public boolean isCellEditable( final int row, final int column ) {
         return column != 0;
      }
      @Override
      public void setValueAt( final Object aValue, final int row, final int column ) {
         if ( column == 1 ) {
            if ( row < STANDARD_NAMES.length ) {
               _standardValues.put( STANDARD_NAMES[ row ], (String)aValue );
            } else {
               _cliValues.put( _cliNames.get( row - STANDARD_NAMES.length ), (String) aValue );
            }
            fireTableChanged( new TableModelEvent( this, row, row, column ) );
         } else if ( column == 2 && File.class.isInstance( aValue ) ) {
            final String path = ((File)aValue).getPath();
            if ( row < STANDARD_NAMES.length ) {
               _standardValues.put( STANDARD_NAMES[ row ], path );
            } else {
               _cliValues.put( _cliNames.get( row - STANDARD_NAMES.length ), path );
            }
            fireTableChanged( new TableModelEvent( this, row, row, 1 ) );
         }
      }
      @Override
      public void addTableModelListener( final TableModelListener listener ) {
         _listenerList.add( TableModelListener.class, listener );
      }
      @Override
      public void removeTableModelListener( final TableModelListener listener ) {
         _listenerList.remove( TableModelListener.class, listener );
      }
      private void fireTableChanged( final TableModelEvent event ) {
         // Guaranteed to return a non-null array
         Object[] listeners = _listenerList.getListenerList();
         // Process the listeners last to first, notifying
         // those that are interested in this event
         for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[ i ] == TableModelListener.class ) {
               ((TableModelListener)listeners[ i + 1 ]).tableChanged( event );
            }
         }
      }
   }





   private final class OpenPiperAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final int option = _piperChooser.showOpenDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         final File file = _piperChooser.getSelectedFile();
         String text = loadPiperText( file.getPath() );
          try {
            _piperDocument.remove( 0, _piperDocument.getLength() );
            _piperDocument.insertString( 0, text, null );
         } catch ( BadLocationException blE ) {
            LOGGER.warn( blE.getMessage() );
         }
         _cliNames.clear();
         _cliChars.clear();
         _cliValues.clear();
         if ( !loadPiperCli( text ) ) {
            error( "Could not load Piper File: " + file.getPath() );
            return;
         }
         _piperPath = file.getPath();
         _runButton.setEnabled( true );
         _cliTable.revalidate();
         _cliTable.repaint();
      }
      private String loadPiperText( final String filePath ) {
         LOGGER.info( "Loading Piper File: " + filePath);
         try {
            return  Files.lines( Paths.get( filePath ) ).collect( Collectors.joining( "\n" ) );
         } catch ( IOException ioE ) {
            error( ioE.getMessage() );
            return "";
         }
      }
      private boolean loadPiperCli( final String text ) {
         for ( String line : text.split( "\\n" ) ) {
            if ( line.startsWith( "cli " ) && line.length() > 5 ) {
               final String[] allValues = line.substring( 4 ).split( "\\s+" );
               for ( String allValue : allValues ) {
                  final String[] values = allValue.split( "=" );
                  if ( values.length != 2 || values[1].length() != 1 ) {
                     error( "Illegal cli values: " + line );
                     return false;
                  }
                  final String name = values[ 0 ] + " (-" + values[1] + ")";
                  if ( _cliChars.put( name, values[1].charAt( 0 ) ) != null ) {
                     error( "Repeated cli value: " + line );
                     return false;
                  }
                  _cliNames.add( name );
               }
            } else if ( line.startsWith( "load " ) && line.length() > 6 ) {
               final String filePath = line.substring( 6 ).trim();
               final String subText = loadPiperText( filePath );
               if ( subText.isEmpty() ) {
                  error( "Piper File not found: " + filePath );
                  return false;
               }
               if ( !loadPiperCli( subText ) ) {
                  error( "Could not load Piper File: " + filePath );
                  return false;
               }
            }
         }
         return true;
      }
   }

   private void error( final String error ) {
      LOGGER.error( error );
      JOptionPane.showMessageDialog( this, error, "Piper File Error", JOptionPane.ERROR_MESSAGE );
      _piperPath = "";
      _cliNames.clear();
      _cliChars.clear();
      _cliValues.clear();
      _runButton.setEnabled( false );
      _cliTable.revalidate();
      _cliTable.repaint();
   }

   private final class OpenParmAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final int option = _parmChooser.showOpenDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         final File file = _parmChooser.getSelectedFile();
         LOGGER.info( "Loading Piper cli values file: " + file.getPath() );
         try {
            Files.lines( Paths.get( file.getPath() ) ).forEach( this::loadValueLine );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
         _cliTable.revalidate();
         _cliTable.repaint();
      }
      private void loadValueLine( final String line ) {
         if ( line.trim().isEmpty() ) {
            return;
         }
         final String[] values = line.trim().split( "=" );
         if ( values.length != 2 ) {
            LOGGER.error( "Invalid parameter line: " + line );
            return;
         }
         if ( _standardChars.keySet().contains( values[0] ) ) {
            _standardValues.put( values[0], values[1] );
         } else if ( _cliNames.contains( values[0] ) ) {
            _cliValues.put( values[ 0 ], values[ 1 ] );
         } else {
            LOGGER.warn( "Unknown parameter: " + values[0] );
         }
      }
   }

   private final class SaveParmAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final int option = _parmChooser.showSaveDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         final File file = _parmChooser.getSelectedFile();
         String path = file.getPath();
         if ( !path.endsWith( ".piper_cli" ) ) {
            path += ".piper_cli";
         }
         LOGGER.info( "Saving Piper cli values file: " + path );
         final Collection<String> lines = Arrays.stream( STANDARD_NAMES )
               .filter( n -> _standardValues.get( n ) != null )
               .filter( n -> !_standardValues.get( n ).isEmpty() )
               .map( n -> n + "=" + _standardValues.get( n ) + "\n" )
               .collect( Collectors.toList() );
         _cliValues.entrySet().stream()
               .filter( e -> e.getValue() != null )
               .filter( e -> !e.getValue().isEmpty() )
               .forEach( e -> lines.add( e.getKey() + "=" + e.getValue() + "\n" ) );
         try {
            Files.write( Paths.get( path ),  lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
         }
      }
   }


   private final class RunAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _runButton == null ) {
            return;
         }
         LOGGER.info( "Running Piper File ..." );
         final ExecutorService executor = Executors.newSingleThreadExecutor();
         executor.execute( new PiperFileRunnable() );
         executor.shutdown();
      }
   }

   private class PiperFileRunnable implements Runnable {
      @Override
      public void run() {
         final JFrame frame = (JFrame)SwingUtilities.getRoot( PiperRunnerPanel.this );
         frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         final java.util.List<String> args = new ArrayList<>();
         args.add( 0, "-p" );
         args.add( 1, _piperPath );
         for ( String standard : STANDARD_NAMES ) {
            final String value = _standardValues.get( standard );
            if ( value != null && !value.isEmpty() ) {
               args.add( _standardChars.get( standard ) );
               args.add( value );
            }
         }
         for ( String cli : _cliNames ) {
            final String value = _cliValues.get( cli );
            if ( value != null && !value.isEmpty() ) {
               args.add( _cliChars.get( cli ) + "" );
               args.add( value );
            }
         }
         try {
            PiperFileRunner.run( args.toArray( new String[ args.size() ] ) );
         } catch ( Throwable t ) {
            LOGGER.error( "Pipeline Run caused Exception:", t );
         }
         DisablerPane.getInstance().setVisible( false );
         frame.setCursor( Cursor.getDefaultCursor() );
      }
   }

   // TODO refactor ; extract the common inner classes here and in mainpanel2
   static private final class PiperFileView extends FileView {
      private Icon _piperIcon = null;

      private PiperFileView() {
         SwingUtilities.invokeLater( new FileIconLoader() );
      }

      @Override
      public String getTypeDescription( final File file ) {
         final String name = file.getName();
         if ( name.endsWith( ".piper" ) ) {
            return "Pipeline Definition (Piper) file.";
         }
         return super.getTypeDescription( file );
      }

      @Override
      public Icon getIcon( final File file ) {
         final String name = file.getName();
         if ( name.endsWith( ".piper" ) && _piperIcon != null ) {
            return _piperIcon;
         }
         return super.getIcon( file );
      }

      /**
       * Simple Runnable that loads an icon
       */
      private final class FileIconLoader implements Runnable {
         @Override
         public void run() {
            final String dir = "org/apache/ctakes/gui/pipeline/icon/";
            final String piperPng = "PiperFile.png";
            _piperIcon = IconLoader.loadIcon( dir + piperPng );
         }
      }
   }

   /**
    * Simple Runnable that loads an icon
    */
   private final class ButtonIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String openPng = "OpenPiper.png";
         final String parmPng = "BoxOfStuff.png";
         final String savePng = "Package.png";
         final String runPng = "RunPiper.png";
         final Icon openIcon = IconLoader.loadIcon( dir + openPng );
         final Icon parmIcon = IconLoader.loadIcon( dir + parmPng );
         final Icon saveIcon = IconLoader.loadIcon( dir + savePng );
         final Icon runIcon = IconLoader.loadIcon( dir + runPng );
         _openButton.setIcon( openIcon );
         _parmButton.setIcon( parmIcon );
         _saveButton.setIcon( saveIcon );
         _runButton.setIcon( runIcon );
      }
   }

}
