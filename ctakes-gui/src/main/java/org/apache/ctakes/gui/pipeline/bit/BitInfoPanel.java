package org.apache.ctakes.gui.pipeline.bit;

import org.apache.ctakes.gui.component.SmoothTipTable;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterCellRenderer;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterInfoPanel;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterTableModel;
import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/19/2017
 */
abstract public class BitInfoPanel extends JPanel {

   static private final Logger LOGGER = Logger.getLogger( "BitInfoPanel" );

   protected JComponent _name;
   protected JLabel _description;
   protected JLabel _dependencies;
   protected JLabel _usables;
   protected JLabel _outputs;

   protected ParameterTableModel _parameterTableModel;
   protected ParameterInfoPanel _parameterInfoPanel;

   public BitInfoPanel() {
      super( new BorderLayout( 5, 5 ) );

      add( createMainPanel(), BorderLayout.NORTH );

      _parameterTableModel = new ParameterTableModel();
      final JTable parameterTable = createParameterTable( _parameterTableModel );
      add( new JScrollPane( parameterTable ), BorderLayout.CENTER );

      _parameterInfoPanel = createParameterInfoPanel();
      _parameterInfoPanel.setParameterTable( parameterTable );
      add( _parameterInfoPanel, BorderLayout.SOUTH );
   }

   abstract protected String getNameLabelPrefix();

   abstract protected JComponent createNameEditor();

   abstract protected void setBitName( final String text );

   abstract protected ParameterInfoPanel createParameterInfoPanel();

   protected void clear() {
      setBitName( "" );
      _description.setText( "" );
      _dependencies.setText( "" );
      _usables.setText( "" );
      _outputs.setText( "" );
      _parameterTableModel.setParameterHolder( null );
      _parameterInfoPanel.setParameterHolder( null );
   }

   private JComponent createMainPanel() {
      _name = createNameEditor();
      final JComponent namePanel = createNamePanel( getNameLabelPrefix() + " Bit Name:", _name );
      _description = new JLabel();
      final JComponent descPanel = createNamePanel( "Description:", _description );
      _dependencies = new JLabel();
      final JComponent inPanel = createNamePanel( "Dependencies:", _dependencies );
      _usables = new JLabel();
      final JComponent usablePanel = createNamePanel( "Usable:", _usables );
      _outputs = new JLabel();
      final JComponent outPanel = createNamePanel( "Products:", _outputs );

      final JPanel panel = new JPanel( new GridLayout( 5, 1 ) );
      panel.add( namePanel );
      panel.add( descPanel );
      panel.add( inPanel );
      panel.add( usablePanel );
      panel.add( outPanel );
      return panel;
   }

   static private JTable createParameterTable( final TableModel model ) {
      final JTable table = new SmoothTipTable( model );
      table.setRowHeight( 20 );
      table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
      table.getColumnModel().getColumn( 0 ).setPreferredWidth( 100 );
      final TableRowSorter<TableModel> sorter = new TableRowSorter<>( model );
      table.setAutoCreateRowSorter( true );
      table.setRowSorter( sorter );
      table.setRowSelectionAllowed( true );
      table.setCellSelectionEnabled( true );
      table.setDefaultRenderer( ConfigurationParameter.class, new ParameterCellRenderer() );
      ListSelectionModel selectionModel = table.getSelectionModel();
      selectionModel.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
      return table;
   }

   static private JComponent createNamePanel( final String name, final JComponent nameLabel ) {
      final JPanel panel = new JPanel( new BorderLayout( 10, 10 ) );
      panel.setBorder( new EmptyBorder( 2, 10, 2, 10 ) );
      final JLabel label = new JLabel( name );
      label.setPreferredSize( new Dimension( 90, 20 ) );
      label.setHorizontalAlignment( SwingConstants.TRAILING );
      final Border emptyBorder = new EmptyBorder( 0, 10, 0, 0 );
      final Border border
            = new CompoundBorder( UIManager.getLookAndFeelDefaults().getBorder( "TextField.border" ), emptyBorder );
      nameLabel.setBorder( border );
      panel.add( label, BorderLayout.WEST );
      panel.add( nameLabel, BorderLayout.CENTER );
      return panel;
   }

}
