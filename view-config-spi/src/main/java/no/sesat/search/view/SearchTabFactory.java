/* Copyright (2006-2012) Schibsted ASA
 * This file is part of Possom.
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
 * ViewFactory.java
 *
 * Created on 19. april 2006, 20:48
 */

package no.sesat.search.view;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import no.sesat.commons.ioc.ContextWrapper;
import no.sesat.Interpreter;
import no.sesat.search.site.config.AbstractConfigFactory;
import no.sesat.search.view.navigation.NavigationConfig;
import no.sesat.search.site.config.DocumentLoader;
import no.sesat.search.site.config.ResourceContext;
import no.sesat.search.site.Site;
import no.sesat.search.site.SiteContext;
import no.sesat.search.site.SiteKeyedFactory;
import no.sesat.search.site.config.AbstractDocumentFactory;
import no.sesat.search.site.config.Spi;
import no.sesat.search.view.config.SearchTab.Layout;
import no.sesat.search.view.config.SearchTab;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Deserialises views.xml into SearchTab (& its inner classes).
 *
 *
 * @version $Id$
 */
public final class SearchTabFactory extends AbstractDocumentFactory implements SiteKeyedFactory{

    /**
     * The context any SearchTabFactory must work against.
     */
    public interface Context extends ResourceContext, AbstractConfigFactory.Context {}

    // Constants -----------------------------------------------------

    private static final Map<Site, SearchTabFactory> INSTANCES = new HashMap<Site,SearchTabFactory>();
    private static final ReentrantReadWriteLock INSTANCES_LOCK = new ReentrantReadWriteLock();

    private static final TabFactory TAB_FACTORY = new TabFactory();

    /**
     * Name of the configuration file.
     */
    public static final String VIEWS_XMLFILE = "views.xml";

    private static final Logger LOG = Logger.getLogger(SearchTabFactory.class);
    private static final String ERR_DOC_BUILDER_CREATION
            = "Failed to DocumentBuilderFactory.newInstance().newDocumentBuilder()";
    private static final String INFO_PARSING_TAB = "Parsing tab ";
    private static final String INFO_PARSING_ENRICHMENT = " Parsing enrichment ";

    private static final String RESET_NAV_ELEMENT = "reset";
    private static final String NAV_CONFIG_ELEMENT = "config";

    // Attributes ----------------------------------------------------

    private final Map<String,SearchTab> tabsByName = new HashMap<String,SearchTab>();
    private final Map<String,SearchTab> tabsByKey = new HashMap<String,SearchTab>();

    private final DocumentLoader loader;
    private final Context context;




    // Static --------------------------------------------------------

    /** Return the factory in use for the skin defined within the context. *
     * @param cxt
     * @return
     */
    public static SearchTabFactory instanceOf(final Context cxt) {

        final Site site = cxt.getSite();
        assert null != site;

        SearchTabFactory instance;
        try{
            INSTANCES_LOCK.readLock().lock();
            instance = INSTANCES.get(site);
        }finally{
            INSTANCES_LOCK.readLock().unlock();
        }

        if (instance == null) {
            try {
                instance = new SearchTabFactory(cxt);
            } catch (ParserConfigurationException ex) {
                LOG.error(ERR_DOC_BUILDER_CREATION,ex);
            }
        }
        return instance;
    }

    /** Remove the factory in use for the skin defined within the context. **/
    public boolean remove(final Site site){

        try{
            INSTANCES_LOCK.writeLock().lock();
            return null != INSTANCES.remove(site);
        }finally{
            INSTANCES_LOCK.writeLock().unlock();
        }
    }

    // Constructors --------------------------------------------------

    /** Creates a new instance of ViewFactory */
    private SearchTabFactory(final Context cxt) throws ParserConfigurationException {

        LOG.trace("SearchTabFactory(cxt)");
        try{
            INSTANCES_LOCK.writeLock().lock();

            context = cxt;

            // configuration files
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            final DocumentBuilder builder = factory.newDocumentBuilder();
            loader = context.newDocumentLoader(cxt, VIEWS_XMLFILE, builder);

            // start initialisation
            init();

            // update the store of factories
            INSTANCES.put(context.getSite(), this);
            LOG.debug("site: "+ context.getSite() + "; tabsByName:" + tabsByName);
        }finally{
            INSTANCES_LOCK.writeLock().unlock();
        }

    }

