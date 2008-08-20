<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="2.0"
        xmlns:jsp="http://java.sun.com/JSP/Page"
        xmlns:search="urn:jsptld:/WEB-INF/SearchPortal.tld"><!-- XXX a little awkward since SearchPortal.tld never exists in the skin --><jsp:output
        doctype-root-element="html"
        doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
        doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd" />
 <jsp:directive.page contentType="text/html" />
 
 <!--
 * Copyright (2008) Schibsted Søk AS
 *   This file is part of SESAT.
 *
 *   SESAT is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   SESAT is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with SESAT.  If not, see <http://www.gnu.org/licenses/>.
 *
    Document   : index
    Author     : mick
    Version    : $Id$
-->

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <search:include include="head-element"/>
</head>
<body>
    <table id="frame"><tr><td><!-- use a table instead of a div here to get automatic width plus centering. -->
        <div id="header">
            <search:include include="top-col-one" />
        </div>

        <br/><br/><br/><br/><br/><hr/><br/>
        <p style="padding-left: 50px; font-size:16px;font-weight:bold;">Other Sesam search engines:</p>
        <p style="padding: 5px 0px 0px 50px; margin-bottom:20px; font-size:14px;">
            <a href="http://www.sesam.no/">Click here to visit the Norwegian site.</a><br/>
            <a href="http://www.sesam.se/">Click here to visit the Swedish site.</a><br/>
        </p>

        <hr/>
        <p style="padding: 5px 0px 0px 50px; margin-bottom:20px; font-size:14px; font-weight: bold;">
            For more information, please contact us:</p>

        <p style="padding-left: 50px; margin-top:20px;">
                <b>Schibsted Søk AS</b><br />
                Postboks 277 Sentrum<br />
                0103 Oslo<br />
                Norway<br/><br />
                Telephone: +47 23 05 97 00<br />
                Fax:<![CDATA[&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]]> +47 22 42 96 97 <br />
                Email:<![CDATA[&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]]>
                <a href="mailto:info@sesam.no?SUBJECT=Feedback fra www.sesam.com">info@sesam.no</a><br/><br/>
        </p>
        <p style="padding-left: 50px; margin-top:20px;">
                <b>Schibsted Sök AB</b><br />
                105 17 STOCKHOLM<br />
                Sweden<br/><br />
                Telephone: +46 08 13 53 10<br />
                Fax:<![CDATA[&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]]>+46 08 20 96 70<br />
                Email:<![CDATA[&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]]>
                <a href="mailto:kontakt@sesam.se?SUBJECT=Feedback fra www.sesam.com">kontakt@sesam.se</a><br />
        </p>
        <br/><hr/>

        <div id="footer">
            <search:include include="bottom-col-four" />
        </div>
    </td></tr></table>

</body>
</html>
</jsp:root>