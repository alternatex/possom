/* Copyright (2012) Schibsted ASA
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
 */
/*
 * SearchResultModule.java
 */

package no.sesat.search.view.output.syndication.modules;

import com.sun.syndication.feed.module.Module;

/**
 * This interface describes the additional fields defined in the sesam
 * syndication feed format.
 */
public interface SearchResultModule extends Module {

    public static final String URI = "http://www.sesam.no/rss/ns/search/1.0";
    public static final String PREFIX = "sesam";
    String ELEM_NUMBER_OF_HITS = "numberOfHits";
    String ELEM_ARTICLE_AGE = "articleAge";
    String ELEM_NEWS_SOURCE = "newsSource";

    /**
     * Returns the number of hits for the search.
     */
    String getNumberOfHits();

    /**
     * Sets the number of hits.
     *
     * @param numberOfHits The number of hits.
     */
    void setNumberOfHits(String numberOfHits);

    /**
     * Returns the age of the article.
     *
     * @return The age or null if not applicable.
     */
    String getArticleAge();

    /**
     * Sets the article age.
     *
     * @param articleAge The article age.
     */
    void setArticleAge(String articleAge);

    /**
     * Sets the news source.
     *
     * @param newsSource The news source.
     */
    void setNewsSource(String newsSource);

    /**
     * Returns the news source (newspaper name).
     *
     * @return The news paper name or null if not applicable.
     */
    String getNewsSource();
}
