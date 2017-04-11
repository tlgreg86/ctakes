package org.apache.ctakes.gui.pipeline.bit.info;


import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.gui.pipeline.bit.BitInfoPanel;
import org.apache.ctakes.gui.pipeline.bit.available.AvailablesListModel;
import org.apache.ctakes.gui.pipeline.bit.parameter.DefaultParameterHolder;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterHolder;
import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterInfoPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/5/2017
 */
final public class PipeBitInfoPanel extends BitInfoPanel {

   static private final Logger LOGGER = Logger.getLogger( "PipeBitInfoPanel" );

   public void setPipeBitInfoList( final JList<PipeBitInfo> pipeBitList ) {
      pipeBitList.getSelectionModel().addListSelectionListener( new PipeBitListListener( pipeBitList ) );
   }

   @Override
   protected String getNameLabelPrefix() {
      return "Pipe";
   }

   @Override
   protected JComponent createNameEditor() {
      return new JLabel();
   }

   @Override
   protected void setBitName( final String text ) {
      ((JLabel)_name).setText( text );
   }

   @Override
   protected ParameterInfoPanel createParameterInfoPanel() {
      return new ParameterInfoPanel();
   }


   private void setPipeBit( final PipeBitInfo info, final Class<?> pipeBitClass ) {
      if ( info == null ) {
         clear();
         return;
      }
      _pipeBitInfo = info;
      _pipeBitClass = pipeBitClass;
      setBitName( info.name() );
      _description.setText( info.description() );
      _dependencies.setText( Arrays.stream( info.dependencies() )
            .map( PipeBitInfo.TypeProduct::toString )
            .collect( Collectors.joining( ", " ) ) );
      _usables.setText( Arrays.stream( info.usables() )
            .map( PipeBitInfo.TypeProduct::toString )
            .collect( Collectors.joining( ", " ) ) );
      _products.setText( Arrays.stream( info.products() )
            .map( PipeBitInfo.TypeProduct::toString )
            .collect( Collectors.joining( ", " ) ) );
      final ParameterHolder holder = new DefaultParameterHolder( pipeBitClass );
      _parameterTableModel.setParameterHolder( holder );
      _parameterInfoPanel.setParameterHolder( holder );
   }

   private class PipeBitListListener implements ListSelectionListener {
      private final JList<PipeBitInfo> __pipeBitList;

      private PipeBitListListener( final JList<PipeBitInfo> pipeBitList ) {
         __pipeBitList = pipeBitList;
      }

      @Override
      public void valueChanged( final ListSelectionEvent event ) {
         if ( event.getValueIsAdjusting() ) {
            return;
         }
         final PipeBitInfo pipeBitInfo = __pipeBitList.getSelectedValue();
         setPipeBit( pipeBitInfo, ((AvailablesListModel)__pipeBitList.getModel()).getPipeBit( pipeBitInfo ) );
      }
   }

}
