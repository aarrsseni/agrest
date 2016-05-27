package com.nhl.link.rest.it.fixture.cayenne.auto;

import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.exp.Property;

import com.nhl.link.rest.it.fixture.cayenne.E23;

/**
 * Class _E22 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _E22 extends CayenneDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "id";

    public static final Property<String> NAME = new Property<String>("name");
    public static final Property<List<E23>> E23S = new Property<List<E23>>("e23s");

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

    public void addToE23s(E23 obj) {
        addToManyTarget("e23s", obj, true);
    }
    public void removeFromE23s(E23 obj) {
        removeToManyTarget("e23s", obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<E23> getE23s() {
        return (List<E23>)readProperty("e23s");
    }


}
