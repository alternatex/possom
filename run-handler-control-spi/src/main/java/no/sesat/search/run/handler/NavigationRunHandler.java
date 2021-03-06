/* Copyright (2007-2012) Schibsted ASA
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
 */
package no.sesat.search.run.handler;

import java.util.Map;
import no.sesat.search.datamodel.DataModel;
import no.sesat.search.datamodel.generic.StringDataObject;
import no.sesat.search.datamodel.navigation.NavigationDataObject;
import no.sesat.search.view.NavigationControllerSpiFactory;
import no.sesat.search.result.NavigationItem;
import no.sesat.search.result.BasicNavigationItem;
import no.sesat.search.site.Site;
import no.sesat.search.site.SiteContext;

import java.util.List;
import java.util.Properties;
import java.lang.reflect.Constructor;

import java.lang.reflect.InvocationTargetException;
import javax.xml.parsers.DocumentBuilder;
import no.sesat.search.site.config.BytecodeLoader;
import no.sesat.search.site.config.DocumentLoader;
import no.sesat.search.site.config.PropertiesLoader;
import no.sesat.search.site.config.SiteClassLoaderFactory;
import no.sesat.search.site.config.Spi;
import no.sesat.search.view.navigation.NavigationConfig;
import no.sesat.search.view.navigation.NavigationController;
import no.sesat.search.view.navigation.NavigationControllerFactory;
import no.sesat.search.view.navigation.NavigationManager;
import no.sesat.search.view.navigation.NavigationState;
import no.sesat.search.view.navigation.UrlGenerator;


/**
 * To help generating navigation urls in the view. I got tired of all
 * the URL handling velocity code. Some of the effects from this is virtually impossible to
 * code in velocity.
 * <p/>
 * As a bonus from using this, you don't need to data-model the commands that only are
 * there for navigation.
 *
 *
 *
 * @version $Id: NavigationRunHandler.java 5846 2007-10-28 13:15:35Z ssmaeklu $
 */
public final class NavigationRunHandler implements RunHandler{

    private NavigationControllerSpiFactory controllerFactoryFactory;
    private NavigationManager navigationManager;

    private static final String PARAM_LAYOUT = "layout";

    public NavigationRunHandler(final RunHandlerConfig rhc) {}

    private boolean isRss(final DataModel datamodel) {
        final Map<String,StringDataObject> parameters = datamodel.getParameters().getValues();
        return parameters.get(PARAM_LAYOUT) != null && parameters.get(PARAM_LAYOUT).getString().equals("rss");
    }

    public void handleRunningQuery(final Context context) {

        if (isRss(context.getDataModel())) {
            return;
        }
        final NavigationControllerSpiFactory.Context cxt = new NavigationControllerSpiFactory.Context() {

            public Site getSite() {
                return context.getSite();
            }

            public BytecodeLoader newBytecodeLoader(SiteContext siteContext, String className, String jarFileName) {
                return context.newBytecodeLoader(siteContext, className, jarFileName);
            }
        };

        this.controllerFactoryFactory = new NavigationControllerSpiFactory(cxt);
        this.navigationManager = new NavigationManager(context.getDataModel());

        final SiteClassLoaderFactory.Context classLoadingContext = createClassLoadingContext(context);

        // Update the datamodel
        final NavigationDataObject navDO = context.getDataModel().getNavigation();

        if (navDO.getConfiguration() != null) {
            for (final NavigationConfig.Navigation n : navDO.getConfiguration().getNavigationList()) {
                final UrlGenerator urlGenerator = getUrlGeneratorInstance(classLoadingContext, n, context);

                final NavigationController.Context navCxt = createNavigationControllerContext(urlGenerator, context);

                processNavs(n.getNavList(), context.getDataModel(), navCxt, urlGenerator);
            }
        }
    }

