<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                version="1.0">

  <xsl:output
    method="xml"
    version="1.0"
    encoding="UTF-8"
    omit-xml-declaration="no"
    indent="no"
  />
  <xsl:variable name="documentations" select="document('http://meeuw.org/documentations')" />
  <xsl:param name="xmlStyleSheet" />
  <xsl:param name="debug" select="false" />


  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="/xs:schema">
    <xsl:if test="$xmlStyleSheet != ''">
      <xsl:processing-instruction name="xml-stylesheet">
        <xsl:text>href="xs3p.xsl" type="text/xsl"</xsl:text>
      </xsl:processing-instruction>
    </xsl:if>
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>

  </xsl:template>


  <xsl:template match="xs:complexType|xs:simpleType">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:call-template name="generate_annotation">
        <xsl:with-param name="key">
          <xsl:call-template name="typeKey">
            <xsl:with-param name="node" select="." />
          </xsl:call-template>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="xs:attribute">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>

      <xsl:call-template name="generate_annotation">
        <xsl:with-param name="key">
          <xsl:call-template name="typeKey">
            <xsl:with-param name="node" select="ancestor::xs:complexType" />
          </xsl:call-template>
          <xsl:text>|ATTRIBUTE|</xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="xs:element">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:call-template name="generate_annotation">
        <xsl:with-param name="key">
          <xsl:call-template name="typeKey">
            <xsl:with-param name="node" select="ancestor::xs:complexType"/>
          </xsl:call-template>
          <xsl:text>|ELEMENT|</xsl:text>
          <xsl:value-of select="@name"/>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="xs:enumeration">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:call-template name="generate_annotation">
        <xsl:with-param name="key">
          <xsl:call-template name="typeKey">
            <xsl:with-param name="node" select="ancestor::xs:simpleType"/>
          </xsl:call-template>
          <xsl:text>|ENUMERATION|</xsl:text>
          <xsl:value-of select="@value"/>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template name="typeKey">
    <xsl:param name="node"/>
    <xsl:text>{</xsl:text>
    <xsl:value-of select="/xs:schema/@targetNamespace"/>
    <xsl:text>}</xsl:text>
    <xsl:value-of select="$node/@name"/>
  </xsl:template>

  <xsl:template name="generate_annotation">
    <xsl:param name="key" />
    <xsl:variable name="documentation" select="$documentations/properties/entry[@key = $key]/text()"/>
    <xsl:if test="$debug">
      <xsl:comment>
        <xsl:text>documentation key: </xsl:text>
        <xsl:value-of select="$key" />
        <xsl:if test="not($documentation)"> (not found)</xsl:if>
      </xsl:comment>
    </xsl:if>
    <xsl:if test="$documentation">
      <xs:annotation>
        <xs:documentation>
          <xsl:value-of select="$documentation"/>
        </xs:documentation>
      </xs:annotation>
    </xsl:if>

  </xsl:template>

</xsl:stylesheet>
