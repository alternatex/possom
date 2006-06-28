/*
 * Copyright (2005-2006) Schibsted Søk AS
 */
package no.schibstedsok.front.searchportal.analyzer;

import no.schibstedsok.front.searchportal.query.analyser.AnalysisRule;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;

import no.schibstedsok.front.searchportal.TestCase;

/**
 * A test class for {@link no.schibstedsok.front.searchportal.analyzer.AnalysisRule}.
 *
 * @author <a href="magnus.eklund@sesam.no">Magnus Eklund</a>
 * @version $Revision$
 */
public class AnalysisRuleTest extends TestCase {

    private AnalysisRule rule = null;
    private Predicate truePredicate = PredicateUtils.truePredicate();
    private Predicate falsePredicate = PredicateUtils.falsePredicate();

    public AnalysisRuleTest(final String testName) {
        super(testName);
    }	 
    
    protected void setUp() throws Exception {
        super.setUp();
        this.rule = new AnalysisRule();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for 'no.schibstedsok.front.searchportal.analyzer.AnalysisRule.addPredicateScore(Predicate, int)'.
     */
    public void testAddPredicateScore() {
        rule.addPredicateScore(truePredicate, 0);
    }

}