    /**
     * Process the navs in a top-down fashion so that children can use the result of their parents.
     *
     * @param navs
     * @param dataModel
     * @param navCxt
     * @param urlGenerator
     */
    private void processNavs(
            final List<NavigationConfig.Nav> navs,
            final DataModel dataModel,
            final NavigationController.Context navCxt,
            final UrlGenerator urlGenerator) {

        final NavigationDataObject navDO = dataModel.getNavigation();

        for (final NavigationConfig.Nav nav : navs) {
            final NavigationItem items = getNavigators(nav, navCxt);

            // Navs with null id are considered anonymous. These navs typically just modify the result of their
            // parent and will not be found in the navmap.
            if (items != null && nav.getId() != null) {
                navDO.setNavigation(nav.getId(), items);

                // Create a "back" navigation item.
                final NavigationItem reset
                        = new BasicNavigationItem("reset_" + nav.getId(), urlGenerator.getURL("", nav), 0);

                navDO.setNavigation("reset_" + nav.getId(), reset);

            }

            processNavs(nav.getChildNavs(), dataModel, navCxt, urlGenerator);
        }
    }

    private NavigationItem getNavigators(
            final NavigationConfig.Nav navEntry,
            final NavigationController.Context navCxt) {

        final NavigationControllerFactory<NavigationConfig.Nav> factory
                = controllerFactoryFactory.getController(navEntry);

        return factory.get(navEntry).getNavigationItems(navCxt);
    }

    private NavigationController.Context createNavigationControllerContext(
            final UrlGenerator urlGenerator,
            final Context context) {

        return new NavigationController.Context() {

            public DataModel getDataModel() {
                return context.getDataModel();
            }

            public DocumentLoader newDocumentLoader(SiteContext siteCxt, String resource, DocumentBuilder builder) {
                return context.newDocumentLoader(siteCxt, resource, builder);
            }

            public PropertiesLoader newPropertiesLoader(SiteContext siteCxt, String resource, Properties properties) {
                return context.newPropertiesLoader(siteCxt, resource, properties);
            }

            public BytecodeLoader newBytecodeLoader(SiteContext siteContext, String className, String jarFileName) {
                return context.newBytecodeLoader(siteContext, className, jarFileName);
            }

            public Site getSite() {
                return context.getSite();
            }

            public UrlGenerator getUrlGenerator() {
                return urlGenerator;
            }
        };
    }

    private UrlGenerator getUrlGeneratorInstance(
            final SiteClassLoaderFactory.Context classLoadingContext,
            final NavigationConfig.Navigation navigation,
            final Context context) {

        try {
            final SiteClassLoaderFactory f = SiteClassLoaderFactory.instanceOf(classLoadingContext);

            final Class clazz = f.getClassLoader().loadClass(navigation.getUrlGenerator());

            @SuppressWarnings("unchecked")
            final Constructor<? extends UrlGenerator> s
                    = clazz.getConstructor(DataModel.class, NavigationConfig.Navigation.class, NavigationState.class);

            return s.newInstance(context.getDataModel(), navigation, navigationManager.getNavigationState());

        }catch (InstantiationException ex) {
            throw new IllegalArgumentException(
                    "Unable to load desired url generator: " + navigation.getUrlGenerator(),
                    ex);
        }catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(
                    "Unable to load desired url generator: " + navigation.getUrlGenerator(),
                    ex);
        }catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unable to load desired url generator: " + navigation.getUrlGenerator(),
                    ex);
        }catch (InvocationTargetException ex) {
            throw new IllegalArgumentException(
                    "Unable to load desired url generator: " + navigation.getUrlGenerator(),
                    ex);
        }catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(
                    "Unable to load desired url generator: " + navigation.getUrlGenerator(),
                    ex);
        }catch (SecurityException ex) {
            throw new IllegalArgumentException(
                    "Unable to load desired url generator: " + navigation.getUrlGenerator(),
                    ex);
        }catch(ClassNotFoundException ex){
            throw new IllegalArgumentException(
                    "Unable to load desired url generator: " + navigation.getUrlGenerator(),
                    ex);
        }
    }

    private SiteClassLoaderFactory.Context createClassLoadingContext(final Context context) {

        return new SiteClassLoaderFactory.Context() {
            public BytecodeLoader newBytecodeLoader(SiteContext siteContext, String className, String jarFileName) {
                return context.newBytecodeLoader(siteContext, className, jarFileName);
            }

            public Site getSite() {
                return context.getSite();
            }

            public Spi getSpi() {
                return Spi.VIEW_CONTROL;
            }
        };
    }
}
