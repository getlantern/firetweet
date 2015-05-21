package org.getlantern.querybuilder.query;

import org.getlantern.querybuilder.SQLLang;

public interface IBuilder<T extends SQLLang> {

    public T build();

    /**
     * Equivalent to {@link #build()}.{@link SQLLang#getSQL()}
     *
     * @return
     */
    public String buildSQL();

}
