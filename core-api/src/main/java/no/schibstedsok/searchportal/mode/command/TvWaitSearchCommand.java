// Copyright (2007) Schibsted Søk AS
/*
 * TvWaitSearchCommand.java
 *
 * Created on 26 October 2006, 14:03
 *
 */

package no.schibstedsok.searchportal.mode.command;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import java.util.Map;
import no.schibstedsok.commons.ioc.ContextWrapper;
import no.schibstedsok.searchportal.datamodel.generic.StringDataObject;
import no.schibstedsok.searchportal.mode.config.TvwaitsearchCommandConfig;
import no.schibstedsok.searchportal.result.*;
import no.schibstedsok.searchportal.site.Site;
import no.schibstedsok.searchportal.site.SiteContext;
import no.schibstedsok.searchportal.util.Channel;
import no.schibstedsok.searchportal.util.Channels;
import org.apache.log4j.Logger;

/**
 *
 * @author andersjj
 * @version $Id$
 */
public final class TvWaitSearchCommand extends AbstractSimpleFastSearchCommand {

    // Constants -----------------------------------------------------
    
    private static final GeneralCacheAdministrator CACHE = new GeneralCacheAdministrator();   
    private static final int REFRESH_PERIOD = 60; // one minute

    /** Logger */
    private static final Logger LOG = Logger.getLogger(TvWaitSearchCommand.class);

    /** Millis in day */
    private static final int MILLIS_IN_DAY = 86400000;

    /** Sort options */
    private static enum SortBy {
        CHANNEL,
        DAY,
        CATEGORY;
    }

    // Attributes ----------------------------------------------------

    /** Search command configuration */
    private final TvwaitsearchCommandConfig config;

    /** Wait on search result */
    private FastSearchResult<ResultItem> wosr;

    /** Index to use when creating filters */
    private final int index;

    /** Sort mode */
    private final SortBy userSortBy;

    /** Blank query */
    private final boolean blankQuery;

    /** Execute query */
    private boolean executeQuery = true;

    /** Use all channels */
    private boolean useAllChannels = false;

    /** Cache for channel categories */
    private final List<Channel.Category> categories = new ArrayList<Channel.Category>();
    
    // Static --------------------------------------------------------


    // Constructors --------------------------------------------------

    /** Creates a new instance of TvWaitSearchCommand */
    public TvWaitSearchCommand(final Context cxt) {

        super(cxt);

        blankQuery = getQuery().isBlank();
        this.config = (TvwaitsearchCommandConfig) cxt.getSearchConfiguration();

        final Map<String,StringDataObject> parameters = datamodel.getParameters().getValues();
        final String defaultUserSortBy = blankQuery ? "CHANNEL" : "DAY";
        final String usbp = parameters.containsKey("userSortBy")
                ? parameters.get("userSortBy").getString().toUpperCase()
                : defaultUserSortBy;

        SortBy tmpUserSortBy = null;
        try {
            tmpUserSortBy = SortBy.valueOf(usbp);
        } catch (IllegalArgumentException e) {
            /* Fall back to default sorting */
            tmpUserSortBy = SortBy.valueOf(defaultUserSortBy);
        } finally {
            userSortBy = tmpUserSortBy;
        }

        final int offset = parameters.containsKey("offset")  ? Integer.parseInt(parameters.get("offset").getString()) : 0;

        if (userSortBy == SortBy.DAY || !config.isPaging() || config.getIndex() == -1) {
            index = config.getIndex();
        } else {
            index = config.getIndex() + (offset);
        }

        useAllChannels = parameters.get("allChannels") != null && "true".equals(parameters.get("allChannels").getString());
    }


    // Public --------------------------------------------------------