    // Public --------------------------------------------------------

    /** Find the tab with the given id.
     * Search recursively up through the skin's parents.
     * <b>Allow to return null.</b>
     * @param id
     * @return
     */
    public SearchTab getTabByName(final String id){

        LOG.trace("getTabByName(" + id + ')');
        LOG.trace(tabsByName);

        SearchTab tab = getTabImpl(id);
        Site site = context.getSite().getParent();
        while(null == tab && null != site){
            // not found in this site's views.xml. look in parent's site.
            final SearchTabFactory factory = instanceOf(ContextWrapper.wrap(
                    Context.class,
                    site.getSiteContext(),
                    context
                ));
            tab = factory.getTabByName(id);
            site = site.getParent();
        }
        if(null != tab){
            LOG.trace("found tab for " + id + " against SearchTabFactory for " + context.getSite());
        }

        return tab;
    }

    /** Find the tab with the given key.
     * Search recursively up through the skin's parents.
     * <b>Allow to return null.</b>
     * @param key
     * @return
     */
    public SearchTab getTabByKey(final String key){

        LOG.trace("getTabByKey(" + key + ')');

        SearchTab tab = getTabByKeyImpl(key);
        Site site = context.getSite().getParent();
        while(null == tab && null != site){
            // not found in this site's views.xml. look in parent's site.
            final SearchTabFactory factory = instanceOf(ContextWrapper.wrap(
                    Context.class,
                    site.getSiteContext(),
                    context
                ));
            tab = factory.getTabByKeyImpl(key);
            site = site.getParent();
        }
        if(null != tab){
            LOG.trace("found tab for " + key + " against SearchTabFactory for " + context.getSite());
        }

        return tab;
    }

