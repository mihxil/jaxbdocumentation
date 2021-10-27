<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

  <xsl:output
    method="xml"
    version="1.0"
    encoding="UTF-8"
    omit-xml-declaration="no"
    indent="no"
  />
  <xsl:variable name="types-constraints" select="document('http://meeuw.org/type-constraints')" />

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