    public ResultList<? extends ResultItem> execute() {

        if (!executeQuery) {
            return new FastSearchResult(this);
        }

        final String waitOn = useAllChannels ? config.getWaitOnAllChannels() : config.getWaitOnMyChannels();
        final Map<String,StringDataObject> parameters = datamodel.getParameters().getValues();
        
        if (waitOn != null) {
            LOG.debug("Waiting on: " + waitOn);
            try {
                wosr = (FastSearchResult) getSearchResult(waitOn, datamodel);
                if (wosr == null) {
                    throw new NullPointerException("wait-on result is null");
                }
            } catch (Exception e) {
                LOG.error(e);
                return new FastSearchResult(this);
            }
            
            /* Abort if navigator gave no result */
            if (wosr.getHitCount() == 0) {
                executeQuery = false;
            } else if (index > 0 && wosr.getHitCount() > 0) {
                /* Abort execution of commands that are not needed */

                /* If using channel navigator and sorting by channels */
                if (userSortBy == SortBy.CHANNEL) {
                    if (parameters.get("nav_channels") != null || wosr.getModifiers("channels").size() < index + 1 ) {
                        executeQuery = false;
                    }
                }

                /* If using category navigator and sorting by category */
                if (userSortBy == SortBy.CATEGORY) {
                    if (parameters.get("nav_categories") != null || wosr.getModifiers("categories").size() < index + 1) {
                        executeQuery = false;
                    }
                }

                /* If using day navigator and sorting on day */
                if (userSortBy == SortBy.DAY) {
                    if (parameters.get("day") != null) {
                        executeQuery = false;
                    }
                }
            }

        }


        /* Abort execution if this is a Set My Channels request */
        if (executeQuery && config.getUseMyChannels() && parameters.containsKey("setMyChannels")) {
            executeQuery = false;
        }

        if (executeQuery == false) {
            return new FastSearchResult(this);
        }

        ResultList<ResultItem> sr = null;
        if (blankQuery && waitOn != null && parameters.get("nav_categories") == null) {
            final String cacheKey = getCacheKey();
            sr = getResultsFromCache(cacheKey);
        } else {
            sr = (ResultList<ResultItem>) super.execute();
        }
        
        if (sr.getHitCount() > 0) {
            /* Mark current running shows */
            ResultItem sri = sr.getResults().get(0);
            String startTime = sri.getField("starttime");
            String endTime = sri.getField("endtime");
            Calendar cal = Calendar.getInstance();
            Date now = cal.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

            try {
                Date st = sdf.parse(startTime);
                Date et = sdf.parse(endTime);

                if (now.compareTo(st) >= 0 && now.compareTo(et) < 0) {
                    sr.replaceResult(sri, sri.addField("current", "true"));
                }
                
            } catch (ParseException e) {
                LOG.trace(e.getMessage(), e);
            }
        }
        return sr;
    }

    public static final String MY_CHANNELS_KEY = "myChannels";
    private String[] myChannels = null;
    
    private final String[] getMyChannels() {
        synchronized (this) {
            if (myChannels == null) {    
                final Map<String,StringDataObject> parameters = datamodel.getParameters().getValues();
                if (config.getUseMyChannels() && parameters.containsKey(MY_CHANNELS_KEY)) {
                    final String value = parameters.get(MY_CHANNELS_KEY).getString();
                    if (value != null && value.length() > 0) {
                        myChannels = value.split(",");
                    }
                } else {
                    myChannels = new String[0];
                }
            }
        }
        return myChannels;
    }
    
    private String additionalFilter = null;
    
