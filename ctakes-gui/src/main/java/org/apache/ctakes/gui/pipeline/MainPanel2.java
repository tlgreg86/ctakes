package org.apache.ctakes.gui.pipeline;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.component.DisablerPane;
import org.apache.ctakes.gui.component.LoggerPanel;
import org.apache.ctakes.gui.component.PositionedSplitPane;
import org.apache.ctakes.gui.component.SmoothTipList;
import org.apache.ctakes.gui.pipeline.bit.PipeBitFinder;
import org.apache.ctakes.gui.pipeline.bit.available.AvailablesListModel;
import org.apache.ctakes.gui.pipeline.bit.info.*;
import org.apache.ctakes.gui.pipeline.piper.PiperTextFilter;
import org.apache.ctakes.gui.util.IconLoader;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 12/20/2016
 */
final class MainPanel2 extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "MainPanel" );

   private final JFileChooser _chooser = new JFileChooser();

   private final AvailablesListModel _availablesListModel = new AvailablesListModel();
   private JList<PipeBitInfo> _availablesList;

   private DefaultStyledDocument _piperDocument;

   private JButton _newButton;
   private JButton _openButton;
   private JButton _saveButton;
   private JButton _runButton;
   private JButton _helpButton;


   MainPanel2() {
      super( new BorderLayout() );

      final JSplitPane logSplit = new PositionedSplitPane( JSplitPane.VERTICAL_SPLIT );
      logSplit.setTopComponent( createMainPanel() );
      logSplit.setBottomComponent( LoggerPanel.createLoggerPanel() );
      logSplit.setDividerLocation( 0.6d );

      add( createToolBar(), BorderLayout.NORTH );

      add( logSplit, BorderLayout.CENTER );
      SwingUtilities.invokeLater( new ButtonIconLoader() );
   }


   private JComponent createWestPanel() {
      final JTable fakeTable = new JTable();
      final JTableHeader fakeHeader = fakeTable.getTableHeader();
      final Component header = fakeHeader.getDefaultRenderer().getTableCellRendererComponent( null,
            "Available Pipe Bits", false, false, -1, -1 );
      ((JLabel)header).setHorizontalAlignment( SwingConstants.CENTER );

      _availablesList = createPipeBitList( _availablesListModel );
      final JScrollPane scroll = new JScrollPane( _availablesList );
      scroll.setColumnHeaderView( header );
      final JList<PipeBitInfo> rowHeaders = new JList<>( _availablesListModel );
      rowHeaders.setFixedCellHeight( 20 );
      rowHeaders.setCellRenderer( new RoleRenderer() );
      scroll.setRowHeaderView( rowHeaders );
      scroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

      final JSplitPane split = new PositionedSplitPane();
      split.setLeftComponent( scroll );
      split.setRightComponent( createBitInfoPanel( _availablesList ) );
      split.setDividerLocation( 0.3d );
      return split;
   }


   private JComponent createEastPanel() {
      _piperDocument = new DefaultStyledDocument();
      new PiperTextFilter( _piperDocument );
      final JTextPane textPane = new JTextPane( _piperDocument );
      return new JScrollPane( textPane );
   }


   private JComponent createMainPanel() {
      final JComponent westPanel = createWestPanel();
      final JComponent eastPanel = createEastPanel();
      return new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, westPanel, eastPanel );
   }

   private JToolBar createToolBar() {
      final JToolBar toolBar = new JToolBar();
      toolBar.setFloatable( false );
      toolBar.setRollover( true );
      _newButton = addButton( toolBar, "Create New Piper File" );
      _newButton.addActionListener( new NewPiperAction() );
      _openButton = addButton( toolBar, "Open Existing Piper File" );
      _openButton.addActionListener( new OpenPiperAction() );
      _saveButton = addButton( toolBar, "Save Current Piper File" );
      _saveButton.addActionListener( new SavePiperAction() );
      toolBar.addSeparator( new Dimension( 20, 0 ) );
      _helpButton = addButton( toolBar, "Help" );
      _helpButton.addActionListener( new HelpAction() );
      toolBar.add( Box.createHorizontalGlue() );
      _runButton = addButton( toolBar, "Run Current Piper File" );
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      return toolBar;
   }

   static private JButton addButton( final JToolBar toolBar, final String toolTip ) {
      toolBar.addSeparator( new Dimension( 10, 0 ) );
      final JButton button = new JButton();
      button.setFocusPainted( false );
      // prevents first button from having a painted border
      button.setFocusable( false );
      button.setToolTipText( toolTip );
      toolBar.add( button );
      return button;
   }

   static private JList<PipeBitInfo> createPipeBitList( final ListModel<PipeBitInfo> model ) {
      final JList<PipeBitInfo> bitList = new SmoothTipList<>( model );
      bitList.setCellRenderer( new PipeBitInfoRenderer() );
      bitList.setFixedCellHeight( 20 );
      return bitList;
   }

   static private PipeBitInfoPanel createBitInfoPanel( final JList<PipeBitInfo> list ) {
      final PipeBitInfoPanel pipeBitInfoPanel = new PipeBitInfoPanel();
      pipeBitInfoPanel.setPipeBitInfoList( list );
      pipeBitInfoPanel.setBorder( UIManager.getBorder( "ScrollPane.border" ) );
      return pipeBitInfoPanel;
   }

   void findPipeBits() {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute( new PiperBitParser() );
   }

   private class PiperBitParser implements Runnable {
      @Override
      public void run() {
         final JFrame frame = (JFrame)SwingUtilities.getRoot( MainPanel2.this );
         frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
         DisablerPane.getInstance().setVisible( true );
         PipeBitFinder.getInstance().scan();
         _availablesListModel.setPipeBits( PipeBitFinder.getInstance().getPipeBits() );
         DisablerPane.getInstance().setVisible( false );
         frame.setCursor( Cursor.getDefaultCursor() );
      }
   }


   /**
    * Simple Runnable that loads an icon
    */
   private final class ButtonIconLoader implements Runnable {
      @Override
      public void run() {
         final String dir = "org/apache/ctakes/gui/pipeline/icon/";
         final String newFile = "NewPiper.png";
         final String openFile = "OpenPiper.png";
         final String saveFile = "SavePiper.png";
         final String runFile = "RunPiper.png";
         final String helpFile = "Help_32.png";
         final Icon newIcon = IconLoader.loadIcon( dir + newFile );
         final Icon openIcon = IconLoader.loadIcon( dir + openFile );
         final Icon saveIcon = IconLoader.loadIcon( dir + saveFile );
         final Icon runIcon = IconLoader.loadIcon( dir + runFile );
         final Icon helpIcon = IconLoader.loadIcon( dir + helpFile );
         _newButton.setIcon( newIcon );
         _openButton.setIcon( openIcon );
         _saveButton.setIcon( saveIcon );
         _runButton.setIcon( runIcon );
         _helpButton.setIcon( helpIcon );
      }
   }

   private final class NewPiperAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         try {
            _piperDocument.remove( 0, _piperDocument.getLength() );
         } catch ( BadLocationException blE ) {
            LOGGER.warn( blE.getMessage() );
         }
      }
   }

   private final class OpenPiperAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final int option = _chooser.showOpenDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         String text = "";
         final File file = _chooser.getSelectedFile();
         try {
            text = Files.lines( Paths.get( file.getPath() ) ).collect( Collectors.joining() );
         } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
            return;
         }
         try {
            _piperDocument.remove( 0, _piperDocument.getLength() );
            _piperDocument.insertString( 0, text, null );
         } catch ( BadLocationException blE ) {
            LOGGER.warn( blE.getMessage() );
         }
      }
   }

   private final class SavePiperAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         if ( _piperDocument.getLength() == 0 ) {
            return;
         }
         final int option = _chooser.showSaveDialog( null );
         if ( option != JFileChooser.APPROVE_OPTION ) {
            return;
         }
         final File file = _chooser.getSelectedFile();
         try {
            final String text = _piperDocument.getText( 0, _piperDocument.getLength() );
            Files.write( Paths.get( file.getPath() ), text.getBytes() );
            _piperDocument.remove( 0, _piperDocument.getLength() );
            _piperDocument.insertString( 0, text, null );
         } catch ( BadLocationException | IOException multE ) {
            LOGGER.warn( multE.getMessage() );
         }
      }
   }

   static private final class HelpAction implements ActionListener {
      @Override
      public void actionPerformed( final ActionEvent event ) {
         final JPanel panel = new JPanel( new BorderLayout() );
         panel.add( new JLabel( "Dependency and Product Types." ), BorderLayout.NORTH );
         final JList<PipeBitInfo.TypeProduct> list = new JList<>( new TypeProductListModel() );
         list.setCellRenderer( new TypeProductRenderer() );
         panel.add( list, BorderLayout.CENTER );
         panel.add( new JLabel( "Types are associated with Pipe Bits." ), BorderLayout.SOUTH );
         JOptionPane.showMessageDialog( null, panel, "Type Products Help", PLAIN_MESSAGE, null );
      }
   }

}
