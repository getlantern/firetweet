package org.getlantern.querybuilder.query;

public class SQLDropTriggerQuery extends SQLDropQuery {

    public SQLDropTriggerQuery(final boolean dropIfExists, final String table) {
        super(dropIfExists, "TRIGGER", table);
    }

}
