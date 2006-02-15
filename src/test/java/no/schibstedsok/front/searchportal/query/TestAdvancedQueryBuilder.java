// Copyright (2005-2006) Schibsted Søk AS
package no.schibstedsok.front.searchportal.query;

import junit.framework.TestCase;
import no.schibstedsok.front.searchportal.query.run.AdvancedQueryBuilder;

/**
 * TestAdvancedQueryBuilder is part of no.schibstedsok.front.searchportal.query package.
 *
 * @author Ola Marius Sagli <a href="ola@schibstedsok.no">ola at schibstedsok</a>
 * @version 0.1
 * @vesrion $Revision$, $Author$, $Date$
 */
public final class TestAdvancedQueryBuilder extends TestCase {

    /** Test a basic query combination.
     */
    public void testQuery() {


        final String qAll = "ola marius";
        final String qAny = "hoff sagli";
        String qPhrase = null;
        String qNot = null;



        AdvancedQueryBuilder qb =
                new AdvancedQueryBuilder(qAll,
                                         qPhrase,
                                         qAny,
                                         qNot);

        assertEquals("ola AND marius OR hoff OR sagli", qb.getQuery());
        qb = new AdvancedQueryBuilder();

        qb.setQueryNot("ola marius hoff sagli");
        assertEquals("NOT ola ANDNOT marius ANDNOT hoff ANDNOT sagli", qb.getQuery());

        qb.setQueryPhrase("dette er en test");
        assertEquals("NOT ola ANDNOT marius ANDNOT hoff ANDNOT sagli AND \"dette er en test\"", qb.getQuery());

    }

    /** Test advanced query detection works.
     */
    public void testIsAdvanced () {
        assertTrue(AdvancedQueryBuilder.isAdvancedQuery("ola AND marius NOT sagli"));
        assertFalse(AdvancedQueryBuilder.isAdvancedQuery("ola marius hoff sagli"));
    }


    /** Test duplicate whitespace can be normalised.
     */
    public void testRemoveDups() {
        assertEquals("marius", AdvancedQueryBuilder.trimDuplicateSpaces("           marius").trim());
        assertEquals("\" marius\"", AdvancedQueryBuilder.trimDuplicateSpaces("\"           marius\"").trim());

    }
}
