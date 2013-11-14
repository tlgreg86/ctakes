<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:t="http://uima.apache.org/resourceSpecifier">

<xsl:template match="/t:typeSystemDescription">
  <html>
  <body>
  <h2>Apache cTAKES Data Dictionary </h2>
  <table border="1">
    <tr bgcolor="#9acd32">
      <th>Type</th>
      <th>Features</th>
    </tr>
    <xsl:for-each select="t:types/t:typeDescription">
    <xsl:sort select="t:name"/>
    <tr>
      <td>
      <b><xsl:value-of select="t:name" />
      </b>
      <!-- extends <xsl:value-of select="t:supertypeName" />  -->
      <p>
      <xsl:value-of select="t:description" />
      </p>
      </td>
      <td>
		      <table border="1">
		          <xsl:for-each select="t:features/t:featureDescription">
				    <tr>
				      <td><xsl:value-of select="t:name" /></td>
				      <td><xsl:value-of select="t:description" /></td>
				    </tr>
				    </xsl:for-each>  
		      </table>
      </td>
    </tr>  
    </xsl:for-each>
  </table>
  </body>
  </html>
</xsl:template>

</xsl:stylesheet>