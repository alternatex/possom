/*
 * AdvancedFastSearchCommand.java
 *
 * Created on May 30, 2006, 3:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package no.schibstedsok.searchportal.command;

import java.util.Map;

/**
 * A search command used for querrying FAST ESP 5.0 query servers.  
 */
public class AdvancedFastSearchCommand extends AbstractAdvancedFastSearchCommand {
    
    /** Creates a new instance of FastSearchCommand
     *
     * @param cxt Search command context.
     * @param parameters Search command parameters.
     */
    public AdvancedFastSearchCommand(final Context cxt, final Map parameters) {
        super(cxt, parameters);
    }
}