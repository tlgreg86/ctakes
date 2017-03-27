package org.apache.ctakes.gui.pipeline.bit.user;

import org.apache.ctakes.gui.pipeline.bit.parameter.ParameterInfoPanel;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.util.logging.Logger;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/20/2017
 */
public class UserParameterInfoPanel extends ParameterInfoPanel {

   static private final Logger LOGGER = Logger.getLogger( "UserParameterInfoPanel" );

   protected String getValueLabelPrefix() {
      return "User";
   }

   protected JComponent createValuesEditor() {
      return new JTextField();
   }

   protected void setParameterValues( final String values ) {
      ((JTextComponent)_values).setText( values );
   }

}
