package org.apache.ctakes.gui.pipeline.bit.parameter;


import org.apache.log4j.Logger;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/5/2017
 */
final public class ParameterTableModel implements TableModel {

   static private final Logger LOGGER = Logger.getLogger( "ParameterTableModel" );

   static private final String[] COLUMN_NAMES = { "Parameter Name", "Value" };
   static private final Class<?>[] COLUMN_CLASSES = { ConfigurationParameter.class, String[].class };

   private final EventListenerList _listenerList = new EventListenerList();

   private ParameterHolder _parameterHolder;

   /**
    * Populate the list
    *
    * @param holder - holder with all parameter information
    */
   public void setParameterHolder( final ParameterHolder holder ) {
      final int oldSize = _parameterHolder == null ? 0 : _parameterHolder.getParameterCount();
      _parameterHolder = holder;
      if ( holder == null ) {
         if ( oldSize > 0 ) {
            fireTableChanged(
                  new TableModelEvent( this, 0, oldSize - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
         }
         return;
      }
      if ( holder.getParameterCount() > 0 ) {
         fireTableChanged( new TableModelEvent( this ) );
      } else if ( oldSize > 0 ) {
         fireTableChanged(
               new TableModelEvent( this, 0, oldSize - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE ) );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getRowCount() {
      if ( _parameterHolder == null ) {
         return 0;
      }
      return _parameterHolder.getParameterCount();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getColumnCount() {
      return COLUMN_NAMES.length;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getColumnName( final int columnIndex ) {
      return COLUMN_NAMES[ columnIndex ];
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Class<?> getColumnClass( final int columnIndex ) {
      return COLUMN_CLASSES[ columnIndex ];
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isCellEditable( final int rowIndex, final int columnIndex ) {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getValueAt( final int rowIndex, final int columnIndex ) {
      switch ( columnIndex ) {
         case 0:
            return _parameterHolder.getParameter( rowIndex );
         case 1:
            return Arrays.stream( _parameterHolder.getParameterValue( rowIndex ) )
                  .filter( v -> !ConfigurationParameter.NO_DEFAULT_VALUE.equals( v ) )
                  .collect( Collectors.joining( " , " ) );
      }
      return "ERROR";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex ) {
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addTableModelListener( final TableModelListener listener ) {
      _listenerList.add( TableModelListener.class, listener );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void removeTableModelListener( final TableModelListener listener ) {
      _listenerList.remove( TableModelListener.class, listener );
   }

   /**
    * Forwards the given notification event to all
    * <code>TableModelListeners</code> that registered
    * themselves as listeners for this table model.
    *
    * @param event the event to be forwarded
    * @see #addTableModelListener
    * @see TableModelEvent
    * @see EventListenerList
    */
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
