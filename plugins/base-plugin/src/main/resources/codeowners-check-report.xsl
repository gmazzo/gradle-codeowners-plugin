<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" indent="yes"/>
    <xsl:param name="pluginURL"/>
    <xsl:param name="pluginVersion"/>
    <xsl:param name="relativePath"/>
    <xsl:param name="failedMessage"/>

    <xsl:decimal-format decimal-separator="." grouping-separator="," />

    <xsl:template match="codeowners">
        <html>
            <head>
                <title>CodeOwners</title>
                <!-- vaguely adapted from Gradle's CSS -->
                <style type="text/css">
                    body {
                        background-color: #fff;
                        color: #02303A;
                    }

                    a {
                        color: #1DA2BD;
                    }
                    a.link {
                        color: #02303A;
                    }

                    p {
                        font-size: 1rem;
                    }

                    h1 a[name] {
                        margin: 0;
                        padding: 0;
                    }

                    tr:nth-child(even) {
                        background: white;
                    }

                    th {
                        font-weight:bold;
                    }
                    tr {
                        background: #efefef;
                    }
                    table th, td, tr {
                        font-size:100%;
                        border: none;
                        text-align: left;
                        vertical-align: top;
                    }
                </style>
            </head>
            <body>
                <p>
                    <a name="top"><h1>CodeOwners</h1></a>
                </p>
                <hr align="left" width="95%" size="1"/>
                <h2>Ownership</h2>
                <table class="summary" width="95%" >
                    <tr>
                        <th>Team</th>
                        <th>Files owned</th>
                    </tr>
                    <xsl:for-each select="owner">
                        <xsl:sort data-type="number" order="descending" select="@files"/>
                        <xsl:sort select="@name"/>
                        <tr>
                            <td>
                                <details>
                                  <summary><tt><xsl:value-of select="@name"/></tt></summary>
                                  <table class="summary" width="100%">
                                  <xsl:for-each select="file">
                                    <xsl:sort select="@path"/>
                                    <tr><td><tt><a><xsl:attribute name="href"><xsl:value-of select="$relativePath"/>/<xsl:value-of select="@path"/></xsl:attribute><xsl:value-of select="@path"/></a></tt></td></tr>
                                  </xsl:for-each>
                                  </table>
                                </details>
                            </td>
                            <td>
                                <xsl:value-of select="@files"/>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
                <hr align="left" width="95%" size="1"/>
                <div class="violations">
                    <h2>Unowned files</h2>
                    <p>
                        <xsl:choose>
                            <xsl:when test="count(descendant::unowned/file) > 0">
                                <table class="filelist" width="95%">
                                    <tr>
                                        <th>File</th>
                                    </tr>
                                    <xsl:for-each select="unowned/file">
                                        <xsl:sort select="@path"/>
                                        <tr>
                                           <td><tt><a><xsl:attribute name="href"><xsl:value-of select="$relativePath"/>/<xsl:value-of select="@path"/></xsl:attribute><xsl:value-of select="@path"/></a></tt></td>
                                        </tr>
                                    </xsl:for-each>
                                </table>
                                <p/>
                                <xsl:apply-templates>
                                    <!-- sort entries by file name alphabetically -->
                                    <xsl:sort select="@name"/>
                                </xsl:apply-templates>
                                <p/>
                                <xsl:if test="string-length($failedMessage) > 0">
                                  ❌ <xsl:value-of select="$failedMessage"/>.
                                </xsl:if>

                            </xsl:when>
                            <xsl:otherwise>
                                ✅ All files have been attributed to an owner.
                            </xsl:otherwise>
                        </xsl:choose>
                    </p>
                </div>
                <hr align="left" width="95%" size="1"/>
              <p>Generated by <a><xsl:attribute name="href"><xsl:value-of select="$pluginURL"/></xsl:attribute>CodeOwners Gradle Plugin <xsl:value-of select="$pluginVersion"/></a>.</p>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