    public String getAdditionalFilter() {

        LOG.debug("getAdditionalFilter()");
        synchronized (this) {
            if (additionalFilter == null) {
                final Map<String,StringDataObject> parameters = datamodel.getParameters().getValues();
                final int day = parameters.containsKey("day") ? Integer.parseInt(parameters.get("day").getString()) : 0;
                final StringBuilder filter = new StringBuilder();

                /* If navigator root command and blankQuery, set default navigator */
                if (blankQuery && !parameters.containsKey("nav_channelcategories") && !parameters.containsKey("allhits")) {
                    if (useAllChannels && !config.getUseMyChannels()) {
                        filter.append(addChannelCategoryNavigator(false));
                    } else {
                        if (getMyChannels().length > 50) {
                            filter.append(addChannelCategoryNavigator(true));
                        }
                    }
                }

                final Calendar cal = Calendar.getInstance();

                 /* Adjust time to selected day */
                final int adjustment = index < 0 ? 0 : index;
                cal.setTimeInMillis(cal.getTimeInMillis() + MILLIS_IN_DAY * (SortBy.DAY == userSortBy && parameters.get("day") == null ? adjustment : day));

                if (userSortBy == SortBy.CHANNEL) {
                    /* Starttime greater than now() or 05:00 on selected day */
                    final String dateFmt = day == 0 ? "yyyy-MM-dd'T'HH:mm:ss'Z'" : "yyyy-MM-dd'T'05:00:00'Z'";
                    filter.append("+endtime:>").append(new SimpleDateFormat(dateFmt).format(cal.getTime())).append(" ");

                    /* Starttime less than 05:00 the next day */
                    cal.setTimeInMillis(cal.getTimeInMillis() + MILLIS_IN_DAY);
                    filter.append("+starttime:<").append(new SimpleDateFormat("yyyy-MM-dd'T05:00:00Z'").format(cal.getTime())).append(" ");

                    /* Use channels navigator in waitOn command */
                    if (config.getWaitOnAllChannels() != null) {
                        final Modifier modifier = (Modifier) wosr.getModifiers("channels").get(index);
                        final Navigator navigator = modifier.getNavigator();
                        filter.append("+").append(navigator.getField()).append(":").append(modifier.getName()).append(" ");
                    }
                } else if (userSortBy == SortBy.DAY) {
                    /* Starttime greater than now() or 05:00 on selected day */
                    final String dateFmt = index < 1 ? "yyyy-MM-dd'T'HH:mm:ss'Z'" : "yyyy-MM-dd'T'05:00:00'Z'";
                    filter.append("+endtime:>").append(new SimpleDateFormat(dateFmt).format(cal.getTime())).append(" ");

                    /* Starttime less than 05:00 the next day or less than 05:00 seven days from now for the navigator */
                    if (config.getWaitOnAllChannels() != null) {
                        cal.setTimeInMillis(cal.getTimeInMillis() + MILLIS_IN_DAY);
                    } else {
                        cal.setTimeInMillis(cal.getTimeInMillis() + MILLIS_IN_DAY * 7);
                    }
                    filter.append("+starttime:<").append(new SimpleDateFormat("yyyy-MM-dd'T05:00:00Z'").format(cal.getTime())).append(" ");

                    /* Use channels navigator to add filter for top 5 channels */
                    if (config.getWaitOnAllChannels() != null && blankQuery && wosr.getModifiers("channels").size() > 0) {
                        filter.append("+(");
                        final int maxIdx = wosr.getModifiers("channels").size() < 5 ? wosr.getModifiers("channels").size() : 5;
                        for (Modifier modifier : wosr.getModifiers("channels").subList(0, maxIdx)) {
                            final Navigator navigator = modifier.getNavigator();
                            filter.append(navigator.getField()).append(":").append(modifier.getName()).append(" ");
                        }
                        filter.append(")");
                    }

                } else if (userSortBy == SortBy.CATEGORY) {
                    /* Starttime greater than now() or 05:00 on selected day */
                    final String dateFmt = day == 0 ? "yyyy-MM-dd'T'HH:mm:ss'Z'" : "yyyy-MM-dd'T'05:00:00'Z'";
                    filter.append("+endtime:>").append(new SimpleDateFormat(dateFmt).format(cal.getTime())).append(" ");

                    /* Starttime less than 05:00 the next day */
                    cal.setTimeInMillis(cal.getTimeInMillis() + MILLIS_IN_DAY);
                    filter.append("+starttime:<").append(new SimpleDateFormat("yyyy-MM-dd'T05:00:00Z'").format(cal.getTime())).append(" ");

                    /* Use categories navigator to select categories to display */
                    if (config.getWaitOnAllChannels() != null) {
                        final Modifier modifier = (Modifier) wosr.getModifiers("categories").get(index);
                        final Navigator navigator = modifier.getNavigator();
                        filter.append("+").append(navigator.getField()).append(":").append(modifier.getName()).append(" ");

                        /* Only include the top 5 channels */
                        filter.append("+(");
                        final int maxIdx = wosr.getModifiers("channels").size() < 5 ? wosr.getModifiers("channels").size() : 5;
                        for (Modifier channelModifier : wosr.getModifiers("channels").subList(0, maxIdx)) {
                            final Navigator channelNavigator = channelModifier.getNavigator();
                            filter.append(channelNavigator.getField()).append(":").append(channelModifier.getName()).append(" ");
                        }
                        filter.append(") ");
                    }

                    
                }

                if (!useAllChannels && getMyChannels().length > 0) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("+(");
                    for (String str : getMyChannels()) {
                        sb.append(" sgeneric5nav:'");
                        sb.append(str);
                        sb.append("'");
                    }
                    filter.append(sb);
                    filter.append(") ");
                }
                
                additionalFilter = filter.toString();
            }
        }
        return additionalFilter;
    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------


    /** Return offset to use when collecting results.
     * @return Will always return 0
     */
    protected int getCurrentOffset(final int i) {

        return 0;
    }