    public Map<String,SearchTab> getTabsByName(){

        LOG.trace("getTabsByName()");

        return Collections.unmodifiableMap(tabsByName);


    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    /** Initialises instance.
     * Crucial that this method is called from the constructor,
     *  otherwise read/write synchronisation must be re-added to the tabsByName & tabsByKey fields.
     *
     * @throws javax.xml.parsers.ParserConfigurationException
     */
    private void init() throws ParserConfigurationException {

        loader.abut();
        LOG.info("Parsing " + VIEWS_XMLFILE + " started. " + "Site: " + context.getSite());
        final Document doc = loader.getDocument();
        final Element root = doc.getDocumentElement();
        if( null != root ){

            final NodeList tabList = root.getChildNodes();

            for(int i = 0 ; i < tabList.getLength(); ++i){
                if(tabList.item(i) instanceof Element){
                    final Element tabE = (Element) tabList.item(i);
                    if("tab".equals(tabE.getTagName())){
                        final String key = parseString(tabE.getAttribute("key"), "");
                        final SearchTab inherit = getTabByName(tabE.getAttribute("inherit"));
                        final SearchTab tab = TAB_FACTORY.parseTab(tabE, context, inherit);

                        tabsByName.put(tab.getId(), tab);
                        if(key.length() > 0){
                            tabsByKey.put(key, tab);
                        }
                    }
                }
            }
        }

        // finished
        LOG.info("Parsing " + VIEWS_XMLFILE + " finished");

    }

    private SearchTab getTabImpl(final String id){

        LOG.trace("getTabImpl(" + id + ')');

        return tabsByName.get(id);

    }

    private SearchTab getTabByKeyImpl(final String key){

        LOG.trace("getTabByKeyImpl(" + key + ')');

        return tabsByKey.get(key);

    }

    // Inner classes -------------------------------------------------

    private static final class TabFactory extends AbstractConfigFactory<SearchTab> {

        private static final NavFactory NAV_FACTORY = new NavFactory();

        SearchTab parseTab(
                final Element tabE,
                final Context context,
                final SearchTab inherit) throws ParserConfigurationException {



                final String id = tabE.getAttribute("id");
                LOG.info(INFO_PARSING_TAB + id);
                final String mode = parseString(tabE.getAttribute("mode"), inherit != null ? inherit.getMode() : "");
                final String key = parseString(tabE.getAttribute("key"), "");
                final String parentKey = parseString(tabE.getAttribute("parent-key"),
                        inherit != null ? inherit.getParentKey() : "");
                final String adCommand = parseString(tabE.getAttribute("ad-command"),
                        inherit != null ? inherit.getAdCommand() : "");
                final String allCss = parseString(tabE.getAttribute("css"), null);
                final String[] css = allCss != null ? allCss.split(",") : new String[]{};
                final String allJavascript = parseString(tabE.getAttribute("javascript"), null);
                final String[] javascript = allJavascript != null ? allJavascript.split(",") : new String[]{};

                // enrichment placement hints
                final NodeList placementsNodeList = tabE.getElementsByTagName("enrichment-placement");
                final Collection<SearchTab.EnrichmentPlacementHint> placements = new ArrayList<SearchTab.EnrichmentPlacementHint>();
                for(int j = 0 ; j < placementsNodeList.getLength(); ++j){
                    final Element e = (Element) placementsNodeList.item(j);
                    final String placementId = e.getAttribute("id");
                    final int threshold = parseInt(e.getAttribute("threshold"), 0);
                    final int max = parseInt(e.getAttribute("max"), 0);
                    final Map<String,String> properties = new HashMap<String,String>();
                    final NodeList nodeList = e.getChildNodes();
                    for (int l = 0; l < nodeList.getLength(); l++) {
                        final Node propNode = nodeList.item(l);
                        if (propNode instanceof Element){
                            final Element propE = (Element)propNode;
                            properties.put(propE.getNodeName(), propE.getFirstChild().getNodeValue());
                        }
                    }

                    placements.add(new SearchTab.EnrichmentPlacementHint(placementId, threshold, max, properties));
                }

                // enrichment hints
                final NodeList enrichmentNodeList = tabE.getElementsByTagName("enrichment");
                final Collection<SearchTab.EnrichmentHint> enrichments = new ArrayList<SearchTab.EnrichmentHint>();
                for(int j = 0 ; j < enrichmentNodeList.getLength(); ++j){
                    final Element e = (Element) enrichmentNodeList.item(j);
                    final String rule = e.getAttribute("rule");
                    LOG.info(INFO_PARSING_ENRICHMENT + rule);
                    final int baseScore = parseInt(e.getAttribute("base-score"), 0);
                    final int threshold = parseInt(e.getAttribute("threshold"), 0);
                    final float weight = parseFloat(e.getAttribute("weight"), 0);
                    final String command = e.getAttribute("command");
                    final Map<String,String> properties = new HashMap<String,String>();
                    final NodeList nodeList = e.getChildNodes();
                    for (int l = 0; l < nodeList.getLength(); l++) {
                        final Node propNode = nodeList.item(l);
                        if (propNode instanceof Element){
                            final Element propE = (Element)propNode;
                            properties.put(propE.getNodeName(), propE.getFirstChild().getNodeValue());
                        }
                    }

                    final SearchTab.EnrichmentHint enrichment = new SearchTab.EnrichmentHint(
                            rule,
                            baseScore,
                            threshold,
                            weight,
                            command,
                            properties);

                    enrichments.add(enrichment);
                }

                // navigation hints
                final NodeList navigationNodeList = tabE.getElementsByTagName("navigation");
                Element navE = null;
                for(int j = 0 ; null == navE && j < navigationNodeList.getLength(); ++j){
                    final Element n = (Element) navigationNodeList.item(j);
                    // only interested in the direct children
                    if(tabE == n.getParentNode()){

                        navE = n;
                    }
                }

                final NavigationConfig navConf = parseNavigation(
                        mode,
                        null != navE ? navE.getElementsByTagName("navigation") : new NodeList() {
                            public Node item(final int arg0) {
                                throw new IllegalArgumentException("empty nodelist");
                            }
                            public int getLength() {
                                return 0;
                            }
                        },
                        context,
                        null != inherit ? inherit.getNavigationConfiguration() : null);

                // the tab's layout
                final NodeList layoutsNodeList = tabE.getElementsByTagName("layout");

                Layout defaultLayout = null;
                final Layout defaultInheritedLayout = null != inherit
                        ? inherit.getDefaultLayout()
                        : null;
                final Map<String,Layout> layouts = new HashMap<String,Layout>();

                for(int j = 0 ;j < layoutsNodeList.getLength(); ++j){

                    final Element layoutE = (Element) layoutsNodeList.item(j);
                    final String layoutId = null != layoutE.getAttribute("id") ? layoutE.getAttribute("id") : "";
                    final Layout inheritedLayout = null != inherit && null != inherit.getLayouts().get(layoutId)
                            ? inherit.getLayouts().get(layoutId)
                            : defaultInheritedLayout;
                    final Layout layout = new Layout(inheritedLayout).readLayout(layoutE);

                    layouts.put(layoutId, layout);

                    if(0 == layoutId.length()){
                        defaultLayout = layout;
                    }
                }
                final String scopeStr = tabE.getAttribute("scope");
                final SearchTab.Scope scope = 0 < scopeStr.length()
                        ? SearchTab.Scope.valueOf(scopeStr.toUpperCase())
                        : null != inherit ? inherit.getScope() : SearchTab.Scope.REQUEST;

                return new SearchTab(
                        inherit,
                        id,
                        mode,
                        key,
                        parentKey,
                        tabE.getAttribute("rss-result-name"),
                        parseBoolean(tabE.getAttribute("rss-hidden"), false),
                        navConf,
                        placements,
                        enrichments,
                        adCommand,
                        parseInt(tabE.getAttribute("ad-limit"), inherit != null ? inherit.getAdLimit() : -1),
                        parseInt(tabE.getAttribute("ad-on-top"), inherit != null ? inherit.getAdOnTop() : -1),
                        Arrays.asList(css),
                        Arrays.asList(javascript),
                        parseBoolean(tabE.getAttribute("display-css"), true),
                        parseBoolean(tabE.getAttribute("execute-on-blank"), inherit != null
                        ? inherit.isExecuteOnBlank()
                        : false),
                        null != defaultLayout ? defaultLayout : defaultInheritedLayout,
                        layouts,
                        scope);


        }


        private NavigationConfig parseNavigation(
                final String modeId,
                final NodeList navigationElements,
                final Context context,
                final NavigationConfig inherit) throws ParserConfigurationException {

            final NavigationConfig cfg = new NavigationConfig(inherit);

            for (int i = 0; i < navigationElements.getLength(); i++) {
                final Element navigationElement = (Element) navigationElements.item(i);
                final NavigationConfig.Navigation navigation = new NavigationConfig.Navigation(navigationElement);

                final NodeList navs = navigationElement.getChildNodes();

                for (int l = 0; l < navs.getLength(); l++) {
                    final Node navNode = navs.item(l);

                    if (navNode instanceof Element
                            && ! (RESET_NAV_ELEMENT.equals(navNode.getNodeName())
                            || NAV_CONFIG_ELEMENT.equals(navNode.getNodeName()))) {

                        navigation.addNav(
                                NAV_FACTORY.parseNav((Element) navNode, navigation,  context, null),
                                cfg);
                    }
                }

                for (int j = 0; j < navs.getLength(); j++) {
                    final Node navElement = navs.item(j);

                    if (RESET_NAV_ELEMENT.equals(navElement.getNodeName())) {
                        final String resetNavId = ((Element)navElement).getAttribute("id");
                        if (resetNavId != null) {
                            final NavigationConfig.Nav nav = cfg.getNavMap().get(resetNavId);
                            if (nav != null) {
                                navigation.addReset(nav);
                            } else {
                                LOG.error("Error in config, <reset id=\"" + resetNavId + "\" />, in tab " + modeId + " not found");
                            }
                        }
                    }
                }

                cfg.addNavigation(navigation);
            }
            return cfg;
        }


        /** XXX Implement me. Everything is hardcoded to SearchTab. Not even used i believe. */
        protected Class<SearchTab> findClass(final String xmlName, final Context context)
                throws ClassNotFoundException {

            final String bName = xmlToBeanName(xmlName);
            final String className = Character.toUpperCase(bName.charAt(0)) + bName.substring(1, bName.length());

            LOG.debug("findClass " + className);

            // Special case for "nav".
            final String classNameFQ = xmlName.equals("nav")
                    ? NavigationConfig.Nav.class.getName()
                    : "no.sesat.search.view.SearchTab";

            final Class<SearchTab> clazz = loadClass(context, classNameFQ, Spi.VIEW_CONFIG);

            LOG.debug("Found class " + clazz.getName());
            return clazz;
        }
    }

    private static final class NavFactory extends AbstractConfigFactory<NavigationConfig.Nav> {

        NavigationConfig.Nav parseNav(
                final Element element,
                final NavigationConfig.Navigation navigation,
                final Context context,
                final NavigationConfig.Nav parent) throws ParserConfigurationException {

            try {

                Class<NavigationConfig.Nav> clazz = null;

                // TODO: Temporary to keep old-style modes.xml working.
                if ("reset".equals(element.getNodeName()) || "static-parameter".equals(element.getNodeName())) {
                    clazz = findClass("nav", context);
                } else {
                    clazz = findClass(element.getNodeName(), context);
                }

                final Constructor<NavigationConfig.Nav> c
                        = clazz.getConstructor(NavigationConfig.Nav.class, NavigationConfig.Navigation.class, Element.class);

                final NavigationConfig.Nav nav = c.newInstance(parent, navigation, element);

                final NodeList children = element.getChildNodes();

                for (int i = 0; i < children.getLength(); ++i) {
                    final Node navNode = children.item(i);

                    if (navNode instanceof Element && !NAV_CONFIG_ELEMENT.equals(navNode.getNodeName())) {
                        nav.addChild(parseNav((Element) navNode, navigation, context, nav));
                    }

                }
                return nav;
            } catch (InstantiationException ex) {
                LOG.error(ex.getMessage(), ex);
                throw new ParserConfigurationException(ex.getMessage());
            } catch (IllegalAccessException ex) {
                LOG.error(ex.getMessage(), ex);
                throw new ParserConfigurationException(ex.getMessage());
            } catch (ClassNotFoundException e) {
                LOG.error(e.getMessage(), e);
                return null;
            } catch (NoSuchMethodException e) {
                LOG.error(e.getMessage(), e);
                return null;
            } catch (InvocationTargetException e) {
                LOG.error(e.getMessage(), e);
                return null;
            }
        }

        protected Class<NavigationConfig.Nav> findClass(final String xmlName, final Context context)
                throws ClassNotFoundException {

            final String bName = xmlToBeanName(xmlName);
            final String className = Character.toUpperCase(bName.charAt(0)) + bName.substring(1, bName.length());

            LOG.debug("findClass " + className);

            // Special case for "nav".
            final String classNameFQ = xmlName.equals("nav")
                    ? NavigationConfig.Nav.class.getName()
                    : "no.sesat.search.view.navigation."+ className+ "NavigationConfig";

            final Class<NavigationConfig.Nav> clazz = loadClass(context, classNameFQ, Spi.VIEW_CONFIG);

            LOG.debug("Found class " + clazz.getName());
            return clazz;
        }
    }

    static {
        Interpreter.addFunction("tabs", new Interpreter.Function() {
            public String execute(Interpreter.Context ctx) {
                String res = "";
                try{
                    INSTANCES_LOCK.readLock().lock();
                    for(Site site : INSTANCES.keySet()) {
                        res += "Site: " + site.getName() + "\n";
                        SearchTabFactory factory = INSTANCES.get(site);
                        for (String s : factory.tabsByKey.keySet()) {
                            res += "    View: " + s + "\n";
                            res += "          " + factory.tabsByKey.get(s).toString();
                            res += "\n";
                        }
                        res += "\n";
                    }

                }finally{
                    INSTANCES_LOCK.readLock().unlock();
                }
                return res;
            }

            public String describe() {
                return "Print out the tabs in tabsByKey for each site.";
            }
        });
    }
}