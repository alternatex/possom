<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="2.0"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:search="urn:jsptld:/WEB-INF/SearchPortal.tld"><!-- XXX a little awkward since this never exists in the skin -->
<!-- 
 * Copyright (2012) Schibsted ASA
 *   This file is part of Possom.
 *
 *   Possom is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Possom is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with Possom.  If not, see <http://www.gnu.org/licenses/>.
 *
    Version    : $Id$
-->
<title><c:if test="${not empty DataModel.query.utf8UrlEncoded}">${DataModel.query.xmlEscaped} - </c:if> Sesam</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

<!-- Rss -->
<c:set var="msg_rss_link"><search:text key="rss_link"/></c:set>
<link rel="alternate" type="application/rss+xml" title="${msg_rss_link}" href=""><![CDATA[&nbsp;]]></link>

<!-- Css -->
<c:forEach var="ww" items="${tab.css}">
    <c:set var="ww_link"><search:findResource url="/css/tab/${ww.id}.css"/></c:set>
    <c:if test="${not empty ww_link}">
        <link rel="stylesheet" type="text/css" href="${ww_link}" media="screen"><![CDATA[&nbsp;]]></link>
    </c:if>
</c:forEach>
<c:forEach var="w" items="${tab.ancestry}">
    <c:set var="w_link"><search:findResource url="/css/tab/${w.id}.css"/></c:set>
    <c:if test="${not empty w_link and w_link ne 'null'}">
        <link rel="stylesheet" type="text/css" href="${w_link}" media="screen"><![CDATA[&nbsp;]]></link>
    </c:if>
</c:forEach>
<c:set var="w_link"><search:findResource url="/css/print.css"/></c:set>
<link rel="stylesheet" type="text/css" href="${w_link}" media="print"/>

<!-- Page Icons -->
<link rel="icon" href="/images/favicon.gif" type="image/x-icon" ><![CDATA[&nbsp;]]></link>
<link rel="shortcut icon" href="/images/favicon.gif" type="image/x-icon" ><![CDATA[&nbsp;]]></link>

<!-- OpenSearch -->
<jsp:text><![CDATA[<link rel="search" type="application/opensearchdescription+xml" title="Sesam.com" href="/search/?q=*&amp;c=${tab.key}&amp;layout=opensearch">&nbsp;</link>]]></jsp:text>

<!-- JavaScript -->
<c:set var="js_link"><search:findResource url="/javascript/common.js"/></c:set>
<script type='text/javascript' src="${js_link}"><![CDATA[&nbsp;]]></script>

<c:forEach var="js" items="${tab.javascript}">
    <c:set var="js_link"><search:findResource url="/javascript/${js}.js"/></c:set>
    <c:if test="${not empty js_link}">
        <script type='text/javascript' src="${js_link}"><![CDATA[&nbsp;]]></script>
    </c:if>
</c:forEach>
<search:include include="head-element-extra"/>
</jsp:root>
