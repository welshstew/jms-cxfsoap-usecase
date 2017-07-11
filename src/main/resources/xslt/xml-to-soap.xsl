<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output omit-xml-declaration="yes" indent="yes" method="xml"/>
    <xsl:template match="*">
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fak="http://www.example.org/FakeFlexicube/">
            <soapenv:Body>
                <fak:FakeElement>
                    <xsl:copy-of select="*"/>
                </fak:FakeElement>
            </soapenv:Body>
        </soapenv:Envelope>
    </xsl:template>
</xsl:stylesheet>
