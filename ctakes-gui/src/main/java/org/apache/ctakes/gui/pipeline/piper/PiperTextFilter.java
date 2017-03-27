package org.apache.ctakes.gui.pipeline.piper;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 3/25/2017
 */
final public class PiperTextFilter extends DocumentFilter {

   static private final Logger LOGGER = Logger.getLogger( "PiperTextFilter" );

   final private TextFormatter _textFormatter;


   public PiperTextFilter( final DefaultStyledDocument document ) {
      _textFormatter = new TextFormatter( document );
      document.setDocumentFilter( this );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void remove( final FilterBypass fb, final int begin, final int length ) throws BadLocationException {
      String text = "";
      final Document document = fb.getDocument();
      if ( begin + length <= document.getLength() ) {
         text = fb.getDocument().getText( begin, length );
      }
      super.remove( fb, begin, length );
      if ( shouldReformat( document, begin, length ) ) {
         formatText( document );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void insertString( final FilterBypass fb, final int begin, final String text, final AttributeSet attr )
         throws BadLocationException {
      super.insertString( fb, begin, text, attr );
      if ( shouldReformat( fb.getDocument(), begin, text.length() ) ) {
         formatText( fb.getDocument() );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void replace( final FilterBypass fb, final int begin, final int length, final String text,
                        final AttributeSet attrs )
         throws BadLocationException {
      super.replace( fb, begin, length, text, attrs );
      if ( shouldReformat( fb.getDocument(), begin, length ) ) {
         formatText( fb.getDocument() );
      }
   }

   static private boolean shouldReformat( final Document document, final int begin, final int length )
         throws BadLocationException {
      final int testLength = Math.min( length + 2, document.getLength() - begin );
      final String deltaText = document.getText( begin, testLength );
      return deltaText.contains( " " ) || deltaText.contains( "\n" ) || deltaText.contains( "\t" );
   }

   private void formatText( final Document document ) {
      if ( StyledDocument.class.isInstance( document ) ) {
         SwingUtilities.invokeLater( _textFormatter );
      }
   }

   static private final class TextFormatter implements Runnable {
      final private StyledDocument _document;
      final private Map<String, Style> _styles = new HashMap<>();

      private TextFormatter( final StyledDocument document ) {
         _document = document;
         createStyles();
      }

      @Override
      public void run() {
         try {
            final String text = _document.getText( 0, _document.getLength() );
            int lineBegin = 0;
            boolean lineEnded = false;
            for ( int i = 0; i < _document.getLength(); i++ ) {
               lineEnded = false;
               if ( text.charAt( i ) == '\n' ) {
                  formatLine( lineBegin, i );
                  lineBegin = i + 1;
                  lineEnded = true;
               }
            }
            if ( !lineEnded ) {
               formatLine( lineBegin, _document.getLength() );
            }
         } catch ( BadLocationException blE ) {
            LOGGER.error( blE.getMessage() );
         }
      }

      private void createStyles() {
         createStyle( "PLAIN", Color.BLACK, "PLAIN" );
         createStyle( "COMMENT", Color.GRAY, "COMMENT" );
         final Style error = createStyle( "ERROR", Color.RED, "ERROR" );
         StyleConstants.setStrikeThrough( error, true );
         createStyle( "PARAMETER", Color.YELLOW, "PARAMETER" );
         createStyle( "LOAD", Color.MAGENTA, "load" );
         createStyle( "PACKAGE", Color.YELLOW.darker(), "package" );
         createStyle( "SET", Color.ORANGE.darker(), "set", "cli" );
         createStyle( "READER", Color.GREEN.darker().darker(), "reader", "readFiles" );
         createStyle( "ADD", Color.CYAN.darker().darker(), "add", "addLogged", "addDescription", "addLast" );
         createStyle( "WRITE_XMI", Color.BLUE, "writeXmis", "collectCuis", "collectEntities" );
      }

      private Style createStyle( final String name, final Color color, final String... keys ) {
         final Style style = _document.addStyle( name, null );
         StyleConstants.setForeground( style, color );
         Arrays.stream( keys ).forEach( k -> _styles.put( k, style ) );
         return style;
      }

      private void formatLine( final int begin, final int end ) throws BadLocationException {
         final int length = end - begin;
         if ( length <= 0 ) {
            return;
         }
         final String text = _document.getText( begin, length );
         if ( text.startsWith( "#" ) || text.startsWith( "//" ) || text.startsWith( "!" ) ) {
            _document.setCharacterAttributes( begin, length, _styles.get( "COMMENT" ), true );
            return;
         }
         int commandEnd = text.indexOf( ' ' );
         if ( commandEnd < 0 ) {
            commandEnd = length;
         }
         final Style commandStyle = getCommandStyle( text.substring( 0, commandEnd ) );
         _document.setCharacterAttributes( begin, commandEnd, commandStyle, true );
         if ( length > commandEnd ) {
            _document.setCharacterAttributes( begin + commandEnd, length - commandEnd, _styles.get( "PLAIN" ), true );
         }
//         int nextSpace = text.indexOf( ' ', commandEnd );
//         while ( nextSpace > 0 ) {
//
//
//            nextSpace = text.indexOf( ' ', nextSpace + 1 );
//         }
      }

      private Style getCommandStyle( final String command ) {
         final Style style = _styles.get( command );
         if ( style == null ) {
            return _styles.get( "ERROR" );
         }
         return style;
      }
//      private Style getParameterStyle( final String parameter ) {
//         final Style style = _styles.get( command );
//         if ( style == null ) {
//            return _styles.get( "ERROR" );
//         }
//         return style;
//      }
   }

}