    /**
     * Add search category navigator parameter.
     * 
     * If using my channels the category navigator will be set to the highest 
     * category that contains selected channels. Otherwise the highest ranked category will be used.
     * 
     * @param useMyChannels use selected channels or not
     * @return filter for highest ranked category
     */
    private final String addChannelCategoryNavigator(final boolean useMyChannels) {

        final Navigator navigator = getNavigators().get("channelcategories");
        if (navigator == null) {
            return "";
        }
       
        final Map<String,StringDataObject> parameters = datamodel.getParameters().getValues();
        if (categories.isEmpty()) {
            if (useMyChannels) {
                final Site site = datamodel.getSite().getSite();
                final Channels.Context siteContext = ContextWrapper.wrap(
                    Channels.Context.class,
                    new SiteContext() {
                        public Site getSite() {
                            return site;
                        }
                });

                Channels channels = Channels.valueOf(siteContext);
                for (String id : getMyChannels()) {
                    final Channel channel = channels.getChannel(id);
                    if (channel != null && !categories.contains(channel.getCategory())) {
                        categories.add(channel.getCategory());
                    }
                }

                Collections.sort(categories);
            } else {
                for(Channel.Category category : Channel.Category.values()) {
                    categories.add(category);
                }
                Collections.sort(categories);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(" +").append(navigator.getField()).append(":'");
        
        /* TODO: This is not a good solution. We need to store the posible categories in a database together 
         * with the list of channels. */
        switch(categories.get(0)) {
        case NORWEGIAN:
            sb.append("Norske");
            break;
        case NORDIC:
            sb.append("Nordiske");
            break;
        case INTERNATIONAL:
            sb.append("Internasjonale");
            break;
        case NEWS:
            sb.append("Nyheter");
            break;
        case MOVIE:
            sb.append("Film");
            break;
        case SPORT:
            sb.append("Sport");
            break;
        case MUSIC:
            sb.append("Musikk");
            break;
        case NATURE:
            sb.append("Natur/reise");
            break;
        case CHILDREN:
            sb.append("Barn");
            break;
        }
        sb.append("' ");
        
        return sb.toString();
    }

    private final String getCacheKey() {
        
        final int day = datamodel.getParameters().getValue("day") != null 
                ? Integer.parseInt((String) datamodel.getParameters().getValue("day").getString()) : 0;
        final StringBuilder sb = new StringBuilder();
        sb.append("child/").append(userSortBy.name()).append("/");
        int maxIdx = 0;
        switch(userSortBy) {
        case CHANNEL:
            sb.append(day);
            sb.append("/").append(((Modifier) wosr.getModifiers("channels").get(index)).getName());
            break;
        case DAY:
            sb.append(index);
            maxIdx = wosr.getModifiers("channels").size() < 5 ? wosr.getModifiers("channels").size() : 5;
            for (Modifier modifier : wosr.getModifiers("channels").subList(0, maxIdx)) {
                sb.append("/").append(modifier.getName());
            }
            break;
        case CATEGORY:
            sb.append(day);
            final Modifier modifier = (Modifier) wosr.getModifiers("categories").get(index);
            sb.append("/").append(modifier.getName());
            maxIdx = wosr.getModifiers("channels").size() < 5 ? wosr.getModifiers("channels").size() : 5;
            for (Modifier channelModifier : wosr.getModifiers("channels").subList(0, maxIdx)) {
                sb.append("/").append(channelModifier.getName());
            }
            break;
        }
        return sb.toString();
    }
    
    private final ResultList<ResultItem> getResultsFromCache(final String cacheKey) {
       
        ResultList<ResultItem> sr;
        try {
            final BasicResultList<? extends ResultItem> tmpsr = (BasicResultList<? extends ResultItem>) CACHE.getFromCache(cacheKey, REFRESH_PERIOD);
            if (getNavigators() != null) {
                for (String navigatorKey : getNavigators().keySet()) {

                    addNavigatedTo(navigatorKey, datamodel.getParameters().getValues().containsKey("nav_" + navigatorKey)
                            ? datamodel.getParameters().getValue("nav_" + navigatorKey).getString()
                            : null);
                }
            }

            sr = new FastSearchResult<ResultItem>(this);
            sr.addResults(tmpsr.getResults());
            sr.setHitCount(tmpsr.getHitCount());
        } catch (NeedsRefreshException e) {
            
            boolean updatedCache = false;
            try{
                sr = (ResultList<ResultItem>) super.execute();
                CACHE.putInCache(cacheKey, sr);
                updatedCache = true;
            }finally{
                if(!updatedCache){ 
                    // prevents a deadlock in CACHE!
                    CACHE.cancelUpdate(cacheKey);
                }
            }
        }
        return sr;
    }

    // Inner classes -------------------------------------------------

}
